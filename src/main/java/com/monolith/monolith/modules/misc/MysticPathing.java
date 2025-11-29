package com.monolith.monolith.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;

public class MysticPathing {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static double range = 60.0D;
    public static double speed = 10.0D;
    public static boolean autoJump = true;
    public static boolean pathValuables = false;

    // AimAssist Constants
    private static final double complimentYaw = 15.0D;
    private static final double complimentPitch = 15.0D;

    private static final Minecraft mc = Minecraft.getMinecraft();

    // --- TARGET LOCKING ---
    private EntityItem currentTarget = null;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // 1. PIT SAFETY (Auto-Disable)
        if (mc.thePlayer.posY < 75.0D) {
            if (enabled) {
                enabled = false;
                resetInput();
                currentTarget = null;
            }
            return;
        }

        // 2. GUI SAFETY
        if (mc.currentScreen != null) {
            resetInput();
            return;
        }

        // 3. DISABLE LOGIC
        if (!enabled) {
            if (currentTarget != null) {
                currentTarget = null;
                resetInput();
            }
            return;
        }

        // 4. TARGET VALIDATION
        if (currentTarget != null) {
            if (!isValidTarget(currentTarget)) {
                currentTarget = null;
            }
        }

        // 5. TARGET SEARCH
        if (currentTarget == null) {
            currentTarget = getClosestTarget();
        }

        // 6. MOVEMENT LOGIC
        if (currentTarget != null) {
            double dist = mc.thePlayer.getDistanceToEntity(currentTarget);

            // --- SPIN & OVERSHOOT FIX ---
            // 1. Aim until we are very close (0.6) to ensure we face it directly.
            if (dist > 0.6D) {
                applyExactAimAssist(currentTarget);
            }

            // 2. Stop walking slightly before (0.8) to let momentum carry us onto the item.
            // This prevents running through it at full speed.
            boolean shouldWalk = dist > 0.8D;
            // ----------------------------

            if (shouldWalk && isSafeToWalk()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);

                if (autoJump && mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                }
            } else {
                // Stop walking if close enough OR unsafe
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            }
        } else {
            resetInput();
        }
    }

    private boolean isValidTarget(EntityItem item) {
        if (item == null) return false;
        if (!item.isEntityAlive()) return false;
        if (item.posY < 75.0D) return false;
        if (mc.thePlayer.posY - item.posY > 3.5D) return false;
        if (mc.thePlayer.getDistanceToEntity(item) > range) return false;
        if (!mc.thePlayer.canEntityBeSeen(item)) return false;
        return true;
    }

    private EntityItem getClosestTarget() {
        EntityItem closest = null;
        double closestDist = range * range;

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityItem)) continue;
            EntityItem item = (EntityItem) obj;

            if (!isValidTarget(item)) continue;

            ItemStack stack = item.getEntityItem();
            boolean isValid = false;

            if (isMystic(stack)) isValid = true;
            if (!isValid && pathValuables && isValuable(stack)) isValid = true;

            if (!isValid) continue;

            double dist = mc.thePlayer.getDistanceSqToEntity(item);
            if (dist < closestDist) {
                closest = item;
                closestDist = dist;
            }
        }
        return closest;
    }

    private boolean isSafeToWalk() {
        if (mc.thePlayer.posY < 75) return true;

        double rad = Math.toRadians(mc.thePlayer.rotationYaw);
        double dx = -Math.sin(rad) * 1.2;
        double dz = Math.cos(rad) * 1.2;

        BlockPos nextPos = new BlockPos(mc.thePlayer.posX + dx, mc.thePlayer.posY - 1, mc.thePlayer.posZ + dz);

        if (mc.theWorld.isAirBlock(nextPos)) {
            return false;
        }
        return true;
    }

    private void resetInput() {
        int key = mc.gameSettings.keyBindForward.getKeyCode();
        boolean pressed = false;
        try { if (key > 0) pressed = Keyboard.isKeyDown(key); } catch (Exception e) {}
        KeyBinding.setKeyBindState(key, pressed);
    }

    private void applyExactAimAssist(Entity target) {
        double aimSpeed = 20.0 + (speed * 2.0);
        if (aimSpeed < 5.0) aimSpeed = 5.0;
        if (aimSpeed > 90.0) aimSpeed = 90.0;

        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;

        double n = fovFromEntity(target);
        if (n > 1.0D || n < -1.0D) {
            double complimentSpeed = n * (ThreadLocalRandom.current().nextDouble(complimentYaw - 1.47, complimentYaw + 2.48) / 100);
            float val = (float) (-(complimentSpeed + (n / (101.0D - (float) ThreadLocalRandom.current().nextDouble(aimSpeed - 4.72, aimSpeed)))));
            val -= val % gcd;
            mc.thePlayer.rotationYaw += val;
        }

        double nPitch = pitchFromEntity(target, 0.0f);
        if (nPitch > 1.0D || nPitch < -1.0D) {
            double complimentSpeed = nPitch * (ThreadLocalRandom.current().nextDouble(complimentPitch - 1.47, complimentPitch + 2.48) / 100);
            float val = (float) (-(complimentSpeed + (nPitch / (101.0D - (float) ThreadLocalRandom.current().nextDouble(aimSpeed - 4.72, aimSpeed)))));
            val -= val % gcd;
            mc.thePlayer.rotationPitch += val;

            if (mc.thePlayer.rotationPitch > 90.0F) mc.thePlayer.rotationPitch = 90.0F;
            if (mc.thePlayer.rotationPitch < -90.0F) mc.thePlayer.rotationPitch = -90.0F;
        }
    }

    public double fovFromEntity(Entity en) {
        return ((mc.thePlayer.rotationYaw - getRotations(en)[0]) % 360.0D + 540.0D) % 360.0D - 180.0D;
    }

    public double pitchFromEntity(Entity en, float offset) {
        return mc.thePlayer.rotationPitch - (getRotations(en)[1] + offset);
    }

    private float[] getRotations(Entity e) {
        double deltaX = e.posX + (e.posX - e.lastTickPosX) - mc.thePlayer.posX;
        double deltaY = e.posY + 0.25 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = e.posZ + (e.posZ - e.lastTickPosZ) - mc.thePlayer.posZ;

        double dist = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaZ, 2));

        float yaw = (float) Math.toDegrees(-Math.atan(deltaX / deltaZ));
        float pitch = (float) -Math.toDegrees(Math.atan(deltaY / dist));

        if (deltaX < 0 && deltaZ < 0) yaw = 90 + (float) Math.toDegrees(Math.atan(deltaZ / deltaX));
        else if (deltaX > 0 && deltaZ < 0) yaw = -90 + (float) Math.toDegrees(Math.atan(deltaZ / deltaX));

        return new float[] { yaw, pitch };
    }

    private boolean isMystic(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.leather_leggings) return true;
        if (item == Items.golden_sword || item == Items.iron_sword || item == Items.bow || item == Items.diamond_sword) {
            return true;
        }
        if (stack.hasDisplayName()) {
            String name = stack.getDisplayName().toLowerCase();
            return name.contains("fresh") || name.contains("mystic") || name.contains("tier");
        }
        return false;
    }

    private boolean isValuable(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.gold_ingot) return true;
        if (item == Items.coal || item == Item.getItemFromBlock(Blocks.cactus) || item == Item.getItemFromBlock(Blocks.obsidian)) return true;
        if (item == Items.diamond_helmet || item == Items.diamond_chestplate || item == Items.diamond_leggings || item == Items.diamond_boots || item == Items.diamond_sword) return true;
        return false;
    }
}
