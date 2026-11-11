package com.example.simplehud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimpleHudClient implements ClientModInitializer {

    // Danh sách các tính năng hiển thị trong ClickGUI và HUD
    public static final List<SimpleModule> MODULES = new ArrayList<>();
    public static final SimpleModule WATERMARK = new SimpleModule("Watermark", true);
    public static final SimpleModule COORDINATES = new SimpleModule("Coordinates", true);
    public static final SimpleModule TOGGLE_SPRINT = new SimpleModule("ToggleSprint", false);
    public static final SimpleModule ARRAY_LIST = new SimpleModule("ModuleList", true);

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        MODULES.add(WATERMARK);
        MODULES.add(COORDINATES);
        MODULES.add(TOGGLE_SPRINT);
        MODULES.add(ARRAY_LIST);

        // Đăng ký phím mở ClickGUI (Mặc định: Right Shift, có thể đổi trong cài đặt Controls của game)
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simplehud.clickgui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.simplehud"
        ));

        // Lắng nghe sự kiện mỗi Tick của game
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Mở ClickGUI khi nhấn phím
            while (openGuiKey.wasPressed()) {
                client.setScreen(new ClickGuiScreen());
            }

            // Thực thi tính năng ToggleSprint (tự giữ chạy nhanh khi đi tới)
            if (client.player != null && TOGGLE_SPRINT.isEnabled()) {
                if (client.options.forwardKey.isPressed() && !client.player.isSneaking()) {
                    client.player.setSprinting(true);
                }
            }
        });

        // Đăng ký sự kiện vẽ HUD
        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = context.getScaledWindowWidth();
        int leftY = 4;

        // 1. Vẽ Watermark nếu được bật trong ClickGUI
        if (WATERMARK.isEnabled()) {
            String watermark = "SimpleHUD 1.21.11 | FPS: " + client.getCurrentFps();
            context.fill(4, leftY, 8 + textRenderer.getWidth(watermark), leftY + 12, 0x90000000);
            context.drawTextWithShadow(textRenderer, watermark, 6, leftY + 2, 0xFF55FFFF);
            leftY += 15;
        }

        // 2. Vẽ Tọa độ nếu được bật trong ClickGUI
        if (COORDINATES.isEnabled()) {
            String coords = String.format("XYZ: %.1f / %.1f / %.1f",
                    client.player.getX(), client.player.getY(), client.player.getZ());
            context.drawTextWithShadow(textRenderer, coords, 6, leftY, 0xFFFFFFFF);
        }

        // 3. Vẽ danh sách các tính năng đang bật ở góc phải (ArrayList)
        if (ARRAY_LIST.isEnabled()) {
            List<SimpleModule> enabledModules = new ArrayList<>();
            for (SimpleModule mod : MODULES) {
                if (mod.isEnabled()) {
                    enabledModules.add(mod);
                }
            }

            // Sắp xếp theo chiều rộng chữ giảm dần
            enabledModules.sort(Comparator.comparingInt((SimpleModule m) -> textRenderer.getWidth(m.getName())).reversed());

            int rightY = 4;
            for (int i = 0; i < enabledModules.size(); i++) {
                String moduleName = enabledModules.get(i).getName();
                int textWidth = textRenderer.getWidth(moduleName);
                int xPos = screenWidth - textWidth - 6;

                int rainbowColor = getRainbowColor(i * 150);

                context.fill(xPos - 3, rightY - 1, screenWidth - 2, rightY + 10, 0x80000000);
                context.fill(screenWidth - 2, rightY - 1, screenWidth, rightY + 10, rainbowColor);
                context.drawTextWithShadow(textRenderer, moduleName, xPos, rightY, rainbowColor);

                rightY += 11;
            }
        }
    }

    private int getRainbowColor(int delay) {
        double rainbowState = Math.ceil((System.currentTimeMillis() + delay) / 15.0);
        rainbowState %= 360;
        return Color.getHSBColor((float) (rainbowState / 360.0f), 0.7f, 1.0f).getRGB();
    }
}
