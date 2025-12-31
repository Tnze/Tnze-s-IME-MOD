package tech.tnze.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.components.EditBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tnze.msctf.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

public class IMEClient implements ClientModInitializer {
    public static final String MOD_ID = "tnze-s-ime-mod";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ThreadManager mThreadManager = null;
    public static DocumentManager mDocumentManager = null;
    public static UIElementManager mUIElementManager = null;
    public static HashMap<EditBox, Context> mContexts = new HashMap<>();
    public static int mClientId = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Tnze's IME mod is initializing");

        // Load native library here
//        loadNativeLibrary();
//        LOGGER.debug("Load native library success");

    }

//    private void loadNativeLibrary() {
//        try (InputStream src = getClass().getResourceAsStream("/natives/msctf-jni.dll")) {
//            if (src == null) {
//                throw new RuntimeException("Native library resource not found");
//            }
//            Path tmp = Files.createTempFile(MOD_ID, "-jni.dll");
//            Files.copy(src, tmp, StandardCopyOption.REPLACE_EXISTING);
//            LOGGER.debug("Native library copied to {}", tmp);
//            System.load(tmp.toAbsolutePath().toString());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
