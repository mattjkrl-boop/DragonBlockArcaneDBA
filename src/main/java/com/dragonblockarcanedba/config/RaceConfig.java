package com.dragonblockarcanedba.config;

public record RaceConfig(
    float hitboxWidth,
    float hitboxHeight,
    float eyeHeight,
    float eyeOffsetX,
    float eyeOffsetY,
    float eyeOffsetZ
) {
    public static final RaceConfig DEFAULT = new RaceConfig(0.6f, 1.8f, 1.62f, 0.0f, 7.5f, -2.1f);
}
