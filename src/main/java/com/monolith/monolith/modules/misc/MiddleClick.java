package com.monolith.monolith.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Mouse;

public class MiddleClick {
    public static boolean enabled = true;
    private boolean wasDown = false;
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        if (!enabled || mc.thePlayer == null) return;

        // Button 2 is Middle Click
        boolean isDown = Mouse.isButtonDown(2);

        if (isDown && !wasDown) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
                EntityPlayer target = (EntityPlayer) mc.objectMouseOver.entityHit;
                String name = target.getName();

                if (SocialManager.isFriend(name)) {
                    SocialManager.remove(name);
                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Removed " + name + " from Friends."));
                } else {
                    SocialManager.addFriend(name);
                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "Added " + name + " to Friends."));
                }
            }
        }
        wasDown = isDown;
    }
}
