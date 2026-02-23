package bakery.controller;

import bakery.dto.request.IngredientDTO;
import bakery.entity.Ingredient;
import bakery.entity.IngredientStock;
import bakery.entity.IngredientStockHistory;
import bakery.repository.IngredientRepository;
import bakery.repository.IngredientStockHistoryRepository;
import bakery.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/ingredients")

public class IngredientController {

    private final IngredientService ingredientService;
    private final IngredientStockHistoryRepository historyRepo;
    private final IngredientRepository ingredientRepository;

    public IngredientController(
            IngredientService ingredientService,
            IngredientStockHistoryRepository historyRepo,
            IngredientRepository ingredientRepository) {
        this.ingredientService = ingredientService;
        this.historyRepo = historyRepo;
        this.ingredientRepository = ingredientRepository;
    }


    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page, // Trang hiện tại (mặc định 0)
                       @RequestParam(defaultValue = "5") int size  // Kích thước trang (mặc định 5)
    ) {
        // 1. Gọi Service để lấy Page đã được lọc và phân trang
        Page<Ingredient> ingredientPage = ingredientService.filterAndPaginate(keyword, status, page, size);

        // 2. Truyền dữ liệu ra View
        model.addAttribute("ingredientPage", ingredientPage);       // Dùng cho thanh phân trang
        model.addAttribute("ingredients", ingredientPage.getContent()); // Dùng cho bảng dữ liệu (List)

        // 3. Giữ lại các giá trị filter (để khi chuyển trang không bị mất filter)
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "admin/ingredient-list";
    }

    // --- THÊM VÀO CONTROLLER ---
    // Trả về form THÊM MỚI (Add)
    @GetMapping("/modal/add")
    public String getAddModal(Model model) {
        Ingredient ingredient = new Ingredient();
        // Khởi tạo stock để tránh lỗi null pointer khi trỏ tới minQuantity
        IngredientStock stock = new IngredientStock();
        stock.setMinQuantity(BigDecimal.TEN);
        ingredient.setStock(stock);

        model.addAttribute("ingredient", ingredient);
        return "admin/ingredient-form+"; // Trả về file bạn yêu cầu
    }

    // Trả về form CHỈNH SỬA (Edit)
    @GetMapping("/modal/edit/{id}")
    public String getEditModal(@PathVariable Long id, Model model) {
        Ingredient ingredient = ingredientService.findById(id);
        model.addAttribute("ingredient", ingredient);
        return "admin/ingredient-form+"; // Trả về file bạn yêu cầu
    }

    @PostMapping("/save")
    @ResponseBody // Quan trọng: Trả về JSON cho AJAX
    public ResponseEntity<?> save(@ModelAttribute Ingredient ingredient) {
        // Tạo map để chứa kết quả trả về
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Kiểm tra Add hay Edit
            if (ingredient.getIngredientId() == null) {
                // --- THÊM MỚI ---

                // Xử lý MinQuantity (tránh Null Pointer)
                BigDecimal minQty = BigDecimal.TEN; // Mặc định
                if (ingredient.getStock() != null && ingredient.getStock().getMinQuantity() != null) {
                    minQty = ingredient.getStock().getMinQuantity();
                }

                // Gọi hàm importNewIngredient của bạn
                ingredientService.importNewIngredient(
                        ingredient.getName(),
                        ingredient.getUnit(),
                        minQty
                );
            } else {
                // --- CẬP NHẬT ---
                ingredientService.updateIngredient(ingredient);
            }

            // 2. Nếu chạy đến đây là thành công -> Trả về JSON success
            response.put("status", "success");
            response.put("message", "Lưu dữ liệu thành công!");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 3. Bắt lỗi Validate (Trùng tên, Số âm...) -> Trả về lỗi 400
            response.put("status", "error");
            response.put("message", e.getMessage()); // Lấy message từ Service
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            // 4. Lỗi không mong muốn khác
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @GetMapping("/import")
    public String showImportPage(Model model) {
        // Lấy danh sách nguyên liệu để đổ vào thẻ <select>
        model.addAttribute("ingredients", ingredientService.findAll());
        return "admin/ingredient-import";
    }

    // 2. Xử lý API nhập kho (Nhận JSON từ Frontend)
    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<?> processImport(@RequestBody List<IngredientDTO> importList) {
        try {
            ingredientService.importStock(importList);
            // Trả về JSON để Frontend hiển thị SweetAlert
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Đã nhập kho thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 1. Hiển thị trang Xuất kho
    @GetMapping("/export")
    public String showExportPage(Model model) {
        // Cần lấy danh sách kèm theo số lượng tồn để hiển thị lên giao diện
        // Giả sử Ingredient entity có quan hệ OneToOne với Stock
        model.addAttribute("ingredients", ingredientService.findAll());
        return "admin/ingredient-export";
    }

    // 2. Xử lý API Xuất kho
    @PostMapping("/export")
    @ResponseBody
    public ResponseEntity<?> processExport(@RequestBody List<IngredientDTO> exportList) {
        try {
            ingredientService.exportStock(exportList);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã xuất kho thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ingredientService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa nguyên liệu và lịch sử liên quan thành công!");
        } catch (Exception e) {
            // Bắt mọi lỗi CSDL và ném ra màn hình cho người dùng thấy
            redirectAttributes.addFlashAttribute("error", "Không thể xóa! Nguyên liệu này có thể đang được dùng trong Công thức bánh.");
        }
        return "redirect:/admin/ingredients";
    }

    // Method mới: trả fragment HTML cho modal lịch sử (không phải full page)
    @GetMapping("/history/{id}")
    public String getHistory(@PathVariable Long id, Model model) {
        Ingredient ingredient = ingredientService.findById(id);
        List<IngredientStockHistory> historyList = historyRepo.findByIngredientIdOrderByUpdatedAtDesc(id);

        model.addAttribute("ingredient", ingredient);
        model.addAttribute("historyList", historyList);

        return "admin/ingredient-history";  // Trả full file này
    }


}
