package io.WizardsChessMaster.view.interfaces;

import io.WizardsChessMaster.model.MatchHistoryEntry;
import java.util.List;

/**
 * Interface defining the methods a ProfileController can use
 * to interact with its corresponding View (ProfileScreen).
 */
public interface IProfileView {

    void updateUI();

    void showStatus(String message, boolean isError);

    void updateMatchHistory(List<MatchHistoryEntry> history, boolean isLoadingError);
}