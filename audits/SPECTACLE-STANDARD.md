# Spectacle Standard (coordinating pass, Phase 1 output)

Written by the coordinating pass from the five fresh audits (fresh-audit-systems.md,
fresh-audit-blades.md, fresh-audit-dark.md, fresh-audit-heavy.md, fresh-audit-technique.md)
at HEAD 94708b6. Every Phase 2 weapon agent builds to this standard and uses the shared
blocks below. The theme of each weapon (colors, identity, character) does NOT change.
What changes is density, depth, lifecycle, world interaction, and honesty between the
visual and the numbers.

## The five points, as acceptance criteria

1. LAYERED DEPTH. Every area effect, storm, beam, and burst is built in three layers:
   a dense bright core, a mid layer that carries the theme color, and a wispy outer
   layer that fades to nothing. Layers rotate or flow at different speeds. Renderer
   geometry scales with camera distance (see LayeredRender.distScale): segment counts
   and alpha step down beyond 32 and 64 blocks so distance costs less and near views
   stay rich. A single flat mesh or a single ring never passes on its own.
2. DEBRIS. Signature moves throw individual debris pieces (DebrisEntity) that tumble
   on helical orbit paths: at least 24 pieces for a finisher-class move, 60 or more
   for a domain or storm-class move, pieces of at least 3 different sizes, sampled
   from the ground the move is standing on where the theme allows. Debris rises,
   orbits, then rains back down. It never vanishes mid-air.
3. WORLD INTERACTION. Big moves touch the world: a dust burst at the base on cast,
   temporary ground scarring through GroundScar (restored, crash safe) for elemental
   and heavy themes, and real mob forces through VortexPhysics where the theme is
   pull, lift, or fling. Permanent block destruction stays rare: heaviest finishers
   only, weak blocks only, with drops, never protected blocks (bedrock, spawner,
   chest, barrel and friends, enforced inside GroundScar).
4. ATMOSPHERE. Weapons that already have weather or darkness themes (Azure storm,
   Curse tempest, Dabura and Curse domains, Bansho wind) drive AtmosphereServer:
   real server weather (rain or thunder) for the move's life plus a client gloom
   grade through AtmosphereClient for nearby players, both fading with the move's
   envelope. Weapons with no weather theme do not gain random weather; their
   atmosphere is light and particles (BoltEntity strikes, glow layers).
5. LIFECYCLE. Every effect runs the shared envelope (FxEnvelope): build-up over the
   first 15 to 25 percent of life, peak at full density, dissipation over the last
   25 to 40 percent. Nothing pops in at full alpha and nothing discards at visible
   alpha. Damage lands INSIDE the visual's active window, spread across it for
   sustained effects, never all on tick 1 of a long visual and never after the
   visual has ended. Telegraphs are honest: the warning's size equals the hit's size.

## Numbers guidance (damage, cooldowns, timing are free game)

- A finisher's cooldown is at least its visual lifetime; a domain or storm cooldown
  exceeds the effect's life so two cannot stack unless stacking is the design.
- Charge scaling is monotonic and pays: a full charge is worth at least 2x a tap on
  the headline number (damage, radius, or duration).
- Sustained effects tick damage at a stated interval with the total printed in the
  report; instant finishers may stay instant but their visual must build before the
  hit lands (telegraph first, damage at the visual's peak tick).
- Every number change is reported before and after. Ki costs are charged only when
  the move actually fires; a blocked or cancelled move refunds or never charges.

## Shared building blocks (coordinating pass owns these files)

- util/FxEnvelope.java: envelope(age, life, buildFrac, decayFrac) returns 0..1;
  smoothstep helpers; pulse(age, speed). Use it in entities (particle counts,
  damage windows) and renderers (alpha, scale) so both sides agree.
- util/LayeredParticles.java (server): emitVortex(level, center, radius, height,
  age, life, dustColor) emits core, mid, and outer particle layers scaled by
  DbaEffectsConfig.effectDensity; emitGroundBurst for base dust; emitRain for
  dissipation drizzle of theme particles.
- entity/DebrisEntity.java + util/DebrisSpawner.java: spawnField(level, center,
  count, radius, rise, lifeTicks, palette) samples real ground blocks near the
  center (or takes an explicit BlockState palette), pieces orbit helically, then
  fall and fade on landing. Renderer draws the real block model, tumbling.
- util/VortexPhysics.java: applyVortex(level, center, radius, height, strength,
  swirl, lift) per tick: inward pull plus tangential swirl plus core lift, scaled
  by the same weight classes MobPhysics uses (bosses barely move). fling(entity,
  dir, power, up) for ejection beats.
- util/GroundScar.java: scar(level, pos, state, restoreTicks) and scarDisk(level,
  center, radius, state, restoreTicks, chance). Records originals in SavedData,
  restores on schedule, restores overdue entries on server start (crash safe).
  Refuses protected blocks and non-air-replacing rules match LavaSwordGround's.
- entity/BoltEntity.java: branched lightning from a synced seed: trunk plus 2 to 4
  forks, weapon color, 6 to 10 tick life with envelope flicker, ground glow at the
  strike point. Spawn it where a weapon's lightning should read as a real strike.
- network/AtmospherePayload.java + util/AtmosphereServer.java +
  client/fx/AtmosphereClient.java: AtmosphereServer.begin(level, center, radius,
  kind, ticks) sets weather (RAIN or THUNDER) with saved prior state restored at
  the end, and broadcasts gloom to players in radius. Client lerps the gloom grade
  in and out; no hard cuts.
- client/render/ki/LayeredRender.java: drawVortexBands (three counter-rotating
  bands with gradient alpha), drawLayeredDome (outer shell, inner core shell,
  rim glow), drawAnchorGlow (additive ground disc), distScale(cameraDistance,
  baseSegments) and the standard band alphas (core 0.85, mid 0.45, outer 0.18)
  multiplied by the envelope.

## Known platform limit (do not fight it)

KiRenderHelper.kiAdditiveType() returns the same translucent emissive type as
kiRenderType() on purpose: the true additive eyes shader ignores vertex alpha, so
fades would break. Glow in this mod is layered translucent copies with rising alpha
toward the core. Keep that technique; do not switch render types globally.

## Coordination rules

- A weapon agent edits ONLY its weapon's files: its item class, its own entity
  classes, its own renderer classes, and its per-weapon report. Shared or common
  files (anything under util/, fx/, network/, movement/, config/, the registries,
  the client init, KiRenderHelper, LayeredRender, other weapons' files) are read
  only. A needed shared change goes in the agent's report for the coordinating
  pass to make once.
- Shared entities: HollowAfterimage belongs to the Hollow's Edge agent. Katana and
  Saber agents change only their own spawn parameters. SlamDebris stays as is;
  new debris work uses DebrisSpawner. BanshoShockwaveEntity belongs to the Bansho
  agent even though a mixin also constructs it; keep its constructors compatible.
- Agents do not commit. The coordinating pass merges, sweeps, and commits.
- Each agent writes audits/spectacle-report-<weapon>.md with: before and after
  numbers for every changed damage, cooldown, timing, and Ki value; what was added
  per point (1 to 5); a self audit of its own diff; and any shared change requests.
- If a rebuild makes the weapon look or play worse, that is a fail: revert the
  weapon's files and say so in the report. An honest revert beats a worse weapon.
- No em dashes in code comments, reports, or commit messages.
