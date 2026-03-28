package bakery.service;

import bakery.dto.request.PosOrderRequest;
import bakery.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AdOrderService {
    List<Order> getAllOrders(); // Dùng cho Thống kê (Statistics)
    Page<Order> getAllOrders(Pageable pageable);
    Order getOrderById(Long id);
    Order saveOrder(Order order);
    void deleteOrder(Long id);
    Order createOrderFromPos(PosOrderRequest request);
    Page<Order> getOrdersByDateRange(LocalDate start, LocalDate end, Pageable pageable);
}


