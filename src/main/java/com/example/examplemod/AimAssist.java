package com.example.examplemod;

import java.util.concurrent.ThreadLocalRandom;
import org.lwjgl.input.Mouse;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class AimAssist {
    // --- SETTINGS (Blatant Removed) ---
    public static double speedYaw = 45.0D, complimentYaw = 15.0D;
    public static double speedPitch = 45.0D, complimentPitch = 15.0D;
    public static double fov = 90.0D, distance = 4.5D, pitchOffSet = 4.0D;
    public static boolean clickAim = true, breakBlocks = true, weaponOnly = false, aimPitch = false;

    private Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != Phase.END) return;
        try {
            if (mc.currentScreen != null || mc.thePlayer == null || mc.theWorld == null) return;

            // Break Blocks Check
            if (breakBlocks && mc.objectMouseOver != null) {
                BlockPos p = mc.objectMouseOver.getBlockPos();
                if (p != null) {
                    Block bl = mc.theWorld.getBlockState(p).getBlock();
                    if (bl != Blocks.air && !(bl instanceof BlockLiquid)) return;
                }
            }

            // Weapon Only Check
            if (weaponOnly && !isPlayerHoldingWeapon()) return;

            // Trigger Logic
            boolean isAutoClicking = LeftClicker.enabled && Mouse.isButtonDown(0);
            if ((clickAim && isAutoClicking) || (Mouse.isButtonDown(0)) || !clickAim) {
                Entity en = getEnemy();
                if (en != null) {
                    // --- AUTHENTIC RAVEN MATH (Legit Mode Only) ---

                    // Yaw Logic
                    double n = fovFromEntity(en);
                    if (n > 1.0D || n < -1.0D) {
                        double complimentSpeed = n * (ThreadLocalRandom.current().nextDouble(complimentYaw - 1.47328, complimentYaw + 2.48293) / 100);
                        float val = (float) (-(complimentSpeed + (n / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedYaw - 4.723847, speedYaw)))));
                        mc.thePlayer.rotationYaw += val;
                    }

                    // Pitch Logic
                    if (aimPitch) {
                        double nPitch = pitchFromEntity(en, (float) pitchOffSet);
                        double complimentSpeed = nPitch * (ThreadLocalRandom.current().nextDouble(complimentPitch - 1.47328, complimentPitch + 2.48293) / 100);
                        float val = (float) (-(complimentSpeed + (nPitch / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedPitch - 4.723847, speedPitch)))));
                        mc.thePlayer.rotationPitch += val;
                    }
                }
            }
        } catch (Exception e) {}
    }

    // --- UTILS (Identical Logic) ---
    public Entity getEnemy() {
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity e : mc.theWorld.playerEntities) {
            if (e == mc.thePlayer) continue;
            if (!isValid(e)) continue;
            double dist = mc.thePlayer.getDistanceToEntity(e);
            if (dist <= distance && dist < closestDist) { closest = e; closestDist = dist; }
        }
        return closest;
    }

    private boolean isValid(Entity e) {
        if (!(e instanceof EntityLivingBase)) return false;
        if (((EntityLivingBase) e).getHealth() <= 0) return false;
        if (e.isInvisible()) return false;
        if (Math.abs(fovFromEntity(e)) > fov) return false;
        return true;
    }

    public double fovFromEntity(Entity en) { return ((mc.thePlayer.rotationYaw - getRotations(en)[0]) % 360.0D + 540.0D) % 360.0D - 180.0D; }
    public double pitchFromEntity(Entity en, float offset) { return mc.thePlayer.rotationPitch - (getRotations(en)[1] + offset); }

    private float[] getRotations(Entity e) {
        double deltaX = e.posX + (e.posX - e.lastTickPosX) - mc.thePlayer.posX;
        double deltaY = e.posY - 3.5 + e.getEyeHeight() - mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double deltaZ = e.posZ + (e.posZ - e.lastTickPosZ) - mc.thePlayer.posZ;
        double dist = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaZ, 2));
        float yaw = (float) Math.toDegrees(-Math.atan(deltaX / deltaZ));
        float pitch = (float) -Math.toDegrees(Math.atan(deltaY / dist));
        if (deltaX < 0 && deltaZ < 0) yaw = 90 + (float) Math.toDegrees(Math.atan(deltaZ / deltaX));
        else if (deltaX > 0 && deltaZ < 0) yaw = -90 + (float) Math.toDegrees(Math.atan(deltaZ / deltaX));
        return new float[] { yaw, pitch };
    }

    public boolean isPlayerHoldingWeapon() {
        if (mc.thePlayer.getCurrentEquippedItem() == null) return false;
        if (mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword) return true;
        if (mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemAxe) return true;
        return false;
    }
}
