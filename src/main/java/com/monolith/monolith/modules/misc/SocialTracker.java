package com.monolith.monolith.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.HashSet;
import java.util.Set;

public class SocialTracker {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<String> alertedPlayers = new HashSet<>();
    private int tickTimer = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        // Run only at end of tick, if player exists, and alerts are ON
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || !SocialManager.kosAlerts) return;

        // Scan every 60 ticks (3 seconds) to save performance
        if (tickTimer++ < 60) return;
        tickTimer = 0;

        if (mc.getNetHandler() == null) return;

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile() == null) continue;
            String name = info.getGameProfile().getName();

            // If player is KOS and we haven't alerted yet
            if (SocialManager.isKOS(name) && !alertedPlayers.contains(name)) {

                // Send Chat Message
                mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "[WARNING] " +
                    EnumChatFormatting.RESET + EnumChatFormatting.RED + "KOS Player Detected: " +
                    EnumChatFormatting.WHITE + name
                ));

                // Play "Orb" sound
                mc.thePlayer.playSound("random.orb", 1.0f, 0.5f);

                // Mark as alerted so we don't spam chat
                alertedPlayers.add(name);
            }
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Clear cache when leaving server
        alertedPlayers.clear();
    }
}
