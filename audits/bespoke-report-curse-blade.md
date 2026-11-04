# Bespoke Pass Report: Cursed Blade

Scope: item/CurseBladeItem.java, entities CurseChainEntity, CurseGroundShatterEntity,
CurseLightningEntity, CurseTelegraphEntity, SkyCracksEntity, AbyssalDomainEntity, and
their six renderers. Base commit 2ef870b. Not committed, per instructions.
Every number, cooldown, radius, and mechanic from the spectacle pass is unchanged.
Plain melee stays vanilla player attack by design (hurtEnemy untouched, item
attributes untouched).

## Before and after counts (all 13 files)

| Metric | Before | After | Note |
| --- | --- | --- | --- |
| SoundEvents references | 10 (9 call sites + import) | 0 | All in CurseBladeItem |
| ParticleTypes references | 0 | 0 | Entities already used DbaParticles (VOID_SMOKE, SPARK) |
| DustParticleOptions references | 0 | 0 | None existed |
| damageSources() call sites | 5 | 0 | 4 in the item, 1 in CurseChainEntity |
| Hand-emitted vertices (addVertex) | 85 | 0 | All geometry now emitted by BespokeGeometry |
| Local draw/render geometry helpers | 31 | 0 | Every private geometry helper declaration in the six renderers (drawQuad x3, renderBeam x3, rings, fans, boltHalf x2, spanRibbon, fadeStrip, and the rest) |
| java.util.Random in renderers | 4 files | 0 | Replaced by entity-synced seeds and BespokeGeometry.Rng |
| KiRenderHelper.draw* primitive calls | 0 | 0 | Was already zero; geometry was hand-rolled, now toolkit |
| BespokeGeometry call sites | 0 | 53 | Chain 9, Shatter 9, Lightning 6, Telegraph 8, SkyCracks 6, Domain 15 |

Remaining private helpers in the renderers, explained: segmentRibbon and
drawAuraRibbons in CurseChainRenderer are pose glue only (they copy a pose, aim it
along a path segment, and delegate all vertex emission to tornRibbon), and ease()
is a scalar curve. No vertices are built outside the toolkit anywhere.

## Sound swaps (CurseBladeItem, same call sites, volumes, pitches)

| Was | Now | Sites |
| --- | --- | --- |
| SoundEvents.CHAIN_PLACE | DbaSounds.CHAIN_RATTLE | chain stream |
| SoundEvents.ITEM_BREAK | DbaSounds.ITEM_SHATTER | disarm |
| SoundEvents.LIGHTNING_BOLT_THUNDER | DbaSounds.THUNDER_DARK | telegraphed strike, ambient bolt, ambient storm tick, verdict |
| SoundEvents.LIGHTNING_BOLT_IMPACT | DbaSounds.ZAP_DARK | telegraphed strike, ambient bolt, verdict |

DbaSounds.LIGHTNING_ZAP in the telegraphed strike was already custom and is kept.
No weapon plays thunder directly beyond these bank events; storm thunder belongs
to AtmosphereClient per the standard.

## Damage swaps (values identical, still capDamage wrapped)

| Site | Was | Now |
| --- | --- | --- |
| Telegraphed strike (450 + Str x 1.5) | damageSources().playerAttack | DbaDamage.curse(player) |
| Ambient bolt (120 + Str x 0.35) | damageSources().playerAttack | DbaDamage.curse(player) |
| Slam (220 + Str x 1.0) | damageSources().playerAttack | DbaDamage.curse(player) |
| Tempest Verdict (150 + 650 x ratio + Str x (0.8 + 1.7 x ratio)) | damageSources().playerAttack | DbaDamage.curse(player) |
| Chain attach (flat 75) | damageSources().mobProjectile | DbaDamage.curse(this, owner) |

## Geometry rebuild, per renderer

- SkyCracksRenderer (signature piece): seven main fractures are BespokeGeometry.branch
  bolts radiating from the heart, crimson sheath (width 0.34) with a white core
  (width 0.13) on the identical seed so the core runs exactly down the sheath path,
  endpoints jittered by BespokeGeometry.Rng from the entity's synced seed. The web
  between them is two crackStars (crimson radius 20, violet radius 14), and the tear
  shadow is a glowFan. Flat builders are drawn through a pose rotated so their ground
  plane becomes the billboard plane. Deleted: ribbon(), drawDiscFan() (28 segment
  fan), and the v() vertex shorthand.
- CurseChainRenderer: links are tornRibbon segments between the same arc/trail link
  centers (dark sheath 0.12/0.08/0.16, violet rune core on the same seed), knotted
  with displacedDome lumps at each center. Collar is three brokenVortexBands, the
  attach snap is a glowFan, glyphs and the hex core are displacedDomes, and the aura
  is two helical tornRibbon columns on the old helix path (rotated onto the trail in
  flight). Deleted: drawSpikedChainLink, drawDemonicShackleCollar,
  drawFloatingRunicGlyph, drawAuraRibbons (beam version), drawHexCore,
  drawSpikePyramid, drawChainSpine, spanRibbon, fadeStrip, drawDiamondQuad,
  renderBeam, drawQuad.
- AbyssalDomainRenderer: dome is two displacedDomes (outer crimson, inner violet at
  0.93 radius, counter-seeded), opening and eye are small displacedDome caps at the
  dome top point, whirlpool floor is four brokenVortexBands on rotated poses, mid
  rings and rim are brokenVortexBands, the sixteen spires are tornRibbons stood on
  end leaning inward, interior strikes are branch bolts on the same 8 tick flash
  cycle. LayeredRender.drawVortexBands and drawAnchorGlow kept (Phase 1 signature:
  pose based, seeds derived from radius and height, so the old seed argument is
  gone; radius, height, age, colors, and envelopes unchanged). Deleted:
  drawRotatingRing, draw3DShardPyramid, drawBoltRibbon, boltHalf, renderBeam,
  drawQuad, and both hand-built lat/long dome grids and the funnel stack.
- CurseLightningRenderer: trunk and twin bolt are branch bolts (sheath and white
  core share a seed; the trunk still stabs down over 2.5 ticks), ground marks are
  a crackStar over a glowFan. Deleted: renderJaggedBolt, boltHalf, drawGroundRing.
- CurseTelegraphRenderer: the pentagram rings are two crackStars, the contracting
  indicator is a brokenVortexBand, the six rising pillars are tornRibbons stood on
  end (sheath plus white core), the hex eye is a displacedDome. The entity has no
  synced seed field, so the seed derives from the entity id (stable per entity,
  same on every client for the marker's 20 tick life). Deleted: drawRotatingRing,
  renderBeam, drawOctahedron, drawQuad.
- CurseGroundShatterRenderer: fissures are two crackStars (crimson sheath, pale
  core), the shockwave is a brokenVortexBand over a glowFan, and the twelve slabs
  are tornRibbons stood on end leaning outward (dark sheath, crimson core). The
  spectacle fade fix (progress over the 35 tick lifetime) is unchanged. Deleted:
  drawGroundRing, drawTrenchSegment, drawRockSlab, drawQuad.

Seeds: SkyCracks uses the entity's synced SEED; Domain, Shatter, and Lightning use
the entity UUID MSB (as before); Chain and Telegraph use the entity id. All are
frame-stable and identical across clients.

## Verification (targeted javac, JDK 25, both classpaths)

Method: javac -proc:none -Xmaxerrs 10000 with the audit classpath argfiles,
-sourcepath src/main/java:src/client/java, compiling exactly the 13 Curse files so
the dependency closure is attributed with them.

- 26.2 (cp262.txt): 0 errors in all 13 Curse files, 43 closure classes emitted.
  Remaining errors: 4, all in shared files DbaMenus (2) and DbaItems (2), pre-existing
  baseline kinds, not Curse files.
- 26.3 (cp263.txt): 0 errors in all 13 Curse files, 43 closure classes emitted.
  Remaining errors: 11, all in shared closure files (DbaItems, DbaMenus,
  DbaHostileEntity, FriezaSoldierEntity, LavaSwordGround, NamekianWarriorEntity,
  PatrolRobotEntity), including the known pre-existing 26.3 LavaSwordGround kind.
  None are Curse files.

Process note, stated plainly: an early full-tree sweep showed zero Curse errors but
was misleading, because parse errors in a sibling weapon file (ErasureChargeOrbRenderer)
abort javac before it attributes the rest of the tree. The targeted compile above is
the reliable check, and it caught three real problems in this conversion, all fixed:
(1) Pose.rotate(Axis, float) does not exist on 26.2, replaced with
rotate(Axis.rotation(angle)) quaternion form, which exists on both versions;
(2) PoseStack.mulPose(Quaternionf) does not exist on 26.3, replaced with the
Matrix4f rotation form the old code used; (3) Phase 1 changed the LayeredRender
signatures (pose based, no seed argument on drawVortexBands), call sites updated.
No Gradle build was run; the compile check stands as javac only, per the standing
waiver.

## Toolkit gaps (shared change requests, not privately worked around)

1. No torus or ring builder: literal interlocking chain links cannot be drawn with
   the current toolkit. Links are ribbon segments with dome knots instead.
2. No octahedron or gem builder: the hex core and runic glyphs are displacedDome
   lumps. The irregular read fits the curse theme, but a shard or gem builder would
   restore the faceted look.
3. brokenVortexBand has no phase offset parameter: time rotation is done by
   rotating a copied pose, which works, but a phase parameter would be cleaner and
   cheaper.
4. No segment orientation helper in the toolkit: aiming tornRibbon along an
   arbitrary segment needs local pose glue (segmentRibbon in CurseChainRenderer).
   A toolkit aim helper would standardize this for every weapon with chains,
   trails, or tendrils.

## Honest worse check

Nothing was reverted. Three trades to name plainly:

- Chain links: the old torus links with barbed spikes had a literal iron-link
  silhouette. The ribbon and knot rebuild reads as a torn, knotted curse chain:
  heavier in motion, less literal as hardware. This is the one real visual trade,
  forced by gap 1 above, and it stays inside the standard.
- Domain funnel: the old funnel was a stacked 8 level cone; the rebuild is two
  counter-winding broken bands. It reads as storm, less solid. Acceptable, and the
  dome, whirlpool, and vortex bands carry the volume.
- Telegraph eye and chain glyphs: faceted octahedra became displaced lumps. In a
  20 tick warning marker and at chain scale this is not noticeable in play.

Everything else is equal or better: the sky cracks are strictly more jagged and
branched than the hand-rolled network, the dome wall is storm-torn instead of a
smooth lat/long bubble, and every effect now fades and flickers on the same
envelopes as before with zero vanilla particles, zero vanilla sounds, and zero
vanilla ability damage sources in the weapon's files.

## Other notes

- One stale comment in CurseBladeItem said AtmosphereServer saves and restores
  prior weather; updated to describe the bespoke tracked storm volume (Rule 5).
  The AtmosphereServer.begin call itself is unchanged.
- CurseLightningEntity remains registered and unspawned, as the spectacle pass
  left it; its renderer is converted anyway so the type is bespoke if it is
  ever spawned again.
- Effect files MovementCurseEffect and PetrificationCurseEffect show as modified
  in the working tree; they are not Curse Blade scope files and were not touched
  by this conversion.
