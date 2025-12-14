package tech.tnze.mixin.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.tnze.client.IMEClient;
import tech.tnze.msctf.*;

import static tech.tnze.client.IMEClient.mThreadManager;
import static tech.tnze.client.IMEClient.LOGGER;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public abstract Window getWindow();

    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At("TAIL"))
    private void tnze$initTextServiceFramework(GameConfig gameConfig, CallbackInfo ci) {
        long winHandle = GLFWNativeWin32.glfwGetWin32Window(getWindow().handle());
        LOGGER.debug("Window handle: {}", winHandle);

        DocumentManager docManager = mThreadManager.createDocumentManager();
        DocumentManager prevDocManager = mThreadManager.associateFocus(winHandle, docManager);
        if (prevDocManager != null) {
            LOGGER.warn("Previous document manager should be null, got {}", prevDocManager);
            try {
                prevDocManager.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Context ctx = docManager.createContext(IMEClient.mClientId, 0, new ContextOwnerCompositionSink() {
            @Override
            public boolean onStartComposition(CompositionView composition) {
                LOGGER.info("Start composition");
                return true;
            }

            @Override
            public void onUpdateComposition(CompositionView composition, Range range) {
                LOGGER.info("Update composition");
            }

            @Override
            public void onEndComposition(CompositionView composition) {
                LOGGER.info("End composition");
            }
        });
        LOGGER.info("IME Context: {}", ctx);

        docManager.push(ctx);
        LOGGER.info("Pushed Context: {}", ctx);
    }
}
