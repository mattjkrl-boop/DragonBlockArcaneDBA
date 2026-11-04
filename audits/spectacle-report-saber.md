# Spectacle Report: Saber (Phase 2)

Agent scope: item/SaberItem.java, entity/SaberSlashEntity.java,
entity/SaberDimensionalLineSlashEntity.java, entity/SaberVoidTearEntity.java,
entity/SaberDodgeSparkEntity.java, and their four renderers. HollowAfterimage
was not touched (owned by the Hollow's Edge agent); Saber's spawn of it is
unchanged. Theme kept exactly: dimensional precision, void tears, blitz
chains. Electric cyan and white for slashes and the line beam, violet rim for
void tears, gold for the dodge spark. No shared or common file was edited.

## Before and after numbers

Damage totals are unchanged in formula total unless the row says so; what
changed is WHEN the damage lands. Splits are applied per slice through
DbaEffectsConfig.capDamage, same as the unsplit hits were.

| Value | Before | After |
| --- | --- | --- |
| Blitz arrival damage | 280 + Strength x 1.8, all in the arrival tick | Same total, split: 60 percent on the arrival tick, 40 percent 5 ticks later, inside the 16 tick slash's active window |
| Fencing lunge damage | 280 + Strength x 1.8 (identical formula to a blitz hit) | 200 + Strength x 1.4, instant (a thrust: the slash travels with the hit). The fallback no longer hits as hard as a chained hit |
| Blitz slash 1 lifetime | 10 ticks | 16 ticks |
| Blitz slash 2 lifetime | 8 ticks | 14 ticks |
| Blitz travel time | Fixed 6 ticks at any distance (64 block radius) | 6 to 16 ticks, scaled by hop distance (6 + distance x 0.12, clamped) |
| Lunge slash lifetime | 8 ticks | 12 ticks |
| Lunge slash speed | 1.35 blocks per tick, about 10.8 blocks of travel vs 5.5 block damage reach | 0.46 blocks per tick, 5.5 blocks of travel, exactly the damage reach |
| Flash Step path damage | 400 + Strength x 2.0, all in the teleport tick (harder than a blitz hit) | 320 + Strength x 1.6 total, split: 60 percent on the step tick, 40 percent 5 ticks later, inside the 20 tick tear window |
| Flash Step path slash lifetime | 8 ticks | 14 ticks |
| Void tear lifetime | 12 ticks | 20 ticks (entity synced default also 12 to 20) |
| Perfect Dodge window | Set to now + 15 but tested with <=, so active for 16 game time values | Exactly 15 (test is now <) |
| Dodge spark lifetime | 14 ticks | 20 ticks (entity synced default also 12 to 20) |
| Finisher damage | 850 + Strength x 4.0, one hurtServer per target at animation end | 1000 + Strength x 4.5 total, split across the beam window: 50 percent at beam ignition, 30 percent at beam tick 8, 20 percent at beam tick 16 |
| Finisher snap duration | 12 ticks, comment claimed 0.5s | 18 ticks, comment corrected to 0.9s. The snap is the finisher's telegraph: targets are visibly dragged onto the line before any damage lands |
| Line slash lifetime | 22 ticks | 34 ticks (entity synced default also 20 to 34) |
| Finisher cross slash lifetimes | 12 and 10 ticks | 18 and 16 ticks |
| Finisher bounciness request | 0.80 requested, helper silently clamped to 0.30 | 0.30 requested, matching the DbaPhysicsAttributes timed clamp exactly (helper default duration 20 ticks, unchanged) |
| Finisher cooldown | 100 ticks | 100 ticks, unchanged. Total finisher visual is now snap 18 + beam 34 = 52 ticks, so the cooldown is about 1.9x the full visual instead of 4.5x the old beam alone. Meets the standard: cooldown at least the visual lifetime |
| Finisher debris | 0 pieces | 28 DebrisEntity pieces (density scaled, minimum 8), ground palette sampled at the centroid, helical orbit 26 ticks, rise 3.5 blocks, then rain down |
| Finisher lightning | Thunder and zap as sound only | 1 BoltEntity at the centroid (cyan white 0xFFBFEFFF, length 14, life 8 ticks) plus the sounds |
| Flash Step Ki cost | 75 percent per minute discrete over 40 ticks = 2.5 percent of max Ki, drained BEFORE the immobilized check, so a blocked step still charged | Same 2.5 percent, but the immobilized check now runs first: Ki is charged only when the step actually fires |
| Blitz stamina drain | 100 percent per minute while running | Unchanged |
| Flash Step charges and recharge | 3 charges, 1 per 60 ticks | Unchanged |

## What was added, per point

### Point 1: layered depth
- Every Saber entity now emits real layered particles from its server tick
  through LayeredParticles (the item previously had zero particle calls; its
  only particle lines were two unused imports, now removed):
  - SaberSlashEntity: emitVortex every 2 ticks (KI_MOTE core, cyan dust mid
    band, WIND wisps), envelope scaled.
  - SaberVoidTearEntity: emitVortex every tick (KI_MOTE core, violet dust
    mid, VOID_SMOKE outer).
  - SaberDimensionalLineSlashEntity: emitVortex at three sample points along
    the beam every tick (cyan mid, SPARK wisps), so the beam has a particle
    sheath along its whole length, not just mesh.
  - SaberDodgeSparkEntity: gold emitVortex every 2 ticks (gold mid, SPARK
    wisps).
- SaberSlashRenderer gains a wispy outer crescent at the standard outer band
  alpha (LayeredRender.ALPHA_OUTER) around the existing cyan mid crescent and
  white core: three real layers instead of two stacked copies.
- All four renderers now distance scale their segment counts through
  LayeredRender.distScale and FxEnvelope.segments (slash 28 near to 12 far,
  beam 10/7, rings 20 to 24 near stepping down, helix 18 to 8).

### Point 2: debris
- The finisher (the weapon's finisher class move) spawns a 28 piece
  DebrisSpawner field at the centroid: real ground blocks sampled from under
  the line, three sizes inside DebrisEntity, helical orbit for 26 ticks at
  rise 3.5, then the pieces rain back down inside DebrisEntity itself.
  Nothing vanishes mid air. Sub finisher moves do not spawn debris fields;
  their dissipation is the envelope drizzle under point 5.

### Point 3: world interaction
- Dust at the base: LayeredParticles.emitGroundBurst at the blitz target's
  feet on every arrival, at Flash Step origin and destination, at the dodge
  spark's feet on its first tick, and at the finisher centroid.
- Real mob force: the finisher's snap already drags targets through space;
  on release they now get exactly the bounce the code asks for (0.30, see
  bugs), and the debris field throws the actual ground the move stands on.
- No ground scarring and no block breaking: Saber's theme is precision cuts
  and phasing, not elemental destruction, and the standard reserves scarring
  for elemental and heavy themes. The Flash Step already phases past walls
  without breaking them; that identity is kept.

### Point 4: atmosphere
- Saber has no weather theme and gained none, per the standard. Its
  atmosphere is light and particles: the finisher's thunder sounds are now
  backed by a real branched BoltEntity strike at the centroid, and every
  ability carries the layered particle atmosphere from point 1.
- The registered SABER_HUM sound existed but was never played by this
  weapon; Flash Step now plays it quietly at the destination (audit issue
  14).

### Point 5: lifecycle
- Lifetimes extended so every effect has a build, peak, dissipate arc
  (table above): slashes 8 to 10 become 12 to 16, tears 12 become 20, dodge
  spark 14 becomes 20, line slash 22 becomes 34, finisher snap 12 becomes 18.
- Renderer alpha now rides FxEnvelope alongside the existing fades on the
  void tear (build 0.25, decay 0.35) and the line slash (build 0.15, decay
  0.35), the same curve the entity particles and the damage windows use.
- SaberDodgeSparkRenderer pop in fixed: the burst used to start at 0.9x
  scale and near full alpha on frame one. It now opens over the first 20
  percent of life and grows from 0.55x scale.
- Dissipation drizzle: slash entities emitRain KI_MOTE past 70 percent of
  life, the line slash rains motes along its length past 60 percent, void
  tears rain VOID_SMOKE past 65 percent.
- Damage honesty: blitz arrival, Flash Step path, and finisher damage are
  split across their visuals' active windows through a new PendingHit queue
  processed in tickServer (see numbers table). The finisher's 18 tick snap
  is the telegraph and lands before the first damage slice, and the warning
  size still equals the hit size because the snap line is the hit line.
- Blitz travel now scales with distance and paints a per tick vortex wake
  along the arc, so long hops read as flight with a trail, not a cut.

## Bugs fixed (from the fresh audit's 18 Saber issues)

1. Unused imports (DustParticleOptions, ParticleTypes, bare PlayerStats):
   removed. The item now emits particles for real through LayeredParticles.
2. Dead field BlitzSequence.lastAttackGameTime: removed. The sequence also
   gained the level and travelTicks fields the fixes below needed.
3. Dead sort priority in acquireTarget (unchained first branch that could
   never differ, since chained candidates are filtered out first): removed,
   comparator now sorts by look alignment then distance only.
4. Dead renderer method SaberSlashRenderer.drawLunge: removed. Deliberate
   call, see the worse check: the lunge keeps the proven crescent.
5. Synced IS_DESTINATION never read by SaberVoidTearRenderer: now read. The
   destination tear renders wider (slit x1.30) at full core alpha; the
   origin tear is narrower at 0.8 core alpha. Origin and destination are no
   longer identical except for scale.
6. Void Tear doc mismatch (promised dual counter-rotating rings, drew one):
   the renderer now draws the second, tighter violet ring counter-rotating
   against the cyan ring, both while the strike pulse is active.
7. Line Slash doc mismatch and dead drawTransversalRib: the method is now
   called, five ribs spaced along the beam, and its unused local variable
   was removed.
8. Flash Step charged Ki for a step that never happened: the immobilized
   check now runs before the Ki drain. A blocked step charges nothing and
   spends no charge. (The task's headline defect.)
9. Weapon swap finisher cooled down the swapped to item: tickServer now
   hands the finisher a saber stack (offhand if the saber is there, else a
   bare DbaItems.SABER stack; the cooldown keys by item), so the 100 tick
   cooldown always lands on the Saber.
10. Disconnect cleanup gaps: onPlayerDisconnect now routes through the
    finisher's own restore path. The restore loop was extracted into
    restoreFinisherTargets, used by both the tick loop's disconnect safety
    and the disconnect handler, which restores noPhysics and strips the
    crowd control before dropping the animation. A mid blitz disconnect also
    frees chained targets through releaseChainedTargets, and the player's
    pending split hits are dropped.
11. Immobilized cleanup was partial: it now also restores the player's own
    noPhysics in its block instead of relying on the next tickServer pass.
    It shares releaseChainedTargets with the disconnect path. Known limit,
    not fixed: the 80 tick Judgement Lock applied to every nearby entity on
    launch is not tracked per entity, so cleanup cannot strip it early; it
    expires naturally within 80 ticks.
12. Finisher comment said 0.5s for 12 ticks: snap is now 18 ticks and the
    comment says 0.9s.
13. Bounce value mismatch (0.80 requested, 0.30 applied): the request is
    now 0.30, agreeing with the helper clamp on my side of the call.
14. SABER_HUM never used: played on Flash Step (see point 4).
15. Legacy global tick rate reset on every finisher call: removed. No saber
    code sets a tick rate, so the reset only risked cancelling another
    system's rate change.
16. Lifetime default mismatches in my files: SaberDodgeSparkEntity default
    12 now 20, SaberDimensionalLineSlashEntity default 20 now 34,
    SaberVoidTearEntity default 12 now 20, all agreeing with the spawn
    values. The HollowAfterimage half of this issue is in the Hollow's Edge
    agent's files and is listed as a shared note below.
17. Perfect Dodge window off by one: fixed (see numbers table).
18. Lunge visual overshot its damage: slash speed and life now travel
    exactly the 5.5 block reach (see numbers table).
- Also fixed, from the task brief: the finisher's bounciness agreement (13),
  damage landing in one tick while visuals decorate (point 5 splits), and
  the blitz arrival and lunge sharing one damage formula (lunge is now its
  own 200 + Strength x 1.4).

## Self audit of the diff

- Files changed: exactly the nine owned files. No shared file, registry,
  network, config, or other weapon file was edited. HollowAfterimageEntity
  and its renderer are untouched; Saber's spawn call for it is unchanged.
- Em dashes: none in the changed files (the five pre-existing header em
  dashes in my files were also replaced with colons).
- Compile check: targeted javac of the nine files with sourcepath against
  the audit classpaths. On 26.3: all nine files compiled to class files with
  zero errors in any Saber owned file (the only errors in that run were
  pre-existing and in other weapons' files pulled in through the
  sourcepath). On 26.2: the same targeted compile produced 9 errors, every
  one in a file outside this weapon's scope (SpiritImpaleEntity,
  ZShockwaveEntity, DbaMenus, BlasterGunItem, CurseBladeItem, DbaItems,
  SpiritSwordItem: other agents' in flight work and baseline noise pulled
  in through the sourcepath), and zero errors in any Saber owned file.
  This is a javac audit compile, not a Gradle build; the sandbox cannot
  run the Gradle build and none is claimed.
- Behavior re-read: the PendingHit queue is processed at the top of
  tickServer before finishers, skips dead or removed targets, and falls
  back to a generic damage source if the player is gone, matching the
  finisher's existing fallback. Split slices pass through capDamage per
  slice, so a configured damage cap applies per slice rather than per
  event; at the default cap of 0 (uncapped) totals are exactly the table
  values.
- The immobilized cleanup, disconnect handler, and tick loop safety now
  share two helpers (releaseChainedTargets, restoreFinisherTargets), so the
  three end paths cannot drift apart again.

## Shared change requests

None required for this weapon. Two notes for the coordinating pass:

1. Audit issue 16, second half: HollowAfterimageEntity hardcodes its 100
   tick life in the entity and duplicates it as a hardcoded maxLife in
   HollowAfterimageRenderer. That is the Hollow's Edge agent's file pair;
   flagged here only because the Saber section of the audit recorded it.
2. If the coordinating pass wants split damage slices to respect a
   configured global damage cap per event instead of per slice, that change
   belongs in DbaEffectsConfig or the queue design and should be decided
   once for all weapons, not per weapon.

## Honest worse check

- One deliberate non-upgrade: SaberSlashRenderer.drawLunge, the thrust
  geometry written for the fencing lunge, was dead code. Wiring it in would
  have replaced the lunge's proven crescent with a thrust whose axis
  alignment against the moving entity cannot be visually verified in this
  sandbox (no client can run here). A wrong axis would make the lunge look
  worse, which the standard counts as a fail, so the dead method was
  removed and the lunge keeps the crescent it already had, now with matched
  travel distance. If Carter wants the thrust look, it is a small follow up
  to verify in game first.
- Split damage changes feel in one honest way: a blitz target can now
  survive the first 60 percent slice where the old full hit would have
  killed instantly, and the finisher's last 20 percent lands 16 ticks after
  the beam ignites. That is the standard's own requirement (damage inside
  the visual's window, telegraph first), totals are equal or higher, and no
  part of it was reverted.
- Nothing else was reverted. Every other change is additive layering,
  lifetime, or a verified bug fix, and no ability lost damage, reach, or
  uptime except the lunge and Flash Step path adjustments recorded in the
  numbers table, which were made to fix the identical formula and the
  movement ability out damaging the chain hit.
