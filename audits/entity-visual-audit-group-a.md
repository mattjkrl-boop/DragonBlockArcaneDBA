# Entity Visual Audit, Group A (Katana, Z Sword, Saber, Brave Sword, Grand Sword)

Audit date: Nov 1, 2026. Audit only, no code changed. Scope: every ability entity spawned by the five weapon items, plus its client renderer, read in full.

## Shared render base (applies to almost every entry below)

- All renderers in this group build custom mesh geometry in code through `submitCustomGeometry`. None use GeckoLib models, billboards, or item renders. The only invisible no-op renderer is DelayedLaunchRenderer.
- Shared helper: `src/client/java/com/dragonblockarcanedba/client/render/ki/KiRenderHelper.java`. `kiRenderType()` (lines 28-29) is `RenderTypes.entityTranslucentEmissive` over the plain white texture `textures/entity/ki_white.png` (line 20): normal alpha blending, full bright, no overlay. All color comes from vertex colors. An additive variant `kiAdditiveType()` (lines 23-24, `RenderTypes.eyes`) exists but is barely used by this group.
- Because the texture is a flat white square, every gradient, glow, and fade in this group is done with per-vertex alpha and color. Where a renderer skips vertex alpha variation, the effect reads flat no matter how much geometry it has.
- Only three entities in the whole group emit server particles in their own tick: ZShockwaveEntity, BraveSlashEntity, and ValorFieldEntity. ZGravitySlamEntity fires a one-time SlamDebris burst. Everything else is mesh only.

## Katana

### KatanaChargeEntity / KatanaChargeRenderer
- Files: `entity/KatanaChargeEntity.java:19`, `client/render/KatanaChargeRenderer.java:23`. Trigger: Flashdraw charge-up visual that follows the caster. Lifetime: event driven, discarded when the caster stops holding the katana or after 6 ticks without a charge update.
- Technique: custom mesh. Counter-rotating ground rings (electric cyan outer, silver-white inner), hilt diamond sparks, 10 to 18 converging crystalline motes.
- Colors and blending: normal blending via kiRenderType, vertex alpha grows on the motes as they converge, global alpha 0.65 + charge x 0.35.
- Motion: rotates, pulses with a sine wave, motes converge inward. No exit fade, it pops out the moment the charge ends.
- Entity particles: none.
- Grade: DECENT. Rich layered charge visual, held back only by the hard pop-out at charge end.

### KatanaAimGuideEntity / KatanaAimGuideRenderer
- Files: `entity/KatanaAimGuideEntity.java:20`, `client/render/KatanaAimGuideRenderer.java:21`. Trigger: aim guide shown while charging the katana dash. Lifetime: event driven, same 6 tick watchdog as the charge entity.
- Technique: custom mesh. Cross-quad laser beam (cyan sheath, white-hot core, radius grows with charge), rotating reticle rings every 6 blocks with diamond brackets, terminal crosshair ring with spokes, 4 energy nodes traveling down the beam, first-person hand offset handling.
- Colors and blending: normal blending, cyan and white vertex colors, beam alpha pulses.
- Motion: rings rotate, nodes travel, beam pulses. Event driven lifetime so no lifetime fade is needed.
- Entity particles: none.
- Grade: RICH. The most complete targeting visual in the group.

### SwiftCrescentEntity / SwiftCrescentRenderer
- Files: `entity/SwiftCrescentEntity.java:17`, `client/render/SwiftCrescentRenderer.java:21`. Trigger: Swift Crescent flying slash wave. Lifetime: synced MAX_LIFETIME, default 10 ticks.
- Technique: custom mesh, three layered volumetric crescents (outer cyan wake, silver crest, white core), inverted cross-cleave crescent, shrinking diamond sparks, rotating shock ring.
- Colors and blending: normal blending, vertex alpha fades to 0 at the trail edge of each layer.
- Motion: quadratic fade out (1 - progress^2), grows 45 percent over its life, ring rotates.
- Entity particles: none.
- Grade: RICH. Layered volume, spatial gradient, and a proper lifetime fade.

### HeavenSplitterEntity / HeavenSplitterRenderer
- Files: `entity/HeavenSplitterEntity.java:17`, `client/render/HeavenSplitterRenderer.java:28`. Trigger: Heaven Splitter finisher dash. Lifetime: synced MAX_LIFETIME, default 20 ticks.
- Technique: custom mesh. 6.5 block vertical blade wall, volumetric beam along the dash path, horizontal shock planes, animated transversal fracture ribs, three rotating shock rings, angled cross-cleave arcs.
- Colors and blending: normal blending, vertical alpha gradient on the wall (0 at the bottom to full at the top), white core wall inside a colored sheath.
- Motion: quadratic fade, grows 20 percent, rings rotate, ribs animate.
- Entity particles: none.
- Grade: RICH. The biggest and most layered single effect in the Katana set.

### HollowAfterimageEntity / HollowAfterimageRenderer (shared: Katana and Saber)
- Files: `entity/HollowAfterimageEntity.java:22`, `client/render/HollowAfterimageRenderer.java:27`. Trigger: dash afterimage left by both the Katana and the Saber. Lifetime: 100 ticks (discard at `HollowAfterimageEntity.java:106`). Side effect: applies DARK_FADED to enemies within 4 blocks every 5 ticks.
- Technique: custom mesh, but the ghost body itself is plain colored boxes built with `KiRenderHelper.drawColoredBox`. It is not the player model and uses no skin texture. Dressing around the body: pulsing expanded halo pass, eye slit box, 6 animated scanline slices, two trailing ribbons, expanding footstep ripple ring, 16 ascending shrinking motes.
- Colors and blending: normal blending, fixed cyan and violet palette. The entity syncs SKIN_COLOR and HAIR_COLOR (`HollowAfterimageEntity.java:26-27`, set from the caster at lines 47-48) and the renderer extracts both into its render state (`HollowAfterimageRenderer.java:62-63`) but never uses them in submit, so every afterimage wears the same colors regardless of the player.
- Motion: ribbons trail with alpha gradient to 0, motes ascend, halo pulses, quadratic fade. But the renderer hardcodes maxLife 80 (`HollowAfterimageRenderer.java:70`) while the entity lives 100 ticks, so the ghost is invisible for its last 20 ticks while still applying its debuff.
- Entity particles: none.
- Grade: DECENT. Rich dressing around a crude box body, with synced per-player colors going unused and a 20 tick invisible tail.

## Z Sword

### ZChargeEntity / ZChargeRenderer
- Files: `entity/ZChargeEntity.java:19`, `client/render/ZChargeRenderer.java:22`. Trigger: Z Sword charge-up visual at the caster. Lifetime: event driven, 6 tick watchdog.
- Technique: custom mesh. Golden ground mandala (two counter-rotating rings plus 8 spokes), 4-arm spiral vortex funnel ribbons, 3 counter-rotating inclined rings, 8 to 14 levitating octahedron prisms, solar ray flares at charge 0.4 and up.
- Colors and blending: normal blending, vertex color gradient on the prisms (gold base to white-gold tip), ribbon alpha increases along the spiral.
- Motion: everything rotates, radius and alpha scale with charge, tremble at high charge. No exit fade, pops out when the charge ends.
- Entity particles: none.
- Grade: DECENT. Dense and well layered, same pop-out weakness as KatanaCharge.

### ZShockwaveEntity / ZShockwaveRenderer
- Files: `entity/ZShockwaveEntity.java:35`, `client/render/ZShockwaveRenderer.java:23`. Trigger: traveling shockwave fired by the Z Sword, including the two trailing sub-waves at max charge. Lifetime: up to 80 ticks, or until a block clip unless pierce is active (charge 0.90 and up).
- Technique: custom mesh. Volumetric golden crescent (outer sheath, main body, white-gold spine), transverse shock ribs, 4 layered wake ribbons, apex solar core, two counter-rotating front compression rings, first-person scale ramp over the first 6 ticks.
- Colors and blending: normal blending, strong lead-alpha to trail-0 vertex gradient along the wave.
- Motion: travels and rotates its rings, but the renderer contains no lifetime progress or fade calculation at all (no progress or fade code anywhere in ZShockwaveRenderer.java), so the wave disappears at full opacity the tick it is discarded.
- Entity particles: yes, the best in the group. SPARK for the main wave and EMBER for sub-waves every 2 ticks, scaled by effect density (`ZShockwaveEntity.java:125-134`), plus a PLAYER_ATTACK_SWEEP sound every 15 ticks (lines 137-140) and 2 trailing sub-waves spawned at tick 3.
- Grade: DECENT. Rich mesh plus real particle support, dragged down by the total absence of a fade on a wave that can live 80 ticks.

### ZStanceAuraEntity / ZStanceAuraRenderer
- Files: `entity/ZStanceAuraEntity.java:19`, `client/render/ZStanceAuraRenderer.java:22`. Trigger: aura shown while the caster holds the Z Sword stance. Lifetime: event driven, 6 tick watchdog, requires the caster to be using the sword.
- Technique: custom mesh. Violet ground rings with a dark singularity disc, 4 inward-contracting event-horizon rings that rise as they shrink, 6 to 8 leaning monoliths with floating gold rune diamonds, inverted gravity funnel mesh, 3 downward spiral stream ribbons.
- Colors and blending: normal blending, dark base to violet tip gradient on the monoliths, alpha gradient down the funnel.
- Motion: rings contract and rise, ribbons spiral downward, pulse and tremble scale with power.
- Entity particles: none.
- Grade: RICH. Distinct silhouette and constant internal motion.

### ZGravitySlamEntity / ZGravitySlamRenderer
- Files: `entity/ZGravitySlamEntity.java:18`, `client/render/ZGravitySlamRenderer.java:22`. Trigger: Z Sword gravity slam impact zone. Lifetime: 45 + powerRatio x 15 ticks (45 to 60).
- Technique: custom mesh. Expanding shock disc, hemispherical dome, 14 to 20 branching trench paths that extend with age, 18 to 28 monolith slabs that rise with age, 8 to 12 vertical geysers.
- Colors and blending: normal blending, violet base with gold core on the trenches, geysers fade from gold at the base to violet alpha 0 at the top, dome alpha 0 at its top.
- Motion: disc expands, trenches extend, slabs rise, linear overall fade (1 - progress).
- Entity particles: one-time SlamDebris burst with the Z_GRAVITY profile on tick 1 (`ZGravitySlamEntity.java:66`).
- Grade: RICH. Ground destruction read is layered, animated, and fades correctly.

### DelayedLaunchEntity / DelayedLaunchRenderer
- Files: `entity/DelayedLaunchEntity.java:17`, `client/render/DelayedLaunchRenderer.java:11`. Trigger: invisible logic timer for the gravity slam root-then-launch; after its delay (40 ticks) it launches the rooted targets with bounce and air-drag modifiers and discards. Synced data: none at all.
- Technique: no-op. The renderer submit method draws nothing (`DelayedLaunchRenderer.java:27-30`, comment at line 28 says it is an invisible marker entity).
- Colors and blending: none, nothing is rendered.
- Motion: none visible. Rooted targets get no telegraph from this entity; the slam visuals come from ZGravitySlamEntity at the impact point instead.
- Entity particles: none.
- Grade: FLAT. Literally invisible by design; it is a timer, not an effect.

## Saber

### SaberSlashEntity / SaberSlashRenderer
- Files: `entity/SaberSlashEntity.java:17`, `client/render/SaberSlashRenderer.java:21`. Trigger: Saber flying slash wave, including the stronger finisher variant. Lifetime: synced MAX_LIFETIME, default 10 ticks.
- Technique: custom mesh, same volumetric crescent family as SwiftCrescent: three layers with trail alpha to 0, tip spurs with alpha gradient; the strong variant is 1.4x scale with an inverted cross crescent and a central clash spark.
- Colors and blending: normal blending, layered vertex alpha.
- Motion: quadratic fade, grows 35 percent over its life.
- Entity particles: none.
- Grade: RICH. Matches the best slash waves in the group.

### SaberVoidTearEntity / SaberVoidTearRenderer
- Files: `entity/SaberVoidTearEntity.java:17`, `client/render/SaberVoidTearRenderer.java:21`. Trigger: Saber void tear rift. Lifetime: synced MAX_LIFETIME, default 12 ticks.
- Technique: custom mesh. Two counter-rotating shock rings, sonic cone (white base to cyan alpha 0 tip), central rift slit that narrows as it fades, 6 rotating rupture spikes.
- Colors and blending: normal blending, expansion from 0.6x to 2.0x scale, quadratic fade. Flaw: the outer dark violet halo quad in drawRiftSlit has alpha 0.0 on all four vertices (`SaberVoidTearRenderer.java:161-167`), so the halo is dead geometry and never renders; the bright slit has no dark rim to sit in.
- Motion: expands, rings counter-rotate, spikes rotate, slit narrows while fading.
- Entity particles: none.
- Grade: DECENT. Good motion and shape, but its one dark contrast element is switched off by the alpha bug.

### SaberDodgeSparkEntity / SaberDodgeSparkRenderer
- Files: `entity/SaberDodgeSparkEntity.java:17`, `client/render/SaberDodgeSparkRenderer.java:25`. Trigger: Saber dodge and clash spark burst. Lifetime: synced MAX_LIFETIME, default 12 ticks.
- Technique: custom mesh. Golden counter-rotating clash rings, 8 clash spikes with base-to-tip gradient, 12 flying diamond shards that travel outward and shrink, white-gold core flash.
- Colors and blending: normal blending, vertex gradients on spikes and shards, quadratic fade, expansion from 0.9x to 2.0x.
- Motion: rings rotate, shards fly and shrink, core flashes. Weakness: rings and spikes are single-sided flat planes in the local XY plane with no camera facing, so the burst thins to nothing edge-on and is culled from behind.
- Entity particles: none.
- Grade: DECENT. Rich content in one plane; viewing angle decides whether it reads.

### SaberDimensionalLineSlashEntity / SaberDimensionalLineSlashRenderer
- Files: `entity/SaberDimensionalLineSlashEntity.java:17`, `client/render/SaberDimensionalLineSlashRenderer.java:25`. Trigger: Saber dimensional line slash finisher. Lifetime: synced MAX_LIFETIME, default 20 ticks.
- Technique: custom mesh. Three nested volumetric beams along the slash line (outer cyan sheath in 12 segments, mantle in 8, white core in 6), transversal crack ribs with alpha 0 tips whose span oscillates, shock rings at both ends and the center.
- Colors and blending: normal blending, ring outer edges fade to alpha 0, rib tips fade to alpha 0.
- Motion: quadratic fade, grows 25 percent, ribs oscillate.
- Entity particles: none.
- Grade: RICH. Layered beam with animated cracks and a correct fade.

## Brave Sword

### BraveCrossSlashEntity / BraveCrossSlashRenderer
- Files: `entity/BraveCrossSlashEntity.java:24`, `client/render/BraveCrossSlashRenderer.java:21`. Trigger: Brave Sword finisher cross slash (the splash half; the primary target is excluded from its hit set). Lifetime: fixed maxLifetime 18 ticks; its AoE damage lands on ticks 1 to 2 only.
- Technique: custom mesh. Two volumetric crescent blades tilted plus and minus 45 degrees (gold sheath, cyan core, white edge, tip spurs), central geometric starburst core flare, 4 piercing diagonal light vanes.
- Colors and blending: normal blending, trail alpha to 0 on each blade layer, starburst center-to-edge color change.
- Motion: quadratic fade, grows 35 percent, starburst rotates (age x 25 degrees), vanes extend and narrow.
- Entity particles: none.
- Grade: RICH. Full X composition with rotation, growth, and fade.

### BraveSlashEntity / BraveSlashRenderer
- Files: `entity/BraveSlashEntity.java:24`, `client/render/BraveSlashRenderer.java:20`. Trigger: Brave Sword basic flying slash projectile (600 default damage). Lifetime: up to 40 ticks (`BraveSlashEntity.java:96`), ending early on hit or block.
- Technique: custom mesh, three nested crescents (gold glow, cyan core, white edge) from a single drawCrescentArc helper, the simplest slash renderer in the group at 104 lines.
- Colors and blending: normal blending, alpha gradient only along the trailing edge of each crescent; the body alpha is constant.
- Motion: the flaw. The renderer extracts age into its render state (`BraveSlashRenderer.java:41`) and then never uses it in submit: no lifetime progress, no fade, no growth. The slash flies at full opacity for up to 40 ticks and pops out. The entity also syncs no data at all (empty defineSynchedData, `BraveSlashEntity.java:46`), so the renderer cannot even know a per-slash scale or lifetime.
- Entity particles: vanilla CRIT, 10 per hit target (`BraveSlashEntity.java:91`).
- Grade: THIN. Decent crescent shape, but it is the only slash in the group with zero lifetime animation.

### BraveChargeEntity / BraveChargeRenderer
- Files: `entity/BraveChargeEntity.java:18`, `client/render/BraveChargeRenderer.java:20`. Trigger: Brave Sword heroic focus charge-up at the caster. Lifetime: event driven, same charge pattern as KatanaChargeEntity (CHARGE_RATIO and CASTER_ID synced, 6 tick idle watchdog, requires holding the Brave Sword).
- Technique: custom mesh. Three counter-rotating ground rings (gold starburst, cyan, white-gold core), 8 to 14 inward-converging valor prism shards with gold base to cyan tip gradient, nested counter-rotating octahedron core (third white octahedron appears at charge 0.4 and up), twin helical valor ribbons.
- Colors and blending: normal blending, base alpha 0.65 + charge x 0.35, ribbon alpha falls along the strand.
- Motion: rings counter-rotate, shards orbit faster and converge as charge rises, core pulses and shrinks, ribbons ascend. No exit fade.
- Entity particles: none.
- Grade: RICH. The fullest charge visual of the three charge entities.

### BraveRushTrailEntity / BraveRushTrailRenderer
- Files: `entity/BraveRushTrailEntity.java:15`, `client/render/BraveRushTrailRenderer.java:21`. Trigger: trail left along the Brave Sword piercing rush dash. Lifetime: fixed maxLifetime 14 ticks; TRAIL_SCALE and TRAIL_LENGTH synced.
- Technique: custom mesh. Faceted Mach shock cone (gold outer, cyan inner) at the head, twin helical streamlines running the full dash length, cross-quad kinetic thrust beam core, expanding wake vortex rings spaced along the path, first-person owner scale handling (0.45x and reduced cone alpha).
- Colors and blending: normal blending, streamline alpha falls along the trail, quadratic lifetime fade (alpha = (1 - progress^2) x 0.90).
- Motion: grows 40 percent, helices rotate with age, rings expand.
- Entity particles: none.
- Grade: RICH. Complete dash read from cone to wake rings, with a proper fade.

### BraveShockwaveEntity / BraveShockwaveRenderer
- Files: `entity/BraveShockwaveEntity.java:15`, `client/render/BraveShockwaveRenderer.java:20`. Trigger: Brave Sword heroic impact shockwave. Lifetime: fixed maxLifetime 18 ticks; MAX_RADIUS synced, default 7.5.
- Technique: custom mesh. Raised beveled golden shock ring (outer edge alpha 0 rising to a white-gold crest), inner cyan harmonic ring, 18 vertical crystalline valor blades around the rim that lean outward, shrinking epicenter starburst core.
- Colors and blending: normal blending, height and radial vertex alpha gradients on the ring.
- Motion: sine-eased expansion to max radius, crest height falls as it expands, blades shrink, quadratic alpha fade. Renderer hardcodes maxLife 18 (`BraveShockwaveRenderer.java`, submit) which matches the entity, so the fade stays in sync.
- Entity particles: none.
- Grade: RICH. Beveled ring plus rim blades is a step above a flat ring effect, and it fades correctly.

## Grand Sword

### GrandBladeShardEntity / GrandBladeShardRenderer
- Files: `entity/GrandBladeShardEntity.java:41`, `client/render/GrandBladeShardRenderer.java:24`. Trigger: blade shard fired by the Grand Sword left click. Lifetime: flying up to 60 ticks with a gravity arc; on block hit it embeds and stays as a tripping hazard for up to MAX_EMBEDDED_TICKS 200 ticks (10 seconds).
- Technique: custom mesh with two full states. Flying: faceted diamond-beveled blade (steel blue and gold faces, raised golden fuller, white-gold edge lines) spinning at 35 degrees per tick with a helical energy wake ribbon. Embedded: the same blade stuck in the ground at its impact angle, plus 6 golden ground fracture crack rays, a pulsing warning ring, and a vertical beacon plume that fades to alpha 0 at the top.
- Colors and blending: normal blending, flat face colors with alpha near 0.95, wake and crack alpha gradients. No fade in or out in either state: it pops in at spawn, and pops out at 200 ticks or the instant an enemy trips it.
- Entity particles: none, despite the class header promising golden crit particles on shatter; there is no sendParticles call anywhere in GrandBladeShardEntity.java (DustParticleOptions is imported and unused).
- Grade: RICH. Two complete visual states with real geometry; only the missing end fades and the missing shatter particles hold it back.

### GrandClashSparkEntity / GrandClashSparkRenderer
- Files: `entity/GrandClashSparkEntity.java:16`, `client/render/GrandClashSparkRenderer.java:25`. Trigger: clash burst spawned when the Grand Cyclone spin deflects an enemy projectile. Lifetime: 10 ticks (entity fields at `GrandClashSparkEntity.java:19-20`); SCALE synced.
- Technique: custom mesh. Two counter-rotating golden clash rings, 8 diamond clash spikes, 12 flying diamond spark shards (seeded by entity UUID so they are stable per burst), white-gold core flash diamond.
- Colors and blending: normal blending, ring alpha fades to 0 at the outer edge, spikes gradient base to tip, quadratic lifetime fade, scale grows from 0.85x to 1.8x.
- Motion: rings counter-rotate, shards fly outward and shrink, core shrinks. Same single-plane weakness as SaberDodgeSpark: rings and spikes live in the local XY plane with no camera facing, so the burst nearly vanishes edge-on.
- Entity particles: none.
- Grade: DECENT. Same rich burst recipe as the Saber dodge spark, same one-plane flaw.

### GrandCrescentWaveEntity / GrandCrescentWaveRenderer
- Files: `entity/GrandCrescentWaveEntity.java:26`, `client/render/GrandCrescentWaveRenderer.java:25`. Trigger: piercing crescent wave fired when Grand Cyclone is released. Lifetime: up to 80 ticks (`GrandCrescentWaveEntity.java:125`) or until a block clip; only CASTER_ID is synced.
- Technique: custom mesh, the most layered crescent in the group: three volumetric bevel layers (gold canopy, white-gold core, white razor edge), upper and lower vortex wing ribbons, two trailing shockwave arcs, 7 inscribed valor glyph diamonds along the arc, first-person scale ramp over the first 6 ticks.
- Colors and blending: normal blending, trail alpha to 0 on every layer, quadratic fade.
- Motion: travels, wing ribbons undulate with age. Flaw: the renderer hardcodes maxLife 50 (`GrandCrescentWaveRenderer.java:70`) while the entity lives up to 80 ticks, so the fade completes at tick 50 and the wave keeps flying and dealing piercing damage invisibly for up to 30 ticks.
- Entity particles: none.
- Grade: DECENT. Richest crescent geometry in the group, undermined by an invisible damaging tail that outlives its own render.

### ValorFieldEntity / ValorFieldRenderer
- Files: `entity/ValorFieldEntity.java:39`, `client/render/ValorFieldRenderer.java:30`. Trigger: Grand Sword right-click valor field, a 9 block protective dome that follows the caster while the sword is in use and Ki drains. Lifetime: event driven (ends when the caster stops using the sword, stops holding it, or runs out of Ki). Synced data: none at all (empty defineSynchedData in ValorFieldEntity.java); the renderer works from position and tick count only and finds suspended projectiles itself on the client.
- Technique: custom mesh at the largest scale in the group. Translucent geodesic dome shell (10 latitudes x 24 longitudes, alpha rising with latitude), 3-tier ground sanctum mandala with counter-rotating rings and 8 cardinal ray trenches, equatorial ring plus two tilted orbital rings precessing in opposite directions, 8 vertical meridian ribs, and rotating octahedral stasis cages drawn around projectiles the field has frozen.
- Colors and blending: normal blending, single gold family (1.0, 0.88, 0.25) with a breathing pulse (0.88 + 0.12 x sin) across all alphas; dome shell sits at low alpha 0.16 to 0.34 so it reads as a shell, not a wall.
- Motion: dome slowly rotates per latitude band, rings and ribs rotate, cages spin at 35 degrees per tick, everything breathes. No fade in or out: the whole 9 block dome appears and disappears in one tick.
- Entity particles: yes. SPARK burst of 36 x effect density on the spawn tick (`ValorFieldEntity.java:125-127`) and 3 SPARK per enemy on each pressure damage tick (lines 158-160), plus beacon sounds.
- Grade: RICH. The most ambitious render in the group; only the instant on/off at cast and cancel is unpolished.

## The 5 worst-looking entities in this group, ranked

1. **DelayedLaunchEntity (Z Sword)** - FLAT. Its renderer draws literally nothing (`DelayedLaunchRenderer.java:27-30`), so for 40 ticks rooted targets stand in a slam zone with no telegraph of the launch that is coming. Most impactful fix: render a small contracting ground ring or tether runes at each rooted target for the delay window, so the pending launch is readable.
2. **BraveSlashEntity (Brave Sword)** - THIN. The renderer extracts age (`BraveSlashRenderer.java:41`) and never uses it: no fade, no growth, full opacity for up to 40 ticks, then a pop. It is also the only slash with no synced data (`BraveSlashEntity.java:46`). Most impactful fix: drive a quadratic fade and slight growth from the already-extracted age, matching every sibling slash renderer.
3. **ZShockwaveEntity (Z Sword)** - DECENT held back by one flaw. The mesh and its own SPARK and EMBER particle trail are strong, but ZShockwaveRenderer.java contains no lifetime fade at all, so an 80 tick wave vanishes at full opacity on discard or block clip. Most impactful fix: add a progress-based fade over the final portion of its life (and a fast fade when it clips a block).
4. **GrandCrescentWaveEntity (Grand Sword)** - DECENT held back by one flaw. The renderer fade ends at tick 50 (`GrandCrescentWaveRenderer.java:70`) while the entity keeps flying and piercing until tick 80 (`GrandCrescentWaveEntity.java:125`): up to 30 ticks of invisible damage. Most impactful fix: sync the real lifetime (or a MAX_LIFETIME value) from the entity and fade against that, so the wave is visible for exactly as long as it can hurt.
5. **HollowAfterimageEntity (Katana and Saber)** - DECENT held back by two flaws. The ghost body is plain colored boxes while the entity already syncs the caster's SKIN_COLOR and HAIR_COLOR (`HollowAfterimageEntity.java:26-27`) that the renderer extracts (`HollowAfterimageRenderer.java:62-63`) and never uses; and its hardcoded maxLife 80 (line 70) makes it invisible for the last 20 of its 100 ticks while it still debuffs. Most impactful fix: color the body boxes from the synced skin and hair colors so the afterimage actually looks like the player who left it.

Honorable mentions: SaberVoidTearRenderer's alpha-0 halo quad (`SaberVoidTearRenderer.java:161-167`) is dead geometry that costs the rift its dark rim. SaberDodgeSparkRenderer and GrandClashSparkRenderer build their bursts in a single local plane, so they nearly disappear edge-on. KatanaChargeRenderer, ZChargeRenderer, and BraveChargeRenderer all pop out with no fade when a charge ends. GrandBladeShardEntity promises shatter particles in its class header and emits none.
