package com.monolith.monolith.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Random;

public class RightClicker {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static double minCPS = 12;
    public static double maxCPS = 16;
    public static double jitter = 0.0;
    public static double startDelay = 85.0; // ms

    // NOTE: In the original logic, "True" meant "Check Enabled" -> "Block Item".
    // So if allowBow is true, it checks for bow and blocks it.
    // To fix the confusion, we keep the logic but you should toggle these OFF to use the items.
    public static boolean onlyBlocks = false;
    public static boolean noSword = true;
    public static boolean ignoreRods = true;
    public static boolean allowEat = true;
    public static boolean allowBow = true;

    // --- INTERNAL VARIABLES ---
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random rand = new Random();

    private long righti;
    private long rightj;
    private long rightk;
    private long rightl;
    private double rightm;
    private boolean rightn;

    private boolean rightClickWaiting;
    private double rightClickWaitStartTime;
    private boolean allowedClick;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        // Run on Start of Render Frame (like Raven)
        if (event.phase != TickEvent.Phase.START) return;

        if (!enabled || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return; // Don't click in menus

        ravenClick();
    }

    private void ravenClick() {
        if (Mouse.isButtonDown(1)) {
            this.rightClickExecute(mc.gameSettings.keyBindUseItem.getKeyCode());
        } else {
            this.rightClickWaiting = false;
            this.allowedClick = false;
            this.righti = 0L;
            this.rightj = 0L;
        }
    }

    public void rightClickExecute(int key) {
        if (!this.rightClickAllowed())
            return;

        // --- JITTER LOGIC ---
        if (jitter > 0.0D) {
            double jitterMultiplier = jitter * 0.45D;
            if (rand.nextBoolean()) {
                mc.thePlayer.rotationYaw += rand.nextFloat() * jitterMultiplier;
            } else {
                mc.thePlayer.rotationYaw -= rand.nextFloat() * jitterMultiplier;
            }

            if (rand.nextBoolean()) {
                mc.thePlayer.rotationPitch += rand.nextFloat() * jitterMultiplier * 0.45D;
            } else {
                mc.thePlayer.rotationPitch -= rand.nextFloat() * jitterMultiplier * 0.45D;
            }
        }

        // --- CLICK TIMING LOGIC ---
        if (this.rightj > 0L && this.righti > 0L) {
            if (System.currentTimeMillis() > this.rightj) {
                // Press
                KeyBinding.setKeyBindState(key, true);
                KeyBinding.onTick(key);
                this.genRightTimings();
            } else if (System.currentTimeMillis() > this.righti) {
                // Release
                KeyBinding.setKeyBindState(key, false);
            }
        } else {
            this.genRightTimings();
        }
    }

    public void genRightTimings() {
        // Exact Raven Math
        double clickSpeed = minCPS + (maxCPS - minCPS) * rand.nextDouble() + 0.4D * rand.nextDouble();
        long delay = (int) Math.round(1000.0D / clickSpeed);

        if (System.currentTimeMillis() > this.rightk) {
            if (!this.rightn && rand.nextInt(100) >= 85) {
                this.rightn = true;
                this.rightm = 1.1D + rand.nextDouble() * 0.15D;
            } else {
                this.rightn = false;
            }

            this.rightk = System.currentTimeMillis() + 500L + (long) rand.nextInt(1500);
        }

        if (this.rightn) {
            delay = (long) ((double) delay * this.rightm);
        }

        if (System.currentTimeMillis() > this.rightl) {
            if (rand.nextInt(100) >= 80) {
                delay += 50L + (long) rand.nextInt(100);
            }

            this.rightl = System.currentTimeMillis() + 500L + (long) rand.nextInt(1500);
        }

        this.rightj = System.currentTimeMillis() + delay;
        this.righti = System.currentTimeMillis() + delay / 2L - (long) rand.nextInt(10);
    }

    public boolean rightClickAllowed() {
        ItemStack item = mc.thePlayer.getHeldItem();

        if (item != null) {
            // In Raven logic, if the setting is ON, it returns FALSE (Blocking the click)
            if (allowEat && (item.getItem() instanceof ItemFood || item.getItem() instanceof ItemPotion || item.getItem() instanceof ItemBucketMilk)) {
                return false;
            }

            if (ignoreRods && item.getItem() instanceof ItemFishingRod) {
                return false;
            }

            if (allowBow && item.getItem() instanceof ItemBow) {
                return false;
            }

            if (onlyBlocks && !(item.getItem() instanceof ItemBlock)) {
                return false;
            }

            if (noSword && item.getItem() instanceof ItemSword) {
                return false;
            }
        }

        // Start Delay Check
        if (startDelay != 0) {
            if (!rightClickWaiting && !allowedClick) {
                this.rightClickWaitStartTime = System.currentTimeMillis();
                this.rightClickWaiting = true;
                return false;
            } else if (this.rightClickWaiting && !allowedClick) {
                double passedTime = System.currentTimeMillis() - this.rightClickWaitStartTime;
                if (passedTime >= startDelay) {
                    this.allowedClick = true;
                    this.rightClickWaiting = false;
                    return true;
                } else {
                    return false;
                }
            }
        }

        return true;
    }
}
