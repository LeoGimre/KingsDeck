package io.WizardsChessMaster.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.config.ConfigLoader;
import io.WizardsChessMaster.config.GameSettings;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.presenter.MatchmakingPresenter;
import io.WizardsChessMaster.view.interfaces.IMatchmakingView;

import java.util.List;
import java.util.Objects;

public class MatchmakingScreen extends ScreenAdapter implements IMatchmakingView {

    private static final String TAG = "MatchmakingScreen";

    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final MatchmakingPresenter controller;
    private final GameSettings settings;

    // --- UI Elements ---
    private Label titleLabel;
    private Label statusLabel;
    private ProgressBar searchIndicator;
    private TextButton backButton;

    // --- Tab Controls ---
    private ButtonGroup<TextButton> tabGroup;
    private TextButton rankedTabButton;
    private TextButton hostTabButton;
    private TextButton joinTabButton;
    private Table rankedTabContent;
    private Table hostTabContent;
    private Table joinTabContent;
    private Stack contentStack;

    // --- Ranked Tab Elements ---
    private Label rankedPointLimitLabel;
    private SelectBox<Integer> rankedPointLimitSelectBox;
    private Label rankedTimeLimitLabel;
    private SelectBox<String> rankedTimeLimitSelectBox;
    private Label rankedDeckLabel;
    private SelectBox<String> rankedDeckSelectBox;
    private TextButton findMatchButton;
    private TextButton cancelRankedButton;

    // --- Host Tab Elements ---
    private Label hostPointLimitLabel;
    private SelectBox<Integer> hostPointLimitSelectBox;
    private Label hostTimeLimitLabel;
    private SelectBox<String> hostTimeLimitSelectBox;
    private Label hostDeckLabel;
    private SelectBox<String> hostDeckSelectBox;
    private TextButton hostGameButton;
    private Label hostCodeLabel;
    private TextButton cancelHostButton;

    // --- Join Tab Elements ---
    private Label joinDeckLabel;
    private SelectBox<String> joinDeckSelectBox;
    private Label joinCodeInputLabel;
    private TextField joinCodeTextField;
    private TextButton joinGameButton;
    private TextButton cancelJoinButton;

    private IMatchmakingView.MatchmakingMode currentMode = MatchmakingMode.RANKED;

    public MatchmakingScreen(Main game, FirebaseService firebaseService, Skin sharedSkin) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        this.settings = ConfigLoader.getSettings();

        if (sharedSkin == null) {
            Gdx.app.error(TAG, "Error: Shared skin is null!");
            throw new RuntimeException("Shared skin cannot be null for MatchmakingScreen");
        }
        this.skin = sharedSkin;

        this.controller = new MatchmakingPresenter(game, firebaseService, this);
        setupUi();
        addListeners();
        showTab(MatchmakingMode.RANKED);
    }

    // UI Setup
    private void setupUi() {
        Table mainTable = new Table(skin);
        mainTable.setFillParent(true);
        mainTable.pad(10);
        stage.addActor(mainTable);

        titleLabel = new Label("Matchmaking", skin, "subtitle");
        mainTable.add(titleLabel).colspan(3).padBottom(10).center().row();

        // --- Tab Buttons ---
        rankedTabButton = new TextButton("Ranked", skin, "toggle");
        hostTabButton = new TextButton("Host Game", skin, "toggle");
        joinTabButton = new TextButton("Join Game", skin, "toggle");
        tabGroup = new ButtonGroup<>(rankedTabButton, hostTabButton, joinTabButton);
        tabGroup.setMaxCheckCount(1);
        tabGroup.setMinCheckCount(1);
        tabGroup.setChecked("Ranked");

        Table tabsTable = new Table();
        tabsTable.defaults().pad(5).growX();
        tabsTable.add(rankedTabButton);
        tabsTable.add(hostTabButton);
        tabsTable.add(joinTabButton);
        mainTable.add(tabsTable).colspan(3).fillX().padBottom(15).row();

        // --- Content Area ---
        buildRankedTabContent();
        buildHostTabContent();
        buildJoinTabContent();

        contentStack = new Stack();
        contentStack.add(rankedTabContent);
        contentStack.add(hostTabContent);
        contentStack.add(joinTabContent);
        mainTable.add(contentStack).colspan(3).grow().row();

        // --- Bottom Controls (Status, Indicator, Back) ---
        searchIndicator = new ProgressBar(0f, 1f, 0.1f, false, skin);
        searchIndicator.setAnimateDuration(0.5f);
        searchIndicator.setValue(0.5f);
        searchIndicator.setVisible(false);
        mainTable.add(searchIndicator).colspan(3).width(300).padTop(10).padBottom(5).row();

        statusLabel = new Label("", skin);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);
        mainTable.add(statusLabel).colspan(3).minHeight(40).growX().padBottom(10).row();

        backButton = new TextButton("Back to Menu", skin);
        mainTable.add(backButton).colspan(3).width(200).height(40).padTop(10);
    }

    // UI Content Builders
    private void buildRankedTabContent() {
        rankedTabContent = new Table(skin);
        rankedTabContent.pad(15);

        rankedPointLimitLabel = new Label("Point Limit:", skin);
        rankedPointLimitSelectBox = new SelectBox<>(skin);
        rankedPointLimitSelectBox.setItems(new Array<>(settings.matchmaking.pointLimits.toArray(new Integer[0])));

        rankedTimeLimitLabel = new Label("Time Limit:", skin);
        rankedTimeLimitSelectBox = new SelectBox<>(skin);
        rankedTimeLimitSelectBox.setItems(new Array<>(settings.matchmaking.timeLimits.toArray(new String[0])));
        if (settings.matchmaking.timeLimits.contains(settings.matchmaking.defaultTimeLimit)) {
            rankedTimeLimitSelectBox.setSelected(settings.matchmaking.defaultTimeLimit);
        }

        rankedDeckLabel = new Label("Deck:", skin);
        rankedDeckSelectBox = new SelectBox<>(skin);
        rankedDeckSelectBox.setItems("Select point limit");

        findMatchButton = new TextButton("Find Ranked Match", skin);
        cancelRankedButton = new TextButton("Cancel Search", skin);
        cancelRankedButton.setVisible(false);

        rankedTabContent.add(rankedPointLimitLabel).align(Align.right).padRight(10);
        rankedTabContent.add(rankedPointLimitSelectBox).width(200).align(Align.left);
        rankedTabContent.row().padTop(15);
        rankedTabContent.add(rankedTimeLimitLabel).align(Align.right).padRight(10);
        rankedTabContent.add(rankedTimeLimitSelectBox).width(200).align(Align.left);
        rankedTabContent.row().padTop(15);
        rankedTabContent.add(rankedDeckLabel).align(Align.right).padRight(10);
        rankedTabContent.add(rankedDeckSelectBox).width(200).align(Align.left);
        rankedTabContent.row().padTop(30);
        rankedTabContent.add(findMatchButton).colspan(2).width(250).height(50).padBottom(10);
        rankedTabContent.row();
        rankedTabContent.add(cancelRankedButton).colspan(2).width(200).height(40).padTop(5);
        rankedTabContent.row();
    }

    private void buildHostTabContent() {
        hostTabContent = new Table(skin);
        hostTabContent.pad(15);

        hostPointLimitLabel = new Label("Point Limit:", skin);
        hostPointLimitSelectBox = new SelectBox<>(skin);
        hostPointLimitSelectBox.setItems(new Array<>(settings.matchmaking.pointLimits.toArray(new Integer[0])));


        hostTimeLimitLabel = new Label("Time Limit:", skin);
        hostTimeLimitSelectBox = new SelectBox<>(skin);
        hostTimeLimitSelectBox.setItems(new Array<>(settings.matchmaking.timeLimits.toArray(new String[0])));
        if (settings.matchmaking.timeLimits.contains(settings.matchmaking.defaultTimeLimit)) {
            hostTimeLimitSelectBox.setSelected(settings.matchmaking.defaultTimeLimit);
        }

        hostDeckLabel = new Label("Deck:", skin);
        hostDeckSelectBox = new SelectBox<>(skin);
        hostDeckSelectBox.setItems("Select point limit");

        hostGameButton = new TextButton("Host Game", skin);
        hostCodeLabel = new Label("Code: ---", skin, "subtitle");
        hostCodeLabel.setAlignment(Align.center);
        hostCodeLabel.setVisible(false);

        cancelHostButton = new TextButton("Cancel Hosting", skin);
        cancelHostButton.setVisible(false);

        hostTabContent.add(hostPointLimitLabel).align(Align.right).padRight(10);
        hostTabContent.add(hostPointLimitSelectBox).width(200).align(Align.left);
        hostTabContent.row().padTop(15);
        hostTabContent.add(hostTimeLimitLabel).align(Align.right).padRight(10);
        hostTabContent.add(hostTimeLimitSelectBox).width(200).align(Align.left);
        hostTabContent.row().padTop(15);
        hostTabContent.add(hostDeckLabel).align(Align.right).padRight(10);
        hostTabContent.add(hostDeckSelectBox).width(200).align(Align.left);
        hostTabContent.row().padTop(30);
        hostTabContent.add(hostGameButton).colspan(2).width(250).height(50).padBottom(10);
        hostTabContent.row();
        hostTabContent.add(hostCodeLabel).colspan(2).padTop(10).padBottom(10);
        hostTabContent.row();
        hostTabContent.add(cancelHostButton).colspan(2).width(200).height(40).padTop(5);
        hostTabContent.row();
    }

    private void buildJoinTabContent() {
        joinTabContent = new Table(skin);
        joinTabContent.pad(15);

        joinDeckLabel = new Label("Your Deck:", skin);
        joinDeckSelectBox = new SelectBox<>(skin);
        joinDeckSelectBox.setItems("Load decks first...");

        joinCodeInputLabel = new Label("Enter Code:", skin);
        joinCodeTextField = new TextField("", skin);
        joinCodeTextField.setMessageText("ABCDEF");
        joinCodeTextField.setMaxLength(6);
        joinCodeTextField.setTextFieldFilter(new TextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField textField, char c) {
                return "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789".indexOf(Character.toUpperCase(c)) != -1;
            }
        });

        joinGameButton = new TextButton("Join Game", skin);
        cancelJoinButton = new TextButton("Cancel Join", skin);
        cancelJoinButton.setVisible(false);

        joinTabContent.add(joinDeckLabel).align(Align.right).padRight(10);
        joinTabContent.add(joinDeckSelectBox).width(200).align(Align.left);
        joinTabContent.row().padTop(15);
        joinTabContent.add(joinCodeInputLabel).align(Align.right).padRight(10);
        joinTabContent.add(joinCodeTextField).width(150).align(Align.left);
        joinTabContent.row().padTop(15);
        joinTabContent.add(joinGameButton).colspan(2).width(250).height(50).padTop(15).padBottom(10);
        joinTabContent.row();
        joinTabContent.add(cancelJoinButton).colspan(2).width(200).height(40).padTop(5);
        joinTabContent.row();
    }

    // Listeners Setup
    private void addListeners() {
        // Tab Button Listeners
        rankedTabButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { if (rankedTabButton.isChecked()) showTab(MatchmakingMode.RANKED); }
        });
        hostTabButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { if (hostTabButton.isChecked()) showTab(MatchmakingMode.HOST); }
        });
        joinTabButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { if (joinTabButton.isChecked()) showTab(MatchmakingMode.JOIN); }
        });

        // Ranked Tab Listeners
        rankedPointLimitSelectBox.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handlePointLimitChanged(rankedPointLimitSelectBox.getSelected()); } });
        rankedTimeLimitSelectBox.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handleTimeLimitChanged(rankedTimeLimitSelectBox.getSelected()); } });
        rankedDeckSelectBox.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handleDeckChanged(rankedDeckSelectBox.getSelected()); } });
        findMatchButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (!findMatchButton.isDisabled() && controller != null) controller.handleFindRankedMatch(); } });
        cancelRankedButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handleCancel(MatchmakingMode.RANKED); } });

        // Host Tab Listeners
        hostPointLimitSelectBox.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handlePointLimitChanged(hostPointLimitSelectBox.getSelected()); } });
        hostTimeLimitSelectBox.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handleTimeLimitChanged(hostTimeLimitSelectBox.getSelected()); } });
        hostDeckSelectBox.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handleDeckChanged(hostDeckSelectBox.getSelected()); } });
        hostGameButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (!hostGameButton.isDisabled() && controller != null) controller.handleHostGame(); } });
        cancelHostButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handleCancel(MatchmakingMode.HOST); } });

        // Join Tab Listeners
        joinDeckSelectBox.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handleDeckChanged(joinDeckSelectBox.getSelected()); } });
        joinCodeTextField.setTextFieldListener((textField, key) -> { int cursor = textField.getCursorPosition(); textField.setText(textField.getText().toUpperCase()); textField.setCursorPosition(cursor); });
        joinGameButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (!joinGameButton.isDisabled() && controller != null) controller.handleJoinGameByCode(); } });
        cancelJoinButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handleCancel(MatchmakingMode.JOIN); } });

        // General Listeners
        backButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { if (controller != null) controller.handleBack(); } });
    }

    // --- IMatchmakingView Implementation ---
    @Override public void updateDeckList(List<String> deckNames, String placeholder) { Array<String> items = new Array<>(); boolean hasDecks = deckNames != null && !deckNames.isEmpty(); if (hasDecks) { items.addAll(deckNames.toArray(new String[0])); } else { items.add(placeholder != null ? placeholder : "No valid decks"); } SelectBox<String> targetBox = null; switch (currentMode) { case RANKED: targetBox = rankedDeckSelectBox; break; case HOST: targetBox = hostDeckSelectBox; break; case JOIN: targetBox = joinDeckSelectBox; if (!hasDecks) items.set(0, "Create a deck first"); break; } if (targetBox != null) { String currentSelection = targetBox.getSelected(); targetBox.setItems(items); targetBox.setDisabled(!hasDecks && currentMode != MatchmakingMode.JOIN); if (currentSelection != null && items.contains(currentSelection, false)) { targetBox.setSelected(currentSelection); } else { targetBox.setSelectedIndex(0); if (controller != null && targetBox.getSelected() != null && !Objects.equals(targetBox.getSelected(), currentSelection)) { controller.handleDeckChanged(targetBox.getSelected()); } } } }
    @Override public void clearDeckSelection() { SelectBox<String> targetBox = null; switch (currentMode) { case RANKED: targetBox = rankedDeckSelectBox; break; case HOST: targetBox = hostDeckSelectBox; break; case JOIN: targetBox = joinDeckSelectBox; break; } if (targetBox != null && targetBox.getItems().size > 0) { targetBox.setSelectedIndex(0); } }
    @Override public void updateActionButtonState(MatchmakingMode mode, boolean enabled) { TextButton targetButton = null; switch (mode) { case RANKED: targetButton = findMatchButton; break; case HOST: targetButton = hostGameButton; break; case JOIN: targetButton = joinGameButton; break; } if (targetButton != null) { targetButton.setDisabled(!enabled); } }
    @Override public void updateCancelButtonState(MatchmakingMode mode, boolean enabled, boolean visible) { TextButton targetButton = null; switch (mode) { case RANKED: targetButton = cancelRankedButton; break; case HOST: targetButton = cancelHostButton; break; case JOIN: targetButton = cancelJoinButton; break; } if (targetButton != null) { targetButton.setVisible(visible); targetButton.setDisabled(!enabled); if (targetButton.getParent() instanceof Table) { ((Table)targetButton.getParent()).invalidateHierarchy(); } } }
    @Override public void setStatusMessage(String message, boolean isError) { if (statusLabel != null) { statusLabel.setText(message != null ? message : ""); statusLabel.setColor(isError ? Color.RED : Color.LIME); if (message != null && !message.isEmpty()) { Gdx.app.log(TAG, "Status: " + message); } } }
    @Override public void showSearchingIndicator(boolean show) { if (searchIndicator != null) { searchIndicator.setVisible(show); } }
    @Override public void disableControls(boolean disable) { rankedPointLimitSelectBox.setDisabled(disable); rankedTimeLimitSelectBox.setDisabled(disable); rankedDeckSelectBox.setDisabled(disable || rankedDeckSelectBox.getItems().size <= 1); findMatchButton.setDisabled(disable || !controller.areMatchParametersSelected(MatchmakingMode.RANKED)); hostPointLimitSelectBox.setDisabled(disable); hostTimeLimitSelectBox.setDisabled(disable); hostDeckSelectBox.setDisabled(disable || hostDeckSelectBox.getItems().size <= 1); hostGameButton.setDisabled(disable || !controller.areMatchParametersSelected(MatchmakingMode.HOST)); joinDeckSelectBox.setDisabled(disable || joinDeckSelectBox.getItems().size <= 1); joinCodeTextField.setDisabled(disable); joinGameButton.setDisabled(disable || !controller.areMatchParametersSelected(MatchmakingMode.JOIN)); rankedTabButton.setDisabled(disable); hostTabButton.setDisabled(disable); joinTabButton.setDisabled(disable); backButton.setDisabled(disable); }
    @Override public String getCurrentStatusMessage() { return statusLabel != null ? statusLabel.getText().toString() : ""; }
    @Override public void showTab(MatchmakingMode mode) { Gdx.app.log(TAG, "Switching to tab: " + mode); currentMode = mode; switch (mode) { case RANKED: rankedTabButton.setChecked(true); break; case HOST: hostTabButton.setChecked(true); break; case JOIN: joinTabButton.setChecked(true); break; } rankedTabContent.setVisible(mode == MatchmakingMode.RANKED); hostTabContent.setVisible(mode == MatchmakingMode.HOST); joinTabContent.setVisible(mode == MatchmakingMode.JOIN); setStatusMessage("", false); if (controller != null) { controller.handleTabSwitched(mode); controller.updateDeckListForCurrentMode(); controller.updateButtonStatesForCurrentMode(); } clearJoinCodeInput(); displayHostCode(null); }
    @Override public void displayHostCode(String code) { if (hostCodeLabel != null) { if (code != null && !code.isEmpty()) { hostCodeLabel.setText("Code: " + code); hostCodeLabel.setVisible(true); } else { hostCodeLabel.setText("Code: ---"); hostCodeLabel.setVisible(false); } if (hostTabContent != null) hostTabContent.invalidateHierarchy(); } }
    @Override public String getJoinCodeInput() { return joinCodeTextField != null ? joinCodeTextField.getText().toUpperCase().trim() : ""; }
    @Override public void clearJoinCodeInput() { if (joinCodeTextField != null) { joinCodeTextField.setText(""); } }

    // --- Screen Lifecycle Methods ---
    @Override public void show() { Gdx.input.setInputProcessor(stage); setStatusMessage("", false); showSearchingIndicator(false); disableControls(false); showTab(MatchmakingMode.RANKED); if (controller != null) { controller.initialize(); } }
    @Override public void render(float delta) { Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f)); stage.draw(); }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void hide() { if (controller != null) { controller.cleanupMatchmakingAttempt(); } Gdx.input.setInputProcessor(null); }
    @Override public void dispose() { Gdx.app.log(TAG, "Disposing screen"); if (controller != null) { controller.dispose(); } stage.dispose();
    }
}