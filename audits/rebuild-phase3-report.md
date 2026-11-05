# Rebuild Phase 3 Report: Universal Particles, Recipes, Seeded Packets

Date: 2026-11-04. Base: ef5a77e (Phase 2 head). Scope: the
ability-side particle system the research prescribes. Non-ability
users of the material particle system (auras, weather, StaggerFx,
the wish ritual) were not touched. No damage, cost, cooldown,
timing, or carrier behavior changed: presentation routing only,
verified by reading the full server-side diff (every hunk is an
added packet send at the same tick as the event it dresses, plus
the deletion of the Phase 1 server charge motes).

## The universal particle class

One registered type, dragonblockarcanedba:ability_fx
(DbaParticles.ABILITY_FX), a Fabric complex particle type. Every
instance is fully parameterized at spawn by one options record
(particle/AbilityFxOptions, MapCodec plus StreamCodec in the
DbaMaterialOptions pattern): entry index, ARGB color, size start,
size end, lifetime, gravity, drag, seed.

The entry table (particle/AbilityFxEntries) is the one place data
names meet sprites: 7 entries, 8 flipbook frames each, 56 slots
in assets/dragonblockarcanedba/particles/ability_fx.json, reusing
the sprite families the old material set established:

- shard_chip (lit): ki_shard geometry chips
- spark_streak (additive): spark streaks
- wisp_curl (additive): ki_shard energy frames
- ember_fragment (additive): spark streak frames
- ash_flake (lit): ash flakes
- soft_mote (additive): ki_mote sprites
- smoke_puff (lit): smoke sprites

The client particle (client/particle/AbilityFxParticle) renders
through the existing pipeline as a SingleQuadParticle: size
lerps from size start to size end across life, alpha fades in
over the first 12 percent and out with a soft tail, roll spins at
a seed-derived rate. Lit entries take world light, ride the
translucent layer, and collide with the ground, so chips and
flakes shade like physical matter (the standing world-lit rule).
Additive entries are full bright on the additive layer, which
never writes depth (the research rule). Registration follows the
mod's existing conventions: the type in DbaParticles, the
provider in DbaParticleProviders.

## Emission recipes in data

The ability schema gained an "fx" list of recipes, parsed with
the same strictness as every other block. Each recipe: event
(cast_start, fire, carrier_tick, carrier_hit, carrier_end,
charge_tick; "strike" accepted as an alias for fire), entry,
shape (point, ring, disc, cone, sphere_shell, band), count for
burst events or rate per second for continuous ones (the wrong
one for the event kind is rejected), speed, spread, cone angle,
optional radius override, size as a value or [min, max] range,
sizeEndScale, lifetime range, gravity, drag, alpha override,
color as core, mid, rim, or a literal hex, a hard per-burst cap,
and a cap group name. Unknown events, entries, shapes, and
fields are rejected by name; cap must be at least 1; burst
counts must be at least 1; continuous rates must be positive.

Caps hold twice over. Every route scales emission by a
deterministic distance factor from the caps block (full rate
inside fullDetailDistance, linear fall to 0.35 at cullDistance,
nothing past it), times the effects density setting, the
definition's particleRate, and a power scale. On top of that,
client/fx/FxBudget enforces maxParticlesPerSecond per ability
and per cap group across bursts and continuous emitters alike,
in one-second windows: a recipe can ask for more than the cap,
it cannot emit more.

## The seeded packet protocol

network/AbilityFxPayload: ability id, event ordinal, origin
(3 doubles), direction (3 floats), seed, power, repeat. That is
the whole packet; the server never streams particles.
ability/AbilityFxService sends it only when the definition has
recipes for the event, to the anchor entity's tracking players
plus the anchor player itself.

Routes, and why:

- cast_start, fire, carrier_hit, carrier_end: by packet. These
  are one-shot moments where every client should see the same
  burst. Fire reuses the exact seed rolled for the carrier, so
  burst and carrier agree. The cloud's strike burst reuses the
  strike's already-synced seed, so the particle burst and the
  renderer's bolt describe the same strike. The wave aggregates
  a tick's hits into one packet whose repeat (capped at 8)
  replays its small per-hit recipe, so a wide sweep stays one
  small packet.
- carrier_tick: carrier-local in the renderers. The carrier is
  already synced; a continuous shed needs no cross-client
  identity, and a packet per tick would be waste. Renderers now
  run List<RecipeEmitter> built from the definition's authored
  carrier_tick recipes, or from ability/AbilityFxDefaults when
  the definition authors none (the defaults mirror the old
  Phase 1 hardcoded sheds: wave band shed, cloud ground puffs
  plus drift, projectile cluster plus wake band plus a capped
  spark trickle).
- charge_tick: client-local from the Phase 2 cast store.
  client/ability/ClientAbilityFx ticks one emitter per charging
  player and recipe, rate ramping with charge fraction. The
  Phase 1 server-sent charge motes in AbilityService are
  deleted; the server sends no charge particles at all.

Determinism: client/fx/AbilityFxExpander expands a burst from a
splitmix stream keyed on the packet seed (the mod's SeedHash
mixing), per-particle seeds offset by a fixed stride of 1013,
and a fixed random draw order per shape (position draws, velocity
draws, exactly three jitter draws even at zero spread, then
lifetime and size), so stream position never depends on recipe
values. Positions and velocities derive only from the seed, the
definition, and the packet context.

## Proof (harness, run against the compiled sources)

23 checks, all passed:

- Both proof files parse with their fx blocks (Z Shockwave 7
  recipes, Cursed Tempest 6).
- Eight malformed variants rejected by name: bad event
  (fx[0].event), bad entry (fx[0].entry), bad shape (fx[0].shape),
  cap 0 (fx[0].cap), burst count 0 (fx[1].count), continuous
  recipe with no rate (fx[3].rate), rate on a burst recipe
  (fx[1].rate), unknown field (fx[0].bogusField).
- Determinism: the Cursed Tempest carrier_hit burst expanded
  twice independently from seed 12345 produces record-identical
  spawn lists (22 spawns); seed 999 differs from the first spawn
  on; a count limit of 5 clips the burst to exactly 5.

## Cap math (computed from the parsed data by the harness)

The research cap is roughly 4000 shared particles alive inside a
32-block radius.

- Cursed Tempest at full tilt: carrier_tick shed is 230 per
  second (smoke 150 plus wisp 80, cap group "weather"), inside
  its 320 per second data cap. Worst-case simultaneous estimate
  is about 564 alive: 542 from the shed (smoke averages 2.6 s of
  life, wisp 1.9 s) plus about 22 from strike bursts (32 max per
  strike, one strike every 16 ticks).
- Z Shockwave at full tilt: shed 280 per second inside its 400
  per second cap, about 222 alive worst case including hit
  bursts (capped at 48 per tick aggregate). While charging it
  adds about 50 alive from its charge_tick recipe; charging and
  its own wave never overlap.
- Both proof attacks at full tilt in the same radius: about 786
  alive, roughly a fifth of the shared cap, before the FxBudget
  clamp is even needed.

## What migrated, what did not

Migrated: all three carriers' continuous sheds (from
MaterialEmitters to recipe emitters), the charge dressing (from
server-sent material motes to a client-local charge_tick
recipe), and every burst moment on the ability surface (cast
start, fire, wave hit ticks, cloud strikes, projectile hits,
carrier ends) to the seeded protocol. The two proof attacks
author their full recipe sets in data.

Not migrated, with reasons:

- Carrier geometry (wave band strips and ground ring, cloud dome
  shells, ambient lightning, telegraph ring, strike bolt,
  projectile casing and core): geometry, not particles. It stays
  in the renderers and is Phase 4 kit material.
- The projectile carrier's server-side burst() impact flash: it
  is the carrier entity's own flash on the shared material
  system, fired identically for any definition, not an ability
  recipe emission. The recipe-driven carrier_hit packet now
  fires alongside it on the same tick. Folding the flash itself
  into the ability system belongs with the Phase 4 pass over
  carrier presentation, and it is named here rather than
  silently dropped.
- The cloud renderer's shed context uses the live radius each
  tick, where the packet route uses the definition's radius
  hints; that is the documented difference between a synced
  continuous shed and a one-shot burst, kept deliberately.

## Gates

- Dual javac sweeps: 120 error kinds on the 26.2 classpath and
  126 on the 26.3 classpath, exactly the strip-base rebuild
  baselines; comm -13 against kinds-rebuild-base262.txt and
  kinds-rebuild-base263.txt prints nothing on both.
- All 570 resource JSONs parse (569 pre-existing plus the new
  ability_fx.json).
- git diff --check clean. Em dash scan: zero in every file this
  phase created or modified. (Two pre-existing hits remain in
  src/client/java/com/dragonblockarcanedba/client/gui/
  DbaHudOverlay.java, legacy in-game HUD strings in a file this
  phase did not touch; per Carter's rule, past text is not gone
  back and changed.)
- Behavior audit: the full server diff was read line by line.
  Damage application, Ki costs, cooldowns, phase timing, carrier
  movement, and hit ticks are byte-identical; the only deletions
  are the Phase 1 charge motes (presentation) and the renderers'
  MaterialEmitters wiring (presentation).

Honest limits, same as every phase: the compile check is the
javac sweep, not a Gradle build (waived in this VM), and the
in-game look of the new particle system is unverified until
Carter's PC test. The two proof attacks keep their Phase 2
picture verdicts (Z Shockwave PARTIAL-STRONG, Cursed Tempest
PARTIAL) because Phase 3 changed plumbing, not compositions.

## What Phase 4 inherits

- The primitive geometry kit builds behind the three carrier
  renderers, whose geometry code this phase deliberately left
  byte-identical: wave band and rings, cloud dome shells and
  bolt, projectile casing and core, plus the named leftovers
  from earlier phases (the Z Shockwave raised torn ring at waist
  height, rockWall host-up tilt and orbit helper, flat plate
  builder, per-slab ground following, fracturePlates rim band,
  chunkier spike girth class).
- The projectile burst() flash decision: fold it into the
  ability recipe system or keep it as carrier infrastructure,
  decided with the Phase 4 carrier presentation pass.
- Recipes are ready for the kit to consume: the band, disc, and
  sphere_shell shapes already speak in carrier terms, so kit
  primitives can take recipe parameters without schema changes.
- Per-strike and per-hit seeds are already synced or derivable
  everywhere a Phase 4 primitive needs to agree across clients
  (cloud strike seed, wave tick seed, projectile hit seed).
