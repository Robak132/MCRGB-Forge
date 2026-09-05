# MCRGB Mod for Minecraft Fabric 1.20.1+

- [Modrinth](https://modrinth.com/mod/mcrgb_forge)

- [Curseforge](https://legacy.curseforge.com/minecraft/mc-mods/mcrgb_forge-a-colour-picker-for-minecraft-blocks)

- [Discord](https://discord.gg/883ybFjSEf)

- Trailer:

[![Youtube Trailer](https://github.com/bacco-bacco/MCRGB/assets/22712685/83dd58c1-7452-449e-bb7d-36e930dd1182)](https://www.youtube.com/watch?v=MNBBPMVZp5g)

## Requirements:

- Minecraft 1.20.1+
- Fabric
- Fabric API

## Recommended:

- Cloth Config

## Features:

### A Color Picker for Minecraft Blocks!

MCRGB is a client-side Minecraft Mod designed to assist building. It allows you to search for blocks based on the
dominant colors in their textures.
Press the color picker key (I by default) to open the color picker GUI.
![Color Picker GUI](https://github.com/bacco-bacco/MCRGB/assets/22712685/2cb6bf2f-5c4c-4ee9-865a-78af099a416c)

### HSV, HSL, RGB and Hex Support!

Input your colors using your favourite color space, and the mod will suggest blocks that closely match that color.
If you are in Creative Mode and have Operator Level Permissions, you can click blocks to give them to yourself.

You can also see the breakdown of a block's color info by hovering over it. This tells you the percentage each color
takes up in each texture, and which
blockstates it corresponds to.
Optional: You can also view this info in regular inventories by changing the config.
![Tooltip for Pink Concrete Powder with Color Info](https://github.com/bacco-bacco/MCRGB/assets/22712685/1faa3ce7-027a-4a42-99b8-eead07c98fba)

![Tooltip for Birch Log with Color Info](https://github.com/bacco-bacco/MCRGB/assets/22712685/6cb37ba7-db54-4b4e-8a6c-2755b333b98b)

![Tooltip for Redstone Lamp with Color Info](https://github.com/bacco-bacco/MCRGB/assets/22712685/c4dcae23-e560-4606-b493-83e4201acb11)

### Build using Color Theory

![Funky House Built with Complementary Colors](https://github.com/bacco-bacco/MCRGB/assets/22712685/82634e97-aad7-4a53-b664-1bb6d5ccfa24)
![Cottage Built with an Adobe Color Palette](https://github.com/bacco-bacco/MCRGB/assets/22712685/419a3c95-0197-4b8d-a014-37a91d647b4f)

### Easily /give Yourself Dyed Leather Armor

![Color Picker UI with the hex color "#CEBBED" selected. A preview is shown for leather armour and horse armour](https://github.com/bacco-bacco/MCRGB/assets/22712685/67c68653-350b-4aa7-8da0-779bf8f06ee0)

![A Player wearing leather armour of the hex color "#CEBBED"](https://github.com/bacco-bacco/MCRGB/assets/22712685/b79150e5-5ec7-4820-9935-21795273d3ae)

## How it works:

On first launch, MCRGB will generate a file, located in `.minecraft/mcrgb_forge_colors/file.json`
MCRGB attempts to scan every block texture in the game and calculates the dominant colors by grouping together similar
pixels based on their euclidean distance
in sRGB space, and calculating the mean average of each group. The results are saved in this file. If you ever need to
regenerate the file (if you've changed
resource packs or added new mods which add more blocks), click the "Refresh" button in the color picker UI.

When you input a color to the color picker, the list of blocks is sorted by the euclidean distance to each of the
dominant colors in each texture. Each texture
is weighted according to how much of that color takes up in the texture.

### EMI color search

When EMI is installed, prefix its search with `^` and a six-digit RGB hex color to sort matching block items by color
similarity:

- `^ff0000` sorts all indexed blocks from closest to furthest from red.
- `^ff0000 wool` applies EMI's normal `wool` filter, then sorts the results by their similarity to red.

Non-block items and blocks without cached color data remain after color-scored results in their original order.
