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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
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
    public static final List<SimpleModule> HUD_ELEMENTS = new ArrayList<>();

    // Các Module chính hiển thị ngoài ClickGUI và trên góc phải màn hình
    public static final SimpleModule MASTER_HUD = new SimpleModule("HUD", true);
    public static final SimpleModule TOGGLE_SPRINT = new SimpleModule("Sprint", true);
    public static final SimpleModule STORAGE_ESP = new SimpleModule("Storage ESP", true);

    // Các tùy chọn con nằm bên trong menu chuột phải của HUD
    public static final SimpleModule WATERMARK = new SimpleModule("Watermark", true);
    public static final SimpleModule INFO_HUD = new SimpleModule("Info", true);
    public static final SimpleModule TIME_HUD = new SimpleModule("Time", true);
    public static final SimpleModule COORDINATES = new SimpleModule("Coordinates", true);
    public static final SimpleModule ARRAY_LIST = new SimpleModule("Modules", true);
    public static final SimpleModule RAINBOW_HUD = new SimpleModule("Rainbow", false);

    public static float hudScale = 1.25f;
    public static int cornerRadius = 2;

    public static final FontDescription CUSTOM_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("simplehud", "custom"));

    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss");
    private static final Color PRIMARY_COLOR = new Color(65, 185, 255, 255);
    private static final Color SECONDARY_COLOR = new Color(255, 110, 230, 255);
    private static final int BG_COLOR = new Color(28, 28, 32, 215).getRGB();

    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        MODULES.add(MASTER_HUD);
        MODULES.add(TOGGLE_SPRINT);
        MODULES.add(STORAGE_ESP);

        HUD_ELEMENTS.add(WATERMARK);
        HUD_ELEMENTS.add(INFO_HUD);
        HUD_ELEMENTS.add(TIME_HUD);
        HUD_ELEMENTS.add(COORDINATES);
        HUD_ELEMENTS.add(ARRAY_LIST);
        HUD_ELEMENTS.add(RAINBOW_HUD);

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

    public static Component styledText(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(CUSTOM_FONT));
    }

    // Hàm tạo màu cầu vồng chuyển động mượt mà theo thời gian và độ lệch vị trí (offset)
    private int getRainbowColor(int offsetMs) {
        float speed = 3500.0f; // Tốc độ chạy màu cầu vồng (3.5 giây / vòng)
        float hue = ((System.currentTimeMillis() + offsetMs) % (int) speed) / speed;
        return Color.HSBtoRGB(hue, 0.78f, 1.0f);
    }

    private void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui || !MASTER_HUD.isEnabled()) {
            return;
        }

        Font font = client.font;
        int screenWidth = (int) (graphics.guiWidth() / hudScale);
        int screenHeight = (int) (graphics.guiHeight() / hudScale);
        boolean isRainbow = RAINBOW_HUD.isEnabled();

        graphics.pose().pushMatrix();
        graphics.pose().scale(hudScale, hudScale);

        // 1. WATERMARK
        if (WATERMARK.isEnabled()) {
            Component wmComp = styledText("KRYPTON+");
            int wmWidth = font.width(wmComp);
            int wmColor = isRainbow ? getRainbowColor(0) : PRIMARY_COLOR.getRGB();

            drawFullRoundedRect(graphics, 5, 5, 5 + wmWidth + 14, 20, cornerRadius, BG_COLOR);
            graphics.drawString(font, wmComp, 12, 9, wmColor);
        }

        // 2. SYSTEM INFO: FPS | PING | SERVER IP
        if (INFO_HUD.isEnabled()) {
            Component fpsComp = styledText("FPS: " + client.getFps() + "  ");
            Component pingComp = styledText(getPingInfo(client));
            String ipStr = client.getCurrentServer() == null ? "SINGLEPLAYER" : client.getCurrentServer().ip.toUpperCase();
            Component ipComp = styledText(ipStr);

            int w1 = font.width(fpsComp);
            int w2 = font.width(pingComp);
            int w3 = font.width(ipComp);
            int totalW = w1 + w2 + w3;

            int fpsColor = isRainbow ? getRainbowColor(200) : PRIMARY_COLOR.getRGB();
            int pingColor = isRainbow ? getRainbowColor(400) : SECONDARY_COLOR.getRGB();
            int ipColor = isRainbow ? getRainbowColor(600) : new Color(210, 210, 210, 255).getRGB();

            drawFullRoundedRect(graphics, 5, 24, 5 + totalW + 14, 39, cornerRadius, BG_COLOR);
            graphics.drawString(font, fpsComp, 12, 28, fpsColor);
            graphics.drawString(font, pingComp, 12 + w1, 28, pingColor);
            graphics.drawString(font, ipComp, 12 + w1 + w2, 28, ipColor);
        }

        // 3. TIME (Bo tròn hình viên thuốc ở giữa mép trên)
        if (TIME_HUD.isEnabled()) {
            Component timeComp = styledText(TIME_FORMATTER.format(new Date()));
            int timeW = font.width(timeComp);
            int centerX = screenWidth / 2;
            int timeColor = isRainbow ? getRainbowColor(300) : PRIMARY_COLOR.getRGB();

            drawFullRoundedRect(graphics, centerX - (timeW / 2) - 8, 5, centerX + (timeW / 2) + 8, 20, 7, BG_COLOR);
            graphics.drawString(font, timeComp, centerX - (timeW / 2), 9, timeColor);
        }

        // 4. COORDINATES
        if (COORDINATES.isEnabled()) {
            double x = client.player.getX();
            double y = client.player.getY();
            double z = client.player.getZ();
            String altCoords = "";

            if (client.level != null) {
                if (client.level.dimension() == Level.NETHER) {
                    altCoords = String.format(java.util.Locale.US, "  %.1f, %.1f", x * 8.0, z * 8.0);
                } else if (client.level.dimension() == Level.OVERWORLD) {
                    altCoords = String.format(java.util.Locale.US, "  %.1f, %.1f", x / 8.0, z / 8.0);
                }
            }

            Component coordComp = styledText(String.format(java.util.Locale.US, "X: %.1f  Y: %.1f  Z: %.1f%s", x, y, z, altCoords));
            int coordW = font.width(coordComp);
            int coordColor = isRainbow ? getRainbowColor(500) : PRIMARY_COLOR.getRGB();

            drawFullRoundedRect(graphics, 5, screenHeight - 20, 5 + coordW + 14, screenHeight - 5, cornerRadius, BG_COLOR);
            graphics.drawString(font, coordComp, 12, screenHeight - 16, coordColor);
        }

        // 5. MODULE LIST (Hộp chữ bo góc + Vạch dọc sáng màu nối liền mạch ở mép phải)
        if (ARRAY_LIST.isEnabled()) {
            List<SimpleModule> enabledModules = new ArrayList<>();
            for (SimpleModule mod : MODULES) {
                if (mod.isEnabled()) {
                    enabledModules.add(mod);
                }
            }
            enabledModules.sort(Comparator.comparingInt((SimpleModule m) -> font.width(styledText(m.getName().toUpperCase()))).reversed());

            int rightY = 5;
            int boxHeight = 13;
            int gap = 2;
            int total = enabledModules.size();

            for (int i = 0; i < total; i++) {
                SimpleModule mod = enabledModules.get(i);
                Component nameComp = styledText(mod.getName().toUpperCase());
                int nameW = font.width(nameComp);

                int barRight = screenWidth - 5;
                int barLeft = barRight - 2;
                int boxRight = barLeft - 3;
                int boxLeft = boxRight - nameW - 8;

                int color;
                if (isRainbow) {
                    // Mỗi dòng lệch nhau 250ms tạo hiệu ứng dải cầu vồng trôi từ trên xuống dưới
                    color = getRainbowColor(i * 250);
                } else {
                    float ratio = total > 1 ? (float) i / (float) (total - 1) : 0.0f;
                    color = blendColors(PRIMARY_COLOR, SECONDARY_COLOR, ratio).getRGB();
                }

                drawFullRoundedRect(graphics, boxLeft, rightY, boxRight, rightY + boxHeight, cornerRadius, BG_COLOR);
                graphics.drawString(font, nameComp, boxLeft + 4, rightY + 3, color);
                int lineEndY = (i == total - 1) ? (rightY + boxHeight) : (rightY + boxHeight + gap);
                graphics.fill(barLeft, rightY, barRight, lineEndY, color);

                rightY += boxHeight + gap;
            }
        }

        graphics.pose().popMatrix();
    }

    private void drawFullRoundedRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int color) {
        int width = x2 - x1;
        int height = y2 - y1;
        int r = Math.min(radius, Math.min(width / 2, height / 2));

        if (r <= 0) {
            graphics.fill(x1, y1, x2, y2, color);
            return;
        }

        graphics.fill(x1 + r, y1, x2 - r, y2, color);
        graphics.fill(x1, y1 + r, x1 + r, y2 - r, color);
        graphics.fill(x2 - r, y1 + r, x2, y2 - r, color);

        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            graphics.fill(x1 + dx, y1 + i, x1 + r, y1 + i + 1, color);
            graphics.fill(x2 - r, y1 + i, x2 - dx, y1 + i + 1, color);
            graphics.fill(x1 + dx, y2 - i - 1, x1 + r, y2 - i, color);
            graphics.fill(x2 - r, y2 - i - 1, x2 - dx, y2 - i, color);
        }
    }

    private String getPingInfo(Minecraft client) {
        if (client.player != null && client.getConnection() != null) {
            PlayerInfo entry = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (entry != null) {
                return "PING: " + entry.getLatency() + "MS  ";
            }
        }
        return "PING: 0MS  ";
    }

    private Color blendColors(Color c1, Color c2, float ratio) {
        int r = Mth.clamp((int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio), 0, 255);
        int g = Mth.clamp((int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio), 0, 255);
        int b = Mth.clamp((int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio), 0, 255);
        return new Color(r, g, b, 255);
    }
}

