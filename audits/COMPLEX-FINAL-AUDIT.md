# DBA Complex Effects Redo: Final Audit

Merge of the complex effects redo, on top of Phase 1 commit 9178eac.
Base for the whole pass: d74f13a (destruction expansion). Binding
standard: audits/COMPLEX-STANDARD.md. Work order: audits/complex-audit.md.
Per-slot reports: audits/complex-report-<slug>.md (31 reports).

Why this pass exists, in one line: the no-shapes pass left every effect
as soft round glowing particles with no structured pieces, and Carter
rejected that read. This pass rebuilt every effect as a layered
composition: authored complex pieces (ComplexGeometry), irregular
complex particle sprites (SHARD_CHIP, SPARK_STREAK, WISP_CURL,
EMBER_FRAGMENT, ASH_FLAKE), and the existing material emission demoted
to filler. Themes, colors, damage numbers, cooldowns, costs, timing,
mechanics, and synced data fields are identical to HEAD throughout.
The d74f13a destruction wiring is untouched.

## Executive result

- All 31 owner slots landed and were verified at the merge. 124 owner
  Java files plus the merge's own central changes (below) and 31
  reports are in this commit.
- All six merge gates in COMPLEX-STANDARD.md pass, checked fresh (see
  Composition gates). The two documented audit exemptions hold
  byte-for-byte.
- The pass's two double-bolt flags (shared Bolt, CurseLightning) are
  fixed at the root by the merge's one foundation change: the
  branchedLightning polyline overload. Geometry and emission now walk
  the same seeded path in both renderers.
- Dual javac sweeps: baseline at 9178eac measured fresh at 132 error
  kinds (26.2) and 138 (26.3); the integrated tree measures 132 and
  138; comm -13 is empty on both, and comm -23 is empty on both, so
  the kind sets are identical. No Gradle build was run or claimed;
  the in-game look is unverified until Carter's PC test.

## Ownership check

- Every changed Java file was matched to exactly one owner slot across
  the 31 reports (name matching in the reports is loose in places, so
  the check was closed by direct grep per file; the last four apparent
  gaps, OxStanceAuraRenderer, SorrowSlashRenderer,
  TemporalRiftRenderer, and TimeShatterRenderer, are all named in
  their own slot reports).
- Foundation stayed untouched by owners: ComplexGeometry,
  ComplexTextures, DbaShaders, DbaParticles, DbaGlowParticle,
  MaterialEmitters, DbaMaterialOptions, DbaMaterialParticle,
  FxEnvelope, PermanentDestruction, and LavaField all show an empty
  diff before the merge's own central changes. The only foundation
  file changed in this commit is ComplexGeometry, changed centrally
  in step 3 of the merge (the polyline overload and the crackFan
  javadoc note), plus the two renderer rewires in BoltRenderer and
  CurseLightningRenderer.
- The FissureStunEffect and OxBraceEffect overlap from the no-shapes
  pass did not recur. The Ox King's Ax report states in its own header
  that it did not edit either file; both diffs are single-authored by
  the Status Effects owner, +7 lines each, particle-send additions
  only, colors and tick gates unchanged.
- Exemptions verified byte-for-byte at the merge: BleedingEffect.java
  and MeltingEffect.java are identical to HEAD (the deliberate class S
  exemption: no complex sprite silhouette matches fluid droplets or
  corrosive vapor), and OtherworldCloudParticleManager.java is
  unmodified (a background sky field, not an effect body).

## Composition gates (fresh at the merge)

1. Pieces plus sprites per effect. All 92 class M and class L rows of
   audits/complex-audit.md were checked against the tree: every class
   M and L renderer holds at least one ComplexGeometry builder call
   and at least one complex sprite type emission in its own file.
   Zero failures. Class L two-builder rule: class L is 25 effects
   (23 table rows plus SwingTrail and Bolt, classed L in the audit's
   prose); 19 use two or more builder types. The six single-builder
   class L effects are the ones the audit's own work order specifies
   as a single builder, each run at full budget and stated in its
   report: AzureTornado (tendrilFan at its shipped strand and sample
   maximum), BanshoCyclone (tendrilFan, six strands, the full fan
   budget), KiBarrier (six tendril lashes at full segment and count
   budget, 2304 of 6000 vertices), DimensionalSlash (three full
   tendril strands as the slash body), SwingTrail (tendril chains
   along the recorded swing path), and Bolt (branchedLightning, whose
   subject is the full strike path itself).
   Two audit-shaped exceptions, both recorded in the audit itself:
   AtmosphereClient (class M system) carries no pieces of its own
   because its one geometric element, thunder, is the shared Bolt
   owner's geometry, converted in the shared systems slot; and the
   status effects are class S, which requires the sprite layer only.
2. No particles-only bodies, no PLASMA dominance. Every owner report
   states the filler demotion explicitly and the merge spot-checked
   the flagship rows: in each sampled class M and L effect the pieces
   layer carries the silhouette and PLASMA emission continues at its
   pre-pass rates as filler only.
3. No plain primitives. Fresh greps: BespokeGeometry references 0,
   LayeredRender references 0 (both stay deleted), KiRenderHelper
   drawColoredBox, drawCylinder, drawDisc, drawSphere, and drawHelix
   call sites 0 outside KiRenderHelper's own definitions. The only
   addVertex users outside ComplexGeometry are the physical model
   paths (weapon model parts, ObjMesh, DbaHairRenderer,
   AlphaModulatingVertexConsumer), which are physical objects and
   out of the gate, exactly as in prior passes.
4. Shader discipline. Submit routing counted fresh across owner
   files: ComplexGeometry.submitEnergy 111 calls, submitSolid 28,
   submitVeil 11 (the remaining unqualified submit calls are inside
   ComplexGeometry's own helper overloads). Energy and fire pieces
   go through submitEnergy or submitVeil; solid shard and plate
   pieces go through submitSolid. Peak alphas on shader pieces are
   0.85 to 1.0 per the reports; no owner fakes faintness with a low
   constant alpha.
5. No private toolkits. No owner file defines its own geometry
   builder, particle system, or texture style; every piece goes
   through ComplexGeometry and every complex sprite through the
   registered DbaParticles types and MaterialEmitters. Variants were
   filed as change requests instead (decisions below).
6. Worse checks. Every report carries an honest worse check; no
   owner shipped a known-worse conversion silently. The flags are
   carried into the PC-test review list at the end of this audit.
   The merge reverted nothing: the two flags that named a fixable
   defect (the double bolts) were fixed centrally instead.

## The polyline fix (merge centerpiece)

Change request 1 of both the shared systems report and the Cursed
Blade report, applied: ComplexGeometry.branchedLightning gained an
overload that consumes a caller-supplied trunk polyline, with
caller-supplied fork polylines drawn exactly as given when present.

- Implementation: the generating overload's fork generation,
  sub-branch generation, and two-pass draw were extracted into shared
  private helpers (addForks, addSubBranches, drawBoltLines) that both
  overloads call. The generating overload's seeded behavior is
  unchanged: the refactor preserves its rng consumption order and the
  draw math verbatim. The overload draws the supplied trunk with no
  displacement of its own; supplied branches are drawn at fork order
  with seeded sub-branches grown off them; when no branches are
  supplied, seeded forks and sub-branches grow off the supplied trunk
  by the same machinery. Only taper, per-segment width jitter, the
  sheath and core crossed ribbon passes, and the shader life remap
  are applied to caller points.
- BoltRenderer rewired: buildSegments (BoltEntity's seeded walk,
  byte-identical, in the entity's shared data path) is split back
  into polylines in submit: the first twelve segments chain into the
  trunk, the remaining segments chain into the fork polylines by
  endpoint equality, and both are fed to the overload. The geometry
  bolt and the plasma corona now share endpoints and path by
  construction.
- CurseLightningRenderer rewired: the emission trunk polyline
  (boltPath, unchanged) is computed once and used for both layers;
  the geometry call feeds its local-space copy to the overload with
  no supplied branches, so the builder grows its seeded forks and
  sub-branches off the exact emission line. Endpoints coincide with
  the old geometry endpoints (cloud point and travelling ground end).
- Verification: targeted javac over ComplexGeometry.java,
  BoltRenderer.java, and CurseLightningRenderer.java on both
  classpaths: zero errors in the three files; the only errors in the
  closure are the baseline kinds (4 on 26.2, the documented set on
  26.3). The full dual sweeps below confirm no new error kinds
  anywhere. The double-bolt worse flags for Bolt and CurseLightning
  are therefore re-stated as fixed by merge; their in-game read joins
  the PC test like everything else.

## Change request decisions

Applied:

- branchedLightning polyline overload (shared systems, Cursed Blade):
  applied, above. This also retires the Bolt anchoring approximation
  and the CurseLightning parallel-path risk at the root.
- crackFan radius documentation (Azure): applied as a javadoc note on
  ComplexGeometry.crackFan stating the overshoot arithmetic (each
  step is radius over segments times a seeded 0.60 to 1.35 factor, a
  near-straight roll reaches about 1.35 times the nominal radius,
  forks run a further 0.45 radius) and directing telegraph callers to
  scale down (Azure uses 0.78) instead of expecting a clamp. Comment
  only; zero behavior change.

Deferred, with reasons (owners shipped working workarounds for every
one; rewriting the affected slots at merge time risks more than it
gains, and none blocks):

- Converging tendrilFan variant (gather form): filed by Katana,
  Brave Sword, Dabura's Sword, Cursed Blade, Hollow's Edge, and Evil
  Spear. The most requested item in the pass. Deferred because six
  or more slots already ship the one-count-fan-per-strand workaround
  and a new builder plus rewiring all of them is a foundation
  project, not a merge fix. First candidate for a future toolkit
  pass.
- tendrilChain helper (Blaster Gun, Spirit Sword): beam owners chain
  tendrils by hand today and the chains read correctly; a shared
  helper is deduplication, not a defect fix. Deferred.
- Tendril phase parameter (Brave Sword, Z Sword, Wish Ritual,
  Cursed Blade): strands are static per seed and motion comes from
  envelope and shed particles. A phase input changes the builder's
  motion model for every caller; deferred to the same future
  toolkit pass as the gather variant, which it pairs with.
- crackFan plane basis and contact-normal variants (Blaster Gun,
  Cursed Blade, Dimensional Sword, Lava Sword observation, Ox
  King's Ax observation, Power Pole impact case): the pose copy and
  translate pattern covers every shipped case; wall and ceiling
  cracks are not a current effect need. Deferred.
- shardBurst distribution variants: flat fan (Saber, TimeShatter),
  spherical (Whis Staff), scatter origins (Cursed Blade), ring mode
  (Hollow's Edge), and a distScale parameter (Spirit Sword). All
  worked around inside budget with per-anchor calls. Deferred as a
  group.
- flameTongues direction alignment (Evil Spear, Devil Trident
  observation): tongues rise in world Y regardless of flight
  direction. The shipped reads are judged acceptable by both owners
  (short tongues, wake dominates). A direction parameter is a
  builder change with pass-wide blast radius. Deferred.
- tendrilPath builder (Azure): chained tendrils share endpoints
  today; the joints read as strand breaks, which suits torn wakes.
  Deferred.
- tendrilFan azimuth parameter (Azure, Bansho observation):
  documented workaround is the pose copy and rotate pattern, used
  consistently. Deferred; the crackFan note sets the precedent that
  the pattern gets documented when it is next touched.
- Tendril curl scale and pinch placement parameters (Katana, Saber,
  shared systems): real but minor; the affected effects ship with
  bounded, stated risks (aim guide wander, line slash waver, trail
  pinch placement). Deferred.
- Shared billboard pose helper (Hollow's Edge) and shared submit
  hook for tick-driven client fx (shared systems): consistency
  helpers for future work, no current defect. Deferred.
- fracturePlates internal plate count (War Mace observation): the
  builder draws 10 plates at distScale 1.0 against a standard table
  cap of 12; every slam owner used the builder as shipped, so full
  plate budget in the reports means the builder's full output.
  Recorded as an observation; no change.

Evaluated and not applied:

- Power Pole whirlwind fan spin halving: the owner names it as a
  one-line fix only if a playtest shows strobing. The geometry spin
  deliberately matches the filler cone spin, so halving it blind at
  the merge would desync the strands from the cone they ride, which
  is itself a worse-read risk. Deferred to Carter's PC test, where
  the strobing question can actually be answered; the one-line fix
  is recorded in audits/complex-report-power-pole.md.

Held exactly as instructed:

- Devil Trident telegraph sizing: the slam plate fan stays at 0.90
  and the crack web at 0.95 of the seal radius, per the owner's
  telegraph math (the fan at full size would have drawn about 1.16
  times the damage radius). Not resized at the merge.

## Resources and hygiene

- All 567 resource JSONs parse. All 184 particle texture references
  resolve, including the five complex sprite sets (8 frames each, 40
  frames: shard_chip, spark_streak, wisp_curl, ember_fragment,
  ash_flake) and both geometry canvas textures (flame_tongue.png,
  tendril.png under textures/entity/fx).
- git diff --check clean. Added-lines em dash scan: zero across the
  tracked diff, the 31 reports, and this audit.
- Split source sets stay clean: no file under src/main references a
  client-only class; the merge's central edits are all in the client
  source set.

## Dual javac sweeps

Baseline worktree at 9178eac, measured fresh: 132 unique (file,
message) error kinds on the 26.2 classpath, 138 on the 26.3
classpath. Integrated tree: 132 and 138. comm -13 prints nothing on
either classpath (zero new kinds), and comm -23 also prints nothing
on either (no baseline kind removed), so the kind sets are
identical. Targeted compile of the three centrally edited files is
clean in owned files on both classpaths, as recorded above. The
baseline worktree was removed and pruned. No Gradle build exists in
this VM and none was run; compile verification is the javac audit
sweep, as in every prior pass, and the standing waiver applies.

## Per-slot outcomes

- Katana: tendril and tendrilFan pieces on all four effects;
  HeavenSplitter gains the landing crackFan; SPARK_STREAK and
  SHARD_CHIP layers in. Aim guide filaments converge on the blade.
- Z Sword: fracturePlates plus crackFan on both slams, tendrilFan
  charge and stance wisps, DelayedLaunch crack telegraph web with a
  launch lash.
- Saber: tendril bodies on both slashes, shardBurst flat fan plus
  inward tendrils on the void tear, class S dodge spark sprite layer.
- Brave Sword: tendrilFan charge, crossed tendril cross slash,
  tendril rush segments, plates plus cracks on the shockwave.
- Grand Sword: shard mesh blade shard body with tendril wake,
  clash lash fan, crescent tendril arc, Valor Field rim tendrils
  with suspended shard meshes matching its stasis identity.
- Azure Dragon Sword: telegraph crack web inside the warned disc,
  quake plates and cracks, storm lash fans, tornado tendrilFan at
  full strand and sample budget.
- Dabura's Sword: veil tendril wakes and charge strands, domain rim
  tendrils plus base crackFan, shatter web as geometry.
- Cursed Blade: domain rim tendrils and base web, chain tendril per
  chain path, shatter plates and cracks, CurseLightning geometry
  (rewired at merge, above), telegraph warning web, SkyCracks crack
  plane with falling dark shards.
- Hollow's Edge: veil tendrils around the afterimage silhouette,
  inward charge fan, rush tendril, void shard ring with inward
  streamers, slash tendril.
- Dimensional Sword: three full tendril strands as the Dimensional
  Slash body; warp rift shard fan, inward tendrils, plane crackFan.
- Spirit Sword: tendril beam core with terminus fan, cleave tendril,
  shard mesh spirit spike with tendril wake.
- Evil Spear: tip flame tongues and inward fans on charge and
  projectile, crackFan plus burst lashes on Hell Hunt impact.
- Devil Trident: the reference composition on Devil Slam
  (fracturePlates brimstone fan, crackFan magma web, flameTongues
  ring) sized inside the hit edge; shard mesh prong heads and shard
  body restoring the silhouette the no-shapes pass lost.
- Sickle of Sorrow: rift shardBurst with inward tendrils, slash
  tendril body.
- Ox King's Ax: plates and cracks on KingsSlam and OxShockwave,
  fissure crackFan core with origin plates, dust tendril fans on
  charge and stance.
- Power Pole: wind tendril along the extension, impact crackFan,
  whirlwind circling tendrilFan with airborne ash.
- Whis Staff: temporal glass shardBurst at the tear with inward
  tendrils.
- Bansho Fan: cyclone tendrilFan at full fan budget, gale tendril,
  shockwave crackFan plus light plates, double tendril spiral wake.
- Blaster Gun: tendril bolt wake, Erasure beam chained tendril core
  with terminus fan and ground crackFan, orb lash fan.
- Lava Sword: flame tongues on blob, geyser, and meteor; cleave
  tendril body; eruption crown plus base web; meteor impact plates
  and cracks.
- Spike Flail: tendril arcs on the sampled head path, swing tendril
  body, contact streaks.
- War Mace: rise crackFan plus small plates; smash at the full plate
  budget plus crackFan over the real crater.
- Shared systems: SwingTrail tendril chains on the recorded swing
  path with tip streaks; Bolt geometry on the shared seeded path
  (rewired at merge, above).
- Ki Techniques: barrier boundary lashes, beam and spiral chained
  tendril cores with terminus fans, blast and laser tendril cores,
  disk rim lashes, explosion plates, cracks, and tongues. The
  KiBarrier hit ripple remains blocked on the entity having no
  synced hit field; that is a data change, out of this pass, and
  stays Carter's call exactly as the no-shapes merge recorded it.
- Wish Ritual: golden tendrilFan strands rising to the dragon ball
  ring, gold flame tongues at the ritual peak, wisp layer on the
  ball cluster.
- TimeShatter: frozen shardBurst fan of temporal glass plus plane
  crackFan, both on the effect's existing two-tone gradient, healing
  the color split the no-shapes pass noted.
- Evil Spear Dread: near-black veil tendrils curling through the
  shroud with ash drift; the detailed eye sprites are unchanged.
- Status Effects: one complex sprite layer per effect at the same
  tick points and rates across 25 of 27 classes; BleedingEffect and
  MeltingEffect exempt (above). FissureStunEffect and OxBraceEffect
  single-authored here.
- Mob Ability Effects: SPARK_STREAK hit feedback on the Frieza
  Soldier, WISP_CURL and ASH_FLAKE in the Saibaman burst, StaggerFx
  streak and chip burst. Class S, no pieces, per the audit.
- Storms and AtmosphereClient: WISP_CURL wind wisps and ASH_FLAKE
  grit in the storm volume; thunder geometry is the shared Bolt's.
  Otherworld cloud field exempt (above).
- Forms and Player Auras: flameTongues aura lashes in form colors
  plus tendrilFan wisps at high grades; first person pieces skipped
  for the local player so the near plane stays clear.

## PC-test review list (owner worse-check flags, carried in substance)

Fixed by this merge: the shared Bolt geometry versus emission path
divergence, and the CurseLightning double-bolt read. Both now draw
over the same seeded path as their emission.

Standing flags for Carter's in-game look, by slot:

- Azure Dragon Sword: the lightning telegraph's worst-case seed roll
  can poke a crack tip a few percent past the warned disc edge
  (typical endpoints sit near 0.76 of the radius; the disc itself
  still marks the exact hit size), and the center arc fan can read
  as a small fountain. Sonic Quake plates scale a fixed pattern
  rather than cracking progressively. Storm strands reseed in steps
  every eight ticks.
- Devil Trident: slam plates and cracks are drawn in the local
  ground plane and can clip or hover on sloped or stepped ground
  (the real crater and conversions conform; the filler still
  samples the area). Prong flame tongues rise in world Y on
  downward shots.
- Forms and Auras: busyness at high grades in crowds is the slot's
  named risk; the first dials are tendril count and tongue height
  in AuraRenderer. First person is handled (pieces skipped for the
  local player).
- Evil Spear Dread: near-black strands against a near-black shroud
  are subtle to nearly invisible in fully dark scenes; net gain in
  lit scenes, neutral in the dark. The theme forbids brighter
  strands.
- Lava Sword: the meteor impact pieces can only ramp in over the
  last five blocks of the fall, so the impact reads as a short
  ground break flash; a lingering crater composition would need a
  synced impact field or a short-lived impact entity, both out of
  this pass. Eruption rear crack tips read as base cracking.
- Bansho Fan: shockwave plates at the first ticks, when the radius
  is near minimum, could read as a green shard cluster rather than
  scoured ground; dropping the plates (keeping crackFan plus chips)
  is the named fallback if they read wrong.
- Power Pole: whirlwind geometry spin strobing is the watch item
  behind the deferred spin decision above; impact cracks stand on
  the strike plane in the ground case.
- Cursed Blade: Abyssal rim tendrils are seeded static (no phase
  parameter) and could read as spikes if stared at; CurseChain
  pinch windows could read as the chain nearly breaking; SkyCracks
  reads as a radial fan with wander and forks, less web-like than a
  true web.
- Shared systems: SwingTrail pieces are drawn through a
  LevelRenderEvents submit hook verified against the vanilla
  pattern but not seen running in game from this VM; if the pose
  convention were wrong, trails would sit offset from the player,
  which is immediately obvious and a one-line fix.
- Hollow's Edge: afterimage veil strands could read as flames
  licking the silhouette if they splay; narrowing the fan is the
  named one-line fix.
- Katana: aim guide filaments wander at full range (fixed curl
  amplitude) and could read as loose whips mid-path; charge fan
  could read as a small crown at full charge.
- Saber: the Dimensional Line Slash body wavers slightly where a
  razor-straight line is wanted (bow 0.16 times scale); slash pinch
  windows could read as segmentation on the short slash.
- Spirit Sword and Blaster Gun: chained beam cores bow slightly
  (0.22 blocks on Spirit Cannon, baseRadius times 0.35 per segment
  on Erasure); endpoints are shared so cores never gap, and the
  straight plasma runs remain around them.
- Dimensional Sword: warp rift shards are static geometry for the
  rift's 24 tick life; only their alpha follows the tear.
- Grand Sword: clash fan axis is seeded, not tied to the deflected
  projectile's direction; blade shard mesh may read slightly large.
- Ox King's Ax and all ground slams: fracture plates on steep
  slopes can float or clip like any ground decal; slam entities
  spawn on struck ground, bounding the case.
- War Mace: rise plates would draw at feet level if a rise ever
  triggered mid-air (pre-existing framing, 8 tick life).
- Sickle of Sorrow: rift shards fan as a fountain around the up
  axis, not a spherical burst.
- Spike Flail: the short spin arc strand could read as a chunky
  ribbon stub at the starting orbit radius.
- Wish Ritual: the five tendrils share one root, reading as a
  rising fountain around the ring rather than one strand per ball
  (a vertex budget compromise, stated by the owner).
- Status Effects: fixed sprite style colors against themed filler
  are tightest on AncientWeightEffect and OxBraceEffect (gray chips
  in gold and orange-red dust); both are earth family effects.
- Storms: the grit and wisp additions were judged against the
  volume's existing wind dust and gloom; no flag held up, recorded
  here for completeness.
- TimeShatter, Z Sword, Dabura's Sword, Evil Spear, Whis Staff,
  Mob Effects: owners report no worse read, with bounded placement
  and scale caveats in their reports.

## Standing lines

- No Gradle build was run and none is claimed. Verification is the
  dual javac audit sweep, per the standing waiver; Carter's PC test
  is the first real look at every effect in this pass.
- Plain melee, the internal damage pipeline, all mechanics, and the
  destruction wiring from d74f13a are untouched by this pass.
- Deferred toolkit items (gather tendrilFan, tendrilChain, tendril
  phase, crackFan plane basis, shardBurst distributions,
  flameTongues direction) are recorded above as the candidate list
  for a future shared toolkit pass, each with shipped workarounds
  in the tree.
