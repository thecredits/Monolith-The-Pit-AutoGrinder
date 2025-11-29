package com.monolith.monolith.modules.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.net.ssl.*;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PitEventManager {

    public static boolean enabled = true;

    // Position on screen
    public static int x = 5;
    public static int y = 120;

    // 1. Raw GitHub File (The one you saw on GitHub)
    // Note: We use 'raw.githubusercontent.com' to get the pure JSON data.
    private static final String BROOKE_GITHUB_URL = "https://raw.githubusercontent.com/brooke-gill/pit/main/events.json";

    // 2. The API Brooke's website actually calls (Backup)
    private static final String BROOKE_API_SOURCE = "https://events.mcpqndq.dev/";

    private List<PitEventData> events = new ArrayList<>();
    private long lastFetchTime = 0;
    private boolean isLoading = true;
    private String statusMessage = "Initializing...";

    private final long FETCH_DELAY = 60000; // Fetch every 60s

    public PitEventManager() {
        disableSSLVerification(); // Fix for Java 8 / Let's Encrypt
        fetchEventsAsync();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (System.currentTimeMillis() - lastFetchTime > FETCH_DELAY) {
            fetchEventsAsync();
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        List<String> linesToDraw = new ArrayList<>();

        if (isLoading) {
            linesToDraw.add(EnumChatFormatting.GRAY + statusMessage);
        } else if (events.isEmpty()) {
            linesToDraw.add(EnumChatFormatting.RED + "No Events Found");
            linesToDraw.add(EnumChatFormatting.DARK_GRAY + statusMessage);
        } else {
            linesToDraw.add(EnumChatFormatting.GOLD + "Pit Events");

            int count = 0;
            long now = System.currentTimeMillis();

            for (PitEventData e : events) {
                if (count >= 5) break; // Show top 5

                String timeString;
                String colorPrefix;

                // Active Event
                if (now >= e.start && now <= e.end) {
                    long timeLeft = e.end - now;
                    timeString = EnumChatFormatting.GREEN + "Active: " + formatDuration(timeLeft);
                    colorPrefix = EnumChatFormatting.GREEN.toString();
                    if (e.isMajor) colorPrefix = EnumChatFormatting.LIGHT_PURPLE.toString();
                }
                // Future Event
                else {
                    long timeUntil = e.start - now;
                    if (timeUntil < -60000) continue; // Skip old events

                    if (timeUntil < 180000) colorPrefix = EnumChatFormatting.YELLOW.toString();
                    else colorPrefix = EnumChatFormatting.GRAY.toString();

                    if (e.isMajor) colorPrefix = EnumChatFormatting.LIGHT_PURPLE.toString();

                    if (timeUntil < 0) timeString = "Starting...";
                    else timeString = formatDuration(timeUntil);
                }

                linesToDraw.add(colorPrefix + e.name + EnumChatFormatting.WHITE + " - " + timeString);
                count++;
            }
        }

        if (linesToDraw.isEmpty()) return;

        int maxWidth = 100;
        for (String s : linesToDraw) {
            int w = fr.getStringWidth(s);
            if (w > maxWidth) maxWidth = w;
        }

        int totalHeight = (linesToDraw.size() * 10) + 4;
        Gui.drawRect(x - 2, y - 2, x + maxWidth + 4, y + totalHeight, 0x90000000);

        int currentY = y;
        for (String line : linesToDraw) {
            fr.drawStringWithShadow(line, x, currentY, 0xFFFFFFFF);
            currentY += 10;
        }
    }

    private void fetchEventsAsync() {
        lastFetchTime = System.currentTimeMillis();
        isLoading = true;
        statusMessage = "Loading...";

        new Thread(() -> {
            // 1. Try the GitHub Raw file first
            boolean success = tryFetch(BROOKE_GITHUB_URL);

            // 2. If that fails (or file is missing), try the API source she uses
            if (!success) {
                System.out.println("GitHub file failed, trying API source...");
                success = tryFetch(BROOKE_API_SOURCE);
            }

            if (!success) {
                statusMessage = "All Sources Failed";
            }
            isLoading = false;
        }).start();
    }

    private boolean tryFetch(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0"); // GitHub requires a User-Agent
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JsonElement root = new JsonParser().parse(reader);

                JsonArray eventsArray = null;

                // Handle the structure you provided: Top-level Array
                if (root.isJsonArray()) {
                    eventsArray = root.getAsJsonArray();
                } else if (root.isJsonObject()) {
                    // Handle older APIs that might wrap it in {"events": [...]}
                    JsonObject rootObj = root.getAsJsonObject();
                    if (rootObj.has("events")) eventsArray = rootObj.getAsJsonArray("events");
                    else if (rootObj.has("data")) eventsArray = rootObj.getAsJsonArray("data");
                }

                if (eventsArray == null) {
                    statusMessage = "Invalid JSON";
                    return false;
                }

                List<PitEventData> tempList = new ArrayList<>();
                long now = System.currentTimeMillis();

                for (JsonElement el : eventsArray) {
                    try {
                        JsonObject obj = el.getAsJsonObject();

                        // Parse based on your snippet: {"event":"Auction","timestamp":1764...,"type":"minor"}
                        String name = "Unknown";
                        if (obj.has("event")) name = obj.get("event").getAsString();

                        long start = 0;
                        if (obj.has("timestamp")) start = obj.get("timestamp").getAsLong();
                        else if (obj.has("start")) start = obj.get("start").getAsLong();

                        // Default end time (5 mins) since your snippet doesn't have it
                        long end = start + 300000;
                        if (obj.has("end")) end = obj.get("end").getAsLong();

                        boolean isMajor = false;
                        if (obj.has("type")) {
                            String type = obj.get("type").getAsString();
                            if ("major".equalsIgnoreCase(type)) isMajor = true;
                        }

                        // Fix milliseconds if needed (some APIs return seconds)
                        if (start < 10000000000L) start *= 1000;
                        if (end < 10000000000L) end *= 1000;

                        // Add if the event ends in the future (or ended < 1 min ago)
                        // And skip events too far in the future (e.g. > 12 hours) to save memory
                        if (end > now - 60000 && start < now + 43200000) {
                            tempList.add(new PitEventData(formatName(name), start, end, isMajor));
                        }

                    } catch (Exception ignored) {}
                }

                Collections.sort(tempList, (o1, o2) -> Long.compare(o1.start, o2.start));

                synchronized (this) {
                    this.events = tempList;
                }

                if (tempList.isEmpty()) {
                    statusMessage = "0 Upcoming Events";
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void disableSSLVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {}
    }

    private String formatName(String raw) {
        if (raw == null) return "Unknown";
        raw = raw.replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String s : raw.split(" ")) {
            if (s.length() > 0) {
                sb.append(Character.toUpperCase(s.charAt(0)));
                if (s.length() > 1) sb.append(s.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes >= 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return hours + "h " + minutes + "m";
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static class PitEventData {
        String name;
        long start;
        long end;
        boolean isMajor;
        public PitEventData(String name, long start, long end, boolean isMajor) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.isMajor = isMajor;
        }
    }
}
