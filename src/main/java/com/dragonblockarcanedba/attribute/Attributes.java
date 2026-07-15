package com.dragonblockarcanedba.attribute;

import com.google.gson.JsonObject;

public record Attributes(
    int strength,
    int defense,
    int kiCapacity,
    int kiControl,
    int agility
) {
    public static Attributes fromJson(JsonObject json) {
        if (json == null) {
            return new Attributes(0, 0, 0, 0, 0);
        }
        return new Attributes(
            json.has("strength") ? json.get("strength").getAsInt() : 0,
            json.has("defense") ? json.get("defense").getAsInt() : 0,
            json.has("kiCapacity") ? json.get("kiCapacity").getAsInt() : 0,
            json.has("kiControl") ? json.get("kiControl").getAsInt() : 0,
            json.has("agility") ? json.get("agility").getAsInt() : 0
        );
    }

    public Attributes multiply(double scale) {
        return new Attributes(
            (int) (strength * scale),
            (int) (defense * scale),
            (int) (kiCapacity * scale),
            (int) (kiControl * scale),
            (int) (agility * scale)
        );
    }
}
