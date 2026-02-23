package bakery.entity;

import bakery.validation.UniqueProductName;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity

@Table(name = "Product",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_name", columnNames = "name"))

public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @NotBlank(message = "Tên bánh không được để trống")
    @Size(min = 3, max = 150, message = "Tên bánh phải từ 3 đến 150 ký tự")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Loại bánh không được để trống")
    @Size(max = 100, message = "Loại bánh không được vượt quá 100 ký tự")
    private String category;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "999.0", inclusive = false, message = "Giá bán phải lớn hơn 1000")
    @Digits(integer = 10, fraction = 2, message = "Giá bán không hợp lệ (tối đa 10 chữ số nguyên và 2 chữ số thập phân)")
    private BigDecimal price;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // === [MỚI] 2 TRƯỜNG DÙNG CHO NGHIỆP VỤ BÁN HÀNG ===

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true; // Mặc định là luôn hiện (true)

    @Column(name = "discount_percent", nullable = false)
    @Min(value = 0, message = "Phần trăm giảm giá không được âm")
    @Max(value = 100, message = "Phần trăm giảm giá không được vượt quá 100%")
    private Integer discountPercent = 0; // Mặc định không giảm giá (0%)

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ProductImage> images = new ArrayList<>();

    // Method tiện lợi (giữ nguyên nếu có)
    public ProductImage getMainImage() {
        return images.stream()
                .filter(ProductImage::getIsMain)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));
    }
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recipe> recipes = new ArrayList<>();

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
    }
}
