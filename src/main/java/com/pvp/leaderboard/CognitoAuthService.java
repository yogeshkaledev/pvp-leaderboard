package com.pvp.leaderboard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

public class CognitoAuthService {
    private static final String COGNITO_DOMAIN = "osrs-mmr-a8959e04.auth.us-east-1.amazoncognito.com";
    private static final String CLIENT_ID = "5ho4mj5d17v44s4vavnkmp2mmo";
    private static final String REDIRECT_URI = "http://127.0.0.1:49215/callback";
    private static final int CALLBACK_PORT = 49215;
    
    private final Preferences prefs = Preferences.userNodeForPackage(CognitoAuthService.class);
    private HttpServer callbackServer;
    private String accessToken;
    private String refreshToken;
    private long tokenExpiry;
    
    public CompletableFuture<Boolean> login() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        try {
            // Start callback server
            startCallbackServer(future);
            
            // Generate PKCE
            String verifier = generatePKCEVerifier();
            String challenge = generatePKCEChallenge(verifier);
//            String state = generateState();
            
            // Build login URL
            String loginUrl = String.format(
                "https://%s/login?client_id=%s&response_type=code&scope=openid+email+profile&redirect_uri=%s&code_challenge_method=S256&code_challenge=%s&state=test",
                COGNITO_DOMAIN, CLIENT_ID, URLEncoder.encode(REDIRECT_URI, "UTF-8"), challenge
            );
            
            // Store verifier for callback
            prefs.put("pkce_verifier", verifier);
            
            // Open browser
            Desktop.getDesktop().browse(URI.create(loginUrl));
            
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    private void startCallbackServer(CompletableFuture<Boolean> future) throws IOException {
        callbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", CALLBACK_PORT), 0);
        callbackServer.createContext("/callback", new CallbackHandler(future));
        callbackServer.start();
    }
    
    private class CallbackHandler implements HttpHandler {
        private final CompletableFuture<Boolean> future;
        
        CallbackHandler(CompletableFuture<Boolean> future) {
            this.future = future;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                String code = extractParam(query, "code");

                
                // Exchange code for tokens
                exchangeCodeForTokens(code);
                
                // Send success response
                String response = "Login successful! You can close this window.";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                
                future.complete(true);
                
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                callbackServer.stop(0);
            }
        }
    }
    
    private void exchangeCodeForTokens(String code) throws Exception {
        String verifier = prefs.get("pkce_verifier", "");
        
        String postData = String.format(
            "grant_type=authorization_code&client_id=%s&code=%s&redirect_uri=%s&code_verifier=%s",
            CLIENT_ID, code, URLEncoder.encode(REDIRECT_URI, "UTF-8"), verifier
        );
        
        URL url = new URL("https://" + COGNITO_DOMAIN + "/oauth2/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        
        conn.getOutputStream().write(postData.getBytes());
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = reader.lines().reduce("", String::concat);
        reader.close();
        
        JsonObject tokens = JsonParser.parseString(response).getAsJsonObject();
        accessToken = tokens.get("access_token").getAsString();
        tokenExpiry = System.currentTimeMillis() + (tokens.get("expires_in").getAsInt() * 1000L);
        
        // Clean up
        prefs.remove("pkce_verifier");
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    

    
    public boolean isLoggedIn() {
        return accessToken != null && System.currentTimeMillis() < tokenExpiry;
    }
    
    public void logout() {
        accessToken = null;
        tokenExpiry = 0;
    }
    
    public String getStoredIdToken() {
        // Return access token as ID token for testing
        // In real implementation, would store and return actual ID token
        return accessToken;
    }
    
    private String generatePKCEVerifier() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
        SecureRandom random = new SecureRandom();
        StringBuilder verifier = new StringBuilder(64);
        for (int i = 0; i < 64; i++) {
            verifier.append(chars.charAt(random.nextInt(chars.length())));
        }
        return verifier.toString();
    }
    
    private String generatePKCEChallenge(String verifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).replace('+', '-').replace('/', '_');
    }
    

    
    private String extractParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && param.equals(kv[0])) {
                try {
                    return URLDecoder.decode(kv[1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return kv[1];
                }
            }
        }
        return null;
    }
}
