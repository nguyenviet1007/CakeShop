package bakery.service;

import bakery.entity.*;
import bakery.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements  OrderService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private DailyStockRepository dailyStockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Transactional
    public void saveOrder(User user, List<Cart> cartItems, String paymentMethod,String voucherCode) {
        // 1. Khởi tạo Order
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setDeliveryAddress(user.getAddress());

        // Thiết lập trạng thái dựa trên phương thức thanh toán
        if ("COD".equals(paymentMethod)) {
            order.setStatus("UNPAID");
            order.setPayment("CASH");
        } else {
            order.setStatus("WAIT");
            order.setPayment("ONLINE PAYMENT");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> details = new ArrayList<>();

        // 2. Duyệt giỏ hàng để kiểm tra kho và tính tiền
        for (Cart item : cartItems) {
            // Kiểm tra an toàn: Tránh NullPointerException
            if (item.getProduct() == null) {
                throw new RuntimeException("Lỗi: Có sản phẩm trống trong giỏ hàng.");
            }

            Long productId = item.getProduct().getProductId();
            String productName = item.getProduct().getName(); // Giả sử có getName để log lỗi

            // Lấy tồn kho (Kết hợp: Tìm theo Product ID và Ngày hiện tại nếu hệ thống yêu cầu DailyStock)
            DailyStock stock = dailyStockRepository
                    .findByProduct_ProductIdAndDate(productId, LocalDate.now())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm '" + productName + "' (ID: " + productId + ") không có dữ liệu kho hôm nay."));

            // Kiểm tra số lượng tồn
            if (stock.getAvailableQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + productName + "' không đủ số lượng. Còn lại: " + stock.getAvailableQuantity());
            }

            // Cập nhật trừ kho ngay trong vòng lặp (Dirty Checking của Spring Data JPA sẽ tự save khi kết thúc Transaction)
            stock.setAvailableQuantity(stock.getAvailableQuantity() - item.getQuantity());
            dailyStockRepository.save(stock);

            // Tạo chi tiết đơn hàng
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(item.getProduct());
            detail.setQuantity(item.getQuantity());

            // Chốt giá tại thời điểm mua (Rất quan trọng)
            BigDecimal priceAtPurchase = item.getProduct().getPrice();
            detail.setPrice(priceAtPurchase);

            // Tính tổng tiền
            BigDecimal itemSubtotal = priceAtPurchase.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemSubtotal);

            details.add(detail);
        }

        if (voucherCode != null && !voucherCode.isEmpty()) {
            Voucher voucher = voucherRepository.findByCodeIgnoreCase(voucherCode).orElse(null);

            if (voucher != null && voucher.getActive() &&
                    totalAmount.compareTo(voucher.getMinOrderValue()) >= 0) {

                // Tính số tiền giảm: (Tổng * % / 100)
                BigDecimal discount = totalAmount.multiply(new BigDecimal(voucher.getDiscountPercent()))
                        .divide(new BigDecimal(100));

                // Kiểm tra mức giảm tối đa
                if (discount.compareTo(voucher.getMaxDiscount()) > 0) {
                    discount = voucher.getMaxDiscount();
                }

                // Trừ tiền giảm giá vào tổng đơn hàng
                totalAmount = totalAmount.subtract(discount);
            }
        }

        // 3. Hoàn tất và lưu vào Database
        order.setTotalAmount(totalAmount);

        // Lưu Order trước để sinh ID (Cần thiết cho quan hệ 1-N với OrderDetail)
        Order savedOrder = orderRepository.save(order);

        // Lưu hàng loạt chi tiết đơn hàng để tối ưu performance
        orderDetailRepository.saveAll(details);
        sendOrderConfirmationEmail(user, cartItems,totalAmount);
    }

    private void sendOrderConfirmationEmail(User user, List<Cart> items,BigDecimal totalAmount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("phanmanhtuan015@gmail.com");
        message.setTo(user.getEmail());
        message.setSubject("Xác nhận đơn hàng thành công - [Mã Đơn Hàng]");

        StringBuilder content = new StringBuilder();
        content.append("Chào ").append(user.getName()).append(",\n\n");
        content.append("Đơn hàng của bạn đã được tiếp nhận thành công.\n");
        content.append("Trạng thái: Đang chờ xử lý\n\n");
        content.append("Chi tiết giỏ hàng:\n");

        for (Cart item : items) {
            content.append("- ")
                    .append(item.getProduct().getName())
                    .append(" - ")
                    .append(item.getProduct().getPrice())
                    .append(" x ")
                    .append(item.getQuantity())
                    .append("\n");
        }
        content.append("Tổng hóa đơn: ").append(totalAmount).append(",\n\n");

        content.append("\nCảm ơn bạn đã mua sắm tại cửa hàng!");

        message.setText(content.toString());

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log lỗi nhưng không làm roll-back giao dịch chính nếu bạn muốn
            System.err.println("Lỗi gửi mail: " + e.getMessage());
        }
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
//    public List<Order> getOrdersByCustomer(Long id){
//
//        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
//
//        return orderRepository.findByUserId(user.getId());
//    }

    public boolean checkUserPurchasedProduct(Long userId, Long productId) {
        return orderRepository.hasUserPurchasedProduct(userId, productId);
    }
}