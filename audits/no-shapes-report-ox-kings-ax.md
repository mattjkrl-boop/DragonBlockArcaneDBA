# No-Shapes Report: Ox King's Ax

Weapon: Ox King's Axe (item class OxKingsAxeItem).
Pass: no-shapes realism, Phase 2. Base HEAD ad22b32 (Phase 1 foundation).
This owner is a replacement: the previous Ox agent died to an
infrastructure error mid-work. All seven files below arrived as its
partial diff in the working tree. Each was audited line by line for
completeness before anything was finished or fixed. Nothing was reset
or reverted, and no other owner's files were touched.

## Files

From the partial diff (all seven):

- client/render/KingsSlamRenderer.java
- client/render/OxChargeRenderer.java
- client/render/OxFissureRenderer.java
- client/render/OxShockwaveRenderer.java
- client/render/OxStanceAuraRenderer.java
- effect/FissureStunEffect.java
- effect/OxBraceEffect.java

fx/SlamDebris.java is read-only for this owner and is untouched. The
ring-destruction behavior in the shockwave entity is mechanics and is
untouched. Item and entity classes were not in the partial diff and are
unchanged in this pass; the bespoke pass already converted their
particles, sounds, and damage.

## Audit of the partial diff, and what had to be fixed

KingsSlamRenderer: the flagged inconsistency (a mid-edit snapshot whose
submit() referenced Frame fields vaporHeight and geysers that the Frame
class did not declare) was a snapshot artifact only. The file as
inherited is coherent: Frame declares exactly center and radius, both
read by its sources, and the geyser anchors live on
Emission.geyserOffsets, which is declared, built for the flawless
variant, and read by the geyser source. No fix was needed there.

OxShockwaveRenderer: two real defects, both fixed.

1. Five compile errors: the crestRing call sites passed bare double
   literals (0.14, 0.10, 0.02, 0.06, 0.05) to a float parameter, a lossy
   conversion error on both classpaths. Fixed by writing them as float
   literals. Values unchanged.
2. Frame.crestHeight was computed and written every frame but never
   read by any source, a half-finished touch. It is now wired into
   crestRing: the vertical spread of the crest emission scales with the
   live crest height (clamped 0.25 to 2.5), so the dust and vapor
   column grows and settles with the wave the way the bespoke dust
   walls did.

OxChargeRenderer, OxFissureRenderer, OxStanceAuraRenderer,
FissureStunEffect, and OxBraceEffect: audited complete and coherent.
Every Frame field is declared and read, every emitter is built and
ticked, lifecycle envelopes match the bespoke builds, and no fix was
needed.

The two effect files are presentation-only particle swaps, colors and
rates preserved: FissureStunEffect's DUST_PUFF became the DUST material
(0xEBE8E0), and OxBraceEffect's DbaDustOptions (0xFF4500, 1.2f) became
the DUST material with the same color and scale, seeded per tick.

## Geometry counts, before and after

Before counts are the binding inventory in audits/no-shapes-audit.md,
verified against HEAD ad22b32 by direct grep (builder call sites).

| File | BespokeGeometry before | after | LayeredRender geometry before | after | addVertex before | after |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| KingsSlamRenderer | 10 | 0 | 1 | 0 | 0 | 0 |
| OxChargeRenderer | 8 | 0 | 1 | 0 | 0 | 0 |
| OxFissureRenderer | 7 | 0 | 1 | 0 | 0 | 0 |
| OxShockwaveRenderer | 6 | 0 | 1 | 0 | 0 | 0 |
| OxStanceAuraRenderer | 16 | 0 | 2 | 0 | 0 | 0 |
| FissureStunEffect | 0 | 0 | 0 | 0 | 0 | 0 |
| OxBraceEffect | 0 | 0 | 0 | 0 | 0 | 0 |
| Owner total | 47 | 0 | 6 | 0 | 0 | 0 |

KiRenderHelper references in all seven files: zero before and after
(the renderers no longer submit custom geometry at all). No new
private geometry helper, particle system, or texture style was added.
All emission goes through client/fx/MaterialEmitters with
DbaMaterialOptions materials, the Emitter accumulator, FxEnvelope
distance scaling, and per-second budgets.

## Materials per effect

Theme throughout: ground dust, stone debris, shock vapor, embers, with
hot plasma in the cracks, per the audit section.

- KingsSlam: DUST (0xA0825F) and VAPOR (0xD8C6A8) rolling across the
  impact disc, PLASMA (0xFF8C1F normal, 0xFFD24A flawless) low in the
  disc, EMBER, and BLOCK_CHUNKS of the actual ground block under the
  impact. The flawless variant adds PLASMA and VAPOR geyser emitters at
  8 seeded anchors in the bespoke geysers' 0.45 to 0.85 radius band.
  Growth curve (0.55 of life), fade out from 0.62 of life, radii, and
  lifetime are the bespoke values.
- OxShockwave: DUST, VAPOR, PLASMA (0xFF8C1F), EMBER, and BLOCK_CHUNKS
  of the actual ground block, all sampled on the crest ring as data
  with outward drift velocity. Wave lifetime is recomputed from the
  synced charge exactly as the bespoke build did, and the envelope
  fades across the final 30 percent.
- OxFissure: PLASMA (0xFF6A12) along the crack line and a PLASMA
  (0xFFD98A) white-hot core stream, DUST, EMBER, and occasional
  BLOCK_CHUNKS, sampled inside the same 4.2 by 2.0 footprint the entity
  damages, with the bespoke pulse and fade in and out.
- OxCharge: PLASMA (0xFF7A1A) motes on a gathering source with inward
  drift velocity toward the brace point, VAPOR and DUST across the
  shatter radius, EMBER tendrils that only emit at 25 percent charge
  and above (as before), and BLOCK_CHUNKS knocked loose. Radius growth
  and the 6 tick fade in are the bespoke values.
- OxStanceAura: PLASMA (0xFF7A1A, peak 0xFFEB59) and VAPOR gathered in
  the body volume, sparse PLASMA motes drifting at the 12 block
  boundary radius so the edge is sensed rather than drawn, DUST across
  the denial field, EMBER, and radial PLASMA bursts in the peak window
  only. Pulse, fade in over 8 ticks, and intensity ramp over 100 held
  ticks are the bespoke values.

No damage number, damage type, cooldown, Ki cost, timing, duration,
radius, hitbox, or synced data field changed anywhere in this family.

## Verification

Targeted javac, full tree compile with errors filtered to this owner's
files, JDK 25, run after the fixes above:

- 26.2 classpath (~/workspace/dba-audit-libs/cp262.txt): zero errors in
  all seven files.
- 26.3 classpath (~/workspace/dba-audit-libs/cp263.txt): zero errors in
  all seven files.
- Before the literal fix, the same runs showed exactly the five
  OxShockwaveRenderer lossy conversion errors on each classpath and
  nothing else in these files; after the fix, none.
- The tree as a whole still exits nonzero in both runs because other
  Phase 2 owners' conversions are in flight in the same working tree
  and the known baseline artifacts are present; the tree-wide kinds
  comparison against the pass baseline is the merge's job, as in prior
  passes.
- Geometry gates re-grepped after the fixes: BespokeGeometry 0,
  LayeredRender 0, addVertex 0, KiRenderHelper 0, in all seven files.
- Em dash scan of added lines in all seven files: zero.

Gradle cannot run in this VM (sandbox loopback restriction) and the
compile check is waived by Carter; no Gradle pass is claimed. The
in-game look is unverified until Carter's PC test, as always.

## Honest worse check

What got better: nothing in this family reads as a shape anymore. The
wave is a rolling crest of dust, vapor, embers, and real chunks of the
block it is crossing; the fissure glows from material inside the crack
instead of sitting on it as a mesh; the charge visibly pulls material
inward instead of spinning rings; the slam's flawless geysers are
plasma and vapor columns instead of cylinders.

What is lost, stated plainly:

- The stance's titan avatar is gone entirely: no pauldrons, horns,
  aegis plates, or heat shells. That is the standard working as
  intended (they were armor-shaped meshes around the player), but the
  stance now reads as pressure and gathered material around the caster
  rather than a colossal figure standing over him. This is the biggest
  identity trade in the family.
- The shockwave's rising rock crags and teeth and the slam's monoliths
  no longer exist as silhouettes. The crest is dust and debris, lower
  and less monumental than the bespoke rock ring, though the crest
  height now drives the dust column so a full charge wave still towers
  more than a weak one.
- The fissure's raised basalt teeth are gone; the crack is a glowing
  material line in the ground plane, flatter than the bespoke trench.

No piece was reverted: every conversion reads as its material at the
bespoke positions, timings, and colors, and a missing silhouette is
the rule of this pass, not a defect. If Carter judges the stance's
lost titan read or the wave's lost crags worse in game, those two
renderers are the ones to revisit first.

## Change requests

None new. The bespoke report's two toolkit requests (an upright shard
builder, a shell band builder) are moot under this standard, since the
geometry toolkit is deprecated for effect visuals; they are not
revived here.
