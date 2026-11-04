# No-Shapes Report: Devil Trident

Owner slot: Devil Trident (replacement owner; the original owner died in a runtime restart mid-pass).
Base: HEAD ad22b32 (Phase 1 foundation). Nothing committed.

## Assignment

Per audits/no-shapes-audit.md, Devil Trident section: 3 renderers, 24 BespokeGeometry builder calls at HEAD, theme hellfire flame, magma embers, brimstone smoke, ground dust.

- DevilSlamShockwaveRenderer.java: 13 calls. Must become compressed ground dust, vapor, physical debris, and impact embers or sparks.
- TridentProngRenderer.java: 3 calls. Must become a dense plasma particle cluster with a material trail and impact plume; physical projectile models, where one exists, stay detailed models.
- TridentShardRenderer.java: 8 calls. Must become physical fragment material where a real fragment exists, otherwise dust, sparks, and plasma; no shard mesh.

## Takeover audit of the partial diff

The dead agent left all 3 renderers fully rewritten (417 insertions across 4 files) plus a 6-line change in effect/DevilsHandsEffect.java. I audited every file line by line against HEAD. Findings:

What was complete and coherent, kept as found:

- All geometry is gone from all 3 renderers. Final grep over the 3 renderers finds zero references to BespokeGeometry, BespokeGeometry.Rng, LayeredRender (including drawVortexBands and drawAnchorGlow), KiRenderHelper (including KiRenderHelper.vertex), VertexConsumer, addVertex, and submitCustomGeometry. No private geometry helper, particle system, or texture style was invented; everything emits through the shared MaterialEmitters foundation with DbaMaterialOptions materials.
- Lifecycle and envelope math is numerically identical to HEAD in all 3 renderers:
  - Devil Slam: lifeT smoothstep fade over the entity lifetime; grow over 10 ticks (cubic out); shockR = radius * 1.05 * grow (the seal edge is still the hit radius, telegraph honesty preserved); pillar smoothstep over 18 ticks, pillar height 6.7 rising over 5.5 ticks, pillar radius 0.72 * (1 + ease * 0.28); follow front from age 5 over 14 ticks at radius * (1.2 + 0.9 * ease) with the same squared falloff. Only the distance scaling call changed form: HEAD's LayeredRender.distScale segment stepping became the foundation's MaterialEmitters.distanceScale on the emission envelope, which is the standard Phase 2 pattern.
  - Trident Prong: intro fade over 2 ticks (smoothstep), post-impact dissolve over 6 ticks from the synced dieTick, needle offsets (-0.26, 0, 0.26) and radii (0.95, 1.25, 0.95) identical; the three-needle formation is preserved spatially as three emission clusters.
  - Trident Shard: pulse 0.85 + 0.15 * sin(age * 0.35), body pulse 0.93 + 0.07 * sin(age * 0.35), spawn scale-in 0.5 + 0.5 * min(1, age / 5), and the size step formula 0.80 + step * 0.09 (five steps from the UUID hash) all identical.
- Render state extraction is identical: same entity getters, same UUID-derived seeds (Devil Slam most-significant bits; Prong and Shard MSB xor LSB). The only state addition is the entity UUID field, which is client-side render state extracted from the entity itself. No entity class was edited, no synced data field changed, and no damage number, cooldown, cost, timing, or mechanic changed anywhere in this slot.
- Trident Shard and Trident Prong color constants are exact conversions of the HEAD float palette (standard rounding): Shard shell (0.34, 0.02, 0.04) = 0x57050A, Shard core (0.95, 0.22, 0.07) = 0xF23812, Prong shell (0.36, 0.02, 0.04) = 0x5C050A, Prong core (0.96, 0.24, 0.08) = 0xF53D14.
- Emission structure follows the audit rows: Devil Slam emits ground dust, flame, dark red plasma, hot plasma, and brimstone smoke across the seal disk, a hot core cluster, a flame, plasma, and ember column on the pillar envelope, and a dust front at the follow radius. Prong emits a plasma cluster and flame at each needle head, embers at the needle bases, and a plasma trail behind each needle. Shard emits a plasma cluster and flame at the head and a plasma and ember wake along the flight path.

Defects found in the partial work, fixed by me (all in DevilSlamShockwaveRenderer.java):

1. DARK_RED was 0x80010D but is commented as the conversion of (0.5, 0.01, 0.05). The green channel was wrong: 0.01 * 255 rounds to 3, not 1. Fixed to 0x80030D.
2. HOT was 0xFF380D but is commented as the conversion of (1.0, 0.22, 0.1). The blue channel was wrong: 0.1 * 255 rounds to 26 (0x1A), not 13 (0x0D, which is the blue of the darker red). Fixed to 0xFF381A.
3. The core emitter used HOT, but at HEAD the core pool glowFan had its own brighter color (1.0, 0.35, 0.14), distinct from the inner crack web color (1.0, 0.22, 0.1) that HOT correctly represents. Added CORE = 0xFF5924 (the exact conversion) and pointed the core emitter at it, so the slam's heart keeps its original hotter orange read.

No other defects found. Nothing was reverted.

## DevilsHandsEffect.java verification

The 6-line partial change swaps the two server-sent particle options from DbaDustOptions (THEME_DUST) to DbaMaterialOptions: dark red (0x8B0000, scale 1.5) becomes PLASMA material, near black (0x111111, scale 1.0) becomes DARKNESS material, each with a per-tick seed. I verified against HEAD that nothing else in the file changed: attribute modifiers (-0.60 movement speed, -0.50 attack speed), the 0.08 + 0.03 per amplifier freeze chance, the 0.1 + 0.05 per amplifier damage chance, the damage formula (max of 5.0 and 1 percent of max health per amplifier level), the hellfire damage source, the every-2-ticks particle gate, particle counts (3 and 1), positions, deltas, and speeds are all byte-identical. Presentation only. The change is also consistent with the no-shapes foundation: DbaMaterialOptions.getType() resolves PLASMA and DARKNESS to the registered material particle types, and server code sending material options is the pattern the standard allows.

Disclosure: the class javadoc at HEAD contains a pre-existing em dash (line 15). It is not part of this pass's diff and I left it untouched; the merge's added-lines scan will not trip on it.

## Geometry counts

| File | BespokeGeometry at HEAD (audit) | LayeredRender geometry at HEAD | Direct addVertex at HEAD | All three, final |
|---|---|---|---|---|
| DevilSlamShockwaveRenderer.java | 13 | 0 | 0 | 0 |
| TridentProngRenderer.java | 3 | 0 | 0 | 0 |
| TridentShardRenderer.java | 8 | 0 | 0 | 0 |
| DevilsHandsEffect.java | 0 | 0 | 0 | 0 |
| Total | 24 | 0 | 0 | 0 |

## Verification

Targeted javac with JDK 25 (~/workspace/tools/jdk-25.0.5+8/bin/javac), sourcepath src/main/java:src/client/java, over the 4 owned files, private output tags under /tmp/d49c78bd-*:

- cp262 (~/workspace/dba-audit-libs/cp262.txt): 4 errors total, all pre-existing baseline kinds in the compile closure (DbaItems x2, DbaMenus x2). Zero errors in owned files.
- cp263 (~/workspace/dba-audit-libs/cp263.txt): 10 errors total, exactly the pre-existing baseline set (DbaItems x2, DbaMenus x2, DbaHostileEntity x2, PatrolRobotEntity x2, FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero errors in owned files.

No Gradle build was run and no in-game look is claimed; per the standing rule the compile check in this environment is the javac audit, and the first real look is Carter's PC test.

## Foundation change requests (not implemented; shared files are read-only for owners)

1. Shared ground crack and scorch decal system. Devil Slam's crack webs are now disk-scattered plasma and dust, which carries the burn but not the crack lines on the ground itself. A shared decal layer (already requested by several other owners) would restore that read without any mesh.
2. Shared per-entity emitter registry. Like every other owner this slot hand-rolled per-UUID emitter maps with time-based pruning in 3 renderers. A foundation registry with live-frame source suppliers would replace all of them at once.
3. DbaMaterialOptions lifetime factor honored by DbaMaterialParticle. Material profile lifetimes are fixed, so short hosts (the shard, the prongs, the 35-tick slam) can leave a brief afterglow tail after the host dies. A per-options lifetime scale would let short effects die exactly with their hosts.

## Honest worse check

- Trident Shard is the softest conversion in this slot. At HEAD the shard had a readable crossed-crescent silhouette with a fast axial roll; it is now a hellfire plasma cluster with a wake, because the audit row forbids a shard mesh and the shard was a glowing mesh, not a detailed physical model. The flight read survives (cluster plus trail plus embers, same pulse and spawn scale-in), but the distinct spinning-blade identity does not. If any Devil Trident effect is judged worse on the PC test, it will be this one. Within the standard there is no compliant way to keep the silhouette, so I shipped the conversion and flag it here rather than reverting silently.
- Devil Slam's seal edge is now a density edge instead of a hard burning band. The radius math is unchanged and emission stays at or inside the hit radius, but at low effects-density settings the disk scatter thins and the telegraph reads softer than the old band. The crack web structure is likewise gone by design; the disk reads as scattered burning ground, dust, and smoke.
- Trident Prong keeps its formation read (three clusters at the exact HEAD offsets), so the trident identity survives spatially, but the needle silhouettes are gone and at long distance the shot reads as three fire bolts.
- Afterglow tails: already-spawned material particles finish their profile lifetimes, so brief effects may linger a few ticks past the host's end. Emission itself is envelope-gated and stops with the host. See change request 3.
- Nothing in this slot was reverted, and no conversion other than the shard silhouette loss above was judged worse than HEAD overall.
