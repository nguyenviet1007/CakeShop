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

    private BigDecimal quantityInput;  // Số lượng thùng/bao nhập vào (VD: 2)
    private String unitName;           // Tên đơn vị nhập (VD: "Thùng")
    private BigDecimal conversionRate;  // Hệ số quy đổi (VD: 8500)

}
