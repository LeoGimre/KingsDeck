package io.WizardsChessMaster.model.pieces.move;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.Piece;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Component for Pawn's forward movement (1 or 2 steps).
 * Relies on the owning Piece having an "hasMoved" boolean state variable.
 * Checks GameModel for temporary effects like "pawn_storm_active".
 * The initial two-step move is allowed from any square if hasMoved is false.
 */
public class PawnForwardMoveComponent implements MoveComponent {

    // Effect names
    private static final String PAWN_STORM_EFFECT_NAME = "pawn_storm_active";

    public PawnForwardMoveComponent() {}

    /**
     * Initializes the component. This component does not require parameters.
     */
    @Override
    public void initialize(Piece piece, Map<String, Integer> params) {
    }

    @Override
    public Set<BoardPosition> getValidMoves(Piece piece, GameModel gameModel) {
        Set<BoardPosition> moves = new HashSet<>();
        if (piece.getPosition() == null || piece.getTeam() == null || gameModel == null) {
            return moves;
        }

        BoardPosition position = piece.getPosition();
        Team team = piece.getTeam();
        // Find the player ID based on team color to check for effects
        String playerId = null;
        if (team == Team.WHITE && gameModel.getPlayer1Color() != null && gameModel.getPlayer1Color().equalsIgnoreCase("white")) {
            playerId = gameModel.getPlayer1Id();
        } else if (team == Team.BLACK && gameModel.getPlayer1Color() != null && gameModel.getPlayer1Color().equalsIgnoreCase("black")) {
            playerId = gameModel.getPlayer1Id();
        } else if (team == Team.WHITE && gameModel.getPlayer2Color() != null && gameModel.getPlayer2Color().equalsIgnoreCase("white")) {
            playerId = gameModel.getPlayer2Id();
        } else if (team == Team.BLACK && gameModel.getPlayer2Color() != null && gameModel.getPlayer2Color().equalsIgnoreCase("black")) {
            playerId = gameModel.getPlayer2Id();
        }

        int direction = (team == Team.WHITE) ? 1 : -1;

        boolean hasMoved = false;
        Object hasMovedObj = piece.getStateVariable("hasMoved");
        if (hasMovedObj instanceof Boolean) {
            hasMoved = (Boolean) hasMovedObj;
        } else if (hasMovedObj != null) {
        }

        // Check for active effects
        boolean pawnStormActive = false;
        if (playerId != null) {
            pawnStormActive = gameModel.hasTurnEffect(playerId, PAWN_STORM_EFFECT_NAME);
        }

        // Check one step forward
        BoardPosition oneStep = position.add(0, direction);
        if (gameModel.isWithinBounds(oneStep) && gameModel.getPieceAt(oneStep) == null) {
            // One step is always valid if clear and doesn't cause check
            if (!moveLeavesKingInCheck(piece, gameModel, oneStep)) {
                moves.add(oneStep);
            }

            // Check two steps forward
            boolean allowTwoSteps = pawnStormActive || !hasMoved;

            if (allowTwoSteps) {
                BoardPosition twoStep = position.add(0, 2 * direction);
                // Path must be clear for two steps
                if (gameModel.isWithinBounds(twoStep) && gameModel.getPieceAt(twoStep) == null) {
                    if (!moveLeavesKingInCheck(piece, gameModel, twoStep)) {
                        moves.add(twoStep);
                    }
                }
            }
        }
        return moves;
    }

    @Override
    public Set<BoardPosition> getAttackedSquares(Piece piece, GameModel gameModel) {
        // Pawn forward move doesn't attack squares
        return new HashSet<>();
    }

    @Override
    public String getIdentifier() {
        return "PAWN_FORWARD";
    }
}