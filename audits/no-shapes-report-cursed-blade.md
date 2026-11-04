# No-Shapes Realism Pass Report: Cursed Blade

Scope: the six Cursed Blade renderers assigned in audits/no-shapes-audit.md
(AbyssalDomainRenderer, CurseChainRenderer, CurseGroundShatterRenderer,
CurseLightningRenderer, CurseTelegraphRenderer, SkyCracksRenderer). Base
commit ad22b32 (Phase 1 foundation). Not committed, per instructions.
No entity, item, damage, cooldown, cost, timing, or synced data file was
touched: the diff for all six entities and CurseBladeItem is empty. Every
renderer now emits through client/fx/MaterialEmitters with
DbaMaterialOptions materials and draws no geometry at all.

## Before and after geometry counts

Before counts are the audit rows at d9f8a90 (B = BespokeGeometry builder
calls, L = LayeredRender geometry calls, V = direct vertex writes). After
counts are grep-verified in the converted files: zero BespokeGeometry
references, zero LayeredRender references, zero KiRenderHelper references,
and zero addVertex calls across all six files.

| Effect | B before | B after | L before | L after | V before | V after |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| AbyssalDomain | 14 | 0 | 3 | 0 | 0 | 0 |
| CurseChain | 8 | 0 | 0 | 0 | 0 | 0 |
| CurseGroundShatter | 8 | 0 | 0 | 0 | 0 | 0 |
| CurseLightning | 5 | 0 | 0 | 0 | 0 | 0 |
| CurseTelegraph | 8 | 0 | 0 | 0 | 0 | 0 |
| SkyCracks | 5 | 0 | 0 | 0 | 0 | 0 |
| **Owner total** | **48** | **0** | **3** | **0** | **0** | **0** |

The pose-glue helpers went with the geometry: CurseChainRenderer's
segmentRibbon and drawAuraRibbons are deleted. What survives in their place
is data only: point lists, polylines, and MaterialEmitters Source lambdas
that sample emission points. Distance scaling now comes from
MaterialEmitters.distanceScale, and every continuous stream runs through
the toolkit Emitter in its deterministic tick(delta, envelope) form, with
delta derived from the render state's age progression and the rate passed
as the per-second budget.

## Materials per effect

Audit theme for the owner: curse plasma, darkness smoke, lightning plasma,
ground dust. Soul wisps ride the chain glyphs, the telegraph heart, and the
sky tear, per the pass brief.

- **AbyssalDomain**: DARKNESS and SMOKE fill the standing volume sampled
  from sphereVolume and volume sources; PLASMA crimson and violet drift as
  motes through it; DUST creeps on a groundArea source; the boundary is
  only suggested by sparse large VAPOR, PLASMA, and DARKNESS sprites from a
  shell sampling source. Interior strikes on the old eight tick cycle are
  PLASMA emitted along seeded straight paths, and the top opening is a
  dense PLASMA point emission that strengthens with the old open envelope.
  The forty tick build and thirty tick fade envelopes are unchanged.
- **CurseChain**: the link layout survives as world-space emission data
  (the old arc and trail centers, transformed by the same scale, yaw, and
  pitch the pose stack applied). DARKNESS and violet PLASMA emit along the
  segment polyline, SOUL gathers at the link centers and the four glyph
  points, the heart is a PLASMA point gathering, the collar is sampled
  emission points on the three old ring radii, the attach snap is a PLASMA
  and SOUL burst, and the helical aura is two PLASMA polylines in the old
  violet and crimson. Fade in and the synced fade out are unchanged.
- **CurseGroundShatter**: the fissures are seeded jagged ground paths
  regenerated from the entity UUID, carrying crimson and pale PLASMA with
  DARKNESS venting along them; DUST rolls across the groundArea and
  thickens on the expanding shock radius the old band used to mark; the
  twelve slab positions are DUST and DARKNESS emission points with the old
  growth curve. The corrected fade over the 35 tick life is unchanged.
- **CurseLightning**: the bolt is a midpoint-displaced jagged polyline
  regenerated deterministically from the UUID seed each frame, carried by
  sheath PLASMA (crimson, or violet on rare strikes) and a white hot PLASMA
  core, with DARKNESS along the same path. The twin strike is a second
  seeded path. Ground arcs are short seeded PLASMA paths over DUST. The
  2.5 tick stab and the fade reaching zero at tick 9.5 are unchanged.
- **CurseTelegraph**: PLASMA and DARKNESS gather across the ground area in
  the old crimson and violet, DUST drifts with them, a hot PLASMA ground
  source shrinks from the full radius to nothing on the same progress
  curve the contracting band drew, rim emission sampled up to the old
  tendril height replaces the ribbons, and the heart is a PLASMA and SOUL
  point gathering that thickens with progress. The accelerating pulse is
  unchanged.
- **SkyCracks**: see the hard case below.

## The hard case: SkyCracks

The sky cracks were the signature branch and crackStar network. Under this
standard they are emission paths, not geometry and not a decal quad. Seven
main jagged polylines and eight shorter web paths are regenerated from the
synced seed in the camera billboard plane on every frame (the same plane
and seed the drawn version used, so the tear is the same tear on every
client). Crimson PLASMA bleeds along the paths, a white hot PLASMA core
rides the same paths, violet PLASMA fills the web, SOUL wisps drift off
the fractures, and a broad DARKNESS emission sampled across the billboard
disc behind the paths reads as the shadow of the tear. The lifecycle
envelope over 100 ticks and the electrical flicker are unchanged. No
branch, crackStar, glowFan, mesh, or textured quad remains.

One limit stated plainly: Phase 1 built no shared crack decal system, and
the sky has no surface to decal. A persistent glowing crack image would
need either a drawn quad (a shape, forbidden by the standard) or a central
decal toolkit that does not exist yet. The crack paths are therefore
carried by dense plasma emission, which is the standard's rift method
applied to a surface that cannot hold a decal. This is change request 1
below, not a private workaround.

## Verification (targeted javac, JDK 25, both classpaths)

Method: javac -proc:none -Xmaxerrs 10000 with the audit classpath argfiles,
-sourcepath src/main/java:src/client/java, compiling the six converted
renderers plus the six Cursed Blade entities and CurseBladeItem, so the
dependency closure is attributed with them.

- 26.2 (cp262.txt): 0 errors in all 13 Curse files; all six renderer
  classes emitted. Remaining errors: 4, all in shared files DbaItems (2)
  and DbaMenus (2), the known pre-existing baseline kinds.
- 26.3 (cp263.txt): 0 errors in all 13 Curse files; all six renderer
  classes emitted. Remaining errors: 10, all in known baseline files
  (DbaItems 2, DbaMenus 2, DbaHostileEntity 2, PatrolRobotEntity 2,
  FriezaSoldierEntity 1, NamekianWarriorEntity 1). None are Curse files.
- git diff --check on the six renderers: clean. Em dash scan of the six
  renderers and this report: zero.
- No Gradle build was run; the verification stands as javac audit only,
  per the standing waiver. No in-game look is claimed.

## Shared change requests (not privately worked around)

1. **Crack decal surface.** The foundation has no decal renderer or crack
   texture set. Ground cracks (Shatter, Telegraph) and the sky tear are
   carried by plasma, darkness, and dust emission along seeded crack
   paths. If a persistent glowing crack image is wanted, the coordinating
   pass should add one shared decal system centrally; owners must not
   invent private ones.
2. **Per-entity emitter registry.** Renderers hold no per-entity emitter
   state, so each submit constructs Emitter instances and drives them with
   the deterministic tick(delta, envelope) form. Budgets are therefore
   enforced per call rather than as a strict per-second window per entity.
   A shared registry keyed by entity would tighten this centrally.

## Honest worse check

Nothing was reverted. Four trades to name plainly:

- SkyCracks loses the persistent, sharply defined crack image. The tear is
  now dense plasma along the same seeded paths, so at long range or low
  effects density it reads softer than the drawn network. That is the
  standard's price for the signature piece, and it is the conversion most
  likely to be judged on Carter's PC test.
- CurseChain no longer has a continuous solid body: it is a dense stream
  of darkness and plasma along the exact old link layout. At low density
  it reads as cursed energy in chain formation rather than hardware.
- AbyssalDomain loses its dome silhouette by design. The boundary is
  suggested by shell-sampled sprites, so the domain edge is less precise
  than the displaced dome was, especially far away where distance scaling
  steps emission down.
- CurseTelegraph loses the drawn marker's exact edge. The shrinking
  emission source contracts on the identical progress curve, but a player
  judging the strike radius by eye gets a soft material edge instead of a
  line.

Everything else carries over: same colors, same envelopes, same seeds,
same lifetimes, and zero mechanics touched.
