# Bespoke Report: Whis Staff

Phase 2 bespoke conversion, written against audits/BESPOKE-STANDARD.md.
Files edited, only these three: item/WhisStaffItem.java,
entity/TemporalRiftEntity.java, client/render/TemporalRiftRenderer.java.
TimeShatter files untouched (coordinating follow-up, as before).
ErasureChargeOrb files untouched (Blaster Gun agent). Every number,
cooldown, Ki cost, and mechanic from the spectacle pass is identical,
including the split rift damage (200 + Spirit x 0.75 at cast, the same
again at tick 15), the reversal starting at tick 60 for 100 ticks, the
140 tick cooldown, and the auto-dodge (LivingEntityMixin, not my files,
unchanged).

## Before and after counts

| Check | Before | After |
|---|---|---|
| Vanilla ParticleTypes references, 3 files | 0 | 0 |
| Vanilla DustParticleOptions references | 0 | 0 |
| Vanilla SoundEvents call sites, item | 6 | 0 |
| Vanilla SoundEvents call sites, entity | 2 | 0 |
| Vanilla damage sources (damageSources() calls), item | 4 | 0 |
| Vanilla damage sources, entity | 1 | 0 |
| KiRenderHelper primitive draws, renderer | 0 | 0 |
| Local regular geometry helpers, renderer | 8 | 0 |
| Hand-rolled segment loops in renderer submit | 7 blocks | 0 |

Particles were already fully custom before this pass (KI_MOTE core and
LayeredParticles theme dust), so rule 2 needed no swaps here.

## Geometry (rule 1)

All eight local helpers are deleted (drawQuad, drawRotatingRing,
drawLash, lashSpan, drawClockHand, drawTiltedOrbitalRing,
drawMeridianStream, drawOctahedralJewel), along with every lat/long and
equal-segment loop in submit. The renderer is now composed only of
BespokeGeometry builders:

- Outer dome shell: displacedDome, celestial cyan (0.45, 0.80, 1.0),
  alpha 0.30 at peak, matching the old shell's apex alpha.
- Inner core shell: displacedDome at 0.72 radius, silver (0.85, 0.96,
  1.0), alpha 0.22.
- Internal time currents: three brokenVortexBand calls at radii 0.20,
  0.52, and 0.75 of the dome radius (the old LayeredRender band radii),
  twisting at +2.6, -1.7, and +0.9 degrees per tick of age, at the
  standard layer alphas (core 0.85, mid 0.45, outer 0.18, each x 0.85
  envelope factor as the old shared call applied). These replace the
  LayeredRender.drawVortexBands call with the toolkit bands directly,
  as the brief specified.
- Anchor disc: glowFan at 0.98 radius, same color and 0.50 peak alpha
  as the old drawAnchorGlow call.
- Hour steles: shardFan at 0.88 radius. The toolkit has no ring or
  stele builder; the fan's uneven shards burst around the same rim
  annulus the twelve even steles occupied (0.82 to 0.98 radius).
- Meridian streams: twelve branch ribbons from the rim (0.98 radius)
  converging just off the apex, endpoints turning at the old 6 degrees
  per tick of age, per-stream seeds.
- Clock hands: tornRibbon on Y-rotated poses. Hour hand length 0.42
  radius at pose rotation age x 15 degrees, minute hand 0.72 radius at
  age x 45 degrees; the pose degrees are the negation of the old hand
  angles because a Y rotation carries +X toward negative Z, so the
  hands sweep backwards at exactly the old rates and directions.
- Orbital rings: brokenVortexBand on X-tilted poses at plus and minus
  42 degrees around the old ring height (y 1.0), thin band (y -0.10 to
  0.10), twisting against each other at the old 16 degrees per tick.
- Center and zenith jewels: small displacedDome crystal caps (0.55 and
  0.85 x build scale) where the octahedron jewels sat. The toolkit has
  no polyhedron builder; a noise-displaced dome reads as a faceted
  crystal and keeps the silhouette bespoke. The zenith cap sits in a
  pose translated to dome height plus 0.10, the old jewel center.

Seed: TemporalRiftEntity gains a synced SEED field (LONG), rolled once
in the cast constructor from the level random and read by the renderer
through a new render state field. Every builder call derives its seed
from it. If the synced value is still 0 (pre-sync frames or a spawn
that bypassed the cast constructor), the renderer falls back to a hash
of the synced block position and caster id, which agree on every
client. No client-side random and no wall-clock anywhere. Shapes are
frame-stable per the standard: only alpha, radius unfold, and band or
stream twist move.

The shared FxEnvelope (build 0.15, decay 0.35), the build-only radius
unfold, the pulse, and distance scaling through LayeredRender.distScale
and FxEnvelope.segments (inside the builders) are all preserved.

## Particles (rule 2)

No changes needed and none made: zero vanilla references before and
after. The rift's layered emission (LayeredParticles.emitVortex and
emitRain with DbaParticles.KI_MOTE and celestial cyan theme dust) was
already all-custom after Phase 1.

## Sounds (rule 3)

All eight vanilla call sites swapped per the DbaSounds guide, same
positions, volumes, and pitches:

| Site | Before | After |
|---|---|---|
| Entity ambient hum, every 20 ticks | AMETHYST_BLOCK_RESONATE (0.8, 1.2) | DbaSounds.RIFT_AMBIENT |
| Entity peak tick burst | AMETHYST_CLUSTER_BREAK (0.8, 1.8) | DbaSounds.CRYSTAL_CHIME |
| hurtEnemy shatter | AMETHYST_CLUSTER_BREAK (1.2, 1.6) | DbaSounds.CRYSTAL_CHIME |
| hurtEnemy activation layer | BEACON_ACTIVATE (0.9, 1.9) | DbaSounds.CHARGE_RISE |
| performTemporalStrike cracks | AMETHYST_CLUSTER_BREAK (1.0, 1.7) | DbaSounds.CRYSTAL_CHIME |
| applyStaffHit shatter | AMETHYST_CLUSTER_BREAK (1.2, 1.6) | DbaSounds.CRYSTAL_CHIME |
| use() milestone layer | BEACON_POWER_SELECT (1.4, 1.2) | DbaSounds.CHARGE_FULL |
| use() cast layer | EVOKER_PREPARE_SUMMON (1.0, 1.6) | DbaSounds.CAST_MAGIC |

The pre-existing DbaSounds.ABILITY_CHARGE layers were already bespoke
and are unchanged. CAST_MAGIC was chosen over CAST_DARK for the evoker
swap: the staff is celestial, not dark.

## Damage (rule 4)

All five ability damage calls now use DbaDamage.temporal; the plain
melee swing itself is untouched (it is the vanilla item attack path,
the standard's stated exception, and is not a call in these files):

| Site | Before | After |
|---|---|---|
| use() cast half, 200 + Spirit x 0.75 | damageSources().magic() | DbaDamage.temporal(player) |
| Entity peak half at tick 15 | damageSources().magic() | DbaDamage.temporal(this, owner) |
| hurtEnemy Dexterity x 2 bonus hit | damageSources().playerAttack(player) | DbaDamage.temporal(player, player) |
| hurtEnemy 1.5 percent max HP bonus hit | damageSources().magic() | DbaDamage.temporal(player) |
| applyStaffHit extended strike hit | damageSources().playerAttack(player) | DbaDamage.temporal(player, player) |

On the brief's direct-plus-attacker question: DbaDamage supports it
(the two-argument factory), and it is used where the old source had
that shape. The vanilla playerAttack source is direct and attacker
both the player, so the Dexterity bonus hit and the extended strike
hit use temporal(player, player), an identical source shape with the
type swapped. The extended strike's percent-health bonus is folded
into that single combined hit in the code (base plus Dexterity plus
percent health in one hurtServer call), so the whole hit converts
together rather than being split; splitting it into two same-tick
calls would let the target's invulnerability window eat the second
call and silently change the total. The rift entity's peak half uses
temporal(this, owner): the rift is the direct entity, the caster the
attacker, and the factory anchors on the rift if the caster is gone.
The two magic-shaped halves (cast and percent health in hurtEnemy)
use the attacker-only factory.

## Incidental fix in my own file

TemporalRiftEntity's two target filters captured the local `owner`,
which the caster re-fetch reassigns, so the file carried two javac
error kinds (lambda capture of a non-effectively-final variable) at
HEAD, one per filter. Both filters now capture a final alias,
ownerRef, declared beside ownerLiving. Behavior is identical (same
owner value at filter time), and the two pre-existing kinds in my file
are gone. This is the only non-conversion edit.

## Verification

Targeted javac audit (the standing method; no Gradle run or claimed):
my three files compiled with -sourcepath over the whole tree against
both audit classpaths, before the ownerRef fix and after.

- First run: the only error lines naming my files were the two
  pre-existing TemporalRiftEntity lambda kinds, identical on 26.2 and
  26.3 (13 total errors on 26.2 and 17 on 26.3 across the closure, the
  rest in sibling agents' in-flight files and known baseline noise:
  DbaItems, DbaMenus, SpiritSwordItem, BanshoCycloneEntity, and the
  26.3 mob entity kinds).
- After the ownerRef fix: zero error lines naming WhisStaffItem,
  TemporalRiftEntity, or TemporalRiftRenderer on either classpath.
  Remaining errors are the same sibling and baseline files as the
  first run.
- So the conversion introduces zero new error kinds on 26.2 and zero
  on 26.3, and removes two pre-existing ones. Grep counts after:
  ParticleTypes 0, DustParticleOptions 0, SoundEvents 0,
  damageSources() 0, primitive draws 0, local draw helpers 0, across
  all three files. No em dashes in any edited file or this report.

## Shared change requests

None required. Two toolkit limits were worked within, recorded here
for the coordinator's awareness rather than as requests: there is no
ring or stele builder (the ground rim is carried by glowFan plus
shardFan) and no polyhedron builder (the jewels are displacedDome
crystal caps). Both substitutions stay inside BespokeGeometry as the
standard requires; if the coordinator later adds a ring or gem
builder to the toolkit, this renderer is the natural first adopter.

## Worse check (honest)

- The dome silhouette is now noise-displaced and irregular instead of
  a smooth lat/long glass shell. That is the pass working as written
  (bespoke over regular), and the two-shell cyan-to-silver depth, the
  alphas, and the envelope are preserved; but a reader who loved the
  perfectly smooth dome will see a more organic one. Not reverted:
  keeping the smooth shell would mean keeping the banned hand-rolled
  regular geometry.
- The hour steles no longer rotate with the ground fan and are uneven
  shards instead of twelve even markers: builders are frame-stable by
  design, and shardFan has no twist parameter. The clock read is
  carried by the two sweeping hands and the turning meridian streams,
  which do still move at the old rates. Accepted, flagged here.
- The orbital rings are broken bands now, with seeded gaps, instead
  of continuous thin rings. Same tilt, height, radius, and counter
  twist; the gaps are the toolkit's signature. Accepted.
- The jewels lost their octahedron facets (see above). The caps keep
  the positions, sizes, color, and alpha. Accepted, and the first
  candidate if a gem builder ever joins the toolkit.
- Sound identity: the amethyst and beacon sounds are gone, replaced
  by the synthesized bank at the same volumes and pitches. The chime
  and rise sit in the same roles; whether the synthesized timbres read
  as celestial as the vanilla amethyst did is a judgment only an
  in-game listen settles, and it is Carter's call on his PC, as with
  every sound swap in this pass.
- Nothing was reverted. No file outside my list was edited by me.
