# Spectacle Report: Katana

Phase 2 weapon pass, built on audits/SPECTACLE-STANDARD.md and the Katana
section of audits/fresh-audit-blades.md. Theme kept: swift iaido, pale
crescents, heaven splitting finisher. Files changed: item/KatanaItem.java,
entity/KatanaChargeEntity.java, entity/SwiftCrescentEntity.java,
entity/KatanaAimGuideEntity.java, entity/HeavenSplitterEntity.java, and the
KatanaCharge, SwiftCrescent, KatanaAimGuide, and HeavenSplitter renderers.
HollowAfterimageEntity and its renderer were not touched; only how Katana
spawns the afterimage changed (one extra spawn on the freeform dash).

## Before and after numbers

### Flashdraw (left click)

| Value | Before | After |
|---|---|---|
| Charge Ki drain | 30% per minute (2.5% of max Ki per full 100 tick charge) | 40% per minute (3.33% per full charge) |
| Ki runs out mid charge | Fires anyway at whatever charge was reached | Cancels: no dash, no damage, no cooldown |
| Ki on immobilized block | Kept | Refunded in full for that charge |
| Chain target order | Entity query order (nearby.get(0)) | Nearest to the previous target first |
| Strike timing | Same tick as the teleport, queued through server execute | Crescent tick 4 + 2 per chain step, inside the crescent's life |
| Hit crescent lifetime | 10 ticks | 16 ticks |
| Trail crescent lifetime | 8 ticks | 12 ticks |
| Freeform dash afterimage | None | 1 at the launch point, same as the chained branch |
| Cooldown | 30 ticks (1.5 s) | 60 ticks (3 s) |
| Damage formula | (500 + Str x 2.5) x (1 + 0.2 per step) x (0.55 + 0.75 x ratio) | Unchanged |

### Heaven Splitter (right click)

| Value | Before | After |
|---|---|---|
| Charge Ki drain | 30% per minute (2.5% per full charge) | 60% per minute (5% per full charge) |
| Ki runs out mid hold | Fires anyway at the 0.1 ratio floor | Cancels: no dash, no damage, no cooldown |
| Base damage | 1000 + 1000 x ratio | 600 + 1400 x ratio |
| Base damage at tap floor (ratio 0.1) | 1100 | 740 |
| Base damage at full charge | 2000 | 2000 |
| Full charge vs tap, base | 1.8x | 2.7x |
| Strength term | Str x 4.0 x ratio | Unchanged |
| Distance falloff | None | x (1 - 0.30 x distance along path), 70% at the far end |
| Target order | Entity query order | Sorted along the dash path; impact burst lands on the nearest |
| Damage timing | Release tick, before the wall visual existed | Wall tick 10 of 40, when the wall reaches full height |
| Wall lifetime | 20 ticks | 40 ticks |
| Wall rise window | 42% of life | 25% of life, completing exactly at the damage tick |
| Aim guide range | Fixed 24 at every charge | 16 + 8 x ratio, equal to the real dash range, updated every tick through setMaxRange |
| Aim guide entity default range | 24 | 16 |
| Cooldown | 80 ticks (4 s) | 120 ticks (6 s) |
| Kill resets katana cooldown | Yes, in the item's release loop | Yes, in the wall entity's strike (same behavior) |
| Thunder sounds | At release | At the strike tick, in the wall entity |
| Debris pieces | 0 | 28, ground sampled, at the cut's ground midpoint |
| Lightning bolts | Sound only | 3 BoltEntity strikes along the path |

## What was added, per point

1. LAYERED DEPTH. The charge entity now emits three particle layers every
   tick (KI_MOTE core rising through the stance, pale cyan dust mid ring,
   SPARK outer wisps), scaled by a FxEnvelope smoothstep build and the
   charge ratio, and KatanaChargeRenderer adds LayeredRender.drawVortexBands
   around the stance: three counter rotating bands at the standard alphas.
   SwiftCrescentRenderer draws a second copy of the crescent at 1.3x scale
   and 0.18 alpha as a wispy outer layer. The Heaven Splitter wall keeps its
   three wall layers, now driven by the shared envelope, plus a
   LayeredRender.drawAnchorGlow disc under the path midpoint. All four
   renderers scale segment counts with camera distance through
   LayeredRender.distScale and FxEnvelope.segments. HeavenSplitterEntity
   emits core motes, a theme dust sheet, and outer sparks along the path
   every tick, envelope scaled.
2. DEBRIS. The Heaven Splitter strike calls DebrisSpawner.spawnField at the
   ground midpoint of the cut: 28 pieces, palette sampled from the actual
   ground, three sizes, helical orbit, raining back down over 34 ticks.
   Flashdraw deliberately gets no debris field: it is a precision dash, and
   orbiting blocks would fight the theme. Its world weight comes from dust
   and the afterimage residue instead.
3. WORLD INTERACTION. LayeredParticles.emitGroundBurst now fires at the
   charge start (stance settling), at the freeform dash launch and landing,
   at every crescent strike target's feet, and at both ends of the Heaven
   Splitter path. The splitter also throws the debris field above. No block
   breaking or scarring: the standard reserves GroundScar for elemental and
   heavy themes, and the katana's identity is a clean cut.
4. ATMOSPHERE. Heaven Splitter already promised thunder with its sounds;
   it now has the visual: three BoltEntity branched strikes at 25, 55, and
   85 percent of the path at the strike tick, pale cyan white, with the
   thunder and zap sounds moved to that same tick. No weather added: the
   katana has no weather theme.
5. LIFECYCLE. Every katana visual now runs the shared envelope. Crescents:
   FxEnvelope build 25%, decay 35%, and the damage lands at the strike tick
   inside that window, staggered per chain step. The wall: envelope build
   20%, decay 35%, rise completing at 25% of its 40 tick life, damage at
   that exact tick, shock rings re-peaked to the same moment. The charge
   stance and the aim guide fade in with smoothstep and fade OUT over 6 and
   5 ticks respectively on release and on cancel, where both used to be
   discarded mid-frame. During the fades the charge entity rains KI_MOTE
   drizzle through LayeredParticles.emitRain. The aim guide's length is the
   dash's length at the current charge, so the telegraph is honest.

## Bugs fixed (audit issue numbers from fresh-audit-blades.md)

1. FIXED. Ki exhaustion fired Flashdraw. The charge tick now cancels: the
   stance fades out, a cancel flag swallows the coming release, and an
   action bar message says the move was cancelled. No fire, no cooldown.
2. FIXED. Ki exhaustion fired Heaven Splitter at the 0.1 floor. The use
   tick sets a cancel flag before stopUsingItem, and releaseUsing checks
   it first and returns without firing or cooling down.
3. FIXED. The "Delayed simultaneous strike 0.3s (6 ticks) later" comment
   described a delay the code did not have. The queue hack is gone; the
   crescent entity carries the strike and lands it at a real tick, and the
   comment in the item states that timing.
4. FIXED. The "Delayed massive vertical energy cut (0.5s / 10 ticks)"
   comment described a delay the code did not have. The 10 ticks are now
   real: HeavenSplitterEntity.DAMAGE_TICK, with the item and entity
   comments stating it.
5. FIXED. The "snaps to nearest enemy" comment described a snap that does
   not exist. The comment now says the end point is pure look direction.
6. FIXED. Aim guide range mismatch and dead setter. The item creates the
   guide at 16 and calls setMaxRange(16 + 8 x ratio) every use tick, so
   the drawn beam ends where the dash ends at the current charge. The
   entity's synced default also dropped from 24 to 16.
7. FIXED. Crescent variant was synced but unused. Variant 1 now mirrors
   the arc and swaps the cyan lip (0.0, 0.86, 1.0) for pale silver-blue
   (0.55, 0.80, 1.0), in both the body pass and the halo pass.
8. NOT MINE. Hollow Afterimage pitch and owner UUID unused by its
   renderer. That entity and renderer belong to the Hollow's Edge agent;
   untouched.
9. FIXED. HeavenSplitterRenderer's unused seed now drives fracture rib
   placement, and the Random import is used.
10. FIXED. All five dead helpers resolved: drawVerticalBladeWall,
    drawVolumetricBeamZ, drawHorizontalShockPlanes, and drawDiamondSpark
    deleted; drawTransversalFractureRib is now called by the submit.
11. FIXED. SwiftCrescentRenderer's dead drawVolumetricCrescent and
    drawShockRing deleted.
12. FIXED. The HeavenSplitterRenderer header promised features that were
    not drawn (plasma mantle ribbons, fracture ribs, fissures). The header
    now lists exactly what renders, and the fracture ribs it names now
    actually render.
13. FIXED. Charge and aim visuals popped out on release. Both entities
    gained a synced fade start and a beginFadeOut the item calls on
    release and on cancel; renderers multiply alpha by the fade, and the
    aim guide also fades in.
14. DOCUMENTED, behavior kept. Target filters still take any living
    target, not a faction filtered enemy set. The comments that called
    them enemies now say plainly that the dash cuts anything living in
    its path. Whether the katana should spare passive mobs and allies is
    a design call, flagged here for the coordinating pass rather than
    silently changed.
15. FIXED. The empty hurtEnemy override is removed. The class javadoc
    now states the real behavior: plain swings deal attribute damage
    only and cost no Ki; the two charged abilities are what spend Ki.
16. FIXED. Flashdraw chain order was query order. The chain now walks
    nearest first, so the streak bonus, tilt, and scale progressions
    follow the dash path.

Also corrected while in the files: the KatanaItem class header's Ki drain
line (it said 30% per minute for both abilities; they are now 40 and 60),
and the kill reset comment (it said "reset Flashdraw cooldown"; the code
removes the shared katana item cooldown, and the comment in the wall
entity now says exactly that).

## Shared change requests

None required. Three notes for the coordinating pass:

- LayeredParticles.emitVortex takes an age and a life, which does not fit
  an open ended charge whose duration the player controls. The charge
  entity emits the same three layers directly, scaled by
  FxEnvelope.smoothstep build up. If more weapons grow held charges, a
  charge friendly emit helper in LayeredParticles would be worth adding
  once, centrally.
- HollowAfterimageEntity has no lifetime parameter (hardcoded 100 ticks).
  Katana spawns it as is. A lifetime setter owned by the Hollow's Edge
  agent would let dash ghosts be shorter than Hollow's Edge ghosts if
  Carter wants that.
- After a server side Ki cancel, the client keeps sending left click
  charge ticks until the button is released. The item swallows them
  safely, but the client input code (outside my files) could stop the
  charge on the cancel message for a cleaner feel.

## Worse check

Nothing was reverted. Two honest tradeoffs, both deliberate:

- Heaven Splitter damage now lands 10 ticks after the dash instead of
  instantly. A target that leaves the corridor in that half second takes
  no damage, where the old code would have hit it. That is the standard's
  telegraph honesty working as intended: the rising wall is the warning,
  and full charge base damage is unchanged at 2000. Tap damage dropped
  from 1100 to 740 base, which the standard's charge rule required (a
  full charge must be worth at least 2x a tap; it was 1.8x).
- A strike crescent resolves its target by entity ID at the strike tick,
  so its damage follows a moving target while the crescent visual stays
  where the dash passed. The offset window is 4 to 12 ticks and reads as
  the cut finishing on the marked target, but it is a real divergence
  between the visual position and the damage position, stated here
  plainly.

## Verification

Javac audit sweeps on the 26.2 and 26.3 audit classpaths (the sandbox
cannot run the Gradle build; that check stays waived per Carter's
standing decision). Whole tree baselines measured before my edits: 135
error kinds on 26.2, 144 on 26.3. A runtime restart wiped the baseline
kind lists mid session, so the final check is per file: the final sweeps
show zero error kinds in all nine katana files on both classpaths, and
zero errors referencing any new API this pass uses (beginFadeOut,
DebrisSpawner.spawnField, BoltEntity, LayeredRender, FxEnvelope). Whole
tree totals moved during the session (134 to 135 on 26.2, 144 to 149 on
26.3) because other Phase 2 agents were editing the same tree
concurrently; none of that drift is in katana files. One error of mine
was caught and fixed during verification: a lambda local named dist in
KatanaChargeRenderer shadowed the distance local (renamed to moteDist).
No git commands were run and nothing was committed.
