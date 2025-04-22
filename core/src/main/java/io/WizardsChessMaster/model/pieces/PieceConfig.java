package io.WizardsChessMaster.model.pieces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the configuration data for a piece type, loaded from an external source (e.g., JSON).
 * Uses specific HashMap types (String, Boolean/Integer) for fields to aid deserialization.
 */
public class PieceConfig {

    public static class MoveComponentConfig {
        public String type;
        public HashMap<String, Integer> params;

        public MoveComponentConfig() {
            this.params = new HashMap<>();
        }

        // Constructor updated
        public MoveComponentConfig(String type, Map<String, Integer> params) {
            this.type = type;
            this.params = (params != null) ? new HashMap<>(params) : new HashMap<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MoveComponentConfig that = (MoveComponentConfig) o;
            return Objects.equals(type, that.type) && Objects.equals(params, that.params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, params);
        }

        @Override
        public String toString() {
            return "MoveComponentConfig{" +
                    "type='" + type + '\'' +
                    ", params=" + params +
                    '}';
        }
    }

    public String typeName;
    public int pointCost;
    public String displayName;
    public String description;
    public String assetBaseName;
    public HashMap<String, Boolean> initialState;
    public List<MoveComponentConfig> moveComponents;

    public PieceConfig() {
        this.initialState = new HashMap<>();
        this.moveComponents = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PieceConfig that = (PieceConfig) o;
        return pointCost == that.pointCost &&
                Objects.equals(typeName, that.typeName) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(description, that.description) &&
                Objects.equals(assetBaseName, that.assetBaseName) &&
                Objects.equals(initialState, that.initialState) &&
                Objects.equals(moveComponents, that.moveComponents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, pointCost, displayName, description, assetBaseName, initialState, moveComponents);
    }

    @Override
    public String toString() {
        return "PieceConfig{" +
                "typeName='" + typeName + '\'' +
                ", pointCost=" + pointCost +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", assetBaseName='" + assetBaseName + '\'' +
                ", initialState=" + initialState +
                ", moveComponents=" + moveComponents +
                '}';
    }
}
