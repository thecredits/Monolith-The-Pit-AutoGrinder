package com.monolith.monolith.modules.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent; // IMPORT THIS

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebhookAlerts {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static String webhookUrl = "";
    public static boolean alertPrestige = true;
    public static boolean alertBan = true;
    public static boolean alertLevelUp = true;
    public static boolean alertRoutine = true;
    public static boolean alertMention = false;

    // --- INTERNAL STATE ---
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static long lastRoutineTime = 0;
    private static long sessionStartTime = 0;

    private static String lastBracket = "[?]";
    private static int lastLevelNum = -1;
    private static double lastGold = -1;
    private static boolean hasInitialized = false;

    // --- 1. BAN / KICK DETECTION (NEW METHOD) ---
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (!enabled || !alertBan) return;

        // This event fires whenever you leave the server (kick, ban, or manual disconnect)
        // We cannot easily read the "reason" here without complex packet handling,
        // BUT we can detect that the connection closed unexpectedly.

        // If we are sending this, it's usually bad news unless the user pressed Disconnect.
        // We send a generic alert so you can check.

        sendEmbed("⚠️ Disconnected / Kicked",
            "The client disconnected from the server.\nCheck screen for Ban/Kick reason.",
            0xFF0000); // Red
    }

    // --- 2. CHAT MENTION ---
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!enabled || !alertMention || mc.thePlayer == null || event.type == 2) return;

        String msg = event.message.getUnformattedText();
        String myName = mc.thePlayer.getName();

        if (msg.toLowerCase().contains(myName.toLowerCase()) && !msg.startsWith(myName)) {
            sendEmbed("🔔 Mention Detected", "```" + msg + "```", 0xFFD700);
        }

        // Backup Ban Detection via Chat
        // Sometimes Hypixel puts the ban ID in chat right before kicking
        if (msg.contains("Ban ID:") || msg.contains("banned from the server")) {
             sendEmbed("🚨 BAN DETECTED (Chat)", "```" + msg + "```", 0xFF0000);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled || mc.thePlayer == null || mc.theWorld == null) return;
        if (webhookUrl.isEmpty() || !webhookUrl.startsWith("http")) return;

        if (sessionStartTime == 0) sessionStartTime = System.currentTimeMillis();

        parseScoreboard();

        if (hasInitialized && alertRoutine) {
            long now = System.currentTimeMillis();
            if (lastRoutineTime == 0) {
                sendEmbed("🟢 Session Started", "Monolith attached to player.", 0x9B59B6);
                lastRoutineTime = now;
            }
            else if (now - lastRoutineTime > 300000) {
                sendEmbed("📊 Routine Update", "Current stats report.", 0x9B59B6);
                lastRoutineTime = now;
            }
        }
    }

    // --- HELPER METHODS ---

    private static int countMystics() {
        if (mc.thePlayer == null) return 0;
        int count = 0;
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            if (stack != null && stack.hasDisplayName()) {
                String name = stack.getDisplayName().toLowerCase();
                if (name.contains("fresh") || name.contains("tier") || name.contains("sewer") || name.contains("dark")) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String getRuntime() {
        if (sessionStartTime == 0) return "0h 0m";
        long millis = System.currentTimeMillis() - sessionStartTime;
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return String.format("%dh %dm", hours, minutes);
    }

    private void parseScoreboard() {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        Scoreboard sb = mc.theWorld.getScoreboard();
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        if (obj == null) return;

        List<String> lines = getScoreboardLines(sb, obj);

        int currentLevelNum = -1;
        String currentBracket = "";
        double currentGold = -1;

        for (String line : lines) {
            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(line).trim();

            if (clean.contains("Gold")) {
                try {
                    String num = clean.replaceAll("[^0-9.]", "");
                    if (!num.isEmpty()) currentGold = Double.parseDouble(num);
                } catch (Exception ignored) {}
            }

            if (clean.contains("Level")) {
                Matcher m = Pattern.compile("(\\[.*?\\])").matcher(clean);
                if (m.find()) {
                    currentBracket = m.group(1);
                    String numStr = currentBracket.replaceAll("[^0-9]", "");
                    if(!numStr.isEmpty()) currentLevelNum = Integer.parseInt(numStr);
                }
            }
        }

        if (currentLevelNum != -1 && currentGold != -1) {
            if (!hasInitialized) {
                hasInitialized = true;
                lastLevelNum = currentLevelNum;
                lastBracket = currentBracket;
                lastGold = currentGold;
                mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[Monolith] Webhook linked to Scoreboard!"));
                return;
            }

            if (currentLevelNum > lastLevelNum) {
                if (alertLevelUp) {
                    sendEmbed("🎉 Level Up!",
                        "Progress: `" + lastBracket + "` ➡ `" + currentBracket + "`",
                        0x2ECC71);
                }
                lastLevelNum = currentLevelNum;
                lastBracket = currentBracket;
            }
            lastGold = currentGold;
        }
    }

    private List<String> getScoreboardLines(Scoreboard sb, ScoreObjective obj) {
        List<String> lines = new ArrayList<>();
        Collection<Score> scores = sb.getSortedScores(obj);
        for (Score score : scores) {
            ScorePlayerTeam team = sb.getPlayersTeam(score.getPlayerName());
            String text = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            lines.add(text);
        }
        return lines;
    }

    public static void sendEmbed(String title, String description, int color) {
        if (webhookUrl.isEmpty()) return;

        new Thread(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setDoOutput(true);

                JsonObject root = new JsonObject();
                if (mc.thePlayer != null) {
                    root.addProperty("username", "Monolith - " + mc.thePlayer.getName());
                    root.addProperty("avatar_url", "https://cravatar.eu/helmavatar/" + mc.thePlayer.getName() + "/64.png");
                } else {
                    root.addProperty("username", "Monolith Client");
                }

                JsonObject embed = new JsonObject();
                embed.addProperty("title", title);
                embed.addProperty("description", description);
                embed.addProperty("color", color);

                JsonObject footer = new JsonObject();
                footer.addProperty("text", "Monolith Client • 1.8.9 Forge");
                embed.add("footer", footer);

                if (mc.thePlayer != null) {
                    JsonObject thumb = new JsonObject();
                    thumb.addProperty("url", "https://cravatar.eu/helmavatar/" + mc.thePlayer.getName() + "/128.png");
                    embed.add("thumbnail", thumb);
                }

                if (title.contains("Routine") || title.contains("Session")) {
                    JsonArray fields = new JsonArray();
                    fields.add(createField("💰 Gold", String.format("%,.0f g", lastGold), true));
                    fields.add(createField("⭐ Prestige/Level", lastBracket, true));
                    fields.add(createField("🔮 Mystics in Inv", String.valueOf(countMystics()), true));
                    fields.add(createField("⏱ Runtime", getRuntime(), true));
                    embed.add("fields", fields);
                }

                JsonArray embeds = new JsonArray();
                embeds.add(embed);
                root.add("embeds", embeds);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = root.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                conn.getInputStream().close();
            } catch (Exception e) {
                System.err.println("Failed to send webhook: " + e.getMessage());
            }
        }).start();
    }

    private static JsonObject createField(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", inline);
        return field;
    }

    public static void sendTest() {
        sendEmbed("💜 Monolith Test", "Webhook system is online.", 0x9B59B6);
    }
}
