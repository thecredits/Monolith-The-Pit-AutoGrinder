package com.monolith.monolith.gui;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Timer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

// --- MODULE IMPORTS ---
import com.monolith.monolith.modules.combat.LeftClicker;
import com.monolith.monolith.modules.combat.RightClicker;
import com.monolith.monolith.modules.combat.AimAssist;
import com.monolith.monolith.modules.combat.AntiBot;
import com.monolith.monolith.modules.combat.AutoGHead;
import com.monolith.monolith.modules.combat.AutoWeapon;

import com.monolith.monolith.modules.movement.LegitPathing;

import com.monolith.monolith.modules.render.ESP;
import com.monolith.monolith.modules.render.ItemESP;
import com.monolith.monolith.modules.render.Chams;
import com.monolith.monolith.modules.render.Fullbright;
import com.monolith.monolith.modules.render.NameHider;
import com.monolith.monolith.modules.render.Nametags;
import com.monolith.monolith.modules.render.Tracers;
import com.monolith.monolith.modules.render.ChestESP;
import com.monolith.monolith.modules.render.MidESP;
import com.monolith.monolith.modules.render.MiscBlockESP;

import com.monolith.monolith.modules.misc.MiddleClick;
import com.monolith.monolith.modules.misc.SocialManager;
import com.monolith.monolith.modules.misc.PitMapDetector;
import com.monolith.monolith.modules.misc.PitEventManager;
import com.monolith.monolith.modules.misc.GrinderTimer;
import com.monolith.monolith.modules.misc.LobbySniper;
import com.monolith.monolith.modules.misc.MysticPathing;
import com.monolith.monolith.modules.misc.WebhookAlerts;

// --- GUI IMPORTS ---
import com.monolith.monolith.gui.TargetHUD;
import com.monolith.monolith.gui.KosOverlay;
import com.monolith.monolith.gui.HudManager;
import com.monolith.monolith.gui.PitEventOverlay;

public class MonolithModGUI extends GuiScreen {

    // --- DIMENSIONS ---
    private final int pW = 460;
    private final int pH = 320;

    private static int pX = -1;
    private static int pY = -1;

    private final int SIDEBAR_WIDTH = 120;

    private static int currentTab = 0;
    private static int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScroll = false;

    public static boolean editHudMode = false;

    private GuiTextField socialField;
    private String socialMatch = "";

    // Lists for settings
    private static List<Dropdown> combatDrops = new ArrayList<>();
    private static List<Dropdown> grinderDrops = new ArrayList<>();
    private static List<Dropdown> clientDrops = new ArrayList<>();
    private static List<Dropdown> miscDrops = new ArrayList<>();
    private static List<Dropdown> renderDrops = new ArrayList<>();
    private static List<Dropdown> eventDrops = new ArrayList<>();

    private static Dropdown shortcutsDropdown;

    public static boolean hudFPS = true;
    public static boolean hudCPS = true;
    public static boolean blurGuiOnly = true;

    private static boolean hasLoadedConfig = false;

    private static BlurShader blurShader;
    private static final int COLOR_BG_TINT = 0xB5000000;
    private static final int COLOR_GLASS_FILL = 0x608000FF;
    private static final int COLOR_GLASS_BORDER = 0xAA9050FF;
    private static final int COLOR_GUI_OUTLINE = 0x30FFFFFF;
    private static final int COLOR_DISCORD = 0xFF5865F2;
    private final String DISCORD_LINK = "https://discord.gg/yourcodehere";

    @Override
    public void initGui() {
        pX = (this.width - pW) / 2;
        pY = (this.height - pH) / 2;

        if (OpenGlHelper.shadersSupported && blurShader == null) {
            try { blurShader = new BlurShader(); } catch (Exception e) { e.printStackTrace(); }
        }

        if (HudManager.INSTANCE.components.isEmpty()) {
            HudManager.INSTANCE.init();
        }

        if (!hasLoadedConfig) {
            ConfigManager.load();
            hasLoadedConfig = true;
        }

        this.socialField = new GuiTextField(1, this.fontRendererObj, 0, 0, 180, 20);
        this.socialField.setMaxStringLength(16);
        this.socialField.setFocused(false);

        if(combatDrops.isEmpty()) initCombat();
        if(grinderDrops.isEmpty()) initGrinder();
        if(clientDrops.isEmpty()) initClient();
        if(miscDrops.isEmpty()) initMisc();
        if(renderDrops.isEmpty()) initRender();
        if(eventDrops.isEmpty()) initEvents();

        updateDropdownPositions();
    }

    @Override
    public void onGuiClosed() {
        try {
            Timer timer = ReflectionHelper.getPrivateValue(Minecraft.class, mc, "field_71428_T", "timer");
            if (timer != null) timer.timerSpeed = 1.0F;
        } catch (Exception e) { e.printStackTrace(); }

        if (!editHudMode) {
            ConfigManager.save();
        }
        super.onGuiClosed();
    }

    private void updateDropdownPositions() {
        int contentX = pX + SIDEBAR_WIDTH + 15;
        updateListPos(combatDrops, contentX);
        updateListPos(grinderDrops, contentX);
        updateListPos(clientDrops, contentX);
        updateListPos(miscDrops, contentX);
        updateListPos(renderDrops, contentX);
        updateListPos(eventDrops, contentX);
    }

    private void updateListPos(List<Dropdown> list, int x) {
        for(Dropdown d : list) d.x = x;
    }

    // --- TABS ---

    private void initCombat() {
        Dropdown clicker = new Dropdown("Left Clicker");
        clicker.add(new CheckSetting("Enabled", () -> LeftClicker.enabled, v -> LeftClicker.enabled = v));
        clicker.add(new CheckSetting("TriggerBot", () -> LeftClicker.triggerBot, v -> LeftClicker.triggerBot = v));
        clicker.add(new CheckSetting("Weapon Only", () -> LeftClicker.weaponOnly, v -> LeftClicker.weaponOnly = v));
        clicker.add(new SliderSetting("Min CPS", () -> (double)LeftClicker.minCPS, v -> LeftClicker.minCPS = v.intValue(), 1, 20, true));
        clicker.add(new SliderSetting("Max CPS", () -> (double)LeftClicker.maxCPS, v -> LeftClicker.maxCPS = v.intValue(), 1, 20, true));
        combatDrops.add(clicker);

        Dropdown rc = new Dropdown("Right Clicker");
        rc.add(new CheckSetting("Enabled", () -> RightClicker.enabled, v -> RightClicker.enabled = v));
        rc.add(new SliderSetting("Min CPS", () -> RightClicker.minCPS, v -> RightClicker.minCPS = v, 1, 25, true));
        rc.add(new SliderSetting("Max CPS", () -> RightClicker.maxCPS, v -> RightClicker.maxCPS = v, 1, 25, true));
        rc.add(new SliderSetting("Start Delay", () -> RightClicker.startDelay, v -> RightClicker.startDelay = v, 0, 500, true));
        rc.add(new SliderSetting("Jitter", () -> RightClicker.jitter, v -> RightClicker.jitter = v, 0, 3, false));
        rc.add(new CheckSetting("Blocks Only", () -> RightClicker.onlyBlocks, v -> RightClicker.onlyBlocks = v));
        rc.add(new CheckSetting("No Swords", () -> RightClicker.noSword, v -> RightClicker.noSword = v));
        rc.add(new CheckSetting("Ignore Rods", () -> RightClicker.ignoreRods, v -> RightClicker.ignoreRods = v));
        rc.add(new CheckSetting("Disable Eat", () -> RightClicker.allowEat, v -> RightClicker.allowEat = v));
        rc.add(new CheckSetting("Disable Bow", () -> RightClicker.allowBow, v -> RightClicker.allowBow = v));
        combatDrops.add(rc);

        Dropdown aim = new Dropdown("Aim Assist");
        aim.add(new CheckSetting("Enabled", () -> AimAssist.enabled, v -> AimAssist.enabled = v));
        aim.add(new CheckSetting("Click Aim", () -> AimAssist.clickAim, v -> AimAssist.clickAim = v));
        aim.add(new SliderSetting("Speed H", () -> AimAssist.speedYaw, v -> AimAssist.speedYaw = v, 1, 100, false));
        aim.add(new SliderSetting("Speed V", () -> AimAssist.speedPitch, v -> AimAssist.speedPitch = v, 1, 100, false));
        combatDrops.add(aim);

        Dropdown head = new Dropdown("Auto GHead");
        head.add(new CheckSetting("Enabled", () -> AutoGHead.enabled, v -> AutoGHead.enabled = v));
        head.add(new SliderSetting("Health", () -> AutoGHead.health, v -> AutoGHead.health = v, 1, 20, false));
        head.add(new SliderSetting("Min Delay", () -> AutoGHead.minDelay, v -> AutoGHead.minDelay = v, 0, 200, true));
        combatDrops.add(head);

        Dropdown weapon = new Dropdown("Auto Weapon");
        weapon.add(new CheckSetting("Enabled", () -> AutoWeapon.enabled, v -> AutoWeapon.enabled = v));
        weapon.add(new CheckSetting("On Click Only", () -> AutoWeapon.onlyWhenHoldingDown, v -> AutoWeapon.onlyWhenHoldingDown = v));
        weapon.add(new CheckSetting("Revert Slot", () -> AutoWeapon.goBackToPrevSlot, v -> AutoWeapon.goBackToPrevSlot = v));
        combatDrops.add(weapon);
    }

    private void initGrinder() {
        Dropdown main = new Dropdown("Main");
        main.add(new CheckSetting("Enabled", () -> AutoGrinder.enabled, v -> AutoGrinder.enabled = v));
        grinderDrops.add(main);

        Dropdown timer = new Dropdown("Auto-Stop Timer");
        timer.add(new CheckSetting("Enable Timer", () -> GrinderTimer.enabled, v -> GrinderTimer.enabled = v));
        timer.add(new SliderSetting("Hours", () -> (double)GrinderTimer.hours, v -> GrinderTimer.hours = v.intValue(), 0, 12, true));
        timer.add(new SliderSetting("Minutes", () -> (double)GrinderTimer.minutes, v -> GrinderTimer.minutes = v.intValue(), 0, 59, true));
        timer.add(new SliderSetting("Seconds", () -> (double)GrinderTimer.seconds, v -> GrinderTimer.seconds = v.intValue(), 0, 59, true));
        grinderDrops.add(timer);

        Dropdown spawnLogic = new Dropdown("Spawn Logic");
        spawnLogic.add(new CheckSetting("Auto-Walk to Mid", () -> LegitPathing.enabled, v -> LegitPathing.enabled = v));
        spawnLogic.add(new CheckSetting("Random Start Delay", () -> AutoGrinder.useSpawnDelay, v -> AutoGrinder.useSpawnDelay = v));
        spawnLogic.add(new SliderSetting("Max Delay (s)", () -> AutoGrinder.spawnDelaySec, v -> AutoGrinder.spawnDelaySec = v, 1, 10, false));

        spawnLogic.add(new CheckSetting("Show Mid ESP", () -> MidESP.enabled, v -> MidESP.enabled = v));
        spawnLogic.add(new ButtonSetting("Set Custom Mid", PitMapDetector::setCustomMid));
        spawnLogic.add(new ButtonSetting("Reset to Map Mid", PitMapDetector::rescanMap));
        grinderDrops.add(spawnLogic);

        Dropdown bot = new Dropdown("Bot Logic");
        bot.add(new CheckSetting("Jump & Sprint", () -> AutoGrinder.jumpAndSprint, v -> AutoGrinder.jumpAndSprint = v));
        bot.add(new CheckSetting("Aim Pitch", () -> AutoGrinder.aimPitch, v -> AutoGrinder.aimPitch = v));
        bot.add(new CheckSetting("Auto Walk", () -> AutoGrinder.autoWalk, v -> AutoGrinder.autoWalk = v));
        bot.add(new CheckSetting("Auto Click", () -> AutoGrinder.autoClick, v -> AutoGrinder.autoClick = v));
        bot.add(new CheckSetting("Auto Weapon", () -> AutoGrinder.autoWeapon, v -> AutoGrinder.autoWeapon = v));
        bot.add(new CheckSetting("Auto GHead", () -> AutoGrinder.autoGHead, v -> AutoGrinder.autoGHead = v));
        grinderDrops.add(bot);

        // --- LOBBY SWAPPER ---
        Dropdown swapper = new Dropdown("Lobby Swapper");
        swapper.add(new CheckSetting("Enabled", () -> AutoGrinder.autoSwapEnabled, v -> AutoGrinder.autoSwapEnabled = v));
        swapper.add(new SliderSetting("Min Players", () -> AutoGrinder.autoSwapThreshold, v -> AutoGrinder.autoSwapThreshold = v, 1, 20, true));
        grinderDrops.add(swapper);

        Dropdown targeting = new Dropdown("Targeting");
        targeting.add(new CheckSetting("Ignore Diamond Armor", () -> AutoGrinder.ignoreDiamond, v -> AutoGrinder.ignoreDiamond = v));
        targeting.add(new CheckSetting("Ignore Friends", () -> AutoGrinder.ignoreFriends, v -> AutoGrinder.ignoreFriends = v));
        targeting.add(new SliderSetting("FOV", () -> AutoGrinder.fov, v -> AutoGrinder.fov = v, 10, 360, false));
        targeting.add(new SliderSetting("Distance", () -> AutoGrinder.distance, v -> AutoGrinder.distance = v, 1, 8, false));
        grinderDrops.add(targeting);

        Dropdown progression = new Dropdown("Progression");
        progression.add(new CheckSetting("Auto Prestige", () -> AutoGrinder.autoPrestige, v -> AutoGrinder.autoPrestige = v));
        progression.add(new CheckSetting("Auto Perk", () -> AutoGrinder.autoPerk, v -> AutoGrinder.autoPerk = v));
        progression.add(new SliderSetting("Perk 1 ID", () -> AutoGrinder.perkSlot1, v -> AutoGrinder.perkSlot1 = v, 0, 25, true));
        progression.add(new SliderSetting("Perk 2 ID", () -> AutoGrinder.perkSlot2, v -> AutoGrinder.perkSlot2 = v, 0, 25, true));
        progression.add(new SliderSetting("Perk 3 ID", () -> AutoGrinder.perkSlot3, v -> AutoGrinder.perkSlot3 = v, 0, 25, true));
        grinderDrops.add(progression);

        Dropdown events = new Dropdown("Events");
        events.add(new CheckSetting("Don't Do Events", () -> AutoGrinder.ignoreAllEvents, v -> AutoGrinder.ignoreAllEvents = v));
        events.add(new CheckSetting("Do Spire", () -> AutoGrinder.doSpire, v -> AutoGrinder.doSpire = v));
        events.add(new CheckSetting("Do Rage", () -> AutoGrinder.doRage, v -> AutoGrinder.doRage = v));
        grinderDrops.add(events);

        // --- WEBHOOK ALERT SECTION ---
        Dropdown webhook = new Dropdown("Webhook Alerts");
        webhook.add(new CheckSetting("Enabled", () -> WebhookAlerts.enabled, v -> WebhookAlerts.enabled = v));
        webhook.add(new TextFieldSetting("Webhook URL:", () -> WebhookAlerts.webhookUrl, v -> WebhookAlerts.webhookUrl = v));

        // --- TEST BUTTON ---
        webhook.add(new ButtonSetting(EnumChatFormatting.BLUE + "Test Webhook", () -> {
            if (WebhookAlerts.webhookUrl.isEmpty()) {
                mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[Monolith] Error: No Webhook URL set!"));
            } else {
                mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[Monolith] Sending test embed... Check Discord!"));
                WebhookAlerts.sendTest(); // Calls new embed method
            }
        }));

        // --- NEW ALERT MENTIONS ---
        webhook.add(new CheckSetting("Alert Mentions", () -> WebhookAlerts.alertMention, v -> WebhookAlerts.alertMention = v));
        // --------------------------

        webhook.add(new CheckSetting("Alert Prestige", () -> WebhookAlerts.alertPrestige, v -> WebhookAlerts.alertPrestige = v));
        webhook.add(new CheckSetting("Alert Bans", () -> WebhookAlerts.alertBan, v -> WebhookAlerts.alertBan = v));
        webhook.add(new CheckSetting("Alert Level Up", () -> WebhookAlerts.alertLevelUp, v -> WebhookAlerts.alertLevelUp = v));
        webhook.add(new CheckSetting("Routine Stats (5m)", () -> WebhookAlerts.alertRoutine, v -> WebhookAlerts.alertRoutine = v));
        grinderDrops.add(webhook);
    }

    private void initClient() {
        Dropdown hud = new Dropdown("HUD");
        hud.add(new CheckSetting("FPS Display", () -> hudFPS, v -> hudFPS = v));
        hud.add(new CheckSetting("CPS Display", () -> hudCPS, v -> hudCPS = v));
        hud.add(new CheckSetting("Target HUD", () -> TargetHUD.enabled, v -> TargetHUD.enabled = v));
        hud.add(new CheckSetting("KOS Overlay", () -> KosOverlay.enabled, v -> KosOverlay.enabled = v));
        hud.add(new CheckSetting("Event Overlay", () -> PitEventOverlay.enabled, v -> PitEventOverlay.enabled = v));
        hud.add(new CheckSetting("Pit Events (Old)", () -> PitEventManager.enabled, v -> PitEventManager.enabled = v));

        hud.add(new CheckSetting("EDIT HUD POSITIONS", () -> editHudMode, v -> editHudMode = v));
        clientDrops.add(hud);

        Dropdown visuals = new Dropdown("Visuals");
        visuals.add(new CheckSetting("Blur GUI Only", () -> blurGuiOnly, v -> blurGuiOnly = v));
        clientDrops.add(visuals);

        Dropdown cf = new Dropdown("Configuration");
        cf.add(new ButtonSetting("Save Config", () -> {
            ConfigManager.save();
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[Monolith] Config Saved."));
        }));
        cf.add(new ButtonSetting("Load Config", () -> {
            ConfigManager.load();
            rebuildShortcuts();
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[Monolith] Config Loaded."));
        }));
        cf.add(new ButtonSetting(EnumChatFormatting.RED + "Reset to Defaults", () -> {
            ConfigManager.resetDefaults();
            rebuildShortcuts();
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[Monolith] Settings Reset."));
        }));
        clientDrops.add(cf);
    }

    private void initMisc() {
        Dropdown antiBot = new Dropdown("Anti-Bot");
        antiBot.add(new CheckSetting("Enable AntiBot", () -> AntiBot.enabled, v -> AntiBot.enabled = v));
        miscDrops.add(antiBot);

        Dropdown sniper = new Dropdown("Lobby Sniper");
        sniper.add(new CheckSetting("Enabled", () -> LobbySniper.enabled, v -> LobbySniper.enabled = v));
        sniper.add(new TextFieldSetting("Target IGN:", () -> LobbySniper.targetName, v -> LobbySniper.targetName = v));
        miscDrops.add(sniper);

        // --- MYSTIC PATHING ---
        Dropdown mysticPath = new Dropdown("Mystic Pathing");
        mysticPath.add(new CheckSetting("Enabled", () -> MysticPathing.enabled, v -> MysticPathing.enabled = v));
        mysticPath.add(new SliderSetting("Range", () -> MysticPathing.range, v -> MysticPathing.range = v, 10, 100, true));
        mysticPath.add(new SliderSetting("Speed", () -> MysticPathing.speed, v -> MysticPathing.speed = v, 1, 20, false));
        mysticPath.add(new CheckSetting("Auto Jump", () -> MysticPathing.autoJump, v -> MysticPathing.autoJump = v));
        mysticPath.add(new CheckSetting("Path Valuables", () -> MysticPathing.pathValuables, v -> MysticPathing.pathValuables = v));
        miscDrops.add(mysticPath);
        // ----------------------

        Dropdown midClick = new Dropdown("Middle Click Friend");
        midClick.add(new CheckSetting("Enabled", () -> MiddleClick.enabled, v -> MiddleClick.enabled = v));
        miscDrops.add(midClick);

        Dropdown other = new Dropdown("Name Hider");
        other.add(new CheckSetting("Enabled", () -> NameHider.enabled, v -> NameHider.enabled = v));
        other.add(new TextFieldSetting("Custom Name:", () -> NameHider.fakeName, v -> NameHider.fakeName = v));
        miscDrops.add(other);

        shortcutsDropdown = new Dropdown("Shortcuts");
        rebuildShortcuts();
        miscDrops.add(shortcutsDropdown);
    }

    private void rebuildShortcuts() {
        if (shortcutsDropdown == null) return;
        shortcutsDropdown.components.clear();
        for (ShortcutManager.Shortcut s : ShortcutManager.shortcuts) {
            shortcutsDropdown.add(new ShortcutComp(s));
        }
        shortcutsDropdown.add(new ButtonSetting(EnumChatFormatting.GREEN + "+ Add New Shortcut", () -> {
            ShortcutManager.shortcuts.add(new ShortcutManager.Shortcut("/command", Keyboard.KEY_NONE, true));
            rebuildShortcuts();
        }));
    }

    private void initRender() {
        Dropdown esp = new Dropdown("ESP");
        esp.add(new CheckSetting("Enabled", () -> ESP.enabled, v -> ESP.enabled = v));
        esp.add(new CheckSetting("3D Box", () -> ESP.box, v -> ESP.box = v));
        esp.add(new CheckSetting("Skeleton", () -> ESP.skeleton, v -> ESP.skeleton = v));
        esp.add(new CheckSetting("Chams", () -> Chams.enabled, v -> Chams.enabled = v));
        renderDrops.add(esp);

        Dropdown itemEsp = new Dropdown("Item ESP");
        itemEsp.add(new CheckSetting("Enabled", () -> ItemESP.enabled, v -> ItemESP.enabled = v));
        itemEsp.add(new CheckSetting("Sound Alert", () -> ItemESP.soundAlert, v -> ItemESP.soundAlert = v));
        itemEsp.add(new CheckSetting("Show Gold", () -> ItemESP.showGold, v -> ItemESP.showGold = v));
        itemEsp.add(new CheckSetting("Show Mystics", () -> ItemESP.showMystics, v -> ItemESP.showMystics = v));
        itemEsp.add(new CheckSetting("Show Valuables", () -> ItemESP.showValuables, v -> ItemESP.showValuables = v));
        itemEsp.add(new CheckSetting("Trace Gold", () -> ItemESP.traceGold, v -> ItemESP.traceGold = v));
        itemEsp.add(new CheckSetting("Trace Mystics", () -> ItemESP.traceMystics, v -> ItemESP.traceMystics = v));
        itemEsp.add(new CheckSetting("Trace Valuables", () -> ItemESP.traceValuables, v -> ItemESP.traceValuables = v));
        renderDrops.add(itemEsp);

        Dropdown nametags = new Dropdown("Nametags");
        nametags.add(new CheckSetting("Enabled", () -> Nametags.enabled, v -> Nametags.enabled = v));
        nametags.add(new CheckSetting("Show Health", () -> Nametags.showHealth, v -> Nametags.showHealth = v));
        nametags.add(new CheckSetting("Show Invis", () -> Nametags.showInvis, v -> Nametags.showInvis = v));
        nametags.add(new CheckSetting("Remove Tags", () -> Nametags.removeTags, v -> Nametags.removeTags = v));
        nametags.add(new SliderSetting("Offset", () -> Nametags.offset, v -> Nametags.offset = v, -40, 40, true));
        renderDrops.add(nametags);

        Dropdown tracers = new Dropdown("Tracers");
        tracers.add(new CheckSetting("Enabled", () -> Tracers.enabled, v -> Tracers.enabled = v));
        tracers.add(new SliderSetting("Filter Mode", () -> Tracers.filterMode, v -> Tracers.filterMode = v, 0, 3, true));
        tracers.add(new SliderSetting("Width", () -> Tracers.lineWidth, v -> Tracers.lineWidth = v, 1, 5, false));
        renderDrops.add(tracers);

        Dropdown chest = new Dropdown("Chest ESP");
        chest.add(new CheckSetting("Enabled", () -> ChestESP.enabled, v -> ChestESP.enabled = v));
        chest.add(new CheckSetting("Rainbow", () -> ChestESP.rainbow, v -> ChestESP.rainbow = v));
        chest.add(new SliderSetting("Red", () -> (double)ChestESP.red, v -> ChestESP.red = v.intValue(), 0, 255, true));
        renderDrops.add(chest);

        Dropdown fb = new Dropdown("Fullbright");
        fb.add(new CheckSetting("Enabled", () -> Fullbright.enabled, v -> { Fullbright.enabled = v; Fullbright.toggle(); }));
        renderDrops.add(fb);
    }

    private void initEvents() {
        Dropdown cake = new Dropdown("Cake Event");
        cake.add(new CheckSetting("Cake ESP", () -> MiscBlockESP.cakeEnabled, v -> MiscBlockESP.cakeEnabled = v));
        cake.add(new CheckSetting("Auto Cake", () -> AutoGrinder.autoCake, v -> AutoGrinder.autoCake = v));
        eventDrops.add(cake);

        Dropdown squads = new Dropdown("Squads");
        squads.add(new CheckSetting("Banner ESP", () -> MiscBlockESP.bannerEnabled, v -> MiscBlockESP.bannerEnabled = v));
        eventDrops.add(squads);

        // --- NEW RAFFLE ESP ---
        Dropdown raffle = new Dropdown("Raffle");
        raffle.add(new CheckSetting("Enabled", () -> RaffleESP.enabled, v -> RaffleESP.enabled = v));
        raffle.add(new CheckSetting("Tracers", () -> RaffleESP.tracers, v -> RaffleESP.tracers = v));
        eventDrops.add(raffle);
        // ----------------------

        eventDrops.add(new Dropdown("Rage Pit"));
        eventDrops.add(new Dropdown("The Beast"));
        eventDrops.add(new Dropdown("Robbery"));
        eventDrops.add(new Dropdown("KOTH"));
        eventDrops.add(new Dropdown("Spire"));
        eventDrops.add(new Dropdown("Care Package"));
        eventDrops.add(new Dropdown("Blockhead"));
    }

    // --- DRAW SCREEN ---
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!Display.isActive()) return;

        if (editHudMode) {
            drawRect(0, 0, width, height, 0x90000000);
            HudManager.INSTANCE.drawEditor(mouseX, mouseY);
            RenderUtil.drawCenteredString("DRAG TO MOVE - SNAP ENABLED", width/2, 20, -1);
            RenderUtil.drawCenteredString("Press ESC or Toggle 'EDIT HUD' to Close", width/2, 35, 0xFFAAAAAA);
        }

        drawBackgroundLayers();
        drawSidebar(mouseX, mouseY);

        int viewX = pX + SIDEBAR_WIDTH;
        int viewY = pY + 20;
        int viewW = pW - SIDEBAR_WIDTH;
        int viewH = pH - 40;

        if (currentTab == 6) {
            drawSocialTab(mouseX, mouseY, viewX, viewY);
        } else {
            List<Dropdown> targetList = getActiveList();
            int totalHeight = 0;
            if (targetList != null) {
                for (Dropdown d : targetList) totalHeight += d.getHeight() + 10;
            }

            handleScrolling(totalHeight, viewH);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.scissor(viewX, viewY, viewW, viewH);

            int currentY = viewY - scrollOffset;
            int listX = viewX + 15;

            if (targetList != null) {
                for (Dropdown d : targetList) {
                    d.y = currentY;
                    d.x = listX;
                    d.draw(mouseX, mouseY);
                    currentY += d.getHeight() + 10;
                }
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            drawScrollbar(viewX + viewW - 10, viewY, 4, viewH, totalHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBackgroundLayers() {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glColorMask(false, false, false, false);
        RenderUtil.drawPerfectRoundedRect(pX, pY, pW, pH, 18, -1);
        GL11.glColorMask(true, true, true, true);
        if (blurGuiOnly) { GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF); }
        else { GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF); }
        if (blurShader != null) {
            if (blurGuiOnly) blurShader.renderBlur(18); else blurShader.renderBlurFullscreen(18);
        } else {
            RenderUtil.drawRect(pX, pY, pW, pH, 0xDD000000);
        }
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderUtil.drawPerfectRoundedRect(pX, pY, pW, pH, 18, COLOR_BG_TINT);
        RenderUtil.drawGradientRect(pX, pY, pX + pW, pY + pH, 0x15FFFFFF, 0x00000000);
        RenderUtil.drawRoundedOutline(pX, pY, pW, pH, 18, 1.0f, COLOR_GUI_OUTLINE);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private void drawSocialTab(int mX, int mY, int x, int y) {
        int startX = x + 15;
        int startY = y + 5;

        RenderUtil.drawString("Add Player (Enter to add Friend):", startX, startY, 0xFFAAAAAA);
        this.socialField.xPosition = startX;
        this.socialField.yPosition = startY + 12;
        this.socialField.drawTextBox();

        int btnW = 60; int btnH = 18; int btnY = startY + 38;
        boolean hF = isOver(mX, mY, startX, btnY, btnW, btnH);
        boolean hK = isOver(mX, mY, startX + btnW + 10, btnY, btnW, btnH);

        RenderUtil.drawPerfectRoundedRect(startX, btnY, btnW, btnH, 4, hF ? 0xFF00AAAA : 0xFF008888);
        RenderUtil.drawCenteredString("Add Friend", startX + btnW/2, btnY + 5, -1);

        RenderUtil.drawPerfectRoundedRect(startX + btnW + 10, btnY, btnW, btnH, 4, hK ? 0xFFAA0000 : 0xFF880000);
        RenderUtil.drawCenteredString("Add KOS", startX + btnW + 10 + btnW/2, btnY + 5, -1);

        int cbX = startX + 140;
        int cbY = btnY + 4;
        boolean alerts = SocialManager.kosAlerts;
        RenderUtil.drawRoundedOutline(cbX, cbY, 10, 10, 2, 1, alerts ? 0xFF00FF00 : 0xFF888888);
        if(alerts) RenderUtil.drawPerfectRoundedRect(cbX + 2, cbY + 2, 6, 6, 1, 0xFF00FF00);
        RenderUtil.drawString("Notify KOS", cbX + 14, cbY + 1, -1);

        if (this.socialField.isFocused() && !this.socialField.getText().isEmpty()) {
            findMatches();
            if (!socialMatch.isEmpty()) {
                RenderUtil.drawString(EnumChatFormatting.GRAY + "Match: " + socialMatch, startX + 210, startY + 18, -1);
            }
        }

        int listY = btnY + 30;
        int colW = 130;

        RenderUtil.drawString("Friends (" + SocialManager.friends.size() + ")", startX, listY, 0xFF55FFFF);
        int fy = listY + 15;
        for(String f : SocialManager.friends) {
            if (fy > y + pH - 60) break;
            RenderUtil.drawString(f, startX, fy, -1);
            if(isOver(mX, mY, startX + colW - 15, fy, 10, 10)) {
                RenderUtil.drawString("x", startX + colW - 15, fy, 0xFFFF0000);
            } else {
                RenderUtil.drawString("x", startX + colW - 15, fy, 0xFF555555);
            }
            fy += 12;
        }

        int kX = startX + colW + 20;
        RenderUtil.drawString("KOS (" + SocialManager.kos.size() + ")", kX, listY, 0xFFFF5555);
        int ky = listY + 15;
        for(String k : SocialManager.kos) {
            if (ky > y + pH - 60) break;
            RenderUtil.drawString(k, kX, ky, -1);
            if(isOver(mX, mY, kX + colW - 15, ky, 10, 10)) {
                RenderUtil.drawString("x", kX + colW - 15, ky, 0xFFFF0000);
            } else {
                RenderUtil.drawString("x", kX + colW - 15, ky, 0xFF555555);
            }
            ky += 12;
        }
    }

    private void findMatches() {
        socialMatch = "";
        String input = socialField.getText().toLowerCase();
        if (mc.getNetHandler() != null && mc.getNetHandler().getPlayerInfoMap() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if(info.getGameProfile() == null) continue;
                String name = info.getGameProfile().getName();
                if (name != null && name.toLowerCase().startsWith(input)) {
                    socialMatch = name;
                    return;
                }
            }
        }
    }

    private void drawSidebar(int mouseX, int mouseY) {
        RenderUtil.drawRect(pX + SIDEBAR_WIDTH, pY + 10, 1, pH - 20, 0x20FFFFFF);
        RenderUtil.drawCenteredString("Monolith", pX + SIDEBAR_WIDTH / 2, pY + 25, 0xFFFFFFFF);

        int dW = 70; int dH = 16;
        int dX = pX + (SIDEBAR_WIDTH / 2) - (dW / 2);
        int dY = pY + 52;
        boolean hover = isOver(mouseX, mouseY, dX, dY, dW, dH);
        RenderUtil.drawPerfectRoundedRect(dX, dY, dW, dH, 4, hover ? 0xFF6875ff : COLOR_DISCORD);
        RenderUtil.drawCenteredString("Discord", dX + dW/2, dY + 4, -1);

        int tabY = pY + 80;
        int tabX = pX + 12;
        String[] tabs = {"Combat", "Autogrinder", "Client", "Misc", "Render", "Events", "Social"};

        for(int i=0; i<tabs.length; i++) {
            drawSidebarButton(tabX, tabY + (i*36), 96, 28, tabs[i], i, mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mX, int mY, int btn) throws IOException {
        if (editHudMode) {
            HudManager.INSTANCE.mouseClicked(mX, mY, btn);
        }

        int dX = pX + (SIDEBAR_WIDTH / 2) - 35;
        if (isOver(mX, mY, dX, pY + 52, 70, 16)) {
            try { Desktop.getDesktop().browse(new URI(DISCORD_LINK)); } catch(Exception e){}
            return;
        }

        int tabX = pX + 12; int tabY = pY + 80;
        for(int i=0; i<7; i++) {
            if (isOver(mX, mY, tabX, tabY + (i*36), 96, 28)) {
                currentTab = i; scrollOffset = 0; return;
            }
        }

        if (currentTab == 6) {
            int startX = pX + SIDEBAR_WIDTH + 15;
            int startY = pY + 25;

            this.socialField.mouseClicked(mX, mY, btn);

            int btnY = startY + 38;
            if(isOver(mX, mY, startX, btnY, 60, 18)) {
                String t = socialField.getText();
                if(!t.isEmpty()) { SocialManager.addFriend(t); socialField.setText(""); }
            }
            if(isOver(mX, mY, startX + 70, btnY, 60, 18)) {
                String t = socialField.getText();
                if(!t.isEmpty()) { SocialManager.addKOS(t); socialField.setText(""); }
            }
            if(isOver(mX, mY, startX + 140, btnY + 4, 80, 10)) {
                SocialManager.kosAlerts = !SocialManager.kosAlerts;
            }

            int listY = btnY + 30; int colW = 130;
            int fy = listY + 15;
            for(String f : new ArrayList<>(SocialManager.friends)) {
                if(isOver(mX, mY, startX + colW - 15, fy, 10, 10)) SocialManager.remove(f);
                fy += 12;
            }
            int kX = startX + colW + 20;
            int ky = listY + 15;
            for(String k : new ArrayList<>(SocialManager.kos)) {
                if(isOver(mX, mY, kX + colW - 15, ky, 10, 10)) SocialManager.remove(k);
                ky += 12;
            }
            return;
        }

        int viewX = pX + SIDEBAR_WIDTH;
        int viewY = pY + 20;
        int viewW = pW - SIDEBAR_WIDTH;
        int viewH = pH - 40;

        if (maxScroll > 0 && isOver(mX, mY, viewX + viewW - 12, viewY, 12, viewH)) {
            isDraggingScroll = true; return;
        }

        if (isOver(mX, mY, viewX, viewY, viewW, viewH)) {
            List<Dropdown> targetList = getActiveList();
            if (targetList != null) {
                for (Dropdown d : new ArrayList<>(targetList)) d.mouseClicked(mX, mY, btn);
            }
        }
        super.mouseClicked(mX, mY, btn);
    }

    @Override
    protected void mouseReleased(int mX, int mY, int state) {
        if (editHudMode) {
            HudManager.INSTANCE.mouseReleased(mX, mY, state);
        }

        isDraggingScroll = false;
        List<Dropdown> targetList = getActiveList();
        if(targetList != null) for(Dropdown d : targetList) d.mouseReleased(mX, mY, state);
        super.mouseReleased(mX, mY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDraggingScroll && maxScroll > 0) {
            int viewY = pY + 20; int viewH = pH - 40;
            float relativeY = mouseY - viewY;
            float percent = relativeY / (float)viewH;
            scrollOffset = (int)(maxScroll * percent);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    private void drawSidebarButton(int x, int y, int w, int h, String text, int id, int mX, int mY) {
        boolean active = currentTab == id;
        boolean hover = isOver(mX, mY, x, y, w, h);
        int bg = active ? COLOR_GLASS_FILL : (hover ? 0x20FFFFFF : 0x00000000);
        int txt = active ? 0xFFFFFFFF : (hover ? 0xFFFFFFFF : 0xFFAAAAAA);
        if (active || hover) {
            RenderUtil.drawPerfectRoundedRect(x, y, w, h, 8, bg);
            if(active) RenderUtil.drawRoundedOutline(x, y, w, h, 8, 1.0f, COLOR_GLASS_BORDER);
        }
        RenderUtil.drawCenteredString(text, x + w / 2, y + (h/2) - 4, txt);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (currentTab != 6) {
            List<Dropdown> targetList = getActiveList();
            if (targetList != null) {
                for(Dropdown d : targetList) d.keyTyped(typedChar, keyCode);
            }
        }

        if (currentTab == 6 && this.socialField.isFocused()) {
            if (keyCode == Keyboard.KEY_TAB && !socialMatch.isEmpty()) {
                this.socialField.setText(socialMatch);
            } else if (keyCode == Keyboard.KEY_RETURN) {
                if(!socialField.getText().isEmpty()) {
                    SocialManager.addFriend(socialField.getText());
                    socialField.setText("");
                }
            } else {
                this.socialField.textboxKeyTyped(typedChar, keyCode);
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        if (socialField != null) socialField.updateCursorCounter();
        super.updateScreen();
    }

    private boolean isOver(int mX, int mY, int x, int y, int w, int h) {
        return mX >= x && mX <= x + w && mY >= y && mY <= y + h;
    }

    private void handleScrolling(int totalHeight, int viewHeight) {
        int dWheel = Mouse.getDWheel();
        maxScroll = Math.max(0, totalHeight - viewHeight);
        if (dWheel != 0) {
            scrollOffset -= dWheel / 6;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        }
    }

    private void drawScrollbar(int x, int y, int w, int h, int totalHeight) {
        if (maxScroll <= 0) return;
        RenderUtil.drawPerfectRoundedRect(x, y, w, h, 2, 0x20FFFFFF);
        float viewportRatio = (float)h / (float)totalHeight;
        int thumbHeight = (int)(h * viewportRatio);
        if (thumbHeight < 20) thumbHeight = 20;
        float scrollRatio = (float)scrollOffset / (float)maxScroll;
        int thumbY = (int)((h - thumbHeight) * scrollRatio);
        int color = isDraggingScroll ? COLOR_GLASS_BORDER : 0x80FFFFFF;
        RenderUtil.drawPerfectRoundedRect(x, y + thumbY, w, thumbHeight, 2, color);
    }

    private List<Dropdown> getActiveList() {
        switch(currentTab) {
            case 0: return combatDrops;
            case 1: return grinderDrops;
            case 2: return clientDrops;
            case 3: return miscDrops;
            case 4: return renderDrops;
            case 5: return eventDrops;
        }
        return null;
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    public static class ShortcutManager {
        public static class Shortcut {
            public String command;
            public int key;
            public boolean enabled;
            public Shortcut(String c, int k, boolean e) { command = c; key = k; enabled = e; }
        }

        public static List<Shortcut> shortcuts = new ArrayList<>();

        public static void onKeyInput(int keyCode) {
            if (keyCode == Keyboard.KEY_NONE) return;
            if (Minecraft.getMinecraft().currentScreen != null) return;

            for (Shortcut s : shortcuts) {
                if (s.enabled && s.key == keyCode) {
                    if (s.command != null && !s.command.isEmpty()) {
                        Minecraft.getMinecraft().thePlayer.sendChatMessage(s.command);
                    }
                }
            }
        }
    }

    public static class ConfigManager {
        private static final File CONFIG_FILE = new File(Minecraft.getMinecraft().mcDataDir, "melomod_config.txt");

        public static void save() {
            try (PrintWriter writer = new PrintWriter(new FileWriter(CONFIG_FILE))) {
                writer.println("LeftClicker.enabled:" + LeftClicker.enabled);
                writer.println("LeftClicker.triggerBot:" + LeftClicker.triggerBot);
                writer.println("LeftClicker.weaponOnly:" + LeftClicker.weaponOnly);
                writer.println("LeftClicker.minCPS:" + LeftClicker.minCPS);
                writer.println("LeftClicker.maxCPS:" + LeftClicker.maxCPS);

                writer.println("RightClicker.enabled:" + RightClicker.enabled);
                writer.println("RightClicker.minCPS:" + RightClicker.minCPS);
                writer.println("RightClicker.maxCPS:" + RightClicker.maxCPS);
                writer.println("RightClicker.startDelay:" + RightClicker.startDelay);
                writer.println("RightClicker.jitter:" + RightClicker.jitter);
                writer.println("RightClicker.onlyBlocks:" + RightClicker.onlyBlocks);
                writer.println("RightClicker.noSword:" + RightClicker.noSword);
                writer.println("RightClicker.ignoreRods:" + RightClicker.ignoreRods);
                writer.println("RightClicker.allowEat:" + RightClicker.allowEat);
                writer.println("RightClicker.allowBow:" + RightClicker.allowBow);

                writer.println("AimAssist.enabled:" + AimAssist.enabled);
                writer.println("AimAssist.clickAim:" + AimAssist.clickAim);
                writer.println("AimAssist.speedYaw:" + AimAssist.speedYaw);
                writer.println("AimAssist.speedPitch:" + AimAssist.speedPitch);

                writer.println("AutoGHead.enabled:" + AutoGHead.enabled);
                writer.println("AutoGHead.health:" + AutoGHead.health);
                writer.println("AutoGHead.minDelay:" + AutoGHead.minDelay);

                writer.println("AutoWeapon.enabled:" + AutoWeapon.enabled);
                writer.println("AutoWeapon.onlyWhenHoldingDown:" + AutoWeapon.onlyWhenHoldingDown);
                writer.println("AutoWeapon.goBackToPrevSlot:" + AutoWeapon.goBackToPrevSlot);

                writer.println("AutoGrinder.enabled:" + AutoGrinder.enabled);
                writer.println("AutoGrinder.aimPitch:" + AutoGrinder.aimPitch);
                writer.println("AutoGrinder.autoWalk:" + AutoGrinder.autoWalk);
                writer.println("AutoGrinder.autoClick:" + AutoGrinder.autoClick);
                writer.println("AutoGrinder.autoWeapon:" + AutoGrinder.autoWeapon);
                writer.println("AutoGrinder.autoGHead:" + AutoGrinder.autoGHead);

                // --- SAVE NEW SETTINGS ---
                writer.println("AutoGrinder.jumpAndSprint:" + AutoGrinder.jumpAndSprint);

                // --- SAVE AUTO SWAP SETTINGS ---
                writer.println("AutoGrinder.autoSwap:" + AutoGrinder.autoSwapEnabled);
                writer.println("AutoGrinder.swapThreshold:" + AutoGrinder.autoSwapThreshold);
                // -------------------------------

                writer.println("AutoGrinder.ignoreFriends:" + AutoGrinder.ignoreFriends);
                writer.println("AutoGrinder.autoPrestige:" + AutoGrinder.autoPrestige);
                writer.println("AutoGrinder.autoPerk:" + AutoGrinder.autoPerk);
                writer.println("AutoGrinder.perk1:" + AutoGrinder.perkSlot1);
                writer.println("AutoGrinder.perk2:" + AutoGrinder.perkSlot2);
                writer.println("AutoGrinder.perk3:" + AutoGrinder.perkSlot3);
                // -------------------------

                writer.println("AutoGrinder.useSpawnDelay:" + AutoGrinder.useSpawnDelay);
                writer.println("AutoGrinder.spawnDelaySec:" + AutoGrinder.spawnDelaySec);

                writer.println("AutoGrinder.fov:" + AutoGrinder.fov);
                writer.println("AutoGrinder.distance:" + AutoGrinder.distance);
                writer.println("AutoGrinder.ignoreDiamond:" + AutoGrinder.ignoreDiamond);

                writer.println("AutoGrinder.ignoreAllEvents:" + AutoGrinder.ignoreAllEvents);
                writer.println("AutoGrinder.doSpire:" + AutoGrinder.doSpire);
                writer.println("AutoGrinder.doRage:" + AutoGrinder.doRage);
                writer.println("AutoGrinder.autoCake:" + AutoGrinder.autoCake);

                writer.println("GrinderTimer.enabled:" + GrinderTimer.enabled);
                writer.println("GrinderTimer.hours:" + GrinderTimer.hours);
                writer.println("GrinderTimer.minutes:" + GrinderTimer.minutes);
                writer.println("GrinderTimer.seconds:" + GrinderTimer.seconds);

                writer.println("LegitPathing.enabled:" + LegitPathing.enabled);
                writer.println("MidESP.enabled:" + MidESP.enabled);

                writer.println("MonolithModGUI.hudFPS:" + MonolithModGUI.hudFPS);
                writer.println("MonolithModGUI.hudCPS:" + MonolithModGUI.hudCPS);
                writer.println("MonolithModGUI.blurGuiOnly:" + MonolithModGUI.blurGuiOnly);
                writer.println("TargetHUD.enabled:" + TargetHUD.enabled);
                writer.println("KosOverlay.enabled:" + KosOverlay.enabled);

                writer.println("PitEventOverlay.enabled:" + PitEventOverlay.enabled);

                writer.println("PitEventManager.enabled:" + PitEventManager.enabled);

                writer.println("AntiBot.enabled:" + AntiBot.enabled);
                writer.println("LobbySniper.targetName:" + LobbySniper.targetName);
                writer.println("MiddleClick.enabled:" + MiddleClick.enabled);
                writer.println("NameHider.enabled:" + NameHider.enabled);
                writer.println("NameHider.fakeName:" + NameHider.fakeName);

                // --- SAVE WEBHOOKS ---
                writer.println("Webhook.enabled:" + WebhookAlerts.enabled);
                writer.println("Webhook.url:" + WebhookAlerts.webhookUrl);

                // --- SAVE NEW ALERT MENTIONS ---
                writer.println("Webhook.mention:" + WebhookAlerts.alertMention);
                // -------------------------------

                writer.println("Webhook.prestige:" + WebhookAlerts.alertPrestige);
                writer.println("Webhook.ban:" + WebhookAlerts.alertBan);
                writer.println("Webhook.level:" + WebhookAlerts.alertLevelUp);
                writer.println("Webhook.routine:" + WebhookAlerts.alertRoutine);
                // ---------------------

                writer.println("MysticPathing.enabled:" + MysticPathing.enabled);
                writer.println("MysticPathing.range:" + MysticPathing.range);
                writer.println("MysticPathing.speed:" + MysticPathing.speed);
                writer.println("MysticPathing.autoJump:" + MysticPathing.autoJump);
                writer.println("MysticPathing.pathValuables:" + MysticPathing.pathValuables);

                // --- SAVE NEW RAFFLE SETTINGS ---
                writer.println("RaffleESP.enabled:" + RaffleESP.enabled);
                writer.println("RaffleESP.tracers:" + RaffleESP.tracers);
                // --------------------------------

                for (int i = 0; i < ShortcutManager.shortcuts.size(); i++) {
                    ShortcutManager.Shortcut s = ShortcutManager.shortcuts.get(i);
                    writer.println("Shortcut." + i + ":" + s.command + "|" + s.key + "|" + s.enabled);
                }

                // --- SAVE SOCIAL LISTS ---
                writer.println("Social.kosAlerts:" + SocialManager.kosAlerts);
                for (String friend : SocialManager.friends) {
                    writer.println("Social.Friend:" + friend);
                }
                for (String kos : SocialManager.kos) {
                    writer.println("Social.Kos:" + kos);
                }
                // -------------------------

                writer.println("ESP.enabled:" + ESP.enabled);
                writer.println("ESP.box:" + ESP.box);
                writer.println("ESP.skeleton:" + ESP.skeleton);
                writer.println("Chams.enabled:" + Chams.enabled);

                writer.println("ItemESP.enabled:" + ItemESP.enabled);
                writer.println("ItemESP.showGold:" + ItemESP.showGold);
                writer.println("ItemESP.showMystics:" + ItemESP.showMystics);
                writer.println("ItemESP.showValuables:" + ItemESP.showValuables);
                writer.println("ItemESP.soundAlert:" + ItemESP.soundAlert);
                writer.println("ItemESP.traceGold:" + ItemESP.traceGold);
                writer.println("ItemESP.traceMystics:" + ItemESP.traceMystics);
                writer.println("ItemESP.traceValuables:" + ItemESP.traceValuables);

                writer.println("Nametags.enabled:" + Nametags.enabled);
                writer.println("Nametags.showHealth:" + Nametags.showHealth);
                writer.println("Nametags.showInvis:" + Nametags.showInvis);
                writer.println("Nametags.removeTags:" + Nametags.removeTags);
                writer.println("Nametags.offset:" + Nametags.offset);

                writer.println("Tracers.enabled:" + Tracers.enabled);
                writer.println("Tracers.filterMode:" + Tracers.filterMode);
                writer.println("Tracers.lineWidth:" + Tracers.lineWidth);

                writer.println("ChestESP.enabled:" + ChestESP.enabled);
                writer.println("ChestESP.rainbow:" + ChestESP.rainbow);
                writer.println("ChestESP.red:" + ChestESP.red);

                writer.println("MiscBlockESP.cake:" + MiscBlockESP.cakeEnabled);
                writer.println("MiscBlockESP.banner:" + MiscBlockESP.bannerEnabled);

                writer.println("Fullbright.enabled:" + Fullbright.enabled);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void load() {
            if (!CONFIG_FILE.exists()) return;

            ShortcutManager.shortcuts.clear();
            SocialManager.friends.clear();
            SocialManager.kos.clear();

            try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":", 2);
                    if (parts.length < 2) continue;
                    String key = parts[0];
                    String val = parts[1];

                    try {
                        // --- LOAD SOCIAL LISTS ---
                        if (key.equals("Social.Friend")) {
                            SocialManager.friends.add(val);
                            continue;
                        }
                        if (key.equals("Social.Kos")) {
                            SocialManager.kos.add(val);
                            continue;
                        }
                        // -------------------------

                        if (key.startsWith("Shortcut.")) {
                            String[] sData = val.split("\\|");
                            if (sData.length >= 3) {
                                String cmd = sData[0];
                                int kCode = Integer.parseInt(sData[1]);
                                boolean en = Boolean.parseBoolean(sData[2]);
                                ShortcutManager.shortcuts.add(new ShortcutManager.Shortcut(cmd, kCode, en));
                            }
                        } else {
                            switch (key) {
                                case "Social.kosAlerts": SocialManager.kosAlerts = Boolean.parseBoolean(val); break;

                                case "LeftClicker.enabled": LeftClicker.enabled = Boolean.parseBoolean(val); break;
                                case "LeftClicker.triggerBot": LeftClicker.triggerBot = Boolean.parseBoolean(val); break;
                                case "LeftClicker.weaponOnly": LeftClicker.weaponOnly = Boolean.parseBoolean(val); break;
                                case "LeftClicker.minCPS": LeftClicker.minCPS = Integer.parseInt(val); break;
                                case "LeftClicker.maxCPS": LeftClicker.maxCPS = Integer.parseInt(val); break;

                                case "RightClicker.enabled": RightClicker.enabled = Boolean.parseBoolean(val); break;
                                case "RightClicker.minCPS": RightClicker.minCPS = Double.parseDouble(val); break;
                                case "RightClicker.maxCPS": RightClicker.maxCPS = Double.parseDouble(val); break;
                                case "RightClicker.jitter": RightClicker.jitter = Double.parseDouble(val); break;
                                case "RightClicker.startDelay": RightClicker.startDelay = Double.parseDouble(val); break;
                                case "RightClicker.onlyBlocks": RightClicker.onlyBlocks = Boolean.parseBoolean(val); break;
                                case "RightClicker.noSword": RightClicker.noSword = Boolean.parseBoolean(val); break;
                                case "RightClicker.ignoreRods": RightClicker.ignoreRods = Boolean.parseBoolean(val); break;
                                case "RightClicker.allowEat": RightClicker.allowEat = Boolean.parseBoolean(val); break;
                                case "RightClicker.allowBow": RightClicker.allowBow = Boolean.parseBoolean(val); break;

                                case "AimAssist.enabled": AimAssist.enabled = Boolean.parseBoolean(val); break;
                                case "AimAssist.clickAim": AimAssist.clickAim = Boolean.parseBoolean(val); break;
                                case "AimAssist.speedYaw": AimAssist.speedYaw = Double.parseDouble(val); break;
                                case "AimAssist.speedPitch": AimAssist.speedPitch = Double.parseDouble(val); break;

                                case "AutoGHead.enabled": AutoGHead.enabled = Boolean.parseBoolean(val); break;
                                case "AutoGHead.health": AutoGHead.health = Double.parseDouble(val); break;
                                case "AutoGHead.minDelay": AutoGHead.minDelay = Double.parseDouble(val); break;

                                case "AutoWeapon.enabled": AutoWeapon.enabled = Boolean.parseBoolean(val); break;
                                case "AutoWeapon.onlyWhenHoldingDown": AutoWeapon.onlyWhenHoldingDown = Boolean.parseBoolean(val); break;
                                case "AutoWeapon.goBackToPrevSlot": AutoWeapon.goBackToPrevSlot = Boolean.parseBoolean(val); break;

                                case "AutoGrinder.enabled": AutoGrinder.enabled = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.aimPitch": AutoGrinder.aimPitch = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.autoWalk": AutoGrinder.autoWalk = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.autoClick": AutoGrinder.autoClick = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.autoWeapon": AutoGrinder.autoWeapon = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.autoGHead": AutoGrinder.autoGHead = Boolean.parseBoolean(val); break;

                                // --- LOAD NEW SETTINGS ---
                                case "AutoGrinder.jumpAndSprint": AutoGrinder.jumpAndSprint = Boolean.parseBoolean(val); break;

                                // --- LOAD AUTO SWAP ---
                                case "AutoGrinder.autoSwap": AutoGrinder.autoSwapEnabled = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.swapThreshold": AutoGrinder.autoSwapThreshold = Double.parseDouble(val); break;
                                // ----------------------

                                case "AutoGrinder.ignoreFriends": AutoGrinder.ignoreFriends = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.autoPrestige": AutoGrinder.autoPrestige = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.autoPerk": AutoGrinder.autoPerk = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.perk1": AutoGrinder.perkSlot1 = Double.parseDouble(val); break;
                                case "AutoGrinder.perk2": AutoGrinder.perkSlot2 = Double.parseDouble(val); break;
                                case "AutoGrinder.perk3": AutoGrinder.perkSlot3 = Double.parseDouble(val); break;
                                // -------------------------

                                case "AutoGrinder.useSpawnDelay": AutoGrinder.useSpawnDelay = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.spawnDelaySec": AutoGrinder.spawnDelaySec = Double.parseDouble(val); break;

                                case "AutoGrinder.fov": AutoGrinder.fov = Double.parseDouble(val); break;
                                case "AutoGrinder.distance": AutoGrinder.distance = Double.parseDouble(val); break;
                                case "AutoGrinder.ignoreDiamond": AutoGrinder.ignoreDiamond = Boolean.parseBoolean(val); break;

                                case "AutoGrinder.ignoreAllEvents": AutoGrinder.ignoreAllEvents = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.doSpire": AutoGrinder.doSpire = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.doRage": AutoGrinder.doRage = Boolean.parseBoolean(val); break;
                                case "AutoGrinder.autoCake": AutoGrinder.autoCake = Boolean.parseBoolean(val); break;

                                case "GrinderTimer.enabled": GrinderTimer.enabled = Boolean.parseBoolean(val); break;
                                case "GrinderTimer.hours": GrinderTimer.hours = Integer.parseInt(val); break;
                                case "GrinderTimer.minutes": GrinderTimer.minutes = Integer.parseInt(val); break;
                                case "GrinderTimer.seconds": GrinderTimer.seconds = Integer.parseInt(val); break;

                                case "LegitPathing.enabled": LegitPathing.enabled = Boolean.parseBoolean(val); break;
                                case "MidESP.enabled": MidESP.enabled = Boolean.parseBoolean(val); break;

                                case "MonolithModGUI.hudFPS": MonolithModGUI.hudFPS = Boolean.parseBoolean(val); break;
                                case "MonolithModGUI.hudCPS": MonolithModGUI.hudCPS = Boolean.parseBoolean(val); break;
                                case "MonolithModGUI.blurGuiOnly": MonolithModGUI.blurGuiOnly = Boolean.parseBoolean(val); break;
                                case "TargetHUD.enabled": TargetHUD.enabled = Boolean.parseBoolean(val); break;
                                case "KosOverlay.enabled": KosOverlay.enabled = Boolean.parseBoolean(val); break;
                                case "PitEventOverlay.enabled": PitEventOverlay.enabled = Boolean.parseBoolean(val); break;

                                case "PitEventManager.enabled": PitEventManager.enabled = Boolean.parseBoolean(val); break;

                                case "AntiBot.enabled": AntiBot.enabled = Boolean.parseBoolean(val); break;
                                case "LobbySniper.targetName": LobbySniper.targetName = val; break;
                                case "MiddleClick.enabled": MiddleClick.enabled = Boolean.parseBoolean(val); break;
                                case "NameHider.enabled": NameHider.enabled = Boolean.parseBoolean(val); break;
                                case "NameHider.fakeName": NameHider.fakeName = val; break;

                                // --- LOAD WEBHOOKS ---
                                case "Webhook.enabled": WebhookAlerts.enabled = Boolean.parseBoolean(val); break;
                                case "Webhook.url": WebhookAlerts.webhookUrl = val; break;

                                // --- LOAD NEW ALERT MENTIONS ---
                                case "Webhook.mention": WebhookAlerts.alertMention = Boolean.parseBoolean(val); break;
                                // -------------------------------

                                case "Webhook.prestige": WebhookAlerts.alertPrestige = Boolean.parseBoolean(val); break;
                                case "Webhook.ban": WebhookAlerts.alertBan = Boolean.parseBoolean(val); break;
                                case "Webhook.level": WebhookAlerts.alertLevelUp = Boolean.parseBoolean(val); break;
                                case "Webhook.routine": WebhookAlerts.alertRoutine = Boolean.parseBoolean(val); break;
                                // ---------------------

                                case "MysticPathing.enabled": MysticPathing.enabled = Boolean.parseBoolean(val); break;
                                case "MysticPathing.range": MysticPathing.range = Double.parseDouble(val); break;
                                case "MysticPathing.speed": MysticPathing.speed = Double.parseDouble(val); break;
                                case "MysticPathing.autoJump": MysticPathing.autoJump = Boolean.parseBoolean(val); break;
                                case "MysticPathing.pathValuables": MysticPathing.pathValuables = Boolean.parseBoolean(val); break;

                                // --- LOAD NEW RAFFLE SETTINGS ---
                                case "RaffleESP.enabled": RaffleESP.enabled = Boolean.parseBoolean(val); break;
                                case "RaffleESP.tracers": RaffleESP.tracers = Boolean.parseBoolean(val); break;
                                // --------------------------------

                                case "ESP.enabled": ESP.enabled = Boolean.parseBoolean(val); break;
                                case "ESP.box": ESP.box = Boolean.parseBoolean(val); break;
                                case "ESP.skeleton": ESP.skeleton = Boolean.parseBoolean(val); break;
                                case "Chams.enabled": Chams.enabled = Boolean.parseBoolean(val); break;

                                case "ItemESP.enabled": ItemESP.enabled = Boolean.parseBoolean(val); break;
                                case "ItemESP.showGold": ItemESP.showGold = Boolean.parseBoolean(val); break;
                                case "ItemESP.showMystics": ItemESP.showMystics = Boolean.parseBoolean(val); break;
                                case "ItemESP.showValuables": ItemESP.showValuables = Boolean.parseBoolean(val); break;
                                case "ItemESP.soundAlert": ItemESP.soundAlert = Boolean.parseBoolean(val); break;
                                case "ItemESP.traceGold": ItemESP.traceGold = Boolean.parseBoolean(val); break;
                                case "ItemESP.traceMystics": ItemESP.traceMystics = Boolean.parseBoolean(val); break;
                                case "ItemESP.traceValuables": ItemESP.traceValuables = Boolean.parseBoolean(val); break;

                                case "Nametags.enabled": Nametags.enabled = Boolean.parseBoolean(val); break;
                                case "Nametags.showHealth": Nametags.showHealth = Boolean.parseBoolean(val); break;
                                case "Nametags.showInvis": Nametags.showInvis = Boolean.parseBoolean(val); break;
                                case "Nametags.removeTags": Nametags.removeTags = Boolean.parseBoolean(val); break;
                                case "Nametags.offset": Nametags.offset = Double.parseDouble(val); break;

                                case "Tracers.enabled": Tracers.enabled = Boolean.parseBoolean(val); break;
                                case "Tracers.filterMode": Tracers.filterMode = Double.parseDouble(val); break;
                                case "Tracers.lineWidth": Tracers.lineWidth = Double.parseDouble(val); break;

                                case "ChestESP.enabled": ChestESP.enabled = Boolean.parseBoolean(val); break;
                                case "ChestESP.rainbow": ChestESP.rainbow = Boolean.parseBoolean(val); break;
                                case "ChestESP.red": ChestESP.red = Integer.parseInt(val); break;

                                case "MiscBlockESP.cake": MiscBlockESP.cakeEnabled = Boolean.parseBoolean(val); break;
                                case "MiscBlockESP.banner": MiscBlockESP.bannerEnabled = Boolean.parseBoolean(val); break;

                                case "Fullbright.enabled":
                                    boolean fb = Boolean.parseBoolean(val);
                                    if(Fullbright.enabled != fb) {
                                        Fullbright.enabled = fb;
                                        Fullbright.toggle();
                                    }
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Skipping bad config line: " + line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void resetDefaults() {
            LeftClicker.enabled = false;
            RightClicker.enabled = false;
            AimAssist.enabled = false;
            AutoGrinder.enabled = false;

            // Reset Event flags
            AutoGrinder.ignoreAllEvents = false;
            AutoGrinder.doSpire = false;
            AutoGrinder.doRage = false;
            AutoGrinder.autoCake = false;

            // --- RESET NEW SETTINGS ---
            AutoGrinder.jumpAndSprint = false;

            // --- RESET AUTO SWAP ---
            AutoGrinder.autoSwapEnabled = false;
            AutoGrinder.autoSwapThreshold = 5; // Default threshold
            // -----------------------

            AutoGrinder.ignoreFriends = false;
            AutoGrinder.autoPrestige = false;
            AutoGrinder.autoPerk = false;
            AutoGrinder.perkSlot1 = 0;
            AutoGrinder.perkSlot2 = 0;
            AutoGrinder.perkSlot3 = 0;
            // -------------------------

            PitEventManager.enabled = false;
            ShortcutManager.shortcuts.clear();

            SocialManager.friends.clear();
            SocialManager.kos.clear();
            SocialManager.kosAlerts = false;

            MiscBlockESP.cakeEnabled = false;
            MiscBlockESP.bannerEnabled = false;

            // --- RESET NEW RAFFLE SETTINGS ---
            RaffleESP.enabled = false;
            RaffleESP.tracers = false;
            // ---------------------------------

            ItemESP.enabled = true;
            ItemESP.showGold = true;
            ItemESP.showMystics = true;
            ItemESP.showValuables = true;
            ItemESP.soundAlert = true;
            ItemESP.traceGold = false;
            ItemESP.traceMystics = true;
            ItemESP.traceValuables = false;

            LobbySniper.targetName = "";

            // --- RESET WEBHOOKS ---
            WebhookAlerts.enabled = false;
            WebhookAlerts.webhookUrl = "";
            WebhookAlerts.alertMention = false; // Reset mention alert
            WebhookAlerts.alertPrestige = true;
            WebhookAlerts.alertBan = true;
            WebhookAlerts.alertLevelUp = true;
            WebhookAlerts.alertRoutine = true;
            // ----------------------

            MysticPathing.enabled = false;
            MysticPathing.range = 60;
            MysticPathing.speed = 5;
            MysticPathing.autoJump = true;
            MysticPathing.pathValuables = false;

            GrinderTimer.enabled = false;
            GrinderTimer.hours = 0;
            GrinderTimer.minutes = 0;
            GrinderTimer.seconds = 0;

            if (Fullbright.enabled) Fullbright.toggle();
            Fullbright.enabled = false;
        }
    }

    public static class AutoGrinder {
        public static boolean enabled = false;
        public static boolean autoCenter = false;
        public static boolean useSpawnDelay = false;
        public static double spawnDelaySec = 3.0;

        public static boolean aimPitch = false;
        public static boolean autoWalk = false;
        public static boolean autoClick = false;
        public static boolean autoWeapon = false;
        public static boolean autoGHead = false;

        // --- NEW VARIABLES ---
        public static boolean jumpAndSprint = false;

        // --- AUTO SWAP LOBBY ---
        public static boolean autoSwapEnabled = false;
        public static double autoSwapThreshold = 5.0; // Default 5 players
        // -----------------------

        public static boolean ignoreFriends = false;
        public static boolean autoPrestige = false;
        public static boolean autoPerk = false;
        // Perk Slots (Represented as IDs for now)
        public static double perkSlot1 = 0;
        public static double perkSlot2 = 0;
        public static double perkSlot3 = 0;
        // --------------------

        public static boolean breakBlocks = false;
        public static boolean weaponOnly = false;
        public static double fov = 90;
        public static double distance = 4.5;
        public static double pitchOffSet = 0;
        public static double speedYaw = 20;
        public static double speedPitch = 20;
        public static double complimentYaw = 2;
        public static double complimentPitch = 2;
        public static double walkFov = 10;
        public static boolean ignoreDiamond = false;

        // --- EVENT VARIABLES ---
        public static boolean ignoreAllEvents = false;
        public static boolean doSpire = false;
        public static boolean doRage = false;
        public static boolean autoCake = false;
        // -----------------------
    }

    // --- ADDED RAFFLE CLASS FOR STORAGE ---
    public static class RaffleESP {
        public static boolean enabled = false;
        public static boolean tracers = false;
    }
    // --------------------------------------

    public class Dropdown {
        public String title;
        public int x, y, width = 290, headerHeight = 25;
        public boolean expanded = false;
        public List<Comp> components = new ArrayList<>();
        public float animHeight = 0;

        public Dropdown(String title) { this.title = title; }
        public void add(Comp c) { components.add(c); }

        public int getHeight() {
            int contentH = 0;
            for(Comp c : components) contentH += c.getHeight();
            if(!expanded) contentH = 0;
            animHeight = animHeight + ( (expanded ? contentH : 0) - animHeight) * 0.2f;
            return headerHeight + (int)Math.ceil(animHeight);
        }

        public void draw(int mX, int mY) {
            RenderUtil.drawPerfectRoundedRect(x, y, width, headerHeight, 6, 0x90202020);
            RenderUtil.drawString(title, x + 10, y + 8, -1);
            RenderUtil.drawString(expanded ? "-" : "+", x + width - 15, y + 8, 0xFFAAAAAA);

            if (animHeight > 1) {
                int cY = y + headerHeight;
                for (Comp c : components) {
                    if ((cY - (y + headerHeight)) > animHeight) break;
                    c.x = x + 5; c.y = cY; c.width = width - 10;
                    c.draw(mX, mY);
                    cY += c.getHeight();
                }
            }
            RenderUtil.drawRoundedOutline(x, y, width, headerHeight + (int)animHeight, 6, 1f, 0x40FFFFFF);
        }

        public void mouseClicked(int mX, int mY, int btn) {
            if (isOver(mX, mY, x, y, width, headerHeight)) { expanded = !expanded; return; }
            if (expanded) {
                for (Comp c : new ArrayList<>(components)) c.mouseClicked(mX, mY, btn);
            }
        }
        public void mouseReleased(int mX, int mY, int state) {
            if(expanded) for(Comp c : components) c.mouseReleased(mX, mY, state);
        }
        public void keyTyped(char t, int k) {
            if(expanded) for(Comp c : components) c.keyTyped(t, k);
        }
    }

    public abstract class Comp {
        public int x, y, width;
        public abstract void draw(int mX, int mY);
        public abstract void mouseClicked(int mX, int mY, int btn);
        public abstract void mouseReleased(int mX, int mY, int state);
        public abstract void keyTyped(char t, int k);
        public abstract int getHeight();
    }

    public class ShortcutComp extends Comp {
        ShortcutManager.Shortcut shortcut;
        GuiTextField field;
        boolean binding = false;

        public ShortcutComp(ShortcutManager.Shortcut s) {
            this.shortcut = s;
            this.field = new GuiTextField(0, Minecraft.getMinecraft().fontRendererObj, 0, 0, 160, 16);
            this.field.setMaxStringLength(100);
            this.field.setText(s.command);
        }

        @Override
        public void draw(int mX, int mY) {
            RenderUtil.drawPerfectRoundedRect(x, y, width, getHeight()-2, 4, 0x30000000);
            int cbS = 12; int cbX = x + 5; int cbY = y + 7;
            RenderUtil.drawRoundedOutline(cbX, cbY, cbS, cbS, 3, 1f, shortcut.enabled ? 0xFF00FF00 : 0xFF666666);
            if(shortcut.enabled) RenderUtil.drawPerfectRoundedRect(cbX+2, cbY+2, cbS-4, cbS-4, 2, 0xFF00FF00);

            String keyName = binding ? "..." : (shortcut.key == Keyboard.KEY_NONE ? "Key: None" : "Key: " + Keyboard.getKeyName(shortcut.key));
            int kbW = 70; int kbX = cbX + 20;
            boolean kbHover = isOver(mX, mY, kbX, y + 4, kbW, 18);
            RenderUtil.drawPerfectRoundedRect(kbX, y + 4, kbW, 18, 4, binding ? COLOR_GLASS_BORDER : (kbHover ? 0x50FFFFFF : 0x30FFFFFF));
            RenderUtil.drawCenteredString(keyName, kbX + kbW/2, y + 9, -1);

            int delS = 14; int delX = x + width - delS - 5;
            boolean delHover = isOver(mX, mY, delX, y + 6, delS, delS);
            RenderUtil.drawString("x", delX + 4, y + 6, delHover ? 0xFFFF0000 : 0xFF880000);

            this.field.xPosition = kbX + kbW + 5;
            this.field.yPosition = y + 5;
            this.field.width = delX - (kbX + kbW + 10);
            this.field.drawTextBox();
            if(this.field.getText().isEmpty() && !this.field.isFocused()) {
                RenderUtil.drawString("/command...", this.field.xPosition + 4, this.field.yPosition + 4, 0xFF666666);
            }
        }

        @Override
        public void mouseClicked(int mX, int mY, int btn) {
            if(isOver(mX, mY, x + 5, y + 7, 12, 12)) {
                shortcut.enabled = !shortcut.enabled;
                return;
            }
            if(isOver(mX, mY, x + 25, y + 4, 70, 18)) {
                binding = !binding;
                return;
            }
            if(isOver(mX, mY, x + width - 20, y + 6, 14, 14)) {
                ShortcutManager.shortcuts.remove(shortcut);
                rebuildShortcuts();
                return;
            }
            this.field.mouseClicked(mX, mY, btn);
            if(binding && btn != 0) binding = false;
        }

        @Override
        public void keyTyped(char t, int k) {
            if (binding) {
                if (k == Keyboard.KEY_ESCAPE) shortcut.key = Keyboard.KEY_NONE;
                else shortcut.key = k;
                binding = false;
                return;
            }
            if (this.field.isFocused()) {
                this.field.textboxKeyTyped(t, k);
                shortcut.command = this.field.getText();
            }
        }

        @Override public void mouseReleased(int mX, int mY, int s) {}
        @Override public int getHeight() { return 26; }
    }

    public class TextFieldSetting extends Comp {
        String name;
        GuiTextField field;
        Supplier<String> getter;
        Consumer<String> setter;

        public TextFieldSetting(String name, Supplier<String> getter, Consumer<String> setter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.field = new GuiTextField(0, Minecraft.getMinecraft().fontRendererObj, 0, 0, 140, 18);
            this.field.setMaxStringLength(500); // <--- FIXED HERE
        }

        @Override
        public void draw(int mX, int mY) {
            RenderUtil.drawString(name, x + 5, y + 6, 0xFFAAAAAA);
            this.field.xPosition = x + width - 145;
            this.field.yPosition = y + 2;
            if(!field.isFocused()) {
               field.setText(getter.get());
            }
            this.field.drawTextBox();
        }

        @Override
        public void mouseClicked(int mX, int mY, int btn) {
            this.field.mouseClicked(mX, mY, btn);
        }

        @Override public void mouseReleased(int mX, int mY, int state) {}

        @Override
        public void keyTyped(char t, int k) {
            if(this.field.isFocused()) {
                this.field.textboxKeyTyped(t, k);
                setter.accept(this.field.getText());
            }
        }

        @Override public int getHeight() { return 24; }
    }

    public class CheckSetting extends Comp {
        String name; Supplier<Boolean> getter; Consumer<Boolean> setter;
        public CheckSetting(String n, Supplier<Boolean> g, Consumer<Boolean> s) { name = n; getter = g; setter = s; }
        @Override public void draw(int mX, int mY) {
            boolean enabled = getter.get();
            RenderUtil.drawString(name, x + 5, y + 6, 0xFFDDDDDD);
            int boxSize = 12; int boxX = x + width - boxSize - 5;
            RenderUtil.drawRoundedOutline(boxX, y + 4, boxSize, boxSize, 3, 1f, enabled ? COLOR_GLASS_BORDER : 0xFF666666);
            if(enabled) RenderUtil.drawPerfectRoundedRect(boxX + 2, y + 6, boxSize - 4, boxSize - 4, 2, COLOR_GLASS_FILL);
        }
        @Override public void mouseClicked(int mX, int mY, int btn) {
            if(isOver(mX, mY, x, y, width, getHeight())) setter.accept(!getter.get());
        }
        @Override public void mouseReleased(int mX, int mY, int s) {}
        @Override public void keyTyped(char t, int k) {}
        @Override public int getHeight() { return 20; }
    }

    public class SliderSetting extends Comp {
        String name; Supplier<Double> getter; Consumer<Double> setter;
        double min, max; boolean dragging, isInt;
        public SliderSetting(String n, Supplier<Double> g, Consumer<Double> s, double min, double max, boolean i) {
            this.name=n; this.getter=g; this.setter=s; this.min=min; this.max=max; this.isInt=i;
        }
        @Override public void draw(int mX, int mY) {
            double val = getter.get();
            if(dragging) {
                double valX = mX - (x + width/2);
                double pct = valX / (double)(width/2 - 5);
                if(pct < 0) pct = 0; if(pct > 1) pct = 1;
                double newVal = min + (max - min) * pct;
                if(isInt) newVal = Math.round(newVal);
                setter.accept(newVal); val = newVal;
            }
            RenderUtil.drawString(name, x + 5, y + 6, 0xFFAAAAAA);
            String sVal = isInt ? String.valueOf((int)val) : String.format("%.1f", val);
            RenderUtil.drawString(sVal, x + width - Minecraft.getMinecraft().fontRendererObj.getStringWidth(sVal) - 5, y + 6, -1);
            int barW = width / 2; int barX = x + width - barW - 20;
            RenderUtil.drawPerfectRoundedRect(barX, y + 15, barW, 2, 1, 0xFF303030);
            double pct = (val - min) / (max - min);
            int fill = (int)(barW * pct);
            if(fill > 0) RenderUtil.drawPerfectRoundedRect(barX, y + 15, fill, 2, 1, COLOR_GLASS_FILL);
            RenderUtil.drawCircle(barX + fill, y + 16, 3, -1);
            if(dragging) RenderUtil.drawCircle(barX + fill, y + 16, 6, 0x40FFFFFF);
        }
        @Override public void mouseClicked(int mX, int mY, int btn) {
            if(isOver(mX, mY, x + width/2 - 30, y, width/2 + 30, getHeight())) dragging = true;
        }
        @Override public void mouseReleased(int mX, int mY, int s) { dragging = false; }
        @Override public void keyTyped(char t, int k) {}
        @Override public int getHeight() { return 24; }
    }

    public class ButtonSetting extends Comp {
        String text; Runnable action;
        public ButtonSetting(String t, Runnable a) { text=t; action=a; }
        @Override public void draw(int mX, int mY) {
            boolean hover = isOver(mX, mY, x + 20, y, width - 40, getHeight()-4);
            RenderUtil.drawPerfectRoundedRect(x + 20, y, width - 40, getHeight() - 4, 4, hover ? 0x40FFFFFF : 0x20FFFFFF);
            RenderUtil.drawCenteredString(text, x + width/2, y + 4, -1);
        }
        @Override public void mouseClicked(int mX, int mY, int btn) {
            if(isOver(mX, mY, x + 20, y, width - 40, getHeight()-4)) action.run();
        }
        @Override public void mouseReleased(int mX, int mY, int s) {}
        @Override public void keyTyped(char t, int k) {}
        @Override public int getHeight() { return 22; }
    }

    public static class RenderUtil {
        public static void drawPerfectRoundedRect(float x, float y, float w, float h, float r, int c) {
            float x2 = x + w; float y2 = y + h;
            drawRect(x + r, y, w - 2*r, h, c);
            drawRect(x, y + r, r, h - 2*r, c);
            drawRect(x2 - r, y + r, r, h - 2*r, c);
            drawQuarterCircle(x + r, y + r, r, 0, c);
            drawQuarterCircle(x2 - r, y + r, r, 1, c);
            drawQuarterCircle(x2 - r, y2 - r, r, 2, c);
            drawQuarterCircle(x + r, y2 - r, r, 3, c);
        }
        public static void drawRoundedOutline(float x, float y, float w, float h, float r, float thick, int c) {
            setupColor(c);
            GL11.glDisable(GL11.GL_CULL_FACE); GL11.glEnable(GL11.GL_LINE_SMOOTH); GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST); GL11.glLineWidth(thick); GL11.glBegin(GL11.GL_LINE_LOOP);
            for(int i=270; i<=360; i+=3) { double rad = Math.toRadians(i); GL11.glVertex2d((x + w - r) + Math.cos(rad) * r, (y + r) + Math.sin(rad) * r); }
            for(int i=0; i<=90; i+=3) { double rad = Math.toRadians(i); GL11.glVertex2d((x + w - r) + Math.cos(rad) * r, (y + h - r) + Math.sin(rad) * r); }
            for(int i=90; i<=180; i+=3) { double rad = Math.toRadians(i); GL11.glVertex2d((x + r) + Math.cos(rad) * r, (y + h - r) + Math.sin(rad) * r); }
            for(int i=180; i<=270; i+=3) { double rad = Math.toRadians(i); GL11.glVertex2d((x + r) + Math.cos(rad) * r, (y + r) + Math.sin(rad) * r); }
            GL11.glEnd(); GL11.glDisable(GL11.GL_LINE_SMOOTH); resetColor();
        }
        public static void drawRect(float x, float y, float w, float h, int c) {
            setupColor(c); GL11.glDisable(GL11.GL_CULL_FACE); GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x, y + h); GL11.glVertex2f(x + w, y + h); GL11.glVertex2f(x + w, y); GL11.glVertex2f(x, y);
            GL11.glEnd(); resetColor();
        }
        public static void drawQuarterCircle(float cx, float cy, float r, int mode, int c) {
            setupColor(c); GL11.glDisable(GL11.GL_CULL_FACE); GL11.glBegin(GL11.GL_TRIANGLE_FAN); GL11.glVertex2f(cx, cy);
            int s=0, e=0;
            if(mode==0){s=180;e=270;} else if(mode==1){s=270;e=360;} else if(mode==2){s=0;e=90;} else if(mode==3){s=90;e=180;}
            for (int i=s; i<=e; i+=3) { double rad = Math.toRadians(i); GL11.glVertex2d(cx + Math.cos(rad) * r, cy + Math.sin(rad) * r); }
            GL11.glEnd(); resetColor();
        }
        public static void setupColor(int c) {
            float f3=(c>>24&255)/255.0F; float f=(c>>16&255)/255.0F; float f1=(c>>8&255)/255.0F; float f2=(c&255)/255.0F;
            GlStateManager.enableBlend(); GlStateManager.disableTexture2D(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); GlStateManager.color(f,f1,f2,f3);
        }
        public static void resetColor() { GlStateManager.enableTexture2D(); GlStateManager.disableBlend(); }
        public static void drawCircle(float x, float y, float r, int c) { drawPerfectRoundedRect(x-r, y-r, r*2, r*2, r, c); }
        public static void drawCenteredString(String s, int x, int y, int c) { Minecraft.getMinecraft().fontRendererObj.drawString(s, x - Minecraft.getMinecraft().fontRendererObj.getStringWidth(s)/2, y, c); }
        public static void drawString(String s, int x, int y, int c) { Minecraft.getMinecraft().fontRendererObj.drawString(s, x, y, c); }
        public static void drawGradientRect(int l, int t, int r, int b, int start, int end) {
             float f = (float)(start >> 24 & 255) / 255.0F; float f1 = (float)(start >> 16 & 255) / 255.0F; float f2 = (float)(start >> 8 & 255) / 255.0F; float f3 = (float)(start & 255) / 255.0F;
             float f4 = (float)(end >> 24 & 255) / 255.0F; float f5 = (float)(end >> 16 & 255) / 255.0F; float f6 = (float)(end >> 8 & 255) / 255.0F; float f7 = (float)(end & 255) / 255.0F;
             GlStateManager.disableTexture2D(); GlStateManager.enableBlend(); GlStateManager.disableAlpha(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); GlStateManager.shadeModel(7425);
             GL11.glBegin(GL11.GL_QUADS); GL11.glColor4f(f1, f2, f3, f); GL11.glVertex2f(r, t); GL11.glColor4f(f1, f2, f3, f); GL11.glVertex2f(l, t); GL11.glColor4f(f5, f6, f7, f4); GL11.glVertex2f(l, b); GL11.glColor4f(f5, f6, f7, f4); GL11.glVertex2f(r, b); GL11.glEnd();
             GlStateManager.shadeModel(7424); GlStateManager.disableBlend(); GlStateManager.enableAlpha(); GlStateManager.enableTexture2D();
        }
        public static void scissor(int x, int y, int w, int h) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int scale = sr.getScaleFactor();
            int screenHeight = Minecraft.getMinecraft().displayHeight;
            GL11.glScissor(x * scale, screenHeight - (y + h) * scale, w * scale, h * scale);
        }
    }

    public static class BlurShader {
        private int programID, resU, radU;
        public BlurShader() {
            int v = createShader("#version 120\n void main() { gl_TexCoord[0] = gl_MultiTexCoord0; gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex; }", GL20.GL_VERTEX_SHADER);
            int f = createShader("#version 120\n uniform sampler2D textureIn; uniform vec2 resolution; uniform float radius; void main() { vec2 texelSize = 1.0 / resolution; vec4 color = vec4(0.0); float total = 0.0; for (float x = -radius; x <= radius; x += 1.0) { for (float y = -radius; y <= radius; y += 1.0) { float weight = 1.0 - (length(vec2(x,y)) / radius); if (weight < 0.0) weight = 0.0; color += texture2D(textureIn, gl_TexCoord[0].st + vec2(x, y) * texelSize) * weight; total += weight; } } gl_FragColor = color / total; }", GL20.GL_FRAGMENT_SHADER);
            programID = GL20.glCreateProgram(); GL20.glAttachShader(programID, v); GL20.glAttachShader(programID, f); GL20.glLinkProgram(programID);
            resU = GL20.glGetUniformLocation(programID, "resolution"); radU = GL20.glGetUniformLocation(programID, "radius");
        }
        public void renderBlur(float radius) { render(radius, true); }
        public void renderBlurFullscreen(float radius) { render(radius, false); }
        private void render(float radius, boolean useGui) {
            GL11.glPushMatrix(); GL20.glUseProgram(programID);
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            GL20.glUniform2f(resU, sr.getScaledWidth(), sr.getScaledHeight());
            GL20.glUniform1f(radU, radius);
            Minecraft.getMinecraft().getFramebuffer().bindFramebufferTexture();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(0, 0); GL11.glTexCoord2f(0, 0); GL11.glVertex2f(0, sr.getScaledHeight());
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(sr.getScaledWidth(), sr.getScaledHeight()); GL11.glTexCoord2f(1, 1); GL11.glVertex2f(sr.getScaledWidth(), 0);
            GL11.glEnd(); GL20.glUseProgram(0); GL11.glPopMatrix();
        }
        private int createShader(String src, int type) { int s = GL20.glCreateShader(type); GL20.glShaderSource(s, src); GL20.glCompileShader(s); return s; }
    }
}
