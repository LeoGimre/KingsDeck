package io.WizardsChessMaster.presenter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Timer;
import java.util.Map;
import java.util.HashMap;

import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.config.ConfigLoader;
import io.WizardsChessMaster.config.GameSettings;
import io.WizardsChessMaster.model.DeckModel;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.UserModel;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.view.interfaces.IMatchmakingView;
import io.WizardsChessMaster.view.interfaces.IMatchmakingView.MatchmakingMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MatchmakingPresenter {

    private static final String TAG = "MatchmakingCtrl";

    private final Main game;
    private final FirebaseService firebaseService;
    private IMatchmakingView view;
    private final GameSettings settings;

    // Data
    private List<DeckModel> allUserDecks = new ArrayList<>();
    private UserModel currentUserProfile = null;

    // State per Tab/Mode
    private MatchmakingMode currentMode = MatchmakingMode.RANKED;
    private Integer selectedPointLimit = null;
    private String selectedTimeLimit = null;
    private String selectedDeckName = null;

    // Activity State
    private boolean isSearchingRanked = false;
    private boolean isHosting = false;
    private boolean isJoining = false;
    private String hostedGameId = null;
    private String displayedHostCode = null;

    // Listeners & Timers
    private Timer.Task statusClearTask;
    private Object rankedGameListenerReg = null;
    private Object hostedGameListenerReg = null;
    private Map<String, Object> activeHostedGameListeners = new HashMap<>();


    public MatchmakingPresenter(Main game, FirebaseService firebaseService, IMatchmakingView view) {
        this.game = game;
        this.firebaseService = firebaseService;
        this.view = view;
        this.settings = ConfigLoader.getSettings();
    }

    public void initialize() {
        Gdx.app.log(TAG, "Initializing Matchmaking Presenter...");
        // Reset state variables
        isSearchingRanked = false;
        isHosting = false;
        isJoining = false;
        hostedGameId = null;
        displayedHostCode = null;
        stopListeners();

        selectedPointLimit = settings.matchmaking.defaultPointLimit;
        selectedTimeLimit = settings.matchmaking.defaultTimeLimit;

        loadUserDecks();
        fetchCurrentUserProfile();
    }

    // --- Data Loading ---
    public void loadUserDecks() {
        if (!firebaseService.isLoggedIn()) {
            if (view != null) view.setStatusMessage("Error: Not logged in.", true);
            return;
        }
        if (view != null && allUserDecks.isEmpty()) view.setStatusMessage("Loading decks...", false);
        firebaseService.loadDecks(new FirebaseService.DeckLoadListener() {
            @Override
            public void onSuccess(List<DeckModel> decks) {
                Gdx.app.log(TAG, "Successfully loaded " + (decks != null ? decks.size() : 0) + " decks.");
                allUserDecks = decks != null ? decks : new ArrayList<>();
                allUserDecks.sort(Comparator.comparing(DeckModel::getName, String.CASE_INSENSITIVE_ORDER));
                // Update deck list based on the current mode and its point limit
                updateDeckListForCurrentMode();
                if (view != null && !isErrorStatus()) view.setStatusMessage("", false);
            }
            @Override
            public void onFailure(String errorMessage) {
                Gdx.app.error(TAG, "Failed to load decks: " + errorMessage);
                allUserDecks = new ArrayList<>();
                if (view != null) {
                    view.updateDeckList(new ArrayList<>(), "Error loading decks");
                    view.setStatusMessage("Error loading decks.", true);
                    scheduleStatusClear();
                }
                updateButtonStatesForCurrentMode();
            }
        });
    }

    private void fetchCurrentUserProfile() {
        if (!firebaseService.isLoggedIn()) return;
        firebaseService.fetchUserProfile(new FirebaseService.UserProfileListener() {
            @Override
            public void onSuccess(UserModel userModel) {
                currentUserProfile = userModel;
                Gdx.app.log(TAG, "User profile fetched for ELO: " + (userModel != null ? userModel.getEloRating() : "N/A"));
                updateButtonStatesForCurrentMode();
            }
            @Override
            public void onFailure(String errorMessage) {
                currentUserProfile = null;
                Gdx.app.error(TAG, "Failed to fetch user profile for ELO: " + errorMessage);
                if (view != null) view.setStatusMessage("Could not get player rating.", true);
                updateButtonStatesForCurrentMode();
                scheduleStatusClear();
            }
        });
    }

    // --- UI Event Handlers ---
    public void handleTabSwitched(MatchmakingMode newMode) {
        Gdx.app.log(TAG, "Handling tab switch to: " + newMode);
        if (currentMode == newMode && !isAnyActivityActive()) {
            // Re-sync defaults if switching back to a mode
            selectedPointLimit = settings.matchmaking.defaultPointLimit;
            selectedTimeLimit = settings.matchmaking.defaultTimeLimit;
            selectedDeckName = null;
            updateDeckListForCurrentMode();
            updateButtonStatesForCurrentMode();
            return;
        }
        if (currentMode == newMode) return;

        cleanupMatchmakingAttempt();

        currentMode = newMode;
        // Set defaults for the new mode
        selectedPointLimit = settings.matchmaking.defaultPointLimit;
        selectedTimeLimit = settings.matchmaking.defaultTimeLimit;
        selectedDeckName = null;

        // Update deck list for the new mode
        updateDeckListForCurrentMode();

        // Update button states for the new mode
        updateButtonStatesForCurrentMode();
    }

    public void handlePointLimitChanged(Integer newPointLimit) {
        if (currentMode == MatchmakingMode.JOIN) return;

        if (!Objects.equals(selectedPointLimit, newPointLimit)) {
            Gdx.app.log(TAG, "Point limit changed to: " + newPointLimit);
            selectedPointLimit = newPointLimit;
            selectedDeckName = null;
            updateDeckListForCurrentMode();
            updateButtonStatesForCurrentMode();
        }
    }

    public void handleTimeLimitChanged(String timeLimit) {
        if (currentMode == MatchmakingMode.JOIN) return;

        if (!Objects.equals(selectedTimeLimit, timeLimit)) {
            Gdx.app.log(TAG, "Time limit changed to: " + timeLimit);
            selectedTimeLimit = timeLimit;
            updateButtonStatesForCurrentMode();
        }
    }

    public void handleDeckChanged(String deckName) {
        // Normalize selection (treat placeholders as null)
        String newSelectedDeckName = null;
        if (deckName != null && !deckName.startsWith("Select") && !deckName.startsWith("No decks") && !deckName.startsWith("Error") && !deckName.startsWith("Load") && !deckName.startsWith("Create")) {
            newSelectedDeckName = deckName;
        }

        if (!Objects.equals(selectedDeckName, newSelectedDeckName)) {
            selectedDeckName = newSelectedDeckName;
            Gdx.app.log(TAG, "Deck selection changed to: " + selectedDeckName + " for mode " + currentMode);
            updateButtonStatesForCurrentMode();
        }
    }

    // Check if parameters are valid for the given mode
    public boolean areMatchParametersSelected(MatchmakingMode mode) {
        boolean deckSelected = selectedDeckName != null && !selectedDeckName.isEmpty();
        boolean profileLoaded = currentUserProfile != null;

        switch (mode) {
            case RANKED:
            case HOST:
                boolean pointSelected = selectedPointLimit != null;
                boolean timeSelected = selectedTimeLimit != null && !selectedTimeLimit.isEmpty();
                return pointSelected && timeSelected && deckSelected && profileLoaded;
            case JOIN:
                String joinCode = view != null ? view.getJoinCodeInput() : "";
                boolean codeEntered = !joinCode.isEmpty() && joinCode.length() == 6;
                return deckSelected && codeEntered && profileLoaded;
            default:
                return false;
        }
    }

    // --- Action Button Handlers ---
    public void handleFindRankedMatch() {
        if (currentMode != MatchmakingMode.RANKED || isSearchingRanked || isHosting || isJoining) return;
        if (!areMatchParametersSelected(MatchmakingMode.RANKED)) {
            Gdx.app.log(TAG, "Find Ranked Match clicked but parameters not selected or profile not loaded.");
            if (view != null) view.setStatusMessage("Please select all options.", true);
            scheduleStatusClear();
            return;
        }
        if (rankedGameListenerReg != null) {
            Gdx.app.log(TAG, "Find Ranked Match clicked, but listener already active. Cancelling previous.");
            firebaseService.stopListeningForMyRankedGame(rankedGameListenerReg);
            rankedGameListenerReg = null;
        }

        Gdx.app.log(TAG, "Starting ranked matchmaking process: Points=" + selectedPointLimit + ", Time=" + selectedTimeLimit + ", Deck=" + selectedDeckName + ", ELO=" + currentUserProfile.getEloRating());
        isSearchingRanked = true;
        if (view != null) {
            view.setStatusMessage("Searching for opponent...", false);
            view.showSearchingIndicator(true);
            view.disableControls(true);
            view.updateCancelButtonState(MatchmakingMode.RANKED, true, true);
        }
        String userId = currentUserProfile.getUserId();

        // Start listening for a game assigned to me
        rankedGameListenerReg = firebaseService.listenForMyRankedGame(userId, new FirebaseService.GameFoundListener() {
            @Override
            public void onGameFound(GameModel gameModel) {
                if (!isSearchingRanked) return;
                isSearchingRanked = false;
                rankedGameListenerReg = null;
                firebaseService.cancelRankedMatchmaking(userId, null);

                String gameId = gameModel.getGameId();
                String opponentId = gameModel.getOpponentId(userId);
                String opponentDisplayName = gameModel.getOpponentDisplayName(userId);
                String playerColor = gameModel.getMyColor(userId);

                if (gameId == null || opponentId == null || playerColor == null) {
                    Gdx.app.error(TAG, "Ranked game found via listener but required data is null!");
                    handleMatchmakingError("Error joining game (invalid data). Please try again.");
                    return;
                }

                Gdx.app.log(TAG, "Ranked game found! ID: " + gameId + ", Opponent: " + opponentDisplayName + ", Color: " + playerColor);
                if (view != null) {
                    view.setStatusMessage("Opponent found: " + (opponentDisplayName != null ? opponentDisplayName : "???") + "!", false);
                    view.showSearchingIndicator(false);
                    // Navigation happens in postRunnable
                }
                Gdx.app.postRunnable(() -> {
                    if (game != null) {
                        game.showGameplayScreen(gameId, opponentId, opponentDisplayName != null ? opponentDisplayName : "Opponent", playerColor);
                    } else { handleMatchmakingError("Error transitioning to game."); }
                });
            }
            @Override
            public void onError(String errorMessage) {
                if (!isSearchingRanked) return;
                handleMatchmakingError("Error waiting for match: " + errorMessage);
            }
        });

        if (rankedGameListenerReg == null) {
            handleMatchmakingError("Error starting matchmaking listener.");
            return;
        }

        // Enter the queue
        firebaseService.enterRankedMatchmaking(
                userId, currentUserProfile.getDisplayName(), currentUserProfile.getEloRating(),
                selectedPointLimit, selectedTimeLimit, selectedDeckName,
                new FirebaseService.AuthListener() {
                    @Override public void onSuccess() {
                        if (!isSearchingRanked) return;
                        Gdx.app.log(TAG,"Entered ranked queue/initiated match. Waiting for listener.");
                        if (view != null) view.setStatusMessage("Waiting for opponent...", false);
                    }
                    @Override public void onFailure(String errorMessage) {
                        if (!isSearchingRanked) return;
                        handleMatchmakingError("Matchmaking error: " + errorMessage);
                    }
                }
        );
    }

    public void handleHostGame() {
        if (currentMode != MatchmakingMode.HOST || isSearchingRanked || isHosting || isJoining) return;
        if (!areMatchParametersSelected(MatchmakingMode.HOST)) {
            Gdx.app.log(TAG, "Host Game clicked but parameters not selected or profile not loaded.");
            if (view != null) view.setStatusMessage("Please select all options.", true);
            scheduleStatusClear();
            return;
        }
        // Check the object itself, not the map
        if (hostedGameListenerReg != null) {
            Gdx.app.error(TAG, "Host Game clicked, but already hosting. Should not happen if UI state is correct.");
            return;
        }

        Gdx.app.log(TAG, "Starting host game process: Points=" + selectedPointLimit + ", Time=" + selectedTimeLimit + ", Deck=" + selectedDeckName);
        isHosting = true;
        hostedGameId = null;
        displayedHostCode = null;

        if (view != null) {
            view.setStatusMessage("Creating game...", false);
            view.showSearchingIndicator(true);
            view.disableControls(true);
            view.updateCancelButtonState(MatchmakingMode.HOST, true, true);
            view.displayHostCode(null);
        }
        String userId = currentUserProfile.getUserId();

        // Store the returned Object handle
        hostedGameListenerReg = firebaseService.hostGame(
                userId, currentUserProfile.getDisplayName(), selectedPointLimit, selectedTimeLimit, selectedDeckName,
                new FirebaseService.HostedGameListener() {
                    @Override
                    public void onWaitingForPlayer(String gameId, String joinCode) {
                        if (!isHosting) return;
                        hostedGameId = gameId;
                        displayedHostCode = joinCode;
                        Gdx.app.log(TAG, "Now hosting game " + gameId + " with code " + joinCode);
                        if (view != null) {
                            view.setStatusMessage("Waiting for player to join...", false);
                            view.displayHostCode(joinCode);
                            view.showSearchingIndicator(false);
                        }
                        // Add the registration to the map
                        if(hostedGameListenerReg != null) {
                            activeHostedGameListeners.put(gameId, hostedGameListenerReg);
                        }
                    }
                    @Override
                    public void onPlayerJoined(GameModel gameModel) {
                        if (!isHosting) return;
                        isHosting = false;

                        String gameId = gameModel.getGameId();
                        String opponentId = gameModel.getOpponentId(userId);
                        String opponentDisplayName = gameModel.getOpponentDisplayName(userId);
                        String playerColor = gameModel.getMyColor(userId);

                        // Stop listening AFTER processing the join event
                        firebaseService.stopHostingGameListener(hostedGameListenerReg);
                        activeHostedGameListeners.remove(gameId);
                        hostedGameListenerReg = null;
                        hostedGameId = null;
                        displayedHostCode = null;


                        if (gameId == null || opponentId == null || playerColor == null) {
                            Gdx.app.error(TAG, "Player joined hosted game but required data is null!");
                            handleMatchmakingError("Error starting game (invalid data). Please try again.");
                            return;
                        }

                        Gdx.app.log(TAG, "Player joined hosted game! ID: " + gameId + ", Opponent: " + opponentDisplayName + ", Color: " + playerColor);
                        if (view != null) {
                            view.setStatusMessage("Player joined: " + (opponentDisplayName != null ? opponentDisplayName : "???") + "!", false);
                            view.displayHostCode(null);
                            // Navigation happens in postRunnable
                        }
                        Gdx.app.postRunnable(() -> {
                            if (game != null) {
                                game.showGameplayScreen(gameId, opponentId, opponentDisplayName != null ? opponentDisplayName : "Opponent", playerColor);
                            } else { handleMatchmakingError("Error transitioning to game."); }
                        });
                    }
                    @Override
                    public void onHostingCancelled() {
                        // This is called if the listener detects the game was deleted or ended unexpectedly
                        if (!isHosting) return;
                        // Clear listener reference before error handling
                        firebaseService.stopHostingGameListener(hostedGameListenerReg);
                        activeHostedGameListeners.remove(hostedGameId);
                        hostedGameListenerReg = null;
                        handleMatchmakingError("Hosting was cancelled remotely or timed out.");
                    }
                    @Override
                    public void onError(String errorMessage) {
                        if (!isHosting) return;
                        // Clear listener reference before error handling
                        firebaseService.stopHostingGameListener(hostedGameListenerReg);
                        activeHostedGameListeners.remove(hostedGameId);
                        hostedGameListenerReg = null;
                        handleMatchmakingError("Hosting error: " + errorMessage);
                    }
                }
        );

        if (hostedGameListenerReg == null) {
            handleMatchmakingError("Error starting hosting listener.");
            return;
        }
    }

    public void handleJoinGameByCode() {
        if (currentMode != MatchmakingMode.JOIN || isSearchingRanked || isHosting || isJoining) return;
        if (!areMatchParametersSelected(MatchmakingMode.JOIN)) {
            Gdx.app.log(TAG, "Join Game clicked but deck not selected, code not entered, or profile not loaded.");
            if (view != null) view.setStatusMessage("Please select deck and enter a valid code.", true);
            scheduleStatusClear();
            return;
        }

        String joinCode = view.getJoinCodeInput();
        DeckModel joinerDeck = findDeckModelByName(selectedDeckName);

        if (joinerDeck == null) {
            Gdx.app.error(TAG, "Join Game failed: Could not find selected deck model for name: " + selectedDeckName);
            if (view != null) view.setStatusMessage("Error: Selected deck not found.", true);
            scheduleStatusClear();
            return;
        }

        Gdx.app.log(TAG, "Attempting to join game with code: " + joinCode + ", Deck: " + selectedDeckName);
        isJoining = true;

        if (view != null) {
            view.setStatusMessage("Joining game with code " + joinCode + "...", false);
            view.showSearchingIndicator(true);
            view.disableControls(true);
            view.updateCancelButtonState(MatchmakingMode.JOIN, true, true);
        }
        String userId = currentUserProfile.getUserId();

        firebaseService.joinGameByCode(
                userId, currentUserProfile.getDisplayName(), joinerDeck, joinCode,
                new FirebaseService.JoinGameListener() {
                    @Override
                    public void onJoinSuccess(GameModel gameModel) {
                        if (!isJoining) return;
                        isJoining = false;

                        String gameId = gameModel.getGameId();
                        String opponentId = gameModel.getOpponentId(userId);
                        String opponentDisplayName = gameModel.getOpponentDisplayName(userId);
                        String playerColor = gameModel.getMyColor(userId);

                        if (gameId == null || opponentId == null || playerColor == null) {
                            Gdx.app.error(TAG, "Joined game via code but required data is null!");
                            handleMatchmakingError("Error starting game (invalid data). Please try again.");
                            return;
                        }

                        Gdx.app.log(TAG, "Successfully joined game via code! ID: " + gameId + ", Opponent: " + opponentDisplayName + ", Color: " + playerColor);
                        if (view != null) {
                            view.setStatusMessage("Joined game with " + (opponentDisplayName != null ? opponentDisplayName : "???") + "!", false);
                            view.showSearchingIndicator(false);
                            view.clearJoinCodeInput();
                            // Navigation happens in postRunnable
                        }
                        Gdx.app.postRunnable(() -> {
                            if (game != null) {
                                game.showGameplayScreen(gameId, opponentId, opponentDisplayName != null ? opponentDisplayName : "Opponent", playerColor);
                            } else { handleMatchmakingError("Error transitioning to game."); }
                        });
                    }
                    @Override
                    public void onJoinFailure(String errorMessage) {
                        if (!isJoining) return;
                        handleMatchmakingError("Failed to join game: " + errorMessage);
                    }
                    @Override
                    public void onPointLimitMismatch(int requiredLimit) {
                        if (!isJoining) return;
                        Gdx.app.log(TAG, "Join cancelled due to point limit mismatch. Required: " + requiredLimit);
                        handleMatchmakingError("Deck invalid. Host requires point limit <= " + requiredLimit);
                    }
                }
        );
    }

    // --- Cancel Button Handler ---

    public void handleCancel(MatchmakingMode mode) {
        Gdx.app.log(TAG, "Cancel requested for mode: " + mode);
        switch (mode) {
            case RANKED:
                if (isSearchingRanked) {
                    cleanupMatchmakingAttempt();
                    if(view != null) view.setStatusMessage("Ranked search cancelled.", false);
                    scheduleStatusClear();
                }
                break;
            case HOST:
                if (isHosting) {
                    cleanupMatchmakingAttempt(); // This will call cancelHostedGame if needed
                    if(view != null) view.setStatusMessage("Hosting cancelled.", false);
                    scheduleStatusClear();
                }
                break;
            case JOIN:
                if (isJoining) {
                    // Join is usually a quick operation, but allow cancellation if it's stuck
                    isJoining = false;
                    if(view != null) {
                        view.setStatusMessage("Join attempt cancelled.", false);
                        view.showSearchingIndicator(false);
                        view.disableControls(false);
                        view.updateCancelButtonState(MatchmakingMode.JOIN, false, false);
                        updateButtonStatesForCurrentMode();
                    }
                    scheduleStatusClear();
                }
                break;
        }
    }

    // --- Navigation and Cleanup ---
    public void handleBack() {
        cleanupMatchmakingAttempt();
        game.showMainMenuScreen();
    }

    // Centralized error handling for matchmaking processes
    private void handleMatchmakingError(String message) {
        Gdx.app.error(TAG, "Matchmaking Error: " + message);
        cleanupMatchmakingAttempt();
        if (view != null) {
            view.setStatusMessage(message, true);
            scheduleStatusClear();

            // Ensure UI is reset
            view.showSearchingIndicator(false);
            view.disableControls(false);
            view.updateCancelButtonState(MatchmakingMode.RANKED, false, false);
            view.updateCancelButtonState(MatchmakingMode.HOST, false, false);
            view.updateCancelButtonState(MatchmakingMode.JOIN, false, false);
            updateButtonStatesForCurrentMode();
        }
    }

    // Centralized cleanup logic
    public void cleanupMatchmakingAttempt() {
        Gdx.app.log(TAG, "Cleaning up matchmaking attempt... Ranked: " + isSearchingRanked + ", Hosting: " + isHosting + ", Joining: " + isJoining);
        String userId = firebaseService.getCurrentUserId();

        // Stop ranked search
        if (isSearchingRanked || rankedGameListenerReg != null) {
            if (rankedGameListenerReg != null) {
                firebaseService.stopListeningForMyRankedGame(rankedGameListenerReg);
                rankedGameListenerReg = null;
                Gdx.app.log(TAG, "Stopped ranked game listener.");
            }
            if (userId != null && isSearchingRanked) {
                firebaseService.cancelRankedMatchmaking(userId, null);
                Gdx.app.log(TAG, "Requested removal from ranked queue.");
            }
        }

        // Stop hosting
        if (isHosting || hostedGameListenerReg != null || hostedGameId != null) {
            if (hostedGameListenerReg != null) {
                // If hostGame was called, the listener handle is stored directly
                firebaseService.stopHostingGameListener(hostedGameListenerReg);
                activeHostedGameListeners.remove(hostedGameId);
                hostedGameListenerReg = null;
                Gdx.app.log(TAG, "Stopped hosted game listener for game: " + hostedGameId);
            }
            if (hostedGameId != null && isHosting) {
                firebaseService.cancelHostedGame(hostedGameId, userId, null);
                Gdx.app.log(TAG, "Requested cancellation of hosted game: " + hostedGameId);
            }
        }

        // Reset flags
        isSearchingRanked = false;
        isHosting = false;
        isJoining = false;
        hostedGameId = null;
        displayedHostCode = null;

        // Reset UI state if view exists
        if (view != null) {
            view.showSearchingIndicator(false);
            view.disableControls(false);
            view.updateCancelButtonState(MatchmakingMode.RANKED, false, false);
            view.updateCancelButtonState(MatchmakingMode.HOST, false, false);
            view.updateCancelButtonState(MatchmakingMode.JOIN, false, false);
            view.displayHostCode(null);
            updateButtonStatesForCurrentMode();
        }
        if (statusClearTask != null) statusClearTask.cancel();
    }

    // --- Helper Methods ---

    public void updateDeckListForCurrentMode() {
        if (view == null) return;

        List<String> validDeckNames = new ArrayList<>();
        String placeholder = "No decks available";

        switch (currentMode) {
            case RANKED:
            case HOST:
                // Use the current selectedPointLimit for filtering
                if (selectedPointLimit == null) {
                    placeholder = "Select point limit";
                } else {
                    final int limit = selectedPointLimit;
                    validDeckNames = allUserDecks.stream()
                            .filter(deck -> deck != null && deck.getPointLimit() <= limit)
                            .map(DeckModel::getName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    placeholder = "No decks for " + limit + " points";
                }
                break;
            case JOIN:
                // Join tab shows all decks, filtering happens on join attempt
                validDeckNames = allUserDecks.stream()
                        .map(DeckModel::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                placeholder = "Create a deck first";
                break;
        }
        view.updateDeckList(validDeckNames, placeholder);
    }

    public void updateButtonStatesForCurrentMode() {
        if (view == null) return;
        boolean enableAction = areMatchParametersSelected(currentMode) && !isAnyActivityActive();
        view.updateActionButtonState(currentMode, enableAction);
    }

    private boolean isAnyActivityActive() {
        return isSearchingRanked || isHosting || isJoining;
    }

    private boolean isErrorStatus() {
        if (view == null) return false;
        String status = view.getCurrentStatusMessage();
        return status != null && (status.contains("Error") || status.contains("Failed") || status.contains("invalid"));
    }

    private DeckModel findDeckModelByName(String name) {
        if (name == null || allUserDecks == null) return null;
        return allUserDecks.stream()
                .filter(d -> name.equals(d.getName()))
                .findFirst()
                .orElse(null);
    }

    private void scheduleStatusClear() {
        if (statusClearTask != null) { statusClearTask.cancel(); }
        statusClearTask = Timer.schedule(new Timer.Task() {
            @Override public void run() {
                if (view != null && !isAnyActivityActive() && !isErrorStatus()) {
                    view.setStatusMessage("", false);
                }
            }
        }, 5f);
    }

    private void stopListeners() {
        if(rankedGameListenerReg != null) {
            firebaseService.stopListeningForMyRankedGame(rankedGameListenerReg);
            rankedGameListenerReg = null;
        }
        if (hostedGameListenerReg != null) {
            firebaseService.stopHostingGameListener(hostedGameListenerReg);
            hostedGameListenerReg = null;
            Gdx.app.log(TAG, "Stopped hosted game listener via direct handle.");
            if (hostedGameId != null) {
                activeHostedGameListeners.remove(hostedGameId);
            }
        } else if (!activeHostedGameListeners.isEmpty()) {
            Gdx.app.error(TAG, "Stopping orphaned hosted game listeners from map: " + activeHostedGameListeners.keySet());
            for (Object listenerHandle : activeHostedGameListeners.values()) {
                firebaseService.stopHostingGameListener(listenerHandle);
            }
            activeHostedGameListeners.clear();
        }
    }

    public void dispose() {
        Gdx.app.log(TAG,"Disposing MatchmakingController.");
        cleanupMatchmakingAttempt();
        if (statusClearTask != null) { statusClearTask.cancel(); statusClearTask = null; }
        this.view = null;
    }
}