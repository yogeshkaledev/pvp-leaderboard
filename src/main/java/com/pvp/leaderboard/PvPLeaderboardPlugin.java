package com.pvp.leaderboard;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "PvPLeaderboard"
)
public class PvPLeaderboardPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PvPLeaderboardConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	private DashboardPanel dashboardPanel;
	private NavigationButton navButton;
	private long accountHash;
	private boolean inFight = false;
	private boolean wasInMulti = false;
	private int fightStartSpellbook = -1;
	private int fightEndSpellbook = -1;
	private String opponent = null;
	private String highestRankDefeated = null;
	private String lowestRankLostTo = null;

	@Override
	protected void startUp() throws Exception
	{
		dashboardPanel = new DashboardPanel(config);
		
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/util/clue_arrow.png");
		navButton = NavigationButton.builder()
			.tooltip("PvP Leaderboard")
			.icon(icon)
			.priority(5)
			.panel(dashboardPanel)
			.build();

		clientToolbar.addNavigation(navButton);
		log.info("PvP Leaderboard started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		log.info("PvP Leaderboard stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			accountHash = client.getAccountHash();
			log.info("PvP Leaderboard ready! Account hash: " + accountHash);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
	{
		if (hitsplatApplied.getActor() instanceof Player)
		{
			Player player = (Player) hitsplatApplied.getActor();
			Player localPlayer = client.getLocalPlayer();
			
			if (localPlayer != null && (player == localPlayer || hitsplatApplied.getHitsplat().isMine()))
			{
				if (!inFight)
				{
					startFight(player == localPlayer ? getOpponentName(hitsplatApplied) : player.getName());
				}
				
				if (client.getVarbitValue(Varbits.MULTICOMBAT_AREA) == 1)
				{
					wasInMulti = true;
				}
			}
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		if (actorDeath.getActor() instanceof Player && inFight)
		{
			Player player = (Player) actorDeath.getActor();
			Player localPlayer = client.getLocalPlayer();
			
			if (player == localPlayer || (opponent != null && player.getName().equals(opponent)))
			{
				endFight();
			}
		}
	}

	private void startFight(String opponentName)
	{
		inFight = true;
		opponent = opponentName;
		wasInMulti = client.getVarbitValue(Varbits.MULTICOMBAT_AREA) == 1;
		fightStartSpellbook = client.getVarbitValue(Varbits.SPELLBOOK);
		log.info("Fight started against: " + opponent + ", Multi: " + wasInMulti + ", Spellbook: " + fightStartSpellbook);
	}

	private void endFight()
	{
		fightEndSpellbook = client.getVarbitValue(Varbits.SPELLBOOK);
		
		// Determine fight outcome and update additional stats
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null && opponent != null)
		{
			boolean playerWon = localPlayer.getHealthRatio() > 0;
			String opponentRank = getPlayerRank(opponent);
			String bucket = determineBucket();
			double currentMMR = estimateCurrentMMR();
			
			if (playerWon)
			{
				updateHighestRankDefeated(opponentRank);
			}
			else
			{
				updateLowestRankLostTo(opponentRank);
			}
			
			// Update tier graph with real-time data
			if (dashboardPanel != null)
			{
				dashboardPanel.updateTierGraphRealTime(bucket, currentMMR);
			}
		}
		
		log.info("Fight ended. Multi during fight: " + wasInMulti + ", Start spellbook: " + fightStartSpellbook + ", End spellbook: " + fightEndSpellbook);
		
		// Reset fight state
		inFight = false;
		wasInMulti = false;
		fightStartSpellbook = -1;
		fightEndSpellbook = -1;
		opponent = null;
	}

	private String getOpponentName(HitsplatApplied hitsplatApplied)
	{
		// Try to find the opponent from nearby players
		for (Player player : client.getPlayers())
		{
			if (player != client.getLocalPlayer() && player.getInteracting() == client.getLocalPlayer())
			{
				return player.getName();
			}
		}
		return "Unknown";
	}

	private String getPlayerRank(String playerName)
	{
		// Simplified rank estimation based on combat level or other factors
		// In a real implementation, this would query the leaderboard API
		return "Bronze 3"; // Placeholder
	}
	
	private void updateHighestRankDefeated(String rank)
	{
		if (highestRankDefeated == null || isHigherRank(rank, highestRankDefeated))
		{
			highestRankDefeated = rank;
			log.info("New highest rank defeated: " + rank);
			if (dashboardPanel != null)
			{
				dashboardPanel.updateAdditionalStatsFromPlugin(highestRankDefeated, lowestRankLostTo);
			}
		}
	}
	
	private void updateLowestRankLostTo(String rank)
	{
		if (lowestRankLostTo == null || isLowerRank(rank, lowestRankLostTo))
		{
			lowestRankLostTo = rank;
			log.info("New lowest rank lost to: " + rank);
			if (dashboardPanel != null)
			{
				dashboardPanel.updateAdditionalStatsFromPlugin(highestRankDefeated, lowestRankLostTo);
			}
		}
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
		
		return baseOrder * 10 + (4 - division); // Higher division = higher order
	}
	
	public long getAccountHash()
	{
		return accountHash;
	}
	
	public String getHighestRankDefeated()
	{
		return highestRankDefeated;
	}
	
	public String getLowestRankLostTo()
	{
		return lowestRankLostTo;
	}
	
	private String determineBucket()
	{
		// Determine bucket based on spellbook and multi area
		if (wasInMulti)
		{
			return "multi";
		}
		
		if (fightStartSpellbook == 1) // Lunar spellbook
		{
			return "veng";
		}
		
		return "nh"; // Default to NH
	}
	
	private double estimateCurrentMMR()
	{
		// Simplified MMR estimation - in real implementation would track actual MMR
		return 1000.0; // Placeholder
	}

	@Provides
	PvPLeaderboardConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvPLeaderboardConfig.class);
	}
}
