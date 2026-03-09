package bakery.service;


import bakery.entity.Order;
import bakery.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;

import java.util.List;

@Service
public class AdOrderServiceImpl implements AdOrderService {
    @Autowired
    private OrderRepository orderRepo;

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



