package io.WizardsChessMaster.model.spells;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;

import java.util.Set;

/**
 * Interface representing a castable spell in the game.
 * Implementations will typically be driven by SpellConfig data.
 */
public interface Spell {

    /**
     * Gets the unique, uppercase identifier for this spell type (e.g., "FIREBALL").
     * Should match the typeName in the configuration.
     * @return The type identifier string.
     */
    String getTypeName();

    /**
     * Gets the point cost of including this spell type in a deck.
     * Read from configuration.
     * @return Point cost as an integer.
     */
    int getPointCost();

    /**
     * Gets a user-friendly name for the spell (e.g., "Fireball").
     * Read from configuration.
     * @return The display name string.
     */
    String getDisplayName();

    /**
     * Gets a description of what the spell does.
     * Read from configuration.
     * @return Description string for UI tooltips or info panels.
     */
    String getDescription();

    /**
     * Determines if this spell requires a target square on the board.
     * Read from configuration.
     * @return true if a target square is needed, false otherwise.
     */
    boolean requiresTarget();

    /**
     * Determines if casting this spell ends the player's turn immediately.
     * Read from configuration.
     * @return true if the turn ends upon casting, false otherwise.
     */
    boolean endsTurn();

    /**
     * Calculates the set of valid target positions for this spell, given the caster
     * and the current game state. If requiresTarget() is false, this should return
     * an empty set.
     * Implementations should consider game rules (e.g., cannot target own King).
     *
     * @param casterPlayerId The ID of the player attempting to cast the spell.
     * @param gameModel The current state of the game.
     * @return A Set of valid target BoardPositions. Returns an empty set if no targets are valid or required.
     */
    Set<BoardPosition> getValidTargets(String casterPlayerId, GameModel gameModel);

    /**
     * Executes the spell's effect. This method should:
     * 1. Verify if the caster can legally cast (e.g., not in check). (This check is often done *before* calling cast).
     * 2. Check if the target (if required) is valid according to getValidTargets.
     * 3. Apply the spell's effect to the GameModel (e.g., remove piece, modify state). This logic might be delegated to effect components.
     * 4. Return true if the spell was successfully cast and effects applied, false otherwise
     * (e.g., invalid target, casting condition not met).
     *
     * Note: Consuming the spell from the player's available spells and ending the turn
     * are typically handled by the GameplayPresenter after this method returns true.
     *
     * @param casterPlayerId The ID of the player casting the spell.
     * @param target The selected target position (can be null if requiresTarget() is false).
     * @param gameModel The current state of the game, which will be modified by the spell.
     * @return true if the spell was successfully cast, false otherwise.
     */
    boolean applyEffect(String casterPlayerId, BoardPosition target, GameModel gameModel);

    /**
     * Gets the path to the asset (icon) used to represent this spell in the UI.
     * Constructed based on configuration.
     * @return Asset path string.
     */
    String getIconPath();

    /**
     * Creates a deep copy of this spell instance.
     * Useful if spell instances have state or need to be added to player inventories.
     * @return A new Spell instance with the same properties.
     */
    Spell copy();

}