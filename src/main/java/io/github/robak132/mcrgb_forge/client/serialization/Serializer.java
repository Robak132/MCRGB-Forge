package io.github.robak132.mcrgb_forge.client.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Serializer<T> {

    Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    Path getFile();

    default boolean exists() {
        return Files.exists(getFile());
    }

    T load();

    void save(T palettes);
}
