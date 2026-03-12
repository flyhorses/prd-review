package prd.guide.modules.knowledgebase.repository;

import prd.guide.modules.knowledgebase.model.KnowledgeBaseEntity;
import prd.guide.modules.knowledgebase.model.VectorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntity, Long> {

    Optional<KnowledgeBaseEntity> findByFileHash(String fileHash);

    boolean existsByFileHash(String fileHash);

    List<KnowledgeBaseEntity> findAllByOrderByUploadedAtDesc();
    
    List<KnowledgeBaseEntity> findByUserIdOrderByUploadedAtDesc(Long userId);

    @Query("SELECT DISTINCT k.category FROM KnowledgeBaseEntity k WHERE k.category IS NOT NULL ORDER BY k.category")
    List<String> findAllCategories();

    List<KnowledgeBaseEntity> findByCategoryOrderByUploadedAtDesc(String category);

    List<KnowledgeBaseEntity> findByCategoryIsNullOrderByUploadedAtDesc();

    @Query("SELECT k FROM KnowledgeBaseEntity k WHERE LOWER(k.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(k.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY k.uploadedAt DESC")
    List<KnowledgeBaseEntity> searchByKeyword(@Param("keyword") String keyword);

    List<KnowledgeBaseEntity> findAllByOrderByFileSizeDesc();

    List<KnowledgeBaseEntity> findAllByOrderByAccessCountDesc();

    List<KnowledgeBaseEntity> findAllByOrderByQuestionCountDesc();

    @Modifying
    @Query("UPDATE KnowledgeBaseEntity k SET k.questionCount = k.questionCount + 1 WHERE k.id IN :ids")
    int incrementQuestionCountBatch(@Param("ids") List<Long> ids);

    @Query("SELECT COALESCE(SUM(k.questionCount), 0) FROM KnowledgeBaseEntity k")
    long sumQuestionCount();

    @Query("SELECT COALESCE(SUM(k.accessCount), 0) FROM KnowledgeBaseEntity k")
    long sumAccessCount();
    
    long countByUserId(Long userId);

    long countByVectorStatus(VectorStatus vectorStatus);
}
