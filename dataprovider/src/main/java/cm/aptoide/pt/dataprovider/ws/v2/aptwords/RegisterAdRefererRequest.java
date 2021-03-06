/*
 * Copyright (c) 2016.
 * Modified on 24/06/2016.
 */

package cm.aptoide.pt.dataprovider.ws.v2.aptwords;

import android.os.Build;
import cm.aptoide.pt.networkclient.util.HashMapNotNull;
import lombok.Data;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Observable;

/**
 * Created by rmateus on 29-07-2014.
 */
public class RegisterAdRefererRequest extends Aptwords<RegisterAdRefererRequest.DefaultResponse> {

  private final String q;
  private long adId;
  private long appId;
  private String tracker;
  private String success;

  private RegisterAdRefererRequest(long adId, long appId, String clickUrl, boolean success,
      OkHttpClient httpClient, Converter.Factory converterFactory, String q) {
    super(httpClient, converterFactory);
    this.q = q;
    this.adId = adId;
    this.appId = appId;
    this.success = (success ? "1" : "0");

    extractAndSetTracker(clickUrl);
  }

  public static RegisterAdRefererRequest of(long adId, long appId, String clickUrl, boolean success,
      OkHttpClient httpClient, Converter.Factory converterFactory, String q) {
    return new RegisterAdRefererRequest(adId, appId, clickUrl, success, httpClient,
        converterFactory, q);
  }

  private void extractAndSetTracker(String clickUrl) {
    int i = clickUrl.indexOf("//");

    int last = clickUrl.indexOf("/", i + 2);

    tracker = clickUrl.substring(0, last);
  }

  public void execute() {
    super.execute(defaultResponse -> {
      // Does nothing
    }, e -> {
      // As well :)
    });
  }

  @Override protected Observable<DefaultResponse> loadDataFromNetwork(Interfaces interfaces,
      boolean bypassCache) {

    HashMapNotNull<String, String> map = new HashMapNotNull<>();

    map.put("success", success);
    map.put("adid", Long.toString(adId));
    map.put("appid", Long.toString(appId));
    map.put("q", q);
    map.put("androidversion", Build.VERSION.RELEASE);
    map.put("tracker", tracker);

    return interfaces.load(map);
  }

  @Data public static class DefaultResponse {

    String status;
  }
}
