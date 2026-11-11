package com.example.simplehud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {
    // Tọa độ và kích thước của khung cửa sổ ClickGUI
    private int panelX = 40;
    private int panelY = 40;
    private final int panelWidth = 130;
    private final int headerHeight = 18;
    private final int itemHeight = 16;

    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public ClickGuiScreen() {
        super(Text.literal("SimpleHUD ClickGUI"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Cập nhật vị trí nếu người dùng đang kéo thanh tiêu đề
        if (isDragging) {
            panelX = mouseX - dragOffsetX;
            panelY = mouseY - dragOffsetY;
        }

        // Vẽ lớp nền tối mờ phía sau màn hình
        context.fill(0, 0, this.width, this.height, 0x60000000);

        // 1. Vẽ thanh tiêu đề (Header) của khung Category
        context.fill(panelX, panelY, panelX + panelWidth, panelY + headerHeight, 0xFF1E1E2E);
        context.fill(panelX, panelY + headerHeight - 1, panelX + panelWidth, panelY + headerHeight, 0xFF55FFFF);
        context.drawTextWithShadow(this.textRenderer, "Tiện ích (Render & Misc)", panelX + 6, panelY + 5, 0xFFFFFFFF);

        // 2. Vẽ danh sách các nút tính năng (Module Buttons)
        int currentY = panelY + headerHeight;
        for (SimpleModule module : SimpleHudClient.MODULES) {
            boolean isHovered = mouseX >= panelX && mouseX <= panelX + panelWidth
                    && mouseY >= currentY && mouseY <= currentY + itemHeight;

            // Đổi màu nền nút tùy theo trạng thái Bật / Tắt / Di chuột vào
            int bgColor;
            if (module.isEnabled()) {
                bgColor = isHovered ? 0xDD2A7B88 : 0xDD1F5F6B; // Xanh ngọc tối khi đang bật
            } else {
                bgColor = isHovered ? 0xDD383848 : 0xDD252532; // Xám tối khi đang tắt
            }

            context.fill(panelX, currentY, panelX + panelWidth, currentY + itemHeight, bgColor);

            // Vẽ tên tính năng và chữ ON/OFF ở bên phải
            int textColor = module.isEnabled() ? 0xFF55FFFF : 0xFFAAAAAA;
            context.drawTextWithShadow(this.textRenderer, module.getName(), panelX + 6, currentY + 4, textColor);

            String status = module.isEnabled() ? "ON" : "OFF";
            int statusWidth = this.textRenderer.getWidth(status);
            context.drawTextWithShadow(this.textRenderer, status, panelX + panelWidth - statusWidth - 6, currentY + 4, textColor);

            currentY += itemHeight;
        }

        // Vẽ khung viền bao quanh toàn bộ bảng điều khiển
        int totalHeight = headerHeight + (SimpleHudClient.MODULES.size() * itemHeight);
        context.drawBorder(panelX, panelY, panelWidth, totalHeight, 0xFF45475A);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Chuột trái
            // Kiểm tra bấm vào thanh tiêu đề để kéo cửa sổ
            if (mouseX >= panelX && mouseX <= panelX + panelWidth
                    && mouseY >= panelY && mouseY <= panelY + headerHeight) {
                isDragging = true;
                dragOffsetX = (int) mouseX - panelX;
                dragOffsetY = (int) mouseY - panelY;
                return true;
            }

            // Kiểm tra bấm vào từng tính năng để bật/tắt
            int currentY = panelY + headerHeight;
            for (SimpleModule module : SimpleHudClient.MODULES) {
                if (mouseX >= panelX && mouseX <= panelX + panelWidth
                        && mouseY >= currentY && mouseY <= currentY + itemHeight) {
                    module.toggle();
                    return true;
                }
                currentY += itemHeight;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false; // Không làm đứng hình game khi mở menu
    }
}
