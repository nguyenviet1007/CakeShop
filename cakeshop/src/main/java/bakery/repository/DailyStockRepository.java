package bakery.repository;

import bakery.entity.DailyStock;
import bakery.entity.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStockRepository extends JpaRepository<DailyStock, Long> {
    //
    Optional<DailyStock> findByProductAndDate(Product product, LocalDate date);

    // Tìm toàn bộ số lượng bánh trên kệ theo một ngày cụ thể
    List<DailyStock> findByDate(LocalDate date);

    // Tìm số lượng của 1 loại bánh cụ thể trong 1 ngày
    Optional<DailyStock> findByProduct_ProductIdAndDate(Long productId, LocalDate date);
    // Đổi từ List<DailyStock> thành Optional<DailyStock>
    Optional<DailyStock> findByProduct_ProductId(Long productId);    @Transactional
    @Modifying
    void deleteByProduct_ProductId(Long productId);

}
