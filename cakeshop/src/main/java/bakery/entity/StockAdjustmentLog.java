package bakery.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "StockAdjustmentLog")
public class StockAdjustmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private DailyStock dailyStock;

    // Số lượng thay đổi (Ví dụ: +2 hoặc -1)
    @Column(nullable = false)
    private Integer amount;

    // Lý do điều chỉnh (Ví dụ: "Khách làm rơi", "Hàng hoàn")
    @Column(columnDefinition = "TEXT")
    private String reason;

    @UpdateTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
