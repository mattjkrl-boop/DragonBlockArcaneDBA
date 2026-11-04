# Spectacle Pass Final Audit

Coordinating pass record for the full spectacle pass: Phase 1 (fresh audit,
one shared standard, shared building blocks, commit 5e4b1de) and Phase 2
(one agent per weapon, 22 weapons, reports at audits/spectacle-report-*.md),
plus this merge pass. Player facing ability reference: audits/WEAPON-ABILITIES.md.

## Merge verification

- Tree state at merge: 158 files changed vs 5e4b1de, 10,633 insertions,
  3,676 deletions, plus the two coordinating fixes below.
- Ownership check: no weapon agent edited a shared or common file. The full
  diff touches only weapon item classes, their entities and renderers, the
  audit reports, and (coordinating pass only) util/GroundScar.java and
  effect/HeroicFocusEffect.java. DbaEntities, DbaNetwork, both mod
  initializers, and all Phase 1 shared blocks are byte-identical to 5e4b1de.
- Dual javac audit sweeps (the repo's verification method; the Gradle build
  cannot run in the sandbox and the compile check stands waived by Carter,
  who builds on his PC): baseline worktree at 5e4b1de measured 132 unique
  error kinds on the 26.2 classpath and 141 on the 26.3 classpath. The final
  tree measured exactly 132 and 141, and the set difference (comm -13) is
  EMPTY on both versions: zero new error kinds introduced by the whole pass.
- No Gradle build was run or claimed.

## Coordinating fixes applied in the merge pass

1. util/GroundScar.java: the re-scar path stored the caller's BlockPos
   reference while the new-entry path copied it with immutable(). A caller
   passing a MutableBlockPos (the scarDisk loop does) could have moved a
   stored restore position. The re-scar path now immutable-copies too.
   (Requested by the Evil Spear agent; its own call site already defended
   itself, the shared fix closes it for every caller.)
2. effect/HeroicFocusEffect.java: particle and effect color was lime green
   0x32CD32, off-palette for the Brave Sword it belongs to. Recolored to
   gold 0xFFD700, the Brave valor gold. (Requested by the Brave Sword agent.)

## Shared change requests: disposition

- DONE in merge: GroundScar immutable copy; HeroicFocusEffect recolor.
- Verified safe, no change: LavaSwordGround hooks register from
  LavaSwordItem class init only, and ensureHooks() is guarded by an
  AtomicBoolean compareAndSet, so double registration is impossible and
  crash recovery (adoptPersisted on level tick) is armed from mod init,
  because item registration loads the class during initialization.
- Kept deliberately, no change: the CURSE_LIGHTNING entity registration
  stays even though the rebuilt Curse Blade no longer spawns it (the class
  and renderer remain, harmless, and removing a registered id mid-save
  history is the riskier move). DbaPhysicsAttributes' silent 20 tick
  default modifier duration stays; the Curse Blade chain now refreshes its
  own modifier, which was the only live victim.
- Not done, recorded as future work (none of these block the pass):
  1. TimeShatterEntity and its renderer (Whis Staff family, owned by no
     weapon agent in this pass): 18 tick life, no fade-in over the first
     half of its life, and mismatched random seeds between the dial and
     shard layers. The only known effect left below the standard's
     lifecycle rule.
  2. LayeredParticles.emitVortex uses the standard envelope fractions
     internally; channeled effects with their own envelopes (Z Sword
     charge and stance, Sickle rift) hand-roll layered emission instead.
     An overload taking build and decay fractions would unify them.
  3. AtmosphereServer has no gloom-only kind: Hollow's Edge (a void tear,
     where real weather would be wrong) does its atmosphere with light
     and particles only.
  4. HollowAfterimageEntity behavior is global: Hollow's Edge set its
     Dark Faded to 40 ticks at about a 3 block radius (was 60 ticks in a
     4 block box), and Katana and Saber spawn the same entity, so their
     ghosts use the new numbers too. Constructors and synced data are
     unchanged, so nothing breaks; per-spawn overrides were requested
     and not built.
  5. Damage cap semantics: DbaEffectsConfig.capDamage applies per damage
     event, so abilities that split one hit into slices (Saber finisher
     50/30/20, Brave cross slash pulses) cap per slice, not per cast.
     Left as a cross-weapon balance decision for Carter, not changed
     silently in a merge.
  6. SPECTACLE-STANDARD.md names a LayeredRender.drawLayeredDome helper
     that was never implemented in Phase 1. Nothing calls it; every dome
     in the pass was built from drawVortexBands and drawAnchorGlow. The
     standard text is the only place it exists.
  7. The Dabura dark debris palette constant is duplicated privately in
     DaburaSwordItem and DarknessDomainEntity (weapon agents could not
     edit shared files). Cosmetic only.

## Per weapon outcome (one line each; full detail in each report)

- Katana: Ki exhaustion now cancels instead of firing; aim guide range is
  the real dash range; damage lands inside the visuals; charge and
  splitter rebuilt on the envelope with debris, bolts, and ground bursts.
- Z Sword: telegraph ring is the real slam radius; slam damage moved to
  the visual peak with falloff; stance is a real gravity well; full
  save/load on the delayed launch.
- Saber: Ki drains only after the immobilized check; finisher damage
  lands in slices inside its visual; finisher debris field and a real
  lightning bolt; lifetimes raised across the kit.
- Brave Sword: renderer lifetimes read the entities (desync fixed);
  splash and dash damage land as the visuals reach targets; charge pays
  3.3x; ground scar added at the shockwave impact.
- Grand Sword: full charge pays 3.0x (was 1.86x); finisher cooldown 160
  outlasts the 140 tick charge; Valor Field fades out on every removal
  path; promised buffs now actually apply, to allies only.
- Azure Dragon Sword: the storm calls real server thunder weather with
  gloom, charge-scaled life, 80 debris pieces, seeded lightning strikes,
  and a cooldown that outlasts the storm; the permanent bounciness
  modifier is gone.
- Dabura Sword: the domain runs a full lifecycle at about 220 particles
  per second (was about 28), with vortex pull, dark debris fields,
  obsidian scars, thunder weather with gloom, and real BoltEntity
  strikes replacing the fake arcs.
- Curse Blade (Cursed Blade): sky cracks are a real rendered entity that
  sheds lightning; ambient bolts rebalanced 15 to 120 + Str x 0.35;
  chain physics modifier no longer silently expires mid-bind; one real
  compile bug the first attempt left (Blocks.BLACK_CONCRETE does not
  exist) fixed to BLACKSTONE.
- Hollow's Edge: the rift's pull, lift, and fling run on VortexPhysics
  with honest distance falloff; 60 piece debris wave at collapse; rift
  cooldown scales with charge.
- Dimensional Sword: Ki is charged only after the warp target validates;
  landing validation ends suffocation warps; slash scales with Strength;
  both rift ends pull, burst, scar, and throw debris.
- Spirit Sword: Spirit Cannon damage is now occluded by blocks, ending
  at the same point the beam visual stops; beam fades and retracts
  instead of vanishing; cleave and impale gained trails, debris, scars.
- Evil Spear: Hell Hunt lock-on requires line of sight (no more hunts
  through walls); the throw opens a real vortex; every chain impact
  throws debris and leaves a temporary obsidian scar.
- Devil Trident: slam damage moved to the visual peak with a brimstone
  scar disk and 30 debris pieces; the downward pin is now a weight
  scaled fling; prong piercing is real (750 / 495 / 337.5); the swarm
  starvation cooldown bypass is closed.
- Sickle of Sorrow: Melting cut from amplifier 9 for up to 57 seconds
  to amplifier 0 to 3 for 10 seconds; dual cooldown replaced by one;
  the rift runs on VortexPhysics with a 64 piece soul debris field.
- Ox King's Axe: the two mismatched slam visuals became one expanding
  storm whose damage rides the ring front; max-charge ring blocks
  actually break now (about 2 of 3 were never even tested before);
  debris and scarring throughout.
- Power Pole: the whirlwind is one sustained entity with a real build,
  sustain, dissipate lifecycle (was an 8 tick entity respawned every 6
  ticks); it pulls, lifts, and ejects; the poke no longer pierces walls.
- Whis Staff: the rift runs 100 ticks under a shared envelope at about
  220 particles per second (was about 13), with 60 suspended debris
  pieces and a gentle orbital vortex; the reversal now starts when the
  freeze actually ends.
- Bansho Fan: the cyclone is a true layered funnel with pull, lift, and
  ejection (mobs used to drop straight down at the end), visible from
  inside, with debris and a ground scar ring.
- Blaster Gun: beam damage lands in 6 pulses across its life instead of
  all on tick 1; sub-0.2 charge releases fizzle instead of firing a
  near-full beam for almost no charge; bolt trails, orb layers, and the
  rotating core all made real.
- Lava Sword: ground lava persistence is crash safe (SavedData backed,
  restored after a restart, player blocks never stomped), closing the
  hole where a server crash left permanent lava in the world; melee now
  applies its burn; the 94708b6 deterministic placement and on-top
  landing fixes are verified intact.
- Spike Flail: channel time is real charge (tap crack 480 + 1.0S at
  radius 2.7, full channel 1080 + 2.0S at radius 5.4); damage, scar, and
  debris land when the visual disc actually arrives; first particles
  this weapon has ever emitted.
- War Mace: smash damage has distance falloff and follows the falling
  owner instead of freezing at cast height; rise damage climbs with its
  crescent instead of landing all on tick 1; fracture scars and ejecta
  on every impact.

## Known remaining problems (complete list, fixed or not)

1. TimeShatter lifecycle and seed mismatch (item 1 above). Not fixed.
2. The kiAdditiveType platform limit from Phase 1 stands: the additive
   render type is the translucent emissive type, because the true
   additive eyes shader ignores vertex alpha. Unchanged by this pass.
3. Blocks.STAINED_GLASS and other 26.3-only concerns: none new. The one
   pre-existing 26.3 baseline kind in LavaSwordGround (BlockState
   blocksMotion, removed in 26.3) is part of the 141 kind baseline on
   both sides of the diff; it existed before the pass and the pass did
   not touch that call. It will need a DbaCompat shim in a future compat
   pass if Carter wants the 26.3 baseline at zero.
4. In-game look and feel are unverified by construction: nothing in this
   pass ran in a client. Every agent's worse check is in its report;
   the deliberate trades (damage delayed to visual peaks, charge paying
   more than taps, falloff replacing flat full damage) are the standard
   working as written, and Carter's PC test is the first real look.
5. Ki techniques (ki/KiTechniqueHandler and its entities) were outside
   this pass's scope and are untouched.
