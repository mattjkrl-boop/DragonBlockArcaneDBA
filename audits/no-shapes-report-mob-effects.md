# No-Shapes Realism Pass: Mob Ability Effects Report

Owner: Mob Ability Effects. HEAD at start: ad22b32 (Phase 1 foundation). Files edited, and only these: src/main/java/com/dragonblockarcanedba/entity/FriezaSoldierEntity.java, src/main/java/com/dragonblockarcanedba/entity/SaibamanEntity.java, src/client/java/com/dragonblockarcanedba/client/fx/StaggerFx.java. No shared files edited, no commits. Mob models are physical objects and were not touched, per the audit.

The audit found zero BespokeGeometry, LayeredRender, or direct vertex calls in this area before the pass, and that is still true after: before B/L/V 0/0/0, after B/L/V 0/0/0 in all three files (grep verified, including KiRenderHelper references: none). This area was a material conversion of particle presentation only.

Split source set handling: the two entity files are common source set code, so they send the Phase 1 DbaMaterialOptions through ServerLevel.sendParticles, the server-sent route the Phase 1 report confirms. They do not reference the client MaterialEmitters toolkit. StaggerFx is client code and uses MaterialEmitters directly.

## Frieza Soldier ranged hit feedback

Audit row: plasma sparks and a brief vapor plume at the hit point; the hit itself stays instant under the standing Scope 2 D4 decision. The file contains no separate aura presentation surface; the hit feedback call was the only presentation call in the entity.

- Before: 1 particle call, DbaParticles.CRIT_SPARK x8 at the target eye position, spread 0.2 / 0.25 / 0.2, speed 0.02.
- After: 2 server-sent DbaMaterialOptions calls at the same position, spread, and speed, total count unchanged at 8:
  - PLASMA, color 0xFFD98A, scale 0.95, x5. The warm pale gold carries the old CRIT_SPARK color (its style ran 1.0 / 0.98 / 0.85 toward 1.0 / 0.75 / 0.30).
  - VAPOR, color 0xE8E2D6, scale 0.8, x3, the brief plume.
- Unchanged: instant hit (no projectile added), 5.0 ki damage through DbaDamage.ki, 40 tick shot cooldown, range and line of sight checks, swing, FIRE_SHOT sound and its volume and pitch, all stats and AI.

## Saibaman self-destruct burst

Audit row: physical slime chunks stay physical; add green vapor and dust so the burst is not only chunks.

- Before: 1 particle call, BlockParticleOption on DbaParticles.BLOCK_CHUNKS with the slime block state, x18 at center height +0.6, spread 0.4, speed 0.04.
- After: 3 server-sent calls at the same center, spread, and speed:
  - BLOCK_CHUNKS slime chunks x18, byte-identical call, still the physical material of the burst.
  - VAPOR, color 0x69B84A, scale 0.9, x10, green vapor in the Saibaman green family.
  - DUST, color 0x86A860, scale 0.85, x8, muted green dust.
- Unchanged: burst trigger (health at or below 5 with a target), 2.5 block area, 8.0 ki damage through DbaDamage.ki, exclusion of other Saibamans, BOOM_BIG sound, poison claws, all stats and AI, and the discard at the end of the burst.

## Hostile boss stagger feedback (StaggerFx)

Audit row: impact dust and plasma in the attacker theme; no geometry existed here. Before this pass StaggerFx carried no particle presentation at all: it stored strength and progress for the renderer rock and dip mixin, and that store is unchanged.

- Before: 0 particle calls.
- After: on BossStaggerPayload receipt, after the entry is stored exactly as before, emitImpact spawns two MaterialEmitters bursts sampled from a sphereVolume source around the staggered entity center (data only, no mesh):
  - DUST, color 0xC2B49A, scale 0.9, count 10 + round(8 x strength), gentle upward base velocity.
  - PLASMA, color 0xFFCA6A, scale 0.9, count 6 + round(6 x strength).
  - Counts scale with the payload strength the server already sends (MobPhysics clamps it to at most 1.2), and MaterialEmitters applies the effects density config on top.
- Unchanged: payload handling, entry store, strength and progress reads, the 16 tick duration the server sends, tick expiry, and the renderer animation that consumes them. If the entity is not loaded on the client when the payload lands, the burst is skipped and the animation still applies; no state changes either way.

## Change request (shared foundation gap)

Exact attacker theme for the stagger burst is not reachable inside StaggerFx today. BossStaggerPayload carries only entityId, strength, and durationTicks; no attacker identity, weapon, or theme color reaches the client store, and MobPhysics and BossStaggerPayload are shared files outside this ownership. The burst therefore uses a neutral warm plasma and natural dust, which read as a heavy impact on any boss. To do the audit row literally, the coordinating pass would extend BossStaggerPayload with a theme color (or attacker weapon id) and have MobPhysics.maybeStagger fill it from the damage source it already inspects. One-line consumer change in StaggerFx after that: pass the payload color into the PLASMA preset instead of 0xFFCA6A.

## Verification (javac audit only, no Gradle claim)

Targeted javac with full sourcepath closure on both audit classpaths, the three owned files as roots:

- 26.2: zero errors naming FriezaSoldierEntity or SaibamanEntity; both classes emitted. StaggerFx, recompiled alone after a fix during verification (the sphereVolume factory lives on MaterialEmitters, not on the Source interface; the first run caught that mistake in my own new lines and it is corrected): zero errors naming StaggerFx, class emitted. Residual closure errors are the known baseline kinds in DbaItems and DbaMenus only.
- 26.3: zero errors on any converted line in any owned file; StaggerFx class emitted, with residual closure errors only in the known baseline files (DbaItems, DbaMenus, DbaHostileEntity, NamekianWarriorEntity, PatrolRobotEntity). One pre-existing baseline kind names an owned file on an untouched line: FriezaSoldierEntity line 51, swing(InteractionHand) against the 26.3 signature. That line is context, not part of this diff, and audits/scope2-report-mobs.md already documented the identical kind at the ef5ad02 baseline. No new error kinds were introduced by this conversion on either version.
- Grep proof: CRIT_SPARK references in FriezaSoldierEntity: 1 before, 0 after. DbaMaterialOptions send calls added: Frieza 2, Saibaman 2. MaterialEmitters.burst calls added: StaggerFx 2. BespokeGeometry, LayeredRender, addVertex, and KiRenderHelper references in the three owned files: 0 before, 0 after.
- Em dash scan of the edited files and this report: zero.

## Honest worse check

- Frieza: the spark portion drops from 8 sharp crit pips to 5 plasma sprites, with 3 vapor sprites making the total back up. The hit now reads as a warm energy flash with a small vapor plume instead of a star burst. That is the audit row as written, but if Carter liked the sharp crit read, this is the piece to judge on his PC test. The instant hit itself did not gain a projectile or a travel effect, per D4.
- Saibaman: the burst's one-tick particle count rises from 18 to 36. The chunks are unchanged and the additions are the audit's ask, but server-sent particles are not scaled by the client effects density config, so the added 18 always spawn. It is a single burst on death throes, not a continuous emitter, so the cost is one frame.
- Stagger: the plasma is neutral warm, not the attacker's theme color, for the payload reason above. It is recorded as a change request rather than hidden. Nothing was reverted; no conversion read as mechanically worse, and the two visual trades (softer Frieza hit read, neutral stagger plasma) are stated here for Carter's look pass.
