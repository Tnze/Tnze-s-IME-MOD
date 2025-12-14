package tech.tnze.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.Rect2i;

import org.jspecify.annotations.NonNull;

public class CandidateList implements Renderable {

    private final Minecraft minecraft;
    private final int[] paddings = new int[]{2, 2, 1, 1};
    private final int screenMargin = 4;
    int totalCount, pageCount, currentPage, currentSelection;
    private String[] currentPageContent = new String[0];

    int anchorX, anchorY;

    CandidateList(Minecraft minecraft) {
        this.minecraft = minecraft;
        totalCount = 0;
        pageCount = 0;
        currentPage = 0;
        currentSelection = 0;

        setAnchor(50, 50);
    }

    public void setState(int totalCount, int pageCount, int currentPage, String[] currentPageContent, int currentSelection) {
        this.totalCount = totalCount;
        this.pageCount = pageCount;
        this.currentPage = currentPage;
        this.currentPageContent = currentPageContent;
        this.currentSelection = currentSelection;

        for (int i = 0; i < currentPageContent.length; i++) {
            currentPageContent[i] = (i + 1) + ". " + currentPageContent[i];
        }
    }

    public void setAnchor(int x, int y) {
        anchorX = x;
        anchorY = y;
    }

    private Rect2i relocate(int width, int height, int guiWidth, int guiHeight) {
        Rect2i rect = new Rect2i(anchorX, anchorY, width, height);
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
        setAnchor(mouseX, mouseY); // TODO: Debug only

        // Calculate display size
        Font font = minecraft.font;
        int totalWidth = 0;
        for (String s : currentPageContent) {
            totalWidth += font.width(s) + 5;
        }

        Rect2i rect = relocate(totalWidth, font.lineHeight, gg.guiWidth(), gg.guiHeight());
        renderBackground(gg, rect);

        int offsetX = 0;
        for (int i = 0; i < currentPageContent.length; i++) {
            int width = font.width(currentPageContent[i]);
            gg.drawString(
                    font,
                    currentPageContent[i],
                    rect.getX() + offsetX,
                    rect.getY(),
                    currentSelection == i ? 0xFFFFFF00 : 0xFFE0E0E0
            );
            offsetX += width + 5;
        }
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
