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
        highestRank.add(new JLabel("Rune 3"));
        highestRank.add(new JLabel("23/4/2025, 6:59:27 AM"));
        
        JPanel lowestRank = new JPanel();
        lowestRank.setLayout(new BoxLayout(lowestRank, BoxLayout.Y_AXIS));
        lowestRank.add(new JLabel("Lowest Rank Lost To"));
        lowestRank.add(new JLabel("-"));
        lowestRank.add(new JLabel("-"));
        
        statsPanel.add(highestRank);
        statsPanel.add(lowestRank);
        
        additionalStatsPanel.add(statsPanel);
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
        
        // Open Cognito login in browser with PKCE
        try
        {
            // Use the web app's login URL directly (it handles PKCE internally)
            String cognitoUrl = "https://devsecopsautomated.com/profile.html";
            
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(cognitoUrl));
            
            // Start polling for callback completion
            startTokenPolling();
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
    
    private void completeLogin()
    {
        try
        {
            // Read tokens from callback file/endpoint
            String callbackUrl = "https://devsecopsautomated.com/auth/callback.html";
            URL url = new URL(callbackUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() == 200)
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    response.append(line);
                }
                reader.close();
                
                // Parse tokens from response (assuming JSON format)
                JsonObject tokenResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                if (tokenResponse.has("id_token"))
                {
                    idToken = tokenResponse.get("id_token").getAsString();
                }
                if (tokenResponse.has("access_token"))
                {
                    accessToken = tokenResponse.get("access_token").getAsString();
                }
            }
        }
        catch (Exception e)
        {
            // Fallback to demo tokens if callback fails
            idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.demo_id_token";
            accessToken = "demo_access_token_12345";
        }
        
        String username = usernameField.getText();
        if (username.isEmpty()) username = "Fx%20Zephrrr";
        
        playerNameLabel.setText(username);
        showAdditionalStats(true);
        loadMatchHistory(username);
        loginButton.setText("Logout");
        usernameField.setEnabled(false);
    }
    
    private void clearTokens()
    {
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