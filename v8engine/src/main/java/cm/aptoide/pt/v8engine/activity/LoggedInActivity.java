/*
 * Copyright (c) 2017.
 * Modified by Marcelo Benites on 09/02/2017.
 */

package cm.aptoide.pt.v8engine.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.accountmanager.AptoideLoginUtils;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.repository.IdsRepositoryImpl;
import cm.aptoide.pt.dataprovider.ws.v7.SetUserRequest;
import cm.aptoide.pt.interfaces.AptoideClientUUID;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.utils.GenericDialogs;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.V8Engine;
import com.jakewharton.rxbinding.view.RxView;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by pedroribeiro on 15/12/16.
 */

public class LoggedInActivity extends AccountBaseActivity {

  private static final String TAG = LoggedInActivity.class.getSimpleName();

  private AptoideClientUUID aptoideClientUUID;
  private AptoideAccountManager accountManager;

  private Toolbar mToolbar;
  private Button mContinueButton;
  private Button mMoreInfoButton;
  private CompositeSubscription mSubscriptions;
  private ProgressDialog pleaseWaitDialog;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(getLayoutId());
    aptoideClientUUID =
        new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
            DataProvider.getContext());
    accountManager =
        ((V8Engine)getApplicationContext()).getAccountManager();
    mSubscriptions = new CompositeSubscription();
    bindViews();
    setupToolbar();
    setupListeners();
  }

  @Override public String getActivityTitle() {
    return getString(cm.aptoide.accountmanager.R.string.create_profile_logged_in_activity_title);
  }

  @Override public int getLayoutId() {
    return cm.aptoide.accountmanager.R.layout.logged_in_first_screen;
  }

  private void bindViews() {
    mContinueButton = (Button) findViewById(cm.aptoide.accountmanager.R.id.logged_in_continue);
    mMoreInfoButton = (Button) findViewById(cm.aptoide.accountmanager.R.id.logged_in_more_info_button);
    mToolbar = (Toolbar) findViewById(cm.aptoide.accountmanager.R.id.toolbar);
  }

  private void setupToolbar() {
    setSupportActionBar(mToolbar);
    getSupportActionBar().setTitle(getActivityTitle());
  }

  private void setupListeners() {
    mSubscriptions.add(RxView.clicks(mContinueButton).subscribe(clicks -> {

      pleaseWaitDialog = GenericDialogs.createGenericPleaseWaitDialog(this,
          getApplicationContext().getString(cm.aptoide.accountmanager.R.string.please_wait));
      pleaseWaitDialog.show();

      SetUserRequest.of(aptoideClientUUID.getAptoideClientUUID(), UserAccessState.PUBLIC.toString(),
          accountManager.getAccessToken()).execute(answer -> {
        if (answer.isOk()) {
          Logger.v(TAG, "user is public");
          ShowMessage.asSnack(this, cm.aptoide.accountmanager.R.string.successful);
        } else {
          Logger.v(TAG, "user is public: error: " + answer.getError().getDescription());
          ShowMessage.asSnack(this, cm.aptoide.accountmanager.R.string.unknown_error);
        }
        goTo();
      }, throwable -> {
        goTo();
      });
    }));
    mSubscriptions.add(RxView.clicks(mMoreInfoButton).subscribe(clicks -> {
      startActivity(getIntent().setClass(this, LoggedInActivity2ndStep.class));
      finish();
    }));
  }

  private void goTo() {

    if (getIntent() != null && getIntent().getBooleanExtra(AptoideLoginUtils.IS_FACEBOOK_OR_GOOGLE,
        false)) {
      updateUserInfo();
    } else {
      if (pleaseWaitDialog != null && pleaseWaitDialog.isShowing()) {
        pleaseWaitDialog.dismiss();
      }
      startActivity(getIntent().setClass(this, CreateStoreActivity.class));
      finish();
    }
  }

  private void updateUserInfo() {
    accountManager.refreshAccount().subscribe(() -> {
      if (pleaseWaitDialog != null && pleaseWaitDialog.isShowing()) {
        pleaseWaitDialog.dismiss();
      }
      finish();
    }, throwable -> throwable.printStackTrace());
  }
}


