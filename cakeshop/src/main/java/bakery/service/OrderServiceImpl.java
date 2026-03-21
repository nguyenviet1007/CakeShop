package bakery.service;

import bakery.entity.*;
import bakery.repository.DailyStockRepository;
import bakery.repository.OrderDetailRepository;
import bakery.repository.OrderRepository;
import bakery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements  OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private DailyStockRepository dailyStockRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void saveOrder(User user, List<Cart> cartItems, String paymentMethod) {
        // 1. Tạo đối tượng Order mới
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        if ("COD".equals(paymentMethod)) {
            order.setStatus("UNPAID");
            order.setPayment("CASH");
            } else {
            order.setStatus("PAID");
            order.setPayment("ONLINE PAYMENT");
        }

        // 2. Tính tổng tiền và chuẩn bị danh sách chi tiết đơn hàng
        BigDecimal total = BigDecimal.ZERO;
        List<OrderDetail> details = new ArrayList<>();
        for (Cart item : cartItems) {

            DailyStock stock = dailyStockRepository
                    .findByProduct_ProductId(item.getProduct().getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho"));

            if (stock.getAvailableQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm không đủ số lượng trong kho");
            }
            stock.setAvailableQuantity(
                    stock.getAvailableQuantity() - item.getQuantity()
            );
            dailyStockRepository.save(stock);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());
            // Lưu giá tại thời điểm mua để tránh việc sau này sản phẩm đổi giá làm sai lệch hóa đơn
            BigDecimal priceAtPurchase = item.getProduct().getPrice();
            detail.setPrice(priceAtPurchase);
            BigDecimal itemTotal = priceAtPurchase.multiply(BigDecimal.valueOf(item.getQuantity()));
            // Cộng dồn vào tổng hóa đơn
            total = total.add(itemTotal);
            details.add(detail);
        }
        order.setTotalAmount(total);

        // 3. Lưu Order vào Database trước để lấy ID
        Order savedOrder = orderRepository.save(order);

        // 4. Lưu tất cả chi tiết đơn hàng
        orderDetailRepository.saveAll(details);
    }

    public void validateOrderInfo(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Thông tin người dùng không tồn tại");
        }
        // Case: Để trống số điện thoại
        if (user.getPhone()== null || user.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Không được để trống số điện thoại");
        }
        // Case: Để trống thông tin giao hàng (Địa chỉ)
        if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Phải cập nhật thông tin giao hàng");
        }
    }

    public void cancelOrder(Order deliveredOrder) {
        // Case: Trạng thái đơn hàng là "đã giao"
        if ("đã giao".equalsIgnoreCase(deliveredOrder.getStatus())) {
            throw new IllegalStateException("Đơn hàng đã giao không thể thay đổi");
        }
        deliveredOrder.setStatus("đã hủy");
        // orderRepository.save(deliveredOrder);
    }

    public void updateStatus(Order order, String newStatus) {
        // Case: Thay đổi trạng thái khi đã hủy
        if ("đã hủy".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng đã hủy không thể thay đổi");
        }

        // Ngăn chặn chuyển ngược từ "đã giao" sang các trạng thái khác
        if ("đã giao".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng đã giao không thể thay đổi");
        }

        order.setStatus(newStatus);
        // orderRepository.save(order);
    }

    public void processOrder(User user, ArrayList<User> arrayList) {
    }
    public Order findOrderById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Order ID không được để trống");
        }
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
    }
    public List<Order> getOrdersByCustomer(Long id){

        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        return orderRepository.findByUserId(user.getId());
    }

    public boolean checkUserPurchasedProduct(Long userId, Long productId) {
        return orderRepository.hasUserPurchasedProduct(userId, productId);
    }
}