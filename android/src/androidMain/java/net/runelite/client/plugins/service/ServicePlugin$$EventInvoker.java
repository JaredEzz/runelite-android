package net.runelite.client.plugins.service;

import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.EventBus;

public final class ServicePlugin$$EventInvoker implements EventBus.ClassInvoker
{
	@Override
	public int resolve(String methodName, Class<?> eventType)
	{
		if ("onGameStateChanged".equals(methodName) && GameStateChanged.class.getName().equals(eventType.getName()))
		{
			return 0;
		}
		return -1;
	}

	@Override
	public void invoke(Object target, int slot, Object event)
	{
		if (slot == 0)
		{
			((ServicePlugin) target).onGameStateChanged((GameStateChanged) event);
		}
	}
}
