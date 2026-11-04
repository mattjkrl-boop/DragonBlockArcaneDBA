# Bespoke Report: Sickle of Sorrow

Phase 2 weapon report. Files owned and touched: item/SickleOfSorrowItem.java,
entity/SorrowSlashEntity.java, entity/DimensionalRiftEntity.java,
client/render/SorrowSlashRenderer.java, client/render/DimensionalRiftRenderer.java.
Theme unchanged: soul reaping, a melting dimensional rift, sorrow. Violet and
magenta palette, soul particles, and the singularity core are kept. Every
number and mechanic from the spectacle pass is identical: no constant,
formula, cooldown, drain, radius, or timing line was touched, including the
technique level cost scaling in the item (melee drain over 13 ticks, air
swing over 8, rift over 60, all from the same level scaled base rate).

## Before and after counts

| Category | Before | After |
| --- | --- | --- |
| Local geometry draw helpers in my renderers | 6 (drawCrescentArc; drawBeveledShockRing, drawRotatingFlatRing, drawVerticalTearPlane, draw3DShardPyramid, drawSingularityOctahedron) plus a per-index hash helper | 0 |
| Local helper call sites | 17 (6 crescent layers; 1 beveled ring, 4 flat rings, 3 tear planes, 1 shard loop over 18 pyramids, 2 octahedra) | 0 |
| KiRenderHelper primitive draws (box, cylinder, disc, sphere, helix) | 0 | 0 (this weapon never called them; kiRenderType is still used, which is the shared render type, not a primitive draw) |
| Vanilla ParticleTypes references | 1 (an unused import in SorrowSlashEntity) | 0 |
| Vanilla DustParticleOptions references | 0 | 0 |
| Vanilla SoundEvents call sites | 7 (2 in SorrowSlashEntity, 5 in DimensionalRiftEntity) | 0 |
| Vanilla damageSources() expressions | 4 (item melee bonus hit; slash wave; rift burst; rift tick pulses) | 0 |

Particles needed no call site swaps: every emission in this weapon already ran
on DbaParticles types (SPARK, VOID_SMOKE) or the shared LayeredParticles after
the spectacle pass. The one vanilla reference was a dead import, now removed.

## Geometry: builders used

Seeds come from a new seed field on each render state, computed in
extractRenderState from the entity id and position (both arrive in the spawn
packet, so every client agrees), mixed with xor salts per layer and element.
The slash seed also mixes the synced tilt and impact burst flags.

SorrowSlashRenderer, BespokeGeometry.bladeArc for all six crescent layers
(wake, void silhouette, magenta crest, cyan edge, hot core, and the impact
burst's crossing crescent). Mapping: bladeArc at a 140 degree sweep has a tip
to tip width of radius x sin(70 degrees), so radius 8.9 spans the same 8.4
blocks the old parabolic bow spanned at span 4.2; the other layers scale by
the old span ratios (10.9, 9.85, 6.4, 5.16, 4.9). bladeArc anchors its moon at
its circle center with the belly toward +X, so each layer gets its own submit:
rotate Y -90 to face the belly +Z, the old bow's direction (+90 for the
flipped crossing crescent), and translate by that layer's own center offset
(tips at 0.5 x radius, belly at about 1.12 x radius) so the layers stay
concentric on the entity exactly as the scaled bow copies were. Pose yaw,
pitch, reap tilt, and open scale are untouched, and the shared envelope still
drives alpha.

DimensionalRiftRenderer:
- brokenVortexBand x5: the outer accretion wall (radius, height 0.55 x pulse),
  its bright lip band (radius x 1.03, counter twist at the old lip speed), the
  middle ring (0.65 x radius, twist at the old -1.4 x rot), the inner ring
  (0.35 x radius, twist at the old 2.2 x rot), and the singularity horizon
  (coreSize x 1.3). Ring colors and alphas are the old ones.
- tornRibbon x6: the three vertical fissures. Each plane is one submit,
  rotated upright (Z +90 turns the ribbon's length up) and around Y by the
  plane angle (0, 60, 120 degrees, as before), drawing a wide radiant sheet
  in the plane's color and a narrow dark void strip (0.05, 0, 0.10) down its
  middle, the old lens's core and edge colors in ribbon form. Height and
  width formulas are the old vHeight and vWidth (width doubled, since
  tornRibbon takes full width where the lens took half width).
- shardFan x2: the orbital spikes. Two fans at 0.90 x and 0.68 x of the old
  orbit radius (the old two bands were 1.0 x and 0.84 x of an orbit radius
  that eased inward with pull, and that easing is kept), in the old base and
  tip colors, inside one submit rotated around Y at the old spikes' 0.8 x
  angular speed. The fans' seeded irregular spacing keeps the spikes' varied
  golden angle character.
- displacedDome x4: the singularity. A dark dome pair (top submit plus a
  mirrored bottom submit through scale(1, -1, 1)) for the body and a small
  white dome pair for the heart, centered at y 0.5 where the octahedron sat,
  at the old coreSize formula.
- LayeredRender.drawAnchorGlow and drawVortexBands are kept as they were;
  both are already bespoke after Phase 1. Segment counts now step down with
  LayeredRender.distScale in the slash renderer too (it previously drew a
  fixed 40 segments; the rift already distScaled).

## Particles

Zero vanilla references remain (see counts). No emission counts, types, or
call sites changed.

## Sounds: swaps made (volumes and pitches unchanged everywhere)

SorrowSlashEntity, per hit target:
- PLAYER_ATTACK_SWEEP (1.6, 0.5) -> DbaSounds.SLASH_WHOOSH
- SOUL_ESCAPE (1.2, 0.7) -> DbaSounds.SOUL_DRIFT

DimensionalRiftEntity:
- Cast tick: WARDEN_SONIC_BOOM (2.5, 0.6) -> DbaSounds.BOOM_SONIC;
  RESPAWN_ANCHOR_SET_SPAWN (2.0, 0.5) -> DbaSounds.CHARGE_DARK (the guide's
  dark variant for charge starts). WIND_GUST was already a DbaSounds event
  and stays, now via the import.
- Burst hit: SOUL_ESCAPE (0.9, 0.6) -> DbaSounds.SOUL_DRIFT.
- Ambient every 20 ticks: PORTAL_AMBIENT (1.2, pitch formula) ->
  DbaSounds.RIFT_AMBIENT, same volume and pitch formula.
- Collapse tick 50: RESPAWN_ANCHOR_DEPLETE (2.0, 0.7) ->
  DbaSounds.RELEASE_DARK. Judgment call, flagged for the coordinating pass:
  the swap guide groups RESPAWN_ANCHOR_DEPLETE under charge milestones
  (CHARGE_FULL), but this call site is the implosion beat, where the rift
  lets go and flings everything it caught; a dark release blast reads
  clearly better there than a charge milestone chime. It is a one line
  change if the coordinating pass wants CHARGE_FULL instead.

## Damage: conversions made

All ability damage now uses DbaDamage.voidDmg; every amount still passes
through the same DbaEffectsConfig.capDamage call as before.

- Item melee bonus hit (Strength x 3): playerAttack ->
  DbaDamage.voidDmg(serverPlayer). The plain weapon hit itself is untouched
  and stays the vanilla player attack, per the standard's honest exception:
  it comes from the item attribute pipeline, not from a coded source. Soul
  Rend healing is mechanically identical (3 percent of 750 + Strength x 3,
  same heal call, same point in the sequence).
- Sorrow slash wave: the three way playerAttack / mobAttack / generic
  branch -> DbaDamage.voidDmg(this, owner), the slash as direct entity.
- Rift burst: playerAttack / mobAttack / magic -> DbaDamage.voidDmg(this,
  owner), the rift as direct entity.
- Rift tick pulses: playerAttack / magic -> DbaDamage.voidDmg(this, owner).

One attribution note: the rift and slash now pass the ability entity as the
direct entity in every owner case; the old player owned case used the player
as the direct entity. That matches the standard's mapping for ability
entities, the attacker is still the owner, and kill credit is unchanged.

## Shared change requests

1. A shard orbit builder: seeded 3D shards placed on a ring with per shard
   size, thickness, lean, height band, and bob, so orbiting crystal effects
   (these spikes, and any sibling effect with the same read) can keep their
   floating 3D character on the toolkit. shardFan is flat by design, so I
   used it as prescribed and did not build a private copy (rule 1). This is
   the one visible flattening in this conversion, detailed in the worse
   check.
2. Confirm or flip the collapse sound judgment call above (RELEASE_DARK vs
   the guide's CHARGE_FULL for RESPAWN_ANCHOR_DEPLETE).

## Worse check

Honest trades, none judged a fail, nothing reverted:

- Crescent silhouette: the old bow was shallow (8.4 wide, 1.35 deep) with a
  built in alpha trail behind it. bladeArc is a true scythe moon: same 8.4
  tip to tip, a much deeper belly, nicked edges, no built in trail. It reads
  as a reaper crescent and the wave still trails VOID_SMOKE from the entity,
  with the dim wake layer kept, but it is a real silhouette change, not a
  copy. A wider sweep would flatten it toward the old bow if Carter prefers.
- Tears: the lens profile pinched shut at the crown; tornRibbon sheets taper
  only slightly and end at about 45 percent alpha at the top. The tears read
  as torn sheets rather than eyes now. Colors, heights, widths, and the
  three crossed planes are unchanged.
- Spikes: the 18 floating 3D pyramids, each with its own bob, lean, and
  height band, become two flat shard fans at ground level. They keep the
  palette, the two orbit bands, the inward easing, and the orbit speed, and
  fan tips lift up to 0.22 x fan radius, but the floating crystal volume is
  genuinely reduced. This follows the prescribed builder; change request 1
  is the path back if it reads worse in game.
- Singularity: the regular octahedron becomes a displaced dome pair, less
  crystalline, more organic, which suits a black hole. Horizon band and
  white heart keep their roles and colors.
- Outer accretion ring: the beveled three stop vertical gradient becomes a
  single color broken band with a bottom to top alpha falloff; the bright
  lip survives as its own band. The ring now also has seeded gaps, in
  keeping with the toolkit's broken band language.
- Sounds: the synthesized events necessarily have different timbre from the
  vanilla originals; that is the pass working as intended. No sound was
  added or dropped, and every volume and pitch is the old one.
- Death messages: kills from these abilities now report the void death
  message instead of player attack or magic. Amounts and the cap are
  unchanged; the Phase 1 damage JSONs mirror the vanilla shapes, so armor
  and difficulty scaling behavior is preserved by design.

## Verification

Targeted javac audit (the sandbox cannot run Gradle; this is the repo's
audit method, not a build): my five files compiled with the full sourcepath
against the 26.2 and the 26.3 audit classpaths, before edits and after.
Errors in my files: 0 on 26.2 and 0 on 26.3, before and after; zero new
error kinds. The unrelated error kinds pulled in through the sourcepath
closure (DbaItems, DbaMenus, mob entities) were identical on 26.2 (4 lines)
before and after; on 26.3 the pulled in set shrank between runs because the
Bansho agent's in flight file changed state, which is not my file and not
my dependency. Final greps on my five files: ParticleTypes 0,
DustParticleOptions 0, SoundEvents 0, damageSources() 0, KiRenderHelper
primitive draws 0, local draw helpers 0, em dashes in added lines 0.
Not committed, per the coordination rules.
