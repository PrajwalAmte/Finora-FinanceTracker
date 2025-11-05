import React, { createContext, useCallback, useContext, useEffect, useState, useRef } from 'react';

type Toast = { id: number; type: 'error' | 'success' | 'info'; message: string };

const ToastContext = createContext<{
  error: (message: string) => void;
  success: (message: string) => void;
  info: (message: string) => void;
} | null>(null);

type Listener = (toast: Toast) => void;
const listeners: Listener[] = [];

export const toast = {
  error: (message: string) => {
    const toast: Toast = { id: Date.now() + Math.random(), type: 'error', message };
    listeners.forEach((l) => l(toast));
  },
  success: (message: string) => {
    const toast: Toast = { id: Date.now() + Math.random(), type: 'success', message };
    listeners.forEach((l) => l(toast));
  },
  info: (message: string) => {
    const toast: Toast = { id: Date.now() + Math.random(), type: 'info', message };
    listeners.forEach((l) => l(toast));
  },
};

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const timeoutRefs = useRef<Map<number, NodeJS.Timeout>>(new Map());

  useEffect(() => {
    const listener: Listener = (toast) => {
      setToasts((prev) => {
        const newToasts = [...prev, toast];
        
        // Clear any existing timeout for this toast (shouldn't happen, but safety)
        const existingTimeout = timeoutRefs.current.get(toast.id);
        if (existingTimeout) {
          clearTimeout(existingTimeout);
        }
        
        // Set timeout to remove toast after 4 seconds
        const timeout = setTimeout(() => {
          setToasts((prevToasts) => prevToasts.filter((t) => t.id !== toast.id));
          timeoutRefs.current.delete(toast.id);
        }, 4000);
        
        timeoutRefs.current.set(toast.id, timeout);
        
        return newToasts;
      });
    };
    
    listeners.push(listener);
    
    return () => {
      const idx = listeners.indexOf(listener);
      if (idx >= 0) listeners.splice(idx, 1);
      
      // Clean up all timeouts on unmount
      timeoutRefs.current.forEach((timeout) => clearTimeout(timeout));
      timeoutRefs.current.clear();
    };
  }, []);

  const value = {
    error: (m: string) => toast.error(m),
    success: (m: string) => toast.success(m),
    info: (m: string) => toast.info(m),
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed z-50 right-4 bottom-4 space-y-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`px-4 py-3 rounded-md shadow-lg text-white transition-all duration-300 ${
              t.type === 'error' ? 'bg-red-600' : t.type === 'success' ? 'bg-green-600' : 'bg-neutral-800'
            }`}
          >
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}


