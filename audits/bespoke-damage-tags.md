# Bespoke damage type tags (Nov 3, 2026)

Follow-up to the bespoke pass, closing item 5 of
audits/BESPOKE-FINAL-AUDIT.md. The 12 custom damage types shipped in
Phase 1 with no vanilla damage_type tag memberships, so converted
hits whose old source was magic, indirectMagic, generic, onFire,
hotFloor, or lightningBolt did not inherit bypasses_armor,
no_knockback, witch_resistant_to, or is_fire behavior. Carter
approved adding the tags. This file records the archaeology, the
per-family calls, and every compromise. Amounts, scaling, and
exhaustion are untouched; this is data only (six tag JSONs), no
Java changed.

## Method

Replaced sources were counted from git diff 57902e1..dc2cd1f (the
full bespoke pass, Phase 1 foundation plus Phase 2 merge), Java
files only. A "site" is one distinct hurt expression that a
conversion replaced; "tokens" are the individual damageSources()
branch occurrences inside those expressions at 57902e1, because
most ability expressions were attribution ternaries of the shape
"owner is a player ? playerAttack : (mob branch : magic or generic
fallback)". Pure sites (one source, no ternary) are called out
where they exist, because they carry the most weight: a pure site
shows what the hit simply was before the pass.

Vanilla tag memberships below were read from the 26.2 client jar
(data/minecraft/tags/damage_type/*.json), not from memory. Direct
memberships of the replaced sources:

- magic: bypasses_armor, no_knockback, witch_resistant_to,
  bypasses_wolf_armor, avoids_guardian_thorns,
  always_triggers_silverfish, panic_causes
- indirect_magic: bypasses_armor, witch_resistant_to,
  bypasses_wolf_armor, panic_causes (NOT no_knockback)
- generic: bypasses_armor, no_knockback
- on_fire: bypasses_armor, no_knockback, is_fire,
  burns_armor_stands, panic_environmental_causes (NOT
  bypasses_wolf_armor)
- hot_floor: bypasses_shield, is_fire, no_knockback,
  burn_from_stepping, panic_environmental_causes,
  sulfur_cube_with_block_immune_to (NOT bypasses_armor)
- lightning_bolt: bypasses_shield, is_lightning, no_knockback,
  panic_environmental_causes
- player_attack: is_player_attack,
  sulfur_cube_with_block_immune_to (plus panic_causes through the
  nested is_player_attack entry)
- mob_attack: panic_causes, sulfur_cube_with_block_immune_to
- mob_projectile: is_projectile, panic_causes,
  sulfur_cube_with_block_immune_to
- in_wall: bypasses_armor, bypasses_wolf_armor, no_knockback

Rule applied: a family joins a tag only when a vanilla source it
replaced was a member of that tag. Per family, the dominant
replaced source decides, by site majority first and family
identity as the tiebreak. Where a family's sites conflict, the
compromise is named under that family.

## Per family

### void: magic combat profile restored
Sites: DarkFadedEffect, MeltingEffect, SorrowRiftEffect (all three
pure magic), DimensionalRiftEntity x2, DimensionalSlashEntity,
DimensionalWarpRiftEntity, SorrowSlashEntity, VoidRiftEntity,
VoidSlashEntity, HollowsEdgeItem, SickleOfSorrowItem, SaberItem x5
(blitz, flash step path, pending hit slices, finisher slices).
Tokens: playerAttack 11, magic 7, mobAttack 7, generic 3,
mobProjectile 1.
Compromise, stated plainly: raw tokens favor playerAttack, but
those tokens are the attribution branch every family shares. Void
is set apart by its pure sites: its recurring damage (three effect
dots and the rift pulses) was magic with no physical branch at
all, and its fallbacks were magic and mobAttack. Judged by sites
and identity (erasure magic), magic is the dominant source.
Added to: bypasses_armor, no_knockback, witch_resistant_to,
bypasses_wolf_armor, avoids_guardian_thorns. Accepted side effect:
void hits delivered through former playerAttack branches now also
bypass armor.

### spirit: magic combat profile restored
Sites: SpiritSwordItem x4 (two pure magic: the 2 percent cleave
hit and the 1 percent cannon percent hit; two pure playerAttack:
strength bonus and cannon main pulse), SpiritCleaveEntity
(playerAttack primary, magic fallback). Tokens: playerAttack 3,
magic 3.
Compromise: a token tie, resolved by the pure sites. The sword's
signature percent-health damage was pure magic; the playerAttack
sites are additive bonuses on top of it. Added to the same five
tags as void.

### temporal: magic combat profile restored
Sites: WhisStaffItem x4 (two pure magic: percent hit and cast hit;
two pure playerAttack: dexterity bonus and extended strike),
TemporalRiftEntity (pure magic). Tokens: magic 3, playerAttack 2.
Magic holds the site majority and every magic site is pure. Added
to the same five tags as void.

### tempest: storm magic profile restored, lightning tag not restored
Sites: AzureLightningEntity (indirectMagic primary, lightningBolt
fallback), AzureTornadoEntity x2 (indirectMagic and magic),
AzureDragonSwordItem x2 (pure mobAttack), BanshoCycloneEntity
(playerAttack/mobAttack/magic), BanshoGaleEntity
(playerAttack/magic), BanshoWindProjectileEntity
(playerAttack/mobProjectile/generic), BanshoFanItem (pure
playerAttack), PowerPoleItem spin hit (pure playerAttack),
BleedingEffect (pure magic). Tokens: playerAttack 5, magic 4,
mobAttack 3, indirectMagic 2, mobProjectile 1, generic 1,
lightningBolt 1.
Added to: bypasses_armor, witch_resistant_to, bypasses_wolf_armor
(magic and indirectMagic agree on all three, and the family's
signature Azure lightning and tornado hits used indirectMagic as
the primary branch), and no_knockback. Compromise on no_knockback:
magic and lightningBolt are members and indirectMagic is not, so
the two Azure owner-branch expressions gain no_knockback they did
not have; the magic-class majority (5 of 7 tokens) carries it.
Not added: is_lightning (lightningBolt was one fallback branch of
one site; the family is mostly wind, and marking every wind hit as
lightning would be wrong), avoids_guardian_thorns and
always_triggers_silverfish (magic-only tags; tempest's magic-class
primaries were indirectMagic, which is not a member of either).
Accepted side effect: Bansho wind hits and the Azure item's
mobAttack hits gain armor bypass.

### hellfire: fire profile restored
Sites: DevilsHandsEffect (pure onFire, the only pure onFire
conversion in the pass), DevilSlamShockwaveEntity
(playerAttack/magic), EvilSpearProjectileEntity
(playerAttack/mobProjectile/generic), EvilSpearItem x2 (pure
playerAttack), TridentProngEntity (playerAttack/magic),
TridentShardEntity (mobProjectile/magic). Tokens: playerAttack 5,
magic 3, mobProjectile 2, onFire 1, generic 1.
Added to: is_fire (anchored on the DevilsHands conversion and the
family's fire identity), bypasses_armor and no_knockback (onFire
and magic agree on both). Not added: witch_resistant_to,
avoids_guardian_thorns, bypasses_wolf_armor (onFire is a member of
none of these, and hellfire's magic appears only as fallback
branches, never as a primary or pure source). Compromise, stated
plainly: player-owned trident and spear hits were playerAttack
before the pass and now count as fire, so fire-immune mobs resist
them; accepted because the family's defining applied damage was
onFire and its identity is hellfire.

### ki: no tags
Sites: BlasterBoltEntity, ErasureCannonBeamEntity x2,
HeavenSplitterEntity, SwiftCrescentEntity, SaberItem lunge.
Tokens: playerAttack 6, mobProjectile 3, generic 3. Every site's
primary or only source is playerAttack; mobProjectile and generic
occur only as the no-player fallbacks inside the three Blaster
expressions. No magic or indirectMagic anywhere. Nothing to
restore; saying so plainly rather than forcing tags on a
player-attack family.

### valor: no tags
Sites: BraveCrossSlashEntity, BraveRushTrailEntity x2,
BraveShockwaveEntity, BraveSlashEntity, GrandBladeShardEntity
(valorSource helper), GrandCrescentWaveEntity, ValorFieldEntity
(its own pressure site only; the two suspended-projectile lines
were never converted and stay vanilla by the documented
exception), BraveSwordItem, GrandSwordItem x2. Tokens:
playerAttack 11, generic 5, mobProjectile 4, mobAttack 3. No
magic-class primary or pure site; generic appears only as the
deepest fallback. Nothing restored.

### crush: no tags, three minority losses accepted and named
Sites: KingsSlamEntity (playerAttack/mobAttack/generic),
OxFissureEntity (indirectMagic primary, hotFloor fallback),
OxShockwaveEntity (playerAttack/mobAttack/generic),
OxKingsAxeItem x3 (playerAttack; generic x2, the stance pressure
and overload recoil self-damage), SpikeFlailSpinEntity x2 (pure
playerAttack), SpikeFlailSwingEntity (playerAttack/magic),
WarMaceRiseEntity (playerAttack/magic), WarMaceSmashEntity
(playerAttack/magic), PowerPoleItem poke (pure playerAttack),
ZShockwaveEntity crush site (indirectMagic primary, inWall
fallback). Tokens: playerAttack 8, generic 4, magic 3, mobAttack
2, indirectMagic 2, hotFloor 1, inWall 1.
Decision: crush is the pass's physical family (8 of 11 sites are
playerAttack primary or pure, and the bespoke standard's own
mapping sends inWall to crush as physical crushing), so no family
tags. Named non-parity, accepted: the OxFissure hits (indirectMagic
primary) and the ZShockwave crush hit (indirectMagic/inWall) lose
the armor bypass those sources had, and the Ox self-damage
(generic) loses it too. Family-wide armor bypass would wrongly
make every flail, mace, and ox hit ignore armor. is_fire is not
added for the single hotFloor fallback branch.

### lava: no tags, is_fire deliberately not added
Sites: LavaCleaveEntity, LavaEruptionEntity (each playerAttack
primary, magic fallback). Tokens: playerAttack 2, magic 2, but
magic was never a primary or a pure source. No fire-type vanilla
source was replaced by lava anywhere in the pass (the one hotFloor
conversion went to crush, in OxFissure), so is_fire would not be
parity: it would newly make fire-immune mobs immune to Lava Sword
ability hits, a behavior change in the wrong direction. If lava
should be fire-typed, that is a design change for Carter to ask
for, not a restoration.

### darkness: no tags
Sites: DarknessBladeEntity, DarknessWaveEntity, DaburaSwordItem
x2 (pure playerAttack). Tokens: playerAttack 4, generic 2,
mobProjectile 1. Generic appears only as the deepest fallback; no
magic-class source was replaced. Nothing restored.

### curse: no tags
Sites: CurseBladeItem x4 (all pure playerAttack, verified line by
line at 57902e1: telegraphed strike, ambient bolt, slam, verdict),
CurseChainEntity (mobProjectile). Tokens: playerAttack 4,
mobProjectile 1. Zero magic-class sources. Nothing restored,
stated plainly.

### gravity: no tags
Sites: ZGravitySlamEntity (playerAttack/mobAttack/generic),
ZShockwaveEntity main hit (pure mobProjectile). Tokens:
playerAttack 1, mobAttack 1, mobProjectile 1, generic 1. No
magic-class source. Nothing restored. (The ZShockwave crush site
is counted under crush.)

## Tag files written

All under src/main/resources/data/minecraft/tags/damage_type/,
each with "replace": false:

- bypasses_armor.json: hellfire, spirit, tempest, temporal, void
- no_knockback.json: hellfire, spirit, tempest, temporal, void
- witch_resistant_to.json: spirit, tempest, temporal, void
- bypasses_wolf_armor.json: spirit, tempest, temporal, void
- avoids_guardian_thorns.json: spirit, temporal, void
- is_fire.json: hellfire

## Tags considered and not written

- bypasses_shield: no file. Vanilla bypasses_shield pulls in the
  whole bypasses_armor tag by nested reference, so the five
  families restored to bypasses_armor bypass shields exactly as
  magic, generic, and onFire did. hotFloor and lightningBolt were
  also direct members; their conversions are the OxFissure fallback
  (crush, not restored, see crush) and the AzureLightning fallback
  (tempest, already covered through bypasses_armor).
- is_lightning: not restored (see tempest).
- is_projectile: not restored for any family. mobProjectile
  appears as a fallback branch in ki, void, curse, darkness,
  hellfire, tempest, and valor expressions, and as the single pure
  source of gravity's ZShockwave main hit. In the fallback cases
  the player-owned branch was playerAttack, so projectile identity
  was never the dominant behavior; marking whole families as
  projectiles would subject physical hits to projectile-specific
  rules they never had.
- is_player_attack: not restored for any family. The bespoke pass
  deliberately re-typed ability hits away from player_attack (the
  plain melee hit keeps it, untouched); the only gameplay consumer
  of note is armor stand breaking through can_break_armor_stand.
- panic_causes, panic_environmental_causes,
  always_triggers_silverfish, burns_armor_stands,
  burn_from_stepping, sulfur_cube_with_block_immune_to: ambient
  mob AI and block interaction flavor, excluded for every family
  as a group. They were not part of the parity gap (armor,
  knockback, witch resistance, fire behavior), and panic_causes
  membership covers nearly every vanilla combat type, so it
  carries no family identity. One exception to this grouping:
  avoids_guardian_thorns directly controls guardian thorn
  retaliation against the attacker in combat, so it was restored
  for the three families whose dominant source was magic itself
  (void, spirit, temporal).
- no_impact, ignites_armor_stands, always_kills_armor_stands,
  damages_helmet, is_explosion, is_fall, is_freezing, is_drowning,
  wither_immune_to, bypasses_effects, bypasses_enchantments,
  bypasses_invulnerability, bypasses_resistance,
  always_hurts_ender_dragons, always_most_significant_fall,
  no_anger, mace_smash: no replaced source belongs to any of
  these tags, so there was nothing to restore.

## Verification

Every new JSON parses (python json load), every referenced id
exists as a file in data/dragonblockarcanedba/damage_type/, and
all six files use "replace": false. Data files do not affect the
javac audit sweep. The change surface of this task is the six tag
JSONs plus this file and the addendum in
audits/BESPOKE-FINAL-AUDIT.md; no Java was touched.
