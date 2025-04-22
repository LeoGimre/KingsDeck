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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.presenter.ProfilePresenter;
import io.WizardsChessMaster.model.MatchHistoryEntry;
import io.WizardsChessMaster.model.UserModel;
import io.WizardsChessMaster.view.interfaces.IProfileView;

import java.util.List;
import java.util.Locale;

public class ProfileScreen extends ScreenAdapter implements IProfileView {

    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final ProfilePresenter controller;
    private final UserModel userModel;

    // Profile Info UI
    private Label nameLabel;
    private Label eloLabel;
    private Label gamesPlayedLabel;
    private Label winRateLabel;
    private Label statusLabel;

    // Match History UI Elements
    private Table matchHistoryTable;
    private ScrollPane matchHistoryScrollPane;

    public ProfileScreen(Main game, FirebaseService firebaseService, Skin sharedSkin) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        if (sharedSkin == null) {
            Gdx.app.error("ProfileScreen", "Error: Shared skin is null!");
            throw new RuntimeException("Shared skin cannot be null for ProfileScreen");
        }
        this.skin = sharedSkin;

        this.userModel = new UserModel();
        this.controller = new ProfilePresenter(game, firebaseService, userModel, this);

        setupUi();
    }

    private void setupUi() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(15);
        stage.addActor(mainTable);

        // --- Title ---
        Label titleLabel = new Label("Player Profile", skin, "subtitle");
        mainTable.add(titleLabel).colspan(2).center().padBottom(15).row();

        // --- Status ---
        statusLabel = new Label("Loading...", skin);
        mainTable.add(statusLabel).colspan(2).center().padBottom(15).row();

        // --- Profile Info Table ---
        Table profileInfoTable = new Table(skin);
        profileInfoTable.defaults().padBottom(8);

        nameLabel = new Label("Name: -", skin);
        eloLabel = new Label("ELO: -", skin);
        gamesPlayedLabel = new Label("Games Played: -", skin);
        winRateLabel = new Label("Win Rate: -", skin);

        profileInfoTable.add(new Label("Username:", skin)).align(Align.right).padRight(10);
        profileInfoTable.add(nameLabel).align(Align.left).expandX().fillX();
        profileInfoTable.row();
        profileInfoTable.add(new Label("ELO Rating:", skin)).align(Align.right).padRight(10);
        profileInfoTable.add(eloLabel).align(Align.left);
        profileInfoTable.row();
        profileInfoTable.add(new Label("Games Played:", skin)).align(Align.right).padRight(10);
        profileInfoTable.add(gamesPlayedLabel).align(Align.left);
        profileInfoTable.row();
        profileInfoTable.add(new Label("Win Rate:", skin)).align(Align.right).padRight(10);
        profileInfoTable.add(winRateLabel).align(Align.left);

        // Add profile info table to main layout
        mainTable.add(profileInfoTable).colspan(2).fillX().padBottom(25).row();

        // Match History Setup
        Label historyTitleLabel = new Label("Match History", skin, "subtitle");
        mainTable.add(historyTitleLabel).colspan(2).center().padBottom(10).row();

        matchHistoryTable = new Table(skin);
        matchHistoryTable.top();
        matchHistoryScrollPane = new ScrollPane(matchHistoryTable, skin);
        matchHistoryScrollPane.setFadeScrollBars(false);
        matchHistoryScrollPane.setScrollingDisabled(true, false);

        // Add Headers to Match History Table
        float colPad = 10f;
        matchHistoryTable.add(new Label("Opponent", skin)).expandX().fillX().align(Align.left).padLeft(colPad).padRight(colPad);
        matchHistoryTable.add(new Label("Result", skin)).width(70).align(Align.center).padRight(colPad);
        matchHistoryTable.add(new Label("ELO +/-", skin)).width(80).align(Align.center).padRight(colPad);
        matchHistoryTable.row().padTop(3).padBottom(3);

        Image separator = new Image(skin.newDrawable("white", Color.GRAY));
        matchHistoryTable.add(separator).height(1).colspan(3).fillX().padBottom(5);
        matchHistoryTable.row();

        // Add the scroll pane to the main layout
        mainTable.add(matchHistoryScrollPane).colspan(2).grow().padBottom(25).row();

        // --- Back Button ---
        TextButton backButton = new TextButton("Back to Menu", skin);
        mainTable.add(backButton).colspan(2).width(200).height(50).center().padTop(10);

        // --- Listeners ---
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller != null) {
                    controller.handleBackToMenu();
                }
            }
        });
    }

    // --- IProfileView Implementation ---

    @Override
    public void updateUI() {
        // Reads data directly from the shared userModel instance.
        if (userModel != null && userModel.getUserId() != null) {
            nameLabel.setText(userModel.getDisplayName() != null ? userModel.getDisplayName() : "-");
            eloLabel.setText(String.valueOf(userModel.getEloRating()));
            gamesPlayedLabel.setText(String.valueOf(userModel.getGamesPlayed()));

            if (userModel.getGamesPlayed() > 0) {
                float winRate = (float) userModel.getGamesWon() / userModel.getGamesPlayed() * 100;
                winRateLabel.setText(String.format(Locale.US, "%.1f%%", winRate));
            } else {
                winRateLabel.setText("N/A");
            }
        } else {
            // Update UI to reflect error/no data state
            nameLabel.setText("-");
            eloLabel.setText("-");
            gamesPlayedLabel.setText("-");
            winRateLabel.setText("-");
        }
    }

    @Override
    public void showStatus(String message, boolean isError) {
        statusLabel.setText(message != null ? message : "");
        statusLabel.setColor(isError ? Color.RED : Color.LIME);
    }

    @Override
    public void updateMatchHistory(List<MatchHistoryEntry> history, boolean isLoadingError) {
        matchHistoryTable.clearChildren();

        // Re-add Headers
        float colPad = 10f;
        matchHistoryTable.add(new Label("Opponent", skin)).expandX().fillX().align(Align.left).padLeft(colPad).padRight(colPad);
        matchHistoryTable.add(new Label("Result", skin)).width(70).align(Align.center).padRight(colPad);
        matchHistoryTable.add(new Label("ELO +/-", skin)).width(80).align(Align.center).padRight(colPad);
        matchHistoryTable.row().padTop(3).padBottom(3);
        Image separator = new Image(skin.newDrawable("white", Color.GRAY));
        matchHistoryTable.add(separator).height(1).colspan(3).fillX().padBottom(5);
        matchHistoryTable.row();

        if (isLoadingError) {
            Label errorLabel = new Label("Error loading match history.", skin);
            errorLabel.setColor(Color.RED);
            matchHistoryTable.add(errorLabel).colspan(3).center().pad(20);
        } else if (history == null || history.isEmpty()) {
            Label emptyLabel = new Label("No matches played yet.", skin);
            matchHistoryTable.add(emptyLabel).colspan(3).center().pad(20);
        } else {
            for (MatchHistoryEntry entry : history) {
                Label opponentLabel = new Label(entry.getOpponentDisplayName(), skin);
                opponentLabel.setEllipsis("...");

                Label resultLabel = new Label(entry.getResult(), skin);
                if ("Win".equalsIgnoreCase(entry.getResult())) {
                    resultLabel.setColor(Color.GREEN);
                } else if ("Loss".equalsIgnoreCase(entry.getResult())) {
                    resultLabel.setColor(Color.RED);
                } else {
                    resultLabel.setColor(Color.LIGHT_GRAY);
                }

                Label eloLabelHist = new Label(entry.getEloChange(), skin);
                try {
                    if (!entry.getEloChange().equals("-") && !entry.getEloChange().equals("N/A")) {
                        int change = Integer.parseInt(entry.getEloChange().replace("+", ""));
                        if (change > 0) eloLabelHist.setColor(Color.GREEN);
                        else if (change < 0) eloLabelHist.setColor(Color.RED);
                        else eloLabelHist.setColor(Color.LIGHT_GRAY);
                    } else {
                        eloLabelHist.setColor(Color.LIGHT_GRAY);
                    }
                } catch (NumberFormatException e) {
                    eloLabelHist.setColor(Color.LIGHT_GRAY);
                }

                matchHistoryTable.add(opponentLabel).expandX().fillX().align(Align.left).padLeft(colPad).padRight(colPad);
                matchHistoryTable.add(resultLabel).align(Align.center).padRight(colPad);
                matchHistoryTable.add(eloLabelHist).align(Align.center).padRight(colPad);
                matchHistoryTable.row().padTop(4).padBottom(4);
            }
        }
        matchHistoryTable.invalidateHierarchy();
        matchHistoryScrollPane.layout();
        Gdx.app.postRunnable(() -> matchHistoryScrollPane.setScrollY(0));
    }

    // --- Screen Lifecycle Methods ---

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        // Trigger profile loading AND history loading when the screen is shown
        if (controller != null) {
            controller.loadUserProfile();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}