import React, { useState } from 'react';
import { Button } from '../ui/Button';
import { expenseApi } from '../../api/expenseApi';
import { Expense } from '../../types/Expense';
import { ExpenseForm } from './ExpenseForm';

interface ExpenseActionsProps {
  expense: Expense;
  onUpdate: (updatedExpense: Expense) => void;
  onDelete: (id: number) => void;
}

export const ExpenseActions: React.FC<ExpenseActionsProps> = ({
  expense,
  onUpdate,
  onDelete,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleUpdate = async (data: Partial<Expense>) => {
    if (!expense.id) return;
    
    try {
      setIsLoading(true);
      const updatedExpense = await expenseApi.update(expense.id, data);
      onUpdate(updatedExpense);
      setIsEditing(false);
    } catch (error) {
      console.error('Failed to update expense:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!expense.id) return;
    
    if (!window.confirm('Are you sure you want to delete this expense?')) {
      return;
    }

    try {
      setIsLoading(true);
      await expenseApi.delete(expense.id);
      onDelete(expense.id);
    } catch (error) {
      console.error('Failed to delete expense:', error);
    } finally {
      setIsLoading(false);
    }
  };

  if (isEditing) {
    return (
      <div className="p-4 border rounded-lg bg-white shadow-sm">
        <h3 className="text-lg font-semibold mb-4">Edit Expense</h3>
        <ExpenseForm
          initialData={expense}
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