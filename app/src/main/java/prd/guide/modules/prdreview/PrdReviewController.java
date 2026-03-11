package prd.guide.modules.prdreview;

import prd.guide.common.annotation.RateLimit;
import prd.guide.common.model.AsyncTaskStatus;
import prd.guide.common.result.Result;
import prd.guide.infrastructure.mapper.PrdReviewMapper;
import prd.guide.infrastructure.mapper.PrdReviewMapper.PrdReviewListItemDTO;
import prd.guide.modules.prdreview.model.PrdReviewRequest;
import prd.guide.modules.prdreview.model.PrdReviewResponse;
import prd.guide.modules.prdreview.service.PrdReviewService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * PRD 评审控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PrdReviewController {

    private final PrdReviewService reviewService;
    private final PrdReviewMapper prdReviewMapper;

    /**
     * 提交 PRD 文本进行评审（同步模式）
     */
    @PostMapping("/api/prd/review")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Result<PrdReviewResponse> review(@Valid @RequestBody PrdReviewRequest request) {
        log.info("收到 PRD 评审请求（同步）, title={}", request.title());
        PrdReviewResponse response = reviewService.review(request);
        return Result.success(response);
    }

    /**
     * 提交 PRD 评审任务（异步模式）
     * 立即返回 PRD 记录 ID，评审在后台异步执行
     */
    @PostMapping("/api/prd/review/async")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 10)
    public Result<Map<String, Object>> submitReviewAsync(@Valid @RequestBody PrdReviewRequest request) {
        log.info("收到 PRD 评审请求（异步）, title={}", request.title());
        Long prdId = reviewService.submitReviewAsync(request);
        return Result.success(Map.of("prdId", prdId));
    }

    /**
     * 获取 PRD 评审状态
     */
    @GetMapping("/api/prd/review/{id}/status")
    public Result<Map<String, Object>> getReviewStatus(@PathVariable Long id) {
        AsyncTaskStatus status = reviewService.getReviewStatus(id);
        return Result.success(Map.of(
            "prdId", id,
            "status", status.name()
        ));
    }

    /**
     * 获取 PRD 评审记录列表
     */
    @GetMapping("/api/prd/reviews")
    public Result<List<PrdReviewListItemDTO>> getReviewList() {
        List<PrdReviewListItemDTO> list = reviewService.getReviewList();
        return Result.success(list);
    }

    /**
     * 根据 ID 获取评审详情（包含状态信息）
     */
    @GetMapping("/api/prd/review/{id}/detail")
    public Result<Map<String, Object>> getReviewDetail(@PathVariable Long id) {
        return Result.success(reviewService.getReviewDetail(id));
    }

    /**
     * 删除 PRD 评审记录
     */
    @DeleteMapping("/api/prd/review/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        log.info("收到删除 PRD 评审记录请求, prdId={}", id);
        reviewService.deleteReview(id);
        return Result.success(null);
    }

    /**
     * 根据ID查询历史 PRD 评审结果
     */
    @GetMapping("/api/prd/review/{id}")
    public Result<PrdReviewResponse> getReview(@PathVariable Long id) {
        PrdReviewResponse response = reviewService.getReview(id);
        return Result.success(response);
    }

    /**
     * 重新评审（手动重试）
     */
    @PostMapping("/api/prd/review/{id}/retry")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 2)
    public Result<Void> retryReview(@PathVariable Long id) {
        log.info("收到 PRD 重新评审请求, prdId={}", id);
        reviewService.reReview(id);
        return Result.success(null);
    }

    /**
     * 导出 PRD 评审报告为 PDF
     */
    @GetMapping("/api/prd/review/{id}/export")
    public ResponseEntity<byte[]> exportReviewPdf(@PathVariable Long id) {
        try {
            byte[] pdfBytes = reviewService.exportReviewPdf(id);
            String filename = URLEncoder.encode("PRD评审报告_" + id + ".pdf", StandardCharsets.UTF_8);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
