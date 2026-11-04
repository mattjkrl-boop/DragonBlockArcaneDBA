# No-Shapes Realism Pass: Status Effects Report

Owner: Status Effects. Measured and converted at dba ad22b32 (Phase 1 foundation). Scope: the twenty-seven status effect classes assigned in audits/no-shapes-audit.md under Status Effects. EvilDreadEffect is cross-listed under Evil Spear Dread and was not touched; CinematicTrackingEffect has no particle presentation and was not touched; DbaEffects registration was not touched.

## What the conversion is

The audit found zero BespokeGeometry, LayeredRender, or direct vertex calls in effect/. This surface was particle presentation only, and its problem was material honesty: almost every effect presented itself through the generic THEME_DUST options (DbaDustOptions) or the generic VOID_SMOKE glow style, regardless of what the effect is actually made of. Every assigned effect now presents through the Phase 1 material system (DbaMaterialOptions on the PLASMA, SMOKE, VAPOR, DUST, MIST, SOUL, DARKNESS, and GLASS_DUST family), emitted through the exact spawn points, positions, counts, deltas, speeds, and tick gates the effect already used.

Rules held throughout:

- Every DbaDustOptions color and scale value carried over byte-identical into the DbaMaterialOptions for the same stream. Simple-type streams that carried no per-particle color (VOID_SMOKE, DUST_PUFF) took the closest color of the type they replaced: 0x591A80 for the VOID_SMOKE start color (its style starts at 0.35, 0.10, 0.50) and 0xEBE8E0 for DUST_PUFF (its style sits near 0.92, 0.90, 0.88).
- Scale for the converted simple streams was set against the material profile base size so the sprite lands near the old type's size: DARKNESS profile 0.36 against VOID_SMOKE 0.35 at scale 1.0, PLASMA profile 0.17 against VOID_SMOKE 0.35 at scale 2.0, DUST profile 0.26 against DUST_PUFF 0.28 at scale 1.0.
- Seeds are presentation only: each server stream seeds from the afflicted entity id and tick count, with a small per-stream offset where an effect has several streams, so material phase and spin vary per entity and per stream. No synced data changed.
- No damage line, attribute modifier, duration, amplifier behavior, tick gate, or hurt call changed. A grep of the effect/ diff for damage, hurt, amplifier, attribute, and tick-gate lines returns nothing. The five effect-internal DbaDamage conversions from the bespoke merge (Bleeding, DarkFaded, Melting, SorrowRift, DevilsHands) are byte-identical to before this pass.

## Per-effect material table

| Effect | Before | After (material per stream, in spawn order) |
|---|---|---|
| AncientWeight | 1 DbaDustOptions | DUST (settling stone dust at the feet, goldenrod color and 1.2 scale kept) |
| BladeGuard | 1 DbaDustOptions | PLASMA (steel-white plasma motes, 1.4 scale kept; the plasma profile's velocity-responsive scale carries the spark read) |
| Bleeding | 2 DbaDustOptions | MIST, MIST (crimson fluid and red mist; drip stream keeps its downward delta, splatter stream keeps its ground position; colors 0x8B0000 and 0xCC0000 and scales kept) |
| CelestialGrace | 2 DbaDustOptions | SOUL (cyan stream), VAPOR (white shimmer stream) |
| DarkFaded | ambient VOID_SMOKE in the MobEffect constructor | DARKNESS material options (ambient effect particles; color matched to the replaced VOID_SMOKE start color) |
| DemonSurge | 1 DbaDustOptions | SMOKE (crimson smoke, 1.3 scale kept) |
| DevilsHands | 2 DbaDustOptions | PLASMA (the dark red flame stream, count 3, upward drift kept), DARKNESS (the near-black ash and smoke stream) |
| EarthShatter | 1 BlockParticleOption, count 2 | 1 BlockParticleOption (real dirt chunks, count 1) plus DUST material (ground dust, count 1); total particles per emission unchanged at 2, same position and deltas |
| EnergyOvercharge | 1 DbaDustOptions | PLASMA (plasma and electric sparks in one emissive material, spring green kept) |
| FissureStun | 1 DUST_PUFF stream, count 2 | DUST material (ground dust, count 2, same spread) |
| HeroicFocus | 1 DbaDustOptions | PLASMA (golden plasma motes, Brave gold 0xFFD700 kept) |
| Hollowed | ambient VOID_SMOKE in the MobEffect constructor | PLASMA material options (void plasma; scale 2.0 so the sprite size matches the replaced VOID_SMOKE) |
| JudgementLock | 1 DbaDustOptions | PLASMA (temporal plasma in electric cyan; the glass dust aspect of the audit row is not separately emitted, see the worse check) |
| MarkedByEvil | 1 DbaDustOptions | PLASMA (crimson plasma mark mote, 1.8 scale kept) |
| Melting | 2 DbaDustOptions | PLASMA (the corrosive violet rising stream), SMOKE (the deeper violet stream) |
| MovementCurse | 1 DbaDustOptions stream in a loop plus 1 VOID_SMOKE stream at high stacks | PLASMA (curse plasma, indigo kept, seed varies per loop step), DARKNESS (the heavy-stack darkness smoke, count 2 kept) |
| OxBrace | 1 DbaDustOptions | DUST (ground dust at the feet, ember orange-red color kept; the DUST profile settles on ground contact) |
| PetrificationCurse | 1 DbaDustOptions | DUST (stone dust, stone gray kept) |
| PoleStun | 1 DbaDustOptions | PLASMA (golden sparks circling above the head, motion and positions kept) |
| Rifted | 1 DbaDustOptions plus 1 VOID_SMOKE stream | PLASMA (void plasma, dark violet kept), DARKNESS (darkness smoke) |
| SilentMark | 1 DbaDustOptions | SOUL (pale soul vapor, white kept) |
| SorrowRift | 1 DbaDustOptions plus 1 VOID_SMOKE stream | SOUL (soul wisps, blue violet kept), PLASMA (void plasma, scale 2.0 as above) |
| SpiritImpale | 1 DbaDustOptions | SOUL (soul plasma and golden wisps, gold kept) |
| StormOfDarkness | 1 VOID_SMOKE stream plus 3 DbaDustOptions streams | DARKNESS (count 3 storm core), SMOKE (dark crimson stream), SMOKE (black ash stream, near-black kept), PLASMA (the 20 percent chance yellow static stream) |
| TemporalStasis | 1 DbaDustOptions | PLASMA (temporal plasma, deep sky blue kept; glass dust and mist aspects of the audit row are not separately emitted, see the worse check) |
| Valor | 1 DbaDustOptions | PLASMA (valor plasma, gold kept) |
| ValorStun | 1 DbaDustOptions | PLASMA (valor plasma suppression motes, gold kept) |

## Before and after presentation counts

Measured by grep over the twenty-seven assigned files.

| Presentation form | Before | After |
|---|---:|---:|
| DbaDustOptions constructions | 29 | 0 |
| Explicit simple-type sendParticles streams (VOID_SMOKE x4, DUST_PUFF x1) | 5 | 0 |
| Ambient simple-type constructor particle options (VOID_SMOKE x2) | 2 | 0 |
| DbaMaterialOptions constructions | 0 | 37 |
| BlockParticleOption constructions (EarthShatter physical dirt chunks) | 1 | 1 |
| Explicit sendParticles calls in assigned files | 35 | 36 |
| Total particles per emission, per effect | as coded | unchanged |

The one added sendParticles call is EarthShatter's split: the old single count-2 block chunk call is now a count-1 block chunk call plus a count-1 DUST material call at the same position with the same deltas and speed, so the audit's "real block chunks and ground dust" row is carried by both materials at the same total count.

## Change requests

None. The Phase 1 material family covered every assigned row. Two audit rows name secondary aspects that no DbaMaterialOptions material carries as a separate stream at a single existing spawn point (JudgementLock's glass dust, TemporalStasis's glass dust and mist); see the worse check for how those were handled instead of inventing new spawn points.

## Honest worse check

- Nothing was reverted. No conversion changed a count, rate, position, or behavior, so nothing plays worse; the risks are visual reads that only Carter's PC test can settle.
- Material profiles have their own lifetimes, growth, and alpha curves, so converted streams live longer and move differently than the old theme dust did (the old dust lived 14 to 24 ticks with no gravity and no physics; DUST and SMOKE now settle on the ground, SOUL and PLASMA rise, MIST hugs the ground). That is the point of the pass, but density over time will read higher around a standing afflicted entity than the old dust did, most noticeably on the every-2-tick effects (BladeGuard, DemonSurge, DevilsHands, PoleStun, TemporalStasis).
- Bleeding's MIST profile is the largest sprite in the family (base size 1.05, faint alpha 0.34). At the kept 1.2 and 0.8 scales the blood read is a broad red haze rather than the old small drips; the downward drip delta and ground position of the two streams are unchanged, but this is the conversion most likely to be judged too soft or too big on the PC test.
- JudgementLock and TemporalStasis are PLASMA only. Their audit rows also name glass dust (and mist for TemporalStasis), but each effect has exactly one count-1 stream at one spawn point, and splitting or alternating materials would have changed the per-tick presentation the brief told me to keep. The temporal identity is carried by the plasma color and the existing rotating spawn positions. If Carter wants the glass dust present, it needs a coordinating decision to alternate materials on alternate emissions.
- DevilsHands' flame stream is PLASMA, not the FLAME simple type, because the material family has no flame material id and FLAME's fixed style colors (bright yellow-orange) would have broken the kept dark red color. The plasma profile is emissive and buoyant, so it reads as dark red fire motes; the literal flame flipbook is not used here.
- DemonSurge's audit row names hellfire embers as well as crimson smoke; the single existing stream became SMOKE, and the ember aspect is carried only by the crimson color and upward drift. Same single-stream constraint as JudgementLock.
- EarthShatter now emits one dirt chunk and one dust particle per emission instead of two dirt chunks. Total count is unchanged and the audit row asked for chunks and dust, but the chunk density is halved.
- DarkFaded and Hollowed ambient particles are single material options because MobEffect stores exactly one ParticleOptions for ambient spawning. DarkFaded took DARKNESS and Hollowed took PLASMA; the other aspect of each audit row (void plasma for DarkFaded, darkness vapor for Hollowed) is not in the ambient stream.

## Verification

Targeted javac (JDK 25) of the whole effect/ package with full sourcepath closure, run on both audit classpaths. Compiling the package sweeps in DbaEffects, CinematicTrackingEffect, and EvilDreadEffect as well; all three are unedited and compile as before.

- 26.2 (cp262): 4 error lines total, all the known baseline kinds in untouched files (DbaItems CreativeModeTab Output access x2, DbaMenus MenuType access x2). Zero errors in any effect/ file. All 30 effect package classes emitted.
- 26.3 (cp263): 10 error lines total, all known baseline kinds in untouched files (the same DbaItems and DbaMenus kinds, plus the pre-existing 26.3 hostile mob entity kinds in DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity, and PatrolRobotEntity, which are other owners' files at their baseline state). Zero errors in any effect/ file. All 30 effect package classes emitted.
- Diff hygiene: the effect/ diff touches only particle option constructions, particle imports, and the EarthShatter split. A grep of the diff for damage, hurt, attribute, amplifier, and tick-gate lines returns nothing, so durations, amplifiers, damage, and behavior are unchanged in the diff itself, not just by intention.
- Em dash scan: zero em dashes in added diff lines and zero in this report. Pre-existing javadoc em dashes in untouched comment lines of these files were left as they were; none are in lines this pass added or edited.

Javac audit only. No Gradle build was run and no in-game look is claimed; the first visual judgment is Carter's PC test.
