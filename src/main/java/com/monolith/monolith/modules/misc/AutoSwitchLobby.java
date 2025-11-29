package com.monolith.monolith.modules.misc;

import com.monolith.monolith.gui.MonolithModGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.ThreadLocalRandom;

public class AutoSwitchLobby {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // Logic State
    private int state = 0;
    // 0 = Monitoring / Idle
    // 1 = Sending /lobby
    // 2 = Waiting for lobby
    // 3 = Sending /play pit

    private long nextActionTime = 0;
    private long worldJoinTime = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        // 1. Check if Enabled in GUI
        if (!MonolithModGUI.AutoGrinder.autoSwapEnabled) {
            state = 0;
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) return;

        // 2. Initial Safety: Don't check immediately after joining (Wait 10s for entities to load)
        if (System.currentTimeMillis() - worldJoinTime < 10000) {
            return;
        }

        // 3. Time Delay Check
        if (System.currentTimeMillis() < nextActionTime) return;

        // --- STATE MACHINE ---
        switch (state) {
            case 0: // MONITORING MODE
                int midCount = countPlayersInMid();
                int threshold = (int) MonolithModGUI.AutoGrinder.autoSwapThreshold;

                // Only switch if the count is too low
                if (midCount <= threshold) {

                    // --- SAFETY COOLDOWN ---
                    // If we joined less than 60 seconds ago, DO NOT swap yet.
                    // This prevents getting flagged for "Command Spam" or "Botting".
                    if (System.currentTimeMillis() - worldJoinTime < 60000) {
                        // Optional: Debug log to know it's waiting
                        // log("Mid low (" + midCount + "), waiting for safety cooldown...");
                    } else {
                        log("Players in Mid: " + midCount + " (Threshold: " + threshold + "). Switching Lobbies...");
                        state = 1; // Start Switch Sequence
                    }
                }

                // Check every 2 seconds to save performance
                nextActionTime = System.currentTimeMillis() + 2000;
                break;

            case 1: // SEND /LOBBY
                mc.thePlayer.sendChatMessage("/lobby");
                // Wait 4-5 seconds for server to process
                nextActionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(4000, 5000);
                state = 2;
                break;

            case 2: // PREPARE REJOIN
                // Just a small buffer state
                state = 3;
                break;

            case 3: // SEND /PLAY PIT
                mc.thePlayer.sendChatMessage("/play pit");
                // Reset timers
                worldJoinTime = System.currentTimeMillis();
                // Wait at least 10 seconds before monitoring again
                nextActionTime = System.currentTimeMillis() + 10000;
                state = 0; // Reset to Monitor
                break;
        }
    }

    private int countPlayersInMid() {
        int count = 0;
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer) continue; // Don't count self

            // "Mid" Logic:
            // 1. Must be below Y = 82 (Spawn is usually ~85-100+)
            // 2. This filters out people AFK in spawn or launching
            if (p.posY < 82.0) {
                count++;
            }
        }
        return count;
    }

    private void log(String msg) {
        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[AutoSwap] " + msg));
    }
}
