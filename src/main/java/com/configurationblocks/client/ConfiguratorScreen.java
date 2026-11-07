package com.configurationblocks.client;

import com.configurationblocks.config.Behavior;
import com.configurationblocks.config.BlockConfig;
import com.configurationblocks.network.ApplyConfigPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ConfiguratorScreen extends Screen {
    private static final String DECIMAL = "-?\\d{0,6}(\\.\\d{0,3})?";
    private static final String INT = "\\d{0,2}";

    private final BlockPos pos;
    private final Direction face;
    private BlockConfig cfg;
    private EditBox hardness, blast, light, redstone, bounce;
    private Button settingsBtn;
    private boolean gravity;
    private Behavior behavior;
    private int top, left, right, colW, fullW;

    public ConfiguratorScreen(BlockPos pos, Direction face, BlockConfig cfg, Component blockName) {
        super(Component.translatable("screen.configurationblocks.title", blockName));
        this.pos = pos;
        this.face = face;
        this.cfg = cfg;
        this.gravity = cfg.gravity;
        this.behavior = cfg.behavior;
    }

    public static boolean hasSettings(Behavior b) {
        return b == Behavior.CHEST || b == Behavior.SHULKER_BOX || b.isFurnace() || b == Behavior.SPAWNER;
    }

    @Override
    protected void init() {
        int w = 130, gap = 16;
        colW = w;
        fullW = w * 2 + gap;
        left = width / 2 - w - gap / 2;
        right = width / 2 + gap / 2;
        top = Math.max(26, height / 2 - 90);

        hardness = box(left, top + 12, w, cfg.hasHardness() ? fmt(cfg.hardness) : "", DECIMAL, "hardness");
        blast = box(left, top + 48, w, cfg.blastResistance >= 0 ? fmt(cfg.blastResistance) : "", DECIMAL, "blast");
        light = box(left, top + 84, w, cfg.light > 0 ? Integer.toString(cfg.light) : "", INT, "light");
        redstone = box(right, top + 12, w, cfg.redstone > 0 ? Integer.toString(cfg.redstone) : "", INT, "redstone");
        bounce = box(right, top + 48, w, cfg.bounce > 0 ? fmt(cfg.bounce) : "", DECIMAL, "bounce");

        Button gravityBtn = Button.builder(gravityText(), b -> {
            gravity = !gravity;
            b.setMessage(gravityText());
        }).bounds(right, top + 83, w, 20).build();
        gravityBtn.setTooltip(Tooltip.create(Component.translatable("screen.configurationblocks.gravity.tip")));
        addRenderableWidget(gravityBtn);

        addRenderableWidget(Button.builder(behaviorText(), b -> {
            behavior = Behavior.values()[(behavior.ordinal() + 1) % Behavior.values().length];
            b.setMessage(behaviorText());
            settingsBtn.visible = hasSettings(behavior);
        }).bounds(left, top + 112, fullW, 20).build());

        settingsBtn = Button.builder(Component.translatable("screen.configurationblocks.settings"), b -> {
            cfg = build();
            minecraft.setScreen(new FunctionSettingsScreen(this, cfg));
        }).bounds(left, top + 136, fullW, 20).build();
        settingsBtn.visible = hasSettings(behavior);
        addRenderableWidget(settingsBtn);

        int bw = (fullW - 12) / 3;
        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.apply"), b -> apply())
                .bounds(left, top + 168, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.paint"), b -> {
            send();
            minecraft.setScreen(new PaintScreen(pos, face));
        }).bounds(left + bw + 6, top + 168, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.cancel"), b -> onClose())
                .bounds(left + (bw + 6) * 2, top + 168, bw, 20).build());
    }

    private EditBox box(int x, int y, int w, String value, String regex, String key) {
        EditBox b = new EditBox(font, x, y, w, 18, Component.empty());
        b.setMaxLength(10);
        b.setFilter(s -> s.matches(regex));
        b.setValue(value);
        b.setHint(Component.translatable("screen.configurationblocks.default").withStyle(ChatFormatting.DARK_GRAY));
        b.setTooltip(Tooltip.create(Component.translatable("screen.configurationblocks." + key + ".tip")));
        return addRenderableWidget(b);
    }

    private Component gravityText() {
        return Component.translatable(gravity ? "screen.configurationblocks.gravity.on" : "screen.configurationblocks.gravity.off");
    }

    private Component behaviorText() {
        return Component.translatable("screen.configurationblocks.behavior", Component.translatable(behavior.langKey()));
    }

    static String fmt(float v) {
        return v == (int) v ? Integer.toString((int) v) : Float.toString(v);
    }

    static float parse(EditBox b, float def) {
        String s = b.getValue().trim();
        if (s.isEmpty() || s.equals("-") || s.equals(".")) return def;
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Builds a config from the widgets; settings of the function screen are carried over from cfg. */
    private BlockConfig build() {
        BlockConfig out = new BlockConfig();
        out.copyBasicFrom(cfg);
        out.hardness = parse(hardness, BlockConfig.HARDNESS_UNSET);
        out.blastResistance = parse(blast, BlockConfig.BLAST_UNSET);
        out.light = (int) parse(light, 0);
        out.redstone = (int) parse(redstone, 0);
        out.bounce = parse(bounce, 0);
        out.gravity = gravity;
        out.behavior = behavior;
        out.sanitize();
        return out;
    }

    private void send() {
        PacketDistributor.sendToServer(new ApplyConfigPayload(pos, build()));
    }

    private void apply() {
        send();
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, top - 16, 0xFFFFFF);
        label(g, "hardness", left, top, colW);
        label(g, "blast", left, top + 36, colW);
        label(g, "light", left, top + 72, colW);
        label(g, "redstone", right, top, colW);
        label(g, "bounce", right, top + 36, colW);
    }

    private void label(GuiGraphics g, String key, int x, int y, int maxW) {
        String s = Component.translatable("screen.configurationblocks." + key).getString();
        g.drawString(font, font.plainSubstrByWidth(s, maxW), x, y, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
