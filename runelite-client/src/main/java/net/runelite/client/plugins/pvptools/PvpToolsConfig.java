/*
 * Copyright (c) 2019. PKLite  - All Rights Reserved
 * Unauthorized modification, distribution, or possession of this source file, via any medium is strictly prohibited.
 * Proprietary and confidential. Refer to PKLite License file for more information on
 * full terms of this copyright and to determine what constitutes authorized use.
 * Written by PKLite(ST0NEWALL, others) <stonewall@thots.cc.usa>, 2019
 *
 */

package net.runelite.client.plugins.pvptools;

import java.util.EnumSet;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("pvptools")
public interface PvpToolsConfig extends Config
{
	@ConfigItem(
		keyName = "countPlayers",
		name = "Count Players",
		description = "When in PvP zones, counts the attackable players in and not in player's CC",
		position = 0
	)
	default boolean countPlayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countOverHeads",
		name = "Count Enemy Overheads",
		description = "Counts the number of each protection prayer attackable targets not in your CC are currently using",
		position = 1
	)
	default boolean countOverHeads()
	{
		return true;
	}

	@ConfigItem(
		keyName = "renderSelfHotkey",
		name = "Render Self Hotkey",
		description = "Toggles renderself when you press the hotkey",
		position = 2
	)
	default Keybind renderSelf()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		position = 3,
		keyName = "hideAttackOptionsMode",
		name = "Hide Attack Options for",
		description = "Hides the attack option for the following type(s) of players",
		enumClass = HideAttackOptionsMode.class
	)
	default EnumSet<HideAttackOptionsMode> hideAttackOptionsMode()
	{
		return EnumSet.of(HideAttackOptionsMode.FRIENDS);
	}

	@ConfigItem(
		keyName = "hideCast",
		name = "Hide cast",
		description = "Hides the cast option for clanmates, friends, or both",
		position = 4
	)
	default boolean hideCast()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideCastMode",
		name = "Mode",
		description = "",
		position = 5,
		hidden = true,
		unhide = "hideCast"
	)
	default HideAttackOptionsMode hideCastMode()
	{
		return HideAttackOptionsMode.FRIENDS;
	}

	@ConfigItem(
		keyName = "hideCastIgnored",
		name = "Ignored spells",
		description = "Spells that should not be hidden from being cast, separated by a comma",
		position = 6,
		hidden = true,
		unhide = "hideCast"
	)
	default String hideCastIgnored()
	{
		return "cure other, energy transfer, heal other, vengeance other";
	}

	@ConfigItem(
		keyName = "riskCalculator",
		name = "Risk Calculator",
		description = "Enables a panel in the PvP Tools Panel that shows the players current risk",
		position = 8
	)
	default boolean riskCalculatorEnabled()
	{
		return true;
	}
}