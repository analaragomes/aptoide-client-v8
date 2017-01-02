package cm.aptoide.pt.viewRateAndCommentReviews;

import cm.aptoide.pt.model.v7.Comment;
import cm.aptoide.pt.v8engine.adapters.CommentsAdapter;
import cm.aptoide.pt.v8engine.view.recycler.displayable.Displayable;
import java.util.List;

public interface CommentAdderView<Tadapter extends CommentsAdapter> {
  Tadapter getAdapter();
  void createDisplayableComments(List<Comment> comments, List<Displayable> displayables);
}
