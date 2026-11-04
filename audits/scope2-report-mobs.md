# Scope 2 Report: A3 Mobs

Conversion at HEAD ef5ad02, on top of the Scope 2 foundation (D2 voice
bank, D3 damage decision, D12 guide texture). Only the A3 ownership
list was touched: 5 of the 8 owned files changed (32 insertions, 27
deletions). entity/DbaHostileEntity.java, entity/OtherworldGuideEntity.java,
and entity/DbaHostileSpawns.java carried zero vanilla references before
and after and are unedited. No shared files edited, nothing committed.
Mob behavior, stats, spawn rules, and AI are identical; this pass is
presentation and damage typing only.

## Before and after counts (owned files, grep verified after the edits)

- Vanilla SoundEvents references: 17 to 0 (FriezaSoldier 5,
  NamekianWarrior 4, PatrolRobot 3, Saibaman 5).
- Vanilla ParticleTypes references: 2 to 0 (FriezaSoldier CRIT,
  Saibaman ITEM_SLIME).
- Vanilla damageSources() lines: 2 to 0 (one mobAttack per FriezaSoldier
  and Saibaman).
- Vanilla texture references: 1 to 0 (OtherworldGuideRenderer was the
  only vanilla texture under client/render in the tree).
- Unused SoundEvents and ParticleTypes imports removed where the swaps
  left them dead.

## Sounds, per species

All DbaSounds voice fields are plain SoundEvent, so the
getAmbientSound, getHurtSound, and getDeathSound overrides return them
directly. Every volume, pitch, and SoundSource at every call site is
unchanged.

- Frieza Soldier: PILLAGER_AMBIENT to SOLDIER_AMBIENT, PILLAGER_HURT to
  SOLDIER_HURT, and the death override (which reused PILLAGER_HURT) to
  SOLDIER_DEATH. Attack and action sounds: BLAZE_SHOOT (the ranged ki
  shot) to FIRE_SHOT per the swap guide; PLAYER_ATTACK_WEAK
  (playAttackSound) to SWING_LIGHT, the bank's light swing.
- Namekian Warrior: VINDICATOR_AMBIENT to WARRIOR_AMBIENT,
  VINDICATOR_HURT to WARRIOR_HURT, death (reused VINDICATOR_HURT) to
  WARRIOR_DEATH. PLAYER_ATTACK_WEAK to SWING_LIGHT.
- Patrol Robot: IRON_GOLEM_HURT to ROBOT_HURT, IRON_GOLEM_DEATH to
  ROBOT_DEATH, and the recall interaction's IRON_GOLEM_REPAIR to
  REPAIR. One honest non-wire: the bank's ROBOT_AMBIENT exists, but
  PatrolRobotEntity has no ambient call site. The vanilla iron golem
  has no ambient sound and this entity never overrode
  getAmbientSound; adding one would be new behavior, not conversion,
  so it is deliberately left unwired.
- Saibaman: SLIME_SQUISH_SMALL (ambient) to SAIBAMAN_AMBIENT,
  SLIME_HURT to SAIBAMAN_HURT, death (reused SLIME_HURT) to
  SAIBAMAN_DEATH. The self-destruct's GENERIC_EXPLODE to BOOM_BIG
  (the .value() call went away; BOOM_BIG is a plain SoundEvent).
  SLIME_ATTACK (playAttackSound) to HIT_FLESH, a judgment call: the
  vanilla sound is a wet impact for a claw strike that lands poison,
  not a whoosh, and HIT_FLESH is the bank's flesh-hit event. The two
  fist swingers above took SWING_LIGHT for the opposite reason.

Revert-on-worse disposition: no species was left on vanilla. Every one
of the twelve synthesized voice events had an honest call site fit, so
nothing was forced. Whether the synthesized sets read better than the
pillager, vindicator, golem, and slime voices in game is a listen only
Carter's PC test can make; that risk is the one the audit flagged for
D2 and it stands as recorded there.

## Particles

- FriezaSoldier: ParticleTypes.CRIT to DbaParticles.CRIT_SPARK at the
  ranged hit, same 8 count, spreads, and speed.
- Saibaman: ParticleTypes.ITEM_SLIME on the self-destruct to
  BlockParticleOption(DbaParticles.BLOCK_CHUNKS, slime block state),
  same 18 count, spreads, and speed. Choice stated plainly: ITEM_SLIME
  renders small green slimeball bits; BLOCK_CHUNKS with a slime block
  state renders green slime chunks bursting, which keeps the slime
  color and chunk character. The alternative, DUST_PUFF, is a soft
  neutral puff and would have lost the green entirely. This is the
  same BLOCK_CHUNKS stand-in the standard map and the foundation's
  SlamDebris fix use for block particles.

## Damage (D3)

- FriezaSoldier ranged shot: damageSources().mobAttack(this) to
  DbaDamage.ki(this, this), amount 5.0 unchanged. Direct and attacker
  are both the soldier, exactly the shape vanilla mobAttack(this)
  had; the D4 instant-hit shape is untouched, no projectile added.
- Saibaman self-destruct: damageSources().mobAttack(this) to
  DbaDamage.ki(this, this), amount 8.0 per target unchanged, same
  area, same canBurstHit filter, same discard.
- Mob contact melee is untouched in all four species: it runs through
  vanilla Mob attack plumbing with no damageSources() call in mod
  code, the same exemption as the player's plain melee hit.

## Guide texture (D12)

OtherworldGuideRenderer's TEXTURE now points at
dragonblockarcanedba:textures/entity/otherworld_guide.png, the
foundation-authored texture, in place of
minecraft:textures/entity/villager/villager.png. One line; the
VillagerModel and render state are unchanged, and the texture file
was verified on disk before the swap.

## Verification (javac audit only, no Gradle claimed)

Targeted javac of the five changed files with full sourcepath closure
on both audit classpaths, baseline compared against a pristine
ef5ad02 worktree compiled with identical flags:

- 26.2 (cp262.txt): zero error lines naming any owned file, before
  and after. All five changed files' classes emitted. The only errors
  in the run are the known baseline kinds in shared DbaItems and
  DbaMenus, untouched by this pass.
- 26.3 (cp263.txt): zero NEW error kinds naming owned files
  (comm -13 of normalized kinds, baseline vs current, is empty). The
  four kinds that do name owned files on 26.3 exist identically at
  baseline, all in lines this pass did not edit: DbaHostileEntity and
  PatrolRobotEntity "cannot find symbol" (the swinging and attackAnim
  fields LivingEntity lost in 26.3), FriezaSoldierEntity
  "no suitable method found for swing(InteractionHand)" (26.3 swing
  signature), and NamekianWarriorEntity "cannot find symbol"
  (hurtMarked). They are part of the standing 26.3 baseline, not
  regressions, and fixing them is dual-version port work outside
  this conversion's scope.
- OtherworldGuideRenderer compiled alone on 26.3 as a cross-check
  (the joint run's closure errors suppress its class output): zero
  errors, class emitted. Zero errors on 26.2 in the joint run.
- Post-edit greps over all 8 owned files: SoundEvents 0,
  ParticleTypes 0, damageSources() 0, vanilla texture references 0.
  Em dashes in changed lines: 0.

## Honest worse check

Nothing reverted and no call site left vanilla. The real trades,
stated plainly:

- All four species previously reused their hurt sound as their death
  sound. They now have distinct synthesized death events. That is the
  intended D2 conversion, but it is the most audible change in this
  area and the death cries are longer and more distinct than a
  repeated hurt grunt; Carter's listen decides whether any set reads
  worse than the vanilla voice it replaced.
- The Saibaman burst is chunkier: slime block model chunks are more
  cubic than the slimeball item sprites ITEM_SLIME drew. Same green,
  same count and spread; it reads as slime chunks rather than slime
  droplets.
- FIRE_SHOT at the soldier's 1.5 pitch and BOOM_BIG at the
  self-destruct are synthesized timbres in the same roles as
  BLAZE_SHOOT and GENERIC_EXPLODE; their quality cannot be judged in
  this VM.
- The guide NPC changes appearance by design (D12): the mod's robed
  guide texture on the same villager model replaces the vanilla
  villager look. Intended, and the one visual change in this area.
