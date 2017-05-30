package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;

/**
 * Temporary deeplink receiver for magiclinks. Routes to the proper activity based on the login feature flag. Will be
 *  removed after the feature flag gets removed.
 */
public class LoginMagicLinkInterceptActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(getIntent());
        if (AppPrefs.isLoginWizardStyleActivated()) {
            intent.setClass(this, WPMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            intent.setClass(this, SignInActivity.class);
        }
        startActivity(intent);

        finish();
    }
}
