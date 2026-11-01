# Weapon Attack Visuals Before Audit (Nov 1, 2026)

How weapon attacks work today, per weapon. Server side, every weapon item class runs rich ability logic (projectiles, beams, shockwaves, dashes) with its own entities and renderers; none of that changes here. The shared client attack visual is SwingTrailRenderer, and it is one-size-fits-all:

- 15 of 19 weapons get a trail ribbon (all blades plus spear, trident, sickle, axe). Bansho Fan, Blaster Gun, Power Pole, and Whis Staff get nothing at all on a basic attack.
- Every trailed weapon uses the identical arc: same 140 degree sweep, same 2.0 reach, same 6 tick fade, same 0.55 alpha, one flat color, sampled the same way. A trident stab arcs like a katana slash; the Grand Sword fades as fast as the Saber.
- The ribbon is a single flat color band (hilt transparent, tip colored). No hot core, no per-weapon particles, no thrust motion, nothing keyed to the weapon's actual size or speed.
- The color table is a hardcoded instanceof chain inside the renderer, so adding or tuning a weapon means editing renderer code.

Target design (clean and easy, per Carter): one profile table, WeaponTrailProfiles, one record per weapon declaring color, reach, sweep, fade, alpha, motion kind (ARC, THRUST, FAN, FLASH), and an optional tip particle style. The renderer consumes profiles only. Tuning a weapon becomes a one-line edit in the table.
