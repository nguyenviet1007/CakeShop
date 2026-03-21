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

    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category);
    List<Product> findTop5ByNameContainingIgnoreCase(String name);
    List<Product> findByNameIgnoreCase(String name); // Dùng để check trùng tên lúc thêm mới
    // 1. Tìm theo Tên + Phân trang
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    // 2. Tìm theo Loại + Phân trang (Mới thêm)
    Page<Product> findByCategory(String category, Pageable pageable);
    // 3. Tìm kết hợp Tên + Loại + Phân trang (Mới thêm)
    Page<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category, Pageable pageable);
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.productId = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images")
    List<Product> findAllWithImages();

//    List<Product> findByCategory_Id(Long id);
//    @Query("""
//    SELECT DISTINCT p FROM Product p
//    LEFT JOIN FETCH p.images
//    WHERE p.category.id = :id
//    """)

    // Lấy danh sách các tên Category không trùng lặp để làm Menu
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();

    // Lọc sản phẩm theo Category (String)
    List<Product> findByCategoryIgnoreCase(String category);

//    List<Product> findByCategoryWithImages(@Param("id") Long id);
    @Query("SELECT p FROM Product p JOIN FETCH p.images WHERE p.category = :category")
    List<Product> findByCategoryWithImages(@Param("category") String category);

    Product findByProductId(Long productId);
}
