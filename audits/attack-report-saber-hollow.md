# Attack Upgrade Pass, Phase 2 owner report: Saber and Hollow's Edge

Owner slot: Saber (SaberSlashRenderer, SaberDimensionalLineSlashRenderer,
SaberVoidTearRenderer) and Hollow's Edge (VoidSlashRenderer,
HollowRushTrailRenderer, HollowAfterimageRenderer, VoidRiftRenderer).
Specs: audits/attack-complexity-audit.md sections 3 and 9, plus
audits/attack-foundation-addendum.md. HEAD ccfebf2, no commits made.
Presentation only: no damage, cooldown, cost, timing, mechanic, or
synced field changed anywhere in this slot. Saber's terrain-free rule
holds absolutely: no ground pieces and no debris were added to any
Saber renderer.

Quality bar applied (Carter, Nov 4): pieces were judged as form first,
the way the mod's selectable characters and forms are authored, and
against a live-action read. Where a specced builder's rolled form read
toy-like, the form was fixed at the same count instead of shipping the
roll. The one place that happened is the Saber Slash steel edge, below.

## SaberSlashRenderer

Before: 3 tendrils on the energy submit (near 1150 vertices), no solid
pieces at all.
After: 5 tendrils (near 1920 vertices in the same submit, inside the
class M budget of 2500 per submit), plus 5 solid steel slivers in their
own world-lit submit (1440 vertices).

- Second tendril pair: two strands that start at opposite vertical
  offsets from where they end (high to low and low to high), with
  shallower control bulges than the main arc, so they cross the first
  three strands diagonally at seeded offsets and the cut reads woven
  instead of three parallel ribbons.
- Solid edge, form fix: the audit names a shardBurst of 4 to 6 thin
  blade shards. The burst builder rolls girth at 0.30 to 0.44 of length
  across mixed chip and chunk classes, which reads as a cone of
  pebbles, not a blade edge. The shipped form keeps the count (5) and
  the specced path (submitSolidLit, pale steel root 0x8A93A6 to tip
  0xF2F7FF) but authors the pieces as individual shard calls placed on
  the arc itself: each sliver sits on the parabolic arc the tendrils
  and emission already trace, points along the arc tangent, runs a
  0.15 girth ratio (a blade, not a chunk), and is longest at the arc
  middle where the cut is deepest. They exist only inside the existing
  strike window (the same strike(progress) envelope the impact flash
  uses), scaled by that envelope, so they fly the swing and are gone
  with it. No new timing was invented.

## SaberDimensionalLineSlashRenderer

Before: 2 tendrils (near 770 vertices), no crack layer.
After: 4 tendrils (near 1536 vertices), plus 1 crackFan in its own
submit (near 900 vertices). Per-effect total near 2436, inside the
class M budget of 2500.

- Tendrils 2 to 4 with staggered phase: the added pair runs staggered
  spans (one starts 12 percent in and ends 10 percent early, the other
  shifted the opposite way), offset below and to the side of the line,
  with distinct seeds, so their seeded noise phases and bows differ
  from the original pair instead of echoing it.
- crackFan at the midpoint: the fan walks its local XZ plane, so the
  submit pose carries a basis matrix mapping local X to the synced
  line direction, local Z to the slash plane's up, and local Y (the
  fan's flat normal) to the side axis. The crack therefore webs inside
  the slash plane at the line's midpoint, in the air, void colors
  (0xBFEFFF hot to 0x3D1A78 cool) through submitEnergy. Radius is
  1.05 times the slash scale, so the branches stay short.

Change request 1 (foundation): crackFan takes no branch count. The
spec asked for 4 short branches; the builder internally draws 9 to 11
branches at any radius, so the shipped web has more, shorter branches
than specced. A branch count parameter on crackFan would let the merge
trim it to 4 if the denser web reads busy in game.

## SaberVoidTearRenderer

Before: a flat fan of 5 solid shards (length near 0.55 of scale,
ringing the slit), 2 tendrils, mouth bursts of glass dust 14, plasma
10, WISP_CURL 10, ASH_FLAKE 8.
After: 3 composed solid shards, 2 tendrils unchanged, mouth bursts
with ASH_FLAKE doubled to 16 and a new SHARD_CHIP burst at 16.

- Shard composition per spec (1 to 3): one large shard (length near
  1.02 of scale, about double the old fan shards) crowns the tear
  mouth at the top of the ring, and two smaller shards (0.55 of scale)
  flank it at plus and minus 0.96 radians with seeded jitter. Solid
  vertices drop 1440 to 864; total pieces near 2210 to near 1630,
  inside budget.
- Honest discrepancy, flagged for the merge: the audit's Today line
  counted this renderer as "shard plus tendril" (one shard), but the
  code at ccfebf2 drew a 5 shard fan. The spec's 1 to 3 was implemented
  exactly as written, so the shard count went 5 to 3 while each
  remaining shard got larger and deliberately placed, and the mouth
  sprite density doubled. If the merge prefers the fan restored at 5
  with the large crown added on top, that is a small follow-up; the
  composition as specced is what shipped here.
- Sprite doubling: the ASH_FLAKE impact burst doubled (base count 8
  to 16) and a SHARD_CHIP burst was added at the mouth at the same
  doubled density (base count 16); no SHARD_CHIP layer existed in this
  renderer before, so it was added at the doubled value rather than
  doubled from zero. The continuous ASH_FLAKE emitter rate was NOT
  doubled: the complex standard caps continuous complex sprite
  emission at 40 per second per emitter, and the existing 28 per
  second stream doubling to 56 would break that cap. The doubling
  landed on the one-shot mouth bursts, which are not rate capped.

## VoidSlashRenderer (Hollow's Edge)

Before: 2 tendril arcs (4 tendril calls, 1536 vertices), no solid
pieces. The audit rated this one THIN-leaning.
After: 4 tendril arcs (8 tendril calls), plus a shardBurst of 4 void
shards in their own submit (1152 vertices).

- Crossing pair: the two new arcs run on a frame rolled about the
  forward axis by 0.18 radians (about ten degrees), at radius factors
  1.10 and 0.97, so their strands literally cross the original pair
  along the sweep instead of running parallel to it. Colors stay in
  the slash's own palette (violet 0x9905F2 to pale 0xD9B8FF, cyan
  0x26EBFF to white).
- Void shards: shardBurst of 4 along the slash plane (cone axis on
  the plane's up axis, 58 degree spread, unit 0.42 of the slash
  radius), root 0x2E004D to tip 0x9905F2, the same near-black violet as
  the Void Rift fragment ring. These are energy material (torn void),
  so they go through submitSolid on the emissive path per the audit,
  not the lit path. Their envelope is fade times open, so they burst
  as the slash opens, at the swing.

Budget note, stated plainly: the energy submit is now near 3072
vertices at full spine samples, over the class M budget of 2500 at
distScale 1.0 (point blank). Spine samples distance scale down
(FxEnvelope.segments), so the submit sits inside budget beyond close
range. The audit ordered both the crossing pair and the shards for
this THIN-leaning slash; the overage is the cost, recorded here for
the merge's realism and budget review.

## HollowRushTrailRenderer

Before: normal dash 2 tendrils (768 vertices), third dash 4 tendrils
(1536 vertices), WISP_CURL shed layers only.
After: normal dash 4 tendrils (1536), third dash 6 tendrils (2304),
plus 2 SHARD_CHIP shed emitters. All inside the class M budget.

- SHARD_CHIP shed layer: two new emitters mirror the wisp pair on the
  same sources (chipStream on the helical stream paths for normal
  dashes, chipArc across the arc for the third dash), same 30 per
  second rate and 36 cap as the wisps, ticked on the same envelope
  factors as their wisp twins.
- Extra tendril pair at half alpha: on normal dashes, two strands
  mirror the original pair across the path (opposite right offset,
  below-path strand) at alpha 0.46 against the originals' 0.92. On
  the third dash, one extra inner arc (radius factor 0.74) drawn as
  two half tendrils at alpha 0.46 on the same closing radius curve.

## HollowAfterimageRenderer

Before: tendrilFan of 3 strands, plus 2 side tendrils and 1 hair
strand (6 tendrils, 2304 vertices).
After: tendrilFan of 5 strands (8 tendrils total).

- The fan count goes 3 to 5 per spec, strands at the same width,
  length, and skin to hair color grading. Same budget note as Void
  Slash: the veil submit is near 3072 vertices at full spine samples,
  over 2500 at point blank only, inside budget once distance scaling
  drops the samples. Nothing else in the renderer moved; the 100 tick
  fade curve, colors, and Dark Faded behavior are untouched.

## VoidRiftRenderer

Before: 2 inward rim tendrils (768 vertices).
After: 3 inward rim tendrils (1152 vertices), inside budget.

- The third strand enters from the side rim at mouth height (left
  rim at 1.02 of belly, control pulled toward the throat), on the
  same rim window of the implosion choreography as the original two,
  same width and colors.
- Inner ring decision: NOT added, and the renderer-side shardBurst
  fallback was not added either. Reasoning from the code: the audit
  conditions the second ring on the mouth reading sparse, and the
  mouth does not read sparse in the code. It already carries the five
  real FragmentEntity shards on the world ring (entity pass, held and
  released on the same path), plus seven emission streams including a
  dedicated rim soul stream and the ASH_FLAKE rim layer. A drawn burst
  of 3 at the same mouth radius would sit on top of the real ring and
  duplicate it, reading as clutter against the physical shards rather
  than filling a gap. The rift drag, growth, and implosion timing are
  untouched, and no entity file was edited.

## Verification

Targeted javac, JDK 25, sourcepath src/main/java:src/client/java,
over the seven owned renderer files, full logs read:

- cp262: 4 errors, all standing baseline closure kinds (DbaItems 2,
  DbaMenus 2). Zero errors in owned files.
- cp263: 10 errors, exactly the documented baseline set (DbaItems 2,
  DbaMenus 2, DbaHostileEntity 2, PatrolRobotEntity 2,
  FriezaSoldierEntity 1, NamekianWarriorEntity 1). Zero errors in
  owned files.
- Both classpaths were re-run after the Saber Slash form fix, with
  the same results.
- git diff --check clean on the seven owned files. Byte-level em dash
  scan on added lines: zero.
- No commits. No Gradle build run or claimed; the in-game look is
  unverified until Carter's PC test.

## Honest worse check

- Saber Slash: better. The woven crossing pair and the authored steel
  slivers give the cut a solid edge it never had, and the slivers'
  arc-tangent placement cannot read as floating pebbles. Risk: the
  slivers only live inside the strike window (about 3 to 7 ticks), so
  they read as a flash of steel at the hit moment; that is the specced
  swing-tick behavior, not a bug.
- Line Slash: better. Four staggered strands plus the in-plane crack
  web make the cut feel like it tears space. The one deviation is the
  crackFan branch count (9 to 11 short branches instead of 4), filed
  as change request 1; at radius near 1 block the web is small, and if
  it reads busy the merge can trim it once the builder takes a count.
- Void Tear: mixed, stated plainly. The composed crown and flankers
  are individually stronger than any fan shard was, and the mouth
  bursts are denser, but the shard count went down (5 to 3) because
  the audit counted the starting point as 1. If Carter's eye prefers
  the old fan's spread, restoring 5 with the crown kept is the fix;
  nothing else in the renderer depends on the count.
- Void Slash: better, with the recorded point-blank budget overage.
  The crossing pair fixes the THIN read; the void shards match the
  rift ring's material, so the weapon family reads consistent.
- Rush Trail: better. Chips shed on the dash paths and the half alpha
  mirror pair widen the corridor without brightening it; alpha 0.46
  keeps the new pair subordinate to the original strands.
- Afterimage: better, with the same point-blank budget note. Five fan
  strands fill the silhouette's rise so the ghost reads less sparse
  in its first ticks; the fade curve is unchanged, so the tail of the
  100 ticks is identical to before.
- Void Rift: slightly better (third strand fills the side of the
  mouth the first two never touched), and deliberately conservative
  on the optional ring for the reasons above. Nothing was reverted in
  this slot.
