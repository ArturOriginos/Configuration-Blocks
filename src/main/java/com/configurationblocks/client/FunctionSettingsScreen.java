package com.configurationblocks.client;

import com.configurationblocks.config.Behavior;
import com.configurationblocks.config.BlockConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Extra settings of the selected function (chest rows, lock, spawner parameters). */
public class FunctionSettingsScreen extends Screen {
    private static final String INT = "\\d{0,5}";

    private final Screen parent;
    private final BlockConfig cfg;
    private final Behavior behavior;
    private boolean locked;
    private EditBox rowsBox, spawnerBox, minD, maxD, count, near, pRange, sRange;
    private Button lockBtn;
    private int top, left, right, w, fullW;

    public FunctionSettingsScreen(Screen parent, BlockConfig cfg) {
        super(Component.translatable("screen.configurationblocks.settings.title",
                Component.translatable(cfg.behavior.langKey())));
        this.parent = parent;
        this.cfg = cfg;
        this.behavior = cfg.behavior;
        this.locked = cfg.locked;
    }

    @Override
    protected void init() {
        w = 130;
        int gap = 16;
        fullW = w * 2 + gap;
        left = width / 2 - w - gap / 2;
        right = width / 2 + gap / 2;
        top = Math.max(26, height / 2 - 90);
        int bottom;

        if (behavior == Behavior.SPAWNER) {
            spawnerBox = new EditBox(font, left, top + 12, fullW - 76, 18, Component.empty());
            spawnerBox.setMaxLength(64);
            spawnerBox.setFilter(s -> s.matches("[a-z0-9_.:/-]*"));
            spawnerBox.setValue(cfg.spawnerEntity.toString());
            spawnerBox.setHint(Component.literal("minecraft:zombie").withStyle(ChatFormatting.DARK_GRAY));
            spawnerBox.setTooltip(Tooltip.create(Component.translatable("screen.configurationblocks.spawner.tip")));
            addRenderableWidget(spawnerBox);
            addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.spawner.browse"),
                    b -> {
                        collect();
                        minecraft.setScreen(new MobPickerScreen(this, id -> cfg.spawnerEntity = id));
                    }).bounds(left + fullW - 70, top + 11, 70, 20).build());

            minD = num(left, top + 48, cfg.spawnerMinDelay, "sp_min");
            maxD = num(right, top + 48, cfg.spawnerMaxDelay, "sp_max");
            count = num(left, top + 84, cfg.spawnerCount, "sp_count");
            near = num(right, top + 84, cfg.spawnerMaxNearby, "sp_near");
            pRange = num(left, top + 120, cfg.spawnerPlayerRange, "sp_player");
            sRange = num(right, top + 120, cfg.spawnerRange, "sp_radius");
            bottom = top + 152;
        } else {
            int y = top + 12;
            if (behavior == Behavior.CHEST) {
                rowsBox = num(left, y, cfg.storageRows, "rows");
                rowsBox.setMaxLength(1);
                rowsBox.setFilter(s -> s.matches("[1-6]?"));
                addLock(right, y - 1);
            } else {
                addLock(left, y - 1);
            }
            bottom = top + 52;
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.done"), b -> {
            collect();
            minecraft.setScreen(parent);
        }).bounds(left, bottom, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.cancel"),
                b -> minecraft.setScreen(parent)).bounds(right, bottom, w, 20).build());
    }

    private EditBox num(int x, int y, int value, String key) {
        EditBox b = new EditBox(font, x, y, w, 18, Component.empty());
        b.setMaxLength(5);
        b.setFilter(s -> s.matches(INT));
        b.setValue(Integer.toString(value));
        b.setTooltip(Tooltip.create(Component.translatable("screen.configurationblocks." + key + ".tip")));
        return addRenderableWidget(b);
    }

    private void addLock(int x, int y) {
        lockBtn = Button.builder(lockText(), b -> {
            locked = !locked;
            b.setMessage(lockText());
        }).bounds(x, y, w, 20).build();
        lockBtn.setTooltip(Tooltip.create(Component.translatable("screen.configurationblocks.lock.tip")));
        addRenderableWidget(lockBtn);
    }

    private Component lockText() {
        return Component.translatable(locked ? "screen.configurationblocks.lock.on" : "screen.configurationblocks.lock.off");
    }

    /** Writes widget values into the shared config object. */
    private void collect() {
        cfg.locked = locked;
        if (behavior == Behavior.CHEST && rowsBox != null) {
            cfg.storageRows = (int) ConfiguratorScreen.parse(rowsBox, 3);
        }
        if (behavior == Behavior.SPAWNER && spawnerBox != null) {
            ResourceLocation id = ResourceLocation.tryParse(spawnerBox.getValue().trim());
            if (id != null) cfg.spawnerEntity = id;
            cfg.spawnerMinDelay = (int) ConfiguratorScreen.parse(minD, 200);
            cfg.spawnerMaxDelay = (int) ConfiguratorScreen.parse(maxD, 800);
            cfg.spawnerCount = (int) ConfiguratorScreen.parse(count, 4);
            cfg.spawnerMaxNearby = (int) ConfiguratorScreen.parse(near, 6);
            cfg.spawnerPlayerRange = (int) ConfiguratorScreen.parse(pRange, 16);
            cfg.spawnerRange = (int) ConfiguratorScreen.parse(sRange, 4);
        }
        cfg.sanitize();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, top - 16, 0xFFFFFF);
        if (behavior == Behavior.SPAWNER) {
            label(g, "spawner", left, top, fullW);
            label(g, "sp_min", left, top + 36, w);
            label(g, "sp_max", right, top + 36, w);
            label(g, "sp_count", left, top + 72, w);
            label(g, "sp_near", right, top + 72, w);
            label(g, "sp_player", left, top + 108, w);
            label(g, "sp_radius", right, top + 108, w);
        } else if (behavior == Behavior.CHEST) {
            label(g, "rows", left, top, w);
        }
    }

    private void label(GuiGraphics g, String key, int x, int y, int maxW) {
        String s = Component.translatable("screen.configurationblocks." + key).getString();
        g.drawString(font, font.plainSubstrByWidth(s, maxW), x, y, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
