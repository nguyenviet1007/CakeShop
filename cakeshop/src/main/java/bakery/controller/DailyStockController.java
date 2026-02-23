package bakery.controller;

import bakery.dto.request.DailyStockUpdateDTO;
import bakery.entity.DailyStock;
import bakery.entity.Product;
import bakery.service.DailyStockService;
import bakery.service.ProductService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/daily-stock")
public class DailyStockController {

    private final ProductService productService;
    private final DailyStockService dailyStockService;

    public DailyStockController(ProductService productService, DailyStockService dailyStockService) {
        this.productService = productService;
        this.dailyStockService = dailyStockService;
    }

    @GetMapping
    public String viewDailyStock(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        if (date == null) {
            date = LocalDate.now();
        }

        List<Product> products = productService.getAll();
        List<DailyStock> stocks = dailyStockService.getStockByDate(date);

        // ĐỔI TỪ Map<Long, Integer> SANG Map<Long, DailyStock>
        Map<Long, DailyStock> stockMap = new HashMap<>();
        for (DailyStock stock : stocks) {
            stockMap.put(stock.getProduct().getProductId(), stock);
        }

        model.addAttribute("selectedDate", date);
        model.addAttribute("products", products);
        model.addAttribute("stockMap", stockMap);

        return "admin/daily-stock";
    }

    // 2. API Lưu dữ liệu (Nhận JSON từ JavaScript)
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> saveStock(@RequestBody DailyStockUpdateDTO dto) {
        try {
            // BẢO VỆ NGHIỆP VỤ: Không cho phép lưu ngày trong quá khứ
            if (dto.getDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "Không được phép chỉnh sửa số liệu của ngày trong quá khứ!"));
            }

            dailyStockService.saveDailyStock(dto);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Lưu thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
