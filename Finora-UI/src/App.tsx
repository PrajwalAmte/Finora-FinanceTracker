import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './utils/theme-context';
import { AuthProvider } from './utils/auth-context';
import { LocalVaultProvider } from './utils/local-vault-context';
import ProtectedRoute from './components/auth/ProtectedRoute';
import { Layout } from './components/layout/Layout';
import { LandingPage } from './pages/LandingPage';
import { LocalVaultPage } from './pages/LocalVaultPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { Dashboard } from './pages/Dashboard';
import { InvestmentsPage } from './pages/InvestmentsPage';
import { SipsPage } from './pages/SipsPage';
import { LoansPage } from './pages/LoansPage';
import { ExpensesPage } from './pages/ExpensesPage';
import { ProfilePage } from './pages/ProfilePage';
import { BackupPage } from './pages/BackupPage';

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <LocalVaultProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/welcome" element={<LandingPage />} />
              <Route path="/vault" element={<LocalVaultPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <Layout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<Dashboard />} />
                <Route path="investments" element={<InvestmentsPage />} />
                <Route path="sips" element={<SipsPage />} />
                <Route path="loans" element={<LoansPage />} />
                <Route path="expenses" element={<ExpensesPage />} />
                <Route path="profile" element={<ProfilePage />} />
                <Route path="backup" element={<BackupPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Route>
            </Routes>
          </BrowserRouter>
        </LocalVaultProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;