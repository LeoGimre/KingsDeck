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
 * Component for an L-shaped attack where the attacking piece does not move.
 * E.g., An Archer shooting in an L-shape.
 */
public class RangedLAttackMoveComponent implements MoveComponent {

    private static final int[][] OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    public RangedLAttackMoveComponent() {}

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
                // Target square must contain an opponent's piece
                if (targetPiece != null && targetPiece.getTeam() != team) {
                    // Check if performing this ranged capture leaves the king in check
                    if (!rangedAttackLeavesKingInCheck(piece, gameModel, targetPos)) {
                        moves.add(targetPos);
                    }
                }
            }
        }
        return moves;
    }

    @Override
    public Set<BoardPosition> getAttackedSquares(Piece piece, GameModel gameModel) {
        // Defines the squares threatened by this attack pattern
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

    /**
     * Checks if performing the ranged attack (removing the target piece without moving the attacker)
     * would leave the attacker's king in check.
     */
    private boolean rangedAttackLeavesKingInCheck(Piece attacker, GameModel model, BoardPosition targetSquare) {
        GameModel simModel = model.copy();
        Piece simAttacker = simModel.getPieceAt(attacker.getPosition());
        Piece simTarget = simModel.getPieceAt(targetSquare);

        if (simAttacker == null) {
            Gdx.app.error("RangedLAttackMoveComponent", "Simulation Error: Attacker not found at " + attacker.getPosition());
            return true;
        }
        if (simTarget != null) {
            simModel.removePieceAt(targetSquare);
        } else {
            Gdx.app.error("RangedLAttackMoveComponent", "Simulation Error: Target piece not found at " + targetSquare + " for ranged attack check.");
            return true;
        }

        return simModel.isKingInCheck(attacker.getTeam());
    }


    @Override
    public String getIdentifier() {
        return "RANGED_L_ATTACK";
    }
}