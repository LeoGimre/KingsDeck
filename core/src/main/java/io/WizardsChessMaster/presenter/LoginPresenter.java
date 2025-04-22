package io.WizardsChessMaster.presenter;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.view.interfaces.ILoginView;

public class LoginPresenter {

    private final Main game;
    private final FirebaseService firebaseService;
    private ILoginView view;


    public LoginPresenter(Main game, FirebaseService firebaseService, ILoginView view) {
        this.game = game;
        this.firebaseService = firebaseService;
        this.view = view;
    }

    // The AuthListener provided by the View is primarily for the View to update itself.
    public void handleGoogleLogin(FirebaseService.AuthListener viewFeedbackListener) {
        if (view != null) {
            view.setStatusMessage("Signing in...", false);
        }

        firebaseService.signInWithGoogle(new FirebaseService.AuthListener() {
            @Override
            public void onSuccess() {
                Gdx.app.log("LoginController", "Google Sign-In Successful - Verifying Profile...");
                // Keep profile check logic here, as it's controller-level logic
                String userId = firebaseService.getCurrentUserId();
                String displayName = firebaseService.getCurrentUserDisplayName();

                if (userId != null && displayName != null) {
                    firebaseService.createUserProfileIfNotExists(userId, displayName, new FirebaseService.AuthListener() {
                        @Override
                        public void onSuccess() {
                            Gdx.app.log("LoginController", "User profile checked/created successfully. Navigating...");
                            // Navigate to Main Menu only after profile check is done
                            Gdx.app.postRunnable(() -> game.showMainMenuScreen());
                            // Notify view listener of overall success AFTER profile check
                            if (viewFeedbackListener != null) {
                                viewFeedbackListener.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Gdx.app.error("LoginController", "Failed to check/create user profile: " + errorMessage);
                            Gdx.app.postRunnable(() -> game.showMainMenuScreen());
                            // Notify view listener of overall success even if profile check had issues
                            if (viewFeedbackListener != null) {
                                viewFeedbackListener.onSuccess();
                            }
                        }
                    });
                } else {
                    Gdx.app.error("LoginController", "Could not get user ID or display name after login.");
                    if (viewFeedbackListener != null) {
                        viewFeedbackListener.onFailure("Could not retrieve user details after login.");
                    }
                    if (view != null) {
                        Gdx.app.postRunnable(()-> view.setStatusMessage("Login Error: Could not retrieve user details.", true));
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Gdx.app.error("LoginController", "Google Sign-In Failed: " + errorMessage);
                // Notify view listener of failure
                if (viewFeedbackListener != null) {
                    viewFeedbackListener.onFailure(errorMessage);
                }
                if (view != null) {
                    Gdx.app.postRunnable(()-> view.setStatusMessage("Login Failed: " + errorMessage, true));
                }
            }
        });
    }
}