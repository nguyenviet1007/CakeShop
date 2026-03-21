package bakery.repository;

import bakery.entity.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findByUserId(Long userId);
    List<Order> findByUserIdAndStatus(Long userId, String status);
    @Query("SELECT COUNT(od) > 0 FROM OrderDetail od " +
            "WHERE od.order.user.id = :userId " +
            "AND od.product.productId = :productId " +
            "AND od.order.status = 'COMPLETED'")
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);
    List<Order> findAllByOrderByOrderDateDesc();
    @Query("SELECT SUM(o.totalAmount) FROM Order o")
    BigDecimal sumAllTotalAmount();

    // Hàm tính tổng tiền có kèm bộ lọc Specification
    default BigDecimal sumTotalWithSpecification(Specification<Order> spec) {
        return findAll(spec).stream()
                .map(o -> o.getTotalAmount() != null
                        ? o.getTotalAmount()
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
