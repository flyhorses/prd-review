import { useCallback, useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  AlertCircle,
  CheckCircle,
  Clock,
  Download,
  Eye,
  FileText,
  HardDrive,
  Loader2,
  RefreshCw,
  Search,
  Trash2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { prdReviewApi, PrdReviewListItem, AsyncTaskStatus } from '../api/prdReview';
import DeleteConfirmDialog from '../components/DeleteConfirmDialog';

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
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
      return '未知';
  }
}

function getDetailLevelText(level: string): string {
  switch (level) {
    case 'BASIC':
      return '基础评审';
    case 'DETAILED':
      return '详细评审';
    case 'COMPREHENSIVE':
      return '全面评审';
    default:
      return level;
  }
}

export default function PrdReviewHistoryPage() {
  const navigate = useNavigate();
  const [reviews, setReviews] = useState<PrdReviewListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [deleteItem, setDeleteItem] = useState<PrdReviewListItem | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [retryingId, setRetryingId] = useState<number | null>(null);

  const loadReviews = useCallback(async () => {
    try {
      setLoading(true);
      const data = await prdReviewApi.getReviewList();
      setReviews(data);
    } catch (error) {
      console.error('加载评审记录失败:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadReviews();
  }, [loadReviews]);

  // 轮询：当有 PENDING 或 PROCESSING 状态时，每5秒刷新一次
  useEffect(() => {
    const hasPendingItems = reviews.some(
      r => r.reviewStatus === 'PENDING' || r.reviewStatus === 'PROCESSING'
    );

    if (hasPendingItems && !loading) {
      const timer = setInterval(() => {
        loadReviews();
      }, 5000);

      return () => clearInterval(timer);
    }
  }, [reviews, loading, loadReviews]);

  const handleRetry = async (id: number) => {
    try {
      setRetryingId(id);
      await prdReviewApi.retryReview(id);
      await loadReviews();
    } catch (error) {
      console.error('重试失败:', error);
    } finally {
      setRetryingId(null);
    }
  };

  const handleDownload = async (review: PrdReviewListItem) => {
    try {
      const blob = await prdReviewApi.exportReviewPdf(review.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `PRD评审报告_${review.id}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('下载失败:', error);
    }
  };

  const handleDelete = async () => {
    if (!deleteItem) return;
    try {
      setDeleting(true);
      await prdReviewApi.deleteReview(deleteItem.id);
      setDeleteItem(null);
      await loadReviews();
    } catch (error) {
      console.error('删除失败:', error);
    } finally {
      setDeleting(false);
    }
  };

  const handleViewDetail = (id: number) => {
    navigate(`/prd-review/${id}`);
  };

  return (
    <div className="max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-white flex items-center gap-3">
            <FileText className="w-7 h-7 text-primary-500" />
            PRD 评审记录
          </h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1">
            查看和管理所有 PRD 评审记录
          </p>
        </div>
        <button
          onClick={() => navigate('/prd-review')}
          className="flex items-center gap-2 px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
        >
          新建评审
        </button>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl p-4 shadow-sm border border-slate-100 dark:border-slate-700 mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            placeholder="搜索 PRD 标题..."
            className="w-full pl-10 pr-4 py-2 border border-slate-200 dark:border-slate-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent bg-white dark:bg-slate-700 text-slate-900 dark:text-white"
          />
        </div>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-100 dark:border-slate-700 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
          </div>
        ) : reviews.length === 0 ? (
          <div className="text-center py-20">
            <HardDrive className="w-16 h-16 text-slate-300 mx-auto mb-4" />
            <p className="text-slate-500 dark:text-slate-400">暂无评审记录</p>
            <button
              onClick={() => navigate('/prd-review')}
              className="mt-4 text-primary-500 hover:text-primary-600"
            >
              创建第一个评审
            </button>
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-slate-50 dark:bg-slate-700 border-b border-slate-100 dark:border-slate-600">
              <tr>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600 dark:text-slate-300">
                  标题
                </th>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600 dark:text-slate-300">
                  评审粒度
                </th>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600 dark:text-slate-300">
                  状态
                </th>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600 dark:text-slate-300">
                  评分
                </th>
                <th className="text-left px-6 py-4 text-sm font-medium text-slate-600 dark:text-slate-300">
                  创建时间
                </th>
                <th className="text-right px-6 py-4 text-sm font-medium text-slate-600 dark:text-slate-300">
                  操作
                </th>
              </tr>
            </thead>
            <tbody>
              {reviews.map((review, index) => (
                <motion.tr
                  key={review.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-b border-slate-50 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors"
                >
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <FileText className="w-5 h-5 text-slate-400" />
                      <span className="font-medium text-slate-800 dark:text-white">
                        {review.title || '无标题'}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">
                    {getDetailLevelText(review.detailLevel)}
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <StatusIcon status={review.reviewStatus} />
                      <span className="text-sm text-slate-600 dark:text-slate-300">
                        {getStatusText(review.reviewStatus)}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    {review.overallScore !== null ? (
                      <span className={`text-sm font-bold ${
                        review.overallScore >= 80 ? 'text-green-500' :
                        review.overallScore >= 60 ? 'text-yellow-500' : 'text-red-500'
                      }`}>
                        {review.overallScore}
                      </span>
                    ) : (
                      <span className="text-sm text-slate-400">-</span>
                    )}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-500 dark:text-slate-400">
                    {formatDate(review.createdAt)}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      {review.reviewStatus === 'COMPLETED' && (
                        <>
                          <button
                            onClick={() => handleViewDetail(review.id)}
                            className="p-2 text-slate-400 hover:text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/30 rounded-lg transition-colors"
                            title="查看详情"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDownload(review)}
                            className="p-2 text-slate-400 hover:text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/30 rounded-lg transition-colors"
                            title="下载 PDF"
                          >
                            <Download className="w-4 h-4" />
                          </button>
                        </>
                      )}
                      {review.reviewStatus === 'FAILED' && (
                        <button
                          onClick={() => handleRetry(review.id)}
                          disabled={retryingId === review.id}
                          className="p-2 text-slate-400 hover:text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/30 rounded-lg transition-colors disabled:opacity-50"
                          title="重试"
                        >
                          <RefreshCw className={`w-4 h-4 ${retryingId === review.id ? 'animate-spin' : ''}`} />
                        </button>
                      )}
                      <button
                        onClick={() => setDeleteItem(review)}
                        className="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors"
                        title="删除"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <DeleteConfirmDialog
        open={deleteItem !== null}
        item={deleteItem}
        itemType="评审记录"
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteItem(null)}
      />
    </div>
  );
}
