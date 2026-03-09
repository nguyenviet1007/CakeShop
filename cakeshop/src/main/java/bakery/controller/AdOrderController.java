package bakery.controller;

import bakery.entity.Order;
import bakery.service.AdOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/orders")
public class AdOrderController {

    @Autowired
    private AdOrderService adOrderService;

    // 1. Mở trang Quản lý đơn hàng
    @GetMapping
    public String listOrders(Model model) {
        // Dùng hàm getAllOrders() của bạn
        List<Order> orders = adOrderService.getAllOrders();

        // Mẹo nhỏ: Đảo ngược danh sách để đơn hàng mới nhất (ID to nhất) nhảy lên đầu bảng
        Collections.reverse(orders);

        model.addAttribute("orders", orders);
        return "admin/order-list";
    }

    // 2. Trả về mã HTML của Chi tiết đơn hàng để nhúng vào Modal
    @GetMapping("/{id}/details")
    public String getOrderDetails(@PathVariable Long id, Model model) {
        // Dùng hàm getOrderById() của bạn
        Order order = adOrderService.getOrderById(id);
        model.addAttribute("order", order);
        return "admin/order-list :: modalDetailContent";
    }

    // 3. API Cập nhật trạng thái đơn hàng (Xác nhận / Hủy)
    @PostMapping("/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            // Bước 1: Tìm đơn hàng bằng hàm của bạn
            Order order = adOrderService.getOrderById(id);

            // Bước 2: Đổi trạng thái
            order.setStatus(status);

            // Bước 3: Lưu lại bằng hàm saveOrder() của bạn
            adOrderService.saveOrder(order);

            return ResponseEntity.ok(Map.of("status", "success", "message", "Cập nhật thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
