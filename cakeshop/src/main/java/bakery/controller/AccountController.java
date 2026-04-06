package bakery.controller;

import bakery.entity.*;
import bakery.repository.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class AccountController {

    @Autowired
    private UserRepository userRepository;

    @Autowired private OrderRepository orderRepository;

    @Autowired private OrderDetailRepository orderDetailRepository;

    @Autowired private FavoriteRepository favoriteRepository;

    @Autowired private ProductRepository productRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model){
        User user = (User) session.getAttribute("user");
        model.addAttribute("customer", user);
        return "profile";
    }
    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String status,
                         @RequestParam(defaultValue = "0") int page, // Trang hiện tại (mặc định là 0)
                         @RequestParam(defaultValue = "5") int size, // Số bản ghi mỗi trang
                         HttpSession session,
                         Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Tạo đối tượng Pageable (Sắp xếp theo ngày tạo mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Order> orderPage;

        if (status == null || status.isEmpty()) {
            orderPage = orderRepository.findByUserId(user.getId(), pageable);
        } else {
            orderPage = orderRepository.findByUserIdAndStatus(user.getId(), status, pageable);
        }

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("totalItems", orderPage.getTotalElements());
        model.addAttribute("currentStatus", status);
        model.addAttribute("pageSize", size);

        return "orders";
    }
    @GetMapping("/orders/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Model model) {

        Order order = orderRepository.findById(id).orElse(null);

        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(id);

        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderDetails);

        return "order-detail";
    }

    @PostMapping("/orders/cancel/{id}")
    public String changeOrderStatus(@PathVariable("id") Long orderId, RedirectAttributes redirectAttributes) {
        try {
            // 1. Tìm đơn hàng theo ID
            Order order = orderRepository.findById(orderId).orElse(null);

            // 2. Kiểm tra điều kiện: Đơn hàng phải tồn tại và có trạng thái là UNPAID
            if (order != null && "UNPAID".equals(order.getStatus().toString())) {

                // 3. Đổi trạng thái thành CANCELLED
                order.setStatus("FAILED");

                // 4. Lưu lại vào database
                orderRepository.save(order);

                redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn hàng này do trạng thái không hợp lệ.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi hủy đơn hàng.");
        }

        // Redirect người dùng quay lại trang chi tiết của chính đơn hàng đó
        return "redirect:/orders/" + orderId;
    }
    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("customer") User formUser,
                                BindingResult result,
                                HttpSession session,
                                Model model,
                                RedirectAttributes ra) {

        User user = (User) session.getAttribute("user");

        if(user == null){
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("customer", formUser);
            return "profile";
        }

        try {
            // Cập nhật logic lưu trữ
            user.setName(formUser.getName());
            user.setPhone(formUser.getPhone());
            user.setAddress(formUser.getAddress());
            userRepository.save(user);

            // Cập nhật session
            session.setAttribute("user", user);

            // Gửi thông báo thành công
            ra.addFlashAttribute("message", "Cập nhật thông tin thành công!");
            ra.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            ra.addFlashAttribute("message", "Có lỗi xảy ra: " + e.getMessage());
            ra.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/profile?success";
    }
    @GetMapping("/favorites")
    public String viewFavorites(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Lấy danh sách favorite của user từ Repository
        List<Favorite> favorites = favoriteRepository.findAllByUser(user);
        model.addAttribute("favoriteProducts", favorites);

        return "favorites";
    }
    @PostMapping("/favorites/toggle/{productId}")
    public ResponseEntity<?> toggleFavorite(@PathVariable Long productId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Vui lòng đăng nhập");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        Optional<Favorite> existing = favoriteRepository.findByUserAndProduct(user, product);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return ResponseEntity.ok(false);
        } else {
            Favorite fav = new Favorite();
            fav.setUser(user);
            fav.setProduct(product);
            favoriteRepository.save(fav);
            return ResponseEntity.ok(true);
        }
    }
    @PostMapping("/favorites/delete/{productId}")
    public ResponseEntity<?> deleteFavorite(@PathVariable Long productId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Vui lòng đăng nhập");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        Optional<Favorite> existing = favoriteRepository.findByUserAndProduct(user, product);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return ResponseEntity.ok("Đã xóa sản phẩm khỏi danh sách yêu thích");
        }

        return ResponseEntity.status(404).body("Sản phẩm không có trong danh sách yêu thích");
    }
    @GetMapping("/change-password")
    public String changePasswordPage(Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        return "change_password";
    }

    @PostMapping("/change-password/update")
    public String changePassword(
            Principal principal,
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword, // Thêm tham số này để khớp với form
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            model.addAttribute("message", "Mật khẩu cũ không chính xác!");
            model.addAttribute("messageType", "danger");
            return "change_password";
        }

        // 2. Kiểm tra mật khẩu mới trùng khớp (Backend check)
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("message", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("messageType", "danger");
            return "change_password";
        }

        // 3. Kiểm tra độ dài mật khẩu
        if (newPassword.length() < 6) {
            model.addAttribute("message", "Mật khẩu mới phải có ít nhất 6 ký tự!");
            model.addAttribute("messageType", "danger");
            return "change_password";
        }

        // 4. Lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Thông báo thành công và điều hướng (Có thể logout hoặc giữ lại tùy bạn)
        redirectAttributes.addFlashAttribute("message", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/login"; // Hoặc "redirect:/do-logout" nếu bạn muốn bắt đăng nhập lại ngay
    }
}

