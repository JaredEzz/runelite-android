package net.runelite.client.plugins.mobile;

import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;

public final class MobileInputPlugin$$EventInvoker implements EventBus.ClassInvoker
{
	@Override
	public int resolve(String methodName, Class<?> eventType)
	{
		String en = eventType.getName();
		if ("onGameStateChanged".equals(methodName) && GameStateChanged.class.getName().equals(en)) return 0;
		if ("onConfigChanged".equals(methodName) && ConfigChanged.class.getName().equals(en)) return 1;
		if ("onWidgetLoaded".equals(methodName) && WidgetLoaded.class.getName().equals(en)) return 2;
		if ("onWidgetClosed".equals(methodName) && WidgetClosed.class.getName().equals(en)) return 3;
		if ("onBeforeRender".equals(methodName) && BeforeRender.class.getName().equals(en)) return 4;
		return -1;
	}

	@Override
	public void invoke(Object target, int slot, Object event)
	{
		MobileInputPlugin p = (MobileInputPlugin) target;
		switch (slot)
		{
			case 0: p.onGameStateChanged((GameStateChanged) event); break;
			case 1: p.onConfigChanged((ConfigChanged) event); break;
			case 2: p.onWidgetLoaded((WidgetLoaded) event); break;
			case 3: p.onWidgetClosed((WidgetClosed) event); break;
			case 4: p.onBeforeRender((BeforeRender) event); break;
		}
	}
}
