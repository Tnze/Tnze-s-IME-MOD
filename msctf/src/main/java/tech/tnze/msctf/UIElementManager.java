package tech.tnze.msctf;

import windows.win32.ui.textservices.ITfUIElement;
import windows.win32.ui.textservices.ITfUIElementMgr;

import java.lang.foreign.Arena;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static tech.tnze.msctf.WindowsException.checkResult;

public class UIElementManager {
    private final ITfUIElementMgr inner;

    UIElementManager(ITfUIElementMgr inner) {
        this.inner = inner;
    }

    public UIElement getUIElement(int uiElementId) {
        try (var arena = Arena.ofConfined()) {
            var elementHolder = arena.allocate(ADDRESS.withTargetLayout(ITfUIElement.addressLayout()));
            checkResult(inner.GetUIElement(uiElementId, elementHolder));
            var element = elementHolder.get(ITfUIElement.addressLayout(), 0);
            return new UIElement(ITfUIElement.wrap(element));
        }
    }
}
