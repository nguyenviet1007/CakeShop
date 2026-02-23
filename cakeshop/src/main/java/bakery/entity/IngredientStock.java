package bakery.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "IngredientStock")
public class IngredientStock {

    @Id
    private Long ingredientId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "min_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal minQuantity = BigDecimal.TEN;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
