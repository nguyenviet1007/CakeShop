package bakery.controller;


import bakery.entity.Order;
import bakery.service.AdOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
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
    public String getOrderDetails(@PathVariable("id") Long id, Model model) {
        // 1. Lấy dữ liệu
        Order order = adOrderService.getOrderById(id);

        // 2. Kiểm tra null đơn giản
        if (order == null) {
            // Có thể tạo 1 file error.html nhỏ hoặc trả về trang trống
            return "fragments/invoice-template :: invoiceContent";
        }

        // 3. Đưa vào model
        model.addAttribute("order", order);

        // 4. Trả về đúng đường dẫn
        return "fragments/invoice-template :: invoiceContent";
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
    @GetMapping("/statistics")
    public String getStatistics(
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "method", required = false) String method,
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        List<Order> allOrders = adOrderService.getAllOrders();
        List<Order> filteredOrders = new ArrayList<>();
        double totalAmount = 0;

        for (Order o : allOrders) {
            // 1. Lọc Ngày (Nếu dateStr rỗng thì luôn đúng)
            boolean matchDate = true;
            if (dateStr != null && !dateStr.isEmpty()) {
                matchDate = o.getOrderDate().toLocalDate().equals(java.time.LocalDate.parse(dateStr));
            }

            // 2. Lọc Phương thức (CASH/TRANSFER)
            boolean matchMethod = (method == null || method.isEmpty() || method.equalsIgnoreCase(o.getPayment()));

            // 3. Lọc Kênh bán (Dựa trên user_id)
            boolean isOnline = (o.getUser() != null);
            boolean matchChannel = true;
            if ("POS".equals(channel)) matchChannel = !isOnline;
            else if ("ONLINE".equals(channel)) matchChannel = isOnline;

            // 4. Lọc Trạng thái
            boolean matchStatus = (status == null || status.isEmpty() || status.equalsIgnoreCase(o.getStatus()));

            // 5. Tìm kiếm nhanh (Theo ID hoặc Tên khách)
            boolean matchSearch = true;
            if (search != null && !search.isEmpty()) {
                String searchLower = search.toLowerCase();
                String customerName = (o.getUser() != null) ? o.getUser().getName().toLowerCase() : "khách lẻ";
                matchSearch = o.getId().toString().contains(searchLower) || customerName.contains(searchLower);
            }

            // TỔNG HỢP ĐIỀU KIỆN
            if (matchDate && matchMethod && matchChannel && matchStatus && matchSearch) {
                filteredOrders.add(o);
                totalAmount += (o.getTotalAmount() != null
                        ? o.getTotalAmount().doubleValue()
                        : 0.0);            }
        }

        model.addAttribute("orders", filteredOrders);
        model.addAttribute("statTotal", totalAmount);
        model.addAttribute("statCount", filteredOrders.size());

        return "admin/order-list :: orderTableFragment";
    }
}

