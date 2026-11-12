package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ClickGuiScreen extends Screen {
    private int panelX = 40;
    private int panelY = 40;
    private final int panelWidth = 145;
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
                bgColor = isHovered ? 0xDD2A7D80 : 0xDD1F5F60;
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

        // 3. Hàng chỉnh Kích thước (Scale)
        drawAdjustRow(graphics, mouseX, mouseY, currentY, "Scale: " + SimpleHUDClient.hudScale + "x");
        currentY += itemHeight;

        // 4. Hàng chỉnh Độ bo góc (Radius)
        drawAdjustRow(graphics, mouseX, mouseY, currentY, "Bo góc: " + SimpleHUDClient.cornerRadius + "px");
        currentY += itemHeight;

        // Vẽ khung viền bao quanh toàn bộ bảng (bao gồm cả 2 hàng chỉnh mới)
        int totalHeight = headerHeight + ((SimpleHUDClient.MODULES.size() + 2) * itemHeight);
        graphics.renderOutline(panelX, panelY, panelWidth, totalHeight, 0xFF45475A);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawAdjustRow(GuiGraphics graphics, int mouseX, int mouseY, int y, String label) {
        graphics.fill(panelX, y, panelX + panelWidth, y + itemHeight, 0xDD1E1E2A);
        graphics.drawString(this.font, label, panelX + 6, y + 4, 0xFFFFD700);

        // Vùng nút [-] và [+] ở bên phải
        int plusX = panelX + panelWidth - 18;
        int minusX = plusX - 18;

        boolean hoverMinus = mouseX >= minusX && mouseX < plusX && mouseY >= y && mouseY <= y + itemHeight;
        boolean hoverPlus = mouseX >= plusX && mouseX <= panelX + panelWidth && mouseY >= y && mouseY <= y + itemHeight;

        graphics.fill(minusX, y + 1, plusX - 1, y + itemHeight - 1, hoverMinus ? 0xFF45475A : 0xFF2A2A3A);
        graphics.drawString(this.font, "-", minusX + 6, y + 4, 0xFFFF77AA);

        graphics.fill(plusX, y + 1, panelX + panelWidth - 2, y + itemHeight - 1, hoverPlus ? 0xFF45475A : 0xFF2A2A3A);
        graphics.drawString(this.font, "+", plusX + 5, y + 4, 0xFF55FFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();

            // Kéo thả thanh tiêu đề
            if (mouseX >= panelX && mouseX <= panelX + panelWidth
                    && mouseY >= panelY && mouseY <= panelY + headerHeight) {
                isDragging = true;
                dragOffsetX = (int) mouseX - panelX;
                dragOffsetY = (int) mouseY - panelY;
                return true;
            }

            // Bật/tắt các tính năng
            int currentY = panelY + headerHeight;
            for (SimpleModule module : SimpleHUDClient.MODULES) {
                if (mouseX >= panelX && mouseX <= panelX + panelWidth
                        && mouseY >= currentY && mouseY <= currentY + itemHeight) {
                    module.toggle();
                    return true;
                }
                currentY += itemHeight;
            }

            int plusX = panelX + panelWidth - 18;
            int minusX = plusX - 18;

            // Xử lý bấm [-] hoặc [+] cho Kích thước (Scale)
            if (mouseY >= currentY && mouseY <= currentY + itemHeight) {
                if (mouseX >= minusX && mouseX < plusX) {
                    SimpleHUDClient.hudScale = Math.max(0.5f, Math.round((SimpleHUDClient.hudScale - 0.1f) * 10f) / 10f);
                    return true;
                } else if (mouseX >= plusX && mouseX <= panelX + panelWidth) {
                    SimpleHUDClient.hudScale = Math.min(2.0f, Math.round((SimpleHUDClient.hudScale + 0.1f) * 10f) / 10f);
                    return true;
                }
            }
            currentY += itemHeight;

            // Xử lý bấm [-] hoặc [+] cho Độ bo góc (Radius)
            if (mouseY >= currentY && mouseY <= currentY + itemHeight) {
                if (mouseX >= minusX && mouseX < plusX) {
                    SimpleHUDClient.cornerRadius = Math.max(0, SimpleHUDClient.cornerRadius - 1);
                    return true;
                } else if (mouseX >= plusX && mouseX <= panelX + panelWidth) {
                    SimpleHUDClient.cornerRadius = Math.min(8, SimpleHUDClient.cornerRadius + 1);
                    return true;
                }
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
