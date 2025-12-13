package tech.tnze.client;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
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
            LOGGER.info("Registering UIElementSink");
            source.adviseSink(new UIElementSink() {
                @Override
                public boolean begin(int uiElementId) {
                    LOGGER.info("UIElement BEGIN: ID={}", uiElementId);
                    return false;
                }

                @Override
                public void update(int uiElementId) {
                    try (UIElement element = mUIElementManager.getUIElement(uiElementId)) {
                        LOGGER.info("UIElement UPDATE: ID={} DESC={} GUID={}", uiElementId, element.getDescription(), element.getGUID());
                        try (CandidateListUIElement candidateUI = element.intoCandidateListUIElement()) {
                            if (candidateUI != null) {
                                int count = candidateUI.getCount();
                                int pageCount = candidateUI.getPageIndex(null);
                                int[] indexes = new int[pageCount];
                                pageCount = candidateUI.getPageIndex(indexes);
                                LOGGER.info("Candidate count={} [{}]{}", count, pageCount, indexes);

                                int pageSize = pageCount > 1 ? indexes[1] : count;
                                String[] page = new String[indexes[1]];
                                for (int i = 0; i < pageSize; i++) {
                                    page[i] = candidateUI.getString(i);
                                }
                                LOGGER.info("Candidate list: {}", Arrays.toString(page));
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void end(int uiElementId) {
                    LOGGER.info("UIElement END: ID={}", uiElementId);
                }
            });
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
