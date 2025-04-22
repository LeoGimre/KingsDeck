package io.WizardsChessMaster.model.pieces.move;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.Piece;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Component for King's/Wizard's single-step movement. Does not require parameters.
 */
public class SingleStepMoveComponent implements MoveComponent {

    private static final int[][] DIRECTIONS = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public SingleStepMoveComponent() {}

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
        Team opponentTeam = team.opposite();

        for (int[] dir : DIRECTIONS) {
            BoardPosition targetPos = startPos.add(dir[0], dir[1]);
            if (gameModel.isWithinBounds(targetPos)) {
                if(piece.getTypeName() == "KING") {
                    if (!gameModel.isSquareAttacked(targetPos, opponentTeam)) {
                        Piece targetPiece = gameModel.getPieceAt(targetPos);
                        if (targetPiece == null || targetPiece.getTeam() != team) {
                            moves.add(targetPos);
                        }
                    }
                }
                else {
                    Piece targetPiece = gameModel.getPieceAt(targetPos);
                    if (targetPiece == null || targetPiece.getTeam() != team) {
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

        for (int[] dir : DIRECTIONS) {
            BoardPosition targetPos = startPos.add(dir[0], dir[1]);
            if (gameModel.isWithinBounds(targetPos)) {
                attacked.add(targetPos);
            }
        }
        return attacked;
    }

    @Override
    public String getIdentifier() {
        return "SINGLE_STEP";
    }
}
