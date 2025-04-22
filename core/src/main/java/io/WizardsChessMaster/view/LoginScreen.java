package io.WizardsChessMaster.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.presenter.LoginPresenter;
import io.WizardsChessMaster.view.interfaces.ILoginView;

// Implements the interface
public class LoginScreen extends ScreenAdapter implements ILoginView {

    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final LoginPresenter controller;
    private Label statusLabel;

    public LoginScreen(final Main game, FirebaseService firebaseService, Skin sharedSkin) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        if (sharedSkin == null) {
            Gdx.app.error("LoginScreen", "Error: Shared skin is null!");
            // Handle this error appropriately - maybe load a fallback or throw exception
            throw new RuntimeException("Shared skin cannot be null for LoginScreen");
        }
        this.skin = sharedSkin;

        this.controller = new LoginPresenter(game, firebaseService, this);

        setupUi();
    }

    private void setupUi() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        statusLabel = new Label("Please log in", skin);
        TextButton loginButton = new TextButton("Sign in with Google", skin);

        table.add(statusLabel).padBottom(20).colspan(1).center();
        table.row();
        table.add(loginButton).width(300).height(50);

        loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (controller != null) {
                    // Pass an AuthListener specific to this view's feedback needs
                    controller.handleGoogleLogin(new FirebaseService.AuthListener() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                        }
                    });
                }
            }
        });
    }

    // --- ILoginView Implementation ---

    @Override
    public void setStatusMessage(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message != null ? message : "");
            statusLabel.setColor(isError ? Color.RED : Color.WHITE);
        }
    }

    // --- Screen Lifecycle Methods ---

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
    public void show() {
        Gdx.input.setInputProcessor(stage);
        setStatusMessage("Please log in", false);
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