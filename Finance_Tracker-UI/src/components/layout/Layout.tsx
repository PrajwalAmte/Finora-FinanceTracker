import React, { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';

export const Layout: React.FC = () => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const location = useLocation();
  
  // Helper function to get page title based on current route
  const getPageTitle = (): string => {
    const path = location.pathname;
    
    if (path === '/') return 'Dashboard';
    if (path.startsWith('/investments')) return 'Investments';
    if (path.startsWith('/sips')) return 'Systematic Investment Plans';
    if (path.startsWith('/loans')) return 'Loans';
    if (path.startsWith('/expenses')) return 'Expenses';
    
    return 'Finora';
  };
  
  // Helper function to get page subtitle based on current route
  const getPageSubtitle = (): string | undefined => {
    const path = location.pathname;
    
    if (path === '/') return 'Overview of your financial portfolio';
    if (path.startsWith('/investments')) return 'Manage your stock and mutual fund investments';
    if (path.startsWith('/sips')) return 'Track your systematic investment plans';
    if (path.startsWith('/loans')) return 'Monitor your loans and EMIs';
    if (path.startsWith('/expenses')) return 'Track your daily expenses and spending patterns';
    
    return undefined;
  };
  
  return (
    <div className="h-screen flex overflow-hidden bg-neutral-50 dark:bg-neutral-950">
      <Sidebar 
        isMobileOpen={isMobileMenuOpen} 
        onMobileClose={() => setIsMobileMenuOpen(false)}
      />
      
      <div className="flex-1 flex flex-col overflow-hidden md:ml-64">
        <Header 
          title={getPageTitle()}
          subtitle={getPageSubtitle()}
          onMobileMenuClick={() => setIsMobileMenuOpen(true)} 
        />
        
        <main className="flex-1 overflow-auto p-4 sm:p-6 lg:p-8">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};