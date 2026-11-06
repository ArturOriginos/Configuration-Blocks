package com.configurationblocks.client;

import com.configurationblocks.network.PaintPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/** 16x16 pixel editor for one face of a block. */
public class PaintScreen extends Screen {
    private static final int N = 16, CELL = 10, SIZE = N * CELL;
    private static final int SW = 16, SG = 2; // swatch size / gap
    private static final int[] PALETTE = {
            0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA,
            0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
            0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA,
            0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21
    };

    private enum Tool { PENCIL, ERASER, FILL, PICKER }

    private record Snap(Direction face, int[] pixels) {}

    private final BlockPos pos;
    private Direction face;
    private final EnumMap<Direction, int[]> faces;
    private final EnumSet<Direction> dirty = EnumSet.noneOf(Direction.class);
    private final Deque<Snap> undo = new ArrayDeque<>();
    private int color = 0xFF000000 | PALETTE[14];
    private Tool tool = Tool.PENCIL;
    private EditBox hex;
    private Button toolBtn, faceBtn;
    private TextureAtlasSprite sprite;
    private int cx, cy, px, py;

    public PaintScreen(BlockPos pos, Direction face) {
        super(Component.translatable("screen.configurationblocks.paint.title"));
        this.pos = pos;
        this.face = face;
        this.faces = PaintClient.snapshot(pos);
    }

    @Override
    protected void init() {
        cx = width / 2 - SIZE - 10;
        cy = Math.max(24, height / 2 - 90);
        px = width / 2 + 10;
        py = cy;

        hex = new EditBox(font, px, py + 84, 72, 18, Component.empty());
        hex.setMaxLength(7);
        hex.setFilter(s -> s.matches("#?[0-9a-fA-F]{0,6}"));
        hex.setValue(String.format("#%06X", color & 0xFFFFFF));
        hex.setResponder(s -> {
            String t = s.startsWith("#") ? s.substring(1) : s;
            if (t.length() == 6) {
                try {
                    color = 0xFF000000 | Integer.parseInt(t, 16);
                } catch (NumberFormatException ignored) {
                }
            }
        });
        addRenderableWidget(hex);

        toolBtn = addRenderableWidget(Button.builder(toolText(), b -> {
            tool = Tool.values()[(tool.ordinal() + 1) % Tool.values().length];
            b.setMessage(toolText());
        }).bounds(px, py + 108, 150, 20).build());

        faceBtn = addRenderableWidget(Button.builder(faceText(), b -> {
            face = Direction.values()[(face.ordinal() + 1) % Direction.values().length];
            b.setMessage(faceText());
            refreshSprite();
        }).bounds(px, py + 132, 150, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.paint.undo"), b -> undo())
                .bounds(px, py + 156, 73, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.paint.clear"), b -> {
            pushUndo();
            int[] arr = pixels();
            java.util.Arrays.fill(arr, 0);
            dirty.add(face);
        }).bounds(px + 77, py + 156, 73, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.paint.save"), b -> save())
                .bounds(cx, cy + SIZE + 8, 78, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.configurationblocks.cancel"), b -> onClose())
                .bounds(cx + 82, cy + SIZE + 8, 78, 20).build());

        refreshSprite();
    }

    // ------------------------------------------------------------ helpers

    private Component toolText() {
        return Component.translatable("screen.configurationblocks.paint.tool",
                Component.translatable("screen.configurationblocks.paint.tool." + tool.name().toLowerCase()));
    }

    private Component faceText() {
        return Component.translatable("screen.configurationblocks.paint.face",
                Component.translatable("screen.configurationblocks.paint.face." + face.getName()));
    }

    private int[] pixels() {
        return faces.computeIfAbsent(face, d -> new int[N * N]);
    }

    private void refreshSprite() {
        sprite = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        try {
            BlockState st = mc.level.getBlockState(pos);
            BakedModel model = mc.getBlockRenderer().getBlockModel(st);
            List<BakedQuad> quads = model.getQuads(st, face, RandomSource.create(), ModelData.EMPTY, null);
            sprite = quads.isEmpty() ? model.getParticleIcon(ModelData.EMPTY) : quads.get(0).getSprite();
        } catch (Exception ignored) {
            sprite = null;
        }
    }

    private void setColor(int argb) {
        color = 0xFF000000 | (argb & 0xFFFFFF);
        hex.setValue(String.format("#%06X", color & 0xFFFFFF));
    }

    private int cellAt(double mx, double my) {
        if (mx < cx || my < cy || mx >= cx + SIZE || my >= cy + SIZE) return -1;
        int x = (int) ((mx - cx) / CELL), y = (int) ((my - cy) / CELL);
        return y * N + x;
    }

    private int swatchAt(double mx, double my) {
        for (int i = 0; i < PALETTE.length; i++) {
            int x = px + (i % 4) * (SW + SG), y = py + (i / 4) * (SW + SG);
            if (mx >= x && mx < x + SW && my >= y && my < y + SW) return i;
        }
        return -1;
    }

    private void pushUndo() {
        undo.push(new Snap(face, pixels().clone()));
        while (undo.size() > 30) undo.removeLast();
    }

    private void undo() {
        Snap s = undo.poll();
        if (s == null) return;
        faces.put(s.face(), s.pixels());
        dirty.add(s.face());
        if (s.face() != face) {
            face = s.face();
            faceBtn.setMessage(faceText());
            refreshSprite();
        }
    }

    private void set(int[] arr, int idx, int v) {
        if (arr[idx] != v) {
            arr[idx] = v;
            dirty.add(face);
        }
    }

    private void applyTool(int idx, int button) {
        int[] arr = pixels();
        Tool t = button == 1 ? Tool.ERASER : tool;
        switch (t) {
            case PENCIL -> set(arr, idx, color);
            case ERASER -> set(arr, idx, 0);
            case FILL -> fill(arr, idx, button == 1 ? 0 : color);
            case PICKER -> {
                if (arr[idx] != 0) setColor(arr[idx]);
            }
        }
    }

    private void fill(int[] arr, int start, int repl) {
        int target = arr[start];
        if (target == repl) return;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(start);
        while (!q.isEmpty()) {
            int i = q.poll();
            if (arr[i] != target) continue;
            arr[i] = repl;
            int x = i % N, y = i / N;
            if (x > 0) q.add(i - 1);
            if (x < N - 1) q.add(i + 1);
            if (y > 0) q.add(i - N);
            if (y < N - 1) q.add(i + N);
        }
        dirty.add(face);
    }

    private void save() {
        for (Direction d : dirty) {
            int[] arr = faces.get(d);
            if (arr != null) PacketDistributor.sendToServer(new PaintPayload(pos, d, arr.clone()));
        }
        onClose();
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int idx = cellAt(mx, my);
        if (idx >= 0) {
            if (button == 0 || button == 1) {
                if (tool != Tool.PICKER || button == 1) pushUndo();
                applyTool(idx, button);
            }
            return true;
        }
        int sw = swatchAt(mx, my);
        if (sw >= 0 && button == 0) {
            setColor(PALETTE[sw]);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        int idx = cellAt(mx, my);
        if (idx >= 0 && (button == 0 || button == 1) && (tool == Tool.PENCIL || tool == Tool.ERASER || button == 1)) {
            applyTool(idx, button);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, cy - 14, 0xFFFFFF);

        // base texture as reference, then the painted pixels on top
        if (sprite != null) g.blit(cx, cy, 0, SIZE, SIZE, sprite);
        else g.fill(cx, cy, cx + SIZE, cy + SIZE, 0xFF404040);

        int[] arr = faces.get(face);
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == 0) continue;
                int x = cx + (i % N) * CELL, y = cy + (i / N) * CELL;
                g.fill(x, y, x + CELL, y + CELL, arr[i]);
            }
        }
        for (int i = 0; i <= N; i++) {
            g.fill(cx + i * CELL, cy, cx + i * CELL + 1, cy + SIZE, 0x40000000);
            g.fill(cx, cy + i * CELL, cx + SIZE, cy + i * CELL + 1, 0x40000000);
        }
        int hover = cellAt(mouseX, mouseY);
        if (hover >= 0) {
            g.renderOutline(cx + (hover % N) * CELL, cy + (hover / N) * CELL, CELL, CELL, 0xFFFFFFFF);
        }

        for (int i = 0; i < PALETTE.length; i++) {
            int x = px + (i % 4) * (SW + SG), y = py + (i / 4) * (SW + SG);
            g.fill(x, y, x + SW, y + SW, 0xFF000000 | PALETTE[i]);
            if ((color & 0xFFFFFF) == PALETTE[i]) g.renderOutline(x - 1, y - 1, SW + 2, SW + 2, 0xFFFFFFFF);
        }
        g.fill(px + 78, py + 84, px + 96, py + 102, 0xFF000000 | color);
        g.renderOutline(px + 78, py + 84, 18, 18, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("screen.configurationblocks.paint.hint"), px, py + 74 - 0, 0xA0A0A0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
