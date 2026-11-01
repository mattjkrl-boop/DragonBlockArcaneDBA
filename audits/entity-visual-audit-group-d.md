# Entity Visual Audit Group D (Nov 1, 2026)

Audit only, no code changed. Scope: every spawned ability entity for Power Pole, Whis Staff, Bansho Fan, and Blaster Gun, plus each entity's client renderer read in full. Line numbers are from the current main branch files.

Shared facts for the whole group:

- All 11 renderers draw custom geometry built in code through `collector.submitCustomGeometry`. None uses a GeckoLib model, an item render, a billboarded sprite, or a no-op renderer.
- All 11 use the same render type: `KiRenderHelper.kiRenderType()` returns `RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE)` (`client/render/ki/KiRenderHelper.java:28-29`). That is normal alpha blending (not additive), full-bright emissive, with a plain white texture, so every color below is a flat vertex color and every gradient is a per-vertex alpha or color change coded by hand.
- The group is unusually strong overall: 7 of 11 grade RICH. The ranked list at the end is relative to this group, not to the mod at large.

---

## Power Pole

### 1. PowerPoleWhirlwindEntity (Whirlwind Staff spin visual)
- Entity: `entity/PowerPoleWhirlwindEntity.java`, maxLifetime 8 ticks (lines 26, 43). Renderer: `client/render/PowerPoleWhirlwindRenderer.java`. Trigger: Power Pole left-click hold spin, `item/PowerPoleItem.java:157`. The entity follows the owner's eye position and look angles every tick (entity lines 98-103).
- Technique: custom mesh in code, five layers in `submit` (renderer lines 105-306): two spinning origin discs, two counter-twisting cone sheaths (12 tiers x 20 segments each), 4 helical ribbons, 10 orbiting crescent blades, and 3 traveling shock rings.
- Colors and blending: per-layer flat colors with per-tier alpha falloff along the cone (renderer lines 131, 165): cyan-white inner sheath (0.75, 1.0, 1.0), emerald outer sheath (0.0, 1.0, 0.55), jade and gold origin discs. Normal alpha blending via the shared type.
- Motion: constant fast spin (age x 65 degrees), blades travel forward along the cone and loop (lines 252-293), shock rings travel and fade with progress (lines 297-305). Fade in over 1.5 ticks and fade out over the last 2.5 ticks (lines 80-87).
- Particles: none. The entity emits no particles in its tick.
- Grade: RICH. Five animated mesh layers with real fade in and out; the only limit is the 8-tick life, partly covered by the 6-tick respawn cadence while the spin is held.

### 2. PowerPoleExtensionEntity (Extended Reach pole)
- Entity: `entity/PowerPoleExtensionEntity.java`, maxLifetime 12 ticks (lines 27, 40). Renderer: `client/render/PowerPoleExtensionRenderer.java`. Trigger: Power Pole right-click Extended Reach, `item/PowerPoleItem.java:233`. Follows the owner's eye position (entity lines 95-98).
- Technique: custom mesh in code: 12-sided shaft with per-facet shading variation (renderer lines 107-113, 197-217), gold caps, bevel rings and a dome tip (lines 115-126), two helical gold coils (lines 128-158), 3 sliding Mach cones (lines 160-177), and a pulsing tip starburst (lines 179-184).
- Colors and blending: crimson shaft (0.85, 0.08, 0.08) over a brighter red core, gold fittings (1.0, 0.82, 0.12) and coils. One flat color per part, facet shading on the shaft only.
- Motion: ease-out extension over the first 2 ticks (lines 82-87), coils slowly rotate, Mach cones slide along the shaft, starburst pulses. No fade in (the extension growth covers it); fade out over the last 3 ticks (lines 90-94).
- Particles: none.
- Grade: RICH. A real modeled object with growth animation and a proper retract fade.

### 3. PowerPoleImpactEntity (poke impact burst)
- Entity: `entity/PowerPoleImpactEntity.java`, maxLifetime 14 ticks (lines 26, 40). Renderer: `client/render/PowerPoleImpactRenderer.java`. Trigger: the same right-click poke, spawned only when the poke connects, at the hit point with scale 2.4 (`item/PowerPoleItem.java:241`).
- Technique: custom mesh in code: two expanding shock rings (renderer lines 82-92), a latitude dome with alpha falling by row (lines 94-129), 16 flying diamond shards with seeded random directions (lines 131-156), and an 8-point starburst (lines 158-173).
- Colors and blending: gold outer ring (1.0, 0.85, 0.15), crimson inner ring (0.90, 0.08, 0.12), pale dome, gold or ruby shards. Flat color per part; the dome has a real alpha gradient.
- Motion: rings and dome expand over 8 ticks, shards fly outward and shrink, starburst rotates slowly. One global linear fade across the whole life (line 68); no per-part fades.
- Particles: none.
- Grade: RICH. Layered, expanding, and seeded per impact, held back only by the single global linear fade and the fact that every part sits on the one plane facing the poker.
...[truncated 10674 chars]

---

## Whis Staff

### 4. TimeShatterEntity (Temporal Shatter melee burst)
- Entity: `entity/TimeShatterEntity.java`, lifetime 18 ticks (lines 23-24). Renderer: `client/render/TimeShatterRenderer.java`. Trigger: Whis Staff melee hit, at the target's mid-height (`item/WhisStaffItem.java:144`).
- Technique: custom mesh in code: a 24-segment splintering dial ring (renderer lines 82-83, 131-166), 8 crack spokes with a two-color gradient from center to tip (lines 85-100), 16 tumbling 3D prism shards in three iridescent colors (lines 102-121), and a rotating octahedron core (lines 123-125).
- Colors and blending: diamond white, cyan (0.50, 0.85, 1.0), and lilac shards; spokes gradient from near-white at the center to cyan at the tip. Normal alpha blending.
- Motion: quadratic fade out across the whole life (line 67), the whole burst grows from 0.8x to 1.9x scale (line 68), shards fly outward while tumbling and shrinking, dial segments separate as progress rises. No fade in; the growth covers the first ticks.
- Particles: none.
- Grade: DECENT. The 3D shards and core are good, but the dial and spokes are drawn flat in the entity's local XY plane and the entity syncs no yaw at all, so the flat half of the effect always faces the same world direction no matter where the attacker stood, and nearly vanishes edge-on.

### 5. TemporalRiftEntity (Temporal Rift domain)
- Entity: `entity/TemporalRiftEntity.java`, lifetime 60 ticks (lines 29-30). Renderer: `client/render/TemporalRiftRenderer.java`. Trigger: Whis Staff right-click Temporal Rift, at the caster's feet, radius 12 (`item/WhisStaffItem.java:214`).
- Technique: custom mesh in code, six parts: an 11 x 28 latitude dome (renderer lines 88-126), a ground astrolabe of 5 rotating rings plus 12 hour steles (lines 128-167), two clock hands that sweep backwards (lines 156-167), two tilted orbital rings (lines 169-176), 12 meridian streams (lines 178-183), and two octahedron jewels (lines 165, 185-187).
- Colors and blending: dome has a real color and alpha gradient by latitude, cyan (0.55, 0.85, 1.0) at the base rising to silver (0.95, 0.98, 1.0) at the apex, alpha 0.13 to 0.28 (lines 106-110). Rings and steles in pale cyan and lilac variants.
- Motion: sine ease expansion over the first 6 ticks and collapse over the last 10 (lines 67-73), constant breathing pulse (line 75), every ring rotates at its own rate and direction, clock hands reverse. The most motion-rich renderer in the group.
- Particles: none. The entity plays an ambient AMETHYST_BLOCK_RESONATE hum every 20 ticks (entity lines 72-80), the only self-emitted effect in this weapon.
- Grade: RICH. Gradient dome, entrance and exit animation, and six independently moving parts.


---

## Bansho Fan

### 6. BanshoCycloneEntity (Cyclone Slash proc vortex)
- Entity: `entity/BanshoCycloneEntity.java`, maxLifetime 25 ticks (lines 30, 42). Renderer: `client/render/BanshoCycloneRenderer.java`. Trigger: Bansho Fan melee hit, 30 percent proc, at the target (`item/BanshoFanItem.java:141`). This is also a damage entity: it pulls, lifts, and damages everything inside every 5 ticks (entity lines 64-104).
- Technique: custom mesh in code, six parts: two counter-rotating ground whirlpool rings (renderer lines 72-77), a 10-tier twisting funnel (lines 79-120), 3 ascending helical ribbons (lines 122-163), 8 orbiting razor blades (lines 165-203), a glowing spindle core (lines 205-230), and a flared crown ring (lines 232-236).
- Colors and blending: emerald family throughout (0.15, 1.0, 0.65) funnel, (0.0, 1.0, 0.60) ribbons, near-white core; funnel alpha falls slightly with height (line 97).
- Motion: everything rotates, at different speeds and in both directions; blades orbit while rising. Fade in over 3 ticks and fade out over the last 5 ticks (lines 58-64).
- Particles: none. The entity plays ELYTRA_FLYING and WIND_CHARGE_BURST sounds on tick 1 and every 12 ticks (entity lines 107-112).
- Grade: RICH. Full funnel with ribbons, blades, and proper fades at both ends.

### 7. BanshoShockwaveEntity (launch cone and impact ring, two modes)
- Entity: `entity/BanshoShockwaveEntity.java`, maxLifetime 14 ticks (lines 28, 43). Renderer: `client/render/BanshoShockwaveRenderer.java`. Triggers: cone mode on Tempest Barrage cast (`item/BanshoFanItem.java:192`); radial mode on every fan melee hit (same file, line 151) and on every wind projectile hit (`entity/BanshoWindProjectileEntity.java:169`).
- Technique: custom mesh in code, two full variants selected by the synced IS_CONE flag. Cone: double-walled cone shell, 2 vapor rings, 10 wind needles, muzzle flare ring (renderer lines 80-125). Radial: a beveled 3D ring with a raised crest (lines 133-141), an inner flat ring, 6 rotating crescent blades, and a core disc (lines 143-167).
- Colors and blending: emerald (0.0, 1.0, 0.55) to pale (0.8, 1.0, 0.95). The beveled ring is genuinely vertex-graduated: alpha 0.0 at the outer edge rising to full at the crest, then half alpha at the inner edge (lines 200-224).
- Motion: sine-eased expansion in both modes, crescent blades rotate, one quadratic fade out across the life (lines 66-68). No fade in, but both modes start near zero size so the growth masks it.
- Particles: none.
- Grade: RICH for the cone; the radial mode is RICH on the ground but is drawn as a flat horizontal ring at fixed small heights (y 0.05 to 0.12), so when a wind projectile hits a flying or elevated target the ring hangs in mid-air as a ground effect instead of bursting around the hit.

### 8. BanshoWindProjectileEntity (Tempest Barrage projectile)
- Entity: `entity/BanshoWindProjectileEntity.java`, dies on first block or entity hit, hard cap 50 ticks (lines 148-151). Renderer: `client/render/BanshoWindProjectileRenderer.java`. Trigger: Tempest Barrage right-click, 5 fired per cast (`item/BanshoFanItem.java:214`).
- Technique: custom mesh in code: a 12-segment spinning drill cone with an inner spindle (renderer lines 88-122), 4 orbiting scythe blades (lines 124-166), 3 trailing vapor rings that widen and fade toward the tail (lines 168-178), and a white diamond tip (lines 180-200).
- Colors and blending: emerald drill (0.0, 1.0, 0.60), jade spindle (0.7, 1.0, 0.90), white tip. Every part is one constant alpha (0.85, 0.95, 0.90); only the trailing rings vary, by ring position.
- Motion: drill spins at age x 45 degrees, blades counter-rotate, rings are static relative to the drill. No pulse, no fade in at spawn, no fade out: at the 50-tick cap the drill simply stops existing mid-flight.
- Particles: none. On a hit it spawns a BanshoShockwaveEntity instead (entity lines 169-179), which covers hit despawns but not the timeout.
- Grade: DECENT. A dense, well-built spinning mesh whose ends are unfinished: full alpha from tick 0 and a hard pop out of existence at max range.


---

## Blaster Gun

### 9. BlasterBoltEntity (barrage bolt)
- Entity: `entity/BlasterBoltEntity.java`, dies on block hit, cap 30 ticks (lines 168-170). Renderer: `client/render/BlasterBoltRenderer.java`. Trigger: Blaster Gun left-click barrage, one bolt per shot (`item/BlasterGunItem.java:158`).
- Technique: custom mesh in code: an 8-sided plasma bolt with nose cone, body, and tapered ion tail (renderer lines 158-185), a counter-rotating white core bolt (line 111), 4 swept stabilizer fins (lines 113-128), 2 rotating compression rings (lines 130-132), and a cross flare at the tip (lines 134-141).
- Colors and blending: the palette shifts with the synced heat ratio and overcharge flag (lines 89-103): yellow-white at low heat, orange above 0.6 heat, magenta-red (1.0, 0.12, 0.32) when overcharged, with a white-hot core. The ion tail is a true alpha gradient, full at the body fading to 0.0 at the tail point (lines 166-176).
- Motion: radius pulses (line 80), sheath rotates at age x 20 degrees against the core at age x -30, fins and rings rotate. No spawn or lifetime fade; at 2.4 blocks per tick the flight is short and the block-hit particle burst covers most despawns.
- Particles: yes, the busiest emitter in the group. On block hit: 8 DBA SPARK particles plus a fizzle sound (entity lines 107-113). On an overcharged entity hit: 1 vanilla EXPLOSION particle plus an explode sound (entity line 162).
- Grade: RICH. Heat-reactive color, counter-rotating layers, gradient tail; only the ends are abrupt.

### 10. ErasureChargeOrbEntity (Erasure Cannon charge orb)
- Entity: `entity/ErasureChargeOrbEntity.java`, no fixed lifetime: it lives while the player charges and self-discards via a 6-tick watchdog when updates stop (lines 92-109). Renderer: `client/render/ErasureChargeOrbRenderer.java`. Trigger: Erasure Cannon right-click charge, spawned and tracked by `item/BlasterGunItem.java:221`, glued to the muzzle 1.2 blocks ahead of the eyes (entity lines 78-85).
- Technique: custom mesh in code: a rotating octahedron core (renderer line 104), two counter-rotating geodesic lat-long shells (lines 106-110), 3 accelerator rings at different speeds and directions (lines 112-121), 8 intake spikes that spiral inward faster as charge rises (lines 123-152), and white corona arcs that only appear at charge 0.75 and up (lines 154-170).
- Colors and blending: white core, cyan shells (0.0, 0.95, 1.0) and (0.10, 0.70, 1.0), each shell a single flat color with a single alpha. Radius grows from 0.22 to about 1.17 and alpha from 0.75 to 1.0 with the charge ratio (lines 91-94), plus a slow pulse.
- Motion: constant multi-speed rotation everywhere, spikes converge, pulse breathes. No fade in: at charge 0 the orb is already at alpha 0.75 the first tick it exists. No fade out either, but release replaces it with the beam on the same tick.
- Particles: none.
- Grade: DECENT. Lots of moving geometry, but the shells are flat single-color grids and the orb pops into existence at three-quarter opacity instead of materializing with the charge.

### 11. ErasureCannonBeamEntity (Erasure Cannon beam)
- Entity: `entity/ErasureCannonBeamEntity.java`, MAX_LIFETIME 16 ticks (line 36). Renderer: `client/render/ErasureCannonBeamRenderer.java`. Trigger: Erasure Cannon release (`item/BlasterGunItem.java:284`), length set by the block raycast, up to 48 blocks. All damage lands on tick 1 (entity lines 107-172).
- Technique: custom mesh in code: a 3-layer prism beam (white 12-sided core, cyan 12-sided shroud, hexagonal outer sheath, renderer lines 130-141), 4 counter-rotating helical ribbons capped at 128 segments (lines 143-176), pulsing focusing rings spaced along the length (lines 178-187), a muzzle emitter array (line 189), and a terminus dome with 6 splash fins (line 191).
- Colors and blending: white core at alpha 0.98, cyan shroud 0.75, sheath 0.35, alternating white and cyan ribbons. One flat alpha per layer, uniform along the full length: the tube does not taper or dim toward the terminus.
- Motion: 2-tick scale-in, slow radius pulse (line 107), ribbons and rings rotate continuously, and alpha dissipates on a power curve over the 16 ticks (line 105). No per-length animation beyond the ribbon helix.
- Particles: yes. At charge 0.9 and up, 3 vanilla EXPLOSION_EMITTER particles at the endpoint on tick 1 (entity lines 175-182).
- Grade: RICH. The group's biggest effect is properly layered and animated; the uniform, untapered tube over 48 blocks is its one flat trait.


---

## Ranked: the 5 worst-looking entities in this group

This group has no FLAT or THIN entities; all 11 are multi-part custom meshes. The five below are the weakest relative to the other six, worst first.

1. **TimeShatterEntity** (Whis Staff melee burst). Half of the effect, the splintering dial and the crack spokes, is drawn flat in the local XY plane, and the entity syncs no yaw, so those parts face one fixed world direction regardless of the swing and nearly disappear when viewed edge-on.
   Most impactful fix: billboard the dial and spoke layer toward the camera in `TimeShatterRenderer.submit` (or at minimum rotate them to the attacker's view yaw), so the shatter reads from every angle.
2. **BanshoWindProjectileEntity** (Tempest Barrage projectile). Constant alphas everywhere and a hard despawn: at the 50-tick range cap the drill vanishes mid-flight with no visual at all.
   Most impactful fix: add an age-based alpha ramp in `BanshoWindProjectileRenderer.submit`, in over the first 2 ticks and out over the last 5, with a small radial shockwave or wind puff spawned on the timeout path in the entity tick to match the hit path.
3. **ErasureChargeOrbEntity** (Erasure Cannon charge orb). The orb pops into existence at alpha 0.75 on the first charge tick, and both geodesic shells are single flat colors.
   Most impactful fix: drive the base alpha from the charge ratio (near 0 at charge 0 rising to full) in `ErasureChargeOrbRenderer.submit` lines 91-94, so the orb materializes as it grows instead of appearing.
4. **BanshoShockwaveEntity, radial mode** (fan melee and projectile impact ring). The radial variant is a ground ring drawn at fixed heights near y 0.05, so projectile hits on flying or elevated targets get a flat ring floating horizontally in mid-air at the hit point.
   Most impactful fix: when the shockwave is spawned by a projectile hit, orient the radial burst to the projectile's yaw and pitch (the cone-mode rotation path already exists in `BanshoShockwaveRenderer.submit`) instead of always lying flat.
5. **PowerPoleImpactEntity** (poke impact burst). Everything is built on the single plane facing the poker and fades on one global linear curve, so from behind or from the side the impact reads much thinner than from the front.
   Most impactful fix: draw the rings and starburst a second time on a crossed plane (rotated 90 degrees around the look axis) in `PowerPoleImpactRenderer.submit`, so the burst has volume from any viewpoint.
