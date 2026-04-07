package bakery.repository;

import bakery.entity.Order;
import bakery.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Page<Order> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
    Page<Order> findByUserIdAndStatusIn(Long userId, List<String> statuses, Pageable pageable);
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

    List<Order> findByStatus(String status);
    boolean existsByShipper(User shipper);
    List<Order> findByStatusAndShipper(String status, User shipper);



    @Query("""
    select distinct o from Order o
    left join fetch o.user
    left join fetch o.shipper
    left join fetch o.orderDetails od
    left join fetch od.product p
    
    where o.id = :id
""")
    Optional<Order> findDetailById(@Param("id") Long id);
    List<Order> findByShipperAndStatusIn(User shipper, List<String> statuses);
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID' OR o.status = 'COMPLETED'")
    Double calculateTotalRevenue();

    /**
     * Thống kê số lượng đơn hàng theo từng trạng thái.
     */
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    /**
     * Truy vấn tổng doanh thu theo từng ngày kể từ một mốc thời gian cụ thể.
     */
    @Query("SELECT o.orderDate, SUM(o.totalAmount) FROM Order o " +
            "WHERE o.orderDate >= :date " +
            "GROUP BY o.orderDate " +
            "ORDER BY o.orderDate ASC")
    List<Object[]> getRevenueFromDate(@Param("date") LocalDate date);

    /**
     * Tìm kiếm và lọc danh sách đơn hàng có hỗ trợ phân trang.
     * Tìm kiếm liên kết (JOIN) bảng User để lấy tên khách/SĐT và bảng Product để lấy tên bánh.
     * Dùng DISTINCT để tránh việc 1 đơn hàng bị lặp lại nhiều lần nếu mua nhiều bánh.
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN o.user u " +
            "LEFT JOIN o.orderDetails od " +
            "LEFT JOIN od.product p " +
            "WHERE (:status IS NULL OR o.status = :status) AND " +
            "(:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR u.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Order> searchOrders(@Param("keyword") String keyword,
                             @Param("status") String status,
                             Pageable pageable);
    @Query("SELECT CAST(o.orderDate AS date), SUM(o.totalAmount) " +
            "FROM Order o " +
            "WHERE o.orderDate >= :startDate " + // Đã xóa điều kiện status
            "GROUP BY CAST(o.orderDate AS date) " +
            "ORDER BY CAST(o.orderDate AS date) ASC")
    List<Object[]> getRevenueFromDate(@Param("startDate") LocalDateTime startDate);
    @Query("SELECT o FROM Order o WHERE o.status = 'ASSIGNED' AND " +
            "(LOWER(o.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Order> searchAssigned(String keyword, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = 'SHIPPING' AND o.shipper = :shipper AND " +
            "(LOWER(o.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Order> searchShipping(User shipper, String keyword, Pageable pageable);
    Page<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
