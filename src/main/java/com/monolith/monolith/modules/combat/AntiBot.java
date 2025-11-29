package com.monolith.monolith.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;

public class AntiBot {

    public static boolean enabled = false;

    // --- SETTINGS ---
    public static boolean wait80Ticks = false; // Ignore entities created < 4s ago
    public static boolean removeDead = true;   // Ignore dead entities

    private static final HashMap<EntityPlayer, Long> newEnt = new HashMap<>();
    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!enabled || mc.thePlayer == null) return;

        // Add new players to a map with the current time
        if (wait80Ticks && event.entity instanceof EntityPlayer && event.entity != mc.thePlayer) {
            newEnt.put((EntityPlayer) event.entity, System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || event.phase != TickEvent.Phase.END) return;

        // Remove players from the "new" map after 4 seconds (80 ticks)
        if (wait80Ticks && !newEnt.isEmpty()) {
            long now = System.currentTimeMillis();
            newEnt.values().removeIf(time -> time < now - 4000L);
        }
    }

    /**
     * Checks if an entity is considered a Bot based on current settings.
     */
    public static boolean isBot(Entity en) {
        // UPDATED: Added mc.currentScreen != null
        // If the module is disabled, player is null, OR a GUI is open, return false.
        if (!enabled || mc.thePlayer == null || mc.currentScreen != null) return false;

        // 1. New Entity Check (Watchdog often spawns in right when you hit)
        if (wait80Ticks && !newEnt.isEmpty() && en instanceof EntityPlayer && newEnt.containsKey(en)) {
            return true;
        }

        // 2. Color Code Check (Common bot trait)
        if (en.getName().startsWith("§c")) {
            return true;
        }

        // 3. Dead Check
        if (removeDead && en.isDead) {
            return true;
        }

        // 4. Advanced Name Analysis
        String n = en.getDisplayName().getUnformattedText();

        // NPC Tag check
        if (en.getName().contains("§") && n.contains("[NPC] ")) {
            return true;
        }

        // Empty name check
        if (n.isEmpty() && en.getName().isEmpty()) {
            return true;
        }

        // Watchdog Specific: 10 random chars pattern detection
        if (n.length() == 10) {
            int num = 0;
            int let = 0;
            char[] chars = n.toCharArray();

            for (char c : chars) {
                if (Character.isLetter(c)) {
                    if (Character.isUpperCase(c)) {
                        // Watchdog names rarely have specific uppercase patterns
                        return false;
                    }
                    ++let;
                } else {
                    if (!Character.isDigit(c)) {
                        // Special chars usually mean real player
                        return false;
                    }
                    ++num;
                }
            }
            // If it looks like "a7b9c2d1e5" (mixed letters/nums), likely a bot
            return num >= 2 && let >= 2;
        }

        return false;
    }
}
