package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared chrome for every Dragon Block Arcane screen.
 * It uses the same sprites as the restyled pause menu: widget/button, widget/slider,
 * the menu background behind them, white labels, and the Rajdhani font from the
 * vanilla font override. Screens should not invent a second palette.
 */
public final class DbaUi {
    public static final int HEADER = 28;
    public static final int TAB = 20;
    public static final int STATUS = 14;
    public static final int PAD = 8;
    public static final int GAP = 4;
    public static final int BUTTON_H = 20;

    public static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    public static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
    public static final Identifier SLIDER = Identifier.withDefaultNamespace("widget/slider");
    public static final Identifier SLIDER_HANDLE = Identifier.withDefaultNamespace("widget/slider_handle");
    public static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");

    /** Navy field matching the menu background, used where a sprite cannot be blitted. */
    public static final int INK = 0xF0101824;
    public static final int INK_DEEP = 0xFF0A1220;
    public static final int HEADER_BG = 0xFF101A2A;
    public static final int ROW = 0xFF101A2A;
    public static final int ROW_HOVER = 0xFF163044;
    public static final int ROW_ON = 0xFF143848;
    public static final int LINE = 0xFF1C6E88;
    /** Cyan taken from the pause-button border. Kept under this name so existing call sites stay. */
    public static final int GOLD = 0xFF3AD4FF;
    public static final int GOLD_DIM = 0xFF1A7E9A;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFA9B9C9;
    public static final int DANGER = 0xFFFF6E6E;
    public static final int OK = 0xFF7EE0C8;

    public enum Kind {
        PRIMARY,
        SECONDARY,
        DANGER,
        QUIET
    }

    private DbaUi() {}

    /**
     * The game already paints the restyled menu background before extractRenderState.
     * An extra dim would make DBA screens darker than the pause menu.
     */
    public static void dim(GuiGraphicsExtractor g, int screenW, int screenH) {
    }

    /**
     * Pause has no second frame. Content sits on the menu background.
     * Callers still pass the content rectangle; this keeps that contract without painting a box.
     */
    public static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        sprite(g, BUTTON, x, y, w, h);
    }

    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        sprite(g, SLOT, x, y, 18, 18);
    }

    public static int closeX(int x, int w) {
        return x + w - 64;
    }

    public static int closeY(int y) {
        return y + 4;
    }

    public static void closeControl(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mouseX, int mouseY) {
        int cx = closeX(x, w);
        int cy = closeY(y);
        button(g, font, cx, cy, 56, BUTTON_H, "Close", hit(mouseX, mouseY, cx, cy, 56, BUTTON_H), Kind.SECONDARY);
    }

    public static boolean closeHit(int x, int y, int w, double mx, double my) {
        return hit(mx, my, closeX(x, w), closeY(y), 56, BUTTON_H);
    }

    public static void title(GuiGraphicsExtractor g, Font font, int x, int y, int maxW, String text) {
        int span = Math.max(8, maxW);
        g.centeredText(font, Component.literal(clip(font, text, span)), x + span / 2, y + 6, TEXT);
    }

    public static void tab(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String label, boolean active, boolean hover) {
        sprite(g, active || hover ? BUTTON_HIGHLIGHTED : BUTTON, x, y, w, h);
        int ty = y + Math.max(2, (h - 8) / 2);
        g.centeredText(font, Component.literal(clip(font, label, w - 8)), x + w / 2, ty, active ? TEXT : MUTED);
    }

    public static void row(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hover, boolean selected) {
        sprite(g, hover || selected ? BUTTON_HIGHLIGHTED : BUTTON, x, y, w, h);
    }

    public static void button(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String label, boolean hover, Kind kind) {
        int text = TEXT;
        if (kind == Kind.DANGER) {
            text = DANGER;
        } else if (kind == Kind.QUIET) {
            text = MUTED;
        }
        if (kind != Kind.QUIET) {
            sprite(g, hover ? BUTTON_HIGHLIGHTED : BUTTON, x, y, w, h);
        }
        int ty = y + Math.max(2, (h - 8) / 2);
        g.centeredText(font, Component.literal(clip(font, label, w - 8)), x + w / 2, ty, text);
    }

    public static void slider(GuiGraphicsExtractor g, Font font, int x, int y, int w, String label, String value, float percent) {
        g.text(font, Component.literal(clip(font, label, Math.max(8, w / 2))), x, y, TEXT);
        int valueW = font.width(value);
        g.text(font, Component.literal(value), x + w - valueW, y, TEXT);
        sliderTrack(g, x, y + 11, w, 16, percent);
    }

    public static void sliderTrack(GuiGraphicsExtractor g, int x, int y, int w, int h, float percent) {
        sprite(g, SLIDER, x, y, w, h);
        int handleW = Math.min(8, Math.max(4, w / 12));
        int travel = Math.max(1, w - handleW);
        float clamped = Math.max(0.0f, Math.min(1.0f, percent));
        int hx = x + Math.round(travel * clamped);
        sprite(g, SLIDER_HANDLE, hx, y, handleW, h);
    }

    public static boolean sliderHit(int x, int y, int w, double mx, double my) {
        return hit(mx, my, x - 2, y + 10, w + 4, 18);
    }

    public static void empty(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String message) {
        g.centeredText(font, Component.literal(clip(font, message, w - 24)), x + w / 2, y + h / 2 - 4, MUTED);
    }

    public static void hairline(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, LINE);
    }

    public static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public static boolean escape(KeyEvent event) {
        return event.key() == 256;
    }

    public static String clip(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int budget = maxWidth - font.width(ellipsis);
        if (budget <= 4) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return font.plainSubstrByWidth(text, budget) + ellipsis;
    }

    public static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String trial = current.isEmpty() ? word : current + " " + word;
            if (font.width(trial) <= maxWidth) {
                current = new StringBuilder(trial);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(font.width(word) <= maxWidth ? word : clip(font, word, maxWidth));
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    public static String slotKey(int slot) {
        KeyMapping key = switch (slot) {
            case 0 -> DragonBlockArcaneDBAClient.techSlot1Key;
            case 1 -> DragonBlockArcaneDBAClient.techSlot2Key;
            default -> DragonBlockArcaneDBAClient.techSlot3Key;
        };
        if (key == null) {
            return "Slot " + (slot + 1);
        }
        String name = key.getTranslatedKeyMessage().getString();
        if (name == null || name.isBlank() || name.startsWith("key.")) {
            return "Slot " + (slot + 1);
        }
        return name;
    }

    public static void playClick() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private static void sprite(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int w, int h) {
        if (w < 2 || h < 2) {
            return;
        }
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h, 0xFFFFFFFF);
    }
}
