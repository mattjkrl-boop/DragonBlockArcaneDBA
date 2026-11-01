package com.dragonblockarcanedba.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Gates the handful of mixins whose vanilla targets changed shape in 26.3, so one
 * jar runs on both 26.2 and 26.3 and up. Everything not named here always applies.
 * See audits/MC-263-COMPAT-AUDIT.md items 13 and 15.
 */
public class DbaMixinPlugin implements IMixinConfigPlugin {

    private static Boolean atLeast263;

    private static boolean isAtLeast263() {
        if (atLeast263 != null) return atLeast263;
        boolean result = false;
        try {
            String friendly = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
            String[] parts = friendly.split("[.\\-]");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                int minor = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                result = major > 26 || (major == 26 && minor >= 3);
            }
        } catch (RuntimeException ignored) {
        }
        atLeast263 = result;
        return result;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean newer = isAtLeast263();
        switch (mixinClassName) {
            // Multi-layer Otherworld clouds: addCloudsPass hook before 26.3,
            // CloudRenderer prepare/render hooks from 26.3.
            case "com.dragonblockarcanedba.mixin.CloudTextureMixin":
                return !newer;
            case "com.dragonblockarcanedba.mixin.CloudTextureMixin263":
                return newer;
            // Post effects hook: doEntityOutline before 26.3, blitEntityOutline from 26.3.
            case "com.dragonblockarcanedba.client.mixin.GameRendererPostMixin":
                return !newer;
            case "com.dragonblockarcanedba.client.mixin.GameRendererPostMixin263":
                return newer;
            default:
                return true;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
