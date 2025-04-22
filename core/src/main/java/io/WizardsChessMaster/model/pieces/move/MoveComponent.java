package io.WizardsChessMaster.model.pieces.move;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.pieces.Piece;

import java.util.Map;
import java.util.Set;

/**
 * Interface for a component that calculates a specific type of move
 * or generates potential target squares for a piece.
 */
public interface MoveComponent {

    /**
     * Initializes the component with parameters from the configuration.
     * This is called once by the factory after the component is instantiated.
     *
     * @param piece The piece instance this component belongs to.
     * @param params A map of INTEGER parameters defined in the piece configuration's
     * component entry (e.g., {"dx": 1, "dy": 0}). Can be null or empty.
     * @throws IllegalArgumentException if required parameters are missing or invalid.
     */
    void initialize(Piece piece, Map<String, Integer> params);

    /**
     * Calculates valid moves based on this component's logic for the given piece.
     * (Description unchanged)
     * @param piece The piece whose moves are being calculated.
     * @param gameModel The current state of the game.
     * @return A Set of valid target BoardPositions according to this component's rules.
     */
    Set<BoardPosition> getValidMoves(Piece piece, GameModel gameModel);

    /**
     * Calculates squares attacked by the piece based on this component's logic.
     * (Description unchanged)
     * @param piece The piece whose attacked squares are being calculated.
     * @param gameModel The current state of the game.
     * @return A Set of attacked BoardPositions according to this component's rules.
     */
    Set<BoardPosition> getAttackedSquares(Piece piece, GameModel gameModel);

    default boolean moveLeavesKingInCheck(Piece piece, GameModel model, BoardPosition target) {
        if ("EN_PASSANT".equals(getIdentifier()) && piece.getPosition() != null && model != null && target != null) {
            int dx = target.getX() - piece.getPosition().getX();
            BoardPosition victimPos = piece.getPosition().add(dx, 0);
            return moveLeavesKingInCheckWithVictim(piece, model, target, victimPos);
        }
        GameModel simModel = model.copy();
        Piece simPiece = simModel.getPieceAt(piece.getPosition());
        if (simPiece == null) {
            System.err.println("Error in moveLeavesKingInCheck: Piece not found at original position in simulation for " + piece.getTypeName());
            return true;
        }
        simModel.movePiece(simPiece, target);
        return simModel.isKingInCheck(piece.getTeam());
    }

    default boolean moveLeavesKingInCheckWithVictim(Piece piece, GameModel model, BoardPosition target, BoardPosition victimPos) {
        GameModel simModel = model.copy();
        Piece simPiece = simModel.getPieceAt(piece.getPosition());
        Piece simVictim = simModel.getPieceAt(victimPos);
        if (simPiece == null) {
            System.err.println("EP Check Error: Cannot find simulated moving piece at " + piece.getPosition());
            return true;
        }
        if (simVictim == null) {
            System.err.println("EP Check Error: Cannot find simulated victim piece at " + victimPos);
        } else {
            simModel.removePiece(simVictim);
        }
        simModel.movePiece(simPiece, target);
        return simModel.isKingInCheck(piece.getTeam());
    }

    /**
     * Gets a unique string identifier for this component type (e.g., "SLIDING", "PAWN_FORWARD").
     * (Description unchanged)
     * @return The component type identifier.
     */
    default String getIdentifier() {
        return this.getClass().getSimpleName().replace("MoveComponent", "").toUpperCase();
    }

}
