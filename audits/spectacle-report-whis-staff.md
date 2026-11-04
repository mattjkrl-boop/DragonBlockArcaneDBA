# Spectacle Report: Whis Staff

Phase 2 weapon report, written against audits/SPECTACLE-STANDARD.md and the
Whis Staff section of audits/fresh-audit-technique.md. Theme kept throughout:
angelic time, erasure, calm precision. The rift is a suspension field, not a
storm: everything added below is slow, layered, and deliberate.

Ownership, confirmed from the code: TemporalRiftEntity is the Whis time rift
(spawned only by WhisStaffItem.use). ErasureChargeOrbEntity and its renderer
were in my assignment too, but they are also the Blaster Gun agent's files
(the orb is spawned by BlasterGunItem, not by the staff). That agent worked
both files during this same pass and reported them in
audits/spectacle-report-blaster-gun.md. I did not duplicate or overwrite its
work; the one divergence is flagged under Shared change requests.

Files I edited: item/WhisStaffItem.java, entity/TemporalRiftEntity.java,
client/render/TemporalRiftRenderer.java. Nothing else.

## Before and after numbers

| Value | Before | After |
|---|---|---|
| Melee base damage | 350 | 350 (unchanged) |
| Melee reversal on hit | 100 ticks, immediate | unchanged (correct there: it pairs with its own stasis) |
| Rift total damage | 400 + Spirit x 1.5 | 400 + Spirit x 1.5 (total unchanged) |
| Rift damage timing | all of it on cast tick 0 | 200 + Spirit x 0.75 at cast, 200 + Spirit x 0.75 at rift tick 15 (the envelope peak) |
| Rift stasis | 60 ticks | 60 ticks (unchanged) |
| Rift reversal start | tick 0, immediate, contradicting its own comment | tick 60, the moment the freeze ends, triggered by the rift entity |
| Rift reversal duration | 100 ticks | 100 ticks (unchanged) |
| Rift entity lifetime | 60 ticks | 100 ticks |
| Rift cooldown | 100 ticks | 140 ticks (now exceeds the 100 tick life, so two domes cannot stack) |
| Rift Ki cost | 8.33 percent of max Ki, charged once up front (100 percent per minute prorated over 100 ticks) | unchanged, and still charged only when the drain check passes before anything fires |
| Rift particle rate | about 13 per second (2 KI_MOTE every 3 ticks, one layer, one ring band) | LayeredParticles.emitVortex every tick: 11 particles per tick at density 1.0 and envelope 1.0 (3 core KI_MOTE, 5 mid celestial cyan dust, 3 outer KI_MOTE), about 220 per second at peak, envelope scaled, plus emitRain drizzle through the last 35 percent |
| Rift debris | 0 pieces | 60 DebrisEntity pieces, ground sampled palette, 3 sizes, rise 2.5 blocks, orbit 80 ticks, then rain down |
| Rift mob forces | none (freeze only, nothing moved) | VortexPhysics.applyVortex every tick: inward 0.030, swirl 1.6 (tangential 0.048), core lift 0.05, envelope scaled, weight classed, players at 0.6 |
| Strike shatter positions | 1.5, 3.0, 4.5 blocks, even steps | 1.2, 2.6, 4.2 blocks, escalating |
| Strike shatter scales | 0.75, 0.95, 1.15 | 0.8, 1.05, 1.4 |
| Left click Ki cost | 100 percent per minute over 13 ticks | unchanged |
| Celestial Grace self buff | 200 ticks | unchanged |
| Erasure orb numbers | n/a | no numbers changed by me (see conflict note) |

## What was added, per point

1. LAYERED DEPTH. The rift entity now emits the shared three layer vortex
   particles every tick (bright KI_MOTE core, celestial cyan mid dust on the
   helical band, wispy KI_MOTE rim), replacing one flat mote ring at about
   13 particles per second. In the renderer, the shared
   LayeredRender.drawVortexBands now draws three counter-rotating time
   current bands inside the two existing dome shells, and
   LayeredRender.drawAnchorGlow grounds the dome with the standard anchor
   disc. Dome latitude and longitude counts, core shell counts, and shell
   alphas now step down with camera distance through FxEnvelope.segments
   and the square root of the distance scale.
2. DEBRIS. At rift tick 2 the entity spawns a 60 piece DebrisSpawner field
   sampled from the ground the rift stands on: the terrain itself, caught
   mid-air. It uses the low rise field the brief asked for (rise 2.5 blocks,
   orbit 80 of the rift's 100 ticks) so pieces hang and drift in suspension,
   then release and rain back down while the dome dissipates. Pieces never
   vanish mid-air; DebrisEntity lands and crumbles them.
3. WORLD INTERACTION. VortexPhysics runs for the rift's whole life at
   gentle strength: the swirl dominates the pull on purpose, so bodies and
   debris slowly orbit and hover instead of being dragged to the middle or
   frozen in dead air. Weight classes apply (bosses barely move). No block
   damage and no ground scarring: tearing up terrain does not fit an
   angelic time theme, and the standard reserves permanent destruction for
   the heaviest finishers. The ground still participates, as the debris
   palette.
4. ATMOSPHERE. Whis has no weather theme, so per the standard none was
   added. Its atmosphere is light and particles: the layered motes, the
   anchor glow, the dissipation rain, and the dome's cyan to silver ramp,
   all breathing on one envelope.
5. LIFECYCLE. The renderer now runs the shared FxEnvelope (build 15
   percent, decay 35 percent) for all alpha, and the radius unfolds over
   the build only. The old code shrank the radius to zero over the last 10
   ticks on top of the fade, double-ending the dome, and its whole build,
   peak, dissipation arc was compressed into 60 ticks; life is now 100.
   Entity particles, vortex strength, and damage all key off the same
   envelope: the second damage half lands at tick 15, the envelope peak,
   inside the visual's active window, and the entity discards at tick 100
   where the envelope is already zero, so nothing pops.

## Bugs fixed

- use() comment versus code contradiction: the comment said the reversal
  "starts after freeze ends" while dba$startReversing(100) was called
  immediately in the same loop. Resolution: the CODE moved to match the
  comment. The rift entity now starts the reversal at its tick 60, when the
  60 tick stasis expires, on everything still inside the dome. The comment
  in use() describes that mechanism. Why this side: the header and comment
  both describe freeze, then rewind, as the design, and an immediate rewind
  burns most of its 100 ticks while the target is frozen and cannot move.
- TemporalRiftEntity.maxLifetime was never synced, so the client renderer
  always read the field default of 60. Any other lifetime would have
  rendered with wrong progress and cut off early. Fixed with a synced
  MAX_LIFE accessor that getMaxLifetime() now reads. This fix is what makes
  the 60 to 100 lifetime change render correctly at all.
- TemporalRiftRenderer.drawTrenchSegment was dead code (defined, never
  called; grep count 1, the definition). Removed; grep count is now 0.
- ErasureChargeOrbRenderer.drawRotatingOctahedron converted its yaw and
  pitch parameters and never used them, and the header promised 3 rings
  while submit drew 1. Both were fixed in this same pass by the Blaster Gun
  agent (see the conflict note): vertices now rotate through its rotVertex
  helper, three rings are drawn in three planes (drawPlaneRing plus its new
  drawAxisRing), and a wispy outer shell was added. I verified all three in
  the current tree and did not duplicate the work.
- Em dashes in comments in WhisStaffItem.java and TemporalRiftEntity.java
  (several pre-existing, one in a new comment of mine that copied the old
  phrasing): all replaced, per the no em dash rule.

## Shared change requests and conflicts

1. OWNERSHIP CONFLICT, needs a coordinator ruling. ErasureChargeOrbEntity
   and ErasureChargeOrbRenderer were assigned to both my brief and the
   Blaster Gun agent. Its pass on them is complete and reported in
   audits/spectacle-report-blaster-gun.md. One substantive divergence: my
   brief said to DRAW the header's promised helical ribbons and corona
   discharge arcs. The Blaster agent instead removed those two promises
   from the header and implemented the corona as an EMBER particle layer in
   the entity past 0.75 charge, which its report calls the honest version
   of the idea. I left its resolution in place rather than overwrite a
   finished sibling pass in the same files. Either way, the standard's
   actual requirement is met: the header and the submit now agree, and the
   rings and the rotating core, the two promises my brief named with code
   locations, are drawn. If the coordinator prefers real drawn arcs, the
   addition is small and belongs in ErasureChargeOrbRenderer.submit beside
   the intake spikes: drawLash arcs at seeded directions, re-seeded every
   3 ticks for flicker, alpha scaled by the hot factor.
2. TimeShatterEntity and TimeShatterRenderer are the staff's third piece
   but were not in my file list, so I did not edit them. From the before
   audit they still carry: an 18 tick life for the weapon's signature
   visual; no fade-in over the first half of life (full alpha at spawn,
   fade only after progress 0.5); and two separate Random sequences for the
   billboard dial and the world shards, so the shard pattern does not match
   the dial's cracks. Recommend a small coordinator follow-up: one shared
   seed, and an FxEnvelope fade across both halves. This pass I changed
   only the item side spawn positions and scales.
3. No shared file changes were needed for my work. I only called
   FxEnvelope, LayeredParticles, DebrisSpawner, VortexPhysics, and
   LayeredRender as published.

## Verification

Method: javac audit sweeps, the standing substitute in this sandbox (the
Gradle build cannot run here and the compile check stands waived by
Carter's decision; no Gradle pass is claimed). Baseline is a git worktree
at HEAD 5e4b1de; error kinds are (file, message) pairs with line numbers
stripped, compared on the 26.2 and 26.3 classpaths. An earlier comparison
using the shared base and cur sweep tags was discarded: sibling agents use
the same /tmp tag names, and their runs overwrote the kinds files mid-pass
(one baseline file changed from 105 to 125 entries between two reads, and
its error file was zeroed). The verification below comes from a rerun with
unique tags (wbase, wcur) so nothing else could clobber it.

Results of the unique tag rerun (baseline worktree at 5e4b1de versus the
current tree, which includes every Phase 2 agent's uncommitted work):

- 26.2: baseline 132 error kinds, current 135. New kinds: 3, all in other
  agents' files (KatanaChargeRenderer: variable dist already defined;
  ZShockwaveEntity: variable density already defined; CurseBladeItem:
  cannot find symbol). Kinds in WhisStaffItem, TemporalRiftEntity,
  TemporalRiftRenderer, TimeShatter files, or ErasureCharge files: zero.
- 26.3: baseline 141 error kinds, current 145. New kinds: 4, all in other
  agents' files (KingsSlamEntity and OxShockwaveEntity: cannot find
  symbol; ZShockwaveEntity: variable density already defined;
  CurseBladeItem: cannot find symbol). Kinds in my files: zero. The
  spurious mixin package block from the clobbered shared tag run did not
  reappear, confirming it was a file collision artifact, not a real
  regression.

So my three edited files introduce zero new error kinds on either
version. The 7 tree-wide new kinds belong to sibling agents' in-flight
work and are the coordinating pass's to resolve in its final sweep.

## Worse check (honest)

- Half the rift's damage now lands 15 ticks after cast. Every cast target
  is frozen for 60 ticks, so nothing can dodge the second half; anything
  that walks into the dome after cast takes only the peak half, which is
  fairer than the old instant full burst on tick 1 and matches the visible
  telegraph. Not worse.
- The vortex drift is deliberately weak. Under a full stasis freeze most
  targets barely move; the visible motion lands on the debris, the motes,
  and anything inside after the freeze ends. That is the suspension the
  brief asked for, not a storm. If frozen bodies should visibly orbit, the
  TEMPORAL_STASIS effect itself (not my files) decides whether a frozen
  body accepts velocity; flagged here for the coordinator rather than
  forced from the item side.
- The debris palette samples the ground at the cast point even if the
  caster is flying, so pieces can hang below the dome. Same behavior as
  every other DebrisSpawner user in the pass; acceptable.
- The rift's reversal now depends on the rift entity living to tick 60. If
  the entity is discarded early (chunk unload), targets keep the freeze but
  lose the rewind. Before, the rewind was guaranteed at cast. Accepted
  trade: it is the only way to make the reversal actually start when the
  freeze ends, and a rift in an unloaded chunk has no audience anyway.
- Nothing was reverted. No file outside my list was edited by me.
