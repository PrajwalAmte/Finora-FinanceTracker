import React, { useState, useEffect } from 'react';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Button } from '../ui/Button';
import { Loan } from '../../types/Loan';
import { toast } from '../../utils/notifications';
import { INTEREST_TYPES, COMPOUNDING_FREQUENCIES } from '../../constants';

interface LoanFormProps {
  onSubmit: (data: any) => void;
  onCancel: () => void;
  isLoading?: boolean;
  initialData?: Loan;
  mode?: 'create' | 'edit';
}

// constants moved to shared constants.ts

export const LoanForm: React.FC<LoanFormProps> = ({
  onSubmit,
  onCancel,
  isLoading = false,
  initialData,
  mode = 'create',
}) => {
  const [formData, setFormData] = useState({
    name: '',
    principalAmount: '',
    interestRate: '',
    interestType: 'SIMPLE',
    compoundingFrequency: 'MONTHLY',
    startDate: new Date().toISOString().split('T')[0],
    tenureMonths: '',
    currentBalance: '',
  });

  useEffect(() => {
    if (initialData) {
      setFormData({
        name: initialData.name,
        principalAmount: initialData.principalAmount.toString(),
        interestRate: initialData.interestRate.toString(),
        interestType: initialData.interestType,
        compoundingFrequency: initialData.compoundingFrequency || 'MONTHLY',
        startDate: new Date(initialData.startDate).toISOString().split('T')[0],
        tenureMonths: initialData.tenureMonths.toString(),
        currentBalance: initialData.currentBalance.toString(),
      });
    } else {
      // Reset form to defaults when no initial data (for create mode)
      setFormData({
        name: '',
        principalAmount: '',
        interestRate: '',
        interestType: 'SIMPLE',
        compoundingFrequency: 'MONTHLY',
        startDate: new Date().toISOString().split('T')[0],
        tenureMonths: '',
        currentBalance: '',
      });
    }
  }, [initialData]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const principalAmount = parseFloat(formData.principalAmount);
    const interestRate = parseFloat(formData.interestRate);
    const tenureMonths = parseInt(formData.tenureMonths);
    const currentBalance = parseFloat(formData.currentBalance);
    if ([principalAmount, interestRate, currentBalance].some((n) => isNaN(n) || n <= 0) || isNaN(tenureMonths) || tenureMonths <= 0) {
      toast.error('Please enter valid positive numbers');
      return;
    }
    if (!formData.interestType) {
      toast.error('Please select interest type');
      return;
    }
    onSubmit({
      ...formData,
      principalAmount,
      interestRate,
      tenureMonths,
      currentBalance,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <Input
        label="Loan Name"
        value={formData.name}
        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Principal Amount"
        value={formData.principalAmount}
        onChange={(e) => setFormData({ ...formData, principalAmount: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Interest Rate (%)"
        value={formData.interestRate}
        onChange={(e) => setFormData({ ...formData, interestRate: e.target.value })}
        required
        fullWidth
      />
      
      <Select
        label="Interest Type"
        value={formData.interestType}
        onChange={(e) => setFormData({ ...formData, interestType: e.target.value })}
        options={INTEREST_TYPES}
        required
        fullWidth
      />
      
      {formData.interestType === 'COMPOUND' && (
        <Select
          label="Compounding Frequency"
          value={formData.compoundingFrequency}
          onChange={(e) => setFormData({ ...formData, compoundingFrequency: e.target.value })}
          options={COMPOUNDING_FREQUENCIES}
          required
          fullWidth
        />
      )}
      
      <Input
        type="date"
        label="Start Date"
        value={formData.startDate}
        onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Tenure (Months)"
        value={formData.tenureMonths}
        onChange={(e) => setFormData({ ...formData, tenureMonths: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Current Balance"
        value={formData.currentBalance}
        onChange={(e) => setFormData({ ...formData, currentBalance: e.target.value })}
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
          {mode === 'create' ? 'Add Loan' : 'Update Loan'}
        </Button>
      </div>
    </form>
  );
}; 