import { request } from './request';
import axios from 'axios';

const API_BASE_URL = import.meta.env.PROD ? '' : 'http://localhost:8080';

export type ReviewDetailLevel = 'BASIC' | 'DETAILED' | 'COMPREHENSIVE';
export type AsyncTaskStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface PrdReviewRequest {
  title?: string;
  content: string;
  detailLevel?: ReviewDetailLevel;
  enableKnowledgeBaseHints?: boolean;
  knowledgeBaseIds?: number[];
}

export interface DimensionEvaluation {
  score: number | null;
  issues: string[];
  suggestions: string[];
}

export interface PrdReviewResponse {
  summary: string;
  clarity: DimensionEvaluation | null;
  scope: DimensionEvaluation | null;
  userFlows: DimensionEvaluation | null;
  techRisk: DimensionEvaluation | null;
  metrics: DimensionEvaluation | null;
  estimation: DimensionEvaluation | null;
  overallSuggestions: string[];
}

export interface PrdReviewListItem {
  id: number;
  title: string;
  detailLevel: string;
  reviewStatus: AsyncTaskStatus;
  reviewError: string | null;
  overallScore: number | null;
  createdAt: string;
}

export interface PrdReviewStatusResponse {
  prdId: number;
  status: AsyncTaskStatus;
}

export const prdReviewApi = {
  /**
   * 同步评审 PRD
   */
  async review(req: PrdReviewRequest): Promise<PrdReviewResponse> {
    return request.post<PrdReviewResponse>('/api/prd/review', req, {
      timeout: 180000,
    });
  },

  /**
   * 异步提交 PRD 评审任务
   */
  async submitReviewAsync(req: PrdReviewRequest): Promise<{ prdId: number }> {
    return request.post<{ prdId: number }>('/api/prd/review/async', req);
  },

  /**
   * 获取 PRD 评审状态
   */
  async getReviewStatus(prdId: number): Promise<PrdReviewStatusResponse> {
    return request.get<PrdReviewStatusResponse>(`/api/prd/review/${prdId}/status`);
  },

  /**
   * 获取 PRD 评审结果
   */
  async getReview(prdId: number): Promise<PrdReviewResponse> {
    return request.get<PrdReviewResponse>(`/api/prd/review/${prdId}`);
  },

  /**
   * 获取评审记录列表
   */
  async getReviewList(): Promise<PrdReviewListItem[]> {
    return request.get<PrdReviewListItem[]>('/api/prd/reviews');
  },

  /**
   * 获取评审详情
   */
  async getReviewDetail(prdId: number): Promise<{ id: number; title: string; status: string; hasResult: boolean }> {
    return request.get(`/api/prd/review/${prdId}/detail`);
  },

  /**
   * 重新评审（重试）
   */
  async retryReview(prdId: number): Promise<void> {
    return request.post(`/api/prd/review/${prdId}/retry`);
  },

  /**
   * 删除评审记录
   */
  async deleteReview(prdId: number): Promise<void> {
    return request.delete(`/api/prd/review/${prdId}`);
  },

  /**
   * 导出 PRD 评审报告为 PDF
   */
  async exportReviewPdf(prdId: number): Promise<Blob> {
    const response = await axios.get(`${API_BASE_URL}/api/prd/review/${prdId}/export`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
