# No-Shapes Report: Ki Techniques

Owner: Ki Techniques (replacement agent; the first Ki Techniques agent
died to an infrastructure error mid-work). Base HEAD ad22b32. All work is
in the seven Ki renderer files assigned in audits/no-shapes-audit.md. No
shared file was edited, no entity or handler file was touched, no commit
was made, and no other owner's work was reset or reverted.

## The partial diff: what came from the dead agent, and its audit

The partial diff covered five of the seven renderers: KiBeamRenderer,
KiBlastRenderer, KiDiskRenderer, KiLaserRenderer, KiSpiralBeamRenderer.
KiBarrierRenderer and KiExplosionRenderer were not in the diff at all;
they were still in their bespoke state (6 BespokeGeometry calls each)
and were converted fresh by this agent (see below).

Each of the five partial files was audited in full, not just grepped:

- Completeness: every file is a complete conversion. No half-converted
  file exists in the set: zero BespokeGeometry references, zero
  LayeredRender references, zero KiRenderHelper references, zero
  submitCustomGeometry calls, zero addVertex in all five. Each builds
  its emitters from MaterialEmitters presets and sources, keyed per
  cast by the synced seed and color, with the toolkit Emitter ticked
  through a per-frame delta derived from ageInTicks and an envelope of
  FxEnvelope.distScale times the lifecycle alpha where one exists.
- Synced seed: each of the five extracts entity.getSeed() in
  extractRenderState (verified: exactly one getSeed() extraction per
  file, all seven files), so the scope expansion seeds are actually
  consumed, not just present.
- Lifecycle fidelity, checked against HEAD: the beam's fade is
  byte-identical math (life = min(1, age / 20), alpha = 1 - life^2).
  The laser and spiral beam carried constant alphas at HEAD (sheath
  0.4 and core 1.0; spiral bands 0.85) with no lifecycle fade, and the
  conversions correctly use a distScale-only envelope rather than
  inventing one. The spiral keeps its 3.5 turns over the length, its
  phase rate of age times 0.35 radians, and its helix radius of 0.28
  times scale, now as emission data. The blast keeps its pulse
  (1 + 0.12 sin(age times 0.9)) driving the cluster radius. The disk
  keeps its 0.72 times scale outer radius, with the old spin carried
  as tangential birth velocity.
- Fixes needed in the five partial files: none. No compile error, no
  dangling reference, no leftover helper, and no mechanic drift was
  found in any of them.

Fixes needed in this agent's own two new files: the first javac run
caught long-literal seed XORs (0x2B3CL style, inherited from the old
bespoke seed constants) in KiBarrierRenderer and KiExplosionRenderer,
seven lossy-conversion errors in total. The L suffixes were dropped;
the re-run is clean (see Verification).

## Converted fresh by this agent

- KiBarrierRenderer (6 calls to 0): the dome pair shell becomes a
  drifting plasma mote volume inside the old radius plus a sparse
  boundary film of large vapor sprites (glow color, the old 0.7 lerp
  toward white) and plasma sprites sampled on the surface. The flat
  rim band becomes a white plasma ring emission at 0.97 times radius
  around the equator; the vertical rim band becomes a glow plasma ring
  emission standing in a vertical plane rotating at the old spin times
  1.7. The radius formula is unchanged: (1.02 + 0.28 times used
  percent) times (1 + 0.02 times breathe), breathe = 0.5 + 0.5
  sin(age times 0.22), center still 0.95 above the entity origin, and
  the envelope breathes with the same cycle the shell alpha used.
- KiExplosionRenderer (6 calls to 0): the dome shell becomes
  technique-colored plasma filling the blast volume plus vapor blown
  off the boundary and smoke rising through it; the white core domes
  become a white plasma concentration inside 0.38 times radius; the
  plume is carried by the vapor and smoke streams; the broken ground
  ring becomes impact dust emitted on a ring at the old ring radius,
  ticked with the old ring alpha curve (fade times (1 - open)). The
  lifecycle is unchanged: ease open over 8 ticks, fade across 36 ticks
  squared, early out at fade 0.02, ring radius = eased radius times
  (0.85 + open times 0.35).

## Before and after geometry counts (grep measured)

| File | BespokeGeometry before | after | LayeredRender geometry before | after | Direct vertex before | after |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| KiBarrierRenderer | 6 | 0 | 0 | 0 | 0 | 0 |
| KiBeamRenderer | 1 | 0 | 0 | 0 | 0 | 0 |
| KiBlastRenderer | 4 | 0 | 0 | 0 | 0 | 0 |
| KiDiskRenderer | 2 | 0 | 0 | 0 | 0 | 0 |
| KiExplosionRenderer | 6 | 0 | 0 | 0 | 0 | 0 |
| KiLaserRenderer | 1 | 0 | 0 | 0 | 0 | 0 |
| KiSpiralBeamRenderer | 2 | 0 | 0 | 0 | 0 | 0 |
| Owner total | 22 | 0 | 0 | 0 | 0 | 0 |

After counts also verified for the wider references: LayeredRender
mentions 0, KiRenderHelper mentions 0, and submitCustomGeometry calls
0 in all seven files. The one addVertex left anywhere in the render/ki
folder sits inside KiRenderHelper itself (the deprecated toolkit's own
definition), which gate 3 of the standard excludes and which is a
shared file outside this ownership. Em dashes in the seven files: 0.

## Materials per effect

All themes stay technique-colored (the entity's synced color), per the
audit's binding assignment.

- KiBeam: PLASMA along the full path, white PLASMA concentrated over
  the first 28 percent of the path, VAPOR along the path, PLASMA plume
  and SMOKE at the terminus.
- KiLaser: PLASMA along the full path at thin scale, white PLASMA over
  the first 22 percent, VAPOR along the path, PLASMA plume at the end.
- KiSpiralBeam: white PLASMA core stream and technique PLASMA stream
  along the path, technique PLASMA and VAPOR born on the 3.5 turn
  helix as data, PLASMA plume and SMOKE at the terminus.
- KiBlast: technique PLASMA cluster, white PLASMA heart cluster,
  technique PLASMA and VAPOR trail streaming off the tail.
- KiDisk: technique PLASMA across the disc plane, white PLASMA toward
  the rim, VAPOR off the faces, EMBER and technique PLASMA shed at the
  rim with spin velocity.
- KiBarrier: technique PLASMA motes in the volume, VAPOR and PLASMA
  film sprites on the boundary, white PLASMA flat rim ring, glow
  PLASMA vertical rim ring.
- KiExplosion: technique PLASMA volume fill, white PLASMA core, VAPOR
  boundary blow-off, SMOKE rising, technique DUST ground ring.

## Mechanics and identity

Presentation only. No damage number, damage type, cooldown, Ki cost,
duration, radius, hitbox, or synced data field changed anywhere; no
file outside the seven renderers was edited. The seven entities keep
their scope expansion synced SEED fields untouched, and every renderer
now consumes its seed. Lifecycle envelopes were carried over per file
as documented above, including the explosion's early out and the
barrier's breathe and used-percent radius growth.

## Carried item for the merge to judge

The audit row for KiBarrier names hit ripple particle bursts. There
is no synced hit signal on KiBarrierEntity (its synced fields are
color, used percent, owner, seed), so a renderer-side ripple cannot
be keyed to an actual hit without adding a synced field, which would
break the presentation-only and synced-data-identical rules of this
pass. What exists instead: the boundary film density pulses with the
breathe cycle, and real hit feedback already lives in the entity's own
absorb burst (themed dust, converted in the scope expansion) and was
left exactly as it was. If Carter wants true hit ripples, the merge
can add one synced flash field to the entity as a deliberate,
separately approved change.

## Change requests for shared files

None. MaterialEmitters, DbaParticles, and DbaMaterialParticle already
expose everything this area needed.

## Verification

Targeted javac audit only; no Gradle build was run or claimed.

- Command shape: javac (JDK 25) with the audit classpath file,
  sourcepath covering src/main/java and src/client/java, the seven
  owned renderers as roots.
- 26.2 (cp262.txt): zero errors naming any owned file. The only errors
  in the closure are the 4 pre-existing baseline lines in non-owned
  files (DbaItems, DbaMenus), the same baseline documented in the
  scope 2 Ki report.
- 26.3 (cp263.txt): zero errors naming any owned file. The only
  errors are 10 lines of the same pre-existing baseline kinds
  (DbaItems, DbaMenus, DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity), all outside this area.
- Class files were produced for all seven owned renderers on both
  classpaths (28 class files in the render/ki output package).

## Honest worse check

Nothing mechanical changed, so nothing plays worse. The visual reads
that changed character, stated plainly:

- Beam and laser: the white core no longer runs the full length as
  its own visible stream; it is a brighter concentration near the
  source (first 28 and 22 percent of the path), exactly the standard's
  beam method. The colored plasma stream carries the rest of the
  path. The beam reads less like a solid lance and more like emitted
  energy; anyone expecting the old hard white spine will see the
  difference.
- Spiral beam: the winding now exists only in where particles are
  born and how they drift. At low effects density the spiral read
  will be weaker than the bespoke band was (the literal double helix
  read was already lost in the bespoke pass).
- Disk: there is no solid disc at all. At long range or low density
  it can read as a sparse spinning cloud rather than a cutting disc;
  the rim weighted white plasma and ember shedding carry the edge.
- Barrier: no continuous shell surface. From inside, the wearer sees
  motes and film sprites instead of a wall (the double wall shimmer
  was already lost in the bespoke pass). The film sprites are sparse
  by design, so at minimum effects density the barrier reads faint.
- Explosion: no dome silhouette; the blast reads as churn. The ground
  ring is dust emission strongest in the first ticks, matching the
  old ring alpha curve, but it has no crisp edge.
- Blast: cluster plus trail only; the bespoke ribbon body's nose to
  tail taper is now the trail's widening jitter.

No regression was found that would justify reverting any file: sizes,
colors, envelopes, and motion parameters carry over, and the material
read is the point of the pass. The in-game look itself is unverified
from this environment, as with every pass.
