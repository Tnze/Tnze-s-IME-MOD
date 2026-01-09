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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;

public class IMEClient implements ClientModInitializer {
    public static final String MOD_ID = "tnze-s-ime-mod";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Tnze's IME mod is initializing");
        var windir = System.getenv("windir");
        for (var lib : List.of("KERNEL32.dll", "OLEAUT32.dll", "OLE32.dll")) {
            var path = Path.of(windir, "System32", lib);
            System.load(path.toString());
        }
    }
}
