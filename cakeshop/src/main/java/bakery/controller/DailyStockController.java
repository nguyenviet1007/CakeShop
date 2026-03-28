package bakery.controller;

import bakery.dto.request.DailyStockUpdateDTO;
import bakery.entity.DailyStock;
import bakery.entity.Product;
import bakery.entity.StockAdjustmentLog;
import bakery.repository.DailyStockRepository;
import bakery.repository.StockAdjustmentLogRepository;
import bakery.service.DailyStockService;
import bakery.service.ProductService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager/daily-stock")
public class DailyStockController {

    private final ProductService productService;
    private final DailyStockService dailyStockService;
    private final DailyStockRepository dailyStockRepository;
    private final StockAdjustmentLogRepository stockAdjustmentLogRepository;

    public DailyStockController(ProductService productService, DailyStockService dailyStockService, DailyStockRepository dailyStockRepository, StockAdjustmentLogRepository stockAdjustmentLogRepository) {
        this.productService = productService;
        this.dailyStockService = dailyStockService;
        this.dailyStockRepository = dailyStockRepository;
        this.stockAdjustmentLogRepository = stockAdjustmentLogRepository;
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

        Map<Long, DailyStock> stockMap = new HashMap<>();
        for (DailyStock stock : stocks) {
            stockMap.put(stock.getProduct().getProductId(), stock);
        }

        Set<String> categories = products.stream()
                .map(Product::getCategory)
                .filter(cat -> cat != null && !cat.isEmpty())
                .collect(Collectors.toSet());

        model.addAttribute("selectedDate", date);
        model.addAttribute("products", products);
        model.addAttribute("stockMap", stockMap);
        model.addAttribute("categories", categories); // Gửi sang HTML

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
    @GetMapping("/daily-report-view")
    public String getDailyReportView(@RequestParam("date") String dateStr, Model model) {
        LocalDate date = LocalDate.parse(dateStr);
        List<DailyStock> stocks = dailyStockRepository.findByDate(date);

        List<Map<String, Object>> reportData = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalInitial = 0;
        int totalSold = 0;
        int totalRemaining = 0;

        for (DailyStock s : stocks) {
            int initial = (s.getInitialQuantity() != null) ? s.getInitialQuantity() : 0;
            int remaining = (s.getAvailableQuantity() != null) ? s.getAvailableQuantity() : 0;
            int sold = Math.max(0, initial - remaining);

            BigDecimal revenue = s.getProduct().getPrice().multiply(BigDecimal.valueOf(sold));

            Map<String, Object> item = new HashMap<>();
            item.put("productName", s.getProduct().getName());
            item.put("initial", initial);
            item.put("remaining", remaining);
            item.put("sold", sold);
            item.put("revenue", revenue);

            reportData.add(item);

            // Cộng dồn các con số tổng
            totalInitial += initial;
            totalSold += sold;
            totalRemaining += remaining;
            totalRevenue = totalRevenue.add(revenue);
        }

        model.addAttribute("reportData", reportData);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("sumInitial", totalInitial);
        model.addAttribute("sumSold", totalSold);
        model.addAttribute("sumRemaining", totalRemaining);

        return "admin/daily-report-fragment :: reportContent";
    }
    @PostMapping("/adjust-stock")
    @ResponseBody
    public ResponseEntity<?> adjustStock(
            @RequestParam Long stockId,
            @RequestParam int amount,
            @RequestParam String reason) {
        try {
            dailyStockService.adjustAvailableQuantity(stockId, amount, reason);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Điều chỉnh kho thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    @GetMapping("/adjustment-logs")
    public String viewAdjustmentLogs(Model model) {
        List<StockAdjustmentLog> logs = stockAdjustmentLogRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("logs", logs);
        return "admin/stock-logs"; // Tên file HTML
    }
}
