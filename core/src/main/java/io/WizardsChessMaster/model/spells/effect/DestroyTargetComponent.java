package io.WizardsChessMaster.model.spells.effect;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.Piece;
import io.WizardsChessMaster.model.pieces.PieceType;
import io.WizardsChessMaster.model.spells.Spell;
import io.WizardsChessMaster.model.spells.SpellEffectComponent;

import java.util.*;

public class DestroyTargetComponent implements SpellEffectComponent {

    private static final String TAG = "DestroyTargetComponent";

    private Spell spell;
    private String targetType = "enemy";
    private boolean allowEmpty = false;

    public DestroyTargetComponent() { }

    @Override
    public void initialize(Spell spell, Map<String, String> params) {
        this.spell = spell;
        if (params != null) {
            this.targetType = params.getOrDefault("target_type", "enemy").toLowerCase();
            this.allowEmpty = Boolean.parseBoolean(params.getOrDefault("allow_empty", "false"));
        }
        Gdx.app.debug(TAG, "Initialized for spell " + spell.getTypeName() + " with targetType=" + targetType + ", allowEmpty=" + allowEmpty);
    }

    @Override
    public boolean applyEffect(String casterPlayerId, BoardPosition target, GameModel gameModel) {
        if (target == null) {
            Gdx.app.error(TAG, spell.getTypeName() + " failed: Target is required for DestroyTargetComponent.");
            return false;
        }
        if (!gameModel.isWithinBounds(target)) {
            Gdx.app.error(TAG, spell.getTypeName() + " failed: Target " + target + " is out of bounds.");
            return false;
        }
        Piece targetPiece = gameModel.getPieceAt(target);
        if (targetPiece == null) {
            if (!allowEmpty) {
                Gdx.app.error(TAG, spell.getTypeName() + " failed: Target square " + target + " is empty, and allow_empty is false.");
                return false;
            } else {
                Gdx.app.log(TAG, spell.getTypeName() + " targeted empty square " + target + " (allowed). No piece destroyed.");
                return true;
            }
        }
        Team casterTeam = gameModel.getPlayerTeamById(casterPlayerId);
        if (casterTeam == null) {
            Gdx.app.error(TAG, spell.getTypeName() + " failed: Could not determine caster team for ID " + casterPlayerId);
            return false;
        }
        boolean isValidTarget = false;
        switch (targetType) {
            case "enemy": isValidTarget = targetPiece.getTeam() != casterTeam; break;
            case "friendly": isValidTarget = targetPiece.getTeam() == casterTeam; break;
            case "any": isValidTarget = true; break;
            case "non-king": isValidTarget = targetPiece.getTeam() != casterTeam && !PieceType.KING.name().equals(targetPiece.getTypeName()); break;
            default: Gdx.app.error(TAG, "Unknown target_type parameter: " + targetType); return false;
        }
        if ("non-king".equals(targetType) && PieceType.KING.name().equals(targetPiece.getTypeName())) { isValidTarget = false; }
        if (!isValidTarget) {
            Gdx.app.log(TAG, spell.getTypeName() + " failed: Target " + targetPiece + " at " + target + " does not match required target type '" + targetType + "' for caster " + casterPlayerId);
            return false;
        }
        Gdx.app.log(TAG, "Applying " + spell.getTypeName() + ": Destroying " + targetPiece + " at " + target);
        gameModel.removePieceAt(target);
        return true;
    }

    @Override
    public Set<BoardPosition> getValidTargetsForEffect(String casterPlayerId, GameModel gameModel) {
        Set<BoardPosition> targets = new HashSet<>();
        Team casterTeam = gameModel.getPlayerTeamById(casterPlayerId);
        if (casterTeam == null) {
            Gdx.app.error(TAG, "Cannot get targets: Could not determine caster team for ID " + casterPlayerId);
            return targets;
        }
        Team opponentTeam = casterTeam.opposite();
        for (int y = 0; y < gameModel.getBoardHeight(); y++) {
            for (int x = 0; x < gameModel.getBoardWidth(); x++) {
                BoardPosition pos = new BoardPosition(x, y);
                Piece piece = gameModel.getPieceAt(pos);
                if (piece == null) { if (allowEmpty) { targets.add(pos); } continue; }
                boolean isValidTarget = false;
                switch (targetType) {
                    case "enemy": isValidTarget = piece.getTeam() == opponentTeam; break;
                    case "friendly": isValidTarget = piece.getTeam() == casterTeam; break;
                    case "any": isValidTarget = true; break;
                    case "non-king": isValidTarget = piece.getTeam() == opponentTeam && !PieceType.KING.name().equals(piece.getTypeName()); break;
                    default: break;
                }
                if ("non-king".equals(targetType) && PieceType.KING.name().equals(piece.getTypeName())) { isValidTarget = false; }
                if (isValidTarget) { targets.add(pos); }
            }
        }
        return targets;
    }

    @Override
    public String getIdentifier() {
        return "DESTROY_TARGET";
    }
}