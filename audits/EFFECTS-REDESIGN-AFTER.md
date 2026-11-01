# Effects Redesign Pass, After (Nov 1, 2026)

Carter's direction: "look at how the effects look before changing them so
they look similar but 100 times better." This pass redesigned all 73 weapon
ability effects under the plan in EFFECTS-REDESIGN-PLAN.md. Every effect
kept its identity: same dominant palette, same signature shapes, same
lifetime and role. No damage, behavior, cooldown, trigger, or synced data
changed anywhere; entity diffs are particle emission only. All fixes from
the previous visual pass (ENTITY-VISUALS-AFTER.md) were preserved and
verified present in the final sources.

Verification: dual javac audit sweeps on the 26.2 and 26.3 classpaths,
zero new error kinds against the pre-pass baseline (HEAD 30e1afd) on both
versions. Full diff review: 118 files, 2632 insertions, 320 deletions, no
em dashes in added lines. No Gradle build (waived by Carter).

## The design system, as applied

- T1 Additive glow halo. Before this pass, none of the 73 renderers used
  KiRenderHelper.kiAdditiveType(); everything was normal blending, which
  is why effects read flat and matte. After: 73 renderer files draw an
  additive pass (signature shape at larger scale, low alpha), so cores
  bloom and edges glow.
- T2 Three-stop gradients replacing flat colors: Erasure orb shells (the
  flattest surfaces in the mod) now shade pole to pole; Trident shard
  faces ramp to a hot tip; Power Pole shaft, Bansho funnel, Azure funnel
  and tunnel, Ox fissure (hot at the root, cool at the tip), SorrowSlash
  arc shading, Valor Field dome (deep gold base to near-white zenith).
- T3 Tessellation step-ups across rings, domes, funnels, and discs,
  typically 1.5x (Valor Field dome 10x24 to 14x36, storm dome 10x24 to
  12x36, Temporal Rift dome 11x28 to 14x40, most rings up one step).
- T4 Eased and secondary motion: Power Pole impact expansion is now
  ease-out cubic; Hell Hunt seal rings counter-rotate instead of sitting
  frozen; Void Slash rolls gently on top of its signature tilt; Darkness
  Wave layers breathe; Dimensional Rift shards bob on individual phases;
  Flawless geysers flicker per geyser; Valor Field gained a 6 tick spawn
  fade-in.
- T5 Theme particles: most entities emitted none. Signature and long
  lived effects now emit their theme particle from the server tick,
  density scaled by the effects config: storm eye KI_MOTE, tornado SPARK,
  domains VOID_SMOKE, charges EMBER and VOID_SMOKE, Ox effects EMBER along
  the crest and inside the stance dome, KiBlast SPARK trail plus a burst
  where the hit used to be sound only, Blaster SPARK trails, Erasure orb
  gathering sparks that tighten with charge, and more. Tiny 8 to 18 tick
  bursts were deliberately not given emission (their mesh carries them,
  and particles would linger past the effect).
- T6 Ground anchor glow: additive ground discs and washes under the
  charge constructs, domains, Z Stance Aura, Bansho Cyclone, Temporal
  Rift, and Ox Charge, so standing effects sit in the world.
- T7 Energy flow: the Erasure Cannon beam's uniform 48 block tube is
  gone; a new drawFlowBeam draws each prism layer in length bands with a
  traveling brightness wave and a terminus taper, plus an additive halo
  with the same bands. Spirit Cannon's sheath got the same flow
  treatment, and HeavenSplitter, SaberDimensionalLineSlash, BraveRushTrail,
  GrandCrescentWave (sequenced glyph pulse), and the OxShockwave ridge
  crest got traveling highlights.

## Two correctness fixes found during the redesign

- SkyCracksRenderer: the 16 sky shards received the fade pulse parameter
  and ignored it, holding fixed alpha while the quad and rays faded; they
  now follow the envelope.
- DimensionalWarpRiftRenderer: the tear slit's black core layer was
  hard-coded at 0.98 alpha and the singularity shell at 0.99, both
  ignoring the sine fade envelope; both now multiply by the fade.

## Deliberately not changed

- The three bolt and sky textures (Azure and Curse lightning, sky cracks)
  are identity and were not touched; the redesign elevated the geometry
  and glow around them.
- BlasterBolt's heat palette logic is byte-for-byte untouched.
- GrandBladeShard's flat blade facets got no gradient overlay (z-fighting
  risk); the additive pass and white-gold edges carry its depth.
- Charge and domain exit pops remain, as documented in the previous pass:
  the client cannot know the release tick in advance. They keep their
  fade-ins and gained glow, gradients, and particles here.
- SaberVoidTear's restored dark rim is not in the additive pass, because
  additive blending would brighten the dark rim away.
