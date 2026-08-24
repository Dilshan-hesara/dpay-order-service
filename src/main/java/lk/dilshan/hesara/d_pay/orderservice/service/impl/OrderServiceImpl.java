package lk.dilshan.hesara.d_pay.orderservice.service.impl;

import lk.dilshan.hesara.d_pay.orderservice.client.InventoryClient;
import lk.dilshan.hesara.d_pay.orderservice.dto.OrderItemDTO;
import lk.dilshan.hesara.d_pay.orderservice.dto.OrderRequestDTO;
import lk.dilshan.hesara.d_pay.orderservice.dto.OrderResponseDTO;
import lk.dilshan.hesara.d_pay.orderservice.entity.Order;
import lk.dilshan.hesara.d_pay.orderservice.entity.OrderItem;
import lk.dilshan.hesara.d_pay.orderservice.exception.OrderNotFoundException;
import lk.dilshan.hesara.d_pay.orderservice.repository.OrderRepository;
import lk.dilshan.hesara.d_pay.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    @Override
    public OrderResponseDTO createOrder(OrderRequestDTO dto) {
        log.info("Creating order for user: {}", dto.getUserId());

        // ── 1. Build order items (line totals, etc.) ─────────────────────────
        List<OrderItem> items = dto.getItems().stream().map(itemDto -> {
            BigDecimal lineTotal = itemDto.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemDto.getQty()));
            return OrderItem.builder()
                    .productId(itemDto.getProductId())
                    .productName(itemDto.getProductName())
                    .sku(itemDto.getSku())
                    .qty(itemDto.getQty())
                    .unitPrice(itemDto.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build();
        }).toList();

        // ── 2. Calculate totals ───────────────────────────────────────────────
        BigDecimal subtotal = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal tax      = dto.getTaxAmount()      != null ? dto.getTaxAmount()      : BigDecimal.ZERO;
        BigDecimal total    = subtotal.subtract(discount).add(tax);

        // ── 3. Deduct stock in Inventory Service ──────────────────────────────
        // This is intentionally synchronous and inside the @Transactional boundary.
        // If ANY item fails (out-of-stock, product not found), an exception is thrown
        // before orderRepository.save() is called, so the order is never persisted.
        log.info("Deducting inventory stock for {} item(s)", items.size());
        for (OrderItem item : items) {
            inventoryClient.deductStock(item.getProductId(), item.getProductName(), item.getQty());
        }

        // ── 4. Persist the confirmed order ────────────────────────────────────
        Order order = Order.builder()
                .userId(dto.getUserId())
                .status(Order.OrderStatus.CONFIRMED)
                .totalAmount(total)
                .discountAmount(discount)
                .taxAmount(tax)
                .notes(dto.getNotes())
                .build();

        items.forEach(item -> item.setOrder(order));
        order.getItems().addAll(items);

        OrderResponseDTO response = toResponseDTO(orderRepository.save(order));
        log.info("Order created successfully: orderId={}, total={}", response.getId(), total);
        return response;
    }


    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID id) {
        return toResponseDTO(orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Override
    public OrderResponseDTO updateOrderStatus(UUID id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
        order.setStatus(status);
        return toResponseDTO(orderRepository.save(order));
    }

    @Override
    public void cancelOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order");
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private OrderResponseDTO toResponseDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        List<OrderItemDTO> itemDTOs = order.getItems().stream().map(item -> {
            OrderItemDTO i = new OrderItemDTO();
            i.setProductId(item.getProductId());
            i.setProductName(item.getProductName());
            i.setSku(item.getSku());
            i.setQty(item.getQty());
            i.setUnitPrice(item.getUnitPrice());
            return i;
        }).toList();
        dto.setItems(itemDTOs);
        return dto;
    }
}
