package cm.aptoide.pt.v8engine.presenter;

import cm.aptoide.accountmanager.Account;
import cm.aptoide.pt.model.v7.store.Store;
import cm.aptoide.pt.v8engine.notification.view.InboxFragment;
import cm.aptoide.pt.v8engine.view.account.store.CreateStoreFragment;
import cm.aptoide.pt.v8engine.view.account.store.ManageStoreModel;
import cm.aptoide.pt.v8engine.view.account.user.CreateUserFragment;
import cm.aptoide.pt.v8engine.view.navigator.FragmentNavigator;

/**
 * Created by pedroribeiro on 16/05/17.
 */

public class MyAccountNavigator {

  private final FragmentNavigator fragmentNavigator;

  public MyAccountNavigator(FragmentNavigator fragmentNavigator) {
    this.fragmentNavigator = fragmentNavigator;
  }

  public void navigateToInboxView() {
    fragmentNavigator.navigateTo(new InboxFragment());
  }

  public void navigateToEditStoreView(Store store) {
    fragmentNavigator.navigateTo(CreateStoreFragment.newInstance(
        new ManageStoreModel(store.getId(), store.getAvatar(), false, store.getAppearance()
            .getTheme(), store.getName(), store.getAppearance()
            .getDescription())));
  }

  public void navigateToEditProfileView(Account account) {
    fragmentNavigator.navigateTo(
        CreateUserFragment.newInstance(account.getAvatar(), account.getNickname(),
            CreateUserFragment.FROM_MY_ACCOUNT));
  }
}
