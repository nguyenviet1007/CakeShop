package bakery.repository;

import bakery.entity.StockAdjustmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockAdjustmentLogRepository extends JpaRepository<StockAdjustmentLog, Long> {
    // Tìm lịch sử của một bản ghi kho cụ thể, sắp xếp mới nhất lên đầu
    List<StockAdjustmentLog> findAllByOrderByCreatedAtDesc();}