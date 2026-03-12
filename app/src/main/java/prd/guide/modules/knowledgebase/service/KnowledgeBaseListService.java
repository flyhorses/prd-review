package prd.guide.modules.knowledgebase.service;

import prd.guide.common.exception.BusinessException;
import prd.guide.common.exception.ErrorCode;
import prd.guide.common.security.SecurityUtils;
import prd.guide.infrastructure.file.FileStorageService;
import prd.guide.infrastructure.mapper.KnowledgeBaseMapper;
import prd.guide.modules.knowledgebase.model.KnowledgeBaseEntity;
import prd.guide.modules.knowledgebase.model.KnowledgeBaseListItemDTO;
import prd.guide.modules.knowledgebase.model.KnowledgeBaseStatsDTO;
import prd.guide.modules.knowledgebase.model.RagChatMessageEntity.MessageType;
import prd.guide.modules.knowledgebase.model.VectorStatus;
import prd.guide.modules.knowledgebase.repository.KnowledgeBaseRepository;
import prd.guide.modules.knowledgebase.repository.RagChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseListService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagChatMessageRepository ragChatMessageRepository;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final FileStorageService fileStorageService;

    public List<KnowledgeBaseListItemDTO> listKnowledgeBases(VectorStatus vectorStatus, String sortBy) {
        Long userId = getCurrentUserId();
        List<KnowledgeBaseEntity> entities;
        
        if (userId != null) {
            entities = knowledgeBaseRepository.findByUserIdOrderByUploadedAtDesc(userId);
        } else {
            entities = knowledgeBaseRepository.findAllByOrderByUploadedAtDesc();
        }
        
        if (vectorStatus != null) {
            entities = entities.stream()
                .filter(e -> e.getVectorStatus() == vectorStatus)
                .toList();
        }
        
        if (sortBy != null && !sortBy.isBlank() && !sortBy.equalsIgnoreCase("time")) {
            entities = sortEntities(entities, sortBy);
        }
        
        return knowledgeBaseMapper.toListItemDTOList(entities);
    }

    public List<KnowledgeBaseListItemDTO> listKnowledgeBases() {
        return listKnowledgeBases(null, null);
    }

    public List<KnowledgeBaseListItemDTO> listKnowledgeBasesByStatus(VectorStatus vectorStatus) {
        return listKnowledgeBases(vectorStatus, null);
    }

    public Optional<KnowledgeBaseListItemDTO> getKnowledgeBase(Long id) {
        return knowledgeBaseRepository.findById(id)
            .map(knowledgeBaseMapper::toListItemDTO);
    }

    public Optional<KnowledgeBaseEntity> getKnowledgeBaseEntity(Long id) {
        return knowledgeBaseRepository.findById(id);
    }

    public List<String> getKnowledgeBaseNames(List<Long> ids) {
        return ids.stream()
            .map(id -> knowledgeBaseRepository.findById(id)
                .map(KnowledgeBaseEntity::getName)
                .orElse("未知知识库"))
            .toList();
    }

    public List<String> getAllCategories() {
        return knowledgeBaseRepository.findAllCategories();
    }

    public List<KnowledgeBaseListItemDTO> listByCategory(String category) {
        Long userId = getCurrentUserId();
        List<KnowledgeBaseEntity> entities;
        
        if (userId != null) {
            entities = knowledgeBaseRepository.findByUserIdOrderByUploadedAtDesc(userId);
            if (category == null || category.isBlank()) {
                entities = entities.stream()
                    .filter(e -> e.getCategory() == null)
                    .toList();
            } else {
                entities = entities.stream()
                    .filter(e -> category.equals(e.getCategory()))
                    .toList();
            }
        } else {
            if (category == null || category.isBlank()) {
                entities = knowledgeBaseRepository.findByCategoryIsNullOrderByUploadedAtDesc();
            } else {
                entities = knowledgeBaseRepository.findByCategoryOrderByUploadedAtDesc(category);
            }
        }
        
        return knowledgeBaseMapper.toListItemDTOList(entities);
    }

    @Transactional
    public void updateCategory(Long id, String category) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("知识库不存在"));
        entity.setCategory(category != null && !category.isBlank() ? category : null);
        knowledgeBaseRepository.save(entity);
        log.info("更新知识库分类: id={}, category={}", id, category);
    }

    public List<KnowledgeBaseListItemDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return listKnowledgeBases();
        }
        
        Long userId = getCurrentUserId();
        List<KnowledgeBaseEntity> entities = knowledgeBaseRepository.searchByKeyword(keyword.trim());
        
        if (userId != null) {
            entities = entities.stream()
                .filter(e -> userId.equals(e.getUserId()))
                .toList();
        }
        
        return knowledgeBaseMapper.toListItemDTOList(entities);
    }

    public List<KnowledgeBaseListItemDTO> listSorted(String sortBy) {
        return listKnowledgeBases(null, sortBy);
    }

    private List<KnowledgeBaseEntity> sortEntities(List<KnowledgeBaseEntity> entities, String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "size" -> entities.stream()
                .sorted((a, b) -> Long.compare(b.getFileSize(), a.getFileSize()))
                .toList();
            case "access" -> entities.stream()
                .sorted((a, b) -> Integer.compare(b.getAccessCount(), a.getAccessCount()))
                .toList();
            case "question" -> entities.stream()
                .sorted((a, b) -> Integer.compare(b.getQuestionCount(), a.getQuestionCount()))
                .toList();
            default -> entities;
        };
    }

    public KnowledgeBaseStatsDTO getStatistics() {
        Long userId = getCurrentUserId();
        long totalCount;
        
        if (userId != null) {
            totalCount = knowledgeBaseRepository.countByUserId(userId);
        } else {
            totalCount = knowledgeBaseRepository.count();
        }
        
        return new KnowledgeBaseStatsDTO(
            totalCount,
            ragChatMessageRepository.countByType(MessageType.USER),
            knowledgeBaseRepository.sumAccessCount(),
            knowledgeBaseRepository.countByVectorStatus(VectorStatus.COMPLETED),
            knowledgeBaseRepository.countByVectorStatus(VectorStatus.PROCESSING)
        );
    }

    public byte[] downloadFile(Long id) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "知识库不存在"));

        String storageKey = entity.getStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "文件存储信息不存在");
        }

        log.info("下载知识库文件: id={}, filename={}", id, entity.getOriginalFilename());
        return fileStorageService.downloadFile(storageKey);
    }

    public KnowledgeBaseEntity getEntityForDownload(Long id) {
        return knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "知识库不存在"));
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            log.debug("无法获取当前用户ID，返回所有数据");
        }
        return userId;
    }
}
