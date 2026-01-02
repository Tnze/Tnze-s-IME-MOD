package tech.tnze.mixin.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.tnze.client.Manager;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public abstract Window getWindow();

    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At("TAIL"))
    private void tnze$initTextServiceFramework(GameConfig gameConfig, CallbackInfo ci) {
        long winHandle = getWindow().handle();
        Manager manager = Manager.getInstance();
        manager.init(GLFWNativeWin32.glfwGetWin32Window(winHandle));

        GLFW.glfwSetWindowFocusCallback(winHandle, new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, boolean focused) {
                manager.setFocus(focused);
            }
        });
    }
}
