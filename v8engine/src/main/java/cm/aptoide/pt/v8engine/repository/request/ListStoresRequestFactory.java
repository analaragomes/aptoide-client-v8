package cm.aptoide.pt.v8engine.repository.request;

import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.repository.IdsRepositoryImpl;
import cm.aptoide.pt.dataprovider.ws.v7.V7EndlessController;
import cm.aptoide.pt.dataprovider.ws.v7.store.ListStoresRequest;
import cm.aptoide.pt.interfaces.AccessToken;
import cm.aptoide.pt.interfaces.AptoideClientUUID;
import cm.aptoide.pt.model.v7.store.Store;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;

/**
 * Created by neuro on 03-01-2017.
 */
class ListStoresRequestFactory {

  private final AptoideClientUUID aptoideClientUUID;
  private final AccessToken accessToken;

  public ListStoresRequestFactory() {
    aptoideClientUUID = () -> new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
        DataProvider.getContext()).getAptoideClientUUID();

    accessToken = AptoideAccountManager::getAccessToken;
  }

  public ListStoresRequest newListStoresRequest(int offset, int limit) {
    return ListStoresRequest.ofTopStores(offset, limit, accessToken.get(),
        aptoideClientUUID.getAptoideClientUUID());
  }

  public V7EndlessController<Store> listStores(int offset, int limit) {
    return new V7EndlessController<>(ListStoresRequest.ofTopStores(offset, limit, accessToken.get(),
        aptoideClientUUID.getAptoideClientUUID()));
  }

  public ListStoresRequest newListStoresRequest(String url) {
    return ListStoresRequest.ofAction(url, accessToken.get(),
        aptoideClientUUID.getAptoideClientUUID());
  }
}