package net.runelite.client.plugins.mobile;

import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;

public final class MobilePlugin$$EventInvoker implements EventBus.ClassInvoker
{
	@Override
	public int resolve(String methodName, Class<?> eventType)
	{
		String en = eventType.getName();
		if ("onPostClientTick".equals(methodName) && PostClientTick.class.getName().equals(en)) return 0;
		if ("onFocusChanged".equals(methodName) && FocusChanged.class.getName().equals(en)) return 1;
		if ("onConfigChanged".equals(methodName) && ConfigChanged.class.getName().equals(en)) return 2;
		if ("onBeforeRender".equals(methodName) && BeforeRender.class.getName().equals(en)) return 3;
		if ("onScriptPostFired".equals(methodName) && ScriptPostFired.class.getName().equals(en)) return 4;
		if ("onGameStateChanged".equals(methodName) && GameStateChanged.class.getName().equals(en)) return 5;
		return -1;
	}

	@Override
	public void invoke(Object target, int slot, Object event)
	{
		MobilePlugin p = (MobilePlugin) target;
		switch (slot)
		{
			case 0: p.onPostClientTick((PostClientTick) event); break;
			case 1: p.onFocusChanged((FocusChanged) event); break;
			case 2: p.onConfigChanged((ConfigChanged) event); break;
			case 3: p.onBeforeRender((BeforeRender) event); break;
			case 4: p.onScriptPostFired((ScriptPostFired) event); break;
			case 5: p.onGameStateChanged((GameStateChanged) event); break;
		}
	}
}
