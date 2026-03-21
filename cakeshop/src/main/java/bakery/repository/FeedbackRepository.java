package bakery.repository;

import bakery.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByProduct_ProductIdOrderByCreatedAtDesc(Long productId);

    // Kiểm tra xem User đã từng đánh giá Product này chưa
    boolean existsByUser_IdAndProduct_ProductId(Long userId, Long productId);
}