package bakery.repository;

import bakery.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // ===== BASIC SEARCH =====
    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategory(String category);

    List<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category);

    List<Product> findTop5ByNameContainingIgnoreCase(String name);

    List<Product> findByNameIgnoreCase(String name);

    // ===== PAGINATION =====
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category, Pageable pageable);

    // ===== FETCH WITH IMAGES (FIX LỖI Ở ĐÂY) =====
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.productId = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images")
    List<Product> findAllWithImages();

    // ===== CATEGORY =====
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();

    List<Product> findByCategoryIgnoreCase(String category);

    // ===== FETCH BY CATEGORY WITH IMAGES =====
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.category = :category")
    List<Product> findByCategoryWithImages(@Param("category") String category);

    // ===== FIND BY ID =====
    Product findByProductId(Long productId);
    // Tìm tất cả sản phẩm đang hiển thị có phân trang
    Page<Product> findAllByIsVisibleTrue(Pageable pageable);

    // Lấy sản phẩm giảm giá (thường banner lấy tất cả hoặc top, không cần phân trang quá phức tạp)
    List<Product> findByDiscountPercentGreaterThan(Integer threshold);

}