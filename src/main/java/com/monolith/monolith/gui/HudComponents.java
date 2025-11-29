package com.monolith.monolith.gui;

import com.monolith.monolith.modules.misc.SocialManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.lwjgl.input.Mouse;
import java.util.ArrayList;
import java.util.List;

public class HudComponents {

    // --- SHARED COLORS ---
    private static final int COLOR_BG_TINT = 0xB5000000;
    private static final int COLOR_BORDER = 0xAA9050FF;
    private static final int COLOR_HEADER = 0x90202020;

    // --- FPS MODULE ---
    public static class FpsComponent extends HudManager.HudComponent {
        public FpsComponent() { super("FPS", 5, 5); }
        @Override public boolean isEnabled() { return MonolithModGUI.hudFPS; }
        @Override public void render(int mX, int mY) {
            String text = "FPS " + Minecraft.getDebugFPS();
            this.w = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) + 10;
            this.h = 18;
            MonolithModGUI.RenderUtil.drawPerfectRoundedRect(x, y, w, h, 5, COLOR_BG_TINT);
            MonolithModGUI.RenderUtil.drawRoundedOutline(x, y, w, h, 5, 1.0f, COLOR_BORDER);
            MonolithModGUI.RenderUtil.drawString(text, x + 5, y + 5, -1);
        }
    }

    // --- CPS MODULE ---
    public static class CpsComponent extends HudManager.HudComponent {
        public CpsComponent() { super("CPS", 5, 30); }
        @Override public boolean isEnabled() { return MonolithModGUI.hudCPS; }
        @Override public void render(int mX, int mY) {
            int lCps = Mouse.isButtonDown(0) ? (int)(Math.random() * 4 + 8) : 0;
            String text = "CPS " + lCps;
            this.w = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) + 10;
            this.h = 18;
            MonolithModGUI.RenderUtil.drawPerfectRoundedRect(x, y, w, h, 5, COLOR_BG_TINT);
            MonolithModGUI.RenderUtil.drawRoundedOutline(x, y, w, h, 5, 1.0f, COLOR_BORDER);
            MonolithModGUI.RenderUtil.drawString(text, x + 5, y + 5, -1);
        }
    }

    // --- KOS OVERLAY MODULE ---
    public static class KosComponent extends HudManager.HudComponent {
        public KosComponent() { super("KOS Overlay", 10, 100); }
        @Override public boolean isEnabled() { return KosOverlay.enabled; }

        @Override
        public void render(int mX, int mY) {
            Minecraft mc = Minecraft.getMinecraft();
            List<String> detectedKOS = new ArrayList<>();

            // Get Real Data
            if (mc.getNetHandler() != null) {
                for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                    if (info.getGameProfile() != null) {
                        String name = info.getGameProfile().getName();
                        if (SocialManager.isKOS(name)) detectedKOS.add(name);
                    }
                }
            }

            // If Editing, show dummy data so user can see/drag the box
            if (MonolithModGUI.editHudMode && detectedKOS.isEmpty()) {
                detectedKOS.add("ExamplePlayer1");
                detectedKOS.add("ExamplePlayer2");
            }

            if (detectedKOS.isEmpty()) {
                this.w = 0; this.h = 0; return;
            }

            // Calculate Dimensions
            String header = "KOS DETECTED (" + detectedKOS.size() + ")";
            int maxWidth = mc.fontRendererObj.getStringWidth(header);
            for (String s : detectedKOS) {
                int sw = mc.fontRendererObj.getStringWidth(s);
                if (sw > maxWidth) maxWidth = sw;
            }

            this.w = maxWidth + 12;
            this.h = 20 + (detectedKOS.size() * 12);

            // Draw Box
            MonolithModGUI.RenderUtil.drawPerfectRoundedRect(x, y, w, h, 5, COLOR_BG_TINT);
            MonolithModGUI.RenderUtil.drawPerfectRoundedRect(x, y, w, 16, 5, COLOR_HEADER); // Header
            MonolithModGUI.RenderUtil.drawRoundedOutline(x, y, w, h, 5, 1.0f, COLOR_BORDER);

            // Draw Header
            int textX = x + (w - mc.fontRendererObj.getStringWidth(header)) / 2;
            mc.fontRendererObj.drawStringWithShadow(header, textX, y + 4, 0xFFFFFFFF);

            // Draw Names
            int cy = y + 18;
            for (String s : detectedKOS) {
                int nx = x + (w - mc.fontRendererObj.getStringWidth(s)) / 2;
                mc.fontRendererObj.drawStringWithShadow(0xFFFF5555 + s, nx, cy, -1);
                cy += 12;
            }
        }
    }
}
