package com.monolith.monolith.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class ChestESP {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static boolean rainbow = false;
    public static int red = 0;
    public static int green = 0;
    public static int blue = 255;

    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled) return;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(1.0F);

        // Determine Color
        int rgb = rainbow ?
                Color.getHSBColor((System.currentTimeMillis() % 1000) / 1000f, 0.8f, 0.8f).getRGB() :
                new Color(red, green, blue).getRGB();

        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;

        GlStateManager.color(r, g, b, 1.0F);

        // Iterate and Render
        for (TileEntity te : mc.theWorld.loadedTileEntityList) {
            if (te instanceof TileEntityChest || te instanceof TileEntityEnderChest) {
                renderBox(te.getPos());
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderBox(BlockPos pos) {
        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        double x = pos.getX() - viewerX;
        double y = pos.getY() - viewerY;
        double z = pos.getZ() - viewerZ;

        // Draw standard selection box
        AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
        RenderGlobal.drawSelectionBoundingBox(bb);
    }
}
