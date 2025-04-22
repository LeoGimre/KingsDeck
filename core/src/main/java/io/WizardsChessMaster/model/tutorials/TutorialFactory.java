package io.WizardsChessMaster.model.tutorials;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Factory for accessing TutorialConfig instances based on external JSON configurations.
 * Loads configurations from the 'assets/tutorials/' directory at startup.
 */
public class TutorialFactory {

    private static final String TAG = "TutorialFactory";
    private static final String CONFIG_DIR = "tutorials/";

    // Store loaded configurations mapped by topicId
    private static final Map<String, TutorialConfig> tutorialConfigs = new HashMap<>();

    static {
        Gdx.app.log(TAG, "Initializing TutorialFactory by loading configurations...");
        loadTutorialConfigurations();
        Gdx.app.log(TAG, "TutorialFactory initialized. Loaded " + tutorialConfigs.size() + " tutorial topics.");
    }

    /**
     * Loads all .json files from the assets/tutorials directory and parses them.
     */
    private static void loadTutorialConfigurations() {
        tutorialConfigs.clear();
        Json jsonParser = new Json();

        try {
            FileHandle dirHandle = Gdx.files.internal(CONFIG_DIR);
            if (!dirHandle.exists() || !dirHandle.isDirectory()) {
                Gdx.app.error(TAG, "Tutorial configuration directory not found or not a directory: " + dirHandle.path());
                return;
            }

            FileHandle[] files = dirHandle.list(".json");
            Gdx.app.log(TAG, "Found " + files.length + " potential tutorial JSON config files in " + dirHandle.path());

            for (FileHandle file : files) {
                try {
                    Gdx.app.debug(TAG, "Attempting to load tutorial config: " + file.name());
                    TutorialConfig config = jsonParser.fromJson(TutorialConfig.class, file);

                    if (config == null || config.topicId == null || config.topicId.trim().isEmpty()) {
                        Gdx.app.error(TAG, "Skipping tutorial config file " + file.name() + ": Missing or empty topicId.");
                        continue;
                    }

                    String topicIdKey = config.topicId.trim().toLowerCase();

                    if (tutorialConfigs.containsKey(topicIdKey)) {
                        Gdx.app.error(TAG, "Warning: Duplicate tutorial topicId '" + topicIdKey + "' found in " + file.name() + ". Skipping this file.");
                    } else {
                        if (config.title == null || config.title.trim().isEmpty()) {
                            Gdx.app.error(TAG, "Warning: Tutorial config '" + topicIdKey + "' is missing 'title'.");
                            config.title = config.topicId;
                        }
                        if (config.content == null) {
                            Gdx.app.error(TAG, "Warning: Tutorial config '" + topicIdKey + "' is missing 'content'.");
                            config.content = "Content missing for this topic.";
                        }

                        tutorialConfigs.put(topicIdKey, config);
                        Gdx.app.log(TAG, "Loaded tutorial topic: '" + topicIdKey + "' ('" + config.title + "') from " + file.name());
                    }
                } catch (SerializationException e) {
                    Gdx.app.error(TAG, "Error parsing tutorial config file: " + file.name(), e);
                    Gdx.app.error(TAG, "Check JSON structure matches TutorialConfig.java.");
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Unexpected error loading tutorial config file: " + file.name(), e);
                }
            }
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error accessing tutorial configuration directory: " + CONFIG_DIR, e);
        }
        if (tutorialConfigs.isEmpty()) {
            Gdx.app.log(TAG, "Warning: No tutorial configurations were successfully loaded!");
        }
    }

    /**
     * Gets the configuration data for a specific tutorial topic ID (case-insensitive).
     * @param topicId The unique ID of the topic.
     * @return The TutorialConfig object, or null if not found.
     */
    public static TutorialConfig getConfig(String topicId) {
        if (topicId == null) return null;
        return tutorialConfigs.get(topicId.trim().toLowerCase());
    }

    /**
     * Gets a collection of all loaded tutorial configurations.
     * The order is not guaranteed.
     * @return An unmodifiable collection of TutorialConfig objects.
     */
    public static Collection<TutorialConfig> getAllConfigs() {
        return Collections.unmodifiableCollection(tutorialConfigs.values());
    }

    /**
     * Gets a list of all loaded tutorial configurations, sorted alphabetically by title.
     * @return A sorted list of TutorialConfig objects.
     */
    public static List<TutorialConfig> getAllConfigsSortedByTitle() {
        List<TutorialConfig> sortedList = new ArrayList<>(tutorialConfigs.values());
        sortedList.sort(Comparator.comparing(config -> config.title, String.CASE_INSENSITIVE_ORDER));
        return sortedList;
    }

    /**
     * Gets the set of available tutorial topic IDs (lowercase) loaded from configurations.
     * @return An unmodifiable set of topic ID strings.
     */
    public static Set<String> getAvailableTopicIds() {
        return Collections.unmodifiableSet(tutorialConfigs.keySet());
    }


    /**
     * Reloads tutorial configurations from the JSON files.
     */
    public static void reloadConfigurations() {
        Gdx.app.log(TAG, "Reloading tutorial configurations...");
        loadTutorialConfigurations();
        Gdx.app.log(TAG, "Tutorial configurations reloaded. Found " + tutorialConfigs.size() + " topics.");
    }
}