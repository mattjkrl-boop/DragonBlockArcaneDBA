# Design audit, Nov 5, 2026 (Grok)

Every bound ability JSON (44 attacks plus their summon and variant defs, 55 files) read against brain mods/DBA-ATTACK-DESIGNS.md (a2a774c) and the 9 cards in mods/dba-references/cool/. Mechanic numbers checked from data; code paths spot-checked where the number lives in Java. In-game look is unverified until Carter's PC test.

## Mismatches found and fixed
- Z Sword Katchin Weight: the design says the slam covers 12 to 36 blocks. Data grew radius 6 to 9 and the finale scaled 1.6 (14.4 radius, about 29 wide), with fissures limited to radius 9. Now radius 6 to 11.25 (12 wide growing) and the finale 1.6 gives radius 18 (36 wide); the terrain radius is 18 and the cap is 480. Separate commit.
- Z Sword Katchin Weight: the sword tip rain (691a142) is not in the design; removed (e560b54). The rain_sword_tip model assets stay in the repo unused.
- Hollow's Edge Hollow Rush: the design says chained dashes with an afterimage; afterimages were fx only. It now leaves the katana_afterimage model at each hop (step trigger). Separate commit.

## Confirmed already matching (done earlier, verified this pass)
- Devil Trident Prong Shot: 750 flat, Ki 60 drained discretely over 10 ticks (kiDrainTicks, c75a25e), cooldown 10.
- War Mace Crater Smash: wave self-launch goes through MovementPhysics.setVelocity so the client sees it (b565933); mace_smash and mace_falling_smash both use it.
- Numbers matching the record: Flashdraw 5 chained targets; Heroic Dash 8 to 24; Assault 10-hit finisher; Flash Step 3 charges; Grand Cyclone deflect -1.4x with ownership (AbilityService aura deflect sets owner); Valor Field freeze at 0; Tempest and Cursed Tempest 10 to 30 wide; Eclipse 8 to 30 wide, 0.05x projectile slow, 15 second channel (300 ticks); Curse Chain 10 links; Void Rift from radius 2.5; Warp Rift 1024; Devil deploy 750 radius 6 to 40 percent on tick 8, 10 shards, 100 plus 0.5 Strength, 15000 budget, intercept 12, assign 40, drain 20 percent per minute, recall 600, lockout 20, starve 3600; Sorrow Slash 750 plus 3.0 Strength with lifesteal and Melting; Dimensional Rift 15 wide; Ox strike 10 second charge to 24 block shockwave with fissures at max; Ox Brace 12 block repulsion and the 14 to 15 second window; Wind Spin 25 reach, 35 degrees, 2.2 deflect; Pole extension 30; Whis melee 350 plus 2.0 Dexterity; Temporal Rift 12 radius, 3 second freeze, 5 second rewind; Tempest Barrage 5 shots and 2 second cooldown; Blaster one shot per 3 ticks with overcharge; Erasure 48 block beam; Spirit Cannon drains 5 percent per second (from full Ki that is exactly the 400 tick cap, so it runs while Ki lasts).

## Still PARTIAL (open, presentation only, no mechanic missing)
Blitz Flurry line-up finisher, Flash Step tall rifts, Assault cross slash shape, Curse Chain and Hell Hunt client-drawn chain sets, Void Rift fragment ring, Dimensional Slash tilt alternation (not a data parameter), Warp Rift ring and "destination larger" (single destination cloud), Prong Shot plain projectile, Sickle slash trail, Ox Brace titan scale, Wind Spin reach approximation, Tempest Barrage palette, Blaster energy bolts, Lava Cleave fountains, Spike Flail drawn chain, Rising Blow, Bansho Great Cyclone 64-piece debris field (particles, not 64 physical pieces).

## Hit-stop
Kept deferred. The prompt allows only a pose-clock-only freeze behind an opt-in field, off by default; with it off nothing would change on Carter's test, so it was not built this pass.
