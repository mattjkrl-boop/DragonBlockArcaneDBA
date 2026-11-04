# Complex Effects Redo, Phase 2 Report: Hollow's Edge

Owner slot: Hollow's Edge (theme: soul wisps, void plasma, darkness smoke).
Files edited, and only these five:
HollowAfterimageRenderer.java, HollowChargeRenderer.java,
HollowRushTrailRenderer.java, VoidRiftRenderer.java, and
VoidSlashRenderer.java, all under src/client/java/com/dragonblockarcanedba/client/render/.
No commits. Themes, colors, damage numbers, cooldowns, costs, timing,
mechanics, and synced data fields are identical to HEAD 9178eac. Every
effect keeps its existing material emission as the filler layer, keeps
its per-entity Streams keyed by the entity UUID seed, and gains a pieces
layer plus a complex particle layer. No plain primitives anywhere: zero
drawColoredBox, drawCylinder, drawDisc, drawSphere, and drawHelix call
sites in the five files.

Vertex accounting used throughout: one ComplexGeometry tendril at
distScale 1.0 draws 24 spine segments times 2 crossed quads times 8
vertices, 384 vertices. One shard mesh draws 288 vertices. Class M
budget is 2500 vertices per submit.

## HollowAfterimage (class M)

Pieces layer, through ComplexGeometry.submitVeil with
ComplexTextures.TENDRIL (alpha shader, as the standard calls for veil
tendrils on this effect):
- tendrilFan, 3 strands rising from the feet, dir up, 20 degree spread,
  length scaled by the existing dissolve spread, graded soul cyan
  0x1AD9FF to the caster's synced hair color.
- tendril, 2 strands curling up the sides of the silhouette from hip
  height to above the head, graded from the synced skin tinted body
  color (the same bodyArgb the darkness stream already derives from the
  synced skin color) to soul cyan.
- tendril, 1 short strand across the hair cap, graded from the synced
  hair color to pale 0xD9FBFF.
Total 6 tendrils, 2304 vertices at full detail, inside budget. Alpha is
an honest veil peak of 0.88 and the fade rides the envelope argument on
the existing 100 tick curve (1 minus progress squared, times the
existing pulse), so the strands dissolve only in the tail. The
corrected lifetime behavior is untouched: emission and pieces both run
to tick 100, and the render state still carries the synced skin and
hair colors from the entity.

Complex particle layer: WISP_CURL through MaterialEmitters.Preset.fixed,
a new Streams emitter sampling the same body volume as the soul stream,
36 per second rate with a 40 budget, dense as the audit asks and inside
the 40 per second continuous cap. Ticked at env times 0.9 with the soul
stream.

## HollowCharge (class M)

Pieces layer, through ComplexGeometry.submitEnergy with
ComplexTextures.TENDRIL:
- tendrilFan inward: 5 calls with count 1, one per strand, each rooted
  on the gathering ring at the existing baseRadius times 0.95 and aimed
  at the core at local (0, 0.9, 0), so the fan builder's own curled
  control point bends each strand as it converges. Graded gather violet
  0xA60DF2 to core cyan 0x26E6FF, width growing 0.20 to 0.30 with the
  charge ratio, envelope 0.30 plus 0.70 times charge (the same curve the
  gather stream ticks on), alpha 0.92. The ring rotates slowly with age
  (0.06 rad per tick) to match the swirl stream. 5 tendrils, 1920
  vertices, inside budget.

Complex particle layer: WISP_CURL through Preset.fixed, sampling the
same inward gather path as the plasma stream with the same toward-core
velocity, 30 per second rate, 36 budget, ticked on the gather curve.

## HollowRushTrail (class M)

Pieces layer, through ComplexGeometry.submitEnergy with
ComplexTextures.TENDRIL, in local space from the trail origin along the
synced dash direction:
- Normal dashes: tendril core strand from the origin to the full trail
  length, width 0.30 times the width scale, graded core cyan 0x26E6FF
  to stream violet 0xBF1AFF, plus one thinner offset strand (0.16) graded
  0x730DF2 to 0x26F2FF. 768 vertices.
- Third dash: the slash arc is drawn as tendrils at the same center,
  radius, and closing curve the arc emission samples. The 225 degree
  sweep is split into two tendrils per arc meeting at the arc mid, with
  control points pushed out to the midpoint matched distance (radius
  times about 1.44), so the spines hug the arc instead of cutting
  across it. One white to violet arc at width 0.34 and one cyan inner
  arc at 0.18, four tendrils total, 1536 vertices.
Alpha 0.92, envelope is the trail's existing eased fade alpha. First
person narrowing applies to the pieces through the same widthScale the
emission uses.

Complex particle layer: WISP_CURL through Preset.fixed, two Streams
emitters sharing the existing sources: one shed along the helical
stream paths on normal dashes, one across the arc on the third dash,
30 per second rate and 36 budget each, only one of them ticking per
dash type.

## VoidRift (class M)

Pieces layer, drawn in the billboard plane: the pose is rotated by the
conjugate camera orientation, the same basis the emission streams are
built from (the pattern SkyCracksRenderer already uses), so local X and
Y are the tear's right and up.
- Solid pieces through ComplexGeometry.submitSolid: shardBurst called
  at 5 rim anchors on the rim ellipse (the same bellyX times bandScale
  times 1.16 and halfH times bandScale times 1.05 the rim stream uses),
  count 1 per anchor, axis radial outward, unit radius times 0.38,
  graded dark void 0x2E004D to soul violet 0x9905F2, alpha 0.95. 5
  shards, 1440 vertices.
- Energy pieces through ComplexGeometry.submitEnergy with
  ComplexTextures.TENDRIL: 2 tendrils streaming inward, from the top and
  bottom rim to the throat center, graded 0x9905F2 to pale violet
  0xD9B8FF, alpha 0.90. 768 vertices.
Total 2208 vertices, inside budget. The pieces follow the implosion
choreography windows already in the file: the shards use the body
collapse window (gone by implodeT 0.70, matching the shard emission
partner) and the tendrils use the rim window (fading from 0.50 to
0.86, matching rimA), with the fade-in envelope unchanged when not
imploding.

Complex particle layer: ASH_FLAKE through Preset.fixed, sampling the
rim ellipse with a slow tangential drift, 26 per second rate, 34
budget, ticked at rimA times 0.8 alongside the rim stream.

## VoidSlash (class M)

Pieces layer, through ComplexGeometry.submitEnergy with
ComplexTextures.TENDRIL, in the synced tilt plane the emission already
uses:
- The crescent arc is drawn as tendrils in two arcs: a core arc at
  radius times 1.04, width 0.17 times radius, graded cyan 0x26EBFF to
  violet 0x9905F2, and a cut arc at radius times 1.16 set slightly
  behind the cut plane, width 0.10 times radius, graded 0x9905F2 to
  0x26EBFF. Each arc is split into two tendrils meeting at the arc mid
  with control points at the midpoint matched distance (radius times
  about 1.22 for this 154 degree sweep), so the spines sit on the
  crescent the emission traces instead of sagging inside it. Four
  tendrils, 1536 vertices, inside budget. Alpha 0.94, envelope is the
  existing open and close fade.

Complex particle layer: WISP_CURL shed through Preset.fixed along the
cut arc (the Streams arcPoint sampler at radius scale 1.10), 30 per
second rate, 36 budget, ticked at env times 0.8.

## Verification

Targeted javac, JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
-sourcepath src/main/java:src/client/java, compiling the five owned
files, output to private /tmp tags (hollows-edge, hollows-edge2):
- cp262.txt classpath: zero errors in owned files. Closure errors
  present are the known baseline kinds in files this slot does not own
  (DbaItems CreativeModeTab Output access, DbaMenus MenuType access).
- cp263.txt classpath: zero errors in owned files. Closure errors are
  baseline kinds elsewhere (the same DbaItems and DbaMenus access
  errors, plus cannot find symbol and swing signature errors in common
  entity files).
- All five owned classes and their Streams and state classes were
  emitted on both classpaths.
- git diff --check on the five files: clean. Added lines em dash and
  en dash scan: zero. Forbidden primitive call scan: zero.

## Foundation change requests

No blockers; three small shared toolkit gaps worth a merge decision:
1. tendrilFan fans outward from one origin along one direction. Charge
   style effects need the convergent form (origins on a ring, strands
   aimed at a shared target). HollowCharge works around it with five
   count 1 tendrilFan calls in one submit, which is correct but clumsy.
   A tendrilFanTo builder (ring origins, common target, per strand
   curl) would serve every charge and gather effect in the audit.
2. shardBurst places every shard at one origin in one cone, so a ring
   or fan arrangement needs one call per anchor at count 1 (VoidRift
   does this, five calls). A placement variant that takes per shard
   origins, or a ring mode, would simplify rift and tear effects.
3. There is no shared billboard pose helper. VoidRift hand rolls the
   conjugate camera orientation pose, as SkyCracksRenderer already did.
   A small shared helper would keep future billboard plane pieces
   consistent.

## Honest worse check

Could any of these read worse than the particles only version or the
pre-redo version, and why or why not:

- HollowAfterimage: the main risk in this slot. Veil strands around a
  particle ghost can read as flames licking a silhouette if they splay
  too wide. Mitigations in the build: the strands are alpha veil pieces,
  not additive energy; the side strands stay at body offsets (0.34 to
  0.50 lateral) and span the body's own height; the rising fan spreads
  only 20 degrees; and all colors come from the ghost's own synced skin,
  hair, and soul palette. The particle body remains the dominant read
  and the strands are structure inside it, which is exactly what the
  particles only version lacked. I do not judge this worse, but it is
  the one to check first in game: if the fan reads as fire, narrowing
  the fan spread and dropping its width is a one line change.
- HollowCharge: inward strands ending at one point can read as spokes.
  The fan builder's curled control points and torn pinches keep them
  from being straight spokes, the ring rotation is slow, and the plasma
  gather still streams between them. Better than the particles only
  version, which had no convergence structure at all.
- HollowRushTrail: a single strand on a straight path risks reading as
  a rope. The builder's mid-spine curl, pinch windows, and twist keep
  it a torn strand rather than a tube, and it sits inside the corridor
  volume the emission fills, at about a third of the corridor width.
  The arc version was the real risk (a bezier cutting inside the arc
  would have read as a chord, clearly worse), which is why the sweep is
  split with midpoint matched controls. Not worse.
- VoidRift: solid shards at the rim could fight the billboard read if
  they sat off the rim ellipse; they use the rim stream's own ellipse
  math and collapse on the shard window, so they read as the lip
  crystals the old geometry had, at complex piece quality. The two
  inward tendrils are deliberately plain paths (rim to throat) because
  the accretion read wants streaming, not lashing. Not worse.
- VoidSlash: the doubled arc (core plus cut strand) could double-image
  if the two arcs separated too far; they sit at 1.04 and 1.16 radius
  scales, the same separation as the core and cut emission layers they
  replace as the body, so the read is one thick torn crescent. Not
  worse.

Nothing in this slot is shipped silently against my own judgment: the
afterimage fan is flagged above as the single piece to eyeball in game
first, with its fix named.
