package bakery.entity;

import bakery.validation.UniqueProductName;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

//    @NotNull(message = "Loại bánh không được để trống")
//    @ManyToOne
//    @JoinColumn(name = "category_id")
//    @JsonIgnore
//    private Category category;
    @NotBlank(message = "Loại bánh không được để trống")
    @Column(name = "category", nullable = false)
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
    @JsonIgnore
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Recipe> recipes = new ArrayList<>();

    // Ánh xạ danh sách Feedback (nếu bạn chưa có)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Feedback> feedbacks = new ArrayList<>();

    // Hàm lấy tổng số lượt đánh giá
    @Transient // Đánh dấu đây không phải là cột trong database
    public int getReviewCount() {
        return feedbacks != null ? feedbacks.size() : 0;
    }

    // Hàm tính điểm đánh giá trung bình
    @Transient
    public double getAverageRating() {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (Feedback fb : feedbacks) {
            sum += fb.getRating();
        }
        return sum / feedbacks.size();
    }

    // Method tiện lợi (giữ nguyên nếu có)
    public ProductImage getMainImage() {
        return images.stream()
                .filter(ProductImage::getIsMain)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));
    }
    public List<Recipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
