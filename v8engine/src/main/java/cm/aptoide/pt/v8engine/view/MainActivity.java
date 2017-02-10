/*
 * Copyright (c) 2017.
 * Modified by Marcelo Benites on 18/01/2017.
 */

package cm.aptoide.pt.v8engine.view;

import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.actions.PermissionManager;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.dataprovider.ws.v7.V7;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.model.v7.Event;
import cm.aptoide.pt.model.v7.GetStoreWidgets;
import cm.aptoide.pt.model.v7.Layout;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.AutoUpdate;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.activity.BaseActivity;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.fragment.BaseWizardViewerFragment;
import cm.aptoide.pt.v8engine.fragment.implementations.AppViewFragment;
import cm.aptoide.pt.v8engine.fragment.implementations.HomeFragment;
import cm.aptoide.pt.v8engine.fragment.implementations.ScheduledDownloadsFragment;
import cm.aptoide.pt.v8engine.fragment.implementations.storetab.StoreTabFragmentChooser;
import cm.aptoide.pt.v8engine.install.InstallerFactory;
import cm.aptoide.pt.v8engine.interfaces.DrawerFragment;
import cm.aptoide.pt.v8engine.interfaces.FragmentShower;
import cm.aptoide.pt.v8engine.presenter.MainPresenter;
import cm.aptoide.pt.v8engine.receivers.DeepLinkIntentReceiver;
import cm.aptoide.pt.v8engine.services.ContentPuller;
import cm.aptoide.pt.v8engine.util.ApkFy;
import cm.aptoide.pt.v8engine.util.DownloadFactory;
import cm.aptoide.pt.v8engine.util.FragmentUtils;
import cm.aptoide.pt.v8engine.util.StoreUtils;
import cm.aptoide.pt.v8engine.util.StoreUtilsProxy;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by neuro on 06-05-2016.
 */
public class MainActivity extends BaseActivity implements MainView, FragmentShower {

  private static final String TAG = MainActivity.class.getSimpleName();
  private AptoideAccountManager accountManager;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.frame_layout);
    accountManager = ((V8Engine) getApplicationContext()).getAccountManager();
    final AutoUpdate autoUpdate =
        new AutoUpdate(this, new InstallerFactory().create(this, InstallerFactory.DEFAULT),
            new DownloadFactory(), AptoideDownloadManager.getInstance(), new PermissionManager());
    attachPresenter(
        new MainPresenter(this, new ApkFy(this, getIntent()), autoUpdate, new ContentPuller(this)),
        savedInstanceState);
  }

  @Override public void changeOrientationToPortrait() {
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  }

  @Override public void showWizard() {
    pushFragment(new BaseWizardViewerFragment());
  }

  @Override public void showHome() {
    pushFragment(V8Engine.getFragmentProvider()
        .newHomeFragment(V8Engine.getConfiguration().getDefaultStore(), StoreContext.home,
            V8Engine.getConfiguration().getDefaultTheme()));
  }

  @Override public void showDeepLink() {
    handleDeepLinks();
  }

  private void handleDeepLinks() {
    final Intent intent = getIntent();
    if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksTargets.APP_VIEW_FRAGMENT)) {

      if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksKeys.APP_MD5_KEY)) {
        appViewDeepLink(intent.getStringExtra(DeepLinkIntentReceiver.DeepLinksKeys.APP_MD5_KEY));
      } else if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksKeys.APP_ID_KEY)) {
        appViewDeepLink(intent.getLongExtra(DeepLinkIntentReceiver.DeepLinksKeys.APP_ID_KEY, -1),
            intent.getStringExtra(DeepLinkIntentReceiver.DeepLinksKeys.PACKAGE_NAME_KEY), true);
      } else if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksKeys.PACKAGE_NAME_KEY)) {
        appViewDeepLink(
            intent.getStringExtra(DeepLinkIntentReceiver.DeepLinksKeys.PACKAGE_NAME_KEY),
            intent.getStringExtra(DeepLinkIntentReceiver.DeepLinksKeys.STORENAME_KEY),
            intent.getBooleanExtra(DeepLinkIntentReceiver.DeepLinksKeys.SHOW_AUTO_INSTALL_POPUP,
                true));
      }
    } else if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksTargets.SEARCH_FRAGMENT)) {
      searchDeepLink(intent.getStringExtra(SearchManager.QUERY));
    } else if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksTargets.NEW_REPO)) {
      newrepoDeepLink(
          intent.getExtras().getStringArrayList(DeepLinkIntentReceiver.DeepLinksTargets.NEW_REPO));
    } else if (intent.hasExtra(
        DeepLinkIntentReceiver.DeepLinksTargets.FROM_DOWNLOAD_NOTIFICATION)) {
      downloadNotificationDeepLink(intent);
    } else if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksTargets.FROM_TIMELINE)) {
      fromTimelineDeepLink(intent);
    } else if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksTargets.NEW_UPDATES)) {
      newUpdatesDeepLink(intent);
    } else if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksTargets.GENERIC_DEEPLINK)) {
      genericDeepLink(intent.getParcelableExtra(DeepLinkIntentReceiver.DeepLinksKeys.URI));
    } else if (intent.hasExtra(DeepLinkIntentReceiver.DeepLinksTargets.SCHEDULE_DEEPLINK)) {
      scheduleDownloadsDeepLink(
          intent.getParcelableExtra(DeepLinkIntentReceiver.DeepLinksKeys.URI));
    } else {
      Analytics.ApplicationLaunch.launcher();
    }
  }

  private void appViewDeepLink(String md5) {
    pushFragment(AppViewFragment.newInstance(md5));
  }

  private void appViewDeepLink(long appId, String packageName, boolean showPopup) {
    AppViewFragment.OpenType openType = showPopup ? AppViewFragment.OpenType.OPEN_WITH_INSTALL_POPUP
        : AppViewFragment.OpenType.OPEN_ONLY;
    pushFragment(V8Engine.getFragmentProvider().newAppViewFragment(appId, packageName, openType));
  }

  private void appViewDeepLink(String packageName, String storeName, boolean showPopup) {
    AppViewFragment.OpenType openType = showPopup ? AppViewFragment.OpenType.OPEN_WITH_INSTALL_POPUP
        : AppViewFragment.OpenType.OPEN_ONLY;
    pushFragment(
        V8Engine.getFragmentProvider().newAppViewFragment(packageName, storeName, openType));
  }

  private void searchDeepLink(String query) {
    pushFragment(V8Engine.getFragmentProvider().newSearchFragment(query));
  }

  private void newrepoDeepLink(ArrayList<String> repos) {
    if (repos != null) {
      Observable.from(repos)
          .map(storeUrl -> StoreUtils.split(storeUrl))
          .flatMap(storeName -> StoreUtils.isSubscribedStore(storeName)
              .first()
              .observeOn(AndroidSchedulers.mainThread())
              .doOnNext(isFollowed -> {
                if (isFollowed) {
                  ShowMessage.asSnack(this, getString(R.string.store_already_added));
                } else {
                  StoreUtilsProxy.subscribeStore(storeName, accountManager);
                  ShowMessage.asSnack(this,
                      AptoideUtils.StringU.getFormattedString(R.string.store_followed, storeName));
                }
              }))
          .toList()
          .subscribe(storeName -> {
            setMainPagerPosition(Event.Name.myStores);
            Logger.d(TAG, "newrepoDeepLink: all stores added");
          }, throwable -> {
            Logger.e(TAG, "newrepoDeepLink: " + throwable);
            CrashReport.getInstance().log(throwable);
          });
      getIntent().removeExtra(DeepLinkIntentReceiver.DeepLinksTargets.NEW_REPO);
    }
  }

  private void downloadNotificationDeepLink(Intent intent) {
    Analytics.ApplicationLaunch.downloadingUpdates();
    setMainPagerPosition(Event.Name.myDownloads);
  }

  private void fromTimelineDeepLink(Intent intent) {
    Analytics.ApplicationLaunch.timelineNotification();
    setMainPagerPosition(Event.Name.getUserTimeline);
  }

  private void newUpdatesDeepLink(Intent intent) {
    Analytics.ApplicationLaunch.newUpdatesNotification();
    setMainPagerPosition(Event.Name.myUpdates);
  }

  private void genericDeepLink(Uri uri) {
    Event event = new Event();
    String queryType = uri.getQueryParameter(DeepLinkIntentReceiver.DeepLinksKeys.TYPE);
    String queryLayout = uri.getQueryParameter(DeepLinkIntentReceiver.DeepLinksKeys.LAYOUT);
    String queryName = uri.getQueryParameter(DeepLinkIntentReceiver.DeepLinksKeys.NAME);
    String queryAction = uri.getQueryParameter(DeepLinkIntentReceiver.DeepLinksKeys.ACTION);
    if (validateDeepLinkRequiredArgs(queryType, queryLayout, queryName, queryAction)) {
      try {
        queryAction = URLDecoder.decode(queryAction, "UTF-8");
        event.setAction(queryAction != null ? queryAction.replace(V7.BASE_HOST, "") : null);
        event.setType(Event.Type.valueOf(queryType));
        event.setName(Event.Name.valueOf(queryName));
        GetStoreWidgets.WSWidget.Data data = new GetStoreWidgets.WSWidget.Data();
        data.setLayout(Layout.valueOf(queryLayout));
        event.setData(data);
        pushFragment(V8Engine.getFragmentProvider()
            .newStoreTabGridRecyclerFragment(event,
                uri.getQueryParameter(DeepLinkIntentReceiver.DeepLinksKeys.TITLE),
                uri.getQueryParameter(DeepLinkIntentReceiver.DeepLinksKeys.STORE_THEME),
                V8Engine.getConfiguration().getDefaultTheme()));
      } catch (UnsupportedEncodingException | IllegalArgumentException e) {
        e.printStackTrace();
      }
    }
  }

  private void scheduleDownloadsDeepLink(Uri uri) {
    if (uri != null) {
      String openMode = uri.getQueryParameter(DeepLinkIntentReceiver.DeepLinksKeys.OPEN_MODE);
      if (!TextUtils.isEmpty(openMode)) {
        pushFragment(V8Engine.getFragmentProvider()
            .newScheduledDownloadsFragment(ScheduledDownloadsFragment.OpenMode.valueOf(openMode)));
      }
    }
  }

  private void setMainPagerPosition(Event.Name name) {
    AptoideUtils.ThreadU.runOnIoThread(() -> {
      AptoideUtils.ThreadU.runOnUiThread(() -> {
        if (!(getCurrentFragment() instanceof HomeFragment)) {
          return;
        }

        ((HomeFragment) getCurrentFragment()).setDesiredViewPagerItem(name);
      });
    });
  }

  private boolean validateDeepLinkRequiredArgs(String queryType, String queryLayout,
      String queryName, String queryAction) {
    return !TextUtils.isEmpty(queryType)
        && !TextUtils.isEmpty(queryLayout)
        && !TextUtils.isEmpty(queryName)
        && !TextUtils.isEmpty(queryAction)
        && StoreTabFragmentChooser.validateAcceptedName(Event.Name.valueOf(queryName));
  }

  @Override public void pushFragment(Fragment fragment) {
    FragmentUtils.replaceFragmentV4(this, fragment);
  }

  @Override public Fragment getLast() {
    return FragmentUtils.getLastFragmentV4(this);
  }

  @Override public void onBackPressed() {

    // A little hammered to close the drawer on back pressed :)
    if (getSupportFragmentManager().getFragments()
        .get(getSupportFragmentManager().getFragments().size() - 1) instanceof DrawerFragment) {
      DrawerFragment fragment = (DrawerFragment) getSupportFragmentManager().getFragments()
          .get(getSupportFragmentManager().getFragments().size() - 1);
      if (fragment.isDrawerOpened()) {
        fragment.closeDrawer();
        return;
      } else {
        super.onBackPressed();
      }
    } else {
      super.onBackPressed();
    }
  }

  private Fragment getCurrentFragment() {
    if (getSupportFragmentManager().getFragments() != null
        && getSupportFragmentManager().getFragments().size() > 0) {
      return getSupportFragmentManager().getFragments()
          .get(getSupportFragmentManager().getFragments().size() - 1);
    } else {
      return null;
    }
  }
}