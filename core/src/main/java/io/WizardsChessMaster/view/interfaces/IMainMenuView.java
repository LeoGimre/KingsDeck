package io.WizardsChessMaster.view.interfaces;

/**
 * Interface defining the methods a MainMenuController can use
 * to interact with its corresponding View (MainMenuScreen).
 * Currently minimal as the controller primarily handles navigation.
 */
public interface IMainMenuView {

    void showStatusMessage(String message, boolean isError);

}