package com.monolith.monolith.gui;

import com.monolith.monolith.modules.misc.PitEventDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PitEventOverlay extends Gui {

    public static boolean enabled = true;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        // specific check to ensure we only render text once per frame
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !enabled) return;

        // Only show if an event is actually happening
        if (!PitEventDetector.eventActive) return;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);

        // Format: "Event: The Spire" (Gold + White)
        String text = EnumChatFormatting.GOLD + "Event: " + EnumChatFormatting.WHITE + PitEventDetector.currentEventName;

        int screenWidth = sr.getScaledWidth();
        int stringWidth = mc.fontRendererObj.getStringWidth(text);

        // Position: Top Middle (y=12 provides a nice gap from the top of the screen)
        float x = (screenWidth / 2.0f) - (stringWidth / 2.0f);
        float y = 12;

        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
    }
}
