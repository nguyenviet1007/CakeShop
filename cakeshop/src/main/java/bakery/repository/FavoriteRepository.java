package bakery.repository;

import bakery.entity.Favorite;
import bakery.entity.Product;
import bakery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    Optional<Favorite> findByUserAndProduct(User user, Product product);
    boolean existsByUserAndProduct(User user, Product product);
    List<Favorite> findAllByUser(User user);
    @Transactional
    void deleteByUserAndProduct(User user, Product product);
}
