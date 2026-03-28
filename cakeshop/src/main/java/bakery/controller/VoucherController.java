package bakery.controller;

import bakery.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    @Autowired
    private VoucherRepository voucherRepository;

    @GetMapping("/check")
    public ResponseEntity<?> checkVoucher(@RequestParam String code) {
        return voucherRepository.findByCodeIgnoreCase(code)
                .map(voucher -> {
                    // 1. Kiểm tra trạng thái hoạt động
                    if (!voucher.getActive()) {
                        return ResponseEntity.badRequest().body("Mã giảm giá này hiện không còn áp dụng.");
                    }

                    // 2. Kiểm tra hạn sử dụng
                    if (voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
                        return ResponseEntity.badRequest().body("Mã giảm giá này đã hết hạn.");
                    }

                    // 3. Trả về thông tin voucher nếu hợp lệ
                    return ResponseEntity.ok(voucher);
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Mã giảm giá không tồn tại."));
    }
}