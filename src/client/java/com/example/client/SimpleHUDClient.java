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
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimpleHUDClient implements ClientModInitializer {

    public static final List<SimpleModule> MODULES = new ArrayList<>();
    public static final SimpleModule WATERMARK = new SimpleModule("ServerStats", true);
    public static final SimpleModule ARMOR_HUD = new SimpleModule("ArmorHUD", true);
    public static final SimpleModule COORDINATES = new SimpleModule("Coordinates", true);
    public static final SimpleModule INV_PREVIEW = new SimpleModule("InventoryHUD", true);
    public static final SimpleModule TOGGLE_SPRINT = new SimpleModule("Sprint", true);
    public static final SimpleModule ARRAY_LIST = new SimpleModule("HUD", true);

    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        MODULES.add(WATERMARK);
        MODULES.add(ARMOR_HUD);
        MODULES.add(COORDINATES);
        MODULES.add(INV_PREVIEW);
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

        // Màu sắc chuẩn theo ảnh mẫu
        int bgDark = 0x85000000;
        int goldColor = 0xFFFFAA00;
        int lightGold = 0xFFFFD700;
        int whiteColor = 0xFFFFFFFF;
        int aquaColor = 0xFF55FFFF;

        // 1. GÓC TRÊN TRÁI: FPS, PING & SERVER IP
        if (WATERMARK.isEnabled()) {
            int ping = 0;
            if (client.getConnection() != null) {
                PlayerInfo entry = client.getConnection().getPlayerInfo(client.player.getUUID());
                if (entry != null) ping = entry.getLatency();
            }
            String serverIp = client.getCurrentServer() != null ? client.getCurrentServer().ip.toUpperCase() : "SINGLEPLAYER";
            String statsText = String.format("FPS: %d  PING: %dMS  %s", client.getFps(), ping, serverIp);

            int boxW = font.width(statsText) + 8;
            graphics.fill(4, 4, 4 + boxW, 16, bgDark);
            graphics.drawString(font, statsText, 8, 6, goldColor);
        }

        // 2. BÊN TRÁI: ĐỘ BỀN GIÁP (%) + SỐ LƯỢNG VẬT PHẨM (TOTEM, TÁO VÀNG, EXP)
        if (ARMOR_HUD.isEnabled()) {
            int armorY = 48;
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (EquipmentSlot slot : slots) {
                ItemStack stack = client.player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, 6, armorY);
                    int maxDmg = stack.getMaxDamage();
                    int pct = maxDmg > 0 ? (int) (((double) (maxDmg - stack.getDamageValue()) / maxDmg) * 100) : 100;
                    graphics.drawString(font, pct + "%", 25, armorY + 4, whiteColor);
                    armorY += 18;
                }
            }

            // Hiển thị thêm số lượng Totem và Táo vàng bên cạnh cột giáp
            int totems = countItem(client, Items.TOTEM_OF_UNDYING);
            int gapples = countItem(client, Items.GOLDEN_APPLE) + countItem(client, Items.ENCHANTED_GOLDEN_APPLE);
            
            graphics.renderItem(new ItemStack(Items.TOTEM_OF_UNDYING), 62, 48);
            graphics.drawString(font, String.valueOf(totems), 80, 52, whiteColor);

            graphics.renderItem(new ItemStack(Items.GOLDEN_APPLE), 62, 66);
            graphics.drawString(font, String.valueOf(gapples), 80, 70, whiteColor);
        }

        // 3. GÓC DƯỚI TRÁI: TỌA ĐỘ XYZ HIỆN TẠI + QUY ĐỔI NETHER/OVERWORLD
        if (COORDINATES.isEnabled()) {
            double x = client.player.getX();
            double y = client.player.getY();
            double z = client.player.getZ();
            boolean isNether = client.level != null && client.level.dimension() == Level.NETHER;
            double altX = isNether ? x * 8.0 : x / 8.0;
            double altZ = isNether ? z * 8.0 : z / 8.0;

            String coordStr = String.format("X: %.1f  Y: %.1f  Z: %.1f  [%.1f, %.1f]", x, y, z, altX, altZ);
            int cWidth = font.width(coordStr) + 8;

            graphics.fill(4, screenHeight - 16, 4 + cWidth, screenHeight - 4, bgDark);
            graphics.drawString(font, coordStr, 8, screenHeight - 14, aquaColor);
        }

        // 4. GÓC TRÊN PHẢI: MODULE LIST (CHỮ VÀNG SÁNG GIỐNG ẢNH)
        if (ARRAY_LIST.isEnabled()) {
            List<SimpleModule> enabledModules = new ArrayList<>();
            for (SimpleModule mod : MODULES) {
                if (mod.isEnabled()) {
                    enabledModules.add(mod);
                }
            }
            enabledModules.sort(Comparator.comparingInt((SimpleModule m) -> font.width(m.getName())).reversed());

            int rightY = 4;
            for (SimpleModule mod : enabledModules) {
                String name = mod.getName().toUpperCase();
                int tWidth = font.width(name);
                int xPos = screenWidth - tWidth - 6;

                graphics.fill(xPos - 3, rightY - 1, screenWidth - 2, rightY + 10, bgDark);
                graphics.fill(screenWidth - 2, rightY - 1, screenWidth, rightY + 10, goldColor);
                graphics.drawString(font, name, xPos, rightY, lightGold);
                rightY += 11;
            }
        }

        // 5. GÓC PHẢI DƯỚI: LƯỚI XEM TRƯỚC TÚI ĐỒ (INVENTORY PREVIEW)
        if (INV_PREVIEW.isEnabled()) {
            int invX = screenWidth - 166;
            int invY = screenHeight - 70;
            graphics.fill(invX - 2, invY - 2, invX + 162, invY + 54, bgDark);

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIndex = 9 + row * 9 + col;
                    ItemStack stack = client.player.getInventory().getItem(slotIndex);
                    
                    if (!stack.isEmpty()) {
                        int itemX = invX + col * 18;
                        int itemY = invY + row * 18;
                        graphics.renderItem(stack, itemX, itemY);
                        graphics.renderItemDecorations(font, stack, itemX, itemY);
                    }
                }
            }
        }
    }

    private int countItem(Minecraft client, Item targetItem) {
        if (client.player == null) return 0;
        int count = 0;
        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.is(targetItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int getRainbowColor(int delay) {
        double rainbowState = Math.ceil((System.currentTimeMillis() + delay) / 15.0);
        rainbowState %= 360;
        return Color.getHSBColor((float) (rainbowState / 360.0f), 0.7f, 1.0f).getRGB();
                        }
}
