package tech.tnze.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tnze.msctf.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class IMEClient implements ClientModInitializer {
    public static final String MOD_ID = "tnze-s-ime-mod";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ThreadManager mThreadManager = null;
    public static UIElementManager mUIElementManager = null;
    public static int mClientId = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Tnze's IME mod is initializing");

        // Load native library here
        loadNativeLibrary();
        LOGGER.debug("Load native library success");

        // Create ITfThreadMgr instance
        mThreadManager = new ThreadManager();
        LOGGER.debug("Create ITfThreadMgr success");

        mClientId = mThreadManager.activateEx(ThreadManager.TF_TMAE_UIELEMENTENABLEDONLY);
        LOGGER.debug("Activated TSF with ClientID={}", mClientId);

        mUIElementManager = mThreadManager.getUIElementManager();
        LOGGER.debug("Obtained ITfUIElementMgr");

        try (Source source = mThreadManager.getSource()) {
            source.adviseSink(new Manager());
            LOGGER.info("Registered UIElementSink");
        }
    }

    private void loadNativeLibrary() {
        try (InputStream src = getClass().getResourceAsStream("/natives/msctf-jni.dll")) {
            if (src == null) {
                throw new RuntimeException("Native library resource not found");
            }
            Path tmp = Files.createTempFile(MOD_ID, "-jni.dll");
            Files.copy(src, tmp, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Native library copied to {}", tmp);
            System.load(tmp.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
