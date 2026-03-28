package bakery.controller;

import bakery.dto.request.DashboardStatsDto;
import bakery.dto.request.UserRequestDto;
import bakery.entity.User;
import bakery.repository.OrderRepository;
import bakery.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final OrderRepository orderRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "admin/admin-login";
    }

    @GetMapping("/no-access")
    public String noAccessPage() {
        return "admin/no-access";
    }

    @GetMapping("/dashboard")
    public String getDashboard(Model model) {
        // 1. Truy vấn các số liệu thống kê cơ bản (Tổng user, sản phẩm, đơn hàng, doanh thu)
        DashboardStatsDto stats = adminService.getDashboardStats("all");
        List<User> latestUsers = adminService.getLatestUsers();

        // 2. Xử lý dữ liệu cho biểu đồ tròn (Trạng thái đơn hàng)
        List<Object[]> statusCounts = orderRepository.countOrdersByStatus();
        Long[] statusData = {0L, 0L, 0L, 0L}; // Thứ tự: Hoàn thành, Đang giao, Chờ thanh toán, Đã hủy

        for (Object[] row : statusCounts) {
            if (row[0] == null) continue;
            String stt = row[0].toString();
            Long count = ((Number) row[1]).longValue();

            switch (stt) {
                case "COMPLETED": statusData[0] = count; break;
                case "SHIPPING":  statusData[1] = count; break;
                case "UNPAID":    statusData[2] = count; break;
                case "CANCELLED": statusData[3] = count; break;
            }
        }

        // 3. Xử lý dữ liệu cho biểu đồ đường (Doanh thu 30 ngày gần nhất)
        LocalDate startDateRaw = LocalDate.now().minusDays(29);
        // Chuyển sang LocalDateTime để truyền vào Repository (Sửa lỗi Incompatible Type)
        java.time.LocalDateTime startDate = startDateRaw.atStartOfDay();

        List<Object[]> revenueRecords = orderRepository.getRevenueFromDate(startDate);

        // Chuyển đổi List<Object[]> sang Map<LocalDate, Double> để dễ tra cứu
        Map<LocalDate, Double> revenueMap = revenueRecords.stream()
                .collect(Collectors.toMap(
                        row -> {
                            Object dateObj = row[0];
                            // Xử lý mọi trường hợp kiểu dữ liệu ngày tháng từ DB trả về
                            if (dateObj instanceof java.sql.Date) return ((java.sql.Date) dateObj).toLocalDate();
                            if (dateObj instanceof java.time.LocalDateTime) return ((java.time.LocalDateTime) dateObj).toLocalDate();
                            if (dateObj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) dateObj).toLocalDateTime().toLocalDate();
                            return (LocalDate) dateObj;
                        },
                        row -> row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                        (existing, replacement) -> existing // Nếu trùng ngày thì giữ giá trị đầu
                ));

        Double[] revenueData = new Double[30];
        String[] revenueLabels = new String[30];

        // Lấp đầy dữ liệu cho 30 ngày (ngày nào không có doanh thu thì để 0.0)
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDateRaw.plusDays(i);
            revenueLabels[i] = String.format("%02d/%02d", date.getDayOfMonth(), date.getMonthValue());
            revenueData[i] = revenueMap.getOrDefault(date, 0.0);
        }

        // 4. Đưa dữ liệu vào model để hiển thị lên View
        model.addAttribute("stats", stats);
        model.addAttribute("users", latestUsers);
        model.addAttribute("roles", adminService.getAllRoles());
        model.addAttribute("statusData", Arrays.asList(statusData));
        model.addAttribute("revenueLabels", Arrays.asList(revenueLabels)); // Đã có 30 ngày
        model.addAttribute("revenueData", Arrays.asList(revenueData));     // Đã có doanh thu COMPLETED

        return "admin/admin-manager";
    }

    @GetMapping("/users")
    public String getUsersPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "roleId", required = false) Long roleId,
            @RequestParam(value = "active", required = false) Integer active,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        Page<User> userPage = adminService.getUsersPaginated(keyword, roleId, active, page, size);

        model.addAttribute("userPage", userPage);
        model.addAttribute("roles", adminService.getAllRoles());
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("active", active);
        model.addAttribute("currentPage", page);

        return "admin/admin-users";
    }

    // --- QUẢN LÝ USER (GIỮ LẠI ĐỂ DASHBOARD HOẠT ĐỘNG) ---

    @PostMapping("/users/add")
    public String addAccount(@ModelAttribute("userRequest") UserRequestDto requestDto,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        try {
            adminService.addAccount(requestDto);
            redirectAttributes.addFlashAttribute("successMsg", "Thêm tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    @PostMapping("/users/edit/{id}")
    public String editAccount(@PathVariable("id") Long id, // SỬA: Integer -> Long
                              @ModelAttribute("userRequest") UserRequestDto requestDto,
                              RedirectAttributes redirectAttributes) {
        try {
            adminService.editAccount(id, requestDto);
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id) { // SỬA: Integer -> Long
        try {
            adminService.toggleUserStatus(id);
        } catch (Exception e) {
            System.err.println("Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) { // SỬA: Integer -> Long
        try {
            adminService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("successMsg", "Đã xóa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}