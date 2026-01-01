package tech.tnze.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFWCharModsCallback;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InputConstants.class)
public class InputConstantsMixin {
    @Redirect(method = "setupKeyboardCallbacks", at= @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCharModsCallback(JLorg/lwjgl/glfw/GLFWCharModsCallbackI;)Lorg/lwjgl/glfw/GLFWCharModsCallback;"))
    private static GLFWCharModsCallback tnze$setupCharModesCallback(long window, GLFWCharModsCallbackI cbfun) {
        // Not register the callback
        return null;
    }
}
