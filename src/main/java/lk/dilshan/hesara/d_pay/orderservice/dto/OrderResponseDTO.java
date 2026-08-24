package lk.dilshan.hesara.d_pay.orderservice.dto;

import lk.dilshan.hesara.d_pay.orderservice.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderResponseDTO {

    private UUID id;
    private UUID userId;
    private Order.OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private String notes;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
}
