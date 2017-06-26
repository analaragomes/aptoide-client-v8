package cm.aptoide.pt.v8engine.social.data;

import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.v8engine.InstallManager;
import cm.aptoide.pt.v8engine.Progress;
import cm.aptoide.pt.v8engine.download.DownloadFactory;
import java.util.List;
import rx.Observable;
import rx.Single;

/**
 * Created by jdandrade on 31/05/2017.
 */

public class SocialManager {
  private final SocialService service;
  private final InstallManager installManager;
  private final DownloadFactory downloadFactory;

  public SocialManager(SocialService service, InstallManager installManager,
      DownloadFactory downloadFactory) {
    this.service = service;
    this.installManager = installManager;
    this.downloadFactory = downloadFactory;
  }

  public Single<List<Post>> getCards() {
    return service.getCards();
  }

  public Single<List<Post>> getNextCards() {
    return service.getNextCards();
  }

  public Observable<? extends Progress<Download>> updateApp(CardTouchEvent cardTouchEvent) {
    return installManager.install(downloadFactory.create(
        (cm.aptoide.pt.v8engine.social.data.AppUpdate) cardTouchEvent.getCard(),
        Download.ACTION_UPDATE));
  }
}
