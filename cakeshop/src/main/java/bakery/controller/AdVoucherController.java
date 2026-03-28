package bakery.controller;

import bakery.entity.Voucher;
import bakery.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/vouchers")
public class AdVoucherController {

    @Autowired
    private VoucherService voucherService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer minPct,
            @RequestParam(required = false) Integer maxPct,
            @RequestParam(required = false) BigDecimal minMaxAmt,
            @RequestParam(required = false) BigDecimal maxMaxAmt,
            @RequestParam(required = false) BigDecimal minOrderFrom,
            @RequestParam(required = false) BigDecimal minOrderTo,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate expireFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate expireTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        // Ép kiểu ngày để tìm kiếm bao phủ toàn bộ ngày
        java.time.LocalDateTime expireFromLDT = (expireFrom != null) ? expireFrom.atStartOfDay() : null;
        java.time.LocalDateTime expireToLDT = (expireTo != null) ? expireTo.atTime(23, 59, 59) : null;

        Page<Voucher> voucherPage = voucherService.getVouchersPaginated(
                keyword, active, minPct, maxPct, minMaxAmt, maxMaxAmt,
                minOrderFrom, minOrderTo, expireFromLDT, expireToLDT, page, size);

        model.addAttribute("voucherPage", voucherPage);

        // Giữ lại các giá trị filter để đổ ngược ra Form (Không bị mất khi chuyển trang)
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);
        model.addAttribute("minPct", minPct);
        model.addAttribute("maxPct", maxPct);
        model.addAttribute("minMaxAmt", minMaxAmt);
        model.addAttribute("maxMaxAmt", maxMaxAmt);
        model.addAttribute("minOrderFrom", minOrderFrom);
        model.addAttribute("minOrderTo", minOrderTo);
        model.addAttribute("expireFrom", expireFrom);
        model.addAttribute("expireTo", expireTo);

        model.addAttribute("currentPage", page);

        return "admin/admin-vouchers";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Voucher voucher, RedirectAttributes ra) {
        try {
            voucherService.saveVoucher(voucher);
            ra.addFlashAttribute("successMsg", "Lưu voucher thành công!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes ra) {
        voucherService.toggleStatus(id);
        ra.addFlashAttribute("successMsg", "Đã thay đổi trạng thái Voucher!");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        voucherService.deleteVoucher(id);
        ra.addFlashAttribute("successMsg", "Xóa Voucher thành công!");
        return "redirect:/admin/vouchers";
    }
}