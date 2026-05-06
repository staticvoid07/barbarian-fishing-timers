package com.barbfishingtimer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class BarbFishingOverlay extends Overlay
{
	private static final Color HIGHLIGHT_COLOR = new Color(0, 255, 0, 180);
	private static final Color HIGHLIGHT_FILL_COLOR = new Color(0, 255, 0, 40);

	private final Client client;
	private final BarbFishingPlugin plugin;
	private final BarbFishingConfig config;

	@Inject
	BarbFishingOverlay(Client client, BarbFishingPlugin plugin, BarbFishingConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Map<NPC, Integer> spots = plugin.getActiveSpotMoveTick();
		Set<NPC> unknownSpots = plugin.getUnknownTimerSpots();

		if (spots.isEmpty())
		{
			return null;
		}

		int currentTick = client.getTickCount();

		// Find the known-timer spot with the lowest tick count for highlighting
		NPC lowestSpot = null;
		if (config.highlightLowestTimer())
		{
			int lowestTicks = Integer.MAX_VALUE;
			for (Map.Entry<NPC, Integer> entry : spots.entrySet())
			{
				if (unknownSpots.contains(entry.getKey()))
				{
					continue;
				}
				int ticks = currentTick - entry.getValue();
				if (ticks < lowestTicks)
				{
					lowestTicks = ticks;
					lowestSpot = entry.getKey();
				}
			}
		}

		for (Map.Entry<NPC, Integer> entry : spots.entrySet())
		{
			NPC npc = entry.getKey();

			if (npc == lowestSpot)
			{
				Polygon poly = npc.getCanvasTilePoly();
				if (poly != null)
				{
					OverlayUtil.renderPolygon(graphics, poly, HIGHLIGHT_COLOR, HIGHLIGHT_FILL_COLOR, new java.awt.BasicStroke(2));
				}
			}

			if (unknownSpots.contains(npc))
			{
				continue;
			}

			int ticks = currentTick - entry.getValue();
			String text = String.valueOf(ticks);
			Point textLocation = npc.getCanvasTextLocation(graphics, text, npc.getLogicalHeight() + 40);
			if (textLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLocation, text, colorForTicks(ticks));
			}
		}

		return null;
	}

	private Color colorForTicks(int ticks)
	{
		if (ticks < config.lowThreshold())
		{
			return config.lowColor();
		}
		if (ticks < config.midThreshold())
		{
			return config.midColor();
		}
		return config.highColor();
	}
}
