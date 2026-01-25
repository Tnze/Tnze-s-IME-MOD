package tech.tnze.mixin.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tech.tnze.client.Manager;
import static tech.tnze.client.IMEClient.LOGGER;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public abstract Window getWindow();

    @Shadow
    @Nullable
    public Screen screen;

    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At("TAIL"))
    private void tnze$initTextServiceFramework(GameConfig gameConfig, CallbackInfo ci) {
        Window window = getWindow();
        long winHandle = window.handle();
        Manager manager = Manager.getInstance();

        manager.init(window, GLFWNativeWin32.glfwGetWin32Window(winHandle));

        GLFW.glfwSetWindowFocusCallback(winHandle, new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, boolean focused) {
                manager.onWindowFocusChanged(focused);
            }
        });
    }

    @Inject(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
    private void tnze$setScreen$removed(CallbackInfo ci) {
        Manager.getInstance().onScreenRemoved(this.screen);
    }


    @Inject(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    private void tnze$setScreen$added(CallbackInfo ci) {
        Manager.getInstance().onScreenAdded(this.screen);
    }

}
