package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.accounts.login.LoginEmailFragment;
import org.wordpress.android.ui.accounts.login.LoginListener;
import org.wordpress.android.ui.accounts.login.LoginMagicLinkAttemptLoginFragment;
import org.wordpress.android.ui.accounts.login.LoginMagicLinkRequestFragment;
import org.wordpress.android.ui.accounts.login.LoginMagicLinkSentFragment;
import org.wordpress.android.ui.accounts.login.LoginPrologueFragment;
import org.wordpress.android.util.ToastUtils;

public class LoginActivity extends AppCompatActivity implements LoginListener {
    public static final String MAGIC_LOGIN = "magic-login";
    public static final String TOKEN_PARAMETER = "token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.login_activity);

        if (savedInstanceState == null) {
            if (hasMagicLinkLoginIntent()) {
                addMagicLinkLoginAttemptFragment();
            } else {
                addLoginPrologueFragment();
            }
        }
    }

    private void addLoginPrologueFragment() {
        LoginPrologueFragment loginSignupFragment = new LoginPrologueFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginSignupFragment, LoginPrologueFragment.TAG);
        fragmentTransaction.commit();
    }

    private void addMagicLinkLoginAttemptFragment() {
        String token = getIntent().getData().getQueryParameter(TOKEN_PARAMETER);
        slideInFragment(LoginMagicLinkAttemptLoginFragment.newInstance(token), false,
                LoginMagicLinkAttemptLoginFragment.TAG);
    }

    private void slideInFragment(Fragment fragment, boolean shouldAddToBackStack, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return false;
    }

    private boolean hasMagicLinkLoginIntent() {
        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        return Intent.ACTION_VIEW.equals(action) && uri != null && uri.getHost().contains(MAGIC_LOGIN);
    }


    // LoginListener implementation methods

    @Override
    public void showEmailLoginScreen() {
        LoginEmailFragment loginEmailFragment = new LoginEmailFragment();
        slideInFragment(loginEmailFragment, true, LoginEmailFragment.TAG);
    }

    @Override
    public void doStartSignup() {
        ToastUtils.showToast(this, "Signup is not implemented yet");
    }

    @Override
    public void showMagicLinkRequestScreen(String email) {
        LoginMagicLinkRequestFragment loginMagicLinkRequestFragment = LoginMagicLinkRequestFragment.newInstance(email);
        slideInFragment(loginMagicLinkRequestFragment, true, LoginMagicLinkRequestFragment.TAG);
    }

    @Override
    public void loginViaUsernamePassword() {
        ToastUtils.showToast(this, "Fall back to username/password is not implemented yet.");
    }

    @Override
    public void showMagicLinkSentScreen(String email) {
        LoginMagicLinkSentFragment loginMagicLinkSentFragment = LoginMagicLinkSentFragment.newInstance(email);
        slideInFragment(loginMagicLinkSentFragment, true, LoginMagicLinkSentFragment.TAG);
    }

    @Override
    public void openEmailClient() {
        ToastUtils.showToast(this, "Open email client is not implemented yet.");
    }

    @Override
    public void usePasswordInstead(String email) {
        ToastUtils.showToast(this, "Fall back to password is not implemented yet. Email: " + email);
    }

    @Override
    public void gotSiteAddress(String siteAddress) {
        ToastUtils.showToast(this, "Input site address is not implemented yet. Input site address: " + siteAddress);
    }

    @Override
    public void restartLogin() {
        addLoginPrologueFragment();
    }

    @Override
    public void loggedInViaMagicLink() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_SUCCEEDED);

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void help() {
        ToastUtils.showToast(this, "Help is not implemented yet.");
    }
}
