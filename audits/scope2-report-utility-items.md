# Scope 2 Report: Utility Items and Nimbus (agent A5)

Conversion of the seven A5 files from audits/bespoke-scope2-audit.md,
on top of the Scope 2 foundation at ef5ad02. Only sound event tokens
and the imports they need changed. Item behavior, radar logic, armor
stats, and nimbus flight are byte-identical outside the swaps below.
Nothing committed.

## Before and after counts (owned files, grep verified)

- Vanilla SoundEvents lines: 17 to 0 (SenzuBeanItem 3,
  RecoveryCapsuleItem 2, DragonRadarItem 2, PatrolRobotItem 1,
  FlyingNimbusItem 4, DbaArmorMaterials 2, FlyingNimbusEntity 3).
- Vanilla ParticleTypes lines: 0 to 0. None existed in these files.
- DustParticleOptions constructions: 0 to 0. None existed.
- Vanilla damageSources() lines: 0 to 0. None existed. There was no
  particle or damage conversion to do in this area; sounds were the
  whole job, exactly as the audit measured.

## Swap table (volumes, pitches, and sources unchanged at every site)

Guide-mapped swaps, per the DbaSounds swap guide and the Scope 2
foundation (D13):
- SenzuBeanItem: PLAYER_LEVELUP (1.0, 1.5) to LEVEL_UP;
  BEACON_POWER_SELECT (1.2, 1.6) to CHARGE_FULL (the guide's
  milestone mapping, same as the first pass used for this event).
- DragonRadarItem: NOTE_BLOCK_BASS (0.6, 0.6) to RADAR_BLIP;
  NOTE_BLOCK_BIT (0.8, distance pitch 1.6 / 1.2 / 0.8) to RADAR_BLIP.
  The vanilla sites called .value() on the note block holders;
  RADAR_BLIP is a plain SoundEvent and the playSound overload takes
  it directly, so the .value() calls are gone. The pitch ladder and
  the none-found versus found split are untouched, so the two radar
  states now differ by pitch and volume on one event instead of by
  instrument.
- PatrolRobotItem: IRON_GOLEM_REPAIR (0.8, 1.3, NEUTRAL) to REPAIR.
- FlyingNimbusItem: ELYTRA_FLYING (0.8, 1.5) to FLIGHT_WIND, both
  sites (useOn and use).
- DbaArmorMaterials: ARMOR_EQUIP_IRON and ARMOR_EQUIP_LEATHER to
  EQUIP, both materials. ArmorMaterial takes a Holder<SoundEvent>,
  so the swap is BuiltInRegistries.SOUND_EVENT.wrapAsHolder(
  DbaSounds.EQUIP). Registration order verified in the main class:
  DbaSounds.register() runs at line 31, before DbaItems.register()
  at line 37 first loads this class, so the holder resolves. This
  changes the equip sound of every DBA armor piece, as the audit
  said it would. No armor numbers changed.

Judgment calls (no bank analogue exists; each is a one-line flip if
the coordinating pass disagrees):
- PLAYER_BURP to THUNK, both sites (SenzuBeanItem 0.8, 1.2;
  RecoveryCapsuleItem 0.7, 1.4). The burp is the short gulp beat in
  each item's heal sound stack. THUNK is the bank's short, low,
  soft percussive, the closest shape in the bank; the stack still
  reads gulp, then fanfare.
- BOTTLE_EMPTY to FIZZLE (RecoveryCapsuleItem 0.6, 1.2). The
  capsule drains and is spent; the guide's fizzle bucket is the
  spent-and-used-up category, and its hiss is the nearest drain
  read in the bank.
- WOOL_PLACE to WIND_GUST, all three sites (FlyingNimbusItem useOn
  and use, FlyingNimbusEntity mobInteract; all 1.0, 1.4). The cloud
  materializes under the rider; a soft wind burst is the honest
  bespoke read of a cloud arriving. In the item the gust now layers
  directly before FLIGHT_WIND, summon then flight.
- WOOL_BREAK to WIND_GUST (FlyingNimbusEntity hurtServer, 1.0,
  1.2). The guide's break bucket is ITEM_SHATTER, a glass and metal
  shatter, which is the wrong texture for a cloud popping. The
  cloud dispersing as a gust of wind is the thematic match.
- ITEM_PICKUP to CRYSTAL_CHIME (FlyingNimbusEntity hurtServer,
  1.0, 1.2). The bank has no pickup event; CRYSTAL_CHIME is its
  only short bright collect-style blip.

## Verification

Targeted javac (JDK 25, sourcepath over both source sets) of the
seven files on both audit classpaths: zero error lines naming any
owned file on cp262 and on cp263, and all seven class files were
emitted on both. The only errors in either run sit in dependency
files and are the documented pre-existing baseline kinds: DbaItems
(Output has protected access in CreativeModeTab) and DbaMenus
(MenuType private access) on both versions, plus the known 26.3
mob kinds in DbaHostileEntity, FriezaSoldierEntity,
NamekianWarriorEntity, and PatrolRobotEntity. None references a
swapped symbol. This is the javac audit method, not a Gradle build;
none is claimed. Em dash scan of the added diff lines: zero.

## Honest worse check

Nothing reverted. The guide-mapped swaps are identity-preserving by
construction. The real losses are in the judgment calls, and they
are listen-only calls for Carter's PC test: the burp's comedic gulp
character is gone (THUNK is a neutral soft thud, and if it reads
wrong the fix is a foundation-level synthesized gulp event, not a
per-file workaround); summon and despawn of the nimbus now share
one gust where vanilla had two distinct fabric sounds (place versus
break), so mounting and losing the cloud sound more alike than
before; the capsule's drain reads as a hiss rather than a pour;
and the nimbus return blip is a brighter chime than the vanilla
pickup pop. Armor equips, radar beeps, the senzu fanfare, and the
robot deploy all gained dedicated bespoke events and read as
intended.

## Change requests

None. The foundation supplied radar_blip, equip, repair, and
level_up as promised, and every remaining site mapped onto the
existing bank. One note for the coordinating pass, not a request:
if the burp sites read wrong in game, a dedicated gulp event
belongs in the Phase 1 bank update alongside the other small
entries, so both items change together.
