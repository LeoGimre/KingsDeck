package io.WizardsChessMaster.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to load game settings from a JSON configuration file.
 */
public class ConfigLoader {

    private static final String TAG = "ConfigLoader";
    private static final String CONFIG_FILE_PATH = "game_settings.json";

    private static GameSettings loadedSettings = null;

    /**
     * Loads the game settings from the configuration file.
     * If already loaded, returns the cached settings.
     * If loading fails, returns default settings and logs an error.
     * @return The loaded or default GameSettings.
     */
    public static GameSettings getSettings() {
        if (loadedSettings == null) {
            loadConfig();
        }
        // Ensure we always return a non-null object, even if loading failed
        return (loadedSettings != null) ? loadedSettings : new GameSettings();
    }

    private static void loadConfig() {
        Json json = new Json();
        // Add type hints for lists within nested classes
        try {
            json.setElementType(GameSettings.MatchmakingSettings.class, "pointLimits", Integer.class);
            json.setElementType(GameSettings.MatchmakingSettings.class, "timeLimits", String.class);
            json.setElementType(GameSettings.DeckBuildingSettings.class, "pointLimits", Integer.class);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error setting element types for JSON parser. Parsing might fail.", e);
        }


        FileHandle fileHandle = Gdx.files.internal(CONFIG_FILE_PATH);

        if (!fileHandle.exists()) {
            Gdx.app.error(TAG, "Configuration file not found: " + CONFIG_FILE_PATH + ". Using default settings.");
            loadedSettings = new GameSettings();
            return;
        }

        try {
            loadedSettings = json.fromJson(GameSettings.class, fileHandle);
            Gdx.app.log(TAG, "Successfully loaded game settings from: " + CONFIG_FILE_PATH);
            // Basic validation after loading
            if (loadedSettings == null) {
                Gdx.app.error(TAG, "Parsed settings object is null. Using default settings.");
                loadedSettings = new GameSettings();
            } else {
                // Ensure nested objects are not null
                if (loadedSettings.matchmaking == null) {
                    Gdx.app.log(TAG, "Matchmaking settings missing in JSON, using defaults.");
                    loadedSettings.matchmaking = new GameSettings.MatchmakingSettings();
                }
                if (loadedSettings.deckBuilding == null) {
                    Gdx.app.log(TAG, "DeckBuilding settings missing in JSON, using defaults.");
                    loadedSettings.deckBuilding = new GameSettings.DeckBuildingSettings();
                }
                // Ensure lists are not null and have defaults if empty
                if (loadedSettings.matchmaking.pointLimits == null || loadedSettings.matchmaking.pointLimits.isEmpty()) {
                    Gdx.app.log(TAG, "Matchmaking pointLimits missing or empty, using default [40].");
                    loadedSettings.matchmaking.pointLimits = new ArrayList<>(List.of(40));
                }
                if (loadedSettings.matchmaking.timeLimits == null || loadedSettings.matchmaking.timeLimits.isEmpty()) {
                    Gdx.app.log(TAG, "Matchmaking timeLimits missing or empty, using default ['5 min'].");
                    loadedSettings.matchmaking.timeLimits = new ArrayList<>(List.of("5 min"));
                }
                if (loadedSettings.deckBuilding.pointLimits == null || loadedSettings.deckBuilding.pointLimits.isEmpty()) {
                    Gdx.app.log(TAG, "DeckBuilding pointLimits missing or empty, using default [40].");
                    loadedSettings.deckBuilding.pointLimits = new ArrayList<>(List.of(40));
                }
            }

        } catch (SerializationException e) {
            Gdx.app.error(TAG, "Error parsing configuration file: " + CONFIG_FILE_PATH, e);
            Gdx.app.error(TAG, "Check JSON structure matches GameSettings.java. Using default settings.");
            loadedSettings = new GameSettings();
        } catch (Exception e) {
            Gdx.app.error(TAG, "Unexpected error loading configuration file: " + CONFIG_FILE_PATH, e);
            loadedSettings = new GameSettings();
        }
    }

    /**
     * Forces a reload of the configuration file.
     */
    public static void reloadConfig() {
        Gdx.app.log(TAG, "Reloading game settings...");
        loadedSettings = null;
        loadConfig();
    }
}