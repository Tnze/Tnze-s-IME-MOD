package tech.tnze.mixin.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.tnze.client.IMEClient;
import tech.tnze.client.Manager;
import tech.tnze.msctf.*;

import java.lang.foreign.Arena;

import static tech.tnze.client.IMEClient.*;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public abstract Window getWindow();

    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At("TAIL"))
    private void tnze$initTextServiceFramework(GameConfig gameConfig, CallbackInfo ci) {

//        try {
//            ComBase.CoInitialize();
//        } catch (WindowsException ex) {
//            LOGGER.warn("Failed to CoInitialize", ex);
//        }

        // Create ITfThreadMgr instance
        mThreadManager = new ThreadManager();
        LOGGER.debug("Create ITfThreadMgr success");

        mClientId = mThreadManager.activateEx(ThreadManager.TF_TMAE_UIELEMENTENABLEDONLY);
        LOGGER.debug("Activated TSF with ClientID={}", mClientId);

        mUIElementManager = mThreadManager.getUIElementManager();
        LOGGER.debug("Obtained ITfUIElementMgr");

        try (Source source = mThreadManager.getSource()) {
            source.adviseSink(Manager.getInstance().uiElementSink);
            LOGGER.info("Registered UIElementSink");
        }

        long winHandle = GLFWNativeWin32.glfwGetWin32Window(getWindow().handle());
        LOGGER.debug("Window handle: {}", winHandle);

        mDocumentManager = mThreadManager.createDocumentManager();
        LOGGER.debug("Created DocumentManager: {}", mDocumentManager);

        Context context = mDocumentManager.createContext(mClientId, 0, Manager.getInstance().contextOwnerCompositionSink);
        mDocumentManager.push(context);

        try(DocumentManager oldDocMgr = mThreadManager.associateFocus(winHandle, mDocumentManager)) {
            LOGGER.debug("Old DocumentManager: {}", oldDocMgr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
