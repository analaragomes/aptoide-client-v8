package cm.aptoide.pt.v8engine.view.navigator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import rx.Observable;

public interface ActivityNavigator {

  Observable<Result> navigateForResult(Class<? extends AppCompatActivity> activityClass,
      int requestCode);

  Observable<Result> navigateForResult(Class<? extends Activity> activityClass, int requestCode,
      Bundle bundle);

  void navigateTo(Class<? extends AppCompatActivity> activityClass);

  void navigateTo(Class<? extends AppCompatActivity> activityClass, Bundle bundle);

  void finish(int code, Bundle bundle);

  class Result {

    private final int requestCode;
    private final int resultCode;
    private final Intent data;

    public Result(int requestCode, int resultCode, Intent data) {
      this.requestCode = requestCode;
      this.resultCode = resultCode;
      this.data = data;
    }

    public int getRequestCode() {
      return requestCode;
    }

    public int getResultCode() {
      return resultCode;
    }

    public Intent getData() {
      return data;
    }
  }
}
