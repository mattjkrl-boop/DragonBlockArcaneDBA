# Entity Visual Audit Group C (Nov 1, 2026)

Audit only, no code changed. Scope: every entity spawned by Spirit Sword, Evil Spear, Devil Trident, Sickle of Sorrow, and Ox King's Axe, with its client renderer read in full. 15 entities total.

Shared facts for the whole group:

- Every renderer in this group uses the same render type: `KiRenderHelper.kiRenderType()` (src/client/java/com/dragonblockarcanedba/client/render/ki/KiRenderHelper.java:28-30), which is `RenderTypes.entityTranslucentEmissive` over the plain white canvas `textures/entity/ki_white.png` (KiRenderHelper.java:19-20). That is normal translucent blending, full bright. The additive variant `kiAdditiveType()` (RenderTypes.eyes, KiRenderHelper.java:23-25) exists in the helper and is used by none of these 15 renderers.
- Every entity is a custom mesh built in code via `submitCustomGeometry`. No GeckoLib models, no item renders, no camera facing billboard quads, no real textures anywhere in the group. All color is vertex color.
- Almost no entity emits its own particles. The exceptions are DimensionalRiftEntity (per target SPARK only), OxShockwaveEntity, OxFissureEntity, and KingsSlamEntity. Everything else relies on the mesh alone.

---

## Spirit Sword

### SpiritImpaleEntity
- Entity: entity/SpiritImpaleEntity.java. Renderer: client/render/SpiritImpaleRenderer.java. Lifetime 28 ticks (entity L21-22, discard L92-97); it follows its target each tick (L84-90).
- Trigger: Spirit Sword melee (SpiritSwordItem.hurtEnemy, spawn SpiritSwordItem.java:130-131, scale 1.0, deduped per target) and Spirit Cannon beam hits (onUseTick, spawn L239-240, scale 0.75).
- Technique: custom mesh. 3 ground rings (renderer L79-83), 2 cylinder pillars with helical ribbons (L89-93, 240-266), 6 faceted 3D swords with blade, crossguard, grip, pommel (L101-121, 150-238), 8 crystal spikes (L125-146).
- Color and blending: multi color, not flat. Cyan outer ring, gold middle ring, white inner disk (L79-83), pillar alpha gradient to 0.25 at the top (L319-320), sword faces alternate core cyan and pale edge with a gold guard (L195-216), spikes have a true vertex gradient, cyan base to white tip (L143-144).
- Motion: slam in over 4 ticks drives ring radius, pillar height, and sword drop (L66, 76, 87, 98); pulse 0.85 + 0.15 sin (L67); rings counter rotate (L79-83); swords orbit and bob (L99, 102); helices rotate (L251-252). Fade out over the last 8 ticks only, no fade in (L65).
- Own particles and sounds: none.
- Grade: RICH. Six articulated swords plus rings, pillar, helices, and spikes, with slam in, pulse, and a correct fade out.

### SpiritCannonBeamEntity
- Entity: entity/SpiritCannonBeamEntity.java. Renderer: client/render/SpiritCannonBeamRenderer.java. No fixed lifetime: discarded when the player stops holding the sword or stops using the item, or the caster dies (entity L111-123). Beam length raycast per tick, capped at 32 blocks (L28, 98-109).
- Trigger: Spirit Cannon, right click hold (SpiritSwordItem.use L147-153, created in onUseTick L186-188, discarded in releaseUsing L261-273).
- Technique: custom mesh oriented by caster yaw and pitch (renderer L89-99). 3 prism beam layers (L106-115), 4 helical drill ribbons (L123-154), planar rings along the beam (L157-167), muzzle emitter (L169-170, 220-239), terminus impact dome and fins (L172-173, 241-288).
- Color and blending: layered flat colors per submesh, no within quad gradient. White core alpha 0.95 (L107), cyan shroud 0.65 (L111), outer sheath 0.30 (L115), helices alternate cyan and gold (L123-129), terminus cyan, white, and gold (L261-285).
- Motion: pulse 1 + 0.06 sin scales all beam radii (L85); prism layers rotate at different rates (L107-115); helix phase advances with opposite directions per color (L121, 138-139); rings rotate and breathe (L162-165); muzzle rings and fins rotate (L222-223, 276). No fade in or out: full alpha at spawn, pops out on discard.
- Own particles and sounds: none. The SPARK hits at SpiritSwordItem.java:243-245 come from the item, not the entity.
- Grade: RICH. Three layer prism, drill helices, travelling rings, muzzle and terminus geometry, all rotating and pulsing. Only weakness is the pop in and out.

## Evil Spear

### EvilSpearChargeEntity
- Entity: entity/EvilSpearChargeEntity.java. Renderer: client/render/EvilSpearChargeRenderer.java. No fixed lifetime: follows the caster (entity L70), discards if the caster dies, stops holding the spear, or charge updates stop for 7 ticks (L72-89).
- Trigger: Evil Impale charge up, left click hold (EvilSpearItem.onLeftClickChargeTick L98, spawn or reuse L116-121, ratio pushed every tick L122, cap 120 ticks at item L46). Release discards it (item L135-138).
- Technique: custom mesh. 3 flat ground seal rings (renderer L70-77), 4 gathering rings that contract and rise (L81-90), 8 to 14 orbiting 3D blood crystal shards (L93-132), 2 to 3 nested rotating octahedrons (L135-140, 224-251), twin helical vortex ribbons (L144-179).
- Color and blending: crimson family with real variation. Ground rings go dark crimson to blood red to pink white core (L70-76); gathering ring color shifts with cycle phase (L88-89); shards have a vertex gradient, dark base to pale tip (L129-130); helix alpha decays along the strand (L176).
- Motion: charge driven. Base radius 1.35 + charge x 1.55 and alpha 0.65 + charge x 0.35 (L60-61), pulse (L62), ground rings counter rotate (L70-77), gathering rings cycle, contract from 2.6 to 0.35, rise, and fade per cycle (L82-86), shard count, orbit speed, and length all scale with charge (L93-116), octahedron core shrinks as charge rises (L135-139). No global fade out: discarded on release.
- Own particles and sounds: none.
- Grade: RICH. Charge changes radius, alpha, shard count, and spin speed, plus contracting rings and nested octahedrons. Only weakness is the abrupt discard on release.

### EvilSpearProjectileEntity
- Entity: entity/EvilSpearProjectileEntity.java. Renderer: client/render/EvilSpearProjectileRenderer.java. Lifetime 40 ticks max (entity L178-180), earlier discard on block hit (L86-93). Pierces entities via a hit set (L29, 110-114). On each entity hit it spawns a HellHuntImpactEntity (L169-173).
- Trigger: Evil Impale throw, left click release (EvilSpearItem.onLeftClickRelease L130, spawn L150-152, velocity scales with charge).
- Technique: custom mesh oriented by yaw and pitch (renderer L67-68). 8 sided shaft cylinder plus 6 sided inner core (L78-79), two nested diamond spearheads (L82-83, 187-197), 4 rear fins and 2 barbs (L86-93), 16 segment Mach shock cone (L96-112), twin helical trail ribbons (L119-150), 3 wake rings (L153-160), thrust beam (L163).
- Color and blending: layered crimsons. Obsidian shaft against a bright red inner core (L78-79), outer head against a white pink inner head (L82-83), translucent Mach cone (L111). Trail ribbons have a gradient along their length with alpha falling by segment (L147).
- Motion: continuous spin around the flight axis, 30 degrees per tick of age (L69); first person scale in over 5 ticks (L71-72); trail helix advances and widens (L128-132); wake rings expand from 0.35 to 1.55 and fade per cycle (L154-157). The spear body has no pulse and no fade out.
- Own particles and sounds: no particles. Two TRIDENT_HIT sounds, on block hit (entity L89-90) and entity hit (L175).
- Grade: RICH. Detailed multi part spear with spin, shock cone, animated helical trail, and expanding wake rings. Only weakness is no fade out at end of flight.

### HellHuntImpactEntity
- Entity: entity/HellHuntImpactEntity.java. Renderer: client/render/HellHuntImpactRenderer.java. Lifetime 35 ticks (entity L20, countdown and discard L66-71).
- Trigger: Hell Hunt chain steps (EvilSpearItem.executeHuntStep, spawn L386-389, radius 3.0, or 4.5 with isFinal on the last step), the no target lunge landing (item L338-341, radius 2.6), and Evil Spear projectile hits (EvilSpearProjectileEntity.java:170-173, radius 2.6).
- Technique: custom mesh. Expanding ground shock rings (renderer L76-86), 8 or 10 radiating fissure trenches with 5 jittered segments each (L89-108), 12 or 18 tilted erupting spikes (L111-136), 2 layer vertical pillar (L139-150). Counts rise when isFinal (L89, 111).
- Color and blending: outer ring dark crimson, inner disk brighter (L79-81), final halo white pink (L84), trenches two tone (L102-104), spikes vertex gradient dark base to bright tip (L133-134), pillar crimson with white pink core and a vertical alpha gradient to 0.2 at the top (L147-149, 221-222).
- Motion: shock ring expands (L76), trench width grows (L100), spike height grows (L119), pillar exists only for the first 12 ticks (18 if final) with its own correct progress alpha and growth (L139-144). Ground rings never rotate.
- Fade defect: the global fade at renderer L67 is `1 - (age / (lifetime + age))`, and the lifetime it reads never decrements on the client (the entity decrements server side only), so fade bottoms out near 0.50 at discard. Rings, trenches, and spikes are still at half alpha when the entity vanishes and pop out. The pillar is exempt because it has its own progress alpha (L142).
- Own particles and sounds: none.
- Grade: DECENT. Rich shape count and good gradients, held back by no ring rotation and the broken fade that leaves the ground effects popping out at half alpha.

## Devil Trident

### KiBlastEntity (the trident laser bolt)
- Entity: entity/KiBlastEntity.java. Renderer: client/render/ki/KiBlastRenderer.java. No lifetime cap and no age discard: tick is only super.tick (entity L43-46). It flies until it hits an entity or a block, where explode discards it (L49-76).
- Trigger: Devil Trident left click while not deployed (DevilTridentItem.performLeftClickTargeting L112, fireLasers L126, loop L161-174): 3 bolts, damage 250 each, color set to pure red 0xFF0000 (L163-164). This is the shared ki blast entity, not trident specific.
- Technique: custom 3D bolt, 4 layers (renderer L67-85): 8 sided tapered plasma sheath in the entity color, 6 sided white hot core spindle at alpha 1.0 (L78), 2 strand helical vortex ribbons (L81), 12 segment resonance ring (L84).
- Color and blending: gradient is alpha only, spatially. Tail tip vertices fade to alpha 0, helix alpha falls along its length, ring outer edge fades to 0.
- Motion: pulse 1 + 0.12 sin applied to the radius (renderer L58, 70), axial spin 30 degrees per tick of age (L65), helix phase advances with age. No fade in or out: pops in at spawn, pops out on hit.
- Own particles and sounds: no particles. On hit only, GENERIC_EXPLODE and LIGHTNING_BOLT_IMPACT sounds (entity L70-73). The entity spawns no explosion visual itself.
- Grade: RICH. Layered sheath, white core, helix, and ring, with spin and pulse. Zero lifetime fade at either end.

### DevilSlamShockwaveEntity
- Entity: entity/DevilSlamShockwaveEntity.java. Renderer: client/render/DevilSlamShockwaveRenderer.java. Lifetime 35 ticks (entity L20, decremented server side L56-60).
- Trigger: Devil Trident right click deploy slam (DevilTridentItem.use, spawn L201, radius 5.0). The item also fires DbaImpactFx.heavy and spawns the shard swarm on the same click.
- Technique: custom ground mesh, 4 systems (renderer L68-154): expanding ground rings with 12 runic teeth (L76-88), 9 radiating fissure trenches (L92-110), 14 jagged spikes (L114-140), vertical hellfire pillar in 2 layers (L150-152).
- Color and blending: rings crimson pair (L76-78), trenches magma outer with pale core (L92-110), spikes vertex gradient dark base (0.18, 0, 0.05) to bright tip (1.0, 0.08, 0.22) (L136-137), pillar outer crimson with pale core and top vertices at 0.2 alpha (L150-152, 212 onward).
- Motion: shock radius grows to radius x 1.35 (L73), runic teeth rotate slowly (L82), trench width and spike height grow in (L100 area, L122), pillar lives 14 ticks with a correct progress fade, growing height, and widening radius (L142-147).
- Fade flaw: same broken family as HellHuntImpact. Global fade at renderer L65 uses the client side lifetime, which never decrements, so it ends near 0.5 alpha at discard. Rings, trenches, and spikes pop out at roughly half alpha. Only the pillar fades fully.
- Own particles and sounds: none.
- Grade: RICH. Four distinct geometry systems with growth and a gradient spike, undermined by the broken end fade.

### TridentShardEntity
- Entity: entity/TridentShardEntity.java. Renderer: client/render/TridentShardRenderer.java. No fixed lifetime: discarded when the owner is gone, dead, or over 64 blocks away, on recall arrival, or when swarm health runs out (entity L123-176). Health starts at 15000 (L21).
- Trigger: Devil Trident right click deploy spawns 10 shards (DevilTridentItem.use L241-246); manageShardSwarm respawns missing indexes (L286-292); recallShards recalls them (L176-183). Entity motion: homing at speed 2.5, recall flight at 1.8, idle orbit radius 2.2 with a vertical wave (entity L137-193).
- Technique: custom faceted blade (renderer drawDemonicBlade L77 onward): 4 blade triangles in two tones, raised fuller spine in bright red, cutting edge lines, 2 lateral barbs, bright collar core quad. Plus a 2 strand helical wake (L166-191).
- Color and blending: dark blade faces (0.18, 0.02, 0.06) against crimson bevels (0.45, 0.02, 0.08), spine (1.0, 0.10, 0.20), collar core near white (1.0, 0.85, 0.90). Wake alpha gradients to 0 at the end vertices (L190).
- Motion: oriented to yaw and pitch (L56-57), axial roll 22 degrees per tick of age (L59), fixed scale 0.85 (L60). A pulse exists (L64) but only modulates the fuller alpha and core radius; the blade body itself never scales or pulses. No fade in or out over a swarm life that can last minutes.
- Own particles and sounds: no particles. TRIDENT_HIT sound on a damaging hit (entity L168-169).
- Grade: DECENT. A detailed but sub block sized blade (about 0.95 long at 0.85 scale) with pulse accents and a fading wake, but a constant appearance and pop in and out at both ends of a long lived swarm entity.

## Sickle of Sorrow

### SorrowSlashEntity
- Entity: entity/SorrowSlashEntity.java. Renderer: client/render/SorrowSlashRenderer.java. Lifetime 16 ticks for the impact burst, 30 ticks for the flying wave (entity L153-157). The wave also discards on the first block clip (L84-95); the burst has no movement and stays at its spawn point.
- Trigger: variant A, melee impact burst at damage 0 (SickleOfSorrowItem.executeTechniqueMelee, construction L143-155, isImpactBurst true; the entity skips all targets at damage 0, entity L116-120, so it is pure geometry). Variant B, flying air swing wave at 750 + Strength x 3 (performTechniqueAirSwing, construction L181-192, delta movement look x 1.6).
- Technique: custom crescent mesh, 20 segments (renderer drawCrescentArc L106 onward). 3 stacked layers of the same arc (L86-97): outer void silhouette (0.08, 0, 0.15) at 0.90 alpha, magenta crest (0.80, 0.05, 0.85) at 0.95, cyan white edge (0.90, 0.98, 1.0) at 0.72 scale. The burst adds a 4th reversed cross arc (L99-102). Within each arc the trail vertices taper to alpha 0.
- Motion: fixed diagonal tilt of plus or minus 42 degrees, alternating per swing (renderer L72-73). Quadratic fade out to 0, no fade in (L63-65). Burst grows to 1.4 scale over its life (L76-77); the flying wave stays at scale 1.0, with no rotation, pulse, or growth for its whole flight.
- Own particles and sounds: no particles. Per target hit, damaging wave only: PLAYER_ATTACK_SWEEP and SOUL_ESCAPE (entity L147-150).
- Grade: DECENT. A clean 3 layer tapered crescent with a proper fade out and a distinct burst variant, but it pops in at full alpha and the flying wave is a frozen stamp in motion.

### DimensionalRiftEntity
- Entity: entity/DimensionalRiftEntity.java. Renderer: client/render/DimensionalRiftRenderer.java. Lifetime 60 ticks (entity L34), radius expands 3 to 15 over 40 ticks (L79-81), collapsing flag in the last 10 ticks (L83-85).
- Trigger: Sickle right click Dimensional Rift (SickleOfSorrowItem.use L199-209, performTechniqueDimensionalRift, construction L237-242, burst damage 500 + Spirit x 2).
- Technique: custom mesh, 4 systems: ground rings with a 3 stop vertex gradient and a raised wall (renderer L72-95), 3 vertical tear planes at 0, 60, 120 degrees with near black cores and colored edges (L97-103), 18 orbiting shard pyramids with a base to tip gradient leaning inward (L106-130), singularity core of two nested octahedra, near black outer and white inner (L133-135).
- Motion: the only entity in this group with both ends faded. Fade in over 5 ticks, collapse fade out over the last 10 (renderer L60). Rotation speeds up when collapsing (L63); middle ring counter rotates, inner ring rotates at 2.2x, shards orbit (L111 area); pulse drives ring height, tear height, and core size (L64, 97, 133). Overall scale growth comes from the synced radius.
- Own particles and sounds: per target SPARK on the first burst hit only (entity L142-144), no ambient or core particles despite the size. Sounds are its own: sonic boom set on tick 1, portal ambient every 20 ticks with rising pitch, deplete sound at discard (L165-180).
- Grade: RICH. Expansion, counter rotating rings, pulse, collapse speedup, and real fades at both ends. Only the particle side is thin.

## Ox King's Axe

### OxChargeEntity
- Entity: entity/OxChargeEntity.java. Renderer: client/render/OxChargeRenderer.java. No fixed lifetime: follows the caster, discards if the caster is gone, stops holding the axe, or charge updates stop for 6 ticks (entity L76-88).
- Trigger: Groundbreaker charge, left click hold (OxKingsAxeItem.onLeftClickChargeTick L111, spawn or update L138-146, ratio set every tick, cap 200 ticks). Discarded by the item on release (L166-169).
- Technique: custom mesh, 4 layers: branching ground trenches, 8 to 14 trenches of 4 steps (renderer L72-90), 2 flat rotating ground rings (L93-98), levitating rock chunks (L101-131), ascending tendril quads from charge 0.25 up (L134-146).
- Color and blending: gradient by layer and vertex. Trench base orange over a molten pale core (L85-87), rings in two oranges (L95-98), rocks grade molten base to dark basalt tip (L128-129), tendrils fade to alpha 0 at the top vertex (L208-224).
- Motion: charge driven, not fade driven. Base radius 1.2 + charge x 2.6 and alpha 0.70 + charge x 0.30 (L63-64); tremble grows with charge (L65, 106-107); rings counter rotate (L95-97); rocks bob with a charge scaled height (L110-111); tendrils advance (L138 area). No fade in or out: pops out on discard.
- Own particles and sounds: none. Rumble and full charge cue live in the item.
- Grade: RICH. Four animated layers with charge scaling, rotation, bobbing, and vertex gradients. Only weakness is the abrupt pop out.

### OxShockwaveEntity
- Entity: entity/OxShockwaveEntity.java. Renderer: client/render/OxShockwaveRenderer.java. Lifetime is computed: maxRadius 4 + charge x 20 (x 0.75 for sub waves), maxTicks about 10 to 31 (entity L69-74, discard L239-240).
- Trigger: Groundbreaker release (OxKingsAxeItem.onLeftClickRelease L161, spawn L179-180). It spawns echo copies of itself at ticks 6 and 12 when charge is 0.50 or more (entity L130-140), and at max charge it spawns 6 OxFissureEntity (L116-123) and destroys weak terrain along the ring (L145-154).
- Technique: custom mesh, 3 layers: faceted crags around the expanding perimeter, 24 for sub waves or 28 to 36 for the main wave (renderer L78-118), 2 continuous shockwave ring walls with top ridge, 32 segments (L122-133), 14 debris shards at the leading edge (L135-157).
- Color and blending: vertex gradient molten base to dark basalt tip on the crags (L115-116); outer ring top fades to alpha 0 at the bottom (L125-127); inner ridge grades pale to orange (L130-132).
- Motion: expansion is server synced (entity L100-102, renderer L55). Crag height is fixed per charge and only moves outward with the radius (renderer L70). Alpha has a floor: max(0.2, 1.2 - age x 0.02) at L71, so it never fades below 0.2 and pops at discard. No rotation.
- Own particles and sounds: at charge 0.95 or more, tick 1 EXPLOSION_EMITTER plus GENERIC_EXPLODE (entity L113-114), EXPLOSION per target hit (L230-235), BLOCK particles per destroyed weak block (L253-257).
- Grade: RICH. Tall faceted crags, two layered ring walls, and shards riding a synced expanding radius, held back by the 0.2 alpha floor pop out ending.

### OxFissureEntity
- Entity: entity/OxFissureEntity.java. Renderer: client/render/OxFissureRenderer.java. Lifetime 100 ticks (entity L24, discard L89-90).
- Trigger: only spawner in code is OxShockwaveEntity at max charge: 6 fissures at random angles and distances, damage 50 per second (OxShockwaveEntity.java:116-123). Note the direction of the SlamDebris link: the fissure calls SlamDebris.burst on its own tick 1 (entity L51-52); SlamDebris does not spawn it.
- Technique: custom mesh but the smallest in the group: one jagged ground trench line, 8 segments, fixed total length 3.6 along Z with random X jitter (renderer L65-77), drawn as 2 flat strips (L83-85), plus small single triangle rock teeth on every second segment (L88-103). No rotation is synced or applied, so every fissure points the same way no matter what radial angle it spawned at.
- Color and blending: 2 flat strip colors modulated by pulse (L83-85); teeth grade orange base to dark tip (L99-100).
- Motion: pulse 0.75 + 0.25 sin on the green channels only (renderer L57); full linear fade out over 100 ticks (L58); teeth grow in over 5 ticks (L91). Otherwise static in place for 5 seconds: no rotation, no spread, no branching.
- Own particles and sounds: SlamDebris.burst on tick 1 (ground BLOCK particles, POOF, EMBER accent), ambient EMBER every 4 ticks (entity L57-64), LAVA_POP every 10 ticks (L67-69).
- Grade: DECENT. A genuine pulse, full fade out, and growing teeth, but the geometry is one short flat trench line plus small triangles, with no branching and no orientation variety.

### OxStanceAuraEntity
- Entity: entity/OxStanceAuraEntity.java. Renderer: client/render/OxStanceAuraRenderer.java. No fixed lifetime: follows the caster, discards if the caster is gone, stops using the item, or stance updates stop for 6 ticks (entity L84-96).
- Trigger: Colossal Stance, right click hold (OxKingsAxeItem.onUseTick L246 onward, spawn or update L274-281, peak window at held 280 to 300 ticks). Ended by releaseUsing (L331-335) or overload past 300 ticks (L298-301).
- Technique: custom mesh, 4 layers: 12 block ground disc with 2 counter rotating rings, 12 radial pressure nodes, and an expanding ripple ring (renderer L68-95); titan avatar built from flat plates, 2 shoulder pauldron quads, 2 horn triangles, 2 aegis chest plates (L107-116); heat dome wall, 24 segments, plus 2 helical ribbon spirals (L118-149); peak only energy spikes (L152-168).
- Color and blending: avatar uses one color pair switched by peak state (L102-104); ground rings are hotter variants (L75-78); heat dome is a vertical alpha gradient, 0 at the base to 0.40 alpha at the top (L234-250); peak spikes are pale yellow at full alpha (L165 area).
- Motion: intensity ramps with held ticks and lifts alpha (L62-64); pulse scales avatar height and width and dome radius (L63, 98-120); rings and nodes rotate (L75-83); the ripple expands continuously across the 12 block disc and fades as it goes (L72, 91-94); dome and helices rotate (L121, 139-140); peak spikes rotate and oscillate in length (L156-157). No fade in or out: it pops in and out with the stance.
- Own particles and sounds: none.
- Grade: RICH. The largest component count in the group, with a pulsing avatar, counter rotating disc, expanding ripples, a gradient dome, and double helices, held back by flat single quad avatar plates and no fades.

### KingsSlamEntity
- Entity: entity/KingsSlamEntity.java. Renderer: client/render/KingsSlamRenderer.java. Lifetime is synced: 40 ticks normal, 55 ticks Flawless (entity L34-44, discard L68-70).
- Trigger: stance release in OxKingsAxeItem.releaseUsing (L331 onward). Flawless at held 280 to 300 ticks, radius 20 (spawn L364-366); normal at held 10 to 279, radius 10 (spawn L407-409). One boolean drives radius, lifetime, color, counts, and the geyser layer.
- Technique: custom mesh, 4 layers (3 for normal): expanding ground shockwave of crest ring, solid caldera disc, and hemispherical dome wall (renderer L80-91); radiating canyon trenches, 12 normal or 18 flawless, 6 steps each (L94-112); erupted rock monoliths, 16 normal or 28 flawless (L116-144); Flawless only, 8 vertical magma geysers of crossed quads (L146-159, 236 onward).
- Color and blending: base color switches by flag, orange normal to pale gold flawless (L72-74); caldera and trench cores brighten when flawless (L86, 109); monoliths grade molten base to dark tip (L140-141); dome wall and geysers are vertical alpha gradients to 0 at the top (L205-215, 238-248).
- Motion: the cleanest curve in the group. Progress and fade are linear over the full lifetime and fade multiplies almost every alpha, so it fades out fully (L68-69). Shock radius grows linearly and caps (L81); dome height shrinks as it expands (L89); trench width grows in (L104 area); monoliths rise (L125); geysers are static height and only fade. No rotation anywhere.
- Own particles and sounds: SlamDebris.burst on tick 1 only, scaled by slam radius (entity L64-66). No sounds of its own.
- Grade: RICH. Layered expanding disc, dome, trenches, and rising monoliths with a true full fade out, plus a Flawless only geyser layer.

---

## Summary table

| Entity | Weapon | Lifetime | Fades | Grade |
|---|---|---|---|---|
| SpiritImpaleEntity | Spirit Sword | 28 ticks | out only, correct | RICH |
| SpiritCannonBeamEntity | Spirit Sword | while held | none | RICH |
| EvilSpearChargeEntity | Evil Spear | while charging | per ring cycle only | RICH |
| EvilSpearProjectileEntity | Evil Spear | 40 ticks | trail and rings only | RICH |
| HellHuntImpactEntity | Evil Spear | 35 ticks | broken, ends near 0.50 alpha | DECENT |
| KiBlastEntity | Devil Trident | until hit | none | RICH |
| DevilSlamShockwaveEntity | Devil Trident | 35 ticks | broken, ends near 0.50 alpha | RICH |
| TridentShardEntity | Devil Trident | swarm life | none | DECENT |
| SorrowSlashEntity | Sickle of Sorrow | 16 or 30 ticks | out only, to 0 | DECENT |
| DimensionalRiftEntity | Sickle of Sorrow | 60 ticks | in and out, correct | RICH |
| OxChargeEntity | Ox King's Axe | while charging | none | RICH |
| OxShockwaveEntity | Ox King's Axe | 10 to 31 ticks | floor of 0.2, pops | RICH |
| OxFissureEntity | Ox King's Axe | 100 ticks | full linear out | DECENT |
| OxStanceAuraEntity | Ox King's Axe | while using | none | RICH |
| KingsSlamEntity | Ox King's Axe | 40 or 55 ticks | full linear out | RICH |

Group pattern: the meshes themselves are almost all rich. The recurring weaknesses are endings and smallness, not shape count: two entities share a broken fade formula, one has an alpha floor, five pop in or out with no fade at all, and the two smallest meshes (fissure, shard) sit static for long lifetimes.

## The 5 worst looking entities, ranked

1. **OxFissureEntity** (Ox King's Axe). One flat 3.6 block trench line plus small triangle teeth, sitting static for 100 ticks, and every fissure points the same direction because no rotation is synced (OxFissureRenderer.java:65-77). Most impactful fix: sync the radial spawn angle from OxShockwaveEntity into the entity and rotate the mesh to it, with 2 to 3 jittered branch segments off the main line, so each fissure reads as a radial crack from the slam instead of the same decal stamped 6 times.
2. **TridentShardEntity** (Devil Trident). A sub block sized blade (about 0.95 long at 0.85 scale, TridentShardRenderer.java:60) that can orbit a player for minutes with a fixed look: the pulse at L64 only touches the fuller alpha and core radius, and there is no fade at spawn, recall, or death. Most impactful fix: apply the existing pulse to the whole blade scale and alpha, and add a short fade in on deploy and fade out on recall or death, so the swarm reads as living energy instead of 10 identical static knives popping in and out.
3. **SorrowSlashEntity, flying wave variant** (Sickle of Sorrow). A single crescent arc, well layered but frozen: scale locked at 1.0, no spin, no pulse, no growth across its 30 tick flight, and it pops in at full alpha (SorrowSlashRenderer.java:63-77). Most impactful fix: grow the wave from about 0.7 to 1.3 scale and roll the tilt slowly during flight, with a 3 tick fade in, so it travels like a thrown crescent instead of a sliding stamp.
4. **HellHuntImpactEntity** (Evil Spear). The shapes are rich, but the broken fade at HellHuntImpactRenderer.java:67 leaves rings, trenches, and spikes at roughly half alpha when the entity is discarded at 35 ticks, so the centerpiece impact of every Hell Hunt step visibly pops out. Most impactful fix: replace the fade formula with a fixed lifetime progress fade (1 minus age over 35, easing out in the last 10 ticks), matching the pillar fade the same renderer already does correctly at L141-142.
5. **DevilSlamShockwaveEntity** (Devil Trident). Same broken fade family at DevilSlamShockwaveRenderer.java:65: the deploy slam is the trident's signature move, its pillar fades correctly, and then its rings, trenches, and spikes all pop out together at about half alpha. Most impactful fix: same correction as HellHuntImpact, drive the global fade from a fixed 35 tick lifetime progress so the whole ground set lands at alpha 0 on the same tick the pillar finishes.
