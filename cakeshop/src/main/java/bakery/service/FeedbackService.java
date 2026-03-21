package bakery.service;

import bakery.entity.Feedback;

import java.util.List;

public interface FeedbackService {
    List<Feedback> findByProductId(Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    Feedback save(Feedback feedback);
}
