package com.dragonblockarcanedba.check;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Build-time check: every ability JSON goes through AbilityDef.parse, and every model,
 * clip and summoned ability it names must exist. Run by the checkAbilities task.
 */
public final class AbilityJsonCheck {
    public static void main(String[] args) throws IOException {
        Path res = Path.of(args[0]);
        String ns = "dragonblockarcanedba";
        Path abilities = res.resolve("data/" + ns + "/abilities");
        Path geo = res.resolve("assets/" + ns + "/geckolib/models/entity");
        Path anim = res.resolve("assets/" + ns + "/geckolib/animations/entity");
        List<String> errors = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(abilities)) {
            files = s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        for (Path f : files) {
            String path = abilities.relativize(f).toString().replace('\\', '/').replaceAll("\\.json$", "");
            try {
                JsonObject json = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                AbilityDef def = AbilityDef.parse(Identifier.fromNamespaceAndPath(ns, path), json);
                for (AbilityDef.ModelSpec m : def.models()) {
                    if (!Files.exists(geo.resolve(m.model() + ".geo.json"))) {
                        errors.add(path + ": model " + m.model() + " has no geo file");
                        continue;
                    }
                    Path a = anim.resolve(m.model() + ".animation.json");
                    JsonObject clips = Files.exists(a)
                            ? JsonParser.parseString(Files.readString(a)).getAsJsonObject().getAsJsonObject("animations")
                            : new JsonObject();
                    for (String c : new String[] {m.animSpawn(), m.animHold(), m.animEnd(), m.animAlt()}) {
                        if (c != null && !c.isEmpty() && !clips.has(c)) {
                            errors.add(path + ": model " + m.model() + " has no clip " + c);
                        }
                    }
                }
                for (AbilityDef.SummonSpec s : def.summons()) {
                    Identifier sid = Identifier.parse(s.ability());
                    if (!Files.exists(abilities.resolve(sid.getPath() + ".json"))) {
                        errors.add(path + ": summons missing ability " + s.ability());
                    }
                }
            } catch (Exception e) {
                errors.add(path + ": " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            throw new IllegalStateException(errors.size() + " ability JSON problem(s)");
        }
        System.out.println("checkAbilities: " + files.size() + " ability files OK");
    }
}
