package bakery.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "ProductImage")
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;
    private String imageUrl;
    private Boolean isMain = false;

    @ManyToOne
    @JoinColumn(name = "product_id")
    @ToString.Exclude // Thêm dòng này
    private Product product;
}
