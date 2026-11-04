# No-Shapes Realism Pass Report: Azure Dragon Sword

Owner: Azure Dragon Sword (Phase 2). Base: dba ad22b32 (Phase 1 foundation).
Status: converted, verified by targeted javac on both classpaths, NOT committed.

Replacement agent note: the previous Azure agent died to an infrastructure
rate limit after only reading files. This agent started by checking git
status and the diff: the tree was clean at ad22b32 and there were no partial
Azure changes to audit. The conversion below was done fresh.

Files edited (6, exactly the audit's Azure rows): renderers
AzureLightningRenderer, AzureRushTrailRenderer, AzureSonicQuakeRenderer,
AzureStormRenderer, AzureTempestChannelRenderer, AzureTornadoRenderer.
No item, entity, or shared file was edited. Every damage number, cooldown,
cost, timing, mechanic, and synced data field is untouched; only the
renderer presentation changed.

## Before and after geometry counts (audit rows, Azure files)

| Effect | BespokeGeometry calls | LayeredRender geometry calls | Direct addVertex |
|---|---|---|---|
| AzureLightning | 4 to 0 | 2 to 0 | 0 to 0 |
| AzureRushTrail | 4 to 0 | 0 to 0 | 0 to 0 |
| AzureSonicQuake | 6 to 0 | 0 to 0 | 0 to 0 |
| AzureStorm | 7 to 0 | 1 to 0 | 0 to 0 |
| AzureTempestChannel | 5 to 0 | 1 to 0 | 0 to 0 |
| AzureTornado | 5 to 0 | 3 to 0 | 0 to 0 |
| **Owner total** | **31 to 0** | **7 to 0** | **0 to 0** |

Grep proof after conversion, across all six files: BespokeGeometry 0,
LayeredRender 0 (including distScale references; distance scaling now
comes from MaterialEmitters.distanceScale), KiRenderHelper 0,
submitCustomGeometry 0, addVertex 0. No renderer submits any geometry;
every submit ends at the vanilla super call after emission.

## Method

Each renderer follows the pattern the OxShockwave conversion established:
a per-renderer emission map keyed by the entity's synced render seed, a
mutable Frame holding the current center, radii, and paths as data, and
MaterialEmitters.Emitter instances ticked with a delta derived from the
render state's age (frame-rate independent), the effect's existing
lifecycle envelope, and MaterialEmitters.distanceScale. Emitters are
rebuilt when the client level changes or an entity's age resets. Rates
are hard-budgeted per second inside the toolkit and density-scaled by
the effects config. Old per-piece alpha values became emission density
envelopes; particle alpha itself is profile-driven by the material.

## Materials per effect

- **AzureLightning (telegraph and afterglow):** lightning plasma motes
  gathered across a disc source sampled at exactly the strike radius,
  a near-white plasma stream on a converging disc whose radius shrinks
  on the old converging curve, plasma rising along the charge path
  (the path is a MaterialEmitters.path source, data only), and low
  vapor over the disc. Azure for a normal strike, pale gold for a rare
  one, as before. Blink pulse, telegraph timing, and the 8 tick
  afterglow fade are the old numbers. The bolt itself stays BoltEntity.
- **AzureStorm:** mist volumes filling the zone (large MIST sprites in
  a cylinder source), vapor and suspended dust driven around the center
  by tangential sample velocities, a bright eye updraft of vapor and
  plasma, and lightning plasma emitted along seeded jagged arc paths
  regenerated on the old 3 tick reseed cadence (paths are data; forks
  included). The dome, ground bands, and eye column surfaces are gone.
  Rain inside the zone remains the AtmosphereClient system's job, as in
  the bespoke pass; this renderer does not double it.
- **AzureTornado:** vapor and mist sampled throughout the funnel volume
  with tangential plus rising velocities, wind-driven RAIN sprayed
  through the same volume, ground dust pulled around the base disc,
  physical BLOCK_CHUNKS debris of the actual ground block caught in the
  lower funnel, and tempest plasma threading the flow. Emitter rates and
  sprite scales step down for the mini tornado. The funnel surface, base
  bands, crown band, anchor glow, and orbiting blade crescents are gone;
  the physics zone numbers (radius and height by scale and giant flag)
  still drive the emission volume exactly.
- **AzureTempestChannel:** vapor and plasma rising through the channel
  volume with tangential swirl velocities (the old orbiting wind blades
  are now these swirl streams), mist drifting at the boundary, ground
  dust around the base, and lightning plasma along seeded jagged arc
  paths from a quarter charge up, reseeded on the old half tick cadence.
  Charge scaling of radius and height, the 5 tick intro, and the release
  fade over RELEASE_TICKS are unchanged. The channel render state gained
  the entity's world position fields; no entity code changed.
- **AzureSonicQuake:** ground dust, shock vapor, tempest plasma, sparks
  (CRIT_SPARK), and physical BLOCK_CHUNKS debris of the actual ground
  block, all emitted at the expanding crest (sampled as data from the
  same radius growth curve), plus dust thrown from the shrinking
  epicenter disc. The crest bands, compression ring, pressure needles,
  and crack star are gone. The 18 tick life, expansion ease, and alpha
  envelope drive the emission exactly as before.
- **AzureRushTrail:** the wake sampling, writer election, centerline,
  and radius profile math are byte-identical to the bespoke build. The
  mesh is replaced by sprayed material along the centerline: a bright
  plasma core stream, vapor shed through the old shell volume, RAIN
  sprayed off the moving wake, and a white plasma stream inside the old
  shock lip station window (t 0.28 to 0.48) at 1.22 times the shell
  radius. Azure shell color, green on a double rush, first-person
  density reduction, and the 12 tick life fade are the old numbers.

## Verification (javac audit method, no Gradle claim)

- Targeted javac of the six Azure renderers with the project sourcepath
  on the 26.2 classpath: zero errors naming any Azure file; 25 Azure
  class files emitted. The only closure errors are the known baseline
  kinds in DbaItems and DbaMenus.
- Same run on the 26.3 classpath: zero errors naming any Azure file; 25
  Azure class files emitted. Closure errors are the known baseline kinds
  in DbaItems, DbaMenus, DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, and PatrolRobotEntity, all present at baseline
  in files this conversion did not touch.
- One real error was caught and fixed during verification: five double
  literals passed to a float parameter in the quake crest sources
  (lossy conversion errors on both classpaths). Fixed with float
  literals and re-verified clean on both.
- Em dash scan of the added lines in the six files and in this report:
  zero.

## Honest worse check

Nothing was reverted, but four trades are stated plainly:

1. Telegraph precision. The lightning warning is now plasma density
   across the disc instead of a hard ring at the exact strike radius.
   The disc source still samples exactly that radius and the converging
   stream still closes on the center, but the boundary reads as a
   density edge, not a ruled line. That is the no-shapes standard's
   direct trade, and it is the conversion Carter is most likely to
   judge in game.
2. Storm arcs. The in-storm discharges are now dense plasma particles
   along jagged paths; they flicker and drift instead of reading as one
   continuous bolt. Continuous bolts remain BoltEntity's strikes. If
   the storm interior reads emptier than the bespoke arcs, raising the
   arc plasma rates is a one-line change per emitter.
3. Channel blades. The orbiting wind blades no longer exist as visible
   objects; their orbit is carried by swirl streams. If Carter wants
   literal orbiting blades back, they would have to return as detailed
   physical models, which is a coordinator decision, not a particle
   setting.
4. Rush wake solidity. The vapor volume through the old shell profile
   reads softer than the braided strand shell at full speed. The
   centerline, profile, colors, and fade are identical, but the wake
   will feel less solid until Carter judges it in game.

One smaller note: the tornado's debris chunks use the ground block
sampled when its emission is built. The tornado is stationary, so the
sample cannot go stale in practice.

## Change requests

None. The Phase 1 toolkit covered every Azure surface. Storm rain
density inside the zone belongs to the Storms and AtmosphereClient
owner, as it did before this pass.
