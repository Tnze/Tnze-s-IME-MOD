package tech.tnze.client.uielement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.Rect2i;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NonNull;

public class CandidateList implements Renderable {

    private final Minecraft minecraft;
    private final int[] paddings = new int[]{3, 3, 2, 2};
    private final int screenMargin = 5;
    int totalCount, pageCount, currentPage, currentSelection;
    private MutableComponent displayComponent = Component.empty();
    private final Style selectedItemStyle = Style.EMPTY.withColor(0xFFFFFF00).withUnderlined(true);
    private int anchorX, anchorY;
    private String compositionText;

    CandidateList(Minecraft minecraft) {
        this.minecraft = minecraft;
        totalCount = 0;
        pageCount = 0;
        currentPage = 0;
        currentSelection = 0;
    }

    public void setState(int totalCount, int pageCount, int currentPage, String[] currentPageContent, int currentSelection) {
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

    public void setAnchor(int x, int y) {
        anchorX = x;
        anchorY = y;
    }

    public void setComposition(String text) {
        compositionText = text;
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

        var bounds = new Rect2i(rect.getX(), rect.getY() - paddings[0] - font.lineHeight, font.width(compositionText), font.lineHeight);
        gg.fill(
                bounds.getX() - paddings[0],
                bounds.getY() - paddings[1],
                bounds.getX() + bounds.getWidth() + paddings[2],
                bounds.getY() + bounds.getHeight(),
                minecraft.options.getBackgroundColor(Integer.MIN_VALUE)
        );
        gg.drawString(font, compositionText, bounds.getX(), bounds.getY(), 0xFFE0E0E0);

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
