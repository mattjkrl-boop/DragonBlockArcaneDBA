# Scope 2 A2: Wish Ritual conversion report

Base: ef5ad02 (Scope 2 foundation). Nothing committed. Only 4 of the 7
owned files changed; DbaGeoDragon.java, WishScreen.java, and
WishClient.java were re-verified at zero vanilla references in every
category and left untouched, as the audit said. No Lava, Evil Spear,
Dread, or other agent's files were touched. The dragons, dragon ball
blocks, payloads, and WishScreen plumbing were not churned.

## Before and after counts (owned files, grep verified)

- Vanilla SoundEvents call sites: 12 to 0 (DragonWishRitual 3,
  ShenronEntity 4, PorungaEntity 5), plus the SoundEvents import in
  DragonWishRitual removed.
- KiRenderHelper primitive call sites in WishRitualVisuals: 8 to 0
  (drawSphere 4, drawCylinder 2, drawHelix 2).
- Hand-rolled regular geometry helpers in WishRitualVisuals: 4 to 0
  (drawBall, drawColumn, drawStandingRing, drawTorus). The two submit
  wrappers (submitTorus, submitSphere) were replaced by submit
  wrappers over builders. Surviving private helpers (submitDomePair,
  submitDomePairAt, ringBandPair, drawHelixRibbon) are pose glue
  only: they position the pose stack or chain builder calls and emit
  no vertices. KiRenderHelper.vertex direct calls: 4 (inside
  drawTorus) to 0.
- BespokeGeometry builder call sites in WishRitualVisuals: 0 to 12
  (displacedDome 2, shardFan 1, plume 2, brokenVortexBand 5,
  branch 2, counting the chained calls inside the glue helpers).
- Trig lines in WishRitualVisuals: 14 to 9. The remaining 9 are
  placement math only (mote orbit, ribbon endpoint placement,
  pip seating, rotateY), not geometry emission.
- Vanilla ParticleTypes: 0 to 0. DustParticleOptions: 0 to 0.
  Damage sources: 0 to 0. The area had none and gained none.
- Em dashes in changed files: 0.

## Sounds (12 swaps, volumes, pitches, and sources identical)

Per the DbaSounds swap guide and the Scope 2 foundation (D7):

- BEACON_ACTIVATE to CHARGE_RISE: ritual start (1.6, 0.55, HOSTILE),
  Shenron grant (2.0, 1.2, NEUTRAL), Porunga grant (2.0, 1.2, NEUTRAL).
- PORTAL_TRAVEL to TELEPORT_WHOOSH: storm onset in DragonWishRitual
  (1.2, 0.55, HOSTILE), Shenron departure (1.5, 0.7, NEUTRAL),
  Porunga departure (1.5, 0.7, NEUTRAL). The guide offers
  TELEPORT_WHOOSH or RIFT_OPEN for this event; all three sites are
  one-shot travel whooshes (storm arrival, dragon departure), not
  rift openings, so TELEPORT_WHOOSH is the honest fit at all three.
- ENDER_DRAGON_GROWL to DRAGON_GROWL (new bank event): dragon
  arrival in DragonWishRitual (2.5, 0.8 Earth / 0.5 Namek, HOSTILE),
  Porunga between wishes (2.5, 0.8, HOSTILE).
- ENDER_DRAGON_FLAP to DRAGON_FLAP (new bank event): Shenron
  departure (1.8, 0.8, HOSTILE), Porunga departure (1.8, 0.8, HOSTILE).
- UI_TOAST_CHALLENGE_COMPLETE to WISH_GRANTED (new bank event):
  Shenron grant (1.5, 1.0, NEUTRAL), Porunga grant (1.5, 1.0, NEUTRAL).

ShenronEntity and PorungaEntity use fully qualified sound references
throughout the file by existing style, so the swaps keep that style
(com.dragonblockarcanedba.sound.DbaSounds.X). DragonWishRitual uses
a normal import. The git diff of the three files contains only these
swap lines and the one import change.

## Geometry: WishRitualVisuals rebuilt on BespokeGeometry

Seed: the client Ritual record gained a seed field, mixed in begin()
by seedOf() from synced payload state only: the payload gameTime,
the Namek flag, and the ritual center, which is computed from the
synced ball positions in the payload. No client random, no wall
clock, no new synced data. Every client that received the payload
derives the same seed and draws the same silhouettes. Each element
takes a fixed offset from the ritual seed (per ball, per pip, per
ring, per column layer). Distance scaling now comes from
FxEnvelope.distScale on the camera distance, which the old visuals
did not do at all.

- Dragon balls (was drawSphere body + core + one sphere per star
  pip): body and bright core are displacedDome pairs (upper dome
  plus a flipped dome, the Erasure charge orb precedent), so each
  ball is a lumpy seeded shell at the old radius, still spinning at
  the old age rate. Star pips are shardFan bursts seated on the
  surface at the exact old pip positions (radius 0.82 ring at
  height 0.36), tilted to sit on the surface, in the old pip red.
- Summoning column (was drawCylinder x2 + drawHelix x2 under an
  XP 90 pose): drawn upright now. A plume core (radius 0.56,
  alpha 0.5) carries the volume where the bright core cylinder ran;
  two brokenVortexBand shells (alpha 0.38 and 0.3) turn slowly with
  the phase where the smooth outer cylinder stood; a bright
  brokenVortexBand heart (alpha 0.8) replaces the thin core
  cylinder. The twin helixes are two branch ribbons chained from
  two branch spans each, wrapping about 1.2 turns on the way up at
  the old helix radius, palette color at 0.9 and white at 0.55,
  rotating with the phase exactly as the helix pair did.
- Rings (was the hand-rolled drawTorus at four sites): every torus
  is now a ringBandPair, two brokenVortexBands back to back across
  the ring plane, covering the formation ring around the balls,
  the ground ring, the arrival burst ring, and both rings of the
  standing gate. The standing ring keeps its XP 90 standing pose
  and its two radii (full and 0.72) and colors.
- Rising motes (was drawSphere via submitSphere): each mote is a
  small plume blob centered on the mote point, same orbit radius,
  rise pattern, color, and alpha.
- Arrival burst flash (was drawSphere): a displacedDome pair at the
  old center, radius, color, and alpha envelope.

One audit note corrected by the code: the audit described
drawStandingRing as sphere studs on a ring. The code at ef5ad02
draws two plain toruses and has no studs, so the conversion is a
brokenVortexBand pair per ring and no glowFan studs were invented.
The audit's other mappings (balls, column, torus) matched the code
and were followed.

## Dual-version fix found during verification

The first 26.3 targeted compile showed 5 errors in
WishRitualVisuals: mulPose(Quaternionf) does not exist on the 26.3
PoseStack. Two of the five sites were pre-existing at HEAD (the
ball spin and the standing ring tilt, the same known kind other
files carry); three were mine. All five now use the codebase's
dual-version form, mulPose(new Matrix4f().rotation(...)). This
removes the pre-existing 26.3 error kind from this file. No other
file was changed for this.

## Verification (targeted javac, JDK 25, audit only, no Gradle)

Owned 7 files compiled with full sourcepath closure on both audit
classpaths:

- cp262 (26.2): zero errors in any owned file. WishRitualVisuals
  classes emitted.
- cp263 (26.3): zero errors in any owned file after the mulPose
  fix. WishRitualVisuals classes emitted.

Residual errors in those runs are only in foreign files pulled in
by the sourcepath (DragonBlockArcaneDBAClient classpath artifacts
such as the missing lwjgl package on the audit classpath, and the
known DbaItems baseline kind); zero of them reference a wish,
dragon, or ritual symbol, and two other agents' in-flight areas
were editing this tree concurrently.

## Honest worse check

Nothing reverted. Real trades, stated plainly:

- The balls lose their perfectly smooth sphere read. The dome pair
  leaves a faint equator seam (displacedDome rims die to 0.35
  alpha) and the silhouette is lumpy by design. The star pips are
  flat shard bursts, not round red beads; from some angles a pip
  reads as a small red star decal rather than a stud.
- The rings are ribbons, not tubes. A torus had round volume from
  every angle; the band pair is thinnest exactly edge-on, and the
  seeded band gaps (about 16 percent of columns) mean the ring is
  broken in places where the torus was continuous. This is the
  toolkit's standing trade from the first pass, not new here.
- The column's plume core is five stacked puff discs, so at full
  28 block height the core reads as beads of light inside the band
  shells rather than a continuous shaft. The bands and ribbons
  carry the column's continuity.
- The helix ribbons wrap about 1.2 turns where the old helixes
  coiled 4 turns, and they are jagged branch strips, not smooth
  coils. They rotate and climb at the old rate, but the tight coil
  read is gone.
- The shell bands now turn slowly with the phase; the old outer
  cylinder was static. This reads as energy motion and matches the
  first pass's band conversions, but it is a behavior addition,
  flagged here rather than hidden.
- The synthesized dragon growl, flap, and wish fanfare versus the
  vanilla ender dragon and toast sounds is a listen-only judgment
  for Carter's PC test, as with every sound swap in both passes.

## Shared change requests

None. The foundation's D7 events covered every sound, and
BespokeGeometry covered every shape with shipped builders. No
shared file was edited.
