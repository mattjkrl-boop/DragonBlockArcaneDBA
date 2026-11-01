# Weapon Attack Visuals After (Nov 1, 2026)

Rebuilt around one clean table, per Carter's direction that this be the biggest item and stay clean and easy to use:

- client/fx/WeaponTrailProfiles.java: one record per weapon (color, reach, sweep degrees, fade ticks, alpha, motion kind, tip particle). All 19 weapons have a profile. Tuning any weapon's attack visual is now a one-line edit in this table; the renderer holds no weapon-specific code.
- SwingTrailRenderer rewritten to consume profiles only:
  - Motion kinds: ARC (slash sweep with per-weapon sweep and reach: Saber sweeps 160 degrees and fades in 4 ticks, Grand Sword sweeps 165 and lingers 8, Katana is a clean fast 150/5), THRUST (Evil Spear and Devil Trident now stab along the look direction instead of arcing), FAN (Bansho Fan gets a 200 degree translucent gust arc), FLASH (Blaster Gun gets a muzzle spark burst and a short tracer streak instead of a ribbon).
  - Power Pole (long thin red sweep, reach 2.9) and Whis Staff (gold sweep with spark tips) have attack visuals for the first time; previously the four non-blade weapons had none.
  - Two-tone ribbons: the weapon color at low alpha plus a hot near-white core along the leading half of the blade, replacing the single flat band.
  - Tip particles mid swing, keyed to element: sparks for Z Sword, Saber, Azure, Spirit, and Whis Staff; embers for Brave, Grand, and Ox King's Ax; void smoke for Dabura, Cursed, and Dimensional; wind for the Bansho Fan. Gated by the effects density config.
  - Per-profile fade replaces the global 6 tick fade; sample cleanup uses each trail's own profile.

Colors stay exactly the live trail colors from the old table (the Phase 1 ground truth), including Hollows Edge slate violet. Server ability logic, projectiles, and beams are untouched.

Audit: both files compile clean against the real jars on the full classpath; the only errors in the sweep are the documented pre-existing artifacts in untouched DbaItems and DbaMenus. This is an audit result, not a Gradle build; the build remains waived per BUILD-VERIFY.md.
