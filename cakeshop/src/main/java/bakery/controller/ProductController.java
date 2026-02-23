package bakery.controller;

import bakery.entity.Ingredient;
import bakery.entity.Product;
import bakery.entity.Recipe;
import bakery.repository.IngredientRepository;
import bakery.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    private final ProductService productService;
    private final IngredientRepository ingredientRepository; // 1. Khai báo thêm

    public ProductController(ProductService productService, IngredientRepository ingredientRepository) {
        this.productService = productService;
        this.ingredientRepository = ingredientRepository;
    }

    // Trang danh sách (giữ nguyên)
    @GetMapping
    public String productList(@RequestParam(required = false) Long id,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) String sort,
                              @RequestParam(defaultValue = "0") int page, // Trang mặc định là 0
                              @RequestParam(defaultValue = "10") int size, // 5 sản phẩm/trang
                              Model model) {

        // Gọi hàm filter mới trả về Page
        Page<Product> productPage = productService.filter(id, keyword, category, sort, page, size);

        // Đẩy dữ liệu ra View
        model.addAttribute("productPage", productPage);       // Để dùng cho Fragment phân trang
        model.addAttribute("products", productPage.getContent()); // Để loop hiển thị bảng (List)

        // Giữ lại các tham số tìm kiếm để khi chuyển trang không bị mất
        model.addAttribute("id", id);
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("sort", sort);

        return "product-list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("product", new Product());
        // 3. Gửi list nguyên liệu sang View
        model.addAttribute("allIngredients", ingredientRepository.findAll());
        return "product-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productService.getById(id);
        if (product == null) {
            return "redirect:/admin/products";
        }
        model.addAttribute("product", product);
        // 4. Gửi list nguyên liệu sang View (cả khi edit cũng cần)
        model.addAttribute("allIngredients", ingredientRepository.findAll());
        return "product-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Product product,
                       BindingResult bindingResult,
                       @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
                       @RequestParam(value = "mainImageId", required = false) Long mainImageId,
                       @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
                       @RequestParam(value = "recipesJson", required = false) String recipesJson, // MỚI: Hứng chuỗi JSON từ form
                       Model model,
                       RedirectAttributes redirectAttributes) throws IOException {

        // Nếu có lỗi validation từ Bean Validation (@NotBlank, @Size, v.v.)
        if (bindingResult.hasErrors()) {
            // Reload images nếu đang edit
            if (product.getProductId() != null) {
                Product existing = productService.getById(product.getProductId());
                if (existing != null) {
                    product.setImages(existing.getImages());
                }
            }
            return "product-form"; // Có thể bị mất tạm danh sách nguyên liệu trên giao diện nếu lỗi
        }

        try {
            // [QUAN TRỌNG] Truyền thêm recipesJson vào Service
            productService.save(product, imageFiles, mainImageId, deleteImageIds, recipesJson);

            redirectAttributes.addFlashAttribute("success", "Lưu sản phẩm thành công!");
            return "redirect:/admin/products";

        } catch (IllegalArgumentException ex) {
            // Thêm lỗi unique name vào field "name" để hiển thị dưới input
            bindingResult.rejectValue("name", "duplicate.name", ex.getMessage());

            // Reload images nếu đang edit (để giữ lại list ảnh khi quay lại form)
            if (product.getProductId() != null) {
                Product existing = productService.getById(product.getProductId());
                if (existing != null) {
                    product.setImages(existing.getImages());
                }
            }

            model.addAttribute("product", product);
            return "product-form";  // Quay lại form với lỗi

        } catch (Exception ex) {
            // Catch các lỗi bất ngờ khác (nếu có)
            bindingResult.reject("global", "Có lỗi xảy ra khi lưu: " + ex.getMessage());
            model.addAttribute("product", product);
            return "product-form";
        }
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công!");
        return "redirect:/admin/products";
    }
    @GetMapping("/production-plan")
    public String productionPlan(Model model) {
        // 1. Lấy tất cả Sản phẩm (Bánh) và Nguyên liệu
        List<Product> allProducts = productService.getAll();
        List<Product> products = allProducts.stream()
                .filter(p -> p.getRecipes() != null && !p.getRecipes().isEmpty())
                .collect(Collectors.toList());

        List<Ingredient> ingredients = ingredientRepository.findAll();

        // 2. Gom Tồn kho vào Map: { ingredientId : { ten: "...", tonKho: ..., donVi: "..." } }
        Map<Long, Map<String, Object>> khoNguyenLieu = new HashMap<>();
        for (Ingredient ing : ingredients) {
            Map<String, Object> info = new HashMap<>();
            info.put("ten", ing.getName());
            info.put("donVi", ing.getUnit());
            info.put("tonKho", ing.getStock() != null ? ing.getStock().getQuantity() : 0);
            khoNguyenLieu.put(ing.getIngredientId(), info);
        }

        // 3. Gom Công thức vào Map: { productId : [ { maNL: 1, dinhLuong: 0.5 }, ... ] }
        Map<Long, List<Map<String, Object>>> congThuc = new HashMap<>();
        for (Product p : products) {
            List<Map<String, Object>> recipeList = new ArrayList<>();
            if (p.getRecipes() != null) {
                for (Recipe r : p.getRecipes()) {
                    if (r.getIngredient() != null) {
                        Map<String, Object> rInfo = new HashMap<>();
                        rInfo.put("maNL", r.getIngredient().getIngredientId());
                        rInfo.put("dinhLuong", r.getAmount());
                        recipeList.add(rInfo);
                    }
                }
            }
            congThuc.put(p.getProductId(), recipeList);
        }

        // 4. Đẩy sang Giao diện (View)
        model.addAttribute("products", products);
        model.addAttribute("khoNguyenLieuJson", khoNguyenLieu);
        model.addAttribute("congThucJson", congThuc);

        return "admin/production-plan"; // Trỏ tới file HTML sẽ tạo ở bước 2
    }
    // === [MỚI] API 1: Ẩn/Hiện sản phẩm (Dùng trên màn Tủ Bánh) ===
    @PostMapping("/toggle-visibility/{id}")
    @ResponseBody
    public ResponseEntity<?> toggleVisibility(@PathVariable Long id) {
        try {
            Product product = productService.getById(id);
            if (product == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Không tìm thấy bánh!"));
            }

            // Đảo ngược trạng thái: Đang true thành false, đang false thành true
            product.setIsVisible(!product.getIsVisible());

            // Lưu lại (Bạn cần tạo một hàm lưu không ảnh hưởng ảnh/công thức.
            // Nếu productService chưa có, có thể gọi tạm productRepository trong Service)
            productService.saveQuickInfo(product);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "isVisible", product.getIsVisible(),
                    "message", product.getIsVisible() ? "Đã hiện bánh trên Web!" : "Đã ẩn bánh khỏi Web!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    //  Cập nhật Giảm giá (Dùng trên màn Tủ Bánh) ===
    @PostMapping("/update-discount/{id}")
    @ResponseBody
    public ResponseEntity<?> updateDiscount(@PathVariable Long id, @RequestParam Integer percent) {
        try {
            if (percent == null || percent < 0 || percent > 100) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Mức giảm giá không hợp lệ (0-100%)"));
            }

            Product product = productService.getById(id);
            if (product == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Không tìm thấy bánh!"));
            }

            product.setDiscountPercent(percent);
            productService.saveQuickInfo(product);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "discount", percent,
                    "message", "Đã cập nhật giảm giá thành công!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
