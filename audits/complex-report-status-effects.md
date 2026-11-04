# Complex Effects Redo, Phase 2: Status Effects Report

Owner slot: Status Effects. Scope: the twenty-seven classes in
`src/main/java/com/dragonblockarcanedba/effect/` named in the Status Effects
section of audits/complex-audit.md. Base: dba HEAD 9178eac (Phase 1
foundation), with other owners' uncommitted changes in the same tree left
untouched. No commits made.

Result: twenty-five classes gained exactly one complex sprite layer each, as
one added `level.sendParticles(DbaParticles.<TYPE>, ...)` call per file,
placed inside the same gated block as the existing material emission and
mirroring the primary material call at that point (same position variables
or position expression pattern, same count, same spread, same speed). The
remaining two classes are the recorded exemption (below). The diff for this
slot is 25 files changed, 199 insertions, 0 deletions: no HEAD line was
modified, so themes, colors, every damage number, cooldown, cost, timing,
mechanic, tick gate, and synced data field are byte-identical to HEAD.
Emission runs through ServerLevel.sendParticles, the same server particle
path the existing material emission already uses in these files. DbaParticles
is common code and references no client classes, and no file in this slot
references anything client side: the split source sets stay clean. No
geometry was added anywhere in this slot (class S, per-entity auras).

Budget check: the largest complex count in the slot is 3 per emission
(DevilsHandsEffect, StormOfDarknessEffect), well inside the class S burst
cap of 12. The highest continuous complex rate is DevilsHandsEffect at 3 per
2 ticks (30 per second), inside the 40 per second continuous cap. Every
other effect runs at 13 per second or less.

## Per family

### Hellfire: EMBER_FRAGMENT

- DemonSurgeEffect: ember fragment inside the existing `% 2` tick block,
  count 1, at the same px, py, pz and upward drift as the crimson smoke call
  it rides with.
- DevilsHandsEffect: ember fragment inside the existing `% 2` tick block,
  count 3 with the same spread and upward drift as the dark red plasma call.
  Presentation only: the damage chance (0.08f + amplifier * 0.03f freeze
  trigger), the damage gate (0.1f + amplifier * 0.05f), the damage formula
  (max of 5.0f and 1 percent of max health per level), the hellfire damage
  source, and the freeze velocity code are all untouched HEAD lines. Note,
  as in the previous pass: the HEAD javadoc of this file contains a
  pre-existing em dash that is not part of this change; it was left as it
  was, and no added line in this slot contains an em dash.

### Earth and stone: SHARD_CHIP

- AncientWeightEffect: shard chip inside the `% 3` tick block, count 1, at
  the ground position (y + 0.1) of the goldenrod dust call.
- EarthShatterEffect: shard chip inside the `% 3` tick block, count 1, after
  the existing block chunks and dust calls, same position and spread. This
  file already imported DbaParticles for BLOCK_CHUNKS, so it gained no new
  import.
- FissureStunEffect: shard chip inside the `% 3` tick block, count 2, same
  center position and width-based spread as the tremor dust call.
  Presentation-only change, exactly as in the previous pass: the dust color
  0xEBE8E0 and every other HEAD line are preserved exactly.
- OxBraceEffect: shard chip inside the `% 2` tick block, count 1, at the
  ground position of the orange red dust call. Presentation-only change,
  exactly as in the previous pass: the dust color 0xFF4500 and every other
  HEAD line are preserved exactly.
- PetrificationCurseEffect: shard chip inside the `% 2` tick block, count 1,
  same body-random position and drift as the stone gray dust call.

### Temporal glass: SHARD_CHIP (glass read)

- JudgementLockEffect: shard chip inside the `% 2` tick block, count 1,
  same body-random position as the cyan plasma call. The velocity freeze
  code above the block is untouched.
- TemporalStasisEffect: shard chip inside the `% 2` tick block, count 1, at
  the same orbiting clock position as the blue plasma call. The velocity
  freeze code is untouched.

### Soul and valor: WISP_CURL

- CelestialGraceEffect: wisp curl inside the `% 2` tick block, count 1,
  mirroring the cyan soul call; the white vapor call at the same point is
  unchanged.
- HeroicFocusEffect: wisp curl inside the `% 2` tick block, count 1,
  mirroring the gold plasma call.
- SilentMarkEffect: wisp curl in applyEffectTick behind the existing
  `tickCount % 2 == 0` shouldApply gate, count 1, at the same offset
  position as the silver soul call.
- SorrowRiftEffect: wisp curl inside the `% 2` tick block, count 1,
  mirroring the soul call with its downward drift; the 20 tick void damage
  block is untouched.
- SpiritImpaleEffect: wisp curl inside the `% 2` tick block, count 1,
  mirroring the golden soul call; the levitation velocity code is untouched.
- ValorEffect: wisp curl inside the `% 3` tick block, count 1, mirroring
  the gold plasma call.
- ValorStunEffect: wisp curl inside the `% 2` tick block, count 1,
  mirroring the gold plasma call; the max-stack immobilize code is
  untouched.

### Electric and guard: SPARK_STREAK

- BladeGuardEffect: spark streak inside the `% 2` tick block, count 1,
  mirroring the white plasma glint call.
- EnergyOverchargeEffect: spark streak inside the `% 2` tick block,
  count 1, mirroring the spring green plasma call.
- PoleStunEffect: spark streak inside the `% 2` tick block, count 1, at the
  same orbiting above-head position as the gold dizzy plasma call.

### Darkness and void: ASH_FLAKE

- DarkFadedEffect: ash flake, count 1, on the existing `% 10` tick gate.
  Special case, stated plainly: this effect has no server tick emission of
  its own. Its material is the single ambient ParticleOptions in the
  MobEffect constructor (vanilla ambient effect particle sync), which HEAD
  already sets to DARKNESS material and which cannot carry a second layer.
  The constructor was left unchanged, and the complex layer rides the only
  tick gate this file already has (the void damage gate), with the damage
  numbers and hurt call untouched. Consequence: this layer is sparse by
  construction, 1 flake per 10 ticks (2 per second). Raising that rate
  would have meant inventing a new tick point, which the assignment forbids.
- HollowedEffect: ash flake, count 1, behind a `% 2` tick gate.
  Special case, stated plainly: this effect also has no server tick
  emission of its own; its material is the ambient constructor particle
  (PLASMA material at HEAD), left unchanged, and its applyEffectTick has no
  existing gate or emission point to ride. The layer therefore uses the
  `% 2` cadence that the sibling auras in this family set (RiftedEffect,
  MovementCurseEffect) already use, at body-random positions. The player
  targeting suppression code is untouched. This is the one place in the
  slot where the cadence comes from the family pattern rather than from a
  gate already in the file, and it is flagged here for the merge rather
  than hidden.
- MarkedByEvilEffect: ash flake in applyEffectTick behind the existing
  `tickCount % 5 == 0` shouldApply gate, count 1, at the same orbiting mark
  position as the crimson plasma call.
- MovementCurseEffect: ash flake inside the `% 2` tick block, one call of
  count 1 per gated block at body center, with the same spread as the dark
  smoke call. One deliberate rate decision: the block's plasma emission
  runs inside a per-stack loop (up to 5 calls per gate), and the ash layer
  was not put inside that loop. Per-iteration ash at max stacks would run
  at 50 per second, over the standard's 40 per second continuous cap for
  complex sprites, so the layer is one per gated block (10 per second)
  instead. All stack, dampening, and root mechanics are untouched.
- RiftedEffect: ash flake inside the `% 2` tick block, count 1, mirroring
  the purple plasma call at the same position; the darkness call at the
  same point and the slow falling code are unchanged.
- StormOfDarknessEffect: ash flake inside the `% 3` tick block, count 3,
  mirroring the darkness call's center position, spread, and speed. The
  drag mechanic, the two smoke calls, and the conditional static call are
  unchanged.

### Fluid and corrosive: NONE (recorded exemption)

- BleedingEffect and MeltingEffect gained no layer and are byte-identical
  to HEAD (git diff for both files is empty). The exemption, restated
  exactly as the audit records it: none: no complex sprite silhouette
  matches fluid droplets or corrosive vapor, and forcing a fragment sprite
  here would read wrong. Recorded as a deliberate class S exemption;
  revisit only if a droplet sprite set is ever generated. In the
  assignment's words: this is a recorded deliberate class S exemption (no
  complex sprite silhouette matches fluid droplets or corrosive vapor;
  forcing a fragment sprite would read wrong).

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling all twenty-five edited
files, output to private /tmp dirs tagged status-effects. Full logs were
read before this claim was written.

- 26.2 classpath (cp262.txt): exit 1 with 4 errors, all outside this slot:
  DbaItems.java line 535 (Output has protected access in CreativeModeTab,
  twice) and DbaMenus.java line 16 (MenuType constructor and MenuSupplier
  private access, twice). Zero errors in any owned file (no log line
  references effect/). All 25 owned classes were produced in the output
  directory.
- 26.3 classpath (cp263.txt): exit 1 with 10 errors, all outside this slot:
  the same 4 as 26.2, plus DbaHostileEntity.java line 79 (swinging and
  attackAnim, twice), FriezaSoldierEntity.java line 49 (swing signature),
  NamekianWarriorEntity.java line 33 (hurtMarked), and
  PatrolRobotEntity.java line 181 (swinging and attackAnim, twice). Zero
  errors in any owned file. All 25 owned classes were produced in the
  output directory.
- These closure error kinds are the expected baseline kinds for this tree
  (other owners' in-flight work included); none is in, or caused by, a file
  in this slot.
- git diff --check on the slot is clean. Added-lines scan: 199 added lines,
  zero em dashes, zero client references. The destruction wiring from
  d74f13a was not touched (no file outside effect/ was edited by this slot).

## Foundation change requests

None. The shipped foundation was sufficient: DbaParticles already registers
all five complex types as common SimpleParticleTypes, and
ServerLevel.sendParticles accepts them with no new helper. Two merge notes
that are not requests: the DarkFadedEffect and HollowedEffect handling
above (ambient-only effects cannot take a second constructor particle, so
their layers live in applyEffectTick), and the MovementCurseEffect
one-per-block rate decision, both recorded in place.

## Worse check

Honest answer: no layer in this slot was shipped that I judge reads worse
than the particles-only version, and nothing is being shipped silently.
The reasoning and the real risks, per risk:

- Fixed sprite colors against themed filler. The complex sprites carry
  fixed physical style colors (stone gray chips, pale curls, white-gold
  streaks, orange ember fragments, gray ash) while theme color lives in the
  material filler, which is exactly the split the standard prescribes.
  The two tightest fits are AncientWeightEffect (goldenrod ground dust
  gains gray stone chips at the feet) and OxBraceEffect (orange red dust
  gains gray chips). Both are earth and stone family effects whose chips
  spawn at ground level, where kicked stone reads as ground debris rather
  than as a color error. I judge both as reads that hold.
- Temporal glass read. JudgementLockEffect and TemporalStasisEffect get
  stone gray SHARD_CHIP as the assigned glass read. The silhouette is a
  hard-edged faceted polygon, which reads as a shard next to the cyan and
  blue plasma; at count 1 per 2 ticks it stays sparse and cannot crowd out
  the freeze read. The audit assigned this mapping explicitly, and the
  silhouette supports it.
- DarkFadedEffect sparsity. Its ash layer is the quietest in the slot
  (2 per second) because it rides the only existing gate. That is a weaker
  layer, not a worse effect: the ambient darkness stream still carries the
  aura, and the flakes add silhouette without changing the read. Flagged
  in place above rather than fixed by inventing a tick point.
- HollowedEffect translucency. Ash flakes are physical sprites on an effect
  whose identity is phasing and translucency. At 1 per 2 ticks the flakes
  are sparse flecks inside an aura still dominated by the ambient void
  plasma, and the family assignment (darkness and void) is the audit's.
  The ghost read survives; if Carter judges otherwise in game, this is the
  first layer in the slot to revisit.
- PoleStunEffect shape swap risk. The dizzy read at HEAD is a gold plasma
  orbit; SPARK_STREAK adds elongated slivers on the same orbit. The gold
  plasma is still emitted at the same point, so the streaks read as motion
  trails on the orbit rather than replacing the stars.
- Density. Every complex call mirrors an existing call's count, so no
  effect more than doubles its sprite count at any tick point, and most
  add exactly one sprite per gate. No aura crosses into clutter at class S
  size.
