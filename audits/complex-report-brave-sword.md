# Complex Effects Redo, Phase 2 Report: Brave Sword

Owner slot: Brave Sword (theme: valor plasma, golden motes, impact dust).
Base: HEAD 9178eac. Files edited, and only these five:
BraveChargeRenderer.java, BraveCrossSlashRenderer.java,
BraveRushTrailRenderer.java, BraveShockwaveRenderer.java,
BraveSlashRenderer.java, all under
src/client/java/com/dragonblockarcanedba/client/render/.
No commits. Themes, colors, damage numbers, cooldowns, costs, timing,
mechanics, and synced data fields are identical to HEAD. Every piece is
seeded from the entity UUID seed, the same synced seed the emission
already used. The d74f13a destruction wiring is untouched.

Shared discipline applied in all five files:

- Energy pieces go through ComplexGeometry.submitEnergy with the
  ComplexTextures.TENDRIL canvas, except the BraveShockwave crackFan,
  which uses the white canvas submitEnergy form. Solid pieces go through
  ComplexGeometry.submitSolid.
- Peak alphas are honest body alphas: 0.92 on BraveCharge and
  BraveRushTrail, 0.95 on the other three. No shader piece is fed a low
  constant alpha to fake faintness; the lifecycle envelope from HEAD
  (charge ratio and fade factor, draw-out and envelope, opening
  envelope, sine ease envelope) is passed as the envelope input.
- PLASMA material emission is kept in full as the filler layer in every
  file and is no longer the body of any effect.
- Zero calls to drawColoredBox, drawCylinder, drawDisc, drawSphere, or
  drawHelix in the slot. No private toolkit was built.
- Vertex accounting uses the shipped builder math at distScale 1.0: one
  tendril is 24 spine segments times 2 crossed quads times 8 vertices,
  which is 384 vertices.
- Complex sprite continuous rates are 26 to 34 per second per emitter,
  under the standard cap of 40. Bursts stay inside the class budgets
  (M 30, L 60); the BraveCrossSlash streak burst is clamped to 30.

## BraveCharge (class M)

- Pieces layer: ComplexGeometry.tendrilFan, 5 strands from the caster
  core at local (0, 0.9, 0), direction up, spread 118 degrees, width
  0.17, gold 0xFFD700 root to pale gold 0xFFF2C0 tip, through
  submitEnergy with the TENDRIL canvas. Fan length is 1.15 plus
  chargeRatio times 1.55, the same gather radius the mote stream uses,
  so the strands grow as the charge builds. Vertex cost 5 times 384,
  which is 1920 of the 2500 class M budget.
- Complex particle layer: WISP_CURL through MaterialEmitters.Preset.fixed
  on a new continuous emitter sampling the existing inward gathering
  source, 26 per second.
- Filler kept: plasma gather, soul gather, core vapor, ground dust,
  unchanged.

## BraveCrossSlash (class M)

- Pieces layer: two ComplexGeometry.tendril calls in one submitEnergy
  submission with the TENDRIL canvas, forming the cross body. Each runs
  along one diagonal of the existing blade basis (right plus up, right
  minus up), spanning the same growing span the emission samples (span
  times 0.66 each way), with the bezier control bowed 0.35 forward, the
  same shallow forward offset the blade samples use. Width 0.30 times
  scale. One strand runs gold 0xFFD700 to white 0xFFFFFF, the other gold
  to soul cyan 0x00F2FF, matching the plasma and soul filler split.
  Vertex cost 2 times 384, which is 768 of 2500.
- Complex particle layer: SPARK_STREAK through Preset.fixed on a
  continuous emitter sampling the existing blade path source, 30 per
  second, plus a strike burst of at most 30 streaks at the center inside
  the existing 0.28 to 0.62 strike window, alongside the existing plasma
  and soul bursts.
- Filler kept: blade plasma, white core plasma, volume soul and vapor
  with their merge lifetime factors, unchanged.

## BraveRushTrail (class M)

- Pieces layer: three ComplexGeometry.tendril calls in one submitEnergy
  submission with the TENDRIL canvas, laid along the real rush path in
  the local frame. Tip position, opening length, and first person owner
  scaling reuse the exact math of the existing path emission source. A
  white 0xFFFFFF to gold 0xFFD700 core strand at width 0.24 times scale
  runs tip to tail; two side strands at width 0.16 times scale start at
  the cone width and converge to 0.35 of it at the tail, one gold to
  cyan 0x00F2FF, one gold to pale gold 0xFFF2C0. Vertex cost 3 times
  384, which is 1152 of 2500.
- Complex particle layer: WISP_CURL through Preset.fixed on a continuous
  emitter sampling the existing path source, 28 per second.
- Filler kept: path plasma, white core plasma, cyan vapor, ground dust,
  and the tip strike burst, unchanged.

## BraveShockwave (class L)

- Pieces layer, two builder types, so the class L rule is met by
  composition: ComplexGeometry.fracturePlates through submitSolid
  (gold 0xFFD700 hot center to dark gold 0x7A5E14 rim, lift 0.34) and
  ComplexGeometry.crackFan through submitEnergy (white 0xFFFFFF hot
  center to gold 0xFFD700 tips). Both radii follow the entity sine ease
  expansion, the same radius the emission front samples: plates take the
  current radius, cracks take 0.98 of it. Geometry is skipped below a
  0.35 radius so the first ticks stay clean. Vertex cost at full size is
  about 480 for the plates (10 plates at 48 vertices, minus seeded gaps)
  plus about 530 for the crack fan (9 to 11 cracks with forks), roughly
  1010 of the 6000 class L budget. Per the audit, the entity already
  spawns a server side debris field; these pieces are the fast
  near-field fragments and energy structure, as assigned.
- Complex particle layer: SHARD_CHIP through Preset.fixed in two places:
  an 18 chip burst at the epicenter inside the existing opening burst,
  and a continuous emitter sampling the existing dust front source at
  34 per second.
- Filler kept: front dust, front plasma, soul backwash, core plasma,
  and the opening plasma and dust bursts, unchanged.

## BraveSlash (class M)

- Pieces layer: two ComplexGeometry.tendril calls in one submitEnergy
  submission with the TENDRIL canvas. The main strand is the cut body:
  across the full swing span from the existing span function, endpoints
  bowed 0.10 forward, bezier control 0.62 forward so the strand crests
  on the same belly line the cut emission samples (the quadratic bezier
  midpoint sits at half the control offset), width 0.40, gold 0xFFD700
  to pale gold 0xFFF2C0. A thinner echo strand nests inside at 0.80 of
  the span, width 0.17, white 0xFFFFFF to gold. Vertex cost 2 times
  384, which is 768 of 2500.
- Complex particle layer: SPARK_STREAK through Preset.fixed on a
  continuous emitter sampling the existing cut source, 30 per second,
  plus a 10 streak burst at the belly inside the existing strike window,
  alongside the existing plasma flash bursts.
- Filler kept: cut plasma, white core plasma, cyan vapor wake, and the
  strike flash bursts, unchanged.

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling the five owned
files, private output tags under /tmp (brave-sword-262, brave-sword-263,
and the b rerun after the burst clamp).

- Classpath cp262.txt: 4 errors, all pre-existing baseline kinds in the
  wider closure: DbaItems.java line 535, Output has protected access in
  CreativeModeTab, twice; DbaMenus.java line 16, MenuType constructor
  and MenuSupplier private access, twice. Zero errors in any owned file,
  and all five Brave renderer class files were produced.
- Classpath cp263.txt: 10 errors, all pre-existing baseline kinds in the
  wider closure: the same DbaItems and DbaMenus errors, plus cannot find
  symbol and swing signature errors in DbaHostileEntity.java,
  FriezaSoldierEntity.java, NamekianWarriorEntity.java, and
  PatrolRobotEntity.java. Zero errors in any owned file, and all five
  Brave renderer class files were produced.
- The rerun after the BraveCrossSlash burst clamp reproduced the exact
  same counts (4 and 10) with zero Brave lines in either log.
- git diff --check is clean. Added lines in the five files contain zero
  em dash characters. Banned primitive call scan over the five files
  returns no matches. No other slot files were edited by this owner;
  other modified files in the tree belong to the other Phase 2 owners.

## Foundation change requests

No foundation gap blocked this slot. Two non-blocking extension requests
for the merge to consider as shared toolkit work:

1. Inward tendril fan. tendrilFan only fans outward from one origin.
   Every charge effect in the audit is a gathering effect, and the
   inward read currently has to come from the particle stream while the
   fan lashes outward from the core. A tendrilFan variant whose strands
   start on a seeded shell and converge on the origin, or a per-strand
   origin ring parameter, would serve BraveCharge and every other
   charge slot directly.
2. Tendril phase input. flameTongues takes a caller swayPhase so the
   pieces move; tendril takes no phase, so trail and wake strands are
   static per seed along their path and all motion comes from the shed
   particles. A phase parameter that advances the curl offsets would let
   rush trails and wakes flow. BraveRushTrail is the concrete case in
   this slot.

## Honest worse check

No effect in this slot is shipped in a state I judge worse than the
particles-only version, and none is worse than the pre-redo version.
Per effect, with the real caveats stated:

- BraveCharge: the fan lashes outward from the core rather than visibly
  streaming inward; the inward motion is carried by the mote and wisp
  streams, which were already the dominant motion in the particles-only
  version. The gain is a structured, charge-scaled strand body where
  there was only glow. Caveat recorded, not a regression.
- BraveCrossSlash: the tendril pinch windows can make a strand read
  momentarily broken mid-stroke at unlucky seeds. That is the standard
  torn-strand read, the width swells mid-spine, and the plasma filler
  still fills both diagonals, so the cross silhouette is strictly more
  explicit than the particles-only version.
- BraveRushTrail: the strands do not flow backward along the path (see
  change request 2); flow still reads from the shed plasma, vapor, and
  wisps exactly as before, and the path now has a persistent structured
  body between sheds. Not worse.
- BraveShockwave: the seeded plate pattern scales radially with the
  front instead of new plates cracking open progressively, and plates
  drawn in the local ground frame can clip or float on strongly sloped
  terrain, the same caveat any ground fan carries. The effect lives 44
  ticks, the plates sit over the real craters and debris the destruction
  pass leaves, and the read is far more structured than the
  particles-only front. Not worse.
- BraveSlash: the fixed forward bow is a single authored curve per seed
  family rather than a per-swing sampled arc. It matches the emission
  span and belly exactly, so the body and the filler agree. Not worse.
