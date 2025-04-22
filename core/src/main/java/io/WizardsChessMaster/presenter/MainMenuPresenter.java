package io.WizardsChessMaster.presenter;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.view.interfaces.IMainMenuView;


public class MainMenuPresenter {

    private final Main game;
    private final FirebaseService firebaseService;
    private IMainMenuView view;


    public MainMenuPresenter(Main game, FirebaseService firebaseService, IMainMenuView view) {
        this.game = game;
        this.firebaseService = firebaseService;
        this.view = view;
    }

    public void handlePlayGame() {
        Gdx.app.log("MainMenuController", "Play Game Clicked - Navigating to Matchmaking");
        game.showMatchmakingScreen(); // Navigate to Matchmaking Screen
    }

    public void handleBuildDecks() {
        Gdx.app.log("MainMenuController", "Build Decks Clicked");
        game.showDeckBuildingScreen(); // Navigate to Deck Building Screen
    }

    public void handleProfile() {
        Gdx.app.log("MainMenuController", "Profile Clicked");
        game.showProfileScreen(); // Navigate to Profile Screen
    }

    public void handleTutorial() {
        Gdx.app.log("MainMenuController", "Tutorial Clicked");
        game.showTutorialScreen(); // Navigate to Tutorial Screen
    }

    public void handleSignOut(FirebaseService.AuthListener externalListener) {
        Gdx.app.log("MainMenuController", "Sign Out Clicked");
        if (view != null) {
            view.showStatusMessage("Signing out...", false);
        }
        firebaseService.signOut(new FirebaseService.AuthListener() {
            @Override
            public void onSuccess() {
                Gdx.app.log("MainMenuController", "Sign out successful, navigating to Login Screen.");
                Gdx.app.postRunnable(() -> game.showLoginScreen());
                if (externalListener != null) externalListener.onSuccess();
            }

            @Override
            public void onFailure(String errorMessage) {
                Gdx.app.error("MainMenuController", "Sign out failed: " + errorMessage);
                if (view != null) {
                    final String msg = "Sign out failed: " + errorMessage;
                    Gdx.app.postRunnable(()->view.showStatusMessage(msg, true));
                }
                if (externalListener != null) externalListener.onFailure(errorMessage);
            }
        });
    }
}