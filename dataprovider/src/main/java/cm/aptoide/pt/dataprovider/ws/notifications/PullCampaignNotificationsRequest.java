/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 21/07/2016.
 */

package cm.aptoide.pt.dataprovider.ws.notifications;

import android.text.TextUtils;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.preferences.managed.ManagerPreferences;
import cm.aptoide.pt.utils.AptoideUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Observable;

/**
 * Created by trinkes on 7/13/16.
 */
public class PullCampaignNotificationsRequest
    extends Notifications<List<GetPullNotificationsResponse>> {

  protected static String BASE_HOST = "http://pnp.aptoide.com/pnp/v1/notifications/";

  private final Map<String, String> options;
  private final String id;

  protected PullCampaignNotificationsRequest(String id, Map<String, String> options,
      OkHttpClient httpClient, Converter.Factory converterFactory) {
    super(httpClient, converterFactory);
    this.options = options;
    this.id = id;
  }

  public static PullCampaignNotificationsRequest of(String aptoideClientUuid, String versionName,
      String appId, OkHttpClient httpClient, Converter.Factory converterFactory) {

    Map<String, String> options = new HashMap<>();

    options.put("language", AptoideUtils.SystemU.getCountryCode());
    options.put("aptoide_version", versionName);
    String oemid = DataProvider.getConfiguration()
        .getExtraId();
    if (!TextUtils.isEmpty(oemid)) {
      options.put("oem_id", oemid);
    }
    options.put("aptoide_package", appId);
    if (ManagerPreferences.isDebug()) {
      options.put("debug", "true");
    }

    return new PullCampaignNotificationsRequest(aptoideClientUuid, options, httpClient,
        converterFactory);
  }

  @Override protected Observable<List<GetPullNotificationsResponse>> loadDataFromNetwork(
      Interfaces interfaces, boolean bypassCache) {
    return interfaces.getPullCompaignNotifications(id, options, true);
  }
}
