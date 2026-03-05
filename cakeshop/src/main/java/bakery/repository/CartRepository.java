package bakery.repository;

import bakery.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUserId(Long userId);
    Optional<Cart> findByUserIdAndProductProductId(Long userId, Long productId);
    void deleteById(Long id);
    void deleteByUserId(Long userId);
}
