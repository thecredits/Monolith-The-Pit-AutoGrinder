package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = Main.MODID, version = Main.VERSION)
public class Main {
    public static final String MODID = "examplemod";
    public static final String VERSION = "1.0";

    public static LeftClicker clicker;
    public static AimAssist aimAssist;
    public static AutoGrind autoGrind;
    public static KeyBinding menuKey;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        clicker = new LeftClicker();
        aimAssist = new AimAssist();
        autoGrind = new AutoGrind();

        MinecraftForge.EVENT_BUS.register(clicker);
        MinecraftForge.EVENT_BUS.register(aimAssist);
        MinecraftForge.EVENT_BUS.register(autoGrind);

        FMLCommonHandler.instance().bus().register(clicker);
        FMLCommonHandler.instance().bus().register(aimAssist);
        FMLCommonHandler.instance().bus().register(autoGrind);
        FMLCommonHandler.instance().bus().register(this);

        menuKey = new KeyBinding("MeloMod Menu", Keyboard.KEY_RSHIFT, "MeloMod");
        ClientRegistry.registerKeyBinding(menuKey);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (Minecraft.getMinecraft().currentScreen == null && menuKey.isPressed()) {
                Minecraft.getMinecraft().displayGuiScreen(new MeloModGUI());
            }
        }
    }
}
