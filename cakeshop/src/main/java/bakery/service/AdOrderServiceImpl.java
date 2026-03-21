package bakery.service;


import bakery.dto.request.PosOrderDetailDTO;
import bakery.dto.request.PosOrderRequest;
import bakery.entity.DailyStock;
import bakery.entity.Order;
import bakery.entity.OrderDetail;
import bakery.repository.DailyStockRepository;
import bakery.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdOrderServiceImpl implements AdOrderService {
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private DailyStockRepository dailyStockRepo;
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
    public Order createOrderFromPos(PosOrderRequest request) {
        // 1. Khởi tạo đơn hàng mới
        Order order = new Order();
        // Gán các thông tin cơ bản
        order.setOrderDate(LocalDateTime.now());
        order.setPayment(request.getPayment());
        // Sửa từ: order.setTotalAmount(request.getTotalAmount());
        order.setTotalAmount(BigDecimal.valueOf(request.getTotalAmount()));
        order.setStatus("COMPLETED");

        List<OrderDetail> details = new ArrayList<>();

        for (PosOrderDetailDTO detailDTO : request.getDetails()) {
            // 2. Tìm sản phẩm trong DailyStock để trừ kho
            DailyStock stock = dailyStockRepo.findByProduct_ProductIdAndDate(
                            detailDTO.getProductId(), LocalDate.now())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm mã " + detailDTO.getProductId() + " không có trên kệ!"));

            // 3. Kiểm tra tồn kho
            if (stock.getAvailableQuantity() < detailDTO.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + stock.getProduct().getName() + " không đủ số lượng!");
            }

            // 4. Trừ số lượng tồn kho
            stock.setAvailableQuantity(stock.getAvailableQuantity() - detailDTO.getQuantity());
            dailyStockRepo.save(stock);

            // 5. Tạo chi tiết đơn hàng
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(stock.getProduct());
            detail.setQuantity(detailDTO.getQuantity());
            // Sửa từ: detail.setPrice(detailDTO.getPrice());
            detail.setPrice(BigDecimal.valueOf(detailDTO.getPrice()));
            details.add(detail);
        }

        // 6. Gán danh sách chi tiết vào Order
        order.setOrderDetails(details);

        // 7. QUAN TRỌNG: Lưu và trả về đối tượng Order (để lấy ID)
        return orderRepo.save(order);
    }
}



