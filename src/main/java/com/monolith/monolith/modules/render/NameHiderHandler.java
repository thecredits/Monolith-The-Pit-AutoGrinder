package com.monolith.monolith.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NameHiderHandler {

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent event) {
        // Use NameHider.enabled instead of MonolithModGUI.nameHider
        if (NameHider.enabled && Minecraft.getMinecraft().thePlayer != null) {
            String originalMsg = event.message.getFormattedText();
            String myName = Minecraft.getMinecraft().thePlayer.getName();

            // Only modify if it contains your name
            if (originalMsg.contains(myName)) {
                String newMsg = NameHider.format(originalMsg);
                event.message = new ChatComponentText(newMsg);
            }
        }
    }
}
