import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  AlertCircle,
  CheckCircle,
  Clock,
  Download,
  FileText,
  Loader2,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { prdReviewApi, PrdReviewResponse, AsyncTaskStatus } from '../api/prdReview';

function getStatusText(status: AsyncTaskStatus): string {
  switch (status) {
    case 'COMPLETED':
      return '已完成';
    case 'PROCESSING':
      return '评审中';
    case 'PENDING':
      return '待评审';
    case 'FAILED':
      return '失败';
    default:
      return status;
  }
}

function StatusIcon({ status }: { status: AsyncTaskStatus }) {
  switch (status) {
    case 'COMPLETED':
      return <CheckCircle className="w-4 h-4 text-green-500" />;
    case 'PROCESSING':
      return <Loader2 className="w-4 h-4 text-blue-500 animate-spin" />;
    case 'PENDING':
      return <Clock className="w-4 h-4 text-yellow-500" />;
    case 'FAILED':
      return <AlertCircle className="w-4 h-4 text-red-500" />;
    default:
      return <CheckCircle className="w-4 h-4 text-green-500" />;
  }
}

function getScoreColor(score: number): string {
  if (score >= 80) return 'text-green-500';
  if (score >= 60) return 'text-yellow-500';
  return 'text-red-500';
}

function getProgressBgColor(score: number): string {
  if (score >= 80) return 'bg-green-500';
  if (score >= 60) return 'bg-yellow-500';
  return 'bg-red-500';
}

export default function PrdReviewDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [review, setReview] = useState<PrdReviewResponse | null>(null);
  const [status, setStatus] = useState<AsyncTaskStatus>('PENDING');

  useEffect(() => {
    loadReviewDetail();
  }, [id]);

  const loadReviewDetail = async () => {
    try {
      setLoading(true);
      setError(null);

      const statusRes = await prdReviewApi.getReviewStatus(Number(id));
      setStatus(statusRes.status as AsyncTaskStatus);

      if (statusRes.status === 'COMPLETED') {
        const reviewData = await prdReviewApi.getReview(Number(id));
        setReview(reviewData);
      }
    } catch (err: any) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  const handleRetry = async () => {
    try {
      await prdReviewApi.retryReview(Number(id));
      loadReviewDetail();
    } catch (err: any) {
      setError(err instanceof Error ? err.message : '重试失败');
    }
  };

  const handleDownload = async () => {
    try {
      const blob = await prdReviewApi.exportReviewPdf(Number(id));
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `PRD评审报告_${id}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      setError(err instanceof Error ? err.message : '下载失败');
    }
  };

  const renderDimensionSection = (
    title: string,
    dimension: { score: number | null; issues: string[]; suggestions: string[] } | null
  ) => {
    if (!dimension) return null;

    return (
      <div className="bg-white dark:bg-slate-700 rounded-xl p-6 border border-slate-200 dark:border-slate-600">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-slate-800 dark:text-white">{title}</h3>
          {dimension.score !== null && (
            <span className={`text-2xl font-bold ${getScoreColor(dimension.score)}`}>
              {dimension.score}
            </span>
          )}
        </div>

        {dimension.score !== null && (
          <div className="mb-4">
            <div className="w-full bg-slate-200 dark:bg-slate-600 rounded-full h-2">
              <div
                className={`h-2 rounded-full ${getProgressBgColor(dimension.score)}`}
                style={{ width: `${dimension.score}%` }}
              />
            </div>
          </div>
        )}

        {dimension.issues && dimension.issues.length > 0 && (
          <div className="mb-3">
            <h4 className="text-sm font-medium text-red-500 mb-2">问题点</h4>
            <ul className="space-y-1">
              {dimension.issues.map((issue, index) => (
                <li key={index} className="text-sm text-slate-600 dark:text-slate-300 flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" />
                  {issue}
                </li>
              ))}
            </ul>
          </div>
        )}

        {dimension.suggestions && dimension.suggestions.length > 0 && (
          <div>
            <h4 className="text-sm font-medium text-green-600 dark:text-green-400 mb-2">改进建议</h4>
            <ul className="space-y-1">
              {dimension.suggestions.map((suggestion, index) => (
                <li key={index} className="text-sm text-slate-600 dark:text-slate-300 flex items-start gap-2">
                  <CheckCircle className="w-4 h-4 text-green-400 flex-shrink-0 mt-0.5" />
                  {suggestion}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 dark:bg-red-900/20 rounded-xl p-6">
        <div className="flex items-center gap-3 text-red-600 dark:text-red-400">
          <AlertCircle className="w-5 h-5" />
          <span>{error}</span>
        </div>
      </div>
    );
  }

  if (status === 'PENDING' || status === 'PROCESSING') {
    return (
      <div className="text-center py-20">
        <div className="flex flex-col items-center gap-4">
          {status === 'PROCESSING' ? (
            <Loader2 className="w-12 h-12 text-primary-500 animate-spin" />
          ) : (
            <Clock className="w-12 h-12 text-yellow-500" />
          )}
          <p className="text-slate-600 dark:text-slate-400">
            {status === 'PROCESSING' ? '正在评审中...' : '等待评审...'}
          </p>
          {status === 'PENDING' && (
            <button
              onClick={handleRetry}
              className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
            >
              立即评审
            </button>
          )}
        </div>
      </div>
    );
  }

  if (status === 'FAILED') {
    return (
      <div className="text-center py-20">
        <div className="flex flex-col items-center gap-4">
          <AlertCircle className="w-12 h-12 text-red-500" />
          <p className="text-slate-600 dark:text-slate-400">评审失败</p>
          <button
            onClick={handleRetry}
            className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            重新评审
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <button
          onClick={() => navigate('/prd-review/history')}
          className="text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 mb-4"
        >
          ← 返回列表
        </button>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-100 dark:border-slate-700 overflow-hidden"
      >
        <div className="p-6 border-b border-slate-100 dark:border-slate-700">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <FileText className="w-6 h-6 text-primary-500" />
              <h1 className="text-xl font-bold text-slate-800 dark:text-white">
                PRD 评审详情
              </h1>
            </div>
            <div className="flex items-center gap-2">
              <StatusIcon status={status} />
              <span className="text-sm text-slate-600 dark:text-slate-400">
                {getStatusText(status)}
              </span>
            </div>
          </div>
        </div>

        {review && (
          <div className="p-6 space-y-6">
            {review.summary && (
              <div className="bg-slate-50 dark:bg-slate-700/50 rounded-xl p-4">
                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-2">
                  总体评价
                </h3>
                <p className="text-slate-600 dark:text-slate-400 leading-relaxed">
                  {review.summary}
                </p>
              </div>
            )}

            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              {[
                { key: 'clarity', label: '需求清晰度', data: review.clarity },
                { key: 'scope', label: '范围与边界', data: review.scope },
                { key: 'userFlows', label: '用户场景/流程', data: review.userFlows },
                { key: 'techRisk', label: '技术风险', data: review.techRisk },
                { key: 'metrics', label: '指标与验收标准', data: review.metrics },
                { key: 'estimation', label: '工作量评估', data: review.estimation },
              ].map(({ key, label, data }) => (
                data && (
                  <div key={key} className="bg-slate-50 dark:bg-slate-700/50 rounded-xl p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-medium text-slate-600 dark:text-slate-400">
                        {label}
                      </span>
                      {data.score !== null && (
                        <span className={`text-lg font-bold ${getScoreColor(data.score)}`}>
                          {data.score}
                        </span>
                      )}
                    </div>
                    {data.score !== null && (
                      <div className="w-full bg-slate-200 dark:bg-slate-600 rounded-full h-1.5 mb-2">
                        <div
                          className={`h-1.5 rounded-full ${getProgressBgColor(data.score)}`}
                          style={{ width: `${data.score}%` }}
                        />
                      </div>
                    )}
                  </div>
                )
              ))}
            </div>

            {[
              { key: 'clarity', label: '需求清晰度', data: review.clarity },
              { key: 'scope', label: '范围与边界', data: review.scope },
              { key: 'userFlows', label: '用户场景/流程', data: review.userFlows },
              { key: 'techRisk', label: '技术风险', data: review.techRisk },
              { key: 'metrics', label: '指标与验收标准', data: review.metrics },
              { key: 'estimation', label: '工作量评估', data: review.estimation },
            ].map(({ label, data }) => (
              renderDimensionSection(label, data)
            ))}

            {review.overallSuggestions && review.overallSuggestions.length > 0 && (
              <div className="bg-green-50 dark:bg-green-900/20 rounded-xl p-6">
                <h3 className="text-lg font-semibold text-green-700 dark:text-green-400 mb-4">
                  综合改进建议
                </h3>
                <ul className="space-y-2">
                  {review.overallSuggestions.map((suggestion, index) => (
                    <li key={index} className="flex items-start gap-3 text-slate-600 dark:text-slate-300">
                      <CheckCircle className="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5" />
                      <span>{suggestion}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <div className="flex justify-end pt-4 border-t border-slate-100 dark:border-slate-700">
              <button
                onClick={handleDownload}
                className="flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
              >
                <Download className="w-4 h-4" />
                导出 PDF
              </button>
            </div>
          </div>
        )}
      </motion.div>
    </div>
  );
}
