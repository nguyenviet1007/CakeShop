package bakery.dto.request;

import lombok.Data;

@Data
public class PosOrderDetailDTO {
    private Long productId;
    private Integer quantity;
    private Double price;
}
