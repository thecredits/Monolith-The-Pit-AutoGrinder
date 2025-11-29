package com.monolith.monolith.modules.misc;

import net.minecraft.util.EnumChatFormatting;
import java.util.ArrayList;
import java.util.List;

public class SocialManager {
    // Static lists
    public static List<String> friends = new ArrayList<>();
    public static List<String> kos = new ArrayList<>();

    // NEW SETTING
    public static boolean kosAlerts = true;

    public static void addFriend(String name) {
        String lower = name.toLowerCase();
        if (kos.contains(lower)) kos.remove(lower);
        if (!friends.contains(lower)) friends.add(lower);
    }

    public static void addKOS(String name) {
        String lower = name.toLowerCase();
        if (friends.contains(lower)) friends.remove(lower);
        if (!kos.contains(lower)) kos.add(lower);
    }

    public static void remove(String name) {
        String lower = name.toLowerCase();
        friends.remove(lower);
        kos.remove(lower);
    }

    public static boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }

    public static boolean isKOS(String name) {
        return kos.contains(name.toLowerCase());
    }

    public static String getRelationColor(String name) {
        if (isFriend(name)) return EnumChatFormatting.GREEN.toString(); // UPDATED TO GREEN
        if (isKOS(name)) return EnumChatFormatting.RED.toString();
        return EnumChatFormatting.WHITE.toString();
    }
}
