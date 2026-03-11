package prd.guide.modules.prdreview.repository;

import prd.guide.modules.prdreview.model.PrdReviewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * PRD 评审结果 Repository
 */
@Repository
public interface PrdReviewRepository extends JpaRepository<PrdReviewEntity, Long> {
    
    /**
     * 按创建时间倒序获取所有评审记录
     */
    List<PrdReviewEntity> findAllByOrderByCreatedAtDesc();
}

