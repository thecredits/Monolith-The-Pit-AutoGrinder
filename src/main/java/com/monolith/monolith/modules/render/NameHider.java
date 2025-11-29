package com.monolith.monolith.modules.render;

import net.minecraft.client.Minecraft;

public class NameHider {

    // --- MOVED ENABLED VARIABLE HERE ---
    public static boolean enabled = false;
    public static String fakeName = "MeloUser";

    public static String format(String s) {
        // Check local variable instead of GUI
        if (!enabled) {
            return s;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return s;

        String realName = mc.thePlayer.getName();

        if (s.contains(realName)) {
            // Replace real name with the custom fake name
            s = s.replace(realName, fakeName);
        }

        return s;
    }
}
