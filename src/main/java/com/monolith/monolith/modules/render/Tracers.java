package com.monolith.monolith.modules.render;

import com.monolith.monolith.modules.combat.AntiBot;
import com.monolith.monolith.modules.misc.SocialManager; // Import SocialManager
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import java.awt.Color;

public class Tracers {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static boolean showInvis = true;
    public static boolean rainbow = false;
    public static boolean redShift = false;
    public static double lineWidth = 1.0D;
    public static double distance = 512.0D;

    // 0 = All, 1 = KOS Only, 2 = Friends Only, 3 = KOS & Friends
    public static double filterMode = 0;

    // Colors
    public static int colorR = 0, colorG = 255, colorB = 0; // Default
    public static int friendR = 0, friendG = 255, friendB = 255; // Cyan
    public static int kosR = 255, kosG = 0, kosB = 0; // Red

    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean oldBobbing = false;
    private boolean wasEnabled = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (enabled) {
            if (!wasEnabled) {
                oldBobbing = mc.gameSettings.viewBobbing;
                mc.gameSettings.viewBobbing = false;
                wasEnabled = true;
            }
            if (mc.gameSettings.viewBobbing) mc.gameSettings.viewBobbing = false;
        } else {
            if (wasEnabled) {
                mc.gameSettings.viewBobbing = oldBobbing;
                wasEnabled = false;
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled) return;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth((float) lineWidth);

        for (EntityPlayer en : mc.theWorld.playerEntities) {
            if (en == mc.thePlayer) continue;
            if (en.deathTime != 0) continue;
            if (!showInvis && en.isInvisible()) continue;
            if (mc.thePlayer.getDistanceToEntity(en) > distance) continue;
            if (AntiBot.enabled && AntiBot.isBot(en)) continue; // Assumes AntiBot check

            String name = en.getName();
            boolean isFriend = SocialManager.isFriend(name);
            boolean isKOS = SocialManager.isKOS(name);

            // --- FILTER LOGIC ---
            int mode = (int) filterMode;
            if (mode == 1 && !isKOS) continue; // KOS Only
            if (mode == 2 && !isFriend) continue; // Friends Only
            if (mode == 3 && (!isKOS && !isFriend)) continue; // KOS & Friends Only

            // --- COLOR LOGIC ---
            int r = colorR, g = colorG, b = colorB;

            if (isKOS) {
                r = kosR; g = kosG; b = kosB;
            } else if (isFriend) {
                r = friendR; g = friendG; b = friendB;
            } else {
                // Normal Logic (Rainbow/Redshift) applied only if not social
                if (rainbow) {
                    Color c = Color.getHSBColor((System.currentTimeMillis() % 1000) / 1000f, 0.8f, 0.8f);
                    r = c.getRed(); g = c.getGreen(); b = c.getBlue();
                } else if (redShift) {
                    float dist = mc.thePlayer.getDistanceToEntity(en);
                    if (dist < 25) {
                        r = 255;
                        g = (int) (dist * 10); if (g > 255) g = 255;
                        b = 0;
                    }
                }
            }

            GlStateManager.color(r / 255f, g / 255f, b / 255f, 1.0f);

            // Render Line
            double x = en.lastTickPosX + (en.posX - en.lastTickPosX) * event.partialTicks - mc.getRenderManager().viewerPosX;
            double y = en.lastTickPosY + (en.posY - en.lastTickPosY) * event.partialTicks - mc.getRenderManager().viewerPosY;
            double z = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * event.partialTicks - mc.getRenderManager().viewerPosZ;

            Vec3 eyeVec = new Vec3(0, 0, 1)
                    .rotatePitch(-(float) Math.toRadians(mc.thePlayer.rotationPitch))
                    .rotateYaw(-(float) Math.toRadians(mc.thePlayer.rotationYaw));

            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3d(eyeVec.xCoord, mc.thePlayer.getEyeHeight() + eyeVec.yCoord, eyeVec.zCoord);
            GL11.glVertex3d(x, y, z);
            GL11.glEnd();
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
