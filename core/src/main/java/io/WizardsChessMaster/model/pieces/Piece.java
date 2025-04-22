package io.WizardsChessMaster.model.pieces;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;

import java.util.Set;

/**
 * Interface representing a chess piece.
 * Each implementation defines its specific behavior and properties.
 * Now includes methods for managing piece-specific state variables for configuration.
 */
public interface Piece {

    /**
     * Gets the unique, uppercase identifier for this piece type (e.g., "PAWN", "ROOK").
     * Used by factories and for registration. MUST be consistent and unique.
     * Should match the typeName in the configuration.
     * @return The type identifier string.
     */
    String getTypeName();

    /**
     * Gets the team this piece belongs to.
     * @return The Team (WHITE or BLACK).
     */
    Team getTeam();

    /**
     * Gets the current position of the piece on the board.
     * @return The BoardPosition, or null if off-board.
     */
    BoardPosition getPosition();

    /**
     * Sets the position of the piece. Implementations should handle
     * internal state changes related to movement (e.g., 'hasMoved' for Pawn/King/Rook).
     * This is primarily called by the GameModel during move execution or setup.
     * Use onMove for game-triggered moves.
     * @param position The new BoardPosition.
     */
    void setPosition(BoardPosition position);

    /**
     * Calculates all valid moves for this piece from its current position,
     * considering the board state provided by the GameModel.
     * This includes standard moves and captures. Special moves like castling
     * or en passant should be handled within the specific piece implementation
     * (likely via configured MoveComponents).
     * Crucially, this method *must not* return moves that leave the piece's own King in check.
     *
     * @param gameModel The current state of the game, used to check board bounds,
     * occupied squares, and king safety.
     * @return A Set of valid target BoardPositions.
     */
    Set<BoardPosition> getValidMoves(GameModel gameModel);

    /**
     * Calculates all squares this piece attacks, irrespective of whether moving there
     * would put its own king in check. Used for check detection against the opponent.
     * Pawns attack differently than they move. Relies on MoveComponents.
     *
     * @param gameModel The current state of the game.
     * @return A set of attacked BoardPositions.
     */
    Set<BoardPosition> getAttackedSquares(GameModel gameModel);

    /**
     * Gets the path to the asset (texture/image) used to render this specific piece
     * instance (considering its team) in the UI.
     * The path should be relative to the asset root (e.g., "pieces/white_pawn.png").
     * Constructed based on configuration and team.
     * @return Asset path string.
     */
    String getAssetPath();

    /**
     * Gets the point value of the piece type, used for deck building limits and potential AI scoring.
     * Read from configuration.
     * @return The point value.
     */
    int getPointValue();

    /**
     * Gets a user-friendly display name for the piece type (e.g., "Pawn", "Dragon").
     * Read from configuration.
     * @return The display name string.
     */
    String getDisplayName();

    /**
     * Gets a description of the piece type's abilities or movement.
     * Read from configuration.
     * @return The description string.
     */
    String getDescription();


    /**
     * Called when this piece captures another piece.
     * Can be used for special effects or logic.
     * @param capturedPiece The piece that was captured.
     */
    default void onCapture(Piece capturedPiece) {
        // Default implementation does nothing
    }

    /**
     * Called *after* this piece has successfully moved to a new position within the game flow.
     * Primarily used to update internal state (position, hasMoved flags, etc.).
     * Implementations should update state variables according to rules/configuration.
     * @param newPosition The position the piece moved to.
     */
    void onMove(BoardPosition newPosition);

    /**
     * Creates a deep copy of this piece instance, preserving its current state (team, position, state variables, configured components).
     * Necessary for game state simulation or copying game models.
     * @return A new Piece instance with the same state and configured behavior.
     */
    Piece copy();

    // --- State Variable Management ---

    /**
     * Retrieves the value of a state variable associated with this piece instance.
     * Components might use this to check state like "hasMoved".
     *
     * @param key The name of the state variable (e.g., "hasMoved").
     * @return The value of the state variable, or null if it doesn't exist.
     */
    Object getStateVariable(String key);

    /**
     * Sets or updates a state variable for this piece instance.
     * Used by the piece itself (e.g., in onMove) or potentially by external effects.
     *
     * @param key The name of the state variable (e.g., "hasMoved").
     * @param value The new value for the state variable.
     */
    void setStateVariable(String key, Object value);

}
