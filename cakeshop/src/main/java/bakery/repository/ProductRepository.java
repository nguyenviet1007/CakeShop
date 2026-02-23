package bakery.repository;

import bakery.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category);
    List<Product> findByNameIgnoreCase(String name); // Dùng để check trùng tên lúc thêm mới
    // 1. Tìm theo Tên + Phân trang
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    // 2. Tìm theo Loại + Phân trang (Mới thêm)
    Page<Product> findByCategory(String category, Pageable pageable);
    // 3. Tìm kết hợp Tên + Loại + Phân trang (Mới thêm)
    Page<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category, Pageable pageable);
}


