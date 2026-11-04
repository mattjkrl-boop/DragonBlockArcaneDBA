# No-Shapes Realism Pass: Wish Ritual report

Owner surface: WishRitualVisuals only. Base: ad22b32. Nothing committed.
No shared file was edited by this owner. Dragons, dragon ball blocks,
WishScreen, payloads, sounds, and all ritual mechanics were left unchanged.

## Before and after geometry counts

Counts are for src/client/java/com/dragonblockarcanedba/client/wish/WishRitualVisuals.java.

| Measure | Before | After |
|---|---:|---:|
| BespokeGeometry builder calls | 12 | 0 |
| displacedDome calls | 2 | 0 |
| shardFan calls | 1 | 0 |
| plume calls | 2 | 0 |
| brokenVortexBand calls | 5 | 0 |
| branch calls | 2 | 0 |
| BespokeGeometry references, import included | 13 | 0 |
| LayeredRender geometry calls | 0 | 0 |
| KiRenderHelper references | 2 | 0 |
| Direct addVertex calls | 0 | 0 |
| KiRenderHelper.vertex calls | 0 | 0 |
| submitCustomGeometry calls | 8 | 0 |

The nine geometry-specific private draw or submit methods are also gone:
drawBall, submitDomePair, submitDomePairAt, drawColumn, drawHelixRibbon,
drawStandingRing, submitRingBand, ringBandPair, and submitMote. The methods
that remain in the file either advance the unchanged ritual timeline or
supply emission points as data. They draw nothing.

## Conversion by element

The existing seed derivation stayed byte-identical in behavior: seedOf still
mixes only the synced payload game time, the Namek flag, and the center
computed from the synced ball positions. The long ritual seed is folded to
the toolkit int seed by xor of its two halves, then given a fixed offset for
each emitter. No client random, wall clock time, or new synced field chooses
the material variation. The wall clock is used only to feed the Emitter
accumulator delta, as the toolkit design requires.

Fourteen retained MaterialEmitters.Emitter instances carry the whole
surface. Each one receives the frame delta, an envelope from the unchanged
ritual timeline, the effects density config inside the toolkit, a hard
per-second budget, and MaterialEmitters.distanceScale at the ritual center.

| Element | Material conversion |
|---|---|
| Dragon ball travel and formation positions | Plasma clusters in the Earth orange or Namek green body color, plus soul clusters in the palette soul color. Both sample inside the current computed ball positions while the travel, arc, ring seating, spin, and fade timeline stays unchanged. |
| Formation ring | A circular emission front at the old ring radius and lift. Mist carries the soft body of the front and plasma carries the brighter energy, both with small radial and vertical jitter and a slight outward source velocity. |
| Summoning column | Rising plasma through the core, soul emission on the rising stream near the old mote path, and wider vapor through the column volume. Height growth, fade-in, dragon reveal fade, phase speed, and ground position are unchanged. |
| Ground ring | A circular emission front at the old expanding ground radius. Mist and plasma sample the front with jitter and outward drift instead of the two old band calls. |
| Standing gate rings | Circular emission fronts in the old vertical gate plane at both the full gate radius and the 0.72 inner radius. Mist and plasma share the same dynamic gate source. |
| Rising motes | Folded into the column soul stream. Their old orbit radius, rise character, palette color, and phase relationship are represented by the soul source instead of six separate plume blobs. |
| Arrival burst | An expanding plasma and mist emission front at the old burst radius curve, plus a soul volume inside the old burst core radius curve. The 18 tick window and fade curve are unchanged. |

Palette mapping stayed with the old ritual identity:

- Earth body plasma: 0xFF750F. Earth ring plasma: 0xFFCC40.
- Namek body plasma: 0x2EC751. Namek ring plasma: 0x66FF9E.
- Soul and mist use the lighter palette companions recorded in the file.
- The arrival core keeps the old warm white family, 0xFFF0C7 and 0xFFF5D1.

## Budgets

Emitter rates and hard budgets per second, before envelope, distance, and
density scaling:

- Ball plasma 135 rate, 190 budget. Ball soul 72 rate, 110 budget.
- Formation mist 58 rate, 90 budget. Formation plasma 88 rate, 130 budget.
- Column plasma 175 rate, 250 budget. Column soul 112 rate, 170 budget.
  Column vapor 72 rate, 110 budget.
- Ground mist 76 rate, 115 budget. Ground plasma 92 rate, 140 budget.
- Gate mist 66 rate, 100 budget. Gate plasma 96 rate, 145 budget.
- Burst front plasma 245 rate, 330 budget. Burst front mist 92 rate,
  130 budget. Burst core soul 145 rate, 190 budget.

## Verification

Targeted javac audit only, JDK 25, WishRitualVisuals as the compile root
with the full main and client sourcepath closure:

- cp262: zero errors naming WishRitualVisuals. WishRitualVisuals,
  WishRitualVisuals$Ritual, and WishRitualVisuals$Palette classes emitted.
  Residual log: 4 error lines, all in DbaItems and DbaMenus.
- cp263: zero errors naming WishRitualVisuals. WishRitualVisuals,
  WishRitualVisuals$Ritual, and WishRitualVisuals$Palette classes emitted.
  Residual log: 8 error lines, all in DbaItems, DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, and PatrolRobotEntity.
- Grep after conversion: BespokeGeometry builder calls 0, BespokeGeometry
  references 0, LayeredRender references 0, KiRenderHelper references 0,
  addVertex 0, and submitCustomGeometry 0 in WishRitualVisuals.
- git diff --check on WishRitualVisuals: clean.
- Em dash scan on the changed code and this report: 0.

No Gradle build was run and no Gradle pass is claimed. The in-game look
remains unverified from this environment.

## Honest worse check

Nothing was reverted, but the trades are real:

- WishRitualVisuals no longer draws a solid ball silhouette or the red
  star pips. The unchanged ball travel positions are now marked by dense
  plasma and soul clusters. That is the assigned material conversion, but
  the traveling balls will read less solid than the previous shell did,
  especially at low effects density or long distance.
- The formation, ground, and gate fronts are stochastic emission fronts,
  not continuous bands. At low density they can look broken or sparse
  where the old bands were always complete.
- The column has no continuous shaft silhouette. It should read as rising
  plasma, souls, and vapor, but its height may be less immediately legible
  than the old column until Carter sees it in game.
- The first rendered frame of a ritual emits nothing because the retained
  Emitter accumulator starts with a zero delta. This is a one frame delay
  at ritual start and was accepted rather than inventing a private burst
  path.

## Shared change requests

None. MaterialEmitters already provided the accumulator, budgets, distance
scaling, presets, and source model needed for this surface. No private
particle system, private texture style, or new geometry helper was added.
