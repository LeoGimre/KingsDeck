package io.WizardsChessMaster.model.pieces.move;

import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.Piece;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Component for Pawn's diagonal capture move. Does not require parameters.
 */
public class PawnCaptureComponent implements MoveComponent {

    public PawnCaptureComponent() {}

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
        int direction = (team == Team.WHITE) ? 1 : -1;
        int[] captureDx = {-1, 1};

        for (int dx : captureDx) {
            BoardPosition capturePos = position.add(dx, direction);
            if (gameModel.isWithinBounds(capturePos)) {
                Piece targetPiece = gameModel.getPieceAt(capturePos);
                if (targetPiece != null && targetPiece.getTeam() != team) {
                    if (!moveLeavesKingInCheck(piece, gameModel, capturePos)) {
                        moves.add(capturePos);
                    }
                }
            }
        }
        return moves;
    }

    @Override
    public Set<BoardPosition> getAttackedSquares(Piece piece, GameModel gameModel) {
        Set<BoardPosition> attacked = new HashSet<>();
        if (piece.getPosition() == null || piece.getTeam() == null || gameModel == null) {
            return attacked;
        }
        BoardPosition position = piece.getPosition();
        Team team = piece.getTeam();
        int direction = (team == Team.WHITE) ? 1 : -1;
        int[] captureDx = {-1, 1};

        for (int dx : captureDx) {
            BoardPosition capturePos = position.add(dx, direction);
            if (gameModel.isWithinBounds(capturePos)) {
                attacked.add(capturePos);
            }
        }
        return attacked;
    }

    @Override
    public String getIdentifier() {
        return "PAWN_CAPTURE";
    }
}
