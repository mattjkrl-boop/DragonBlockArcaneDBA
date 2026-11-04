# Complex Effects Redo, Phase 2 Report: Shared Weapon Systems

Owner slot: SwingTrail and Bolt, the two shared weapon systems.
Files edited (only these two):
- src/client/java/com/dragonblockarcanedba/client/fx/SwingTrailRenderer.java
- src/client/java/com/dragonblockarcanedba/client/render/BoltRenderer.java

No commits. Themes, colors, profile data, damage, cooldowns, costs, timing, mechanics, and synced fields are identical to HEAD 9178eac. BoltEntity behavior is untouched. buildSegments (the Bolt emission path math) is byte-identical to HEAD: the same synced seed derives the same trunk and fork path, in the same random consumption order.

## SwingTrail (class L, serves every weapon)

Pieces layer (new):
- ComplexGeometry.tendril, submitted through ComplexGeometry.submitEnergy with ComplexTextures.TENDRIL, so every strand is on the dissolve and fresnel shader treatment the standard requires for class L energy pieces.
- Two chains per trail: one along the blade tip arc (blade fraction 1.0), one along the mid blade (fraction 0.55). Path points become bezier controls literally: the recorded samples are decimated to at most nine points per chain, and each tendril takes three consecutive path points as its quadratic bezier from, control, and to. The strand therefore follows the exact sampled swing path the emission uses; there is no second path anywhere in this system.
- Torn pinches: the builder's own seeded pinch windows tear each strand mid-span, each strand's swell profile narrows it at both ends, and chaining one tendril per pair of path intervals adds a narrowing at every joint, which lands the torn, pinched read at and near the arc ends. See change request 2 for the one part of the audit wording the toolkit cannot express exactly.
- Colors come from WeaponTrailProfiles as required: root color is the profile color, tip color is the existing core color derivation (profile color pushed 70 percent toward white, the same helper the plasma core already used). Peak alpha 0.95, envelope is the existing per-sample fade alpha (strongest surviving sample) times the shared distance scale, so geometry and emission fade together.
- FLASH profiles (blaster) get one tendril along the recorded muzzle to end shot line.
- Vertex cost: worst case 8 tendrils at 384 vertices each is 3072 per trail submit at distScale 1.0, inside the class L budget of 6000.
- Wiring note: SwingTrail has no entity renderer and no submit method; it is a client tick system. The pieces are drawn from Fabric LevelRenderEvents.COLLECT_SUBMITS, registered inside the existing register() method of the same file. The pose stack in that event is camera relative (verified against vanilla LevelRenderer bytecode: block entity submits do pushPose, translate by block position minus camera, submit, popPose). This code follows that exact pattern: push, translate by the trail anchor (oldest surviving tip sample) minus context.levelState().cameraRenderState.pos, submit in anchor local space, pop in a finally block.

Complex particle layer (new):
- SPARK_STREAK, emitted through MaterialEmitters.Preset.fixed(DbaParticles.SPARK_STREAK, Vec3.ZERO, 0.05f) with a continuous Emitter at 30 per second (inside the 40 per second continuous cap) and budget 48. Its source is the existing trail path source restricted to blade fraction 0.85 to 1.0, so streaks shed along the tip fifth of the blade and inherit the swing's own segment motion. Gated to blade profiles (kind not FLASH): the audit asks for streaks at the tip for steel themes, the only profile in the table is the katana, and a FLASH profile is a blaster shot that already has its SPARK burst. It ticks with the same envelope as the other trail emitters.

Material filler layer (unchanged):
- Plasma blade, near-white plasma core, and vapor emitters exactly as at HEAD, same presets, rates, budgets, seeds, and sources.

## Bolt (class L, flagship conversion)

Pieces layer (new):
- ComplexGeometry.branchedLightning, submitted through ComplexGeometry.submitEnergy in BoltRenderer.submit, at peak alpha 0.95 with the strike's existing strength value (envelope times per tick flicker times distance scale) as its envelope, so the geometry and the emission pulse together as one bolt. Core color is the existing coreColor derivation (theme pushed 75 percent toward white), sheath color is the synced theme color, width 0.30 scaled by length within the same clamp family the emission uses. Trunk, forks, and sub-branches are the builder's three branch orders at its full class L budget (its own segment count sits at or under the 130 segment and 4200 vertex branchedLightning budget at distScale 1.0).
- Anchoring (the important part, see change request 1): the builder cannot take the Bolt's existing seeded path as input, so the geometry bolt is pinned to the emission path at both ends. It starts at the strike point (the local origin in the entity submit pose) and ends at the exact top point buildSegments reaches for the synced seed (segment index 11's end point, made local), and it is seeded with the same synced seed. Start, finish, length, drift direction, envelope, and flicker all match the emission bolt.

Complex particle layer (extended):
- Existing SPARK emission at the strike point stays exactly as it was (same preset, rate 90, burst 8).
- SPARK_STREAK added at the strike point: Preset.fixed(DbaParticles.SPARK_STREAK, up velocity 0.10, spread 0.30), continuous Emitter at 36 per second (inside the 40 per second cap), plus a burst of 6 times strength on the first frame alongside the existing initial bursts.

Material filler layer (unchanged):
- The layered plasma emission (strong and weak sheath and core along the seeded segments, ground glow and ground core at the strike point) is kept in full as filler around the geometry core, with every preset, rate, share, and seed exactly as at HEAD.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac, -sourcepath src/main/java:src/client/java, compiling both owned files, full logs read.

- 26.2 classpath (cp262.txt): 4 errors, all baseline, in src/main/java/com/dragonblockarcanedba/item/DbaItems.java (Output protected access, 2) and src/main/java/com/dragonblockarcanedba/inventory/DbaMenus.java (MenuType private access, 2). Zero errors in either owned file. Both owned class files were produced in /tmp/sharedsys-262.
- 26.3 classpath (cp263.txt): 10 errors, all baseline, in DbaItems (2), DbaMenus (2), DbaHostileEntity (2), FriezaSoldierEntity (1), NamekianWarriorEntity (1), PatrolRobotEntity (2). Zero errors in either owned file. Both owned class files were produced in /tmp/sharedsys-263.
- Baseline proof for 26.3: the HEAD versions of both owned files were temporarily restored and compiled on cp263.txt; the run produced the identical 10 errors in the identical 6 files. No new error kinds were introduced on either classpath. (The DbaItems and DbaMenus kinds also appear in the stored sweep logs under ~/workspace/dba-audit-libs.)
- git diff --check on both owned files: clean. Em dash scan on both owned files: zero. Forbidden primitive scan (drawColoredBox, drawCylinder, drawDisc, drawSphere, drawHelix) on both owned files: zero call sites.
- Private output tags used throughout: /tmp/sharedsys-262, /tmp/sharedsys-263, /tmp/sharedsys-head263, logs alongside.

## Foundation change requests

1. branchedLightning cannot consume precomputed path data. Its only signature is branchedLightning(pose, buffer, seed, from, to, width, coreArgb, glowArgb, alpha, envelope, distScale): it generates its own trunk by midpoint displacement and its own forks from its internal splitmix Rng, and exposes no overload that accepts the caller's polylines. The Bolt's emission path is a java.util.Random walk (buildSegments) that must stay byte-identical, so there is no seed choice that makes the builder's internal path equal the emission path. Requested: a branchedLightning variant that takes a list of polylines (trunk, forks, sub-branches with per-line order) and applies only the builder's taper, width jitter, sheath and core crossed ribbon drawing, and life remap to them. Until that exists, the Bolt geometry is anchored as described above (same seed, same start, same exact end point, same envelope and flicker) and the plasma is demoted to wide soft filler, so the pair reads as a bright core inside a corona rather than two competing bolts. This is the centerpiece limitation of this slot and it is a toolkit gap, not an owner workaround.
2. Tendril pinch placement is not owner controllable. The audit wording for SwingTrail asks for torn pinches at the arc ends; the builder places its two pinch windows at seeded t in 0.2 to 0.5 and 0.55 to 0.85, never at the ends. The ends do narrow from the swell profile, and the chain joints add narrowing, but an owner cannot ask for a pinch at a named t. Requested: optional pinch center parameters on tendril (or a tendril variant taking pinch centers), defaulting to the current seeded behavior.
3. Minor: tick driven client fx systems (SwingTrail is the example) have no shared submit hook in the foundation; each one must discover and register Fabric LevelRenderEvents.COLLECT_SUBMITS itself and handle the camera relative pose convention by hand. A small shared helper in the foundation (register a callback that receives collector, pose stack, and camera position, with anchoring handled) would remove a whole class of per-owner wiring risk. This owner used the Fabric event directly inside its own file, so nothing here blocks on it.

## Worse check (honest)

Bolt: this is the one real risk in the slot, and it is the geometry versus emission path question. The geometry trunk is midpoint displaced with amplitude 0.16 of length; the emission trunk is the seeded random walk. They share endpoints, but mid-span they are different jagged lines, and at long strike lengths they can sit a block or more apart in places. Judgment: it still reads better than the particles-only version it replaces, for three reasons. The old version had no structured bolt at all, only glow sprites along a path. The geometry bolt is thin, bright, and high contrast, so the eye takes it as the bolt. The plasma sprites are large (sheath scale 1.55) and soft, so along a nearby path they read as corona and ground glow rather than a second defined line, and both layers share the same flicker and envelope so they brighten and die together. That said, if Carter's in-game look shows the corona visibly offset from the core on long strikes, the correct fix is change request 1 (draw the exact seeded path as geometry), not tuning here; inside the current toolkit the anchoring above is the closest honest approximation, and I am flagging it rather than claiming the two paths are one path.

SwingTrail: low risk of reading worse. The tendrils are built from the same recorded samples as the emission, anchored on the same path, so there is no divergence failure mode like the Bolt's. The residual risks are presentational: a tendril is a filament, not a blade-wide surface, so the trail's width comes from having two chains (tip and mid blade) plus the plasma filler between them; and the COLLECT_SUBMITS pose wiring, while verified against the vanilla bytecode pattern, has not been seen running in game from this VM (no Gradle or client run is possible here). If the pose convention were wrong the trails would be offset from the player, which would be immediately obvious in game and is a one line fix in drawTrailPieces. On the evidence available the composition reads strictly richer than the particles-only trail: same glow, plus a structured, torn, coiling strand on the exact swing path, plus streak sprites at the tip.

Nothing in this slot was shipped silently against a worse read: the Bolt approximation is named above with its fix, and the SwingTrail wiring risk is named with its check.
