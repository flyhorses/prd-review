package prd.guide.modules.prdreview.repository;

import prd.guide.modules.prdreview.model.PrdReviewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrdReviewRepository extends JpaRepository<PrdReviewEntity, Long> {
    
    List<PrdReviewEntity> findAllByOrderByCreatedAtDesc();
    
    List<PrdReviewEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    long countByUserId(Long userId);
}
