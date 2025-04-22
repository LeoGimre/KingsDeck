package io.WizardsChessMaster.view.interfaces;

import java.util.List;

/**
 * Interface defining the methods a MatchmakingPresenter can use
 * to interact with its corresponding View (MatchmakingScreen).
 */
public interface IMatchmakingView {

    enum MatchmakingMode {
        RANKED,
        HOST,
        JOIN
    }

    void updateDeckList(List<String> deckNames, String placeholder);

    void clearDeckSelection();

    void updateActionButtonState(MatchmakingMode mode, boolean enabled);

    void updateCancelButtonState(MatchmakingMode mode, boolean enabled, boolean visible);

    void setStatusMessage(String message, boolean isError);

    String getCurrentStatusMessage();
    void showSearchingIndicator(boolean show);

    void disableControls(boolean disable);


    // --- New methods for Tabbed Interface ---

    void showTab(MatchmakingMode mode);

    void displayHostCode(String code);

    String getJoinCodeInput();

    void clearJoinCodeInput();

}