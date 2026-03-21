package bakery.service;

import bakery.entity.Feedback;
import bakery.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackServiceImpl implements  FeedbackService {
    @Autowired
    private FeedbackRepository feedbackRepository;

    public List<Feedback> findByProductId(Long productId) {
        return feedbackRepository.findByProduct_ProductIdOrderByCreatedAtDesc(productId);
    }

    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return feedbackRepository.existsByUser_IdAndProduct_ProductId(userId, productId);
    }

    public Feedback save(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }
}
