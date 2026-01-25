package tech.tnze.client.uielement;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.Rect2i;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import tech.tnze.client.Manager;
import tech.tnze.msctf.ComObject;
import tech.tnze.msctf.windows.win32.foundation.RECT;
import tech.tnze.msctf.windows.win32.system.com.IUnknown;
import tech.tnze.msctf.windows.win32.ui.textservices.*;

import static tech.tnze.msctf.WindowsException.checkResult;
import static tech.tnze.msctf.windows.win32.foundation.Apis.SysFreeString;
import static tech.tnze.msctf.windows.win32.foundation.Apis.SysStringLen;
import static tech.tnze.msctf.windows.win32.foundation.Constants.E_NOINTERFACE;
import static tech.tnze.msctf.windows.win32.ui.textservices.Constants.TF_DEFAULT_SELECTION;
import static tech.tnze.msctf.windows.win32.ui.textservices.TF_CONTEXT_EDIT_CONTEXT_FLAGS.TF_ES_ASYNCDONTCARE;
import static tech.tnze.msctf.windows.win32.ui.textservices.TF_CONTEXT_EDIT_CONTEXT_FLAGS.TF_ES_READ;

public class CandidateList implements Renderable {

    private final Minecraft minecraft;
    private final int[] paddings = new int[]{3, 3, 2, 2};
    private final int screenMargin = 5;
    int totalCount, pageCount, currentPage, currentSelection;
    private MutableComponent displayComponent = Component.empty();
    private final Style selectedItemStyle = Style.EMPTY.withColor(0xFFFFFF00).withUnderlined(true);
    private int anchorX = 0, anchorY = 0;

    CandidateList(Minecraft minecraft) {
        this.minecraft = minecraft;
        totalCount = 0;
        pageCount = 0;
        currentPage = 0;
        currentSelection = 0;
    }

    public void onStart(ITfCandidateListUIElement elem) {
        getPosition(elem);
    }

    public void onUpdate(ITfCandidateListUIElement elem) {
        try (var arena = Arena.ofConfined()) {
            var countHolder = arena.allocate(JAVA_INT);
            checkResult(elem.GetCount(countHolder));
            int count = countHolder.get(JAVA_INT, 0);

            var pageCountHolder = arena.allocate(JAVA_INT);
            checkResult(elem.GetPageIndex(MemorySegment.NULL, 0, pageCountHolder));
            int pageCount = pageCountHolder.get(JAVA_INT, 0);

            var pageIndexHolder = arena.allocate(JAVA_INT, pageCount);
            checkResult(elem.GetPageIndex(pageIndexHolder, pageCount, pageCountHolder));
            var pageIndex = pageIndexHolder.toArray(JAVA_INT);

            var currentPageHolder = arena.allocate(JAVA_INT);
            checkResult(elem.GetCurrentPage(currentPageHolder));
            int currentPage = currentPageHolder.get(JAVA_INT, 0);

            int currentPageStart = pageIndex[currentPage];
            int currentPageEnd = currentPage + 1 < pageIndex.length ? pageIndex[currentPage + 1] : count;

            var currentPageContents = new String[currentPageEnd - currentPageStart];
            var candidateWordHolder = arena.allocate(ADDRESS);
            for (int i = currentPageStart; i < currentPageEnd; i++) {
                checkResult(elem.GetString(i, candidateWordHolder));
                var candidateWord = candidateWordHolder.get(ADDRESS, 0);
                int candidateWordLen = SysStringLen(candidateWord);
                var chars = candidateWord.reinterpret(candidateWordLen * JAVA_CHAR.byteSize()).toArray(JAVA_CHAR);
                currentPageContents[i - currentPageStart] = String.valueOf(chars);
                SysFreeString(candidateWord);
            }

            var currentSelectionHolder = arena.allocate(JAVA_INT);
            checkResult(elem.GetSelection(currentSelectionHolder));
            int currentSelection = currentSelectionHolder.get(JAVA_INT, 0);

            setState(count, pageCount, currentPage, currentPageContents, currentSelection - currentPageStart);
        }
        getPosition(elem);
    }

    public void onEnd(ITfCandidateListUIElement uiElement) {
    }

    private void getPosition(ITfCandidateListUIElement uiElement) {
        try (var arena = Arena.ofConfined()) {
            var documentMgrHolder = arena.allocate(ADDRESS.withTargetLayout(ITfDocumentMgr.addressLayout()));
            checkResult(uiElement.GetDocumentMgr(documentMgrHolder));

            var documentMgrPtr = documentMgrHolder.get(ITfDocumentMgr.addressLayout(), 0);
            var documentMgr = ITfDocumentMgr.wrap(documentMgrPtr);

            try {
                var contextHolder = arena.allocate(ADDRESS.withTargetLayout(ITfContext.addressLayout()));
                checkResult(documentMgr.GetTop(contextHolder));

                var contextPtr = contextHolder.get(ITfContext.addressLayout(), 0);
                var context = ITfContext.wrap(contextPtr);

                try {
                    var contextViewHolder = arena.allocate(ITfContextView.addressLayout());
                    checkResult(context.GetActiveView(contextViewHolder));

                    var contextViewPtr = contextViewHolder.get(ITfContextView.addressLayout(), 0);
                    var contextView = ITfContextView.wrap(contextViewPtr);

                    try {
                        var session = new GetTextExtSession(context, contextView) {
                            @Override
                            public int DoEditSession(int ec) {
                                var pSelection = this.arena.allocate(TF_SELECTION.layout(), 1);
                                var pcFetched = this.arena.allocate(JAVA_LONG);
                                int hr = this.context.GetSelection(ec, TF_DEFAULT_SELECTION, 1, pSelection, pcFetched);
                                if (hr < 0) return hr;

                                var range = TF_SELECTION.range(pSelection);
                                try {
                                    var rectHolder = this.arena.allocate(RECT.layout());
                                    var clippedHolder = this.arena.allocate(JAVA_BOOLEAN);
                                    hr = this.contextView.GetTextExt(ec, range, rectHolder, clippedHolder);
                                    if (hr < 0) return hr;

                                    updateAnchor(ScreenRect.fromRect(rectHolder).toGuiRect(minecraft.getWindow()));
                                } finally {
                                    ITfRange.wrap(range).Release();
                                }
                                return 0;
                            }
                        };

                        try {
                            var phrSessionHolder = arena.allocate(JAVA_INT);
                            checkResult(context.RequestEditSession(Manager.getInstance().getClientId(), session.getPointer(), TF_ES_ASYNCDONTCARE | TF_ES_READ, phrSessionHolder));
                            checkResult(phrSessionHolder.get(JAVA_INT, 0));
                        } finally {
                            session.Release();
                        }
                    } finally {
                        contextView.Release();
                    }
                } finally {
                    context.Release();
                }
            } finally {
                documentMgr.Release();
            }
        }
    }

    private void updateAnchor(Rect2i rect) {
        anchorX = rect.getX();
        anchorY = rect.getY();
    }

    private record ScreenRect(int left, int top, int right, int bottom ) {
        static ScreenRect fromRect(MemorySegment segment) {
            return new ScreenRect(RECT.left(segment), RECT.top(segment), RECT.right(segment), RECT.bottom(segment));
        }

        Rect2i toGuiRect(Window window) {
            var s = window.getGuiScale();
            int x = (left - window.getX()) / s;
            int y = (top - window.getY()) / s;
            int w = (right - left) / s;
            int h = (bottom - top) / s;
            return new Rect2i(x, y, w, h);
        }
    }

    private static abstract class GetTextExtSession implements ITfEditSession {
        protected Arena arena = Arena.ofConfined();
        private final MemorySegment pointer;
        private int refCount = 1;

        protected final ITfContext context;
        protected final ITfContextView contextView;


        GetTextExtSession(ITfContext context, ITfContextView contextView) {
            pointer = ITfEditSession.create(this, arena);
            this.context = context;
            this.context.AddRef();
            this.contextView = contextView;
            this.contextView.AddRef();
        }

        @Override
        public int QueryInterface(MemorySegment riid, MemorySegment ppvObject) {
            if (ComObject.equalIIDs(riid, IUnknown.iid()) || ComObject.equalIIDs(riid, ITfEditSession.iid())) {
                ppvObject.set(ADDRESS, 0, pointer);
                return 0;
            }
            return E_NOINTERFACE;
        }

        @Override
        public int AddRef() {
            return ++refCount;
        }

        @Override
        public int Release() {
            if (--refCount == 0) {
                context.Release();
                contextView.Release();
                arena.close();
                arena = null;
            }
            return refCount;
        }

        public MemorySegment getPointer() {
            return pointer;
        }
    }

    private void setState(int totalCount, int pageCount, int currentPage, String[] currentPageContent, int currentSelection) {
        this.totalCount = totalCount;
        this.pageCount = pageCount;
        this.currentPage = currentPage;
        this.currentSelection = currentSelection;

        MutableComponent list = Component.empty();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentPageContent.length; i++) {
            if (i != 0) {
                list.append(" ");
            }

            sb.setLength(0);
            sb.append(i + 1);
            sb.append(".");
            sb.append(currentPageContent[i]);

            MutableComponent item = Component.literal(sb.toString());
            if (i == currentSelection) {
                item.withStyle(selectedItemStyle);
            }
            list.append(item);
        }
        displayComponent = list;
    }

    private Rect2i relocate(int width, int height, int guiWidth, int guiHeight) {
        Rect2i rect = new Rect2i(anchorX, anchorY, width, height);

        if (guiHeight - anchorY < 30) { // The text box is on the bottom of screen
            rect.setY(rect.getY() - height - 6);
        } else {
            rect.setY(rect.getY() + height + 6);
        }

        if (width >= guiWidth || height >= guiHeight) {
            // too large for relocation
            return rect;
        }
        if (rect.getX() < screenMargin) {
            rect.setX(screenMargin);
        } else if (rect.getX() + rect.getWidth() > guiWidth - screenMargin) {
            rect.setX(guiWidth - screenMargin - width);
        }
        if (rect.getY() < screenMargin) {
            rect.setY(screenMargin);
        } else if (rect.getY() + rect.getHeight() > guiHeight - screenMargin) {
            rect.setY(guiHeight - screenMargin - height);
        }
        return rect;
    }

    @Override
    public void render(@NonNull GuiGraphics gg, int mouseX, int mouseY, float delta) {
        Font font = minecraft.font;
        Rect2i rect = relocate(font.width(displayComponent), font.lineHeight, gg.guiWidth(), gg.guiHeight());

        renderBackground(gg, rect);
        gg.drawString(font, displayComponent, rect.getX(), rect.getY(), 0xFFE0E0E0);
    }

    private void renderBackground(@NonNull GuiGraphics gg, Rect2i bounds) {
        gg.fill(
                bounds.getX() - paddings[0],
                bounds.getY() - paddings[1],
                bounds.getX() + bounds.getWidth() + paddings[2],
                bounds.getY() + bounds.getHeight() + paddings[3],
                minecraft.options.getBackgroundColor(Integer.MIN_VALUE)
        );
    }
}
