package com.pvp.leaderboard;

import net.runelite.client.ui.PluginPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class DashboardPanel extends PluginPanel
{
    private final JTable matchHistoryTable;
    private final DefaultTableModel tableModel;

    public DashboardPanel()
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD, 16f));
        usernameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(usernameLabel);
        
        topPanel.add(Box.createVerticalStrut(10));
        
        JLabel rankTitle = new JLabel("Rank Progress");
        rankTitle.setFont(rankTitle.getFont().deriveFont(Font.BOLD, 14f));
        rankTitle.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(rankTitle);
        
        topPanel.add(Box.createVerticalStrut(5));
        
        topPanel.add(new JLabel("Overall - Rune 3 (62.4%)"));
        topPanel.add(new JLabel("NH - Rune 3 (57.7%)"));
        topPanel.add(new JLabel("VENG - Rune 3 (68.4%)"));
        topPanel.add(new JLabel("MULTI - Rune 3 (75.6%)"));
        
        topPanel.add(Box.createVerticalStrut(10));
        
        JLabel performanceTitle = new JLabel("Performance Overview");
        performanceTitle.setFont(performanceTitle.getFont().deriveFont(Font.BOLD, 14f));
        performanceTitle.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(performanceTitle);
        
        topPanel.add(Box.createVerticalStrut(5));
        
        topPanel.add(new JLabel("Win % 100.0%"));
        topPanel.add(new JLabel("Ties 1"));
        topPanel.add(new JLabel("Kills 4"));
        topPanel.add(new JLabel("Deaths 0"));
        topPanel.add(new JLabel("KD 4"));
        
        add(topPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        String[] tierColumns = {"Opponent Tier", "Kills", "Deaths", "KD"};
        DefaultTableModel tierTableModel = new DefaultTableModel(tierColumns, 0);
        JTable tierTable = new JTable(tierTableModel);
        tierTable.setPreferredScrollableViewportSize(new Dimension(0, 80));
        
        JScrollPane tierScrollPane = new JScrollPane(tierTable);
        centerPanel.add(tierScrollPane, BorderLayout.NORTH);
        
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Additional Stats
        JLabel statsTitle = new JLabel("Additional Stats");
        statsTitle.setFont(statsTitle.getFont().deriveFont(Font.BOLD, 14f));
        statsTitle.setHorizontalAlignment(SwingConstants.CENTER);
        statsPanel.add(statsTitle);
        
        statsPanel.add(Box.createVerticalStrut(5));
        
        statsPanel.add(new JLabel("Highest Rank Defeated"));

        JLabel runeLabel = new JLabel("Rune 3");
        URL url = null;
        try {
            url = new URL("http://i.imgur.com/xiVXrCD.jpg");
            final BufferedImage bufferedImage = ImageIO.read(url);
            runeLabel.setIcon(new ImageIcon(bufferedImage));
        } catch (Exception e) {
            // ignore
        }
        statsPanel.add(runeLabel);
        statsPanel.add(new JLabel("23/4/2025, 6:59:27 AM"));
        statsPanel.add(new JLabel("Lowest Rank Lost To"));
        statsPanel.add(new JLabel("-"));
        statsPanel.add(new JLabel("-"));
        
        JPanel middlePanel = new JPanel(new BorderLayout());
        middlePanel.add(statsPanel, BorderLayout.NORTH);
        
        JLabel historyTitle = new JLabel("Match History");
        historyTitle.setFont(historyTitle.getFont().deriveFont(Font.BOLD, 14f));
        historyTitle.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(historyTitle, BorderLayout.NORTH);
        
        String[] columns = {"Result", "Opponent", "Match Type", "Match", "Change", "Time"};
        tableModel = new DefaultTableModel(columns, 0);
        matchHistoryTable = new JTable(tableModel);
        matchHistoryTable.setFillsViewportHeight(true);
        
        JScrollPane scrollPane = new JScrollPane(matchHistoryTable);
        historyPanel.add(scrollPane, BorderLayout.CENTER);
        
        middlePanel.add(historyPanel, BorderLayout.CENTER);
        centerPanel.add(middlePanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }
    
    public void addMatch(String result, String opponent, String matchType, String match, String change, String time)
    {
        tableModel.addRow(new Object[]{result, opponent, matchType, match, change, time});
    }
}