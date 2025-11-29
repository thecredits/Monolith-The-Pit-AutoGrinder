package com.monolith.monolith.modules.combat;

import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class AutoWeapon {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static boolean onlyWhenHoldingDown = true;
    public static boolean goBackToPrevSlot = true;

    private boolean onWeapon;
    private int prevSlot;
    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Post event) {
        // Only run on TEXT overlay to simulate Render2D (runs once per frame)
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        if (mc.thePlayer == null || mc.currentScreen != null) return;
        if (!enabled) return;

        // Logic from original code
        if (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null
                || (onlyWhenHoldingDown && !Mouse.isButtonDown(0))) {
            if (onWeapon) {
                onWeapon = false;
                if (goBackToPrevSlot) {
                    mc.thePlayer.inventory.currentItem = prevSlot;
                }
            }
        } else {
            if (onlyWhenHoldingDown) {
                if (!Mouse.isButtonDown(0))
                    return;
            }

            if (!onWeapon) {
                prevSlot = mc.thePlayer.inventory.currentItem;
                onWeapon = true;

                int maxDamageSlot = getMaxDamageSlot();

                // If we found a better weapon, switch to it
                if (maxDamageSlot > -1 && getSlotDamage(maxDamageSlot) > getSlotDamage(mc.thePlayer.inventory.currentItem)) {
                    mc.thePlayer.inventory.currentItem = maxDamageSlot;
                }
            }
        }
    }

    // --- UTILS LOCAL IMPLEMENTATION ---

    private int getMaxDamageSlot() {
        int bestSlot = -1;
        double maxDamage = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null) {
                double damage = getSlotDamage(i);
                if (damage > maxDamage) {
                    maxDamage = damage;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    private double getSlotDamage(int slot) {
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if (stack == null) return 0;

        double damage = 0;

        // 1. Get Attribute Damage (Base tool damage)
        Multimap<String, AttributeModifier> modifiers = stack.getAttributeModifiers();
        if (modifiers.containsKey(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName())) {
            for (AttributeModifier modifier : modifiers.get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName())) {
                damage += modifier.getAmount();
            }
        }

        // 2. Add Base Hand Damage
        damage += 1.0;

        // 3. Add Sharpness Enchantment
        if (EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) > 0) {
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25;
        }

        return damage;
    }
}
