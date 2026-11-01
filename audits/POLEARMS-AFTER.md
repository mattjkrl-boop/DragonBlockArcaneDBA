# Polearms After Audit (Nov 1, 2026): Evil Spear, Devil Trident, Power Pole, Sickle of Sorrow, Ox King's Ax

Built by tools/build_polearms.py (shaft plus head structure, bones weapon > shaft, head, head_glow), concepts in concepts/ (Power Pole is canon-simple: red staff, gold caps, no concept needed), all 3D verified in renders/3d/ before commit. 14 of 19 weapons now migrated.

Designs keyed to the real SwingTrailRenderer colors: Evil Spear crimson 0xB0003A leaf head with a gem socket; Devil Trident red 0xFF2244 triple prong (center longest) over a red-and-black wrapped shaft; Sickle of Sorrow dark red 0x8B0000 inner-edge glow on a true arc blade; Ox King's Ax orange 0xFF6A2B edge on a five-row steel fan head with top spike; Power Pole has no trail and stays the classic red staff with gold caps and rings.

3D catches this batch: the sickle blade's first arc floated detached above the shaft; the arc was re-centered so the blade starts inside the socket, verified connected in the re-render. Also fixed on the swords: Dabura, Cursed, and Hollows glow palettes now match their actual trail colors from SwingTrailRenderer (Dabura is purple 0xC23BFF, Hollows is slate violet 0x5A5A7A; earlier reads of those colors were wrong and the file is ground truth).
