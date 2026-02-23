package bakery.dto.request;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DailyStockUpdateDTO {
    private LocalDate date;
    private List<StockItem> items;

    @Data
    public static class StockItem {
        private Long productId;
        private Integer quantity;
    }
}
