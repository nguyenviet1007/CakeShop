package bakery.controller;

import bakery.entity.Cart;
import bakery.entity.User;
import bakery.service.CartServiceImpl;
import bakery.service.OrderServiceImpl;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private CartServiceImpl cartService;

    @Autowired
    private OrderServiceImpl orderService; // Đảm bảo bạn đã tạo Service này

    /**
     * Bước 1: Tạo thông tin thanh toán và QR Code
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> createPayment(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để thanh toán.");
        }

        List<Cart> cartItems = cartService.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body("Giỏ hàng của bạn đang trống.");
        }

        // Tính tổng tiền (ép kiểu long để tránh lỗi double như bạn gặp phải)
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Thông tin ngân hàng của bạn
        String bankId = "MB";
        String accountNo = "0395551381"; // Thay bằng STK thật
        String accountName = "NGUYEN VAN A";
        String orderInfo = "DH" + System.currentTimeMillis(); // Mã đơn hàng tạm thời

        // Tạo URL VietQR
        String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankId, accountNo, totalAmount, orderInfo, accountName);

        Map<String, Object> response = new HashMap<>();
        response.put("qrUrl", qrUrl);
        response.put("total", totalAmount);
        response.put("orderInfo", orderInfo);

        return ResponseEntity.ok(response);
    }

    /**
     * Bước 2: Xác nhận đã chuyển khoản thành công
     * Thực hiện: Lưu đơn hàng thật và Xóa giỏ hàng
     */
    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<?> confirmOrder(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).build();

        List<Cart> cartItems = cartService.findByUserId(user.getId());
        if (cartItems.isEmpty()) return ResponseEntity.badRequest().body("Không có gì để xác nhận.");

        try {
            // 1. Lưu đơn hàng vào Database (Gọi qua Service)
            orderService.saveOrder(user, cartItems);

            // 2. Xóa sạch giỏ hàng của người dùng
            cartService.clearCart(user.getId());

            return ResponseEntity.ok("Đơn hàng đã được ghi nhận thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}