package com.monolith.monolith.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class ItemESP {

    // --- MAIN TOGGLES ---
    public static boolean enabled = true;

    // --- CATEGORIES ---
    public static boolean showGold = true;      // Gold Ingots
    public static boolean showMystics = true;   // Fresh Pants / Bows / Special Swords
    public static boolean showValuables = true; // Coal / Cactus / Obsi / Diamond

    // --- EXTRAS ---
    public static boolean soundAlert = true;    // Play "Orb" sound on rare drop
    public static boolean traceGold = false;    // Line to Gold
    public static boolean traceMystics = true;  // Line to Mystics
    public static boolean traceValuables = false;// Line to Valuables

    // --- INTERNAL ---
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Set<Integer> soundedEntities = new HashSet<>();

    // View Bobbing Fix Variables
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
        if (!enabled) {
            soundedEntities.clear();
            return;
        }

        // Auto-clear cache on world change
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.ticksExisted < 5) soundedEntities.clear();

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityItem)) continue;

            EntityItem entityItem = (EntityItem) obj;
            ItemStack stack = entityItem.getEntityItem();
            Item item = stack.getItem();

            Color color = null;
            boolean isImportant = false; // For sound/tracers

            // 1. MYSTICS (Priority Red)
            if (showMystics && isMystic(stack)) {
                color = new Color(255, 0, 0); // Red
                isImportant = true;
                if (traceMystics) renderTracer(entityItem, event.partialTicks, color);
            }
            // 2. DIAMONDS (Cyan) -> Part of Valuables category
            else if (showValuables && isDiamond(item)) {
                color = new Color(0, 255, 255); // Cyan
                isImportant = true;
                if (traceValuables) renderTracer(entityItem, event.partialTicks, color);
            }
            // 3. GOLD (Gold)
            else if (showGold && item == Items.gold_ingot) {
                color = new Color(255, 215, 0); // Gold
                if (traceGold) renderTracer(entityItem, event.partialTicks, color);
            }
            // 4. RESOURCES (Green) -> Part of Valuables category
            else if (showValuables && (item == Items.coal || item == Item.getItemFromBlock(Blocks.cactus) || item == Item.getItemFromBlock(Blocks.obsidian))) {
                color = new Color(0, 255, 0); // Green
                if (traceValuables) renderTracer(entityItem, event.partialTicks, color);
            }

            // RENDER BOX & PLAY SOUND
            if (color != null) {
                if (mc.thePlayer.getDistanceToEntity(entityItem) <= 100) {
                    renderItemBox(entityItem, event.partialTicks, color);

                    if (soundAlert && isImportant) {
                        if (!soundedEntities.contains(entityItem.getEntityId())) {
                            mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.orb", 1.0f, 1.0f, false);
                            soundedEntities.add(entityItem.getEntityId());
                        }
                    }
                }
            }
        }
    }

    private boolean isDiamond(Item item) {
        return item == Items.diamond_helmet ||
               item == Items.diamond_chestplate ||
               item == Items.diamond_leggings ||
               item == Items.diamond_boots ||
               item == Items.diamond_sword;
    }

    private boolean isMystic(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.leather_leggings) return true;
        if (item == Items.golden_sword || item == Items.iron_sword || item == Items.bow) return true;
        if (stack.hasDisplayName()) {
            String name = stack.getDisplayName().toLowerCase();
            return name.contains("fresh") || name.contains("mystic") || name.contains("tier");
        }
        return false;
    }

    private void renderItemBox(Entity entity, float partialTicks, Color color) {
        double viewerPosX = mc.getRenderManager().viewerPosX;
        double viewerPosY = mc.getRenderManager().viewerPosY;
        double viewerPosZ = mc.getRenderManager().viewerPosZ;

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - viewerPosZ;

        AxisAlignedBB bb = new AxisAlignedBB(
            x - 0.25, y, z - 0.25,
            x + 0.25, y + 0.5, z + 0.25
        );

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.5F);
        GL11.glLineWidth(1.5F);

        RenderGlobal.drawSelectionBoundingBox(bb);

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    private void renderTracer(Entity entity, float partialTicks, Color color) {
        // --- PREPARE GL STATE (From your snippet) ---
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(1.5F);

        // --- MATH (From your snippet) ---
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;

        // Calculate Eye Vector
        Vec3 eyeVec = new Vec3(0, 0, 1)
                .rotatePitch(-(float) Math.toRadians(mc.thePlayer.rotationPitch))
                .rotateYaw(-(float) Math.toRadians(mc.thePlayer.rotationYaw));

        // Set Color
        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1.0f);

        // Draw Line
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(eyeVec.xCoord, mc.thePlayer.getEyeHeight() + eyeVec.yCoord, eyeVec.zCoord);
        GL11.glVertex3d(x, y + 0.25, z); // +0.25 to target center of item
        GL11.glEnd();

        // --- RESTORE GL STATE ---
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
