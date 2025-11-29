package com.monolith.monolith.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class TargetHUD {

    public static boolean enabled = false;
    private final Minecraft mc = Minecraft.getMinecraft();

    // Animation state
    private float animatedHealth = 20.0f;
    private EntityLivingBase lastTarget = null;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        Entity targetEntity = mc.pointedEntity;

        if (targetEntity instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) targetEntity;

            // Handle animation resetting on target switch
            if (target != lastTarget) {
                lastTarget = target;
                animatedHealth = target.getHealth();
            }

            render(target);
        } else {
            lastTarget = null;
        }
    }

    private void render(EntityLivingBase target) {
        ScaledResolution sr = new ScaledResolution(mc);

        // Position
        float w = 150;
        float h = 50;
        float x = (sr.getScaledWidth() / 2.0f) + 20;
        float y = (sr.getScaledHeight() / 2.0f) + 20;

        // Smooth Health Animation
        float targetHealth = target.getHealth();
        if (animatedHealth < targetHealth) animatedHealth = targetHealth;
        else animatedHealth += (targetHealth - animatedHealth) * 0.1f;

        // Background
        MonolithModGUI.RenderUtil.drawPerfectRoundedRect(x, y, w, h, 8, 0xD0000000);
        MonolithModGUI.RenderUtil.drawRoundedOutline(x, y, w, h, 8, 1.5f, 0xAA9050FF);

        // 3D Face Render
        if (target instanceof AbstractClientPlayer) {
            try {
                mc.getTextureManager().bindTexture(((AbstractClientPlayer) target).getLocationSkin());
                GL11.glColor4f(1, 1, 1, 1);
                int faceSize = 32;
                int faceX = (int)x + 8;
                int faceY = (int)y + 8;
                // Face Layer
                Gui.drawScaledCustomSizeModalRect(faceX, faceY, 8.0F, 8.0F, 8, 8, faceSize, faceSize, 64.0F, 64.0F);
                // Hat Layer
                Gui.drawScaledCustomSizeModalRect(faceX, faceY, 40.0F, 8.0F, 8, 8, faceSize, faceSize, 64.0F, 64.0F);
            } catch (Exception e) {
                MonolithModGUI.RenderUtil.drawRect(x + 8, y + 8, 32, 32, 0xFF555555);
            }
        } else {
            MonolithModGUI.RenderUtil.drawRect(x + 8, y + 8, 32, 32, 0xFF555555);
        }

        // Name & Distance
        String name = target.getName();
        mc.fontRendererObj.drawStringWithShadow(name, x + 46, y + 8, -1);
        String dist = String.format("%.1fm", mc.thePlayer.getDistanceToEntity(target));
        mc.fontRendererObj.drawStringWithShadow(dist, x + w - mc.fontRendererObj.getStringWidth(dist) - 6, y + 8, 0xFFAAAAAA);

        // Health Bar
        float maxHp = target.getMaxHealth();
        float percent = animatedHealth / maxHp;
        if(percent > 1) percent = 1; if(percent < 0) percent = 0;

        float barX = x + 46; float barY = y + 22;
        float barW = w - 54; float barH = 10;

        MonolithModGUI.RenderUtil.drawPerfectRoundedRect(barX, barY, barW, barH, 3, 0xFF303030);
        int color = getHealthColor(target.getHealth() / maxHp);
        if (percent > 0) {
            MonolithModGUI.RenderUtil.drawPerfectRoundedRect(barX, barY, barW * percent, barH, 3, color);
        }

        // HP Text
        String hpStr = String.format("%.1f HP", animatedHealth);
        float scale = 0.8f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(barX + barW/2, barY + 3, 0);
        GlStateManager.scale(scale, scale, 1);
        MonolithModGUI.RenderUtil.drawCenteredString(hpStr, 0, 0, 0xFFFFFFFF);
        GlStateManager.popMatrix();

        // Armor + Enchants
        renderArmor(target, (int)x + 46, (int)y + 34);
    }

    private void renderArmor(EntityLivingBase target, int x, int y) {
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        int spacing = 19; // Slightly wider spacing to separate text

        // 1. Armor (Helm to Boots order)
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = target.getCurrentArmor(i);
            if (stack != null) {
                // Draw Item
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);

                // Draw Enchants (New Method)
                renderItemEnchants(stack, x, y);

                x += spacing;
            }
        }

        // 2. Held Item
        ItemStack held = target.getHeldItem();
        if (held != null) {
            mc.getRenderItem().renderItemAndEffectIntoGUI(held, x + 2, y); // Extra gap
            renderItemEnchants(held, x + 2, y);
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void renderItemEnchants(ItemStack stack, int x, int y) {
        NBTTagList enchants = stack.getEnchantmentTagList();
        if (enchants == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.disableDepth(); // Ensure text draws ON TOP of item
        GlStateManager.scale(0.5, 0.5, 1); // Make text tiny

        int yOffset = -4; // Start slightly above the center to stack

        for (int i = 0; i < enchants.tagCount(); i++) {
            short id = enchants.getCompoundTagAt(i).getShort("id");
            short lvl = enchants.getCompoundTagAt(i).getShort("lvl");

            String code = "";
            // Map Important Enchants to Short Codes
            if (id == 0) code = "P" + lvl;       // Protection
            else if (id == 1) code = "F" + lvl;  // Fire Prot
            else if (id == 3) code = "B" + lvl;  // Blast Prot
            else if (id == 4) code = "Pr" + lvl; // Proj Prot
            else if (id == 7) code = "Th" + lvl; // Thorns
            else if (id == 16) code = "S" + lvl; // Sharpness
            else if (id == 20) code = "F" + lvl; // Fire Aspect
            else if (id == 19) code = "Kb" + lvl;// Knockback
            else if (id == 48) code = "Pw" + lvl;// Power
            else if (id == 49) code = "Pn" + lvl;// Punch
            else if (id == 51) code = "Inf";     // Infinity

            if (!code.isEmpty()) {
                // Centering math for scaled text
                // We are at scale 0.5. To draw at 'x', we use 'x*2'.
                // Item center is x+8. Scaled center is 2x+16.
                int strW = mc.fontRendererObj.getStringWidth(code);
                int drawX = (x * 2) + 16 - (strW / 2);
                int drawY = (y * 2) + 8 + yOffset;

                mc.fontRendererObj.drawStringWithShadow(code, drawX, drawY, 0xFFFFFF);

                yOffset += 9; // Move down for next enchant

                // Don't draw more than 2 lines or it gets messy
                if (yOffset > 6) break;
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private int getHealthColor(float pct) {
        return Color.HSBtoRGB(Math.max(0.0F, pct) / 3.0F, 1.0F, 1.0F);
    }
}
