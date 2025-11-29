package com.monolith.monolith.modules.combat;

import java.util.concurrent.ThreadLocalRandom;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemAxe;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

import com.google.common.collect.Multimap;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;

import com.monolith.monolith.gui.MonolithModGUI;

public class AimAssist {
    // --- SETTINGS ---
    public static boolean enabled = false;
    public static boolean autoWalk = false;
    public static boolean autoClick = false;

    public static double speedYaw = 45.0D, complimentYaw = 15.0D;
    public static double speedPitch = 45.0D, complimentPitch = 15.0D;
    public static double fov = 90.0D, distance = 4.5D, pitchOffSet = 4.0D;
    public static double walkFov = 20.0D;
    public static boolean clickAim = true, breakBlocks = true, weaponOnly = false, aimPitch = false;

    private Minecraft mc = Minecraft.getMinecraft();
    private boolean walkingState = false;
    private boolean aimAssistClicking = false;

    // --- HUMANIZATION VARIABLES ---
    private int strafeTimer = 0;
    private int strafeDirection = 0;
    private int hesitationTimer = 0;
    private int wTapTimer = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != Phase.END) return;
        try {
            boolean isGrinder = MonolithModGUI.AutoGrinder.enabled;

            // 1. Spawn Height Check
            if (isGrinder && mc.thePlayer.posY > 80.0) {
                resetMovement();
                if (aimAssistClicking) {
                    LeftClicker.triggerBot = false;
                    aimAssistClicking = false;
                }
                return;
            }

            // 2. Fall Distance Check (Prevent aiming while dropping)
            if (mc.thePlayer.fallDistance > 2.5F) {
                resetMovement();
                if (aimAssistClicking) {
                    LeftClicker.triggerBot = false;
                    aimAssistClicking = false;
                }
                return;
            }

            // Master Switch
            if (!enabled && !isGrinder) {
                resetMovement();
                if (aimAssistClicking) {
                    LeftClicker.triggerBot = false;
                    aimAssistClicking = false;
                }
                return;
            }

            // Inventory Check
            if (mc.currentScreen != null || mc.thePlayer == null || mc.theWorld == null) {
                resetMovement();
                if (aimAssistClicking) { LeftClicker.triggerBot = false; aimAssistClicking = false; }
                return;
            }

            // Settings Logic
            double useFov = isGrinder ? MonolithModGUI.AutoGrinder.fov : fov;
            double useDist = isGrinder ? MonolithModGUI.AutoGrinder.distance : distance;
            boolean useAutoWalk = isGrinder ? MonolithModGUI.AutoGrinder.autoWalk : autoWalk;
            boolean useAutoClick = isGrinder ? MonolithModGUI.AutoGrinder.autoClick : autoClick;
            boolean useAimPitch = isGrinder ? MonolithModGUI.AutoGrinder.aimPitch : aimPitch;
            boolean useWeaponOnly = isGrinder ? false : weaponOnly;

            // Break Blocks Check
            if (breakBlocks && mc.objectMouseOver != null) {
                BlockPos p = mc.objectMouseOver.getBlockPos();
                if (p != null) {
                    Block bl = mc.theWorld.getBlockState(p).getBlock();
                    if (bl != Blocks.air && !(bl instanceof BlockLiquid)) {
                        resetMovement();
                        if (aimAssistClicking) { LeftClicker.triggerBot = false; aimAssistClicking = false; }
                        return;
                    }
                }
            }

            if (useWeaponOnly && !isPlayerHoldingWeapon()) return;

            boolean isAutoClicking = LeftClicker.enabled && Mouse.isButtonDown(0);
            boolean shouldAim = isGrinder || (clickAim && isAutoClicking) || (Mouse.isButtonDown(0)) || !clickAim;

            Entity en = null;
            if (shouldAim) {
                en = getEnemy(useDist, useFov, isGrinder);

                if (en != null) {
                    // Auto Weapon
                    if (isGrinder && MonolithModGUI.AutoGrinder.autoWeapon) {
                        equipBestWeapon();
                    }

                    // --- GCD CALCULATOR ---
                    float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
                    float gcd = f * f * f * 1.2F;

                    // Aim Math (Yaw)
                    double n = fovFromEntity(en);
                    if (n > 1.0D || n < -1.0D) {
                        double complimentSpeed = n * (ThreadLocalRandom.current().nextDouble(complimentYaw - 1.47, complimentYaw + 2.48) / 100);
                        float val = (float) (-(complimentSpeed + (n / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedYaw - 4.72, speedYaw)))));

                        // Apply GCD Fix
                        val -= val % gcd;
                        mc.thePlayer.rotationYaw += val;
                    }

                    // Aim Math (Pitch)
                    if (useAimPitch) {
                        double nPitch = pitchFromEntity(en, (float) pitchOffSet);
                        double complimentSpeed = nPitch * (ThreadLocalRandom.current().nextDouble(complimentPitch - 1.47, complimentPitch + 2.48) / 100);
                        float val = (float) (-(complimentSpeed + (nPitch / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedPitch - 4.72, speedPitch)))));

                        // Apply GCD Fix
                        val -= val % gcd;
                        mc.thePlayer.rotationPitch += val;

                        if (mc.thePlayer.rotationPitch > 90.0F) mc.thePlayer.rotationPitch = 90.0F;
                        if (mc.thePlayer.rotationPitch < -90.0F) mc.thePlayer.rotationPitch = -90.0F;
                    }

                    // Auto Walk
                    boolean inTightCone = Math.abs(fovFromEntity(en)) <= walkFov;

                    if (useAutoWalk) {
                        if (inTightCone) {
                            walkingState = true;
                            boolean stopMovement = false;

                            if (mc.thePlayer.isCollidedHorizontally) {
                                if (mc.thePlayer.onGround) {
                                    mc.thePlayer.jump();
                                } else if (mc.thePlayer.motionY <= 0) {
                                    stopMovement = true;
                                }
                            }

                            if (stopMovement) {
                                // Release keys if stopped
                                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
                                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
                                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
                                if (isGrinder) {
                                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
                                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
                                }
                            } else {
                                // --- JUMP & SPRINT LOGIC ---
                                // If Grinder Mode is active AND Jump&Sprint setting is ON
                                if (isGrinder && MonolithModGUI.AutoGrinder.jumpAndSprint) {
                                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);

                                    // Disable strafing when BHop/Sprint is active to maintain speed
                                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                                } else {
                                    // STANDARD HUMANIZATION LOGIC
                                    boolean pressForward = true;

                                    if (hesitationTimer > 0) {
                                        pressForward = false;
                                        hesitationTimer--;
                                    } else if ((aimAssistClicking || Mouse.isButtonDown(0)) && mc.thePlayer.getDistanceToEntity(en) <= 3.5) {
                                        if (wTapTimer > 0) {
                                            wTapTimer--;
                                        } else {
                                            hesitationTimer = 2;
                                            pressForward = false;
                                            wTapTimer = 15 + ThreadLocalRandom.current().nextInt(15);
                                        }
                                    } else if (ThreadLocalRandom.current().nextInt(100) < 1) {
                                        hesitationTimer = 2 + ThreadLocalRandom.current().nextInt(5);
                                        pressForward = false;
                                    }

                                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), pressForward);

                                    // Strafe
                                    if (strafeTimer > 0) {
                                        strafeTimer--;
                                        if (strafeDirection == 1) {
                                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
                                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                                        } else if (strafeDirection == 2) {
                                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
                                        }
                                    } else {
                                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
                                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));

                                        if (ThreadLocalRandom.current().nextInt(100) < 2) {
                                            strafeTimer = 5 + ThreadLocalRandom.current().nextInt(11);
                                            strafeDirection = ThreadLocalRandom.current().nextBoolean() ? 1 : 2;
                                        }
                                    }
                                }
                                // ---------------------------
                            }

                        } else if (walkingState) {
                            resetMovement();
                        }
                    }

                    if (useAutoClick) {
                        if (inTightCone) {
                            // Only click if not falling excessively
                            if ((!mc.thePlayer.isCollidedHorizontally || mc.thePlayer.motionY > 0) && mc.thePlayer.fallDistance <= 2.5F) {
                                LeftClicker.triggerBot = true;
                                aimAssistClicking = true;
                            }
                        } else if (aimAssistClicking) {
                            LeftClicker.triggerBot = false;
                            aimAssistClicking = false;
                        }
                    }
                }
            }

            if (en == null || !shouldAim) {
                if (walkingState) resetMovement();
                if (aimAssistClicking) {
                    LeftClicker.triggerBot = false;
                    aimAssistClicking = false;
                }
            }

        } catch (Exception e) {}
    }

    private void resetMovement() {
        setKey(mc.gameSettings.keyBindForward.getKeyCode());
        setKey(mc.gameSettings.keyBindLeft.getKeyCode());
        setKey(mc.gameSettings.keyBindRight.getKeyCode());

        // Also reset Jump and Sprint if we forced them in Grinder mode
        if (MonolithModGUI.AutoGrinder.enabled) {
            setKey(mc.gameSettings.keyBindSprint.getKeyCode());
            setKey(mc.gameSettings.keyBindJump.getKeyCode());
        }

        walkingState = false;
        strafeTimer = 0;
        hesitationTimer = 0;
        wTapTimer = 0;
    }

    private void setKey(int key) {
        if (key > 0) KeyBinding.setKeyBindState(key, Keyboard.isKeyDown(key));
    }

    private void equipBestWeapon() {
        int bestSlot = -1;
        double maxDamage = -1;
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
        if (bestSlot != -1 && mc.thePlayer.inventory.currentItem != bestSlot) {
            mc.thePlayer.inventory.currentItem = bestSlot;
            mc.playerController.updateController();
        }
    }

    private double getSlotDamage(int slot) {
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if (stack == null) return 0;
        double damage = 0;
        Multimap<String, AttributeModifier> modifiers = stack.getAttributeModifiers();
        if (modifiers.containsKey(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName())) {
            for (AttributeModifier modifier : modifiers.get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName())) {
                damage += modifier.getAmount();
            }
        }
        damage += 1.0;
        if (EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) > 0) {
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25;
        }
        return damage;
    }

    public Entity getEnemy(double dist, double fov, boolean isGrinderMode) {
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity e : mc.theWorld.playerEntities) {
            if (e == mc.thePlayer) continue;
            if (!isValid(e, fov, isGrinderMode)) continue;
            double d = mc.thePlayer.getDistanceToEntity(e);
            if (d <= dist && d < closestDist) { closest = e; closestDist = d; }
        }
        return closest;
    }

    private boolean isValid(Entity e, double fovCheck, boolean isGrinderMode) {
        if (!(e instanceof EntityLivingBase)) return false;
        if (((EntityLivingBase) e).getHealth() <= 0) return false;
        if (e.isInvisible()) return false;
        if (!mc.thePlayer.canEntityBeSeen(e)) return false;
        if (Math.abs(fovFromEntity(e)) > fovCheck) return false;
        if (isGrinderMode && MonolithModGUI.AutoGrinder.ignoreDiamond) {
            if (e instanceof EntityPlayer && hasDiamondArmor((EntityPlayer) e)) return false;
        }
        return true;
    }

    private boolean hasDiamondArmor(EntityPlayer player) {
        if (player.inventory.armorInventory == null) return false;
        for (ItemStack stack : player.inventory.armorInventory) {
            if (stack != null && stack.getItem() != null) {
                if (stack.getItem().getUnlocalizedName().toLowerCase().contains("diamond")) return true;
            }
        }
        return false;
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
