package bakery.repository;

import bakery.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IngredientRepository
        extends JpaRepository<Ingredient, Long> {
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
            "FROM Ingredient i WHERE LOWER(i.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);
    @Query("SELECT i FROM Ingredient i WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR lower(i.name) LIKE lower(concat('%', :keyword, '%')) OR CAST(i.ingredientId AS string) LIKE :keyword)")
    List<Ingredient> searchByKeyword(@Param("keyword") String keyword);
    // 1. Tìm theo tên có phân trang
    Page<Ingredient> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // 2. Lấy tất cả có phân trang (JPA có sẵn findAll(Pageable) nhưng khai báo lại cho rõ cũng được)
    Page<Ingredient> findAll(Pageable pageable);
}
