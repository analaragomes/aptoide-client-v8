package cm.aptoide.pt.v8engine.billing.view;

import android.app.Activity;
import android.os.Bundle;
import cm.aptoide.pt.v8engine.BuildConfig;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.billing.PaymentMethod;
import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.Purchase;
import cm.aptoide.pt.v8engine.billing.methods.BoaCompraPaymentMethod;
import cm.aptoide.pt.v8engine.billing.methods.PayPalPaymentMethod;
import cm.aptoide.pt.v8engine.billing.product.InAppProduct;
import cm.aptoide.pt.v8engine.billing.product.PaidAppProduct;
import cm.aptoide.pt.v8engine.view.navigator.ActivityNavigator;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import java.math.BigDecimal;
import rx.Observable;

public class PaymentNavigator {

  private final PurchaseBundleMapper bundleMapper;
  private final ActivityNavigator activityNavigator;

  public PaymentNavigator(PurchaseBundleMapper bundleMapper, ActivityNavigator activityNavigator) {
    this.bundleMapper = bundleMapper;
    this.activityNavigator = activityNavigator;
  }

  public void navigateToAuthorizationView(PaymentMethod paymentMethod, Product product) {
    if (paymentMethod instanceof BoaCompraPaymentMethod) {
      activityNavigator.navigateTo(BoaCompraActivity.class,
          getProductBundle(paymentMethod, product));
    } else {
      throw new IllegalArgumentException("Invalid authorized payment.");
    }
  }

  public void navigateToLocalPaymentView(PaymentMethod paymentMethod, Product product) {
    if (paymentMethod instanceof PayPalPaymentMethod) {
      activityNavigator.navigateTo(PayPalActivity.class, getProductBundle(paymentMethod, product));
    } else {
      throw new IllegalArgumentException("Invalid local payment.");
    }
  }

  public Observable<PayPalResult> navigateToPayPalForResult(int requestCode, String currency,
      String description, double amount) {

    final Bundle bundle = new Bundle();
    bundle.putParcelable(PayPalService.EXTRA_PAYPAL_CONFIGURATION,
        new PayPalConfiguration().environment(BuildConfig.PAYPAL_ENVIRONMENT)
            .clientId(BuildConfig.PAYPAL_KEY)
            .merchantName(V8Engine.getConfiguration()
                .getMarketName()));
    bundle.putParcelable(com.paypal.android.sdk.payments.PaymentActivity.EXTRA_PAYMENT,
        new PayPalPayment(new BigDecimal(amount), currency, description,
            PayPalPayment.PAYMENT_INTENT_SALE));

    return activityNavigator.navigateForResult(
        com.paypal.android.sdk.payments.PaymentActivity.class, requestCode, bundle)
        .map(result -> map(result));
  }

  public void popBackStackWithResult(Purchase purchase) {
    activityNavigator.finish(Activity.RESULT_OK, bundleMapper.map(purchase));
  }

  public void popBackStackWithResult(Throwable throwable) {
    activityNavigator.finish(Activity.RESULT_CANCELED, bundleMapper.map(throwable));
  }

  public void popBackStackWithResult() {
    activityNavigator.finish(Activity.RESULT_CANCELED, bundleMapper.mapCancellation());
  }

  private Bundle getProductBundle(PaymentMethod paymentMethod, Product product) {
    final Bundle bundle;
    if (product instanceof InAppProduct) {
      bundle =
          ProductActivity.getBundle(paymentMethod.getId(), ((InAppProduct) product).getApiVersion(),
              ((InAppProduct) product).getPackageName(), ((InAppProduct) product).getType(),
              ((InAppProduct) product).getSku(), ((InAppProduct) product).getDeveloperPayload());
    } else if (product instanceof PaidAppProduct) {
      bundle =
          ProductActivity.getBundle(paymentMethod.getId(), ((PaidAppProduct) product).getAppId(),
              ((PaidAppProduct) product).getStoreName(), ((PaidAppProduct) product).isSponsored());
    } else {
      throw new IllegalArgumentException("Invalid product. Only in-app and paid apps supported");
    }
    return bundle;
  }

  private PayPalResult map(ActivityNavigator.Result result) {
    switch (result.getResultCode()) {
      case Activity.RESULT_OK:
        final PaymentConfirmation confirmation = result.getData()
            .getParcelableExtra(
                com.paypal.android.sdk.payments.PaymentActivity.EXTRA_RESULT_CONFIRMATION);
        if (confirmation != null && confirmation.getProofOfPayment() != null) {
          return new PaymentNavigator.PayPalResult(PaymentNavigator.PayPalResult.SUCCESS,
              confirmation.getProofOfPayment()
                  .getPaymentId());
        } else {
          return new PaymentNavigator.PayPalResult(PaymentNavigator.PayPalResult.ERROR, null);
        }
      case Activity.RESULT_CANCELED:
        return new PaymentNavigator.PayPalResult(PaymentNavigator.PayPalResult.CANCELLED, null);
      case PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID:
      default:
        return new PaymentNavigator.PayPalResult(PaymentNavigator.PayPalResult.ERROR, null);
    }
  }

  public static class PayPalResult {

    public static final int SUCCESS = 0;
    public static final int ERROR = 1;
    public static final int CANCELLED = 2;

    private final int status;
    private final String paymentConfirmationId;

    public PayPalResult(int status, String paymentConfirmationId) {
      this.status = status;
      this.paymentConfirmationId = paymentConfirmationId;
    }

    public int getStatus() {
      return status;
    }

    public String getPaymentConfirmationId() {
      return paymentConfirmationId;
    }
  }
}