package bakery.controller;


import bakery.entity.*;
import bakery.repository.*;
import bakery.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired private ProductRepository productRepository;

//    @Autowired private CategoryRepository categoryRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private OrderRepository orderRepository;

    @Autowired private OrderDetailRepository orderDetailRepository;

    @Autowired private FeedbackRepository feedbackRepository;

    @Autowired private FeedbackServiceImpl feedbackService;

    @GetMapping("/")
    public String index(Model model) {
//        model.addAttribute("categories", categoryRepository.findAll());
//        model.addAttribute("products", productRepository.findAllWithImages());
//        return "Home";
        model.addAttribute("categories", productRepository.findDistinctCategories());
        model.addAttribute("products", productRepository.findAllWithImages());
        return "Home";
    }
//    @GetMapping("/category/{id}")
//    public String getProductsByCategory(@PathVariable Long id, Model model) {
//
//        List<Product> products = productRepository.findByCategoryWithImages(id);
//        List<Category> categories = categoryRepository.findAll();
//        Category currentCategory = categoryRepository.findById(id).get();
//
//        model.addAttribute("products", products);
//        model.addAttribute("categories", categories);
//        model.addAttribute("currentCategoryName", currentCategory.getName());
//
//        return "Home";
//    }

    @GetMapping("/category/{categoryName}")
    public String getProductsByCategory(@PathVariable("categoryName") String categoryName, Model model) {
        // Vẫn phải truyền danh sách categories để load thanh Menu
        model.addAttribute("categories", productRepository.findDistinctCategories());

        // Lấy danh sách sản phẩm thuộc category này
        model.addAttribute("products", productRepository.findByCategoryIgnoreCase(categoryName));

        // Truyền tên category hiện tại ra để đổi Title "BEST SELLING" thành tên Category
        model.addAttribute("currentCategoryName", categoryName.toUpperCase());

        return "Home";
    }

    @GetMapping("/product/{id}/fragment")
    public String getProductDetail(@PathVariable("id") Long productId, Model model, HttpSession session) {
        // 1. Lấy thông tin Product và gán vào model (code cũ của bạn)
        Product product = productRepository.findByProductId(productId);
        model.addAttribute("product", product);

        // 2. Lấy danh sách Feedback của riêng sản phẩm này
        List<Feedback> feedbacks = feedbackService.findByProductId(productId);
        model.addAttribute("feedbacks", feedbacks);

        // Tính điểm đánh giá trung bình
        double averageRating = feedbacks.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);
        model.addAttribute("averageRating", averageRating);

        // 3. XỬ LÝ LOGIC "CAN REVIEW" (CHỈ CHO NGƯỜI ĐÃ MUA VÀ HOÀN THÀNH ĐƠN ĐÁNH GIÁ)
        boolean canReview = false;
        User currentUser = (User) session.getAttribute("user"); // Lấy user đang đăng nhập

        if (currentUser != null) {
            // Kiểm tra khách đã mua thành công chưa (Order status = COMPLETED)
            boolean hasPurchased = orderDetailRepository.hasUserPurchasedProduct(currentUser.getId(), productId);

            // Kiểm tra xem khách đã đánh giá chưa (để tránh 1 người spam nhiều đánh giá)
            boolean hasReviewed = feedbackRepository.existsByUser_IdAndProduct_ProductId(currentUser.getId(), productId);

            // Chỉ cho phép đánh giá nếu: Đã mua thành công VÀ Chưa từng đánh giá
            if (hasPurchased && !hasReviewed) {
                canReview = true;
            }
        }

        // 4. Đẩy biến canReview ra View HTML
        model.addAttribute("canReview", canReview);

        return "product-details :: productDetail";
    }
    @PostMapping("/api/feedbacks/add")
    public String addFeedback(
            @RequestParam("productId") Long productId,
            @RequestParam("rating") int rating,
            @RequestParam("content") String content,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // 1. Lấy user đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập thì đẩy ra trang login
        }

        try {
            // 2. Kiểm tra lại bảo mật (Backend Validation)
            // Phòng trường hợp user dùng công cụ (như Postman) để bắn API ảo
            boolean hasPurchased = orderDetailRepository.hasUserPurchasedProduct(currentUser.getId(), productId);
            boolean hasReviewed = feedbackRepository.existsByUser_IdAndProduct_ProductId(currentUser.getId(), productId);

            if (!hasPurchased) {
                redirectAttributes.addFlashAttribute("error", "Bạn chưa mua sản phẩm này nên không thể đánh giá!");
                return "redirect:/product/" + productId;
            }
            if (hasReviewed) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã đánh giá sản phẩm này rồi!");
                return "redirect:/product/" + productId;
            }

            // 3. Tạo đối tượng Feedback mới và set dữ liệu
            Feedback feedback = new Feedback();
            feedback.setUser(currentUser);

            // Lấy Product từ DB để set vào Feedback
            Product product = productRepository.findByProductId(productId);
            feedback.setProduct(product);

            feedback.setRating(rating);
            feedback.setContent(content);
            feedback.setCreatedAt(LocalDateTime.now()); // Set thời gian hiện tại (nếu DB chưa tự tạo)

            // 4. Lưu vào Database
            feedbackRepository.save(feedback);

            // 5. Gửi thông báo thành công ra ngoài giao diện
            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá sản phẩm!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi gửi đánh giá, vui lòng thử lại!");
        }

        // Redirect về lại trang chi tiết sản phẩm
        return "redirect:/product/" + productId + "/fragment";
    }
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam("keyword") String keyword) {
        // Lấy danh sách Product từ Database
        List<Product> products = productRepository.findTop5ByNameContainingIgnoreCase(keyword); // (Hoặc tên hàm của bạn)

        // Map sang dạng JSON đơn giản (DTO) để tránh lỗi quan hệ 1-N
        List<Map<String, Object>> result = products.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", p.getProductId()); // Lưu ý tên getter cho product_id
            map.put("name", p.getName());
            map.put("price", p.getPrice());

            // Xử lý logic lấy ảnh chính từ list product_images
            String imgUrl = "chocolate-chip-cake0008choc-AAA.webp"; // Ảnh mặc định
            if (p.getImages() != null && !p.getImages().isEmpty()) {
                // Thử tìm ảnh có is_main = true
                boolean foundMain = false;
                for (ProductImage img : p.getImages()) {
                    if (Boolean.TRUE.equals(img.getIsMain())) {
                        imgUrl = img.getImageUrl();
                        foundMain = true;
                        break;
                    }
                }
                // Nếu không có ảnh is_main = true, lấy đại ảnh đầu tiên
                if (!foundMain) {
                    imgUrl = p.getImages().get(0).getImageUrl();
                }
            }
            map.put("imageUrl", imgUrl); // Trả thẳng chuỗi URL ảnh về cho JS

            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
    @GetMapping("/account/profile")
    public String profile(HttpSession session, Model model){

        User user = (User) session.getAttribute("user");

        model.addAttribute("customer", user);

        return "profile";
    }

    @GetMapping("/account/orders")
    public String orders(@RequestParam(required = false) String status,
                         HttpSession session,
                         Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }
        List<Order> orders;

        // nếu không chọn status -> lấy tất cả
        if (status == null || status.isEmpty()) {
            orders = orderRepository.findByUserId(user.getId());
        }
        // nếu có status -> lọc theo trạng thái
        else {
            orders = orderRepository.findByUserIdAndStatus(user.getId(), status);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        return "orders";
    }
    @GetMapping("/account/orders/{id}")
    public String orderDetail(
            @PathVariable Long id,
            Model model) {

        Order order = orderRepository.findById(id).orElse(null);

        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(id);

        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderDetails);

        return "order-detail";
    }

    @PostMapping("/account/orders/cancel/{id}")
    public String changeOrderStatus(@PathVariable("id") Long orderId, RedirectAttributes redirectAttributes) {
        try {
            // 1. Tìm đơn hàng theo ID
            Order order = orderRepository.findById(orderId).orElse(null);

            // 2. Kiểm tra điều kiện: Đơn hàng phải tồn tại và có trạng thái là UNPAID
            // (Lưu ý: Nếu status của bạn là Enum, hãy dùng .name() hoặc so sánh trực tiếp với Enum OrderStatus.UNPAID)
            if (order != null && "UNPAID".equals(order.getStatus().toString())) {

                // 3. Đổi trạng thái thành CANCELLED
                order.setStatus("CANCELLED"); // Hoặc order.setStatus(OrderStatus.CANCELLED) nếu dùng Enum

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
        return "redirect:/account/orders/" + orderId;
    }
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("customer") User formUser,
                                HttpSession session) {

        User user = (User) session.getAttribute("user");

        if(user == null){
            return "redirect:/login";
        }

        user.setName(formUser.getName());
        user.setEmail(formUser.getEmail());
        user.setPhone(formUser.getPhone());
        user.setAddress(formUser.getAddress());

        userRepository.save(user);

        session.setAttribute("user", user);

        return "redirect:/account/profile?success";
    }
}
