# Attack Upgrade Pass, Phase 2 Owner Report: Azure Dragon Sword and Dabura's Sword

Owner slot: Azure Dragon Sword (section 6 of audits/attack-complexity-audit.md) and Dabura's Sword (section 7), plus the foundation contract in audits/attack-foundation-addendum.md. Base HEAD ccfebf2. No commits made. BoltRenderer.java and BoltEntity.java are untouched: the git diff for both files is empty, and the shared bolt branch math is byte-identical.

All changes are presentation only, inside nine client renderer files. No damage, cooldown, cost, timing, mechanic, or synced field changed anywhere. No entity or item file was edited. Every new physical piece body (storm-glass shards, basalt spikes, dark steel shards, the outer shatter plate ring) draws through ComplexGeometry.submitSolidLit with the render state's lightCoords. No new physical piece was placed on the emissive path.

## Azure Dragon Sword

### AzureTempestChannelRenderer

- Before: 3 individual ComplexGeometry.tendril calls in one submitEnergy, no solid pieces.
- After: 1 tendrilFan call of 5 strands rising through the tunnel (spread 34 degrees, length 0.94 of tunnel height), plus a ring of 6 small shard pieces through submitSolidLit (storm-glass blue, root 0x1D6F8E to tip 0xCFF7FF) orbiting at waist height (0.48 of tunnel height, radius 0.78 of tunnel radius) at the same 0.06 radians per tick rotation the old triad used.
- Piece count: 3 strands and 0 shards before, 5 strands and 6 shards after, 11 authored pieces total.

### AzureStormRenderer

- Before: 2 tendrilFan calls of 2 strands each, 4 strands total, no solid pieces.
- After: both fans raised to 4 strands each, 8 strands total (plus 2 per fan, as specced). Added a shardBurst shed at the storm rim: 4 small shards (unit 0.55, spread 58 degrees, root 0x1E6F8E to tip 0xD9FFFF) through submitSolidLit, drawn for the first 3 ticks of each 10 tick bucket at a seeded rim point, fading across that window, with the bucket seed changing the rim point each cycle.
- Piece count: 4 strands before, 8 strands sustained after, 12 pieces at the shed peak.

### AzureSonicQuakeRenderer

- Before: 1 fracturePlates call plus 1 crackFan call.
- After: a second crackFan at 0.6 of the main crack radius. The builder has no fork direction parameter, so the second fan is drawn through a mirrored local pose (negative X scale on a copied pose), which flips the fork chirality: its forks run against the main web instead of copying it. Same palette, alpha 0.82 against the main fan's 0.90.
- Piece count: 2 builder calls before, 3 after; crack fans 1 to 2.

### AzureRushTrailRenderer

- Pieces unchanged, as specced: the same 3 tendril sections on the wake centerline.
- Sprite layer: the renderer had no SPARK_STREAK emitter to double (its complex sprite layer was WISP_CURL at 26 per second, cap 40). A SPARK_STREAK emitter was added on the same shell volume source at 52 per second, cap 80, exactly double the wisp layer's rate and cap.
- Counts: emitter layers 5 to 6; pieces 3 to 3.

### AzureLightningRenderer (telegraph only)

- Before: 1 crackFan whose base branch count is the distance scaled segment count (9 at distance scale 1.0, rolling 9 to 11 with the builder's seeded bonus), plus a 3 strand tendrilFan.
- After: the telegraph crackFan's density factor is raised by 1.22, so the base branch count is 11 at distance scale 1.0 (rolling 11 to 13): two extra branches, as specced. crackFan has no explicit branch count parameter; the density scale is the only in-foundation control over its branch count, and it is applied to the telegraph fan only. The afterglow fan, the tendrilFan arcs, the radius cap of 0.78 of the warned disc, and all emission are unchanged.
- Piece count: base branches 9 to 11 at full distance scale; builder calls unchanged.

## Dabura's Sword

### DarknessDomainRenderer (the eclipse spike rim)

- Before: 6 rim tendrils through submitVeil plus 1 crackFan at the base. No solid pieces.
- After: a spikeEruption RING added at 0.98 of the domain radius, drawn through submitSolidLit in a near-black basalt palette (root 0xFF050408, tip 0xFF332A3A). Per Carter's steering during this pass (clustered, uneven, fewer and bigger over many and small), the ring runs at the spec minimum of 10 spikes with sizeScale 1.12, so spike lengths roll about 1.34 to 3.58 blocks in the builder's three classes, with the builder's seeded per-spike rise stagger (up to 0.35 of the rise) varying arrival order and height read. The violet seams are a second aligned spikeEruption through submitVeil at sizeScale 1.18 and alpha 0.22, so violet shows only as a thin edge seam around the stone bodies, never as the body itself.
- Animation: riseFactor is the synced fade (the channel envelope), so the spikes rise across the existing 30 tick build, hold at fade 1 while the domain holds, and sinkFactor runs as 1 minus fade once age passes the build ticks, retracting them across the existing teardown. On the first frame the retract is observed, one SHARD_CHIP burst of 36 chips fires around the rim (guarded per domain seed in the render state), so the rim cracks away as it retracts.
- Piece count: 7 authored pieces before (6 tendrils, 1 fan), 27 after (plus 10 stone spikes and 10 seam spikes). Vertex cost: each spike submit is 3840 vertices at distance scale 1.0 (10 spikes at 384 vertices), inside the 6000 per-submit class L budget from the addendum; the two spike submits together are the heaviest addition in this slot and are flagged below.
- Untouched, as required: domain radius growth, the projectile slow factor, channel and teardown timing, and every synced field. Only the renderer's own render state gained two bookkeeping fields for the one-shot chip burst.

### DarknessWaveRenderer

- Before: 3 individual tendril calls across three height lanes.
- After: 1 tendrilFan call of 4 strands, origin at the left rim on the wave's own right axis, spread 16 degrees, length 1.90 of the half span, width 0.44, same veil path and palette.
- Piece count: 3 strands to 4 strands.

### DarknessBladeRenderer

- Before: 1 tendrilFan of 3 strands as the wake, no solid pieces.
- After: 3 shard pieces through submitSolidLit (dark steel, root 0xFF0B0A10, tip 0xFF8A8694) riding the blade arc: positioned at heights 1.25, 0, and minus 1.25 on the same roll angle the emission sources sample (yRot plus age times 18 degrees), each aligned to the roll tangent and tipped downward, lengths 0.98, 0.82, and 0.66.
- Piece count: 3 strands before, 3 strands plus 3 shards after, 6 pieces total.

### DarknessShatterRenderer

- Before: 1 crackFan plus 1 fracturePlates ring at 0.85 of the shock radius, lift 0.38.
- After: a second fracturePlates ring through submitSolidLit at 1.25 radius (the expanding shock radius times 1.25, capped at 1.25 of the entity radius), lift 0.22, a smaller class than the inner ring, in a darker cut of the same palette (0x3B1470 to 0x09000F).
- Piece count: plate rings 1 to 2 (each ring is the builder's plate fan, about 10 plates at distance scale 1.0 before seeded gaps); builder calls 2 to 3.

## Verification

- Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8, sourcepath src/main/java:src/client/java) over all nine owned renderers, full logs read, never summaries:
  - cp262: 4 errors, all standing baseline closure kinds (DbaItems x2, DbaMenus x2). Zero errors in any owned file.
  - cp263: 10 errors, exactly the documented baseline set (the 26.2 four plus DbaHostileEntity x2, PatrolRobotEntity x2, FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero errors in any owned file.
  - DarknessDomainRenderer was recompiled a second time after the steering edits (count 10, sizeScale 1.12 and 1.18) with the same result on both classpaths: baseline closure errors only, zero in the owned file.
- git diff --check on the nine owned files: clean.
- Byte-level added-lines scan for em dash and en dash bytes across the nine owned files: zero.
- Diff scope: the nine client renderers listed above, plus this report. BoltRenderer.java and BoltEntity.java diffs are empty.
- No Gradle build was run or is claimed. The in-game look is unverified until Carter's PC test.

## Change requests (foundation gaps, filed not fixed)

1. crackFan has no branch count or fork direction parameter. Azure Lightning's plus 2 branches is implemented through its density scale (exact at distance scale 1.0, proportional below it), and Azure Sonic Quake's reversed forks through a mirrored pose. A branchDelta and forkSign parameter on crackFan would make both exact and remove the pose trick.
2. spikeEruption's RING layout has no clustering or phase control: placement is an even ring with plus or minus 0.15 radian jitter. The owner-side steering response was count and size only (10 spikes, sizeScale 1.12). True clustered, uneven placement needs the merge-side builder irregularity upgrade already announced; until it lands, the rim's spacing is the builder's ring, and its unevenness comes from length classes and rise stagger only.
3. tendrilFan has no per-strand origin offsets: all strands share one origin. The Tempest Channel and Darkness Wave fans therefore converge at a point where the old pieces started apart. A distributed-origin fan variant would preserve lane structure.
4. AzureRushTrailRenderer had no SPARK_STREAK layer to double. The new layer was set at double the WISP_CURL layer's rate and cap; if the audit intended a different base, the audit should name it.

## Honest worse check

- Darkness Domain spike rim, placement: with the current builder the ten spikes stand on an even ring. They are large, bent, faceted, and staggered, and they do not read as small uniform bars, but they are not yet clustered. This is the slot's top PC-test and merge item, and it is a builder limitation, not an owner choice: the announced merge upgrade is where clustering lands. If Carter judges the even spacing toy-like in game, the fix is that builder upgrade, not more spikes.
- Darkness Domain seam shell: the violet seam is a full aligned duplicate at 1.18 sizeScale and 0.22 alpha. If the veil path renders stronger than expected, it will read as a violet outline rather than a seam. It is deliberately thin and dark, but it is the second thing to judge in game.
- Darkness Domain cost: the two spike submits add up to 7680 vertices at distance scale 1.0 on top of the existing tendrils and crack fan. One channeled domain is the expected case; stacked domains at close range are the load risk.
- Darkness Domain chip burst timing: the burst fires on the first frame the renderer observes the retract. A client that starts observing mid-teardown gets the burst at that moment rather than at teardown start. One burst per domain, never repeated.
- Darkness Wave and Azure Tempest fans: both mandated fans converge at a single origin. The wave's strands no longer each span rim to rim at separate heights, and the channel's fan no longer starts at the tunnel rim the way the old helix triad did. Strand counts and solid pieces rose, but these two reads are less authored than what they replaced in that one respect, and they are flagged for the merge realism review rather than hidden.
- Azure Sonic Quake mirrored fan: the negative X pose is the one transform trick in this slot. The crack geometry is double-sided, so it should render identically from both faces, but it is unverified in game. The inner fan also adds web density where the plates already sit; at small radii it may read busy.
- Azure Storm shed: the burst is drawn geometry on a 3 tick window, not physics shards. At low envelope it may read as a blink at the rim, and the rim point jumps each bucket by design.
- Azure Lightning density: the 1.22 factor also raises per-crack segment counts slightly, so the web reads a little denser overall, not only two branches wider.
- Darkness Blade shards: the renderer has no solid blade body of its own (the wake carries the plunge), so the three steel shards may read as escorting fragments rather than parts of one blade. They are anchored to the same roll the emissions use, which is the best available anchor without touching the entity.
- Darkness Shatter outer ring: the ring expands at 1.25 times the shock radius until it reaches its cap, so its plates can appear slightly ahead of the visible crest line. That is the specced radius relationship, named here so the PC test can judge it with the cause attached.
- Nothing was reverted: every change above is the audit spec as written, with the Dabura rim count and size adjusted once under Carter's mid-pass steering. The risks named here are read risks for the PC test and the merge realism review, not known regressions found in code.
