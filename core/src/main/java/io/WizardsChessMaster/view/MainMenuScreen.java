package io.WizardsChessMaster.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.presenter.MainMenuPresenter;
import io.WizardsChessMaster.view.interfaces.IMainMenuView;

// Implements the interface
public class MainMenuScreen extends ScreenAdapter implements IMainMenuView {

    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final MainMenuPresenter controller;
    private Label statusLabel;
    private Texture logoTexture;

    public MainMenuScreen(final Main game, FirebaseService firebaseService, Skin sharedSkin) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        if (sharedSkin == null) {
            Gdx.app.error("MainMenuScreen", "Error: Shared skin is null!");
            throw new RuntimeException("Shared skin cannot be null for MainMenuScreen");
        }
        this.skin = sharedSkin;

        this.controller = new MainMenuPresenter(game, firebaseService, this);
        setupUi();
    }

    private void setupUi() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // add logo
        logoTexture = new Texture(Gdx.files.internal("KingsDeckLogo.png"));
        Image logoImage = new Image(logoTexture);
        logoImage.setScaling(Scaling.fit); // Keeps aspect ratio if resized
        table.add(logoImage).width(700).height(350).padBottom(40).colspan(1).center();

        // add rest
        TextButton playButton = new TextButton("Play Game", skin);
        TextButton buildDecksButton = new TextButton("Build Decks", skin);
        TextButton tutorialButton = new TextButton("Tutorial", skin);
        TextButton profileButton = new TextButton("Profile", skin);
        TextButton signOutButton = new TextButton("Sign Out", skin);
        statusLabel = new Label("", skin);
        statusLabel.setAlignment(Align.center);

        float buttonWidth = 300;
        float buttonHeight = 50;
        float padBottom = 15;

        table.row();
        table.add(playButton).width(buttonWidth).height(buttonHeight).padBottom(padBottom);
        table.row();
        table.add(buildDecksButton).width(buttonWidth).height(buttonHeight).padBottom(padBottom);
        table.row();
        table.add(profileButton).width(buttonWidth).height(buttonHeight).padBottom(padBottom);
        table.row();
        table.add(tutorialButton).width(buttonWidth).height(buttonHeight).padBottom(padBottom);
        table.row();
        table.add(signOutButton).width(buttonWidth).height(buttonHeight).padTop(30);
        table.row();
        table.add(statusLabel).colspan(1).center().padTop(10).growX();


        // --- Controller Interactions ---
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller != null) controller.handlePlayGame();
            }
        });

        buildDecksButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller != null) controller.handleBuildDecks();
            }
        });

        profileButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller != null) controller.handleProfile();
            }
        });

        tutorialButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller != null) controller.handleTutorial();
            }
        });


        signOutButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller != null) {
                    controller.handleSignOut(new FirebaseService.AuthListener() {
                        @Override
                        public void onSuccess() { }
                        @Override
                        public void onFailure(String errorMessage) {
                        }
                    });
                }
            }
        });

    }

    // --- IMainMenuView Implementation ---

    @Override
    public void showStatusMessage(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.getActions().clear();

            statusLabel.setText(message != null ? message : "");
            statusLabel.setColor(isError ? Color.RED : Color.LIME);
            statusLabel.setVisible(true);
            statusLabel.getColor().a = 1f;
        }
    }


    // --- Screen Lifecycle Methods ---

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        showStatusMessage("", false);
    }
    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }
    @Override
    public void dispose() {
        stage.dispose();
        if (logoTexture != null) {
            logoTexture.dispose();
        }
    }
}