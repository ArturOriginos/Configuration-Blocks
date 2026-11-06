package com.configurationblocks.client;

import com.configurationblocks.config.Behavior;
import com.configurationblocks.config.BlockConfig;
import com.configurationblocks.network.ApplyConfigPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class ConfiguratorScreen extends Screen {
    private static final String DECIMAL = "-?\\d{0,6}(\\.\\d{0,3})?";
    private static final String INT = "\\d{0,2}";

    private final BlockPos pos;
    private final Direction face;
    private final BlockConfig cfg;
    private EditBox hardness, blast, light, redstone, bounce, spawnerBox, rowsBox, limitBox;
    private Button lockBtn;
    private boolean gravity, locked;
    private Behavior behavior;
    private int top, left, right, colW, fullW;

    public ConfiguratorScreen(BlockPos pos, Direction face, BlockConfig cfg, Component blockName) {
        super(Component.translatable("screen.configurationblocks.title", blockName));
        this.pos = pos;
        this.face = face;
        this.cfg = cfg;
        this.gravity = cfg.gravity;
        this.locked = cfg.locked;
        this.behavior = cfg.behavior;
    }

    @Override
    protected void init() {
        int w = 130, gap = 16;
        colW = w;
        fullW = w * 2 + gap;
        left = width / 2 - w - gap / 2;
        right = width / 2 + gap / 2;
        top = Math.max(24, height / 2 - 100);

        hardness = box(left, top + 12, w, cfg.hasHardness() ? fmt(cfg.hardness) : "", DECIMAL);
        blast = box(left, top + 48, w, cfg.blastResistance >= 0 ? fmt(cfg.blastResistance) : "", DECIMAL);
        light = box(left, top + 84, w, cfg.light > 0 ? Integer.toString(cfg.light) : "", INT);
        redstone = box(right, top + 12, w, cfg.redstone > 0 ? Integer.toString(cfg.redstone) : "", INT);
        bounce = box(right, top + 48, w, cfg.bounce > 0 ? fmt(cfg.bounce) : "", DECIMAL);

        addRenderableWidget(Button.builder(gravityText(), b -> {
            gravity = !gravity;
            b.setMessage(gravityText());
        }).bounds(right, top + 84, w, 20).build());

        addRenderableWidget(Button.builder(behaviorText(), b -> {
            behavior = Behavior.values()[(behavior.ordinal() + 1) % Behavior.values().length];
            b.setMessage(behaviorText());
            updateVisibility();
        }).bounds(left, top + 114, fullW, 20).build());

        // spawner row
        spawnerBox = new EditBox(font, left, top + 150, fullW, 18, Component.empty());
        spawnerBox.setMaxLength(64);
        spawnerBox.setFilter(s -> s.matches("[a-z0-9_.:/-]*"));
        spawnerBox.setValue(cfg.spawnerEntity.toString());
        spawnerBox.setHint(Component.literal("minecraft:zombie").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(spawnerBox);

        // storage row: rows | stack limit | lock
        int sw = (fullW - 12) / 3;
        rowsBox = new EditBox(font, left, top + 150, sw, 18, Component.empty());
        rowsBox.setMaxLength(1);
        rowsBox.setFilter(s -> s.matches("[1-6]?"));
        rowsBox.setValue(Integer.toString(cfg.storageRows));
        addRenderableWidget(rowsBox);

        limitBox = new EditBox(font, left + sw + 6, top + 150, sw, 18, Component.empty());
        limitBox.setMaxLength(2);
        limitBox.setFilter(s -> s.matches(INT));
        limitBox.setValue(Integer.toString(cfg.stackLimit));
        addRenderableWidget(limitBox);

        lockBtn = Button.builder(lockText(), b -> {
            locked = !locked;
            b.setMessage(lockText());
        }).bounds(left + (sw + 6) * 2, top + 149, sw, 20).build();
        addRenderableWidget(lockBtn);

        int bw = (fullW - 12) / 3;
        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.apply"), b -> apply())
                .bounds(left, top + 176, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.paint"), b -> {
            send();
            minecraft.setScreen(new PaintScreen(pos, face));
        }).bounds(left + bw + 6, top + 176, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.cancel"), b -> onClose())
                .bounds(left + (bw + 6) * 2, top + 176, bw, 20).build());

        updateVisibility();
    }

    private boolean hasStorage() {
        return behavior == Behavior.CHEST || behavior == Behavior.SHULKER_BOX;
    }

    private void updateVisibility() {
        spawnerBox.visible = behavior == Behavior.SPAWNER;
        rowsBox.visible = hasStorage();
        rowsBox.setEditable(behavior == Behavior.CHEST);
        limitBox.visible = hasStorage();
        lockBtn.visible = behavior.containerSize() > 0;
        if (!hasStorage() && behavior.containerSize() > 0) {
            // furnaces only get the lock button, put it at the left
            lockBtn.setX(left);
            lockBtn.setWidth(colW);
        } else {
            int sw = (fullW - 12) / 3;
            lockBtn.setX(left + (sw + 6) * 2);
            lockBtn.setWidth(sw);
        }
    }

    private EditBox box(int x, int y, int w, String value, String regex) {
        EditBox b = new EditBox(font, x, y, w, 18, Component.empty());
        b.setMaxLength(10);
        b.setFilter(s -> s.matches(regex));
        b.setValue(value);
        b.setHint(Component.translatable("screen.configurationblocks.default").withStyle(ChatFormatting.DARK_GRAY));
        return addRenderableWidget(b);
    }

    private Component gravityText() {
        return Component.translatable(gravity ? "screen.configurationblocks.gravity.on" : "screen.configurationblocks.gravity.off");
    }

    private Component lockText() {
        return Component.translatable(locked ? "screen.configurationblocks.lock.on" : "screen.configurationblocks.lock.off");
    }

    private Component behaviorText() {
        return Component.translatable("screen.configurationblocks.behavior", Component.translatable(behavior.langKey()));
    }

    private static String fmt(float v) {
        return v == (int) v ? Integer.toString((int) v) : Float.toString(v);
    }

    private static float parse(EditBox b, float def) {
        String s = b.getValue().trim();
        if (s.isEmpty() || s.equals("-") || s.equals(".")) return def;
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void apply() {
        send();
        onClose();
    }

    private void send() {
        BlockConfig out = new BlockConfig();
        out.hardness = parse(hardness, BlockConfig.HARDNESS_UNSET);
        out.blastResistance = parse(blast, BlockConfig.BLAST_UNSET);
        out.light = (int) parse(light, 0);
        out.redstone = (int) parse(redstone, 0);
        out.bounce = parse(bounce, 0);
        out.gravity = gravity;
        out.behavior = behavior;
        out.locked = locked;
        out.storageRows = (int) parse(rowsBox, 3);
        out.stackLimit = (int) parse(limitBox, 64);
        ResourceLocation se = ResourceLocation.tryParse(spawnerBox.getValue().trim());
        out.spawnerEntity = se != null ? se : BlockConfig.DEFAULT_SPAWN;
        out.sanitize();
        PacketDistributor.sendToServer(new ApplyConfigPayload(pos, out));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, top - 16, 0xFFFFFF);
        label(g, "hardness", left, top);
        label(g, "blast", left, top + 36);
        label(g, "light", left, top + 72);
        label(g, "redstone", right, top);
        label(g, "bounce", right, top + 36);
        if (behavior == Behavior.SPAWNER) label(g, "spawner", left, top + 138);
        if (hasStorage()) {
            int sw = (fullW - 12) / 3;
            label(g, "rows", left, top + 138);
            label(g, "stack_limit", left + sw + 6, top + 138);
        }
    }

    private void label(GuiGraphics g, String key, int x, int y) {
        g.drawString(font, Component.translatable("screen.configurationblocks." + key), x, y, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
