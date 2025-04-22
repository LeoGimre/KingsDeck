package io.WizardsChessMaster.view.interfaces;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.WizardsChessMaster.model.DeckModel;

import java.util.List;

/**
 * Interface defining methods for the DeckBuildingPresenter to interact
 * with the DeckBuildingScreen.
 */
public interface IDeckBuildingView {

    // --- Getters for Presenter ---
    Skin getSkin();
    Stage getStage();

    // --- UI State Updates ---
    void updateEditorState(DeckModel deck);
    void clearEditorState();
    void updatePointsLabel();
    void populateDeckList(List<DeckModel> decks);
    void selectDeckInList(String deckName);
    void resetDeckNameField(String name);
    void resetPointLimitSelector(int limit);
    void showStatusMessage(String message, boolean isError);
    void showUnsavedChangesIndicator(boolean show);

    // --- Board/Grid Updates ---
    void updateBoardSquare(int index);
    void updateSpellGridSquare(int index);

}