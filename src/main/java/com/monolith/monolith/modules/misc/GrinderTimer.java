package com.monolith.monolith.modules.misc;

import com.monolith.monolith.gui.MonolithModGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class GrinderTimer {

    // Settings accessed by GUI
    public static boolean enabled = false;
    public static int hours = 0;
    public static int minutes = 0;
    public static int seconds = 0;

    // Internal logic variables
    private long endTime = -1;
    private boolean wasGrinderEnabled = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || Minecraft.getMinecraft().thePlayer == null) return;

        // Check the state of the AutoGrinder from the GUI class
        boolean isGrinderOn = MonolithModGUI.AutoGrinder.enabled;

        // 1. Detect when Grinder is freshly turned ON
        if (isGrinderOn && !wasGrinderEnabled) {
            if (enabled) {
                long durationMillis = (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L);

                if (durationMillis > 0) {
                    endTime = System.currentTimeMillis() + durationMillis;
                    printMsg(EnumChatFormatting.GREEN + "Auto-Stop Timer started! Stopping in " +
                             hours + "h " + minutes + "m " + seconds + "s.");
                } else {
                    endTime = -1; // Timer is enabled but set to 0, so we ignore it
                }
            } else {
                endTime = -1;
            }
        }

        // 2. Logic while Grinder is running
        if (isGrinderOn && enabled && endTime != -1) {
            if (System.currentTimeMillis() > endTime) {
                // Time is up!
                MonolithModGUI.AutoGrinder.enabled = false; // Turn off the grinder
                endTime = -1;
                printMsg(EnumChatFormatting.RED + "AutoGrinder stopped (Timer expired).");
            }
        }

        // 3. Reset if manually turned off
        if (!isGrinderOn) {
            endTime = -1;
        }

        wasGrinderEnabled = isGrinderOn;
    }

    private void printMsg(String msg) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "[Monolith] " + EnumChatFormatting.RESET + msg));
        }
    }
}
