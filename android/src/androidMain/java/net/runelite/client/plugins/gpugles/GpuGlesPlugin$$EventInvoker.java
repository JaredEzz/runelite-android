package net.runelite.client.plugins.gpugles;

import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.WorldEntityDespawned;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;

public final class GpuGlesPlugin$$EventInvoker implements EventBus.ClassInvoker
{
	@Override
	public int resolve(String methodName, Class<?> eventType)
	{
		String en = eventType.getName();
		if ("onConfigChanged".equals(methodName) && ConfigChanged.class.getName().equals(en)) return 0;
		if ("onGameStateChanged".equals(methodName) && GameStateChanged.class.getName().equals(en)) return 1;
		if ("onWorldEntitySpawned".equals(methodName) && WorldEntitySpawned.class.getName().equals(en)) return 2;
		if ("onWorldEntityDespawned".equals(methodName) && WorldEntityDespawned.class.getName().equals(en)) return 3;
		if ("onWorldViewLoaded".equals(methodName) && WorldViewLoaded.class.getName().equals(en)) return 4;
		if ("onWorldViewUnloaded".equals(methodName) && WorldViewUnloaded.class.getName().equals(en)) return 5;
		if ("onPostClientTick".equals(methodName) && PostClientTick.class.getName().equals(en)) return 6;
		return -1;
	}

	@Override
	public void invoke(Object target, int slot, Object event)
	{
		GpuGlesPlugin p = (GpuGlesPlugin) target;
		switch (slot)
		{
			case 0: p.onConfigChanged((ConfigChanged) event); break;
			case 1: p.onGameStateChanged((GameStateChanged) event); break;
			case 2: p.onWorldEntitySpawned((WorldEntitySpawned) event); break;
			case 3: p.onWorldEntityDespawned((WorldEntityDespawned) event); break;
			case 4: p.onWorldViewLoaded((WorldViewLoaded) event); break;
			case 5: p.onWorldViewUnloaded((WorldViewUnloaded) event); break;
			case 6: p.onPostClientTick((PostClientTick) event); break;
		}
	}
}
