# In-Game Look Verification: tools/render3d.py (Nov 1, 2026)

The sandbox cannot run the Minecraft client (Gradle daemon loopback restriction, no GPU), so model verification uses a real 3D renderer over the actual repo files instead of flat projections: perspective projection, full bone hierarchy with pivots, per-cube rotations, texture atlas sampling, directional shading, and optional animation poses sampled from the weapon's own animation file (tools/render3d.py <weapon> [--anim swing --t 0.3]).

Renders for all rebuilt weapons live in renders/3d/ (three_quarter and held views at minimum). Every weapon from batch 2 on must pass a 3D look before it is committed.

First catch (same day): the Brave Sword and Azure Dragon Sword wing guards were built from rotated boxes that left visible floating gaps at 3/4 angles. Both were rebuilt as stepped unrotated voxel wings that overlap and always read as one connected piece, verified in the re-render.
