import React, { useState, useEffect } from 'react';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Button } from '../ui/Button';
import { Expense } from '../../types/Expense';
import { toast } from '../../utils/notifications';
import { PAYMENT_METHODS, EXPENSE_CATEGORIES } from '../../constants';

interface ExpenseFormData {
  description: string;
  amount: string;
  date: string;
  category: string;
  paymentMethod: string;
}

interface ExpenseFormProps {
  onSubmit: (data: ExpenseFormData) => void;
  onCancel: () => void;
  isLoading?: boolean;
  initialData?: Expense;
  mode?: 'create' | 'edit';
}

export const ExpenseForm: React.FC<ExpenseFormProps> = ({
  onSubmit,
  onCancel,
  isLoading = false,
  initialData,
  mode = 'create',
}) => {
  const [formData, setFormData] = useState({
    description: '',
    amount: '',
    date: new Date().toISOString().split('T')[0],
    category: EXPENSE_CATEGORIES[0] || '',
    paymentMethod: PAYMENT_METHODS[0] || '',
  });

  useEffect(() => {
    if (initialData) {
      setFormData({
        description: initialData.description,
        amount: initialData.amount.toString(),
        date: new Date(initialData.date).toISOString().split('T')[0],
        category: initialData.category,
        paymentMethod: initialData.paymentMethod,
      });
    } else {
      setFormData({
        description: '',
        amount: '',
        date: new Date().toISOString().split('T')[0],
        category: EXPENSE_CATEGORIES[0] || '',
        paymentMethod: PAYMENT_METHODS[0] || '',
      });
    }
  }, [initialData]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const amountNum = parseFloat(formData.amount);
    if (isNaN(amountNum) || amountNum <= 0) {
      toast.error('Amount must be a positive number');
      return;
    }
    if (!formData.category || !formData.paymentMethod) {
      toast.error('Please select category and payment method');
      return;
    }
    onSubmit({
      ...formData,
      amount: amountNum,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <Input
        label="Description"
        value={formData.description}
        onChange={(e) => setFormData({ ...formData, description: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Amount"
        value={formData.amount}
        onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="date"
        label="Date"
        value={formData.date}
        onChange={(e) => setFormData({ ...formData, date: e.target.value })}
        required
        fullWidth
      />
      
      <Select
        label="Category"
        value={formData.category}
        onChange={(e) => setFormData({ ...formData, category: e.target.value })}
        options={EXPENSE_CATEGORIES.map(cat => ({ value: cat, label: cat }))}
        required
        fullWidth
      />
      
      <Select
        label="Payment Method"
        value={formData.paymentMethod}
        onChange={(e) => setFormData({ ...formData, paymentMethod: e.target.value })}
        options={PAYMENT_METHODS.map(method => ({ value: method, label: method }))}
        required
        fullWidth
      />
      
      <div className="flex justify-end space-x-2 pt-4">
        <Button
          type="button"
          variant="outline"
          onClick={onCancel}
          disabled={isLoading}
        >
          Cancel
        </Button>
        <Button
          type="submit"
          isLoading={isLoading}
        >
          {mode === 'create' ? 'Add Expense' : 'Update Expense'}
        </Button>
      </div>
    </form>
  );
};