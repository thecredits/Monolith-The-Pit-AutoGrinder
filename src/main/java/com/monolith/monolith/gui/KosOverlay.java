package com.monolith.monolith.gui;

import com.monolith.monolith.modules.misc.SocialManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class KosOverlay {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static int x = 10;
    public static int y = 100;

    // --- COLORS (Matched to your MonolithModGUI) ---
    private static final int COLOR_BG = 0xB5000000;         // Dark Transparent Background
    private static final int COLOR_HEADER = 0x90202020;     // Header Background
    private static final int COLOR_BORDER = 0xAA9050FF;     // Purple Border
    private static final int COLOR_TEXT_HEADER = 0xFFFFFFFF;
    private static final int COLOR_TEXT_KOS = 0xFFFF5555;   // Red

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!enabled || mc.thePlayer == null || mc.getNetHandler() == null) return;

        // 1. Get List of KOS Players
        List<String> detectedKOS = new ArrayList<>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile() != null) {
                String name = info.getGameProfile().getName();
                if (SocialManager.isKOS(name)) {
                    detectedKOS.add(name);
                }
            }
        }

        if (detectedKOS.isEmpty()) return;

        // 2. Calculate Dimensions
        String headerText = "KOS DETECTED (" + detectedKOS.size() + ")";
        int padding = 5;
        int headerHeight = 16;
        int itemHeight = 12;

        // Find widest string (either the header or a long name)
        int maxWidth = mc.fontRendererObj.getStringWidth(headerText);
        for (String s : detectedKOS) {
            int w = mc.fontRendererObj.getStringWidth(s);
            if (w > maxWidth) maxWidth = w;
        }

        int boxWidth = maxWidth + (padding * 2);
        int boxHeight = headerHeight + (detectedKOS.size() * itemHeight) + padding;

        // 3. Render Background & Border
        // Main Box
        drawPerfectRoundedRect(x, y, boxWidth, boxHeight, 5, COLOR_BG);

        // Header Strip
        drawPerfectRoundedRect(x, y, boxWidth, headerHeight, 5, COLOR_HEADER);

        // Outline
        drawRoundedOutline(x, y, boxWidth, boxHeight, 5, 1.0f, COLOR_BORDER);

        // 4. Render Text
        // Header
        int textX = x + (boxWidth - mc.fontRendererObj.getStringWidth(headerText)) / 2; // Center the header
        mc.fontRendererObj.drawStringWithShadow(headerText, textX, y + 4, COLOR_TEXT_HEADER);

        // Names
        int currentY = y + headerHeight + 2;
        for (String name : detectedKOS) {
            // Draw name centered
            int nameX = x + (boxWidth - mc.fontRendererObj.getStringWidth(name)) / 2;
            mc.fontRendererObj.drawStringWithShadow(name, nameX, currentY, COLOR_TEXT_KOS);
            currentY += itemHeight;
        }
    }

    // ==========================================================
    // RENDER UTILS (Ported from your GUI so they work here)
    // ==========================================================

    public static void drawPerfectRoundedRect(float x, float y, float w, float h, float r, int c) {
        float x2 = x + w; float y2 = y + h;
        drawRect(x + r, y, w - 2*r, h, c);
        drawRect(x, y + r, r, h - 2*r, c);
        drawRect(x2 - r, y + r, r, h - 2*r, c);
        drawQuarterCircle(x + r, y + r, r, 0, c);
        drawQuarterCircle(x2 - r, y + r, r, 1, c);
        drawQuarterCircle(x2 - r, y2 - r, r, 2, c);
        drawQuarterCircle(x + r, y2 - r, r, 3, c);
    }

    public static void drawRoundedOutline(float x, float y, float w, float h, float r, float thick, int c) {
        setupColor(c);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(thick);
        GL11.glBegin(GL11.GL_LINE_LOOP);

        // Top Right
        for(int i=270; i<=360; i+=6) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d((x + w - r) + Math.cos(rad) * r, (y + r) + Math.sin(rad) * r);
        }
        // Bottom Right
        for(int i=0; i<=90; i+=6) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d((x + w - r) + Math.cos(rad) * r, (y + h - r) + Math.sin(rad) * r);
        }
        // Bottom Left
        for(int i=90; i<=180; i+=6) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d((x + r) + Math.cos(rad) * r, (y + h - r) + Math.sin(rad) * r);
        }
        // Top Left
        for(int i=180; i<=270; i+=6) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d((x + r) + Math.cos(rad) * r, (y + r) + Math.sin(rad) * r);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        resetColor();
    }

    public static void drawRect(float x, float y, float w, float h, int c) {
        setupColor(c);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
        resetColor();
    }

    public static void drawQuarterCircle(float cx, float cy, float r, int mode, int c) {
        setupColor(c);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        int s=0, e=0;
        if(mode==0){s=180;e=270;} else if(mode==1){s=270;e=360;} else if(mode==2){s=0;e=90;} else if(mode==3){s=90;e=180;}
        for (int i=s; i<=e; i+=6) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(cx + Math.cos(rad) * r, cy + Math.sin(rad) * r);
        }
        GL11.glEnd();
        resetColor();
    }

    public static void setupColor(int c) {
        float f3=(c>>24&255)/255.0F; float f=(c>>16&255)/255.0F; float f1=(c>>8&255)/255.0F; float f2=(c&255)/255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f,f1,f2,f3);
    }

    public static void resetColor() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
