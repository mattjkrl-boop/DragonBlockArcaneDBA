# Spectacle Report: Azure Dragon Sword (Phase 2)

Flagship storm weapon. Theme unchanged: azure storm dragon (cyan azure
palette, wind, lightning, the dragon storm domain). Everything below was
built on the shared blocks from audits/SPECTACLE-STANDARD.md: FxEnvelope,
LayeredParticles, LayeredRender, DebrisSpawner, VortexPhysics, GroundScar,
BoltEntity, AtmosphereServer. No shared file was edited.

Files touched (this weapon only): item/AzureDragonSwordItem.java;
entity/AzureStormEntity.java, entity/AzureTornadoEntity.java,
entity/AzureLightningEntity.java, entity/AzureSonicQuakeEntity.java,
entity/AzureTempestChannelEntity.java; client renderers AzureStormRenderer,
AzureTornadoRenderer, AzureLightningRenderer, AzureTempestChannelRenderer,
AzureRushTrailRenderer. AzureRushTrailEntity is unchanged.

## Before and after numbers

### Dragon Storm Domain (right click release)
| Number | Before | After |
|---|---|---|
| Storm lifetime | 220 ticks flat at every charge | 120 + 220 x chargeRatio ticks: 127 for a 10 tick tap, 340 at full charge |
| Cooldown | 60 + 60 x chargeRatio (60 to 120 ticks), shorter than the storm | lifetime + 100 ticks (227 to 440), always 100 past the storm's end, so storms cannot stack |
| Radius | 10 + 20 x chargeRatio | unchanged |
| Charge Ki cost | 20 percent per minute while held, about 5 percent of max Ki for a full channel | unchanged |
| Strike cadence | one strike every 25 ticks, only if an enemy was inside, about 8 per storm | every 30 to 16 ticks scaled by the envelope, only inside the active window (12 to 85 percent of life); an empty storm strikes the ground so the weather still reads |
| Strike damage | 450, instant on the bolt's first tick | 450 after a 12 tick telegraph, in a true cylinder of radius 3.5 (was a 3.5 half width box, y minus 2 to plus 4; now y minus 2 to plus 6) |
| Rare strike | 20 percent chance, texture tint only | 20 percent chance, damage x 1.25 (562.5), radius 4.5 with a matching wider telegraph, gold BoltEntity |
| Giant tornado spawn | storm tick 1 | at 20 percent of storm life, the peak of the build window |
| Storm mob force | random jitter every 4 ticks (plus or minus 0.8 horizontal, 0.25 up) | VortexPhysics every tick: pull 0.05 to 0.11 by charge, swirl 1.3, core lift 0.12, envelope scaled, weight classed |
| Weather | none | AtmosphereServer Kind.THUNDER plus 0.7 gloom for the storm's whole life, refreshed every 20 ticks so the gloom follows the storm |
| Debris | 0 pieces | 28 to 72 pieces by charge (density scaled), ground sampled, orbit ends inside the decay window so they rain down |
| Storm particles | WIND 8 per 4 ticks, SPARK 3 per 7, KI_MOTE 4 per 5, fixed counts at any radius | LayeredParticles.emitVortex every 2 ticks (core, mid, outer, envelope and radius scaled) plus emitRain drizzle through the decay window |

### Tornado (mini from Rush, giant from Storm)
| Number | Before | After |
|---|---|---|
| Mini life / damage | 60 ticks, 25 per 10 ticks (up to 6 hits, 150 total) | life unchanged; damage only inside the active window, about 4 hits, about 100 total |
| Giant life / damage | 200 ticks, 60 per 10 ticks (up to 20 hits, 1200 total) | life unchanged; damage only inside the active window, about 14 hits, about 840 total, plus fling, debris, scar, and strikes around it |
| Pull and lift | hand rolled tangent add; lift 0.45 mini / 0.65 giant added in full every tick, uncapped | VortexPhysics: strength 0.30 / 0.50, swirl 1.3 / 1.5, lift 0.55 / 0.85, resulting vertical velocity capped at 0.9 by the shared block, weight classed (bosses barely move) |
| Release | none; victims fell with accumulated velocity | last 15 percent of life: one fling, power 1.1 / 1.7, up 0.7 / 1.0, weight classed |
| Giant lightning | instant AzureLightningEntity, 350, every 20 ticks, random ground point | telegraphed strike (10 tick warning), 350, every 20 ticks inside the active window |
| Debris | 0 (12 flat renderer triangles on the giant) | 24 mini / 72 giant real DebrisEntity pieces in 3 sizes, ground sampled |
| Ground | never touched | grass under the inner disk ripped to dirt (chance 0.3 per column per 10 ticks), restored after 240 ticks by GroundScar; base dust burst on touchdown |
| maxLifetime persistence | not saved; a restored giant reverted to 60 while the renderer faded against 200 and popped | saved and restored, with the giant flag as fallback for old saves |

### Azure Lightning (telegraph entity)
| Number | Before | After |
|---|---|---|
| Damage timing | all damage on tick 1 of a 12 tick life | telegraph 12 ticks (storm) or 10 (tornado), damage on the strike tick, 8 tick afterglow; total life 20 / 18 |
| Damage values | 450 storm / 350 tornado / 400 default | unchanged; rare strikes x 1.25 |
| Damage zone | box, half width 3.5, y minus 2 to plus 4; renderer ring drew 4.2 wide | cylinder radius 3.5 (4.5 rare), y minus 2 to plus 6; telegraph ring drawn at exactly that radius |
| Bolt visual | private textured mesh, 22 segment trunk, 44 tall | shared BoltEntity: branched trunk plus 2 to 4 forks from a synced seed, length 44, life 9, azure 0xFF9FD8FF or rare gold 0xFFFFE9A8, ground glow at the strike point |
| Ground sparks | 20 SPARK only if the strike block was air | always, plus a LayeredParticles ground burst and 8 debris pieces |
| Persistence | damage and life were plain fields, unsaved; a restored bolt reverted to 400 and struck again | damage, radius, telegraph, rare, and struck flag all synced and saved; a bolt restored after striking discards instead of re-striking |

### Sonic Quake (Rush impact)
| Number | Before | After |
|---|---|---|
| Cooldown | none; refired on any rush tick touching ground or wall | 30 tick per player gate |
| Ki cost | none | 1 percent of max Ki (60 percent per minute rate over 20 ticks); if the drain fails, no quake |
| Damage | 450 + Strength x 2.5, flat to the rim | same base, distance falloff to 50 percent at the 8 block rim |
| Bounciness on victims | 0.85 modifier, permanent, no removal anywhere in the mod | 0.85 for 60 ticks, removed by the quake entity; pending list saved so an unload cannot strand it |
| Debris / scar | none | 26 debris pieces; grass in the inner 60 percent disk torn to dirt (chance 0.35), restored after 200 ticks; base dust burst |
| Visual life | 18 ticks | unchanged; the entity now stays server side until bounce cleanup finishes, invisible after tick 18 |

### Azure Dragon Rush and melee
| Number | Before | After |
|---|---|---|
| Double Rush detection | wall clock, 400 ms window | game ticks, 8 tick window |
| Mini tornado proc | roll 0.4 / 0.8 per enemy per rush tick | fresh hits only, at most one proc per 25 ticks per rusher; a crowd pass can no longer stack tornadoes |
| Melee gust | Ki drain result ignored; gust and knockback applied at zero Ki | gust particles and knockback only when the drain pays; the plain 800 damage sword hit still lands |
| Rush trail fade | renderer life term hit zero at age 11 of a 12 tick entity life | fade completes at age 12, aligned with the entity |
| Pass through damage | 350 / 600 + Strength x 2.0 | unchanged |

### Tempest channel (right click hold)
| Number | Before | After |
|---|---|---|
| Channel search box | inflate 4.0 on use tick, 6.0 on release | 6.0 in both |
| Release behavior | channel entity discarded in one tick at full alpha | 8 tick collapse, renderer fades it out while the storm takes over |
| Particles | one WIND emitter, 2 to 6 per 2 ticks | LayeredParticles.emitVortex at the renderer's tunnel size (radius 1.2 to 3.2, height 3.2 to 5.6) |
| World interaction | none in a 15 second channel | above 30 percent charge, a gentle VortexPhysics tug (strength 0.04 to 0.14, swirl 1.2, lift up to 0.2) every 2 ticks; still no damage while channeling |

## What was added, per point

1. Layered depth. The tornado funnel is now LayeredRender.drawVortexBands
   (bright core at 0.38x, theme mid, wispy outer at 1.45x, counter rotating)
   plus an anchor glow, and its old private funnel, ribbons, and halo pass
   were removed so one layered system draws it. The storm dome gained its
   missing Layer B: an inner shell at 0.8 radius and 0.85 height, brighter,
   counter rotating against Layer A, and Layer A's alpha now thins toward
   the rim instead of brightening upward. Both storm and tornado take
   LayeredParticles.emitVortex (core, mid, outer) scaled by the shared
   envelope, and segment counts step down with LayeredRender.distScale in
   the storm (ground bands, dome, shafts, arcs) and tornado (rings, crown,
   vortex bands).
2. Debris. Storm: 28 to 72 pieces by charge. Giant tornado: 72. Mini: 24.
   Quake: 26 (finisher class). Lightning strike: 8 ejecta. All are real
   DebrisEntity blocks sampled from the ground under the effect, three
   sizes, helical orbits, released to rain down (storm and tornado orbits
   end inside the decay window on purpose).
3. World interaction. Storm and tornado run VortexPhysics: pull in, swirl,
   core lift capped, and the tornado flings everything out at release.
   Tornado and quake tear grass to dirt through crash safe GroundScar at
   per column surface heights. Base dust bursts at storm start, tornado
   touchdown, quake impact, and every lightning strike. The channel tugs
   mobs gently while it builds.
4. Atmosphere. The storm drives AtmosphereServer with Kind.THUNDER and 0.7
   gloom for its exact lifetime (prior weather saved and restored by the
   shared system), refreshed every 20 ticks so the gloom center tracks a
   following storm. Lightning is BoltEntity strikes. No other Azure move
   gained weather; the theme did not have it anywhere else.
5. Lifecycle. Storm life scales with charge and every visual keys off
   FxEnvelope over the real life: renderer fade (20 tick build, 40 tick
   decay fractions), particle counts, strike cadence and window, debris
   orbit length, dissipation rain. Tornado damage and strikes only fire
   inside the active window; the funnel releases with a fling instead of
   dropping victims. Lightning telegraphs, strikes at its peak tick, and
   leaves an 8 tick afterglow. The channel collapses over 8 ticks on
   release instead of popping out.

## Bugs fixed (from the fresh audit's Azure list)

1 and 2. Unused imports in the item (MobEffectInstance, AzureLightningEntity): removed.
3 and 4. Unused DustParticleOptions and ParticleTypes imports in the storm and tornado: gone in the rewrites.
5. Promised second dome layer missing: implemented (Layer B above).
6. Weather domain with no weather: AtmosphereServer THUNDER plus gloom.
7. Storm save incomplete (only 2 of the synced fields): radius, follow mode, target (as a UUID, re-resolved lazily), max charge, giant flag, debris flag, lifetime, charge, and age are all saved.
8. Tornado maxLifetime unsaved: saved, renderer now reads the entity's real value.
9. Lightning damage and life unsaved, restored bolt re-struck at 400: all synced and saved; a restored struck bolt discards quietly.
11. Rush trail lifetime mismatch: renderer fade now completes at the entity's 12th tick.
12. Sonic Quake permanent 0.85 bounciness: the quake entity owns removal after 60 ticks, with the pending list persisted and retries for victims in unloaded chunks.
13. Rush trail renderer WAKES and WRITER maps never cleaned: a wake whose newest nose is over 24 ticks old drops both entries in submit.
14. Channel search and discard radii disagreed (4.0 vs 6.0): both 6.0, and releasing channels are excluded from the active channel search.
15. Storm follow target death was a dead end: the storm falls back to following the owner.
16. Tornado's unchecked (LivingEntity) owner cast on the lightning tick: guarded, same pattern the storm already used.
17. Tornado zone and renderer sizes disagreed (giant 14.4 pull vs 12.6 drawn; mini height 6.0 vs 6.5): renderer now uses the entity's numbers, radius scale x 8 / x 3, height scale x 16 / x 6.
19. Lightning visual and damage footprints disagreed (ring 4.2 vs box 3.5): telegraph ring and damage cylinder are both radius 3.5 (4.5 rare).
20. Melee Ki drain result ignored: the gust is gated on payment.
21. Double Rush used wall clock time: game ticks now.
22. Channel visual popped out on release: 8 tick collapse fade.

Not fixed, deliberately: audit item 10 (dead getMaxLifetime accessors on the
rush trail and quake) and item 18 (entity sizes and tracking ranges in
DbaEntities) and item 23 was a checked clear scan. Item 18 is a shared file;
see the requests below. Item 10's constants agree with the entity values
(18 and 12), so the accessors are harmless dead code in files whose
renderers predate them.

## Shared change requests (not edited, per the coordination rules)

1. entity/DbaEntities.java tracking ranges (audit item 18): AZURE_STORM is
   clientTrackingRange 32 while its radius reaches 30 and its dome is drawn
   wider; AZURE_TORNADO is 16 while a giant is 25.2 blocks wide. Suggest
   AZURE_STORM 64 and AZURE_TORNADO 32 so the effects stop popping in at
   tracking range. The new BOLT and DEBRIS entries the coordinating pass
   added are fine as registered.
2. client/render/ki/LayeredRender.java: the standard names a drawLayeredDome
   block, but the shipped LayeredRender has only drawVortexBands and
   drawAnchorGlow. I implemented the storm's second dome locally inside
   AzureStormRenderer. If the Curse and Dabura domain agents need the same
   shape, extracting that Layer B code into LayeredRender.drawLayeredDome
   once would keep the domes in one style.
3. util/DebrisSpawner.java javadoc says counts are scaled by effect density
   "inside DebrisEntity counts at the call sites", which reads both ways. I
   scaled by DbaEffectsConfig.effectDensity() at my call sites and passed
   the scaled count. A one line javadoc clarification would save every other
   agent the same guess.

## Worse check (honest)

Nothing was reverted. The trade offs I can identify, stated plainly:

- Tornado total damage went down (active window gating: giant about 840
  total instead of a theoretical 1200, mini about 100 instead of 150).
  That is the standard's point 5 applied on purpose, and the tornado gained
  the fling, debris, scar, and capped lift around it. Damage per hit is
  unchanged.
- Storm strikes are dodgeable now: the 450 lands 12 ticks after the warning
  appears, where the old bolt hit instantly. That is the telegraph the task
  asked for ("strike damage keeps a telegraph"), traded against guaranteed
  burst.
- The old textured azure lightning bolt mesh (with its rare texture) is
  retired. The BoltEntity bolt is a different visual: seeded branches,
  sheath and core ribbons, no texture. It matches the shared standard; if
  the textured look is preferred, that is the one identity trade in this
  pass, and the telegraph ring keeps the azure and gold rare colors.
- The channel's new vortex tug is interaction the channel never had. It is
  weak (0.14 strength at full charge), weight classed, and deals no damage,
  but it is a behavior addition, flagged here rather than hidden.
- The Minecraft client cannot run in this sandbox, so the final look of
  the funnel bands, the second dome, and the telegraph is verified by code
  audit and the javac sweeps below, not in game. Carter's PC test is the
  real look check.

## Verification

Dual javac sweeps (the agreed audit method; Gradle cannot run in this
sandbox and the compile check stands waived by Carter, not passed): full
tree compiled against the 26.2 and 26.3 classpaths, HEAD baseline worktree
against the post edit working tree, kinds stored under
~/workspace/azure-sweep-out/. Baselines at HEAD 5e4b1de: 132 error kinds on
26.2, 141 on 26.3. Current tree: 135 and 147. New kinds versus baseline on
26.2: KatanaChargeRenderer (variable dist already defined),
ZShockwaveEntity (variable density already defined), CurseBladeItem
(cannot find symbol). On 26.3: the same three plus BraveShockwaveEntity,
KingsSlamEntity, and OxShockwaveEntity (cannot find symbol). Every one of
those sits in another Phase 2 agent's files, mid edit at sweep time. Error
kinds touching any Azure file or AzureDragonSwordItem: zero on 26.2, zero
on 26.3, so this weapon adds no new compile error kinds on either version.
An earlier sweep attempt lost its baseline files when /tmp was wiped mid
run; the rerun above wrote everything to persistent storage and its
baselines reproduce the known HEAD numbers exactly.
No em dashes in any added code, comment, or this report.
