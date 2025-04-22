package io.WizardsChessMaster.model.pieces.move;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.Piece;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Component for pieces that slide along lines (horizontal, vertical, diagonal)
 * until blocked or off-board. E.g., Rooks, Bishops, Queens.
 * Reads 'dx' and 'dy' parameters during initialization.
 */
public class SlidingMoveComponent implements MoveComponent {

    private int dx = 0;
    private int dy = 0;
    private boolean initialized = false;

    public SlidingMoveComponent() {}

    /**
     * Initializes the component with direction parameters from the configuration.
     * Expects 'dx' and 'dy' integer parameters in the params map.
     *
     * @param piece The piece instance this component belongs to (unused).
     * @param params A map containing 'dx' and 'dy' integer parameters.
     * @throws IllegalArgumentException if 'dx' or 'dy' parameters are missing, not integers, or both are zero.
     */
    @Override
    public void initialize(Piece piece, Map<String, Integer> params) {
        if (params == null || !params.containsKey("dx") || !params.containsKey("dy")) {
            throw new IllegalArgumentException("SlidingMoveComponent requires 'dx' and 'dy' integer parameters in configuration.");
        }
        Integer dxInt = params.get("dx");
        Integer dyInt = params.get("dy");

        if (dxInt == null || dyInt == null) {
            throw new IllegalArgumentException("SlidingMoveComponent 'dx' and 'dy' parameters cannot be null.");
        }

        this.dx = dxInt;
        this.dy = dyInt;

        if (this.dx == 0 && this.dy == 0) {
            throw new IllegalArgumentException("SlidingMoveComponent cannot have dx and dy both zero.");
        }
        this.initialized = true;
    }

    @Override
    public Set<BoardPosition> getValidMoves(Piece piece, GameModel gameModel) {
        if (!initialized) throw new IllegalStateException("SlidingMoveComponent not initialized.");
        Set<BoardPosition> moves = new HashSet<>();
        if (piece.getPosition() == null || piece.getTeam() == null || gameModel == null) {
            return moves;
        }
        BoardPosition startPos = piece.getPosition();
        Team team = piece.getTeam();

        for (int i = 1; ; i++) {
            BoardPosition targetPos = startPos.add(dx * i, dy * i);
            if (!gameModel.isWithinBounds(targetPos)) {
                break;
            }

            Piece targetPiece = gameModel.getPieceAt(targetPos);
            if (targetPiece == null) {
                if (!moveLeavesKingInCheck(piece, gameModel, targetPos)) {
                    moves.add(targetPos);
                }
            } else {
                if (targetPiece.getTeam() != team) {
                    if (!moveLeavesKingInCheck(piece, gameModel, targetPos)) {
                        moves.add(targetPos);
                    }
                }
                break;
            }
        }
        return moves;
    }

    @Override
    public Set<BoardPosition> getAttackedSquares(Piece piece, GameModel gameModel) {
        if (!initialized) throw new IllegalStateException("SlidingMoveComponent not initialized.");
        Set<BoardPosition> attacked = new HashSet<>();
        if (piece.getPosition() == null || gameModel == null) {
            return attacked;
        }
        BoardPosition startPos = piece.getPosition();

        for (int i = 1; ; i++) {
            BoardPosition targetPos = startPos.add(dx * i, dy * i);
            if (!gameModel.isWithinBounds(targetPos)) {
                break;
            }
            attacked.add(targetPos);
            if (gameModel.getPieceAt(targetPos) != null) {
                break;
            }
        }
        return attacked;
    }

    @Override
    public String getIdentifier() {
        return "SLIDING";
    }
}
