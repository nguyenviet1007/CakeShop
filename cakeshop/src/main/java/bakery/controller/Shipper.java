package bakery.controller;

import bakery.entity.Order;
import bakery.entity.ShippingFee;
import bakery.entity.User;
import bakery.repository.FeeRepository;
import bakery.repository.OrderRepository;
import bakery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/shipper")
public class Shipper {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ================= DASHBOARD =================
    // ================= DASHBOARD =================
    @GetMapping("")
    public String dashboard(
            Model model,
            Principal principal,

            @RequestParam(defaultValue = "0") int pageAssigned,
            @RequestParam(defaultValue = "0") int pageShipping,

            @RequestParam(defaultValue = "5") int size, // mặc định 5

            @RequestParam(defaultValue = "") String keywordAssigned,
            @RequestParam(defaultValue = "") String keywordShipping
    ) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();

        User shipper = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        // ================= ASSIGNED =================
        Pageable assignedPageable = PageRequest.of(pageAssigned, size);

        Page<Order> assignedPage = orderRepository
                .searchAssigned(keywordAssigned, assignedPageable);

        // ================= SHIPPING =================
        Pageable shippingPageable = PageRequest.of(pageShipping, size);

        Page<Order> shippingPage = orderRepository
                .searchShipping(shipper, keywordShipping, shippingPageable);

        // ================= MODEL =================
        model.addAttribute("assignedPage", assignedPage);
        model.addAttribute("shippingPage", shippingPage);

        model.addAttribute("keywordAssigned", keywordAssigned);
        model.addAttribute("keywordShipping", keywordShipping);

        model.addAttribute("shipper", shipper);

        return "shipper";
    }

    // ================= ORDER DETAIL =================
    @GetMapping("/order/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        String username = principal.getName();

        User currentShipper = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        Order order = orderRepository
                .findDetailById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean canView = "ASSIGNED".equals(order.getStatus())
                || (order.getShipper() != null
                && order.getShipper().getId().equals(currentShipper.getId()));

        if (!canView) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem đơn hàng này");
            return "redirect:/shipper";
        }

        model.addAttribute("order", order);
        return "shipper-order-detail";
    }

    // ================= RECEIVE ORDER =================
    @PostMapping("/receive/{id}")
    public String receiveOrder(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();

        User shipper = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"ASSIGNED".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể nhận đơn ở trạng thái ASSIGNED");
            return "redirect:/shipper";
        }

        order.setStatus("SHIPPING");
        order.setShipper(shipper);

        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("success", "✅ Nhận đơn thành công");
        return "redirect:/shipper/order/" + id;
    }

    // ================= DELIVERED =================
    @PostMapping("/delivered/{id}")
    public String delivered(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        ShippingFee fee = feeRepository.findTopByOrderByIdDesc();

        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();

        User shipper = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getShipper() == null || !order.getShipper().getId().equals(shipper.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền cập nhật đơn này");
            return "redirect:/shipper";
        }

        if (!"SHIPPING".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể giao thành công đơn đang ở trạng thái SHIPPING");
            return "redirect:/shipper";
        }

        order.setStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now());
        order.setDeliveredFee(fee.getDeliveredFee());
        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("success", "✅ Đơn hàng đã giao thành công");
        return "redirect:/shipper";
    }

    // ================= FAILED =================
    @PostMapping("/failed/{id}")
    public String failed(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        ShippingFee fee = feeRepository.findTopByOrderByIdDesc();

        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();

        User shipper = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getShipper() == null || !order.getShipper().getId().equals(shipper.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền cập nhật đơn này");
            return "redirect:/shipper";
        }

        if (!"SHIPPING".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể báo thất bại cho đơn đang ở trạng thái SHIPPING");
            return "redirect:/shipper";
        }

        order.setStatus("FAILED");
        order.setFailureReason(reason);
        order.setDeliveredAt(LocalDateTime.now());
        order.setFailedFee(fee.getFailedFee());
        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("success", "✅ Đã cập nhật đơn thất bại");
        return "redirect:/shipper";
    }

    // ================= PROFILE =================
    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();

        User shipper = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        model.addAttribute("shipper", shipper);

        return "shipper-profile";
    }

    // ================= CHANGE PASSWORD PAGE =================
    @GetMapping("/change-password")
    public String changePasswordPage(Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        return "shipper-change-password";
    }

    // ================= CHANGE PASSWORD =================
    @PostMapping("/change-password")
    public String changePassword(
            Principal principal,
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            model.addAttribute("error", "❌ Mật khẩu cũ không đúng");
            return "shipper-change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "❌ Mật khẩu mới không trùng");
            return "shipper-change-password";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("error", "❌ Mật khẩu mới phải có ít nhất 6 ký tự");
            return "shipper-change-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "✅ Đổi mật khẩu thành công, vui lòng đăng nhập lại");
        return "redirect:/do-logout";
    }

    // ================= EDIT PROFILE PAGE =================
    @GetMapping("/edit")
    public String editProfile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();

        User shipper = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        model.addAttribute("shipper", shipper);

        return "shipper-edit";
    }

    // ================= UPDATE PROFILE =================
    @PostMapping("/edit")
    public String updateProfile(
            Principal principal,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String address,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();

        User shipper = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));
        if (userRepository.existsByEmail(email)
                && !email.equals(shipper.getEmail())) {

            return "redirect:/shipper/edit?error";
        }

        shipper.setName(name);
        shipper.setEmail(email);
        shipper.setPhone(phone);
        shipper.setAddress(address);

        userRepository.save(shipper);

        redirectAttributes.addFlashAttribute("success", "✅ Cập nhật thông tin thành công!");
        return "redirect:/shipper/profile";
    }

    // ================= CANCEL ORDER =================
    @PostMapping("/cancel/{id}")
    public String cancelOrder(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();

        User shipper = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getShipper() == null || !order.getShipper().getId().equals(shipper.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền huỷ nhận đơn này");
            return "redirect:/shipper";
        }

        if (!"SHIPPING".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể huỷ nhận đơn đang ở trạng thái SHIPPING");
            return "redirect:/shipper";
        }

        order.setStatus("ASSIGNED");
        order.setShipper(null);

        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("success", "✅ Đã trả đơn về danh sách ASSIGNED");
        return "redirect:/shipper";
    }

    // ================= HISTORY =================
    @GetMapping("/history")
    public String history(
            Model model,
            Principal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        // --- LẤY PHÍ MỚI NHẤT TỪ REPOSITORY CỦA BẠN ---
        ShippingFee latestFee = feeRepository.findTopByOrderByIdDesc();

        // Nếu chưa có cấu hình nào trong DB, mặc định là 0
        double currentDelivered = (latestFee != null) ? latestFee.getDeliveredFee() : 0.0;
        double currentFailed = (latestFee != null) ? latestFee.getFailedFee() : 0.0;

        model.addAttribute("currentDeliveredFee", currentDelivered);
        model.addAttribute("currentFailedFee", currentFailed);
        // ----------------------------------------------

        // ... (Giữ nguyên logic lấy shipper và filteredOrders của bạn) ...
        String username = principal.getName();
        User shipper = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        List<Order> orders = orderRepository.findByShipperAndStatusIn(shipper, List.of("DELIVERED", "FAILED" ,"COMPLETED"));

        List<Order> filteredOrders = orders.stream()
                .filter(o -> {
                    if (month == null || year == null) return true;
                    if (o.getDeliveredAt() == null) return false;
                    return o.getDeliveredAt().getMonthValue() == month && o.getDeliveredAt().getYear() == year;
                })
                .toList();

        // Tính tổng lương dựa trên phí lưu trong từng Order
        double totalSalary = 0;
        for (Order o : filteredOrders) {
            if ("DELIVERED".equals(o.getStatus()) || "COMPLETED".equals(o.getStatus()) ) {
                totalSalary += (o.getDeliveredFee() != null ? o.getDeliveredFee() : 0);
            } else if ("FAILED".equals(o.getStatus())) {
                totalSalary += (o.getFailedFee() != null ? o.getFailedFee() : 0);
            }
        }

        model.addAttribute("historyOrders", filteredOrders);
        model.addAttribute("totalSalary", totalSalary);
        model.addAttribute("month", month);
        model.addAttribute("year", year);

        return "shipper-history";
    }
}