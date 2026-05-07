/*
 * Copyright (c) 2024, staticvoid07
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 */
package com.barbfishingtimer;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Barbarian Fishing Timers",
	description = "Displays how many ticks each barbarian fishing spot has been at its current position",
	tags = {"fishing", "barbarian", "otto", "timer", "overlay", "tick"}
)
public class BarbFishingPlugin extends Plugin
{
	// 2.5 minutes — after this long unseen, cached data is too stale to be useful
	private static final int CACHE_TTL = 250;

	private static final Set<Integer> BARB_SPOT_IDS = Set.of(
		NpcID._0_39_54_BRUT_FISHING_SPOT,
		NpcID._0_19_55_BRUT_FISHING_SPOT
	);

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BarbFishingOverlay overlay;

	@Inject
	private BarbFishingConfig config;

	@Inject
	private Notifier notifier;

	// Active barbarian fishing NPCs → game tick when they arrived at their current WorldPoint
	@Getter
	private final Map<NPC, Integer> activeSpotMoveTick = new HashMap<>();

	// Tracks each active NPC's last-known position to detect on-screen movement
	private final Map<NPC, WorldPoint> activeSpotPosition = new HashMap<>();

	// WorldPoint → {moveTick, despawnTick}
	// Restores the timer when the player teleports away and back to the same tile.
	private final Map<WorldPoint, int[]> despawnCache = new HashMap<>();

	// NPC index → {moveTick (-1 if unknown), x, y, plane, despawnTick}
	// Records the last on-screen departure so the timer can be recovered if the spot
	// reappears at a different tile within the TTL window.
	private final Map<Integer, int[]> indexCache = new HashMap<>();

	// Spots with no prior record — timer is unknown, show nothing
	@Getter
	private final Set<NPC> unknownTimerSpots = new HashSet<>();

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		activeSpotMoveTick.clear();
		activeSpotPosition.clear();
		despawnCache.clear();
		indexCache.clear();
		unknownTimerSpots.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (!BARB_SPOT_IDS.contains(npc.getId()))
		{
			return;
		}

		WorldPoint pos = npc.getWorldLocation();
		int[] cachedByPos = despawnCache.remove(pos);
		int[] cachedByIndex = indexCache.remove(npc.getIndex());

		if (cachedByPos != null)
		{
			if (cachedByIndex != null)
			{
				WorldPoint prevPos = new WorldPoint(cachedByIndex[1], cachedByIndex[2], cachedByIndex[3]);
				if (!pos.equals(prevPos))
				{
					// cachedByPos was from a different spot that previously occupied this tile.
					// This spot actually moved here from prevPos.
					activeSpotMoveTick.put(npc, cachedByIndex[4]);
					if (client.getLocalPlayer() != null
						&& isOrthogonallyAdjacent(prevPos, client.getLocalPlayer().getWorldLocation()))
					{
						notifier.notify(config.spotMovedNotification(), "A fishing spot beside you moved.");
					}
				}
				else
				{
					// Same tile confirmed by both caches — genuine teleport restore.
					activeSpotMoveTick.put(npc, cachedByPos[0]);
				}
			}
			else
			{
				// No index cache entry — same tile, player teleported away and back.
				activeSpotMoveTick.put(npc, cachedByPos[0]);
			}
		}
		else if (cachedByIndex != null)
		{
			WorldPoint prevPos = new WorldPoint(cachedByIndex[1], cachedByIndex[2], cachedByIndex[3]);
			if (!pos.equals(prevPos))
			{
				// Spot departed from an on-screen tile and reappeared here.
				// Use the departure tick as the move tick — bounded by the TTL so it can't be stale.
				activeSpotMoveTick.put(npc, cachedByIndex[4]);
				if (client.getLocalPlayer() != null
					&& isOrthogonallyAdjacent(prevPos, client.getLocalPlayer().getWorldLocation()))
				{
					notifier.notify(config.spotMovedNotification(), "A fishing spot beside you moved.");
				}
			}
			else if (cachedByIndex[0] >= 0)
			{
				// Same tile, WorldPoint cache expired but index cache still has the moveTick.
				activeSpotMoveTick.put(npc, cachedByIndex[0]);
			}
			else
			{
				// Same tile, timer was unknown before — still unknown.
				activeSpotMoveTick.put(npc, client.getTickCount());
				unknownTimerSpots.add(npc);
			}
		}
		else
		{
			// No prior record — spot appeared from off-screen with no known history.
			activeSpotMoveTick.put(npc, client.getTickCount());
			unknownTimerSpots.add(npc);
		}
		activeSpotPosition.put(npc, pos);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		Integer moveTick = activeSpotMoveTick.remove(npc);
		WorldPoint pos = activeSpotPosition.remove(npc);
		boolean wasUnknown = unknownTimerSpots.remove(npc);
		if (moveTick == null || pos == null)
		{
			return;
		}

		int despawnTick = client.getTickCount();

		if (!wasUnknown)
		{
			despawnCache.put(pos, new int[]{moveTick, despawnTick});
		}

		indexCache.put(npc.getIndex(), new int[]{
			wasUnknown ? -1 : moveTick,
			pos.getX(), pos.getY(), pos.getPlane(),
			despawnTick
		});
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int currentTick = client.getTickCount();

		// Detect spots that moved to a new tile without a despawn cycle
		for (NPC npc : activeSpotMoveTick.keySet())
		{
			WorldPoint current = npc.getWorldLocation();
			WorldPoint last = activeSpotPosition.get(npc);
			if (last != null && !current.equals(last))
			{
				if (client.getLocalPlayer() != null
					&& isOrthogonallyAdjacent(last, client.getLocalPlayer().getWorldLocation()))
				{
					notifier.notify(config.spotMovedNotification(), "A fishing spot beside you moved.");
				}
				activeSpotMoveTick.put(npc, currentTick);
				activeSpotPosition.put(npc, current);
				unknownTimerSpots.remove(npc);
			}
		}

		// Evict entries older than the TTL from both caches
		Iterator<Map.Entry<WorldPoint, int[]>> it = despawnCache.entrySet().iterator();
		while (it.hasNext())
		{
			if (currentTick - it.next().getValue()[1] > CACHE_TTL)
			{
				it.remove();
			}
		}
		Iterator<Map.Entry<Integer, int[]>> it2 = indexCache.entrySet().iterator();
		while (it2.hasNext())
		{
			if (currentTick - it2.next().getValue()[4] > CACHE_TTL)
			{
				it2.remove();
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			activeSpotMoveTick.clear();
			activeSpotPosition.clear();
			despawnCache.clear();
			indexCache.clear();
			unknownTimerSpots.clear();
		}
	}

	private boolean isOrthogonallyAdjacent(WorldPoint a, WorldPoint b)
	{
		int dx = Math.abs(a.getX() - b.getX());
		int dy = Math.abs(a.getY() - b.getY());
		return (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
	}

	@Provides
	BarbFishingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BarbFishingConfig.class);
	}
}
