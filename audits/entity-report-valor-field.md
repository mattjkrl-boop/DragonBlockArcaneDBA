# Entity Objects Pass: Valor Field (Grand Sword) conversion report

Owner slot: Valor Field suspended shards. HEAD 6155dda. Files changed:
entity/ValorFieldEntity.java and client/render/ValorFieldRenderer.java.
GrandSwordItem.java was not edited: the audit names ValorFieldEntity.tick
as the only spawn point, and the item only spawns the field itself.
entity/FragmentEntity.java and its renderer were not touched.

## What moved

The five renderer-drawn shard meshes are now real FragmentEntity
instances in FIELD_HELD mode, spawned and held server side by the field:

- 3 dome shards: spawned in ValorFieldEntity.tick on the existing
  tickCount == 1 beat (the spawn sound and ground burst beat), anchored
  to the field entity itself with the renderer's own anchor formulas:
  angle i * 2.35 + 0.6, ring radius FIELD_RADIUS * (0.34 + 0.13 * i)
  (3.06, 4.23, 5.40), base height 1.9 + i * 0.85. Length 1.15, girth
  0.30, root 0x767F8C, tip 0xFFD833, seeds SEED_DOME + i * 104729, spin
  1.72 degrees per tick (the renderer's 0.03 rad rate), rest 10.
- Up to 2 marker shards: spawned in the suspension loop at the moment
  the field suspends a projectile, anchored to that projectile with
  offset (0, 0.55, 0), length 0.75, girth 0.20, root 0x8A93A6, tip
  0xFFF3B0, spin 2.86 degrees per tick (the renderer's 0.05 rad rate),
  rest 10. A slot index (0 or 1) keeps each marker's seed in the
  SEED_RING_OUTER + slot * 31337 family as markers recycle, matching
  the renderer's per-index seeds. Cap is 2 alive, the renderer's cap.
- Owner is the caster for both sets, so the field's own stasis scan
  (which skips projectiles owned by the caster) never suspends its own
  fragments, and owner-exempt pulls elsewhere exempt them too.

Seed re-homing: SEED_DOME (0xD09E5) and SEED_RING_OUTER (0xB111) moved
from the renderer into ValorFieldEntity as public constants, next to
the spawn code. The renderer's plasma stream still reads the identical
dome value as (int) ValorFieldEntity.SEED_DOME, so its stream seed is
unchanged. SEED_RING_INNER, SEED_RAYS, and SEED_LIP never moved.

The audit's accepted trade is implemented as specified: the renderer's
dome bob (sin(age * 0.07 + i * 2.1) * 0.30) is not reproduced. The
anchor uses base height only; the foundation's per-tick anchor follow
on the moving field carries the read.

## Counts before and after

- ValorFieldRenderer ComplexGeometry.shard call sites: 2 (one loop
  drawing 3 dome meshes, one loop drawing up to 2 marker meshes, inside
  one submitSolid) to 0. The whole submitSolid block is gone. Merge
  gate 1 is satisfied for this host.
- World objects per field: 0 fragments to 3 dome + up to 2 markers,
  spawned once per field life (dome) and on suspension (markers,
  recycling, never more than 2 alive). Well under the foundation caps.

## Release behavior

- Dome shards: released in beginFadeOut, on the same server call path
  that resumes the suspended projectiles, so they drop on the field's
  death beat, fall with the foundation physics, land, rest 10, fade 12.
- Marker shards: released on every path that removes their projectile
  from the suspended map, hooked into the same tick code: projectile
  gone or dead, radius exit resume, contact detonation, the 200 tick
  stasis timer discard, and field fade (inside releaseAllSuspended,
  beside the resume it already performs). The foundation's orphan rule
  (holder unresolvable) remains as the safety net for any missed path.
- release() is idempotent in the foundation, so a marker reached by
  both an individual removal and the fade path releases exactly once.

## Untouched, verified in the diff

- The entire projectile stasis system: suspension, resume, the contact
  detonation including the damage attribution block and its comment
  (the suspended projectile's own damage for its original owner) are
  byte-identical. Fragments deal no damage; they contain no damage
  calls and cannot be hurt (foundation contract).
- Field radius, duration, fade length, Ki drain, buffs, debuffs, and
  pressure damage numbers: unchanged. No host synced field changed;
  the only additions are server-side spawn and release calls and the
  bookkeeping maps they need.
- Renderer: the six rim tendrils, all five emission streams (plasma,
  boundary vapor, ground dust, sparks, stasis motes), the WISP_CURL
  layer, and the SPARK_STREAK suspension bursts are byte-identical.
  Only the shard block, the two moved seed constants, and the doc
  comments describing them changed.

## Verification

- Targeted javac (JDK 25, sourcepath src/main/java:src/client/java)
  over both owned files, private /tmp tags vf-262/vf-263-d573b5b1:
  - cp262: 4 errors total, zero in owned files; all 4 are the standing
    baseline (DbaItems 2, DbaMenus 2).
  - cp263: 10 errors total, zero in owned files; the standing 26.3
    baseline count (the 26.2 set plus the hostile entity kinds).
- git diff --check clean on both files. Added-lines em dash scan: zero
  (byte-level check for both em dash and en dash).
- No Gradle build run, no in-game look claimed.

## Change requests

None. The foundation hold() API covered the whole spec: explicit
length and girth for the audit's fixed sizes, per-fragment spin, rest
ticks, and the anchor follow all exist as designed. No gap found.

## Honest worse check

Three real trades, all inside the audit's own terms; nothing here can
be improved further inside the standard:

- The dome bob is gone (audit-accepted). On a stationary caster the
  old shards gently rose and fell by 0.30; the new ones hang at fixed
  anchor heights and only spin. On a moving field, which is the
  field's normal state, the anchor follow replaces the motion.
- Field death reads differently. The renderer shards dissolved with
  the dome's 12 tick fade envelope; the fragments are instead released
  at full alpha and drop, fading only after they land and rest (rest
  10 plus fade 12). That is the physical behavior this pass exists to
  create, and the audit specifies it, but the death moment is the one
  place the read genuinely changes character. First thing to look at
  on Carter's PC test.
- Marker choice under load: the renderer marked the first two
  projectiles in its client scan order; the server marks in suspension
  order. With three or more projectiles suspended, which two carry a
  marker can differ from the old read. Cap, size, color, and spin are
  identical.

Two smaller notes: fragments use the foundation spawn scale-in (0.5
to full over 5 ticks) instead of riding the renderer's 10 tick rise
envelope, so the dome shards pop to size slightly faster than the old
meshes grew; and the fragment axis is baked from the spawn-phase
direction with the tilt held in xRot while yRot spins server side,
rather than re-derived analytically every frame. Both read the same
in practice. Nothing was reverted; no part of this conversion reads
worse than the renderer version in a way the standard lets me fix.
