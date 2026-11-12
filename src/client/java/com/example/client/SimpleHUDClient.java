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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
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

    // Kích thước mặc định (1.25x) và độ bo tròn toàn bộ góc (7px = bo tròn dạng viên thuốc)
    public static float hudScale = 1.25f;
    public static int cornerRadius = 7;

    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss");
    private static final Color PRIMARY_COLOR = new Color(65, 185, 255, 255);
    private static final Color SECONDARY_COLOR = new Color(255, 110, 230, 255);
    private static final int BG_COLOR = new Color(28, 28, 32, 215).getRGB();

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
        int screenWidth = (int) (graphics.guiWidth() / hudScale);
        int screenHeight = (int) (graphics.guiHeight() / hudScale);

        graphics.pose().pushMatrix();
        graphics.pose().scale(hudScale, hudScale);

        // 1. WATERMARK (Bo tròn toàn bộ)
        if (WATERMARK.isEnabled()) {
            String wmText = "KRYPTON+";
            int wmWidth = font.width(wmText);
            drawFullRoundedRect(graphics, 5, 5, 5 + wmWidth + 14, 20, cornerRadius, BG_COLOR);
            graphics.drawString(font, wmText, 12, 9, PRIMARY_COLOR.getRGB());
        }

        // 2. SYSTEM INFO: FPS | PING | SERVER IP (Bo tròn toàn bộ)
        if (INFO_HUD.isEnabled()) {
            String fpsPart = "FPS: " + client.getFps() + "  ";
            String pingPart = getPingInfo(client);
            String ipPart = client.getCurrentServer() == null ? "SINGLEPLAYER" : client.getCurrentServer().ip.toUpperCase();

            int w1 = font.width(fpsPart);
            int w2 = font.width(pingPart);
            int w3 = font.width(ipPart);
            int totalW = w1 + w2 + w3;

            drawFullRoundedRect(graphics, 5, 24, 5 + totalW + 14, 39, cornerRadius, BG_COLOR);
            graphics.drawString(font, fpsPart, 12, 28, PRIMARY_COLOR.getRGB());
            graphics.drawString(font, pingPart, 12 + w1, 28, SECONDARY_COLOR.getRGB());
            graphics.drawString(font, ipPart, 12 + w1 + w2, 28, new Color(210, 210, 210, 255).getRGB());
        }

        // 3. TIME (Chính giữa mép trên - Bo tròn toàn bộ)
        if (TIME_HUD.isEnabled()) {
            String timeStr = TIME_FORMATTER.format(new Date());
            int timeW = font.width(timeStr);
            int centerX = screenWidth / 2;

            drawFullRoundedRect(graphics, centerX - (timeW / 2) - 7, 5, centerX + (timeW / 2) + 7, 20, cornerRadius, BG_COLOR);
            graphics.drawString(font, timeStr, centerX - (timeW / 2), 9, PRIMARY_COLOR.getRGB());
        }

        // 4. COORDINATES (Góc dưới bên trái - Bo tròn toàn bộ)
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

            drawFullRoundedRect(graphics, 5, screenHeight - 20, 5 + coordW + 14, screenHeight - 5, cornerRadius, BG_COLOR);
            graphics.drawString(font, coordText, 12, screenHeight - 16, PRIMARY_COLOR.getRGB());
        }

        // 5. MODULE LIST (Góc trên bên phải - Từng ô bo tròn mềm mại)
        if (ARRAY_LIST.isEnabled()) {
            List<SimpleModule> enabledModules = new ArrayList<>();
            for (SimpleModule mod : MODULES) {
                if (mod.isEnabled()) {
                    enabledModules.add(mod);
                }
            }
            enabledModules.sort(Comparator.comparingInt((SimpleModule m) -> font.width(m.getName())).reversed());

            int rightY = 5;
            int total = enabledModules.size();
            for (int i = 0; i < total; i++) {
                SimpleModule mod = enabledModules.get(i);
                String name = mod.getName();
                int nameW = font.width(name);
                int boxRight = screenWidth - 5;
                int boxLeft = boxRight - nameW - 14;

                float ratio = total > 1 ? (float) i / (float) (total - 1) : 0.0f;
                int color = blendColors(PRIMARY_COLOR, SECONDARY_COLOR, ratio).getRGB();

                drawFullRoundedRect(graphics, boxLeft, rightY, boxRight, rightY + 15, cornerRadius, BG_COLOR);
                graphics.drawString(font, name, boxLeft + 7, rightY + 4, color);

                rightY += 17;
            }
        }

        graphics.pose().popMatrix();
    }

    // Thuật toán vẽ hình chữ nhật bo tròn 4 góc siêu mịn
    private void drawFullRoundedRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int color) {
        int width = x2 - x1;
        int height = y2 - y1;
        int r = Math.min(radius, Math.min(width / 2, height / 2));

        if (r <= 0) {
            graphics.fill(x1, y1, x2, y2, color);
            return;
        }

        // Phần lõi giữa (không bị đè pixel)
        graphics.fill(x1 + r, y1, x2 - r, y2, color);
        graphics.fill(x1, y1 + r, x1 + r, y2 - r, color);
        graphics.fill(x2 - r, y1 + r, x2, y2 - r, color);

        // Vẽ từng dòng pixel tạo đường cong tròn đều ở 4 góc
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            // 2 góc trên
            graphics.fill(x1 + dx, y1 + i, x1 + r, y1 + i + 1, color);
            graphics.fill(x2 - r, y1 + i, x2 - dx, y1 + i + 1, color);
            // 2 góc dưới
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

    // Lớp quản lý Module
    public static class SimpleModule {
        private final String name;
        private boolean enabled;

        public SimpleModule(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }
        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void toggle() {
            this.enabled = !this.enabled;
        }
    }

    public static class ClickGuiScreen extends Screen {
        public ClickGuiScreen() {
            super(Component.literal("SimpleHUD Settings"));
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int startY = 40;

            for (int i = 0; i < MODULES.size(); i++) {
                SimpleModule mod = MODULES.get(i);
                this.addRenderableWidget(Button.builder(
                        Component.literal(mod.getName() + ": " + (mod.isEnabled() ? "ON" : "OFF")),
                        button -> {
                            mod.toggle();
                            button.setMessage(Component.literal(mod.getName() + ": " + (mod.isEnabled() ? "ON" : "OFF")));
                        }
                ).bounds(centerX - 105, startY + i * 24, 100, 20).build());
            }

            this.addRenderableWidget(Button.builder(
                    Component.literal("Scale -: " + hudScale + "x"),
                    button -> {
                        hudScale = Math.max(0.5f, Math.round((hudScale - 0.1f) * 10f) / 10f);
                        this.rebuildWidgets();
                    }
            ).bounds(centerX + 5, startY, 100, 20).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("Scale +: " + hudScale + "x"),
                    button -> {
                        hudScale = Math.min(2.0f, Math.round((hudScale + 0.1f) * 10f) / 10f);
                        this.rebuildWidgets();
                    }
            ).bounds(centerX + 5, startY + 24, 100, 20).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("Bo góc -: " + cornerRadius + "px"),
                    button -> {
                        cornerRadius = Math.max(0, cornerRadius - 1);
                        this.rebuildWidgets();
                    }
            ).bounds(centerX + 5, startY + 56, 100, 20).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("Bo góc +: " + cornerRadius + "px"),
                    button -> {
                        cornerRadius = Math.min(8, cornerRadius + 1);
                        this.rebuildWidgets();
                    }
            ).bounds(centerX + 5, startY + 80, 100, 20).build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);
            graphics.drawCenteredString(this.font, "SimpleHUD Configuration", this.width / 2, 18, 0xFF41B9FF);
        }
    }
}

                                 
