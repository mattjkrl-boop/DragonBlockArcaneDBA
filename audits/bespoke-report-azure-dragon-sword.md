# Bespoke Pass Phase 2 Report: Azure Dragon Sword

Base commit: 2ef870b (Phase 1 foundation). Status: converted, verified by
targeted javac on both classpaths, NOT committed.

Files edited (13): item/AzureDragonSwordItem.java; entities AzureStormEntity,
AzureTornadoEntity, AzureTempestChannelEntity, AzureSonicQuakeEntity,
AzureLightningEntity; renderers AzureStormRenderer, AzureTornadoRenderer,
AzureTempestChannelRenderer, AzureSonicQuakeRenderer, AzureLightningRenderer,
AzureRushTrailRenderer. One extra one-line touch: AzureRushTrailEntity.java
had a pre-existing em dash in its header comment, cleaned (see Notes).
No shared file was edited.

## Before and after counts (Azure files only, measured by grep)

| Measure | Before | After |
|---|---|---|
| Vanilla ParticleTypes references | 0 | 0 |
| Vanilla DustParticleOptions references | 0 | 0 |
| Vanilla SoundEvents lines (imports + call sites) | 13 (4 imports, 9 call sites) | 0 |
| Vanilla ability damage call sites (damageSources()) | 6 lines (item 2, tornado 2, lightning 2) | 0 |
| KiRenderHelper primitive draws (box/cylinder/disc/sphere/helix) | 0 | 0 |
| Local regular geometry helpers in renderers | 18 named helpers | 0 |
| Direct VertexConsumer.addVertex lines in renderers | 80 | 0 |
| java.util.Random uses in renderers | 3 (storm 2, channel 1) | 0 |
| Entity rain emission (LayeredParticles.emitRain) in Azure entities | 2 (storm, tornado) | 0 |
| Entities with a synced render seed | 0 of 5 | 5 of 5 |
| Em dashes in edited files | 2 (pre-existing header comments) | 0 |

Particles were already clean before this pass (the spectacle pass had moved
every Azure emitter to DbaParticles); the count is verified at HEAD, not
assumed. The geometry problem was entirely hand-rolled local helpers, and
that is what this pass removed.

Helpers deleted, by renderer: Storm (drawRotatingRing, renderBeam, drawQuad);
Tornado (drawRotatingRing, drawTriangle, drawQuad); Channel (drawRotatingRing,
renderBeam, drawTriangle, drawQuad); Quake (drawBeveledShockRing, drawFlatRing,
drawTriangle); Lightning (drawChargeBeam, quad, drawGroundRing); RushTrail
(drawTriangleGrad, drawQuad).

## Rule 1, geometry: builders used

Every renderer now draws only through BespokeGeometry and the already
bespoke LayeredRender helpers (drawVortexBands, drawAnchorGlow). 35 direct
BespokeGeometry call sites across the six renderers.

- Storm: ground whirlpool, 4 brokenVortexBand slices at the old rings' radii,
  heights, spin speeds, colors, and alphas; dome, 2 displacedDome shells
  (outer at full radius, inner at 0.8 radius and brighter, as before); tempest
  eye, 6 stacked brokenVortexBand levels with the old near-white to cyan
  gradient; in-dome arcs, branch ribbons (cyan sheath width 0.32 plus white
  core width 0.10, plus a fork branch), placed by BespokeGeometry.Rng on the
  old 3 tick reseed cadence. The geometric rain shafts are deleted: rain in
  the storm zone is AtmosphereClient's job now (Rule 5 system).
- Tornado: base suction, 2 brokenVortexBand slices at the old rings' speeds
  and alphas; funnel unchanged (drawVortexBands, already bespoke); orbiting
  shards, bladeArc crescents standing vertical in the tangent plane, one
  posed submit per blade, riding the old triangles' exact orbit (heights,
  radii, angular speeds, 12 giant / 6 mini), two nested arcs per blade;
  crown, one brokenVortexBand at the old crown radius and counter rotation.
- Channel: base, 3 brokenVortexBand slices at the old rings' radii, speeds,
  and alphas; tunnel, drawVortexBands sized so its outer band lands on the
  old tunnel wall, mid band alpha matched to the old tunnel alpha; blades,
  bladeArc crescents in posed submits on the old orbit (4 to 8 by charge);
  electric arcs (charge 0.25 and up), branch ribbons on the old half tick
  reseed cadence, sheath and core widths and alphas as before (the core drew
  at alpha 1.0 and still does).
- Quake: crest, brokenVortexBand standing at the old beveled crest's height
  and radius; additive echo, a second brokenVortexBand at 1.05x radius on
  the additive render type, as before; inner compression, flat
  brokenVortexBand at 0.55x radius; pressure needles, shardFan at the old
  needle circle radius (0.85x current radius); epicenter, crackStar plus a
  plume dust crown.
- Lightning: warning ring, flat brokenVortexBand whose columns straddle the
  exact strike radius, over the drawAnchorGlow fill at exactly that radius;
  converging ring, glowFan shrinking on the old curve; charge column, branch
  from ground to 30 high at the old beam width; afterglow, brokenVortexBand
  at the strike radius. The bolt itself stays BoltEntity's job, untouched.
- RushTrail: the wake sampling, writer election, centerline, and frame math
  are byte-for-byte the old logic; only the mesh changed. Core, a chain of
  branch ribbons down the centerline at the old core diameter; shell, 6
  helical branch strands riding the old shell radius profile at the old
  ribbons' twist rate, widths set to overlap into a surface; shock lip,
  branch segments around the mouth at 1.22x shell radius inside the old
  station window (t 0.28 to 0.48); nose, one short bright branch at the old
  cap alpha. Per segment alphas, the fade curve, and the hot to shell color
  blend are the old tube's numbers exactly.

Seeds: AzureStormEntity, AzureTornadoEntity, AzureTempestChannelEntity,
AzureSonicQuakeEntity, and AzureLightningEntity each gained a synced LONG
SEED (RenderSeed), set once in the spawning constructor from the entity
UUID, exposed as getRenderSeed(), and persisted in save/load next to the
rest of each entity's state (the channel is transient by design, so its
seed is synced but not saved, like the channel entity itself). Render
states now read the seed from getRenderSeed() instead of the raw UUID.
The rush trail seeds from its entity's already synced CASTER_ID, the same
key its wake maps use.

## Rule 2, particles

Zero vanilla particle types before and after. The one behavioral change:
LayeredParticles.emitRain calls were removed from AzureStormEntity and
AzureTornadoEntity. Storm rain belongs to the AtmosphereClient system,
which renders rain inside every active AtmosphereServer zone; the storm
refreshes its zone every 20 ticks for its whole life, so storm rain is
still present, drawn by the system instead of the entity. See the worse
check for the mini tornado nuance.

## Rule 3, sounds: swaps (call sites, volumes, pitches unchanged)

| File | Vanilla | Custom |
|---|---|---|
| Item, rush pass-through hit | PLAYER_ATTACK_SWEEP 1.0 / 1.3 | DbaSounds.SLASH_WHOOSH |
| Item, rush loop | ELYTRA_FLYING | DbaSounds.FLIGHT_WIND |
| Item, channel loop (every 20 ticks) | LIGHTNING_BOLT_THUNDER | DbaSounds.THUNDER_CRACK (the paired custom LIGHTNING_ZAP at the same site is unchanged) |
| Item, channel loop | ELYTRA_FLYING | DbaSounds.FLIGHT_WIND |
| Item, quake | GENERIC_EXPLODE (Holder, .value() dropped) | DbaSounds.BOOM_BIG 2.0 / 0.8 |
| Item, storm summon | LIGHTNING_BOLT_IMPACT 3.0 / 0.8 | DbaSounds.LIGHTNING_ZAP |
| Storm entity, ambient every 40 ticks | LIGHTNING_BOLT_IMPACT 1.0 / 0.9 | DbaSounds.LIGHTNING_ZAP |
| Tornado entity, howl every 20 ticks | ELYTRA_FLYING 1.5 / 0.8 | DbaSounds.FLIGHT_WIND |
| Lightning entity, strike | LIGHTNING_BOLT_THUNDER 2.5 / 1.0 | DbaSounds.THUNDER_CRACK (paired LIGHTNING_ZAP unchanged) |

The storm's scheduled thunder is AtmosphereClient's (THUNDER_CRACK /
THUNDER_DARK) and was not touched; AtmosphereServer and AtmosphereClient
are unmodified.

## Rule 4, damage

All ability damage now goes through DbaDamage.tempest, with
DbaEffectsConfig.capDamage wrapping exactly as before and every damage
number unchanged:

- Item rush pass-through: mobAttack(player) to tempest(player).
- Item quake falloff: mobAttack(player) to tempest(quake, player), so the
  quake entity is the direct entity, matching the tornado and bolt pattern.
- Tornado ticks: indirectMagic(this, owner) to tempest(this, owner);
  no-owner fallback magic() to tempest(this, null).
- Lightning strike: indirectMagic(this, owner) to tempest(this, owner);
  no-owner fallback lightningBolt() to tempest(this, null), per the
  standard's mapping.

Plain melee is the sword's ordinary item attack and is untouched. The
storm deals no direct damage itself (its strikes are AzureLightningEntity,
converted above); its vortex forces, debris, and strike cadence are
unchanged.

## Verification (javac audit method, no Gradle claim)

- Baseline at 2ef870b, measured this session with the full-tree sweep:
  132 error kinds on the 26.2 classpath, 145 on the 26.3 classpath, zero
  of them in Azure files.
- Targeted javac of the 13 Azure files (plus AzureRushTrailEntity, pulled
  in as a dependency) with the project sourcepath, both classpaths:
  zero errors in any Azure file on either classpath. The only errors
  reported were pre-existing baseline kinds in unrelated dependency files
  (DbaItems, DbaMenus on both; DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity, LavaSwordGround on 26.3),
  each confirmed present verbatim in the baseline kind lists.
- Full-tree sweep after the conversion: 132 kinds (26.2), 140 kinds
  (26.3). Diff against baseline: zero new kinds on either classpath
  (comm -13 empty both ways), zero Azure kinds. The 5 kinds that
  disappeared on 26.3 are all in sibling weapons' files (SpikeFlailSpin,
  SpikeFlailSwing, VoidRift, ErasureCannonBeam twice), removed by the
  concurrent sibling conversions, not by this work.
- Grep proof after conversion, across all 13 files: ParticleTypes 0,
  DustParticleOptions 0, SoundEvents 0, damageSources() 0, every deleted
  helper name 0, KiRenderHelper primitive names 0, java.util.Random 0,
  emitRain 0.

## Honest worse check

Nothing was reverted, but four changes trade fidelity for the standard,
stated plainly:

1. Storm dome height. displacedDome is a true hemisphere: its height
   equals its radius. The spectacle dome reached 1.35x radius plus a 2
   block base lift (top at about 18.2 for a radius 12 storm); the bespoke
   dome tops out at 12 and sits on the ground. The storm reads lower and
   wider, though the silhouette is now torn instead of a perfect shell,
   and the eye column was shortened to match the dome. This is the
   standard's prescribed builder behaving as designed; see change
   request 3.
2. Telegraph ring crispness. The lightning warning ring was a ruled
   circle at exactly the strike radius; the broken band's columns jitter
   between 0.8x and 1.25x radius and about 16 percent of columns are
   seeded gaps. The filled anchor glow at exactly the strike radius
   remains the precise boundary cue, and the damage circle itself is
   unchanged, but the hard ruled edge is gone.
3. Quake needles. The vertical pressure spikes became a flat shardFan:
   shards radiate along the ground with seeded lift (up to 0.22x radius)
   instead of standing 1.6 blocks tall. The crest also lost its two-tone
   bevel (white crest over a zero alpha outer slope) and is now a single
   color torn band; the additive echo survives. Of all the quake pieces,
   the needles changed silhouette the most.
4. Rush wake texture. The continuous shell tube is now six helical
   branch strands. At speed it reads as braided wind rather than a
   smooth cone; the shape, length, fade, and colors are the old ones,
   but if any piece of this conversion is judged worse in game, it is
   this one. The toolkit has no swept tube builder (change request 1).
5. Mini tornado drizzle. Removing emitRain from the tornado costs the
   standalone mini tornado its falling wind motes in the last 30 percent
   of its 3 second life; no atmosphere zone covers it. The giant tornado
   loses nothing (it lives inside its storm's zone, where
   AtmosphereClient renders rain). The funnel's own envelope fade is
   unchanged.

Everything else maps one to one: same radii, heights, speeds, alphas,
colors, counts, lifetimes, damage numbers, cooldowns, and mechanics.

## Change requests (toolkit gaps, shared changes for the coordinating pass)

1. A swept tube builder: given a centerline (list of points with frames)
   and a per-station radius profile, emit a torn, seeded tube. Rush wakes
   and future trail effects need it; the branch strand shell in
   AzureRushTrailRenderer is an approximation until it exists.
2. An orientable plume or disc stack: plume discs only lie in the local
   XZ plane, so vertical spike fans (quake needles) and arbitrarily
   tilted puffs cannot be expressed; shardFan is the current stand-in.
3. A height control for displacedDome (a Y scale or an explicit height
   parameter): domes are locked to hemisphere proportions, which lowered
   every tall dome converted to it, the Azure storm included.

## Notes

- AzureRushTrailEntity was not in the task's named entity list and was
  not functionally edited; its renderer was converted because it belongs
  to this weapon alone (spawned only by AzureDragonSwordItem) and Rule 1
  covers weapon renderers. Its only entity-file change is the header
  em dash cleanup, and one in the item header; both predate this pass.
  If the coordinating pass wants the rush trail carved out instead, the
  renderer conversion is self-contained and can be reassigned as a unit.
- The channel renderer keeps distScale at 1.0, as the old renderer used
  fixed segment counts and the effect sits on the caster.
- No commit was made. Working tree changes are exactly the files listed
  at the top plus this report.
