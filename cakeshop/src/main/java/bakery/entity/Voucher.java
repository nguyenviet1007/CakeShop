package bakery.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Voucher")
@Data
@Getter
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code; // Mã voucher (VD: SALE50)

    @Column(columnDefinition = "NVARCHAR(255)")
    private String description; //Mô tả chi tiết

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent; // Phần trăm giảm (VD: 50)

    @Column(name = "max_discount")
    private BigDecimal maxDiscount; // Giảm tối đa (VD: 100000)

    @Column(name = "min_order_value", nullable = false)
    private BigDecimal minOrderValue; // Đơn tối thiểu để áp dụng (VD: 0 hoặc 150000)

    @Column(name = "expiry_date", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime expiryDate; // Hạn sử dụng

    @Column(name = "is_active", nullable = false)
    private Boolean active = true; // Trạng thái Bật/Tắt
}