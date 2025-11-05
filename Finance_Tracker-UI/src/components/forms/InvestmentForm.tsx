import React, { useState, useEffect } from 'react';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Button } from '../ui/Button';
import { Investment } from '../../types/Investment';
import { toast } from '../../utils/notifications';
import { INVESTMENT_TYPES } from '../../constants';

interface InvestmentFormProps {
  onSubmit: (data: any) => void;
  onCancel: () => void;
  isLoading?: boolean;
  initialData?: Investment;
  mode?: 'create' | 'edit';
}

// constants moved to shared constants.ts

export const InvestmentForm: React.FC<InvestmentFormProps> = ({
  onSubmit,
  onCancel,
  isLoading = false,
  initialData,
  mode = 'create',
}) => {
  const [formData, setFormData] = useState({
    name: '',
    symbol: '',
    type: INVESTMENT_TYPES[0] || '', // Default to first investment type
    quantity: '',
    purchasePrice: '',
    currentPrice: '',
    purchaseDate: new Date().toISOString().split('T')[0],
  });

  useEffect(() => {
    if (initialData) {
      setFormData({
        name: initialData.name,
        symbol: initialData.symbol,
        type: initialData.type,
        quantity: initialData.quantity.toString(),
        purchasePrice: initialData.purchasePrice.toString(),
        currentPrice: initialData.currentPrice.toString(),
        purchaseDate: new Date(initialData.purchaseDate).toISOString().split('T')[0],
      });
    } else {
      // Reset form to defaults when no initial data (for create mode)
      setFormData({
        name: '',
        symbol: '',
        type: INVESTMENT_TYPES[0] || '',
        quantity: '',
        purchasePrice: '',
        currentPrice: '',
        purchaseDate: new Date().toISOString().split('T')[0],
      });
    }
  }, [initialData]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const quantity = parseFloat(formData.quantity);
    const purchasePrice = parseFloat(formData.purchasePrice);
    const currentPrice = parseFloat(formData.currentPrice);
    if ([quantity, purchasePrice, currentPrice].some((n) => isNaN(n) || n <= 0)) {
      toast.error('Quantity and prices must be positive numbers');
      return;
    }
    if (!formData.type) {
      toast.error('Please select investment type');
      return;
    }
    onSubmit({
      ...formData,
      quantity,
      purchasePrice,
      currentPrice,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <Input
        label="Investment Name"
        value={formData.name}
        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        label="Symbol"
        value={formData.symbol}
        onChange={(e) => setFormData({ ...formData, symbol: e.target.value })}
        required
        fullWidth
      />
      
      <Select
        label="Investment Type"
        value={formData.type}
        onChange={(e) => setFormData({ ...formData, type: e.target.value })}
        options={INVESTMENT_TYPES.map(type => ({ value: type, label: type }))}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Quantity"
        value={formData.quantity}
        onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Purchase Price"
        value={formData.purchasePrice}
        onChange={(e) => setFormData({ ...formData, purchasePrice: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Current Price"
        value={formData.currentPrice}
        onChange={(e) => setFormData({ ...formData, currentPrice: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="date"
        label="Purchase Date"
        value={formData.purchaseDate}
        onChange={(e) => setFormData({ ...formData, purchaseDate: e.target.value })}
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
          {mode === 'create' ? 'Add Investment' : 'Update Investment'}
        </Button>
      </div>
    </form>
  );
}; 