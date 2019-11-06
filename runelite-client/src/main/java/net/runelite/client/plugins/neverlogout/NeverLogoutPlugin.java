package net.runelite.client.plugins.neverlogout;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.mapping.Import;

@PluginDescriptor(
	name = "NeverLogoutPlugin",
	description = "",
	tags = {}
)
@Slf4j
public class NeverLogoutPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private EventBus eventBus;

	@Override
	protected void startUp() throws Exception
	{
		eventBus.subscribe(ClientTick.class, this, this::onClientTick);
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(this);
	}

	private void onClientTick(ClientTick event)
	{
		log.debug("MouseIdle: {}, keyboardIdle: {}, logoutTimer: {}", client.getMouseIdleTicks(),
			client.getKeyboardIdleTicks(), client.getLogoutTime());
		client.setMouseIdleTicks(0);
		client.setKeyIdleTicks(0);
	}
}
