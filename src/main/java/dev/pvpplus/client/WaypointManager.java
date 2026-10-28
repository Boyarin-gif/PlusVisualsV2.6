package dev.pvpplus.client;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

public final class WaypointManager {
    public static class Waypoint {
        public String name;
        public double x, y, z;
        public String dim;
        public boolean quick;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Waypoint> LIST = new ArrayList<>();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pvpplus_waypoints.json");
    }

    public static List<Waypoint> all() {
        return LIST;
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            try (Reader r = Files.newBufferedReader(f)) {
                Type t = new TypeToken<List<Waypoint>>() {}.getType();
                List<Waypoint> l = GSON.fromJson(r, t);
                if (l != null) {
                    LIST.clear();
                    LIST.addAll(l);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(LIST, w);
        } catch (Exception ignored) {
        }
    }
}
