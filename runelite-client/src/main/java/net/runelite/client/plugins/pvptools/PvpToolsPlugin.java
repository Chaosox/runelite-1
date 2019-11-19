/*
 * ******************************************************************************
 *  * Copyright (c) 2019 openosrs
 *  *  Redistributions and modifications of this software are permitted as long as this notice remains in its original unmodified state at the top of this file.
 *  *  If there are any questions comments, or feedback about this software, please direct all inquiries directly to the file authors:
 *  *  ST0NEWALL#9112
 *  *   openosrs Discord: https://discord.gg/Q7wFtCe
 *  *   openosrs website: https://openosrs.com
 *  *****************************************************************************
 */

package net.runelite.client.plugins.pvptools;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.inject.Provides;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Getter;
import net.runelite.api.ClanMember;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.player.PlayerOverheadChanged;
import net.runelite.api.util.Text;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import static net.runelite.client.plugins.pvptools.HideAttackOptionsMode.CLAN;
import static net.runelite.client.plugins.pvptools.HideAttackOptionsMode.FRIENDS;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import org.codehaus.plexus.util.CollectionUtils;

@PluginDescriptor(
	name = "PvP Tools",
	description = "Enable the PvP Tools panel",
	tags = {"panel", "pvp", "pk", "pklite", "renderself"},
	type = PluginType.PVP,
	enabledByDefault = false
)
@Singleton
public class PvpToolsPlugin extends Plugin
{
	@Inject
	PlayerCountOverlay playerCountOverlay;
	/**
	 * Config field declarations
	 */
	@Getter
	private boolean countPlayers;
	@Getter
	private boolean countOverHeads;
	private boolean hideCast;
	private HideAttackOptionsMode hideCastMode;
	private List<String> unhiddenCasts;
	private Keybind renderSelf;
	private Set<HideAttackOptionsMode> hiddenAttackOptions = EnumSet.noneOf(HideAttackOptionsMode.class);
	/*
	 *  Injected dependency classes
	 * */
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private Client client;
	private final HotkeyListener renderselfHotkeyListener = new HotkeyListener(() -> this.renderSelf)
	{
		public void hotkeyPressed()
		{
			client.setRenderSelf(!client.getRenderSelf());
		}
	};
	@Inject
	private ClientThread clientThread;
	@Inject
	private EventBus eventBus;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private KeyManager keyManager;
	@Inject
	private CurrentPlayersJFrame currentPlayersJFrame;
	@Inject
	private MissingPlayersJFrame missingPlayersJFrame;
	final ActionListener currentPlayersActionListener = new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			final List<String> players = client.getPlayers().stream()
				.filter(Player::isClanMember)
				.map(Player::getName)
				.collect(Collectors.toList());
			if (currentPlayersJFrame != null)
			{
				currentPlayersJFrame.dispose();
				currentPlayersJFrame = null;

				currentPlayersJFrame = new CurrentPlayersJFrame(client, PvpToolsPlugin.this, players);
			}
			else
			{
				currentPlayersJFrame = new CurrentPlayersJFrame(client, PvpToolsPlugin.this, players);
			}
			final List<String> missingPlayers = Arrays.stream(client.getClanMembers()).map(ClanMember::getUsername).filter(s -> !new ArrayList<>(players).contains(s)).collect(Collectors.toList());

			if (missingPlayersJFrame != null)
			{
				missingPlayersJFrame.dispose();
				missingPlayersJFrame = null;
				missingPlayersJFrame = new MissingPlayersJFrame(client, PvpToolsPlugin.this, missingPlayers);
			}
			else
			{
				missingPlayersJFrame = new MissingPlayersJFrame(client, PvpToolsPlugin.this, missingPlayers);
			}
		}
	};
	@Inject
	private PvpToolsConfig config;
	/**
	 * Non-config class fields
	 */
	private PvpToolsPanel panel;
	private NavigationButton navButton;
	@Getter
	private int localClanMembers = 0;
	@SuppressWarnings("unchecked")
	@Getter
	private Map<HeadIcon, Long> prayerCounts = Collections.synchronizedMap(new EnumMap(HeadIcon.class));

	@Provides
	PvpToolsConfig config(ConfigManager configManager)
	{
		return configManager.getConfig(PvpToolsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		Arrays.stream(HeadIcon.values()).forEach((k) -> prayerCounts.put(k, Integer.toUnsignedLong(0)));
		prayerCounts.forEach((k, v) -> prayerCounts.put(k, Integer.toUnsignedLong(0)));
		updateConfig();
		addSubscriptions();
		overlayManager.add(playerCountOverlay);
		keyManager.registerKeyListener(renderselfHotkeyListener);
	}

	@Override
	protected void shutDown() throws Exception
	{
		hiddenAttackOptions.clear();
		updateAttackOptions();
		eventBus.unregister(this);
		overlayManager.remove(playerCountOverlay);
		keyManager.unregisterKeyListener(renderselfHotkeyListener);
		clientToolbar.removeNavigation(navButton);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			resetCastOptions();
		}
	}

	private void buildPanel()
	{
		SwingUtilities.invokeLater(() ->
		{


			final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "skull.png");

			panel = new PvpToolsPanel();

			navButton = NavigationButton.builder()
				.tooltip("PvP Tools")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.tab(true)
				.build();

			panel.init();

			clientToolbar.addNavigation(navButton);
		});
	}

	private void addSubscriptions()
	{
		eventBus.subscribe(ConfigChanged.class, this, this::onConfigChanged);
		eventBus.subscribe(GameStateChanged.class, this, this::onGameStateChanged);
		eventBus.subscribe(PlayerSpawned.class, this, this::onPlayerSpawned);
		eventBus.subscribe(PlayerDespawned.class, this, this::onPlayerDespawned);
		eventBus.subscribe(PlayerOverheadChanged.class, this, this::onPlayerOverheadChanged);
	}

	private void onPlayerOverheadChanged(PlayerOverheadChanged event)
	{
		if (!this.countOverHeads && !client.getGameState().equals(GameState.LOGGED_IN))
		{
			return;
		}
		if (event.getOldPrayer() == null || event.getNewPrayer() == null)
		{
			return;
		}
		prayerCounts = client.getPlayers().stream().collect(Collectors.groupingBy(Player::getOverheadIcon,
			Collectors.counting()));
	}

	private void onConfigChanged(ConfigChanged configChanged)
	{
		if (!"pvptools".equals(configChanged.getGroup()))
		{
			return;
		}
		updateConfig();
	}

	private void onGameStateChanged(GameStateChanged event)
	{
		localClanMembers = 0;
		prayerCounts.forEach((k, v) -> prayerCounts.put(k, Integer.toUnsignedLong(0)));
		if (event.getGameState().equals(GameState.LOGGED_IN))
		{
			buildPanel();
		}
	}

	private void onPlayerSpawned(PlayerSpawned event)
	{
		if (!this.countPlayers)
		{
			return;
		}
		final Player p = event.getPlayer();
		if (p.isClanMember() && !p.equals(client.getLocalPlayer()))
		{
			localClanMembers++;
		}
	}

	private void onPlayerDespawned(PlayerDespawned event)
	{
		if (!this.countPlayers)
		{
			return;
		}
		final Player p = event.getPlayer();
		if (p.isClanMember() && !p.equals(client.getLocalPlayer()))
		{
			localClanMembers--;
		}
	}

	private void updateAttackOptions()
	{

		clientThread.invoke(() ->
		{
			if (this.hiddenAttackOptions.contains(CLAN))
			{
				client.setHideClanmateAttackOptions(true);
			}
			else
			{
				client.setHideClanmateAttackOptions(false);
			}
			if (this.hiddenAttackOptions.contains(FRIENDS))
			{
				client.setHideFriendAttackOptions(true);
			}
			else
			{
				client.setHideFriendAttackOptions(false);
			}
		});
	}

	/**
	 * @param mode The {@link HideAttackOptionsMode} specifying clanmates, friends, or both.
	 */
	private void hideAttackOptions(HideAttackOptionsMode mode)
	{
		switch (mode)
		{
			case CLAN:
				client.setHideClanmateAttackOptions(true);
				client.setHideFriendAttackOptions(false);
				break;
			case FRIENDS:
				client.setHideFriendAttackOptions(true);
				client.setHideClanmateAttackOptions(false);
				break;
			case BOTH:
				client.setHideClanmateAttackOptions(true);
				client.setHideFriendAttackOptions(true);
				break;
		}
	}

	/**
	 * Given an AttackMode, hides the appropriate cast options.
	 *
	 * @param mode The {@link HideAttackOptionsMode} specifying clanmates, friends, or both.
	 */
	@Deprecated
	private void hideCastOptions(HideAttackOptionsMode mode)
	{
		switch (mode)
		{
			case CLAN:
				client.setHideClanmateCastOptions(true);
				client.setHideFriendCastOptions(false);
				break;
			case FRIENDS:
				client.setHideFriendCastOptions(true);
				client.setHideClanmateCastOptions(false);
				break;
			case BOTH:
				client.setHideClanmateCastOptions(true);
				client.setHideFriendCastOptions(true);
				break;
		}
	}

	@Deprecated
	public void setCastOptions()
	{
		clientThread.invoke(() ->
		{
			if ((client.getVar(Varbits.IN_RAID) == 1 || client.getVar(Varbits.THEATRE_OF_BLOOD) == 2)
				|| (client.getVar(Varbits.IN_WILDERNESS) != 1 && !WorldType.isAllPvpWorld(client.getWorldType())))
			{
				return;
			}
			else
			{
				client.setHideFriendAttackOptions(false);
				client.setHideClanmateAttackOptions(false);
			}

			if (this.hideCast)
			{
				hideCastOptions(this.hideCastMode);
			}
			else
			{
				client.setHideFriendCastOptions(false);
				client.setHideClanmateCastOptions(false);
			}

			client.setUnhiddenCasts(this.unhiddenCasts);

		});
	}

	@Deprecated
	private void resetCastOptions()
	{
		clientThread.invoke(() ->
		{
			if (client.getVar(Varbits.IN_RAID) == 1 || client.getVar(Varbits.THEATRE_OF_BLOOD) == 2)
			{
				return;
			}

			client.setHideFriendAttackOptions(false);
			client.setHideFriendCastOptions(false);
		});
	}

	private void updateConfig()
	{
		hiddenAttackOptions.clear();
		this.countPlayers = config.countPlayers();
		this.countOverHeads = config.countOverHeads();
		this.renderSelf = config.renderSelf();
		this.hiddenAttackOptions = config.hideAttackOptionsMode();
		this.hideCast = config.hideCast();
		this.hideCastMode = config.hideCastMode();
		this.unhiddenCasts = (List<String>) Splitter.on(",").omitEmptyStrings().trimResults().splitToList(config.hideCastIgnored().toLowerCase());
		updateAttackOptions();
	}

	public int getEnemyPlayerCount()
	{
		if (client == null || client.getPlayers() == null)
		{
			return 0;
		}
		return ((client.getPlayers() != null) ? client.getPlayers().size() : 0) - this.localClanMembers;
	}
}