package com.example.examplemod;

import java.util.concurrent.ThreadLocalRandom;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoGrind {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static double autoDisableMinutes = 0; // 0 = Disabled (Infinite)

    public static double targetX = 0;
    public static double targetY = 0;
    public static double targetZ = 0;

    // You can set this to 12.0 or higher in your settings GUI
    public static double radius = 3.0;

    // This will be the maximum wait time. The bot picks a random time between 1.0s and this value.
    public static double spawnDelay = 2.0;

    public static double chaseRange = 10.0;
    public static double stopRange = 3.0;

    // --- INTERNAL STATE ---
    private Minecraft mc = Minecraft.getMinecraft();
    private boolean isWalkingBack = false;
    private boolean pausedBySpawn = false;
    private boolean storedClickerState = false;
    private long spawnDelayEnd = 0;
    private boolean awaitingDelay = false;

    // Tracks if player has touched ground after spawning
    private boolean hasLanded = true;

    // Timer State
    private long sessionStartTime = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 1. RESET TIMER IF DISABLED
        if (!enabled) {
            sessionStartTime = 0;
            if (LeftClicker.autoGrindActive) LeftClicker.autoGrindActive = false;
            return;
        }

        // 2. CHECK TIME LIMIT
        if (autoDisableMinutes > 0) {
            if (sessionStartTime == 0) sessionStartTime = System.currentTimeMillis();

            long limitMillis = (long)(autoDisableMinutes * 60 * 1000);
            if (System.currentTimeMillis() - sessionStartTime > limitMillis) {
                enabled = false;
                LeftClicker.enabled = false;
                stopMovement();

                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[MeloMod] " + EnumChatFormatting.GRAY + "AutoGrind stopped (Time Limit Reached)."));
                }
                sessionStartTime = 0;
                return;
            }
        } else {
            sessionStartTime = 0;
        }

        if (mc.thePlayer == null || mc.theWorld == null) return;

        // 3. GUI SAFETY
        if (mc.currentScreen != null) {
            stopMovement();
            LeftClicker.autoGrindActive = false;
            return;
        }

        // 4. SPAWN DETECTION (Y > 100)
        boolean inSpawn = mc.thePlayer.posY > 100;

        if (inSpawn) {
            hasLanded = false; // We are in spawn, so we haven't landed on the ground below yet

            if (!pausedBySpawn) {
                storedClickerState = LeftClicker.enabled;
                LeftClicker.enabled = false;
                LeftClicker.autoGrindActive = false;
                pausedBySpawn = true;

                // --- RANDOM DELAY CALCULATION ---
                // Generates a random delay between 1.0 second and the 'spawnDelay' setting.
                double minDelay = 1.0;
                double maxDelay = Math.max(minDelay, spawnDelay);
                double randomSeconds = minDelay + (maxDelay - minDelay) * ThreadLocalRandom.current().nextDouble();

                spawnDelayEnd = System.currentTimeMillis() + (long)(randomSeconds * 1000);
                awaitingDelay = true;
            }

            // If we are waiting for the random timer, STOP moving and RETURN.
            if (awaitingDelay) {
                if (System.currentTimeMillis() < spawnDelayEnd) {
                    stopMovement();
                    return;
                } else {
                    awaitingDelay = false; // Timer finished, allow walking back
                }
            }

            // If code reaches here, it means waiting is done, so it will fall through to Step 6 (Walk to Center)

        } else {
            // NOT IN SPAWN (Y <= 100)

            // Re-enable clicker if it was paused
            if (pausedBySpawn) {
                LeftClicker.enabled = storedClickerState;
                pausedBySpawn = false;
                awaitingDelay = false;
            }

            // Check if we have hit the ground yet
            if (!hasLanded) {
                if (mc.thePlayer.onGround) {
                    hasLanded = true; // We hit the ground, enable combat
                }
            }
        }

        // 5. PRIORITY 1: GUARD THE CIRCLE
        // Only fight if:
        // 1. We are NOT in spawn
        // 2. We HAVE landed on the ground (prevents aiming while falling)
        if (!inSpawn && hasLanded) {
            Entity enemy = getEnemy();

            if (enemy != null) {
                isWalkingBack = false;
                faceEntity(enemy);

                double distToEnemy = mc.thePlayer.getDistanceToEntity(enemy);

                if (Math.abs(fovFromEntity(enemy)) < 30.0) {
                    if (distToEnemy < 4.5) LeftClicker.autoGrindActive = true;
                    else LeftClicker.autoGrindActive = false;
                } else {
                    LeftClicker.autoGrindActive = false;
                }

                boolean isMoving = mc.gameSettings.keyBindForward.isKeyDown();
                boolean shouldMove = isMoving ? (distToEnemy > 0.8) : (distToEnemy > 1.5);

                if (shouldMove) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                    if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(true);
                    }
                    if (mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                } else {
                    stopMovement();
                }
                return; // Stop here if fighting
            }
        }

        LeftClicker.autoGrindActive = false;

        // 6. PRIORITY 2: RETURN TO CENTER
        // This runs if:
        // - We are in Spawn (and delay finished) -> Walks off ledge
        // - We are Falling (hasLanded is false) -> Drifts towards center
        // - We are on ground but no enemy -> Returns to center
        double distToCenter = getDistanceToSpot3D();

        if (distToCenter > radius) {
            isWalkingBack = true;

            faceCenter();

            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isSprinting()) {
                mc.thePlayer.setSprinting(true);
            }
            if (mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }
        } else {
            if (isWalkingBack) {
                stopMovement();
                isWalkingBack = false;
            }
        }
    }

    // --- RENDER ESP ---
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled) return;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        double viewX = mc.getRenderManager().viewerPosX;
        double viewY = mc.getRenderManager().viewerPosY;
        double viewZ = mc.getRenderManager().viewerPosZ;

        GlStateManager.color(0.0F, 1.0F, 0.0F, 1.0F);
        GL11.glLineWidth(2.0F);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i += 5) {
            double x = targetX + Math.sin(Math.toRadians(i)) * radius;
            double z = targetZ + Math.cos(Math.toRadians(i)) * radius;
            GL11.glVertex3d(x - viewX, targetY - viewY + 0.1, z - viewZ);
        }
        GL11.glEnd();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // --- UTILS ---
    private Entity getEnemy() {
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity e : mc.theWorld.playerEntities) {
            if (e == mc.thePlayer) continue;
            if (!isValid(e)) continue;
            double distEnemyToCenter = getDist(e.posX, e.posZ, targetX, targetZ);
            if (distEnemyToCenter > radius) continue;
            double distPlayerToEnemy = mc.thePlayer.getDistanceToEntity(e);
            if (distPlayerToEnemy <= chaseRange && distPlayerToEnemy < closestDist) {
                closest = e; closestDist = distPlayerToEnemy;
            }
        }
        return closest;
    }

    private double getDist(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2; double dz = z1 - z2; return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean isValid(Entity e) {
        if (!(e instanceof EntityLivingBase)) return false;
        if (((EntityLivingBase) e).getHealth() <= 0) return false;
        if (e.isInvisible()) return false;
        if (Math.abs(fovFromEntity(e)) > AimAssist.fov) return false;
        return true;
    }

    private double fovFromEntity(Entity en) {
        return ((mc.thePlayer.rotationYaw - getRotations(en)[0]) % 360.0D + 540.0D) % 360.0D - 180.0D;
    }

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

    private void stopMovement() {
        if (mc.gameSettings.keyBindForward.isKeyDown())
             KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        if (mc.gameSettings.keyBindJump.isKeyDown())
             KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
        if (mc.thePlayer != null && mc.thePlayer.isSprinting()) {
            mc.thePlayer.setSprinting(false);
        }
    }

    private double getDistanceToSpot3D() {
        double dx = mc.thePlayer.posX - targetX;
        double dy = mc.thePlayer.posY - targetY;
        double dz = mc.thePlayer.posZ - targetZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // --- AIM ASSIST INTEGRATION ---
    private void faceEntity(Entity en) {
        double nYaw = fovFromEntity(en);
        applySmoothingYaw(nYaw);
        if (AimAssist.aimPitch) {
            double nPitch = mc.thePlayer.rotationPitch - (getRotations(en)[1] + AimAssist.pitchOffSet);
            applySmoothingPitch(nPitch);
        }
    }

    private void faceCenter() {
        double deltaX = targetX - mc.thePlayer.posX;
        double deltaZ = targetZ - mc.thePlayer.posZ;
        float targetYaw = (float) Math.toDegrees(-Math.atan(deltaX / deltaZ));
        if (deltaX < 0 && deltaZ < 0) targetYaw = 90 + (float) Math.toDegrees(Math.atan(deltaZ / deltaX));
        else if (deltaX > 0 && deltaZ < 0) targetYaw = -90 + (float) Math.toDegrees(Math.atan(deltaZ / deltaX));

        double nYaw = ((mc.thePlayer.rotationYaw - targetYaw) % 360.0D + 540.0D) % 360.0D - 180.0D;
        applySmoothingYaw(nYaw);

        // NO PITCH CHANGE HERE (Bot won't look at floor)
    }

    private void applySmoothingYaw(double n) {
        double speedYaw = AimAssist.speedYaw;
        double complimentYaw = AimAssist.complimentYaw;
        if (n > 1.0D || n < -1.0D) {
            double complimentSpeed = n * (ThreadLocalRandom.current().nextDouble(complimentYaw - 1.47328, complimentYaw + 2.48293) / 100);
            float val = (float) (-(complimentSpeed + (n / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedYaw - 4.723847, speedYaw)))));
            mc.thePlayer.rotationYaw += val;
        }
    }

    private void applySmoothingPitch(double n) {
        double speedPitch = AimAssist.speedPitch;
        double complimentPitch = AimAssist.complimentPitch;
        if (n > 1.0D || n < -1.0D) {
             double complimentSpeed = n * (ThreadLocalRandom.current().nextDouble(complimentPitch - 1.47328, complimentPitch + 2.48293) / 100);
             float val = (float) (-(complimentSpeed + (n / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedPitch - 4.723847, speedPitch)))));
             mc.thePlayer.rotationPitch += val;
        }
    }

    public static void setLocation() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            targetX = mc.thePlayer.posX;
            targetY = mc.thePlayer.posY;
            targetZ = mc.thePlayer.posZ;
        }
    }
}
