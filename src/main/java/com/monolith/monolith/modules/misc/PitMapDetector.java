package com.monolith.monolith.modules.misc;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

// IMPORTS
import com.monolith.monolith.modules.movement.LegitPathing;

public class PitMapDetector {

    public static String currentMap = "Scanning...";
    public static BlockPos centerPos = null;
    public static boolean isCustomMid = false;

    private Minecraft mc = Minecraft.getMinecraft();
    private int tickTimer = 0;

    // --- BUTTON ACTIONS ---
    public static void setCustomMid() {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        BlockPos p = Minecraft.getMinecraft().thePlayer.getPosition();
        centerPos = p;
        isCustomMid = true;
        LegitPathing.setTarget(centerPos);
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(
            EnumChatFormatting.GREEN + "[Monolith] Custom Mid set to: " + p.getX() + ", " + p.getY() + ", " + p.getZ()));
    }

    public static void rescanMap() {
        isCustomMid = false;
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(
            EnumChatFormatting.YELLOW + "[Monolith] Rescanning Map for defaults..."));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (tickTimer > 0) {
            tickTimer--;
            return;
        }
        tickTimer = 40;

        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (isCustomMid) {
            if (centerPos != null) LegitPathing.setTarget(centerPos);
            return;
        }

        detectMapAndSetCenter();

        if (centerPos != null) {
            LegitPathing.setTarget(centerPos);
        }
    }

    private void detectMapAndSetCenter() {
        Block surfaceBlock = Blocks.air;
        int floorY = 80; // Default lower fallback

        // Scan from sky (120) down to (40)
        // We scan deeper (down to 40) to ensure we find the real floor on low maps
        for (int y = 120; y > 40; y--) {
            BlockPos pos = new BlockPos(0, y, 0);
            IBlockState state = mc.theWorld.getBlockState(pos);
            Block b = state.getBlock();

            // IGNORE LIST: Air, Barriers, Decoration, Water (Wait, Corals mid is water?)
            // Note: We want to stop at Water for Corals, but ignore Barriers in sky.

            boolean isIgnore = b == Blocks.air ||
                               b == Blocks.barrier ||
                               b == Blocks.tallgrass ||
                               b == Blocks.flower_pot ||
                               b == Blocks.leaves ||
                               b == Blocks.leaves2;

            if (!isIgnore) {
                surfaceBlock = b;
                floorY = y;
                break; // Found the floor!
            }
        }

        // Set Y to floorY (the block itself) or floorY + 1 (standing on it)
        // MidESP draws AT the coordinate, so we probably want the top of the block.
        centerPos = new BlockPos(0, floorY + 1, 0);

        String blockName = surfaceBlock.getUnlocalizedName().toLowerCase();

        if (blockName.contains("prismarine") || blockName.contains("water")) {
            currentMap = "Corals";
        }
        else if (blockName.contains("glass") || blockName.contains("quartz") || blockName.contains("stained")) {
            currentMap = "Genesis";
        }
        else if (blockName.contains("stonebrick") || blockName.contains("cobble")) {
            currentMap = "Castle";
        }
        else if (blockName.contains("grass") || blockName.contains("dirt")) {
            currentMap = "Seasons";
        }
        else {
            currentMap = "Unknown";
        }
    }
}
