package io.WizardsChessMaster.model.spells;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;
import io.WizardsChessMaster.model.spells.SpellConfig.SpellEffectComponentConfig;

import java.util.*;

/**
 * Factory for creating Spell instances based on external JSON configurations.
 * Loads configurations from the 'assets/spells/' directory at startup.
 */
public class SpellFactory {

    private static final String TAG = "SpellFactory";
    private static final String CONFIG_DIR = "spells/";

    // Store loaded configurations
    private static final Map<String, SpellConfig> spellConfigs = new HashMap<>();
    // Store prototype instances
    private static final Map<String, Spell> spellPrototypes = new HashMap<>();
    private static final SpellEffectComponentFactory effectComponentFactory = new SpellEffectComponentFactory();

    static {
        Gdx.app.log(TAG, "Initializing SpellFactory by loading configurations...");
        loadSpellConfigurations();
        Gdx.app.log(TAG, "SpellFactory initialized. Loaded " + spellConfigs.size() + " spell configurations.");
    }

    /**
     * Loads all .json files from the assets/spells directory and parses them into SpellConfig objects.
     */
    private static void loadSpellConfigurations() {
        spellConfigs.clear();
        spellPrototypes.clear();
        Json jsonParser = new Json();

        try {
            jsonParser.setElementType(SpellConfig.class, "effectComponents", SpellEffectComponentConfig.class);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error setting element types for JSON parser. Spell parsing might fail.", e);
        }

        try {
            FileHandle dirHandle = Gdx.files.internal(CONFIG_DIR);
            if (!dirHandle.exists() || !dirHandle.isDirectory()) {
                Gdx.app.error(TAG, "Spell configuration directory not found or not a directory: " + dirHandle.path());
                return;
            }

            FileHandle[] files = dirHandle.list(".json");
            Gdx.app.log(TAG, "Found " + files.length + " potential spell JSON config files in " + dirHandle.path());

            for (FileHandle file : files) {
                try {
                    Gdx.app.debug(TAG, "Attempting to load spell config: " + file.name());
                    SpellConfig config = jsonParser.fromJson(SpellConfig.class, file);

                    if (config == null || config.typeName == null || config.typeName.trim().isEmpty()) {
                        Gdx.app.error(TAG, "Skipping spell config file " + file.name() + ": Missing or empty typeName.");
                        continue;
                    }

                    String typeNameUpper = config.typeName.toUpperCase();
                    if (spellConfigs.containsKey(typeNameUpper)) {
                        Gdx.app.error(TAG, "Warning: Duplicate spell typeName '" + typeNameUpper + "' found in " + file.name() + ". Skipping this file.");
                    } else {
                        if (config.iconBaseName == null || config.iconBaseName.trim().isEmpty()) {
                            Gdx.app.error(TAG, "Warning: Spell config '" + typeNameUpper + "' is missing 'iconBaseName'.");
                        }
                        if (config.effectComponents == null) {
                            config.effectComponents = new ArrayList<>();
                        }

                        spellConfigs.put(typeNameUpper, config);
                        Gdx.app.log(TAG, "Loaded spell configuration: '" + typeNameUpper + "' from " + file.name());

                        try {
                            Spell proto = createSpellInternal(config);
                            spellPrototypes.put(typeNameUpper, proto);
                        } catch (Exception e) {
                            Gdx.app.error(TAG, "Failed to create prototype for spell: " + typeNameUpper, e);
                        }
                    }
                } catch (SerializationException e) {
                    Gdx.app.error(TAG, "Error parsing spell config file: " + file.name(), e);
                    Gdx.app.error(TAG, "Check JSON structure matches SpellConfig.java.");
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Unexpected error loading spell config file: " + file.name(), e);
                }
            }
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error accessing spell configuration directory: " + CONFIG_DIR, e);
        }
        if (spellConfigs.isEmpty()) {
            Gdx.app.log(TAG, "Warning: No spell configurations were successfully loaded!");
        }
    }

    /**
     * Internal helper to create and configure a spell from its config.
     */
    private static Spell createSpellInternal(SpellConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("SpellConfig cannot be null.");
        }
        try {
            ConfigurableSpell newSpell = new ConfigurableSpell();
            newSpell.configure(config, effectComponentFactory);

            if (!config.typeName.equalsIgnoreCase(newSpell.getTypeName())) {
                Gdx.app.error(TAG, "Warning - Mismatch between config typeName '" + config.typeName + "' and created spell.getTypeName() '" + newSpell.getTypeName() + "'.");
            }
            return newSpell;
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error creating or configuring spell from config '" + config.typeName + "'.", e);
            throw new RuntimeException("Failed to create spell from config '" + config.typeName + "'. Cause: " + e.getMessage(), e);
        }
    }


    /**
     * Creates a new Spell instance based on its type name using loaded configurations.
     */
    public static Spell createSpell(String typeName) {
        if (typeName == null) {
            throw new IllegalArgumentException("Spell type name cannot be null.");
        }
        String upperTypeName = typeName.toUpperCase();
        SpellConfig config = spellConfigs.get(upperTypeName);

        if (config == null) {
            Gdx.app.error(TAG, "Unknown spell type requested: '" + typeName + "'. Available types: " + getAvailableSpellTypes());
            throw new IllegalArgumentException("Unknown spell type requested: '" + typeName + "'");
        }
        return createSpellInternal(config);
    }

    /**
     * Gets the set of available spell type names (uppercase) loaded from configurations.
     */
    public static Set<String> getAvailableSpellTypes() {
        return Collections.unmodifiableSet(spellConfigs.keySet());
    }

    /**
     * Gets a collection of prototype instances for all available spell types.
     */
    public static Collection<Spell> getSpellPrototypes() {
        if (spellPrototypes.size() != spellConfigs.size()) {
            Gdx.app.error(TAG, "Prototype count mismatch! Expected " + spellConfigs.size() + ", found " + spellPrototypes.size());
        }
        return Collections.unmodifiableCollection(spellPrototypes.values());
    }

    /**
     * Gets a specific spell prototype instance by type name (case-insensitive).
     */
    public static Spell getPrototype(String typeName) {
        if (typeName == null) return null;
        String upperTypeName = typeName.toUpperCase();

        Spell proto = spellPrototypes.get(upperTypeName);
        if (proto == null) {
            SpellConfig config = spellConfigs.get(upperTypeName);
            if (config != null) {
                Gdx.app.log(TAG, "Attempting to create missing prototype on demand for: " + typeName);
                try {
                    proto = createSpellInternal(config);
                    spellPrototypes.put(upperTypeName, proto);
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Failed to create on-demand prototype for spell type: " + upperTypeName, e);
                }
            } else {
                Gdx.app.error(TAG,"Prototype not found for spell type: " + typeName + " (and no config found)");
            }
        }
        return proto;
    }

    /**
     * Gets the loaded configuration data for a specific spell type.
     */
    public static SpellConfig getConfig(String typeName) {
        if (typeName == null) return null;
        return spellConfigs.get(typeName.toUpperCase());
    }

    /**
     * Reloads spell configurations.
     */
    public static void reloadConfigurations() {
        Gdx.app.log(TAG, "Reloading spell configurations...");
        loadSpellConfigurations();
        Gdx.app.log(TAG, "Spell configurations reloaded. Found " + spellConfigs.size() + " types.");
    }
}