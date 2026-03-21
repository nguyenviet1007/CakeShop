package bakery.service;

import bakery.dto.request.PosOrderRequest;
import bakery.entity.Order;

import java.util.List;

public interface AdOrderService {
    List<Order> getAllOrders();
    Order getOrderById(Long id);
    Order saveOrder(Order order);
    void deleteOrder(Long id);
    Order createOrderFromPos(PosOrderRequest request);
}


