package io.WizardsChessMaster.model.pieces;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;
import java.util.ArrayList;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.move.MoveComponentFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Factory for creating Piece instances based on external JSON configurations.
 * Loads configurations from the 'assets/pieces/' directory at startup.
 * Includes type hints for Json parser.
 */
public class PieceFactory {

    private static final String TAG = "PieceFactory";
    private static final String CONFIG_DIR = "pieces/";

    private static final Map<String, PieceConfig> pieceConfigs = new HashMap<>();
    private static final Map<String, Piece> piecePrototypes = new HashMap<>();
    private static final MoveComponentFactory moveComponentFactory = new MoveComponentFactory();

    static {
        Gdx.app.log(TAG, "Initializing PieceFactory by loading configurations...");
        loadPieceConfigurations();
        Gdx.app.log(TAG, "PieceFactory initialized. Loaded " + pieceConfigs.size() + " piece configurations.");
    }

    /**
     * Loads all .json files from the assets/pieces directory and parses them into PieceConfig objects.
     */
    private static void loadPieceConfigurations() {
        pieceConfigs.clear();
        Json jsonParser = new Json();
        try {
            // Hint for the List<MoveComponentConfig> inside PieceConfig
            jsonParser.setElementType(PieceConfig.class, "moveComponents", PieceConfig.MoveComponentConfig.class);
            // Hint for the HashMap<String, Boolean> inside PieceConfig
            jsonParser.setElementType(PieceConfig.class, "initialState", Boolean.class);
            // Hint for the HashMap<String, Integer> inside MoveComponentConfig
            jsonParser.setElementType(PieceConfig.MoveComponentConfig.class, "params", Integer.class);

        } catch (Exception e) {
            // Catch potential errors during reflection access for setElementType
            Gdx.app.error(TAG, "Error setting element types for JSON parser. Parsing might fail.", e);
        }


        try {
            FileHandle dirHandle = Gdx.files.internal(CONFIG_DIR);
            if (!dirHandle.exists() || !dirHandle.isDirectory()) {
                Gdx.app.error(TAG, "Piece configuration directory not found or not a directory: " + dirHandle.path());
                return;
            }

            FileHandle[] files = dirHandle.list(".json");
            Gdx.app.log(TAG, "Found " + files.length + " potential JSON config files in " + dirHandle.path());

            for (FileHandle file : files) {
                try {
                    Gdx.app.debug(TAG, "Attempting to load config: " + file.name());
                    // Now parse with the hints potentially set
                    PieceConfig config = jsonParser.fromJson(PieceConfig.class, file);

                    if (config == null || config.typeName == null || config.typeName.trim().isEmpty()) {
                        Gdx.app.error(TAG, "Skipping config file " + file.name() + ": Missing or empty typeName.");
                        continue;
                    }

                    String typeNameUpper = config.typeName.toUpperCase();
                    if (pieceConfigs.containsKey(typeNameUpper)) {
                        Gdx.app.error(TAG, "Warning: Duplicate piece typeName '" + typeNameUpper + "' found in " + file.name() + ". Skipping this file.");
                    } else {
                        // Basic validation
                        if (config.assetBaseName == null || config.assetBaseName.trim().isEmpty()) {
                            Gdx.app.error(TAG, "Warning: Piece config '" + typeNameUpper + "' is missing 'assetBaseName'.");
                        }
                        if (config.moveComponents == null) {
                            config.moveComponents = new ArrayList<>();
                            Gdx.app.debug(TAG,"Piece config '" + typeNameUpper + "' has null moveComponents, initialized empty list.");
                        }
                        // Ensure maps are not null after parsing (should be handled by PieceConfig constructor)
                        if (config.initialState == null) {
                            config.initialState = new HashMap<>();
                            Gdx.app.debug(TAG,"Piece config '" + typeNameUpper + "' has null initialState, initialized empty map.");
                        }


                        pieceConfigs.put(typeNameUpper, config);
                        Gdx.app.log(TAG, "Loaded piece configuration: '" + typeNameUpper + "' from " + file.name());
                    }
                } catch (SerializationException e) {
                    Gdx.app.error(TAG, "Error parsing piece config file: " + file.name(), e);
                    // Log details that might help debug JSON structure vs PieceConfig class
                    Gdx.app.error(TAG, "Check JSON structure matches PieceConfig.java, including specific HashMap types for 'initialState' (Boolean values) and 'params' (Integer values).");
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Unexpected error loading piece config file: " + file.name(), e);
                }
            }
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error accessing piece configuration directory: " + CONFIG_DIR, e);
        }
        if (pieceConfigs.isEmpty()) {
            Gdx.app.error(TAG, "No piece configurations were successfully loaded! Check JSON files and directory.");
        }
    }

    /** Creates a new, configured Piece instance based on the loaded configurations. (Unchanged) */
    public static Piece createPiece(String typeName, Team team, BoardPosition position) {
        if (typeName == null) {
            throw new IllegalArgumentException("Piece type name cannot be null.");
        }
        String upperTypeName = typeName.toUpperCase();
        PieceConfig config = pieceConfigs.get(upperTypeName);

        if (config == null) {
            Gdx.app.error(TAG, "Unknown piece type requested: '" + typeName + "'. Available types: " + getAvailablePieceTypes());
            throw new IllegalArgumentException("Unknown piece type requested: '" + typeName + "'");
        }

        try {
            ConfigurablePiece newPiece = new ConfigurablePiece();
            newPiece.configure(config, team, position, moveComponentFactory);

            if (!upperTypeName.equals(newPiece.getTypeName())) {
                Gdx.app.error(TAG, "Warning - Mismatch between requested typeName '" + upperTypeName + "' and configured piece.getTypeName() '" + newPiece.getTypeName() + "'. Check config file.");
            }
            if (newPiece.getTeam() != team || !Objects.equals(newPiece.getPosition(), position)) {
                Gdx.app.error(TAG, "Warning - Mismatch in team/position after configuration for type '" + upperTypeName + "'.");
            }

            return newPiece;
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error creating or configuring piece of type '" + upperTypeName + "'. Check configuration and components.", e);
            throw new RuntimeException("Failed to create piece of type '" + upperTypeName + "'. Cause: " + e.getMessage(), e);
        }
    }

    /** Gets the set of available piece type names (uppercase) loaded from configurations. (Unchanged) */
    public static Set<String> getAvailablePieceTypes() {
        return Collections.unmodifiableSet(pieceConfigs.keySet());
    }

    /** Gets a collection of prototype instances for all available piece types. (Unchanged) */
    public static Collection<Piece> getPiecePrototypes() {
        if (piecePrototypes.isEmpty() && !pieceConfigs.isEmpty()) {
            Gdx.app.log(TAG, "Creating piece prototypes...");
            for (Map.Entry<String, PieceConfig> entry : pieceConfigs.entrySet()) {
                try {
                    BoardPosition dummyPos = new BoardPosition(-1, -1);
                    Piece prototype = createPiece(entry.getKey(), Team.WHITE, dummyPos);
                    piecePrototypes.put(entry.getKey(), prototype);
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Failed to create prototype for piece type: " + entry.getKey(), e);
                }
            }
            Gdx.app.log(TAG, "Created " + piecePrototypes.size() + " prototypes.");
        }
        return Collections.unmodifiableCollection(piecePrototypes.values());
    }

    /** Gets a specific piece prototype instance by type name (case-insensitive). (Unchanged) */
    public static Piece getPrototype(String typeName) {
        if (typeName == null) return null;
        String upperTypeName = typeName.toUpperCase();
        if (piecePrototypes.containsKey(upperTypeName)) {
            return piecePrototypes.get(upperTypeName);
        }
        PieceConfig config = pieceConfigs.get(upperTypeName);
        if (config != null) {
            Gdx.app.debug(TAG, "Creating on-demand prototype for: " + upperTypeName);
            try {
                BoardPosition dummyPos = new BoardPosition(-1, -1);
                Piece prototype = createPiece(upperTypeName, Team.WHITE, dummyPos);
                piecePrototypes.put(upperTypeName, prototype);
                return prototype;
            } catch (Exception e) {
                Gdx.app.error(TAG, "Failed to create on-demand prototype for piece type: " + upperTypeName, e);
                return null;
            }
        }
        Gdx.app.error(TAG,"No configuration found for prototype request: " + typeName);
        return null;
    }

    /** Gets the loaded configuration data for a specific piece type. (Unchanged) */
    public static PieceConfig getConfig(String typeName) {
        if (typeName == null) return null;
        return pieceConfigs.get(typeName.toUpperCase());
    }
}
