package bakery.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_in")
public class StockIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    private BigDecimal quantityInput;  // VD: 2
    private String unitName;           // VD: "Thùng"
    private BigDecimal conversionRate; // VD: 8500
    private BigDecimal quantityBase;   // VD: 17000

    private LocalDateTime createdAt = LocalDateTime.now();
    private String note;
}
