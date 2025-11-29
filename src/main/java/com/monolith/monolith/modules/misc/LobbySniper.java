package com.monolith.monolith.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class LobbySniper {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static String targetName = "";

    // Internal Variables
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int attempts = 0;
    private long nextStepTime = 0;
    private int state = 0;
    // State 0: Init/Send Lobby
    // State 1: Waiting for Lobby Load
    // State 2: Send Play Pit
    // State 3: Waiting for Pit Load & Check

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!enabled) {
            // Reset state when disabled
            state = 0;
            attempts = 0;
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (targetName.isEmpty()) {
            print("Please set a Target Name in the GUI.");
            enabled = false;
            return;
        }

        if (System.currentTimeMillis() < nextStepTime) return;

        switch (state) {
            case 0: // SEND /lobby
                print(EnumChatFormatting.GRAY + "Attempt " + (attempts + 1) + "/5: Sending to lobby...");
                mc.thePlayer.sendChatMessage("/lobby");
                // Wait 4-6 seconds for lobby to load
                nextStepTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(4000, 6000);
                state = 1;
                break;

            case 1: // READY TO SEND /play pit
                state = 2; // Move immediately to next logic block
                break;

            case 2: // SEND /play pit
                mc.thePlayer.sendChatMessage("/play pit");
                // Wait 5-7 seconds for Pit to load and Tab List to populate
                nextStepTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(5000, 7000);
                state = 3;
                break;

            case 3: // CHECK FOR PLAYER
                if (isPlayerFound(targetName)) {
                    print(EnumChatFormatting.GREEN + "FOUND " + targetName + "! Stopping sniper.");
                    mc.thePlayer.playSound("random.levelup", 1.0f, 1.0f); // Play sound
                    enabled = false;
                } else {
                    attempts++;
                    if (attempts >= 5) {
                        print(EnumChatFormatting.RED + "Target not found after 5 tries.");
                        print(EnumChatFormatting.RED + "Maybe player is offline?");
                        enabled = false;
                    } else {
                        print(EnumChatFormatting.YELLOW + "Target not found. Retrying...");
                        // Go back to start
                        state = 0;
                    }
                }
                break;
        }
    }

    private boolean isPlayerFound(String name) {
        if (mc.getNetHandler() == null) return false;
        Collection<NetworkPlayerInfo> playerList = mc.getNetHandler().getPlayerInfoMap();

        for (NetworkPlayerInfo info : playerList) {
            if (info.getGameProfile() != null) {
                String pName = info.getGameProfile().getName();
                if (pName != null && pName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void print(String msg) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Sniper] " + msg));
        }
    }
}
