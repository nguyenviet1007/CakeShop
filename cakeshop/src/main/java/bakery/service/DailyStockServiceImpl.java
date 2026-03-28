package bakery.service;

import bakery.dto.request.DailyStockUpdateDTO;
import bakery.entity.DailyStock;
import bakery.entity.Product;
import bakery.entity.StockAdjustmentLog;
import bakery.repository.DailyStockRepository;
import bakery.repository.ProductRepository;
import bakery.repository.StockAdjustmentLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DailyStockServiceImpl implements DailyStockService {

    private final DailyStockRepository dailyStockRepo;
    private final ProductRepository productRepo;
    @Autowired
    private StockAdjustmentLogRepository logRepo; // Inject Repository mới vào

    public DailyStockServiceImpl(DailyStockRepository dailyStockRepo, ProductRepository productRepo) {
        this.dailyStockRepo = dailyStockRepo;
        this.productRepo = productRepo;
    }

    @Override
    public List<DailyStock> getStockByDate(LocalDate date) {
        return dailyStockRepo.findByDate(date);
    }
    public List<DailyStock> findAll() {
        return dailyStockRepo.findAll();
    }
    // Trong DailyStockServiceImpl.java
    @Override
    public List<DailyStock> findByDate(LocalDate date) {
        return dailyStockRepo.findByDate(date);
    }

    @Override
    public void saveDailyStock(DailyStockUpdateDTO dto) {
        LocalDate date = dto.getDate();

        for (DailyStockUpdateDTO.StockItem item : dto.getItems()) {
            Optional<DailyStock> existingStock = dailyStockRepo.findByProduct_ProductIdAndDate(item.getProductId(), date);

            // BẢO VỆ BACKEND: Nếu FE gửi lên null thì mặc định là 0
            int newInitial = (item.getQuantity() != null) ? item.getQuantity() : 0;

            if (existingStock.isPresent()) {
                DailyStock stock = existingStock.get();

                int oldInitial = stock.getInitialQuantity() != null ? stock.getInitialQuantity() : 0;

                // Tính độ chênh lệch
                int diff = newInitial - oldInitial;

                stock.setInitialQuantity(newInitial);

                int currentAvailable = stock.getAvailableQuantity() != null ? stock.getAvailableQuantity() : 0;
                stock.setAvailableQuantity(currentAvailable + diff);

                dailyStockRepo.save(stock);
            } else {
                Product product = productRepo.findById(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy bánh ID: " + item.getProductId()));

                DailyStock newStock = new DailyStock();
                newStock.setProduct(product);
                newStock.setDate(date);

                newStock.setInitialQuantity(newInitial);
                newStock.setAvailableQuantity(newInitial);

                dailyStockRepo.save(newStock);
            }
        }
    }
    @Override
    @Transactional
    public void adjustAvailableQuantity(Long stockId, int amount, String reason) {
        DailyStock stock = dailyStockRepo.findById(stockId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu kho!"));

        // 1. Cập nhật số lượng trên kệ
        int currentAvailable = (stock.getAvailableQuantity() != null) ? stock.getAvailableQuantity() : 0;
        int newAvailable = currentAvailable + amount;

        if (newAvailable < 0) {
            throw new RuntimeException("Số lượng trên kệ không thể nhỏ hơn 0 sau khi điều chỉnh!");
        }
        stock.setAvailableQuantity(newAvailable);
        dailyStockRepo.save(stock);

        // 2. LƯU VÀO DATABASE BẢNG LOG
        StockAdjustmentLog log = new StockAdjustmentLog();
        log.setDailyStock(stock); // Gán thực thể stock để Hibernate tự tạo Khóa Ngoại
        log.setAmount(amount);
        log.setReason(reason);

        logRepo.save(log); // Lưu dòng nhật ký mới
    }
}
