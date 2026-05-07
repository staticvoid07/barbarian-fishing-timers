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

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Notification;

@ConfigGroup("barbarian-fishing-timers")
public interface BarbFishingConfig extends Config
{
	@ConfigItem(
		keyName = "highlightLowestTimer",
		name = "Highlight lowest timer",
		description = "Highlight the fishing spot with the lowest known tick timer",
		position = 0
	)
	default boolean highlightLowestTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lowThreshold",
		name = "Low threshold",
		description = "Timer below this tick count is shown in the low color",
		position = 1
	)
	default int lowThreshold()
	{
		return 200;
	}

	@Alpha
	@ConfigItem(
		keyName = "lowColor",
		name = "Low color",
		description = "Color used when the timer is below the low threshold",
		position = 2
	)
	default Color lowColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "midThreshold",
		name = "Mid threshold",
		description = "Timer below this tick count (and above low) is shown in the mid color",
		position = 3
	)
	default int midThreshold()
	{
		return 300;
	}

	@Alpha
	@ConfigItem(
		keyName = "midColor",
		name = "Mid color",
		description = "Color used when the timer is between the low and mid thresholds",
		position = 4
	)
	default Color midColor()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(
		keyName = "highColor",
		name = "High color",
		description = "Color used when the timer is above the mid threshold",
		position = 5
	)
	default Color highColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "spotMovedNotification",
		name = "Spot moved notification",
		description = "Notify when a fishing spot adjacent to you moves",
		position = 6
	)
	default Notification spotMovedNotification()
	{
		return Notification.OFF;
	}
}
