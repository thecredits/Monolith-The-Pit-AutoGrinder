package com.monolith.monolith.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.ThreadLocalRandom;

// --- IMPORT GUI ---
import com.monolith.monolith.gui.MonolithModGUI;

public class AutoGHead {

    // --- SETTINGS (Public Static for GUI) ---
    public static boolean enabled = false;
    // Delay Range (ms)
    public static double minDelay = 50;
    public static double maxDelay = 100;
    // Cooldown Range (ms)
    public static double minCooldown = 1000;
    public static double maxCooldown = 1200;
    // Health Threshold
    public static double health = 14; // 7 hearts = 14 health points

    private static final Minecraft mc = Minecraft.getMinecraft();

    // --- LOGIC ---
    private final CoolDown cd = new CoolDown(1);
    private State state = State.WAITINGTOSWITCH;
    private int originalSlot;

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;

        // --- CHECK: Run if Global Enabled OR Grinder Specific Enabled ---
        boolean isGrinderActive = MonolithModGUI.AutoGrinder.enabled && MonolithModGUI.AutoGrinder.autoGHead;

        if (!enabled && !isGrinderActive) return;

        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Logic matched exactly to provided code
        if ((mc.thePlayer.getHealth() < health) && cd.hasFinished()) {
            switch(state) {
                case WAITINGTOSWITCH:
                    cd.setCooldown((long) (getRandom(minDelay, maxDelay) / 3));
                    break;
                case NONE:
                    int slot = getGHeadSlot();
                    if (slot == -1) return;
                    originalSlot = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.inventory.currentItem = slot;

                    cd.setCooldown((long) (getRandom(minDelay, maxDelay) / 3));
                    break;
                case SWITCHED:
                    KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());

                    cd.setCooldown((long) (getRandom(minDelay, maxDelay) / 3));
                    break;
                case SWITCHEDANDCLICKED:
                    mc.thePlayer.inventory.currentItem = originalSlot;

                    cd.setCooldown((long) getRandom(minCooldown, maxCooldown));
                    break;
            }
            state = state.next();
            cd.start();
        }
    }

    public int getGHeadSlot() {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack itemInSlot = mc.thePlayer.inventory.getStackInSlot(slot);
            if ((itemInSlot != null) && (itemInSlot.getItem() instanceof ItemSkull)
                    && (itemInSlot.getDisplayName().toLowerCase().contains("golden") && itemInSlot.getDisplayName().toLowerCase().contains("head")))
                return slot;
        }
        return -1;
    }

    private double getRandom(double min, double max) {
        if (min == max) return min;
        if (min > max) return min; // Safety
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public enum State {
        WAITINGTOSWITCH,
        NONE,
        SWITCHED,
        SWITCHEDANDCLICKED;

        private static State[] vals = values();
        public State next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    // Inner class to replicate KeystrokesMod CoolDown
    public static class CoolDown {
        private long start;
        private long length;

        public CoolDown(long length) {
            this.length = length;
        }

        public void start() {
            this.start = System.currentTimeMillis();
        }

        public void setCooldown(long length) {
            this.length = length;
        }

        public boolean hasFinished() {
            return System.currentTimeMillis() - start >= length;
        }
    }
}
