import React, { useState } from 'react';
import { Button } from '../ui/Button';
import { loanApi } from '../../api/loanApi';
import { Loan } from '../../types/Loan';
import { LoanForm } from './LoanForm';

interface LoanActionsProps {
  loan: Loan;
  onUpdate: (updatedLoan: Loan) => void;
  onDelete: (id: number) => void;
}

export const LoanActions: React.FC<LoanActionsProps> = ({
  loan,
  onUpdate,
  onDelete,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleUpdate = async (data: Partial<Loan>) => {
    if (!loan.id) return;
    
    try {
      setIsLoading(true);
      const updatedLoan = await loanApi.update(loan.id, data);
      onUpdate(updatedLoan);
      setIsEditing(false);
    } catch (error) {
      console.error('Failed to update loan:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!loan.id) return;
    
    if (!window.confirm('Are you sure you want to delete this loan?')) {
      return;
    }

    try {
      setIsLoading(true);
      await loanApi.delete(loan.id);
      onDelete(loan.id);
    } catch (error) {
      console.error('Failed to delete loan:', error);
    } finally {
      setIsLoading(false);
    }
  };

  if (isEditing) {
    return (
      <div className="p-4 border rounded-lg bg-white shadow-sm">
        <h3 className="text-lg font-semibold mb-4">Edit Loan</h3>
        <LoanForm
          initialData={loan}
          mode="edit"
          onSubmit={handleUpdate}
          onCancel={() => setIsEditing(false)}
          isLoading={isLoading}
        />
      </div>
    );
  }

  return (
    <div className="flex space-x-2">
      <Button
        variant="outline"
        onClick={() => setIsEditing(true)}
        disabled={isLoading}
      >
        Edit
      </Button>
      <Button
        variant="danger"
        onClick={handleDelete}
        isLoading={isLoading}
      >
        Delete
      </Button>
    </div>
  );
}; 