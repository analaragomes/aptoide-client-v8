package cm.aptoide.pt.v8engine.social.view;

import cm.aptoide.pt.v8engine.presenter.View;
import cm.aptoide.pt.v8engine.social.data.Card;
import cm.aptoide.pt.v8engine.social.data.CardTouchEvent;
import java.util.List;
import rx.Observable;

/**
 * Created by jdandrade on 31/05/2017.
 */

public interface TimelineView extends View {

  void showCards(List<Card> cards);

  void showProgressIndicator();

  void hideProgressIndicator();

  void hideRefresh();

  void showMoreCards(List<Card> cards);

  void showGenericError();

  Observable<Void> refreshes();

  Observable<Void> reachesBottom();

  Observable<CardTouchEvent> articleClicked();

  Observable<Void> retry();

  void showLoadMoreProgressIndicator();

  void hideLoadMoreProgressIndicator();

  boolean isNewRefresh();
}