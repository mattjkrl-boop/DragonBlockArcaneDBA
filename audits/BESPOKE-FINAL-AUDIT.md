# Bespoke Pass Final Audit (coordinating merge, Phase 2)

Merge of the 23 Phase 2 conversions (22 weapons plus status effects) on
top of the Phase 1 foundation commit 2ef870b. Inputs: the 23 reports at
audits/bespoke-report-*.md, the working tree diff (180 files), and the
checks below, all run at merge time by the coordinating pass.

## Merge verdict

The pass goal is met in weapon scope: zero vanilla ParticleTypes, zero
DustParticleOptions, zero vanilla SoundEvents, zero KiRenderHelper
primitive draws, zero hand emitted vertices in converted renderers, and
ability damage on the mod's own DbaDamage families everywhere except
the two documented exceptions in the still-vanilla list. Dual javac
audit sweeps show zero new error kinds on both classpaths. Details and
the honest remainder are below.

## Ownership audit

No violations. The 180 modified files are exactly the union of the 23
agents' owned files: 77 client renderers, 55 entities, 21 items, 27
effect classes. No shared file was edited by any weapon agent:
BespokeGeometry, LayeredRender, KiRenderHelper, DbaParticles,
DbaSounds, DbaDamage, LayeredParticles, AtmosphereServer, GroundScar,
VortexPhysics, FxEnvelope, DebrisEntity, BoltEntity, and
AtmospherePayload are all untouched in the diff. HEAD was still
2ef870b at merge start; no agent committed. Two comment-only notes:
the Azure and Ox agents cleaned a few pre-existing em dashes in header
comments of their own files (against the usual leave-old-text rule,
but harmless and confined to their own files); every other agent left
pre-existing header text alone.

## Central fixes applied by the merge

1. Effect-internal damage (deferred to the merge by the effects
   agent). Five effect classes dealt damage with vanilla sources as
   part of the effect itself. All five are now on DbaDamage families
   matching their source weapon's theme, amounts, tick intervals, and
   formulas byte-identical:
   - BleedingEffect: magic() to DbaDamage.tempest (Bansho Fan wind
     cuts, per the class javadoc).
   - DarkFadedEffect: magic() to DbaDamage.voidDmg (Hollow's Edge
     void corruption).
   - MeltingEffect: magic() to DbaDamage.voidDmg (applied by the
     Sickle of Sorrow's DimensionalRiftEntity).
   - SorrowRiftEffect: magic() to DbaDamage.voidDmg (Sickle of
     Sorrow, void bleeding).
   - DevilsHandsEffect: onFire() to DbaDamage.hellfire (Devil
     Trident's dark red fire).
   Effects store no attacker, so each source anchors on the afflicted
   entity with no attacker, the same no-attacker shape the Saber
   conversion used for its orphaned delayed hits. A comment at each
   site says so.
2. Nothing else needed a central code fix. The full-tree sweeps came
   back clean on the first merge run (see Verification), including
   the transient parse error the Bansho and Cursed Blade agents saw
   mid-flight (DarknessShatterRenderer hex literal and an
   ErasureChargeOrbRenderer parse error), both of which their owning
   agents had already fixed before finishing.

## Change request dispositions

Disposition rule applied: implement centrally only if small, purely
additive, and requested by multiple weapons or clearly high value.
Every request below already has a working in-weapon workaround that
shipped in this pass, and retrofitting a new builder across finished
renderers at merge time is exactly the regression risk the rule
exists to avoid. Result: none implemented at merge; all deferred and
recorded here as known toolkit limits for a future toolkit phase.

- Vertical shard or blade builder (Evil Spear, Ox King's Axe;
  Dabura's vertical blade note is the same gap). Deferred. Flat
  shardFan compositions stand.
- displacedDome height parameter (Z Sword, Azure, Power Pole,
  Dabura). Deferred. Pose Y-scale at call sites already reproduces
  the spectacle heights exactly.
- bladeArc band thickness or thinness parameter (Saber, Dimensional
  Sword, Z Sword's vertical variant). Deferred. Crescents read
  deeper than the spectacle versions; recorded in those reports.
- Octahedron, gem, or cage builder (Brave Sword, Dabura, Grand
  Sword, Cursed Blade). Deferred. Dome pairs and crossed fans stand.
- Needle builder (Devil Trident, optional). Deferred. Crossed
  crescent heads stand.
- Shard orbit builder (Sickle of Sorrow). Deferred. Two flat fans
  stand.
- Prism, shaft, or strand builder (Power Pole, Spike Flail, Cursed
  Blade's chain links). Deferred. Crossed tornRibbon compositions
  stand.
- Posed figure builder (Hollow's Edge). Deferred. The afterimage
  ghost is a plume column; limb articulation is the pass's biggest
  single visual trade, named in its report.
- brokenVortexBand phase and envelope parameters; glowFan envelope
  (Cursed Blade, Z Sword, Blaster Gun). Deferred. Callers rotate
  pose copies and bake envelope into alpha.
- branch() two-tone gradient variant (Dabura, Blaster Gun note).
  Deferred. Two calls per segment stand.
- Swept tube builder (Azure rush wake). Deferred. Helical branch
  strand shell stands.
- Flat ground ring builder (Katana, Devil Trident). Deferred.
  Squashed brokenVortexBand and glowFan stand.
- crackStar y parameter (Z Sword). Deferred. Pose translates stand.
- Judgment calls left as the agents made them: Sickle's rift
  collapse uses RELEASE_DARK rather than the guide's CHARGE_FULL;
  Katana's two ANVIL_LAND sheathing rings use BLADE_SHING and
  HIT_METAL rather than slam sounds; War Mace's ANVIL_HIT uses
  HIT_METAL. All are one-line flips if Carter prefers otherwise.

## Verification (all measured at merge, on the final tree)

Goal greps over the 180 converted files:
- ParticleTypes references: 0 lines.
- DustParticleOptions references: 0 lines.
- SoundEvents references: 0 lines.
- damageSources() lines: 2, both the documented ValorFieldEntity
  exception (still-vanilla list, item 4). Effect directory: 0.
- KiRenderHelper primitive calls (drawColoredBox, drawCylinder,
  drawDisc, drawSphere, drawHelix) in converted renderers: 0.
- Direct addVertex calls in converted renderers: 0. Every vertex in
  weapon renderers now comes out of BespokeGeometry or LayeredRender.
  Sampled surviving private helpers (Evil Spear ribbon and barb,
  Grand Sword drawArcLayer and drawWing, Cursed Blade drawAuraRibbons,
  Brave drawCross and sheet) emit no vertices; they position the
  pose stack and call builders, as their reports state.
- Unseeded client random in converted renderers: 0. Eight renderers
  (HollowAfterimage, KingsSlam, OxCharge, OxFissure, OxShockwave,
  ZCharge, ZGravitySlam, ZStanceAura) keep a java.util.Random
  placement stream, every instance constructed from the synced
  entity seed, used for placement only, never for geometry emission.
- Em dashes in added diff lines: 0.

Dual javac audit sweeps (unique file-plus-message error kinds):
- Baseline worktree at 2ef870b: 132 kinds on the 26.2 classpath,
  141 kinds on the 26.3 classpath.
- Final tree: 132 kinds on 26.2, 140 kinds on 26.3.
- comm -13 (kinds in the final tree but not the baseline): EMPTY on
  both versions. Zero new error kinds.
- comm -23: one kind removed on 26.3, the pre-existing
  VoidRiftRenderer mulPose(Quaternionf) error, fixed by the Hollow's
  Edge conversion. None removed on 26.2.
This is the javac audit method only; no Gradle build was run or is
claimed, per the standing waiver.

## What changed, per weapon (from the 23 reports)

- Katana: four renderers rebuilt (bladeArc crescents, tornRibbon
  splitter wall, brokenVortexBand charge rings, crossed ribbon aim
  beam); 10 vanilla sounds to the bank; both ability strikes to
  DbaDamage.ki. Crescent reads fuller than the spectacle strip.
- Z Sword: 20 local helpers deleted across five renderers, 45
  builder call sites; synced SEED added to all five entities; 12
  sounds swapped; wave, crush, and slam to gravity and crush.
  Monoliths are flat ribbons now, the report's named loss.
- Saber: 14 helpers and 108 hand vertices gone; slash, void tear,
  dodge spark, and line slash rebuilt on bladeArc, tornRibbon,
  branch, and brokenVortexBand; 9 sounds swapped; lunge to ki,
  dimensional damage to voidDmg, including the orphan fallback
  voidDmg(target, null).
- Brave Sword: 18 helpers gone; charge core octahedra became dome
  pairs, slash and cross slash on bladeArc, rush trail on crossed
  ribbons; CRIT particles to CRIT_SPARK; 8 sounds swapped; 12 damage
  constructions to valor.
- Grand Sword: 22 helpers gone; wave on bladeArc, field dome on
  displacedDome with vortex bands, shard on crossed ribbons; 15
  sounds swapped; 6 damage sites to valor, with the one explained
  exception in the still-vanilla list.
- Azure Dragon Sword: 18 helpers and 80 hand vertices gone across
  six renderers; synced render seeds on all five entities; entity
  rain emission removed (AtmosphereClient owns storm rain); 9 sound
  sites swapped; 6 damage sites to tempest.
- Dabura Sword: 22 helpers gone across five renderers; domain dome,
  wave, blade, charge, and shatter rebuilt on the toolkit; 8 sounds
  swapped (dark variants where the sites existed); 4 damage
  expressions to darkness.
- Cursed Blade: sky cracks rebuilt as the signature piece (branch
  mains, crackStar webs, glowFan shadow) from the synced seed; 31
  helpers gone, 53 builder call sites; 10 sound references swapped
  (thunder and zap to their dark variants); 5 damage sites to curse.
- Hollow's Edge: the 9 drawColoredBox calls and the whole tear
  system deleted; rift mouth is two displacedDome shells with
  accretion bands; afterimage is a plume column (limb trade noted
  above); 8 sounds swapped; 4 damage sites to voidDmg. Also fixed
  the pre-existing 26.3 mulPose kind in VoidRiftRenderer.
- Dimensional Sword: 16 helpers gone; slash and warp rift rebuilt;
  synced SEED on both entities; 11 sounds swapped; slash and both
  rift bursts to voidDmg. Item needed no edits.
- Spirit Sword: 22 helpers gone across three renderers; beam is six
  tornRibbon layers with glowFan apertures, cleave stands on
  bladeArc, impale rebuilt on crackStar, bands, and ribbon swords;
  5 sounds swapped plus a new BEAM_CHARGE at beam spawn; percent
  bonus hits converted to spirit with attacker attribution.
- Evil Spear: 16 helpers gone; charge, projectile, and impact
  rebuilt; client random removed; 7 sounds swapped; throw, hunt,
  and lunge damage to hellfire with the spear as direct entity.
- Devil Trident: 24 helpers gone; slam rebuilt on glowFan, crackStar,
  shardFan, and plume; prong and shard heads are crossed crescents
  via one shared composition helper; 6 sounds swapped plus the
  LIGHTNING_ZAP identity correction to TRIDENT_THROW; 3 damage
  sites to hellfire. SwarmHelper untouched.
- Sickle of Sorrow: 6 helpers gone; wave is six bladeArc layers,
  rift rebuilt on bands, ribbons, fans, and dome pairs; 7 sounds
  swapped; 4 damage expressions to voidDmg; Melting numbers
  untouched.
- Ox King's Axe: 25 helpers and 120 hand vertices gone across five
  renderers, 50 builder calls; block destruction now rides
  BLOCK_CHUNKS; 15 sound sites swapped; 9 damage constructions to
  crush, fissures included per the brief.
- Power Pole: 16 helpers gone across three renderers; whirlwind,
  extension, and impact rebuilt; 5 sounds swapped; spin hits to
  tempest and the poke to crush, per the audit's separation.
- Whis Staff: 8 helpers and 7 hand loops gone; the temporal rift is
  domes, bands, glowFan, branch streams, and ribbon clock hands from
  a new synced SEED; 8 sounds swapped; 5 damage calls to temporal,
  direct-plus-attacker used where the old source had that shape. The
  agent also fixed two pre-existing lambda capture error kinds in
  TemporalRiftEntity with a final alias. TimeShatter untouched.
- Bansho Fan: 19 helpers gone across four renderers; cyclone, gale,
  shockwave, and wind projectile rebuilt; 9 sounds swapped; cyclone,
  gale, barrage, and the melee bonus hit to tempest.
  BanshoShockwaveEntity untouched, so the WindChargeMixin contract
  is byte-identical.
- Blaster Gun: 20 helpers gone across three renderers; beam is
  crossed tornRibbons, bolt is ribbons with branch fins, the charge
  orb core is a displacedDome pair in a shard cage; synced SEED on
  all three entities; EXPLOSION particles to BLAST; 10 sounds
  swapped; 3 damage ternaries to ki.
- Lava Sword: cleave and eruption rebuilt on bladeArc, plume, and
  crackStar with new synced seeds; FLAME particles to the mod's own
  FLAME type; 5 sounds swapped; cleave and eruption damage to lava.
  LavaSwordGround deliberately untouched (still-vanilla list).
- Spike Flail: the 2 drawSphere calls and 5 helpers gone; orbit ring
  is direct brokenVortexBand layers, ball is dome shells with
  shardFan spikes, chain is tornRibbon spans; 5 sounds swapped plus
  GROUND_CRACK added on the crack tick; 4 damage sites to crush.
- War Mace: shell and crescent helpers gone; smash rebuilt on
  glowFan, bands, crackStar, plume, and displacedDome, rise on
  bladeArc layers; synced SEED on both entities; POOF to DUST_PUFF;
  4 sounds swapped; both entities' damage to crush.
- Status effects (27 files): all 29 DustParticleOptions constructions
  became DbaDustOptions on THEME_DUST with byte-identical colors and
  scales; ParticleTypes.BLOCK to BLOCK_CHUNKS in EarthShatterEffect;
  two types the standard's map does not name were swapped like for
  like and documented: PORTAL to VOID_SMOKE (Rifted, SorrowRift) and
  SMOKE to DUST_PUFF (FissureStun); four dead imports removed. No
  durations, amplifiers, tick logic, or colors changed. Effect
  damage was converted centrally at merge (above).

## Still vanilla, complete list, with reasons

1. Plain melee weapon hits keep the vanilla player attack source.
   By design (BESPOKE-STANDARD rule 4): that hit is the game's
   weapon hit; the item attack damage attribute, attack speed,
   knockback, and enchantment pipeline key off it.
2. Damage still resolves through the game's health, armor, and
   resistance pipeline internally. The pass changed damage types,
   sources, and death messages, not the engine's resolution.
3. Lava Sword's ground burn places real vanilla lava and fire
   BLOCKS as world material. LavaSwordGround.java is untouched by
   this pass, so the spectacle crash-safe behavior (SavedData
   mirror, deterministic placement, shutdown restore) is
   unregressed by construction.
4. ValorFieldEntity keeps 2 vanilla damageSources() lines for its
   suspended projectile collision (indirectMagic or mobProjectile
   with the projectile's original owner, generic fallback). That
   damage belongs to the suspended projectile, not the field;
   re-typing it to valor would misattribute Ki blast kills to the
   field's caster. The field's own pressure damage is valor. The
   reason is also commented at the site.
5. Damage type tag behavior differs from vanilla for conversions
   whose old source was magic, indirectMagic, generic, onFire,
   hotFloor, or lightningBolt. The 12 custom types carry no vanilla
   damage_type tag memberships (Phase 1 created no tag files), so
   they do not inherit: bypasses_armor (magic, generic, onFire),
   no_knockback (magic, generic, onFire, hotFloor, lightningBolt),
   witch_resistant_to (magic, indirectMagic), or is_fire immunity
   under Fire Resistance (onFire, hotFloor). Amounts, scaling, and
   exhaustion are unchanged (the JSONs mirror the vanilla shapes);
   armor reduction, per-hit knockback, witch resistance, and fire
   resistance interaction on those specific converted hits are not
   bit-identical to vanilla. Conversions from playerAttack,
   mobAttack, and mobProjectile are unaffected on armor. Fixing
   this needs per-family tag files, a Phase 1 foundation decision,
   and is recorded here rather than improvised at merge.
6. Ki techniques, the dragon wish ritual, mobs, menus, and
   dimension ambience are out of scope and untouched. KiRenderHelper
   primitive methods remain in the helper for those callers; weapon
   renderers never call them.
7. TimeShatterEntity and its renderer (Whis Staff family, owned by
   no Phase 2 agent) keep their pre-existing spectacle state: 18
   tick life, no fade-in, and mismatched random seeds between the
   dial and shard layers. Untouched by this pass.
8. entity/LavaSwordGround.java keeps its pre-existing 26.3 baseline
   error kind (BlockState.blocksMotion, removed in 26.3). Untouched,
   as item 3 records.
9. Storm presentation: AtmosphereServer never reads or writes
   WeatherData (Phase 1); rain, thunder, and gloom are rendered
   client side by AtmosphereClient, and weapon callers are
   unchanged. Nothing vanilla remains in the storm path.
10. Deferred toolkit limits (change request dispositions above):
    no vertical shard, needle, octahedron, cage, prism, strand,
    posed figure, or swept tube builders, and no height, thickness,
    phase, or envelope parameters on the existing builders. Weapon
    visuals use the documented workarounds until a toolkit phase
    adds them.

## Unverified

In-game look is unverified by construction: no Gradle build can run
in this VM and none is claimed. Every report carries an honest worse
check; the silhouettes most changed from the spectacle pass are the
Hollow's Edge afterimage (limbs to plume column), the Z Sword and
stance monoliths (solids to flat ribbons), the Azure rush wake (tube
to braided strands), and the Dabura wave (narrower crescent). Carter's
PC build and look are the first real check, as with prior passes.

## Addendum (Nov 3, 2026): damage type tags added

Item 5 above is now closed. Carter approved the tag files, and
they are in: six JSONs under data/minecraft/tags/damage_type/
(bypasses_armor, no_knockback, witch_resistant_to,
bypasses_wolf_armor, avoids_guardian_thorns, is_fire), each with
"replace": false. Void, spirit, and temporal carry the vanilla
magic combat profile; tempest carries the storm magic profile
(without is_lightning); hellfire is fire-typed with armor bypass
and no knockback. Ki, valor, crush, lava, darkness, curse, and
gravity received no tags: their replaced sources were
player-attack class or physical, and the audit says so per family
rather than forcing membership. Full archaeology (site and token
counts per family from the 57902e1..dc2cd1f diff, vanilla
memberships read from the 26.2 jar), every compromise, and the
list of tags considered and excluded are in
audits/bespoke-damage-tags.md. Data only; no Java changed.
