package tech.tnze.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tech.tnze.client.IMEClient;
import tech.tnze.client.Manager;
import tech.tnze.msctf.*;

import static tech.tnze.client.IMEClient.mThreadManager;
import static tech.tnze.client.IMEClient.LOGGER;

@Mixin(EditBox.class)
public class EditBoxMixin {
    @Inject(method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    public void tnze$registerDocumentMgr(Font font, int x, int y, int w, int h, EditBox editBox, Component message, CallbackInfo ci) {
        Manager manager = Manager.getInstance();
        DocumentManager docMgr = mThreadManager.createDocumentManager();
        Manager.documentManagers.put((EditBox) (Object) this, docMgr);
        Manager.mDocumentCleaner.register(this, () -> {
            LOGGER.info("Unregistering {}", docMgr);
            try {
                Manager.documentManagers.remove((EditBox) (Object) this).close();
                docMgr.close();
            } catch (Exception e) {
                LOGGER.error("Failed to release {}", docMgr);
            }
        });

        try (Context ctx = docMgr.createContext(IMEClient.mClientId, 0, manager)) {
            docMgr.push(ctx);
//            try (Source source = ctx.getSource()) {
//                source.adviseSink(new ContextOwner() {
//                    @Override
//                    public long getACPFromPoint(long x, long y, int flags) throws InvalidPointException, NoLayoutException {
//                        LOGGER.info("getACPFromPoint");
//                        return 0;
//                    }
//
//                    @Override
//                    public Object getAttribute(String guidAttribute) {
//                        LOGGER.info("getAttribute");
//                        return null;
//                    }
//
//                    @Override
//                    public void getScreenExt(Rect out) {
//                        LOGGER.info("getScreenExt");
//                    }
//
//                    @Override
//                    public void getStatus(Status status) {
//                        LOGGER.info("getStatus");
//                    }
//
//                    @Override
//                    public boolean getTextExt(long acpStart, long acpEnd, Rect rect) {
//                        LOGGER.info("getTextExt");
//                        return false;
//                    }
//
//                    @Override
//                    public int getWnd() {
//                        LOGGER.info("getWnd");
//                        return 0;
//                    }
//                });
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(method = "setFocused(Z)V", at = @At("TAIL"))
    public void tnze$setFocused(boolean focused, CallbackInfo ci) {
        LOGGER.info("EditBox focused state changed: {}", focused);
        if (!focused) {
            return;
        }
        try {
            long winHandle = GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().handle());
            DocumentManager priv = mThreadManager.associateFocus(winHandle, Manager.documentManagers.get((EditBox) (Object) this));
            if (priv != null) {
                priv.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
