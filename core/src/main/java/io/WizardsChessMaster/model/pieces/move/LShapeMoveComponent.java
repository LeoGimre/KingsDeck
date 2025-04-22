package io.WizardsChessMaster.model.pieces.move;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.Piece;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Component for Knight's L-shaped movement. Does not require parameters.
 */
public class LShapeMoveComponent implements MoveComponent {

    private static final int[][] OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    public LShapeMoveComponent() {}

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
        BoardPosition startPos = piece.getPosition();
        Team team = piece.getTeam();

        for (int[] offset : OFFSETS) {
            BoardPosition targetPos = startPos.add(offset[0], offset[1]);
            if (gameModel.isWithinBounds(targetPos)) {
                Piece targetPiece = gameModel.getPieceAt(targetPos);
                if (targetPiece == null || targetPiece.getTeam() != team) {
                    if (!moveLeavesKingInCheck(piece, gameModel, targetPos)) {
                        moves.add(targetPos);
                    }
                }
            }
        }
        return moves;
    }

    @Override
    public Set<BoardPosition> getAttackedSquares(Piece piece, GameModel gameModel) {
        Set<BoardPosition> attacked = new HashSet<>();
        if (piece.getPosition() == null || gameModel == null) {
            return attacked;
        }
        BoardPosition startPos = piece.getPosition();

        for (int[] offset : OFFSETS) {
            BoardPosition targetPos = startPos.add(offset[0], offset[1]);
            if (gameModel.isWithinBounds(targetPos)) {
                attacked.add(targetPos);
            }
        }
        return attacked;
    }

    @Override
    public String getIdentifier() {
        return "L_SHAPE";
    }
}
