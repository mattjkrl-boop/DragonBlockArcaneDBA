# No-Shapes Realism Pass Report: Spirit Sword

Scope: client/render/SpiritCannonBeamRenderer.java,
client/render/SpiritCleaveRenderer.java, and
client/render/SpiritImpaleRenderer.java. No item, entity, or shared file was
edited. Nothing committed.

## Geometry counts, before and after

Counts use the no-shapes audit rows for this owner: B is BespokeGeometry
builder calls, L is LayeredRender geometry calls, and V is direct vertex
drawing.

| Effect | B before | B after | L before | L after | V before | V after |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| SpiritCannonBeam | 8 | 0 | 0 | 0 | 0 | 0 |
| SpiritCleave | 8 | 0 | 0 | 0 | 0 | 0 |
| SpiritImpale | 19 | 0 | 2 | 0 | 0 | 0 |
| Owner total | 35 | 0 | 2 | 0 | 0 | 0 |

Additional grep checks across the three renderers:

- BespokeGeometry references: 37 code references before (8 beam, 8 cleave,
  19 builder calls plus 2 Rng references in SpiritImpale), 0 after.
- submitCustomGeometry calls: 6 before (2 per renderer), 0 after.
- KiRenderHelper references: present before for render types, 0 after.
- LayeredRender references: distScale utility only before, 0 after. Distance
  scaling now comes from MaterialEmitters.distanceScale.

## Emission method

Every continuous stream runs through MaterialEmitters.Emitter in the
deterministic tick(deltaSeconds, envelope) form. The delta is the client
frame time, clamped to 0.002 to 0.05 seconds, so a frame spike cannot build
emission debt. The envelope passed to each emitter is the effect lifecycle
envelope multiplied by MaterialEmitters.distanceScale for the effect origin.
Each stream has its own rate and per-second budget in code. Material seeds
derive from the same synced entity seed or UUID seed the geometry used,
split per stream, so the material variation is stable in identity even
though individual particles are client spawned.

## SpiritCannonBeam

Materials and sources:

- SOUL, white, path source along the synced beam: the dense spirit plasma
  core, rate 185 per second length scaled, budget 230.
- SOUL, cyan, same path: the wider sheath, rate 115 per second length
  scaled, budget 150, with larger sprites and more spread than the core.
- VAPOR, cyan, same path: the soft outer atmosphere, rate 42 per second
  length scaled, budget 60.
- SOUL, gold, path over the terminus third: preserves the old gold wisp
  placement without recreating orbital nodes, rate 38 per second length
  scaled, budget 55.
- SOUL, cyan, sphere volume at the muzzle: rate 58 per second, budget 75.
- PLASMA, white, sphere volume at the terminus: the impact plume, rate 95
  per second, budget 120, with velocity continuing along the beam.
- VAPOR, cyan, sphere volume at the terminus: impact haze, rate 34 per
  second, budget 50.
- Fixed SPARK particles at the terminus point: rate 42 per second,
  budget 60.

Carried over unchanged: the beam length and its wall-shortened value from
the entity, the 5 tick smoothstep fade in, the synced FADE retract formula
(0.15 plus 0.85 times fade), the pulse factor, the first person anchor
(sideSign times 0.40, minus 0.46, 0.35) and zStart 0.25 with fpScale 0.5,
applied through the same yaw and pitch rotation matrix the pose stack used.
The corkscrew and the traveling brightness band from the spectacle pass were
not resurrected in any form. Spirit Cannon damage, its percent bonus, the
5 tick pulse cadence, the ramp, and the wall occlusion ClipContext all live
in the item and entity files, which were not touched.

## SpiritCleave

Materials and sources:

- SOUL, white, sphere volume at the projectile position: the moving core of
  the cut, rate 215 per second, budget 260.
- SOUL, cyan, path source from behind the projectile to just ahead of it:
  the main traveled wake, rate 175 per second, budget 220.
- VAPOR, gold, same traveled path: the dissolving outer wake, rate 68 per
  second, budget 90.
- Fixed SPARK particles on the same traveled path: rate 58 per second,
  budget 75.
- SOUL, white, sphere volume at the opening front while the old tipFlash
  window is active: rate 165 per second, budget 200, scaled by tipFlash.

The trail is swing driven in the only way this projectile exists: the
Spirit Sword swing spawns the cleave entity, and the renderer emits along
that entity's real flight direction and traveled path, using its synced
rotation. No arc, crescent, ribbon, fan, or halo is drawn. Carried over
unchanged: the 3 tick open ease, the 2.25 length curve, the FxEnvelope
envelope (0.08 build, 0.30 decay) over the synced life, and the white, cyan,
and gold layer colors. The entity file, including its damage, percent fold,
hit handling, block impact, and Spirit Impale spawn, was not touched.

## SpiritImpale

Materials and sources:

- SOUL, cyan, ground area at the base radius: rate 95 per second,
  budget 120.
- VAPOR, gold, ground area inside the base radius: rate 36 per second,
  budget 50.
- SOUL, white, vertical volume through the old pillar core space: rate 118
  per second, budget 145, rising velocity.
- SOUL, cyan, wider vertical volume through the old pillar shell space:
  rate 88 per second, budget 110, rising velocity.
- PLASMA, white, horizontal volume at the old target center height across
  the six-sword ring radius: rate 78 per second, budget 98, downward
  velocity, carrying the plunging impact distribution as material instead
  of six sword meshes.
- VAPOR, cyan, wide volume around the pillar space: rate 40 per second,
  budget 55.

Carried over unchanged: the FxEnvelope envelope (0.12 build, 0.28 decay)
over the 40 tick life, the 4 tick slam curve, the pulse (0.85 plus 0.15
sine), the base radius, pillar height and radius shares, the sword ring
radius and target center height as emission data, the scale, and the UUID
seed. The entity file was not touched, so its server ground burst, debris
field, self-restoring crystal scar, vortex particles, closing burst, and
lifetime behavior are exactly as before.

## Mechanics statement

No damage number, damage type, percent-health behavior, cooldown, Ki cost,
timing, duration, hitbox, synced data field, or mechanic was changed. The
only edited files are the three client renderers above. Spirit Cannon wall
occlusion is entity and item mechanics and did not regress because that
code is byte-identical to the start of this task.

## Verification

Targeted javac with sourcepath over both source sets, the three owned
renderers as roots:

- 26.2 classpath (cp262.txt): zero errors in any owned file. All three
  renderer classes were emitted. The only errors in the run are the known
  baseline lines in DbaItems.java and DbaMenus.java (4 error lines).
- 26.3 classpath (cp263.txt): zero errors in any owned file. All three
  renderer classes were emitted. The only errors in the run are the known
  baseline lines in DbaItems.java, DbaMenus.java, DbaHostileEntity.java,
  FriezaSoldierEntity.java, NamekianWarriorEntity.java, and
  PatrolRobotEntity.java (10 error lines).
- Grep over the three renderers for BespokeGeometry, LayeredRender,
  addVertex, KiRenderHelper.vertex, and submitCustomGeometry: 0 matches.
- git diff --check over the three renderers: clean.

Compile status is the javac audit only. No Gradle build ran and none is
claimed. No in-game look is claimed.

## Honest worse check

- The beam no longer has a continuous solid silhouette. It reads as a dense
  stream of soul material, which is the point of this pass, but at the
  lowest effects density setting or at long range it will read thinner than
  the ribbon beam did. Soul particles also live longer than the beam's own
  ticks, so a soft drift of material can linger briefly after the aim moves
  or the fade ends; the beam is less surgical than it was.
- The orbital node rhythm, the muzzle seal, and the terminus splash star
  are gone as distinct ornaments. The gold terminus-third stream keeps the
  color placement, but nothing pulses along the beam in discrete nodes any
  more.
- The cleave no longer reads as a blade or a crescent at all. It is a fast
  soul wake behind a moving volume of light. That follows the rule, but it
  is the conversion most likely to be judged worse if a visible flying cut
  silhouette was part of the weapon's identity for Carter.
- Spirit Impale loses the six readable plunging swords, the pillar
  silhouette, and the crystal perimeter in the renderer. The downward
  plasma volume marks where the strikes land as a group, but individual
  sword strikes no longer exist as visible objects. The server's real
  crystal scar blocks and debris still ground the impact.
- Nothing was reverted: every loss above is a direct consequence of the
  no-shapes rule itself, not a defect found in the conversion.

## Change requests for the coordinating pass

Non-blocking observations only; the conversions above are complete without
them.

- MaterialEmitters has no path-volume source. A source that samples around
  a path at a given radius would let beam sheaths spawn their particles in
  a shell around the axis instead of on the axis with larger sprites and
  spread carrying the width.
- MaterialEmitters sources and presets are immutable snapshots, and Emitter
  has no per-entity persistent handle pattern. Renderers therefore rebuild
  source snapshots per submit and drive the deterministic tick form. A
  shared per-entity emitter registry keyed by entity id, with mutable
  source endpoints, would make continuous effects cheaper to run.
