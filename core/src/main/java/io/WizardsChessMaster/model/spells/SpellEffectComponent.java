package io.WizardsChessMaster.model.spells;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;

import java.util.Map;
import java.util.Set;

/**
 * Interface for components defining specific parts of a spell's effect.
 * A spell might have multiple components (e.g., damage + apply status).
 */
public interface SpellEffectComponent {

    /**
     * Initializes the component with parameters from the Spell configuration.
     * @param spell The spell instance this component belongs to.
     * @param params A map of parameters defined in the spell configuration's
     * component entry. Assumed to be String keys and String values.
     * @throws IllegalArgumentException if required parameters are missing or invalid.
     */
    void initialize(Spell spell, Map<String, String> params);

    /**
     * Applies this component's specific effect to the game state.
     * This is called by the Spell's cast method.
     * @param casterPlayerId The ID of the player casting the spell.
     * @param target The selected target position (can be null if the component or spell doesn't require one).
     * @param gameModel The current state of the game, which will be modified.
     * @return true if the effect was successfully applied, false otherwise.
     */
    boolean applyEffect(String casterPlayerId, BoardPosition target, GameModel gameModel);

    /**
     * Calculates valid target squares *specifically for this effect component*.
     * This can differ from the overall spell's valid targets if the spell has multiple effects
     * targeting different things.
     * @param casterPlayerId The ID of the player casting the spell.
     * @param gameModel The current game state.
     * @return A Set of valid BoardPositions for this component's effect. Empty if no target is needed or valid.
     */
    Set<BoardPosition> getValidTargetsForEffect(String casterPlayerId, GameModel gameModel);

    /**
     * Gets a unique string identifier for this component type (e.g., "DAMAGE", "TELEPORT").
     * Used by the factory and for debugging. Conventionally uppercase.
     *
     * @return The component type identifier.
     */
    String getIdentifier();
}