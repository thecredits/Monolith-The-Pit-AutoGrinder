package com.monolith.monolith.modules.render;

import com.monolith.monolith.modules.misc.SocialManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class Nametags {

    public static boolean enabled = false;
    public static boolean showHealth = true;
    public static boolean showInvis = true;
    public static boolean removeTags = false;
    public static double offset = 0; // Y offset from the GUI slider

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Specials.Pre event) {
        if (!enabled) return;

        Entity entity = event.entity;

        // Only target players and ignore yourself
        if (!(entity instanceof EntityPlayer) || entity == Minecraft.getMinecraft().thePlayer) return;

        EntityPlayer player = (EntityPlayer) entity;

        // Cancel vanilla rendering so we can draw our own
        event.setCanceled(true);

        if (removeTags) return;
        if (player.isInvisible() && !showInvis) return;

        // Determine Name String
        String color = SocialManager.getRelationColor(player.getName());
        String displayTag = color + player.getName();

        // Add Health (if enabled)
        if (showHealth) {
            float health = player.getHealth() + player.getAbsorptionAmount();
            EnumChatFormatting hpColor = health > 15 ? EnumChatFormatting.GREEN : (health > 10 ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED);
            displayTag = displayTag + " " + hpColor + (int)Math.ceil(health);
        }

        renderCustomTag(player, displayTag, event.x, event.y, event.z);
    }

    private void renderCustomTag(EntityPlayer player, String text, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        float viewerYaw = mc.getRenderManager().playerViewY;
        float viewerPitch = mc.getRenderManager().playerViewX;
        float scale = 0.0267f; // Standard Nametag Scale

        // Calculate Distance for Scaling (optional, keeps tags readable at distance)
        double dist = mc.thePlayer.getDistanceToEntity(player);
        if (dist > 10) {
            scale += (dist - 10) * 0.002f; // Slight scale up further away
            if (scale > 0.1f) scale = 0.1f; // Cap scale
        }

        GlStateManager.pushMatrix();
        // Move to player position + height
        GlStateManager.translate(x, y + player.height + 0.5 + (offset/10.0), z);

        // Rotate to face the camera
        GlStateManager.rotate(-viewerYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(viewerPitch, 1.0F, 0.0F, 0.0F);

        // Apply Scale
        GlStateManager.scale(-scale, -scale, scale);

        // GL Settings for Text
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth(); // See through walls
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int width = mc.fontRendererObj.getStringWidth(text) / 2;

        // Draw Background Box
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-width - 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(-width - 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(width + 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(width + 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();

        // Draw Text
        mc.fontRendererObj.drawString(text, -width, 0, -1);

        // Restore GL Settings
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
