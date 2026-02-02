package bakery.service;

import bakery.model.Order;
import bakery.repository.OrderRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
        @Autowired
        private OrderRepo orderRepo;

        @Override
        public List<Order> getAllOrders() { return orderRepo.findAll(); }

        @Override
        public Order getOrderById(Long id) {
            return orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        }

        @Override
        public Order saveOrder(Order order) { return orderRepo.save(order); }

        @Override
        public void deleteOrder(Long id) { orderRepo.deleteById(id); }
}

