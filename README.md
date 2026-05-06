# Barbarian Fishing Timers

A [RuneLite](https://runelite.net) plugin that displays how long each barbarian fishing spot has been at its current position, measured in game ticks.

## Features

- **Tick timer** displayed above each active barbarian fishing spot
- **Color-coded thresholds** — green while fresh, yellow as it ages, red when it's been there a while (all colors and thresholds are configurable)
- **Lowest timer highlight** — the spot with the lowest known timer is outlined in green, making it easy to identify the one least likely to move soon (toggleable)
- **Persists through teleports** — if you teleport away and return within 2.5 minutes, timers are restored
- **Off-screen move tracking** — if a spot moves out of render distance and you later reveal it, the timer reflects how long ago it departed its previous position

## How timers are assigned

| Scenario | Display |
|---|---|
| You watch a spot move on-screen | Exact timer starting from 0 |
| Spot departed on-screen, you reveal it at its new off-screen position | Timer based on when it left (bounded by 2.5 min cache) |
| Spot appeared from off-screen with no prior record | No timer shown |
| You teleport away and return — spot unchanged | Timer restored |

## Configuration

| Option | Default | Description |
|---|---|---|
| Highlight lowest timer | On | Outline the spot with the lowest known timer in green |
| Low threshold | 200 ticks | Timer below this is shown in the low color |
| Low color | Green | Color for timers below the low threshold |
| Mid threshold | 300 ticks | Timer below this (and above low) is shown in the mid color |
| Mid color | Yellow | Color for timers between the two thresholds |
| High color | Red | Color for timers above the mid threshold |

## Notes

- Only works for **barbarian fishing** (Otto's Grotto / Barbarian Outpost rod spots)
- Timers are reset on world hop or logout
- Cache entries older than **2.5 minutes (250 ticks)** are discarded automatically
