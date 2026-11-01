# Phase 4 Before Audit (Nov 1, 2026): screen shake

What exists: ScreenShakePayload (server to client: intensity, durationTicks, hitstopTicks) feeds ScreenShakeHandler. One active shake at a time (strongest wins), hitstop holds a single sharp offset for its ticks, and the shake itself is per-tick uniform random yaw and pitch jitter with quadratic decay, applied by CameraShakeMixin after the camera aligns. Config scale multiplies everything, 0 disables.

What is wrong with it: white-noise jitter reads as buzz, not impact. Every tick is an unrelated random direction, so a heavy hit and a light rumble feel the same in kind, only different in size. There is no directional kick (a hit from the side should shove the view that way first), no smooth sway, and nothing distinguishes the initial impulse from the settle.

Plan (handler only, payload and mixin untouched, public API unchanged): trauma-style shake. Two layered sine waves per axis at different frequencies and phases give smooth continuous sway instead of buzz; amplitude stays quadratic in remaining time (trauma squared). On trigger, a random directional kick is stored and decays with the same envelope, so the first frames push one way before the sway takes over. Hitstop behavior is unchanged.
