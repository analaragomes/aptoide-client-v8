/*
 * Copyright (c) 2016.
 * Modified on 04/08/2016.
 */

package cm.aptoide.pt.v8engine.view.app.displayable;

import cm.aptoide.pt.database.realm.MinimalAd;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.app.AppViewAnalytics;
import cm.aptoide.pt.v8engine.view.recycler.displayable.DisplayablePojo;
import java.util.List;

/**
 * Created on 04/05/16.
 */
public class AppViewSuggestedAppsDisplayable extends DisplayablePojo<List<MinimalAd>> {

  private AppViewAnalytics appViewAnalytics;

  public AppViewSuggestedAppsDisplayable() {
  }

  public AppViewSuggestedAppsDisplayable(List<MinimalAd> ads, AppViewAnalytics appViewAnalytics) {
    super(ads);
    this.appViewAnalytics = appViewAnalytics;
  }

  @Override protected Configs getConfig() {
    return new Configs(1, true);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_app_view_suggested_apps;
  }

  public AppViewAnalytics getAppViewAnalytics() {
    return appViewAnalytics;
  }
}
