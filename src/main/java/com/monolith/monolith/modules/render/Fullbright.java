package com.monolith.monolith.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class Fullbright {

    public static boolean enabled = false;
    public static int mode = 0; // 0 = GAMMA, 1 = NIGHT VISION

    private static float oldGamma = -1;
    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Called when the toggle button is clicked.
     */
    public static void toggle() {
        enabled = !enabled;
        if (enabled) {
            enable();
        } else {
            disable();
        }
    }

    /**
     * Called when the mode button is clicked.
     */
    public static void cycleMode() {
        mode = (mode == 0) ? 1 : 0;
        // If we are currently enabled, re-apply the correct effect immediately
        if (enabled) {
            disable(); // Clear old effect (e.g. remove Night Vision)
            enabled = true; // Keep enabled true
            enable();  // Apply new effect (e.g. set Gamma)
        }
    }

    private static void enable() {
        if (mc.thePlayer == null) return;

        if (mode == 0) {
            // --- GAMMA MODE ---
            oldGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 1000.0F; // Massive value guarantees brightness
            mc.thePlayer.removePotionEffect(Potion.nightVision.id); // Remove NV if present
        } else {
            // --- NIGHT VISION MODE ---
            // 1. Reset Gamma to normal if we stored it
            if (oldGamma != -1) {
                mc.gameSettings.gammaSetting = oldGamma;
            }
            // 2. Apply Infinite Night Vision (Duration 999999)
            mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 999999, 0, false, false));
        }
    }

    private static void disable() {
        if (mc.thePlayer == null) return;

        // Restore Gamma
        if (oldGamma != -1) {
            mc.gameSettings.gammaSetting = oldGamma;
        } else {
            mc.gameSettings.gammaSetting = 1.0F; // Default 'Bright'
        }

        // Remove Night Vision
        mc.thePlayer.removePotionEffect(Potion.nightVision.id);
    }
}
