# Bespoke Report: Spike Flail (Phase 2)

Agent scope: item/SpikeFlailItem.java, entity/SpikeFlailSpinEntity.java,
entity/SpikeFlailSwingEntity.java, client/render/SpikeFlailSpinRenderer.java,
client/render/SpikeFlailSwingRenderer.java. Shared blocks were only called,
never edited. Base: commit 2ef870b. Theme kept: steel chain and spiked ball,
warm white crack. Every damage number, cooldown, stamina value, lifetime,
and mechanic from the spectacle pass is unchanged, including the crack
landing on release tick 7. The entity and item diffs are sound swaps and
damage source swaps only.

## Before and after counts

| Category | Before | After |
| --- | --- | --- |
| KiRenderHelper primitive draws (drawSphere) | 2, both in SpikeFlailSwingRenderer | 0 |
| Local regular geometry helpers | 5 (drawCrack; drawChain, drawBall, prism, cone) | 0 |
| LayeredRender.drawVortexBands calls | 1 (orbit ring) | 0, replaced by direct BespokeGeometry.brokenVortexBand layers seeded from the entity (drawVortexBands was already toolkit-built; the task assigned brokenVortexBand to the orbit ring, and the entity seed now drives it instead of dimension-derived seeds) |
| Vanilla ParticleTypes / DustParticleOptions refs | 0 | 0 (already all DbaParticles and LayeredParticles after Phase 1; LayeredParticles.dust already returns DbaDustOptions, so no call site changed) |
| Vanilla SoundEvents refs | 5 (4 in SpikeFlailItem, 1 in SpikeFlailSpinEntity) | 0 |
| Vanilla damage source call sites | 4 (spin contact, spin crack, swing player branch, swing magic fallback) | 0, now 3 DbaDamage.crush call sites |

## Geometry: builders used

All seeds derive from the entity id (network synced identity), stored in the
render state at extract and salted per use, so shapes are frame-stable and
identical on every client.

- Orbit ring (SpinRenderer): BespokeGeometry.brokenVortexBand in three
  layers at the orbit radius: a brightened core at 0.27 x radius twisting
  with age, a theme mid band at 0.70 x radius counter rotating, and a wispy
  outer band at 1.02 x radius, alphas at the standard band values
  (LayeredRender.ALPHA_CORE / MID / OUTER) times the same FxEnvelope the
  particles use. The anchor glow call is unchanged (already bespoke).
- Crack marks (SpinRenderer): BespokeGeometry.crackStar in two layers, a hot
  full-radius star (1.0, 0.95, 0.8) over a dimmer inner star at 0.66 x radius,
  alpha scaled by charge exactly as the old disc was. The stars scale with
  the synced radius each tick, so the marks expand with the telegraph and
  reach full size on the tick the damage lands.
- Chain (both renderers): the prism links are gone. A shared submitSpan
  helper orients a BespokeGeometry.tornRibbon along any span (pose yaw,
  pitch, and roll), and submitChain lays eight spans along the same sag
  curve the prisms followed, alternating width (0.13 / 0.20) and roll
  (0 / 75 degrees) so links read as alternating chain links.
- Ball (both renderers, shared submitBall): two BespokeGeometry.displacedDome
  shells (one flipped 180 degrees) make the lumpy iron body, and
  BespokeGeometry.shardFan in three orthogonal planes makes the spikes.
  Radius, fade, and the swing's open easing are the same numbers the old
  drawSphere version took.
- Swing trail (SwingRenderer): four tornRibbon spans along arc samples of
  SpikeFlailSwingEntity.ballPos behind the ball, computed in extract from
  the entity's own path function, widths 0.20 to 0.44 growing toward the
  ball, alpha 0.10 to 0.31 times fade. New geometry: the spectacle pass had
  a particle trail only (see worse check).

## Particles

Nothing to swap: this weapon's files had zero vanilla particle references
before the pass (DbaParticles.SPARK, DbaParticles.WIND, and LayeredParticles
emissions, whose dust is DbaDustOptions after Phase 1). Counts, colors, and
call sites are untouched.

## Sounds: swaps (same call sites, volumes, and pitches)

| Site | Before | After |
| --- | --- | --- |
| SpikeFlailItem.performSwing | SoundEvents.CHAIN_HIT, 0.9 / 0.8 | DbaSounds.CHAIN_RATTLE, 0.9 / 0.8 |
| SpikeFlailItem.performSwing | SoundEvents.PLAYER_ATTACK_SWEEP, 1.0 / 0.7 | DbaSounds.SWING_HEAVY, 1.0 / 0.7 (guide: sweep by weapon weight, the flail is heavy) |
| SpikeFlailItem.beginSpin | SoundEvents.CHAIN_PLACE, 0.8 / 0.6 | DbaSounds.CHAIN_RATTLE, 0.8 / 0.6 |
| SpikeFlailItem.releaseUsing | SoundEvents.CHAIN_HIT, 1.2 / 1.15 | DbaSounds.CHAIN_RATTLE, 1.2 / 1.15 |
| SpikeFlailSpinEntity.impactFx | SoundEvents.ANVIL_LAND, 0.7 / 0.7 | DbaSounds.SLAM_THUD, 0.7 / 0.7 |
| SpikeFlailSpinEntity.crack | (no sound) | DbaSounds.GROUND_CRACK, 0.9 / 1.0, added on the crack tick, layered under the slam thud in impactFx; this is the third guide event named for this weapon |

## Damage: swaps

| Site | Before | After |
| --- | --- | --- |
| Spin orbit contact, 200 + Strength x 0.5 | damageSources().playerAttack(player) | DbaDamage.crush(this, player) |
| Spin crack, (480 + 600 x charge) + Strength x (1.0 + 1.0 x charge) | damageSources().playerAttack(player) | DbaDamage.crush(this, player) |
| Swing, 720 + Strength x 2.0 | damageSources().playerAttack(player), or magic() when the owner was not a player | DbaDamage.crush(this, living owner) in both cases, per the DbaDamage mapping for ability entities (theme family, ability entity as direct, owner as attacker) |

The plain melee hit is untouched: it is still the vanilla item attack
(attribute damage 820, speed -2.7), and hurtEnemy remains the documented
no-op. All damage still passes through DbaEffectsConfig.capDamage exactly
as before. No number changed anywhere.

## Verification

Targeted javac audit (the project's standing method; Gradle cannot run in
this sandbox): the five files compiled with the full sourcepath against
both audit classpaths. Final result: zero error lines in any Spike Flail
file on the 26.2 classpath and zero on the 26.3 classpath. Two of my own
errors were caught and fixed during verification:

1. 26.2 run: a lambda in SpikeFlailSpinRenderer.submit captured the
   reassigned fade variable. Fixed with a final crackFade copy.
2. 26.3 run: PoseStack.mulPose(Quaternionf) does not exist on 26.3. All
   rotations switched to the codebase's dual-version idiom,
   mulPose(new Matrix4f().rotation(Axis...)), the same pattern
   KiBarrierRenderer uses, and re-verified on both classpaths.

Remaining errors in those compile runs belong to pre-existing baseline
files (DbaItems, DbaMenus, mob entities) and sibling agents' in-flight
files; the coordinator's full-tree baseline diff at merge is the
authoritative count, as in the spectacle pass. Post-edit greps confirm
zero KiRenderHelper primitive draws, zero ParticleTypes or
DustParticleOptions references, zero SoundEvents references, and zero
damageSources() calls in all five files. No em dashes in code, comments,
or this report. No commit made.

## Shared change requests

One, not blocking: a toolkit chain or strut builder (a seeded segmented
strand with per-link roll) would serve this chain better than composed
tornRibbon spans, and strands like it exist elsewhere (chains are a
recurring shape). The composed spans live in SpikeFlailSwingRenderer as
submitSpan and submitChain, shared by both flail renderers, but they are
composition code, not a geometry builder, so a future toolkit strand
builder could replace them in one place. Everything else I needed
(brokenVortexBand, crackStar, tornRibbon, displacedDome, shardFan,
CHAIN_RATTLE, SWING_HEAVY, SLAM_THUD, GROUND_CRACK, DbaDamage.crush)
already existed with the right signature.

## Worse check (honest)

- The ball reads different: the old body was a smooth shaded sphere with
  six regular cone spikes; the new body is two noise-displaced domes with
  irregular shard spikes. It is less machined and more jagged-forged,
  which fits the weapon, but the seam where the two domes meet has the
  dome rim alpha dip (down to 0.35 x), which can read as an equator weld
  line. Noted, not hidden; I judge the overall silhouette better and
  unmistakably no longer a lat-long sphere.
- The chain is the one real trade: flat torn ribbons are less volumetric
  than the solid six-sided prism links they replace. The alternating
  75 degree roll keeps most links readable from most angles and the
  jittered edges catch the emissive render type, but a link seen exactly
  edge-on nearly vanishes. If it reads weak in game, the remedy is the
  toolkit strand builder requested above, not a return to prisms.
- The crack visual changed character: a raised glowing mound disc became
  flat crack stars on the ground plus the anchor glow. For a ground crack
  that also leaves a gravel scar, I judge this neutral to better, and the
  expansion telegraph still reads because the stars grow with the synced
  radius. The old mound's height cue is gone.
- The swing trail ribbon is new and was kept deliberately faint so it
  reads as a motion streak under the existing particle trail. If it reads
  noisy in game, it is a self-contained deletion in SpikeFlailSwingRenderer.
- Synthesized sounds cannot be auditioned in this sandbox. CHAIN_RATTLE,
  SWING_HEAVY, SLAM_THUD, and GROUND_CRACK were swapped at identical
  volumes and pitches; their timbre is Carter's call on his PC build.
- Nothing was reverted. No mechanic, number, or timing changed: the crack
  still lands on release tick 7, charge still scales damage, radius,
  knockback, debris, and scar exactly as the spectacle pass left them.
