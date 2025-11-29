package com.monolith.monolith;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
// MODULE IMPORTS
import com.monolith.monolith.gui.MonolithModGUI;
import com.monolith.monolith.gui.TargetHUD;
import com.monolith.monolith.gui.HudManager;
import com.monolith.monolith.modules.combat.*;
import com.monolith.monolith.modules.movement.LegitPathing;
import com.monolith.monolith.modules.render.*;
import com.monolith.monolith.modules.misc.*;
@Mod(modid = Main.MODID, version = Main.VERSION)
public class Main {
    public static final String MODID = "monolith";
    public static final String VERSION = "1.0";
    // Modules
    public static LeftClicker clicker;
    public static AimAssist aimAssist;
    public static Chams chams;
    public static KeyBinding menuKey;
    @EventHandler
    public void init(FMLInitializationEvent event) {
        // REGISTER MAIN ONLY
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        // Load modules immediately
        activateMod();
    }
    public static void activateMod() {
        System.out.println("[MeloMod] Loading Modules...");
        // --- INIT HUD MANAGER (Handles KOS, FPS, CPS dragging) ---
        HudManager.INSTANCE.init();
        MinecraftForge.EVENT_BUS.register(HudManager.INSTANCE);
        // LOAD YOUR MODULES
        clicker = new LeftClicker();
        aimAssist = new AimAssist();
        chams = new Chams();
        MinecraftForge.EVENT_BUS.register(new com.monolith.monolith.modules.misc.AutoSwitchLobby());
        MinecraftForge.EVENT_BUS.register(new AutoGHead());
        MinecraftForge.EVENT_BUS.register(new AutoWeapon());
        MinecraftForge.EVENT_BUS.register(new MiddleClick());
        MinecraftForge.EVENT_BUS.register(new SocialTracker());
        // --- REGISTER THE NEW PIT DETECTOR & MID ESP ---
        MinecraftForge.EVENT_BUS.register(new PitMapDetector());
        MinecraftForge.EVENT_BUS.register(new MidESP());
        MinecraftForge.EVENT_BUS.register(new PitEventDetector());
        // --- REGISTER NEW PIT EVENT MANAGER (BrookeAFK API) ---
        MinecraftForge.EVENT_BUS.register(new PitEventManager());
        MinecraftForge.EVENT_BUS.register(new ItemESP());
        MinecraftForge.EVENT_BUS.register(new WebhookAlerts());
        MinecraftForge.EVENT_BUS.register(clicker);
        MinecraftForge.EVENT_BUS.register(aimAssist);
        MinecraftForge.EVENT_BUS.register(chams);
        MinecraftForge.EVENT_BUS.register(new TargetHUD());
        MinecraftForge.EVENT_BUS.register(new LegitPathing());
        MinecraftForge.EVENT_BUS.register(new Nametags());
        MinecraftForge.EVENT_BUS.register(new com.monolith.monolith.modules.misc.MysticPathing());
        Tracers tracers = new Tracers();
        MinecraftForge.EVENT_BUS.register(tracers);
        FMLCommonHandler.instance().bus().register(tracers);
        MinecraftForge.EVENT_BUS.register(new AntiBot());
        MinecraftForge.EVENT_BUS.register(new NameHiderHandler());
        MinecraftForge.EVENT_BUS.register(new ChestESP());
        MinecraftForge.EVENT_BUS.register(new RightClicker());
        // --- REGISTER NEW BLOCK ESP ---
        MinecraftForge.EVENT_BUS.register(new MiscBlockESP());
        MinecraftForge.EVENT_BUS.register(new LobbySniper());
        // --- REGISTER NEW GRINDER TIMER ---
        MinecraftForge.EVENT_BUS.register(new GrinderTimer());
        // ----------------------------------
        menuKey = new KeyBinding("MeloMod Menu", Keyboard.KEY_RSHIFT, "MeloMod");
        ClientRegistry.registerKeyBinding(menuKey);
    }
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getMinecraft();
            // Normal Mod Logic
            if (mc.currentScreen == null && menuKey.isPressed()) {
                mc.displayGuiScreen(new MonolithModGUI());
            }
        }
    }

    @SubscribeEvent
    public void onGuiMovementSafety(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        // Check if a screen is open
        if (mc.currentScreen != null) {
            boolean isRiskGui = mc.currentScreen instanceof MonolithModGUI
                    || mc.currentScreen instanceof GuiContainer // Inventory, Chests, etc.
                    || mc.currentScreen instanceof GuiChat // Chat
                    || mc.currentScreen instanceof GuiIngameMenu; // Escape Menu
            if (isRiskGui) {
                GameSettings gs = mc.gameSettings;
                KeyBinding[] moveKeys = {
                        gs.keyBindForward, gs.keyBindBack, gs.keyBindLeft, gs.keyBindRight,
                        gs.keyBindJump, gs.keyBindSneak, gs.keyBindSprint,
                        gs.keyBindAttack, gs.keyBindUseItem
                };
                for (KeyBinding key : moveKeys) {
                    if (key.isKeyDown()) {
                        KeyBinding.setKeyBindState(key.getKeyCode(), false);
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        // The HudManager now handles this automatically via its own event registration.
    }
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        ESP.render(event.partialTicks);
    }
}
