# DBA No-Shapes Final Audit

This is the final integration audit for the no-shapes realism pass.
It covers Phase 1 (the fresh inventory, the binding standard, and the
material foundation, committed as ad22b32), all 31 Phase 2 owner
conversions, the shader pattern deliverable, the destruction
foundation file, the rebuilt lava block art, and the merge work
recorded below.

Commit chain covered by this audit:

- d9f8a90: the follow-on batch finish, the last state pushed before
  this pass.
- ad22b32: no-shapes Phase 1 (audit, standard, material foundation),
  committed locally and never pushed on its own.
- The final integration commit containing the files reviewed here sits
  directly on top of ad22b32 and carries ad22b32 to origin with it.

Verification in this audit is the established javac audit method
only. No Gradle build was run, and no Gradle pass is claimed. No
in-game look was verified in this VM. Carter's PC build and test
remain the first real look at every converted effect.

## Executive result

The pass is integrated as one tree with no ownership violations
found. The final tree has:

- Zero BespokeGeometry references anywhere in src. The class is
  deleted (see the deletion decision below).
- Zero LayeredRender references anywhere in src. The class is
  deleted.
- Zero direct addVertex effect drawing and zero KiRenderHelper
  primitive or vertex calls in effect code. The surviving addVertex
  files are the physical model classes, classified below.
- Every effect surface inventoried in audits/no-shapes-audit.md
  converted to its named material through the shared Phase 1
  foundation (DbaParticles material types carrying
  DbaMaterialOptions, behavior profiles in DbaMaterialParticle,
  emission through client/fx/MaterialEmitters), or carried in this
  audit as an intentional remainder with a reason.
- Dual javac sweeps with zero new error kinds on both Minecraft
  versions: 132 kinds on 26.2 and 138 kinds on 26.3, kind sets
  identical to the fresh ad22b32 baseline measurement (the comm -13
  output is empty on both, and no baseline kind disappeared either).
- All 562 resource JSONs parse, all 144 particle texture references
  resolve to files on disk, and every block model texture reference
  in the rebuilt lava set resolves.
- git diff --check clean, and zero em dashes in added lines across
  the whole integration diff.

The Phase 1 inventory counted 684 BespokeGeometry builder calls in
effect code across 90 files, 44 LayeredRender geometry calls in 29
files, 40 direct addVertex calls, and 30 KiRenderHelper.vertex
calls. All four counts are zero in effect scope in the final tree.

## Ownership check

Every changed file in the integration diff was matched against the
owner reports in audits/no-shapes-report-*.md plus the three side
deliverables (audits/no-shapes-shader-pattern.md,
audits/destruction-expansion-plan.md,
audits/lava-blocks-art-report.md). The reviewed diff is 207 files;
this audit is the one additional file in the commit. Each reviewed
file belongs to exactly one owner, with one known and verified
exception:

- effect/FissureStunEffect.java and effect/OxBraceEffect.java carry
  presentation edits from both the Status Effects owner and the Ox
  King's Ax partial diff. The merge read both diffs together. The
  combined result is one particle options swap per file, colors
  preserved exactly (FissureStun DUST 0xEBE8E0 replacing the DUST_PUFF
  stream at the same count, spread, and tick gate; OxBrace DUST
  0xFF4500 at the same 1.2 scale replacing DbaDustOptions). No damage
  line, tick gate, count, or position changed in either file. The
  combined state is coherent and presentation-only; no central fix
  was needed.

Other cross-checks performed at the merge:

- The 27 changed classes under effect/ are exactly the Status
  Effects owner's assigned set.
- item/WhisStaffItem.java and entity/TemporalRiftEntity.java diffs
  are comment text only (the Whis Staff owner's assigned comment
  updates); no code line changed.
- entity/FriezaSoldierEntity.java, entity/SaibamanEntity.java, and
  client/fx/StaggerFx.java are the Mob Ability Effects owner's whole
  footprint, presentation only.
- tools/gen_lava_block_textures.py and the magma_crust and
  mini_volcano blockstates, models, and textures are the Lava Block
  Art agent's whole footprint; no Java was touched by that work.
- util/PermanentDestruction.java is the Destruction agent's only
  code footprint and is foundation only. It is not wired into any
  weapon in this pass; the wiring is a later pass that follows
  audits/destruction-expansion-plan.md.

## Per-owner outcomes

All 31 Phase 2 slots completed and filed reports. BespokeGeometry
builder calls converted to zero, per owner:

- Katana: 36 to 0. SwiftCrescent, KatanaCharge, KatanaAimGuide.
- Z Sword: 44 to 0. Charge, stance aura, gravity slam, shockwave,
  delayed launch.
- Saber: 35 to 0. Slash, dodge spark, void tear, dimensional line
  slash.
- Brave Sword: 41 to 0. Charge, slash, cross slash, shockwave, rush
  trail.
- Grand Sword: 29 to 0, plus ValorFieldRenderer, which the Grand
  owner also converted (its filename does not carry the Grand
  prefix; the merge verified it is in the integrated diff).
- Azure Dragon Sword: 31 BespokeGeometry and 7 LayeredRender calls
  to 0 across six renderers.
- Dabura's Sword: 43 to 0. Darkness blade, charge, domain, shatter,
  wave.
- Cursed Blade: 48 to 0, the largest single weapon count. Chains,
  ground shatter, lightning, telegraph, and the SkyCracks sky tear.
- Hollow's Edge: 27 to 0. Afterimage (a dissolving soul and darkness
  body-volume presence, no body mesh), charge, rush trail, void
  rift, void slash.
- Dimensional Sword: 25 to 0. Rift, slash, warp rift.
- Spirit Sword: 35 to 0. Cannon beam, cleave, impale. The merge
  notes the owner's verification that the beam's wall occlusion read
  was not regressed by the conversion.
- Evil Spear: 26 to 0. Charge and projectile renderers.
- Devil Trident: 24 to 0. Slam shockwave, prong, shard. The
  replacement owner also fixed three color constant slips the dead
  agent's partial diff introduced (two channel slips and a core
  color that did not match HEAD) and verified the DevilsHandsEffect
  change as presentation-only.
- Sickle of Sorrow: 14 BespokeGeometry and 2 LayeredRender calls to
  0. Dimensional rift and sorrow slash.
- Ox King's Ax: 47 to 0. King's slam, charge, fissure, shockwave,
  stance aura. The replacement owner fixed five double-to-float
  compile errors in the dead agent's OxShockwaveRenderer partial and
  wired a computed crest height value the partial had left unread.
- Power Pole: 26 to 0. Extension, impact, whirlwind.
- Whis Staff: 14 to 0 in TemporalRiftRenderer, plus comment-only
  updates in WhisStaffItem and TemporalRiftEntity.
- Bansho Fan: 28 to 0. Cyclone, gale, shockwave, wind projectile.
  Constructor and mixin compatibility verified untouched.
- Blaster Gun: 22 to 0. Bolt, erasure cannon beam, and the erasure
  charge orb, which is now a churning plasma cluster.
- Lava Sword: 20 to 0 across five renderers, presentation only; the
  block resources were the separate art agent's scope.
- Spike Flail: 11 to 0. Spin and swing.
- War Mace: 11 to 0. Smash and rise, converted fresh by the
  replacement owner after confirming no partial work existed.
- Shared weapon systems: SwingTrailRenderer (swing-driven material
  emission, per-weapon trail profiles intact) and BoltRenderer
  (seeded branch path math byte-identical, rendered as layered glow
  emission and sparks along the path). 22 geometry calls to 0.
- Status Effects: 27 effect classes converted from THEME_DUST and
  VOID_SMOKE presentations to the material family at the same spawn
  points, counts, deltas, speeds, and tick gates. No damage or
  tick-gate line changed.
- Ki Techniques: all seven ki renderers (beam, blast, disk, laser,
  spiral beam, barrier, explosion) converted. One flag carried to
  the intentionally-left list below (barrier hit ripples).
- Wish Ritual: WishRitualVisuals converted.
- Mob Ability Effects: Frieza Soldier and Saibaman presentation
  plus the boss stagger burst in StaggerFx.
- Storms and AtmosphereClient: AtmosphereClient and the
  OtherworldCloudParticleManager cloud conversion completed. The
  replacement owner removed an unsupported plasma thunder glow from
  the dead agent's partial and restored the exact HEAD thunder
  sound expression; thunder scheduling, the gloom curve, the
  payload, and all server behavior are identical to HEAD.
- TimeShatter: TimeShatterRenderer converted (glass dust suspension,
  36 tick life, synced SEED and spawn compatibility kept). Gravity
  wiring at the merge is recorded under change requests.
- Evil Spear Dread: DreadGazeRenderer converted. The shroud is
  DARKNESS and SMOKE emission; the eyes ride the detailed eye texture
  as textured quad particle sprites. The blink squash became alpha
  and scale modulation on the identical openness curve, because a
  particle sprite cannot squash vertically. MeltingOverlayMixin was
  not touched.
- Forms and Player Auras: AuraRenderer converted. Pulse logic
  byte-identical; per-family material colors are sourced from form
  data and grade tints.

No owner reverted any piece, and no owner reported a mechanic,
damage number, cooldown, cost, timing, or synced data change. The
honest worse-check flags the owners filed are carried at the end of
this audit, unedited in substance.

## Grep gates (fresh at the merge)

Measured on the integrated tree after the deletion below:

1. BespokeGeometry references in src: 0 (the file is deleted; gate 1
   of the standard is met by absence, not by an empty call list).
2. LayeredRender references in src, including drawVortexBands and
   drawAnchorGlow: 0 (the file is deleted).
3. Direct addVertex calls in effect code: 0. The surviving addVertex
   files are all physical model infrastructure, classified here:
   render/weapon/model (BoxPart, BladePart, RingPart, CylinderPart,
   ConePart, ModelPart; the authored weapon models),
   render/model/ObjMesh and render/model/DbaHairRenderer (authored
   mesh and hair models), render/AlphaModulatingVertexConsumer (a
   vertex consumer wrapper used by model rendering), and
   render/ki/KiRenderHelper itself (the definition site of its
   vertex helper, see the remainder note below).
4. KiRenderHelper.vertex and KiRenderHelper primitive calls
   (drawColoredBox, drawCylinder, drawDisc, drawSphere, drawHelix)
   in effect code: 0. KiRenderHelper itself survives because its
   render types, its WHITE_TEXTURE, and its color unpack helpers
   are legitimately used by DbaShaders, ProceduralWeaponRenderer,
   RaceFeatureLayer, and HollowAfterimageRenderer (color helpers
   only). Its five primitive definitions and its vertex helper have
   zero call sites in the final tree (their only caller was
   BespokeGeometry) and are carried as a stated remainder, per the
   standard's gate wording.
5. No new private geometry helper, private particle system, or
   private texture style appears in any owner's files. Owners built
   only on the Phase 1 foundation; the per-entity emitter caches
   several owners hand-rolled are caches of shared Emitter
   instances, not private systems.

## Deletion decision

BespokeGeometry.java and LayeredRender.java are DELETED in this
integration. The condition the standard set is met exactly: zero
effect calls remain, and the merge grep shows the two classes were
each other's only remaining reference (LayeredRender's builders
called BespokeGeometry; nothing else in the tree referenced either
class, in code, in resources, or by string). KiRenderHelper is not
deleted, for the live uses listed in gate 4 above; its uncalled
primitives stay as a documented remainder inside a live class
rather than as two dead classes.

## Change requests: decisions

Filed across the owner reports. Decided at the merge:

APPLIED, the lifetime factor and gravity scale mechanism
(Katana, Brave Sword, Saber, and Devil Trident filed the lifetime
factor; TimeShatter filed the gravity scale):

- DbaMaterialOptions gains two opt-in components, lifetimeFactor
  and gravityScale, both defaulting to 1.0. The data codec reads
  them as optional fields with a 1.0 default, so existing encoded
  forms still decode. The four-argument constructor and the of()
  factory are unchanged, and all 45 existing of() call sites and
  every server-side sender behave exactly as before.
- DbaMaterialParticle honors both: the rolled lifetime is
  multiplied by lifetimeFactor (minimum 1 tick), and the profile
  gravity is multiplied by gravityScale.
- MaterialEmitters.Preset gains matching fluent modifiers,
  withLifetimeFactor and withGravityScale, which only affect
  material presets; fixed presets (FLAME, EMBER, ASH, SPARK,
  BLOCK_CHUNKS) are untouched by design.
- Wired cases, each chosen because the host is short, the tail was
  measured against the host window in the owner's own report, and
  no timing or mechanic contract is affected (the factors change
  only how long already-emitted material particles live, and
  emission itself stays envelope-gated):
  - TimeShatter: all eight glass dust presets in
    TimeShatterRenderer carry gravity scale 0.1 (profile gravity
    0.16 becomes 0.016), so the suspension field drifts instead of
    raining. This is the exact fix the owner requested.
  - Katana SwiftCrescent (10 tick host): plasma streams and the
    strike burst at lifetime factor 0.6, wake vapor at 0.45. The
    vapor tail drops from up to 54 ticks to up to about 24.
  - Saber slash (10 to 20 tick hosts): the continuous darkness
    stream at 0.45 (62 tick worst case becomes about 28) and the
    glass dust streams at 0.6, in SaberSlashRenderer only.
  - Brave cross slash: the soul streams at 0.5 (70 tick worst case
    becomes 35) and the vapor stream at 0.6.
- Deferred wirings, with reasons:
  - KatanaCharge and BraveCharge: both are held charge states.
    A factor applies to every particle emitted during the hold,
    not just at release, so wiring it would thin the charge's own
    body read for its whole duration to fix a tail that only
    occurs on release or cancel, which the preset cannot observe.
    The mechanism is in place if Carter wants a tuning follow-up.
  - SaberVoidTear, SaberDimensionalLineSlash, SaberDodgeSpark:
    host lifetimes vary by cast, and the void tear is a persistent
    rift whose haze is part of its idle read. Deferred rather than
    guessed.
  - DevilSlam and the trident shard and prong: the slam's only
    long profile is its brimstone smoke, which is the intended
    lingering aftermath of a ground slam under the standard's slam
    method, and the shard and prong read their plasma as a flight
    trail. Deferred.

DEFERRED, the shared ground crack and scorch decal system (filed
by Dimensional Sword, Whis Staff, Cursed Blade, Dabura's Sword,
Z Sword, and Devil Trident): a decal layer is a new render system
(surface projection, lifetime management, a crack texture set), not
a merge-sized change, and several effects currently carry their
crack reads as ground-sampled particles that a rushed decal system
could make worse. It is the strongest candidate for the next
foundation pass. Not built here.

DEFERRED, the shared per-entity emitter registry and dynamic or
supplier-based Source factories (filed in some form by Katana,
Dimensional Sword, Spirit Sword, Cursed Blade, Blaster Gun, Evil
Spear, Z Sword, and Devil Trident): every owner's hand-rolled
cache works and is verified; replacing all of them at the merge
would churn 30-plus files for a code cleanliness gain with real
regression risk and no visual change. Deferred as a refactor, not
a defect.

DEFERRED, the BossStaggerPayload theme color extension (Mob
Ability Effects): it changes a network payload's shape, which is a
compat decision, to fix a cosmetic read (the stagger burst is
neutral warm plasma because the payload carries no attacker or
theme). Small, but it does not belong in a presentation merge that
promises identical synced data. Deferred to Carter's call.

NOT APPLIED, the Emitter rate-scale setter (Brave Sword): the
envelope route the owner shipped works and stays inside the
budget, and adding a second intensity path into every Emitter at
merge time risks double-scaling in effects that combine both.
Deferred with that reason. (The honest summary for the three small
optionals as a group: none of them were safe enough to land blind
in the merge.)

NOT APPLIED, the Preset.spark factory (Spike Flail): the owner
already rides the existing SPARK type through Preset.fixed, which
is the foundation's intended route for fixed types. A spark alias
would add API surface for no behavior change. Declined, not
deferred.

## Shader pattern adoption

DbaShaders and its two core shaders (assets/dragonblockarcanedba/
shaders/core/no_shapes.vsh and no_shapes.fsh) are in the tree,
documented in audits/no-shapes-shader-pattern.md. Adoption in this
merge: NONE, stated plainly. After the owner conversions, no effect
in the integrated tree still draws a film surface: the final grep
shows no effect renderer holding a VertexConsumer or submitting
custom geometry at all, so there is no genuine surface to wire the
dissolve pattern into, and the merge did not resurrect geometry to
justify wiring it. The pattern stands as the documented, verified
route for any future effect that honestly needs a film surface, with
two recorded constraints from its own report: vertex alpha is the
life input, so an adopting surface must drive its envelope toward
1.0 at peak or sit partially eroded, and local scene refraction is
not part of it (the frame graph evidence in the shader report shows
every load-bearing link is private or absent on at least one
version; distortion stays with the DbaPostFx post shockwave chain).
Refraction remains skipped under Carter's certainty ruling.

## Resources and hygiene

- 562 resource JSONs under src/main/resources and
  src/client/resources all parse, including the rebuilt
  magma_crust and mini_volcano blockstates and models and the two
  new shader JSON-adjacent assets.
- All 144 particle texture references in the particle JSONs resolve
  to PNG files under textures/particle.
- All block model texture references in the rebuilt lava models
  resolve.
- git diff --check on the full integration diff: clean (one blank
  line at EOF in TimeShatterRenderer, introduced before the merge,
  was trimmed centrally).
- Em dash scan of added lines across the integration diff: zero.
  One pre-existing em dash in the DevilsHandsEffect HEAD javadoc is
  not an added line and was left untouched, as recorded in the
  Devil Trident owner's report.

## Dual javac sweeps

Method: the established sweep harness (javac over both source sets,
unique (file, message) error kinds) against a clean baseline
worktree at ad22b32, measured fresh at this merge.

- Baseline at ad22b32: 132 kinds on the 26.2 classpath, 138 kinds
  on the 26.3 classpath. (Identical counts to the d9f8a90
  measurement, so Phase 1 itself introduced no new kinds.)
- Integrated tree: 132 kinds on 26.2, 138 kinds on 26.3.
- comm -13 (new kinds) is EMPTY on both classpaths. comm -23 is
  also empty on both: the kind sets are identical, so the toolkit
  deletion and the merge edits removed no baseline kind and added
  none.
- A targeted compile of every file the merge itself edited
  (DbaMaterialOptions, DbaMaterialParticle, MaterialEmitters,
  TimeShatterRenderer, SwiftCrescentRenderer, SaberSlashRenderer,
  BraveCrossSlashRenderer) shows zero errors in those files on
  both classpaths.
- The baseline worktree was removed and pruned after the sweeps.
- No Gradle build was run. The compile check for this mod remains
  waived in this VM by Carter's standing instruction; the javac
  audit is a sweep, not a build, and is not presented as one.

## Still geometry and intentionally left

Everything below is in the final tree on purpose. None of it is an
unconverted effect hiding from the gates.

- Physical authored models: all 22 weapon models, detailed
  projectile models, debris block models, blocks (including the
  rebuilt magma crust and mini volcano), mob and race models, the
  dragon balls, and the Flying Nimbus. The standard keeps physical
  objects as authored models with detailed textures; only their
  energy, fire, smoke, light, and magic were in scope, and those
  surfaces are converted.
- The weapon model part classes under render/weapon/model and the
  mesh infrastructure (ObjMesh, DbaHairRenderer,
  AlphaModulatingVertexConsumer): model rendering, not effects.
- The DreadGaze eyes: seven pairs of detailed red eyes on the
  generated eye texture, submitted as textured quad particle
  sprites. A detailed textured sprite of a physical thing (an eye)
  is inside the standard's scope line; the shroud around them is
  converted material.
- MeltingOverlayMixin: the melt status effect's screen vignette is
  a screen-space overlay, not a world effect surface. Untouched by
  this pass.
- KiRenderHelper's five primitive definitions and vertex helper:
  zero call sites, retained inside a live class as a stated
  remainder (gate 4 above).
- DbaPostFx and the post shockwave chain: untouched; they remain
  the distortion route, per the standard.
- KiBarrier hit ripples: the Phase 1 audit names hit ripples as the
  barrier's hit feedback, but KiBarrierEntity syncs only COLOR,
  USED_PERCENT, OWNER_ID, and SEED. There is no synced hit field,
  so the renderer cannot key a ripple to a real hit without a
  synced data change, which this presentation pass forbids itself.
  Deferred as Carter's call: add a synced hit tick or position to
  the barrier entity in a mechanics pass, and the ripple becomes a
  small renderer follow-up.
- PermanentDestruction.java: present, compiled, unwired (see
  ownership above).
- Plain melee hits keep the vanilla player attack source, and all
  damage lands through the game's health and armor pipeline
  internally, exactly as before this pass. This pass changed no
  damage number, damage type, cooldown, Ki cost, timing, duration,
  hitbox, movement value, or synced data field anywhere.
- The lava damage family deliberately carries no is_fire tag
  membership. That predates this pass and remains Carter's design
  call, recorded in the follow-on audit and unchanged here.
- Split source sets stay clean: no file under src/main references a
  client-only class. DbaMaterialOptions and DbaParticles remain
  common; DbaMaterialParticle, DbaParticleProviders, and
  MaterialEmitters remain client.

## Honest worse-check flags (carried from the owner reports)

These are the trades the owners themselves flagged, restated so
Carter can judge them on his PC test. Nothing here was hidden, and
nothing in this list was judged by its owner as worse overall than
the geometry it replaced.

- Cursed Blade SkyCracks is the softest conversion in the pass.
  The sky tear is dense plasma emission with no decal or mesh
  behind it; it is the first thing to check in game.
- Trident Shard lost its crossed-crescent silhouette and axial roll
  read. The audit's no-shard-mesh rule required it (the shard was a
  glowing mesh, not a detailed physical model); the flight read
  survives as cluster plus wake, and the Devil Trident owner flags
  it as the item most likely to be judged on the PC test.
- Azure Dragon Sword's lightning telegraph boundary and Devil
  Slam's seal edge are now density edges rather than hard rings.
  The radius math is unchanged and emission stays inside the hit
  radius, so the telegraphs stay honest; they read softer.
- Material afterglow tails remain possible on short effects that
  were not wired at the merge (the deferred list under change
  requests names them and why). Emission stops with the host
  everywhere; only already-spawned particles finish their profiles.
- Several effects read thinner at low effects-density settings or
  at long distance. That is the standard's own trade (emission is
  density-scaled and distance-scaled by design), stated here so it
  is a known property, not a surprise.
- TimeShatter's color split is subtle (a 0.055 scale offset between
  two dust streams reads as a shimmer, not a dramatic chromatic
  tear), per its owner's report.
- The boss stagger burst is neutral warm plasma, not the
  attacker's theme color (the payload reason, deferred above).
- In-game look of every conversion is unverified. The first real
  judgment is Carter's PC build and test.
