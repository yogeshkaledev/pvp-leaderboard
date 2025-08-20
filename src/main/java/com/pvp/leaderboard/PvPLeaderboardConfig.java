package com.pvp.leaderboard;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("PvPLeaderboard")
public interface PvPLeaderboardConfig extends Config
{
	@ConfigItem(
		keyName = "showLeaderboard",
		name = "Show Leaderboard",
		description = "Toggle leaderboard visibility"
	)
	default boolean showLeaderboard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fontSize",
		name = "Font Size",
		description = "Font size for leaderboard text"
	)
	@Range(min = 8, max = 24)
	default int fontSize()
	{
		return 12;
	}
}
