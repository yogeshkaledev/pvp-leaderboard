package com.pvp.leaderboard;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentHashMap;

public class RankOverlay extends Overlay
{
    private final Client client;
    private final PvPLeaderboardConfig config;
    private final RankCacheService rankCache;
    private final ConcurrentHashMap<String, String> displayedRanks = new ConcurrentHashMap<>();
    private int offsetX = 0;
    private int offsetY = -30;
    private boolean dragging = false;
    private java.awt.Point dragStart = null;
    
    @Inject
    public RankOverlay(Client client, PvPLeaderboardConfig config, RankCacheService rankCache)
    {
        this.client = client;
        this.config = config;
        this.rankCache = rankCache;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }
    
    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showRankIcons())
        {
            return null;
        }
        
        for (Player player : client.getPlayers())
        {
            if (player == client.getLocalPlayer() || player.getName() == null)
            {
                continue;
            }
            
            String playerName = player.getName();
            String cachedRank = displayedRanks.get(playerName);
            
            if (cachedRank == null)
            {
                // Fetch rank asynchronously
                rankCache.getPlayerRank(playerName, config.rankBucket()).thenAccept(rank -> {
                    if (rank != null)
                    {
                        displayedRanks.put(playerName, rank);
                    }
                });
                continue;
            }
            
            Point playerLocation = player.getCanvasTextLocation(graphics, "", player.getLogicalHeight() + 40);
            if (playerLocation != null)
            {
                int x = playerLocation.getX() + offsetX;
                int y = playerLocation.getY() + offsetY;
                
                renderRankIcon(graphics, cachedRank, x, y);
            }
        }
        
        return null;
    }
    
    public Dimension onMousePressed(MouseEvent mouseEvent)
    {
        if (mouseEvent.getButton() == MouseEvent.BUTTON1)
        {
            dragging = true;
            dragStart = mouseEvent.getPoint();
        }
        return null;
    }
    
    public Dimension onMouseReleased(MouseEvent mouseEvent)
    {
        dragging = false;
        dragStart = null;
        return null;
    }
    
    public Dimension onMouseDragged(MouseEvent mouseEvent)
    {
        if (dragging && dragStart != null)
        {
            java.awt.Point current = mouseEvent.getPoint();
            offsetX += current.x - dragStart.x;
            offsetY += current.y - dragStart.y;
            dragStart = current;
        }
        return null;
    }
    
    private void renderRankIcon(Graphics2D graphics, String rank, int x, int y)
    {
        String[] parts = rank.split(" ");
        String rankName = parts[0];
        
        Color rankColor = getRankColor(rankName);
        graphics.setColor(rankColor);
        graphics.fillOval(x - 8, y - 8, 16, 16);
        
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        FontMetrics fm = graphics.getFontMetrics();
        String displayText = getRankAbbreviation(rankName);
        int textWidth = fm.stringWidth(displayText);
        graphics.drawString(displayText, x - textWidth / 2, y + 3);
    }
    
    private Color getRankColor(String rank)
    {
        switch (rank)
        {
            case "Bronze": return new Color(184, 115, 51);
            case "Iron": return new Color(192, 192, 192);
            case "Steel": return new Color(154, 162, 166);
            case "Black": return new Color(46, 46, 46);
            case "Mithril": return new Color(59, 167, 214);
            case "Adamant": return new Color(26, 139, 111);
            case "Rune": return new Color(78, 159, 227);
            case "Dragon": return new Color(229, 57, 53);
            case "3rd Age": return new Color(229, 193, 0);
            default: return Color.GRAY;
        }
    }
    
    private String getRankAbbreviation(String rank)
    {
        switch (rank)
        {
            case "Bronze": return "Br";
            case "Iron": return "Ir";
            case "Steel": return "St";
            case "Black": return "Bl";
            case "Mithril": return "Mi";
            case "Adamant": return "Ad";
            case "Rune": return "Ru";
            case "Dragon": return "Dr";
            case "3rd Age": return "3A";
            default: return "?";
        }
    }
}