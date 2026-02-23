package bakery.service;

import bakery.dto.request.DailyStockUpdateDTO;
import bakery.entity.DailyStock;
import bakery.entity.Product;
import bakery.repository.DailyStockRepository;
import bakery.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DailyStockServiceImpl implements DailyStockService {

    private final DailyStockRepository dailyStockRepo;
    private final ProductRepository productRepo;

    public DailyStockServiceImpl(DailyStockRepository dailyStockRepo, ProductRepository productRepo) {
        this.dailyStockRepo = dailyStockRepo;
        this.productRepo = productRepo;
    }

    @Override
    public List<DailyStock> getStockByDate(LocalDate date) {
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
}
