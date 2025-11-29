package com.monolith.monolith.modules.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCake;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class MiscBlockESP {

    public static boolean cakeEnabled = false;
    public static boolean bannerEnabled = false;

    // We cache cake positions to avoid scanning thousands of blocks every frame
    private final List<BlockPos> cakePositions = new ArrayList<>();
    private int tickCounter = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getMinecraft().thePlayer == null) return;

        // Scan for cakes once every second (20 ticks) to save FPS
        if (cakeEnabled && tickCounter++ % 20 == 0) {
            cakePositions.clear();
            int radius = 30; // Scan radius around player
            BlockPos p = Minecraft.getMinecraft().thePlayer.getPosition();

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = p.add(x, y, z);
                        Block block = Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock();
                        if (block instanceof BlockCake) {
                            cakePositions.add(pos);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (Minecraft.getMinecraft().theWorld == null) return;

        // Render Cakes (Color: Magenta)
        if (cakeEnabled) {
            for (BlockPos pos : cakePositions) {
                renderBox(pos, 1.0f, 0.0f, 1.0f);
            }
        }

        // Render Banners (Color: Orange)
        // Banners are TileEntities, so we can access them instantly without scanning blocks
        if (bannerEnabled) {
            for (TileEntity te : Minecraft.getMinecraft().theWorld.loadedTileEntityList) {
                if (te instanceof TileEntityBanner) {
                    renderBox(te.getPos(), 1.0f, 0.5f, 0.0f);
                }
            }
        }
    }

    private void renderBox(BlockPos pos, float r, float g, float b) {
        Minecraft mc = Minecraft.getMinecraft();
        double x = pos.getX() - mc.getRenderManager().viewerPosX;
        double y = pos.getY() - mc.getRenderManager().viewerPosY;
        double z = pos.getZ() - mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(1.5f);

        AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);

        // Outline
        GlStateManager.color(r, g, b, 1.0f);
        RenderGlobal.drawSelectionBoundingBox(box);

        // Fill (semi-transparent)
        GlStateManager.color(r, g, b, 0.2f);
        drawSolidBox(box);

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawSolidBox(AxisAlignedBB bb) {
        GL11.glBegin(GL11.GL_QUADS);
        // Bottom
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        // Top
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        // Front
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        // Back
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        // Left
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        // Right
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glEnd();
    }
}
