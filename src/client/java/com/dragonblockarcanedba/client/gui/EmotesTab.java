package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dragon Block Arcane Menu Tab - Emotes & Combat Stances.
 * Custom cyberpunk DBA styled cards with bright text and glowing neon borders.
 */
public class EmotesTab implements MenuTab {

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

    private DbaMenuScreen parentScreen;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parentScreen = screen;
    }

    private void selectEmote(String emoteId) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("action", "emote");
        nbt.putString("emote", emoteId);
        ClientPlayNetworking.send(new ActionPayload(nbt));
        if (parentScreen != null) {
            parentScreen.onClose();
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        int contentX = parentScreen.getContentX();
        int contentY = parentScreen.getContentY();
        int contentWidth = parentScreen.getContentWidth();
        int contentHeight = parentScreen.getContentHeight();

        // Header Title
        context.text(
            client.font,
            Component.literal("✦ SELECT AN EMOTE OR COMBAT STANCE ✦"),
            contentX + 10,
            contentY + 6,
            0xFFFFD700
        );

        int cols = 2;
        int btnWidth = (contentWidth - 28) / cols;
        int btnHeight = 24;
        int startX = contentX + 10;
        int startY = contentY + 22;

        for (int i = 0; i < EMOTES.size(); i++) {
            EmoteEntry entry = EMOTES.get(i);
            int col = i % cols;
            int row = i / cols;
            int bx = startX + col * (btnWidth + 8);
            int by = startY + row * (btnHeight + 5);

            boolean hovered = mouseX >= bx && mouseX <= bx + btnWidth && mouseY >= by && mouseY <= by + btnHeight;

            // Card background
            context.fill(bx, by, bx + btnWidth, by + btnHeight, hovered ? 0xEE16263D : 0xDD0D1626);
            // Glowing border
            int borderColor = hovered ? 0xFF55FF88 : 0xFF00E5FF;
            context.fill(bx, by, bx + btnWidth, by + 1, borderColor);
            context.fill(bx, by + btnHeight - 1, bx + btnWidth, by + btnHeight, borderColor);
            context.fill(bx, by, bx + 1, by + btnHeight, borderColor);
            context.fill(bx + btnWidth - 1, by, bx + btnWidth, by + btnHeight, borderColor);

            // Icon + Label
            String label = entry.icon() + "  " + entry.displayName();
            int textCol = hovered ? 0xFF55FF88 : 0xFFFFFFFF;
            context.text(client.font, Component.literal(label), bx + 8, by + 7, textCol);
        }

        // Bottom Cancel Button
        int cancelWidth = 140;
        int cancelHeight = 20;
        int cx = contentX + (contentWidth - cancelWidth) / 2;
        int cy = contentY + contentHeight - 26;
        boolean cancelHover = mouseX >= cx && mouseX <= cx + cancelWidth && mouseY >= cy && mouseY <= cy + cancelHeight;

        context.fill(cx, cy, cx + cancelWidth, cy + cancelHeight, cancelHover ? 0xEE331111 : 0xDD220B0B);
        int cBorder = cancelHover ? 0xFFFF6666 : 0xFFAA3333;
        context.fill(cx, cy, cx + cancelWidth, cy + 1, cBorder);
        context.fill(cx, cy + cancelHeight - 1, cx + cancelWidth, cy + cancelHeight, cBorder);
        context.fill(cx, cy, cx + 1, cy + cancelHeight, cBorder);
        context.fill(cx + cancelWidth - 1, cy, cx + cancelWidth, cy + cancelHeight, cBorder);
        context.centeredText(client.font, Component.literal("✖ Stop Emoting"), cx + cancelWidth / 2, cy + 5, cancelHover ? 0xFFFFFFFF : 0xFFFF7777);

        // Movement cancel hint
        context.centeredText(
            client.font,
            Component.literal("§7Movement (WASD), jumping, or attacks will smoothly cancel the emote."),
            contentX + contentWidth / 2,
            cy - 12,
            0xFF8899A6
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0) return false;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        int contentX = parentScreen.getContentX();
        int contentY = parentScreen.getContentY();
        int contentWidth = parentScreen.getContentWidth();
        int contentHeight = parentScreen.getContentHeight();

        int cols = 2;
        int btnWidth = (contentWidth - 28) / cols;
        int btnHeight = 24;
        int startX = contentX + 10;
        int startY = contentY + 22;

        for (int i = 0; i < EMOTES.size(); i++) {
            EmoteEntry entry = EMOTES.get(i);
            int col = i % cols;
            int row = i / cols;
            int bx = startX + col * (btnWidth + 8);
            int by = startY + row * (btnHeight + 5);

            if (mouseX >= bx && mouseX <= bx + btnWidth && mouseY >= by && mouseY <= by + btnHeight) {
                selectEmote(entry.id());
                return true;
            }
        }

        // Cancel button check
        int cancelWidth = 140;
        int cancelHeight = 20;
        int cx = contentX + (contentWidth - cancelWidth) / 2;
        int cy = contentY + contentHeight - 26;
        if (mouseX >= cx && mouseX <= cx + cancelWidth && mouseY >= cy && mouseY <= cy + cancelHeight) {
            selectEmote("");
            return true;
        }

        return false;
    }
}
