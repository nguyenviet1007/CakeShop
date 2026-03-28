package bakery.controller;

import bakery.dto.request.PosOrderRequest;
import bakery.entity.DailyStock;
import bakery.entity.Order;
import bakery.service.AdOrderService;
import bakery.service.DailyStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manager/pos")
public class PosController {

    @Autowired
    private DailyStockService dailyStockService;

    @Autowired
    private AdOrderService adOrderService;

    @GetMapping
    public String viewPosScreen(Model model) {
        LocalDate today = LocalDate.now();
        List<DailyStock> stocks = dailyStockService.findByDate(today);
        model.addAttribute("stocks", stocks);
        return "admin/pos-checkout";
    }

    // CHỈ GIỮ LẠI MỘT HÀM POST DUY NHẤT CHO ĐƯỜNG DẪN /create
    // Trong PosController.java
    @PostMapping("/create")
    @ResponseBody // Hàm này trả về JSON nên cần @ResponseBody
    public ResponseEntity<?> handleCheckout(@RequestBody PosOrderRequest request) {
        try {
            // Sửa hàm này trong Service để trả về object Order
            Order savedOrder = adOrderService.createOrderFromPos(request);

            // Trả về Map để Spring chuyển thành JSON
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", savedOrder.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    @GetMapping("/generate-qr")
    @ResponseBody
    public ResponseEntity<?> generateQr(@RequestParam("amount") BigDecimal amount) {
        // Thông tin cấu hình (Nên khớp với CartController để đồng bộ tài khoản nhận tiền)
        String bankId = "MB";
        String accountNo = "1023100704";
        String template = "qr_only"; // Chỉ hiện mã QR, không hiện khung VietQR rườm rà
        String info = "POS_PAY_" + System.currentTimeMillis();

        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-%s.png?amount=%s&addInfo=%s",
                bankId,
                accountNo,
                template,
                amount.toPlainString(),
                info
        );

        return ResponseEntity.ok(Map.of("qrUrl", qrUrl));
    }
}

