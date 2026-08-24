package lk.dilshan.hesara.d_pay.orderservice.service;

import lk.dilshan.hesara.d_pay.orderservice.dto.OrderRequestDTO;
import lk.dilshan.hesara.d_pay.orderservice.dto.OrderResponseDTO;
import lk.dilshan.hesara.d_pay.orderservice.entity.Order;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponseDTO createOrder(OrderRequestDTO dto);

    OrderResponseDTO getOrderById(UUID id);

    List<OrderResponseDTO> getOrdersByUser(UUID userId);

    List<OrderResponseDTO> getOrdersByStatus(Order.OrderStatus status);

    List<OrderResponseDTO> getAllOrders();

    OrderResponseDTO updateOrderStatus(UUID id, Order.OrderStatus status);

    void cancelOrder(UUID id);
}
