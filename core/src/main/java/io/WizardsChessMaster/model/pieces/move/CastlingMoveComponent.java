package io.WizardsChessMaster.model.pieces.move;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.Piece;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Component to generate valid Castling moves for a piece (intended for King).
 * Relies on the piece having an "hasMoved" boolean state variable.
 * Checks board state for involved Rook (must be "ROOK" type and have "hasMoved" == false).
 */
public class CastlingMoveComponent implements MoveComponent {

    /**
     * Public no-argument constructor required for ServiceLoader/Factory instantiation.
     */
    public CastlingMoveComponent() {}

    /**
     * Initializes the component. This component does not require parameters.
     *
     * @param piece The piece instance this component belongs to.
     * @param params A map of parameters (ignored by this component).
     */
    @Override
    public void initialize(Piece piece, Map<String, Integer> params) {
        if (!"KING".equals(piece.getTypeName())) {
            Gdx.app.error("CastlingMoveComponent", "Warning: CastlingMoveComponent initialized for non-KING piece type: " + piece.getTypeName());
        }
    }

    @Override
    public Set<BoardPosition> getValidMoves(Piece piece, GameModel gameModel) {
        Set<BoardPosition> moves = new HashSet<>();
        if (!"KING".equals(piece.getTypeName()) || piece.getPosition() == null || piece.getTeam() == null || gameModel == null) {
            return moves;
        }

        Team team = piece.getTeam();
        BoardPosition kingPos = piece.getPosition();
        Team opponentTeam = team.opposite();

        Object kingHasMovedObj = piece.getStateVariable("hasMoved");
        boolean kingHasMoved = true;
        if (kingHasMovedObj instanceof Boolean) {
            kingHasMoved = (Boolean) kingHasMovedObj;
        }

        if (kingHasMoved || gameModel.isKingInCheck(team)) {
            return moves;
        }

        int kingRank = (team == Team.WHITE) ? 0 : 7;
        if (kingPos.getY() != kingRank || kingPos.getX() != 4) {
            return moves;
        }

        // Check Kingside Castling (O-O)
        checkCastlingSide(piece, gameModel, kingRank, 7, 5, 6, moves);

        // Check Queenside Castling (O-O-O)
        checkCastlingSide(piece, gameModel, kingRank, 0, 3, 2, moves);

        return moves;
    }

    private void checkCastlingSide(Piece king, GameModel gameModel, int rank, int rookFile, int emptyFileCheck1, int kingTargetFile, Set<BoardPosition> moves) {
        Team team = king.getTeam();
        Team opponentTeam = team.opposite();
        BoardPosition rookPos = new BoardPosition(rookFile, rank);
        Piece potentialRook = gameModel.getPieceAt(rookPos);

        if (potentialRook == null || !"ROOK".equals(potentialRook.getTypeName())) {
            return;
        }
        Object rookHasMovedObj = potentialRook.getStateVariable("hasMoved");
        boolean rookHasMoved = true;
        if (rookHasMovedObj instanceof Boolean) {
            rookHasMoved = (Boolean) rookHasMovedObj;
        }
        if (rookHasMoved) {
            return;
        }

        BoardPosition kingTargetPos = new BoardPosition(kingTargetFile, rank);
        int startFile = Math.min(king.getPosition().getX(), rookFile) + 1;
        int endFile = Math.max(king.getPosition().getX(), rookFile);
        for (int file = startFile; file < endFile; file++) {
            if (gameModel.getPieceAt(new BoardPosition(file, rank)) != null) {
                return;
            }
        }

        BoardPosition kingPassThruPos = new BoardPosition((king.getPosition().getX() + kingTargetFile) / 2, rank);

        if (gameModel.isSquareAttacked(king.getPosition(), opponentTeam) ||
                gameModel.isSquareAttacked(kingPassThruPos, opponentTeam) ||
                gameModel.isSquareAttacked(kingTargetPos, opponentTeam)) {
            return;
        }
        moves.add(kingTargetPos);
    }


    @Override
    public Set<BoardPosition> getAttackedSquares(Piece piece, GameModel gameModel) {
        return new HashSet<>();
    }

    @Override
    public String getIdentifier() {
        return "CASTLING";
    }
}
