# Movement and Mob Follow-up Before Audit (Nov 1, 2026)

Carter asked for the two deferred follow-up groups named after the feature wave: the movement stuff and the mob stuff.

## Movement current state

- movement/MovementPhysics owns only the three generic verbs (dash, Ki step, fast fall), their per-player air-use state, and landing impacts. Its velocity application is private, so abilities cannot reuse it yet.
- Weapon rushes still execute movement locally:
  - Azure Dragon Rush blends toward a target flight velocity by hand and zeroes velocity when immobilized.
  - Brave Sword, Evil Spear Hell Hunt, Hollows Edge, Katana, Saber Blitz and Flash Step, and Dimensional Sword all call player.teleportTo directly in one of two overloads, with rotation preserved locally at each call site.
  - Evil Spear's no-target Hell Hunt sets a lunge velocity directly.
  - Saber Blitz release sets a short look velocity directly.
- KiTechniqueHandler contains no player movement verbs. Ki techniques fire projectiles and effects; there is no Ki technique teleport, dash, or flight path to migrate. Projectile velocities belong to their entities and stay there.
- Flight feel is split: Azure Dragon Rush has its own acceleration blend (half target from rest, then 40 percent blend), Flying Nimbus travel sets velocity instantly and damps after moving, and the client animation state already captures yaw velocity and local velocity for every race and form but the fly animation applies no banking.
- Fall damage has one special case (Azure rush immunity in LivingEntityMixin). Ki step and fast fall have no interplay: a Ki step can add fall height with no credit, and a deliberate fast fall slam takes full vanilla fall damage on top of its landing feedback.

Plan: extend MovementPhysics with public set, add, flight blend, and rotation-preserving teleport helpers, then move every player rush execution listed above onto those helpers without changing distances, damage, costs, or cooldowns. Use the same pure flight blend for Azure and the Nimbus. Compute one flight bank value in the shared player render state from turn rate and lateral velocity, and apply it in the one fly animation branch so every race and form banks the same way. Add a MovementPhysics fall-damage scale (fast fall 0.35, Ki step 0.65) and consume it in the existing fall branch of LivingEntityMixin.

## Mob current state

- MobPhysics already owns weight classes, launch, juggle, ground bounce, wall splat, death tumble, and finisher feedback.
- Slam debris is not generalized. OxFissureEntity emits only its own ember ambience, ZGravitySlamEntity and KingsSlamEntity have no server tick debris at all, and OxShockwaveEntity has a separate weak-block destruction particle path. The three slam patterns named in the Phase 6 remainder share no code.
- Bosses resist launches by design (BOSS scale 0.12), and there is no stagger response at all: a heavy DBA hit on a boss-class target moves it slightly and nothing else happens. There is no stagger payload, client store, or render-state channel; the tumble channel only covers deaths.

Plan: add one server SlamDebris helper (surface-aware block debris sampled from the actual ground block under each point, plus a per-slam accent particle and dust, density-scaled, no block destruction) and call it once on the first server tick of OxFissureEntity, ZGravitySlamEntity, and KingsSlamEntity with each slam's own radius and profile. Add a boss stagger path to MobPhysics: BOSS-class non-player targets taking a DBA hit of 18 or more get a brief slowness and a BossStaggerPayload (30 tick server cooldown per boss), rendered client side as a decaying rock and dip through a DbaStaggerState channel that mirrors the tumble channel.
