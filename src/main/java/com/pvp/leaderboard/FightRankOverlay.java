package com.pvp.leaderboard;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class FightRankOverlay extends Overlay
{
    private final Client client;
    private final PvPLeaderboardPlugin plugin;
    private final PvPLeaderboardConfig config;
    private final PanelComponent panelComponent = new PanelComponent();
    
    @Inject
    private FightRankOverlay(Client client, PvPLeaderboardPlugin plugin, PvPLeaderboardConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setMovable(true);
        setSnappable(true);
    }
    
    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showFightRankOverlay())
        {
            return null;
        }
        
        panelComponent.getChildren().clear();
        
        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("Fight Ranks")
            .color(Color.YELLOW)
            .build());
        
        // Player rank
        String playerRank = plugin.getPlayerRankDisplay();
        if (playerRank != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("You:")
                .right(playerRank)
                .leftColor(Color.WHITE)
                .rightColor(plugin.getRankColor(playerRank))
                .build());
        }
        
        // Opponent rank
        String opponentName = plugin.getCurrentOpponentName();
        String opponentRank = plugin.getOpponentRankDisplay();
        if (opponentName != null && opponentRank != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left(opponentName + ":")
                .right(opponentRank)
                .leftColor(Color.WHITE)
                .rightColor(plugin.getRankColor(opponentRank))
                .build());
        }
        
        panelComponent.setPreferredSize(new Dimension(200, 0));
        return panelComponent.render(graphics);
    }
}