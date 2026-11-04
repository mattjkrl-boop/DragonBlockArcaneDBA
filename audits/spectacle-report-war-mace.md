# Spectacle Report: War Mace

Phase 2 weapon pass, built to audits/SPECTACLE-STANDARD.md from the before
picture in audits/fresh-audit-technique.md section 6. Theme unchanged: brutal
weight, bronze shell and crescent, the heavy smash.

Files changed (mine only):
- entity/WarMaceSmashEntity.java (rewritten tick, impact system, falloff)
- entity/WarMaceRiseEntity.java (climbing hit volume, particles, impact feedback)
- client/render/WarMaceSmashRenderer.java (three layer shell, shared envelope)
- client/render/WarMaceRiseRenderer.java (trail layers, launch ring, shared envelope)

item/WarMaceItem.java is unchanged. Every fix the task named lives in the
entities; the item's numbers were verified from the code and are reported
below as the before values.

The audit section's entity set is Smash plus Rise. The Rise is the weapon's
swing (it plays PLAYER_ATTACK_SWEEP and arcs a crescent), so "the swing" in
the task maps to WarMaceRiseEntity and its renderer.

## Before and after numbers

| Value | Before | After |
|---|---|---|
| Melee attribute damage | 1300 (1299 + 1) | unchanged |
| Attack speed modifier | -3.4 | unchanged |
| Ground smash damage | 900 + Strength x 2.5, flat out to the 4.5 rim | 900 + Strength x 2.5 at the center, falling linearly to 35 percent (315 + Strength x 0.875) at the rim |
| Heavy smash damage | 1600 + Strength x 4.0, flat out to the 7.6 rim | 1600 + Strength x 4.0 at the center, falling linearly to 35 percent (560 + Strength x 1.4) at the rim |
| Ground smash radius / life | 0.7 to 4.5 over 10 ticks | unchanged |
| Heavy smash radius / life | 0.7 to 7.6 over 14 ticks | unchanged |
| Smash damage volume position | frozen at the cast position and cast height for the whole life | follows the falling owner every tick until the owner lands or the shell first connects, then plants |
| Smash knockback | 0.25 out + 0.08 up | unchanged (deliberate: mobs stay under the next falling smash for the relaunch loop) |
| Ground lock / heavy lock / item cooldown | 10 / 16 / 5 ticks | unchanged (both locks already meet the standard: cooldown at least the visual life, 10 = 10 and 16 > 14) |
| Rise damage | 480 + Strength x 1.5, all of it on tick 1 | 480 + Strength x 1.5 per target (same total, once per target), delivered on first contact during ticks 1 to 6 while the hit volume climbs 0.22 per tick with the rendered crescent |
| Rise entity life | 8 ticks, 1 damage tick, 7 pure render ticks | 8 ticks, damage window ticks 1 to 6, particles on all ticks |
| Rise launch on hit | 0.45 out + 0.95 up | unchanged |
| Rise lock / item cooldown | 18 / 6 ticks | unchanged |
| Stamina cost (the mod's Ki equivalent) | 125 percent per minute, paid discretely: ground smash over 28 ticks, heavy over 48, rise over 22; charged only when the move fires | unchanged |
| Particles emitted by both entities | 0 | per tick layered vortex on both entities, plus impact burst and decay rain (point 1 and 5) |
| Debris pieces per smash | 0 | 24 ground / 36 heavy, three sizes (0.45, 0.7, 1.0 from DebrisSpawner), palette sampled from the real ground |
| Ground scar | none | fracture disk radius 3 ground / 5 heavy, surface cells only, restored after 200 / 300 ticks |

## What was added, per point

1. Layered depth. Smash renderer: the original bronze shell stays as the
   bright core; under it an anchor glow disc (LayeredRender.drawAnchorGlow)
   and three counter rotating dust bands (LayeredRender.drawVortexBands at
   0.82 of the shell radius, standard band alphas 0.85 / 0.45 / 0.18 times
   the envelope) carry the ring. Segment counts and band detail scale with
   camera distance through LayeredRender.distScale. Server side, both
   entities emit LayeredParticles.emitVortex every tick (bright KI mote
   core, bronze dust mid band, POOF wisp outer on the smash; KI mote outer
   on the rise). Rise renderer: the crescent is now four layers, two lagging
   trail ghosts (0.40 and 0.20 alpha, offset 0.28 and 0.56 below), one wide
   faint wisp crescent (1.3 radius, 0.30 alpha), and the main crescent.
2. Debris. On impact, DebrisSpawner.spawnField throws 24 (ground) or 36
   (heavy) real ground blocks in three sizes on helical orbits; DebrisEntity
   itself releases them, rains them down, and fades them on landing. Heavy
   clears the standard's 24 piece finisher minimum; this is not a storm or
   domain, so the 60 piece tier does not apply.
3. World interaction. Base dust burst on every impact
   (LayeredParticles.emitGroundBurst, bronze dust plus POOF). GroundScar
   fracture disk on grounded impacts: dirt family tears to coarse dirt,
   stone family crushes to gravel, stone bricks, blackstone bricks,
   deepslate bricks and tiles swap to their cracked forms, deepslate
   crushes to cobbled deepslate. Columns scan 2 up to 6 down so the disk
   follows slopes; placement chance falls from 0.9 at the center to 0.45 at
   the rim; GroundScar restores everything after 200 (ground) or 300
   (heavy) ticks and refuses protected blocks internally. No permanent
   destruction, matching the standard for a non-finisher spam loop move.
4. Atmosphere. The mace has no weather theme and gains none, per the
   standard. Its atmosphere is light and weight: the layered glow bands,
   the anchor glow, spark bursts, and camera shake. A heavy smash now also
   fires DbaImpactFx.shake (2.4 intensity, 14 ticks, hitstop 3) at the true
   landing point; before, the only shake fired at cast height where the
   landing was not felt. The rise's first connect fires DbaImpactFx.medium
   plus a 10 spark burst at the target.
5. Lifecycle. Both renderers run FxEnvelope (smash build 15 percent, decay
   35 percent; rise build 12 percent, decay 30 percent). The smash shell no
   longer appears at full alpha. The impact beat happens at the honest
   moment: tick 1 for the ground smash, the landing tick or first connect
   for the heavy smash, with a final tick fallback projected onto the
   ground below (burst only, no scar or debris, if the ground is more than
   24 blocks down). In the decay window the smash emits settling dust rain.
   The scar outlives the shell by minutes of ticks then restores; the
   debris rains down after the shell is gone. The rise's damage window now
   sits inside its visual climb instead of ending on tick 1 of 8.

## Bugs fixed

- Flat damage: the heavy smash paid its full 1600 + Strength x 4.0 at 0.7
  blocks and at 7.6 blocks alike (audit: line 101 of the old entity). Now
  every hit is multiplied by a distance falloff, 1.0 at the center to 0.35
  at the rim, for both smash variants.
- Frozen damage volume: a heavy smash cast mid fall kept its box and its
  rendered shell at the cast height while the player kept falling. The
  entity now rides the falling owner (position set from the owner each
  tick) and plants on landing or on first connect; the shell, scar,
  debris, and landing shake all happen at the real landing point.
- Rise damage honesty: the rise entity dealt everything on tick 1 and then
  existed only as a render carrier for 7 of its 8 ticks. The hit volume
  now climbs with the crescent across ticks 1 to 6 (lift (tick - 1) x
  0.22, the renderer's own rate), so targets are hit when the visual
  reaches them.
- Comment honesty: the old smash class comment claimed hits travel with
  the lip, but ticks 1 to 2 skip the lip test entirely. The new javadoc
  says what the code does: ticks 1 to 2 are a core blast, then the lip.
- Pop in: the smash shell's old fade only ever decreased from full alpha
  at spawn. It now builds in through the shared envelope.

## Shared change requests

None. Every shared block I needed already exists with the signature I
called: FxEnvelope.envelope and segments, LayeredParticles.emitVortex,
emitGroundBurst, emitRain and dust, DebrisSpawner.spawnField,
GroundScar.scar, LayeredRender.drawVortexBands, drawAnchorGlow and
distScale, DbaImpactFx.shake and medium.

## Verification (javac audit, not a Gradle build)

Full tree sweeps against the real 26.2 and 26.3 jars, baseline worktree at
5e4b1de: 132 error kinds on 26.2, 141 on 26.3, matching the Phase 1 record.
A clean current tree comparison was not possible because every Phase 2
agent is editing the same tree at once and the mid flight current sweep
(135 kinds on 26.2) mixes all agents' unfinished files. So the scoped
proof: targeted javac compiles of my five files (item, both entities, both
renderers) with sourcepath over the whole tree, baseline worktree versus
current tree, on both classpaths. Baseline reachable kinds: 3 on 26.2
(DbaMenus, DbaItems mapping artifacts), 8 on 26.3. Current: 5 on 26.2 and
12 on 26.3, and every new kind is in another agent's in progress file
(ZShockwaveEntity, CurseBladeItem, KingsSlamEntity, OxShockwaveEntity).
Zero error lines name any War Mace file in any of the four error dumps.
No em dashes in any changed file (byte level check).

## Worse check

Nothing reverted. Three honest notes:

- The falloff is a real nerf at range and it is the point: the core hit is
  untouched (900 / 1600 plus Strength scaling at the center), so the
  weapon's job, point blank weight, is exactly as strong as before. Fights
  at the rim no longer end on tick 1 of a 14 tick visual, which the audit
  flagged as the numbers holding the spectacle back.
- Rise targets near the top of the crescent are now hit a few ticks later
  than before (when the climb reaches them instead of instantly). Total
  damage per target is identical and the launch is unchanged.
- One risk I cannot retire in this sandbox: shell plus bands plus anchor
  glow at point blank range could read busy. The alphas are the standard's
  shared band values times the envelope, the same language every other
  weapon in this pass uses, and distance scaling steps the geometry down.
  The Minecraft client cannot run here, so the final look call is Carter's
  on his PC, same as the rest of the pass.
