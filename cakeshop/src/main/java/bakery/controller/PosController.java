package bakery.controller;

import bakery.entity.DailyStock;
import bakery.service.DailyStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/pos")
public class PosController {

    @Autowired
    private DailyStockService dailyStockService;
    // Nếu bạn đang dùng trực tiếp Repository thì đổi thành: private DailyStockRepository dailyStockRepository;

    @GetMapping
    public String viewPosScreen(Model model) {
        // Lấy ngày hôm nay
        LocalDate today = LocalDate.now();

        // CHỈ LẤY DANH SÁCH BÁNH CỦA NGÀY HÔM NAY
        List<DailyStock> stocks = dailyStockService.findByDate(today);

        model.addAttribute("stocks", stocks);
        return "admin/pos-checkout";
    }
}

