package com.configurationblocks.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Scrollable, searchable list of every entity that can be used in a spawner. */
public class MobPickerScreen extends Screen {
    private static final int ROW = 12;

    private record Entry(ResourceLocation id, String name) {}

    private final Screen parent;
    private final Consumer<ResourceLocation> onPick;
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> filtered = new ArrayList<>();
    private EditBox search;
    private int scroll;
    private int listX, listY, listW, listH;

    public MobPickerScreen(Screen parent, Consumer<ResourceLocation> onPick) {
        super(Component.translatable("screen.configurationblocks.picker.title"));
        this.parent = parent;
        this.onPick = onPick;
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (t.canSummon()) {
                all.add(new Entry(BuiltInRegistries.ENTITY_TYPE.getKey(t), t.getDescription().getString()));
            }
        }
        all.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));
        filtered = new ArrayList<>(all);
    }

    @Override
    protected void init() {
        listW = Math.min(320, width - 20);
        listX = width / 2 - listW / 2;
        listY = 52;
        listH = Math.max(ROW * 4, height - listY - 34);

        search = new EditBox(font, listX, 26, listW, 18, Component.empty());
        search.setMaxLength(40);
        search.setHint(Component.translatable("screen.configurationblocks.picker.search"));
        search.setResponder(this::refilter);
        addRenderableWidget(search);
        setInitialFocus(search);

        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.cancel"),
                b -> minecraft.setScreen(parent)).bounds(width / 2 - 50, height - 26, 100, 20).build());
    }

    private int visibleRows() {
        return listH / ROW;
    }

    private void refilter(String q) {
        String s = q.toLowerCase(Locale.ROOT).trim();
        filtered = new ArrayList<>();
        for (Entry e : all) {
            if (s.isEmpty() || e.name().toLowerCase(Locale.ROOT).contains(s) || e.id().toString().contains(s)) {
                filtered.add(e);
            }
        }
        scroll = 0;
    }

    private int rowAt(double mx, double my) {
        if (mx < listX || mx >= listX + listW || my < listY || my >= listY + visibleRows() * ROW) return -1;
        int idx = scroll + (int) ((my - listY) / ROW);
        return idx < filtered.size() ? idx : -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int idx = rowAt(mx, my);
        if (idx >= 0 && button == 0) {
            onPick.accept(filtered.get(idx).id());
            minecraft.setScreen(parent);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int max = Math.max(0, filtered.size() - visibleRows());
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * 3, 0, max);
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);

        g.fill(listX - 1, listY - 1, listX + listW + 1, listY + visibleRows() * ROW + 1, 0xAA000000);
        int hover = rowAt(mouseX, mouseY);
        for (int i = 0; i < visibleRows(); i++) {
            int idx = scroll + i;
            if (idx >= filtered.size()) break;
            Entry e = filtered.get(idx);
            int y = listY + i * ROW;
            if (idx == hover) g.fill(listX, y, listX + listW, y + ROW, 0x55FFFFFF);
            String id = e.id().toString();
            int idW = font.width(id);
            g.drawString(font, font.plainSubstrByWidth(e.name(), listW - idW - 16), listX + 4, y + 2, 0xFFFFFF);
            g.drawString(font, id, listX + listW - idW - 4, y + 2, 0x808080);
        }
        g.drawString(font, filtered.size() + " / " + all.size(), listX, height - 22, 0x808080);
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
