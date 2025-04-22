package io.WizardsChessMaster.presenter;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.model.MatchHistoryEntry;
import io.WizardsChessMaster.model.UserModel;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.view.interfaces.IProfileView;

import java.util.List;

public class ProfilePresenter {

    private final Main game;
    private final FirebaseService firebaseService;
    private final UserModel userModel;
    private IProfileView view;

    public ProfilePresenter(Main game, FirebaseService firebaseService, UserModel userModel, IProfileView view) {
        this.game = game;
        this.firebaseService = firebaseService;
        this.userModel = userModel;
        this.view = view;
    }

    /**
     * Initiates loading of the user profile data AND match history from Firebase.
     * Calls view methods upon success or failure.
     */
    public void loadUserProfile() {
        if (view == null) {
            Gdx.app.error("ProfilePresenter", "View is null, cannot load profile.");
            return;
        }

        if (!firebaseService.isLoggedIn()) {
            Gdx.app.error("ProfilePresenter", "User not logged in, cannot load profile.");
            handleLoadError("User not logged in.");
            handleHistoryLoadError("User not logged in.");
            return;
        }

        view.showStatus("Loading profile...", false);

        // Fetch Profile Info
        firebaseService.fetchUserProfile(new FirebaseService.UserProfileListener() {
            @Override
            public void onSuccess(UserModel fetchedModel) {
                Gdx.app.log("ProfilePresenter", "User profile fetched successfully: " + fetchedModel.toString());
                userModel.setUserId(fetchedModel.getUserId());
                userModel.setDisplayName(fetchedModel.getDisplayName());
                userModel.setEloRating(fetchedModel.getEloRating());
                userModel.setGamesPlayed(fetchedModel.getGamesPlayed());
                userModel.setGamesWon(fetchedModel.getGamesWon());

                if (view != null) {
                    Gdx.app.postRunnable(() -> {
                        if (view != null) {
                            view.showStatus("", false);
                            view.updateUI();
                        }
                    });
                }
                // Now fetch match history AFTER profile is loaded
                loadMatchHistory();
            }

            @Override
            public void onFailure(String errorMessage) {
                Gdx.app.error("ProfilePresenter", "Failed to fetch user profile: " + errorMessage);
                handleLoadError("Error loading profile: " + errorMessage);
                handleHistoryLoadError("Profile load failed.");
            }
        });
    }

    /**
     * Initiates loading of the match history data. Called after profile loads successfully.
     */
    private void loadMatchHistory() {
        String userId = userModel.getUserId();
        if (userId == null) {
            handleHistoryLoadError("Cannot load history: User ID unknown.");
            return;
        }

        Gdx.app.log("ProfilePresenter", "Loading match history for user: " + userId);
        Gdx.app.postRunnable(() -> {
            if (view != null) view.showStatus("Loading match history...", false);
        });


        firebaseService.fetchMatchHistory(userId, new FirebaseService.MatchHistoryListener() {
            @Override
            public void onSuccess(List<MatchHistoryEntry> history) {
                Gdx.app.log("ProfilePresenter", "Match history fetched successfully. Count: " + (history != null ? history.size() : 0));
                if (view != null) {
                    Gdx.app.postRunnable(() -> {
                        if (view != null) {
                            view.showStatus("", false);
                            view.updateMatchHistory(history, false);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Gdx.app.error("ProfilePresenter", "Failed to fetch match history: " + errorMessage);
                handleHistoryLoadError("Error loading history: " + errorMessage);
            }
        });
    }


    private void handleLoadError(String message) {
        // Update the shared UserModel instance with default/error values
        userModel.setUserId(null);
        userModel.setDisplayName("-");
        userModel.setEloRating(0);
        userModel.setGamesPlayed(0);
        userModel.setGamesWon(0);

        if (view != null) {
            Gdx.app.postRunnable(() -> {
                if (view != null) {
                    view.showStatus(message, true);
                    view.updateUI();
                }
            });
        }
    }

    private void handleHistoryLoadError(String message) {
        if (view != null) {
            Gdx.app.postRunnable(() -> {
                if (view != null) {
                    view.showStatus(message, true);
                    view.updateMatchHistory(null, true);
                }
            });
        }
    }

    /**
     * Handles the action to navigate back to the main menu.
     */
    public void handleBackToMenu() {
        Gdx.app.log("ProfilePresenter", "Navigating back to Main Menu");
        game.showMainMenuScreen();
    }
}