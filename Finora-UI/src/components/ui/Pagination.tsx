import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Button';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  totalItems?: number;
  itemsPerPage?: number;
  className?: string;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  totalItems,
  itemsPerPage,
  className = '',
}) => {
  if (totalPages <= 1) return null;

  const pages = [];
  const maxVisible = 5;
  let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2));
  const endPage = Math.min(totalPages, startPage + maxVisible - 1);

  if (endPage - startPage + 1 < maxVisible) {
    startPage = Math.max(1, endPage - maxVisible + 1);
  }

  for (let i = startPage; i <= endPage; i++) {
    pages.push(i);
  }

  const showingFrom = totalItems ? (currentPage - 1) * (itemsPerPage || 10) + 1 : 0;
  const showingTo = totalItems ? Math.min(currentPage * (itemsPerPage || 10), totalItems) : 0;

  return (
    <div className={`flex flex-col sm:flex-row items-center justify-between gap-4 py-3 ${className}`}>
      {totalItems && (
        <p className="text-sm text-neutral-500 dark:text-neutral-400">
          Showing {showingFrom} to {showingTo} of {totalItems}
        </p>
      )}

      <div className="flex items-center gap-1">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          iconLeft={<ChevronLeft size={16} />}
        >
          Prev
        </Button>

        {startPage > 1 && (
          <>
            <button
              onClick={() => onPageChange(1)}
              className="px-3 py-1.5 text-sm rounded-md hover:bg-neutral-100 dark:hover:bg-neutral-700 text-neutral-700 dark:text-neutral-300"
            >
              1
            </button>
            {startPage > 2 && <span className="px-2 text-neutral-400">...</span>}
          </>
        )}

        {pages.map((page) => (
          <button
            key={page}
            onClick={() => onPageChange(page)}
            className={`px-3 py-1.5 text-sm rounded-md transition-colors ${
              page === currentPage
                ? 'bg-primary-600 text-white'
                : 'hover:bg-neutral-100 dark:hover:bg-neutral-700 text-neutral-700 dark:text-neutral-300'
            }`}
          >
            {page}
          </button>
        ))}

        {endPage < totalPages && (
          <>
            {endPage < totalPages - 1 && <span className="px-2 text-neutral-400">...</span>}
            <button
              onClick={() => onPageChange(totalPages)}
              className="px-3 py-1.5 text-sm rounded-md hover:bg-neutral-100 dark:hover:bg-neutral-700 text-neutral-700 dark:text-neutral-300"
            >
              {totalPages}
            </button>
          </>
        )}

        <Button
          variant="ghost"
          size="sm"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          iconRight={<ChevronRight size={16} />}
        >
          Next
        </Button>
      </div>
    </div>
  );
};
