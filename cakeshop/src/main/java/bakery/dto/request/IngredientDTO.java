package bakery.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IngredientDTO {
    private Long ingredientId;
    private String name;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal minQuantity;
    private String note;

}
