package com.example.examplemod;

import java.io.IOException;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import java.awt.Color;

public class MeloModGUI extends GuiScreen {

    private final int pW = 340;
    private final int pH = 260;
    private int pX, pY;

    // 0 = Clicker, 1 = Aim, 2 = AutoGrind
    private int currentTab = 0;

    // Slider Dragging States
    private boolean dMinCPS, dMaxCPS, dJitter;
    private boolean dSpeedYaw, dCompYaw, dSpeedPitch, dCompPitch, dFov, dDist, dOffset;
    private boolean dRadius, dSpawnDelay, dChaseRange, dStopRange, dAutoDisable;

    @Override
    public void initGui() {
        pX = (this.width - pW) / 2;
        pY = (this.height - pH) / 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. Background
        drawRoundedRect(pX, pY, pX + pW, pY + pH, 15, new Color(15, 15, 15, 220).getRGB());
        drawRoundedOutline(pX, pY, pX + pW, pY + pH, 15, 1.5f, new Color(255, 255, 255, 30).getRGB());

        // 2. Title
        drawCenteredString(this.fontRendererObj, "M E L O  M O D", pX + pW / 2, pY + 15, 0xFFFFFFFF);

        // 3. Tabs
        int tabWidth = 80;
        int tabGap = 10;
        int totalTabsW = (tabWidth * 3) + (tabGap * 2);
        int startTabs = pX + (pW - totalTabsW) / 2;

        drawTab(startTabs, pY + 40, "Clicker", 0);
        drawTab(startTabs + tabWidth + tabGap, pY + 40, "Aim Assist", 1);
        drawTab(startTabs + (tabWidth + tabGap) * 2, pY + 40, "AutoGrind", 2);

        // 4. Content
        int contentY = pY + 70;
        if (currentTab == 0) drawClickerPage(mouseX, mouseY, contentY);
        else if (currentTab == 1) drawAimPage(mouseX, mouseY, contentY);
        else drawGrindPage(mouseX, mouseY, contentY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawTab(int x, int y, String name, int id) {
        int color = currentTab == id ? 0xFFFFFFFF : 0xFF888888;
        drawCenteredString(this.fontRendererObj, name, x + 40, y, color);
        if (currentTab == id) drawRoundedRect(x, y + 12, x + 80, y + 14, 1, 0xFFFFFFFF);
    }

    // --- PAGE 3: AUTO GRIND ---
    private void drawGrindPage(int mX, int mY, int startY) {
        int leftX = pX + 40;
        int y = startY;

        // Toggle
        drawModernToggle(leftX, y, "Enabled", AutoGrind.enabled);
        if (checkToggle(mX, mY, leftX, y) && org.lwjgl.input.Mouse.isButtonDown(0)) clickedToggle("enabled");
        y += 40;

        // Button
        drawButton(leftX, y, "Set Location");
        this.fontRendererObj.drawString("X:" + (int)AutoGrind.targetX + " Z:" + (int)AutoGrind.targetZ, leftX + 160, y + 6, 0xAAAAAA);
        y += 25; // Gap for Sliders

        // Sliders
        int rx = pX + 40;

        // Calculate Values
        if (dRadius) AutoGrind.radius = calc(mX, rx, 3, 6);
        if (dSpawnDelay) AutoGrind.spawnDelay = calc(mX, rx, 0, 10);
        if (dChaseRange) AutoGrind.chaseRange = calc(mX, rx, 5, 30);
        if (dStopRange) AutoGrind.stopRange = calc(mX, rx, 1, 6);
        if (dAutoDisable) AutoGrind.autoDisableMinutes = calc(mX, rx, 0, 60);

        // Draw Sliders
        drawModernSlider(rx, y, "Circle Radius", AutoGrind.radius, 3, 6); y += 30;
        drawModernSlider(rx, y, "Spawn Delay (s)", AutoGrind.spawnDelay, 0, 10); y += 30;
        drawModernSlider(rx, y, "Chase Range", AutoGrind.chaseRange, 5, 30); y += 30;
        drawModernSlider(rx, y, "Stop Range", AutoGrind.stopRange, 1, 6); y += 30;

        // Auto Disable Custom Slider
        String timeStr = (AutoGrind.autoDisableMinutes < 1.0) ? "Disabled" : String.format("%.0f min", AutoGrind.autoDisableMinutes);
        drawCustomTextSlider(rx, y, "Auto Disable", timeStr, AutoGrind.autoDisableMinutes, 0, 60);
    }

    private void drawCustomTextSlider(int x, int y, String label, String valueStr, double val, double min, double max) {
        this.fontRendererObj.drawString(label, x, y, 0xAAAAAA);
        this.fontRendererObj.drawString(valueStr, x + 100, y, 0xFFFFFFFF);
        int slideY = y + 15;
        drawRoundedRect(x, slideY, x + 120, slideY + 2, 1, 0xFF404040);
        double pct = (val - min) / (max - min);
        drawRoundedRect(x, slideY, x + (int)(120 * pct), slideY + 2, 1, 0xFFFFFFFF);
        drawCircle(x + (int)(120 * pct), slideY + 1, 4, 0xFFFFFFFF);
    }

    // --- OTHER PAGES ---
    private void drawClickerPage(int mX, int mY, int startY) {
        int leftX = pX + 40; int rightX = pX + 190; int y = startY;

        drawModernToggle(leftX, y, "Enabled", LeftClicker.enabled); y += 30;
        drawModernToggle(leftX, y, "Always On", LeftClicker.triggerBot); y += 30;
        drawModernToggle(leftX, y, "Weapon Only", LeftClicker.weaponOnly); y += 30;
        drawModernToggle(leftX, y, "Break Blocks", LeftClicker.breakBlocks);

        y = startY;
        if (dMinCPS) LeftClicker.minCPS = calc(mX, rightX, 1, 60);
        if (dMaxCPS) LeftClicker.maxCPS = calc(mX, rightX, 1, 60);
        if (dJitter) LeftClicker.jitterLeft = calc(mX, rightX, 0, 3);

        drawModernSlider(rightX, y, "Min CPS", LeftClicker.minCPS, 1, 60); y += 40;
        drawModernSlider(rightX, y, "Max CPS", LeftClicker.maxCPS, 1, 60); y += 40;
        drawModernSlider(rightX, y, "Jitter", LeftClicker.jitterLeft, 0, 3);
    }

    private void drawAimPage(int mX, int mY, int startY) {
        int leftX = pX + 30; int rightX = pX + 190; int y = startY;

        drawModernToggle(leftX, y, "Click Aim", AimAssist.clickAim); y += 25;
        drawModernToggle(leftX, y, "Aim Pitch", AimAssist.aimPitch); y += 25;
        drawModernToggle(leftX, y, "Weapon Only", AimAssist.weaponOnly); y += 25;
        drawModernToggle(leftX, y, "Break Blocks", AimAssist.breakBlocks);

        int sy = startY;
        if (dSpeedYaw) AimAssist.speedYaw = calc(mX, rightX, 5, 100);
        if (dCompYaw) AimAssist.complimentYaw = calc(mX, rightX, 2, 97);
        if (dSpeedPitch) AimAssist.speedPitch = calc(mX, rightX, 5, 100);
        if (dCompPitch) AimAssist.complimentPitch = calc(mX, rightX, 2, 97);

        drawModernSlider(rightX, sy, "Speed H", AimAssist.speedYaw, 5, 100); sy += 35;
        drawModernSlider(rightX, sy, "Speed H2", AimAssist.complimentYaw, 2, 97); sy += 35;
        drawModernSlider(rightX, sy, "Speed V", AimAssist.speedPitch, 5, 100); sy += 35;
        drawModernSlider(rightX, sy, "Speed V2", AimAssist.complimentPitch, 2, 97);

        int bx = pX + 30; int by = pY + 195;
        if (dFov) AimAssist.fov = calc(mX, bx, 10, 360);
        if (dDist) AimAssist.distance = calc(mX, bx + 100, 1, 8);
        if (dOffset) AimAssist.pitchOffSet = calc(mX, bx + 200, -2, 2);

        drawModernSliderSmall(bx, by, "FOV", AimAssist.fov, 10, 360);
        drawModernSliderSmall(bx + 100, by, "Dist", AimAssist.distance, 1, 8);
        drawModernSliderSmall(bx + 200, by, "Offset", AimAssist.pitchOffSet, -2, 2);
    }

    // --- COMPONENTS ---
    private void drawButton(int x, int y, String text) {
        drawRoundedRect(x, y, x + 150, y + 20, 4, 0xFF404040);
        this.fontRendererObj.drawString(text, x + 10, y + 6, 0xFFFFFFFF);
    }
    private void drawModernToggle(int x, int y, String label, boolean state) {
        this.fontRendererObj.drawString(label, x, y + 4, 0xDDDDDD);
        int switchX = x + 90;
        int colorBg = state ? 0xFFFFFFFF : 0xFF404040;
        drawRoundedRect(switchX, y, switchX + 30, y + 14, 7, colorBg);
        int knobX = state ? (switchX + 30 - 10 - 2) : (switchX + 2);
        int knobColor = state ? 0xFF000000 : 0xFFBBBBBB;
        drawCircle(knobX + 5, y + 7, 5, knobColor);
    }
    private void drawModernSlider(int x, int y, String label, double val, double min, double max) {
        this.fontRendererObj.drawString(label, x, y, 0xAAAAAA);
        this.fontRendererObj.drawString(String.format("%.1f", val), x + 100, y, 0xFFFFFFFF);
        int slideY = y + 15;
        drawRoundedRect(x, slideY, x + 120, slideY + 2, 1, 0xFF404040);
        double pct = (val - min) / (max - min);
        drawRoundedRect(x, slideY, x + (int)(120 * pct), slideY + 2, 1, 0xFFFFFFFF);
        drawCircle(x + (int)(120 * pct), slideY + 1, 4, 0xFFFFFFFF);
    }
    private void drawModernSliderSmall(int x, int y, String label, double val, double min, double max) {
        this.fontRendererObj.drawString(label, x, y, 0xAAAAAA);
        int slideY = y + 12;
        drawRoundedRect(x, slideY, x + 80, slideY + 2, 1, 0xFF404040);
        double pct = (val - min) / (max - min);
        drawRoundedRect(x, slideY, x + (int)(80 * pct), slideY + 2, 1, 0xFFFFFFFF);
        drawCircle(x + (int)(80 * pct), slideY + 1, 3, 0xFFFFFFFF);
    }

    // --- DRAW HELPERS ---
    public static void drawRoundedRect(float x, float y, float x2, float y2, float round, int color) {
        x += (float)(round / 2.0f); y += (float)(round / 2.0f);
        x2 -= (float)(round / 2.0f); y2 -= (float)(round / 2.0f);
        drawRect(x, y, x2, y2, color);
        drawCircle(x2 - round / 2.0f, y + round / 2.0f, round, color);
        drawCircle(x + round / 2.0f, y2 - round / 2.0f, round, color);
        drawCircle(x + round / 2.0f, y + round / 2.0f, round, color);
        drawCircle(x2 - round / 2.0f, y2 - round / 2.0f, round, color);
        drawRect(x - round / 2.0f, y + round / 2.0f, x2 + round / 2.0f, y2 - round / 2.0f, color);
        drawRect(x + round / 2.0f, y - round / 2.0f, x2 - round / 2.0f, y + round / 2.0f, color);
        drawRect(x + round / 2.0f, y2 - round / 2.0f, x2 - round / 2.0f, y2 + round / 2.0f, color);
    }
    public static void drawCircle(float x, float y, float radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F; float red = (color >> 16 & 0xFF) / 255.0F; float green = (color >> 8 & 0xFF) / 255.0F; float blue = (color & 0xFF) / 255.0F;
        GlStateManager.pushMatrix(); GlStateManager.enableBlend(); GlStateManager.disableTexture2D(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glColor4f(red, green, blue, alpha); GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 360; i++) { double x2 = Math.sin(i * Math.PI / 180.0D) * radius; double y2 = Math.cos(i * Math.PI / 180.0D) * radius; GL11.glVertex2d(x + x2, y + y2); }
        GL11.glEnd(); GlStateManager.enableTexture2D(); GlStateManager.disableBlend(); GlStateManager.popMatrix();
    }
    public static void drawRoundedOutline(float x, float y, float x2, float y2, float round, float thickness, int color) {
        drawRect(x + round, y, x2 - round, y + thickness, color); drawRect(x + round, y2 - thickness, x2 - round, y2, color);
        drawRect(x, y + round, x + thickness, y2 - round, color); drawRect(x2 - thickness, y + round, x2, y2 - round, color);
    }
    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left < right) { float i = left; left = right; right = i; } if (top < bottom) { float j = top; top = bottom; bottom = j; }
        float f3 = (float)(color >> 24 & 255) / 255.0F; float f = (float)(color >> 16 & 255) / 255.0F; float f1 = (float)(color >> 8 & 255) / 255.0F; float f2 = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance(); WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend(); GlStateManager.disableTexture2D(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); GlStateManager.color(f, f1, f2, f3);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION); worldrenderer.pos((double)left, (double)bottom, 0.0D).endVertex();
        worldrenderer.pos((double)right, (double)bottom, 0.0D).endVertex(); worldrenderer.pos((double)right, (double)top, 0.0D).endVertex();
        worldrenderer.pos((double)left, (double)top, 0.0D).endVertex(); tessellator.draw(); GlStateManager.enableTexture2D(); GlStateManager.disableBlend();
    }

    // --- INPUT HANDLING ---
    private double calc(int mX, int x, double min, double max) { double pos = (double)(mX - x) / 120.0; if (pos < 0) pos = 0; if (pos > 1) pos = 1; return min + (max - min) * pos; }
    private double calcSmall(int mX, int x, double min, double max) { double pos = (double)(mX - x) / 80.0; if (pos < 0) pos = 0; if (pos > 1) pos = 1; return min + (max - min) * pos; }
    private boolean checkToggle(int mX, int mY, int x, int y) { return mX >= x + 90 && mX <= x + 120 && mY >= y && mY <= y + 14; }
    private boolean checkSlider(int mX, int mY, int x, int y, int width) { return mX >= x && mX <= x + width && mY >= y + 10 && mY <= y + 20; }
    private void clickedToggle(String name) {}

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            if (mouseY >= pY + 40 && mouseY <= pY + 54) {
                int startTabs = pX + (pW - (260)) / 2;
                if (mouseX >= startTabs && mouseX <= startTabs + 80) currentTab = 0;
                if (mouseX >= startTabs + 90 && mouseX <= startTabs + 170) currentTab = 1;
                if (mouseX >= startTabs + 180 && mouseX <= startTabs + 260) currentTab = 2;
                return;
            }

            if (currentTab == 0) { // CLICKER
                int lx = pX + 40; int y = pY + 70;
                if (checkToggle(mouseX, mouseY, lx, y)) LeftClicker.enabled = !LeftClicker.enabled; y+=30;
                if (checkToggle(mouseX, mouseY, lx, y)) LeftClicker.triggerBot = !LeftClicker.triggerBot; y+=30;
                if (checkToggle(mouseX, mouseY, lx, y)) LeftClicker.weaponOnly = !LeftClicker.weaponOnly; y+=30;
                if (checkToggle(mouseX, mouseY, lx, y)) LeftClicker.breakBlocks = !LeftClicker.breakBlocks;
                int rx = pX + 190; int sy = pY + 70;
                if (checkSlider(mouseX, mouseY, rx, sy, 120)) dMinCPS = true; sy+=40;
                if (checkSlider(mouseX, mouseY, rx, sy, 120)) dMaxCPS = true; sy+=40;
                if (checkSlider(mouseX, mouseY, rx, sy, 120)) dJitter = true;
            } else if (currentTab == 1) { // AIM
                int lx = pX + 30; int y = pY + 70;
                if (checkToggle(mouseX, mouseY, lx, y)) AimAssist.clickAim = !AimAssist.clickAim; y+=25;
                if (checkToggle(mouseX, mouseY, lx, y)) AimAssist.aimPitch = !AimAssist.aimPitch; y+=25;
                if (checkToggle(mouseX, mouseY, lx, y)) AimAssist.weaponOnly = !AimAssist.weaponOnly; y+=25;
                if (checkToggle(mouseX, mouseY, lx, y)) AimAssist.breakBlocks = !AimAssist.breakBlocks;
                int rx = pX + 190; int sy = pY + 70;
                if (checkSlider(mouseX, mouseY, rx, sy, 120)) dSpeedYaw = true; sy+=35;
                if (checkSlider(mouseX, mouseY, rx, sy, 120)) dCompYaw = true; sy+=35;
                if (checkSlider(mouseX, mouseY, rx, sy, 120)) dSpeedPitch = true; sy+=35;
                if (checkSlider(mouseX, mouseY, rx, sy, 120)) dCompPitch = true;
                int bx = pX + 30; int by = pY + 195;
                if (checkSlider(mouseX, mouseY, bx, by, 80)) dFov = true;
                if (checkSlider(mouseX, mouseY, bx + 100, by, 80)) dDist = true;
                if (checkSlider(mouseX, mouseY, bx + 200, by, 80)) dOffset = true;
            } else { // AUTO GRIND
                int lx = pX + 40; int y = pY + 70;
                if (checkToggle(mouseX, mouseY, lx, y)) AutoGrind.enabled = !AutoGrind.enabled; y+=40;
                if (mouseX >= lx && mouseX <= lx + 150 && mouseY >= y && mouseY <= y + 20) {
                    AutoGrind.setLocation();
                }
                y+=25;
                int rx = pX + 40;
                if (checkSlider(mouseX, mouseY, rx, y, 120)) dRadius = true; y+=30;
                if (checkSlider(mouseX, mouseY, rx, y, 120)) dSpawnDelay = true; y+=30;
                if (checkSlider(mouseX, mouseY, rx, y, 120)) dChaseRange = true; y+=30;
                if (checkSlider(mouseX, mouseY, rx, y, 120)) dStopRange = true; y+=30;
                if (checkSlider(mouseX, mouseY, rx, y, 120)) dAutoDisable = true;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dMinCPS = false; dMaxCPS = false; dJitter = false;
        dSpeedYaw = false; dCompYaw = false; dSpeedPitch = false; dCompPitch = false;
        dFov = false; dDist = false; dOffset = false;
        dRadius = false; dSpawnDelay = false; dChaseRange = false; dStopRange = false;
        dAutoDisable = false; // Reset the drag state
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
