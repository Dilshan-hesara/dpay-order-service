package lk.dilshan.hesara.d_pay.orderservice.repository;

import lk.dilshan.hesara.d_pay.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);
}
