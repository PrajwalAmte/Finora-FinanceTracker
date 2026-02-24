import React from 'react';
import { Navigate } from 'react-router-dom';
import { DollarSign } from 'lucide-react';
import { useAuth } from '../../utils/auth-context';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="h-screen flex items-center justify-center bg-neutral-50 dark:bg-neutral-950">
        <div className="flex flex-col items-center space-y-4">
          <DollarSign className="text-primary-600 dark:text-primary-500 animate-pulse" size={40} />
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
