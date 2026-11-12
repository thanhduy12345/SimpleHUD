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
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class SimpleHUDClient implements ClientModInitializer {

    public static final List<SimpleModule> MODULES = new ArrayList<>();
    public static final SimpleModule WATERMARK = new SimpleModule("Watermark", true);
    public static final SimpleModule INFO_HUD = new SimpleModule("Info", true);
    public static final SimpleModule TIME_HUD = new SimpleModule("Time", true);
    public static final SimpleModule COORDINATES = new SimpleModule("Coordinates", true);
    public static final SimpleModule TOGGLE_SPRINT = new SimpleModule("Sprint", true);
    public static final SimpleModule ARRAY_LIST = new SimpleModule("Modules", true);

    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss");
    private static final Color PRIMARY_COLOR = new Color(65, 185, 255, 255);
    private static final Color SECONDARY_COLOR = new Color(255, 110, 230, 255);
    private static final int BG_COLOR = new Color(35, 35, 35, 204).getRGB();

    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        MODULES.add(WATERMARK);
        MODULES.add(INFO_HUD);
        MODULES.add(TIME_HUD);
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
        int screenHeight = graphics.guiHeight();

        // 1. WATERMARK (Góc trên bên trái)
        if (WATERMARK.isEnabled()) {
            String wmText = "SimpleHUD 1.21.11";
            int wmWidth = font.width(wmText);
            drawStyledBox(graphics, 4, 4, 4 + wmWidth + 8, 17, PRIMARY_COLOR.getRGB());
            graphics.drawString(font, wmText, 8, 7, PRIMARY_COLOR.getRGB());
        }

        // 2. SYSTEM INFO: FPS | PING | SERVER IP (Ngay dưới Watermark)
        if (INFO_HUD.isEnabled()) {
            String fpsPart = "FPS: " + client.getFps() + " | ";
            String pingPart = getPingInfo(client);
            String ipPart = client.getCurrentServer() == null ? "Single Player" : client.getCurrentServer().ip;

            int w1 = font.width(fpsPart);
            int w2 = font.width(pingPart);
            int w3 = font.width(ipPart);
            int totalW = w1 + w2 + w3;

            drawStyledBox(graphics, 4, 21, 4 + totalW + 8, 34, SECONDARY_COLOR.getRGB());
            graphics.drawString(font, fpsPart, 8, 24, PRIMARY_COLOR.getRGB());
            graphics.drawString(font, pingPart, 8 + w1, 24, SECONDARY_COLOR.getRGB());
            graphics.drawString(font, ipPart, 8 + w1 + w2, 24, new Color(175, 175, 175, 255).getRGB());
        }

        // 3. TIME (Chính giữa mép trên màn hình)
        if (TIME_HUD.isEnabled()) {
            String timeStr = TIME_FORMATTER.format(new Date());
            int timeW = font.width(timeStr);
            int centerX = screenWidth / 2;

            drawStyledBox(graphics, centerX - (timeW / 2) - 4, 4, centerX + (timeW / 2) + 4, 17, PRIMARY_COLOR.getRGB());
            graphics.drawString(font, timeStr, centerX - (timeW / 2), 7, PRIMARY_COLOR.getRGB());
        }

        // 4. COORDINATES (Góc dưới bên trái + quy đổi Nether/Overworld)
        if (COORDINATES.isEnabled()) {
            double x = client.player.getX();
            double y = client.player.getY();
            double z = client.player.getZ();
            String altCoords = "";

            if (client.level != null) {
                if (client.level.dimension() == Level.NETHER) {
                    altCoords = String.format(" [%.1f, %.1f]", x * 8.0, z * 8.0);
                } else if (client.level.dimension() == Level.OVERWORLD) {
                    altCoords = String.format(" [%.1f, %.1f]", x / 8.0, z / 8.0);
                }
            }

            String coordText = String.format("X: %.1f | Y: %.1f | Z: %.1f%s", x, y, z, altCoords);
            int coordW = font.width(coordText);

            drawStyledBox(graphics, 4, screenHeight - 17, 4 + coordW + 8, screenHeight - 4, PRIMARY_COLOR.getRGB());
            graphics.drawString(font, coordText, 8, screenHeight - 14, PRIMARY_COLOR.getRGB());
        }

        // 5. MODULE LIST (Góc trên bên phải - Gradient Xanh Dương -> Hồng Tím)
        if (ARRAY_LIST.isEnabled()) {
            List<SimpleModule> enabledModules = new ArrayList<>();
            for (SimpleModule mod : MODULES) {
                if (mod.isEnabled()) {
                    enabledModules.add(mod);
                }
            }
            enabledModules.sort(Comparator.comparingInt((SimpleModule m) -> font.width(m.getName())).reversed());

            int rightY = 4;
            int total = enabledModules.size();
            for (int i = 0; i < total; i++) {
                SimpleModule mod = enabledModules.get(i);
                String name = mod.getName();
                int nameW = font.width(name);
                int boxRight = screenWidth - 4;
                int boxLeft = boxRight - nameW - 8;

                float ratio = total > 1 ? (float) i / (float) (total - 1) : 0.0f;
                int color = blendColors(PRIMARY_COLOR, SECONDARY_COLOR, ratio).getRGB();

                graphics.fill(boxLeft, rightY, boxRight, rightY + 13, BG_COLOR);
                graphics.fill(boxRight - 2, rightY, boxRight, rightY + 13, color);
                graphics.drawString(font, name, boxLeft + 4, rightY + 3, color);

                rightY += 15;
            }
        }
    }

    private void drawStyledBox(GuiGraphics graphics, int x1, int y1, int x2, int y2, int accentColor) {
        graphics.fill(x1, y1, x2, y2, BG_COLOR);
        graphics.fill(x1, y1, x1 + 2, y2, accentColor);
    }

    private String getPingInfo(Minecraft client) {
        if (client.player != null && client.getConnection() != null) {
            PlayerInfo entry = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (entry != null) {
                return "Ping: " + entry.getLatency() + "ms | ";
            }
        }
        return "Ping: N/A | ";
    }

    private Color blendColors(Color c1, Color c2, float ratio) {
        int r = Mth.clamp((int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio), 0, 255);
        int g = Mth.clamp((int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio), 0, 255);
        int b = Mth.clamp((int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio), 0, 255);
        return new Color(r, g, b, 255);
    }
}
