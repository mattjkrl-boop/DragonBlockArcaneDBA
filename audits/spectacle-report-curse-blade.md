# Spectacle Report: Curse Blade (Cursed Blade)

Phase 2 weapon pass, built to audits/SPECTACLE-STANDARD.md against the before
picture in audits/fresh-audit-dark.md section 2. Theme unchanged: cursed
chains, sky cracks, doom bolts, crimson and violet.

## State found (previous agent's partial work)

A previous agent on this task died when the runtime restarted, mid work. Its
edits were already in the tree and were audited line by line before anything
else was touched:

- item/CurseBladeItem.java: substantially rebuilt (AtmosphereServer weather,
  VortexPhysics wind, BoltEntity strikes, Tempest Verdict release finisher,
  GroundScar scorching, DebrisSpawner calls, ambient bolt rebalance, slam
  raise, cooldown raise, disarm fix). Coherent and kept, with one real bug
  found and fixed (see Bugs).
- entity/CurseChainEntity.java: attach damage 35 to 75, attach spark burst,
  heal raised. Kept.
- entity/AbyssalDomainEntity.java and client/render/AbyssalDomainRenderer.java:
  synced 30 tick dissipation fade, LayeredParticles vortex and rain, renderer
  lifecycle envelope on every alpha, shared LayeredRender vortex bands and
  anchor glow. Verified against the shared APIs and kept as is.
- entity/SkyCracksEntity.java and client/render/SkyCracksRenderer.java: the
  dead sky cracks made real (synced seed, caster following, visual bolt
  shedding, branching layered crack renderer on the FxEnvelope). Kept as is.

What the previous agent had NOT done, and this pass completed: the chain
renderer's three dead helpers, the chain's physics honesty (see below), the
CurseLightning entity/renderer lifetime mismatch, and two dead helpers in
CurseLightningRenderer.

## Before and after numbers

Chains (left click):

| Value | Before | After |
|---|---|---|
| Melee base damage | 750 | 750 (unchanged) |
| Attack speed modifier | -2.0 | -2.0 (unchanged) |
| Chain stream cooldown | 5 ticks | 5 ticks (unchanged) |
| Chain Ki drain | 30.0 discrete over 4 ticks per shot | unchanged |
| Chain attach damage | 35 | 75 |
| Chain attach heal | 2.0 + amp x 1.5 (max 15.5) | 4.0 + amp x 2.5 (max 26.5) |
| Chain attach life | 100 ticks | 100 ticks (ATTACH_LIFE_TICKS, remaining now synced) |
| Friction/drag weight while bound | applied once at attach, silently expired after 20 ticks (timed modifier) | refreshed every 10 ticks at the current amplifier for the whole 100 tick bind |
| Pull at amp 4+ | one 0.7 impulse on the attach tick only | same impulse, plus a sustained reel-in for the whole bind: 0.05 x weightScale per tick toward the owner, toward-speed capped at 0.30, stops within 1.5 blocks |
| Chain rendering | links and collar only, constant alpha, pop in and out | links and collar plus hex core, 4 runic glyphs, helical aura ribbons; fade in over 4 ticks, fade out over the bind's last 12 ticks; link segments distance scaled (12 down to 6) |

Cursed Tempest (right click channel):

| Value | Before | After |
|---|---|---|
| Channel cap | 300 ticks | 300 ticks (unchanged) |
| Domain radius | 10 to 30 | unchanged |
| Tempest Ki drain | 30 percent per minute per tick | unchanged |
| Server weather | none | AtmosphereServer THUNDER for the channel's life, gloom 0.45 rising to 0.90 with storm ratio, refreshed every 20 ticks in 70 tick grants, prior weather saved and restored by the shared system |
| Storm wind | random jitter: 0.6 chance per 5 ticks, up to plus or minus 0.75 horizontal and +0.6 up | VortexPhysics.applyVortex every tick: pull 0.055, swirl 1.5, lift 0.06, strength enveloped over the first 60 ticks, weight classed |
| Disarm | 5 percent per 20 ticks, pickup delay 40 ticks | 10 percent per 20 ticks, pickup delay 100 ticks |
| Telegraphed strike damage | 450 + Str x 1.5 | unchanged |
| Telegraphed strike visual | CurseLightningEntity picture, fixed 16 block height | BoltEntity, length dome height + 6 (16 to 30 blocks), life 8 ticks, seeded branching, crimson or violet (15 percent) |
| Telegraphed strike ground | one FIRE block | FIRE block (unchanged), plus GroundScar scorch disk radius 1 at 70 percent (BLACKSTONE, restore 160 ticks), 12 debris pieces, ground burst |
| Ambient strike rate | 0.12 per tick | unchanged |
| Ambient strike placement | uniform random in the domain | 45 percent land within 2 blocks of a random enemy in the storm, rest uniform |
| Ambient strike damage | 15 | 120 + Str x 0.35 |
| Ambient strike box | 3.0 blocks | 3.5 blocks |
| Ambient strike ignite | 5 s | 3 s |
| Ambient strike ground | nothing | scorch 1 block (restore 120 ticks) plus ground burst; loose FIRE stays exclusive to the telegraphed strike so identical bolts scorch alike without untelegraphed fire spread |
| Sky cracks | entity at fixed y 280, renders nothing, does nothing, life 100 ticks | follows the caster at min(280, caster y + 55), synced seed, branching crack network renderer (dark disc, crimson sheath, white core, violet forks) on the FxEnvelope with flicker, sheds a visual BoltEntity (length 34) every 20 ticks |
| Cast beat | none | ground burst at the caster on tick 0 to 1 |
| Peak beat at 300 ticks | none | 60 piece DebrisSpawner field (storm class count) plus ground burst |
| Slam phase damage (held 300+) | 150 + Str x 0.8 every 20 ticks | 220 + Str x 1.0 every 20 ticks, plus per slam: 12 debris pieces, scorch disk radius 2 at 50 percent (restore 200 ticks), ground burst |
| Release before this pass | nothing at all, storm just ended | Tempest Verdict finisher from 40 ticks held: damage 150 + 650 x ratio + Str x (0.8 + 1.7 x ratio), about 237 + Str x 1.03 at the 40 tick minimum and 800 + Str x 2.5 at full charge (about 3.4x on the base, monotonic); plus ignite 4 s, curse amplifier +3 (capped at 9), fling 0.6 + 1.2 x ratio with up 0.5 + 0.5 x ratio, a BoltEntity on up to 6 victims, 24 to 60 debris pieces, scorch disk up to radius 8 at 45 percent (restore 240 ticks) |
| Release cooldown | 40 + ratio x 60 (40 to 100 ticks) | 100 + ratio x 100 (100 to 200 ticks), always longer than the domain's 30 tick dissipation, so storms cannot stack |
| Domain particles | about 28 per second flat (5 VOID_SMOKE per 4 ticks, 2 KI_MOTE per 12 ticks) | LayeredParticles.emitVortex three layer emitter every 2 ticks scaled by effectDensity and envelope, plus emitRain through the dissipation |
| Domain end | entity discarded the tick the channel stopped (pop out) | synced 30 tick collapse: renderer envelope falls with it, particles wind down, rain falls |

CurseLightning (legacy strike type, no longer spawned by the item):

| Value | Before | After |
|---|---|---|
| Entity life | 20 ticks | 10 ticks (LIFE_TICKS), exactly the renderer's visible flash |
| Renderer draw window | stopped at flashTime 12, fade done at 9.5, last 8 ticks invisible | early out at LIFE_TICKS, fade reaches zero at 9.5, picture and lifetime agree |
| Renderer age source | hardcoded 20 in extractRenderState | CurseLightningEntity.LIFE_TICKS |

Ki summary: no Ki values were added or raised. Chain drain and tempest drain
are unchanged; the Verdict costs no extra Ki because it is the release of a
channel already paid for per tick.

## What was added, per point

1. Layered depth. The domain entity now emits through LayeredParticles
   (core, mid, outer) instead of flat void smoke, and the domain renderer adds
   the shared LayeredRender vortex bands and anchor glow on top of its own
   two dome shells, ground bands, and vortex column. The chain gained its
   promised layers: solid links (core), four runic glyphs (mid), helical aura
   ribbons (outer, wispy), and the hex curse core (bright center). Sky cracks
   render in tiers: dark tear disc, crimson sheath, white hot core, violet
   forks. Distance scaling: domain renderer already passes distScale into the
   shared bands; the cracks renderer scales disc segments by distScale; the
   chain renderer now scales link segments by distScale.
2. Debris. All through DebrisSpawner with ground sampled palettes: 60 pieces
   at the full charge peak (storm class), 24 to 60 on the Verdict release,
   12 per telegraphed strike, 12 per slam. Pieces orbit, release, rain down,
   and fade on landing per the shared DebrisEntity.
3. World interaction. The storm wind is a real vortex (VortexPhysics) instead
   of random jitter. Chains physically bind: friction and drag now last the
   whole attach, and heavily cursed targets are reeled in continuously.
   GroundScar scorching at strikes, slams, and the Verdict (temporary,
   restored, crash safe, protected blocks refused by the shared system).
   Ground bursts at cast, strike, slam, and release.
4. Atmosphere. The tempest is one of the weapons the standard names for real
   weather: AtmosphereServer drives server THUNDER plus client gloom for the
   channel's life. All strikes are BoltEntity (seeded, branched, dome height)
   instead of a fixed 16 block picture. The sky cracks make the torn sky the
   audit found missing.
5. Lifecycle. The domain builds (radius growth, 40 tick renderer build
   envelope, vortex strength envelope over 60 ticks), peaks (slam phase,
   peak debris beat), and dissipates over a synced 30 tick collapse with
   rain, instead of vanishing the tick the channel ends. Early release now
   pays a monotonic finisher (the Verdict) instead of nothing. Damage lands
   inside visual windows: strikes damage on the tick their BoltEntity spawns
   after a 20 tick telegraph; chains fade in and out on the envelope;
   CurseLightning's entity and renderer lifetimes now agree exactly.

## Bugs fixed

- SkyCracksEntity plus SkyCracksRenderer were dead weight: spawned every 80
  ticks, rendered nothing, did nothing. Now a real, visible system (previous
  agent's fix, verified here).
- Ambient doom bolts dealt 15 next to the telegraphed 450 + Str x 1.5, a 30
  to 1 gap that made the storm's main visual pointless. Now 120 + Str x 0.35
  (about a quarter of a telegraphed strike at equal Strength) with hunting
  placement (previous agent's fix, verified here).
- BROKEN SCORCH (found in this pass, left by the previous agent):
  scorchGround placed Blocks.BLACK_CONCRETE, which does not exist in
  Minecraft 26.2 or 26.3 (the color block fields are gone from Blocks), so
  the item did not compile. Changed to Blocks.BLACKSTONE, present in both
  versions and the right scorched earth read. This was the only compile
  error in the weapon's files.
- Chain friction and drag silently expired 20 ticks into the 100 tick bind:
  DbaPhysicsAttributes.applyModifier is a 20 tick timed modifier and nothing
  refreshed it, so for 80 percent of every bind the chain showed a grounding
  it was not applying. Refreshed every 10 ticks at the current amplifier.
- CurseLightningRenderer stopped drawing at tick 12 while the entity lived
  20 (and extractRenderState hardcoded the 20). Entity life is now 10,
  matching the fade, and both sides read the one LIFE_TICKS constant.
- Fire behavior inconsistency between strike types: both now scorch the
  ground the same way through GroundScar; loose block FIRE remains only on
  the telegraphed strike, which is aimed and warned, so untelegraphed bolts
  cannot spread fire into builds.
- Disarm undid itself: pickup delay 40 let the enemy re-grab at once. Now
  100 ticks, and the chance doubled to 10 percent so it reads.
- Tempest dissipation pop and worthless early release: fixed by the domain
  fade and the Verdict (above).
- Dead code: CurseChainRenderer's drawFloatingRunicGlyph, drawAuraRibbons,
  and drawHexCore were defined and never called (the class comment promised
  all three); they are wired in now. CurseLightningRenderer's
  renderBoltSegmentTextured and renderBeam were defined and never called;
  removed (about 50 lines).
- Sky cracks spawned at a fixed y of 280, unreachable in low ceiling
  dimensions; they now follow the caster at y + 55, capped at 280.
- Five pre-existing em dashes in this weapon's class comments were replaced,
  per the no em dash rule.

## Shared change requests

- CURSE_LIGHTNING (DbaEntities.java registration and the renderer
  registration in DragonBlockArcaneDBAClient.java): nothing spawns this type
  anymore; the tempest's strikes are BoltEntity. The entity and renderer are
  internally consistent now, so keeping the registration is harmless, but
  retiring the type is a coordinating pass decision.
- AbyssalDomainRenderer still carries an unused private renderBeam helper,
  the same dead helper pattern cleaned up in CurseLightningRenderer. It sits
  in the renderer family duplicated with Dabura's domain, so it was left for
  the coordinating pass rather than edited piecemeal here.
- DbaPhysicsAttributes.applyModifier's silent 20 tick default lifespan is a
  trap for any long duration bind or stance effect; the chain now refreshes
  around it, but a documented duration taking overload as the obvious choice
  would keep the next weapon from repeating the bug.

## Honest worse check

- The Verdict also fires when the channel ends because Ki ran out
  (stopUsingItem routes through releaseUsing). Judged a feature, not a
  regression: a release is a release, and the finisher is scaled by charge
  gathered either way. Noted here so it is a decision, not a surprise.
- The chain reel-in is new continuous force where there was one impulse. It
  is capped (toward speed 0.30), weight classed through
  VortexPhysics.weightScale (bosses at 0.12 barely move), and stops within
  1.5 blocks of the owner, so it reads as a drag. If playtesting shows it
  pinning targets against the caster too hard, the 0.05 step is the one
  number to lower.
- Sky cracks bolts are visual only by design: the item owns all lightning
  damage, so the cracks never double dip. Their job is the sky picture plus
  extra strike flashes inside the storm, which is what the audit asked the
  entity to become.
- Nothing was reverted. No file outside this weapon's set was edited.

## Verification

- Targeted javac audit (the standing audit method, not a Gradle build; the
  build stays waived per Carter): all 13 Curse Blade files compiled against
  both audit classpaths. 26.2: zero errors in this weapon's files (4 errors
  remain in the pulled-in tree, all in shared files DbaItems.java and
  DbaMenus.java, pre-existing baseline). 26.3: zero errors in this weapon's
  files (11 errors in the pulled-in tree, all in DbaItems.java, DbaMenus.java,
  DbaHostileEntity.java, FriezaSoldierEntity.java, NamekianWarriorEntity.java,
  PatrolRobotEntity.java, none of them this weapon's).
- Shared API signatures verified against the real shared sources before
  relying on them: AtmosphereServer.begin (radius, gloom, kind, ticks),
  BoltEntity constructor, DebrisSpawner.spawnField overloads,
  VortexPhysics.applyVortex and weightScale, LayeredParticles emitVortex and
  emitRain, LayeredRender drawVortexBands, drawAnchorGlow, and distScale,
  FxEnvelope envelope, smoothstep, and segments, GroundScar.scar.
- Blocks.BLACKSTONE verified present in the 26.3 jar via javap, and the
  26.2 sweep confirms it compiles there; Blocks.BLACK_CONCRETE verified
  absent in both.
