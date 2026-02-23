package bakery.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "DailyStock", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "stock_date"})
})
public class DailyStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với bảng Product (Bánh)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Ngày bán
    @Column(name = "stock_date", nullable = false)
    private LocalDate date;

    // Số lượng bánh ban đầu thợ làm ra (Tổng nhập)
    @Column(name = "initial_quantity")
    private Integer initialQuantity;

    // Số lượng bánh còn lại trên kệ (Số tồn thực tế để bán)
    @Column(name = "available_quantity")
    private Integer availableQuantity;

    // Cột thời gian ngầm (Lưu giờ phút giây cập nhật cuối cùng)
    // Annotation @UpdateTimestamp của Hibernate sẽ tự động làm hết, bạn KHÔNG CẦN code thêm gì!
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
