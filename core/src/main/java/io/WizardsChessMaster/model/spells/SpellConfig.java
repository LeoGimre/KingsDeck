package io.WizardsChessMaster.model.spells;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the configuration data for a spell type, loaded from an external source (e.g., JSON).
 * This defines the properties and potentially the effects of a spell.
 */
public class SpellConfig {

    // Inner class for effect components
    public static class SpellEffectComponentConfig {
        public String type;
        public HashMap<String, String> params;

        public SpellEffectComponentConfig() {
            this.params = new HashMap<>();
        }

        public SpellEffectComponentConfig(String type, Map<String, String> params) {
            this.type = type;
            this.params = (params != null) ? new HashMap<>(params) : new HashMap<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpellEffectComponentConfig that = (SpellEffectComponentConfig) o;
            return Objects.equals(type, that.type) && Objects.equals(params, that.params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, params);
        }

        @Override
        public String toString() {
            return "SpellEffectComponentConfig{" +
                    "type='" + type + '\'' +
                    ", params=" + params +
                    '}';
        }
    }

    // Fields matching the desired JSON structure
    public String typeName;
    public int pointCost;
    public String displayName;
    public String description;
    public String iconBaseName;
    public boolean requiresTarget;
    public boolean endsTurn;
    public List<SpellEffectComponentConfig> effectComponents;

    // Default constructor for JSON parsing
    public SpellConfig() {
        this.effectComponents = new ArrayList<>();
    }

    public String getIconPath() {
        if (iconBaseName == null || iconBaseName.trim().isEmpty()) {
            return "spells/default_spell_icon.png";
        }
        String base = iconBaseName.endsWith(".png") ? iconBaseName.substring(0, iconBaseName.length() - 4) : iconBaseName;
        return "spells/" + base + ".png";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpellConfig that = (SpellConfig) o;
        return pointCost == that.pointCost &&
                requiresTarget == that.requiresTarget &&
                endsTurn == that.endsTurn &&
                Objects.equals(typeName, that.typeName) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(description, that.description) &&
                Objects.equals(iconBaseName, that.iconBaseName) &&
                Objects.equals(effectComponents, that.effectComponents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, pointCost, displayName, description, iconBaseName, requiresTarget, endsTurn, effectComponents);
    }

    @Override
    public String toString() {
        return "SpellConfig{" +
                "typeName='" + typeName + '\'' +
                ", pointCost=" + pointCost +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", iconBaseName='" + iconBaseName + '\'' +
                ", requiresTarget=" + requiresTarget +
                ", endsTurn=" + endsTurn +
                ", effectComponents=" + effectComponents +
                '}';
    }
}