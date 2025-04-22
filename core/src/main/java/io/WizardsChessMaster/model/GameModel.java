package io.WizardsChessMaster.model;

import com.badlogic.gdx.Gdx;

import io.WizardsChessMaster.model.pieces.Piece;
import io.WizardsChessMaster.model.pieces.PieceFactory;
import io.WizardsChessMaster.model.pieces.PieceType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the state of a single game session, managing Piece objects internally.
 * Includes game logic methods previously found in MoveValidator and a static
 * helper to generate board state strings for repetition checks from map data.
 * Uses GameStatus enum for game state.
 * Includes player spell lists and persistent effect tracking.
 */
public class GameModel {
    private static final String TAG = "GameModel";

    // --- Constants ---
    private static final int BOARD_WIDTH = 8;
    private static final int BOARD_HEIGHT = 8;
    private static final int POSITION_HISTORY_LIMIT = 60;
    private static final String MOVED_SUFFIX = "_MOVED";

    // --- Game Identification ---
    private String gameId;

    // --- Player Info ---
    private String player1Id;
    private String player2Id;
    private String player1DisplayName;
    private String player2DisplayName;
    private String player1DeckName;
    private String player2DeckName;
    private String player1Color;
    private String player2Color;
    private List<String> playerIds;

    // --- Game Settings ---
    private int pointLimit;
    private String timeLimit;
    private String joinCode;

    // --- Game State ---
    private transient GameStatus status;
    private String statusString;

    private String currentTurnPlayerId;
    private transient Map<BoardPosition, Piece> internalBoard;
    private long player1TimeRemainingMillis;
    private long player2TimeRemainingMillis;
    private Date lastUpdateTime;
    private Date player1LastSeen;
    private Date player2LastSeen;

    // --- Draw Offer State ---
    private String drawOfferedByPlayerId;

    // --- Draw Rule State ---
    private int fiftyMoveRuleCounter;
    private List<String> positionHistory;
    private BoardPosition enPassantTargetSquare;

    // --- Spell State ---
    private List<String> player1Spells;
    private List<String> player2Spells;

    private Map<String, List<String>> activeEffects;

    // --- Game Outcome ---
    private String winnerId;
    private String loserId;
    private String winReason;
    private int eloChangePlayer1;
    private int eloChangePlayer2;

    // Firestore requires a public no-argument constructor
    public GameModel() {
        this.positionHistory = new ArrayList<>();
        this.player1Spells = new ArrayList<>();
        this.player2Spells = new ArrayList<>();
        this.internalBoard = new HashMap<>();
        this.playerIds = new ArrayList<>();
        this.enPassantTargetSquare = null;
        this.statusString = GameStatus.PENDING_JOIN.getFirestoreValue();
        this.status = GameStatus.PENDING_JOIN;
        this.activeEffects = new HashMap<>();
    }

    // --- Getters and Setters ---
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getPlayer1Id() { return player1Id; }
    public void setPlayer1Id(String player1Id) { this.player1Id = player1Id; updatePlayerIds(); }
    public String getPlayer2Id() { return player2Id; }
    public void setPlayer2Id(String player2Id) { this.player2Id = player2Id; updatePlayerIds(); }
    public List<String> getPlayerIds() { return playerIds == null ? new ArrayList<>() : playerIds; }
    public void setPlayerIds(List<String> playerIds) { this.playerIds = playerIds; }
    private void updatePlayerIds() { if (player1Id != null && player2Id != null) { this.playerIds = new ArrayList<>(Arrays.asList(player1Id, player2Id)); } else if (player1Id != null) { this.playerIds = new ArrayList<>(Collections.singletonList(player1Id)); } else { this.playerIds = new ArrayList<>(); } }
    public String getPlayer1DisplayName() { return player1DisplayName; }
    public void setPlayer1DisplayName(String player1DisplayName) { this.player1DisplayName = player1DisplayName; }
    public String getPlayer2DisplayName() { return player2DisplayName; }
    public void setPlayer2DisplayName(String player2DisplayName) { this.player2DisplayName = player2DisplayName; }
    public String getPlayer1DeckName() { return player1DeckName; }
    public void setPlayer1DeckName(String player1DeckName) { this.player1DeckName = player1DeckName; }
    public String getPlayer2DeckName() { return player2DeckName; }
    public void setPlayer2DeckName(String player2DeckName) { this.player2DeckName = player2DeckName; }
    public String getPlayer1Color() { return player1Color; }
    public void setPlayer1Color(String player1Color) { this.player1Color = player1Color; }
    public String getPlayer2Color() { return player2Color; }
    public void setPlayer2Color(String player2Color) { this.player2Color = player2Color; }
    public int getPointLimit() { return pointLimit; }
    public void setPointLimit(int pointLimit) { this.pointLimit = pointLimit; }
    public String getTimeLimit() { return timeLimit; }
    public void setTimeLimit(String timeLimit) { this.timeLimit = timeLimit; }
    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }
    public GameStatus getStatusEnum() { if (this.status == null && this.statusString != null) { this.status = GameStatus.fromFirestoreValue(this.statusString); if (this.status == null) { Gdx.app.error(TAG, "getStatusEnum: Unknown status string '" + this.statusString + "'. Defaulting to ERROR."); this.status = GameStatus.ERROR; } } else if (this.status == null) { Gdx.app.error(TAG, "getStatusEnum: Status enum and string are both null. Defaulting to ERROR."); this.status = GameStatus.ERROR; } return this.status; }
    public void setStatusEnum(GameStatus status) { this.status = (status != null) ? status : GameStatus.ERROR; this.statusString = this.status.getFirestoreValue(); }
    public String getStatus() { return (this.status != null) ? this.status.getFirestoreValue() : this.statusString; }
    public void setStatus(String statusString) { this.statusString = statusString; this.status = GameStatus.fromFirestoreValue(statusString); if (this.status == null) { Gdx.app.error(TAG, "setStatus(String): Unknown status string received from Firestore: '" + statusString + "'. Setting status enum to ERROR."); this.status = GameStatus.ERROR; this.statusString = GameStatus.ERROR.getFirestoreValue(); } }
    public String getCurrentTurnPlayerId() { return currentTurnPlayerId; }
    public void setCurrentTurnPlayerId(String currentTurnPlayerId) { this.currentTurnPlayerId = currentTurnPlayerId; }
    public Map<String, Object> getBoardState() { Map<String, Object> firebaseBoardState = new HashMap<>(); if (this.internalBoard != null) { for (Map.Entry<BoardPosition, Piece> entry : this.internalBoard.entrySet()) { BoardPosition pos = entry.getKey(); Piece piece = entry.getValue(); if (pos != null && piece != null && piece.getTeam() != null && piece.getTypeName() != null) { String algebraic = boardPositionToAlgebraic(pos); String basePieceValue = piece.getTeam().name().toUpperCase() + "_" + piece.getTypeName().toUpperCase(); String finalPieceValue = basePieceValue; String typeName = piece.getTypeName(); if ("PAWN".equals(typeName) || "ROOK".equals(typeName) || "KING".equals(typeName)) { Object hasMovedObj = piece.getStateVariable("hasMoved"); if (Boolean.TRUE.equals(hasMovedObj)) { finalPieceValue += MOVED_SUFFIX; } } if (algebraic != null) { firebaseBoardState.put(algebraic, finalPieceValue); } } else if (piece != null) { Gdx.app.error(TAG, "Skipping piece in getBoardState due to null data: " + piece); } } } return firebaseBoardState; }
    public void setBoardState(Map<String, Object> firebaseBoardState) { this.internalBoard = new HashMap<>(); if (firebaseBoardState != null) { for (Map.Entry<String, Object> entry : firebaseBoardState.entrySet()) { String algebraicSquare = entry.getKey(); Object pieceValueObj = entry.getValue(); if (algebraicSquare != null && pieceValueObj instanceof String) { String rawPieceValue = (String) pieceValueObj; BoardPosition pos = algebraicToBoardPosition(algebraicSquare); String pieceValue = rawPieceValue; boolean hasMoved = false; if (rawPieceValue.endsWith(MOVED_SUFFIX)) { pieceValue = rawPieceValue.substring(0, rawPieceValue.length() - MOVED_SUFFIX.length()); hasMoved = true; } if (pos != null && pieceValue.contains("_")) { String[] parts = pieceValue.split("_", 2); if (parts.length == 2) { try { Team team = Team.valueOf(parts[0].toUpperCase()); String typeName = parts[1].toUpperCase(); Piece piece = PieceFactory.createPiece(typeName, team, pos); if (piece != null) { if (hasMoved) { piece.setStateVariable("hasMoved", true); } this.internalBoard.put(pos, piece); } else { Gdx.app.error(TAG, "PieceFactory returned null for: '" + pieceValue + "' at " + algebraicSquare); } } catch (IllegalArgumentException e) { Gdx.app.error(TAG, "Error parsing/creating piece: '" + pieceValue + "' (from '" + rawPieceValue + "') at " + algebraicSquare, e); } catch (Exception e) { Gdx.app.error(TAG, "Unexpected error creating piece: '" + pieceValue + "' (from '" + rawPieceValue + "') at " + algebraicSquare, e); } } else { Gdx.app.error(TAG, "Invalid piece format: '" + pieceValue + "' (from '" + rawPieceValue + "') at " + algebraicSquare); } } else if (pos == null) { Gdx.app.error(TAG, "Invalid square notation: '" + algebraicSquare + "'"); } } else if (pieceValueObj != null) { Gdx.app.error(TAG, "Unexpected type in boardState map for key '" + algebraicSquare + "': " + pieceValueObj.getClass().getName()); } } Gdx.app.debug(TAG, "Internal board recreated from boardState. Size: " + this.internalBoard.size()); } else { Gdx.app.debug(TAG, "Received null boardState."); } }
    public long getPlayer1TimeRemainingMillis() { return player1TimeRemainingMillis; }
    public void setPlayer1TimeRemainingMillis(long player1TimeRemainingMillis) { this.player1TimeRemainingMillis = player1TimeRemainingMillis; }
    public long getPlayer2TimeRemainingMillis() { return player2TimeRemainingMillis; }
    public void setPlayer2TimeRemainingMillis(long player2TimeRemainingMillis) { this.player2TimeRemainingMillis = player2TimeRemainingMillis; }
    public Date getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(Date lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    public Date getPlayer1LastSeen() { return player1LastSeen; }
    public void setPlayer1LastSeen(Date player1LastSeen) { this.player1LastSeen = player1LastSeen; }
    public Date getPlayer2LastSeen() { return player2LastSeen; }
    public void setPlayer2LastSeen(Date player2LastSeen) { this.player2LastSeen = player2LastSeen; }
    public String getDrawOfferedByPlayerId() { return drawOfferedByPlayerId; }
    public void setDrawOfferedByPlayerId(String drawOfferedByPlayerId) { this.drawOfferedByPlayerId = drawOfferedByPlayerId; }
    public int getFiftyMoveRuleCounter() { return fiftyMoveRuleCounter; }
    public void setFiftyMoveRuleCounter(int fiftyMoveRuleCounter) { this.fiftyMoveRuleCounter = fiftyMoveRuleCounter; }
    public List<String> getPositionHistory() { return positionHistory == null ? new ArrayList<>() : positionHistory; }
    public void setPositionHistory(List<String> positionHistory) { this.positionHistory = positionHistory; }
    public String getEnPassantTargetSquareString() { return GameModel.boardPositionToAlgebraic(this.enPassantTargetSquare); }
    public void setEnPassantTargetSquareString(String algebraicSquare) { this.enPassantTargetSquare = GameModel.algebraicToBoardPosition(algebraicSquare); }
    public List<String> getPlayer1Spells() { return player1Spells == null ? new ArrayList<>() : new ArrayList<>(player1Spells); }
    public void setPlayer1Spells(List<String> player1Spells) { this.player1Spells = player1Spells == null ? new ArrayList<>() : player1Spells; }
    public List<String> getPlayer2Spells() { return player2Spells == null ? new ArrayList<>() : new ArrayList<>(player2Spells); }
    public void setPlayer2Spells(List<String> player2Spells) { this.player2Spells = player2Spells == null ? new ArrayList<>() : player2Spells; }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public String getLoserId() { return loserId; }
    public void setLoserId(String loserId) { this.loserId = loserId; }
    public String getWinReason() { return winReason; }
    public void setWinReason(String winReason) { this.winReason = winReason; }
    public int getEloChangePlayer1() { return eloChangePlayer1; }
    public void setEloChangePlayer1(int eloChangePlayer1) { this.eloChangePlayer1 = eloChangePlayer1; }
    public int getEloChangePlayer2() { return eloChangePlayer2; }
    public void setEloChangePlayer2(int eloChangePlayer2) { this.eloChangePlayer2 = eloChangePlayer2; }

    public Map<String, List<String>> getActiveEffects() {
        return activeEffects == null ? new HashMap<>() : activeEffects;
    }
    public void setActiveEffects(Map<String, List<String>> activeEffects) {
        this.activeEffects = activeEffects == null ? new HashMap<>() : activeEffects;
    }

    // --- Model Helper Methods ---
    public int getBoardWidth() { return BOARD_WIDTH; }
    public int getBoardHeight() { return BOARD_HEIGHT; }
    public boolean isWithinBounds(BoardPosition position) { if (position == null) return false; return position.getX() >= 0 && position.getX() < BOARD_WIDTH && position.getY() >= 0 && position.getY() < BOARD_HEIGHT; }
    public BoardPosition getEnPassantTargetSquareObject() { return this.enPassantTargetSquare; }
    public void setEnPassantTargetSquareObject(BoardPosition pos) { this.enPassantTargetSquare = pos; }
    public Piece getPieceAt(BoardPosition position) { if (internalBoard == null) { Gdx.app.error(TAG, "getPieceAt called but internalBoard is null!"); return null; } if (position == null) return null; return internalBoard.get(position); }
    public Collection<Piece> getAllPieces() { return internalBoard != null ? Collections.unmodifiableCollection(internalBoard.values()) : Collections.emptyList(); }
    public List<Piece> getPiecesForTeam(Team team) { if (internalBoard == null || team == null) return Collections.emptyList(); return internalBoard.values().stream() .filter(p -> p != null && p.getTeam() == team) .collect(Collectors.toList()); }
    public BoardPosition findKingPosition(Team team) { if (internalBoard == null || team == null) return null; String kingTypeName = PieceType.KING.name(); for (Piece piece : internalBoard.values()) { if (piece != null && kingTypeName.equals(piece.getTypeName()) && piece.getTeam() == team) { return piece.getPosition(); } } return null; }
    public boolean isSquareAttacked(BoardPosition square, Team attackerTeam) { if (internalBoard == null || square == null || attackerTeam == null) return false; for (Piece piece : getPiecesForTeam(attackerTeam)) { if (piece == null) continue; try { if (piece.getAttackedSquares(this).contains(square)) { return true; } } catch (Exception e) { Gdx.app.error(TAG, "Error checking attacks for " + piece + " at " + piece.getPosition(), e); } } return false; }
    public boolean isKingInCheck(Team team) { BoardPosition kingPos = findKingPosition(team); if (kingPos == null) { return false; } return isSquareAttacked(kingPos, team.opposite()); }
    public void clearTemporaryPieceFlags() { if (internalBoard == null) return; for (Piece piece : internalBoard.values()) { if (piece == null) continue; if ("PAWN".equals(piece.getTypeName())) { try { piece.setStateVariable("justMovedTwoSquares", false); } catch (Exception e) { Gdx.app.error(TAG, "Error clearing flags for piece " + piece + " at " + piece.getPosition(), e); } } } }

    public void addTurnEffect(String playerId, String effectName) {
        if (playerId == null || effectName == null || effectName.trim().isEmpty()) {
            Gdx.app.error(TAG, "Attempted to add null/empty turn effect for player: " + playerId);
            return;
        }
        if (activeEffects == null) {
            activeEffects = new HashMap<>();
        }
        // Ensure list exists and add effect if not already present
        List<String> playerEffects = activeEffects.computeIfAbsent(playerId, k -> new ArrayList<>());
        if (!playerEffects.contains(effectName)) {
            playerEffects.add(effectName);
            Gdx.app.debug(TAG, "Added turn effect '" + effectName + "' for player " + playerId);
        }
    }

    public boolean hasTurnEffect(String playerId, String effectName) {
        if (playerId == null || effectName == null || activeEffects == null) {
            return false;
        }
        List<String> effects = activeEffects.get(playerId);
        return effects != null && effects.contains(effectName);
    }

    public void clearTurnEffects(String playerId) {
        if (playerId != null && activeEffects != null) {
            // Replace with an empty list instead of removing the key,
            List<String> removedEffects = activeEffects.put(playerId, new ArrayList<>());
            if (removedEffects != null && !removedEffects.isEmpty()) {
                Gdx.app.debug(TAG, "Cleared turn effects for player " + playerId + ": " + removedEffects);
            } else {
                // Ensure the key exists with an empty list if it wasn't there before
                activeEffects.computeIfAbsent(playerId, k -> new ArrayList<>());
            }
        }
    }

    public GameModel copy() {
        GameModel copy = new GameModel();
        // Shallow copy fields
        copy.gameId = this.gameId;
        copy.player1Id = this.player1Id;
        copy.player2Id = this.player2Id;
        copy.player1DisplayName = this.player1DisplayName;
        copy.player2DisplayName = this.player2DisplayName;
        copy.player1DeckName = this.player1DeckName;
        copy.player2DeckName = this.player2DeckName;
        copy.player1Color = this.player1Color;
        copy.player2Color = this.player2Color;
        copy.pointLimit = this.pointLimit;
        copy.timeLimit = this.timeLimit;
        copy.joinCode = this.joinCode;
        copy.status = this.status;
        copy.statusString = this.statusString;
        copy.currentTurnPlayerId = this.currentTurnPlayerId;
        copy.player1TimeRemainingMillis = this.player1TimeRemainingMillis;
        copy.player2TimeRemainingMillis = this.player2TimeRemainingMillis;
        copy.lastUpdateTime = this.lastUpdateTime != null ? (Date) this.lastUpdateTime.clone() : null;
        copy.player1LastSeen = this.player1LastSeen != null ? (Date) this.player1LastSeen.clone() : null;
        copy.player2LastSeen = this.player2LastSeen != null ? (Date) this.player2LastSeen.clone() : null;
        copy.drawOfferedByPlayerId = this.drawOfferedByPlayerId;
        copy.fiftyMoveRuleCounter = this.fiftyMoveRuleCounter;
        copy.enPassantTargetSquare = this.enPassantTargetSquare;
        copy.winnerId = this.winnerId;
        copy.loserId = this.loserId;
        copy.winReason = this.winReason;
        copy.eloChangePlayer1 = this.eloChangePlayer1;
        copy.eloChangePlayer2 = this.eloChangePlayer2;
        copy.playerIds = this.playerIds != null ? new ArrayList<>(this.playerIds) : new ArrayList<>();
        copy.positionHistory = this.positionHistory != null ? new ArrayList<>(this.positionHistory) : new ArrayList<>();
        copy.player1Spells = this.player1Spells != null ? new ArrayList<>(this.player1Spells) : new ArrayList<>();
        copy.player2Spells = this.player2Spells != null ? new ArrayList<>(this.player2Spells) : new ArrayList<>();
        copy.internalBoard = new HashMap<>();
        if (this.internalBoard != null) {
            for (Map.Entry<BoardPosition, Piece> entry : this.internalBoard.entrySet()) {
                if (entry.getValue() != null) {
                    try { copy.internalBoard.put(entry.getKey(), entry.getValue().copy()); }
                    catch (Exception e) { Gdx.app.error(TAG, "Error copying piece " + entry.getValue() + " during GameModel copy", e); }
                } else { copy.internalBoard.put(entry.getKey(), null); }
            }
        }
        copy.activeEffects = new HashMap<>();
        if (this.activeEffects != null) {
            for (Map.Entry<String, List<String>> entry : this.activeEffects.entrySet()) {
                if (entry.getValue() != null) {
                    copy.activeEffects.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                } else {
                    copy.activeEffects.put(entry.getKey(), new ArrayList<>());
                }
            }
        }
        return copy;
    }

    // --- Piece Manipulation Methods ---
    public Piece movePiece(Piece pieceToMove, BoardPosition targetPosition) { if (pieceToMove == null || targetPosition == null || internalBoard == null || !isWithinBounds(targetPosition)) { Gdx.app.error(TAG, "Invalid movePiece arguments: Piece=" + pieceToMove + ", Target=" + targetPosition); return null; } BoardPosition originalPosition = pieceToMove.getPosition(); if (originalPosition == null || !Objects.equals(internalBoard.get(originalPosition), pieceToMove)) { Piece actualPiece = internalBoard.get(originalPosition); Gdx.app.error(TAG, "Move attempt failed: Piece " + pieceToMove.getTypeName() + " ("+pieceToMove+") not found at its own position " + originalPosition + " in internalBoard. Found: " + actualPiece); return null; } Piece capturedPiece = internalBoard.remove(targetPosition); if (capturedPiece != null) { try { pieceToMove.onCapture(capturedPiece); } catch (Exception e) { Gdx.app.error(TAG, "Error during onCapture callback for " + pieceToMove, e); } } internalBoard.remove(originalPosition); internalBoard.put(targetPosition, pieceToMove); try { pieceToMove.onMove(targetPosition); } catch (Exception e) { Gdx.app.error(TAG, "Error during onMove callback for " + pieceToMove + " to " + targetPosition, e); } return capturedPiece; }
    public Piece removePieceAt(BoardPosition position) { if (position == null || internalBoard == null || !isWithinBounds(position)) return null; return internalBoard.remove(position); }
    public boolean removePiece(Piece pieceToRemove) { if (pieceToRemove == null || pieceToRemove.getPosition() == null || internalBoard == null) return false; if (internalBoard.get(pieceToRemove.getPosition()) == pieceToRemove) { return removePieceAt(pieceToRemove.getPosition()) != null; } Gdx.app.error(TAG, "Attempted to remove piece by reference, but it was not found at its position: " + pieceToRemove); return false; }
    public void placePiece(Piece piece) { if (piece == null || piece.getPosition() == null || internalBoard == null || !isWithinBounds(piece.getPosition())) { Gdx.app.error(TAG, "Cannot place invalid piece: " + piece); return; } internalBoard.put(piece.getPosition(), piece); }


    // --- Game Logic Methods ---
    public boolean hasLegalMoves(Team team) { if (team == null || internalBoard == null) return false; for (Piece piece : getPiecesForTeam(team)) { if (piece != null) { Set<BoardPosition> moves = piece.getValidMoves(this); if (moves != null && !moves.isEmpty()) { return true; } } } return false; }
    public boolean isCheckmate(Team team) { if (team == null) return false; return isKingInCheck(team) && !hasLegalMoves(team); }
    public boolean isStalemate(Team team) { if (team == null) return false; if (findKingPosition(team) == null) return false; return !isKingInCheck(team) && !hasLegalMoves(team); }
    public boolean isInsufficientMaterial() { if (internalBoard == null || internalBoard.isEmpty()) { return false; } List<PieceType> whitePieceTypes = new ArrayList<>(); List<PieceType> blackPieceTypes = new ArrayList<>(); List<BoardPosition> whiteBishopPositions = new ArrayList<>(); List<BoardPosition> blackBishopPositions = new ArrayList<>(); for (Piece piece : internalBoard.values()) { if (piece == null || piece.getTypeName() == null) continue; String typeName = piece.getTypeName(); PieceType type = null; try { type = PieceType.valueOf(typeName); } catch (IllegalArgumentException e) { Gdx.app.debug(TAG, "Insufficient material check: Found custom/unknown piece '" + typeName + "', assuming sufficient material."); return false; } if (type == PieceType.PAWN || type == PieceType.ROOK || type == PieceType.QUEEN) { Gdx.app.debug(TAG, "Insufficient material check: Found " + type + ", assuming sufficient material."); return false; } if (piece.getTeam() == Team.WHITE) { whitePieceTypes.add(type); if (type == PieceType.BISHOP) whiteBishopPositions.add(piece.getPosition()); } else { blackPieceTypes.add(type); if (type == PieceType.BISHOP) blackBishopPositions.add(piece.getPosition()); } } int whiteCount = whitePieceTypes.size(); int blackCount = blackPieceTypes.size(); if (whiteCount == 1 && blackCount == 1) { if (whitePieceTypes.get(0) == PieceType.KING && blackPieceTypes.get(0) == PieceType.KING) { Gdx.app.debug(TAG, "Insufficient material: K vs K detected."); return true; } } if ((whiteCount == 1 && blackCount == 2) || (whiteCount == 2 && blackCount == 1)) { List<PieceType> twoPieces = (whiteCount == 2) ? whitePieceTypes : blackPieceTypes; boolean hasKing = twoPieces.contains(PieceType.KING); boolean hasMinor = twoPieces.contains(PieceType.KNIGHT) || twoPieces.contains(PieceType.BISHOP); if (hasKing && hasMinor) { Gdx.app.debug(TAG, "Insufficient material: K vs K + Minor Piece detected."); return true; } } if (whiteCount == 2 && blackCount == 2) { boolean whiteKB = whitePieceTypes.contains(PieceType.KING) && whitePieceTypes.contains(PieceType.BISHOP); boolean blackKB = blackPieceTypes.contains(PieceType.KING) && blackPieceTypes.contains(PieceType.BISHOP); if (whiteKB && blackKB) { if (whiteBishopPositions.size() == 1 && blackBishopPositions.size() == 1) { BoardPosition whitePos = whiteBishopPositions.get(0); BoardPosition blackPos = blackBishopPositions.get(0); if (whitePos != null && blackPos != null) { boolean whiteIsDark = (whitePos.getX() + whitePos.getY()) % 2 != 0; boolean blackIsDark = (blackPos.getX() + blackPos.getY()) % 2 != 0; if (whiteIsDark == blackIsDark) { Gdx.app.debug(TAG, "Insufficient material: K+B vs K+B (Same color bishops) detected."); return true; } } } } } return false; }
    public String getBoardStateString() { if (internalBoard == null || currentTurnPlayerId == null) return null; Team currentTeam = getPlayerTeamById(currentTurnPlayerId); if (currentTeam == null) return null; TreeMap<String, String> sortedBoard = new TreeMap<>(); Map<String, Object> stateWithMoved = getBoardState(); for(Map.Entry<String, Object> entry : stateWithMoved.entrySet()) { if (entry.getValue() instanceof String) { sortedBoard.put(entry.getKey(), (String) entry.getValue()); } } StringBuilder sb = new StringBuilder(); for (Map.Entry<String, String> entry : sortedBoard.entrySet()) { sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";"); } sb.append("|Turn=").append(currentTeam == Team.WHITE ? "w" : "b"); sb.append("|Castle=").append("-"); sb.append("|EP=").append(getEnPassantTargetSquareString() != null ? getEnPassantTargetSquareString() : "-"); return sb.toString(); }

    // --- Static Helper Methods ---
    public static BoardPosition algebraicToBoardPosition(String square) { if (square == null || square.length() != 2) return null; int file = square.charAt(0) - 'a'; int rank = square.charAt(1) - '1'; if (file < 0 || file >= BOARD_WIDTH || rank < 0 || rank >= BOARD_HEIGHT) return null; return new BoardPosition(file, rank); }
    public static String boardPositionToAlgebraic(BoardPosition pos) { if (pos == null) return null; int file = pos.getX(); int rank = pos.getY(); if (file < 0 || file >= BOARD_WIDTH || rank < 0 || rank >= BOARD_HEIGHT) return null; return "" + (char)('a' + file) + (char)('1' + rank); }
    public static String generateBoardStateString(Map<String, String> boardState, String turnColor, String castlingRights, String epTargetSquare) { if (boardState == null || turnColor == null) return null; TreeMap<String, String> sortedBoard = new TreeMap<>(boardState); StringBuilder sb = new StringBuilder(); for (Map.Entry<String, String> entry : sortedBoard.entrySet()) { if (entry.getValue() != null) { sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";"); } } sb.append("|Turn=").append(turnColor.toLowerCase().startsWith("w") ? "w" : "b"); sb.append("|Castle=").append(castlingRights != null ? castlingRights : "-"); sb.append("|EP=").append(epTargetSquare != null ? epTargetSquare : "-"); return sb.toString(); }


    // --- Convenience Methods ---
    public String getOpponentId(String myId) { if (myId == null) return null; if (myId.equals(player1Id)) return player2Id; if (myId.equals(player2Id)) return player1Id; return null; }
    public String getMyColor(String myId) { if (myId == null) return null; if (myId.equals(player1Id)) return player1Color; if (myId.equals(player2Id)) return player2Color; return null; }
    public String getOpponentDisplayName(String myId) { if (myId == null) return null; if (myId.equals(player1Id)) return player2DisplayName; if (myId.equals(player2Id)) return player1DisplayName; return null; }
    public int getEloChangeForPlayer(String playerId) { if (playerId == null) return 0; if (playerId.equals(player1Id)) return eloChangePlayer1; if (playerId.equals(player2Id)) return eloChangePlayer2; return 0; }
    public String getPlayerColorById(String playerId) { if (playerId == null) return null; if (playerId.equals(player1Id)) return player1Color; if (playerId.equals(player2Id)) return player2Color; return null; }
    public Team getPlayerTeamById(String playerId) { String color = getPlayerColorById(playerId); if ("white".equalsIgnoreCase(color)) return Team.WHITE; if ("black".equalsIgnoreCase(color)) return Team.BLACK; return null; }
    public List<String> getSpellsForPlayer(String playerId) { if (playerId == null) return new ArrayList<>(); if (playerId.equals(player1Id)) return getPlayer1Spells(); if (playerId.equals(player2Id)) return getPlayer2Spells(); return new ArrayList<>(); }
    public boolean hasPendingDrawOffer() { return drawOfferedByPlayerId != null && getStatusEnum().isActive(); }
    public boolean isDrawOfferedToPlayer(String playerId) { return hasPendingDrawOffer() && playerId != null && !drawOfferedByPlayerId.equals(playerId); }
    public boolean isDrawOfferedByPlayer(String playerId) { return hasPendingDrawOffer() && playerId != null && drawOfferedByPlayerId.equals(playerId); }

}