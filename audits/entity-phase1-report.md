# Entity Objects Pass, Phase 1 report: audit, standard, foundation

Date: Nov 4, 2026. Base: ef14656 (complex effects redo). This phase
audited the seven conversions, wrote the binding standard, and built
the shared foundation. No host was converted yet; that is Phase 2,
judged by the gates in audits/ENTITY-STANDARD.md. Nothing here was
pushed.

## What landed

- audits/entity-audit.md: per-host conversion specs for SkyCracks,
  Temporal Rift, Dimensional Rift, Void Rift, Warp Rift, TimeShatter,
  and Valor Field, each with today's exact piece counts, sizes,
  trajectories, lifetimes, colors, seed families, the layers the host
  renderer keeps, and the server spawn point with counts and caps.
- audits/ENTITY-STANDARD.md: the FragmentEntity contract (synced
  fields, modes, physics, lifetime and fade rules, caps, chunk and
  restart safety, registration, renderer rules) plus the seven Phase 2
  merge gates.
- entity/FragmentEntity.java: the shared fragment entity with the full
  spawn API (burst, rain, hang, hold, release).
- entity/DbaEntities.java: FRAGMENT registered after DEBRIS
  (id "fragment", sized 0.5, tracking range 10, update interval 1).
- client/render/FragmentRenderer.java plus its registration beside
  DEBRIS in DragonBlockArcaneDBAClient.

## Design decisions and their reasons

- One shared entity, not seven. All seven hosts draw the same
  ComplexGeometry shard silhouette; what differs is theme color, size
  unit, and motion. One entity with synced seed, colors, length,
  girth, and mode covers all of them, and Phase 2 owners get one API
  instead of seven bespoke entities to keep consistent.
- Four mode values, two physics paths, one held path. BALLISTIC and
  BURST share the DebrisEntity physics (gravity 0.045, terminal -1.8,
  per-mode drag, block-below landing test) and differ only in helper
  tuning. FROZEN and FIELD_HELD share a held phase that releases into
  the same physics. The standard records the collapse plainly instead
  of pretending four engines exist.
- Size and seeds are synced, rolled server side. The helpers roll
  length and girth over shardBurst's own size classes and derive each
  fragment's mesh seed with the burst salt (baseSeed + index *
  0x9E37L), so a fragment renders the identical silhouette the
  renderer burst drew for that index, on every client, as world truth.
- Non-persistence is structural. FragmentEntity overrides
  shouldBeSaved() to false (verified present on the 26.2 Entity API),
  writes no save data, keeps no SavedData and no static state. Chunk
  unload or restart simply ends fragments. No orphan cleanup pass
  exists because none is needed.
- FIELD_HELD anchors to the holder, not to the host class.
  FragmentEntity stores a holder UUID plus an anchor offset and
  follows holder.position() each tick; it has no ValorField-specific
  code. If the holder vanishes, the fragment releases and drops that
  tick (a safety net; the authored release calls in Phase 2 set the
  real timing). This mirrors how ValorFieldEntity already tracks
  suspended projectiles by UUID.
- Fade follows the FADE_START precedent. The server stamps FADE_START
  when the fade window opens (rest elapsed, lifetime reached, or the
  2400 tick backstop), the renderer fades over the shared FADE_TICKS
  12, and the server discards at the end. Held fragments cannot be
  aged out from under their hosts: helper lifetimes sit beyond the
  longest honest hold.
- The FROZEN growth rule lives in the renderer, keyed to RELEASE_AT.
  TimeShatter's shards grow 0.55 to 1.40 on a cubic-out curve while
  they hang; RELEASE_AT is exactly that window, so the renderer
  reproduces the curve from synced data with no TimeShatter-specific
  code anywhere.
- Landing is felt, cheaply. A landed fragment sends a two-count
  SHARD_CHIP puff, the landing tell DebrisEntity already uses, at
  fragment scale. No per-fragment continuous emission exists in the
  foundation; hosts keep their own layers.
- No lang entry. Debris and the trident shard, the two fragment-class
  entities already in the mod, carry no entity name in en_us.json;
  the fragment follows them.
- No damage, verified before building. A grep across all seven hosts
  and the four spawning items found every shard path purely visual;
  the hosts deal their own damage (SkyCracks' own javadoc states its
  bolts are visual only). FragmentEntity contains no damage call,
  cannot be hurt, and is not pickable.

## Per-host spec summary

Full specs in audits/entity-audit.md. The short version:

| Host | Mode | Count | Spawn beat |
| --- | --- | --- | --- |
| SkyCracks | BALLISTIC rain | 5 per beat, cap 40 | host tick, tickCount % 20 == 10 |
| Temporal Rift | BURST | 5 | host tick 2, heart of the tear |
| Dimensional Rift | BURST | 5 | host tick 2, early heart |
| Void Rift | FROZEN ring | 5 | first host tick, release in triggerImplosion |
| Warp Rift | BURST | 4 per rift | first host tick, both rifts |
| TimeShatter | FROZEN fan | 5 per shatter | item, at all three spawn sites |
| Valor Field | FIELD_HELD | 3 dome + up to 2 markers | field tick 1; markers on suspension |

Honest trades recorded in the audit, all deliberate: renderer billboard
planes (SkyCracks clusters, TimeShatter fan, Void Rift rim anchors)
become world-anchored planes, because entities cannot face a camera;
Temporal Rift's slow pose rotation and Valor dome's bob are not
reproduced (server spin replaces the dome's rotation read); the
Dimensional Rift fan bursts at the early heart and does not track the
growing column (tendrils and emission keep that read); Void Rift's
ring keeps fixed world heights instead of the renderer's live ellipse
tilt. Two renderer constants Phase 2 must re-home server side are
named in the audit (TimeShatter's fan seed salts, Valor's dome and
ring seeds).

## Verification

- Dual javac sweeps against a clean baseline worktree at ef14656:
  baseline 132 error kinds on the 26.2 classpath and 138 on the 26.3
  classpath; current tree 132 and 138; comm -13 prints nothing on
  either version, so the foundation adds zero new error kinds and
  neither FragmentEntity nor FragmentRenderer appears in either error
  output. (First sweep caught two real mistakes in the new renderer,
  a wrong CameraRenderState import and a stale submitSolid call shape;
  both were fixed against the actual ComplexGeometry API and the
  re-sweep is clean. The baseline numbers above are the fresh
  measurements, matching the pass brief.)
- All 567 resource JSONs parse.
- git diff --check clean. Added-lines em dash scan: zero (the two em
  dashes in DragonBlockArcaneDBAClient.java are pre-existing at
  ef14656, verified by git show, and are not in added lines).
- No Gradle build exists in this VM and none is claimed. The compile
  check here is the javac audit sweep only, per the standing rule.

## Honest risks for Phase 2

- In-game look is unverified until Carter's PC test, as with every
  pass in this VM. Specific watches: fragment scale against the hosts
  they left (the renderer drew at billboard offsets; entities sit at
  true positions), the TimeShatter growth window reading at real
  distances, and SkyCracks rain density from the ground.
- Server tick cost: fragments are real entities at updateInterval 1.
  Worst cases are bounded by the audit caps (SkyCracks about 30 alive,
  Valor Field 5), far under the area cap of 256, but a server stress
  test with several tempest channels at once is worth one PC session.
- The orphan release in FIELD_HELD fires the tick after a holder is
  removed if a host forgets its release calls; Phase 2 owners must
  wire the authored releases (field fade, implosion, every suspended
  map removal) so the net never carries the timing.
- TimeShatter's three item spawn sites all need the hang set; missing
  one leaves that variant's shards renderer-drawn, which merge gate 1
  will catch only if the checker greps WhisStaffItem as well as the
  renderers. The audit names all three sites.
