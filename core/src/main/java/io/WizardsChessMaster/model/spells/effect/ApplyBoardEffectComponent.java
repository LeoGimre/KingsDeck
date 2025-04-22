package io.WizardsChessMaster.model.spells.effect;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.spells.Spell;
import io.WizardsChessMaster.model.spells.SpellEffectComponent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ApplyBoardEffectComponent implements SpellEffectComponent {

    private static final String TAG = "ApplyBoardEffectComp";
    private static final String PAWN_STORM_EFFECT_NAME = "pawn_storm_active";

    private Spell spell;
    private String effectName;
    private String duration = "permanent";

    public ApplyBoardEffectComponent() {}

    @Override
    public void initialize(Spell spell, Map<String, String> params) {
        this.spell = spell;
        if (params != null) {
            this.effectName = params.get("effect_name");
            this.duration = params.getOrDefault("duration", "permanent").toLowerCase();
        }
        if (this.effectName == null || this.effectName.trim().isEmpty()) {
            Gdx.app.error(TAG, "Initialization failed for spell " + spell.getTypeName() + ": Missing required 'effect_name' parameter.");
            throw new IllegalArgumentException("ApplyBoardEffectComponent requires 'effect_name' parameter.");
        }
        Gdx.app.debug(TAG, "Initialized for spell " + spell.getTypeName() + " with effectName=" + effectName + ", duration=" + duration);
    }

    @Override
    public boolean applyEffect(String casterPlayerId, BoardPosition target, GameModel gameModel) {
        Gdx.app.log(TAG, "Applying board effect '" + effectName + "' for spell " + spell.getTypeName() + " (Caster: " + casterPlayerId + ", Duration: " + duration + ")");

        if ("current_turn".equals(duration)) {
            if (PAWN_STORM_EFFECT_NAME.equalsIgnoreCase(effectName)) {
                gameModel.addTurnEffect(casterPlayerId, PAWN_STORM_EFFECT_NAME);
                return true;
            } else {
                Gdx.app.error(TAG, "Unhandled current_turn effect name: " + effectName);
                return false;
            }
        } else if ("permanent".equals(duration)) {
            // TODO: Implement logic for permanent effects
            Gdx.app.error(TAG, "Permanent effects not yet implemented for: " + effectName);
            return false;
        } else {
            Gdx.app.error(TAG, "Unknown effect duration: " + duration + " for effect: " + effectName);
            return false;
        }
    }

    @Override
    public Set<BoardPosition> getValidTargetsForEffect(String casterPlayerId, GameModel gameModel) {
        return Collections.emptySet();
    }

    @Override
    public String getIdentifier() {
        return "APPLY_BOARD_EFFECT";
    }
}