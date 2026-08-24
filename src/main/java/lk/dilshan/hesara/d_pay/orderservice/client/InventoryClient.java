package lk.dilshan.hesara.d_pay.orderservice.client;

import lk.dilshan.hesara.d_pay.orderservice.exception.InsufficientStockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Typed HTTP client for the Inventory Service.
 *
 * Uses Spring's RestClient (already on the classpath via spring-boot-starter-restclient).
 * The base URL is resolved through the API Gateway so that no hard-coded port
 * coupling exists between services.
 *
 * All calls are intentionally synchronous inside the @Transactional createOrder()
 * flow so that a stock failure causes the entire order transaction to roll back.
 */
@Component
@Slf4j
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(
            @Value("${inventory.service.url:http://localhost:7000}") String inventoryBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(inventoryBaseUrl)
                .build();
    }

    /**
     * Decrements the stock of the given product by {@code qty} units.
     *
     * Sends:  PATCH /api/v1/inventory/{productId}/stock?delta=-{qty}
     *
     * @param productId  the product UUID or ID string
     * @param productName human-readable name used in error messages
     * @param qty        positive quantity to subtract (will be negated internally)
     * @throws InsufficientStockException if the Inventory Service responds 400
     *         because the resulting stock would go below zero
     */
    public void deductStock(String productId, String productName, int qty) {
        int delta = -qty; // negative delta = deduction
        log.info("Deducting stock: productId={}, delta={}", productId, delta);

        try {
            restClient.patch()
                    .uri("/api/v1/inventory/{id}/stock?delta={delta}", productId, delta)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Stock deducted successfully: productId={}, qty={}", productId, qty);

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST
                    || ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                // Inventory Service returns 400 when newQty < 0 (see ProductServiceImpl.adjustStock)
                throw new InsufficientStockException(
                        String.format("Insufficient stock for '%s' (productId=%s). Requested: %d",
                                productName, productId, qty));
            }
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InsufficientStockException(
                        String.format("Product not found in Inventory Service: '%s' (productId=%s)",
                                productName, productId));
            }
            // Any other 4xx/5xx — rethrow to let the transaction roll back
            log.error("Inventory stock deduction failed: productId={}, status={}, body={}",
                    productId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        }
    }
}
