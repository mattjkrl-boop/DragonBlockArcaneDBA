package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dragon Block Arcane - In-Game Quick Emote & Stance Selection Wheel / Screen.
 * Styled with DBA cyberpunk neon theme matching the rest of the UI.
 */
public class EmoteSelectionScreen extends Screen {

    public record EmoteEntry(String id, String displayName, String icon, String description, int color) {}

    private static final List<EmoteEntry> EMOTES = new ArrayList<>();

    static {
        EMOTES.add(new EmoteEntry("dance", "Victory Dance", "💃", "Celebrate triumph with an energetic rhythm", 0xFFFFD700));
        EMOTES.add(new EmoteEntry("wave", "Greeting Wave", "👋", "A friendly martial artist salute", 0xFF00FFCC));
        EMOTES.add(new EmoteEntry("talk", "Conversational", "🗣️", "Speak and gesture expressively", 0xFFFFAA00));
        EMOTES.add(new EmoteEntry("shout", "Ki Power Shout", "⚡", "Channel immense battle spirit with a roaring aura!", 0xFFFF3333));
        EMOTES.add(new EmoteEntry("zombie_walk", "Zombie Shuffle", "🧟", "Shuffle forward with an undead swagger", 0xFF55FF55));
        EMOTES.add(new EmoteEntry("sit", "Meditation / Sit", "🧘", "Sit down in deep spiritual concentration", 0xFF66CCFF));
        EMOTES.add(new EmoteEntry("kick_right", "High Kick", "🥋", "Unleash a high-speed martial arts roundhouse", 0xFFFF8800));
        EMOTES.add(new EmoteEntry("cross_punch_right", "Power Cross", "🥊", "Strike forward with a heavy boxing punch", 0xFFFF5555));
        EMOTES.add(new EmoteEntry("arm_parry", "Parry Guard", "🛡️", "Raise your guard in defensive anticipation", 0xFFAAAAFF));
        EMOTES.add(new EmoteEntry("sword_idle", "Blade Stance", "🗡️", "Poised two-handed katana ready stance", 0xFFE0E0FF));
    }

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public EmoteSelectionScreen() {
        super(Component.literal("DBA Emote Menu"));
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(420, this.width - 30);
        panelHeight = Math.min(270, this.height - 30);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
    }

    private void selectEmote(String emoteId) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("action", "emote");
        nbt.putString("emote", emoteId);
        ClientPlayNetworking.send(new ActionPayload(nbt));
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Dark translucent Dragon Ball styled card backdrop
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        // Main panel background with glowing neon border
        extractor.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF00A101C);
        extractor.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY, 0xFF00E5FF); // top
        extractor.fill(panelX - 1, panelY + panelHeight, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF00E5FF); // bottom
        extractor.fill(panelX - 1, panelY, panelX, panelY + panelHeight, 0xFF00E5FF); // left
        extractor.fill(panelX + panelWidth, panelY, panelX + panelWidth + 1, panelY + panelHeight, 0xFF00E5FF); // right

        // Header Title
        extractor.centeredText(this.font, Component.literal("✦ DRAGON BLOCK ARCANE — EMOTES & STANCES ✦"), panelX + panelWidth / 2, panelY + 12, 0xFFFFD700);
        extractor.centeredText(this.font, Component.literal("Click an emote to perform it. Movement (WASD) or attacks cancel the emote."), panelX + panelWidth / 2, panelY + 28, 0xFF8899A6);

        int cols = 2;
        int btnWidth = (panelWidth - 28) / cols;
        int btnHeight = 26;
        int startX = panelX + 10;
        int startY = panelY + 44;

        for (int i = 0; i < EMOTES.size(); i++) {
            EmoteEntry entry = EMOTES.get(i);
            int col = i % cols;
            int row = i / cols;
            int bx = startX + col * (btnWidth + 8);
            int by = startY + row * (btnHeight + 6);

            boolean hovered = mouseX >= bx && mouseX <= bx + btnWidth && mouseY >= by && mouseY <= by + btnHeight;

            extractor.fill(bx, by, bx + btnWidth, by + btnHeight, hovered ? 0xEE16263D : 0xDD0D1626);
            int borderColor = hovered ? 0xFF55FF88 : 0xFF00E5FF;
            extractor.fill(bx, by, bx + btnWidth, by + 1, borderColor);
            extractor.fill(bx, by + btnHeight - 1, bx + btnWidth, by + btnHeight, borderColor);
            extractor.fill(bx, by, bx + 1, by + btnHeight, borderColor);
            extractor.fill(bx + btnWidth - 1, by, bx + btnWidth, by + btnHeight, borderColor);

            String label = entry.icon() + "  " + entry.displayName();
            int textCol = hovered ? 0xFF55FF88 : 0xFFFFFFFF;
            extractor.text(this.font, Component.literal(label), bx + 8, by + 8, textCol);
        }

        // Close / Cancel button at bottom
        int cancelWidth = 140;
        int cancelHeight = 22;
        int cx = panelX + (panelWidth - cancelWidth) / 2;
        int cy = panelY + panelHeight - 30;
        boolean cancelHover = mouseX >= cx && mouseX <= cx + cancelWidth && mouseY >= cy && mouseY <= cy + cancelHeight;

        extractor.fill(cx, cy, cx + cancelWidth, cy + cancelHeight, cancelHover ? 0xEE331111 : 0xDD220B0B);
        int cBorder = cancelHover ? 0xFFFF6666 : 0xFFAA3333;
        extractor.fill(cx, cy, cx + cancelWidth, cy + 1, cBorder);
        extractor.fill(cx, cy + cancelHeight - 1, cx + cancelWidth, cy + cancelHeight, cBorder);
        extractor.fill(cx, cy, cx + 1, cy + cancelHeight, cBorder);
        extractor.fill(cx + cancelWidth - 1, cy, cx + cancelWidth, cy + cancelHeight, cBorder);
        extractor.centeredText(this.font, Component.literal("✖ Stop Emoting"), cx + cancelWidth / 2, cy + 6, cancelHover ? 0xFFFFFFFF : 0xFFFF7777);

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0) return false;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        int cols = 2;
        int btnWidth = (panelWidth - 28) / cols;
        int btnHeight = 26;
        int startX = panelX + 10;
        int startY = panelY + 44;

        for (int i = 0; i < EMOTES.size(); i++) {
            EmoteEntry entry = EMOTES.get(i);
            int col = i % cols;
            int row = i / cols;
            int bx = startX + col * (btnWidth + 8);
            int by = startY + row * (btnHeight + 6);

            if (mouseX >= bx && mouseX <= bx + btnWidth && mouseY >= by && mouseY <= by + btnHeight) {
                selectEmote(entry.id());
                return true;
            }
        }

        // Cancel button check
        int cancelWidth = 140;
        int cancelHeight = 22;
        int cx = panelX + (panelWidth - cancelWidth) / 2;
        int cy = panelY + panelHeight - 30;
        if (mouseX >= cx && mouseX <= cx + cancelWidth && mouseY >= cy && mouseY <= cy + cancelHeight) {
            selectEmote("");
            return true;
        }

        return super.mouseClicked(event, isRepeat);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
