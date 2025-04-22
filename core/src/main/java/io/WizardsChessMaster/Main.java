package io.WizardsChessMaster;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;

import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.model.DeckModel;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.view.DeckBuildingScreen;
import io.WizardsChessMaster.view.GameplayScreen;
import io.WizardsChessMaster.view.LoginScreen;
import io.WizardsChessMaster.view.MainMenuScreen;
import io.WizardsChessMaster.view.MatchmakingScreen;
import io.WizardsChessMaster.view.ProfileScreen;
import io.WizardsChessMaster.view.TutorialScreen;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {

    private final FirebaseService firebaseService;
    private Screen loginScreen;
    private Screen mainMenuScreen;
    private Screen profileScreen;
    private Screen deckBuildingScreen;
    private Screen matchmakingScreen;
    private Screen gameplayScreen;
    private Screen tutorialScreen;

    private Skin sharedSkin;

    public Main(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @Override
    public void create() {
        Gdx.app.log("Main", "Application Create");

        // Load shared resources like Skin here
        try {
            sharedSkin = new Skin(Gdx.files.internal("ui/uiskin.json"));
            Gdx.app.log("Main", "Shared skin loaded successfully.");

            // --- Apply Global Font Scaling ---
            float globalScale = 2.0f;
            Gdx.app.log("Main", "Applying global font scale: " + globalScale);

            // Get all BitmapFont resources from the skin and scale them
            ObjectMap<String, BitmapFont> fonts = sharedSkin.getAll(BitmapFont.class);
            if (fonts != null) {
                for (ObjectMap.Entry<String, BitmapFont> entry : fonts.entries()) {
                    BitmapFont font = entry.value;
                    String fontName = entry.key;
                    if (font != null) {
                        // Avoid rescaling if already scaled
                        if (font.getData().scaleX != globalScale || font.getData().scaleY != globalScale) {
                            font.getData().setScale(globalScale);
                            Gdx.app.log("FontScaling", "Scaled font '" + fontName + "' by " + globalScale);
                        } else {
                            Gdx.app.log("FontScaling", "Font '" + fontName + "' already scaled to " + globalScale);
                        }
                    }
                }
            } else {
                Gdx.app.log("Main", "No BitmapFont resources found in skin to scale.");
            }

        } catch (Exception e) {
            Gdx.app.error("Main", "FATAL: Could not load shared skin 'ui/uiskin.json'. Exiting.", e);
            Gdx.app.exit();
            return;
        }

        // --- Start Application Logic ---
        if (firebaseService.isLoggedIn()) {
            Gdx.app.log("Main", "User logged in. Checking for active game...");
            String userId = firebaseService.getCurrentUserId();
            if (userId != null) {
                firebaseService.checkForActiveGame(userId, new FirebaseService.ActiveGameCheckListener() {
                    @Override
                    public void onActiveGameFound(GameModel gameModel) {
                        Gdx.app.log("Main", "Active game found: " + gameModel.getGameId() + ". Navigating to GameplayScreen.");
                        // Extract necessary info from gameModel
                        String opponentId = gameModel.getOpponentId(userId);
                        String opponentDisplayName = gameModel.getOpponentDisplayName(userId);
                        String playerColor = gameModel.getMyColor(userId);

                        if (gameModel.getGameId() != null && opponentId != null && playerColor != null) {
                            // Ensure opponentDisplayName is not null
                            String finalOpponentDisplayName = opponentDisplayName != null ? opponentDisplayName : "Opponent";
                            showGameplayScreen(gameModel.getGameId(), opponentId, finalOpponentDisplayName, playerColor);
                        } else {
                            Gdx.app.error("Main", "Active game found, but required data is missing. Navigating to Main Menu.");
                            showMainMenuScreen();
                        }
                    }

                    @Override
                    public void onNoActiveGameFound() {
                        Gdx.app.log("Main", "No active game found. Navigating to Main Menu.");
                        showMainMenuScreen();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Gdx.app.error("Main", "Error checking for active game: " + errorMessage + ". Navigating to Main Menu.");
                        showMainMenuScreen();
                    }
                });
            } else {
                Gdx.app.error("Main", "User logged in but user ID is null? Showing Login Screen.");
                showLoginScreen();
            }
        } else {
            Gdx.app.log("Main", "User not logged in, showing Login Screen.");
            showLoginScreen();
        }
    }

    // --- Screen Management Methods ---

    private void setActiveScreen(Screen newScreen) {
        final Screen oldScreen = getScreen();

        // Hide the old screen before setting the new one
        if (oldScreen != null) {
            oldScreen.hide();
        }

        setScreen(newScreen);

        // Dispose the old screen after setting the new one, deferred to next frame
        if (oldScreen != null && oldScreen != newScreen) {
            Gdx.app.log("Main", "Scheduling disposal for screen: " + oldScreen.getClass().getSimpleName());
            // Defer disposal to the next frame using postRunnable
            Gdx.app.postRunnable(() -> {
                Gdx.app.log("Main", "Executing disposal for screen: " + oldScreen.getClass().getSimpleName());
                oldScreen.dispose();

                if (loginScreen == oldScreen) { loginScreen = null; }
                if (mainMenuScreen == oldScreen) { mainMenuScreen = null; }
                if (profileScreen == oldScreen) { profileScreen = null; }
                if (deckBuildingScreen == oldScreen) { deckBuildingScreen = null; }
                if (matchmakingScreen == oldScreen) { matchmakingScreen = null; }
                if (gameplayScreen == oldScreen) { gameplayScreen = null; }
                if (tutorialScreen == oldScreen) { tutorialScreen = null; }
            });
        }
    }


    public void showLoginScreen() {
        Gdx.app.log("Main", "Showing Login Screen");
        // Dispose previous instance if it exists
        if(loginScreen != null) {
            loginScreen.dispose();
            loginScreen = null;
        }
        // Pass sharedSkin instance
        if (sharedSkin == null) {
            Gdx.app.error("Main", "Cannot show LoginScreen, sharedSkin is null!");
            return;
        }
        loginScreen = new LoginScreen(this, firebaseService, sharedSkin);
        setActiveScreen(loginScreen);
    }

    public void showMainMenuScreen() {
        Gdx.app.log("Main", "Showing Main Menu Screen");
        // Dispose previous instance if it exists
        if(mainMenuScreen != null) {
            mainMenuScreen.dispose();
            mainMenuScreen = null;
        }
        // Pass sharedSkin instance
        if (sharedSkin == null) {
            Gdx.app.error("Main", "Cannot show MainMenuScreen, sharedSkin is null!");
            return;
        }
        mainMenuScreen = new MainMenuScreen(this, firebaseService, sharedSkin);
        setActiveScreen(mainMenuScreen);
    }

    public void showProfileScreen() {
        Gdx.app.log("Main", "Showing Profile Screen");
        // Dispose previous instance if it exists
        if(profileScreen != null) {
            profileScreen.dispose();
            profileScreen = null;
        }
        if (sharedSkin == null) {
            Gdx.app.error("Main", "Cannot show ProfileScreen, sharedSkin is null!");
            return;
        }
        profileScreen = new ProfileScreen(this, firebaseService, sharedSkin);
        setActiveScreen(profileScreen);
    }

    // Method for creating a NEW deck
    public void showDeckBuildingScreen() {
        Gdx.app.log("Main", "Showing Deck Building Screen (with new deck)");
        if (deckBuildingScreen != null) {
            final Screen oldDeckScreen = deckBuildingScreen;
            Gdx.app.postRunnable(oldDeckScreen::dispose);
            deckBuildingScreen = null;
        }

        if (sharedSkin == null) {
            Gdx.app.error("Main", "Cannot show DeckBuildingScreen, sharedSkin is null!");
            return;
        }
        deckBuildingScreen = new DeckBuildingScreen(this, firebaseService, null, sharedSkin);
        setActiveScreen(deckBuildingScreen);
    }

    // Method for EDITING an existing deck
    public void showDeckBuildingScreen(DeckModel deckToEdit) {
        Gdx.app.log("Main", "Showing Deck Building Screen (editing: " + (deckToEdit != null ? deckToEdit.getName() : "null") + ")");
        if (deckBuildingScreen != null) {
            final Screen oldDeckScreen = deckBuildingScreen;
            Gdx.app.postRunnable(oldDeckScreen::dispose);
            deckBuildingScreen = null;
        }

        if (sharedSkin == null) {
            Gdx.app.error("Main", "Cannot show DeckBuildingScreen, sharedSkin is null!");
            return;
        }
        deckBuildingScreen = new DeckBuildingScreen(this, firebaseService, deckToEdit, sharedSkin);
        setActiveScreen(deckBuildingScreen);
    }

    public void showMatchmakingScreen() {
        Gdx.app.log("Main", "Showing Matchmaking Screen");
        if(matchmakingScreen != null) {
            matchmakingScreen.dispose();
            matchmakingScreen = null;
        }

        if (sharedSkin == null) {
            Gdx.app.error("Main", "Cannot show MatchmakingScreen, sharedSkin is null!");
            return;
        }
        matchmakingScreen = new MatchmakingScreen(this, firebaseService, sharedSkin);
        setActiveScreen(matchmakingScreen);
    }

    public void showGameplayScreen(String gameId, String opponentId, String opponentDisplayName, String playerColor) {
        Gdx.app.log("Main", "Showing Gameplay Screen for game: " + gameId);
        if (gameplayScreen != null) {
            final Screen oldGameplayScreen = gameplayScreen;
            Gdx.app.postRunnable(oldGameplayScreen::dispose);
            gameplayScreen = null;
        }

        if (sharedSkin == null) {
            Gdx.app.error("Main", "Cannot show GameplayScreen, sharedSkin is null!");
            return;
        }
        gameplayScreen = new GameplayScreen(this, firebaseService, gameId, opponentId, opponentDisplayName, playerColor, sharedSkin);
        setActiveScreen(gameplayScreen);
    }

    /**
     * Shows the Tutorial screen.
     */
    public void showTutorialScreen() {
        Gdx.app.log("Main", "Showing Tutorial Screen");
        if (tutorialScreen != null) {
            tutorialScreen.dispose();
            tutorialScreen = null;
        }
        if (sharedSkin == null) {
            Gdx.app.error("Main", "Cannot show TutorialScreen, sharedSkin is null!");
            return;
        }
        tutorialScreen = new TutorialScreen(this, sharedSkin);
        setActiveScreen(tutorialScreen);
    }


    @Override
    public void dispose() {
        Gdx.app.log("Main", "Disposing application.");
        // Dispose the currently active screen first
        Screen currentScreen = getScreen();
        if (currentScreen != null) {
            currentScreen.dispose();
        }
        // Dispose any other screen instances that might still be referenced
        if (loginScreen != null && loginScreen != currentScreen) loginScreen.dispose();
        if (mainMenuScreen != null && mainMenuScreen != currentScreen) mainMenuScreen.dispose();
        if (profileScreen != null && profileScreen != currentScreen) profileScreen.dispose();
        if (deckBuildingScreen != null && deckBuildingScreen != currentScreen) deckBuildingScreen.dispose();
        if (matchmakingScreen != null && matchmakingScreen != currentScreen) matchmakingScreen.dispose();
        if (gameplayScreen != null && gameplayScreen != currentScreen) gameplayScreen.dispose();
        if (tutorialScreen != null && tutorialScreen != currentScreen) tutorialScreen.dispose();

        // Clear all references
        loginScreen = null;
        mainMenuScreen = null;
        profileScreen = null;
        deckBuildingScreen = null;
        matchmakingScreen = null;
        gameplayScreen = null;
        tutorialScreen = null;

        // Dispose shared resources like Skin here
        if (sharedSkin != null) {
            sharedSkin.dispose();
            sharedSkin = null;
            Gdx.app.log("Main", "Disposed shared skin.");
        }

        Gdx.app.log("Main", "Application disposed.");
        super.dispose();
    }
}