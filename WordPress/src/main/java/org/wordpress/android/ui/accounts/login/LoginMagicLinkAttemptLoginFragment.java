package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class LoginMagicLinkAttemptLoginFragment extends Fragment {
    public static final String TAG = "login_magic_link_attempt_login_fragment_tag";

    private static final String KEY_STATE_NAME = "KEY_STATE_NAME";

    private static final String ARG_TOKEN = "arg_token";

    private enum State {
        INIT,
        AUTH,
        AUTH_ERROR,
        ACCOUNT,
        ACCOUNT_ERROR,
        ACCOUNT_SETTINGS,
        ACCOUNT_SETTINGS_ERROR,
        SITES,
        SITES_ERROR,
        DONE;

        public boolean isBeyond(State state) {
            return this.ordinal() > state.ordinal();
        }
    }

    private View mLoginChecklist;
    private View mProgressAuth;
    private View mDoneAuth;
    private View mErrorAuth;
    private View mProgressAcc;
    private View mDoneAcc;
    private View mErrorAcc;
    private View mProgressAccSettings;
    private View mDoneAccSettings;
    private View mErrorAccSettings;
    private View mProgressSites;
    private View mDoneSites;
    private View mErrorSites;
    private TextView mErrorLabel;
    private View mRestartLogin;

    private String mToken;
    private State mState;

    private LoginListener mLoginListener;

    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;

    public static LoginMagicLinkAttemptLoginFragment newInstance(String token) {
        LoginMagicLinkAttemptLoginFragment fragment = new LoginMagicLinkAttemptLoginFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TOKEN, token);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (getArguments() != null) {
            mToken = getArguments().getString(ARG_TOKEN);
        }

        if (savedInstanceState == null) {
            mState = State.INIT;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_magic_link_attempt_login_fragment, container, false);

        mLoginChecklist = view.findViewById(R.id.login_checklist);
        mProgressAuth = view.findViewById(R.id.progress_bar_auth);
        mDoneAuth = view.findViewById(R.id.done_auth);
        mErrorAuth = view.findViewById(R.id.err_auth);
        mProgressAcc = view.findViewById(R.id.progress_bar_acc);
        mDoneAcc = view.findViewById(R.id.done_acc);
        mErrorAcc = view.findViewById(R.id.err_acc);
        mProgressAccSettings = view.findViewById(R.id.progress_bar_acc_settings);
        mDoneAccSettings = view.findViewById(R.id.done_acc_settings);
        mErrorAccSettings = view.findViewById(R.id.err_acc_settings);
        mProgressSites = view.findViewById(R.id.progress_bar_sites);
        mDoneSites = view.findViewById(R.id.done_sites);
        mErrorSites = view.findViewById(R.id.err_sites);

        mRestartLogin = view.findViewById(R.id.login_restart);
        mRestartLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    mLoginListener.restartLogin();
                }
            }
        });

        mErrorLabel = (TextView) view.findViewById(R.id.error_label);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mState = State.valueOf(savedInstanceState.getString(KEY_STATE_NAME));
        }

        updateProgressViews();
    }

    @Override
    public void onStart() {
        super.onStart();

        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        mDispatcher.unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mState == State.INIT) {
            gotoState(State.AUTH);
            attemptLoginWithMagicLink();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginListener) {
            mLoginListener = (LoginListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
    }

    private void updateProgressViews() {
        mProgressAuth.setVisibility(mState == State.AUTH ? View.VISIBLE : View.INVISIBLE);
        mDoneAuth.setVisibility(mState.isBeyond(State.AUTH_ERROR) ? View.VISIBLE : View.INVISIBLE);
        mErrorAuth.setVisibility(mState == State.AUTH_ERROR ? View.VISIBLE : View.INVISIBLE);

        mProgressAcc.setVisibility(mState == State.ACCOUNT ? View.VISIBLE : View.INVISIBLE);
        mDoneAcc.setVisibility(mState.isBeyond(State.ACCOUNT_ERROR) ? View.VISIBLE : View.INVISIBLE);
        mErrorAcc.setVisibility(mState == State.ACCOUNT_ERROR ? View.VISIBLE : View.INVISIBLE);

        mProgressAccSettings.setVisibility(mState == State.ACCOUNT_SETTINGS ? View.VISIBLE : View.INVISIBLE);
        mDoneAccSettings.setVisibility(mState.isBeyond(State.ACCOUNT_SETTINGS_ERROR) ? View.VISIBLE : View.INVISIBLE);
        mErrorAccSettings.setVisibility(mState == State.ACCOUNT_SETTINGS_ERROR ? View.VISIBLE : View.INVISIBLE);

        mProgressSites.setVisibility(mState == State.SITES ? View.VISIBLE : View.INVISIBLE);
        mDoneSites.setVisibility(mState.isBeyond(State.SITES_ERROR) ? View.VISIBLE : View.INVISIBLE);
        mErrorSites.setVisibility(mState == State.SITES_ERROR ? View.VISIBLE : View.INVISIBLE);

        boolean erred = mState == State.AUTH_ERROR
                || mState == State.ACCOUNT_ERROR
                || mState == State.ACCOUNT_SETTINGS_ERROR
                || mState == State.SITES_ERROR;

        mErrorLabel.setVisibility(erred ? View.VISIBLE : View.GONE);
        mRestartLogin.setVisibility(erred ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_STATE_NAME, mState.name());
    }

    private void gotoState(State state) {
        mState = state;
        updateProgressViews();
    }

    public void attemptLoginWithMagicLink() {
        // Save Token to the AccountStore. This will trigger a onAuthenticationChanged.
        AccountStore.UpdateTokenPayload payload = new AccountStore.UpdateTokenPayload(mToken);
        mDispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);

            gotoState(State.AUTH_ERROR);

            if (isAdded()) {
                mErrorLabel.setText(event.error.message);
            }

            return;
        }

        // fetch user account
        gotoState(State.ACCOUNT);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (mState == State.ACCOUNT) {
            boolean accountFetched = event.causeOfChange == AccountAction.FETCH_ACCOUNT;
            if (!accountFetched || event.isError()) {
                AppLog.e(T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);

                gotoState(State.ACCOUNT_ERROR);

                if (isAdded()) {
                    mErrorLabel.setText(R.string.error_fetch_my_profile);
                }

                return;
            }

            // fetch user sites
            gotoState(State.ACCOUNT_SETTINGS);
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());

            return;
        }

        if (mState == State.ACCOUNT_SETTINGS) {
            boolean accountSettingsFetched = event.causeOfChange == AccountAction.FETCH_SETTINGS;
            if (!accountSettingsFetched || event.isError()) {
                AppLog.e(T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);

                gotoState(State.ACCOUNT_SETTINGS_ERROR);

                if (isAdded()) {
                    mErrorLabel.setText(R.string.error_fetch_account_settings);
                }

                return;
            }

            // fetch user sites
            gotoState(State.SITES);
            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

            return;
        }

        // we should never get to this point!

        AppLog.e(T.API, "onAccountChanged called while on the wrong state! State: " + mState.name());
        if (isAdded()) {
            mLoginChecklist.setVisibility(View.GONE);
            mErrorLabel.setText(R.string.error_generic);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (mState.isBeyond(State.SITES)) {
            // ignore if we're already received this even before.
            //  Happens because updating the site emits another onSiteChanged.
            return;
        }

        if (event.isError()) {
            AppLog.e(T.API, "onSiteChanged has error: " + event.error.type + " - " + event.error.toString());
            if (!isAdded() || event.error.type != SiteErrorType.DUPLICATE_SITE) {
                return;
            }

            if (event.rowsAffected == 0) {
                // If there is a duplicate site and not any site has been added, show an error and
                // stop the sign in process
                gotoState(State.SITES_ERROR);
                mErrorLabel.setText(R.string.cannot_add_duplicate_site);
                return;
            } else {
                // If there is a duplicate site, notify the user something could be wrong,
                // but continue the sign in process
                ToastUtils.showToast(getContext(), R.string.duplicate_site_detected);
            }
        }

        gotoState(State.DONE);

        // Start Notification service
        NotificationsUpdateService.startService(getActivity().getApplicationContext());

        if (mLoginListener != null) {
            mLoginListener.loggedInViaMagicLink();
        }
    }

}
