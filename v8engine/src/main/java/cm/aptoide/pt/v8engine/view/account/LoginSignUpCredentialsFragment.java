package cm.aptoide.pt.v8engine.view.account;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.GenericDialogs;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.account.LoginPreferences;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.presenter.LoginSignUpCredentialsPresenter;
import cm.aptoide.pt.v8engine.presenter.LoginSignUpCredentialsView;
import cm.aptoide.pt.v8engine.view.ThrowableToStringMapper;
import cm.aptoide.pt.v8engine.view.account.user.CreateUserFragment;
import cm.aptoide.pt.v8engine.view.navigator.FragmentNavigator;
import cm.aptoide.pt.v8engine.view.store.home.HomeFragment;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxrelay.PublishRelay;
import com.trello.rxlifecycle.android.FragmentEvent;
import java.util.Arrays;
import java.util.List;
import rx.Observable;

public class LoginSignUpCredentialsFragment extends GoogleLoginFragment
    implements LoginSignUpCredentialsView {

  private static final String DISMISS_TO_NAVIGATE_TO_MAIN_VIEW = "dismiss_to_navigate_to_main_view";
  private static final String CLEAN_BACK_STACK = "clean_back_stack";

  private static final String USERNAME_KEY = "username_key";
  private static final String PASSWORD_KEY = "password_key";

  private ProgressDialog progressDialog;
  private CallbackManager callbackManager;
  private PublishRelay<FacebookAccountViewModel> facebookLoginSubject;
  private LoginManager facebookLoginManager;
  private AlertDialog facebookEmailRequiredDialog;
  private Button googleLoginButton;
  private View facebookLoginButton;
  private Button hideShowAptoidePasswordButton;
  private View loginArea;
  private View signUpArea;
  private EditText aptoideEmailEditText;
  private EditText aptoidePasswordEditText;
  private TextView forgotPasswordButton;
  private Button buttonSignUp;
  private Button buttonLogin;
  private View loginSignupSelectionArea;
  private Button loginSelectionButton;
  private Button signUpSelectionButton;
  private TextView termsAndConditions;
  private View separator;

  private boolean isPasswordVisible = false;
  private View credentialsEditTextsArea;
  private BottomSheetBehavior<View> bottomSheetBehavior;
  private ThrowableToStringMapper errorMapper;
  private LoginSignUpCredentialsPresenter presenter;
  private List<String> facebookRequestedPermissions;
  private FragmentNavigator fragmentNavigator;

  public static LoginSignUpCredentialsFragment newInstance(boolean dismissToNavigateToMainView,
      boolean cleanBackStack) {
    final LoginSignUpCredentialsFragment fragment = new LoginSignUpCredentialsFragment();

    final Bundle bundle = new Bundle();
    bundle.putBoolean(DISMISS_TO_NAVIGATE_TO_MAIN_VIEW, dismissToNavigateToMainView);
    bundle.putBoolean(CLEAN_BACK_STACK, cleanBackStack);
    fragment.setArguments(bundle);

    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    errorMapper = new AccountErrorMapper(getContext());
    facebookRequestedPermissions = Arrays.asList("email", "user_friends");
    fragmentNavigator = getFragmentNavigator();
    presenter = new LoginSignUpCredentialsPresenter(this,
        ((V8Engine) getContext().getApplicationContext()).getAccountManager(),
        facebookRequestedPermissions,
        new LoginPreferences(getContext(), V8Engine.getConfiguration(),
            GoogleApiAvailability.getInstance()),
        getArguments().getBoolean(DISMISS_TO_NAVIGATE_TO_MAIN_VIEW),
        getArguments().getBoolean(CLEAN_BACK_STACK));
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(USERNAME_KEY, aptoideEmailEditText.getText()
        .toString());
    outState.putString(PASSWORD_KEY, aptoidePasswordEditText.getText()
        .toString());
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_login_sign_up_credentials, container, false);
  }

  @Override public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);

    if (savedInstanceState != null) {
      aptoideEmailEditText.setText(savedInstanceState.getString(USERNAME_KEY, ""));
      aptoidePasswordEditText.setText(savedInstanceState.getString(PASSWORD_KEY, ""));
    }
  }

  @Override public Observable<Void> showAptoideLoginAreaClick() {
    return RxView.clicks(loginSelectionButton);
  }

  @Override public Observable<Void> showAptoideSignUpAreaClick() {
    return RxView.clicks(signUpSelectionButton);
  }

  @Override public void showAptoideSignUpArea() {
    setAptoideSignUpLoginAreaVisible();
    loginArea.setVisibility(View.GONE);
    signUpArea.setVisibility(View.VISIBLE);
    separator.setVisibility(View.GONE);
    termsAndConditions.setVisibility(View.VISIBLE);
  }

  @Override public void showAptoideLoginArea() {
    setAptoideSignUpLoginAreaVisible();
    loginArea.setVisibility(View.VISIBLE);
    signUpArea.setVisibility(View.GONE);
    separator.setVisibility(View.GONE);
    termsAndConditions.setVisibility(View.GONE);
  }

  @Override public void showLoading() {
    progressDialog.show();
  }

  @Override public void hideLoading() {
    progressDialog.dismiss();
  }

  @Override public void showError(Throwable throwable) {
    // FIXME: 23/2/2017 sithengineer find a better solution than this.
    ShowMessage.asToast(getContext(), errorMapper.map(throwable));
  }

  @Override public void showFacebookLogin() {
    facebookLoginButton.setVisibility(View.VISIBLE);
    facebookLoginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
      @Override public void onSuccess(LoginResult loginResult) {
        facebookLoginSubject.call(new FacebookAccountViewModel(loginResult.getAccessToken(),
            loginResult.getRecentlyDeniedPermissions()));
      }

      @Override public void onCancel() {
        showFacebookLoginError(R.string.facebook_login_cancelled);
        Analytics.Account.loginStatus(Analytics.Account.LoginMethod.FACEBOOK,
            Analytics.Account.SignUpLoginStatus.FAILED, Analytics.Account.LoginStatusDetail.CANCEL);
      }

      @Override public void onError(FacebookException error) {
        Analytics.Account.loginStatus(Analytics.Account.LoginMethod.FACEBOOK,
            Analytics.Account.SignUpLoginStatus.FAILED,
            Analytics.Account.LoginStatusDetail.SDK_ERROR);
        showFacebookLoginError(R.string.error_occured);
      }
    });
  }

  @Override public void showPermissionsRequiredMessage() {
    facebookEmailRequiredDialog.show();
  }

  @Override public void hideFacebookLogin() {
    facebookLoginButton.setVisibility(View.GONE);
  }

  @Override public void navigateToForgotPasswordView() {
    // FIXME remove hardcoded links
    Uri mobilePageUri = Uri.parse("http://m.aptoide.com/account/password-recovery");
    startActivity(new Intent(Intent.ACTION_VIEW, mobilePageUri));
  }

  @Override public void showPassword() {
    isPasswordVisible = true;
    aptoidePasswordEditText.setTransformationMethod(null);
    hideShowAptoidePasswordButton.setBackgroundResource(R.drawable.icon_open_eye);
  }

  @Override public void hidePassword() {
    isPasswordVisible = false;
    aptoidePasswordEditText.setTransformationMethod(new PasswordTransformationMethod());
    hideShowAptoidePasswordButton.setBackgroundResource(R.drawable.icon_closed_eye);
  }

  @Override public Observable<Void> showHidePasswordClick() {
    return RxView.clicks(hideShowAptoidePasswordButton);
  }

  @Override public Observable<Void> forgotPasswordClick() {
    return RxView.clicks(forgotPasswordButton);
  }

  @Override public void navigateToMainView() {
    Fragment home = HomeFragment.newInstance(V8Engine.getConfiguration()
        .getDefaultStore(), StoreContext.home, V8Engine.getConfiguration()
        .getDefaultTheme());
    fragmentNavigator.cleanBackStack();
    fragmentNavigator.navigateTo(home);
  }

  @Override public void goBack() {
    fragmentNavigator.popBackStack();
  }

  @Override public void dismiss() {
    getActivity().finish();
  }

  @Override public void hideKeyboard() {
    AptoideUtils.SystemU.hideKeyboard(getActivity());
  }

  @Override public Observable<FacebookAccountViewModel> facebookLoginClick() {
    return facebookLoginSubject.doOnNext(
        __ -> Analytics.Account.clickIn(Analytics.Account.StartupClick.CONNECT_FACEBOOK,
            getStartupClickOrigin()));
  }

  @Override public Observable<AptoideAccountViewModel> aptoideLoginClick() {
    return RxView.clicks(buttonLogin)
        .doOnNext(__ -> Analytics.Account.clickIn(Analytics.Account.StartupClick.LOGIN,
            getStartupClickOrigin()))
        .map(click -> getCredentials());
  }

  @Override public Observable<AptoideAccountViewModel> aptoideSignUpClick() {
    return RxView.clicks(buttonSignUp)
        .doOnNext(__ -> Analytics.Account.clickIn(Analytics.Account.StartupClick.JOIN_APTOIDE,
            getStartupClickOrigin()))
        .map(click -> getCredentials());
  }

  @Override public boolean tryCloseLoginBottomSheet() {
    if (credentialsEditTextsArea.getVisibility() == View.VISIBLE) {
      credentialsEditTextsArea.setVisibility(View.GONE);
      loginSignupSelectionArea.setVisibility(View.VISIBLE);
      loginArea.setVisibility(View.GONE);
      signUpArea.setVisibility(View.GONE);
      separator.setVisibility(View.VISIBLE);
      termsAndConditions.setVisibility(View.VISIBLE);
      return true;
    }
    return false;
  }

  @Override @NonNull public AptoideAccountViewModel getCredentials() {
    return new AptoideAccountViewModel(aptoideEmailEditText.getText()
        .toString(), aptoidePasswordEditText.getText()
        .toString());
  }

  @Override public void setCredentials(@NonNull AptoideAccountViewModel model) {
    aptoideEmailEditText.setText(model.getUsername());
    aptoidePasswordEditText.setText(model.getPassword());
  }

  @Override public boolean isPasswordVisible() {
    return isPasswordVisible;
  }

  @Override public void navigateToCreateProfile() {
    getFragmentNavigator().cleanBackStack();
    getFragmentNavigator().navigateTo(CreateUserFragment.newInstance());
  }

  @Override public Context getApplicationContext() {
    return getActivity().getApplicationContext();
  }

  @Override public void lockScreenRotation() {
    int orientation;
    int rotation =
        ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
            .getRotation();
    switch (rotation) {
      default:
      case Surface.ROTATION_0:
        orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        break;
      case Surface.ROTATION_90:
        orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        break;
      case Surface.ROTATION_180:
        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        break;
      case Surface.ROTATION_270:
        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        break;
    }

    getActivity().setRequestedOrientation(orientation);
  }

  @Override public void unlockScreenRotation() {
    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
  }

  private Analytics.Account.StartupClickOrigin getStartupClickOrigin() {
    if (loginArea.getVisibility() == View.VISIBLE) {
      return Analytics.Account.StartupClickOrigin.LOGIN_UP;
    } else if (signUpArea.getVisibility() == View.VISIBLE) {
      return Analytics.Account.StartupClickOrigin.JOIN_UP;
    } else {
      return Analytics.Account.StartupClickOrigin.MAIN;
    }
  }

  private void showFacebookLoginError(@StringRes int errorRes) {
    ShowMessage.asToast(getContext(), errorRes);
  }

  private void setAptoideSignUpLoginAreaVisible() {
    credentialsEditTextsArea.setVisibility(View.VISIBLE);
    loginSignupSelectionArea.setVisibility(View.GONE);
    if (bottomSheetBehavior != null) {
      bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
  }

  @Override public void googleLoginClicked() {
    Analytics.Account.clickIn(Analytics.Account.StartupClick.CONNECT_GOOGLE,
        getStartupClickOrigin());
  }

  @Override protected Button getGoogleButton() {
    return googleLoginButton;
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    callbackManager.onActivityResult(requestCode, resultCode, data);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bindViews(view);
    attachPresenter(presenter, savedInstanceState);
    registerBackClickHandler(presenter);
  }

  @Override protected void showGoogleLoginError() {
    ShowMessage.asToast(getContext(), R.string.google_login_cancelled);
  }

  private void bindViews(View view) {
    forgotPasswordButton = (TextView) view.findViewById(R.id.forgot_password);

    googleLoginButton = (Button) view.findViewById(R.id.google_login_button);

    buttonLogin = (Button) view.findViewById(R.id.button_login);
    buttonSignUp = (Button) view.findViewById(R.id.button_sign_up);
    buttonSignUp.setText(String.format(getString(R.string.join_company), getCompanyName()));

    aptoideEmailEditText = (EditText) view.findViewById(R.id.username);
    aptoidePasswordEditText = (EditText) view.findViewById(R.id.password);
    hideShowAptoidePasswordButton = (Button) view.findViewById(R.id.btn_show_hide_pass);

    facebookLoginButton = view.findViewById(R.id.fb_login_button);
    RxView.clicks(facebookLoginButton)
        .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
        .subscribe(
            __ -> facebookLoginManager.logInWithReadPermissions(LoginSignUpCredentialsFragment.this,
                facebookRequestedPermissions));

    callbackManager = CallbackManager.Factory.create();
    facebookLoginManager = LoginManager.getInstance();
    facebookLoginSubject = PublishRelay.create();

    loginSignupSelectionArea = view.findViewById(R.id.login_signup_selection_layout);
    credentialsEditTextsArea = view.findViewById(R.id.credentials_edit_texts);
    signUpSelectionButton = (Button) view.findViewById(R.id.show_join_aptoide_area);
    loginSelectionButton = (Button) view.findViewById(R.id.show_login_with_aptoide_area);
    signUpSelectionButton.setText(
        String.format(getString(R.string.join_company), getCompanyName()));
    loginArea = view.findViewById(R.id.login_button_area);
    signUpArea = view.findViewById(R.id.sign_up_button_area);
    termsAndConditions = (TextView) view.findViewById(R.id.terms_and_conditions);
    separator = view.findViewById(R.id.separator);

    final Context context = getContext();

    facebookEmailRequiredDialog = new AlertDialog.Builder(context).setMessage(
        R.string.facebook_email_permission_regected_message)
        .setPositiveButton(R.string.facebook_grant_permission_button, (dialog, which) -> {
          facebookLoginManager.logInWithReadPermissions(this, Arrays.asList("email"));
        })
        .setNegativeButton(android.R.string.cancel, null)
        .create();

    progressDialog = GenericDialogs.createGenericPleaseWaitDialog(context);

    try {
      bottomSheetBehavior = BottomSheetBehavior.from(view.getRootView()
          .findViewById(R.id.login_signup_layout));
    } catch (IllegalArgumentException ex) {
      // this happens because in landscape the R.id.login_signup_layout is not
      // a child of CoordinatorLayout
    }
  }

  public String getCompanyName() {
    return ((V8Engine) getActivity().getApplication()).createConfiguration()
        .getMarketName();
  }

  @Override public void onDestroyView() {
    unregisterBackClickHandler(presenter);
    unlockScreenRotation();
    super.onDestroyView();
  }
}
