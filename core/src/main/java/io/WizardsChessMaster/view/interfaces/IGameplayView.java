package io.WizardsChessMaster.view.interfaces;

import io.WizardsChessMaster.model.spells.Spell;

import java.util.List;
import java.util.Map;

/**
 * Interface defining the methods a GameplayController can use
 * to interact with its corresponding View (GameplayScreen).
 */
public interface IGameplayView {

    void setPlayerInfoText(String text);

    void setOpponentInfoText(String text);

    void setPlayerDeckText(String text);

    void setStatusText(String text, boolean isError);

    void displayBoard(Map<String, String> boardState);

    void displaySpells(List<Spell> spells);

    void highlightValidMoves(List<String> squares);

    void highlightSpellTargets(List<String> squares);

    void clearHighlights();

    void setResignButtonEnabled(boolean enabled);

    void setOfferDrawButtonEnabled(boolean enabled);

    void showDrawOfferReceived(boolean visible);

    void showGameOverOverlay(String title, String message, String eloChange);

    void showErrorDialog(String message);

    void setPlayerColor(String color);

    void setPlayer1Timer(String time);

    void setPlayer2Timer(String time);

    void showOpponentDisconnected(boolean disconnected, String timeUntilTimeout);

    void showConnectionStatus(boolean connected, boolean reconnecting);
}