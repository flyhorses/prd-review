import { BrowserRouter, Navigate, Route, Routes, useNavigate, useLocation } from 'react-router-dom';
import Layout from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AuthProvider } from './contexts/AuthContext';
import { Suspense, lazy } from 'react';
import type { UploadKnowledgeBaseResponse } from './api/knowledgebase';

const PrdReviewPage = lazy(() => import('./pages/PrdReviewPage'));
const PrdReviewHistoryPage = lazy(() => import('./pages/PrdReviewHistoryPage'));
const PrdReviewDetailPage = lazy(() => import('./pages/PrdReviewDetailPage'));
const KnowledgeBaseQueryPage = lazy(() => import('./pages/KnowledgeBaseQueryPage'));
const KnowledgeBaseUploadPage = lazy(() => import('./pages/KnowledgeBaseUploadPage'));
const KnowledgeBaseManagePage = lazy(() => import('./pages/KnowledgeBaseManagePage'));
const UserManagePage = lazy(() => import('./pages/UserManagePage'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const RegisterPage = lazy(() => import('./pages/RegisterPage'));

const Loading = () => (
  <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-slate-50 to-indigo-50 dark:from-slate-900 dark:to-slate-800">
    <div className="w-10 h-10 border-3 border-slate-200 border-t-primary-500 rounded-full animate-spin" />
  </div>
);

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Suspense fallback={<Loading />}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/" element={<Layout />}>
              <Route index element={<Navigate to="/prd-review" replace />} />
              <Route path="prd-review" element={<PrdReviewPage />} />
              <Route path="prd-review/history" element={<PrdReviewHistoryPage />} />
              <Route path="prd-review/:id" element={<PrdReviewDetailPage />} />
              <Route path="knowledgebase" element={<KnowledgeBaseManagePageWrapper />} />
              <Route path="knowledgebase/upload" element={<KnowledgeBaseUploadPageWrapper />} />
              <Route path="knowledgebase/chat" element={<KnowledgeBaseQueryPageWrapper />} />
              <Route 
                path="users" 
                element={
                  <ProtectedRoute requireAdmin>
                    <UserManagePage />
                  </ProtectedRoute>
                } 
              />
            </Route>
          </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  );
}

function KnowledgeBaseManagePageWrapper() {
  const navigate = useNavigate();

  const handleUpload = () => {
    navigate('/knowledgebase/upload');
  };

  const handleChat = () => {
    navigate('/knowledgebase/chat');
  };

  return <KnowledgeBaseManagePage onUpload={handleUpload} onChat={handleChat} />;
}

function KnowledgeBaseQueryPageWrapper() {
  const navigate = useNavigate();
  const location = useLocation();
  const isChatMode = location.pathname === '/knowledgebase/chat';

  const handleBack = () => {
    if (isChatMode) {
      navigate('/knowledgebase');
    } else {
      navigate('/prd-review');
    }
  };

  const handleUpload = () => {
    navigate('/knowledgebase/upload');
  };

  return <KnowledgeBaseQueryPage onBack={handleBack} onUpload={handleUpload} />;
}

function KnowledgeBaseUploadPageWrapper() {
  const navigate = useNavigate();

  const handleUploadComplete = (_result: UploadKnowledgeBaseResponse) => {
    navigate('/knowledgebase');
  };

  const handleBack = () => {
    navigate('/knowledgebase');
  };

  return <KnowledgeBaseUploadPage onUploadComplete={handleUploadComplete} onBack={handleBack} />;
}

export default App;
