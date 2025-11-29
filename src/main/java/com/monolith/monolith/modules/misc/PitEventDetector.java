package com.monolith.monolith.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PitEventDetector {

    // --- LIVE EVENT STATUS (READ ONLY) ---
    public static String currentEventName = "None";
    public static boolean eventActive = false;

    // Specific Flags for Logic
    public static boolean isSpireActive = false;
    public static boolean isRageActive = false;
    public static boolean isTDMActive = false;
    public static boolean isRaffleActive = false;
    public static boolean isBeastActive = false;
    public static boolean isSquadsActive = false;
    public static boolean isBlockheadActive = false;
    public static boolean isRobberyActive = false;
    public static boolean isKOTHActive = false;

    // Timer to prevent scanning every single tick (Optimized)
    private int tickDelay = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        // Scan every 10 ticks (0.5 seconds) to save FPS
        if (tickDelay > 0) {
            tickDelay--;
            return;
        }
        tickDelay = 10;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        resetFlags();
        scanScoreboard();
    }

    private void resetFlags() {
        currentEventName = "None";
        eventActive = false;
        isSpireActive = false;
        isRageActive = false;
        isTDMActive = false;
        isRaffleActive = false;
        isBeastActive = false;
        isSquadsActive = false;
        isBlockheadActive = false;
        isRobberyActive = false;
        isKOTHActive = false;
    }

    private void scanScoreboard() {
        List<String> lines = getScoreboardLines();

        for (String line : lines) {
            // Strip colors to check plain text
            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(line).trim();

            // Detect The Spire
            if (clean.contains("The Spire") || (clean.contains("Spire") && clean.contains(":"))) {
                currentEventName = "The Spire";
                isSpireActive = true;
                eventActive = true;
            }
            // Detect Rage Pit
            else if (clean.contains("Rage Pit")) {
                currentEventName = "Rage Pit";
                isRageActive = true;
                eventActive = true;
            }
            // Detect TDM
            else if (clean.contains("Team Deathmatch") || clean.contains("TDM")) {
                currentEventName = "TDM";
                isTDMActive = true;
                eventActive = true;
            }
            // Detect Raffle
            else if (clean.contains("Raffle")) {
                currentEventName = "Raffle";
                isRaffleActive = true;
                eventActive = true;
            }
            // Detect The Beast
            else if (clean.contains("The Beast") || clean.contains("Beast")) {
                currentEventName = "The Beast";
                isBeastActive = true;
                eventActive = true;
            }
            // Detect Squads
            else if (clean.contains("Squads")) {
                currentEventName = "Squads";
                isSquadsActive = true;
                eventActive = true;
            }
            // Detect Blockhead
            else if (clean.contains("Blockhead")) {
                currentEventName = "Blockhead";
                isBlockheadActive = true;
                eventActive = true;
            }
            // Detect Robbery
            else if (clean.contains("Robbery")) {
                currentEventName = "Robbery";
                isRobberyActive = true;
                eventActive = true;
            }
            // Detect KOTH
            else if (clean.contains("KOTH") || clean.contains("King of the Hill")) {
                currentEventName = "KOTH";
                isKOTHActive = true;
                eventActive = true;
            }
        }
    }

    /**
     * Extracts the raw text lines from the Sidebar Scoreboard.
     * This is passive reading of client memory.
     */
    private List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<>();
        Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
        if (scoreboard == null) return lines;

        // Slot 1 is the Sidebar
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return lines;

        Collection<Score> scores = scoreboard.getSortedScores(objective);

        // Filter out irrelevant scores and sort
        List<Score> list = scores.stream()
                .filter(input -> input != null && input.getPlayerName() != null && !input.getPlayerName().startsWith("#"))
                .collect(Collectors.toList());

        // Scoreboard usually holds max 15 lines
        if (list.size() > 15) {
            scores = list.subList(list.size() - 15, list.size());
        } else {
            scores = list;
        }

        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            // Combine prefix + playername + suffix to get the full visible line
            String text = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            lines.add(text);
        }
        return lines;
    }
}
