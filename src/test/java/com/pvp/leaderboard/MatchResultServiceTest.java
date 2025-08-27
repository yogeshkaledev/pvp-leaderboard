package com.pvp.leaderboard;

import org.junit.Test;
import java.util.concurrent.CompletableFuture;

public class MatchResultServiceTest
{
    @Test
    public void testUnauthenticatedMatchSubmission() throws Exception
    {
        MatchResultService service = new MatchResultService();
        
        CompletableFuture<Boolean> result = service.submitMatchResult(
            "TestPlayer",
            "TestOpponent", 
            "win",
            317,
            System.currentTimeMillis() / 1000 - 60, // 1 minute ago
            System.currentTimeMillis() / 1000,      // now
            "Ancient",
            "Ancient",
            false,
            123456789L,
            null // no token = unauthenticated
        );
        
        Boolean success = result.get();
        System.out.println("Unauthenticated submission result: " + success);
    }
    
    @Test
    public void testAuthenticatedMatchSubmission() throws Exception
    {
        MatchResultService service = new MatchResultService();
        CognitoAuthService authService = new CognitoAuthService();
        
        // Check for existing token first
        String idToken = authService.getStoredIdToken();
        
        if (idToken == null || idToken.isEmpty())
        {
            System.out.println("No stored token found. Starting login flow...");
            
            // Start login process
            CompletableFuture<Boolean> loginResult = authService.login();
            
            System.out.println("Please complete login in browser. Waiting up to 60 seconds...");
            
            // Wait for login completion with timeout
            Boolean loginSuccess = loginResult.get(60, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!loginSuccess)
            {
                System.out.println("Login failed or timed out.");
                return;
            }
            
            System.out.println("Login completed successfully!");
            
            // Get token after successful login
            idToken = authService.getStoredIdToken();
            
            if (idToken == null || idToken.isEmpty())
            {
                System.out.println("Failed to retrieve token after login.");
                return;
            }
        }
        
        System.out.println("Using token for authenticated submission...");
        
        CompletableFuture<Boolean> result = service.submitMatchResult(
            "TestPlayer",
            "TestOpponent",
            "loss", 
            317,
            System.currentTimeMillis() / 1000 - 120, // 2 minutes ago
            System.currentTimeMillis() / 1000,       // now
            "Lunar",
            "Standard",
            true, // was in multi
            987654321L,
            idToken // authenticated with token
        );
        
        Boolean success = result.get();
        System.out.println("Authenticated submission result: " + success);
    }
}