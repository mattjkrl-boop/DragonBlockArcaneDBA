# Bespoke report: Lava Sword

Phase 2 weapon conversion, from audits/BESPOKE-STANDARD.md, the Lava rows of
audits/bespoke-audit.md, and audits/spectacle-report-lava-sword.md. Files owned
and edited: item/LavaSwordItem.java, entity/LavaCleaveEntity.java,
entity/LavaEruptionEntity.java, client/render/LavaCleaveRenderer.java,
client/render/LavaEruptionRenderer.java. entity/LavaSwordGround.java was read
and deliberately NOT edited (see the ground burn note below). Nothing outside
the Lava set was edited. Theme, colors, identity, damage numbers, cooldowns,
Ki costs, and mechanics are unchanged; only what the effects are made of
changed.

## Counts: before and after

| Rule | Before | After |
| --- | --- | --- |
| Vanilla ParticleTypes references in my files | 4 (LavaSwordItem 1, LavaCleaveEntity 2, LavaEruptionEntity 1, all FLAME) | 0 |
| Vanilla SoundEvents references in my files | 5 (LavaSwordItem: BLAZE_SHOOT x2, PLAYER_ATTACK_SWEEP, LAVA_POP, FIRECHARGE_USE) | 0 |
| KiRenderHelper primitive draws (drawColoredBox, drawCylinder, drawDisc, drawSphere, drawHelix) in my renderers | 0 already (the spectacle renderers emitted raw vertices, no primitive calls) | 0 |
| Hand-rolled regular geometry helpers in my renderers | 3 (LavaCleaveRenderer.drawCrescent, LavaCleaveRenderer.drawDrips with its poseStackTranslate bead fan, LavaEruptionRenderer.drawWave uniform grid sheet) | 0 |
| Vanilla damage sources for ability damage (damageSources() calls) | 2 sites (cleave and eruption: playerAttack, magic fallback) | 0 |
| BespokeGeometry builders used | 0 | bladeArc x3, plume x3, crackStar x1 |

## Geometry (rule 1)

- Both entities gained a synced SEED (EntityDataSerializers.INT, set from the
  level random in the spawning constructor, the same pattern BoltEntity uses),
  extracted into the render states. Every shape below is the shared seeded
  builder's output, so every client draws the same silhouette.
- Cleave: the three hand-rolled drawCrescent passes (equal-segment arc with
  per-vertex trig, plus a second flat face strip) are now three
  BespokeGeometry.bladeArc calls: the main mid arc (radius 1.35 scaled by the
  same open ease, sweep 168 degrees, theme red-orange 0.72/0.18/0.04), a
  nested bright core arc (radius 0.78, sweep 150, 1.0/0.78/0.22), and the
  trailing arc behind at z -0.55 (radius shrinking with the same trail ease,
  alpha ALPHA_MID times the trail factor). bladeArc lies in its own XZ plane
  centered on +X; the renderer rotates Z then X to stand it in the crescent's
  XY plane centered on +Y, the orientation the old crescent used. The seeded
  nicks and noise profile replace the old uniform bow.
- Cleave drips: the 4 hand-placed triangle beads (a 5-segment fan helper)
  are now one BespokeGeometry.plume (radius 0.38, height 0.85, bright orange)
  flipped 180 degrees about X so it hangs downward off the crescent's base.
- Eruption: the uniform grid sheet (drawWave, two passes over a 14 by 5
  equal grid) is now two BespokeGeometry.plume calls pushed forward along
  the facing by 0.30 times the reach, so the crown sits inside the cone the
  damage and the ground burn use: a dark mid plume (radius 0.60 times reach,
  0.45/0.07/0.02) and a brighter taller core plume (radius 0.33 times reach,
  height times 1.3, 1.0/0.55/0.10). Heights follow the same growth ease the
  old bow used (0.7 rising to 1.8).
- Eruption scorch mark: new BespokeGeometry.crackStar at the base (seeded
  wandering cracks, radius min(0.85 times reach, 5.5), lava orange, alpha
  ALPHA_MID times the burn-down ease), inside the ground the wave and the
  real burn actually cover. The shared LayeredRender.drawAnchorGlow at the
  origin is unchanged (already bespoke, compliant per the standard).
- Envelope and distance behavior preserved: builders receive the same
  FxEnvelope envelope and LayeredRender.distScale the old code computed,
  and alphas keep the old products (envelope times the burn-down or trail
  eases), just factored across the builders' alpha and envelope parameters.

## Particles (rule 2)

- All 4 FLAME references now use the mod's own DbaParticles.FLAME (same
  registration name, the standard's like-for-like swap): the melee burn spit
  in LavaSwordItem.hurtEnemy (6 particles, same counts and spreads), and the
  LayeredParticles outer type in LavaCleaveEntity (emitVortex every tick,
  emitRain over the last 2 ticks) and LavaEruptionEntity (emitVortex every
  tick). No other particle types were referenced in my files; the KI_MOTE
  core and theme dust mid layers already came from LayeredParticles.

## Sounds (rule 3)

All 5 plays in LavaSwordItem swapped by use case from the DbaSounds guide,
same call sites, volumes, and pitches:

| Was | Now | Site |
| --- | --- | --- |
| BLAZE_SHOOT (0.85, 0.7) | DbaSounds.FIRE_SHOT | cleave cast |
| PLAYER_ATTACK_SWEEP (1.0, 0.6) | DbaSounds.SLASH_WHOOSH | cleave cast |
| LAVA_POP (1.1, 0.6) | DbaSounds.LAVA_BUBBLE | eruption cast |
| BLAZE_SHOOT (1.0, 0.5) | DbaSounds.FIRE_SHOT | eruption cast |
| FIRECHARGE_USE (0.9, 0.8) | DbaSounds.FIRE_IGNITE | eruption cast |

## Damage (rule 4)

- Cleave damage now goes through DbaDamage.lava(this, owner): the cleave
  entity is the direct entity, the owner is the attacker. The old branch
  (playerAttack for a player owner, magic otherwise) is one call now; the
  lava damage type JSON mirrors the player attack and magic shapes per the
  standard, so scaling and exhaustion are what they were. Damage value,
  capDamage, the 60 tick burn, and the shove are byte-identical.
- Eruption damage likewise: DbaDamage.lava(this, owner), value, cap, the 80
  tick burn, and the push unchanged.
- The plain melee hit is untouched and keeps the vanilla player attack
  source, per the standard's honest exception: hurtEnemy still only adds
  the 60 tick burn and the flame spit on top of the game's weapon hit.

## Ground burn note (read this before judging the merge)

LavaSwordGround places real vanilla lava and fire BLOCKS (Blocks.LAVA,
BaseFireBlock states). That is world material the weapon leaves in the
world, not an effect particle, texture, or sound, so it stays vanilla by
design and this file was not edited at all in this pass. The spectacle
pass's crash-safe behavior is therefore trivially unregressed: the
SavedData mirror, deterministic budget-first placement (no random roll),
scorchLanding putting the pile on top of the hit block, the climb out of a
solid hint cell, the no-flow PLACE_FLAGS, the neighbor hold reverts, the
water fallback, the player-cell skip, the protected ground list, and the
shutdown restore are all byte-identical to commit 2ef870b (git diff shows
no change to the file). The one 26.3 javac kind against it (blocksMotion,
below) is pre-existing and also untouched.

## Shared change requests

None. The toolkit had every shape this weapon needed (bladeArc, plume,
crackStar), the particle and sound swaps were all in the bank, and
DbaDamage.lava existed. The spectacle report's older optional request
(registering LavaSwordGround from the mod initializer) is unchanged by
this pass and still stands as written there.

## Verification

- Targeted javac of my six files (the five edited plus LavaSwordGround)
  with sourcepath closure on both audit classpaths, JDK 25, -Xmaxerrs
  10000. On 26.2: zero errors in any of my files; the 7 errors in the run
  are all in other files (DbaItems, DbaMenus, and another agent's in-flight
  ErasureCannonBeamEntity). On 26.3: exactly one error in my files,
  LavaSwordGround.java line 212, the blocksMotion() call, which is the
  same pre-existing baseline kind recorded in the spectacle report (the
  26.3 audit jar has no blocksMotion on BlockState); the file is unchanged
  in this pass, so the kind is inherited, not introduced. The other 13
  errors are in other files. No new error kinds in my files on either
  version versus the baseline.
- Grep checks on my six files after the edits: ParticleTypes 0,
  SoundEvents 0, damageSources() 0, KiRenderHelper primitive draws 0,
  local draw helpers 0, em dashes 0.
- The Gradle build was not run (sandbox cannot, compile check waived by
  Carter, as in the spectacle pass). No build pass is claimed. A comment
  only edit to LavaEruptionEntity's CONE_MIN_DOT javadoc was made after
  the compile runs to keep it honest about the new plume visuals; javac
  ignores comments, so the verification stands.

## Worse check (honest)

- The cleave crescent is flatter than before. bladeArc is a single ribbon
  sheet where the old drawCrescent drew two crossed faces (a lip with real
  depth in z). From the front and back it reads the same or better (nicked
  edge, swelling profile, brighter core); from a pure side angle it is
  thinner. That is the toolkit's house style for arcs (the same trade
  LayeredRender's bands make), the three layers plus the trail keep the
  volume cue, and no private geometry was kept to dodge it, per rule 1.
  Noted, kept.
- The eruption silhouette changed the most: a forward bowing sheet became
  a forward-pushed crown of flame puffs. It is less of a "wave" and more
  of a gout, which arguably fits an eruption better, and the crack star
  plus anchor glow ground it where the damage lands. The damage cone, the
  scorch, and the telegraph timing did not move, so no target is hit by
  something the visual does not cover: the bulk of the plume crown sits
  well inside the cone (radius parameter 0.60 times reach, centered 0.30
  times reach forward), though the bulge and rim jitter can push the
  wispiest outer points to about the old sheet's radius and a touch past
  it at the extreme, at low alpha. If Carter dislikes the gout read in game, the
  fix is a toolkit-level sheet builder, which is a coordinating pass call,
  not a private renderer.
- The plume drips hang as a small stack of discs rather than stretching
  teardrops; at 0.38 radius under a moving crescent this reads as molten
  blobs and costs nothing. Kept.
- Nothing was reverted: no swap made a number, a timing, or a mechanic
  worse, and the two visual trades above are the standard's rules applied
  as written.
