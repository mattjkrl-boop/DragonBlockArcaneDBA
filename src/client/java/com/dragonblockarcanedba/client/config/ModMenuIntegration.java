package com.dragonblockarcanedba.client.config;

import com.dragonblockarcanedba.client.gui.DbaSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration implements ModMenuApi {
    @SuppressWarnings("rawtypes")
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreenFactory) java.lang.reflect.Proxy.newProxyInstance(
                ConfigScreenFactory.class.getClassLoader(),
                new Class[]{ConfigScreenFactory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("create")) {
                        return new DbaSettingsScreen((Screen) args[0]);
                    }
                    return null;
                }
        );
    }
}
