import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  AlertCircle,
  CheckCircle,
  FileText,
  HelpCircle,
  Loader2,
  Sparkles,
} from 'lucide-react';
import { prdReviewApi, PrdReviewRequest, PrdReviewResponse, ReviewDetailLevel } from '../api/prdReview';
import { knowledgeBaseApi, KnowledgeBaseItem } from '../api/knowledgebase';
import { useEffect } from 'react';

const DETAIL_LEVEL_OPTIONS: { value: ReviewDetailLevel; label: string; description: string }[] = [
  { value: 'BASIC', label: '基础评审', description: '快速评审，关注核心问题' },
  { value: 'DETAILED', label: '详细评审', description: '深入分析，提供详细建议' },
  { value: 'COMPREHENSIVE', label: '全面评审', description: '全方位评审，包含风险评估' },
];

export default function PrdReviewPage() {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [detailLevel, setDetailLevel] = useState<ReviewDetailLevel>('BASIC');
  const [enableKnowledgeBaseHints, setEnableKnowledgeBaseHints] = useState(false);
  const [selectedKbIds, setSelectedKbIds] = useState<number[]>([]);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PrdReviewResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadKnowledgeBases();
  }, []);

  const loadKnowledgeBases = async () => {
    try {
      const kbs = await knowledgeBaseApi.getAllKnowledgeBases('time');
      setKnowledgeBases(kbs.filter(kb => kb.vectorStatus === 'COMPLETED'));
    } catch (err) {
      console.error('加载知识库失败:', err);
    }
  };

  const handleSubmit = async () => {
    if (!content.trim()) {
      setError('请输入 PRD 内容');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const request: PrdReviewRequest = {
        title: title.trim() || undefined,
        content: content.trim(),
        detailLevel,
        enableKnowledgeBaseHints,
        knowledgeBaseIds: enableKnowledgeBaseHints ? selectedKbIds : undefined,
      };

      const response = await prdReviewApi.review(request);
      setResult(response);
    } catch (err: any) {
      setError(err.message || '评审失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleKbToggle = (kbId: number) => {
    setSelectedKbIds(prev =>
      prev.includes(kbId)
        ? prev.filter(id => id !== kbId)
        : [...prev, kbId]
    );
  };

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-800 dark:text-white flex items-center gap-3">
          <FileText className="w-7 h-7 text-primary-500" />
          PRD 评审
        </h1>
        <p className="text-slate-500 dark:text-slate-400 mt-1">
          输入产品需求文档，AI 将从多个维度进行专业评审
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-6">
          <div className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border border-slate-100 dark:border-slate-700">
            <h2 className="text-lg font-semibold text-slate-800 dark:text-white mb-4">
              PRD 内容
            </h2>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                  标题（可选）
                </label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="输入 PRD 标题"
                  className="w-full px-4 py-2 border border-slate-200 dark:border-slate-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white dark:bg-slate-700 text-slate-900 dark:text-white"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                  PRD 内容 <span className="text-red-500">*</span>
                </label>
                <textarea
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  placeholder="粘贴或输入 PRD 文档内容..."
                  rows={12}
                  className="w-full px-4 py-3 border border-slate-200 dark:border-slate-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white dark:bg-slate-700 text-slate-900 dark:text-white resize-none font-mono text-sm"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  评审粒度
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {DETAIL_LEVEL_OPTIONS.map((option) => (
                    <button
                      key={option.value}
                      onClick={() => setDetailLevel(option.value)}
                      className={`px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                        detailLevel === option.value
                          ? 'bg-primary-500 text-white'
                          : 'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-600'
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                  {DETAIL_LEVEL_OPTIONS.find(o => o.value === detailLevel)?.description}
                </p>
              </div>

              <div className="border-t border-slate-200 dark:border-slate-700 pt-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={enableKnowledgeBaseHints}
                    onChange={(e) => setEnableKnowledgeBaseHints(e.target.checked)}
                    className="w-4 h-4 rounded border-slate-300 text-primary-500 focus:ring-primary-500"
                  />
                  <span className="text-sm text-slate-700 dark:text-slate-300">
                    启用知识库辅助评审
                  </span>
                  <span title="使用知识库中的内容辅助评审">
                    <HelpCircle className="w-4 h-4 text-slate-400 cursor-help" />
                  </span>
                </label>

                {enableKnowledgeBaseHints && knowledgeBases.length > 0 && (
                  <div className="mt-3 p-3 bg-slate-50 dark:bg-slate-700/50 rounded-lg">
                    <p className="text-xs text-slate-500 dark:text-slate-400 mb-2">
                      选择要使用的知识库：
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {knowledgeBases.map((kb) => (
                        <button
                          key={kb.id}
                          onClick={() => handleKbToggle(kb.id)}
                          className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                            selectedKbIds.includes(kb.id)
                              ? 'bg-primary-500 text-white'
                              : 'bg-white dark:bg-slate-600 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-500'
                          }`}
                        >
                          {kb.name}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {error && (
                <div className="flex items-center gap-2 text-red-500 text-sm">
                  <AlertCircle className="w-4 h-4" />
                  {error}
                </div>
              )}

              <button
                onClick={handleSubmit}
                disabled={loading || !content.trim()}
                className="w-full flex items-center justify-center gap-2 px-6 py-3 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    正在评审...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-5 h-5" />
                    开始评审
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          {result ? (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border border-slate-100 dark:border-slate-700"
            >
              <h2 className="text-lg font-semibold text-slate-800 dark:text-white mb-4 flex items-center gap-2">
                <CheckCircle className="w-5 h-5 text-green-500" />
                评审结果
              </h2>

              <div className="space-y-6">
                {result.summary && (
                  <div className="p-4 bg-slate-50 dark:bg-slate-700/50 rounded-lg">
                    <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-2">
                      总体评价
                    </h3>
                    <p className="text-slate-600 dark:text-slate-400 text-sm leading-relaxed">
                      {result.summary}
                    </p>
                  </div>
                )}

                <div className="grid grid-cols-2 gap-3">
                  {[
                    { key: 'clarity', label: '需求清晰度', data: result.clarity },
                    { key: 'scope', label: '范围与边界', data: result.scope },
                    { key: 'userFlows', label: '用户场景/流程', data: result.userFlows },
                    { key: 'techRisk', label: '技术风险', data: result.techRisk },
                    { key: 'metrics', label: '指标与验收标准', data: result.metrics },
                    { key: 'estimation', label: '工作量评估', data: result.estimation },
                  ].map(({ key, label, data }) => (
                    data && (
                      <div key={key} className="p-3 bg-slate-50 dark:bg-slate-700/50 rounded-lg">
                        <div className="flex items-center justify-between mb-1">
                          <span className="text-xs font-medium text-slate-600 dark:text-slate-400">
                            {label}
                          </span>
                          {data.score !== null && (
                            <span className={`text-sm font-bold ${
                              data.score >= 80 ? 'text-green-500' :
                              data.score >= 60 ? 'text-yellow-500' : 'text-red-500'
                            }`}>
                              {data.score}
                            </span>
                          )}
                        </div>
                        {data.score !== null && (
                          <div className="w-full bg-slate-200 dark:bg-slate-600 rounded-full h-1.5">
                            <div
                              className={`h-1.5 rounded-full ${
                                data.score >= 80 ? 'bg-green-500' :
                                data.score >= 60 ? 'bg-yellow-500' : 'bg-red-500'
                              }`}
                              style={{ width: `${data.score}%` }}
                            />
                          </div>
                        )}
                      </div>
                    )
                  ))}
                </div>

                {result.overallSuggestions && result.overallSuggestions.length > 0 && (
                  <div>
                    <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-2">
                      综合改进建议
                    </h3>
                    <ul className="space-y-2">
                      {result.overallSuggestions.map((suggestion, index) => (
                        <li key={index} className="flex items-start gap-2 text-sm text-slate-600 dark:text-slate-400">
                          <span className="text-primary-500 mt-0.5">•</span>
                          {suggestion}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </motion.div>
          ) : (
            <div className="bg-white dark:bg-slate-800 rounded-xl p-12 shadow-sm border border-slate-100 dark:border-slate-700 text-center">
              <FileText className="w-16 h-16 text-slate-300 dark:text-slate-600 mx-auto mb-4" />
              <p className="text-slate-500 dark:text-slate-400">
                输入 PRD 内容后点击"开始评审"
              </p>
              <p className="text-sm text-slate-400 dark:text-slate-500 mt-2">
                AI 将从需求清晰度、范围边界、用户场景、技术风险等多个维度进行评审
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
