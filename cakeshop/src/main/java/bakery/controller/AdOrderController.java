package bakery.controller;


import bakery.entity.Order;
import bakery.service.AdOrderService;
import bakery.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Controller
@RequestMapping("/manager/orders")
@Slf4j
public class AdOrderController {

    @Autowired
    private AdOrderService adOrderService;
    @Autowired
    private MailService mailService;
    // 1. Mở trang Quản lý đơn hàng
    @GetMapping
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status, // Thêm status để lọc nếu cần
            Model model) {

        // 1. ÉP BUỘC: Mỗi trang 10 dòng, Sắp xếp ID Giảm dần (Mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());

        // 2. Gọi hàm phân trang từ Service
        Page<Order> orderPage = adOrderService.getAllOrders(pageable);

        // 3. Đưa dữ liệu vào Model
        model.addAttribute("pageData", orderPage); // Dùng cho Fragment phân trang
        model.addAttribute("orders", orderPage.getContent()); // Danh sách 10 đơn hàng
        model.addAttribute("status", status); // Để giữ trạng thái lọc khi chuyển trang

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
    // AdOrderController.java
    @PostMapping("/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Order order = adOrderService.getOrderById(id);
            order.setStatus(status);

            // 1. Lưu DB trước
            adOrderService.saveOrder(order);

            // 2. Sau khi DB xong xuôi mới xử lý Mail
            if ("CANCELLED".equalsIgnoreCase(status)) {
                sendCancellationEmail(order);
            }

            return ResponseEntity.ok(Map.of("status", "success", "message", "Cập nhật thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void sendCancellationEmail(Order order) {
        try {
            if (order.getUser() != null && order.getUser().getEmail() != null) {
                mailService.send(order.getUser().getEmail(), "Hủy đơn", "Đơn hàng của bạn đã bị hủy");
            }
        } catch (Exception e) {
            // Chỉ ghi log, không ném ngoại lệ ra ngoài để tránh ảnh hưởng đến phản hồi của Controller
            log.error("Không gửi được mail: {}", e.getMessage());
        }
    }
    @GetMapping("/statistics")
    public String getStatistics(
            @RequestParam(value = "page", defaultValue = "0") int page, // Tên biến trên URL là 'page'
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "method", required = false) String method,
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        List<Order> allOrders = adOrderService.getAllOrders();
        List<Order> filteredOrders = new ArrayList<>();
        double totalSum = 0;

        for (Order o : allOrders) {
            // Kiểm tra ngày an toàn để tránh lỗi 400/500 khi dateStr rỗng
            boolean matchDate = true;
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    matchDate = o.getOrderDate().toLocalDate().equals(java.time.LocalDate.parse(dateStr));
                } catch (Exception e) { matchDate = false; }
            }

            boolean matchMethod = (method == null || method.isEmpty() || method.equalsIgnoreCase(o.getPayment()));

            boolean isOnline = (o.getUser() != null);
            boolean matchChannel = true;
            if ("POS".equals(channel)) matchChannel = !isOnline;
            else if ("ONLINE".equals(channel)) matchChannel = isOnline;

            boolean matchStatus = (status == null || status.isEmpty() || status.equalsIgnoreCase(o.getStatus()));

            boolean matchSearch = true;
            if (search != null && !search.isEmpty()) {
                String s = search.toLowerCase();
                String name = (o.getUser() != null) ? o.getUser().getName().toLowerCase() : "khách lẻ";
                matchSearch = o.getId().toString().contains(s) || name.contains(s);
            }

            if (matchDate && matchMethod && matchChannel && matchStatus && matchSearch) {
                filteredOrders.add(o);
                totalSum += (o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0);
            }
        }

        // Sắp xếp ID giảm dần (Mới nhất lên đầu)
        filteredOrders.sort((o1, o2) -> o2.getId().compareTo(o1.getId()));

        // Logic phân trang thủ công (10 đơn/trang)
        int pageSize = 10;
        int totalElements = filteredOrders.size();
        int start = Math.min(page * pageSize, totalElements);
        int end = Math.min(start + pageSize, totalElements);

        List<Order> pagedList = filteredOrders.subList(start, end);
        Page<Order> orderPage = new PageImpl<>(pagedList, PageRequest.of(page, pageSize), totalElements);

        model.addAttribute("orders", pagedList);
        model.addAttribute("pageData", orderPage); // Để pagination fragment hoạt động
        model.addAttribute("statTotal", totalSum);
        model.addAttribute("statCount", totalElements);

        return "admin/order-list :: orderTableFragment";
    }
    @GetMapping("/{id}/info")
    public String getOrderInfo(@PathVariable("id") Long id, Model model) {
        Order order = adOrderService.getOrderById(id);

        // Logic xử lý địa chỉ thông minh
        String displayAddress = order.getDeliveryAddress();

        // Nếu địa chỉ trong đơn trống VÀ đây là khách có tài khoản (Online)
        if ((displayAddress == null || displayAddress.trim().isEmpty()) && order.getUser() != null) {
            // Lấy địa chỉ từ bảng User
            displayAddress = order.getUser().getAddress();
        }

        // Nếu sau tất cả vẫn trống thì mới coi là nhận tại quầy
        if (displayAddress == null || displayAddress.trim().isEmpty()) {
            displayAddress = "Nhận tại quầy";
        }

        model.addAttribute("order", order);
        model.addAttribute("displayAddress", displayAddress); // Truyền địa chỉ đã xử lý sang View
        return "admin/order-info-fragment :: infoContent";
    }

}

