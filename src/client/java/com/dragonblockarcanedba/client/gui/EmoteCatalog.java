package com.dragonblockarcanedba.client.gui;

import java.util.List;

/** Emotes the menu and the wheel both offer. Ids match the server emote action. */
public final class EmoteCatalog {
    public record Entry(String id, String name, String detail) {}

    public static final List<Entry> ALL = List.of(
            new Entry("dance", "Victory Dance", "A short celebration"),
            new Entry("wave", "Greeting Wave", "A martial salute"),
            new Entry("talk", "Conversational", "Speak and gesture"),
            new Entry("shout", "Ki Shout", "A battle cry"),
            new Entry("zombie_walk", "Zombie Shuffle", "An undead walk"),
            new Entry("sit", "Meditate", "Sit and concentrate"),
            new Entry("kick_right", "High Kick", "A rising kick"),
            new Entry("cross_punch_right", "Power Cross", "A heavy punch"),
            new Entry("arm_parry", "Parry Guard", "Raise a guard"),
            new Entry("sword_idle", "Blade Stance", "A ready stance")
    );

    private EmoteCatalog() {}
}
