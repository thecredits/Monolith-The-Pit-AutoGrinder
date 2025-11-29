package com.monolith.monolith.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HudManager {
    public static final HudManager INSTANCE = new HudManager();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final File configFile;

    public List<HudComponent> components = new ArrayList<>();

    // Dragging State
    private boolean isDragging = false;
    private HudComponent draggingComponent = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudManager() {
        this.configFile = new File(mc.mcDataDir, "monolith_hud_config.txt");
    }

    public void init() {
        // Register Components
        components.add(new HudComponents.FpsComponent());
        components.add(new HudComponents.CpsComponent());
        components.add(new HudComponents.KosComponent());
        loadConfig();
    }

    // --- GAME RENDER LOOP ---
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.gameSettings.showDebugInfo) return;

        // Render enabled components normally
        for (HudComponent c : components) {
            if (c.isEnabled()) {
                c.render(0, 0);
            }
        }
    }

    // --- EDITOR RENDER LOOP ---
    public void drawEditor(int mouseX, int mouseY) {
        handleMouseMovement(mouseX, mouseY);

        if (draggingComponent != null) {
            runSnappingLogic(draggingComponent);
        }

        for (HudComponent c : components) {
            if (!c.isEnabled()) continue;

            // Draw visual editor box
            boolean isSelected = (c == draggingComponent);
            boolean isHovered = isOver(mouseX, mouseY, c);

            int bgColor = isSelected ? 0x6050FF50 : (isHovered ? 0x40FFFFFF : 0x20FFFFFF);
            int borderColor = isSelected ? 0xFF00FF00 : 0xFFFFFFFF;

            MonolithModGUI.RenderUtil.drawPerfectRoundedRect(c.x, c.y, c.w, c.h, 4, bgColor);
            MonolithModGUI.RenderUtil.drawRoundedOutline(c.x, c.y, c.w, c.h, 4, 1f, borderColor);

            // Render content
            c.render(mouseX, mouseY);
        }
    }

    // --- SNAPPING LOGIC ---
    private void runSnappingLogic(HudComponent selected) {
        int snapRange = 4;
        boolean snappedX = false;
        boolean snappedY = false;

        float destX = selected.x;
        float destY = selected.y;

        for (HudComponent other : components) {
            if (other == selected || !other.isEnabled()) continue;

            // X-Axis Snapping
            if (Math.abs(selected.x - other.x) < snapRange) { destX = other.x; drawSnapLineX(other.x); snappedX = true; } // L-L
            else if (Math.abs((selected.x + selected.w) - (other.x + other.w)) < snapRange) { destX = other.x + other.w - selected.w; drawSnapLineX(other.x + other.w); snappedX = true; } // R-R
            else if (Math.abs(selected.x - (other.x + other.w)) < snapRange) { destX = other.x + other.w; drawSnapLineX(other.x + other.w); snappedX = true; } // L-R
            else if (Math.abs((selected.x + selected.w) - other.x) < snapRange) { destX = other.x - selected.w; drawSnapLineX(other.x); snappedX = true; } // R-L
            else if (Math.abs((selected.x + selected.w/2) - (other.x + other.w/2)) < snapRange) { destX = other.x + other.w/2 - selected.w/2; drawSnapLineX(other.x + other.w/2); snappedX = true; } // Center

            // Y-Axis Snapping
            if (Math.abs(selected.y - other.y) < snapRange) { destY = other.y; drawSnapLineY(other.y); snappedY = true; } // T-T
            else if (Math.abs((selected.y + selected.h) - (other.y + other.h)) < snapRange) { destY = other.y + other.h - selected.h; drawSnapLineY(other.y + other.h); snappedY = true; } // B-B
            else if (Math.abs(selected.y - (other.y + other.h)) < snapRange) { destY = other.y + other.h; drawSnapLineY(other.y + other.h); snappedY = true; } // T-B
            else if (Math.abs((selected.y + selected.h) - other.y) < snapRange) { destY = other.y - selected.h; drawSnapLineY(other.y); snappedY = true; } // B-T
        }

        if (snappedX) selected.x = (int)destX;
        if (snappedY) selected.y = (int)destY;
    }

    private void drawSnapLineX(float x) {
        ScaledResolution sr = new ScaledResolution(mc);
        MonolithModGUI.RenderUtil.drawRect(x, 0, 1, sr.getScaledHeight(), 0xFF00FFFF);
    }

    private void drawSnapLineY(float y) {
        ScaledResolution sr = new ScaledResolution(mc);
        MonolithModGUI.RenderUtil.drawRect(0, y, sr.getScaledWidth(), 1, 0xFF00FFFF);
    }

    // --- MOUSE INPUT ---
    private void handleMouseMovement(int mX, int mY) {
        if (isDragging && draggingComponent != null) {
            draggingComponent.x = mX - dragOffsetX;
            draggingComponent.y = mY - dragOffsetY;
        }
    }

    public void mouseClicked(int mX, int mY, int btn) {
        if (btn == 0) {
            // Iterate reverse to click top-most element
            for (int i = components.size() - 1; i >= 0; i--) {
                HudComponent c = components.get(i);
                if (c.isEnabled() && isOver(mX, mY, c)) {
                    isDragging = true;
                    draggingComponent = c;
                    dragOffsetX = mX - c.x;
                    dragOffsetY = mY - c.y;
                    return;
                }
            }
        }
    }

    public void mouseReleased(int mX, int mY, int state) {
        isDragging = false;
        draggingComponent = null;
        saveConfig();
    }

    private boolean isOver(int mX, int mY, HudComponent c) {
        return mX >= c.x && mX <= c.x + c.w && mY >= c.y && mY <= c.y + c.h;
    }

    // --- SAVE / LOAD ---
    public void saveConfig() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(configFile))) {
            for (HudComponent c : components) {
                writer.println(c.name + ":" + c.x + ":" + c.y);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void loadConfig() {
        if (!configFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    for (HudComponent c : components) {
                        if (c.name.equals(parts[0])) {
                            try {
                                c.x = Integer.parseInt(parts[1]);
                                c.y = Integer.parseInt(parts[2]);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- ABSTRACT COMPONENT ---
    public static abstract class HudComponent {
        public String name;
        public int x, y, w, h;
        public HudComponent(String name, int x, int y) { this.name = name; this.x = x; this.y = y; }
        public abstract void render(int mX, int mY);
        public abstract boolean isEnabled();
    }
}
