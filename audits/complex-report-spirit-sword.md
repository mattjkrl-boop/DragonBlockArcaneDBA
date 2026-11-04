# Complex Effects Redo, Phase 2 Report: Spirit Sword

Owner slot: Spirit Sword (theme: soul plasma, spirit wisps, vapor).
Base: HEAD 9178eac. Files edited, and only these three:
SpiritCannonBeamRenderer.java, SpiritCleaveRenderer.java,
SpiritImpaleRenderer.java, all under
src/client/java/com/dragonblockarcanedba/client/render/.
No commits. No entity, damage, cooldown, cost, timing, mechanic, or
synced field changes. Every pre-existing material emission line is kept
verbatim as the filler layer; the only non-comment change to an old
line is splitting the distance scale product into a named local in the
cleave and impale renderers, with identical values.

## SpiritCannonBeam (class L)

Pieces layer, one submitEnergy call with the tendril canvas, so every
energy piece carries the shader treatment:

- Tendril core along the beam: the synced beam path is chained into
  two, three, or four segments by length (segment endpoints shared, so
  the core is continuous), with two strands per segment. The main
  strand is white at the root to cyan at the tip, width 0.60 times
  pulse and first person scale, peak alpha 0.95. The secondary strand
  is cyan to gold, width 0.30, peak alpha 0.90, offset slightly off the
  main strand, with alternating seeded bow on each segment control
  point so the curl stays tight over a long beam.
- TendrilFan impact lashes at the terminus: six tendrils (the fan
  maximum in the standard) at 58 degrees spread, length 2.6, cyan root
  to white tip, peak alpha 0.92.

Vertex budget at distScale 1.0: core at most 8 tendrils times 384
equals 3072, fan 6 times 384 equals 2304, total 5376 of the 6000
class L budget. Two builder uses (tendril and tendrilFan), fan at
full count, core at full 24 sample tendrils.

Complex particle layer: SPARK_STREAK at the terminus through
MaterialEmitters.Preset.fixed, velocity along the beam direction,
rate 32 per second with a 40 budget, inside the continuous limit.
The existing SPARK preset at the terminus stays as filler beside it.

Filler kept: soul white core, soul cyan sheath, vapor sheath, gold
soul gather, muzzle soul, terminus plasma and vapor, terminus SPARK,
all at their original rates, sizes, colors, and seeds. PLASMA is one
terminus filler stream among eight streams and is not the dominant
layer; the tendril core is the beam body.

## SpiritCleave (class M)

Pieces layer, one submitEnergy call with the tendril canvas:

- Tendril body: a thick strand bowed sideways off the flight line
  along the traveled path the emission already samples, cyan root to
  white tip, width 0.52, peak alpha 0.95.
- A second thinner strand on the opposite bow, gold root to cyan tip,
  width 0.26, peak alpha 0.90, as the torn wake edge.

Vertex budget: 2 tendrils times 384 equals 768 of the 2500 class M
budget.

Complex particle layer: WISP_CURL through Preset.fixed along the
traveled path, rate 26 per second with a 40 budget.

Filler kept: soul white volume, soul cyan path, vapor gold path,
SPARK path, and the opening tip flash soul burst, all unchanged.

## SpiritImpale (class M)

Pieces layer, through the normal depth tested paths only (submitSolid
on the plain ki type, submitEnergy on the shader type whose depth
state is test on, write off, less than or equal), so wall occlusion
behavior is the same path the rest of the mod geometry uses and does
not regress:

- Shard mesh as the spirit spike body, through submitSolid: one
  central shard along the pillar space the emission already rises
  through, length equal to the slam driven pillar height, girth equal
  to the pulsing pillar radius, soul gradient cyan root to white tip,
  peak alpha 0.96 (literal solid opacity times the lifecycle
  envelope). Four smaller seeded ring shards stand on the sword ring
  radius the plasma stream already uses, tilted inward, gold root to
  white tip, peak alpha 0.94. Ring angles come from a
  ComplexGeometry.Rng seeded by the entity UUID seed, so every client
  draws the same ring.
- Tendril wake, through submitEnergy with the tendril canvas: two
  strands starting on opposite sides of the base radius, curling
  inward and up to near the spike tip, cyan root to white tip, width
  0.30 times scale, peak alpha 0.90.

Vertex budget at distScale 1.0: five shards times 288 equals 1440,
plus two tendrils times 384 equals 768, total 2208 of the 2500
class M budget.

Complex particle layer: WISP_CURL through Preset.fixed rising through
the pillar volume, rate 24 per second with a 40 budget, gated to the
same slam height window as the pieces.

Filler kept: soul ground area, vapor gold ground area, soul white
pillar volume, soul cyan pillar volume, plasma ring volume, vapor
perimeter volume, all unchanged. PLASMA is one of six filler streams
and is not the dominant layer.

Untouched as required: the entity and its server side DebrisSpawner
field, and the d74f13a destruction wiring, including the permanent
crystal conversion. The renderer shards are transient pieces on the
lifecycle envelope and are not the permanent crystal.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, private output tags
/tmp/dba-spirit-sword-262 and /tmp/dba-spirit-sword-263:

- 26.2 classpath (cp262.txt): 4 errors, all baseline and outside this
  slot (DbaItems CreativeModeTab Output access, two; DbaMenus MenuType
  access, two). Zero errors in the three owned files, and all three
  owned class files were produced.
- 26.3 classpath (cp263.txt): 10 errors, all baseline and outside
  this slot (the same four as 26.2, plus DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, and PatrolRobotEntity
  vanilla field and method changes). Zero errors in the three owned
  files, and all three owned class files were produced.
- Zero drawColoredBox, drawCylinder, drawDisc, drawSphere, or
  drawHelix calls in the owned files. Zero em dashes in the owned
  files. git diff --check clean on the owned files. The diff removes
  no emission preset, rate, color, or seed; themes (white, cyan,
  gold soul colors in each file) are identical to HEAD.
- Peak alphas on shader pieces are 0.90 to 0.95, and 0.94 to 0.96 on
  the solid shards. No shader piece is fed a low constant alpha to
  fake faintness; the thinner strands read fainter through width and
  color, not alpha.

## Foundation change requests

No private toolkit was built and nothing here blocks on these. Two
gaps worth a shared toolkit pass at the merge:

1. ComplexGeometry.tendril takes its sample count only from
   distScale (24 at full detail), with no length awareness. Over a
   segment longer than about eight blocks the bezier curl stretches
   and the pinch windows spread out. Beam owners currently work
   around this by chaining tendrils by hand, as this slot does. A
   shared tendrilChain helper, or a length based sample count inside
   tendril, would serve the beam, spiral beam, and cannon slots at
   once.
2. ComplexGeometry.shard has no distScale parameter, so a solid
   shard costs a fixed 288 vertices at any distance and owners
   cannot scale shard counts down for far viewers the way the
   energy builders scale. A distScale argument on shard and
   shardBurst, thinning ring interpolation steps at range, would
   help every shard body slot.

## Honest worse check

SpiritCannonBeam: the risk is the core reading wavier than a clean
beam line, or the seeded pinch windows reading as breaks in the
core. The bow amplitude is small (0.22 blocks at full pulse), the
segment endpoints are shared so the core never gaps, and the
straight soul sheath emission that defined the beam before is still
present around it, so the beam axis still reads straight while the
core gains structure. The terminus fan adds lashes where there was
only glow before; at 2.6 blocks against a beam up to 32 blocks it
stays a contact detail. Verdict: not worse than the particles only
version; it is the first version with a visible beam body.

SpiritCleave: the honest risk in this slot. The tendril body is
bowed along the flight path, so it can read as a curved energy
strand trailing the projectile rather than as a blade shaped
crescent in a vertical plane. The renderer has no synced roll or
arc plane data beyond yaw and pitch, and inventing one would change
the synced data, which is out of scope. Within the standard, the
bowed strand plus the kept volume emission reads as a structured
cleave wave with a torn gold wake edge, which is strictly more
shaped than the previous glow only volume. It does not read worse
than the pre-redo version, but the merge should know the arc plane
is inferred from the flight line, not authored.

SpiritImpale: the central shard is a solid faceted spike, and a
solid could in principle read as a physical crystal rather than
spirit energy. Three things keep it on theme: the cyan to white
soul gradient with facet steps, the two shader tendrils curling up
it dissolving on the envelope tail, and the rising soul and vapor
filler still filling the pillar volume around it. It also gives the
slam a body the particles only version never had, at the exact
height and radius the emission already described. One deliberate
trade: the ring carries four shards, not six, because five shards
plus the wake already use 2208 of the 2500 vertex budget, and the
six impact points were never discrete objects in the particles only
version (they were a distributed plasma volume, which stays).
Verdict: not worse; the spike is the effect body the audit asked
for.
