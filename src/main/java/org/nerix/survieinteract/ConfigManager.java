package org.nerix.survieinteract;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath;
    private static JsonObject root;

    public static void init() {
        configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("survieinteract.json");

        load();
    }

    private static void load() {
        if (!Files.exists(configPath)) {
            createDefault();
            save();
            return;
        }

        try (Reader r = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("[SurvieInteract] Erreur lecture config, recréation: " + e.getMessage());
            createDefault();
            save();
        }
    }

    private static void save() {
        try (Writer w = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            System.out.println("[SurvieInteract] Erreur sauvegarde config: " + e.getMessage());
        }
    }

    private static void createDefault() {
        root = new JsonObject();
        root.addProperty("default_lives", 3);
        root.add("players", new JsonObject());
    }

    private static JsonObject getPlayerNode(UUID uuid) {
        JsonObject players = root.getAsJsonObject("players");
        String key = uuid.toString();

        if (!players.has(key)) {
            JsonObject p = new JsonObject();
            p.addProperty("consent", false);
            p.addProperty("lives", root.get("default_lives").getAsInt());
            players.add(key, p);
            save();
        }

        return players.getAsJsonObject(key);
    }

    // ---------- consent ----------

    public static boolean getConsent(UUID uuid) {
        return getPlayerNode(uuid).get("consent").getAsBoolean();
    }

    public static void setConsent(UUID uuid, boolean value) {
        getPlayerNode(uuid).addProperty("consent", value);
        save();
    }

    // ---------- vies ----------

    public static int getLives(UUID uuid) {
        return getPlayerNode(uuid).get("lives").getAsInt();
    }

    public static void setLives(UUID uuid, int lives) {
        getPlayerNode(uuid).addProperty("lives", lives);
        save();
    }

    public static void addLives(UUID uuid, int amount) {
        int current = getLives(uuid);
        setLives(uuid, current + amount);
    }

    public static void decrementLife(UUID uuid) {
        int l = getLives(uuid);
        if (l > 0) {
            setLives(uuid, l - 1);
        }
    }

    public static boolean isDead(UUID uuid) {
        return getLives(uuid) <= 0;
    }
}
