package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimpleHUDClient implements ClientModInitializer {

    public static final List<SimpleModule> MODULES = new ArrayList<>();
    public static final SimpleModule WATERMARK = new SimpleModule("Watermark", true);
    public static final SimpleModule COORDINATES = new SimpleModule("Coordinates", true);
    public static final SimpleModule TOGGLE_SPRINT = new SimpleModule("ToggleSprint", false);
    public static final SimpleModule ARRAY_LIST = new SimpleModule("ModuleList", true);

    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        MODULES.add(WATERMARK);
        MODULES.add(COORDINATES);
        MODULES.add(TOGGLE_SPRINT);
        MODULES.add(ARRAY_LIST);

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.simplehud.clickgui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                client.setScreen(new ClickGuiScreen());
            }

            if (client.player != null && TOGGLE_SPRINT.isEnabled()) {
                if (client.options.keyUp.isDown() && !client.player.isShiftKeyDown()) {
                    client.player.setSprinting(true);
                }
            }
        });

        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }

        Font font = client.font;
        int screenWidth = graphics.guiWidth();
        int leftY = 4;

        // 1. Vẽ Watermark & FPS
        if (WATERMARK.isEnabled()) {
            String watermark = "SimpleHUD 1.21.11 | FPS: " + client.getFps();
            graphics.fill(4, leftY, 8 + font.width(watermark), leftY + 12, 0x90000000);
            graphics.drawString(font, watermark, 6, leftY + 2, 0xFF55FFFF);
            leftY += 15;
        }

        // 2. Vẽ Tọa độ XYZ
        if (COORDINATES.isEnabled()) {
            String coords = String.format("XYZ: %.1f / %.1f / %.1f",
                    client.player.getX(), client.player.getY(), client.player.getZ());
            graphics.drawString(font, coords, 6, leftY, 0xFFFFFFFF);
        }

        // 3. Vẽ danh sách tính năng đang bật ở góc phải
        if (ARRAY_LIST.isEnabled()) {
            List<SimpleModule> enabledModules = new ArrayList<>();
            for (SimpleModule mod : MODULES) {
                if (mod.isEnabled()) {
                    enabledModules.add(mod);
                }
            }

            enabledModules.sort(Comparator.comparingInt((SimpleModule m) -> font.width(m.getName())).reversed());

            int rightY = 4;
            for (int i = 0; i < enabledModules.size(); i++) {
                String moduleName = enabledModules.get(i).getName();
                int textWidth = font.width(moduleName);
                int xPos = screenWidth - textWidth - 6;

                int rainbowColor = getRainbowColor(i * 150);

                graphics.fill(xPos - 3, rightY - 1, screenWidth - 2, rightY + 10, 0x80000000);
                graphics.fill(screenWidth - 2, rightY - 1, screenWidth, rightY + 10, rainbowColor);
                graphics.drawString(font, moduleName, xPos, rightY, rainbowColor);

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
