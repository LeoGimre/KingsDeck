package io.WizardsChessMaster.model.pieces.move;

import com.badlogic.gdx.Gdx; // For logging
import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.Piece;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Component for Pawn's En Passant capture move.
 * Relies on the GameModel providing the en passant target square.
 */
public class EnPassantMoveComponent implements MoveComponent {

    public EnPassantMoveComponent() {
    }

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
        BoardPosition enPassantTarget = gameModel.getEnPassantTargetSquareObject();

        if (enPassantTarget == null) {
            return moves;
        }

        int enPassantRank = (team == Team.WHITE) ? 4 : 3;
        if (position.getY() != enPassantRank) {
            return moves;
        }

        int direction = (team == Team.WHITE) ? 1 : -1;
        int[] captureDx = {-1, 1};

        for (int dx : captureDx) {
            BoardPosition potentialTarget = position.add(dx, direction);
            // Use .equals() for BoardPosition comparison
            if (enPassantTarget.equals(potentialTarget)) {
                BoardPosition victimPos = position.add(dx, 0);
                if (!moveLeavesKingInCheckWithVictim(piece, gameModel, potentialTarget, victimPos)) {
                    moves.add(potentialTarget);
                    Gdx.app.debug("EnPassantMoveComponent", "Valid EP move found for " + piece.getTypeName() + " at " + position + " to " + potentialTarget);
                }
            }
        }
        return moves;
    }

    @Override
    public Set<BoardPosition> getAttackedSquares(Piece piece, GameModel gameModel) {
        return Collections.emptySet();
    }

    @Override
    public String getIdentifier() {
        return "EN_PASSANT";
    }
}