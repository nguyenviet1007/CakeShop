package bakery.service;

import bakery.dto.request.DailyStockUpdateDTO;
import bakery.entity.DailyStock;
import java.time.LocalDate;
import java.util.List;

public interface DailyStockService {
    List<DailyStock> getStockByDate(LocalDate date);
    void saveDailyStock(DailyStockUpdateDTO dto);
}
