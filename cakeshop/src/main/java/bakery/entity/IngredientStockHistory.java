package bakery.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "IngredientStockHistory")
public class IngredientStockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(precision = 10, scale = 2)
    private BigDecimal oldQuantity;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal newQuantity;

    @Column(name = "old_min_quantity", precision = 10, scale = 2)
    private BigDecimal oldMinQuantity;

    @Column(name = "new_min_quantity", precision = 10, scale = 2)
    private BigDecimal newMinQuantity;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    private String note;

    private String updatedBy;  // có thể null nếu chưa có auth
}
