package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public class ClickGuiScreen extends Screen {

    private int panelX = 40;
    private int panelY = 30;
    private final int panelWidth = 175;
    private final int headerHeight = 22;
    private final int rowHeight = 18;

    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean panelCollapsed = false;
    private boolean hudExpanded = false;

    // Bảng màu chuẩn theo mẫu giao diện BASEFINDING
    private static final int HEADER_BG = new Color(36, 40, 52, 248).getRGB();
    private static final int PANEL_BG = new Color(46, 51, 66, 242).getRGB();
    private static final int ROW_HOVER_BG = new Color(54, 60, 77, 245).getRGB();
    private static final int SUB_BG = new Color(38, 42, 54, 245).getRGB();
    private static final int BORDER_COLOR = new Color(68, 75, 94, 255).getRGB();

    private static final int TEXT_HEADER = new Color(255, 255, 255, 255).getRGB();
    private static final int TEXT_OFF = new Color(195, 200, 212, 255).getRGB();
    private static final int TEXT_ON = new Color(128, 255, 85, 255).getRGB();
    private static final int NEON_GREEN = new Color(115, 255, 75, 255).getRGB();
    private static final int ICON_DARK_GREEN = new Color(25, 75, 45, 255).getRGB();

    private static final int SWITCH_OFF_BG = new Color(40, 44, 58, 255).getRGB();
    private static final int SWITCH_ON_BG = new Color(128, 255, 85, 255).getRGB();
    private static final int KNOB_COLOR = new Color(255, 255, 255, 255).getRGB();

    public ClickGuiScreen() {
        super(Component.literal("SimpleHUD ClickGUI"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int currentY = panelY;

        // 1. Vẽ thanh tiêu đề (Header) kèm biểu tượng Radar xanh lá và nút thu gọn (-)
        graphics.fill(panelX, currentY, panelX + panelWidth, currentY + headerHeight, HEADER_BG);
        graphics.fill(panelX, currentY + headerHeight - 1, panelX + panelWidth, currentY + headerHeight, BORDER_COLOR);

        drawRadarIcon(graphics, panelX + 7, currentY + 5);
        graphics.drawString(this.font, SimpleHUDClient.styledText("SIMPLEHUD"), panelX + 23, currentY + 7, TEXT_HEADER, true);

        String collapseSymbol = panelCollapsed ? "+" : "-";
        graphics.drawString(this.font, SimpleHUDClient.styledText(collapseSymbol), panelX + panelWidth - 13, currentY + 7, TEXT_OFF, true);

        currentY += headerHeight;

        // 2. Vẽ danh sách các Module nếu bảng đang mở
        if (!panelCollapsed) {
            for (SimpleModule mod : SimpleHUDClient.MODULES) {
                boolean enabled = mod.isEnabled();
                boolean hovered = isHovered(mouseX, mouseY, panelX, currentY, panelWidth, rowHeight);

                graphics.fill(panelX, currentY, panelX + panelWidth, currentY + rowHeight, hovered ? ROW_HOVER_BG : PANEL_BG);

                // Thanh dọc màu xanh lá neon sát mép trái khi BẬT
                if (enabled) {
                    graphics.fill(panelX, currentY + 1, panelX + 3, currentY + rowHeight - 1, NEON_GREEN);
                }

                String label = mod.getName().toUpperCase();
                if (mod == SimpleHUDClient.MASTER_HUD) {
                    label += hudExpanded ? " [-]" : " [+]";
                }
                graphics.drawString(this.font, SimpleHUDClient.styledText(label), panelX + 8, currentY + 5, enabled ? TEXT_ON : TEXT_OFF, true);

                // Nút gạt viên thuốc bên phải
                drawToggleSwitch(graphics, panelX + panelWidth - 28, currentY + 4, enabled);

                currentY += rowHeight;

                // 3. Menu cài đặt con của HUD
                if (mod == SimpleHUDClient.MASTER_HUD && hudExpanded) {
                    for (SimpleModule sub : SimpleHUDClient.HUD_ELEMENTS) {
                        boolean subEnabled = sub.isEnabled();
                        boolean subHovered = isHovered(mouseX, mouseY, panelX, currentY, panelWidth, rowHeight);

                        graphics.fill(panelX, currentY, panelX + panelWidth, currentY + rowHeight, subHovered ? ROW_HOVER_BG : SUB_BG);
                        if (subEnabled) {
                            graphics.fill(panelX + 4, currentY + 2, panelX + 6, currentY + rowHeight - 2, NEON_GREEN);
                        }

                        graphics.drawString(this.font, SimpleHUDClient.styledText(sub.getName().toUpperCase()), panelX + 13, currentY + 5, subEnabled ? TEXT_ON : TEXT_OFF, true);
                        drawToggleSwitch(graphics, panelX + panelWidth - 28, currentY + 4, subEnabled);
                        currentY += rowHeight;
                    }

                    // Thanh chỉnh Scale
                    graphics.fill(panelX, currentY, panelX + panelWidth, currentY + rowHeight, SUB_BG);
                    String scaleStr = String.format(java.util.Locale.US, "SCALE: %.2fX", SimpleHUDClient.hudScale);
                    graphics.drawString(this.font, SimpleHUDClient.styledText(scaleStr), panelX + 13, currentY + 5, TEXT_OFF, true);
                    graphics.drawString(this.font, SimpleHUDClient.styledText("[-]  [+]"), panelX + panelWidth - 44, currentY + 5, TEXT_ON, true);
                    currentY += rowHeight;

                    // Thanh chỉnh Radius
                    graphics.fill(panelX, currentY, panelX + panelWidth, currentY + rowHeight, SUB_BG);
                    String radStr = "RADIUS: " + SimpleHUDClient.cornerRadius + "PX";
                    graphics.drawString(this.font, SimpleHUDClient.styledText(radStr), panelX + 13, currentY + 5, TEXT_OFF, true);
                    graphics.drawString(this.font, SimpleHUDClient.styledText("[-]  [+]"), panelX + panelWidth - 44, currentY + 5, TEXT_ON, true);
                    currentY += rowHeight;
                }
            }
        }

        // Vẽ viền mảnh bao quanh toàn bộ khung
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, BORDER_COLOR);
        graphics.fill(panelX, currentY - 1, panelX + panelWidth, currentY, BORDER_COLOR);
        graphics.fill(panelX, panelY, panelX + 1, currentY, BORDER_COLOR);
        graphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, currentY, BORDER_COLOR);

        super.render(graphics, mouseX, mouseY, delta);
    }
    private void drawRadarIcon(GuiGraphics graphics, int x, int y) {
        // Vẽ huy hiệu tròn xanh lá ở góc trái Header giống ảnh mẫu
        graphics.fill(x + 3, y, x + 9, y + 12, ICON_DARK_GREEN);
        graphics.fill(x, y + 3, x + 12, y + 9, ICON_DARK_GREEN);
        graphics.fill(x + 1, y + 1, x + 11, y + 11, ICON_DARK_GREEN);

        // Vòng sáng xanh lá bên ngoài và tâm radar
        graphics.fill(x + 3, y + 1, x + 9, y + 2, NEON_GREEN);
        graphics.fill(x + 3, y + 10, x + 9, y + 11, NEON_GREEN);
        graphics.fill(x + 1, y + 3, x + 2, y + 9, NEON_GREEN);
        graphics.fill(x + 10, y + 3, x + 11, y + 9, NEON_GREEN);
        graphics.fill(x + 4, y + 4, x + 8, y + 8, NEON_GREEN);
        graphics.fill(x + 5, y + 5, x + 7, y + 7, ICON_DARK_GREEN);
    }

    private void drawToggleSwitch(GuiGraphics graphics, int x, int y, boolean enabled) {
        int width = 20;
        int height = 9;
        int bg = enabled ? SWITCH_ON_BG : SWITCH_OFF_BG;

        // Vẽ thân viên thuốc bo tròn
        graphics.fill(x + 2, y, x + width - 2, y + height, bg);
        graphics.fill(x, y + 2, x + width, y + height - 2, bg);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, bg);

        // Vẽ chấm tròn trắng
        int knobX = enabled ? (x + width - 8) : (x + 1);
        int knobY = y + 1;
        graphics.fill(knobX + 1, knobY, knobX + 6, knobY + 7, KNOB_COLOR);
        graphics.fill(knobX, knobY + 1, knobX + 7, knobY + 6, KNOB_COLOR);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        // Xử lý bấm vào thanh tiêu đề (Header)
        if (isHovered(mouseX, mouseY, panelX, panelY, panelWidth, headerHeight)) {
            if (button == 1 || (button == 0 && mouseX >= panelX + panelWidth - 20)) {
                panelCollapsed = !panelCollapsed;
                return true;
            } else if (button == 0) {
                isDragging = true;
                dragOffsetX = (int) mouseX - panelX;
                dragOffsetY = (int) mouseY - panelY;
                return true;
            }
        }

        if (panelCollapsed) {
            return super.mouseClicked(event, doubleClick);
        }

        int currentY = panelY + headerHeight;

        for (SimpleModule mod : SimpleHUDClient.MODULES) {
            if (isHovered(mouseX, mouseY, panelX, currentY, panelWidth, rowHeight)) {
                if (button == 0) {
                    mod.toggle();
                    return true;
                } else if (button == 1 && mod == SimpleHUDClient.MASTER_HUD) {
                    hudExpanded = !hudExpanded;
                    return true;
                }
            }
            currentY += rowHeight;

            if (mod == SimpleHUDClient.MASTER_HUD && hudExpanded) {
                for (SimpleModule sub : SimpleHUDClient.HUD_ELEMENTS) {
                    if (button == 0 && isHovered(mouseX, mouseY, panelX, currentY, panelWidth, rowHeight)) {
                        sub.toggle();
                        return true;
                    }
                    currentY += rowHeight;
                }

                // Bấm chỉnh Scale
                if (button == 0 && isHovered(mouseX, mouseY, panelX, currentY, panelWidth, rowHeight)) {
                    if (mouseX >= panelX + panelWidth - 46 && mouseX <= panelX + panelWidth - 24) {
                        SimpleHUDClient.hudScale = Math.max(0.5f, Math.round((SimpleHUDClient.hudScale - 0.05f) * 100.0f) / 100.0f);
                        return true;
                    } else if (mouseX > panelX + panelWidth - 24) {
                        SimpleHUDClient.hudScale = Math.min(2.0f, Math.round((SimpleHUDClient.hudScale + 0.05f) * 100.0f) / 100.0f);
                        return true;
                    }
                }
                currentY += rowHeight;

                // Bấm chỉnh Radius
                if (button == 0 && isHovered(mouseX, mouseY, panelX, currentY, panelWidth, rowHeight)) {
                    if (mouseX >= panelX + panelWidth - 46 && mouseX <= panelX + panelWidth - 24) {
                        SimpleHUDClient.cornerRadius = Math.max(0, SimpleHUDClient.cornerRadius - 1);
                        return true;
                    } else if (mouseX > panelX + panelWidth - 24) {
                        SimpleHUDClient.cornerRadius = Math.min(8, SimpleHUDClient.cornerRadius + 1);
                        return true;
                    }
                }
                currentY += rowHeight;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (isDragging && event.button() == 0) {
            panelX = (int) event.x() - dragOffsetX;
            panelY = (int) event.y() - dragOffsetY;
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            isDragging = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        SimpleHUDClient.saveConfig();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
