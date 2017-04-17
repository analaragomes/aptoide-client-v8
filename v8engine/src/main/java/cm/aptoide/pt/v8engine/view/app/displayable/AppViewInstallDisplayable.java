/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 25/08/2016.
 */

package cm.aptoide.pt.v8engine.view.app.displayable;

import android.widget.Button;
import cm.aptoide.pt.database.realm.MinimalAd;
import cm.aptoide.pt.model.v7.GetApp;
import cm.aptoide.pt.v8engine.InstallManager;
import cm.aptoide.pt.v8engine.InstallationProgress;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.repository.InstalledRepository;
import cm.aptoide.pt.v8engine.repository.RollbackRepository;
import cm.aptoide.pt.v8engine.util.DownloadFactory;
import lombok.Getter;
import lombok.Setter;
import rx.Observable;
import rx.Single;

/**
 * Created by sithengineer on 06/05/16.
 */
public class AppViewInstallDisplayable extends AppViewDisplayable {

  private int versionCode;
  @Getter @Setter private boolean shouldInstall;
  @Getter private MinimalAd minimalAd;

  private InstallManager installManager;
  private String md5;
  private String packageName;
  private Button installButton;
  private DownloadFactory downloadFactory;

  public AppViewInstallDisplayable() {
    super();
  }

  public AppViewInstallDisplayable(InstallManager installManager, GetApp getApp,
      MinimalAd minimalAd, boolean shouldInstall, InstalledRepository installedRepository,
      DownloadFactory downloadFactory) {
    super(getApp);
    this.installManager = installManager;
    this.md5 = getApp.getNodes().getMeta().getData().getFile().getMd5sum();
    this.packageName = getApp.getNodes().getMeta().getData().getPackageName();
    this.versionCode = getApp.getNodes().getMeta().getData().getFile().getVercode();
    this.minimalAd = minimalAd;
    this.shouldInstall = shouldInstall;
    this.downloadFactory = downloadFactory;
  }

  public static AppViewInstallDisplayable newInstance(GetApp getApp, InstallManager installManager,
      MinimalAd minimalAd, boolean shouldInstall, InstalledRepository installedRepository,
      DownloadFactory downloadFactory) {
    return new AppViewInstallDisplayable(installManager, getApp, minimalAd, shouldInstall,
        installedRepository, downloadFactory);
  }

  public void startInstallationProcess() {
    if (installButton != null) {
      installButton.performClick();
    }
  }

  public void setInstallButton(Button installButton) {
    this.installButton = installButton;
  }

  @Override protected Configs getConfig() {
    return new Configs(1, true);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_app_view_install;
  }

  public Observable<InstallationProgress> getInstallState() {
    return installManager.getInstallationProgress(md5, packageName, versionCode);
  }

  public Observable<InstallManager.InstallationType> getInstallationType() {
    return installManager.getInstallationType(packageName, versionCode);
  }

  public DownloadFactory getDownloadFactory() {
    return downloadFactory;
  }

  public Single<InstallManager.Error> getError() {
    return installManager.getError(md5);
  }

}
