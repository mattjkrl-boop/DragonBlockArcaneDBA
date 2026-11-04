# Spectacle report: Lava Sword

Phase 2 weapon pass, from audits/SPECTACLE-STANDARD.md and the Lava Sword section
of audits/fresh-audit-heavy.md. Files owned and edited: item/LavaSwordItem.java,
entity/LavaCleaveEntity.java, entity/LavaEruptionEntity.java,
entity/LavaSwordGround.java, client/render/LavaCleaveRenderer.java,
client/render/LavaEruptionRenderer.java. Nothing outside those files was edited.
Theme unchanged: living lava and ground that burns.

## Before picture (from the fresh audit, verified in the code)

- Melee hurtEnemy was an empty no-op: the sword's most common hit applied no burn.
- Both effects emitted zero particles. Each renderer was a single mesh pass with
  no core, mid, outer structure and no distance scaling.
- No debris of any kind.
- Ground burn was the strongest block interaction in the set, but its restore
  list was an in-memory static ArrayList: a crash or hard kill left real lava
  sources and fire in the saved world permanently, flowing like natural lava.
- Eruption damage cone was about 162 degrees (dot at least 0.15) while the
  renderer drew about 132 degrees (arc 1.15 x 2 radians): side targets took
  full damage from a wave that never visually reached them.
- protectedSolid was consulted against the placed cell and the restore state,
  both always air, so it could never fire in either place.
- Cleave had no knockback at all; the whole weapon was over in short timescales
  (10 tick cleave, 12 tick eruption) with no build-up phase anywhere.

## Numbers: before and after

| Value | Before | After |
| --- | --- | --- |
| Melee hit burn | none (empty hurtEnemy) | 60 ticks fire, plus 6 FLAME particles on the target |
| Melee damage attribute | 859 (+1 base = 860) | unchanged |
| Cleave damage | 740 + Strength x 2.0 | unchanged |
| Cleave knockback on hit | none | about 0.35 horizontal along the flight line, +0.18 up |
| Cleave flight life | 10 ticks (about 13.5 blocks) | 12 ticks (about 16.2 blocks) |
| Cleave lava budget | 3 sources | 4 sources |
| Cleave hit burn | 60 ticks | unchanged |
| Cleave cooldown | 8 ticks | unchanged |
| Cleave Ki | drainKiDiscrete 90.0 per 14 ticks | unchanged |
| Eruption damage | 880 + Strength x 2.2 | unchanged |
| Eruption cone | dot at least 0.15, about 162.7 degrees total | dot at least 0.4085, about 131.8 degrees total, equal to the drawn arc |
| Eruption push on hit | 0.45 forward, +0.28 up | 0.60 forward, +0.40 up |
| Eruption lava budget | 5 sources | 8 sources |
| Eruption life and reach | 12 ticks, 1.1 to 6.5 blocks | unchanged |
| Eruption hit burn | 80 ticks | unchanged |
| Eruption cooldown | 40 ticks | unchanged (still exceeds the 12 tick visual life) |
| Eruption Ki | drainKiDiscrete 90.0 per 40 ticks | unchanged |
| Eruption debris at cast | none | 24 pieces (netherrack, magma block, basalt, blackstone) |
| Cleave debris on block hit | none | 12 pieces, same palette |
| Ground burn life | 80 ticks | 80 ticks, unchanged and now crash persisted |

Ki is still charged only inside performCleave and performEruption after the
cooldown and same-tick guards, so a blocked cast still never charges.

## What was added, per point

1. LAYERED DEPTH. Both entities now call LayeredParticles.emitVortex every
   server tick: KI_MOTE core, theme dust (0xFF5A00) mid, FLAME wisps outer,
   counts driven by the shared envelope and the config density. The cleave
   rides it along its flight path; the eruption scales its radius with
   reachAt. Renderer side, the cleave gained a bright core crescent (radius
   0.78) nested inside the main mid crescent, with the existing trailing
   crescent as the outer layer. The eruption gained an inner core sheet at
   0.55 reach inside the main sheet, plus a LayeredRender anchor glow at the
   origin. Both renderers now scale segment counts with
   LayeredRender.distScale.
2. DEBRIS. The eruption throws 24 DebrisSpawner pieces at cast from a lava
   palette (netherrack, magma block, basalt, blackstone, three sizes from the
   shared spawner), helical orbit for 16 ticks, then they rain back down. The
   cleave throws 12 pieces where it lands on a block.
3. WORLD INTERACTION. The ground burn system is unchanged in reach and rules
   and is now crash safe (below). LayeredParticles.emitGroundBurst fires at
   the eruption's base on cast and at the cleave's impact point. Mobs are
   moved now: the cleave shoves targets along its flight line, and the
   eruption push rose from 0.45/0.28 to 0.60/0.40. Placement now also refuses
   ground whose top block is on the protection list.
4. ATMOSPHERE. No weather theme exists on this weapon and none was invented,
   per the standard. Its atmosphere is light and particles: the real lava
   light (already there), the layered flame wisps, and ember rain as the
   cleave dies. Nothing darkens, correctly for a lava weapon.
5. LIFECYCLE. Both renderers now multiply alpha by FxEnvelope.envelope
   (build over the first 20 percent, decay over the last 30), replacing the
   old pop-in at full alpha. The cleave's life went 10 to 12 ticks so the
   envelope has room, and over its last 2 ticks it calls
   LayeredParticles.emitRain so it sheds embers as it dies. Eruption damage
   already landed inside the wave's annulus each tick and still does; the
   telegraph is now honest because the drawn arc and the damage cone are the
   same angle. Ground cells still restore on their exact per-cell 80 tick
   schedule, now including across a crash.

## Bugs fixed

1. CRASH PERSISTENCE HOLE (the headline). LavaSwordGround's PENDING list was
   in-memory only. Every pending restore is now mirrored into the level's
   SavedData at placement (Entry record with pos, serialized original,
   absolute restore game time, lava flag, and for lava cells the 26 neighbor
   snapshots), removed from the store when the cell is restored, dropped, or
   restored at shutdown. On the first level tick of a server run,
   adoptPersisted takes the saved entries back: overdue ones restore
   immediately, live ones resume their tick watch with neighbor snapshots
   intact, and entries whose cell no longer holds our burn are dropped so a
   block placed while the server was down is never stomped. The restore time
   is stored as an absolute game time, so the 80 tick life stays exact across
   the crash. Registration now also happens from the item's class init, so
   recovery runs from the first tick instead of after the first swing.
2. DEAD protectedSolid CHECKS. The list was consulted against the placed cell
   (always air, placement requires air) and against the restore state (always
   the same air), where it could never fire. Both consults are removed. The
   list now guards something real at placement: the ground block under the
   burn, so fire and lava are never placed directly on top of a chest,
   trapped chest, ender chest, barrel, spawner, trial spawner, or bedrock.
   Its existing real work in hold(), guarding neighbor reverts, is unchanged.
3. ERUPTION CONE VS DRAWN ARC. Damage and scorch used dot at least 0.15
   (about 162.7 degrees); the renderer draws 1.15 radians half angle (about
   131.8 degrees). All three now use the drawn angle: the entity's
   CONE_MIN_DOT is 0.4085, cos(1.15), used by the damage check and passed
   into scorchArc through its new minDot parameter. LavaSwordGround was the
   only caller set, so the signature change touches nothing else.
4. MELEE IDENTITY. hurtEnemy was an empty no-op on a sword made of lava. A
   plain hit now sets the target's remaining fire ticks to at least 60, the
   same burn the cleave applies, with a small FLAME particle spit.

## Kept intact (regression check against commit 94708b6)

- Budgeted lava sources still place deterministically first on the first
  eligible surface cells; no random roll was reintroduced.
- scorchLanding still puts the cleave's pile on top of the exact block hit,
  and scorchColumn still climbs up to 4 cells out of a solid hint cell.
- Placement flags unchanged (UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE |
  UPDATE_SKIP_ON_PLACE): lava still does not flow or cook neighbors on
  placement, block and fluid ticks are still cleared while pending, the
  neighbor hold watch still reverts flow, conversions, and stray fire.
- Water-adjacent cells still fall back to fire; player-occupied cells are
  still skipped; SERVER_STOPPING still restores everything (and now also
  clears the saved entries).

## Shared change requests

1. Optional cleanup for the coordinating pass: add LavaSwordGround.register()
   next to GroundScar.register() in DragonBlockArcaneDBA.onInitialize, and
   the static block in LavaSwordItem can then be dropped. What is in place
   now works without it (the hooks register when the item class loads, which
   happens during item registration), but the initializer is the cleaner home
   and matches how GroundScar does it. The register() call is idempotent
   (AtomicBoolean), so having both is harmless in the meantime.
2. Deliberate non-change to flag: I did NOT reimplement LavaSwordGround on
   top of GroundScar.scar. GroundScar places with Block.UPDATE_ALL, which
   runs the fluid's on-place, so a lava source would schedule flow and cook
   neighboring water, exactly what this system exists to prevent. Its restore
   test also compares serialized block states, and a fire state's AGE
   property changes as it burns, so the comparison would silently skip
   restores. The SavedData store here follows GroundScar's pattern (same
   SavedDataType shape, same restore-overdue-on-start behavior) while keeping
   the no-flow flags, tick clearing, and neighbor hold. If the coordinator
   wants one system later, GroundScar would first need placement flags and a
   non-serialized restore test.

## Verification

- Baseline before edits (full javac sweeps at HEAD 5e4b1de, -Xmaxerrs 10000,
  both audit classpaths): 144 error kinds on 26.2, 151 on 26.3. Lava files
  carried zero kinds on 26.2 and exactly one on 26.3 (below).
- After edits, my six files were compiled with javac on both classpaths
  (sourcepath closure, so everything they touch compiles with them). On 26.2:
  zero errors in my files; the 6 errors present are all in other files
  (DbaItems, DbaMenus, ZShockwaveEntity, CurseBladeItem). On 26.3: exactly
  one error in my files, LavaSwordGround's blocksMotion() call, which is the
  same pre-existing baseline kind (line 163 before this pass, line 212 after
  the header additions; the call itself is untouched). The other 14 errors
  are in other files. A full-tree 26.2 sweep after the edits measured 135
  total kinds, down from the 144 baseline; the full-tree 26.3 rerun was lost
  when /tmp was wiped mid-verification, so the 26.3 claim rests on the
  targeted closure compile above.
- Pre-existing, noted, not mine to fix here: the 26.3 audit jar has no
  blocksMotion() on BlockState, so that one call reads as an error kind
  against 26.3 only. It predates this pass and changing it risks the 26.2
  behavior, so it is left for the coordinating pass to judge.
- The Gradle build was not run: the sandbox cannot connect to the Gradle
  daemon (known restriction, compile check waived by Carter). No build pass
  is claimed. The final look in game is Carter's to judge on his PC.

## Worse check (honest)

- The cone narrowing is a real coverage reduction: targets between about 132
  and 162 degrees no longer take eruption damage or ground burn. That is the
  honesty fix working as intended; they were being hit by a wave that never
  visually reached them. It is offset by the stronger push (0.45/0.28 to
  0.60/0.40) and the bigger lava budget (5 to 8). Net: fair trade, kept.
- The cleave now starts dimmer than before (envelope build over roughly the
  first 2.4 ticks instead of full alpha at spawn). The crescent's existing
  open animation covers the same window, so it reads as the crescent forming.
  This is the standard's lifecycle rule applied as written. Kept.
- Debris worst case: eruption debris lives up to 16 + 20 orbit ticks plus
  fall time against a 40 tick cooldown, so a few pieces of the last field can
  still be falling when the next cast fires. Counts are density scaled in the
  shared spawner path. Acceptable, noted rather than hidden.
- The eruption anchor glow is a full disc under a frontal wave; its radius is
  capped at 2.2 blocks at the caster's feet, inside the area the wave and the
  ground burn actually cover at the origin, so it reads as a base splash and
  does not claim side or rear ground.
- Nothing was reverted: no change made the weapon look or play worse against
  the audit criteria. The renderer edits are additive passes and envelope
  multipliers on the existing geometry; the geometry, palette, and lifetimes
  that carry the weapon's identity are untouched except the cleave's 10 to 12
  tick life, which lengthens the same look rather than replacing it.
