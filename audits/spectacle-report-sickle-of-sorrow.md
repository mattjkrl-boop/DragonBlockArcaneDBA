# Spectacle Report: Sickle of Sorrow

Phase 2 weapon report. Files owned and touched: item/SickleOfSorrowItem.java,
entity/SorrowSlashEntity.java, entity/DimensionalRiftEntity.java,
client/render/DimensionalRiftRenderer.java. Theme unchanged throughout: soul
reaping, a melting dimensional rift, sorrow. Violet and magenta palette, soul
particles, and the singularity core are all kept as the weapon's identity.

## State found on arrival (previous agent's partial work)

A previous agent died mid-work from an infrastructure error. Its edits were in
the working tree, uncommitted, across the item, both entities, and the rift
renderer. I audited all four files in full against the fresh audit's before
picture and against the shared block signatures in util/ and
client/render/ki/LayeredRender.java. Finding: the work was not half done, it
was complete and coherent. Every shared call matches its real signature
(VortexPhysics.applyVortex and fling, LayeredParticles.emitVortex,
emitGroundBurst and emitRain, DebrisSpawner.spawnField and sampleGroundPalette,
GroundScar.scarDisk, FxEnvelope.envelope, smoothstep and segments,
LayeredRender.drawVortexBands, drawAnchorGlow and distScale). I kept all of it
and made no further code changes of my own; my work was the audit, the
verification sweeps, and this report. TimeShatter is not assigned to the sickle
in the fresh audit (no mention of it in the sickle section), so no TimeShatter
files were touched.

## Before and after numbers

Melee and air swing numbers did not change: melee 750 + Strength x 3, Melting
III (amplifier 2) for 300 ticks, Sorrow Rift 80 ticks, Soul Rend 3 percent,
Ki drain spread over 13 ticks (melee) and 8 ticks (air swing), wave speed 1.6,
wave life 30 ticks. Everything below is the Dimensional Rift (right click).

| Value | Before | After |
| --- | --- | --- |
| Rift cooldown | 60 ticks item cooldown PLUS a static UUID map gate of 60 ticks (two sources of truth) | 120 ticks (6 s), vanilla item cooldown only, longer than the rift's 60 tick life so rifts cannot stack |
| Ki charge timing | Drain charged inside the gated call; the two gates could disagree about whether the move was allowed | Drain charged only after the cooldown check passes; a failed drain returns before the cooldown is set, so a blocked cast costs nothing and locks nothing |
| Burst damage scaling | 500 + Spirit x 2 (the only Strength weapon ability in the set scaling off Spirit) | 500 + Strength x 3, same stat as this weapon's melee and air swing |
| Burst timing | On a target's first entry into the radius, tracked per target in a HashSet | Once, at tick 20, the expansion peak, on every target inside at that moment |
| Tick damage | 35 every 10 ticks across the whole life, about 210 total | 60 every 10 ticks at ticks 15, 25, 35, 45 (the hold only), 240 total |
| Rift radius curve | 3 to 15 linearly over 40 ticks, 10 ticks at full size, collapse flag at 50 | 3 to 15 smoothstepped over 20 ticks, 30 tick hold at full size, collapse from tick 50 contracting 15 to 7 by tick 60 |
| Pull | Hand rolled straight line drag, max(0.3, 1.4 x (1 - distance / 15)) per tick plus 0.15 up, no weight classes, no swirl | VortexPhysics.applyVortex: strength 0.5, swirl 0.85, lift 0.55, scaled by the shared envelope and by MobPhysics weight classes (bosses barely move, players keep 40 percent more control) |
| Collapse | Nothing: pulled targets simply stopped being pulled at tick 60 | Implosion fling at tick 50: VortexPhysics.fling power 1.5, up 0.85, weight scaled, plus a particle inrush burst and sound |
| Melting from the rift | Amplifier up to 9 by closeness, duration 30 + amplifier x 3 seconds, up to 57 s (1140 ticks), nearly 4x the longest effect anywhere else in the set | Amplifier 0 to 3 (Melting I to IV) by closeness, fixed 200 ticks (10 s), refreshed while inside. Now the set's sane range: the weapon's own melee Melting is 300 ticks |
| Particles | 4 SPARK plus 2 VOID_SMOKE every 8 ticks (about 15 per second over a radius 15 disc), plus one collapse burst | LayeredParticles.emitVortex every tick (bright KI_MOTE core, violet helical mid band, VOID_SMOKE outer wisps), ground burst on cast, emitRain drizzle through the collapse |
| Debris | None. The only orbiting geometry was the renderer's 18 identical pyramids | 64 DebrisSpawner pieces at full density (soul sand, soul soil, basalt, blackstone, crying obsidian, blended with blocks sampled from the ground under the cast), helical orbits for 52 ticks, released to rain down through the collapse |
| Ground interaction | None | GroundScar: soul soil disk, radius 4, 45 percent chance per cell, restored after 300 ticks, protected blocks refused inside GroundScar |

## Per point

1. LAYERED DEPTH. The renderer now draws the shared LayeredRender anchor glow
   under everything, keeps the three counter rotating accretion rings, and adds
   LayeredRender.drawVortexBands: three counter rotating bands (bright core,
   violet mid, wispy outer) around the tear planes, so the rift body has depth
   above the ground plane, not just flat rings. Server side, emitVortex runs
   the matching three particle layers every tick. Segment counts and the band
   work step down with LayeredRender.distScale beyond 32 and 64 blocks.
   Renderer alpha now comes from the same FxEnvelope fractions the entity
   uses, so geometry, particles, and pull all breathe together.
2. DEBRIS. The cast throws a 64 piece DebrisSpawner field on helical orbits
   (soul themed palette plus real ground blocks sampled at the cast site),
   released at tick 52 so pieces rain back down during the collapse instead of
   vanishing. The renderer's 18 signature spikes are kept as the weapon's
   identity, but each is now its own shard: deterministic per spike variation
   in size (0.75x to 1.30x), thickness, orbit band (two bands), height band,
   bob phase and amplitude, and precession speed, with golden angle spacing
   breaking the uniform ring.
3. WORLD INTERACTION. Pull is the shared VortexPhysics (inward plus tangential
   swirl plus core lift, weight classed). The collapse flings everything
   caught. The ground under the rift is temporarily scarred to soul soil
   through GroundScar and restored. A base dust burst fires on cast. No
   permanent block destruction: a soul rift scars and restores, which fits
   the theme's scale.
4. ATMOSPHERE. The sickle has no weather theme, so per the standard it gains
   no weather. Its atmosphere is light and particles: the anchor glow disc,
   the layered glow bands, per tick layered particles, and the collapse
   drizzle. AtmosphereServer was deliberately not called.
5. LIFECYCLE. The rift runs the shared FxEnvelope across a restructured 60
   ticks: 20 tick expansion (build fraction 1/3), 30 tick hold at full size
   and full density, collapse from tick 50 (decay fraction 1/6) with radius
   contraction, the fling, and the rain drizzle. The burst lands at tick 20,
   the visual's peak, and tick damage is spread across the hold and stops
   before the collapse. Nothing pops in at full alpha and nothing discards at
   visible alpha: renderer alpha is the entity's envelope.

## Bugs fixed

- Dual cooldown desync: the vanilla item cooldown and the static
  RIFT_COOLDOWNS UUID map ran side by side and could disagree, double locking
  or unlocking early, and the map held server state for the life of the
  process, pruned only on ability attempts. The map is deleted. The vanilla
  item cooldown is the single source of truth, checked and set inside
  performTechniqueDimensionalRift, which both use() and the technique
  callbacks in DragonBlockArcaneDBA funnel through. A cooldownStack helper
  resolves the held sickle (or a bare stack of the registered item, same
  cooldown group) for the callback path where a different item may be held.
- Melee burst entity with damage 0.0f: SorrowSlashEntity's impact burst mode
  is now an explicit, documented pure visual branch at the top of its tick:
  it never scans targets and never runs hit logic (the old invisible
  damage <= 0 guard inside the wave's hit loop is gone), it puffs soul
  particles on its first tick, and it expires at 16 ticks. The item comments
  at the spawn site state the same contract, so the convention is no longer
  invisible to a reader of either file.
- Melting duration out of family: amplifier 9 for up to 57 seconds is now
  amplifier 3 maximum for a fixed 10 seconds (see the numbers table).
- Burst stat mismatch: the rift burst scaled off Spirit while the weapon's
  melee and air swing scale off Strength; now Strength x 3 throughout.
- Burst timing dishonesty: the burst used to land on first entry, including
  tick 1 of a 60 tick visual. It now lands at the expansion peak, tick 20,
  per the standard's telegraph rule.

## Shared change requests

None required. One observation for the coordinating pass, not a request:
LayeredParticles.emitVortex applies the standard envelope fractions (build
0.20, decay 0.30) internally, while the rift's own envelope uses build 1/3 and
decay 1/6, so the particle density peaks a few ticks before the renderer
geometry does. It reads fine as a lead in, but if strict agreement is wanted,
an emitVortex overload taking build and decay fractions would close it for
every weapon that passes custom fractions.

## Worse check

Honest trades, none judged a fail, nothing reverted:

- Pull feel changed: the old hand rolled drag was stronger per tick at point
  blank (up to 1.4 velocity straight in) than VortexPhysics at strength 0.5.
  The new pull is gentler per tick but adds swirl, core lift, weight classes,
  and envelope scaling, and the rift's own friction and air drag modifiers
  still hold targets once caught. Net: capture is less violent, control is
  better, and the motion matches the standard's shared physics. Kept.
- The burst no longer hits a target that walks in after tick 20. That is the
  standard's peak tick rule, and the raised tick damage (240 versus 210)
  compensates anyone standing in the hold. Kept.
- Everything else is additive or a strict fix: no visual element was removed,
  the spikes, rings, tears, and singularity core are all still drawn.

## Verification

Targeted javac audit (the sandbox cannot run Gradle; this is the repo's audit
method, not a build): my four files plus SorrowSlashRenderer compiled against
the 26.2 and 26.3 classpaths, baseline worktree at HEAD 5e4b1de versus the
current tree. Error kinds in my files: baseline 0 on 26.2, 0 on 26.3; current
0 on 26.2, 0 on 26.3. Zero new error kinds. No em dashes in any added line.
Not committed, per the coordination rules.
