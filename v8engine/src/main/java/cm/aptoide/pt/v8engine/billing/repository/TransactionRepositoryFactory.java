package cm.aptoide.pt.v8engine.billing.repository;

import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.product.InAppProduct;
import cm.aptoide.pt.v8engine.billing.product.PaidAppProduct;

public class TransactionRepositoryFactory {

  private final InAppTransactionRepository inAppTransactionRepository;
  private final PaidAppTransactionRepository paidAppTransactionRepository;

  public TransactionRepositoryFactory(InAppTransactionRepository inAppTransactionRepository,
      PaidAppTransactionRepository paidAppTransactionRepository) {
    this.inAppTransactionRepository = inAppTransactionRepository;
    this.paidAppTransactionRepository = paidAppTransactionRepository;
  }

  public TransactionRepository getTransactionRepository(Product product) {
    if (product instanceof InAppProduct) {
      return inAppTransactionRepository;
    } else if (product instanceof PaidAppProduct) {
      return paidAppTransactionRepository;
    } else {
      throw new IllegalArgumentException(
          "No compatible repository for product " + product.getTitle());
    }
  }
}