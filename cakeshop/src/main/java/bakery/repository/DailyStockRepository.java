package bakery.repository;

import bakery.entity.DailyStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStockRepository extends JpaRepository<DailyStock, Long> {

    // Tìm toàn bộ số lượng bánh trên kệ theo một ngày cụ thể
    List<DailyStock> findByDate(LocalDate date);

    // Tìm số lượng của 1 loại bánh cụ thể trong 1 ngày
    Optional<DailyStock> findByProduct_ProductIdAndDate(Long productId, LocalDate date);
}
