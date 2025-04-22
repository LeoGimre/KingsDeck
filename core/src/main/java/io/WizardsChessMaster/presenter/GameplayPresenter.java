package io.WizardsChessMaster.presenter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Timer;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.DeckModel;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.GameStatus;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.UserModel;
import io.WizardsChessMaster.model.pieces.Piece;
import io.WizardsChessMaster.model.spells.Spell;
import io.WizardsChessMaster.model.spells.SpellConfig;
import io.WizardsChessMaster.model.spells.SpellFactory;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.view.interfaces.IGameplayView;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GameplayPresenter implements FirebaseService.GameEventListener, FirebaseService.GameEndListener, FirebaseService.DeckLoadListener {

    private static final String TAG = "GameplayCtrl";
    private static final long ABANDONMENT_TIMEOUT_MILLIS = 60 * 1000; // 1 minute
    private static final float HEARTBEAT_INTERVAL_SECONDS = 3f; // Send heartbeat every 3s

    private final Main game;
    private final FirebaseService firebaseService;
    private IGameplayView view;

    // Game identifiers/state
    private final String gameId;
    private final String opponentId;
    private final String opponentDisplayName;
    private final String playerColorString;
    private final Team playerTeam;
    private final Team opponentTeam;
    private final String currentPlayerId;
    private GameModel currentGameModel;
    private List<DeckModel> playerDecks;
    private DeckModel playerDeckModel;
    private UserModel currentUserProfile;
    private String selectedPieceSquare = null;
    private List<String> validMovesForSelectedPiece = new ArrayList<>();
    private Object gameListenerRegistration;
    private boolean gameEnded = false;
    private boolean gameResultRecordingAttempted = false;
    private boolean isResigning = false;
    private boolean drawOfferedByMe = false;
    private boolean drawOfferedByOpponent = false;
    private boolean localBoardInitialized = false;

    // Spell State
    private List<Spell> availableSpells = new ArrayList<>();
    private Spell selectedSpell = null;
    private boolean isTargetingSpell = false;
    // Track last non-turn-ending spell cast locally
    private boolean justCastNonEndingSpell = false;
    private String lastCastedSpellName = null;

    // Timer and Heartbeat state
    private Timer.Task heartbeatTask = null;
    private long turnStartTimeMillis = 0L;
    private boolean isConnected = true;
    private boolean isAttemptingReconnect = false;

    // Local timers for smooth countdown
    private long localP1TimeMillis = 0L;
    private long localP2TimeMillis = 0L;
    private boolean localTimersInitialized = false;


    public GameplayPresenter(Main game, FirebaseService firebaseService, String gameId, String opponentId, String opponentDisplayName, String playerColorStr) {
        this.game = game;
        this.firebaseService = firebaseService;
        this.gameId = gameId;
        this.opponentId = opponentId;
        this.opponentDisplayName = opponentDisplayName;
        this.playerColorString = playerColorStr;
        this.playerTeam = "white".equalsIgnoreCase(playerColorStr) ? Team.WHITE : Team.BLACK;
        this.opponentTeam = (this.playerTeam == Team.WHITE) ? Team.BLACK : Team.WHITE;
        this.currentPlayerId = firebaseService.getCurrentUserId();
    }

    public void setView(IGameplayView view) {
        this.view = view;
        if (this.view != null) {
            this.view.setPlayerColor(this.playerColorString);
            initialize();
        }
    }

    public void initialize() {
        Gdx.app.log(TAG, "Initializing Gameplay: " + gameId);
        if (view == null) return;
        // Reset state variables
        gameEnded = false;
        gameResultRecordingAttempted = false;
        isResigning = false; drawOfferedByMe = false; drawOfferedByOpponent = false; localBoardInitialized = false;
        selectedPieceSquare = null; validMovesForSelectedPiece.clear();
        availableSpells.clear(); selectedSpell = null; isTargetingSpell = false;
        justCastNonEndingSpell = false; lastCastedSpellName = null;
        turnStartTimeMillis = 0L;
        isConnected = true;
        isAttemptingReconnect = false;
        localP1TimeMillis = 0L;
        localP2TimeMillis = 0L;
        localTimersInitialized = false;
        stopHeartbeatTimer();

        // Initialize UI
        view.setPlayerInfoText("You (" + playerColorString + ") vs " + opponentDisplayName);
        view.setStatusText("Connecting...", false);
        view.displaySpells(availableSpells);
        view.clearHighlights();
        view.setPlayer1Timer("--:--");
        view.setPlayer2Timer("--:--");
        view.showOpponentDisconnected(false, null);
        view.showConnectionStatus(isConnected, isAttemptingReconnect);

        if (this.gameId == null || this.gameId.trim().isEmpty()) { handleGameError("Invalid Game ID."); return; }

        fetchCurrentUserProfile();
        firebaseService.loadDecks(this);
        // Start listening ONLY AFTER basic setup, including view reference
        if (gameListenerRegistration == null) {
            gameListenerRegistration = firebaseService.listenToGameUpdates(this.gameId, this);
            if (gameListenerRegistration == null) { handleGameError("Failed to connect."); }
        }
    }

    private void fetchCurrentUserProfile() {
        if (!firebaseService.isLoggedIn()) return;
        firebaseService.fetchUserProfile(new FirebaseService.UserProfileListener() {
            @Override public void onSuccess(UserModel u) { currentUserProfile = u; }
            @Override public void onFailure(String m) { Gdx.app.error(TAG, "Profile fetch fail: " + m); }
        });
    }

    // --- Game Update Handling ---

    /** Called every frame to update timers and other real-time elements. */
    public void update(float delta) {
        if (gameEnded || currentGameModel == null || !localTimersInitialized) {
            return;
        }

        // Decrement local timer based on whose turn it is
        if (currentGameModel.getStatusEnum() == GameStatus.ACTIVE && currentGameModel.getCurrentTurnPlayerId() != null) {
            long decrementAmount = (long) (delta * 1000f);
            if (currentGameModel.getCurrentTurnPlayerId().equals(currentGameModel.getPlayer1Id())) {
                localP1TimeMillis -= decrementAmount;
            } else if (currentGameModel.getCurrentTurnPlayerId().equals(currentGameModel.getPlayer2Id())) {
                localP2TimeMillis -= decrementAmount;
            }
            localP1TimeMillis = Math.max(0, localP1TimeMillis);
            localP2TimeMillis = Math.max(0, localP2TimeMillis);
        }

        // Update view labels
        if (view != null) {
            view.setPlayer1Timer(formatTimeMillis(localP1TimeMillis));
            view.setPlayer2Timer(formatTimeMillis(localP2TimeMillis));
        }

        // Client-side timeout detection (triggers callRecordGameResult but NOT handleGameEnd)
        if (currentGameModel.getStatusEnum() == GameStatus.ACTIVE && !gameEnded) {
            String currentTurnPlayer = currentGameModel.getCurrentTurnPlayerId();
            String winner = null;
            String loser = null;
            if (currentTurnPlayer != null && currentTurnPlayer.equals(currentGameModel.getPlayer1Id()) && localP1TimeMillis <= 0) {
                loser = currentGameModel.getPlayer1Id(); winner = currentGameModel.getPlayer2Id();
            } else if (currentTurnPlayer != null && currentTurnPlayer.equals(currentGameModel.getPlayer2Id()) && localP2TimeMillis <= 0) {
                loser = currentGameModel.getPlayer2Id(); winner = currentGameModel.getPlayer1Id();
            }
            if (loser != null && winner != null) {
                Gdx.app.log(TAG, "TIMEOUT detected locally for player " + loser);
                callRecordGameResult(winner, loser, FirebaseService.WIN_REASON_TIMEOUT);
            }
        }
    }

    @Override
    public void onGameStateUpdate(GameModel newGameModel) {
        if (gameEnded) {
            Gdx.app.debug(TAG, "onGameStateUpdate ignored, gameEnded flag is true.");
            return;
        }

        if (view == null) return;
        Gdx.app.debug(TAG, "State Update Received. Status: " + newGameModel.getStatusEnum() + ", Turn: " + newGameModel.getCurrentTurnPlayerId());

        if (isAttemptingReconnect) {
            Gdx.app.log(TAG, "Connection re-established.");
            isConnected = true; isAttemptingReconnect = false;
            if(view != null) view.showConnectionStatus(isConnected, isAttemptingReconnect);
        }

        GameStatus receivedStatus = newGameModel.getStatusEnum();
        if (receivedStatus == GameStatus.FINISHED) {
            Gdx.app.debug(TAG, "Received FINISHED status update via listener.");
            // Let onGameEnded handle the logic IF the listener wasn't stopped first by handleGameEnd
            return;
        }

        // --- Turn Change Detection and Effect Clearing ---
        String previousTurnPlayerId = (currentGameModel != null) ? currentGameModel.getCurrentTurnPlayerId() : null;
        String newTurnPlayerId = newGameModel.getCurrentTurnPlayerId();

        boolean turnChanged = previousTurnPlayerId != null && newTurnPlayerId != null && !previousTurnPlayerId.equals(newTurnPlayerId);

        if (turnChanged) {
            // Clear the turn effects for the player whose turn just ended
            if (currentGameModel != null) {
                currentGameModel.clearTurnEffects(previousTurnPlayerId);
                Gdx.app.log(TAG, "Turn changed from " + previousTurnPlayerId + " to " + newTurnPlayerId + ". Cleared turn effects for " + previousTurnPlayerId);
            } else {
                Gdx.app.error(TAG, "Turn changed, but previous currentGameModel was null. Cannot clear effects.");
            }
            // Clear spell tracking flags on turn change
            Gdx.app.log(TAG, "Turn changed, clearing spell tracking flags.");
            justCastNonEndingSpell = false;
            lastCastedSpellName = null;
        }

        // Store previous state of spell tracking flags before updating the model
        boolean wasTrackingSpell = justCastNonEndingSpell;
        String trackedSpellName = lastCastedSpellName;

        // Update the local model *after* potentially clearing effects based on the *previous* model's turn
        Gdx.app.log(TAG, "Updating local currentGameModel instance.");
        this.currentGameModel = newGameModel;
        this.currentGameModel.clearTemporaryPieceFlags();

        // Sync local timers with the authoritative state from the server
        localP1TimeMillis = this.currentGameModel.getPlayer1TimeRemainingMillis();
        localP2TimeMillis = this.currentGameModel.getPlayer2TimeRemainingMillis();
        localTimersInitialized = true;
        Gdx.app.debug(TAG, "Synced local timers: P1=" + localP1TimeMillis + ", P2=" + localP2TimeMillis);

        if (!turnChanged && wasTrackingSpell && trackedSpellName != null && Objects.equals(this.currentGameModel.getCurrentTurnPlayerId(), currentPlayerId)) {
            Gdx.app.log(TAG, "Re-applying effects for non-turn-ending spell: " + trackedSpellName);
            SpellConfig config = SpellFactory.getConfig(trackedSpellName);
            if (config != null && config.effectComponents != null) {
                for (SpellConfig.SpellEffectComponentConfig compConfig : config.effectComponents) {
                    // Check specifically for components that apply temporary board effects
                    if ("APPLY_BOARD_EFFECT".equals(compConfig.type.toUpperCase())) {
                        if (compConfig.params != null) {
                            String effectName = compConfig.params.get("effect_name");
                            String duration = compConfig.params.getOrDefault("duration", "permanent").toLowerCase();
                            // Re-apply only effects with "current_turn" duration
                            if ("current_turn".equals(duration) && effectName != null) {
                                this.currentGameModel.addTurnEffect(currentPlayerId, effectName);
                                Gdx.app.log(TAG, "Re-applied turn effect: " + effectName + " to updated GameModel instance.");
                            }
                        }
                    }
                }
            } else {
                Gdx.app.log(TAG, "Could not find config or components for spell " + trackedSpellName + " during re-application.");
            }
            // Reset the flags *after* attempting re-application
            justCastNonEndingSpell = false;
            lastCastedSpellName = null;
            Gdx.app.log(TAG, "Reset spell tracking flags AFTER re-application attempt.");
        } else {
            // If conditions not met (e.g., turn did change, or wasn't tracking), ensure flags are reset
            if (justCastNonEndingSpell || lastCastedSpellName != null) {
                Gdx.app.log(TAG, "Conditions for re-applying effects not met. Resetting spell tracking flags.");
                justCastNonEndingSpell = false;
                lastCastedSpellName = null;
            }
        }


        // Continue with other checks and UI updates using the new model
        handleTimeoutCheck(this.currentGameModel);
        if (gameEnded) return;

        checkAbandonment(this.currentGameModel);
        if (gameEnded) return;

        // --- UI Updates ---
        updateUIDisplayInfo(this.currentGameModel);
        updateViewForDrawStatus();
        updateAvailableSpells(this.currentGameModel.getSpellsForPlayer(currentPlayerId));
        if (view != null) view.displaySpells(availableSpells);
        view.displayBoard(castBoardState(this.currentGameModel.getBoardState()));

        // --- Turn Logic ---
        boolean wasMyTurn = turnStartTimeMillis > 0;
        boolean isNowMyTurn = isPlayersTurn();
        if (isNowMyTurn) {
            startHeartbeatTimer();
            if (!wasMyTurn) { turnStartTimeMillis = System.currentTimeMillis(); }
        } else {
            stopHeartbeatTimer(); clearSelection(); turnStartTimeMillis = 0L;
        }

        // --- Endgame Checks (Local detection, only calls recordGameResult) ---
        boolean boardSeemsInitialized = currentGameModel.findKingPosition(Team.WHITE) != null && currentGameModel.findKingPosition(Team.BLACK) != null;
        if (receivedStatus == GameStatus.ACTIVE && boardSeemsInitialized) {
            performEndgameChecks(this.currentGameModel);
        } else {
            Gdx.app.log(TAG, "Skipping endgame checks (Board not ready or game not active). Status: " + receivedStatus);
            if (view != null) view.setStatusText("Waiting for board setup...", false);
            if (playerDeckModel != null && !localBoardInitialized) { attemptInitialBoardSetup(); }
        }
    }

    private void performEndgameChecks(GameModel gameModel) {
        if (gameEnded) return;
        String playerWhoseTurnItIs = gameModel.getCurrentTurnPlayerId();
        Team currentTurnTeam = gameModel.getPlayerTeamById(playerWhoseTurnItIs);

        if (currentTurnTeam != null) {
            boolean inCheck = gameModel.isKingInCheck(currentTurnTeam);
            boolean hasLegalPieceMoves = gameModel.hasLegalMoves(currentTurnTeam);
            if (!hasLegalPieceMoves) {
                String reason; String winner = null, loser = null;
                if (inCheck) { reason = FirebaseService.WIN_REASON_CHECKMATE; loser = playerWhoseTurnItIs; winner = gameModel.getOpponentId(loser); Gdx.app.log(TAG, "CHECKMATE detected against " + currentTurnTeam); }
                else { reason = FirebaseService.WIN_REASON_STALEMATE; Gdx.app.log(TAG, "STALEMATE detected for " + currentTurnTeam); }
                if (winner == null && reason.equals(FirebaseService.WIN_REASON_CHECKMATE)) { Gdx.app.error(TAG, "Checkmate error: Opponent ID is null!"); reason = "error_checkmate"; }
                callRecordGameResult(winner, loser, reason); return;
            }
            if (gameModel.getFiftyMoveRuleCounter() >= 100) { Gdx.app.log(TAG, "DRAW by 50-move rule."); callRecordGameResult(null, null, FirebaseService.DRAW_REASON_50_MOVE); return; }
            String currentStateString = gameModel.getBoardStateString(); if (currentStateString != null) { List<String> history = gameModel.getPositionHistory(); int reps = 0; if (history != null) { for(String s : history) if(currentStateString.equals(s)) reps++; } if (reps >= 2) { Gdx.app.log(TAG, "DRAW by threefold repetition detected"); callRecordGameResult(null, null, FirebaseService.DRAW_REASON_REPETITION); return; } }
            if (gameModel.isInsufficientMaterial()) { Gdx.app.log(TAG, "DRAW by insufficient material."); callRecordGameResult(null, null, FirebaseService.DRAW_REASON_MATERIAL); return; }
            if (view != null && !drawOfferedByMe && !drawOfferedByOpponent && !isTargetingSpell) { String status; if (isPlayersTurn()) { status = inCheck ? "Your turn (Check!)" : "Your turn"; } else { String oppName = opponentDisplayName != null ? opponentDisplayName : "Opponent"; status = inCheck ? oppName + "'s turn (Check!)" : oppName + "'s turn"; } view.setStatusText(status, false); }
            else if (isTargetingSpell && view != null && selectedSpell != null) { view.setStatusText("Select target for " + selectedSpell.getDisplayName() + "...", false); }
        } else { Gdx.app.error(TAG, "Cannot determine team for current player: " + playerWhoseTurnItIs); if(view != null) view.setStatusText("Error: Unknown turn", true); }
    }

    private void handleTimeoutCheck(GameModel gameModel) {
        if (gameModel == null || gameEnded) return;
        if (gameModel.getStatusEnum() == GameStatus.ACTIVE) {
            long p1Time = gameModel.getPlayer1TimeRemainingMillis();
            long p2Time = gameModel.getPlayer2TimeRemainingMillis();
            String currentTurnPlayer = gameModel.getCurrentTurnPlayerId();
            String winner = null;
            String loser = null;
            if (currentTurnPlayer != null && currentTurnPlayer.equals(gameModel.getPlayer1Id()) && p1Time <= 0) { loser = gameModel.getPlayer1Id(); winner = gameModel.getPlayer2Id(); }
            else if (currentTurnPlayer != null && currentTurnPlayer.equals(gameModel.getPlayer2Id()) && p2Time <= 0) { loser = gameModel.getPlayer2Id(); winner = gameModel.getPlayer1Id(); }
            if (loser != null && winner != null) { Gdx.app.log(TAG, "TIMEOUT confirmed by server data for player " + loser); if (!gameEnded) { callRecordGameResult(winner, loser, FirebaseService.WIN_REASON_TIMEOUT); } }
        }
    }
    private String formatTimeMillis(long millis) {
        if (millis < 0) millis = 0; if (millis > TimeUnit.DAYS.toMillis(1)) return "Unlimited";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
    private void checkAbandonment(GameModel gameModel) {
        if (gameModel == null || gameModel.getStatusEnum() != GameStatus.ACTIVE || gameEnded) { return; }
        Date opponentLastSeen = null; if (opponentId != null && opponentId.equals(gameModel.getPlayer1Id())) opponentLastSeen = gameModel.getPlayer1LastSeen(); else if (opponentId != null && opponentId.equals(gameModel.getPlayer2Id())) opponentLastSeen = gameModel.getPlayer2LastSeen();
        boolean opponentDisconnected = false; String timeUntilTimeoutStr = null;
        if (opponentLastSeen != null) { long now = System.currentTimeMillis(); long opponentTimeSinceSeen = now - opponentLastSeen.getTime(); if (opponentTimeSinceSeen > ABANDONMENT_TIMEOUT_MILLIS) { Gdx.app.log(TAG, "Opponent timed out. Claiming victory."); if (!gameEnded) callRecordGameResult(currentPlayerId, opponentId, FirebaseService.WIN_REASON_ABANDONMENT); if(view != null) view.showOpponentDisconnected(true, "Abandoned"); return; } else if (opponentTimeSinceSeen > HEARTBEAT_INTERVAL_SECONDS * 1500) { opponentDisconnected = true; long remainingMillis = ABANDONMENT_TIMEOUT_MILLIS - opponentTimeSinceSeen; timeUntilTimeoutStr = formatTimeMillis(remainingMillis); } else opponentDisconnected = false; }
        else { Gdx.app.debug(TAG, "Cannot check abandonment, opponentLastSeen is null."); opponentDisconnected = false; }
        if (view != null) view.showOpponentDisconnected(opponentDisconnected, timeUntilTimeoutStr);
    }
    private void startHeartbeatTimer() {
        if (heartbeatTask != null && heartbeatTask.isScheduled()) { return; }
        Gdx.app.log(TAG, "Starting heartbeat timer.");
        heartbeatTask = Timer.schedule(new Timer.Task() { @Override public void run() { if (!gameEnded && isPlayersTurn() && firebaseService != null && gameId != null && currentPlayerId != null) { firebaseService.updateLastSeen(gameId, currentPlayerId, new FirebaseService.AuthListener() { @Override public void onSuccess() { if (!isConnected || isAttemptingReconnect) { Gdx.app.log(TAG, "Heartbeat ACK - Connection confirmed/re-established."); isConnected = true; isAttemptingReconnect = false; if(view != null) Gdx.app.postRunnable(() -> view.showConnectionStatus(isConnected, isAttemptingReconnect)); } } @Override public void onFailure(String m) { Gdx.app.error(TAG,"Heartbeat NACK: "+m); if (isConnected) { Gdx.app.log(TAG, "Heartbeat failed - Connection possibly lost."); isConnected = false; isAttemptingReconnect = true; if(view != null) Gdx.app.postRunnable(() -> view.showConnectionStatus(isConnected, isAttemptingReconnect)); } } }); } else { stopHeartbeatTimer(); } } }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS);
    }
    private void stopHeartbeatTimer() {
        if (heartbeatTask != null) { if (heartbeatTask.isScheduled()) { Gdx.app.log(TAG, "Stopping heartbeat timer."); heartbeatTask.cancel(); } heartbeatTask = null; }
    }

    @Override
    public void onGameEnded(GameModel gameModel) {
        Gdx.app.log(TAG, "onGameEnded callback triggered. Status: " + (gameModel != null ? gameModel.getStatusEnum() : "null"));
        if (gameEnded) {
            Gdx.app.debug(TAG, "onGameEnded callback ignored, gameEnded flag already true.");
            return;
        }
        // This is now the main entry point for handling game end
        if (gameModel != null && gameModel.getStatusEnum() == GameStatus.FINISHED) {
            Gdx.app.log(TAG, "onGameEnded: status is FINISHED. Calling handleGameEnd (triggered by listener).");
            handleGameEnd(gameModel);
        } else {
            Gdx.app.error(TAG, "onGameEnded called but status is not FINISHED or model is null. Status: " + (gameModel != null ? gameModel.getStatusEnum() : "null"));
            if (gameModel != null && gameModel.getStatusEnum() == GameStatus.ERROR) {
                handleGameError("Game ended with error status from listener.");
            }
        }
    }

    @Override public void onError(String msg) { if (gameEnded || view == null) return; Gdx.app.error(TAG, "Listener Error: " + msg); isConnected = false; isAttemptingReconnect = true; if(view != null) view.showConnectionStatus(isConnected, isAttemptingReconnect); handleGameError("Connection error: " + msg); }
    @Override public void onSuccess(List<DeckModel> decks) { Gdx.app.log(TAG, "Decks loaded."); this.playerDecks = decks; if (currentGameModel != null) { String myDeckName = currentPlayerId.equals(currentGameModel.getPlayer1Id()) ? currentGameModel.getPlayer1DeckName() : currentGameModel.getPlayer2DeckName(); this.playerDeckModel = findDeckByName(myDeckName); updateDeckInfoLabel(myDeckName); if (playerDeckModel == null) { Gdx.app.error(TAG,"Could not find player deck model: " + myDeckName); handleGameError("Cannot find your deck: " + myDeckName); return; } if (!gameEnded) attemptInitialBoardSetup(); } else { Gdx.app.log(TAG,"Decks loaded, but currentGameModel is null."); } }
    @Override public void onFailure(String msg) { Gdx.app.error(TAG, "Deck load fail: " + msg); if (view != null) view.setPlayerDeckText("Deck Error"); handleGameError("Failed to load deck: " + msg); }
    private void attemptInitialBoardSetup() { if (gameEnded || currentGameModel == null || playerDeckModel == null || localBoardInitialized) return; Map<String, Object> currentBoardState = currentGameModel.getBoardState(); List<String> currentSpells = currentGameModel.getSpellsForPlayer(currentPlayerId); boolean playerPiecesExist = false; if (currentBoardState != null) { String expectedPrefix = playerColorString.toUpperCase() + "_"; for (Object val : currentBoardState.values()) { if (val instanceof String && ((String) val).startsWith(expectedPrefix)) { playerPiecesExist = true; break; } } } if (!playerPiecesExist || currentSpells == null || currentSpells.isEmpty()) { Gdx.app.log(TAG, "Attempting initial board/spell setup TX for player " + currentPlayerId); localBoardInitialized = true; firebaseService.initializePlayerBoardStateTransactionally( gameId, currentPlayerId, playerColorString, playerDeckModel, new FirebaseService.AuthListener() { @Override public void onSuccess() {} @Override public void onFailure(String msg) { Gdx.app.error(TAG, "Board/Spell initialization TX NACK: " + msg); handleGameError("Board setup failed: " + msg); } }); } else { Gdx.app.log(TAG, "Board/spells already initialized for " + currentPlayerId); localBoardInitialized = true; } }
    public void handleBoardClick(String square) { if (gameEnded || currentGameModel == null || view == null || !isPlayersTurn()) { clearSelection(); return; } BoardPosition clickedPos = GameModel.algebraicToBoardPosition(square); if (clickedPos == null) { return; } if (isTargetingSpell && selectedSpell != null) { Set<BoardPosition> validTargets = selectedSpell.getValidTargets(currentPlayerId, currentGameModel); List<String> validTargetStrings = validTargets.stream().map(GameModel::boardPositionToAlgebraic).filter(Objects::nonNull).collect(Collectors.toList()); if (validTargetStrings.contains(square)) { castSpellAndApplyLocally(selectedSpell, clickedPos); clearSelection(); } else { clearSelection(); } return; } Piece clickedPiece = currentGameModel.getPieceAt(clickedPos); if (selectedPieceSquare == null) { handleSquareSelection(square, clickedPos, clickedPiece); } else { if (selectedPieceSquare.equals(square)) { clearSelection(); } else if (validMovesForSelectedPiece.contains(square)) { handleMoveAttempt(square, clickedPos); } else if (clickedPiece != null && clickedPiece.getTeam() == playerTeam) { handleSquareSelection(square, clickedPos, clickedPiece); } else { clearSelection(); } } }
    public void handleSpellClick(Spell spell) { if (gameEnded || currentGameModel == null || view == null || !isPlayersTurn() || isTargetingSpell) return; if (spell == null) return; boolean hasSpell = availableSpells.stream().anyMatch(s -> s.getTypeName().equals(spell.getTypeName())); if (!hasSpell) { if(view != null) view.setStatusText("Spell not available!", true); return; } if (currentGameModel.isKingInCheck(playerTeam)) { if(view != null) view.setStatusText("Cannot cast spells while in check!", true); clearSelection(); return; } clearSelection(); selectedSpell = spell; Gdx.app.log(TAG, "Spell selected: " + spell.getDisplayName()); boolean requiresTarget = spell.requiresTarget(); if (requiresTarget) { isTargetingSpell = true; if (view != null) { view.setStatusText("Select target for " + spell.getDisplayName() + "...", false); Set<BoardPosition> potentialTargets = spell.getValidTargets(currentPlayerId, currentGameModel); List<String> targetStrings = potentialTargets.stream().map(GameModel::boardPositionToAlgebraic).filter(Objects::nonNull).collect(Collectors.toList()); view.highlightSpellTargets(targetStrings); } } else { castSpellAndApplyLocally(spell, null); clearSelection(); } }

    // Modified castSpellAndApplyLocally
    private void castSpellAndApplyLocally(Spell spell, BoardPosition targetPos) {
        if (spell == null || currentGameModel == null || view == null || !isPlayersTurn() || gameEnded) return;
        long timeNow = System.currentTimeMillis();
        long timeTakenMillis = (turnStartTimeMillis > 0) ? (timeNow - turnStartTimeMillis) : 0L;

        // --- Apply Effect Logically ---
        boolean effectAppliedLocally = spell.applyEffect(currentPlayerId, targetPos, currentGameModel);
        Gdx.app.log(TAG, "Local spell applyEffect result: " + effectAppliedLocally + " for " + spell.getTypeName());


        if (effectAppliedLocally) {
            long currentRemainingTime = (currentPlayerId.equals(currentGameModel.getPlayer1Id())) ? localP1TimeMillis : localP2TimeMillis;
            long newRemainingTime = Math.max(0, currentRemainingTime - timeTakenMillis);
            view.displayBoard(castBoardState(currentGameModel.getBoardState()));
            view.clearHighlights();
            view.setStatusText("Casting " + spell.getDisplayName() + "...", false);

            // --- Set tracking flags if spell doesn't end turn ---
            if (!spell.endsTurn()) {
                justCastNonEndingSpell = true;
                lastCastedSpellName = spell.getTypeName();
                Gdx.app.log(TAG, "Tracking non-turn-ending spell cast: " + lastCastedSpellName + ". Flags set: justCast=" + justCastNonEndingSpell + ", name=" + lastCastedSpellName);
            } else {
                justCastNonEndingSpell = false; // Ensure flags are false if turn ends
                lastCastedSpellName = null;
                Gdx.app.log(TAG, "Spell ends turn, not tracking: " + spell.getTypeName());
            }

            castSpellNetwork(spell, GameModel.boardPositionToAlgebraic(targetPos), newRemainingTime);
            turnStartTimeMillis = 0L;
        } else {
            if (view != null) view.setStatusText("Spell effect failed locally.", true);
            clearSelection();
            // Ensure tracking flags are reset on failure
            Gdx.app.log(TAG, "Resetting spell tracking flags due to local effect failure.");
            justCastNonEndingSpell = false;
            lastCastedSpellName = null;
        }
    }


    private void castSpellNetwork(Spell spell, String targetSquare, long newRemainingTimeMillis) { if (gameEnded || !isPlayersTurn() || spell == null || currentGameModel == null) return; Map<String, String> targetInfo = null; if (targetSquare != null) { targetInfo = new HashMap<>(); targetInfo.put("targetSquare", targetSquare); } long p1TimeToSend, p2TimeToSend; if (currentPlayerId.equals(currentGameModel.getPlayer1Id())) { p1TimeToSend = newRemainingTimeMillis; p2TimeToSend = localP2TimeMillis; } else { p1TimeToSend = localP1TimeMillis; p2TimeToSend = newRemainingTimeMillis; } stopHeartbeatTimer(); firebaseService.performSpellCast(gameId, currentPlayerId, spell.getTypeName(), targetInfo, p1TimeToSend, p2TimeToSend, new FirebaseService.AuthListener() { @Override public void onSuccess() {} @Override public void onFailure(String msg) { Gdx.app.error(TAG, "Spell cast network NACK: " + msg); if (view != null && !gameEnded) { view.setStatusText("Spell cast failed: " + msg, true); }
        Gdx.app.log(TAG, "Resetting spell tracking flags due to network failure.");
        justCastNonEndingSpell = false;
        lastCastedSpellName = null;
        if (!gameEnded && isPlayersTurn()) startHeartbeatTimer(); } }); }
    private void handleSquareSelection(String square, BoardPosition clickedPos, Piece clickedPiece) { clearSelection(); if (clickedPiece != null && clickedPiece.getTeam() == playerTeam && currentGameModel != null) { selectedPieceSquare = square; Set<BoardPosition> validMovePositions = clickedPiece.getValidMoves(currentGameModel); validMovesForSelectedPiece = validMovePositions.stream().map(GameModel::boardPositionToAlgebraic).filter(Objects::nonNull).collect(Collectors.toList()); if (view != null) { view.highlightValidMoves(validMovesForSelectedPiece); String name = clickedPiece.getDisplayName(); view.setStatusText("Selected " + name + (validMovesForSelectedPiece.isEmpty() ? ". No moves." : ". Choose move."), false); } } else { if (view != null && isPlayersTurn()) { view.setStatusText("Your turn", false); } } }
    private void handleMoveAttempt(String targetSquare, BoardPosition targetPos) {
        // --- Pre-conditions check ---
        if (selectedPieceSquare == null || currentGameModel == null || gameEnded || !isPlayersTurn()) {
            Gdx.app.log(TAG, "handleMoveAttempt ignored: Invalid state.");
            clearSelection();
            return;
        }

        final String originalSquare = selectedPieceSquare;
        BoardPosition originalPos = GameModel.algebraicToBoardPosition(originalSquare);
        if (originalPos == null) {
            Gdx.app.error(TAG, "handleMoveAttempt failed: Could not parse original square: " + originalSquare);
            clearSelection();
            return;
        }

        Piece movingPiece = currentGameModel.getPieceAt(originalPos);
        if (movingPiece == null) {
            Gdx.app.error(TAG, "handleMoveAttempt failed: Piece not found at selected square: " + originalSquare);
            clearSelection();
            if (view != null) view.setStatusText("Error: Piece missing", true);
            return;
        }

        // --- Determine Move Details ---
        Piece targetPiece = currentGameModel.getPieceAt(targetPos);
        final boolean isCapture = (targetPiece != null);
        final String pieceTypeName = movingPiece.getTypeName();
        final boolean isPawnMove = "PAWN".equals(pieceTypeName);
        // Pass the BASE piece value (without _MOVED) to performMove
        final String pieceValueString = movingPiece.getTeam().name().toUpperCase() + "_" + pieceTypeName.toUpperCase();
        final String movingPieceName = movingPiece.getDisplayName();

        // Ranged Attack Detection
        boolean isRangedAttack = false;
        if ("ARCHER".equals(pieceTypeName) && isCapture && targetPiece != null && targetPiece.getTeam() != movingPiece.getTeam()) {
            isRangedAttack = true;
            Gdx.app.log(TAG, "Detected Archer Ranged Attack: " + originalSquare + " -> " + targetSquare);
        }


        // --- Calculate Time ---
        long timeNow = System.currentTimeMillis();
        long timeTakenMillis = (turnStartTimeMillis > 0) ? (timeNow - turnStartTimeMillis) : 0L;
        long currentRemainingTime = (currentPlayerId.equals(currentGameModel.getPlayer1Id())) ? localP1TimeMillis : localP2TimeMillis;
        long newRemainingTimeMillis = Math.max(0, currentRemainingTime - timeTakenMillis);

        long p1TimeToSend, p2TimeToSend;
        if (currentPlayerId.equals(currentGameModel.getPlayer1Id())) {
            p1TimeToSend = newRemainingTimeMillis;
            p2TimeToSend = localP2TimeMillis;
        } else {
            p1TimeToSend = localP1TimeMillis;
            p2TimeToSend = newRemainingTimeMillis;
        }

        // --- Update UI and Stop Timers ---
        if (view != null) {
            view.setStatusText("Moving " + movingPieceName + "...", false);
            view.clearHighlights();
        }
        stopHeartbeatTimer();

        // --- Call Firebase Presenter ---
        firebaseService.performMove(
                gameId,
                originalSquare,
                targetSquare,
                pieceValueString,
                opponentId,
                isCapture,
                isPawnMove,
                isRangedAttack,
                p1TimeToSend,
                p2TimeToSend,
                new FirebaseService.AuthListener() {
                    @Override
                    public void onSuccess() {
                        turnStartTimeMillis = 0L;
                        clearSelection();
                        Gdx.app.log(TAG, "performMove ACK received.");
                    }

                    @Override
                    public void onFailure(String msg) {
                        Gdx.app.error(TAG, "performMove NACK: " + msg);
                        if (view != null && !gameEnded) {
                            view.setStatusText("Move failed: " + msg, true);
                        }
                        clearSelection();
                        if (!gameEnded && isPlayersTurn()) {
                            startHeartbeatTimer();
                        }
                    }
                }
        );

        // Clear selection immediately after sending the move attempt
        clearSelection();
    }
    private void clearSelection() { selectedPieceSquare = null; validMovesForSelectedPiece.clear(); selectedSpell = null; isTargetingSpell = false; if (view != null) { view.clearHighlights(); if (!drawOfferedByMe && !drawOfferedByOpponent && isPlayersTurn() && !gameEnded && currentGameModel != null) { boolean inCheck = currentGameModel.isKingInCheck(playerTeam); view.setStatusText(inCheck ? "Your turn (Check!)" : "Your turn", false); } } }
    private boolean isPlayersTurn() { return currentGameModel != null && !gameEnded && currentGameModel.getStatusEnum() == GameStatus.ACTIVE && currentPlayerId.equals(currentGameModel.getCurrentTurnPlayerId()); }

    public void handleResignButtonClicked() {
        if (gameEnded || isResigning || gameResultRecordingAttempted || gameId == null) {
            Gdx.app.log(TAG, "Resign button ignored: gameEnded=" + gameEnded + ", isResigning=" + isResigning + ", recordingAttempted=" + gameResultRecordingAttempted);
            return;
        }
        isResigning = true;
        gameResultRecordingAttempted = true;
        if (view != null) {
            view.setResignButtonEnabled(false);
            view.setStatusText("Resigning...", false);
        }
        stopHeartbeatTimer();
        turnStartTimeMillis = 0L;
        // The result will be handled by the listener via onGameEnded
        firebaseService.resignGame(gameId, currentPlayerId, this);
    }
    public void handleOfferDrawClicked() { if (gameEnded || drawOfferedByMe || drawOfferedByOpponent || gameId == null || !isPlayersTurn()) return; if (view != null) { view.setOfferDrawButtonEnabled(false); view.setStatusText("Offering draw...", false); } stopHeartbeatTimer(); turnStartTimeMillis = 0L; firebaseService.offerDraw(gameId, currentPlayerId, new FirebaseService.AuthListener() { @Override public void onSuccess() {} @Override public void onFailure(String msg) { Gdx.app.error(TAG,"Draw offer failed: " + msg); if (!gameEnded && view != null) { view.setStatusText("Offer failed: " + msg, true); updateViewForDrawStatus(); } if (!gameEnded && isPlayersTurn()) startHeartbeatTimer(); } }); }
    public void handleAcceptDrawClicked() {
        if (gameEnded || !drawOfferedByOpponent || gameResultRecordingAttempted || gameId == null) {
            Gdx.app.log(TAG, "Accept Draw button ignored: gameEnded=" + gameEnded + ", drawOfferedByOpponent=" + drawOfferedByOpponent + ", recordingAttempted=" + gameResultRecordingAttempted);
            return;
        }
        gameResultRecordingAttempted = true;
        if (view != null) {
            view.showDrawOfferReceived(false);
            view.setStatusText("Accepting draw...", false);
        }
        stopHeartbeatTimer();
        turnStartTimeMillis = 0L;
        // The result will be handled by the listener via onGameEnded
        firebaseService.acceptDraw(gameId, currentPlayerId, this);
    }
    public void handleDeclineDrawClicked() { if (gameEnded || !drawOfferedByOpponent || gameId == null) return; if (view != null) { view.showDrawOfferReceived(false); view.setStatusText("Declining draw...", false); } stopHeartbeatTimer(); firebaseService.declineDraw(gameId, currentPlayerId, new FirebaseService.AuthListener() { @Override public void onSuccess() {} @Override public void onFailure(String msg) { Gdx.app.error(TAG,"Draw decline failed: " + msg); if (!gameEnded && view != null) { view.setStatusText("Decline failed: " + msg, true); updateViewForDrawStatus(); } if (!gameEnded && isPlayersTurn()) startHeartbeatTimer(); } }); }
    @Override public void onGameEndSuccess(int eloChange) { Gdx.app.log(TAG, "GameEnd action (Resign/AcceptDraw) ACK. ELO change: " + eloChange); isResigning = false; /* UI update happens via listener */ }
    @Override public void onGameEndFailure(String msg) {
        // Reset flags on failure
        gameResultRecordingAttempted = false;
        isResigning = false;
        if (gameEnded) return;
        Gdx.app.error(TAG, "GameEnd action failed: " + msg);
        if (view != null) {
            view.setStatusText("Action failed: " + msg, true);
            updateViewForDrawStatus();
        }
        if (!gameEnded && isPlayersTurn()) startHeartbeatTimer();
    }


    private void callRecordGameResult(String winnerId, String loserId, String reason) {
        // Check flags
        if (gameEnded || gameResultRecordingAttempted) {
            Gdx.app.log(TAG, "callRecordGameResult ignored, already ended or recording attempted. Reason: " + reason);
            return;
        }
        gameResultRecordingAttempted = true;

        Gdx.app.log(TAG, "callRecordGameResult invoked. Reason: " + reason + ", W: " + winnerId + ", L: " + loserId);

        // Initiate the Firebase transaction to record the result
        firebaseService.recordGameResult(gameId, winnerId, loserId, reason, new FirebaseService.AuthListener() {
            @Override public void onSuccess() { Gdx.app.log(TAG, "Firebase ACK: recordGameResult (" + reason + ") successful. Listener will handle game end."); }
            @Override public void onFailure(String m) {
                Gdx.app.error(TAG, "Firebase NACK: recordGameResult (" + reason + ") failed: " + m);
                // Reset the flag ONLY if the recording failed for a reason OTHER than already finished
                boolean alreadyFinishedError = m != null && (m.contains("already finished") || m.contains("ABORTED") || m.contains("precondition failed"));
                if (!gameEnded) {
                    if (!alreadyFinishedError) {
                        gameResultRecordingAttempted = false;
                        handleGameError("Failed to record game result: " + m);
                    } else {
                        Gdx.app.log(TAG, "Ignoring expected 'already finished' or 'precondition' error from recordGameResult NACK, assuming listener handled or will handle the game end.");
                        // Keep gameResultRecordingAttempted = true here
                    }
                } else {
                    Gdx.app.log(TAG, "recordGameResult NACK received, but gameEnded flag already set. Ignoring NACK msg: " + m);
                }
            }
        });
    }

    // This method is only called by onGameEnded
    private void handleGameEnd(GameModel endedGameModel) {
        // Check flag at the very beginning
        if (gameEnded) {
            Gdx.app.log(TAG, "handleGameEnd called, but gameEnded flag already true. Ignoring.");
            return;
        }
        // Set flag immediately to prevent re-entry
        gameEnded = true;

        // Stop timers and listeners immediately
        stopHeartbeatTimer();
        stopListening();

        clearSelection();
        turnStartTimeMillis = 0L;

        this.currentGameModel = endedGameModel;

        if (view != null) {
            view.setStatusText("Game Over!", false);
        }

        // Prepare overlay data
        String winnerId = endedGameModel.getWinnerId(); String loserId = endedGameModel.getLoserId(); String rawWinReason = endedGameModel.getWinReason();
        boolean eloDataAvailable = endedGameModel.getEloChangePlayer1() != 0 || endedGameModel.getEloChangePlayer2() != 0 || (rawWinReason != null && rawWinReason.equals(FirebaseService.WIN_REASON_DRAW_AGREEMENT));
        int playerEloChange = endedGameModel.getEloChangeForPlayer(currentPlayerId);
        Gdx.app.log(TAG, "Game End Data: W=" + winnerId + ", L=" + loserId + ", R=" + rawWinReason + ", ELO+/-=" + (eloDataAvailable ? playerEloChange : "N/A (Pending)"));
        boolean playerWon = currentPlayerId != null && currentPlayerId.equals(winnerId); boolean playerLost = currentPlayerId != null && currentPlayerId.equals(loserId); boolean isDraw = winnerId == null && loserId == null; if (!isDraw && rawWinReason != null) { isDraw = FirebaseService.WIN_REASON_STALEMATE.equals(rawWinReason) || FirebaseService.WIN_REASON_DRAW_AGREEMENT.equals(rawWinReason) || FirebaseService.DRAW_REASON_REPETITION.equals(rawWinReason) || FirebaseService.DRAW_REASON_50_MOVE.equals(rawWinReason) || FirebaseService.DRAW_REASON_MATERIAL.equals(rawWinReason); }
        String eloString; if (eloDataAvailable && currentUserProfile != null) { int pElo = currentUserProfile.getEloRating(); int nElo = pElo + playerEloChange; eloString = String.format(Locale.getDefault(), "ELO: %d %+d = %d", pElo, playerEloChange, nElo); } else if (eloDataAvailable) { eloString = String.format(Locale.getDefault(), "ELO Change: %+d", playerEloChange); } else { eloString = "ELO: Pending update"; }
        String reason = "Unknown"; if (rawWinReason != null && !rawWinReason.isEmpty()) { reason = rawWinReason.replace("_", " "); reason = reason.substring(0, 1).toUpperCase() + reason.substring(1); } else { if (playerWon) reason = "Opponent Defeated"; else if (playerLost) reason = "Defeat"; else if (isDraw) reason = "Draw"; }
        String title, message; if (playerWon) { title = "Victory!"; message = "You won by " + reason + "!"; } else if (playerLost) { title = "Defeat"; message = "You lost by " + reason + "."; } else { title = "Draw"; message = "Game drawn by " + reason + "."; }
        Gdx.app.log(TAG, "Overlay Data: T='" + title + "', M='" + message + "', E='" + eloString + "'");
        final String finalTitle = title; final String finalMessage = message; final String finalEloString = eloString;

        Gdx.app.log(TAG, "handleGameEnd: Posting showGameOverOverlay runnable...");
        // Post the UI update to the main thread
        Gdx.app.postRunnable(() -> {
            Gdx.app.log(TAG, "handleGameEnd Runnable: Executing...");
            if (view == null) {
                Gdx.app.log(TAG,"Skipping showGameOverOverlay runnable as view is null.");
                return;
            }
            if (!gameEnded) {
                Gdx.app.log(TAG,"Skipping showGameOverOverlay runnable as gameEnded became false.");
                return;
            }

            Gdx.app.log(TAG, "Executing showGameOverOverlay via postRunnable...");
            view.setResignButtonEnabled(false);
            view.setOfferDrawButtonEnabled(false);
            view.showDrawOfferReceived(false);
            view.showOpponentDisconnected(false, null);
            view.showConnectionStatus(true, false);
            view.showGameOverOverlay(finalTitle, finalMessage, finalEloString);
        });
        Gdx.app.log(TAG, ">>> handleGameEnd EXIT <<<");
    }

    private void handleGameError(String message) {
        // Only proceed if the game hasn't already been flagged as ended by handleGameEnd
        if (gameEnded) {
            Gdx.app.log(TAG, "handleGameError called, but game already ended. Error: " + message);
            return;
        }

        Gdx.app.error(TAG, "Handling game error: " + message);
        // Ensure flags are set when an error occurs
        gameEnded = true;
        gameResultRecordingAttempted = true;

        // Stop listeners and timers immediately
        stopListening();
        stopHeartbeatTimer();
        turnStartTimeMillis = 0L;

        final String finalMessage = (message != null ? message : "An unknown error occurred.") + "\nReturning to menu.";
        Gdx.app.postRunnable(() -> {
            if (view != null) {
                view.showErrorDialog(finalMessage);
            } else {
                Gdx.app.log(TAG,"View null during handleGameError, returning to menu directly.");
                game.showMainMenuScreen();
            }
        });
    }

    // --- Helper Methods ---
    private void updateUIDisplayInfo(GameModel model) {
        if (view == null || model == null || gameEnded) return;
        String oppDeckName = null;
        if (currentPlayerId != null && model.getPlayer1Id() != null && model.getPlayer2Id() != null) {
            oppDeckName = currentPlayerId.equals(model.getPlayer1Id()) ? model.getPlayer2DeckName() : model.getPlayer1DeckName();
        }
        String oppDisplay = opponentDisplayName != null ? opponentDisplayName : "Opponent";
        view.setOpponentInfoText(oppDisplay + (oppDeckName != null ? " ("+oppDeckName+")" : " (...)"));

        if (playerDeckModel != null) {
            view.setPlayerDeckText(playerDeckModel.getName() + " (" + playerDeckModel.getCurrentPoints() + ")");
        } else if (playerDecks != null && currentPlayerId != null) {
            String myDeckName = currentPlayerId.equals(model.getPlayer1Id()) ? model.getPlayer1DeckName() : model.getPlayer2DeckName();
            updateDeckInfoLabel(myDeckName);
        } else {
            if(view!=null) view.setPlayerDeckText("Loading Deck...");
        }
    }

    private void updateViewForDrawStatus() {
        if (view == null || gameEnded) return;
        // Determine draw offer status based on GameModel
        drawOfferedByMe = currentGameModel != null && currentPlayerId != null && currentPlayerId.equals(currentGameModel.getDrawOfferedByPlayerId());
        drawOfferedByOpponent = currentGameModel != null && opponentId != null && opponentId.equals(currentGameModel.getDrawOfferedByPlayerId());

        boolean canOffer = !gameEnded && !gameResultRecordingAttempted && isPlayersTurn() && !drawOfferedByMe && !drawOfferedByOpponent;
        boolean canResign = !gameEnded && !gameResultRecordingAttempted;
        view.setOfferDrawButtonEnabled(canOffer);
        view.setResignButtonEnabled(canResign);
        view.showDrawOfferReceived(drawOfferedByOpponent);
        // Update status text only if not targeting a spell AND game isn't ended
        if (!isTargetingSpell && !gameEnded) {
            if (drawOfferedByMe) {
                view.setStatusText("Draw offer sent.", false);
            } else if (drawOfferedByOpponent) {
                String oppName = opponentDisplayName != null ? opponentDisplayName : "Opponent";
                view.setStatusText(oppName + " offered draw.", false);
            }
            // If no draw offer, the status text is handled elsewhere
        }
    }

    private Map<String, String> castBoardState(Map<String, Object> objMap) {
        if (objMap == null) return null; Map<String, String> strMap = new HashMap<>();
        for (Map.Entry<String, Object> e: objMap.entrySet()) { if (e.getValue() instanceof String) { strMap.put(e.getKey(), (String) e.getValue()); } else if (e.getValue() != null) { Gdx.app.error(TAG, "Unexpected type in boardState map cast: " + e.getValue().getClass().getName()); } } return strMap;
    }
    private DeckModel findDeckByName(String name) {
        if (name == null || playerDecks == null) return null;
        return playerDecks.stream().filter(d -> name.equals(d.getName())).findFirst().orElse(null);
    }
    private void updateDeckInfoLabel(String deckNameToFind) {
        if (deckNameToFind == null) { if (view != null) view.setPlayerDeckText("Deck: ---"); return; }
        if (this.playerDeckModel == null) { this.playerDeckModel = findDeckByName(deckNameToFind); }
        if (view != null) { if (playerDeckModel != null) { view.setPlayerDeckText(playerDeckModel.getName() + " (" + playerDeckModel.getCurrentPoints() + ")"); } else { view.setPlayerDeckText("Deck: " + deckNameToFind + " (Not Found?)"); Gdx.app.error(TAG,"Could not find deck model: " + deckNameToFind); } }
    }
    private void updateAvailableSpells(List<String> spellNamesFromModel) {
        availableSpells.clear(); if(spellNamesFromModel != null) { for (String name : spellNamesFromModel) { if (name == null || name.trim().isEmpty()) continue; Spell proto = SpellFactory.getPrototype(name); if (proto != null) { availableSpells.add(proto.copy()); } else { Gdx.app.error(TAG, "Could not find spell prototype for: '" + name + "'"); } } } availableSpells.sort(Comparator.comparingInt(Spell::getPointCost).thenComparing(Spell::getTypeName));
    }
    public void handleGameOverClosed() { handleReturnToMenu(); }
    public void handleReturnToMenu() { Gdx.app.log(TAG, "Returning to main menu."); stopListening(); stopHeartbeatTimer(); Gdx.app.postRunnable(game::showMainMenuScreen); }
    private void stopListening() { if (gameListenerRegistration != null) { firebaseService.stopListeningToGame(gameListenerRegistration); gameListenerRegistration = null; Gdx.app.log(TAG, "Stopped listening to game updates."); } }
    public void dispose() { Gdx.app.log(TAG, "Disposing GameplayPresenter."); stopListening(); stopHeartbeatTimer(); this.gameEnded = true; this.view = null; this.currentGameModel = null; this.playerDecks = null; this.playerDeckModel = null; this.currentUserProfile = null; this.selectedPieceSquare = null; this.validMovesForSelectedPiece.clear(); this.availableSpells.clear(); this.selectedSpell = null; }
    // Added getter for GameplayScreen check
    public boolean isGameEnded() { return gameEnded; }
    // Added for GameplayScreen.show()
    public GameModel getCurrentGameModel() { return currentGameModel; }
    public UserModel getCurrentUserProfile() { return currentUserProfile; }
    public String getCurrentPlayerId() { return currentPlayerId; }

    public void refreshView() {
        if (view == null) return;
        Gdx.app.log(TAG, "Refreshing View...");
        view.showConnectionStatus(isConnected, isAttemptingReconnect);
        if (currentGameModel != null) {
            GameStatus currentStatus = currentGameModel.getStatusEnum();
            updateUIDisplayInfo(currentGameModel);
            view.displayBoard(castBoardState(currentGameModel.getBoardState()));
            updateAvailableSpells(currentGameModel.getSpellsForPlayer(currentPlayerId));
            view.displaySpells(availableSpells);
            updateViewForDrawStatus();
            view.clearHighlights();
            view.setPlayer1Timer(formatTimeMillis(localP1TimeMillis));
            view.setPlayer2Timer(formatTimeMillis(localP2TimeMillis));
            checkAbandonment(currentGameModel);
            if (selectedPieceSquare != null && !validMovesForSelectedPiece.isEmpty()) {
                Piece p = currentGameModel.getPieceAt(GameModel.algebraicToBoardPosition(selectedPieceSquare));
                String name = (p != null) ? p.getDisplayName() : "Piece";
                view.highlightValidMoves(validMovesForSelectedPiece);
                view.setStatusText("Selected " + name + (validMovesForSelectedPiece.isEmpty() ? ". No moves." : ". Choose move."), false);
            } else if (isTargetingSpell && selectedSpell != null) {
                Set<BoardPosition> targets = selectedSpell.getValidTargets(currentPlayerId, currentGameModel);
                List<String> targetStrings = targets.stream().map(GameModel::boardPositionToAlgebraic).filter(Objects::nonNull).collect(Collectors.toList());
                view.highlightSpellTargets(targetStrings);
                view.setStatusText("Select target for " + selectedSpell.getDisplayName() + "...", false);
            } else if (currentStatus == GameStatus.ACTIVE) {
                boolean inCheck = false;
                Team currentTeam = currentGameModel.getPlayerTeamById(currentGameModel.getCurrentTurnPlayerId());
                if(currentTeam != null) {
                    inCheck = currentGameModel.isKingInCheck(currentTeam);
                } else {
                    Gdx.app.error(TAG, "refreshView: Could not determine team for current player ID: " + currentGameModel.getCurrentTurnPlayerId());
                }
                String statusText;
                if (isPlayersTurn()) {
                    statusText = inCheck ? "Your turn (Check!)" : "Your turn";
                } else {
                    String oppName = opponentDisplayName != null ? opponentDisplayName : "Opponent";
                    statusText = inCheck ? oppName + "'s turn (Check!)" : oppName + "'s turn";
                }
                view.setStatusText(statusText, false);
            } else if (currentStatus == GameStatus.PENDING_JOIN || currentStatus == GameStatus.PENDING_CODE_JOIN) {
                view.setStatusText("Waiting for Opponent...", false);
            } else if (currentStatus == GameStatus.FINISHED || currentStatus == GameStatus.ERROR) {
                // If the listener gets a finished/error update *before* handleGameEnd sets the flag, we should still trigger handleGameEnd.
                if (!gameEnded) {
                    Gdx.app.log(TAG, "RefreshView found finished/error status from listener, calling onGameEnded.");
                    onGameEnded(currentGameModel);
                }
            } else {
                view.setStatusText(currentStatus.name(), false);
            }
        } else {
            view.setStatusText("Waiting for game data...", false);
            view.displayBoard(new HashMap<>());
            view.displaySpells(new ArrayList<>());
            view.clearHighlights();
            view.setPlayer1Timer("--:--");
            view.setPlayer2Timer("--:--");
            view.showOpponentDisconnected(false, null);
            localTimersInitialized = false;
        }
    }

}