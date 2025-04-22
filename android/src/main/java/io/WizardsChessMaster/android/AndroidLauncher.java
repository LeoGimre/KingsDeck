package io.WizardsChessMaster.android;

import android.content.Intent;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import io.WizardsChessMaster.Main;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {

    private AndroidFirebaseService androidFirebaseController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        androidFirebaseController = new AndroidFirebaseService(this);

        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;

        initialize(new Main(androidFirebaseController), configuration);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (androidFirebaseController != null) {
            androidFirebaseController.handleActivityResult(requestCode, resultCode, data);
        }
    }
}