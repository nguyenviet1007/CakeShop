package bakery.repository;

import bakery.entity.IngredientStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientStockRepository
        extends JpaRepository<IngredientStock, Long> {
}
