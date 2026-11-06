# Configuration Blocks

[English](README.md) | [Русский](README.ru.md)

**NeoForge 1.21.1** · MIT

Adds a single item - the **Configurator** (a paper). Right-click any placed block with it to change almost any of its properties without replacing the block.

## Usage

* **Recipe:** paper + redstone + glowstone dust + iron ingot (shapeless). Also in the *Tools & Utilities* creative tab.
* **Right-click a block** while holding the Configurator - the settings screen opens.
* **Sneak + right-click** - reset all settings of that block.

## Configurable properties

| Property | Effect |
|---|---|
| Hardness | Mining speed. `-1` = unbreakable (like bedrock). Empty = vanilla |
| Blast resistance | `17`+ = immune to explosions; lower values give a proportional chance to survive |
| Light 0-15 | Light around the block (via invisible `minecraft:light` blocks in adjacent air) |
| Redstone power 0-15 | The block emits a redstone signal |
| Bounciness | Acts like a trampoline and cancels fall damage (sneak to avoid bouncing) |
| Gravity | The block falls like sand; its configuration and contents travel with it |
| Storage settings | For chest / shulker: **rows** (1-6, chest only), **stack limit per slot** (1-99; unstackable items stay at 1), and a **lock** (only the owner or ops can open, edit or break the block; furnaces can be locked too) |
| Painting | **Paint...** button opens a 16x16 pixel editor for the face you clicked (switch faces inside the editor). Tools: pencil, eraser, fill, picker, undo; 16 dye colors plus any `#RRGGBB`; right mouse button erases. The original texture is shown underneath, unpainted pixels stay vanilla |
| Function | Chest (27 slots, persistent), **Shulker Box** (27 slots), Ender Chest, Crafting Table, Furnace, Blast Furnace, Smoker, Anvil, Enchanting Table, Smithing Table, Grindstone, Loom, Cartography Table, Stonecutter, Jukebox (right-click with a music disc), **Spawner** (spawns the entity whose id you type, e.g. `minecraft:zombie`) |

Settings are bound to the **specific block in the world** and stored per dimension. If the block is broken or replaced, its settings are removed and stored items are dropped.

## Limitations

* Works on placed blocks, not on a block type globally.
* Blocks that already have a block entity (real chests etc.) cannot use gravity.
* A block moved by a piston loses its settings.
* Blast resistance can only increase protection; bedrock cannot be made explodable.
* Spawner uses vanilla spawner rules (light, space, player range 16 blocks) and default settings.
* Shulker function is a 27-slot container; contents drop as items when the block is broken (they are not kept inside the block item).
* Paintings are drawn as an overlay, so they fit full-cube blocks best (stairs, slabs and plants show the picture on the full cube face). Faces hidden by solid neighbors are not drawn, and paintings render within 128 blocks.
* The mod is required on both client and server. One mixin is used (`BlockStateBase`).

## Uploading to GitHub

```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/<YOUR_NAME>/configuration-blocks.git
git push -u origin main
```
