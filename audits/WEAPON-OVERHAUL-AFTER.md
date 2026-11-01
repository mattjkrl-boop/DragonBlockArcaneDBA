# Weapon Ability Overhaul: After (Nov 1, 2026)

Scope: the parts the phase 1 visual overhaul never touched: abilities, beams,
projectiles, damage and cooldown numbers where the per-weapon audits showed them
broken, stubbed, or defeated. Every weapon was audited first (audits/
weapon-audit-group-a.md through group-d.md), then changed one at a time, and each
weapon was verified before the next one started: dual javac sweeps against the
26.2 and 26.3 classpaths, requiring zero new error kinds against the pre-pass
baseline. Final state: zero error kinds unique to 26.2, and the only kind unique
to 26.3 is the gated-off 26.2 cloud mixin calling the removed render method
(it never loads on 26.3). This is an audit result, not a Gradle build; the build
stays waived per BUILD-VERIFY.md.

## Katana
- Flashdraw charge now scales damage (0.55x tap to 1.3x full) and freeform dash
  distance (8 to 14 blocks); delayed strike guards dead, removed, and
  dimension-changed targets; first chain target gets DbaImpactFx.medium plus a
  burst; freeform dash leaves a SwiftCrescent trail and sweep sound; Heaven
  Splitter fires DbaImpactFx.heavy at the first target hit.

## Z Sword
- ZShockwaveEntity travel particles (SPARK main wave, EMBER sub-waves) and a sweep
  sound every 15 ticks; max-charge root-and-crush got a SlamDebris Z_GRAVITY burst,
  DbaImpactFx.medium, and an anvil crunch; stance released before 10 ticks gives a
  sound and action-bar feedback instead of silently wasting drain.

## Saber
- Headline bug: BlitzSequence.totalHits was initialized to 0 and read at 4 sites
  but never incremented, so the escalating speed buff and strong-hit logic were
  dead. It increments on arrival now and the buff actually escalates (6.6 toward
  the 24.0 cap). The immobilized abort path removes the 999999-tick Movement Curse
  and Cinematic Tracking from all chained targets, clears the speed buff, and
  messages the player instead of silently deleting the sequence. Finisher snap
  fires DbaImpactFx.heavy at the centroid.

## Brave Sword
- Finisher cross slash is splash-only now (BraveCrossSlashEntity excludes the
  primary target at half damage), ending the full-damage double-hit. Combo decays
  after 100 ticks without a landed hit. Assault drain only runs when a target is
  actually in the strike box instead of draining at full rate into empty air.

## Grand Sword
- Cyclone spin deals the header-promised contact damage every 3 ticks (100 plus
  Strength x 0.5, outward knockback) plus a SPARK blade ring. The dead
  MAX_LEFT_SPIN_TICKS constant is wired in: release finisher and wave damage scale
  with spin ratio. ValorFieldEntity got a spawn sound and SPARK feedback. Dead
  imports removed.

## Azure Dragon Sword
- Melee had no on-hit effect at all: hurtEnemy added (WIND particles, light gust
  push, small Ki sip). Rush pass-through hits get a sweep sound and SPARK burst
  gated on fresh full hits. Tempest lock-on is validated at release (dead or over
  64 blocks means the storm follows the player) and shows an action-bar message
  and sound when set. AzureStormEntity.isMaxCharged is a synced, persisted data
  field now (was a plain field lost on relog). Dead imports removed.

## Dabura Sword
- hurtEnemy applies Petrification Curse for 40 ticks (the petrification sword's
  basic hit applied nothing). Full-charge bonus waves are staggered by projectile
  speed instead of a fake server.execute delay that fired both on the next tick.
  Releasing the channel at 200 of 300 ticks or more fires a partial eclipse at
  0.6 x charge scaling instead of paying out zero. Domain entity initial lifetime
  aligned with its refresh lifetime.

## Curse Blade (class CurseBladeItem)
- The right-click finisher was renamed in comments from "Abyssal Eclipse" to
  "Cursed Tempest" (the old name collided with Dabura's finisher). Chain streaming
  throttled with a 5-tick item cooldown (was near-free click spam to 10 stacks).
  Targeted lightning scales (450 plus Strength x 1.5) with DbaImpactFx.medium;
  max-charge slam scales (150 plus Strength x 0.8) with one heavy per batch.
  Ambient domain lightning strikes inside the actual domain radius and no longer
  places world fire blocks.

## Hollow's Edge
- hurtEnemy added (Dark Faded 40 ticks plus Ki drain; the highest base damage in
  its group had no on-hit effect). Out-of-window fallback blink costs Ki and has
  a 10-tick cooldown (was free forever; in-window blinks stay free). Void Rift
  damage scales live during the channel and pull ticks take 5 percent of it;
  implosion fires DbaImpactFx.heavy; the rift applies DARK_FADED instead of the
  RIFTED buff. Swing profile tip changed to VOID_SMOKE.

## Dimensional Sword
- Warp origin rift burst cut from 750 to 250 (destination stays 750), so warping
  away no longer nukes everyone left behind at full slash damage. fireSlash got
  an 8-tick cooldown (was none, roughly 90 spammable 750-damage slashes per
  stamina bar) plus a fire sound. Destination warp fires DbaImpactFx.medium.

## Spirit Sword
- Disarm guarded: never against players or boss-class targets. SpiritImpaleEntity
  spawns are deduped per target (was a fresh 28-tick entity per hit at 3 attacks
  per second). Beam damage ramps over the first 40 ticks (0.6 / 0.85 / 1.0)
  instead of instant full power. SPARK terminus particles per beam hit; melee
  sound pitch jittered.

## Evil Spear
- Melee applies MARKED_BY_EVIL for 120 ticks (it could not set up its own Hell
  Hunt). The projectile got a block collision check (was noPhysics through walls
  for its full flight). Hell Hunt picks the nearest marked target and executes
  staggered, one step every 3 ticks, with the cooldown applied at hunt start and
  Demon Surge at completion. The no-target lunge registers a landing check that
  deals damage, marks, and spawns the impact entity (was no end state at all).

## Devil Trident
- fireLasers was free and spammable at 750 damage per click: it now costs Ki
  (60 percent per minute over 10 ticks), carries a 10-tick cooldown, plays a
  LIGHTNING_ZAP, and messages on insufficient Ki. Deploy slam damage falls off
  with distance (full within 2.5 blocks, down to 40 percent at the rim) and fires
  DbaImpactFx.heavy. Manual shard recall cooldown cut from 3600 to 600 ticks; the
  3600 stays for the forced Ki-starved recall. Shard damage scales (100 plus
  owner Strength x 0.5), the swarm health budget is spent in damage dealt, and
  hits play TRIDENT_HIT. Swarm auto-assignment only hunts Enemy mobs, never
  passive or friendly mobs on its own, and counts shards with the scaled damage.

## Sickle of Sorrow
- The melee impact burst (damage 0) re-applied Melting III, Sorrow Rift, the
  friction distortion, and a 0-damage Soul Rend to every bystander in its box: it
  is pure geometry now. Slash tilt alternation moved from one static boolean
  shared by all players to per-stack data. Both techniques play a swing sound at
  swing time. The flying wave dies on block contact (was through walls). The
  rift cooldown map is kept, because the technique-active callbacks in the main
  class call the rift directly and never pass through the item cooldown, but it
  is pruned on every call now instead of leaking forever. Rift burst targets get
  SPARK particles and a sound.

## Ox King's Axe
- The Flawless slam called DbaImpactFx.heavy twice (copy-paste slip): once now.
  The class header promised Resistance IV and Slowness IV; it names the effects
  the code actually applies (Ox Brace, Fissure Stun). The stance aura no longer
  pressures tamed pets (slams stay full AoE). Groundbreaker charge has a
  full-charge ready cue (sound plus action bar) and its release sounds scale in
  volume and pitch with charge ratio.

## Power Pole
- Whirlwind Staff re-damaged everything in the cone every 2 ticks while held:
  per-target hits are gated to once per 10 ticks, and the visual entity, WIND
  particles, and sounds run on a 6-tick cadence instead of spawning an entity
  every call. The poke's impact entity and heavy sounds only fire when the poke
  actually connects (was detonating in mid-air on a total miss). The 15 percent
  concussion roll now lasts 100 to 140 ticks at amplifier 1, so it can no longer
  end sooner than the guaranteed base stun. The misleading hurtEnemy comment was
  corrected (melee is a stamina cost only).

## Whis Staff
- Time reversal requests (200 and 300 ticks) exceeded the 100-entry position
  history: both are 100 ticks now, the documented buffer maximum. The swing Ki
  cost was charged twice for a landed hit (network handler plus hurtEnemy):
  hurtEnemy no longer drains; one charge per swing. The control package (reversal,
  stasis, drag and friction modifiers) no longer applies for non-player attackers.
  Temporal Rift fires DbaImpactFx.medium plus an ABILITY_CHARGE sound on cast.
  Auto-Dodge plays a gust sound on top of its dust burst. The class header was
  rewritten to match the code (Temporal Stasis, real reversal durations).

## Bansho Fan
- Cyclone Slash double-dipped: the item applied the full cyclone damage instantly
  in a 3-block AoE and the cyclone entity then shredded for 25 more ticks. The
  entity is the only damage source now. The Gale Force launch has a WIND gust
  burst. Tempest Barrage sounds play server side only in one category (was both
  sides, mixed categories), with WIND_GUST replacing ELYTRA_FLYING. The melee
  stamina double-charge is fixed the same way as Whis Staff. Header corrected
  (discrete costs, launch description, dead imports removed).

## Blaster Gun
- Bolts pierced solid blocks for their whole 72-block flight: they now die on
  block contact with a SPARK burst and fizzle sound (entity piercing unchanged).
  Erasure Cannon requires its full ammo cost (was firing at full damage while
  paying only what remained) and lands DbaImpactFx.heavy at the beam impact
  point. Heat has a downside: an overcharged shot vents with a 20-tick barrage
  pause. The beam's fat AABB candidate box got a real distance-to-segment test,
  so targets blocks off the beam line or behind cover no longer take beam hits.
  The charge loop layers a rising ABILITY_CHARGE over the beacon hum.

## Notes
- Four claims that arrived by chat relay during this pass (Devil Trident stub
  "Judgment" ability, Sickle "Reap the Fallen" abilities, Whis Staff "Angelic
  Judgement" and "Orbital Dominion" plus a multishot bug, Bansho sneak
  double-trigger, Blaster infinite energy cells) do not exist in the code and
  were verified absent by grep before work started. The written group audits
  governed; no action was taken on the relay claims.
