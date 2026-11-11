package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ClickGuiScreen extends Screen {
    private int panelX = 40;
    private int panelY = 40;
    private final int panelWidth = 135;
    private final int headerHeight = 18;
    private final int itemHeight = 16;

    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public ClickGuiScreen() {
        super(Component.literal("SimpleHUD ClickGUI"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isDragging) {
            panelX = mouseX - dragOffsetX;
            panelY = mouseY - dragOffsetY;
        }

        // Vẽ nền mờ phía sau
        graphics.fill(0, 0, this.width, this.height, 0x60000000);

        // 1. Vẽ thanh tiêu đề (Header)
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + headerHeight, 0xFF1E1E2E);
        graphics.fill(panelX, panelY + headerHeight - 1, panelX + panelWidth, panelY + headerHeight, 0xFF55FFFF);
        graphics.drawString(this.font, "Tiện ích (Render & Misc)", panelX + 6, panelY + 5, 0xFFFFFFFF);

        // 2. Vẽ các nút tính năng
        int currentY = panelY + headerHeight;
        for (SimpleModule module : SimpleHUDClient.MODULES) {
            boolean isHovered = mouseX >= panelX && mouseX <= panelX + panelWidth
                    && mouseY >= currentY && mouseY <= currentY + itemHeight;

            int bgColor;
            if (module.isEnabled()) {
                bgColor = isHovered ? 0xDD2A7B88 : 0xDD1F5F6B;
            } else {
                bgColor = isHovered ? 0xDD383848 : 0xDD252532;
            }

            graphics.fill(panelX, currentY, panelX + panelWidth, currentY + itemHeight, bgColor);

            int textColor = module.isEnabled() ? 0xFF55FFFF : 0xFFAAAAAA;
            graphics.drawString(this.font, module.getName(), panelX + 6, currentY + 4, textColor);

            String status = module.isEnabled() ? "ON" : "OFF";
            int statusWidth = this.font.width(status);
            graphics.drawString(this.font, status, panelX + panelWidth - statusWidth - 6, currentY + 4, textColor);

            currentY += itemHeight;
        }

        // Vẽ khung viền bao quanh
        int totalHeight = headerHeight + (SimpleHUDClient.MODULES.size() * itemHeight);
        graphics.renderOutline(panelX, panelY, panelWidth, totalHeight, 0xFF45475A);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();

            if (mouseX >= panelX && mouseX <= panelX + panelWidth
                    && mouseY >= panelY && mouseY <= panelY + headerHeight) {
                isDragging = true;
                dragOffsetX = (int) mouseX - panelX;
                dragOffsetY = (int) mouseY - panelY;
                return true;
            }

            int currentY = panelY + headerHeight;
            for (SimpleModule module : SimpleHUDClient.MODULES) {
                if (mouseX >= panelX && mouseX <= panelX + panelWidth
                        && mouseY >= currentY && mouseY <= currentY + itemHeight) {
                    module.toggle();
                    return true;
                }
                currentY += itemHeight;
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            isDragging = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
