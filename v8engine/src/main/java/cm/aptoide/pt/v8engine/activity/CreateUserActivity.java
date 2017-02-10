/*
 * Copyright (c) 2017.
 * Modified by Marcelo Benites on 09/02/2017.
 */

package cm.aptoide.pt.v8engine.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import cm.aptoide.accountmanager.AccountManagerPreferences;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.accountmanager.AptoideLoginUtils;
import cm.aptoide.accountmanager.ws.CreateUserRequest;
import cm.aptoide.accountmanager.ws.ErrorsMapper;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.repository.IdsRepositoryImpl;
import cm.aptoide.pt.imageloader.ImageLoader;
import cm.aptoide.pt.interfaces.AptoideClientUUID;
import cm.aptoide.pt.preferences.Application;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.FileUtils;
import cm.aptoide.pt.utils.GenericDialogs;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.V8Engine;
import com.jakewharton.rxbinding.view.RxView;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by pedroribeiro on 24/11/16.
 */

public class CreateUserActivity extends AccountPermissionsBaseActivity
    implements AptoideAccountManager.ICreateProfile {

  private static final String TYPE_STORAGE = "storage";
  private static final String TYPE_CAMERA = "camera";
  private static int CREATE_USER_REQUEST_CODE = 0; //1:Username and Avatar 2: Username

  private final AptoideClientUUID aptoideClientUUID;

  private String userEmail;
  private String userPassword;
  private String username;
  private String avatarPath;
  private String accessToken;
  private Boolean UPDATE = true;
  private String SIGNUP = "signup";
  private Toolbar mToolbar;
  private RelativeLayout mUserAvatar;
  private EditText mUsername;
  private Button mCreateButton;
  private ImageView mAvatar;
  private View content;
  private CompositeSubscription mSubscriptions;
  private Boolean result = false;
  private String ERROR_TAG = "Error update user";
  private AptoideAccountManager accountManager;

  public CreateUserActivity() {
    this.aptoideClientUUID = new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
        DataProvider.getContext());
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(getLayoutId());
    accountManager =
        ((V8Engine)getApplicationContext()).getAccountManager();
    mSubscriptions = new CompositeSubscription();
    bindViews();
    getUserData();
    setupToolbar();
    setupListeners();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mSubscriptions.clear();
  }

  @Override public String getActivityTitle() {
    return getString(cm.aptoide.accountmanager.R.string.create_user_title);
  }

  @Override public int getLayoutId() {
    return cm.aptoide.accountmanager.R.layout.activity_create_user;
  }

  @Override public void showIconPropertiesError(String errors) {
    mSubscriptions.add(GenericDialogs.createGenericOkMessage(this,
        getString(cm.aptoide.accountmanager.R.string.image_requirements_error_popup_title), errors).subscribe());
  }

  @Override public void loadImage(Uri imagePath) {
    ImageLoader.loadWithCircleTransform(imagePath, mAvatar);
  }

  private void bindViews() {
    mToolbar = (Toolbar) findViewById(cm.aptoide.accountmanager.R.id.toolbar);
    mUserAvatar = (RelativeLayout) findViewById(cm.aptoide.accountmanager.R.id.create_user_image_action);
    mUsername = (EditText) findViewById(cm.aptoide.accountmanager.R.id.create_user_username_inserted);
    mCreateButton = (Button) findViewById(cm.aptoide.accountmanager.R.id.create_user_create_profile);
    mAvatar = (ImageView) findViewById(cm.aptoide.accountmanager.R.id.create_user_image);
    content = findViewById(android.R.id.content);
  }

  private void getUserData() {
    userEmail = getIntent().getStringExtra(AptoideLoginUtils.APTOIDE_LOGIN_USER_NAME_KEY);
    userPassword = getIntent().getStringExtra(AptoideLoginUtils.APTOIDE_LOGIN_PASSWORD_KEY);
    accessToken = getIntent().getStringExtra(AptoideLoginUtils.APTOIDE_LOGIN_ACCESS_TOKEN_KEY);
  }

  private void setupToolbar() {
    if (mToolbar != null) {
      setSupportActionBar(mToolbar);
      getSupportActionBar().setHomeButtonEnabled(true);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle(getActivityTitle());
    }
  }

  private void setupListeners() {
    mSubscriptions.add(RxView.clicks(mUserAvatar).subscribe(click -> chooseAvatarSource()));
    mSubscriptions.add(RxView.clicks(mCreateButton).subscribe(click -> {
      AptoideUtils.SystemU.hideKeyboard(this);
      username = mUsername.getText().toString().trim();
      validateProfileData();
      if (CREATE_USER_REQUEST_CODE == 1) {
        ProgressDialog pleaseWaitDialog = GenericDialogs.createGenericPleaseWaitDialog(this,
            getApplicationContext().getString(cm.aptoide.accountmanager.R.string.please_wait_upload));
        pleaseWaitDialog.show();
        mSubscriptions.add(
            CreateUserRequest.of("true", userEmail, username, userPassword, avatarPath,
                aptoideClientUUID.getAptoideClientUUID(), this, accountManager)
                .observe()
                .filter(answer -> {
                  if (answer.hasErrors()) {
                    if (answer.getErrors() != null && answer.getErrors().size() > 0) {
                      onRegisterFail(ErrorsMapper.getWebServiceErrorMessageFromCode(
                          answer.getErrors().get(0).code));
                      pleaseWaitDialog.dismiss();
                    } else {
                      onRegisterFail(cm.aptoide.accountmanager.R.string.unknown_error);
                      pleaseWaitDialog.dismiss();
                    }
                    return false;
                  }
                  return true;
                })
                .timeout(90, TimeUnit.SECONDS)
                .subscribe(answer -> {
                  //Successfull update
                  saveUserDataOnPreferences();
                  onRegisterSuccess(pleaseWaitDialog);
                  pleaseWaitDialog.dismiss();
                }, err -> {
                  if (err.getClass().equals(SocketTimeoutException.class)) {
                    pleaseWaitDialog.dismiss();
                    ShowMessage.asObservableSnack(this, cm.aptoide.accountmanager.R.string.user_upload_photo_failed)
                        .subscribe(visibility -> {
                          if (visibility == ShowMessage.DISMISSED) {
                            finish();
                          }
                        });
                  } else if (err.getClass().equals(TimeoutException.class)) {
                    pleaseWaitDialog.dismiss();
                    ShowMessage.asObservableSnack(this, cm.aptoide.accountmanager.R.string.user_upload_photo_failed)
                        .subscribe(visibility -> {
                          if (visibility == ShowMessage.DISMISSED) {
                            finish();
                          }
                        });
                  }
                }));
      } else if (CREATE_USER_REQUEST_CODE == 2) {
        avatarPath = "";
        ProgressDialog pleaseWaitDialog = GenericDialogs.createGenericPleaseWaitDialog(this,
            getApplicationContext().getString(cm.aptoide.accountmanager.R.string.please_wait));
        pleaseWaitDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pleaseWaitDialog.show();
        CreateUserRequest.of("true", userEmail, username, userPassword, avatarPath,
            aptoideClientUUID.getAptoideClientUUID(), this, accountManager).execute(answer -> {
          if (answer.hasErrors()) {
            if (answer.getErrors() != null && answer.getErrors().size() > 0) {
              onRegisterFail(
                  ErrorsMapper.getWebServiceErrorMessageFromCode(answer.getErrors().get(0).code));
              pleaseWaitDialog.dismiss();
            } else {
              onRegisterFail(cm.aptoide.accountmanager.R.string.unknown_error);
              pleaseWaitDialog.dismiss();
            }
          } else {
            //Successfull update
            saveUserDataOnPreferences();
            onRegisterSuccess(pleaseWaitDialog);
            pleaseWaitDialog.dismiss();
          }
        });
      }
    }));
  }

  public int validateProfileData() {
    if (getUserUsername().length() != 0) {
      if (getUserAvatar().length() != 0) {
        CREATE_USER_REQUEST_CODE = 1;
      } else if (getUserAvatar().length() == 0) {
        CREATE_USER_REQUEST_CODE = 2;
      }
    } else {
      CREATE_USER_REQUEST_CODE = 0;
      onRegisterFail(cm.aptoide.accountmanager.R.string.nothing_inserted_user);
    }
    return CREATE_USER_REQUEST_CODE;
  }

  private void saveUserDataOnPreferences() {
    AccountManagerPreferences.setUserAvatar(avatarPath);
    AccountManagerPreferences.setUserNickName(username);
  }

  @Override public void onRegisterSuccess(ProgressDialog progressDialog) {
    ShowMessage.asSnack(content, cm.aptoide.accountmanager.R.string.user_created);
    //data.putString(AptoideLoginUtils.APTOIDE_LOGIN_FROM, SIGNUP);
    progressDialog.dismiss();
    if (Application.getConfiguration().isCreateStoreAndSetUserPrivacyAvailable()) {
      startActivity(new Intent(this, LoggedInActivity.class));
    } else {
      Toast.makeText(this, cm.aptoide.accountmanager.R.string.create_profile_pub_pri_suc_login, Toast.LENGTH_LONG).show();
      accountManager.sendLoginCancelledBroadcast();
    }
    finish();
  }

  @Override public void onRegisterFail(@StringRes int reason) {
    ShowMessage.asSnack(content, reason);
  }

  @Override public String getUserUsername() {
    return mUsername == null ? "" : mUsername.getText().toString();
  }

  @Override public String getUserAvatar() {
    return avatarPath == null ? "" : avatarPath;
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    FileUtils fileUtils = new FileUtils();
    Uri avatarUrl = null;
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      avatarUrl = getPhotoFileUri(AccountPermissionsBaseActivity.createAvatarPhotoName(photoAvatar));
      avatarPath = fileUtils.getPathAlt(avatarUrl, getApplicationContext());
    } else if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {
      avatarUrl = data.getData();
      avatarPath = fileUtils.getPath(avatarUrl, getApplicationContext());
    }
    checkAvatarRequirements(avatarPath, avatarUrl);
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    switch (requestCode) {
      case STORAGE_REQUEST_CODE:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          changePermissionValue(true);
          callGallery();
        } else {
          //TODO: Deal with permissions not being given by user
        }
        return;
      case CAMERA_REQUEST_CODE:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          changePermissionValue(true);
          dispatchTakePictureIntent(getApplicationContext());
        } else {
          //TODO: Deal with permissions not being given by user
        }
        break;
    }
  }
}