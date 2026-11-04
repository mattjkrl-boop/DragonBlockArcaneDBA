# DBA Entity Objects Pass: Final Audit

Result: all seven renderer-decorated shard sets from the complex redo
are now real FragmentEntity objects in the world, spawned server side
by their hosts, with counts, sizes, colors, seed families, and modes
matching audits/entity-audit.md. The merge applied five central fixes
(the Void Rift ring follow, frozen growth onto girth, the growth gate,
held-mode spawn scale-in removal, and the explicit hang/hold lifetime
path wired to the Void Rift bound) and deferred one cleanup item with
reasons. Dual javac sweeps against the Phase 1 baseline (6155dda):
132 error kinds on 26.2 and 138 on 26.3, before and after, with
comm -13 empty on both classpaths, so the kind sets are identical.
No Gradle build was run or claimed (the standing VM restriction);
the in-game look is unverified until Carter's PC test.

## Executive result

- Seven hosts converted, one shared entity (FragmentEntity,
  foundation from Phase 1, untouched by owners): SkyCracks,
  Temporal Rift, Dimensional Rift, Void Rift, Warp Rift, TimeShatter,
  Valor Field. Per-host reports: audits/entity-report-<slug>.md.
- Fragments deal no damage in any mode (foundation contract:
  hurtServer returns false, isPickable false, no damage call exists
  in FragmentEntity). No host damage number, cooldown, cost, timing,
  mechanic, or synced field changed anywhere in the pass; the tree
  wide diff over src/main adds no hurt, cooldown, or drain line and
  removes no damage line. The Valor Field projectile stasis system,
  including the suspended-projectile damage attribution block, is
  byte-identical: the ValorFieldEntity diff contains zero removed
  lines, only fragment spawn and releaseMarker insertions beside the
  unchanged stasis code.
- Themes and colors are exact per host (verified at each spawn call
  against the audit): SkyCracks 0x14030A to 0x5A1230, Temporal Rift
  0x2F6DB3 to 0xDDF7FF, Dimensional Rift 0x24053D to 0xB84DFF, Void
  Rift 0x2E004D to 0x9905F2, Warp Rift 0x1A0533 to 0xE8CCFF,
  TimeShatter 0xFFFFFF to 0x65D8FF, Valor Field dome 0x767F8C to
  0xFFD833 and markers 0x8A93A6 to 0xFFF3B0.

## Ownership check

- The uncommitted Phase 2 diff was 14 files; every one belongs to
  exactly one of the seven owners, matching the files each report
  claims: the seven host renderers (DimensionalRift,
  DimensionalWarpRift, SkyCracks, TemporalRift, TimeShatter,
  ValorField, VoidRift), six host entities (the same hosts minus
  TimeShatter, whose entity is untouched by design), and
  item/WhisStaffItem.java for the TimeShatter spawn sites.
- entity/FragmentEntity.java and client/render/FragmentRenderer.java
  show zero modifications by any owner. The only foundation edits in
  the integrated tree are the merge's own central fixes below.
- All seven reports are present, each carrying per-effect match data,
  verification on both classpaths, and an honest worse check.

## Entity gates (fresh at the merge, per ENTITY-STANDARD)

1. Zero ComplexGeometry.shard and shardBurst calls remain in the
   seven hosts' renderers (grep over SkyCracksRenderer,
   TemporalRiftRenderer, DimensionalRiftRenderer, VoidRiftRenderer,
   DimensionalWarpRiftRenderer, TimeShatterRenderer,
   ValorFieldRenderer: no calls in any). The only remaining
   ComplexGeometry.shard caller in effect scope is FragmentRenderer
   itself, one shard per fragment; the other callers
   (TridentShardRenderer, TridentProngRenderer,
   GrandBladeShardRenderer, SpiritImpaleRenderer,
   SaberVoidTearRenderer) draw bodies for real entities outside this
   pass's seven hosts and are untouched.
2. Spawn counts, sizes, colors, and modes match the audit at every
   call site (read directly at the merge): SkyCracks rain of 5
   (2/2/1) per 20 tick beat, disc 9.5, unit 1.05; Temporal Rift burst
   of 5 at tick 2, 150 degree cone, gravity 0.3, life 110; Dimensional
   Rift burst of 5 at tick 2, unit 0.80, gravity 0.5, life 70; Void
   Rift ring of 5 (mode changed at the merge, see fix a); Warp Rift
   burst of 4 per rift, life 60 origin and 72 destination;
   TimeShatter hang of 5 per shatter, RELEASE_AT preset to the host's
   36 tick life, from all three WhisStaffItem spawn sites; Valor
   Field 3 dome fragments at field tick 1 plus up to 2 marker
   fragments on suspension, FIELD_HELD throughout.
3. All fragment spawns come from the audit's server code paths; no
   client-side spawning exists anywhere (grep for the four helper
   names finds only the eight server call sites above plus the
   foundation definitions).
4. No damage change anywhere (see Executive result). Host diffs add
   only spawn calls, release calls, and the bookkeeping they need.
5. Worst-case fragment totals sit far under the foundation caps
   (64 per call, 256 per 96 block area): channeling SkyCracks peaks
   near 30 alive, chained TimeShatters are area-capped by the helper
   itself, Valor markers never exceed 2 alive.
6. Dual sweeps clean (below); all resource JSONs parse; diff check
   and em dash scan clean (below).
7. Host renderers keep every non-shard layer: tendrils, crack fans,
   fracture plates, and all emission lines are behavior-identical in
   each renderer's diff; only the shard blocks, their dead locals,
   and doc comments changed.

## Central fixes (merge, each verified by targeted javac on both classpaths)

a. VOID RIFT FOLLOW (applied, as decided). The Void Rift ring shipped
   FROZEN at the rift's opening position, but HollowsEdgeItem drags a
   channeling rift toward the crosshair and the ring stayed behind.
   spawnRimShards now uses FragmentEntity.hold with the rift as
   holder on the identical anchor offsets, so the ring follows the
   rift's movement for real. Counts (5), per-shard seeds
   (riftSeed ^ (0xE11 + i * 131)), colors, size rolls (unit radius *
   0.38), axis math, rest 8, and the release path (triggerImplosion
   calls release() on each fragment, covering the item release and
   the auto implode at MAX_CHANNEL_TICKS) are unchanged. Accepted
   remainder, recorded per the decision: the anchor offsets are taken
   at spawn, so the ring keeps the opening radius (unit from radius
   2.5) while the rift grows toward 9.0; the ring follows position,
   it does not rescale. If the rift is discarded without imploding,
   the foundation orphan rule releases the ring on its next tick.
b. FROZEN GROWTH TO GIRTH (TimeShatter change request 1, applied).
   FragmentRenderer's FROZEN growth scaled drawn length only; the
   drawn fan it replaced rolled girth from the grown length each
   frame, so fragment shards read slimmer than the fan late in the
   hang. The same growth factor now multiplies drawGirth.
c. GROWTH GATE (Void Rift change request 2, applied). The growth rule
   keyed on RELEASE_AT > 0, but release() restamps RELEASE_AT with
   the current age, so a host-released FROZEN piece flipped straight
   to the 1.40 full-growth value on the release frame. The
   foundation now syncs a separate GROWTH_WINDOW, set only at spawn
   from hang's preset releaseAtAge and never restamped, and the
   renderer keys growth to it with the effective age capped at the
   release age: once a piece is released, its growth freezes at the
   value it had reached and can never step. TimeShatter's preset
   growth is preserved exactly (window and RELEASE_AT both 36;
   0.55 + 0.85 * cubicOut over the window saturates to 1.40 at tick
   36 and holds through the fall, identical numbers to the pre-merge
   renderer at every age).
d. SPAWN SCALE-IN (TimeShatter change request 2, applied).
   FragmentRenderer applied the 5 tick spawn scale-in to every mode,
   while the standard describes it as except-FROZEN. FROZEN and
   FIELD_HELD fragments now skip the scale-in and appear at full size
   on their hang point or anchor; BALLISTIC and BURST keep it. This
   also removes the TimeShatter double-shrink the owner flagged
   (scale-in stacked on the 0.55 growth floor in the first ticks) and
   the Void Rift spawn read its report flagged (ring scaling in over
   5 ticks at the opening). Recorded consequence: Valor Field dome
   and marker fragments now appear at full size instead of scaling
   in over 5 ticks; they materialize on existing beats (the field's
   spawn burst, a suspension event), so the merge judges the pop
   masked, and it is on the PC-test list below.
e. HANG LIFETIME (Void Rift change request 1, applied). hang()
   derived its lifetime as (releaseAtAge or 600) + 300 and held
   fragments never consulted it, so an orphaned held ring was
   bounded only by HARD_LIFE_TICKS 2400, not the audit's
   MAX_CHANNEL_TICKS + IMPLOSION_TICKS + 120 = 390. The foundation
   now has explicit lifeTicks overloads on hang() and hold()
   (existing callers keep their derived or hard-cap lifetimes), and
   the held tick path stamps the fade at min(lifeTicks,
   HARD_LIFE_TICKS) - FADE_TICKS, so a still-held fragment fades and
   discards at its declared lifetime. The Void Rift ring is wired
   through the hold() overload (it is FIELD_HELD after fix a) with
   RIM_SHARD_LIFE_TICKS = 390. TimeShatter (derived 336, released at
   36 in every real flow) and Valor Field (hard cap 2400) keep their
   previous effective lifetimes exactly.
f. DEFERRED. The public length/girth roll helper (TimeShatter
   change request 3): TimeShatter and Void Rift each mirror the
   foundation's private size roll in host code. Both mirrors were
   read at the merge and are value-identical to the foundation's
   classes today, so this is a drift risk, not a defect; a public
   helper is a cleanup that would touch two verified hosts for no
   behavior gain in this pass. Deferred with that reason. The
   FADE_TICKS 12 versus audit 10 discrepancy (Dimensional Rift and
   Warp Rift audits say fade 10) stays resolved for the standard, as
   both owners recorded: FADE_TICKS is a foundation constant at 12
   for every fragment, and both hosts' fade timing intent holds
   under it.

Verification of the central fixes: targeted javac over
FragmentEntity.java, FragmentRenderer.java, and VoidRiftEntity.java
on both classpaths gave exactly the standing baseline closure errors
(4 on 26.2: DbaItems x2, DbaMenus x2; 10 on 26.3: the same plus
DbaHostileEntity x2, PatrolRobotEntity x2, FriezaSoldierEntity x1,
NamekianWarriorEntity x1) and zero errors in the three edited files.
The full dual sweeps below then re-verified the whole tree.

## Resources and hygiene

- All 567 resource JSONs parse. No resource file changed in this
  pass.
- Registration is consistent: DbaEntities.FRAGMENT (id "fragment",
  MobCategory.MISC, sized 0.5 x 0.5, clientTrackingRange 10,
  updateInterval 1, the standard's shape) and FragmentRenderer
  registered in DragonBlockArcaneDBAClient immediately after the
  DEBRIS renderer. No lang entry for the fragment, per the debris
  and trident shard precedent recorded in the standard; no new
  texture references (the fragment draws through ComplexGeometry
  canvases the complex pass already shipped).
- git diff --check clean on the integrated tree. Added-lines em
  dash scan: zero across the full diff and all seven reports.

## Dual javac sweeps

Baseline worktree at 6155dda measured fresh: 132 error kinds (26.2),
138 (26.3). Integrated tree: 132 and 138. comm -13 prints nothing on
both classpaths (zero new kinds), and comm -23 also prints nothing,
so the kind sets are identical to the Phase 1 baseline. Baseline
worktree removed and pruned after the run.

## Per-host outcomes

1. SkyCracks (Cursed Blade): the three billboard shardBursts are
   gone; SkyCracksEntity.tick rains 5 real fragments per 20 tick
   beat on the bolt-shed cadence, clusters 2/2/1 in a 9.5 disc under
   the crack plane, falling the full height, landing, resting 30,
   fading 12. Worst case about 30 alive while channeling. Crack
   fans, plasma paths, wisp curls, soul stream, and the darkness
   disc are untouched.
2. Temporal Rift (Whis Staff): 5 BURST fragments at tick 2 from the
   tear heart (radius * 0.441), 150 degree cone, gravity scale 0.3,
   life 110, riding the same one-shot guard as the untouched
   60 piece DebrisSpawner field. Tendrils, all eight emitters, and
   the host's damage and rewind beats are untouched.
3. Dimensional Rift (Sickle of Sorrow): 5 BURST fragments at tick 2
   at the early tear heart (about 1.9 blocks at spawn radius), seed
   family recomputed server side including the renderer's position
   terms, gravity 0.5, life 70 so they settle just after the host
   dies at 60. Tendrils and all emission untouched.
4. Void Rift (Hollow's Edge): the 5 rim shards are FIELD_HELD on the
   rift after merge fix a (owner shipped FROZEN; the follow decision
   and its remainder are recorded there and above), released by
   triggerImplosion exactly as before, rest 8, life bounded at 390
   after fix e. Tendrils, ASH_FLAKE rim, all emission, and the
   DebrisSpawner waves untouched.
5. Warp Rift (Dimensional Sword): 4 BURST fragments per rift on the
   first tick at the tear heart (+1.4), both rifts of a warp, life
   60 origin and 72 destination, gravity 0.6. Tendrils, ground
   crackFan, and WISP_CURL layer untouched.
6. TimeShatter (Whis Staff): 5 FROZEN fragments per shatter hung by
   WhisStaffItem at all three host spawn sites (15 across a full
   stepped strike), fan angles and mesh seeds replicated exactly
   (Random over hostSeed ^ 0xFA17; per-spoke seed hostSeed ^
   (0x5A1D + i * 131)), world plane from the caster's look direction,
   RELEASE_AT preset to 36; at 36 they drop, land with the landing
   puff, rest 10, fade 12. Merge fixes b, c, and d all land on this
   host's read. Crack fan, spoke chips, glass dust, and core streams
   untouched; TimeShatterEntity itself untouched.
7. Valor Field (Grand Sword): 3 dome fragments FIELD_HELD on the
   field at tick 1 (anchor formulas, sizes 1.15 x 0.30, spin 1.72)
   plus up to 2 marker fragments FIELD_HELD on suspended projectiles
   (offset +0.55, spin 2.86), released on every suspended-map removal
   path including beginFadeOut's releaseAllSuspended beside the
   projectile resumes. The dome bob is not reproduced (audit trade;
   the anchor follow on the moving field carries the read). Stasis,
   detonation, attribution, radius, drain, and pressure are
   byte-identical.

## PC-test review list (owner worse-check flags, carried in substance)

- SkyCracks: the rain fires all 5 shards on one beat every 20 ticks,
  so the fall reads in pulses (overlapping falls keep the column
  populated, but the beat is visible if watched); and because the
  shards now fall the full height, the tear heart itself reads
  emptier than the billboard clusters made it look, carried by the
  crack fans and plasma paths.
- Temporal Rift: the fragments drift from the heart at tick 2 instead
  of hanging in the slow rotating fan for the rift's life (the
  audit's authored trade); they spawn at full unit and height during
  the build phase, softened by the scale-in; and fade is
  fragment-local, so an early-landed fragment can fade while the
  rift still lives.
- Dimensional Rift: fragments spawn once at the early heart (about
  1.9) and do not climb with the growing column, so the peak-size
  tear reads emptier of solid pieces than the riding fan did; the
  owner names a second authored burst beat as the possible future
  fix inside the standard, noted here as future work, not done.
  Settled fragments also outlive the host by up to 10 ticks.
- Warp Rift: fragments appear at full presence on tick 1 (scale-in
  only), where the drawn fan faded in with the tear envelope; the
  landing SHARD_CHIP puff is a small visual addition the fan never
  had.
- TimeShatter: the fan is world-anchored, so from behind or edge-on
  the five shards read as a thin plane of glass where the billboard
  always faced the viewer (from the caster's view it matches by
  construction); and the entity fan is a cleaner five-spoke
  regularity than the jittered drawing (exact center, exact axes per
  the audit spec).
- Valor Field: on field death the fragments release at full alpha
  and drop, fading only after landing and resting, where the drawn
  shards dissolved with the dome fade; first thing to judge on the
  PC test. Marker choice under load follows server suspension order
  rather than the renderer's client scan order. After merge fix d,
  dome and marker fragments also appear at full size rather than
  scaling in over 5 ticks.
- Void Rift: the implosion choreography changed in kind (the ring
  empties by falling through the 30 tick collapse instead of dying
  in place on the shard window); the world ring is flatter and wider
  than the tall mouth ellipse the billboard anchors drew (audit
  trade); and after fix a the ring radius staying at spawn scale
  while the rift grows remains as the accepted remainder.

## Standing lines

- No Gradle build exists in this VM and none was run; verification
  is the dual javac audit sweeps above plus targeted javac per
  owner and per central fix. The in-game look is unverified until
  Carter's PC test, with the review list above as the checklist.
- Fragments are never saved (shouldBeSaved false), are removed on
  chunk unload, and cannot survive a restart; a crash mid-effect
  loses the fragments and nothing else.
