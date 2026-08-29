package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient;
import com.dragonblockarcanedba.network.C2SEquipTechniquePayload;
import com.dragonblockarcanedba.network.C2SUnlockTechniquePayload;
import com.dragonblockarcanedba.network.C2SUpgradeTechniquePayload;
import com.dragonblockarcanedba.registry.Technique;
import com.dragonblockarcanedba.registry.TechniqueRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Constellation / Node-Based Skill Tree for Arcane Techniques.
 * Inspired by Passive Skill Trees (Daripher):
 * - Cosmic space canvas with interactive node graph
 * - Illuminated golden/green prerequisite paths
 * - Tactile glowing nodes with level badges and mastery rings
 * - Smooth drag panning and canvas navigation
 * - Floating RPG hover tooltip with zero text overlap
 * - Dedicated Inspector Drawer for upgrading and equipping
 */
public class TechniquesTab implements MenuTab {
    private DbaMenuScreen parent;

    // Canvas Panning & Zoom (Static so location and zoom level are preserved across tab switches and menu closes)
    private static double panX = 0;
    private static double panY = 0;
    private static double zoom = 1.0;
    private static boolean hasInitializedView = false;

    private boolean isDraggingCanvas = false;
    private double lastDragMouseX = 0;
    private double lastDragMouseY = 0;

    // Selected Technique Inspector
    private String selectedTechId = "ki_sense";
    private boolean inspectorOpen = false;
    private double drawerScrollY = 0;

    // Selected slot to equip into (0=F7, 1=F8, 2=F9)
    private int selectedSlot = 0;

    // Stars for cosmic background
    private static final int STAR_COUNT = 60;
    private static final int[][] STARS = new int[STAR_COUNT][3]; // x, y, brightness

    static {
        java.util.Random rnd = new java.util.Random(1337);
        for (int i = 0; i < STAR_COUNT; i++) {
            STARS[i][0] = rnd.nextInt(800) - 400;
            STARS[i][1] = rnd.nextInt(600) - 300;
            STARS[i][2] = 120 + rnd.nextInt(135);
        }
    }

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.isDraggingCanvas = false;
        // Notice: We intentionally do NOT reset panX, panY, or zoom here so the player's view is preserved!
    }

    public static void fitAllView(int listW, int listH, List<Technique> techs) {
        if (techs == null || techs.isEmpty()) {
            panX = 0;
            panY = 0;
            zoom = 1.0;
            return;
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < techs.size(); i++) {
            Technique t = techs.get(i);
            int nx = t.x();
            int ny = t.y();
            if (nx < minX) minX = nx;
            if (nx > maxX) maxX = nx;
            if (ny < minY) minY = ny;
            if (ny > maxY) maxY = ny;
        }

        int spanX = Math.max(80, (maxX - minX) + 30);
        int spanY = Math.max(80, (maxY - minY) + 30);

        // Top 48px of canvas is reserved for the Equipped Techniques HUD so nodes don't collide with it
        int availW = Math.max(100, listW - 24);
        int availH = Math.max(100, listH - 52);

        double fitZoom = Math.min((double) availW / spanX, (double) availH / spanY);
        zoom = Math.max(0.20, Math.min(1.20, fitZoom));

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;

        panX = -midX * zoom;
        // Shift tree down so top nodes start cleanly below the Equipped Techniques HUD
        panY = -midY * zoom + 18.0;
    }

    private List<Technique> getAvailableTechniques(PlayerStatsAccessor accessor) {
        Identifier raceId = accessor.dba$getRaceId();
        if (raceId != null) {
            List<Technique> list = TechniqueRegistry.getTechniquesForRace(raceId);
            if (!list.isEmpty()) return list;
        }
        return TechniqueRegistry.getTechniquesForRace(Identifier.tryParse("dragonblockarcanedba:human"));
    }

    private int getNodeCanvasX(Technique tech, int index, int total) {
        if (tech.x() != 0 || tech.y() != 0) {
            return tech.x();
        }
        // Dynamic fallback circular distribution
        double angle = (2.0 * Math.PI * index) / Math.max(1, total) - Math.PI / 2.0;
        return (int) (Math.cos(angle) * 120.0);
    }

    private int getNodeCanvasY(Technique tech, int index, int total) {
        if (tech.x() != 0 || tech.y() != 0) {
            return tech.y();
        }
        // Dynamic fallback circular distribution
        double angle = (2.0 * Math.PI * index) / Math.max(1, total) - Math.PI / 2.0;
        return (int) (Math.sin(angle) * 120.0);
    }

    public static int getGroupColor(String group) {
        if (group == null) return 0xFF55FF88;
        return switch (group.toLowerCase(Locale.ROOT)) {
            case "arcane" -> 0xFF00E5FF;     // Vivid Cyan / Azure
            case "celestial" -> 0xFFFFDD44;  // Radiant Solar Amber Gold
            case "astral" -> 0xFFBF55EC;     // Mystic Amethyst Purple
            case "abyssal" -> 0xFFFF4444;    // Primal Ruby Crimson
            default -> 0xFF55FF88;           // Core Awakened Mint
        };
    }

    public static String getGroupName(String group) {
        if (group == null) return "CORE ABILITIES";
        return switch (group.toLowerCase(Locale.ROOT)) {
            case "arcane" -> "ARCANE CONSTELLATION";
            case "celestial" -> "CELESTIAL CONSTELLATION";
            case "astral" -> "ASTRAL CONSTELLATION";
            case "abyssal" -> "ABYSSAL CONSTELLATION";
            default -> "CORE ABILITIES";
        };
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;

        int listX = parent.getContentX();
        int listY = parent.getContentY();
        int listW = parent.getContentWidth();
        int listH = parent.getContentHeight();

        // 1. Scissor to content area
        context.enableScissor(listX, listY, listX + listW, listY + listH);

        // 2. Cosmic Deep Space Background
        context.fill(listX, listY, listX + listW, listY + listH, 0xFF060910);

        int centerX = listX + listW / 2 + (int) Math.round(panX);
        int centerY = listY + listH / 2 + (int) Math.round(panY);

        // Twinkling stars
        long time = System.currentTimeMillis();
        for (int i = 0; i < STAR_COUNT; i++) {
            int sx = centerX + STARS[i][0];
            int sy = centerY + STARS[i][1];
            if (sx >= listX && sx <= listX + listW && sy >= listY && sy <= listY + listH) {
                int twinkle = (int) (Math.sin(time / 400.0 + i) * 35);
                int alpha = Math.max(40, Math.min(255, STARS[i][2] + twinkle));
                int starColor = (alpha << 24) | 0xCCDDEE;
                context.fill(sx, sy, sx + 1, sy + 1, starColor);
            }
        }

        List<Technique> techs = getAvailableTechniques(accessor);

        // Auto-fit on first opening
        if (!hasInitializedView) {
            fitAllView(listW, listH, techs);
            hasInitializedView = true;
            centerX = listX + listW / 2 + (int) Math.round(panX);
            centerY = listY + listH / 2 + (int) Math.round(panY);
        }

        // 3. Draw Prerequisite Lines (Paths) Color-Coded by Group
        for (int i = 0; i < techs.size(); i++) {
            Technique tech = techs.get(i);
            if (tech.hasPrerequisites()) {
                int childX = centerX + (int) Math.round(getNodeCanvasX(tech, i, techs.size()) * zoom);
                int childY = centerY + (int) Math.round(getNodeCanvasY(tech, i, techs.size()) * zoom);

                int grpCol = getGroupColor(tech.group());
                int rgb = grpCol & 0x00FFFFFF;

                for (String pId : tech.prerequisiteTechniqueIds()) {
                    for (int p = 0; p < techs.size(); p++) {
                        Technique parentTech = techs.get(p);
                        if (parentTech.id().equals(pId)) {
                            int parentX = centerX + (int) Math.round(getNodeCanvasX(parentTech, p, techs.size()) * zoom);
                            int parentY = centerY + (int) Math.round(getNodeCanvasY(parentTech, p, techs.size()) * zoom);

                            boolean parentUnlocked = accessor.dba$hasTechnique(pId) || accessor.dba$getTechniqueLevel(pId) > 0;
                            int lineColor = parentUnlocked ? (0xEE000000 | rgb) : (0x44000000 | rgb);
                            int glowColor = parentUnlocked ? (0x44000000 | rgb) : (0x11000000 | rgb);

                            int glowW = (zoom >= 0.75) ? 4 : 2;
                            int lineW = (zoom >= 0.75) ? 2 : 1;

                            drawLine(context, parentX, parentY, childX, childY, glowColor, glowW);
                            drawLine(context, parentX, parentY, childX, childY, lineColor, lineW);
                            break;
                        }
                    }
                }
            }
        }

        // 4. Draw Nodes (Radius scales smoothly with zoom, Color-Coded by Group)
        Technique hoveredTech = null;
        int hoveredScreenX = 0;
        int hoveredScreenY = 0;

        int nodeRadius = Math.max(6, Math.min(15, (int) Math.round(11 * Math.sqrt(zoom))));
        int clickR = Math.max(9, nodeRadius + 3);

        for (int i = 0; i < techs.size(); i++) {
            Technique tech = techs.get(i);
            int nodeX = centerX + (int) Math.round(getNodeCanvasX(tech, i, techs.size()) * zoom);
            int nodeY = centerY + (int) Math.round(getNodeCanvasY(tech, i, techs.size()) * zoom);

            int grpCol = getGroupColor(tech.group());
            int rgb = grpCol & 0x00FFFFFF;

            int level = accessor.dba$getTechniqueLevel(tech.id());
            boolean unlocked = accessor.dba$hasTechnique(tech.id()) || level > 0;
            boolean active = accessor.dba$isTechniqueActive(tech.id());
            int playerLevel = accessor.dba$getLevel();
            boolean meetsLevel = playerLevel >= tech.unlockLevel();

            // Check prerequisites
            boolean prereqsMet = true;
            String missingPrereqName = null;
            if (tech.hasPrerequisites()) {
                for (String prereqId : tech.prerequisiteTechniqueIds()) {
                    if (!accessor.dba$hasTechnique(prereqId)) {
                        prereqsMet = false;
                        Technique prereqTech = TechniqueRegistry.getTechnique(Identifier.tryParse(prereqId));
                        missingPrereqName = (prereqTech != null) ? prereqTech.name() : prereqId;
                        break;
                    }
                }
            }

            boolean isEquipped = tech.id().equals(accessor.dba$getEquippedTechnique(0))
                || tech.id().equals(accessor.dba$getEquippedTechnique(1))
                || tech.id().equals(accessor.dba$getEquippedTechnique(2));

            boolean isSelected = tech.id().equals(selectedTechId) && inspectorOpen;
            boolean isHover = (mouseX >= nodeX - clickR && mouseX <= nodeX + clickR
                && mouseY >= nodeY - clickR && mouseY <= nodeY + clickR);

            if (isHover) {
                hoveredTech = tech;
                hoveredScreenX = nodeX;
                hoveredScreenY = nodeY;
            }

            // Outer Aura & Glow Rings (Color-Coded)
            if (level >= 10) {
                // Mastered Sunburst Aura
                drawCircleRing(context, nodeX, nodeY, nodeRadius + 3, 2, 0x66FFDD44);
                drawCircleRing(context, nodeX, nodeY, nodeRadius + 1, 1, 0xFFFFDD44);
                drawCircle(context, nodeX, nodeY, nodeRadius, 0xDD2A1E06);
            } else if (unlocked) {
                // Unlocked: Group-Colored Aura & Double Rings
                drawCircleRing(context, nodeX, nodeY, nodeRadius + 2, 1, 0x55000000 | rgb);
                drawCircleRing(context, nodeX, nodeY, nodeRadius + 1, 1, 0xFF000000 | rgb);
                drawCircle(context, nodeX, nodeY, nodeRadius, 0xDD000000 | ((rgb >> 2) & 0x3F3F3F));
            } else if (prereqsMet && meetsLevel) {
                // Available to Unlock: Pulsing Group Aura
                int pulseAlpha = 80 + (int) (Math.sin(time / 250.0) * 50);
                drawCircleRing(context, nodeX, nodeY, nodeRadius + 2, 1, (pulseAlpha << 24) | rgb);
                drawCircleRing(context, nodeX, nodeY, nodeRadius + 1, 1, 0xFF000000 | rgb);
                drawCircle(context, nodeX, nodeY, nodeRadius, 0xDD000000 | ((rgb >> 3) & 0x1F1F1F));
            } else {
                // Locked / Unavailable: Muted Group-Tinted Ring & Dark Obsidian Core
                drawCircleRing(context, nodeX, nodeY, nodeRadius + 1, 1, 0x66000000 | rgb);
                drawCircle(context, nodeX, nodeY, nodeRadius, 0xEE10141C);
            }

            // Selection Accent Ring
            if (isSelected) {
                drawCircleRing(context, nodeX, nodeY, nodeRadius + 4, 1, 0xFFFFFFFF);
            }

            // Center Glyph / Monogram (visible if zoom allows)
            if (zoom >= 0.40) {
                String initials = getInitials(tech.name());
                int glyphCol = unlocked ? 0xFFFFFFFF : (prereqsMet && meetsLevel ? grpCol : (0x88000000 | rgb));
                context.centeredText(client.font, Component.literal(initials), nodeX, nodeY - 4, glyphCol);
            }

            // Active / Equipped Dot
            if (active || isEquipped) {
                drawCircle(context, nodeX + nodeRadius - 2, nodeY - nodeRadius + 2, 3, 0xFF55FF88);
                drawCircle(context, nodeX + nodeRadius - 2, nodeY - nodeRadius + 2, 1, 0xFFFFFFFF);
            }

            // Under-Node Level Badge Pill (Visible when zoom >= 0.50)
            if (zoom >= 0.50) {
                String badge;
                int badgeBg;
                int badgeTxtCol;
                if (level >= 10) {
                    badge = "Lv.10";
                    badgeBg = 0xDD443306;
                    badgeTxtCol = 0xFFFFDD44;
                } else if (unlocked) {
                    badge = "Lv." + level;
                    badgeBg = 0xDD000000 | ((rgb >> 2) & 0x3F3F3F);
                    badgeTxtCol = grpCol;
                } else if (!prereqsMet || !meetsLevel) {
                    badge = "LOCK";
                    badgeBg = 0xDD141820;
                    badgeTxtCol = 0x88000000 | rgb;
                } else {
                    badge = "UNLOCK";
                    badgeBg = 0xDD000000 | ((rgb >> 2) & 0x3F3F3F);
                    badgeTxtCol = grpCol;
                }

                int bW = client.font.width(badge) + 6;
                int bH = 9;
                int bX = nodeX - bW / 2;
                int bY = nodeY + nodeRadius + 2;
                context.fill(bX, bY, bX + bW, bY + bH, badgeBg);
                context.fill(bX, bY, bX + bW, bY + 1, badgeTxtCol);
                context.centeredText(client.font, Component.literal(badge), nodeX, bY + 1, badgeTxtCol);
            }
        }

        // 5. Quick HUD: Equipped Slots [F7, F8, F9] (Top Left of canvas)
        renderEquippedSlotsHud(context, listX + 8, listY + 8, accessor);

        // 6. Recenter Button (Bottom Left of canvas)
        int recW = 60;
        int recH = 14;
        int recX = listX + 8;
        int recY = listY + listH - recH - 8;
        boolean hoverRec = mouseX >= recX && mouseX <= recX + recW && mouseY >= recY && mouseY <= recY + recH;
        context.fill(recX, recY, recX + recW, recY + recH, hoverRec ? 0xAA223344 : 0x66111822);
        context.fill(recX, recY, recX + recW, recY + 1, 0x8800E5FF);
        String recText = String.format(Locale.US, "⌖ %.0f%%", zoom * 100.0);
        context.centeredText(client.font, Component.literal(recText), recX + recW / 2, recY + 3, hoverRec ? 0xFFFFFFFF : 0xFF88CCEE);

        // 7. Inspector Drawer (Slide-out panel on right if a node is selected)
        if (inspectorOpen && selectedTechId != null) {
            Technique sel = TechniqueRegistry.getTechnique(Identifier.tryParse(selectedTechId));
            if (sel != null) {
                renderInspectorDrawer(context, mouseX, mouseY, listX + listW - 170, listY, 170, listH, sel, accessor);
            }
        }

        // 8. Floating RPG Tooltip (Drawn above everything if hovered and not dragging)
        if (hoveredTech != null && !isDraggingCanvas && !inspectorOpen) {
            renderFloatingTooltip(context, mouseX, mouseY, listX, listY, listW, listH, hoveredTech, accessor);
        }

        context.disableScissor();
    }

    private void renderEquippedSlotsHud(GuiGraphicsExtractor context, int startX, int startY, PlayerStatsAccessor accessor) {
        Minecraft client = Minecraft.getInstance();
        String[] defaultKeys = {"F7", "F8", "F9"};

        // Sleek frosted glass backing plate so panned nodes glide behind cleanly
        int hudW = 3 * 58 - 4;
        int hudH = 35;
        context.fill(startX - 4, startY - 3, startX + hudW + 4, startY + hudH + 2, 0xF0080D16);
        context.fill(startX - 4, startY - 3, startX + hudW + 4, startY - 2, 0x4400E5FF);
        context.fill(startX - 4, startY + hudH + 1, startX + hudW + 4, startY + hudH + 2, 0x2200E5FF);

        context.text(client.font, Component.literal("EQUIPPED TECHNIQUES"), startX, startY, 0xFF55FF88);

        for (int i = 0; i < 3; i++) {
            int slotX = startX + i * 58;
            int slotY = startY + 11;
            int slotW = 54;
            int slotH = 22;

            boolean isCurrentSlot = (i == selectedSlot);
            String equippedId = accessor.dba$getEquippedTechnique(i);
            boolean hasTech = equippedId != null && !equippedId.isEmpty();
            Technique tech = hasTech ? TechniqueRegistry.getTechnique(Identifier.tryParse(equippedId)) : null;

            int bg = isCurrentSlot ? 0xDD0E2B1E : 0xAA0B131C;
            int border = isCurrentSlot ? 0xFF55FF88 : 0x4400E5FF;

            context.fill(slotX, slotY, slotX + slotW, slotY + slotH, bg);
            context.fill(slotX, slotY, slotX + slotW, slotY + 1, border);
            context.fill(slotX, slotY + slotH - 1, slotX + slotW, slotY + slotH, border);
            context.fill(slotX, slotY, slotX + 1, slotY + slotH, border);
            context.fill(slotX + slotW - 1, slotY, slotX + slotW, slotY + slotH, border);

            // Key tag
            String keyStr = "[" + defaultKeys[i] + "]";
            context.text(client.font, Component.literal(keyStr), slotX + 4, slotY + 3, 0xFFFFAA00);

            // Name
            String nameStr = (tech != null) ? client.font.plainSubstrByWidth(tech.name(), 46) : "EMPTY";
            int nameCol = (tech != null) ? 0xFFFFFFFF : 0xFF556677;
            context.text(client.font, Component.literal(nameStr), slotX + 4, slotY + 12, nameCol);
        }
    }

    private void renderInspectorDrawer(GuiGraphicsExtractor context, int mouseX, int mouseY,
                                      int drawX, int drawY, int drawW, int drawH, Technique tech, PlayerStatsAccessor accessor) {
        Minecraft client = Minecraft.getInstance();

        int grpCol = getGroupColor(tech.group());

        // Background
        context.fill(drawX, drawY, drawX + drawW, drawY + drawH, 0xF40A101A);
        context.fill(drawX, drawY, drawX + 1, drawY + drawH, grpCol); // Glowing border line in group color

        // Title and Close Button (Stationary Header)
        context.text(client.font, Component.literal(getGroupName(tech.group())), drawX + 8, drawY + 8, grpCol);

        int cW = 14;
        int cH = 14;
        int cX = drawX + drawW - cW - 6;
        int cY = drawY + 6;
        boolean hoverClose = mouseX >= cX && mouseX <= cX + cW && mouseY >= cY && mouseY <= cY + cH;
        context.fill(cX, cY, cX + cW, cY + cH, hoverClose ? 0xAAFF4444 : 0x33442222);
        context.centeredText(client.font, Component.literal("✕"), cX + cW / 2, cY + 3, hoverClose ? 0xFFFFFFFF : 0xFFAAAAAA);

        // Scissored Scrollable Content
        int scrollStartY = drawY + 24;
        int scrollH = drawH - 28;
        context.enableScissor(drawX, scrollStartY, drawX + drawW, scrollStartY + scrollH);

        int curY = scrollStartY + (int) drawerScrollY;

        int level = accessor.dba$getTechniqueLevel(tech.id());
        boolean unlocked = accessor.dba$hasTechnique(tech.id()) || level > 0;
        int playerLevel = accessor.dba$getLevel();
        boolean meetsLevel = playerLevel >= tech.unlockLevel();

        // Check prerequisites
        boolean prereqsMet = true;
        String missingPrereqName = null;
        if (tech.hasPrerequisites()) {
            for (String prereqId : tech.prerequisiteTechniqueIds()) {
                if (!accessor.dba$hasTechnique(prereqId)) {
                    prereqsMet = false;
                    Technique prereqTech = TechniqueRegistry.getTechnique(Identifier.tryParse(prereqId));
                    missingPrereqName = (prereqTech != null) ? prereqTech.name() : prereqId;
                    break;
                }
            }
        }

        int upgradeCost = PlayerStats.getTechniqueUpgradeCost(tech.id(), Math.min(10, Math.max(1, level + 1)));
        boolean canAfford = accessor.dba$getStatPoints() >= upgradeCost;

        // Tech Name
        context.text(client.font, Component.literal(tech.name()), drawX + 8, curY, 0xFFFFFFFF);
        curY += 13;

        // Level & Progress Bar
        String lvlStr = unlocked ? ("Level " + level + " / 10") : (prereqsMet ? "Available" : "Locked");
        int lvlCol = unlocked ? 0xFFFFAA00 : (prereqsMet ? 0xFF55FF88 : 0xFFFF5555);
        context.text(client.font, Component.literal(lvlStr), drawX + 8, curY, lvlCol);
        curY += 11;

        int pX = drawX + 8;
        int pY = curY;
        int pW = drawW - 16;
        int pH = 4;
        context.fill(pX, pY, pX + pW, pY + pH, 0x88000000);
        int fillW = (int) (pW * (Math.min(10, Math.max(0, level)) / 10.0f));
        context.fill(pX, pY, pX + fillW, pY + pH, (level >= 10) ? 0xFFFFDD44 : 0xFF00E5FF);
        curY += 8;

        // Requirements info
        if (!unlocked && !prereqsMet) {
            String reqTxt = "🔒 Req: " + missingPrereqName;
            context.text(client.font, Component.literal(reqTxt), drawX + 8, curY, 0xFFFF5555);
            curY += 11;
        } else if (!unlocked && !meetsLevel) {
            String reqTxt = "Req Player Lv. " + tech.unlockLevel();
            context.text(client.font, Component.literal(reqTxt), drawX + 8, curY, 0xFFFF8844);
            curY += 11;
        }

        // Description (cleanly wrapped)
        String desc = tech.description();
        if (desc != null && !desc.isEmpty()) {
            List<String> lines = wrapText(client.font, desc, drawW - 16);
            for (String l : lines) {
                context.text(client.font, Component.literal(l), drawX + 8, curY, 0xFFAAAAAA);
                curY += 10;
            }
        }
        curY += 6;

        // Unlock / Upgrade Action Button (compact: 18px)
        int btnW = drawW - 16;
        int btnH = 18;
        int bX = drawX + 8;
        int bY = curY;
        boolean hoverBtn = mouseX >= bX && mouseX <= bX + btnW && mouseY >= bY && mouseY <= bY + btnH && mouseY >= scrollStartY && mouseY <= scrollStartY + scrollH;

        String actionTxt;
        int actionBg;
        int actionBorder;
        int actionTxtCol;

        if (!unlocked) {
            if (!prereqsMet) {
                actionTxt = "LOCKED (REQ TECH)";
                actionBg = 0x44221111;
                actionBorder = 0x66FF4444;
                actionTxtCol = 0xFFFF5555;
            } else if (meetsLevel) {
                actionTxt = (upgradeCost > 0) ? ("UNLOCK (" + upgradeCost + " AP)") : "UNLOCK (FREE)";
                actionBg = canAfford ? (hoverBtn ? 0xDD00C853 : 0xAA009624) : 0x44332222;
                actionBorder = canAfford ? 0xFF00FF77 : 0x88FF4444;
                actionTxtCol = canAfford ? 0xFFFFFFFF : 0xFF888888;
            } else {
                actionTxt = "REQ LEVEL " + tech.unlockLevel();
                actionBg = 0x33332211;
                actionBorder = 0x66FF8844;
                actionTxtCol = 0xFFFF8844;
            }
        } else if (level < 10) {
            actionTxt = (upgradeCost > 0) ? ("UPGRADE (" + upgradeCost + " AP)") : "UPGRADE (FREE)";
            actionBg = canAfford ? (hoverBtn ? 0xDD00E5FF : 0xAA0099CC) : 0x33223333;
            actionBorder = canAfford ? 0xFF00E5FF : 0x66888888;
            actionTxtCol = canAfford ? 0xFFFFFFFF : 0xFF888888;
        } else {
            actionTxt = "MAX LEVEL (10)";
            actionBg = 0x33222222;
            actionBorder = 0x55FFAA00;
            actionTxtCol = 0xFFFFAA00;
        }

        context.fill(bX, bY, bX + btnW, bY + btnH, actionBg);
        context.fill(bX, bY, bX + btnW, bY + 1, actionBorder);
        context.fill(bX, bY + btnH - 1, bX + btnW, bY + btnH, actionBorder);
        context.fill(bX, bY, bX + 1, bY + btnH, actionBorder);
        context.fill(bX + btnW - 1, bY, bX + btnW, bY + btnH, actionBorder);
        context.centeredText(client.font, Component.literal(actionTxt), bX + btnW / 2, bY + 5, actionTxtCol);

        curY += btnH + 10;

        // Equip to Quick-Slots Buttons (compact: 15px)
        if (unlocked) {
            context.text(client.font, Component.literal("ASSIGN TO QUICK-SLOT:"), drawX + 8, curY, 0xFF55FF88);
            curY += 11;

            for (int s = 0; s < 3; s++) {
                int eqH = 15;
                int eqY = curY + s * (eqH + 3);
                boolean isEquippedInThisSlot = tech.id().equals(accessor.dba$getEquippedTechnique(s));
                boolean hoverEq = mouseX >= bX && mouseX <= bX + btnW && mouseY >= eqY && mouseY <= eqY + eqH && mouseY >= scrollStartY && mouseY <= scrollStartY + scrollH;

                int eqBg = isEquippedInThisSlot ? 0xDD114422 : (hoverEq ? 0x77162638 : 0x440E1824);
                int eqBorder = isEquippedInThisSlot ? 0xFF55FF88 : 0x3300E5FF;
                String slotLabel = "SLOT " + (s + 1) + " [F" + (s + 7) + "] " + (isEquippedInThisSlot ? "✓ EQUIPPED" : "+ EQUIP");

                context.fill(bX, eqY, bX + btnW, eqY + eqH, eqBg);
                context.fill(bX, eqY, bX + btnW, eqY + 1, eqBorder);
                context.fill(bX, eqY + eqH - 1, bX + btnW, eqY + eqH, eqBorder);
                context.fill(bX, eqY, bX + 1, eqY + eqH, eqBorder);
                context.fill(bX + btnW - 1, eqY, bX + btnW, eqY + eqH, eqBorder);
                context.centeredText(client.font, Component.literal(slotLabel), bX + btnW / 2, eqY + 4, isEquippedInThisSlot ? 0xFF55FF88 : 0xFFCCDDEE);
            }
        }

        context.disableScissor();
    }

    private void renderFloatingTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY,
                                      int listX, int listY, int listW, int listH, Technique tech, PlayerStatsAccessor accessor) {
        Minecraft client = Minecraft.getInstance();

        int level = accessor.dba$getTechniqueLevel(tech.id());
        boolean unlocked = accessor.dba$hasTechnique(tech.id()) || level > 0;
        int upgradeCost = PlayerStats.getTechniqueUpgradeCost(tech.id(), Math.min(10, Math.max(1, level + 1)));

        boolean prereqsMet = true;
        String missingPrereqName = null;
        if (tech.hasPrerequisites()) {
            for (String prereqId : tech.prerequisiteTechniqueIds()) {
                if (!accessor.dba$hasTechnique(prereqId)) {
                    prereqsMet = false;
                    Technique prereqTech = TechniqueRegistry.getTechnique(Identifier.tryParse(prereqId));
                    missingPrereqName = (prereqTech != null) ? prereqTech.name() : prereqId;
                    break;
                }
            }
        }

        int grpCol = getGroupColor(tech.group());
        int tipW = 158;
        int tipH = 78;

        int tipX = mouseX + 12;
        int tipY = mouseY - 10;

        // Keep inside bounds
        if (tipX + tipW > listX + listW - 4) {
            tipX = mouseX - tipW - 10;
        }
        if (tipY + tipH > listY + listH - 4) {
            tipY = listY + listH - tipH - 4;
        }
        if (tipY < listY + 4) {
            tipY = listY + 4;
        }

        // Draw Tooltip Box
        context.fill(tipX, tipY, tipX + tipW, tipY + tipH, 0xF40A101C);
        int borderCol = unlocked ? grpCol : (prereqsMet ? 0xFF55FF88 : 0xFFFF5555);
        context.fill(tipX, tipY, tipX + tipW, tipY + 1, borderCol);
        context.fill(tipX, tipY + tipH - 1, tipX + tipW, tipY + tipH, borderCol);
        context.fill(tipX, tipY, tipX + 1, tipY + tipH, borderCol);
        context.fill(tipX + tipW - 1, tipY, tipX + tipW, tipY + tipH, borderCol);

        // Group Header Tag
        context.text(client.font, Component.literal("✦ " + getGroupName(tech.group())), tipX + 6, tipY + 5, grpCol);

        // Title
        context.text(client.font, Component.literal("★ " + tech.name()), tipX + 6, tipY + 16, 0xFFFFFFFF);

        // Level
        String lvlStr = unlocked ? ("Level: " + level + "/10") : "Status: Locked";
        context.text(client.font, Component.literal(lvlStr), tipX + 6, tipY + 27, unlocked ? 0xFFFFAA00 : 0xFF888888);

        // Requirements
        int y = tipY + 38;
        if (!unlocked) {
            if (!prereqsMet) {
                context.text(client.font, Component.literal("🔒 Requires: " + missingPrereqName), tipX + 6, y, 0xFFFF5555);
            } else {
                String costStr = (upgradeCost > 0) ? ("Cost: " + upgradeCost + " AP") : "Cost: Free (0 AP)";
                context.text(client.font, Component.literal(costStr), tipX + 6, y, 0xFF55FF88);
            }
            y += 11;
        }

        // Description
        String cleanDesc = client.font.plainSubstrByWidth(tech.description(), tipW - 12);
        context.text(client.font, Component.literal(cleanDesc), tipX + 6, y, 0xFFAAAAAA);

        // Prompt
        context.text(client.font, Component.literal("Click to inspect & equip"), tipX + 6, tipY + tipH - 11, 0xFF55FF88);
    }

    private List<String> wrapText(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String w : words) {
            String test = current.length() == 0 ? w : (current + " " + w);
            if (font.width(test) <= maxWidth) {
                current = new StringBuilder(test);
            } else {
                if (current.length() > 0) result.add(current.toString());
                current = new StringBuilder(w);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    private String getInitials(String name) {
        String[] words = name.split(" ");
        if (words.length >= 2) {
            return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase();
        } else if (name.length() >= 2) {
            return name.substring(0, 2).toUpperCase();
        }
        return name.toUpperCase();
    }

    private void drawLine(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color, int thickness) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) return;
        int steps = (int) (dist / 2);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int px = (int) (x1 + t * dx);
            int py = (int) (y1 + t * dy);
            context.fill(px - thickness / 2, py - thickness / 2, px + thickness / 2 + 1, py + thickness / 2 + 1, color);
        }
    }

    private void drawCircle(GuiGraphicsExtractor context, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int w = (int) Math.sqrt(radius * radius - y * y);
            context.fill(cx - w, cy + y, cx + w + 1, cy + y + 1, color);
        }
    }

    private void drawCircleRing(GuiGraphicsExtractor context, int cx, int cy, int radius, int thickness, int color) {
        int rInner = radius - thickness;
        for (int y = -radius; y <= radius; y++) {
            int wOuter = (int) Math.sqrt(radius * radius - y * y);
            int wInner = (Math.abs(y) <= rInner) ? (int) Math.sqrt(rInner * rInner - y * y) : 0;
            if (wInner > 0) {
                context.fill(cx - wOuter, cy + y, cx - wInner, cy + y + 1, color);
                context.fill(cx + wInner + 1, cy + y, cx + wOuter + 1, cy + y + 1, color);
            } else {
                context.fill(cx - wOuter, cy + y, cx + wOuter + 1, cy + y + 1, color);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        double mx = event.x();
        double my = event.y();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;

        int listX = parent.getContentX();
        int listY = parent.getContentY();
        int listW = parent.getContentWidth();
        int listH = parent.getContentHeight();

        if (mx < listX || mx > listX + listW || my < listY || my > listY + listH) {
            return false;
        }

        // 1. Recenter button click
        int recW = 60;
        int recH = 14;
        int recX = listX + 8;
        int recY = listY + listH - recH - 8;
        if (mx >= recX && mx <= recX + recW && my >= recY && my <= recY + recH) {
            DbaMenuScreen.playClickSound();
            List<Technique> availableTechs = getAvailableTechniques(accessor);
            fitAllView(listW, listH, availableTechs);
            return true;
        }

        // 2. Equipped slots click (Top Left)
        for (int i = 0; i < 3; i++) {
            int slotX = listX + 8 + i * 58;
            int slotY = listY + 19;
            int slotW = 54;
            int slotH = 22;
            if (mx >= slotX && mx <= slotX + slotW && my >= slotY && my <= slotY + slotH) {
                DbaMenuScreen.playClickSound();
                this.selectedSlot = i;
                return true;
            }
        }

        // 3. Inspector Drawer clicks (if open)
        if (inspectorOpen && selectedTechId != null) {
            int drawW = Math.min(180, listW / 2);
            int drawX = listX + listW - drawW;
            int drawY = listY;
            int drawH = listH;

            if (mx >= drawX && mx <= drawX + drawW && my >= drawY && my <= drawY + drawH) {
                // Close Inspector
                int cW = 14;
                int cH = 14;
                int cX = drawX + drawW - cW - 6;
                int cY = drawY + 6;
                if (mx >= cX && mx <= cX + cW && my >= cY && my <= cY + cH) {
                    DbaMenuScreen.playClickSound();
                    this.inspectorOpen = false;
                    return true;
                }

                int scrollStartY = drawY + 24;
                int scrollH = drawH - 28;

                if (my >= scrollStartY && my <= scrollStartY + scrollH) {
                    Technique tech = TechniqueRegistry.getTechnique(Identifier.tryParse(selectedTechId));
                    if (tech != null) {
                        int level = accessor.dba$getTechniqueLevel(tech.id());
                        boolean unlocked = accessor.dba$hasTechnique(tech.id()) || level > 0;
                        int upgradeCost = PlayerStats.getTechniqueUpgradeCost(tech.id(), Math.min(10, Math.max(1, level + 1)));
                        boolean canAfford = accessor.dba$getStatPoints() >= upgradeCost;

                        // Prerequisite check
                        boolean prereqsMet = true;
                        if (tech.hasPrerequisites()) {
                            for (String prereqId : tech.prerequisiteTechniqueIds()) {
                                if (!accessor.dba$hasTechnique(prereqId)) {
                                    prereqsMet = false;
                                    break;
                                }
                            }
                        }

                        int curY = scrollStartY + (int) drawerScrollY;
                        curY += 13; // tech name
                        curY += 11; // level string
                        curY += 8;  // progress bar
                        if (!unlocked && (!prereqsMet || accessor.dba$getLevel() < tech.unlockLevel())) {
                            curY += 11;
                        }
                        String desc = tech.description();
                        if (desc != null && !desc.isEmpty()) {
                            curY += wrapText(client.font, desc, drawW - 16).size() * 10;
                        }
                        curY += 6;

                        // Unlock / Upgrade button
                        int bW = drawW - 16;
                        int btnH = 18;
                        int bX = drawX + 8;
                        int bY = curY;
                        if (mx >= bX && mx <= bX + bW && my >= bY && my <= bY + btnH) {
                            DbaMenuScreen.playClickSound();
                            if (!unlocked) {
                                if (prereqsMet && accessor.dba$getLevel() >= tech.unlockLevel() && canAfford) {
                                    ClientPlayNetworking.send(new C2SUnlockTechniquePayload(tech.id()));
                                }
                            } else if (level < 10 && canAfford) {
                                ClientPlayNetworking.send(new C2SUpgradeTechniquePayload(tech.id()));
                            }
                            return true;
                        }

                        // Equip Slots buttons
                        if (unlocked) {
                            int eqStartY = bY + btnH + 21;
                            for (int s = 0; s < 3; s++) {
                                int eqH = 15;
                                int eqY = eqStartY + s * (eqH + 3);
                                if (mx >= bX && mx <= bX + bW && my >= eqY && my <= eqY + eqH) {
                                    DbaMenuScreen.playClickSound();
                                    boolean isAlready = tech.id().equals(accessor.dba$getEquippedTechnique(s));
                                    ClientPlayNetworking.send(new C2SEquipTechniquePayload(s, isAlready ? "" : tech.id()));
                                    return true;
                                }
                            }
                        }
                    }
                }
                return true;
            }
        }

        // 4. Node clicks on canvas
        List<Technique> techs = getAvailableTechniques(accessor);
        int centerX = listX + listW / 2 + (int) Math.round(panX);
        int centerY = listY + listH / 2 + (int) Math.round(panY);
        int nodeRadius = Math.max(6, Math.min(15, (int) Math.round(11 * Math.sqrt(zoom))));
        int clickR = Math.max(9, nodeRadius + 3);

        for (int i = 0; i < techs.size(); i++) {
            Technique tech = techs.get(i);
            int nodeX = centerX + (int) Math.round(getNodeCanvasX(tech, i, techs.size()) * zoom);
            int nodeY = centerY + (int) Math.round(getNodeCanvasY(tech, i, techs.size()) * zoom);

            if (mx >= nodeX - clickR && mx <= nodeX + clickR
                && my >= nodeY - clickR && my <= nodeY + clickR) {
                DbaMenuScreen.playClickSound();
                this.selectedTechId = tech.id();
                this.inspectorOpen = true;
                this.drawerScrollY = 0;
                return true;
            }
        }

        // 5. Start background panning
        this.isDraggingCanvas = true;
        this.lastDragMouseX = mx;
        this.lastDragMouseY = my;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDraggingCanvas) {
            double mx = event.x();
            double my = event.y();
            this.panX += (mx - lastDragMouseX);
            this.panY += (my - lastDragMouseY);
            this.lastDragMouseX = mx;
            this.lastDragMouseY = my;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDraggingCanvas = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listX = parent.getContentX();
        int listY = parent.getContentY();
        int listW = parent.getContentWidth();
        int listH = parent.getContentHeight();

        // 1. Inspector Drawer text scrolling (if open and mouse is over drawer)
        int drawW = Math.min(180, listW / 2);
        int drawX = listX + listW - drawW;
        if (inspectorOpen && mouseX >= drawX && mouseX <= drawX + drawW && mouseY >= listY && mouseY <= listY + listH) {
            this.drawerScrollY += verticalAmount * 16.0;
            this.drawerScrollY = Math.min(0, Math.max(-140, this.drawerScrollY));
            return true;
        }

        // 2. Canvas Zoom In / Zoom Out with mouse scroll wheel
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            double oldZoom = zoom;
            double factor = (verticalAmount > 0) ? 1.15 : 0.87;
            double newZoom = Math.max(0.20, Math.min(2.50, oldZoom * factor));

            if (Math.abs(newZoom - oldZoom) > 0.0001) {
                int listCenterX = listX + listW / 2;
                int listCenterY = listY + listH / 2;

                // Zoom smoothly anchored to mouse cursor location
                panX = panX + (mouseX - listCenterX - panX) * (1.0 - newZoom / oldZoom);
                panY = panY + (mouseY - listCenterY - panY) * (1.0 - newZoom / oldZoom);
                zoom = newZoom;
                return true;
            }
        }

        return false;
    }
}
