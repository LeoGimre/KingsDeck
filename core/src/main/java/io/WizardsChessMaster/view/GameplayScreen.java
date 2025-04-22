package io.WizardsChessMaster.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.UserModel;
import io.WizardsChessMaster.model.pieces.PieceConfig;
import io.WizardsChessMaster.model.pieces.PieceFactory;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.presenter.GameplayPresenter;
import io.WizardsChessMaster.model.spells.Spell;
import io.WizardsChessMaster.model.spells.SpellConfig;
import io.WizardsChessMaster.model.spells.SpellFactory;
import io.WizardsChessMaster.view.interfaces.IGameplayView;

public class GameplayScreen extends ScreenAdapter implements Disposable, IGameplayView {

    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private GameplayPresenter controller;

    private String playerColor = "white";
    private static final String MOVED_SUFFIX = "_MOVED";

    // UI Elements
    private Label playerInfoLabel, opponentInfoLabel, playerDeckLabel, statusLabel;
    private Label player1TimerLabel, player2TimerLabel;
    private Label opponentStatusLabel;
    private Label connectionStatusLabel;
    private TextButton resignButton, offerDrawButton, acceptDrawButton, declineDrawButton, gameOverBackButton;
    private Table drawOfferTable;
    private Label drawOfferReceivedLabel;
    private Dialog gameOverDialog, errorDialog;
    private Label gameOverTitleLabel, gameOverResultMessageLabel, gameOverEloChangeLabel;
    private HorizontalGroup spellBarGroup;

    // Board and Piece Elements
    private Image boardImage;
    private Group boardGroup;
    private Group pieceGroup;
    private Group highlightGroup;
    private Map<String, Image> pieceActors;
    private List<Image> highlightActors;
    private Map<String, Texture> pieceTextures;
    private ObjectMap<String, Texture> spellTextures;
    private Texture boardTexture;
    private Texture highlightTexture;
    private Texture spellSlotTexture;
    private Texture connectedIconTexture;
    private Texture disconnectedIconTexture;

    // Layout cache
    private float boardSize = 0;
    private float squareSize = 0;
    private float boardStageX = 0;
    private float boardStageY = 0;

    public GameplayScreen(Main game, FirebaseService firebaseService, String gameId, String opponentId, String opponentDisplayName, String playerColor, Skin sharedSkin) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        if (sharedSkin == null) {
            Gdx.app.error("GameplayScreen", "Error: Shared skin is null!");
            throw new RuntimeException("Shared skin cannot be null for GameplayScreen");
        }
        this.skin = sharedSkin;

        loadAssets();
        setupUi();
        setupDrawOfferUi();
        setupGameOverDialog();
        setupErrorDialog();
        addInputListener();

        this.controller = new GameplayPresenter(game, firebaseService, gameId, opponentId, opponentDisplayName, playerColor);
        this.controller.setView(this);
    }

    @Override
    public void show() {
        Gdx.app.log("GameplayScreen", "show() called.");
        Gdx.input.setInputProcessor(stage);
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        calculateBoardLayout();
        if (errorDialog != null) errorDialog.hide();

        if (controller != null) {
            if (controller.isGameEnded() && gameOverDialog != null && controller.getCurrentGameModel() != null) {
                Gdx.app.log("GameplayScreen", "show(): Game already ended, ensuring game over dialog is visible.");
                GameModel finalModel = controller.getCurrentGameModel();
                String winnerId = finalModel.getWinnerId(); String loserId = finalModel.getLoserId(); String rawWinReason = finalModel.getWinReason();
                int playerEloChange = finalModel.getEloChangeForPlayer(controller.getCurrentPlayerId());
                UserModel profile = controller.getCurrentUserProfile();

                boolean eloDataAvailable = finalModel.getEloChangePlayer1() != 0 || finalModel.getEloChangePlayer2() != 0 || (rawWinReason != null && rawWinReason.equals(FirebaseService.WIN_REASON_DRAW_AGREEMENT));
                boolean playerWon = controller.getCurrentPlayerId() != null && controller.getCurrentPlayerId().equals(winnerId); boolean playerLost = controller.getCurrentPlayerId() != null && controller.getCurrentPlayerId().equals(loserId); boolean isDraw = winnerId == null && loserId == null; if (!isDraw && rawWinReason != null) { isDraw = FirebaseService.WIN_REASON_STALEMATE.equals(rawWinReason) || FirebaseService.WIN_REASON_DRAW_AGREEMENT.equals(rawWinReason) || FirebaseService.DRAW_REASON_REPETITION.equals(rawWinReason) || FirebaseService.DRAW_REASON_50_MOVE.equals(rawWinReason) || FirebaseService.DRAW_REASON_MATERIAL.equals(rawWinReason); }
                String eloString; if (eloDataAvailable && profile != null) { int pElo = profile.getEloRating(); int nElo = pElo + playerEloChange; eloString = String.format(Locale.getDefault(), "ELO: %d %+d = %d", pElo, playerEloChange, nElo); } else if (eloDataAvailable) { eloString = String.format(Locale.getDefault(), "ELO Change: %+d", playerEloChange); } else { eloString = "ELO: Pending update"; }
                String reason = "Unknown"; if (rawWinReason != null && !rawWinReason.isEmpty()) { reason = rawWinReason.replace("_", " "); reason = reason.substring(0, 1).toUpperCase() + reason.substring(1); } else { if (playerWon) reason = "Opponent Defeated"; else if (playerLost) reason = "Defeat"; else if (isDraw) reason = "Draw"; }
                String title, message; if (playerWon) { title = "Victory!"; message = "You won by " + reason + "!"; } else if (playerLost) { title = "Defeat"; message = "You lost by " + reason + "."; } else { title = "Draw"; message = "Game drawn by " + reason + "."; }

                showGameOverOverlay(title, message, eloString);
            } else {
                controller.refreshView();
            }
        } else {
            Gdx.app.error("GameplayScreen", "show(): Controller is null!");
        }
    }

    // --- IGameplayView Implementation ---

    @Override
    public void setPlayerColor(String color) {
        if (color != null && (color.equalsIgnoreCase("white") || color.equalsIgnoreCase("black"))) { this.playerColor = color.toLowerCase(); } else { this.playerColor = "white"; }
        Gdx.app.log("GameplayScreen", "Player color set to: " + this.playerColor);
        if (boardSize > 0) {
            calculateBoardLayout();
        }
    }

    // --- Private Asset Loading and Setup ---
    private void loadAssets() {
        pieceTextures = new HashMap<>();
        try {
            boardTexture = new Texture(Gdx.files.internal("board.png"));
            String[] colors = {"white", "black"};
            Collection<String> pieceTypeNames = PieceFactory.getAvailablePieceTypes();
            for (String typeName : pieceTypeNames) {
                PieceConfig config = PieceFactory.getConfig(typeName);
                if (config != null && config.assetBaseName != null && !config.assetBaseName.isEmpty()) {
                    for (String c : colors) {
                        String fn = "pieces/" + c + "_" + config.assetBaseName + ".png";
                        try { if (Gdx.files.internal(fn).exists()) { Texture t = new Texture(Gdx.files.internal(fn)); pieceTextures.put(c.toUpperCase() + "_" + typeName, t); } else { Gdx.app.log("GameplayScreen", "Piece texture file not found: " + fn); } } catch (Exception e) { Gdx.app.log("GameplayScreen", "Piece texture load fail: " + fn, e); }
                    }
                }
            }

            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888); pixmap.setColor(0, 1, 0, 0.4f); pixmap.fill(); highlightTexture = new Texture(pixmap); pixmap.dispose();
            try { connectedIconTexture = new Texture(Gdx.files.internal("ui/connected_icon.png")); } catch (Exception e) { Gdx.app.error("GameplayScreen", "connected_icon.png not found, creating placeholder."); connectedIconTexture = createPlaceholderTexture(Color.GREEN); }
            try { disconnectedIconTexture = new Texture(Gdx.files.internal("ui/disconnected_icon.png")); } catch (Exception e) { Gdx.app.error("GameplayScreen", "disconnected_icon.png not found, creating placeholder."); disconnectedIconTexture = createPlaceholderTexture(Color.RED); }

        } catch (Exception e) { Gdx.app.error("GameplayScreen", "Load board/highlight texture fail!", e); }

        spellTextures = new ObjectMap<>();
        try {
            Collection<String> spellTypeNames = SpellFactory.getAvailableSpellTypes();
            for (String typeName : spellTypeNames) {
                SpellConfig config = SpellFactory.getConfig(typeName);
                if (config != null) {
                    String path = config.getIconPath(); Texture texture = null;
                    try {
                        FileHandle handle = Gdx.files.internal(path);
                        if (handle.exists()) { texture = new Texture(handle); }
                        else { Gdx.app.log("GameplayScreen", "Spell texture file not found (using placeholder): " + path); Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888); pixmap.setColor(Color.PURPLE); pixmap.drawCircle(31, 31, 30); texture = new Texture(pixmap); pixmap.dispose(); }
                    } catch (Exception e) { Gdx.app.error("GameplayScreen", "Error loading texture for spell '" + typeName + "' at path: " + path, e); Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888); pixmap.setColor(Color.RED); pixmap.drawCircle(31, 31, 30); texture = new Texture(pixmap); pixmap.dispose(); }
                    if (texture != null) { spellTextures.put(typeName, texture); }
                }
            }
            Gdx.app.log("GameplayScreen", "Spell textures loaded. Count: " + spellTextures.size);
        } catch (Exception e) { Gdx.app.error("GameplayScreen", "Error loading spell textures: " + e.getMessage()); }
    }

    private Texture createPlaceholderTexture(Color color) {
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(color); pixmap.fillCircle(8, 8, 7);
        Texture texture = new Texture(pixmap); pixmap.dispose(); return texture;
    }

    // SetupUI
    private void setupUi() {
        Table mainTable = new Table(skin); mainTable.setFillParent(true); mainTable.pad(10); stage.addActor(mainTable);

        // Top Info Bar
        Table topInfoTable = new Table();
        playerInfoLabel = new Label("...", skin);
        player1TimerLabel = new Label("P1: --:--", skin);
        player2TimerLabel = new Label("P2: --:--", skin);
        connectionStatusLabel = new Label("", skin);
        connectionStatusLabel.setColor(Color.GREEN);

        topInfoTable.add(playerInfoLabel).expandX().left().padLeft(10);
        topInfoTable.add(connectionStatusLabel).padLeft(20);
        topInfoTable.add(player1TimerLabel).padLeft(20).padRight(10);
        topInfoTable.add(player2TimerLabel).padRight(20);
        offerDrawButton = new TextButton("Offer Draw", skin); topInfoTable.add(offerDrawButton).right().padLeft(20);
        resignButton = new TextButton("Resign", skin); topInfoTable.add(resignButton).right().padLeft(10).padRight(10);
        mainTable.add(topInfoTable).growX().padBottom(10).row();

        // Board Area
        Container<Group> boardContainer = new Container<>(); boardContainer.align(Align.center);
        mainTable.add(boardContainer).grow().row();
        boardGroup = new Group(); boardContainer.setActor(boardGroup);
        if (boardTexture != null) { boardImage = new Image(boardTexture); boardGroup.addActor(boardImage); }
        highlightGroup = new Group(); highlightActors = new ArrayList<>(); boardGroup.addActor(highlightGroup);
        pieceGroup = new Group(); pieceActors = new HashMap<>(); boardGroup.addActor(pieceGroup);

        // Bottom Area
        Table bottomAreaTable = new Table(); bottomAreaTable.pad(10).defaults().space(5);
        Table bottomInfoTable = new Table();
        playerDeckLabel = new Label("Your Deck: ...", skin); opponentInfoLabel = new Label("Opponent Deck: ...", skin); statusLabel = new Label("Initializing...", skin);
        opponentStatusLabel = new Label("", skin); opponentStatusLabel.setColor(Color.ORANGE); opponentStatusLabel.setVisible(false);
        bottomInfoTable.add(playerDeckLabel).expandX().left(); bottomInfoTable.add(opponentStatusLabel).right().padLeft(20); bottomInfoTable.row();
        bottomInfoTable.add(opponentInfoLabel).expandX().left().colspan(2).row(); bottomInfoTable.add(statusLabel).expandX().center().padTop(5).colspan(2);
        bottomAreaTable.add(bottomInfoTable).growX().row();

        // Spell Bar
        spellBarGroup = new HorizontalGroup(); spellBarGroup.space(5); spellBarGroup.wrap(false);
        ScrollPane spellScrollPane = new ScrollPane(spellBarGroup, skin); spellScrollPane.setScrollingDisabled(false, true); spellScrollPane.setFadeScrollBars(false);
        bottomAreaTable.add(spellScrollPane).growX().fillX().minHeight(65).padTop(10).row();

        // Draw Offer Area
        drawOfferTable = new Table(skin); bottomAreaTable.add(drawOfferTable).growX().padTop(10);
        mainTable.add(bottomAreaTable).growX().minHeight(150).bottom();
        addDrawResignListeners();
    }

    // addInputListener
    private void addInputListener() {
        stage.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                Actor target = event.getTarget();
                boolean isSpellClick = target instanceof Image && target.getParent() == spellBarGroup; if (isSpellClick) { return false; }
                boolean isDialogClick = false;
                Actor current = target;
                while (current != null) { if (current instanceof Dialog) { isDialogClick = true; break; } current = current.getParent(); }
                boolean isHandledByOtherUI = target != stage.getRoot() && !(target instanceof Group) && target.getParent() != pieceGroup && target.getParent() != highlightGroup && target != boardImage && !isDialogClick; if (isHandledByOtherUI) { return false; }
                if (boardSize > 0 && x >= boardStageX && x < (boardStageX + boardSize) && y >= boardStageY && y < (boardStageY + boardSize)) { String clickedSquare = coordinatesToAlgebraic(x, y); if (clickedSquare != null && controller != null) { controller.handleBoardClick(clickedSquare); return true; } }
                return false;
            }
        });
    }

    // Layout and Coordinate Conversion
    public void calculateBoardLayout() {
        float stageWidth = stage.getWidth(); float stageHeight = stage.getHeight(); if (stageWidth <= 0 || stageHeight <= 0) return;
        float availableHeight = stageHeight * 0.70f; float availableWidth = stageWidth * 0.95f;
        boardSize = Math.min(availableWidth, availableHeight); if (boardSize <= 0) return;
        squareSize = boardSize / 8f;
        boardStageX = (stageWidth - boardSize) / 2f;
        float topBarHeightEst = 60; float bottomBarHeightEst = 170;
        boardStageY = bottomBarHeightEst + (stageHeight - topBarHeightEst - bottomBarHeightEst - boardSize) / 2f;
        if (boardGroup != null) boardGroup.setBounds(boardStageX, boardStageY, boardSize, boardSize);
        if (boardImage != null) boardImage.setSize(boardSize, boardSize);
        if (pieceGroup != null) pieceGroup.setSize(boardSize, boardSize);
        if (highlightGroup != null) highlightGroup.setSize(boardSize, boardSize);
        Actor spellPane = spellBarGroup.getParent();
        if (spellPane instanceof ScrollPane) { Actor parent = spellPane.getParent(); if(parent instanceof Table) { Cell<?> cell = ((Table)parent).getCell(spellPane); if (cell != null) { cell.height(squareSize + 10); ((Table)parent).invalidateHierarchy(); } } }
        Gdx.app.log("GameplayScreen", "Layout: Size=" + boardSize + ", SquareSize=" + squareSize + ", StagePos=(" + boardStageX + "," + boardStageY + ")");
        repositionActors();
    }

    private void repositionActors() {
        if (pieceActors != null && !pieceActors.isEmpty() && squareSize > 0) { Map<String, Image> current = new HashMap<>(pieceActors); for (Map.Entry<String, Image> entry : current.entrySet()) { Vector2 pos = algebraicToCoordinates(entry.getKey()); if (pos != null && entry.getValue() != null) { entry.getValue().setPosition(pos.x, pos.y); entry.getValue().setSize(squareSize, squareSize); } } }
        if (spellBarGroup != null && squareSize > 0) { for (Actor actor : spellBarGroup.getChildren()) { if (actor instanceof Image) { ((Image) actor).setSize(squareSize, squareSize); } } spellBarGroup.layout(); }
        clearHighlights();
    }

    private String coordinatesToAlgebraic(float stageX, float stageY) {
        if (boardSize <= 0 || squareSize <= 0) return null;
        float boardRelativeX = stageX - boardStageX; float boardRelativeY = stageY - boardStageY;
        if (boardRelativeX < 0 || boardRelativeX >= boardSize || boardRelativeY < 0 || boardRelativeY >= boardSize) return null;
        int fileIndex = (int) (boardRelativeX / squareSize); int rankIndexTemp = (int) (boardRelativeY / squareSize);
        int rankIndex = playerColor.equals("black") ? (7 - rankIndexTemp) : rankIndexTemp;
        if (fileIndex < 0 || fileIndex > 7 || rankIndex < 0 || rankIndex > 7) return null;
        char fileChar = (char) ('a' + fileIndex); char rankChar = (char) ('1' + rankIndex); return "" + fileChar + rankChar;
    }

    private Vector2 algebraicToCoordinates(String square) {
        if (square == null || square.length() != 2 || squareSize <= 0) return null;
        char fileChar = square.charAt(0); char rankChar = square.charAt(1);
        if (fileChar < 'a' || fileChar > 'h' || rankChar < '1' || rankChar > '8') return null;
        int fileIndex = fileChar - 'a'; int rankIndex = rankChar - '1';
        float x = fileIndex * squareSize; float y = playerColor.equals("black") ? ((7 - rankIndex) * squareSize) : (rankIndex * squareSize);
        return new Vector2(x, y);
    }

    // Texture Getters
    private Texture getPieceTexture(String pieceValue) {
        if (pieceValue == null) return null;
        String lookupKey = pieceValue;
        if (pieceValue.endsWith(MOVED_SUFFIX)) { lookupKey = pieceValue.substring(0, pieceValue.length() - MOVED_SUFFIX.length()); }
        Texture texture = pieceTextures.get(lookupKey);
        if (texture == null) { Gdx.app.error("GameplayScreen", "Texture not found for piece value: '" + pieceValue + "' (lookup key: '" + lookupKey + "')"); }
        return texture;
    }
    private Texture getSpellTexture(String spellTypeName) { if (spellTypeName == null) return null; return spellTextures.get(spellTypeName.toUpperCase()); }

    // --- IGameplayView Implementation ---
    @Override
    public void displayBoard(Map<String, String> boardState) {
        if (pieceGroup == null || pieceActors == null) return; if (boardSize <= 0) { calculateBoardLayout(); if (boardSize <= 0) return; }
        pieceGroup.clearChildren(); pieceActors.clear(); if (boardState == null) return;
        for (Map.Entry<String, String> entry : boardState.entrySet()) {
            String square = entry.getKey(); String pieceValue = entry.getValue(); if (pieceValue == null || pieceValue.isEmpty()) continue;
            Texture tex = getPieceTexture(pieceValue); Vector2 coords = algebraicToCoordinates(square);
            if (tex != null && coords != null) { Image img = new Image(tex); img.setSize(squareSize, squareSize); img.setPosition(coords.x, coords.y); pieceGroup.addActor(img); pieceActors.put(square, img); }
            else { if (coords == null) { Gdx.app.error("GameplayScreen", "Invalid coordinates for square: " + square); } }
        }
    }
    @Override
    public void displaySpells(List<Spell> spells) {
        if (spellBarGroup == null) return; spellBarGroup.clearChildren(); if (spells == null || spells.isEmpty()) { spellBarGroup.invalidate(); return; };
        if (squareSize <= 0) { calculateBoardLayout(); if (squareSize <= 0) { Gdx.app.error("DisplaySpells", "Cannot display spells, squareSize is zero after layout calculation."); return; } }
        float spellIconSize = squareSize;
        for (final Spell spell : spells) {
            Texture tex = getSpellTexture(spell.getTypeName());
            if (tex != null) {
                Image spellImage = new Image(tex); spellImage.setScaling(Scaling.fit); spellImage.setSize(spellIconSize, spellIconSize);
                spellImage.setUserObject(spell); spellImage.setTouchable(Touchable.enabled);
                spellImage.addListener(new TextTooltip( String.format(Locale.US, "%s (%d pts)\n%s", spell.getDisplayName(), spell.getPointCost(), spell.getDescription()), skin));
                spellImage.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { Object userObject = event.getListenerActor().getUserObject(); if (userObject instanceof Spell && controller != null) { controller.handleSpellClick((Spell) userObject); } else { Gdx.app.error("SpellClick", "Controller is null or UserObject is not a Spell!"); } event.stop(); } });
                spellBarGroup.addActor(spellImage);
            } else { Gdx.app.error("DisplaySpells", "Missing texture for spell: " + spell.getTypeName()); }
        }
        spellBarGroup.layout();
    }
    @Override
    public void highlightValidMoves(List<String> squares) {
        clearHighlights(); if (highlightGroup == null || squares == null || squares.isEmpty() || highlightTexture == null || squareSize <= 0) return;
        for (String square : squares) { Vector2 coords = algebraicToCoordinates(square); if (coords != null) { Image img = new Image(highlightTexture); img.setSize(squareSize, squareSize); img.setPosition(coords.x, coords.y); highlightGroup.addActor(img); highlightActors.add(img); } }
    }
    @Override
    public void highlightSpellTargets(List<String> squares) {
        clearHighlights(); Texture spellTargetTexture = highlightTexture; Color spellTargetColor = Color.CYAN;
        if (highlightGroup == null || squares == null || squares.isEmpty() || spellTargetTexture == null || squareSize <= 0) return;
        for (String square : squares) { Vector2 coords = algebraicToCoordinates(square); if (coords != null) { Image img = new Image(spellTargetTexture); img.setColor(spellTargetColor); img.setSize(squareSize, squareSize); img.setPosition(coords.x, coords.y); highlightGroup.addActor(img); highlightActors.add(img); } }
    }
    @Override public void clearHighlights() { if(highlightActors != null) { for (Image actor : highlightActors) { actor.remove(); } highlightActors.clear(); } }

    // --- UI Setup Helpers ---
    private void setupDrawOfferUi() { drawOfferTable.setVisible(false); drawOfferTable.center(); drawOfferReceivedLabel = new Label("Opponent offers a draw.", skin); acceptDrawButton = new TextButton("Accept", skin); declineDrawButton = new TextButton("Decline", skin); drawOfferTable.add(drawOfferReceivedLabel).colspan(2).padBottom(10).row(); drawOfferTable.add(acceptDrawButton).width(100).padRight(10); drawOfferTable.add(declineDrawButton).width(100).padLeft(10); acceptDrawButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { if(controller != null) controller.handleAcceptDrawClicked(); } }); declineDrawButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { if(controller != null) controller.handleDeclineDrawClicked(); } }); }
    private void setupGameOverDialog() { gameOverDialog = new Dialog("", skin, "dialog") { @Override protected void result(Object object) { if(controller != null) controller.handleGameOverClosed(); } }; gameOverDialog.setModal(true); gameOverDialog.setMovable(false); gameOverDialog.setResizable(false); Table contentTable = gameOverDialog.getContentTable(); contentTable.pad(20); gameOverTitleLabel = new Label("Game Over", skin); gameOverResultMessageLabel = new Label("Reason", skin); gameOverEloChangeLabel = new Label("ELO: ????", skin); gameOverBackButton = new TextButton("Back to Menu", skin); contentTable.add(gameOverTitleLabel).padBottom(20).center().row(); contentTable.add(gameOverResultMessageLabel).padBottom(10).center().row(); contentTable.add(gameOverEloChangeLabel).padBottom(30).center().row(); contentTable.add(gameOverBackButton).width(200).height(40); gameOverBackButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { gameOverDialog.hide(); if(controller != null) controller.handleGameOverClosed(); } }); }
    private void setupErrorDialog() { errorDialog = new Dialog("Error", skin, "dialog") { @Override protected void result(Object object) { if (Boolean.TRUE.equals(object) && controller != null) { controller.handleReturnToMenu(); } } }; errorDialog.setModal(true); errorDialog.setMovable(false); errorDialog.getContentTable().clearChildren(); errorDialog.text("An unexpected error occurred.").pad(20); errorDialog.button("Back to Menu", true); }
    private void addDrawResignListeners() { resignButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { if (!resignButton.isDisabled() && controller != null) controller.handleResignButtonClicked(); } }); offerDrawButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { if (!offerDrawButton.isDisabled() && controller != null) controller.handleOfferDrawClicked(); } }); }

    // --- UI Update Methods Called by Controller ---
    @Override public void setPlayerInfoText(String text) { if (playerInfoLabel != null) playerInfoLabel.setText(text); }
    @Override public void setPlayerDeckText(String text) { if (playerDeckLabel != null) playerDeckLabel.setText("Your Deck: "+text); }
    @Override public void setOpponentInfoText(String text) { if (opponentInfoLabel != null) opponentInfoLabel.setText("Opponent Deck: "+text); }
    @Override public void setStatusText(String text, boolean isError) { if (statusLabel != null) { statusLabel.setText(text); statusLabel.setColor(isError ? Color.RED : Color.WHITE); } }
    @Override public void setResignButtonEnabled(boolean enabled) { if (resignButton != null) resignButton.setDisabled(!enabled); }
    @Override public void setOfferDrawButtonEnabled(boolean enabled) { if (offerDrawButton != null) offerDrawButton.setDisabled(!enabled); }
    @Override public void showDrawOfferReceived(boolean visible) { if (drawOfferTable != null) drawOfferTable.setVisible(visible); if (visible && offerDrawButton != null) offerDrawButton.setDisabled(true); }
    @Override public void setPlayer1Timer(String time) { if (player1TimerLabel != null) { player1TimerLabel.setText("P1: " + (time != null ? time : "--:--")); } }
    @Override public void setPlayer2Timer(String time) { if (player2TimerLabel != null) { player2TimerLabel.setText("P2: " + (time != null ? time : "--:--")); } }
    @Override public void showOpponentDisconnected(boolean disconnected, String timeUntilTimeout) { if (opponentStatusLabel != null) { if (disconnected) { opponentStatusLabel.setText("Opponent disconnected" + (timeUntilTimeout != null ? " (" + timeUntilTimeout + ")" : "")); opponentStatusLabel.setVisible(true); } else { opponentStatusLabel.setVisible(false); } } }
    @Override public void showConnectionStatus(boolean connected, boolean reconnecting) { if (connectionStatusLabel != null) { if (connected) { connectionStatusLabel.setText("Connected"); connectionStatusLabel.setColor(Color.GREEN); } else if (reconnecting) { connectionStatusLabel.setText("Reconnecting..."); connectionStatusLabel.setColor(Color.ORANGE); } else { connectionStatusLabel.setText("Disconnected"); connectionStatusLabel.setColor(Color.RED); } } }
    @Override public void showGameOverOverlay(String title, String message, String eloChange) { Gdx.app.log("GameplayScreen", "Attempting to show Game Over Overlay... Title: " + title); if (gameOverDialog == null || stage == null) { Gdx.app.error("GameplayScreen", "Dialog or Stage is NULL when trying to show Game Over!"); return; } Gdx.input.setInputProcessor(stage); gameOverTitleLabel.setText(title != null ? title : "Game Over"); gameOverResultMessageLabel.setText(message != null ? message : ""); gameOverEloChangeLabel.setText(eloChange != null ? eloChange : ""); gameOverDialog.pack(); if (gameOverDialog.getStage() == null) { stage.addActor(gameOverDialog); Gdx.app.log("GameplayScreen", "Added gameOverDialog to stage."); } gameOverDialog.show(stage); gameOverDialog.setPosition( Math.round((stage.getWidth() - gameOverDialog.getWidth()) / 2f), Math.round((stage.getHeight() - gameOverDialog.getHeight()) / 2f) ); Gdx.app.log("GameplayScreen", "Game Over Overlay shown/updated. Visible: " + gameOverDialog.isVisible()); gameOverDialog.toFront(); }
    @Override public void showErrorDialog(String message) { if (errorDialog == null) setupErrorDialog(); Table contentTable = errorDialog.getContentTable(); contentTable.clearChildren(); contentTable.add(new Label(message != null ? message : "Error", skin)).pad(20); errorDialog.pack(); Gdx.input.setInputProcessor(stage); if (errorDialog.getStage() == null) { stage.addActor(errorDialog); } if (!errorDialog.isVisible()) { errorDialog.show(stage); errorDialog.setPosition( Math.round((stage.getWidth() - errorDialog.getWidth()) / 2f), Math.round((stage.getHeight() - errorDialog.getHeight()) / 2f) ); } errorDialog.toFront(); Gdx.app.log("GameplayScreen", "Error dialog shown."); }

    // --- LibGDX Screen Methods ---
    @Override public void render(float delta) { Gdx.gl.glClearColor(0.2f, 0.1f, 0.3f, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); if (controller != null) { controller.update(delta); } stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f)); stage.draw(); }
    @Override public void resize(int width, int height) { Gdx.app.log("GameplayScreen", "Resizing to " + width + "x" + height); stage.getViewport().update(width, height, true); calculateBoardLayout(); }
    @Override public void hide() { Gdx.app.log("GameplayScreen", "hide() called."); }
    @Override public void dispose() {
        Gdx.app.log("GameplayScreen", "Disposing...");
        if (controller != null) { controller.dispose(); }
        if (stage != null) stage.dispose();
        if (boardTexture != null) boardTexture.dispose();
        if (highlightTexture != null) highlightTexture.dispose();
        if (spellSlotTexture != null) spellSlotTexture.dispose();
        if (pieceTextures != null) { for (Texture t : pieceTextures.values()) { if (t != null) t.dispose(); } pieceTextures.clear(); }
        if (spellTextures != null) { for (Texture t : spellTextures.values()) { if (t != null) t.dispose(); } spellTextures.clear(); }
        if (connectedIconTexture != null) connectedIconTexture.dispose();
        if (disconnectedIconTexture != null) disconnectedIconTexture.dispose();
        Gdx.app.log("GameplayScreen", "Dispose complete.");
    }
}