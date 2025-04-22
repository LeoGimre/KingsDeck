package io.WizardsChessMaster.model;

import com.badlogic.gdx.Gdx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.WizardsChessMaster.model.pieces.PieceConfig;
import io.WizardsChessMaster.model.pieces.PieceFactory;
import io.WizardsChessMaster.model.pieces.PieceType;
import io.WizardsChessMaster.model.spells.SpellConfig;
import io.WizardsChessMaster.model.spells.SpellFactory;

/**
 * Model representing a customizable chess deck configuration.
 * Pieces are placed on a conceptual 16-slot grid (Map<String index, String typeName>).
 * Spells are placed on a conceptual 5-slot grid (Map<String index, String typeName>).
 */
public class DeckModel {
    private static final String TAG = "DeckModel";

    public static final int PIECE_GRID_SIZE = 16;
    public static final int SPELL_GRID_SIZE = 5;

    private String name;
    private int pointLimit;
    private Map<String, String> pieceConfiguration;
    private Map<String, String> spellConfiguration;

    private transient int currentPoints = -1;

    public DeckModel() {
        this.pieceConfiguration = new HashMap<>();
        for (int i = 0; i < PIECE_GRID_SIZE; i++) {
            pieceConfiguration.put(String.valueOf(i), null);
        }

        this.spellConfiguration = new HashMap<>();
        for (int i = 0; i < SPELL_GRID_SIZE; i++) {
            spellConfiguration.put(String.valueOf(i), null);
        }
    }

    public DeckModel(String name, int pointLimit) {
        this();
        this.name = name;
        this.pointLimit = pointLimit;
    }

    // --- Getters and Setters ---

    public String getName() { return name; }
    public void setName(String name) {
        if (!Objects.equals(this.name, name)) {
            this.name = name;
        }
    }

    public int getPointLimit() { return pointLimit; }
    public void setPointLimit(int pointLimit) {
        if (this.pointLimit != pointLimit) {
            this.pointLimit = pointLimit;
        }
    }

    public Map<String, String> getPieceConfiguration() { return pieceConfiguration; }
    public void setPieceConfiguration(Map<String, String> pieceConfiguration) {
        this.pieceConfiguration = pieceConfiguration != null ? new HashMap<>(pieceConfiguration) : new HashMap<>();
        for (int i = 0; i < PIECE_GRID_SIZE; i++) {
            this.pieceConfiguration.putIfAbsent(String.valueOf(i), null);
        }
        invalidatePointCache();
    }

    public Map<String, String> getSpellConfiguration() {
        return spellConfiguration;
    }

    public void setSpellConfiguration(Map<String, String> spellConfiguration) {
        this.spellConfiguration = spellConfiguration != null ? new HashMap<>(spellConfiguration) : new HashMap<>();
        for (int i = 0; i < SPELL_GRID_SIZE; i++) {
            this.spellConfiguration.putIfAbsent(String.valueOf(i), null);
        }
        invalidatePointCache();
    }

    private void invalidatePointCache() {
        this.currentPoints = -1;
    }

    public int getCurrentPoints() {
        if (currentPoints == -1) {
            int totalPoints = 0;
            // Pieces
            if (pieceConfiguration != null) {
                for (String pieceName : pieceConfiguration.values()) {
                    PieceConfig config = PieceFactory.getConfig(pieceName);
                    if (config != null) { totalPoints += config.pointCost; }
                    else if (pieceName != null) { Gdx.app.error(TAG, "Could not find piece config for '" + pieceName + "'"); }
                }
            }
            // Spells
            if (spellConfiguration != null) {
                for (String spellName : spellConfiguration.values()) {
                    SpellConfig spellConfig = SpellFactory.getConfig(spellName);
                    if (spellConfig != null) { totalPoints += spellConfig.pointCost; }
                    else if (spellName != null) { Gdx.app.error(TAG, "Could not find spell config for '" + spellName + "'"); }
                }
            }
            currentPoints = totalPoints;
        }
        return currentPoints;
    }

    // --- Helper Methods ---

    public String getPieceTypeNameAt(int index) {
        if (index < 0 || index >= PIECE_GRID_SIZE || pieceConfiguration == null) return null;
        String pieceName = pieceConfiguration.get(String.valueOf(index));
        return (pieceName != null && !pieceName.trim().isEmpty()) ? pieceName.toUpperCase() : null;
    }

    public boolean setPieceAt(int index, String pieceTypeName) {
        if (index < 0 || index >= PIECE_GRID_SIZE || pieceConfiguration == null) return false;
        String currentPieceName = getPieceTypeNameAt(index);
        String newPieceName = (pieceTypeName == null || pieceTypeName.trim().isEmpty()) ? null : pieceTypeName.toUpperCase();
        if (Objects.equals(currentPieceName, newPieceName)) { return false; }
        pieceConfiguration.put(String.valueOf(index), newPieceName);
        invalidatePointCache();
        return true;
    }

    public String getSpellTypeNameAt(int index) {
        if (index < 0 || index >= SPELL_GRID_SIZE || spellConfiguration == null) return null;
        String spellName = spellConfiguration.get(String.valueOf(index));
        return (spellName != null && !spellName.trim().isEmpty()) ? spellName.toUpperCase() : null;
    }

    public boolean setSpellAt(int index, String spellTypeName) {
        if (index < 0 || index >= SPELL_GRID_SIZE || spellConfiguration == null) return false;
        String currentSpellName = getSpellTypeNameAt(index);
        String newSpellName = (spellTypeName == null || spellTypeName.trim().isEmpty()) ? null : spellTypeName.toUpperCase();
        if (Objects.equals(currentSpellName, newSpellName)) { return false; }
        spellConfiguration.put(String.valueOf(index), newSpellName);
        invalidatePointCache();
        Gdx.app.debug(TAG, "Set spell at index " + index + " to: " + newSpellName);
        return true;
    }

    public int findKingIndex() {
        if (pieceConfiguration == null) return -1;
        String kingTypeName = PieceType.KING.name();
        for (int i = 0; i < PIECE_GRID_SIZE; i++) {
            if (kingTypeName.equals(getPieceTypeNameAt(i))) { return i; }
        }
        return -1;
    }

    /** Checks if adding a piece defined by PieceConfig would exceed the point limit. */
    public boolean canAddPiece(PieceConfig pieceConfig) {
        if (pieceConfig == null) return true;
        return getCurrentPoints() + pieceConfig.pointCost <= pointLimit;
    }

    /** Checks if swapping the piece at index with a new piece defined by PieceConfig is valid point-wise. */
    public boolean canSwapPiece(int index, PieceConfig newPieceConfig) {
        String oldPieceName = getPieceTypeNameAt(index);
        PieceConfig oldConfig = PieceFactory.getConfig(oldPieceName);
        int oldPoints = (oldConfig != null) ? oldConfig.pointCost : 0;
        int newPoints = (newPieceConfig != null) ? newPieceConfig.pointCost : 0;
        int currentTotal = getCurrentPoints();
        return (currentTotal - oldPoints + newPoints) <= pointLimit;
    }

    /** Checks if swapping the spell at index with a new spell is valid point-wise. */
    public boolean canSwapSpell(int index, SpellConfig newSpellConfig) {
        if (index < 0 || index >= SPELL_GRID_SIZE) return false;

        String oldSpellName = getSpellTypeNameAt(index);
        SpellConfig oldConfig = SpellFactory.getConfig(oldSpellName);

        int oldPoints = (oldConfig != null) ? oldConfig.pointCost : 0;
        int newPoints = (newSpellConfig != null) ? newSpellConfig.pointCost : 0;
        int currentTotal = getCurrentPoints();

        return (currentTotal - oldPoints + newPoints) <= pointLimit;
    }
    @Override
    public String toString() {
        String piecesStr = (pieceConfiguration != null) ?
                pieceConfiguration.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(", ", "{", "}")) : "{}";
        String spellsStr = (spellConfiguration != null) ?
                spellConfiguration.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(", ", "{", "}")) : "{}";

        return "DeckModel{" +
                "name='" + name + '\'' +
                ", pointLimit=" + pointLimit +
                ", currentPoints=" + getCurrentPoints() +
                ", pieces=" + piecesStr +
                ", spells=" + spellsStr +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeckModel deckModel = (DeckModel) o;
        return pointLimit == deckModel.pointLimit &&
                Objects.equals(name, deckModel.name) &&
                Objects.equals(pieceConfiguration, deckModel.pieceConfiguration) &&
                Objects.equals(spellConfiguration, deckModel.spellConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pointLimit, pieceConfiguration, spellConfiguration);
    }
}