import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { ToastProvider } from './utils/notifications';
import { initJwtToken } from './api/apiClient';
import './index.css';

(async () => {
  await initJwtToken();

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <ToastProvider>
        <App />
      </ToastProvider>
    </StrictMode>
  );
})();
