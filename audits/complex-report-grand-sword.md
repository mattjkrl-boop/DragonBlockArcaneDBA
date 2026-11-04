# Complex Effects Redo, Phase 2 Report: Grand Sword

Owner slot: Grand Sword (theme: valor plasma, steel sparks, stone dust).
Base: HEAD 9178eac (Phase 1 foundation). Files edited: only the four
assigned renderers. No commits. No damage number, cooldown, cost,
timing, mechanic, or synced data field changed in any file. The
ValorFieldEntity suspended-projectile behavior and its damage
attribution exception were not touched (renderer only). All existing
material emission kept as the filler layer in every effect.

## Per effect

### GrandBladeShard (class M), GrandBladeShardRenderer.java

- Pieces layer: `ComplexGeometry.shard` through `submitSolid` as the
  blade shard body in both states, steel gradient root 0x767F8C to tip
  0xE9EDF3, peak alpha 0.95. In flight it aligns to the real velocity
  (rotation as fallback), length 1.7, girth 0.30. Embedded, it is set
  back 0.60 along its flight line so it reads as driven into the
  ground. `ComplexGeometry.tendril` through `submitEnergy` with
  ComplexTextures.TENDRIL: in flight a gold wake (root 0xFFD833, tip
  0xFFF3B0, width 0.34, alpha 0.90) streaming 2.35 blocks behind the
  shard with an age-based sway; embedded, one short tendril leaking up
  from the shard base. Vertex estimate per submit: 288 (shard) plus
  384 (tendril) = 672 of the class M 2500 budget.
- Complex sprite layer: in flight, SPARK_STREAK streamed backward at
  28 per second and SHARD_CHIP shed at 11 per second (both under the
  40 per second continuous cap). At the embedding moment (tickCount
  resets to 0 on embedding, so embedded age under 3 ticks is that
  moment), one guarded burst per entity: 16 SPARK_STREAK plus 10
  SHARD_CHIP, 26 of the class M 30 burst budget.
- Envelope discipline: geometry receives the life envelope only
  (build-in times flight fade, or the embedded pulse), never the
  distance scale; distScale goes to the builders as their segment
  scale parameter.

### GrandClashSpark (class M), GrandClashSparkRenderer.java

- Pieces layer: `ComplexGeometry.tendrilFan` through `submitEnergy`
  with ComplexTextures.TENDRIL: three short lashes at the clash point,
  fan axis seeded from the entity UUID seed and tilted up, spread 68
  degrees, length 1.15 times clash scale, width 0.24, gold root
  0xFFD833 to tip 0xFFF3B0, peak alpha 0.95, envelope is the lifecycle
  fade. Vertex estimate: 3 tendrils at 384 = 1152 of 2500.
- Complex sprite layer: one guarded SPARK_STREAK burst of 24 in the
  opening frames (age under 2.5 ticks), under the class M 30 burst
  budget. The existing SPARK fixed preset stays as filler.

### GrandCrescentWave (class M), GrandCrescentWaveRenderer.java

- Pieces layer: two `ComplexGeometry.tendril` submits through
  `submitEnergy` with ComplexTextures.TENDRIL. The body tendril runs
  tip to tip across the real wave span with its control point bowed
  forward 0.95 and up 0.52 of the chord, endpoints sagging 0.28 of the
  chord, width 0.58 times the first-person scale, gold root 0xFFD833
  to tip 0xFFF3B0, peak alpha 0.95, envelope is the wave fade
  (including hit fade). A thinner echo tendril (width 0.30, alpha
  0.88) trails below and 0.42 behind. Vertex estimate: 768 of 2500.
- Complex sprite layer: SPARK_STREAK shed at the front rim through the
  existing front source at 30 per second (under the 40 per second
  cap).

### ValorField (class L), ValorFieldRenderer.java

- Pieces layer, two builder types at class L budget: one
  `submitEnergy` submit (ComplexTextures.TENDRIL) holding six vertical
  `ComplexGeometry.tendril` strands standing at 0.92 of the field
  radius, rising to 0.88 of the dome height, swaying tangentially with
  age, gold root 0xFFD833 to tip 0xFFF3B0, peak alpha 0.92, envelope
  is rise times fade-out. One `submitSolid` submit holding three slow
  steel `ComplexGeometry.shard` meshes suspended in the volume
  (seeded ring positions, gentle bob and spin, steel root 0x767F8C to
  gold tip 0xFFD833), plus one small shard mesh hovering over each of
  up to two actually suspended projectiles, taken from the state's
  existing suspendedRelPositions list (the entity's own synced
  suspended ids, unchanged). Vertex estimate: 6 tendrils at 384 plus
  5 shards at 288 = 3744 of the class L 6000 budget.
- Shader treatment: the rim tendrils are energy pieces on the
  additive shader through submitEnergy; the shards are solid pieces
  on the plain ki type through submitSolid, per the standard.
- Complex sprite layer: WISP_CURL drifting through the volume at 22
  per second (under the 40 per second cap). SPARK_STREAK on suspend
  events: the renderer tracks the suspended count per entity, and
  when it grows, fires a burst of 10 streaks at each newly suspended
  projectile's real position (class L burst budget 60).

## Verification

Targeted javac, JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
-sourcepath src/main/java:src/client/java, private output tags
/tmp/grandsword262 and /tmp/grandsword263:

- 26.2 classpath (cp262.txt): zero errors in owned files, and all
  four owned classes emitted. The closure carries 4 baseline errors
  outside this slot: DbaItems.java (CreativeModeTab Output protected
  access) and DbaMenus.java (MenuType constructor and MenuSupplier
  private access).
- 26.3 classpath (cp263.txt): zero errors in owned files, and all
  four owned classes emitted. The closure carries 10 baseline errors
  outside this slot, in DbaItems.java, DbaMenus.java,
  DbaHostileEntity.java, FriezaSoldierEntity.java,
  NamekianWarriorEntity.java, and PatrolRobotEntity.java.

Correction to the first draft of this section: two earlier runs in
this session were misread as fully clean. The definitive logs showed
a real owned-file error in GrandCrescentWaveRenderer: the tendril
lambdas captured `fade`, which is reassigned by the hit-fade min and
so is not effectively final. Fixed by snapshotting `waveFade` and
`waveFpScale` as final locals before the submits. The verification
results above are from the post-fix reruns, with full logs at
/tmp/grandsword-262.log and /tmp/grandsword-263.log.
git diff --check clean on all four owned files. Zero calls to
drawColoredBox, drawCylinder, drawDisc, drawSphere, or drawHelix in
owned files. Em dash scan of owned files and this report: zero.

## Foundation change requests

None. The shipped toolkit covered every assigned build: shard,
tendril, and tendrilFan signatures, the submit helpers, the TENDRIL
canvas, and MaterialEmitters.Preset.fixed and burst were sufficient.

## Honest worse check

- GrandBladeShard: low risk of reading worse. The pre-redo version had
  no body at all, so any coherent mesh is an improvement; the risk is
  the shard mesh (length 1.7) reading slightly large against the old
  compact plasma core. It is aligned to the real velocity and the
  plasma core still emits inside it, so the silhouette should read as
  a shard with a glowing heart, not a replacement blob. Embedded, the
  setback along the flight line assumes the renderer position is the
  impact point, which the entity code confirms (it snaps to the hit
  position on embedding).
- GrandClashSpark: low risk. The entity lives 24 ticks and the lashes
  use the same lifecycle fade as the burst, so they cannot outlive
  the spark. The one judgment call: the fan axis is seeded but not
  tied to the deflected projectile's direction, because the entity
  exposes no synced direction; the audit asked for lashes at the
  clash point, which is what these are.
- GrandCrescentWave: moderate but accepted risk. A single tendril,
  even a thick torn one, is thinner than the old full-volume plasma
  front (span 4.8, chord 1.55). The echo tendril plus the kept plasma
  front emission fill the volume, and the tendril gives the wave the
  structured leading edge it lacked. If in-game it reads thin, the
  fix inside the standard is a third mid-height tendril, not a revert.
- ValorField: low risk. Rim tendrils at 0.92 radius could in principle
  read as a fence rather than a field edge, but they sway, lean
  inward at the top, and sit inside the existing boundary vapor and
  plasma volume, which carry the dome read. The suspended shards are
  deliberately slow and small so they reinforce the stasis identity
  instead of competing with real suspended projectiles, and the
  per-projectile shard markers are capped at two so a crowded field
  cannot turn into a shard storm.
- Nothing in this slot reads worse than the particles-only version in
  a way that cannot be tuned inside the standard, so nothing is held
  back or flagged for revert.
