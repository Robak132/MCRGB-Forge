package io.github.robak132.mcrgb_forge.client.serialization;

import com.google.common.reflect.TypeToken;
import io.github.robak132.mcrgb_forge.client.analysis.SpriteDetails;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

@Slf4j(topic = "MCRGB")
public class CacheSerializer implements Serializer<Map<Block, List<SpriteDetails>>> {

    public Path getFile() {
        return Path.of("mcrgb_forge_colors", "cache.json");
    }

    public Map<Block, List<SpriteDetails>> load() {
        if (!exists()) {
            return Collections.emptyMap();
        }

        try (Reader reader = Files.newBufferedReader(getFile())) {
            var rawType = new TypeToken<Map<String, List<SpriteDetails>>>() {
            }.getType();
            Map<String, List<SpriteDetails>> raw = GSON.fromJson(reader, rawType);

            if (raw == null || raw.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<Block, List<SpriteDetails>> mapped = new HashMap<>();

            for (var e : raw.entrySet()) {
                try {
                    ResourceLocation rl = ResourceLocation.parse(e.getKey());
                    Block b = ForgeRegistries.BLOCKS.getValue(rl);
                    if (b != null) {
                        mapped.put(b, e.getValue());
                    }
                } catch (Exception ex) {
                    log.warn("Skipping malformed block key {} in cache", e.getKey());
                }
            }

            return mapped.isEmpty() ? Collections.emptyMap() : mapped;

        } catch (Exception e) {
            log.error("Failed to load cache: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public void save(Map<Block, List<SpriteDetails>> data) {
        try {
            Files.createDirectories(getFile().getParent());

            Map<String, List<SpriteDetails>> stringMap = new HashMap<>();

            for (var e : data.entrySet()) {
                Block block = e.getKey();
                ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
                if (key != null) {
                    stringMap.put(key.toString(), e.getValue());
                }
            }

            try (Writer writer = Files.newBufferedWriter(getFile())) {
                GSON.toJson(stringMap, writer);
            }

        } catch (IOException e) {
            log.error("Failed to save cache: {}", e.getMessage());
        }
    }

    @Override
    public boolean delete() {
        try {
            return Files.deleteIfExists(getFile());
        } catch (IOException e) {
            log.error("Could not delete cache file: {}", e.getMessage());
            return false;
        }
    }
}
