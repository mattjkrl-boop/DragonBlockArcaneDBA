# Batch 2 After Audit: Dabura Sword, Cursed Blade, Hollows Edge (Nov 1, 2026)

Built with tools/build_swords.py, concepts in concepts/, 3D verified with tools/render3d.py before commit (renders in renders/3d/). All three added to MIGRATED.

- dabura_sword: new "fangs" guard style (crossguard bars hooking toward the blade), dark scarred steel palette, silver edge glow matching its 0xC0C0C0 trail, crimson grip, spiked pommel, swing 0.42.
- cursed_blade: black violet blade with a dual_edge glow option (energy strips on both edges, matching the concept), thorn stepped wings in blackened metal, purple grip, gem pommel with the gem in the glowmask so it shines, swing 0.36.
- hollows_edge: pale ghost blade, teal glow edge matching its 0x00FFCC trail, slate double bar guard, glowing teal orb pommel, fastest swing so far at 0.32 with the strongest charge tilt.

3D check notes: all three read connected at 3/4 and held views, no floating parts. Dabura's fang tips read as short posts at this resolution; acceptable at game scale and noted as a tuning candidate. Same safety properties as batch 1: all asset files present, flat item JSONs untouched, per-weapon reversible.
