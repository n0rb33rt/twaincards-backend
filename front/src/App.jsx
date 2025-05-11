// src/App.jsx
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// Context Providers
import { AuthProvider, useAuth } from './context/AuthContext.jsx';
import { isTokenValid } from './utils/tokenManager';

// Components
import Header from './components/common/Header.jsx';
import Footer from './components/common/Footer.jsx';
import ProtectedRoute from './components/auth/ProtectedRoute.jsx';
import PrivateRoute from './components/auth/PrivateRoute.jsx';

// Pages
import Login from './pages/auth/Login.jsx';
import Register from './pages/auth/Register.jsx';
import EmailConfirmation from './pages/auth/EmailConfirmation.jsx';
import ForgotPassword from './pages/ForgotPassword.jsx';
import ResetPassword from './pages/ResetPassword.jsx';
import Dashboard from './pages/Dashboard.jsx';
import CollectionList from './pages/collections/CollectionList.jsx';
import CollectionDetail from './pages/collections/CollectionDetail.jsx';
import CollectionForm from './pages/collections/CollectionForm.jsx';
import CardList from './pages/cards/CardList.jsx';
import CardForm from './pages/cards/CardForm.jsx';
import StudyDashboard from './pages/study/StudyDashboard.jsx';
import StudySession from './pages/study/StudySession.jsx';
import ReviewSummary from './pages/study/ReviewSummary.jsx';
import Statistics from './pages/Statistics.jsx';
import Profile from "./pages/Profile.jsx";
import NotFound from './pages/NotFound.jsx';
import Security from './pages/Security';
import AdminDashboard from './pages/AdminDashboard';

// Styles
import './styles/main.css';

// Root component to handle the home route
const RootRedirect = () => {
  // Directly check if token is valid without needing to wait for auth context to be ready
  const hasValidToken = isTokenValid();
  
  // If we have a valid token, go to dashboard, otherwise go to login
  return <Navigate to={hasValidToken ? "/dashboard" : "/login"} replace />;
};

const AppRoutes = () => {
  const { currentUser, loading } = useAuth();

  return (
    <Router>
      <div className="app-container">
        <Header />
        <main className="main-content" style={{ maxWidth: '100%' }}>
          <Routes>
            {/* Root Route - Redirect based on token validity */}
            <Route path="/" element={<RootRedirect />} />

            {/* Public Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/confirm-email" element={<EmailConfirmation />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />

            {/* Protected Routes */}
            <Route path="/dashboard" element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            } />

            {/* Collections Routes */}
            <Route path="/collections" element={
              <ProtectedRoute>
                <CollectionList />
              </ProtectedRoute>
            } />

            <Route path="/collections/new" element={
              <ProtectedRoute>
                <CollectionForm />
              </ProtectedRoute>
            } />

            <Route path="/collections/:id" element={
              <ProtectedRoute>
                <CollectionDetail />
              </ProtectedRoute>
            } />

            <Route path="/collections/:id/edit" element={
              <ProtectedRoute>
                <CollectionForm isEdit={true} />
              </ProtectedRoute>
            } />

            {/* Cards Routes */}
            <Route path="/collections/:collectionId/cards" element={
              <ProtectedRoute>
                <CardList />
              </ProtectedRoute>
            } />

            <Route path="/collections/:collectionId/cards/new" element={
              <ProtectedRoute>
                <CardForm />
              </ProtectedRoute>
            } />

            <Route path="/collections/:collectionId/cards/:id/edit" element={
              <ProtectedRoute>
                <CardForm isEdit={true} />
              </ProtectedRoute>
            } />

            <Route path="/cards/:id/edit" element={
              <ProtectedRoute>
                <CardForm isEdit={true} />
              </ProtectedRoute>
            } />

            {/* Study Routes */}
            <Route path="/study" element={
              <ProtectedRoute>
                <StudyDashboard />
              </ProtectedRoute>
            } />

            {/* Support both the old and new URL patterns for backward compatibility */}
            <Route path="/study/:collectionId" element={
              <ProtectedRoute>
                <StudySession />
              </ProtectedRoute>
            } />

            <Route path="/study/session/:collectionId" element={
              <ProtectedRoute>
                <StudySession />
              </ProtectedRoute>
            } />

            {/* Summary routes with collection ID */}
            <Route path="/study/:collectionId/summary" element={
              <ProtectedRoute>
                <ReviewSummary />
              </ProtectedRoute>
            } />

            <Route path="/study/session/:collectionId/summary" element={
              <ProtectedRoute>
                <ReviewSummary />
              </ProtectedRoute>
            } />

            {/* Summary routes with session ID */}
            <Route path="/study/sessions/:sessionId/summary" element={
              <ProtectedRoute>
                <ReviewSummary />
              </ProtectedRoute>
            } />

            {/* Statistics Route */}
            <Route path="/statistics" element={
              <ProtectedRoute>
                <Statistics />
              </ProtectedRoute>
            } />

            {/* Profile Route */}
            <Route path="/profile" element={
              <ProtectedRoute>
                <Profile />
              </ProtectedRoute>
            } />

            <Route path="/security" element={
              <ProtectedRoute>
                <Security />
              </ProtectedRoute>
            } />

            {/* Admin Dashboard Route */}
            <Route
              path="/admin"
              element={
                <ProtectedRoute>
                  {currentUser?.role === 'ADMIN' ? (
                    <AdminDashboard />
                  ) : (
                    <Navigate to="/dashboard" replace />
                  )}
                </ProtectedRoute>
              }
            />

            {/* Admin Languages Route - direct to AdminDashboard with languages tab active */}
            <Route
              path="/admin/languages"
              element={
                <ProtectedRoute>
                  {currentUser?.role === 'ADMIN' ? (
                    <AdminDashboard activeTab="languages" />
                  ) : (
                    <Navigate to="/dashboard" replace />
                  )}by
                </ProtectedRoute>
              }
            />

            {/* Not Found Route */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </main>
        <ToastContainer position="bottom-right" />
      </div>
    </Router>
  );
};

const App = () => {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
};

export default App;