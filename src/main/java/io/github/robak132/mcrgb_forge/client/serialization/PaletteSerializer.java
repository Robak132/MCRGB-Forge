package io.github.robak132.mcrgb_forge.client.serialization;

import com.google.common.reflect.TypeToken;
import io.github.robak132.mcrgb_forge.client.analysis.Palette;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "MCRGB")
public class PaletteSerializer implements Serializer<List<Palette>> {

    @Override
    public Path getFile() {
        return Path.of("mcrgb_forge_colors", "palettes.json");
    }

    @Override
    public boolean delete() {
        try {
            return Files.deleteIfExists(getFile());
        } catch (Exception e) {
            log.error("Failed to delete palettes file: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Palette> load() {
        try {
            if (!exists()) {
                return new ArrayList<>();
            }
            String json = Files.readString(getFile());
            List<Palette> data = GSON.fromJson(json, new TypeToken<List<Palette>>() {
            }.getType());
            return data != null ? data : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Failed to load palettes: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void save(List<Palette> palettes) {
        try {
            Files.createDirectories(getFile().getParent());
            Files.writeString(getFile(), GSON.toJson(palettes));

        } catch (Exception e) {
            log.error("Failed to save palettes: {}", e.getMessage());
        }
    }
}
