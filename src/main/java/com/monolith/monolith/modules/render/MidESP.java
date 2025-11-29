package com.monolith.monolith.modules.render;

import com.monolith.monolith.gui.MonolithModGUI;
import com.monolith.monolith.modules.misc.PitMapDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class MidESP {

    public static boolean enabled = false;
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        // Only render if MidESP is on AND AutoGrinder Master is on
        if (!enabled || !MonolithModGUI.AutoGrinder.enabled) return;

        if (mc.thePlayer == null || PitMapDetector.centerPos == null) return;

        BlockPos pos = PitMapDetector.centerPos;

        int color = 0xFFFFFF; // Default White
        String map = PitMapDetector.currentMap;

        if (map.equals("Corals")) color = 0x00FFFF;       // Cyan
        else if (map.equals("Seasons")) color = 0x00FF00; // Green
        else if (map.equals("Genesis")) color = 0xA020F0; // Purple
        else if (map.equals("Castle")) color = 0x808080;  // Gray

        double x = pos.getX() + 0.5 - mc.getRenderManager().viewerPosX;
        double y = pos.getY() - mc.getRenderManager().viewerPosY;
        double z = pos.getZ() + 0.5 - mc.getRenderManager().viewerPosZ;

        drawBeam(x, y, z, color);
        drawFloatingText(x, y + 3.5, z, "MID", color);
    }

    private void drawBeam(double x, double y, double z, int colorHex) {
        float r = ((colorHex >> 16) & 0xFF) / 255f;
        float g = ((colorHex >> 8) & 0xFF) / 255f;
        float b = (colorHex & 0xFF) / 255f;
        float alpha = 0.4f;
        float radius = 0.4f;
        double height = 255.0;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Inner Beam
        GL11.glColor4f(r, g, b, alpha);
        GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3d(radius, 0, radius); GL11.glVertex3d(radius, height, radius);
            GL11.glVertex3d(-radius, height, radius); GL11.glVertex3d(-radius, 0, radius);
            GL11.glVertex3d(radius, 0, -radius); GL11.glVertex3d(radius, height, -radius);
            GL11.glVertex3d(radius, height, radius); GL11.glVertex3d(radius, 0, radius);
            GL11.glVertex3d(-radius, 0, -radius); GL11.glVertex3d(-radius, height, -radius);
            GL11.glVertex3d(radius, height, -radius); GL11.glVertex3d(radius, 0, -radius);
            GL11.glVertex3d(-radius, 0, radius); GL11.glVertex3d(-radius, height, radius);
            GL11.glVertex3d(-radius, height, -radius); GL11.glVertex3d(-radius, 0, -radius);
        GL11.glEnd();

        // Outline
        GL11.glLineWidth(2.0f);
        GL11.glColor4f(r, g, b, 1.0f);
        GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3d(radius, 0, radius); GL11.glVertex3d(radius, height, radius);
            GL11.glVertex3d(-radius, 0, radius); GL11.glVertex3d(-radius, height, radius);
            GL11.glVertex3d(radius, 0, -radius); GL11.glVertex3d(radius, height, -radius);
            GL11.glVertex3d(-radius, 0, -radius); GL11.glVertex3d(-radius, height, -radius);
        GL11.glEnd();

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
    }

    private void drawFloatingText(double x, double y, double z, String text, int color) {
        float scale = 0.03f;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-scale, -scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int width = mc.fontRendererObj.getStringWidth(text) / 2;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-width - 2, -2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(-width - 2, 9, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(width + 2, 9, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(width + 2, -2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        mc.fontRendererObj.drawString(text, -width, 0, color);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
