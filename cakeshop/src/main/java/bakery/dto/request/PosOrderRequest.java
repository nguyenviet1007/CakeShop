package bakery.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PosOrderRequest {
    private String payment;
    private Double totalAmount;
    private List<PosOrderDetailDTO> details;
}
