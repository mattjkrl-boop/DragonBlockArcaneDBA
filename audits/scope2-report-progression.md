# Scope 2 Report: A4 Progression and Forms

Agent A4, at HEAD ef5ad02 (Scope 2 foundation). Files owned and edited:
network/DbaNetwork.java and tail/TailHelper.java. Nothing committed.
No other file was touched; the Lava Sword, Evil Spear, and Dread work
sitting uncommitted in this tree was left alone.

## Before and after counts

network/DbaNetwork.java:
- Vanilla SoundEvents call sites: 17 to 0
- Vanilla ParticleTypes call sites: 1 to 0 (the emote CRIT burst)
- The now unused SoundEvents import was removed. Post-edit grep for
  SoundEvents and ParticleTypes in the file: 0 matches.

tail/TailHelper.java:
- Vanilla ParticleTypes call sites: 4 to 0
- Vanilla SoundEvents call sites: 3 to 0
- The now unused ParticleTypes and SoundEvents imports were removed.
  Post-edit grep: 0 matches.

A full git diff review of both files confirms the only changed lines
are the sound and particle tokens below plus the three import
removals. Every volume, pitch, sound source, particle count, spread,
and speed is byte-identical. Packet shapes, handler logic,
progression math, form requirements, and tail physics are untouched.

## DbaNetwork swaps

By handler, in file order:

- Stat upgrade purchase: EXPERIENCE_ORB_PICKUP to CRYSTAL_CHIME
  (0.8f, 1.3f). Judgment call, see below.
- Transform to "none": BEACON_DEACTIVATE to RELEASE_DARK (1.2f,
  1.2f) and FIRE_EXTINGUISH to EXTINGUISH (1.0f, 0.8f). Judgment call
  for the deactivate, see below.
- Form activation: BEACON_ACTIVATE to TRANSFORM_GENERIC (1.8f,
  1.0f); GENERIC_EXPLODE to BOOM_BIG (1.5f, 1.2f);
  LIGHTNING_BOLT_THUNDER to THUNDER_CRACK (1.0f, 1.5f, source stays
  WEATHER). The stack's other two layers (ABILITY_CHARGE and
  LIGHTNING_ZAP) were already custom and are unchanged.
- Untransform: BEACON_DEACTIVATE to RELEASE_DARK (1.2f, 1.2f) and
  FIRE_EXTINGUISH to EXTINGUISH (1.0f, 0.8f), same as the "none"
  branch.
- Race select fanfare: PLAYER_LEVELUP to LEVEL_UP (1.0f, 1.0f),
  per decision D13 and the Scope 2 swap guide.
- Emote burst: ParticleTypes.CRIT to DbaParticles.CRIT_SPARK (5
  particles, same spreads), per the audit's direction for this line.
- Technique unlock: PLAYER_LEVELUP to LEVEL_UP (1.0f, 1.2f).
- Technique upgrade: PLAYER_LEVELUP to LEVEL_UP (1.0f, 1.2f).
- Technique equip: ARMOR_EQUIP_GENERIC to EQUIP (1.0f, 1.0f),
  following the Scope 2 guide's equip mapping for the bank's EQUIP
  event.
- Ki technique save: ENCHANTMENT_TABLE_USE to CAST_MAGIC (1.0f,
  1.4f), per the guide's cast mapping (same swap the Katana pass
  made for this vanilla event).
- Sickle of Sorrow summon in toggleTechnique: WITHER_SPAWN to
  CAST_DARK (0.7f, 1.8f). Judgment call, see below.
- Sickle of Sorrow dismiss: RESPAWN_ANCHOR_DEPLETE to RELEASE_DARK
  (0.7f, 1.5f). This is the same mapping the Sickle weapon's own
  first-pass conversion chose for this exact vanilla sound at its
  implosion beat, so the toggle now matches the weapon.
- Ki Sense toggle: UI_BUTTON_CLICK to UI_CLICK (0.8f, conditional
  pitch unchanged), per the Scope 2 guide and the foundation
  report, which names these two plays for UI_CLICK.
- Generic technique toggle: UI_BUTTON_CLICK to UI_CLICK (0.8f,
  conditional pitch unchanged).

TRANSFORM_GENERIC wiring (D13): the bank's TRANSFORM_GENERIC event
had zero call sites; its home is form activation, and the transform
handler's activation branch is the only form activation site in the
file. It takes the slot BEACON_ACTIVATE held. The swap guide's
charge mapping for BEACON_ACTIVATE (CHARGE_RISE and family) was not
used here because that same stack already plays ABILITY_CHARGE at
the same volume in the next line; doubling the charge layer would
change the mix, while TRANSFORM_GENERIC is the event that exists
for exactly this beat.

Judgment calls, stated plainly:
- EXPERIENCE_ORB_PICKUP (two sites across both files: the stat
  upgrade confirmation here, the tail regrow chime in TailHelper)
  has no guide entry. LEVEL_UP was synthesized for PLAYER_LEVELUP
  plays specifically (D13), so it was not stretched to cover orb
  pickup. CRYSTAL_CHIME is the bank's bright confirmation chime and
  takes both sites at identical volume and pitch. One line each if
  the coordinating pass prefers a different home.
- BEACON_DEACTIVATE (both detransform branches) has no guide entry.
  Detransformation is a release of held power, not a failure (so
  not FIZZLE) and not an explosion (so not RELEASE_BLAST).
  RELEASE_DARK carries the power-draining fall, and the paired
  EXTINGUISH keeps the aura-dying beat the vanilla pair had.
- WITHER_SPAWN at the Sickle summon is a dark summoning boom;
  CAST_DARK is the bank's dark cast event and matches the sickle's
  own converted sound palette.

## TailHelper swaps

Tail sever (severTail):
- CRIT to DbaParticles.CRIT_SPARK (25, same spreads and speed)
- DAMAGE_INDICATOR to DbaParticles.DAMAGE_SPARK (10, same values),
  per decision D10
- SMOKE to DbaParticles.DUST_PUFF (8, same values), per the
  standard map
- ITEM_BREAK to ITEM_SHATTER (1.3F, 0.75F), per the guide
- PLAYER_ATTACK_CRIT to CRIT_HIT (1.1F, 0.65F), per the guide
- The already custom BLADE_SHING layer is unchanged.

Tail regrow (regrowTail):
- HAPPY_VILLAGER to DbaParticles.HAPPY_SPARKLE (15, same values),
  per decision D10
- EXPERIENCE_ORB_PICKUP to CRYSTAL_CHIME (1.0F, 1.2F), same
  judgment call as above.

References use the fully qualified DbaSounds and DbaParticles names,
matching both files' existing style (DbaNetwork already referenced
DbaSounds fully qualified; TailHelper already referenced
DbaSounds.BLADE_SHING fully qualified).

## Verification

Targeted javac (JDK 25) of the two files with sourcepath over both
source sets, on both audit classpaths:
- 26.2 (cp262): zero error lines in DbaNetwork.java or
  TailHelper.java. The run's 4 error lines are the pre-existing
  baseline kinds in DbaItems and DbaMenus pulled in by the closure.
- 26.3 (cp263): zero error lines in DbaNetwork.java or
  TailHelper.java. The run's 10 error lines are the known 26.3
  baseline kinds in DbaItems, DbaMenus, DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, and
  PatrolRobotEntity, none of them in files this task opened.
- DbaNetwork.class and TailHelper.class were emitted on both
  classpaths.
- Zero em dashes in any added diff line.

This is a javac audit only. No Gradle build was run or is claimed.

## Honest worse check

Nothing reverted. The real trades, all of them listen-only or
look-only judgments for Carter's PC test:

- Form activation's lead sound is now TRANSFORM_GENERIC, an event
  synthesized for this beat, where the vanilla beacon chime used to
  lead. The rest of the stack (charge, boom, thunder, zap) is
  unchanged in structure, so the moment keeps its weight; only the
  lead timbre is new.
- Detransformation reads darker than vanilla: RELEASE_DARK plus
  EXTINGUISH is a moodier power-down than beacon deactivate plus
  fire extinguish. It fits the mod's palette, but it is a tone
  change, stated plainly.
- The tail sever burst and the emote burst now throw CRIT_SPARK,
  the mod's additive glow spark, instead of vanilla crit stars.
  Same counts and spreads; the read is the house style, slightly
  softer than vanilla's hard stars.
- The D10 glyphs change shape by design: DAMAGE_SPARK is a red
  jagged star pip where vanilla showed damage indicator hearts,
  and HAPPY_SPARKLE is a green rising sparkle where vanilla showed
  happy villager hearts. Color and motion carry the same meaning
  (red damage pips at the wound, green sparkles rising on heal),
  but anyone who read the hearts literally will notice the swap.

## Change requests

None. The foundation's DAMAGE_SPARK, HAPPY_SPARKLE, LEVEL_UP,
UI_CLICK, and EQUIP covered every directed swap, and the three
judgment calls above were all served by existing bank events.
Shared files were not edited.
