package io.WizardsChessMaster.service;

import androidx.annotation.Nullable;

import io.WizardsChessMaster.model.DeckModel;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.MatchHistoryEntry;
import io.WizardsChessMaster.model.UserModel;

import java.util.List;
import java.util.Map;

/**
 * Interface defining the contract for interacting with the Firebase backend.
 * This abstracts the specific implementation (e.g., Android) from the core logic.
 */
public interface FirebaseService {

    // --- Win Reason Constants ---
    String WIN_REASON_CHECKMATE = "checkmate";
    String WIN_REASON_STALEMATE = "stalemate";
    String WIN_REASON_RESIGNATION = "resignation";
    String WIN_REASON_DRAW_AGREEMENT = "draw_agreement";
    String DRAW_REASON_REPETITION = "draw_repetition";
    String DRAW_REASON_50_MOVE = "draw_50_move";
    String DRAW_REASON_MATERIAL = "draw_material";
    String WIN_REASON_TIMEOUT = "timeout";
    String WIN_REASON_ABANDONMENT = "abandonment";
    String GAME_STATUS_PENDING_CODE_JOIN = "pending_code_join";


    // --- Listener Interfaces ---

    /** Callback for asynchronous authentication operations. */
    interface AuthListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /** Callback for fetching user profile data. */
    interface UserProfileListener {
        void onSuccess(UserModel userModel);
        void onFailure(String errorMessage);
    }

    /** Callback for saving deck data. */
    interface DeckSaveListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /** Callback for loading multiple decks. */
    interface DeckLoadListener {
        void onSuccess(List<DeckModel> decks);
        void onFailure(String errorMessage);
    }

    /** Callback for deleting a deck. */
    interface DeckDeleteListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /** Callback for ranked matchmaking finding a game. */
    interface GameFoundListener {
        void onGameFound(GameModel gameModel);
        void onError(String errorMessage);
    }

    /** Callback for hosting a custom game. */
    interface HostedGameListener {
        void onWaitingForPlayer(String gameId, String joinCode);
        void onPlayerJoined(GameModel gameModel);
        void onHostingCancelled();
        void onError(String errorMessage);
    }

    /** Callback for joining a custom game by code. */
    interface JoinGameListener {
        void onJoinSuccess(GameModel gameModel);
        void onJoinFailure(String errorMessage);
        void onPointLimitMismatch(int requiredLimit);
    }

    /** Listener for real-time game state updates during gameplay. */
    interface GameEventListener {
        void onGameStateUpdate(GameModel gameModel);
        void onGameEnded(GameModel gameModel);
        void onError(String errorMessage);
    }

    /** Callback for actions that end the game (resign, accept draw, timeout etc.). */
    interface GameEndListener {
        void onGameEndSuccess(int eloChange);
        void onGameEndFailure(String errorMessage);
    }

    /** Callback for checking if a user has an active game. */
    interface ActiveGameCheckListener {
        void onActiveGameFound(GameModel gameModel);
        void onNoActiveGameFound();
        void onError(String errorMessage);
    }

    /** Callback for fetching match history data. */
    interface MatchHistoryListener {
        void onSuccess(List<MatchHistoryEntry> history);
        void onFailure(String errorMessage);
    }

    // --- Methods ---

    // Authentication
    void signInWithGoogle(AuthListener listener);
    void signOut(AuthListener listener);
    boolean isLoggedIn();
    String getCurrentUserId();
    String getCurrentUserDisplayName();

    // User Profile
    void fetchUserProfile(UserProfileListener listener);
    void createUserProfileIfNotExists(String userId, String displayName, AuthListener listener);

    // Deck Management
    void saveDeck(DeckModel deck, DeckSaveListener listener);
    void loadDecks(DeckLoadListener listener);
    void deleteDeck(String deckName, DeckDeleteListener listener);

    // Matchmaking (Ranked)
    void enterRankedMatchmaking(String userId, String displayName, int elo, int pointLimit, String timeLimit, String deckName, AuthListener queueListener);
    void cancelRankedMatchmaking(String userId, AuthListener listener);
    Object listenForMyRankedGame(String userId, GameFoundListener listener);
    void stopListeningForMyRankedGame(Object listenerRegistration);

    // Matchmaking (Host & Join Custom Game)
    Object hostGame(String hostUserId, String hostDisplayName, int pointLimit, String timeLimit, String hostDeckName, HostedGameListener listener);
    void cancelHostedGame(String gameId, @Nullable String hostUserId, AuthListener listener);
    void stopHostingGameListener(Object listenerRegistration);
    void joinGameByCode(String joinerUserId, String joinerDisplayName, DeckModel joinerDeckModel, String joinCode, JoinGameListener listener);

    // General Game Status
    void checkForActiveGame(String userId, ActiveGameCheckListener listener);

    // Gameplay
    Object listenToGameUpdates(String gameId, GameEventListener listener);
    void stopListeningToGame(Object listenerRegistration);
    void offerDraw(String gameId, String offeringPlayerId, AuthListener listener);
    void acceptDraw(String gameId, String acceptingPlayerId, GameEndListener listener);
    void declineDraw(String gameId, String decliningPlayerId, AuthListener listener);
    void resignGame(String gameId, String resigningPlayerId, GameEndListener listener);
    void updateLastSeen(String gameId, String playerId, AuthListener listener);
    void initializePlayerBoardStateTransactionally(String gameId, String playerId, String playerColor, DeckModel playerDeckModel, AuthListener listener);

    /**
     * Performs a standard piece move or a special ranged attack on the backend.
     * Updates board state, turn, timers, and draw counters.
     *
     * @param gameId            The ID of the game.
     * @param originalSquare    Algebraic notation of the piece's starting square.
     * @param targetSquare      Algebraic notation of the piece's target square.
     * @param pieceValue        String identifier of the moving piece (e.g., "WHITE_PAWN").
     * @param nextTurnPlayerId  The ID of the player whose turn it will be next.
     * @param isCapture         True if this move captures an opponent's piece (for standard moves).
     * @param isPawnMove        True if the moving piece is a pawn (for standard moves).
     * @param isRangedAttack    True if this is a special ranged attack (e.g., Archer) where the piece doesn't move.
     * @param player1TimeRemaining The updated time remaining for player 1 (in milliseconds).
     * @param player2TimeRemaining The updated time remaining for player 2 (in milliseconds).
     * @param listener          Callback for success or failure.
     */
    void performMove(String gameId, String originalSquare, String targetSquare, String pieceValue, String nextTurnPlayerId,
                     boolean isCapture, boolean isPawnMove, boolean isRangedAttack,
                     long player1TimeRemaining, long player2TimeRemaining, AuthListener listener);

    /**
     * Performs a spell cast on the backend.
     * Updates player spell list, turn, timers. May update board state depending on spell effect.
     *
     * @param gameId            The ID of the game.
     * @param castingPlayerId   The ID of the player casting the spell.
     * @param spellName         The type name of the spell being cast.
     * @param targetInfo        Optional map containing target information (e.g., {"targetSquare": "e5"}). Null if no target.
     * @param player1TimeRemaining The updated time remaining for player 1 (in milliseconds).
     * @param player2TimeRemaining The updated time remaining for player 2 (in milliseconds).
     * @param listener          Callback for success or failure.
     */
    void performSpellCast(String gameId, String castingPlayerId, String spellName, @Nullable Map<String, String> targetInfo,
                          long player1TimeRemaining, long player2TimeRemaining, AuthListener listener);

    /**
     * Records the final result of the game and updates player stats/ELO.
     * Sets the game status to FINISHED.
     *
     * @param gameId    The ID of the game.
     * @param winnerId  The ID of the winning player, or null for a draw.
     * @param loserId   The ID of the losing player, or null for a draw.
     * @param winReason A string constant indicating the reason for the game end (e.g., WIN_REASON_CHECKMATE).
     * @param listener  Callback for success or failure.
     */
    void recordGameResult(String gameId, @Nullable String winnerId, @Nullable String loserId, String winReason, AuthListener listener);

    /**
     * Fetches a list of recent completed matches for the given user.
     *
     * @param userId   The ID of the user whose history is being requested.
     * @param listener Callback to handle the result (list of entries or error).
     */
    void fetchMatchHistory(String userId, MatchHistoryListener listener);
}