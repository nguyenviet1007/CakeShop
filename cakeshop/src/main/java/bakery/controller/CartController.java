package bakery.controller;

import bakery.entity.Cart;
import bakery.entity.User;
import bakery.service.CartServiceImpl;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartServiceImpl cartService;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam Long productId,
                                       @RequestParam Integer quantity,
                                       HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập!");
        }

        cartService.addToCart(user.getId(), productId, quantity);

        return ResponseEntity.ok(cartService.findByUserId(user.getId()));
    }

    @GetMapping("/items")
    public ResponseEntity<?> getCartItems(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(cartService.findByUserId(user.getId()));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateQuantity(@RequestParam Long cartId, @RequestParam Integer quantity) {
        cartService.updateQuantity(cartId, quantity);
        return ResponseEntity.ok("Success");
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Vui lòng đăng nhập");

        if (user.getAddress() == null || user.getAddress().trim().isEmpty()
                || user.getPhone() == null || user.getPhone().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body("Vui lòng cập nhật địa chỉ và số điện thoại trước khi thanh toán");
        }

        List<Cart> items = cartService.findByUserId(user.getId());
        if (items.isEmpty()) return ResponseEntity.badRequest().body("Giỏ hàng trống");

        // 1. Tính tổng tiền
        // Tính tổng tiền từ danh sách giỏ hàng
        BigDecimal totalAmount = items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Tạo thông tin QR (VietQR format)
        // Cú pháp: https://img.vietqr.io/image/<BANK_ID>-<ACCOUNT_NO>-<TEMPLATE>.png?amount=<AMOUNT>&addInfo=<INFO>
        String bankId = "MB";
        String accountNo = "1023100704";
        String template = "qr_only";
        String info = "Thanh toan don hang " + System.currentTimeMillis();

        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-%s.png?amount=%s&addInfo=%s",
                bankId,
                accountNo,
                template,
                totalAmount.toPlainString(),
                info
        );
        return ResponseEntity.ok(Map.of("qrUrl", qrUrl, "total", totalAmount));
    }
}

