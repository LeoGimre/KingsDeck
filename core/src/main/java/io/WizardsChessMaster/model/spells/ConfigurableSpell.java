package io.WizardsChessMaster.model.spells;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A generic Spell implementation driven by external configuration (SpellConfig).
 * Delegates effects to configured SpellEffectComponents.
 */
public class ConfigurableSpell implements Spell {

    private static final String TAG = "ConfigurableSpell";

    protected SpellConfig config;
    protected List<SpellEffectComponent> effectComponents;

    // Reference to the factory
    private static SpellEffectComponentFactory effectComponentFactory = new SpellEffectComponentFactory();

    // Public no-arg constructor needed for factory/reflection
    public ConfigurableSpell() {
        this.effectComponents = new ArrayList<>();
    }

    /**
     * Configures the spell based on loaded data. Called by SpellFactory.
     * <<< FIXED: Added SpellEffectComponentFactory parameter back >>>
     */
    public void configure(SpellConfig config, SpellEffectComponentFactory compFactory) {
        if (config == null || compFactory == null) {
            throw new IllegalArgumentException("Cannot configure spell with null config or component factory.");
        }
        this.config = config;

        SpellEffectComponentFactory factoryToUse = (compFactory != null) ? compFactory : ConfigurableSpell.effectComponentFactory;


        // Create and add EffectComponents based on configuration
        this.effectComponents.clear();
        if (config.effectComponents != null) {
            for (SpellConfig.SpellEffectComponentConfig compConfig : config.effectComponents) {
                if (compConfig == null || compConfig.type == null || compConfig.type.trim().isEmpty()) {
                    Gdx.app.error(TAG, "Skipping invalid effect component config (null or missing type) for spell " + config.typeName);
                    continue;
                }
                try {
                    // Use the factory passed in
                    SpellEffectComponent component = factoryToUse.createComponent(compConfig.type, this, compConfig.params);
                    this.effectComponents.add(component);
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Failed to create or initialize effect component type '" + compConfig.type + "' for spell '" + config.typeName + "': " + e.getMessage(), e);
                }
            }
        }
        Gdx.app.debug(TAG, "Configured spell: " + getTypeName() + " with components: " + effectComponents.stream().map(SpellEffectComponent::getIdentifier).collect(Collectors.toList()));
    }


    // --- Spell Interface Implementation ---

    @Override public String getTypeName() { return config != null ? config.typeName : "UNKNOWN_SPELL"; }
    @Override public int getPointCost() { return config != null ? config.pointCost : 99; }
    @Override public String getDisplayName() { return config != null ? config.displayName : "Unknown Spell"; }
    @Override public String getDescription() { return config != null ? config.description : "No description."; }
    @Override public String getIconPath() { return config != null ? config.getIconPath() : "spells/default_spell_icon.png"; }
    @Override public boolean requiresTarget() { return config != null && config.requiresTarget; }
    @Override public boolean endsTurn() { return config != null && config.endsTurn; }

    @Override
    public Set<BoardPosition> getValidTargets(String casterPlayerId, GameModel gameModel) {
        if (!requiresTarget() || effectComponents == null || effectComponents.isEmpty()) {
            // If spell requires target but has no components defining targets
            if (requiresTarget()) {
                Gdx.app.error(TAG, "Spell " + getTypeName() + " requires target but has no effect components to determine valid targets.");
            }
            return Collections.emptySet();
        }

        // Union of all targets from all components.
        Set<BoardPosition> allValidTargets = new HashSet<>();
        for (SpellEffectComponent component : effectComponents) {
            try {
                allValidTargets.addAll(component.getValidTargetsForEffect(casterPlayerId, gameModel));
            } catch (Exception e) {
                Gdx.app.error(TAG, "Error getting targets from component " + component.getIdentifier() + " for spell " + getTypeName(), e);
            }
        }
        return allValidTargets;
    }

    @Override
    public boolean applyEffect(String casterPlayerId, BoardPosition target, GameModel gameModel) {
        if (casterPlayerId == null || gameModel == null || config == null) {
            Gdx.app.error(TAG, getTypeName() + " applyEffect failed: Null casterId, gameModel, or config.");
            return false;
        }
        if (requiresTarget() && target == null) {
            Gdx.app.error(TAG, getTypeName() + " applyEffect failed: Requires target, but target is null.");
            return false;
        }

        // Apply Effects from Components
        Gdx.app.log(TAG, "Applying effects for " + getDisplayName() + (target != null ? " on " + target : "") + " by " + casterPlayerId);
        boolean oneEffectSucceeded = false;
        boolean allEffectsSucceeded = true;

        if (effectComponents != null && !effectComponents.isEmpty()) {
            for (SpellEffectComponent component : effectComponents) {
                try {
                    if (component.applyEffect(casterPlayerId, target, gameModel)) {
                        oneEffectSucceeded = true;
                    } else {
                        Gdx.app.error(TAG, "Effect component " + component.getIdentifier() + " failed to apply for spell " + getTypeName());
                        allEffectsSucceeded = false;
                    }
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Exception applying effect component " + component.getIdentifier() + " for spell " + getTypeName(), e);
                    allEffectsSucceeded = false;
                }
            }
            return allEffectsSucceeded;

        } else {
            Gdx.app.log(TAG, "Spell " + getTypeName() + " has no effect components defined. Applying effect considered successful.");
            return true;
        }
    }

    @Override
    public Spell copy() {
        if (this.config == null) {
            Gdx.app.error(TAG, "Cannot copy spell - configuration is null!");
            return new ConfigurableSpell();
        }
        ConfigurableSpell newSpell = new ConfigurableSpell();
        // Configure using the same config and static factory
        newSpell.configure(this.config, ConfigurableSpell.effectComponentFactory);
        return newSpell;
    }

    // toString, equals, hashCode
    @Override
    public String toString() {
        return "Spell:" + getTypeName() + "(" + getPointCost() + "pts)";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurableSpell that = (ConfigurableSpell) o;
        return Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(config);
    }
}