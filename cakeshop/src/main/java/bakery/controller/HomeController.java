package bakery.controller;


import bakery.entity.*;
import bakery.repository.*;
import bakery.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired private ProductRepository productRepository;

//    @Autowired private CategoryRepository categoryRepository;`

    @Autowired private OrderDetailRepository orderDetailRepository;

    @Autowired private FeedbackRepository feedbackRepository;

    @Autowired private FeedbackServiceImpl feedbackService;

    @Autowired private BlogRepository blogRepository;

    @Autowired private BlogServiceImpl blogService;

    @Autowired private FavoriteRepository favoriteRepository;


    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "0") int page,
                        HttpSession session,
                        Model model) {
        try {
            int size = 8; // Lưới 8 sản phẩm 1 trang theo yêu cầu
            Pageable pageable = PageRequest.of(page, size);

            // 1. Lấy danh sách danh mục (giữ nguyên để hiện navbar)
            List<String> categories = productRepository.findDistinctCategories();

            // 2. Lấy sản phẩm có phân trang
            Page<Product> productPage = productRepository.findAllByIsVisibleTrue(pageable);
            List<Product> products = productPage.getContent();

            // 3. Kiểm tra đăng nhập để xác định trạng thái "Yêu thích"
            User user = (User) session.getAttribute("user");
            if (user != null) {
                for (Product product : products) {
                    boolean isLiked = favoriteRepository.existsByUserAndProduct(user, product);
                    product.setIsFavorited(isLiked);
                }
            }

            // 4. Lấy sản phẩm giảm giá cho Banner (không phân trang)
            List<Product> discountedProducts = productRepository.findByDiscountPercentGreaterThan(0);

            // 5. Đưa dữ liệu ra view
            model.addAttribute("categories", categories);
            model.addAttribute("products", products); // Danh sách 8 sản phẩm
            model.addAttribute("discountedProducts", discountedProducts);

            // Dữ liệu phân trang
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());

            return "Home";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
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
    public String getProductsByCategory(@PathVariable("categoryName") String categoryName,
                                        @RequestParam(defaultValue = "0") int page,
                                        HttpSession session,
                                        Model model) {
        try {
            int size = 8;
            Pageable pageable = PageRequest.of(page, size);

            // 1. Lấy danh sách danh mục để hiện navbar
            model.addAttribute("categories", productRepository.findDistinctCategories());

            // 2. Lấy sản phẩm theo category có PHÂN TRANG
            Page<Product> productPage = productRepository.findByCategoryIgnoreCaseAndIsVisibleTrue(categoryName, pageable);
            List<Product> products = productPage.getContent();

            // 3. Kiểm tra trạng thái "Yêu thích" (Copy logic từ index qua)
            User user = (User) session.getAttribute("user");
            if (user != null) {
                for (Product product : products) {
                    boolean isLiked = favoriteRepository.existsByUserAndProduct(user, product);
                    product.setIsFavorited(isLiked);
                }
            }

            // 4. Đưa dữ liệu ra view
            model.addAttribute("products", products);
            model.addAttribute("currentCategoryName", categoryName.toUpperCase());

            // Dữ liệu phân trang
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());

            // Để view biết đang ở chế độ xem category nhằm tạo Link phân trang đúng
            model.addAttribute("isCategoryView", true);
            model.addAttribute("catName", categoryName);

            return "Home";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
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
    @GetMapping("/blogs")
    public String listBlogs(Model model) {
        List<Blog> blogs = blogRepository.findAll();
        model.addAttribute("categories", productRepository.findDistinctCategories());
        model.addAttribute("blogs", blogs);
        return "Home"; // Trả về trang Home nhưng có thêm list blog
    }
    @GetMapping("/blogs/{id}")
    public String getBlogDetail(@PathVariable Integer id, Model model) {
        // Gọi hàm getBlogById từ BlogServiceImpl đã có của bạn
        Blog blog = blogService.getBlogById(id);
        model.addAttribute("blog", blog);
        model.addAttribute("categories", productRepository.findDistinctCategories());
        return "blog-detail";
    }

}