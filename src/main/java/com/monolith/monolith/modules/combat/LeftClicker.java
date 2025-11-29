package com.monolith.monolith.modules.combat;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Random;
import org.lwjgl.input.Mouse;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

// IMPORT GUI TO READ GRINDER SETTINGS
import com.monolith.monolith.gui.MonolithModGUI;

public class LeftClicker {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static boolean triggerBot = false;

    public static double minCPS = 9;
    public static double maxCPS = 13;
    public static double jitterLeft = 0.0;
    public static boolean weaponOnly = false;
    public static boolean breakBlocks = false;

    // --- INTERNAL VARIABLES ---
    private Minecraft mc = Minecraft.getMinecraft();
    private boolean leftDown;
    private long leftDownTime, leftUpTime, leftk, leftl;
    private double leftm;
    private boolean leftn, breakHeld;
    private Random rand = new Random();

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        // --- MODIFIED: Wake up if Clicker IS enabled OR if Grinder IS enabled ---
        boolean isGrinder = MonolithModGUI.AutoGrinder.enabled;

        if ((!enabled && !isGrinder) || ev.phase != Phase.END || mc.thePlayer == null) return;

        if (mc.currentScreen != null || !mc.inGameHasFocus) return;

        ravenClick(isGrinder);
    }

    private void ravenClick(boolean isGrinderMode) {
        Mouse.poll();

        // --- TRIGGERBOT LOGIC ---
        // In Grinder mode, AimAssist sets 'triggerBot' to true when locked on.
        // We rely on that.

        // 1. Safety Release
        // Stop if: Mouse Up AND Not Mid-Click AND TriggerBot Off
        if (!Mouse.isButtonDown(0) && !leftDown && !triggerBot) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            setMouseButtonState(0, false);
            return;
        }

        // 2. Execution
        // Click if: Mouse Down OR TriggerBot OR Mid-Click
        if (Mouse.isButtonDown(0) || triggerBot || leftDown) {

            // --- MODIFIED: WEAPON CHECK ---
            // If in Grinder Mode, we IGNORE the "Weapon Only" setting.
            // If in Combat Mode, we respect it.
            if (!isGrinderMode && weaponOnly && !isPlayerHoldingWeapon()) return;

            leftClickExecute(mc.gameSettings.keyBindAttack.getKeyCode());
        }
    }

    // --- EXACT SOURCE CODE MATH BELOW (UNTOUCHED) ---

    public void leftClickExecute(int key) {
        if (breakBlock()) return;
        if (jitterLeft > 0.0D) applyJitter();

        if (leftUpTime > 0L && leftDownTime > 0L) {
            if (System.currentTimeMillis() > leftUpTime && leftDown) {
                KeyBinding.setKeyBindState(key, true);
                KeyBinding.onTick(key);
                genLeftTimings();
                setMouseButtonState(0, true);
                leftDown = false;
            } else if (System.currentTimeMillis() > leftDownTime) {
                KeyBinding.setKeyBindState(key, false);
                leftDown = true;
                setMouseButtonState(0, false);
            }
        } else genLeftTimings();
    }

    public void genLeftTimings() {
        double clickSpeed = (minCPS + (maxCPS - minCPS) * rand.nextDouble()) + (0.4D * rand.nextDouble());
        long delay = (int) Math.round(1000.0D / clickSpeed);
        if (System.currentTimeMillis() > leftk) {
            if (!leftn && rand.nextInt(100) >= 85) { leftn = true; leftm = 1.1D + (rand.nextDouble() * 0.15D); }
            else leftn = false;
            leftk = System.currentTimeMillis() + 500L + rand.nextInt(1500);
        }
        if (leftn) delay = (long) (delay * leftm);
        if (System.currentTimeMillis() > leftl) {
            if (rand.nextInt(100) >= 80) delay += 50L + rand.nextInt(100);
            leftl = System.currentTimeMillis() + 500L + rand.nextInt(1500);
        }
        leftUpTime = System.currentTimeMillis() + delay;
        leftDownTime = (System.currentTimeMillis() + (delay / 2L)) - rand.nextInt(10);
    }

    private void applyJitter() {
        double a = jitterLeft * 0.45D;
        EntityPlayerSP p = mc.thePlayer;
        if (rand.nextBoolean()) p.rotationYaw += rand.nextFloat() * a; else p.rotationYaw -= rand.nextFloat() * a;
        if (rand.nextBoolean()) p.rotationPitch += rand.nextFloat() * a * 0.45D; else p.rotationPitch -= rand.nextFloat() * a * 0.45D;
    }

    public boolean isPlayerHoldingWeapon() {
        if (mc.thePlayer.getCurrentEquippedItem() == null) return false;
        String item = mc.thePlayer.getCurrentEquippedItem().getItem().getUnlocalizedName().toLowerCase();
        return item.contains("sword") || item.contains("axe");
    }

    public boolean breakBlock() {
        if (breakBlocks && mc.objectMouseOver != null) {
            BlockPos p = mc.objectMouseOver.getBlockPos();
            if (p != null) {
                Block bl = mc.theWorld.getBlockState(p).getBlock();
                if (bl != Blocks.air && !(bl instanceof BlockLiquid)) {
                    if (!breakHeld) {
                        int e = mc.gameSettings.keyBindAttack.getKeyCode();
                        KeyBinding.setKeyBindState(e, true);
                        KeyBinding.onTick(e);
                        breakHeld = true;
                    }
                    return true;
                }
                if (breakHeld) breakHeld = false;
            }
        }
        return false;
    }

    public static void setMouseButtonState(int mouseButton, boolean held) {
        try {
            Method m = Mouse.class.getDeclaredMethod("getReadBuffer", (Class<?>[]) null);
            m.setAccessible(true);
            ByteBuffer buf = (ByteBuffer) m.invoke(null, (Object[]) null);
            if (buf != null) {
                int orig = buf.position();
                buf.position(buf.limit() - 64);
                buf.put(mouseButton, (byte) (held ? 1 : 0));
                buf.position(orig);
            }
        } catch (Exception e) {}
    }
}
