package bakery.repository;

import bakery.entity.IngredientStockHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientStockHistoryRepository extends JpaRepository<IngredientStockHistory, Long> {
    List<IngredientStockHistory> findByIngredientIdOrderByUpdatedAtDesc(Long ingredientId);
    void deleteByIngredientId(Long ingredientId);
}
