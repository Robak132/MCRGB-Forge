package io.github.robak132.mcrgb_forge.client.serialization;

import io.github.robak132.libgui_forge.widget.data.colors.RGB;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteColor;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import io.github.robak132.mcrgb_forge.client.analysis.TextureNoise;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

@Slf4j(topic = "MCRGB")
public class CacheSerializer implements Serializer<Map<Block, List<SpriteDetails>>> {

    private static final int CACHE_MAGIC = 0x4D435247;
    private static final int CACHE_VERSION = 1;
    private static final int COLOR_BYTES = Integer.BYTES + Byte.BYTES;

    @Override
    public Path getFile() {
        return Path.of("mcrgb_forge_colors.bin");
    }

    @Override
    public Map<Block, List<SpriteDetails>> load() {
        long started = System.nanoTime();
        if (!exists()) {
            log.info("Color cache not found at {}", getFile().toAbsolutePath());
            return Collections.emptyMap();
        }

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(getFile())))) {
            if (input.readInt() != CACHE_MAGIC || input.readUnsignedByte() != CACHE_VERSION) {
                throw new IOException("Unsupported color cache version");
            }
            int blockCount = readCount(input, ForgeRegistries.BLOCKS.getValues().size(), "block");
            Map<Block, List<SpriteDetails>> mapped = new HashMap<>(blockCount);
            boolean containsSprite = false;

            for (int i = 0; i < blockCount; i++) {
                ResourceLocation blockId = readResourceLocation(input, "block");
                List<SpriteDetails> sprites = readSprites(input);
                containsSprite |= !sprites.isEmpty();

                Block block = ForgeRegistries.BLOCKS.getValue(blockId);
                if (block != null) {
                    mapped.put(block, sprites);
                }
            }

            if (input.read() != -1) {
                throw new IOException("Unexpected data after cache entries");
            }
            Map<Block, List<SpriteDetails>> result = mapped.isEmpty() || !containsSprite
                    ? Collections.emptyMap()
                    : mapped;
            log.info("Loaded color cache in {} ms: {} bytes, {} blocks, {} sprites, {} colors",
                    elapsedMillis(started), Files.size(getFile()), result.size(),
                    countSprites(result), countColors(result));
            return result;
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to load color cache after {} ms; regenerating it: {}",
                    elapsedMillis(started), e.getMessage());
            return Collections.emptyMap();
        }
    }

    private @NotNull @Unmodifiable List<SpriteDetails> readSprites(DataInputStream input) throws IOException {
        int spriteCount = readCount(input, input.available(), "sprite");
        List<SpriteDetails> sprites = new ArrayList<>(spriteCount);

        for (int i = 0; i < spriteCount; i++) {
            ResourceLocation spriteId = readResourceLocation(input, "sprite");
            int globalNoise = input.readUnsignedByte();
            int spatialNoise = input.readUnsignedByte();
            if (globalNoise > 100 || spatialNoise > 100) {
                throw new IOException("Texture noise is outside 0..100");
            }

            int colorCount = readCount(input, input.available() / COLOR_BYTES, "color");
            if (colorCount == 0) {
                throw new IOException("Sprite has no colors");
            }
            List<SpriteColor> colors = new ArrayList<>(colorCount);
            int totalWeight = 0;
            for (int j = 0; j < colorCount; j++) {
                RGB color = new RGB(input.readInt());
                int weight = input.readUnsignedByte();
                if (weight > 100) {
                    throw new IOException("Color weight is outside 0..100");
                }
                colors.add(new SpriteColor(color, weight));
                totalWeight += weight;
            }
            if (totalWeight != 100) {
                throw new IOException("Sprite color weights do not total 100");
            }

            sprites.add(new SpriteDetails(spriteId.toString(), List.copyOf(colors),
                    new TextureNoise(globalNoise, spatialNoise)));
        }
        return List.copyOf(sprites);
    }

    private @NotNull ResourceLocation readResourceLocation(
            @NotNull DataInputStream input, String type) throws IOException {
        String value = input.readUTF();
        if (!value.contains(":")) {
            throw new IOException("Missing namespace in " + type + " identifier");
        }
        ResourceLocation location = ResourceLocation.tryParse(value);
        if (location == null) {
            throw new IOException("Invalid " + type + " identifier");
        }
        return location;
    }

    private int readCount(@NotNull DataInputStream input, int maximum, String type) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > maximum) {
            throw new IOException("Invalid " + type + " count");
        }
        return count;
    }

    @Override
    public void save(@NotNull Map<Block, List<SpriteDetails>> data) {
        long started = System.nanoTime();
        try {
            List<Map.Entry<ResourceLocation, List<SpriteDetails>>> entries = new ArrayList<>(data.size());
            data.forEach((block, sprites) -> {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
                if (id != null) {
                    entries.add(Map.entry(id, sprites));
                }
            });

            try (DataOutputStream output = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(getFile())))) {
                output.writeInt(CACHE_MAGIC);
                output.writeByte(CACHE_VERSION);
                output.writeInt(entries.size());
                for (Map.Entry<ResourceLocation, List<SpriteDetails>> entry : entries) {
                    output.writeUTF(entry.getKey().toString());
                    writeSprites(output, entry.getValue());
                }
            }
            log.info("Saved color cache in {} ms: {} bytes, {} blocks, {} sprites, {} colors",
                    elapsedMillis(started), Files.size(getFile()), entries.size(),
                    countSprites(data), countColors(data));
        } catch (IOException e) {
            log.error("Failed to save color cache after {} ms: {}", elapsedMillis(started), e.getMessage());
        }
    }

    private int countSprites(Map<Block, List<SpriteDetails>> data) {
        return data.values().stream().mapToInt(List::size).sum();
    }

    private int countColors(Map<Block, List<SpriteDetails>> data) {
        return data.values().stream()
                .flatMap(List::stream)
                .mapToInt(sprite -> sprite.getColors().size())
                .sum();
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private void writeSprites(
            @NotNull DataOutputStream output, @NotNull List<SpriteDetails> sprites) throws IOException {
        output.writeInt(sprites.size());
        for (SpriteDetails sprite : sprites) {
            output.writeUTF(sprite.getName());
            output.writeByte(sprite.getNoise().global());
            output.writeByte(sprite.getNoise().kernel());
            output.writeInt(sprite.getColors().size());
            for (SpriteColor color : sprite.getColors()) {
                output.writeInt(color.color().argb());
                output.writeByte(color.weight());
            }
        }
    }

}
