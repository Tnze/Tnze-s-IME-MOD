package tech.tnze.modmenu;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import static tech.tnze.client.IMEClient.LOGGER;

public class ConfigScreen extends OptionsSubScreen {

    protected ConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("tech.tnze.ime.options.title"));
    }

    @Override
    protected void addOptions() {
        if (this.list != null) {
            this.list.addSmall(OptionInstance.createBoolean("tech.tnze.ime.options.uiless_mode", true, newValue -> {
                LOGGER.info("UILess mode: {}", newValue);
            }));
        }
    }

    @Override
    public void removed() {
        // TODO: save options
    }
}
