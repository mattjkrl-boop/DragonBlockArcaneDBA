# Complex Effects Redo, Phase 2 Report: Blaster Gun

Owner slot: Blaster Gun (theme: ki plasma, steel sparks, smoke, vapor).
Base: HEAD 9178eac. Files edited, and only these three:
BlasterBoltRenderer.java, ErasureCannonBeamRenderer.java,
ErasureChargeOrbRenderer.java, all under
src/client/java/com/dragonblockarcanedba/client/render/.
No commits. No entity, damage, cooldown, cost, timing, mechanic, or
synced field changes. The beam line-hit behavior, the bolt block
collision, the d74f13a destruction wiring (beam trench fissures and
endpoint crater), and the beam entity's server side DebrisSpawner
field all stay exactly as wired. Every pre-existing material emission
line is kept verbatim as the filler layer; the diff removes only the
old no-shapes header comment blocks, which were rewritten to describe
the layered composition.

## BlasterBolt (class M)

Pieces layer, one submitEnergy call with the tendril canvas, so both
energy pieces carry the shader treatment:

- Short tendril wake: a main strand from the tail end forward to the
  nose, root in the synced heat palette color (the same argb the
  plasma body uses, so overcharged red-pink, hot orange, and warm
  yellow all carry through) to a near-white 0xFFF7D1 tip, width
  max(0.16, drawRadius times 1.05), peak alpha 0.95.
- A second thinner torn edge strand offset to the side and below,
  width 0.55 of the main strand, peak alpha 0.88, same gradient.
  Faintness on the second strand comes from width, not from feeding
  the shader a low alpha.
- Both strands scale from the same drawHead, drawTail, and drawRadius
  the emission already collapses on impact, so the wake dies with the
  bolt instead of outliving it.

Vertex budget at distScale 1.0: 2 tendrils times 384 equals 768 of
the 2500 class M budget.

Complex particle layer: SPARK_STREAK at impact only, through
MaterialEmitters.burst with Preset.fixed, fired once on the frame the
synced impact age first turns non-negative (tracked per entity in the
existing Emission holder, so a bolt first seen already impacting still
bursts once). Count 18, or 26 when overcharged, inside the class M
burst budget of 30. Source is a small sphere volume at the nose.

Filler kept: plasma body path, plasma white-hot core path, plasma
nose cluster, SPARK fixed at the nose, and vapor tail path, all at
their original rates, sizes, colors, and seeds. PLASMA is the filler
around the wake strands, not the bolt body; the tendril pair is the
structured silhouette the flight segment now carries.

## ErasureCannonBeam (class L)

Pieces layer, two submitEnergy calls (one with the tendril canvas
for the core and fan, one on the white canvas for the crack fan), so
every energy piece carries the shader treatment. Three builder types
(tendril, tendrilFan, crackFan) at class L:

- Tendril core, thick and white-hot at the root: the synced beam path
  is chained into two, three, or four segments by span, segment
  endpoints shared so the core is continuous. The main strand is
  width baseRadius times 1.20, peak alpha 0.95, with its gradient
  mixed per segment from 0xFFFFFF at the muzzle to 0x00EBFF at the
  terminus, so white-hot lives at the root and the body cyan takes
  over along the length. A secondary strand, width baseRadius times
  0.55, peak alpha 0.90, offset off the main strand on the synced
  right and up basis, chains one or two segments in 0x00EBFF grading
  to the existing deep body color 0x008CD9, with its bow opposed to
  the main strand.
- TendrilFan terminus lashes: six tendrils (the fan maximum) at 62
  degrees spread along the beam direction, length plumeRadius times
  1.90, white root to the plume color 0x00F2FF at the tips, peak
  alpha 0.92.
- CrackFan at ground contact: a short client side probe steps down
  from the terminus in 0.25 steps to 1.25 blocks for the first
  non-air block, and only then lays a crack fan on that block's top
  surface, centered under the terminus, radius plumeRadius times
  1.35, hot 0xFFFFFF to cool 0x00EBFF, peak alpha 0.90. A beam ending
  in air at full range or on a high wall draws no fan. This is a
  presentation probe only; it reads block state and changes no
  mechanic, and the trench and crater wiring is untouched.

Vertex budget at distScale 1.0: main chain at most 4 tendrils (1536),
secondary chain at most 2 (768), fan 6 (2304), crack fan at most
about 900 (9 to 11 cracks plus forks, per the toolkit counts), for
about 5508 of the 6000 class L budget.

Complex particle layer: SPARK_STREAK heavy at the terminus through
Preset.fixed at the plume source, rate 38 per second with a 48
budget, just under the 40 per second continuous limit for one
emitter. The existing SPARK preset at the terminus stays as filler
beside it.

Filler kept: white, cyan, and deep blue plasma path runs, vapor path
run, the three focusing station plasma clusters, the muzzle cluster,
and the terminus plume of cyan plasma, white plasma, smoke, and
vapor, all at their original rates, sizes, colors, and seeds.
PLASMA is no longer the beam: the chained tendril core carries the
beam read and the emission is glow and atmosphere around it.

## ErasureChargeOrb (class M)

Pieces layer, one submitEnergy call with the tendril canvas:

- TendrilFan lashes orbiting the orb: five tendrils rooted at the
  orb center (zero in third person, the synced hand offset in first
  person, both in entity local space), spread 128 degrees around a
  fan axis built from the synced right and up basis rotating with
  age, so the lash cone sweeps around the aim line instead of
  sitting as a fixed fan. Length is orbRadius times (2.0 plus
  chargeEase times 0.80), width max(0.09, orbRadius times 0.30),
  white-hot 0xFFFFFF root to the body cyan 0x37E6FF at the tips,
  peak alpha 0.92. The lashes grow with the same chargeEase curve
  that grows the orb.

Vertex budget at distScale 1.0: 5 tendrils times 384 equals 1920 of
the 2500 class M budget.

Complex particle layer: SPARK_STREAK through Preset.fixed in the
body volume at 18 plus charge times 18 per second (36 at full
charge), and WISP_CURL through Preset.fixed at the boundary volume
at 14 plus charge times 12 per second (26 at full charge). Both sit
inside the 40 per second continuous budget for one emitter.

Filler kept: white plasma core volume, cyan plasma body volume, the
hot red-shifted plasma layer past 0.75 charge, vapor boundary
volume, the inward-streaming intake plasma, the ember preset, and
the SPARK fixed preset, all unchanged. As the task ordered, the orb
stays a churning plasma cluster as filler, and the orbiting lashes
are its new structured body, so PLASMA is not the sole or dominant
read even though the cluster itself remains.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling only the three
owned files, private output tags /tmp/dbg262 and /tmp/dbg263 for
this slot. Full logs were read:

- 26.2 classpath (cp262.txt): 4 errors, all baseline and outside this
  slot (DbaItems CreativeModeTab Output access, two; DbaMenus
  MenuType access, two). Zero errors in the three owned files, and
  all three owned class files were produced.
- 26.3 classpath (cp263.txt): 10 errors, all baseline and outside
  this slot (the same four as 26.2, plus DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, and PatrolRobotEntity
  vanilla field and method changes). Zero errors in the three owned
  files, and all three owned class files were produced.
- Zero drawColoredBox, drawCylinder, drawDisc, drawSphere, or
  drawHelix calls in the owned files. Zero em dashes in the owned
  files. git diff --check clean on the owned files. The diff removes
  no emission preset, rate, color, or seed; themes (heat palette for
  the bolt; white, 0x00EBFF, 0x008CD9, 0x59F2FF, and 0x00F2FF for the
  beam; white, 0x37E6FF, and 0x59F2FF for the orb) are identical to
  HEAD.
- Peak alphas on shader pieces are 0.88 to 0.95. No shader piece is
  fed a low constant alpha to fake faintness; thinner strands read
  fainter through width and color.

## Foundation change requests

No private toolkit was built and nothing here blocks on these. Two
gaps worth a shared toolkit pass at the merge:

1. ComplexGeometry.crackFan always draws around its local origin in
   the XZ plane, with no offset or contact-normal parameter. Ground
   or contact placement therefore needs a pose push and translate in
   every owner, and a contact on a non-horizontal face cannot be
   expressed at all. A crackFan variant taking a center point and a
   plane normal would serve the beam, ki beam, and every impact slot
   at once. This slot works around it with the ground probe and the
   pose translate described above.
2. ComplexGeometry.tendril takes its sample count only from
   distScale, with no length awareness, so beam owners chain tendrils
   by hand to keep the curl and pinch windows tight over long spans,
   and must hand-mix the gradient per segment to keep a root to tip
   color journey continuous across the chain. A shared tendrilChain
   helper that takes a start, an end, and endpoint colors would
   remove that duplication from this slot and the spirit and ki beam
   slots at once.

## Honest worse check

BlasterBolt: the risk is the wake reading as two wavy ribbons
trailing a projectile that used to read as a clean glowing shot.
The strands run inside the same tail to head segment the dense
plasma already filled, at widths close to the old bolt radius, and
the plasma cluster is still there around them, so the shot keeps its
glow body and gains a torn spine. On impact the strands collapse on
the same math as the emission. Verdict: not worse than the particles
only version.

ErasureCannonBeam: two honest risks. First, the chained core bows
sideways by baseRadius times 0.35 per segment, so the beam axis can
read slightly wavy; the bow is small against a beam up to 48 blocks,
segment endpoints are shared so the core never gaps, and the
straight plasma path runs that defined the beam before are still
present around it. Second, the crack fan is gated on a ground probe,
so a beam fired level into a distant wall gets terminus lashes and
streaks but no crack web; that is deliberate, because the audit
names the fan for ground contact and a horizontal web floating on a
wall face would read worse than none. Verdict: not worse; the beam
has a visible white-hot core body for the first time, and the fan
appears exactly where the trench and crater systems already say the
beam meets ground.

ErasureChargeOrb: the risk is the orbiting fan reading as a spiky
starburst over the orb at low charge, when the orb radius is small
and the lash length is about twice it. Three things hold it back:
the fan envelope follows the same 0.25 plus charge curve as the
emission, so lashes are weakest exactly when the orb is smallest;
the 128 degree spread around a rotating axis keeps strands wrapped
around the volume rather than spiking in one direction; and the
churning cluster, intake stream, and boundary vapor all stay, so
the lashes read as energy lashing off a contained charge, which is
the charge-up read. It does not read worse than the pre-redo version,
but the merge should know the orbit is driven by age and the synced
right and up basis, not by a synced orbit field, because no such
field exists and adding one was out of scope.
