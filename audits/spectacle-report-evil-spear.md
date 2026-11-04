# Spectacle Report: Evil Spear

Phase 2 weapon agent report. Base commit 5e4b1de. Theme kept: hell hunt, the
thrown spear that drags its prey. Crimson and black demonic identity unchanged
in every color, sound, and name.

## State found (previous agent audit)

A previous agent on this weapon died mid-work from an infrastructure error.
Auditing its partial edits file by file against the fresh audit before picture
(audits/fresh-audit-heavy.md section 2), the edits were not half done: all
seven files carried coherent, finished implementations that matched the
standard, each change commented with its reasoning. I verified every shared
call signature against the shared sources (VortexPhysics.applyVortex,
LayeredParticles emitVortex, emitRain, emitGroundBurst, dust,
DebrisSpawner.spawnField, GroundScar.scar, LayeredRender.drawVortexBands and
drawAnchorGlow, FxEnvelope) and all of them match. One real hazard was found
and fixed by me (GroundScar mutable position, below). Nothing else needed
completion or repair.

## Files owned and edited

- item/EvilSpearItem.java
- entity/EvilSpearChargeEntity.java
- entity/EvilSpearProjectileEntity.java
- entity/HellHuntImpactEntity.java
- client/render/EvilSpearChargeRenderer.java
- client/render/EvilSpearProjectileRenderer.java
- client/render/HellHuntImpactRenderer.java

No shared or foreign files were edited.

## Before and after numbers

| Value | Before | After |
| --- | --- | --- |
| Throw (Impale) damage | 450 + 700 x charge + Str x 3 x charge (tap 520 + 0.3 Str, full 1150 + 3 Str) | Unchanged |
| Throw cooldown | 20 + 30 x charge ticks (23 to 50) | 40 + 20 x charge ticks (44 to 60), floor set by the projectile's 40 tick visual life |
| Throw charge time | 120 ticks | Unchanged |
| Projectile life | 40 ticks, discard on wall hit | 40 ticks; on a wall hit it bursts and, if its vortex is still open, lodges until the vortex closes (max about 10 extra ticks) |
| Projectile trail | EMBER 2 every 2 ticks (about 20 per flight) | Three strata per substep along each tick's movement segment: KI mote core, theme dust mid, ember wisps, density scaled, fading over the last 8 ticks |
| On-hit pull | Instant tug 0.8 horizontal and +0.2 up on the pierced target, plus an outward push 1.2 and +0.2 on everything in the target's box inflated by 3.5, all in one tick | Removed. First pierce opens a real vortex at the impact point: VortexPhysics radius 6.0, height 4.0, strength 0.5, swirl 0.9, lift 0.30, 10 ticks, envelope scaled, weight class scaled so bosses barely move |
| Hell Hunt step spacing | 3 ticks (full 8 chain in 21 ticks) | 6 ticks (full 8 chain in 42 ticks), each 35 to 50 tick impact reads before the next lands |
| Hell Hunt cooldown | 100 ticks | 160 ticks, covers the full chain plus the final impact's 50 tick life |
| Hell Hunt step damage | 350 + Str x 2 per step, x (1 + 0.15 x step); final 400 + chains x 150 + Str x 3.5 | Unchanged |
| Hell Hunt Ki cost | drainKiDiscrete 45.0 per 40 ticks, charged even when the hunt was then blocked as immobilized | Same amount, now charged only after the immobilized check passes, so a blocked hunt costs nothing |
| Lunge (no target) damage and cooldown | 200 + Str x 1, cooldown 40 | Unchanged |
| Impact life | 35 ticks for every impact | 35 normal, 50 final, entity and renderer agree through the shared LIFE_NORMAL and LIFE_FINAL constants |
| Impact particles | EMBER about 3 every 2 ticks, first 14 ticks only | LayeredParticles.emitVortex every tick for the whole life, plus an ember emitRain drizzle past 60 percent of life |
| Impact debris | None | DebrisSpawner field: 10 pieces normal, 24 pieces final (density scaled), hell palette (obsidian, blackstone, crimson nylium, netherrack), 26 orbit ticks, then pieces rain down and fade on landing |
| Impact ground | Drawn fissures only, zero block changes | GroundScar obsidian: disk radius 1 at 80 percent per cell restoring in 100 ticks (normal), radius 2 at 65 percent restoring in 160 ticks (final), each column followed to its real surface, protected blocks refused |
| Charge visual on release | Charge entity discarded the same tick the throw left | 6 tick dissipation (RELEASE_FADE_TICKS): renderer expands and fades on the synced release counter, entity rains the gathered energy back down |
| Charge visual on spawn | Popped in at alpha 0.65 or more on frame one | 6 tick smoothstep build-in |
| Charge particles | EMBER (2 + 4 x charge) every 2 ticks only | Three layers every 2 ticks: KI_MOTE core (1 + 2 x charge), EMBER mid (2 + 4 x charge), VOID_SMOKE outer (1 + 2 x charge), density scaled |

Ki values not listed (melee drain 45.0 per 8 ticks, charge drain 45.0 per
minute) are unchanged.

## Per point

1. Layered depth. Charge entity emits a three layer particle structure (bright
   mote core, ember mid band, void smoke rim) and its renderer adds the shared
   three band vortex column around the seal heart. The projectile lays a three
   strata trail along its flight segment and its renderer grew a third, wide
   and faint ribbon strand (outer wisp) beside the two dense strands. The
   impact emits the shared emitVortex layers for its whole life and its
   renderer adds the anchor glow and vortex bands. All three renderers step
   segment counts down with camera distance through LayeredRender.distScale
   and FxEnvelope.segments.
2. Debris. Every Hell Hunt impact (chain steps, projectile hits, lunge
   landings) throws a DebrisSpawner field of hell rock torn from the impact:
   10 pieces on a normal impact, 24 on the final step, at least 3 sizes via
   the spawner, helical orbit for 26 ticks, then rain back down. On release
   the charge rains its gathered embers; the impact rains embers while it
   dies. Nothing vanishes mid-air.
3. World interaction. The throw's first pierce opens a VortexPhysics vortex
   at the honest radius of 6.0 blocks for 10 ticks that pulls, swirls, and
   lifts everything nearby toward where the spear struck, replacing the old
   one tick tug plus contradictory outward push. Every impact scars the real
   ground with temporary obsidian through GroundScar where its renderer
   draws fissures and spikes. Ground bursts fire at the throw release, at a
   wall hit, and at every impact. A spear that pins into a wall while its
   vortex is open stays lodged until the pull finishes.
4. Atmosphere. This weapon has no weather or darkness theme, so per the
   standard it gains none. Its atmosphere is light and particles: ember rain,
   void smoke wisps, layered glow.
5. Lifecycle. Charge: 6 tick build-in, 6 tick release dissipation. Projectile:
   3 tick spawn fade, end fade over ticks 32 to 40 matched to the discard.
   Impact: FxEnvelope drives the entity particles and the renderer alpha from
   the same constants, with a settling rain after 60 percent of life and the
   obsidian scar outliving the visual (100 or 160 ticks) as the residue.
   The hunt chain itself was re-timed (3 to 6 tick steps) so eight impacts
   no longer stack into one field, and damage still lands on the step tick
   inside each impact's opening window.

## Bugs fixed

- Lock-on through walls: findLockOnTarget now requires
  player.hasLineOfSight(e), so the hunt can no longer lock onto and teleport
  the player onto a target behind terrain.
- Dead code removed: EvilSpearProjectileRenderer.drawBeam (defined, never
  called) is gone, and the write only PendingHunt.stack field is gone, along
  with the unused PlayerStatsAccessor local in performHellHunt.
- Ki charged for a blocked move: the immobilized check in use() now runs
  before the Ki drain.
- Disconnect asymmetry: onPlayerDisconnect now clears PENDING_HUNTS and
  PENDING_LUNGES as well as ACTIVE_CHARGE_MAP.
- Charge cut on release: the six second build-up no longer ends in a one
  frame cut (beginRelease dissipation, above).
- GroundScar mutable position hazard at my call site: scarGround passed its
  MutableBlockPos cursor straight into GroundScar.scar, which stores the
  position. I now pass m.immutable(), the same pattern BanshoCycloneEntity
  uses. Fixed in HellHuntImpactEntity during this audit pass.

## Shared change requests

- GroundScar.scar: the re-scar path (entry already exists) stores the
  caller's BlockPos reference in the new Entry, while the new-entry path
  copies with pos.immutable(). If any caller passes a MutableBlockPos, a
  re-scar of an already scarred cell can store a position the caller later
  mutates. Suggest the coordinating pass make the re-scar path use
  pos.immutable() as well. My own call site already defends itself, so the
  Evil Spear is safe either way.
- No other shared changes needed.

## Worse check

Nothing was reverted; nothing reads worse than the before picture. Honest
residuals, stated rather than hidden:

- A spear that pins into a wall late in a long flight can outlive its own
  body fade (ticks 32 to 40) by a few ticks while its vortex finishes. It is
  short (the vortex is 10 ticks) and rare (the pin must happen after a hit
  opened the vortex), and I left it rather than add a synced pinned flag for
  one weapon.
- Chain step targets are chosen at cast time and do not get a per step line
  of sight recheck. The required lock-on check is in; chain targets are by
  definition enemies the spear or the blade already marked, so this was a
  deliberate leave, not an oversight.
- Normal impacts throw 10 debris pieces, under the 24 piece finisher floor.
  Deliberate: a normal impact is a per-hit marker that can land several
  times a second, and 24 each would flood the field. The final step, the
  finisher, meets the floor at 24.

## Verification

Gradle does not run in this sandbox (standing waiver; javac sweeps are the
verification, never presented as a Gradle pass). Dual javac sweeps with
-Xmaxerrs 10000 against the 26.2 and 26.3 audit classpaths: baseline at
5e4b1de measured 132 error kinds (26.2) and 141 (26.3). Filtered to this
weapon's seven files, baseline kinds are 0 and current kinds are 0 on both
versions: zero new error kinds in any Evil Spear file. (Whole tree totals
moved to 134 and 149 because every weapon agent's files are in flight in the
same tree; none of the new kinds are in Evil Spear files.) The one line
immutable() edit landed after the current sweep; it replicates the exact
GroundScar call pattern at BanshoCycloneEntity line 231, verified by reading
that file. Zero em dashes in the added lines of all seven files. No commits
made; no git write commands run.
