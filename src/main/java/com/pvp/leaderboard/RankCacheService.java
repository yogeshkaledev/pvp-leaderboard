package com.pvp.leaderboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RankCacheService
{
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours
    private static final String API_URL = "https://kekh0x6kfk.execute-api.us-east-1.amazonaws.com/prod/leaderboard";
    
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, CachedRank>> bucketCaches = new ConcurrentHashMap<>();
    
    public CompletableFuture<String> getPlayerRank(String playerName, String bucket)
    {
        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<String, CachedRank> bucketCache = bucketCaches.computeIfAbsent(bucket, k -> new ConcurrentHashMap<>());
            CachedRank cached = bucketCache.get(playerName.toLowerCase());
            
            if (cached != null && !cached.isExpired())
            {
                return cached.rank;
            }
            
            try
            {
                String rank = fetchPlayerRankFromAPI(playerName, bucket);
                bucketCache.put(playerName.toLowerCase(), new CachedRank(rank, System.currentTimeMillis()));
                return rank;
            }
            catch (Exception e)
            {
                log.error("Failed to fetch rank for player {} in bucket {}", playerName, bucket, e);
                return null;
            }
        });
    }
    
    private String fetchPlayerRankFromAPI(String playerName, String bucket) throws Exception
    {
        String apiUrl = API_URL + "?bucket=" + bucket + "&limit=550";
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
        {
            response.append(line);
        }
        reader.close();
        
        JsonObject data = JsonParser.parseString(response.toString()).getAsJsonObject();
        JsonArray players = data.getAsJsonArray("players");
        
        for (int i = 0; i < players.size(); i++)
        {
            JsonObject player = players.get(i).getAsJsonObject();
            if (player.get("player_name").getAsString().equalsIgnoreCase(playerName))
            {
                double mmr = player.get("mmr").getAsDouble();
                return calculateRankFromMMR(mmr);
            }
        }
        
        return null;
    }
    
    private String calculateRankFromMMR(double mmr)
    {
        String[][] thresholds = {
            {"Bronze", "3", "0"}, {"Bronze", "2", "170"}, {"Bronze", "1", "240"},
            {"Iron", "3", "310"}, {"Iron", "2", "380"}, {"Iron", "1", "450"},
            {"Steel", "3", "520"}, {"Steel", "2", "590"}, {"Steel", "1", "660"},
            {"Black", "3", "730"}, {"Black", "2", "800"}, {"Black", "1", "870"},
            {"Mithril", "3", "940"}, {"Mithril", "2", "1010"}, {"Mithril", "1", "1080"},
            {"Adamant", "3", "1150"}, {"Adamant", "2", "1250"}, {"Adamant", "1", "1350"},
            {"Rune", "3", "1450"}, {"Rune", "2", "1550"}, {"Rune", "1", "1650"},
            {"Dragon", "3", "1750"}, {"Dragon", "2", "1850"}, {"Dragon", "1", "1950"},
            {"3rd Age", "0", "2100"}
        };
        
        String[] current = thresholds[0];
        for (String[] threshold : thresholds)
        {
            if (mmr >= Double.parseDouble(threshold[2]))
            {
                current = threshold;
            }
            else
            {
                break;
            }
        }
        
        String rank = current[0];
        int division = Integer.parseInt(current[1]);
        return rank + (division > 0 ? " " + division : "");
    }
    
    private static class CachedRank
    {
        final String rank;
        final long timestamp;
        
        CachedRank(String rank, long timestamp)
        {
            this.rank = rank;
            this.timestamp = timestamp;
        }
        
        boolean isExpired()
        {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
}