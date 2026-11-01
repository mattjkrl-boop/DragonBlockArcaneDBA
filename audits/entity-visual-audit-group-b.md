# Entity Visual Audit Group B (Nov 1, 2026)

Audit only, no code changed. Scope: every spawned ability entity and its client renderer for Azure Dragon Sword, Dabura Sword, Curse Blade, Hollow's Edge, and Dimensional Sword (24 entities). Line numbers are from the current main branch files.

Shared pipeline facts that apply to all 24:

- Every renderer in this group draws through `KiRenderHelper.kiRenderType()` (`client/render/ki/KiRenderHelper.java:28-29`), which is `RenderTypes.entityTranslucentEmissive(textures/entity/ki_white.png)`: normal translucent emissive blending, NOT additive. The additive helper `kiAdditiveType()` (KiRenderHelper.java:23-24) is used by none of these 24 renderers. Almost every entity is untextured geometry on the plain white ki_white.png canvas (KiRenderHelper.java:20), colored only by vertex colors at full bright.
- Every entity is a custom mesh built in code. None use GeckoLib models, none use camera facing billboards, none render an item model. There are no FLAT and no THIN grades in this group: the weakest entities are DECENT.
- Self emitted particles are nearly absent: only AzureLightningEntity emits its own particles (one SPARK burst on tick 1). VoidRiftEntity gets particles indirectly through DbaImpactFx.heavy at implosion. The other 22 emit none; all richness is renderer geometry.
- The recurring weakness is the fade envelope. Long lived effects (AzureStormEntity 11 s, AzureTornadoEntity up to 10 s, SkyCracksEntity 5 s, DimensionalSlashEntity up to 5 s) mostly have no fade in or fade out at all and pop at both ends, and two shatter entities share a fade formula that mathematically cannot reach zero.

---

## 1. Azure Dragon Sword (item/AzureDragonSwordItem.java)

### AzureRushTrailEntity / AzureRushTrailRenderer
- Lifetime: 12 ticks (0.6 s). `maxLifetime = 12` at AzureRushTrailEntity.java:20, discard at :73; renderer mirrors it at AzureRushTrailRenderer.java:74.
- Trigger: Azure Dragon Rush (left click hold), spawned every tick while rushing by `onDragonRushTick` at AzureDragonSwordItem.java:177, scale 1.5 on double rush.
- Technique: custom mesh in `submitCustomGeometry` (AzureRushTrailRenderer.java:91): 16 segment Mach cone, twin helical ribbons (2 strands x 10 steps), cross quad thrust core beam (:155), 16 segment wake ring (:160).
- Colors and blending: flat base color per part, normal (0.0, 0.85, 1.0) or double rush (0.0, 1.0, 0.55) at AzureRushTrailRenderer.java:81-83, ribbons fade alpha along their length and shift red, core beam near white (0.95, 1.0, 1.0). Normal translucent emissive, ki_white.png only.
- Motion: fades out and expands, no fade in. Alpha `(1.0 - progress) * 0.75` (0.90 double) at AzureRushTrailRenderer.java:78, scale grows `1.0 + progress * 0.5` at :79, ribbons twist with age (:131-132).
- Particles: none.
- Grade: RICH, four distinct mesh parts with expansion, twist, and a clean fade out inside 0.6 seconds.

### AzureTornadoEntity / AzureTornadoRenderer
- Lifetime: 60 ticks (3 s) mini, 200 ticks (10 s) giant. AzureTornadoEntity.java:27 and :41, discard at :124.
- Trigger: mini from rush pass through hits (AzureDragonSwordItem.java:205); giant spawned once by AzureStormEntity at max charge (AzureStormEntity.java:114-116).
- Technique: custom mesh (AzureTornadoRenderer.java:66): 2 ground suction rings, inner core funnel 8 levels x 16 segments, 3 outer helical ribbons, 6 or 12 orbiting wind shard triangles, crown outflow ring.
- Colors and blending: flat single color per layer in cyan shades, rings (0.0, 0.9, 1.0) and (0.2, 1.0, 0.9) at AzureTornadoRenderer.java:72-73, funnel (0.7, 1.0, 1.0) at :108, ribbons (0.0, 0.90, 1.0) at :150. Only variation is core alpha easing. Normal translucent emissive, ki_white.png only.
- Motion: rotates everywhere (rings at age * 35 and age * -45, core at age * 28, shards at age * (25 + i * 4)), but submit() has no lifetime alpha term at all: no fade in, no fade out, no pulse. Pops in at full alpha and pops out at discard.
- Particles: none. DustParticleOptions and ParticleTypes are imported at AzureTornadoEntity.java:3-4 and never used.
- Grade: DECENT, geometry and rotation are rich, but a 3 to 10 second effect with no fade at either end pops visibly.

### AzureSonicQuakeEntity / AzureSonicQuakeRenderer
- Lifetime: 18 ticks (0.9 s). AzureSonicQuakeEntity.java:18, discard at :51.
- Trigger: Dragon Rush ground or wall impact, `triggerSonicQuake` at AzureDragonSwordItem.java:238, spawn at :264 with radius 8.0.
- Technique: custom mesh (AzureSonicQuakeRenderer.java:64): 32 segment beveled shock ring with raised crest, secondary compression ring, 18 vertical pressure shard triangles, flat epicenter core disc.
- Colors and blending: a true per vertex gradient on the ring (base (0.0, 0.90, 1.0) to top (0.85, 1.0, 1.0), outer base vertices at alpha 0) at AzureSonicQuakeRenderer.java:73-77; other parts flat. Normal translucent emissive, ki_white.png only.
- Motion: expands and fades out, no rotation, no fade in. Sine eased expansion at AzureSonicQuakeRenderer.java:58-59, alpha `(1.0 - progress * progress) * 0.90` at :60, crest and shards shrink with (1.0 - progress).
- Particles: none from the entity (the item separately calls DbaImpactFx.heavy at AzureDragonSwordItem.java:272).
- Grade: RICH, short but complete: eased expansion, beveled gradient ring, shards and core all fading out together.

### AzureTempestChannelEntity / AzureTempestChannelRenderer
- Lifetime: no fixed lifetime. Follows the caster and discards when the caster stops holding the sword or stops using the item (AzureTempestChannelEntity.java:63-76), also discarded explicitly on release (AzureDragonSwordItem.java:344-345). Charge caps at 300 ticks (AzureDragonSwordItem.java:45).
- Trigger: Call of the Tempest right click channel, created by `onUseTick` at AzureDragonSwordItem.java:314, charge ratio updated every tick.
- Technique: custom mesh (AzureTempestChannelRenderer.java:72): 3 counter rotating ground rings, faceted wind tunnel column 8 levels x 20 segments, 4 to 8 orbiting wind blade triangles, charge gated random lightning arc beams.
- Colors and blending: flat color per part, rings (0.0, 0.85 + band * 0.07, 1.0) at AzureTempestChannelRenderer.java:83-84, tunnel (0.0, 0.90, 1.0) with per level alpha, blades (0.2, 1.0, 1.0), arcs cyan with a white core (:174-175). Normal translucent emissive, ki_white.png only.
- Motion: rotates, orbits, and bobs; growth is charge driven, not age driven (radius, height, and base alpha at AzureTempestChannelRenderer.java:68-70); arcs appear at charge 0.25 or more and reseed every tick so they flicker (:156-157). No fade out: it vanishes instantly on release.
- Particles: none.
- Grade: DECENT, four rotating subsystems that visibly intensify with charge, but it pops in at partial alpha and vanishes the tick the channel ends.

### AzureStormEntity / AzureStormRenderer
- Lifetime: 220 ticks (11 s). AzureStormEntity.java:30, discard at :161.
- Trigger: Call of the Tempest release (AzureDragonSwordItem.java:367), radius 10 to 30 blocks by charge, giant tornado and lightning at max charge.
- Technique: custom mesh, the largest in this group (AzureStormRenderer.java:69): 4 counter rotating ground rings, outer cyclone dome 10 x 24, inner counter rotating dome 6 x 18, 14 animated rain shaft beams, central eye updraft column, 4 branching lightning arc chains.
- Colors and blending: outer dome is a real latitude gradient (green 0.22 + lat * 0.55, blue 0.55 + lat * 0.45, alpha 0.38 + lat * 0.30) at AzureStormRenderer.java:103-106; ground bands and inner dome are flat per part. Normal translucent emissive, ki_white.png only.
- Motion: rotates at several speeds, rain streaks fall (AzureStormRenderer.java:178), arcs reseed about every 3 ticks. submit() contains no progress or lifetime term at all: no fade in, no fade out across an 11 second life.
- Particles: none from the entity. DustParticleOptions and ParticleTypes imported at AzureStormEntity.java:3-4, never used; the tick spawns AzureLightningEntity and the giant AzureTornadoEntity instead.
- Grade: DECENT, the biggest and most layered mesh in the group with a real color gradient, capped by total absence of fades over an 11 second life.

### AzureLightningEntity / AzureLightningRenderer
- Lifetime: 12 ticks (0.6 s). AzureLightningEntity.java:22, discard at :82-83.
- Trigger: parent entities only: AzureStormEntity every 25 ticks (AzureStormEntity.java:141, damage 450, 20 percent rare) and giant AzureTornadoEntity every 20 ticks (AzureTornadoEntity.java:109, damage 350).
- Technique: custom mesh plus a real texture, the only textured bolt in the group. Pass 1 draws cross quad segments (two orthogonal quads per segment): 22 segment jagged trunk over 44 blocks (AzureLightningRenderer.java:85-100) plus 5 to 8 forks (:113). Pass 2 draws 2 ground impact rings (:140-152).
- Colors and blending: bolt vertices are flat white (1.0, 1.0, 1.0) at AzureLightningRenderer.java:82, all color comes from the texture: textures/entity/azure_lightning.png, or azure_lightning_rare.png when rare (:20-21). Rings are cyan or rare teal (:143-145). Translucent emissive on both passes.
- Motion: static seeded bolt shape, flickering fade out (alpha 1.0 to tick 3, then linear fade with a 0.65 multiplier on alternating steps, AzureLightningRenderer.java:63-70), ground ring expands to 4.2 blocks while fading (:147-148).
- Particles: yes, the only self emitter in the group: on tick 1 it sends 20 DbaParticles.SPARK at the impact point (AzureLightningEntity.java:56, :73-77).
- Grade: RICH, seeded jagged trunk with forks, a real texture, a burst then flicker then fade curve, and an expanding impact ring.

---

## 2. Dabura Sword (item/DaburaSwordItem.java)

### DarknessChargeEntity / DarknessChargeRenderer
- Lifetime: no fixed lifetime; persists while the charge refreshes it. Discards after 6 ticks without an update (DarknessChargeEntity.java:85-88), on release (DaburaSwordItem.java:136-139), or when the caster stops holding the sword (:76). Charge caps at 160 ticks (DaburaSwordItem.java:46).
- Trigger: Abyssal Slash left click charge, `onLeftClickChargeTick` at DaburaSwordItem.java:119.
- Technique: custom mesh (DarknessChargeRenderer.java:63): 3 rotating ground rings, 3D shards, nested octahedrons, helical ribbon quads.
- Colors and blending: layered flat section colors, rings (0.12, 0.0, 0.25), (0.55, 0.05, 0.85), (0.95, 0.05, 0.35) at DarknessChargeRenderer.java:69-76; shards have a per vertex gradient from (0.08, 0.0, 0.18) to (0.65, 0.05, 0.95) at :115-116; a third octahedron color appears at charge 0.4 or more (:122-125). Normal translucent emissive, ki_white.png only.
- Motion: rotates (rings at age * -14, 22, -32; octahedrons up to age * 50), pulses (`0.90 + 0.10 * sin(age * 0.5)` at DarknessChargeRenderer.java:61), and scales with charge (radius and alpha at :59-60, shard count grows with charge). No fade in or out; pops at both ends.
- Particles: none.
- Grade: RICH, multi layer construct whose size, shard count, and brightness all track the charge.

### DarknessWaveEntity / DarknessWaveRenderer
- Lifetime: 40 ticks (2 s), discard at DarknessWaveEntity.java:149-150.
- Trigger: Abyssal Slash release (DaburaSwordItem.java:152); two extra slower waves at full charge (:163).
- Technique: custom mesh (DarknessWaveRenderer.java:79): a volumetric crescent built by draw3DVolumetricCrescent in 4 nested layers (:83-92), plus 6 mini shard crescents.
- Colors and blending: flat color per layer, outer (0.08, 0.0, 0.16), violet core (0.58, 0.05, 0.90), crimson edge (0.95, 0.08, 0.35), white purple spine (1.0, 0.85, 1.0) at DarknessWaveRenderer.java:83-92, with a per vertex alpha gradient to 0 at the trailing ridge (:138-143). Normal translucent emissive, ki_white.png only.
- Motion: travels physically (server side delta movement), but the renderer is nearly static: only a first person scale in over the first 5 ticks (DarknessWaveRenderer.java:72) and trailing shard bob (:101). No rotation, no pulse, constant alpha until the 40 tick discard, so it pops out mid flight.
- Particles: none. DustParticleOptions and ParticleTypes imported at DarknessWaveEntity.java:3-4, never used.
- Grade: DECENT, a genuinely volumetric 4 layer crescent with a trailing gradient, held back by almost no age animation and no fade out.

### DarknessDomainEntity / DarknessDomainRenderer
- Lifetime: 10 ticks unrefreshed (DarknessDomainEntity.java:23), refreshed every channel tick by the item, so it lives for the whole World of Darkness channel (cap 300 ticks, DaburaSwordItem.java:47) plus a short tail.
- Trigger: World of Darkness right click channel (DaburaSwordItem.java:228), radius 8 to 30 blocks.
- Technique: custom mesh (DarknessDomainRenderer.java:69): 4 counter rotating ground rings, double layered lat/long dome, 16 orbiting spire pyramids, central vortex column, lightning beams.
- Colors and blending: gradient dome (red 0.10 + lat * 0.50, blue 0.25 + lat * 0.70, alpha 0.35 + lat * 0.35) at DarknessDomainRenderer.java:103-106; alternating ground bands, gradient spires, flat vortex (0.65, 0.05, 0.95, alpha 0.45). Normal translucent emissive, ki_white.png only.
- Motion: rotates extensively (bands, both domes at different rates, orbiting and bobbing spires, vortex at age * (22 + level * 4)); lightning reseeds every 3 ticks and flickers (DarknessDomainRenderer.java:224). No fade in or out.
- Particles: none.
- Grade: RICH, the largest custom mesh of the Dabura set: gradient double dome, rotating bands, spires, vortex, and flickering lightning.

### DarknessBladeEntity / DarknessBladeRenderer
- Lifetime: until impact or 40 ticks max (DarknessBladeEntity.java:53, detonation at :75). Falls at -1.8 per tick from 18 to 23 blocks up (:37), so impact normally comes first.
- Trigger: Abyssal Eclipse finisher only, 10 blades from `triggerAbyssalEclipse` at DaburaSwordItem.java:295. The partial eclipse spawns none.
- Technique: custom mesh, a procedural greatsword: draw3DGreatsword called 3 times for layered blades (DarknessBladeRenderer.java:72-78), crossguard (:81-82), hilt (:85), and plunge wake cones. Fixed downward orientation (Axis.XP 90 degrees plus yRot, :61).
- Colors and blending: flat single color per layer, aura (0.65, 0.05, 0.95, alpha 0.80), obsidian core (0.06, 0.0, 0.12, alpha 0.98), crimson fuller (1.0, 0.05, 0.25), crossguard and hilt dark violet (DarknessBladeRenderer.java:72-85); only the wake cones gradient to alpha 0 (:190-191). Normal translucent emissive, ki_white.png only.
- Motion: physical falling only. The renderer uses no age term at all: blade length 7.5 and width 1.0 are constants (DarknessBladeRenderer.java:64-65), no rotation, pulse, scale, or fade in submit. Pops in at spawn height, pops out on detonation.
- Particles: none. DustParticleOptions and ParticleTypes imported at DarknessBladeEntity.java:3-4, never used. On impact it spawns a DarknessShatterEntity instead (DarknessBladeEntity.java:68-71).
- Grade: DECENT, a detailed multi part greatsword mesh, but completely frozen in the renderer with no fade or age animation on the finisher's centerpiece.

### DarknessShatterEntity / DarknessShatterRenderer
- Lifetime: 40 ticks (2 s). DarknessShatterEntity.java:20, decrement and discard at :57-59.
- Trigger: Abyssal Eclipse finisher (DaburaSwordItem.java:300), partial eclipse (DaburaSwordItem.java:333), and every Darkness blade impact (DarknessBladeEntity.java:68-71, radius 2.8).
- Technique: custom mesh (DarknessShatterRenderer.java:68): 3 ground rings, 8 radiating fissure trenches, 12 erupting rock slabs, central eruption column, all placed with a per entity seeded Random (:70).
- Colors and blending: flat color per element multiplied by fade, rings (0.55, 0.05, 0.90) and (0.95, 0.08, 0.35) at DarknessShatterRenderer.java:76-78, slabs gradient from (0.08, 0.0, 0.16) to (0.75, 0.05, 0.95) at :128-129. Normal translucent emissive, ki_white.png only.
- Motion: expands (shock radius min(radius * 1.35, age * 0.55) at DarknessShatterRenderer.java:73), trenches, slabs, and column grow with age, column rotates. The fade is broken: `fade = 1.0 - age / (lifetime + age)` at :65 is about 0.5 at age 40, so half the alpha remains at discard and it still pops out. No fade in.
- Particles: none.
- Grade: RICH geometry (expanding rings, seeded fissures, erupting slabs, rotating column), held back by the fade never reaching zero.

---

## 3. Curse Blade (item/CurseBladeItem.java)

### CurseChainEntity / CurseChainRenderer
- Lifetime: attached phase 100 ticks (5 s), attachTimer set at CurseChainEntity.java:186, discard at :244-245; in flight it ends only on owner or target death or beyond 64 blocks.
- Trigger: left click chain streaming, `streamCurseChain` spawn at CurseBladeItem.java:141-143.
- Technique: custom mesh (CurseChainRenderer.java:70 onward): 4 spiked chain links, a shackle collar, 4 orbiting runic glyphs, helical aura ribbons, an octahedron core.
- Colors and blending: flat per part colors with pulse modulation, base metal (0.12, 0.08, 0.16, alpha 0.95), rune faces (0.65 * pulse, 0.05, 0.85 * pulse) at CurseChainRenderer.java:118-119, spikes gradient to a white pink tip (1.0, 0.9, 1.0). Normal translucent emissive, ki_white.png only.
- Motion: rotates (Y at age * 12 attached or age * 24 in flight, CurseChainRenderer.java:66-68), pulses (`0.85 + 0.15 * sin(age * 0.35 + orbit)` at :73), and the entity itself orbits the target's torso. No fade in or out.
- Particles: none.
- Grade: RICH, multi part animated 3D mesh with rotation, pulse, orbiting glyphs, and ribbons; only the fades are missing.

### AbyssalDomainEntity / AbyssalDomainRenderer
- Lifetime: 10 ticks nominal (AbyssalDomainEntity.java:23), refreshed every channel tick (CurseBladeItem.java:199-200), so it lives for the whole Cursed Tempest channel plus 10 ticks.
- Trigger: Cursed Tempest right click channel (CurseBladeItem.java:195), radius 10 to 30 blocks.
- Technique: custom mesh (AbyssalDomainRenderer.java:67): 4 ground rings, double layered lat/long dome, 16 orbiting spire pyramids, 8 level vortex column, 4 random lightning arcs.
- Colors and blending: latitude gradient on the outer dome (red 0.15 + lat * 0.65, blue 0.35 + lat * 0.55, alpha 0.35 + lat * 0.35), alternating ground bands (0.95, 0.02, 0.20) and (0.45, 0.02, 0.95), gradient spires. Normal translucent emissive, ki_white.png only.
- Motion: rotates everywhere (rings, dome, counter rotating inner dome, orbiting and bobbing spires, vortex), arcs re-randomize every 3 ticks. Radius growth is driven by the item. No fade in or out; pops out 10 ticks after the channel ends.
- Particles: none.
- Grade: RICH, a full volumetric environment with 5 layered animated systems; only fades are missing.

### CurseTelegraphEntity / CurseTelegraphRenderer
- Lifetime: 20 ticks (1 s), duration passed at CurseTelegraphEntity.java:26 and stored at :32-33; the item passes 20 (CurseBladeItem.java:253-255).
- Trigger: Cursed Tempest channel, spawned every 40 ticks at CurseBladeItem.java:253-255, exactly 20 ticks before the targeted lightning strike.
- Technique: custom mesh (CurseTelegraphRenderer.java:61): 2 counter rotating ground rings, 1 contracting ring, 6 rising beam pillars, central octahedron core.
- Colors and blending: flat per element, outer ring (0.95, 0.02, 0.15) times pulse at CurseTelegraphRenderer.java:72, inner ring (0.65, 0.01, 0.85) at :73, contracting ring near white (1.0, 0.9, 0.95) at :78, pillars red with a white inner beam (:89-90). Normal translucent emissive, ki_white.png only.
- Motion: rotates, contracts, rises, and pulses faster as it completes: pulse speed 0.3 + progress * 0.7 (CurseTelegraphRenderer.java:67-68), contracting ring radius is radius * (1.0 - progress) (:76-78), pillar height 1.2 + progress * 2.5, core grows with progress (:94). No fade in or out.
- Particles: none.
- Grade: DECENT, a clear readable contracting timer with rising pillars and an accelerating pulse, built only from flat rings and beams.

### CurseLightningEntity / CurseLightningRenderer
- Lifetime: 20 ticks (1 s), CurseLightningEntity.java:18; the renderer stops drawing after age 18 (CurseLightningRenderer.java:69-70).
- Trigger: Cursed Tempest channel: targeted strikes (CurseBladeItem.java:261 onward) and ambient storm strikes (:299 onward), 15 and 20 percent rare respectively.
- Technique: custom mesh in two passes: a 24 segment jagged trunk 46 blocks high with 6 to 9 branching forks and tertiary mini branches (textured pass), plus ground crawling arcs and 2 expanding ground rings (glow pass).
- Colors and blending: textured pass vertices are white, so color comes from the texture. The rare flag swaps both texture and glow color at CurseLightningRenderer.java:82-88: normal uses textures/entity/curse_lightning_red.png with a red glow (1.0, 0.02, 0.15), rare uses curse_lightning.png with a violet glow (0.85, 0.05, 1.0). Note the file naming reads inverted (the red named file is the normal variant). Translucent emissive on both passes.
- Motion: static seeded geometry per entity (the bolt shape does not crawl), with flash, flicker, and fade out (CurseLightningRenderer.java:73-78): alpha 1.0 to age 3, then a 15 tick fade multiplied by 0.65 on alternating steps; ground ring expands and fades. No fade in.
- Particles: none from the entity (the item separately calls DbaImpactFx.medium at the targeted strike site).
- Grade: RICH, fractal branching, textured plus glow passes, ground crawlers, and a proper flickering fade out.

### SkyCracksEntity / SkyCracksRenderer
- Lifetime: 100 ticks (5 s). SkyCracksEntity.java:10.
- Trigger: Cursed Tempest channel, spawned every 80 ticks at y 280 above the player (CurseBladeItem.java:326-331). Shared entity class, used here by the Curse storm.
- Technique: hybrid. One giant flat horizontal textured quad, 960 x 960 blocks, facing down (SkyCracksRenderer.java:67-75), plus custom mesh overlays: 12 procedural fractal rays and 16 octahedron shards (:78 onward).
- Colors and blending: the quad is white vertices at alpha 240 * pulse (SkyCracksRenderer.java:71-74), so its look is entirely the texture textures/environment/sky_cracks.png (:27). Rays (0.95, 0.05, 0.25) with a near white core (:97-99), shards gradient violet to pink. Translucent emissive on both passes.
- Motion: pulses (`0.85 + 0.15 * sin(age * 0.18)` at SkyCracksRenderer.java:64), shards orbit, bob, and spin (:108-115), rays are static per seed. No fade in or fade out across the 5 second life: it pops in and out at near full alpha.
- Particles: none.
- Grade: DECENT, procedural rays and tumbling shards lift it, but the dominant element is still one flat textured quad with no fades.

### CurseGroundShatterEntity / CurseGroundShatterRenderer
- Lifetime: 35 ticks (1.75 s). CurseGroundShatterEntity.java:19, discard at :56-58.
- Trigger: Cursed Tempest at max charge only, per heavily cursed enemy (CurseBladeItem.java:357-360).
- Technique: custom mesh (CurseGroundShatterRenderer.java:65): 2 expanding ground rings, 8 radiating fissure trench strips, 12 erupting rock slab pyramids.
- Colors and blending: flat per segment scaled by fade, rings (0.95, 0.05, 0.25) and (0.70, 0.02, 0.95) at CurseGroundShatterRenderer.java:74-75, trenches with a near white inner (1.0, 0.85, 0.95) at :92-94, slabs gradient to (0.95, 0.05, 0.35) at :123-124. Normal translucent emissive, ki_white.png only.
- Motion: expands and erupts in (shock radius, trench width, and slab height all grow with age at CurseGroundShatterRenderer.java:72, :90, :109), no rotation. The fade out is broken the same way as DarknessShatterRenderer: `1.0 - age / (lifetime + age)` at :64, and here lifetime is read client side through getLifetime() (CurseGroundShatterEntity.java:47-49) where it never decrements, so at age 35 the fade is only about 0.5 and the effect pops out at half alpha.
- Particles: none.
- Grade: DECENT, real 3D slabs, fissures, and expansion, undermined by the faulty fade formula.

---

## 4. Hollow's Edge (item/HollowsEdgeItem.java)

### HollowChargeEntity / HollowChargeRenderer
- Lifetime: no fixed lifetime; follows the caster, discards when the caster stops holding the weapon (HollowChargeEntity.java:68-82) or after 6 ticks without a charge update (:84-88). Charge caps at 100 ticks (HollowsEdgeItem.java:50).
- Trigger: Hollow Rush left click charge hold (HollowsEdgeItem.java:124), charge ratio updated at :128.
- Technique: custom mesh (HollowChargeRenderer.java:62): 3 flat ground rings, 3D pyramid shards, nested octahedrons, helical ribbon quads.
- Colors and blending: layered flat colors, rings (0.35, 0.02, 0.65), (0.65, 0.05, 0.95), (0.15, 0.90, 1.0) at HollowChargeRenderer.java:68-75; shards are a per vertex gradient from (0.20, 0.0, 0.40) to (0.15, 0.95, 1.0) at :114-115; a white octahedron appears at charge 0.5 or more (:124). Normal translucent emissive, ki_white.png only.
- Motion: counter rotating rings (age * -14, 24, -36), shards orbit faster as charge rises, octahedrons rotate, pulse `0.88 + 0.12 * sin(age * 0.45)` at HollowChargeRenderer.java:60; radius and alpha scale with charge only (:58-59). Pops in at alpha 0.60 minimum, pops out on discard.
- Particles: none.
- Grade: RICH, layered rotating charge construct whose size, shard count, and brightness track the charge; only the fades are missing.

### HollowAfterimageEntity / HollowAfterimageRenderer
- Lifetime: 100 ticks (5 s), discard at HollowAfterimageEntity.java:106. Mismatch: the renderer stops drawing at 80 ticks (HollowAfterimageRenderer.java:70-72), so the last 20 ticks are invisible while the entity still applies its Dark Faded aura (HollowAfterimageEntity.java:96-103).
- Trigger: Hollow Rush dash, `performBlink` at HollowsEdgeItem.java:242, left at the dash start position. (Also spawned by Katana and Saber dashes, outside this group.)
- Technique: custom mesh: a humanoid ghost assembled from axis aligned boxes via KiRenderHelper.drawColoredBox (HollowAfterimageRenderer.java:142-175: torso, head, arms, legs), plus wake ribbons, a ripple ring, and diamond motes (:178-220). Not a real player model render. The synced skin and hair colors (HollowAfterimageEntity.java:41-47) are extracted into the render state (:62-63) but never used in submit; the body is fixed colors.
- Colors and blending: two body passes, expanded halo (0.10, 0.85, 1.0) at alpha * 0.35 (HollowAfterimageRenderer.java:86) and main body (0.15, 0.05, 0.35) at alpha * 0.85 (:89), eye slit (0.85, 0.98, 1.0), scanline boxes (0.0, 0.95, 1.0), motes cyan; ribbons and ring edges gradient to alpha 0 (:179-182, :202-203). Normal translucent emissive, ki_white.png only.
- Motion: fades out quadratically over the renderer's 80 tick life (`0.90 * (1 - progress * progress)`, HollowAfterimageRenderer.java:70-74), pulses, scanlines oscillate, the footstep ring expands, and 16 motes rise, shrink, and fade on a loop (:123-130). No fade in.
- Particles: none from the entity (the motes and ring are renderer geometry).
- Grade: RICH, a full box built ghost with halo pass, scanlines, wake ribbons, ripple ring, and disintegration motes under a smooth fade out; docked only for the invisible final second of entity life.

### HollowRushTrailEntity / HollowRushTrailRenderer
- Lifetime: 14 ticks (0.7 s). HollowRushTrailEntity.java:20, discard at :69.
- Trigger: Hollow Rush dash, `performBlink` at HollowsEdgeItem.java:252, spanning dash start to destination.
- Technique: custom mesh: an 8 sided open tunnel corridor with an inner core tunnel (HollowRushTrailRenderer.java:97-128), twin spiral ribbon strands, flat rings at both ends, and a 4 plane white core spike.
- Colors and blending: flat per part colors, outer corridor (0.25, 0.02, 0.55), inner core (0.15, 0.90, 1.0), streamlines (0.75, 0.10, 1.0), origin ring violet and destination ring cyan (HollowRushTrailRenderer.java:107-174); third dash is wider and brighter (:78, :81). No per vertex color gradient. Normal translucent emissive, ki_white.png only.
- Motion: fades out linearly (`(1.0 - progress) * (0.95 or 0.80)`, HollowRushTrailRenderer.java:78), end rings expand, streamlines rotate with age. No fade in, no pulse.
- Particles: none.
- Grade: RICH, a genuine volumetric corridor with layered core, spirals, end rings, and spike, inside a 0.7 second life.

### VoidSlashEntity / VoidSlashRenderer
- Lifetime: 41 ticks (about 2 s), discard at VoidSlashEntity.java:86-88; renderer maxLife 40 (VoidSlashRenderer.java:60).
- Trigger: every 3rd Hollow Rush dash, spawned in `performBlink` at HollowsEdgeItem.java:278 with velocity look * 1.8 (:279).
- Technique: custom mesh: four nested volumetric crescents with a trailing wake (VoidSlashRenderer.java:105-139, called at :82-95) plus flat tip spur quads, whole mesh tilted plus or minus 42 degrees (:68-72).
- Colors and blending: layered flat colors, outer aura (0.08, 0.0, 0.16), violet blade (0.60, 0.05, 0.95), cyan core (0.15, 0.92, 1.0), white edge (1.0, 1.0, 1.0) at VoidSlashRenderer.java:82-95, with a per vertex alpha gradient to 0 at the trailing edge (:124-129) and gradient tip spurs (:143-152). Normal translucent emissive, ki_white.png only.
- Motion: flies by delta movement (VoidSlashEntity.java:63-64) and fades out quadratically (`1.0 - progress * progress`, VoidSlashRenderer.java:60-62). The shape itself never rotates, pulses, or grows while flying. No fade in.
- Particles: none.
- Grade: DECENT, four layer volumetric crescent with a feathered trailing gradient and a smooth fade out, but the mesh is static while it flies.

### VoidRiftEntity / VoidRiftRenderer
- Lifetime: indefinite while channeled. Release triggers a 10 tick implosion countdown (VoidRiftEntity.java:69-72, burst at :101-131); if never released it auto implodes at 400 ticks (:176-178).
- Trigger: Void Rift right click channel (HollowsEdgeItem.java:305-317), placed at the crosshair point up to 12 blocks out, implosion on release (HollowsEdgeItem.java:395).
- Technique: custom mesh, the largest of the Hollow set (VoidRiftRenderer.java): 3 nested octahedron cores (:71-77), a 3D beveled accretion disk, a second inclined disk, 14 orbiting shard pyramids, 4 vertical tear planes, and 2 flat shockwave rings while imploding (:133-138).
- Colors and blending: gradient rich. The disk alone uses a 3 stop scheme, base (0.40, 0.02, 0.70), mid (0.85, 0.10, 1.0), edge (0.15, 0.90, 1.0) (VoidRiftRenderer.java:85-89); shards gradient from (0.30, 0.02, 0.60) to (0.15, 0.95, 1.0) (:119-120); cores near black, violet, and white (:73-77). Normal translucent emissive, ki_white.png only.
- Motion: rotates and pulses continuously (rotation age * 12, or age * 40 while imploding, VoidRiftRenderer.java:60; pulse at :61-62), shards orbit. One timing defect: the implosion collapse scale and ring phase are keyed to total age modulo 10 (:63, :134-135) instead of time since the implosion was triggered, so the collapse animation phase depends on when in the modulo cycle the release happened. No fade in at spawn, no overall fade out.
- Particles: none directly. At the implosion burst it calls DbaImpactFx.heavy (VoidRiftEntity.java:122), which sends DbaParticles.SPARK and WIND plus shake and sound (DbaImpactFx.java:26-27). The pull phase emits no particles, only a PORTAL_AMBIENT sound every 25 ticks (VoidRiftEntity.java:170-173).
- Grade: RICH, multi layer rotating singularity with gradient disks, orbiting shards, and a dedicated implosion state; docked for the modulo timing defect and no spawn fade.

---

## 5. Dimensional Sword (item/DimensionalSwordItem.java)

### DimensionalSlashEntity / DimensionalSlashRenderer
- Lifetime: up to 101 ticks (5 s), discard at DimensionalSlashEntity.java:101-103; usually ends earlier because onHit discards it on any hit (:143-148).
- Trigger: Dimensional Slash left click (DimensionalSwordItem.java:98), velocity look * 2.5 (:100), 8 tick cooldown (:93).
- Technique: custom mesh: three nested volumetric crescents (DimensionalSlashRenderer.java:97-106), tip spur quads, a 3 color singularity octahedron, two concentric rings, and four tapered wake ribbons. Fixed Z tilt of plus or minus 35 degrees (:76-80).
- Colors and blending: layered flat colors, outer crescent (0.65, 0.05, 0.95), magenta core (1.0, 0.10, 0.35), white seam (1.0, 0.95, 1.0) at DimensionalSlashRenderer.java:97-106; spurs, rings, and ribbons carry per vertex alpha gradients to 0 (:177-186, :207-208, :129-131); octahedron is void black, magenta glow, and white core (:112-115). Normal translucent emissive, ki_white.png only.
- Motion: pulses (`0.90 + 0.10 * sin(age * 0.6)` at DimensionalSlashRenderer.java:94), yaw wobble, spinning octahedron (age * 30), counter rotating rings. But there is no fade in or fade out at all: every alpha is constant, with only a first person scale ramp over the first 5 ticks for the owner's own view (:82-83). It pops in at spawn and pops out on hit or timeout, potentially after 5 full seconds.
- Particles: none.
- Grade: DECENT, geometry and internal animation are rich, but constant alpha with no fade envelope makes it pop at both ends, unlike its Hollow's Edge counterpart VoidSlashEntity which fades out.

### DimensionalWarpRiftEntity / DimensionalWarpRiftRenderer
- Lifetime: 24 ticks (1.2 s). MAX_LIFETIME 24 at DimensionalWarpRiftEntity.java:33, discard at :160-162.
- Trigger: Dimensional Warp right click (DimensionalSwordItem.use): an origin rift at the player (DimensionalSwordItem.java:153-156, 250 burst damage) and a destination rift at the raycast target (:163-166, 750 burst damage).
- Technique: custom mesh, seven parts (DimensionalWarpRiftRenderer.java): 4 rotating ground rings (:92-105), 8 radiating fissure trench strips, 10 tilted debris slab pyramids, a vertical tear slit drawn at 4 angles, 2 tilted orbital rings, 8 orbiting shard pyramids, and a spinning octahedron core.
- Colors and blending: layered colors with several per vertex gradients: rings crimson, violet, magenta, and a white eye (:92-105); trenches violet over a red core (:121-125); slabs gradient to (0.95, 0.10, 0.45) (:154-155); the tear slit stacks violet corona, magenta, a near black core, and a white seam (:279-292), though the black core layer ignores the fade at a fixed alpha 0.98 (:287-288). Normal translucent emissive, ki_white.png only.
- Motion: the most complete envelope in this group: fade is sin(progress * PI) over the 24 tick life (DimensionalWarpRiftRenderer.java:69-75), radius ramps in over 4 ticks, destination grows while origin shrinks (:77-78), pulse, rotating rings, shards spiral inward at origin and outward at destination, octahedron spins at age * 35.
- Particles: none from the entity (the item's DbaImpactFx.medium at the destination is shake only, no particles).
- Grade: RICH, full sine fade envelope, origin versus destination behavior split, and seven animated geometry layers inside a 1.2 second life. This is the standard the other long lived effects should match.

---

## Ranked: the 5 worst looking entities in this group

Ranked worst first. Nothing in this group is FLAT or THIN; these are the five whose presentation most undercuts otherwise strong geometry.

1. **DarknessBladeEntity (Abyssal Eclipse blade rain).** The finisher's centerpiece is the only renderer in the group with zero age based animation: constant 7.5 block blade (DarknessBladeRenderer.java:64-65), no spin, pulse, trail motion, or fade, popping in at spawn height and out on impact, with particle imports in the entity (DarknessBladeEntity.java:3-4) that were never wired. Most impactful fix: give the falling blades a renderer spin and speed streak treatment keyed to fall velocity, plus a short fade in, so the rain reads as motion instead of falling statues.
2. **SkyCracksEntity (Cursed Tempest sky cracks).** The dominant element is one flat 960 x 960 textured quad (SkyCracksRenderer.java:67-75) held at near full alpha for 5 seconds with no fade in or out (SkyCracksEntity.java:10). Most impactful fix: add a fade envelope over the 100 tick life (in over the first 10 ticks, out over the last 20) so the sky does not snap a giant decal on and off.
3. **AzureStormEntity (Call of the Tempest storm domain).** The largest mesh in the group runs 11 seconds (AzureStormEntity.java:30) with no lifetime term anywhere in AzureStormRenderer submit, so a 30 block domain pops in and out at full presence; its entity also imports particle classes it never uses (AzureStormEntity.java:3-4). Most impactful fix: add a progress based fade in over the first 20 ticks and fade out over the last 40 ticks of the 220 tick life.
4. **AzureTornadoEntity (rush mini tornado and storm giant).** Rich spinning geometry stuck at constant alpha for 3 to 10 seconds (AzureTornadoEntity.java:27, :41), flat single colors per layer, and unused particle imports (AzureTornadoEntity.java:3-4). Most impactful fix: the same fade envelope as the storm (about 10 ticks in, 20 ticks out), which also hides the hard spawn and discard edges of the funnel.
5. **DimensionalSlashEntity (Dimensional Slash projectile).** Geometry and internal animation are rich, but every alpha is constant for up to 5 seconds of flight (DimensionalSlashEntity.java:101-103) and it pops at both ends; its closest sibling, VoidSlashEntity, already fades out quadratically (VoidSlashRenderer.java:60-62). Most impactful fix: copy that fade out pattern (and a 3 to 5 tick fade in) into DimensionalSlashRenderer so the slash dissolves instead of snapping off on hit or timeout.

Honorable mentions, in order: DarknessWaveEntity (volumetric crescent, almost no age animation, pops out at its 40 tick discard, DarknessWaveEntity.java:149-150); DarknessShatterEntity and CurseGroundShatterEntity (shared broken fade formula `1.0 - age / (lifetime + age)` at DarknessShatterRenderer.java:65 and CurseGroundShatterRenderer.java:64, which strands both at about half alpha when they vanish); AzureTempestChannelEntity (vanishes instantly on release with no fade out); HollowAfterimageEntity (entity lives 100 ticks but is only drawn for 80, HollowAfterimageEntity.java:106 versus HollowAfterimageRenderer.java:70-72); VoidRiftEntity (implosion animation phase keyed to total age modulo 10 instead of time since trigger, VoidRiftRenderer.java:63).
