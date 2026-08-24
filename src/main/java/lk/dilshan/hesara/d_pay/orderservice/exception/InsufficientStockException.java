package lk.dilshan.hesara.d_pay.orderservice.exception;

/**
 * Thrown when an order item's requested quantity exceeds the
 * available stock in the Inventory Service.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
