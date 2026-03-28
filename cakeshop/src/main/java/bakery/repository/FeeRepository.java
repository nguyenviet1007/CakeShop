package bakery.repository;

import bakery.entity.ShippingFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeeRepository extends JpaRepository<ShippingFee, Long> {
    ShippingFee findTopByOrderByIdDesc();
}