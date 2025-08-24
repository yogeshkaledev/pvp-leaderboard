package com.pvp.leaderboard;

import net.runelite.client.ui.PluginPanel;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Color;

public class DashboardPanel extends PluginPanel
{
    private JTable matchHistoryTable;
    private DefaultTableModel tableModel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel playerNameLabel;
    private final JProgressBar[] progressBars;
    private final JLabel[] progressLabels;
    private JPanel statsGrid;
    private JLabel winPercentLabel;
    private JLabel tiesLabel;
    private JLabel killsLabel;
    private JLabel deathsLabel;
    private JLabel kdLabel;
    private JPanel chartPanel;
    private java.util.List<Double> winRateHistory = new java.util.ArrayList<>();
    private JPanel additionalStatsPanel;
    private boolean isLoggedIn = false;
    private String idToken = null;
    private String accessToken = null;
    private JLabel highestRankLabel;
    private JLabel highestRankTimeLabel;
    private JLabel lowestRankLabel;
    private JLabel lowestRankTimeLabel;
    private JPanel tierGraphPanel;
    private String selectedBucket = "overall";
    private java.util.List<Double> tierHistory = new java.util.ArrayList<>();
    private JsonArray allMatches = null;
    private JButton[] bucketButtons = new JButton[5];

    private PvPLeaderboardConfig config;
    
    public DashboardPanel(PvPLeaderboardConfig config)
    {
        this.config = config;
        progressBars = new JProgressBar[4];
        progressLabels = new JLabel[4];
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(createMainPanel());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void showAdditionalStats(boolean show)
    {
        isLoggedIn = show;
        if (additionalStatsPanel != null)
        {
            additionalStatsPanel.setVisible(show);
            revalidate();
            repaint();
        }
    }
    
    private JPanel createMainPanel()
    {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Auth Bar (Login Section)
        JPanel authContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        authContainer.add(createAuthBar());
        mainPanel.add(authContainer);
        mainPanel.add(Box.createVerticalStrut(16));
        
        // Profile Header
        mainPanel.add(createProfileHeader());
        mainPanel.add(Box.createVerticalStrut(24));
        
        // Rank Progress Section
        mainPanel.add(createRankProgressSection());
        mainPanel.add(Box.createVerticalStrut(24));
        
        // Performance Overview
        mainPanel.add(createPerformanceOverview());
        mainPanel.add(Box.createVerticalStrut(24));
        
        // Additional Stats
        mainPanel.add(createAdditionalStats());
        mainPanel.add(Box.createVerticalStrut(24));
        
        // Match History
        mainPanel.add(createMatchHistory());
        
        return mainPanel;
    }
    
    private JPanel createAuthBar()
    {
        JPanel authBar = new JPanel();
        authBar.setLayout(new BoxLayout(authBar, BoxLayout.Y_AXIS));
        authBar.setBorder(BorderFactory.createTitledBorder("Login for Additional Stats"));
        authBar.setMaximumSize(new Dimension(200, 80));
        authBar.setPreferredSize(new Dimension(200, 80));
        
        authBar.add(new JLabel("Username (optional):"));
        usernameField = new JTextField();
        usernameField.setMaximumSize(new Dimension(180, 25));
        authBar.add(usernameField);
        
        authBar.add(Box.createVerticalStrut(5));
        
        loginButton = new JButton("Login for Additional Stats");
        loginButton.setMaximumSize(new Dimension(180, 25));
        loginButton.addActionListener(e -> handleLogin());
        authBar.add(loginButton);
        
        return authBar;
    }
    
    private JPanel createProfileHeader()
    {
        JPanel header = new JPanel(new BorderLayout());
        
        playerNameLabel = new JLabel("Player Name");
        playerNameLabel.setFont(playerNameLabel.getFont().deriveFont(Font.BOLD, 18f));
        header.add(playerNameLabel, BorderLayout.NORTH);
        
//        JPanel bucketRanks = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        bucketRanks.add(new JLabel("Overall: Rune 3"));
//        bucketRanks.add(new JLabel("NH: Rune 3"));
//        bucketRanks.add(new JLabel("Veng: Rune 3"));
//        bucketRanks.add(new JLabel("Multi: Rune 3"));
//        header.add(bucketRanks, BorderLayout.CENTER);
        
        return header;
    }
    
    private JPanel createRankProgressSection()
    {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("Rank Progress"));
        
        String[] buckets = {"Overall", "NH", "Veng", "Multi"};
        
        for (int i = 0; i < buckets.length; i++)
        {
            JPanel bucketPanel = new JPanel(new BorderLayout());
            
            progressLabels[i] = new JLabel(buckets[i] + " - Loading...");
            progressLabels[i].setFont(progressLabels[i].getFont().deriveFont(Font.BOLD));
            bucketPanel.add(progressLabels[i], BorderLayout.NORTH);
            
            progressBars[i] = new JProgressBar(0, 100);
            progressBars[i].setValue(0);
            progressBars[i].setStringPainted(true);
            progressBars[i].setString("0%");
            progressBars[i].setPreferredSize(new Dimension(0, 16));
            bucketPanel.add(progressBars[i], BorderLayout.CENTER);
            
            section.add(bucketPanel);
            if (i < buckets.length - 1) section.add(Box.createVerticalStrut(8));
        }
        
        return section;
    }
    
    private JPanel createPerformanceOverview()
    {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Performance Overview"));
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // Stats summary row
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        winPercentLabel = new JLabel("Win %: -");
        tiesLabel = new JLabel("Ties: -");
        killsLabel = new JLabel("Kills: -");
        deathsLabel = new JLabel("Deaths: -");
        kdLabel = new JLabel("KD: -");
        
        summaryPanel.add(winPercentLabel);
        summaryPanel.add(tiesLabel);
        summaryPanel.add(killsLabel);
        summaryPanel.add(deathsLabel);
        summaryPanel.add(kdLabel);
        
        contentPanel.add(summaryPanel);
        contentPanel.add(Box.createVerticalStrut(8));
        
        // Win rate chart
        chartPanel = createWinRateChart();
        contentPanel.add(chartPanel);
        
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(0, 300));
        
        mainPanel.add(scrollPane);
        return mainPanel;
    }
    
    private JPanel createAdditionalStats()
    {
        additionalStatsPanel = new JPanel();
        additionalStatsPanel.setLayout(new BoxLayout(additionalStatsPanel, BoxLayout.Y_AXIS));
        additionalStatsPanel.setBorder(BorderFactory.createTitledBorder("Additional Stats"));
        additionalStatsPanel.setVisible(false); // Hidden by default
        
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        
        JPanel highestRank = new JPanel();
        highestRank.setLayout(new BoxLayout(highestRank, BoxLayout.Y_AXIS));
        highestRank.add(new JLabel("Highest Rank Defeated"));
        highestRankLabel = new JLabel("-");
        highestRankTimeLabel = new JLabel("-");
        highestRank.add(highestRankLabel);
        highestRank.add(highestRankTimeLabel);
        
        JPanel lowestRank = new JPanel();
        lowestRank.setLayout(new BoxLayout(lowestRank, BoxLayout.Y_AXIS));
        lowestRank.add(new JLabel("Lowest Rank Lost To"));
        lowestRankLabel = new JLabel("-");
        lowestRankTimeLabel = new JLabel("-");
        lowestRank.add(lowestRankLabel);
        lowestRank.add(lowestRankTimeLabel);
        
        statsPanel.add(highestRank);
        statsPanel.add(lowestRank);
        
        additionalStatsPanel.add(statsPanel);
        additionalStatsPanel.add(Box.createVerticalStrut(16));
        
        // Tier Graph Section
        JPanel tierSection = new JPanel();
        tierSection.setLayout(new BoxLayout(tierSection, BoxLayout.Y_AXIS));
        
        // Create container with header and graph in same scrollable area
        JPanel graphContainer = new JPanel();
        graphContainer.setLayout(new BoxLayout(graphContainer, BoxLayout.Y_AXIS));
        
        JPanel tierHeader = new JPanel(new BorderLayout());
        tierHeader.add(new JLabel("Tier Graph"), BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        String[] buckets = {"Overall", "NH", "Veng", "Multi", "DMM"};
        for (int i = 0; i < buckets.length; i++) {
            String bucket = buckets[i];
            JButton btn = new JButton(bucket);
            btn.setPreferredSize(new Dimension(60, 24));
            btn.addActionListener(e -> {
                selectedBucket = bucket.toLowerCase();
                updateBucketButtonStates(bucket);
                if (allMatches != null) {
                    updateTierGraph(allMatches);
                }
            });
            bucketButtons[i] = btn;
            buttonPanel.add(btn);
        }
        updateBucketButtonStates("Overall");
        tierHeader.add(buttonPanel, BorderLayout.EAST);
        
        graphContainer.add(tierHeader);
        graphContainer.add(Box.createVerticalStrut(8));
        
        tierGraphPanel = createTierGraph();
        graphContainer.add(tierGraphPanel);
        
        JScrollPane tierScrollPane = new JScrollPane(graphContainer);
        tierScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        tierScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tierScrollPane.setPreferredSize(new Dimension(0, 320));
        tierSection.add(tierScrollPane);
        
        additionalStatsPanel.add(tierSection);
        return additionalStatsPanel;
    }
    
    private JPanel createMatchHistory()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Match History"));
        
        String[] columns = {"Result", "Opponent", "Match Type", "Match", "Change", "Time"};
        tableModel = new DefaultTableModel(columns, 0);
        matchHistoryTable = new JTable(tableModel);
        matchHistoryTable.setFillsViewportHeight(true);
        matchHistoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        JScrollPane scrollPane = new JScrollPane(matchHistoryTable);
        scrollPane.setPreferredSize(new Dimension(0, 300));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void handleLogin()
    {
        if (isLoggedIn)
        {
            // Logout
            showAdditionalStats(false);
            loginButton.setText("Login for Additional Stats");
            usernameField.setEnabled(true);
            usernameField.setText("");
            clearTokens();
            return;
        }
        
        // Use Cognito OAuth flow
        try
        {
            CognitoAuthService authService = new CognitoAuthService();
            authService.login().thenAccept(success -> {
                if (success) {
                    SwingUtilities.invokeLater(() -> completeLogin());
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Login failed", "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Login error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
                return null;
            });
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, "Failed to open login page", "Login Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void loadMatchHistory(String playerId)
    {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                try
                {
                    loadPlayerStats(playerId);
                    
                    String apiUrl = "https://kekh0x6kfk.execute-api.us-east-1.amazonaws.com/prod/matches?player_id=" + playerId + "&limit=500";
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
                    
                    JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                    JsonArray matches = jsonResponse.getAsJsonArray("matches");
                    
                    SwingUtilities.invokeLater(() ->
                    {
                        tableModel.setRowCount(0);
                        int wins = 0, losses = 0, ties = 0;
                        
                        for (int i = 0; i < matches.size(); i++)
                        {
                            JsonObject match = matches.get(i).getAsJsonObject();
                            String result = match.has("result") ? match.get("result").getAsString() : "";
                            String opponent = match.has("opponent_id") ? match.get("opponent_id").getAsString() : "";
                            String matchType = match.has("bucket") ? match.get("bucket").getAsString().toUpperCase() : "Unknown";
                            String playerRank = computeRank(match, "player_");
                            String opponentRank = computeRank(match, "opponent_");
                            String matchDisplay = playerRank + " vs " + opponentRank;
                            String change = computeRatingChange(match);
                            String time = match.has("when") ? formatTime(match.get("when").getAsLong()) : "";
                            
                            // Count match results
                            if ("win".equalsIgnoreCase(result)) wins++;
                            else if ("loss".equalsIgnoreCase(result)) losses++;
                            else if ("tie".equalsIgnoreCase(result)) ties++;
                            
                            tableModel.addRow(new Object[]{result, opponent, matchType, matchDisplay, change, time});
                        }
                        
                        updatePerformanceStats(wins, losses, ties);
                        updateWinRateChart(matches);
                        
                        // Update additional stats if logged in
                        if (isLoggedIn) {
                            allMatches = matches;
                            updateAdditionalStats(matches);
                        }
                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }
    
    private String computeRank(JsonObject match, String prefix)
    {
        if (match.has(prefix + "rank"))
        {
            String rank = match.get(prefix + "rank").getAsString();
            int division = match.has(prefix + "division") ? match.get(prefix + "division").getAsInt() : 0;
            return rank + (division > 0 ? " " + division : "");
        }
        return "Unknown";
    }
    
    private String computeRatingChange(JsonObject match)
    {
        if (match.has("rating_change"))
        {
            JsonObject ratingChange = match.getAsJsonObject("rating_change");
            if (ratingChange.has("mmr_delta"))
            {
                double delta = ratingChange.get("mmr_delta").getAsDouble();
                return String.format("%+.2f MMR", delta);
            }
        }
        return "-";
    }
    
    private String formatTime(long timestamp)
    {
        return new SimpleDateFormat("MM/dd/yyyy, HH:mm:ss").format(new Date(timestamp * 1000));
    }
    
    public String getUsername()
    {
        return usernameField.getText();
    }
    
    public String getPassword()
    {
        return new String(passwordField.getPassword());
    }
    
    private void loadPlayerStats(String playerId)
    {
        try
        {
            String apiUrl = "https://kekh0x6kfk.execute-api.us-east-1.amazonaws.com/prod/user?player_id=" + playerId;
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
            
            JsonObject stats = JsonParser.parseString(response.toString()).getAsJsonObject();
            SwingUtilities.invokeLater(() -> updateProgressBars(stats));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private void updateProgressBars(JsonObject stats)
    {
        if (stats.has("mmr"))
        {
            double mmr = stats.get("mmr").getAsDouble();
            RankInfo rankInfo = calculateRankFromMMR(mmr);
            updateProgressBar(0, "Overall", rankInfo.rank, rankInfo.division, rankInfo.progress);
        }
        
        String playerName = null;
        if (stats.has("player_name"))
        {
            playerName = stats.get("player_name").getAsString();
        }
        else if (stats.has("player_id"))
        {
            playerName = stats.get("player_id").getAsString();
        }
        
        if (playerName != null)
        {
            String[] buckets = {"nh", "veng", "multi"};
            for (int i = 0; i < buckets.length; i++)
            {
                loadBucketStats(playerName, buckets[i], i + 1);
            }
        }
    }
    
    private void loadBucketStats(String playerName, String bucket, int index)
    {
        SwingWorker<RankInfo, Void> worker = new SwingWorker<RankInfo, Void>()
        {
            @Override
            protected RankInfo doInBackground() throws Exception
            {
                try
                {
                    String apiUrl = "https://kekh0x6kfk.execute-api.us-east-1.amazonaws.com/prod/leaderboard?bucket=" + bucket + "&limit=550";
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
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                return new RankInfo("Bronze", 3, 0);
            }
            
            @Override
            protected void done()
            {
                try
                {
                    RankInfo rankInfo = get();
                    String bucketName = bucket.substring(0, 1).toUpperCase() + bucket.substring(1);
                    updateProgressBar(index, bucketName.toUpperCase(), rankInfo.rank, rankInfo.division, rankInfo.progress);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void updateProgressBar(int index, String bucketName, String rank, int division, double progress)
    {
        String rankText = rank + (division > 0 ? " " + division : "");
        progressLabels[index].setText(bucketName + " - " + rankText + " (" + String.format("%.1f", progress) + "%)");
        progressBars[index].setValue((int) progress);
        progressBars[index].setString(String.format("%.1f%%", progress));
    }
    
    private RankInfo calculateRankFromMMR(double mmr)
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
        double progress = 0;
        
        if (!rank.equals("3rd Age"))
        {
            int currentIndex = -1;
            for (int i = 0; i < thresholds.length; i++)
            {
                if (thresholds[i][0].equals(rank) && thresholds[i][1].equals(String.valueOf(division)))
                {
                    currentIndex = i;
                    break;
                }
            }
            
            if (currentIndex >= 0 && currentIndex < thresholds.length - 1)
            {
                double currentThreshold = Double.parseDouble(current[2]);
                double nextThreshold = Double.parseDouble(thresholds[currentIndex + 1][2]);
                double span = nextThreshold - currentThreshold;
                progress = Math.max(0, Math.min(100, ((mmr - currentThreshold) / span) * 100));
            }
        }
        else
        {
            progress = 100;
        }
        
        return new RankInfo(rank, division, progress);
    }
    
    private void startTokenPolling()
    {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                // Poll for token from callback (simulate OAuth flow completion)
                for (int i = 0; i < 30; i++) // Poll for 30 seconds
                {
                    Thread.sleep(1000);
                    
                    // Check if callback completed (in real implementation, check callback server/file)
                    if (checkCallbackCompletion())
                    {
                        SwingUtilities.invokeLater(() -> completeLogin());
                        break;
                    }
                }
                return null;
            }
        };
        worker.execute();
    }
    
    private boolean checkCallbackCompletion()
    {
        // Simulate callback completion after 3 seconds
        // In real implementation, check callback endpoint or local server
        return System.currentTimeMillis() % 10000 > 3000;
    }
    
    private CognitoAuthService authService = new CognitoAuthService();
    
    private void completeLogin()
    {
        String username = usernameField.getText();
        if (username.isEmpty()) username = "Fx%20Zephrrr";
        
        playerNameLabel.setText(username);
        showAdditionalStats(true);
        loadMatchHistory(username);
        loginButton.setText("Logout");
        usernameField.setEnabled(false);
    }
    
    private void updateAdditionalStats(JsonArray matches)
    {
        String highestRankDefeated = null;
        String lowestRankLostTo = null;
        String highestTime = null;
        String lowestTime = null;
        
        for (int i = 0; i < matches.size(); i++)
        {
            JsonObject match = matches.get(i).getAsJsonObject();
            String result = match.has("result") ? match.get("result").getAsString().toLowerCase() : "";
            String opponentRank = computeRank(match, "opponent_");
            String time = match.has("when") ? formatTime(match.get("when").getAsLong()) : "";
            
            if ("win".equals(result))
            {
                if (highestRankDefeated == null || isHigherRank(opponentRank, highestRankDefeated))
                {
                    highestRankDefeated = opponentRank;
                    highestTime = time;
                }
            }
            else if ("loss".equals(result))
            {
                if (lowestRankLostTo == null || isLowerRank(opponentRank, lowestRankLostTo))
                {
                    lowestRankLostTo = opponentRank;
                    lowestTime = time;
                }
            }
        }
        
        highestRankLabel.setText(highestRankDefeated != null ? highestRankDefeated : "-");
        highestRankTimeLabel.setText(highestTime != null ? highestTime : "-");
        lowestRankLabel.setText(lowestRankLostTo != null ? lowestRankLostTo : "-");
        lowestRankTimeLabel.setText(lowestTime != null ? lowestTime : "-");
        
        // Update tier graph
        updateTierGraph(matches);
    }
    
    private boolean isHigherRank(String rank1, String rank2)
    {
        return getRankOrder(rank1) > getRankOrder(rank2);
    }
    
    private boolean isLowerRank(String rank1, String rank2)
    {
        return getRankOrder(rank1) < getRankOrder(rank2);
    }
    
    private int getRankOrder(String rank)
    {
        String[] parts = rank.split(" ");
        String baseName = parts[0];
        int division = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        
        int baseOrder;
        switch (baseName) {
            case "Bronze":
                baseOrder = 0;
                break;
            case "Iron":
                baseOrder = 1;
                break;
            case "Steel":
                baseOrder = 2;
                break;
            case "Black":
                baseOrder = 3;
                break;
            case "Mithril":
                baseOrder = 4;
                break;
            case "Adamant":
                baseOrder = 5;
                break;
            case "Rune":
                baseOrder = 6;
                break;
            case "Dragon":
                baseOrder = 7;
                break;
            case "3rd Age":
                baseOrder = 8;
                break;
            default:
                baseOrder = -1;
                break;
        }
        
        return baseOrder * 10 + (4 - division);
    }
    
    private JPanel createTierGraph()
    {
        JPanel panel = new JPanel()
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth() - 40;
                int height = getHeight() - 40;
                
                if (width <= 0 || height <= 0) return;
                
                // Draw axes
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawLine(20, height + 20, width + 20, height + 20);
                g2.drawLine(20, 20, 20, height + 20);
                
                // Draw tier lines and labels
                String[] tiers = {"Bronze", "Iron", "Steel", "Black", "Mithril", "Adamant", "Rune", "Dragon", "3rd Age"};
                Color[] tierColors = {
                    new Color(184, 115, 51), new Color(192, 192, 192), new Color(154, 162, 166),
                    new Color(46, 46, 46), new Color(59, 167, 214), new Color(26, 139, 111),
                    new Color(78, 159, 227), new Color(229, 57, 53), new Color(229, 193, 0)
                };
                
                for (int i = 0; i < tiers.length; i++)
                {
                    int y = 20 + (i * height / tiers.length);
                    g2.setColor(tierColors[i]);
                    g2.drawLine(20, y, width + 20, y);
                    g2.drawString(tiers[tiers.length - 1 - i], 2, y + 5);
                }
                
                // Draw X-axis labels (match numbers) - show every ~50th match for 500+ matches
                if (tierHistory.size() > 1)
                {
                    g2.setColor(Color.WHITE);
                    int totalMatches = tierHistory.size();
                    int labelInterval = Math.max(1, totalMatches / 8); // Show ~8 labels max
                    
                    for (int i = 0; i < totalMatches; i += labelInterval)
                    {
                        int x = 20 + (i * width / Math.max(1, totalMatches - 1));
                        int matchNum = totalMatches - i; // Reverse order (most recent first)
                        g2.drawString("#" + matchNum, x - 10, height + 35);
                    }
                    
                    // Draw tier progression line
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2));
                    for (int i = 0; i < tierHistory.size() - 1; i++)
                    {
                        int x1 = 20 + (i * width / Math.max(1, tierHistory.size() - 1));
                        int y1 = height + 20 - (int)(tierHistory.get(i) * height / 100);
                        int x2 = 20 + ((i + 1) * width / Math.max(1, tierHistory.size() - 1));
                        int y2 = height + 20 - (int)(tierHistory.get(i + 1) * height / 100);
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
                else
                {
                    g2.setColor(Color.GRAY);
                    g2.drawString("No tier data available", width / 2 - 60, height / 2);
                }
            }
        };
        panel.setPreferredSize(new Dimension(Math.max(800, tierHistory.size() * 2), 260));
        return panel;
    }
    
    private void updateTierGraph(JsonArray matches)
    {
        java.util.List<Double> tierData = new java.util.ArrayList<>();
        
        // Sort matches by timestamp for proper chronological order
        java.util.List<JsonObject> sortedMatches = new java.util.ArrayList<>();
        for (int i = 0; i < matches.size(); i++)
        {
            sortedMatches.add(matches.get(i).getAsJsonObject());
        }
        sortedMatches.sort((a, b) -> {
            long timeA = a.has("when") ? a.get("when").getAsLong() : 0;
            long timeB = b.has("when") ? b.get("when").getAsLong() : 0;
            return Long.compare(timeA, timeB);
        });
        
        // Cap at 500 matches maximum
        int maxMatches = Math.min(500, sortedMatches.size());
        
        for (int i = 0; i < maxMatches; i++)
        {
            JsonObject match = sortedMatches.get(i);
            String bucket = match.has("bucket") ? match.get("bucket").getAsString().toLowerCase() : "";
            
            if (selectedBucket.equals("overall") || selectedBucket.equals(bucket))
            {
                if (match.has("player_mmr"))
                {
                    double mmr = match.get("player_mmr").getAsDouble();
                    double tierValue = calculateTierValue(mmr);
                    tierData.add(tierValue);
                }
            }
        }
        
        tierHistory = tierData;
        updateTierGraphDisplay();
    }
    
    private void updateTierGraphDisplay()
    {
        if (tierGraphPanel != null)
        {
            // Update panel size for horizontal scrolling
            tierGraphPanel.setPreferredSize(new Dimension(Math.max(800, tierHistory.size() * 2), 260));
            tierGraphPanel.getParent().setPreferredSize(new Dimension(Math.max(800, tierHistory.size() * 2), 300));
            tierGraphPanel.revalidate();
            tierGraphPanel.repaint();
        }
    }
    
    private double calculateTierValue(double mmr)
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
        
        // Convert to percentage for graph display
        String[] tiers = {"Bronze", "Iron", "Steel", "Black", "Mithril", "Adamant", "Rune", "Dragon", "3rd Age"};
        for (int i = 0; i < tiers.length; i++)
        {
            if (tiers[i].equals(current[0]))
            {
                return (i * 100.0 / tiers.length) + (Integer.parseInt(current[1]) * 10.0 / tiers.length);
            }
        }
        return 0;
    }
    
    private void clearTokens()
    {
        authService.logout();
        idToken = null;
        accessToken = null;
    }
    
    private void updatePerformanceStats(int wins, int losses, int ties)
    {
        int totalMatches = wins + losses + ties;
        int nonTieMatches = wins + losses;
        
        double winPercent = nonTieMatches > 0 ? (wins * 100.0 / nonTieMatches) : 0;
        double kd = losses > 0 ? (wins / (double) losses) : (wins > 0 ? wins : 0);
        
        winPercentLabel.setText(String.format("Win %%: %.1f%%", winPercent));
        tiesLabel.setText("Ties: " + ties);
        killsLabel.setText("Kills: " + wins);
        deathsLabel.setText("Deaths: " + losses);
        kdLabel.setText(String.format("KD: %.1f", kd));
    }
    
    private JPanel createWinRateChart()
    {
        JPanel panel = new JPanel()
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth() - 40;
                int height = getHeight() - 40;
                
                if (width <= 0 || height <= 0) return;
                
                // Draw axes
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawLine(20, height + 20, width + 20, height + 20);
                g2.drawLine(20, 20, 20, height + 20);
                
                // Draw grid lines and Y-axis labels
                g2.setColor(Color.GRAY);
                for (int i = 0; i <= 10; i++)
                {
                    int y = 20 + (i * height / 10);
                    g2.drawLine(20, y, width + 20, y);
                    g2.setColor(Color.WHITE);
                    g2.drawString((100 - i * 10) + "%", 2, y + 5);
                    g2.setColor(Color.GRAY);
                }
                
                // Draw X-axis labels and win rate line
                if (winRateHistory.size() > 1)
                {
                    // Draw X-axis labels (match numbers)
                    g2.setColor(Color.WHITE);
                    int maxTicks = Math.min(8, winRateHistory.size());
                    for (int i = 0; i < maxTicks; i++)
                    {
                        int x = 20 + (i * width / (maxTicks - 1));
                        int matchNum = winRateHistory.size() - (i * winRateHistory.size() / (maxTicks - 1));
                        g2.drawString("#" + matchNum, x - 10, height + 35);
                    }
                    
                    // Draw win rate line
                    g2.setColor(new Color(255, 215, 0)); // Gold color like JS
                    g2.setStroke(new BasicStroke(2));
                    for (int i = 0; i < winRateHistory.size() - 1; i++)
                    {
                        int x1 = 20 + (i * width / (winRateHistory.size() - 1));
                        int y1 = height + 20 - (int)(winRateHistory.get(i) * height / 100);
                        int x2 = 20 + ((i + 1) * width / (winRateHistory.size() - 1));
                        int y2 = height + 20 - (int)(winRateHistory.get(i + 1) * height / 100);
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
                else
                {
                    g2.setColor(Color.GRAY);
                    g2.drawString("No match data available", width / 2 - 60, height / 2);
                }
            }
        };
        panel.setPreferredSize(new Dimension(Math.max(1024, winRateHistory.size() * 2), 280)); // Dynamic width based on data
        return panel;
    }
    
    private void updateWinRateChart(JsonArray matches)
    {
        java.util.List<Double> rolling = new java.util.ArrayList<>();
        
        // Calculate rolling win percentage for each match (20-match window like JS)
        for (int i = 0; i < matches.size(); i++)
        {
            int start = Math.max(0, i - 19);
            int winCount = 0, totalCount = 0;
            
            for (int j = start; j <= i; j++)
            {
                JsonObject match = matches.get(j).getAsJsonObject();
                String result = match.has("result") ? match.get("result").getAsString().toLowerCase() : "";
                if ("win".equals(result) || "loss".equals(result))
                {
                    totalCount++;
                    if ("win".equals(result)) winCount++;
                }
            }
            
            double winRate = totalCount > 0 ? (winCount * 100.0 / totalCount) : 0;
            rolling.add(winRate);
        }
        
        winRateHistory = rolling;
        
        if (chartPanel != null)
        {
            chartPanel.repaint();
        }
    }
    
    public void updateAdditionalStatsFromPlugin(String highestRankDefeated, String lowestRankLostTo)
    {
        if (isLoggedIn && additionalStatsPanel != null && additionalStatsPanel.isVisible())
        {
            SwingUtilities.invokeLater(() -> {
                if (highestRankDefeated != null)
                {
                    highestRankLabel.setText(highestRankDefeated);
                    highestRankTimeLabel.setText("Live tracking");
                }
                if (lowestRankLostTo != null)
                {
                    lowestRankLabel.setText(lowestRankLostTo);
                    lowestRankTimeLabel.setText("Live tracking");
                }
            });
        }
    }
    
    public void updateTierGraphRealTime(String bucket, double mmr)
    {
        if (isLoggedIn && (selectedBucket.equals("overall") || selectedBucket.equals(bucket.toLowerCase())))
        {
            SwingUtilities.invokeLater(() -> {
                double tierValue = calculateTierValue(mmr);
                tierHistory.add(tierValue);
                
                // Keep only last 500 points for performance
                if (tierHistory.size() > 500)
                {
                    tierHistory = tierHistory.subList(tierHistory.size() - 500, tierHistory.size());
                }
                
                updateTierGraphDisplay();
            });
        }
    }
    
    private void updateBucketButtonStates(String activeBucket)
    {
        String[] buckets = {"Overall", "NH", "Veng", "Multi", "DMM"};
        for (int i = 0; i < bucketButtons.length; i++)
        {
            if (bucketButtons[i] != null)
            {
                boolean isActive = buckets[i].equals(activeBucket);
                bucketButtons[i].setEnabled(!isActive);
                bucketButtons[i].setBackground(isActive ? Color.DARK_GRAY : null);
            }
        }
    }
    
    private static class RankInfo
    {
        String rank;
        int division;
        double progress;
        
        RankInfo(String rank, int division, double progress)
        {
            this.rank = rank;
            this.division = division;
            this.progress = progress;
        }
    }
}