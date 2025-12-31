package tech.tnze.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import tech.tnze.msctf.*;

import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.client.IMEClient.mUIElementManager;

import java.lang.ref.Cleaner;
import java.util.*;

public class Manager implements UIElementSink, ContextOwnerCompositionSink { // TODO: Implement ItfContextOwner and ITfTransitoryExtensionSink
    public static final TreeMap<Integer, Renderable> uiElements = new TreeMap<>();
    public static final Cleaner mDocumentCleaner = Cleaner.create();

    private int editCookie = 0;

    private static Manager instance = null;

    public synchronized static Manager getInstance() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    private Manager() {
    }

    @Override
    public boolean begin(int uiElementId) {
        LOGGER.debug("BeginUIElement: ID={}", uiElementId);
        try (UIElement element = mUIElementManager.getUIElement(uiElementId)) {

            CandidateListUIElement candidateList = element.intoCandidateListUIElement();
            if (candidateList != null) {
                try (candidateList) {
                    synchronized (uiElements) {
                        Renderable pre = uiElements.put(uiElementId, new CandidateList(Minecraft.getInstance()));
                        if (pre != null) {
                            LOGGER.warn("Duplicate UIElement registered, what happened?");
                        }
                    }
                }
                return false;
            }

            ToolTipUIElement toolTip = element.intoToolTipUIElement();
            if (toolTip != null) {
                try (toolTip) {
                    synchronized (uiElements) {
                        LOGGER.info("Add ToolTip {}", uiElementId);
                        // TODO: Create UI
                    }
                }
                return false;
            }

            TransitoryExtensionUIElement transitoryExtension = element.intoTransitoryExtensionUIElement();
            if (transitoryExtension != null) {
                try (transitoryExtension) {
                    synchronized (uiElements) {
                        LOGGER.info("Add TransitoryExtension {}", uiElementId);
                        // TODO: Create UI
                    }
                }
                return false;
            }

            LOGGER.warn("Unknown UIElement: DESC={}, GUID={}", element.getDescription(), element.getGUID());
        } catch (Exception e) {
            LOGGER.error("Failed to get UIElement {} on BeginUIElement", uiElementId);
        }
        return true;
    }

    @Override
    public void update(int uiElementId) {
        LOGGER.debug("UIElement UPDATE: ID={}", uiElementId);
        try (UIElement element = mUIElementManager.getUIElement(uiElementId)) {

            CandidateListUIElement candidateList = element.intoCandidateListUIElement();
            if (candidateList != null) {
                try (candidateList) {
                    int totalCount = candidateList.getCount();
                    int currentPage = candidateList.getCurrentPage();
                    int currentSelection = candidateList.getSelection();
                    int[] indexes = new int[candidateList.getPageIndex(null)];
                    int pageCount = candidateList.getPageIndex(indexes);
                    LOGGER.debug("Candidate count={} [{}]{}", totalCount, pageCount, indexes);

                    int currPageStart = indexes[currentPage];
                    int currPageEnd = (currentPage + 1) < pageCount ? indexes[currentPage + 1] : totalCount;
                    int currPageSize = currPageEnd - currPageStart;
                    String[] currentPageContent = new String[currPageSize];
                    for (int i = 0; i < currPageSize; i++) {
                        currentPageContent[i] = candidateList.getString(currPageStart + i);
                    }

                    synchronized (uiElements) {
                        Renderable list = uiElements.get(uiElementId);
                        if (list == null) {
                            LOGGER.warn("Updating an unexist CandidateList");
                            return;
                        }
                        if (list instanceof CandidateList) {
                            ((CandidateList) list).setState(totalCount, pageCount, currentPage, currentPageContent, currentSelection - currPageStart);
                        } else {
                            LOGGER.warn("Not updating a CandidateList, element id underlying type changed?");
                        }
                    }
                }
                return;
            }

            ToolTipUIElement toolTip = element.intoToolTipUIElement();
            if (toolTip != null) {
                try (toolTip) {
                    LOGGER.info("ToolTip: {}", toolTip.getString());
                }
                return;
            }


            TransitoryExtensionUIElement transitoryExtension = element.intoTransitoryExtensionUIElement();
            if (transitoryExtension != null) {
                try (transitoryExtension) {
                    LOGGER.info("TransitoryExtension {}", uiElementId);
                }
                return;
            }

            LOGGER.warn("Unknown UIElement DESC={}, GUID={}", element.getDescription(), element.getGUID());
        } catch (Exception e) {
            LOGGER.error("Failed to get UIElement {} on UpdateUIElement: {}", uiElementId, e);
        }
    }

    @Override
    public void end(int uiElementId) {
        LOGGER.debug("EndUIElement: ID={}", uiElementId);
        synchronized (uiElements) {
            uiElements.remove(uiElementId);
        }
    }

    public void setEditCookie(int cookie) {
        editCookie = cookie;
    }

    @Override
    public boolean onStartComposition(CompositionView composition) {
        LOGGER.info("Start composition");
        return true;
    }

    @Override
    public void onUpdateComposition(CompositionView composition, Range range) {
        LOGGER.info("Update composition: {}", range != null ? range.getText(editCookie) : null);
    }

    @Override
    public void onEndComposition(CompositionView composition) {
        LOGGER.info("End composition: {}", composition.getOwnerClsid());
        try (Range range = composition.getRange()) {
            LOGGER.info("End composition: {}", range);
//            LOGGER.info("End composition: {}", range.getText(editCookie));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}