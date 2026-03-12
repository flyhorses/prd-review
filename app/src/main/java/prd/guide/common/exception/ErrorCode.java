package prd.guide.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // ========== 通用错误 1xxx ==========
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    VALIDATION_ERROR(400, "数据验证失败"),
    
    // ========== PRD 评审模块错误 2xxx ==========
    PRD_REVIEW_NOT_FOUND(2001, "PRD 评审记录不存在"),
    PRD_REVIEW_PARSE_FAILED(2002, "PRD 内容解析失败"),
    PRD_REVIEW_UPLOAD_FAILED(2003, "PRD 上传失败"),
    PRD_REVIEW_DUPLICATE(2004, "PRD 已存在"),
    PRD_REVIEW_FILE_EMPTY(2005, "PRD 文件为空"),
    PRD_REVIEW_FILE_TYPE_NOT_SUPPORTED(2006, "不支持的文件类型"),
    PRD_REVIEW_FAILED(2007, "PRD 评审失败"),
    PRD_REVIEW_RESULT_NOT_FOUND(2008, "PRD 评审结果不存在"),
    
    // ========== 存储模块错误 4xxx ==========
    STORAGE_UPLOAD_FAILED(4001, "文件上传失败"),
    STORAGE_DOWNLOAD_FAILED(4002, "文件下载失败"),
    STORAGE_DELETE_FAILED(4003, "文件删除失败"),
    
    // ========== 导出模块错误 5xxx ==========
    EXPORT_PDF_FAILED(5001, "PDF导出失败"),
    
    // ========== 知识库模块错误 6xxx ==========
    KNOWLEDGE_BASE_NOT_FOUND(6001, "知识库不存在"),
    KNOWLEDGE_BASE_PARSE_FAILED(6002, "知识库文件解析失败"),
    KNOWLEDGE_BASE_UPLOAD_FAILED(6003, "知识库上传失败"),
    KNOWLEDGE_BASE_QUERY_FAILED(6004, "知识库查询失败"),
    KNOWLEDGE_BASE_DELETE_FAILED(6005, "知识库删除失败"),
    KNOWLEDGE_BASE_VECTORIZATION_FAILED(6006, "知识库向量化失败"),
    
    // ========== AI服务错误 7xxx ==========
    AI_SERVICE_UNAVAILABLE(7001, "AI服务暂时不可用，请稍后重试"),
    AI_SERVICE_TIMEOUT(7002, "AI服务响应超时"),
    AI_SERVICE_ERROR(7003, "AI服务调用失败"),
    AI_API_KEY_INVALID(7004, "AI服务密钥无效"),
    AI_RATE_LIMIT_EXCEEDED(7005, "AI服务调用频率超限"),

    // ========== 限流模块错误 8xxx ==========
    RATE_LIMIT_EXCEEDED(8001, "请求过于频繁，请稍后再试");
    
    private final Integer code;
    private final String message;
}
