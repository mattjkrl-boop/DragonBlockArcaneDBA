# Bespoke Pass Report: Hollow's Edge

Base: commit 2ef870b (Bespoke Pass Phase 1). Standard: audits/BESPOKE-STANDARD.md.
Scope: item/HollowsEdgeItem.java, entity/VoidRiftEntity.java, entity/VoidSlashEntity.java,
entity/HollowAfterimageEntity.java, and the three renderers (VoidRiftRenderer,
VoidSlashRenderer, HollowAfterimageRenderer). No other files were edited.
No commit was made; the coordinating pass merges.

## Before and after counts (owned files only, grep verified)

| Borrowed material | Before | After |
| --- | --- | --- |
| KiRenderHelper primitive draws (drawColoredBox etc.) | 9, all drawColoredBox in HollowAfterimageRenderer | 0 |
| Local hand-rolled geometry helpers | 19 (see below) | 0 |
| Vanilla ParticleTypes / DustParticleOptions references | 0 | 0 |
| Vanilla SoundEvents call sites | 8 | 0 |
| Vanilla damage sources on ability damage (hurtServer call sites) | 4 | 0 |
| Em dash characters | 4 | 0 |

Local helpers deleted: VoidSlashRenderer.drawVoidRing; the whole VoidRiftRenderer
tear system (drawTear, fillCut, Cut, put, rag, bite, drawMouth, drawShard,
drawCrystal, drawHalo, drawRingBand, across, along, vert); HollowAfterimageRenderer
renderGhostBody, drawPhantomRibbon, drawRippleRing, drawMoteDiamond.
KiRenderHelper is still used only for kiRenderType(), kiAdditiveType(), and the
channel extractors, which the standard allows.

## Geometry (all through BespokeGeometry, seeds from entity UUIDs)

27 builder call sites total. Every seed comes from the owning entity's UUID
most significant bits, extracted into the render state, so every client draws
the same shape. No synced data was added or changed anywhere.

- VoidSlashRenderer: 5 bladeArc layers (dark aura, violet body, cyan core,
  white edge, partial closing edge) replace the 6 hand-rolled ring layers;
  2 tornRibbon wake smears replace the extruded trail quads. The progress,
  open, close and fade math and the radius derivation are unchanged.
- VoidRiftRenderer: 2 displacedDome shells (violet shell scaled into the
  mouth's tall ellipse, near-black throat dome inside it reaching deeper)
  replace the polygon-cut mouth; 3 brokenVortexBand accretion bands at the
  old radii, widths and band alphas; glowFan for the back halo; tornRibbon
  for the seam and the two lip flares; shardFan for the 7 lip crystals,
  planted with the old layout math (slim profile, lip hugging, implosion
  suck and shrink). Fade-in envelope, implosion alpha windows (body 0.30
  to 0.78, rim 0.50 to 0.86, seam 0.74 to 1.0, shards 0 to 0.70) and the
  distance segment scaling are unchanged.
- HollowAfterimageRenderer: plume columns replace the box body (halo plume,
  skin-tinted body plume, hair plume cap in the synced hair color, additive
  shell plume); tornRibbon for the eye slit, the 6 scanline sheets and the
  2 wake ribbons; glowFan for the footstep ripple; tornRibbon flecks for
  the 16 motes, on the same seeded Random positions and rise curve as the
  old diamond motes, count stepped down with distance via FxEnvelope.
  Public contract untouched: HollowAfterimageEntity changed by one javadoc
  punctuation character only (em dash to colon); the (Level, Player)
  constructor and all five synced fields (SYNC_YAW, SYNC_PITCH, OWNER_UUID,
  SKIN_COLOR, HAIR_COLOR) are identical, so Katana and Saber spawns are
  unaffected (verified by grep: both still construct it the same way).

## Sounds (all DbaSounds, same call sites, volumes and pitches)

| Site | Before | After |
| --- | --- | --- |
| Item charge pulse, every 20 ticks | ENDERMAN_AMBIENT | VOID_AMBIENT |
| Item rush release | WARDEN_HEARTBEAT | HEARTBEAT_DARK |
| Item 3rd dash sweep | PLAYER_ATTACK_SWEEP | SWING_LIGHT (SLASH_WHOOSH layer already present, kept) |
| Item dash destination | ENDERMAN_TELEPORT | TELEPORT_WHOOSH (DARK_PULSE layer already present, kept) |
| Rift sound pulse, every 25 ticks | PORTAL_AMBIENT | RIFT_AMBIENT |
| Rift burst boom | GENERIC_EXPLODE | RELEASE_DARK |
| Rift burst sonic layer | WARDEN_SONIC_BOOM | BOOM_SONIC (WIND_GUST layer already present, kept) |
| Slash hit | PLAYER_ATTACK_SWEEP | SLASH_WHOOSH |

Choice note: the charge pulse plays repeatedly while holding the charge, so
VOID_AMBIENT (the guide's ambience option) reads better than CHARGE_DARK,
which is a one-shot rise.

## Damage (ability damage only, numbers untouched)

- Dash path hit: mobAttack(player) to DbaDamage.voidDmg(player).
- Rift channel pulse and implosion burst: the damageSource() helper's
  playerAttack / mobAttack / mobAttack(victim) branches replaced by
  DbaDamage.voidDmg(this, owner), the rift as direct entity.
- Void slash hit: mobAttack to DbaDamage.voidDmg(this, owner), the slash
  as direct entity. The damage > 0 guard is unchanged, so an unset slash
  still deals no damage.
- The plain melee hit is untouched and stays the vanilla player attack,
  per the standard's honest exception. Every damage number, falloff,
  cooldown, drain, radius, duration and effect application from the
  spectacle pass is byte-identical in the item and entity logic.

## Particles

Nothing to convert: the owned files already emitted only DbaParticles and
LayeredParticles before this pass, and still do. Before 0, after 0.

## Verification (javac audit only, no Gradle build run or claimed)

Targeted compile of the 7 owned files with the sourcepath over the full
tree, JDK 25, -Xmaxerrs 10000, run on both Phase 1 classpaths:

- 26.2 (cp262.txt): zero errors in all 7 owned files. The only errors in
  the pulled graph are 4 pre-existing lines in DbaItems.java and
  DbaMenus.java (registration boilerplate kinds present in the baseline).
- 26.3 (cp263.txt): zero errors in all 7 owned files; 11 pre-existing
  error lines elsewhere in the pulled graph, none in Hollow's Edge files.

Two compile fixes were needed during the pass and are worth the
coordinator's attention: (1) lambda captures in VoidRiftRenderer needed
final snapshots of the reassigned fade and choreography alphas; (2) the
billboard line poseStack.mulPose(Quaternionf) does not exist on the 26.3
PoseStack. It now goes through new Matrix4f().rotation(new
Quaternionf(cameraState.orientation)), the same rotation on 26.2 and the
idiom SkyCracksRenderer and TimeShatterRenderer already use. That line
carried the identical call at HEAD, so this also removes one pre-existing
26.3 error kind rather than adding one.

## Deviations and honest worse check

The client cannot run in this sandbox, so the final look is judged in
game on Carter's PC. Nothing here was reverted, but four trades should be
named plainly:

1. Void slash tilt: the renderer now rolls the crescent 15 degrees toward
   its synced TILT_RIGHT. The entity has synced and extracted this field
   since the spectacle pass and the old renderer never drew with it. It
   is the one deliberate visual addition, and it is a one-line revert if
   the upright slash is preferred.
2. Afterimage silhouette: the box ghost had separately articulated arms
   and legs; a plume column cannot articulate limbs, so the ghost now
   reads as a dissolving spectral column with a hair cap, eye slit,
   scanlines, wake ribbons and rising flecks. Every color, alpha, timing
   and the skin/hair tinting are preserved, but the humanoid limb read is
   weaker. This is the pass's biggest visual trade.
3. Rift mouth outline: the dome rim replaces the drawn lip rim, so the
   mouth edge is softer and the old mouth's slight crook asymmetry is
   gone (the domes are symmetric, noise-displaced instead). Colors, band
   choreography and collapse staging are unchanged.
4. Slash wake: torn ribbons fade to 45 percent at the tail where the old
   extruded trail faded to zero, so the wake is slightly more present
   mid-flight. It still dies with the same fade envelope.

## Toolkit gaps and change requests

- No blocking gaps. All shapes were buildable from the Phase 1 toolkit.
- One real gap for the coordinating pass to consider: the toolkit has no
  humanoid figure builder, so any ghost or clone effect loses limb
  articulation when converted (see trade 2). If Carter wants the limbs
  back, a shared posed-figure builder (plume torso plus tornRibbon limbs
  from synced yaw/pitch) belongs in Phase 1's toolkit, not in one weapon.
- displacedDome floors its rim alpha at 0.35 of the passed alpha, so the
  rift shell rim never fully vanishes into the throat dome. Shared toolkit
  behavior, noted for consistency with other dome users.
