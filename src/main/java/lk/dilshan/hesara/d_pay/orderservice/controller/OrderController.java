package lk.dilshan.hesara.d_pay.orderservice.controller;

import jakarta.validation.Valid;
import lk.dilshan.hesara.d_pay.orderservice.dto.OrderRequestDTO;
import lk.dilshan.hesara.d_pay.orderservice.dto.OrderResponseDTO;
import lk.dilshan.hesara.d_pay.orderservice.entity.Order;
import lk.dilshan.hesara.d_pay.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO dto) {
        log.info("POST /api/v1/orders - user: {}", dto.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID id) {
        log.info("GET /api/v1/orders/{}", id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getOrders(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String status) {
        log.info("GET /api/v1/orders - userId={}, status={}", userId, status);
        if (userId != null) {
            return ResponseEntity.ok(orderService.getOrdersByUser(userId));
        }
        if (status != null) {
            return ResponseEntity.ok(orderService.getOrdersByStatus(Order.OrderStatus.valueOf(status.toUpperCase())));
        }
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        log.info("PATCH /api/v1/orders/{}/status -> {}", id, status);
        return ResponseEntity.ok(orderService.updateOrderStatus(id, Order.OrderStatus.valueOf(status.toUpperCase())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        log.info("DELETE /api/v1/orders/{}", id);
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
