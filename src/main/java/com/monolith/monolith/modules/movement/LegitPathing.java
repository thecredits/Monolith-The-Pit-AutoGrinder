package com.monolith.monolith.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;
import com.monolith.monolith.gui.MonolithModGUI;

public class LegitPathing {

    public static boolean enabled = false;
    public static boolean renderESP = false;

    // Target Coordinates
    public static double tX = 0;
    public static double tY = 0;
    public static double tZ = 0;

    // State Machine
    private int currentState = 0;
    private long waitEndTime = 0;

    private final Minecraft mc = Minecraft.getMinecraft();

    public static void setTarget(BlockPos pos) {
        // Randomize the target slightly to avoid walking to same pixel (Anti-Ban)
        double offset = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
        tX = pos.getX() + 0.5 + offset;
        tY = pos.getY();
        tZ = pos.getZ() + 0.5 + offset;
    }

    public static void setTargetToCurrent() {
        if(Minecraft.getMinecraft().thePlayer == null) return;
        tX = Minecraft.getMinecraft().thePlayer.posX;
        tY = Minecraft.getMinecraft().thePlayer.posY;
        tZ = Minecraft.getMinecraft().thePlayer.posZ;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // --- INVENTORY SAFETY ---
        if (mc.currentScreen != null) {
            if (currentState != 0) {
                resetBotInput();
                currentState = 0;
            }
            return;
        }

        if (!enabled || !MonolithModGUI.AutoGrinder.enabled) {
            if (currentState != 0) {
                resetBotInput();
                currentState = 0;
            }
            return;
        }

        // --- PIT FALL SAFETY (FIXED) ---
        // If we are below Spawn (Y < 80), stop pathing logic.
        if (mc.thePlayer.posY < 80) {
            // ONLY reset inputs if we were previously pathing (currentState != 0).
            // This prevents overwriting AimAssist's movement inputs every tick.
            if (currentState != 0) {
                resetBotInput();
                currentState = 0;
            }
            return; // Stop here so we don't interfere with AimAssist
        }

        double dX = tX - mc.thePlayer.posX;
        double dZ = tZ - mc.thePlayer.posZ;
        double dist = Math.sqrt(dX * dX + dZ * dZ);

        if (dist > 1.5) {
            // STATE 0: DECIDE
            if (currentState == 0) {
                if (MonolithModGUI.AutoGrinder.useSpawnDelay) {
                    double maxSec = MonolithModGUI.AutoGrinder.spawnDelaySec;
                    long delayMs = (long) (ThreadLocalRandom.current().nextDouble(0.5, maxSec) * 1000);
                    waitEndTime = System.currentTimeMillis() + delayMs;
                    currentState = 1;
                } else {
                    currentState = 2;
                }
            }

            // STATE 1: WAITING
            if (currentState == 1) {
                if (System.currentTimeMillis() < waitEndTime) {
                    // Waiting... ensure keys are up
                    resetBotInput();
                    return;
                } else {
                    currentState = 2;
                }
            }

            // STATE 2: WALK & ROTATE
            if (currentState == 2) {
                float[] neededRotations = getRotationsToTarget(dX, tY, dZ);
                float targetYaw = neededRotations[0];
                float targetPitch = 0.0f;

                // 1. Apply Rotation (Smooth)
                applyRavenRotation(targetYaw, targetPitch);

                // 2. MOVEMENT (Curved - No stopping)
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);

                // --- JUMP & SPRINT LOGIC ---
                if (MonolithModGUI.AutoGrinder.jumpAndSprint) {
                    // Always sprint
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);

                    // Only jump if on ground (Prevents "Bhop" flags)
                    if (mc.thePlayer.onGround) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                    } else {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                    }
                } else {
                    // Standard collision jump
                    if (mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                }
            }

        } else {
            // Arrived
            if (currentState != 0) {
                resetBotInput();
                currentState = 0;
            }
        }
    }

    private void resetBotInput() {
        setKey(mc.gameSettings.keyBindForward.getKeyCode(), false);
        setKey(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        setKey(mc.gameSettings.keyBindJump.getKeyCode(), false);
    }

    private void setKey(int key, boolean state) {
        try {
            // Only force keys if the user isn't holding them (User override)
            if (key > 0 && !Keyboard.isKeyDown(key)) {
                KeyBinding.setKeyBindState(key, state);
            } else if (key > 0 && state) {
                // If we want to press it, force it true
                KeyBinding.setKeyBindState(key, true);
            }
        } catch (Exception ignored) {}
    }

    private void applyRavenRotation(float targetYaw, float targetPitch) {
        double speedYaw = MonolithModGUI.AutoGrinder.speedYaw;
        double complimentYaw = MonolithModGUI.AutoGrinder.complimentYaw;
        double speedPitch = MonolithModGUI.AutoGrinder.speedPitch;
        double complimentPitch = MonolithModGUI.AutoGrinder.complimentPitch;

        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;

        // Yaw
        double n = ((mc.thePlayer.rotationYaw - targetYaw) % 360.0D + 540.0D) % 360.0D - 180.0D;
        if (n > 1.0D || n < -1.0D) {
            double complimentSpeed = n * (ThreadLocalRandom.current().nextDouble(complimentYaw - 1.47328, complimentYaw + 2.48293) / 100);
            float val = (float) (-(complimentSpeed + (n / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedYaw - 4.723847, speedYaw)))));
            val -= val % gcd;
            mc.thePlayer.rotationYaw += val;
        }

        // Pitch
        double nPitch = ((mc.thePlayer.rotationPitch - targetPitch) % 360.0D + 540.0D) % 360.0D - 180.0D;
        if (nPitch > 1.0D || nPitch < -1.0D) {
            double complimentSpeed = nPitch * (ThreadLocalRandom.current().nextDouble(complimentPitch - 1.47328, complimentPitch + 2.48293) / 100);
            float val = (float) (-(complimentSpeed + (nPitch / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedPitch - 4.723847, speedPitch)))));
            val -= val % gcd;
            mc.thePlayer.rotationPitch += val;

            if (mc.thePlayer.rotationPitch > 90.0F) mc.thePlayer.rotationPitch = 90.0F;
            if (mc.thePlayer.rotationPitch < -90.0F) mc.thePlayer.rotationPitch = -90.0F;
        }
    }

    private float[] getRotationsToTarget(double deltaX, double targetY, double deltaZ) {
        float yaw = (float) Math.toDegrees(-Math.atan(deltaX / deltaZ));
        if (deltaX < 0 && deltaZ < 0) yaw = 90 + (float) Math.toDegrees(Math.atan(deltaZ / deltaX));
        else if (deltaX > 0 && deltaZ < 0) yaw = -90 + (float) Math.toDegrees(Math.atan(deltaZ / deltaX));
        return new float[] { yaw, 0.0f };
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) { }
}
