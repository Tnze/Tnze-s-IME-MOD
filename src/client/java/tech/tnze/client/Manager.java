package tech.tnze.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import tech.tnze.msctf.CandidateListUIElement;
import tech.tnze.msctf.UIElement;
import tech.tnze.msctf.UIElementSink;

import static tech.tnze.client.IMEClient.LOGGER;
import static tech.tnze.client.IMEClient.mUIElementManager;

import java.util.*;

public class Manager implements UIElementSink {
    public static TreeMap<Integer, CandidateList> uiElements = new TreeMap<>();

    @Override
    public boolean begin(int uiElementId) {
        LOGGER.debug("BeginUIElement: ID={}", uiElementId);
        try (UIElement element = mUIElementManager.getUIElement(uiElementId);
             CandidateListUIElement candidateUI = element.intoCandidateListUIElement()) {
            if (candidateUI == null) {
                LOGGER.debug("Unknown element, allowing Text Service display its UI");
                return true;
            }
            Util.backgroundExecutor().execute(() -> {
                CandidateList pre = uiElements.put(uiElementId, new CandidateList(Minecraft.getInstance()));
                if (pre != null) {
                    LOGGER.warn("Duplicate UIElement registered, what happened?");
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to get UIElement {} on BeginUIElement", uiElementId);
        }
        return false;
    }

    @Override
    public void update(int uiElementId) {
        try (UIElement element = mUIElementManager.getUIElement(uiElementId)) {
            LOGGER.debug("UIElement UPDATE: ID={} DESC={} GUID={}", uiElementId, element.getDescription(), element.getGUID());
            try (CandidateListUIElement candidateUI = element.intoCandidateListUIElement()) {
                if (candidateUI == null) {
                    return;
                }
                int totalCount = candidateUI.getCount();
                int currentPage = candidateUI.getCurrentPage();
                int currentSelection = candidateUI.getSelection();
                int[] indexes = new int[candidateUI.getPageIndex(null)];
                int pageCount = candidateUI.getPageIndex(indexes);
                LOGGER.debug("Candidate count={} [{}]{}", totalCount, pageCount, indexes);

                int currPageStart = indexes[currentPage];
                int currPageEnd = (currentPage + 1) < pageCount ? indexes[currentPage + 1] : totalCount;
                int currPageSize = currPageEnd - currPageStart;
                String[] currentPageContent = new String[currPageSize];
                for (int i = 0; i < currPageSize; i++) {
                    currentPageContent[i] = candidateUI.getString(currPageStart + i);
                }

                Util.backgroundExecutor().execute(() -> {
                    CandidateList list = uiElements.get(uiElementId);
                    if (list == null) {
                        LOGGER.warn("Updating an unexist CandidateList");
                        return;
                    }
                    list.setState(totalCount, pageCount, currentPage, currentPageContent, currentSelection - currPageStart);
                });
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get UIElement {} on UpdateUIElement", uiElementId);
        }
    }

    @Override
    public void end(int uiElementId) {
        LOGGER.debug("EndUIElement: ID={}", uiElementId);
        Util.backgroundExecutor().execute(() -> uiElements.remove(uiElementId));
    }
}