# Attack Upgrade Pass: Z Sword owner report

Spec: audits/attack-complexity-audit.md section 2, builder contract in
audits/attack-foundation-addendum.md. Base HEAD ccfebf2. Files changed:
four client renderers only (DelayedLaunchRenderer, ZGravitySlamRenderer,
ZShockwaveRenderer, ZChargeRenderer). No entity, item, or foundation
files touched. No commits.

## The gravity curtain (DelayedLaunchRenderer + ZGravitySlamRenderer)

One wall, two renderers, a clean handoff at the launch tick:

- Placement is keyed to a position hash (both entities sit at the
  caster's position but carry different seeds, so the curtain cannot
  key to either entity seed). Static helpers on DelayedLaunchRenderer
  (curtainSeed, curtainSpot, curtainLength, curtainHeight) are called
  by both renderers, so every client draws the identical wall: facing
  from the hash, front edge at 0.66 of the slam radius, a seeded
  lateral shift of up to 15 percent of the length so it does not sit
  dead-center like a fence, length clamped to 9.5 to 11.5 blocks
  (8 to 10 slabs at any power), height 2.4 to 3.4 blocks scaled by
  the radius, arc 0.45 (a bowed, clustered front, not a straight row).
- Palette is the slam's own ground pair (root DARK_STONE 0x77675A,
  tip STONE_DUST 0x9A8668), matching the existing fracture plates,
  and every new piece draws through submitSolidLit with the render
  state's lightCoords. No new piece is on the emissive path.
- DelayedLaunchRenderer (the countdown): RISE stretched across the
  launch delay, full height at 80 percent of the delay via smoothstep
  into the builder's riseFactor; the wall origin lifts up to 1.05
  blocks as rise completes, so the slabs hang above the ground on
  the gravity envelope with their seeded tilts and leans showing;
  in the last four ticks the drop begins, sink running to 0.45.
- ZGravitySlamRenderer (the drop): the launch delay is recomputed
  with the item's own formula (40 plus power times 40, the same
  ticks the slam entity pins for). From the launch tick, the wall is
  picked up at sink 0.45 and the same hang height the telegraph left
  it at (identical geometry, identical factors, so the swap between
  renderers is invisible), then sink runs to 1.0 over four ticks on
  the builder's ease-in, which reads as weight. SLAM dressing in
  that window: a fresh full-radius crackFan flash (0.95 of the slam
  radius, pale gold to violet, envelope dying with the sink), three
  big heaved companion stone chunks clustered along the wall line
  (shard builder, length 0.45 to 0.75 of the wall height, girth 0.36
  of length, sinking with the wall), and a one-shot burst pair at
  the window's first frame: a DUST ring of 30 at the curtain band
  and 12 block chunks in the struck ground's real block state thrown
  off the wall base.
- On the chunk burst: a renderer cannot call the server-side
  DebrisSpawner. The literal DebrisSpawner burst at the drop moment
  already exists entity-side (DelayedLaunchEntity.launch fires its
  field at the same tick) and the hard rule protects that count, so
  it is unchanged; the 12 block chunks here are client particles in
  the ground's own material, layered at the wall base where the
  spec's beat lands.

Piece counts, curtain: before, 0 wall pieces in both renderers.
After: rockWall 8 to 10 slabs (576 to 720 vertices) in
DelayedLaunchRenderer for the whole countdown, and in
ZGravitySlamRenderer's drop window the same wall plus 3 companion
shards (1152 vertices) plus the flash crackFan (about 900 vertices).
DelayedLaunchRenderer total pieces go from about 900 (crackFan) to
about 1600; ZGravitySlamRenderer's drop window totals about 4200
against its class L budget, its steady state unchanged.

## ZShockwaveRenderer

- Second fracturePlates ring at 1.3 times the plate radius, lift at
  0.55 (the smaller class), alpha 0.85. Plates per fan are fixed by
  the builder at about 10, so the ring takes the total from about
  10 plates to about 20 at distance scale 1.0 (see change request 2:
  the spec's "up by half" cannot be expressed exactly from this
  side of the foundation).
- Crack branching up: a second crackFan at 0.62 of the main fan's
  radius, alpha 0.80, distinct seed, so the web reads denser and
  more branched at the core (see change request 1).
- Both plate rings draw through submitSolidLit. The original ring
  moved off submitSolid so the stone composition takes world light
  uniformly; leaving one ring emissive and one lit would have read
  as two different materials.
- Pieces before: 1 plate fan plus 1 crackFan (about 1450 vertices).
  After: 2 plate fans plus 2 crackFans (about 2600 vertices).

## ZChargeRenderer

- Four suspended stone shards above the blade core (shard builder
  through submitSolidLit, root DARK_STONE to tip STONE_DUST, lengths
  0.70 and 0.88 alternating, girth 0.36 of length: few, big, chunky).
  They orbit slowly at radius 0.42 around the core point at height
  1.85 with a small bob while the charge builds.
- On release (fadeTicks at or past 0) they drop on the fade
  envelope: drop distance grows with fadeT squared to 1.55 blocks
  over the 8-tick fade, their axes tilt outward as they fall, and a
  one-shot SHARD_CHIP burst of 18 fires at the shards' height on the
  first fade frame. Pieces before: the tendril fan only. After: plus
  4 shards (1536 vertices) and the burst.

## Verification

Targeted javac (JDK 25, sourcepath src/main/java:src/client/java)
over the four owned files, full logs read:

- cp262: 4 errors, all the documented baseline kinds (DbaItems x2,
  DbaMenus x2). Zero in owned files.
- cp263: 10 errors, exactly the documented baseline set (the 26.2
  four plus DbaHostileEntity x2, PatrolRobotEntity x2,
  FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero in owned
  files.

git diff --check clean on the four files. Byte-level em dash and en
dash scan on added lines: zero. No Gradle build run or claimed;
in-game look unverified until the PC test.

## Change requests (foundation is read-only)

1. crackFan has no branch-count parameter: cracks are segments(9,
   distScale, 6) plus an internal 0 to 2 roll. The audit's "branches
   plus 2" is realized as a second seeded fan. A branch-count
   overload would let owners hit the spec exactly.
2. fracturePlates has no plate-count parameter (about 10 plates per
   call, fixed). The audit's "count up by half" is realized as a
   full second ring. A count parameter or partial-fan variant would
   close this.
3. rockWall slab width is derived (length over about 1.15 blocks
   per slab), so "fewer, bigger slabs" can only be steered through
   length and height from the caller. The merge's announced builder
   irregularity upgrade (clustered placement, mountain profile) is
   the right home for this; the curtain is built to the current
   signature and will deepen when that lands.

## Honest worse check

- Empty-ground slams: if a release has no launch targets, no
  DelayedLaunchEntity exists, so nothing rises during the pin
  window, and ZGravitySlamRenderer still drops a fully risen
  curtain at the launch tick. It reads as the ground heaving at the
  drop moment, but the rise is missing in that case and the wall's
  first frame is a pop-in at full height. First thing to judge on
  the PC test.
- The curtain's facing comes from the position hash, not the
  caster's look direction (neither entity syncs a facing usable
  here). The wall is on the same world edge for every viewer of a
  given slam, which keeps multiplayer reads consistent, but it is
  not guaranteed to be the edge the caster is looking at.
- ZShockwave's plate count doubled rather than rising by half
  (change request 2). The second ring is subordinate (0.55 lift,
  0.85 alpha, wider radius), but at point-blank range the origin
  ground is denser than the spec's arithmetic intended.
- ZCharge's pieces total rises to roughly 3000 vertices at distance
  scale 1.0, the top of class M. The four shards are the weapon's
  only solid pieces; the count is the price of giving the charge a
  physical read, and it is stated here rather than hidden.
- The companion chunks exist only in the drop window (a budget
  call: 1152 vertices riding the whole countdown in the class M
  telegraph was too much). During the rise the curtain is the wall
  alone; its uneven slab heights, tilts, and bow carry the heaved
  read, and the merge's builder upgrade will deepen that further.
- ZGravitySlamRenderer's pre-existing fracture plates still draw
  through submitSolid (untouched, outside this spec). In the drop
  window the lit wall sits near emissive plates for a few ticks;
  the plates are fading by then and the reads did not clash in the
  code paths, but it is a visible seam a PC test can judge.

Nothing was reverted. No damage, cooldown, cost, timing, mechanic,
or synced field changed anywhere; DelayedLaunch's timing and its
debris field counts are byte-identical (no main-source file was
edited at all).
