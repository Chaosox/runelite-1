package net.runelite.api.events.player;

import lombok.Data;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.api.events.Event;

@Data
public class PlayerOverheadChanged implements Event
{

	private Player player;

	private HeadIcon newPrayer;

	private HeadIcon oldPrayer;
}
