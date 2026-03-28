package bakery.repository;

import aj.org.objectweb.asm.commons.Remapper;
import bakery.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    boolean existsByCodeIgnoreCase(String code);

    // Lọc theo mọi trường (Tất cả tham số đều có check IS NULL)
    @Query("SELECT v FROM Voucher v WHERE " +
            "(:keyword IS NULL OR LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:active IS NULL OR v.active = :active) AND " +
            "(:minPct IS NULL OR v.discountPercent >= :minPct) AND " +
            "(:maxPct IS NULL OR v.discountPercent <= :maxPct) AND " +
            "(:minMaxAmt IS NULL OR v.maxDiscount >= :minMaxAmt) AND " +
            "(:maxMaxAmt IS NULL OR v.maxDiscount <= :maxMaxAmt) AND " +
            "(:minOrderFrom IS NULL OR v.minOrderValue >= :minOrderFrom) AND " +
            "(:minOrderTo IS NULL OR v.minOrderValue <= :minOrderTo) AND " +
            "(:expireFrom IS NULL OR v.expiryDate >= :expireFrom) AND " +
            "(:expireTo IS NULL OR v.expiryDate <= :expireTo)")
    Page<Voucher> advancedSearch(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            @Param("minPct") Integer minPct,
            @Param("maxPct") Integer maxPct,
            @Param("minMaxAmt") BigDecimal minMaxAmt,
            @Param("maxMaxAmt") BigDecimal maxMaxAmt,
            @Param("minOrderFrom") BigDecimal minOrderFrom,
            @Param("minOrderTo") BigDecimal minOrderTo,
            @Param("expireFrom") LocalDateTime expireFrom,
            @Param("expireTo") LocalDateTime expireTo,
            Pageable pageable);

    Optional<Voucher> findByCodeIgnoreCase(String code);
}