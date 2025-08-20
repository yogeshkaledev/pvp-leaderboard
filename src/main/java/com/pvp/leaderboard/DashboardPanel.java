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

    public DashboardPanel()
    {
        progressBars = new JProgressBar[4];
        progressLabels = new JLabel[4];
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(createMainPanel());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
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
        authBar.setBorder(BorderFactory.createTitledBorder("Login"));
        authBar.setMaximumSize(new Dimension(200, 120));
        authBar.setPreferredSize(new Dimension(200, 120));
        
        authBar.add(new JLabel("Username:"));
        usernameField = new JTextField();
        usernameField.setMaximumSize(new Dimension(180, 25));
        authBar.add(usernameField);
        
        authBar.add(Box.createVerticalStrut(5));
        
        authBar.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        passwordField.setMaximumSize(new Dimension(180, 25));
        authBar.add(passwordField);
        
        authBar.add(Box.createVerticalStrut(5));
        
        loginButton = new JButton("Login");
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
        
        JPanel bucketRanks = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bucketRanks.add(new JLabel("Overall: Rune 3"));
        bucketRanks.add(new JLabel("NH: Rune 3"));
        bucketRanks.add(new JLabel("Veng: Rune 3"));
        bucketRanks.add(new JLabel("Multi: Rune 3"));
        header.add(bucketRanks, BorderLayout.CENTER);
        
        return header;
    }
    
    private JPanel createRankProgressSection()
    {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("Rank Progress"));
        
        String[] buckets = {"Overall", "NH", "Veng", "Multi"};
        String[] values = {"Rune 3 (62.4%)", "Rune 3 (57.7%)", "Rune 3 (68.4%)", "Rune 3 (75.6%)"};
        
        for (int i = 0; i < buckets.length; i++)
        {
            JPanel bucketPanel = new JPanel(new BorderLayout());
            
            progressLabels[i] = new JLabel(buckets[i] + " - " + values[i]);
            progressLabels[i].setFont(progressLabels[i].getFont().deriveFont(Font.BOLD));
            bucketPanel.add(progressLabels[i], BorderLayout.NORTH);
            
            progressBars[i] = new JProgressBar(0, 100);
            progressBars[i].setValue((int)(Math.random() * 100));
            progressBars[i].setStringPainted(false);
            progressBars[i].setPreferredSize(new Dimension(0, 12));
            bucketPanel.add(progressBars[i], BorderLayout.CENTER);
            
            section.add(bucketPanel);
            if (i < buckets.length - 1) section.add(Box.createVerticalStrut(8));
        }
        
        return section;
    }
    
    private JPanel createPerformanceOverview()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Performance Overview"));
        
        JPanel statsGrid = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsGrid.add(new JLabel("Win %: 100.0%"));
        statsGrid.add(new JLabel("Ties: 1"));
        statsGrid.add(new JLabel("Kills: 4"));
        statsGrid.add(new JLabel("Deaths: 0"));
        statsGrid.add(new JLabel("KD: 4.0"));
        
        panel.add(statsGrid);
        return panel;
    }
    
    private JPanel createAdditionalStats()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Additional Stats"));
        
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        
        JPanel highestRank = new JPanel();
        highestRank.setLayout(new BoxLayout(highestRank, BoxLayout.Y_AXIS));
        highestRank.add(new JLabel("Highest Rank Defeated"));
        highestRank.add(new JLabel("Rune 3"));
        highestRank.add(new JLabel("23/4/2025, 6:59:27 AM"));
        
        JPanel lowestRank = new JPanel();
        lowestRank.setLayout(new BoxLayout(lowestRank, BoxLayout.Y_AXIS));
        lowestRank.add(new JLabel("Lowest Rank Lost To"));
        lowestRank.add(new JLabel("-"));
        lowestRank.add(new JLabel("-"));
        
        statsPanel.add(highestRank);
        statsPanel.add(lowestRank);
        
        panel.add(statsPanel);
        return panel;
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
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        
        if (!username.isEmpty() && !password.isEmpty())
        {
            playerNameLabel.setText(username);
            loadMatchHistory("Fx+Zephrrr");
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
                            
                            tableModel.addRow(new Object[]{result, opponent, matchType, matchDisplay, change, time});
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
}