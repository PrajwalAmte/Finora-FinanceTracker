import React, { useState, useEffect } from 'react';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { Sip } from '../../types/Sip';

interface SipFormProps {
  onSubmit: (data: any) => void;
  onCancel: () => void;
  isLoading?: boolean;
  initialData?: Sip;
  mode?: 'create' | 'edit';
}

export const SipForm: React.FC<SipFormProps> = ({
  onSubmit,
  onCancel,
  isLoading = false,
  initialData,
  mode = 'create',
}) => {
  const [formData, setFormData] = useState({
    name: '',
    schemeCode: '',
    monthlyAmount: '',
    startDate: new Date().toISOString().split('T')[0],
    durationMonths: '',
    currentNav: '',
    totalUnits: '',
  });

  useEffect(() => {
    if (initialData) {
      setFormData({
        name: initialData.name,
        schemeCode: initialData.schemeCode,
        monthlyAmount: initialData.monthlyAmount.toString(),
        startDate: new Date(initialData.startDate).toISOString().split('T')[0],
        durationMonths: initialData.durationMonths.toString(),
        currentNav: initialData.currentNav.toString(),
        totalUnits: initialData.totalUnits.toString(),
      });
    } else {
      // Reset form to defaults when no initial data (for create mode)
      setFormData({
        name: '',
        schemeCode: '',
        monthlyAmount: '',
        startDate: new Date().toISOString().split('T')[0],
        durationMonths: '',
        currentNav: '',
        totalUnits: '',
      });
    }
  }, [initialData]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      ...formData,
      monthlyAmount: parseFloat(formData.monthlyAmount),
      durationMonths: parseInt(formData.durationMonths),
      currentNav: parseFloat(formData.currentNav),
      totalUnits: parseFloat(formData.totalUnits),
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <Input
        label="SIP Name"
        value={formData.name}
        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        label="Scheme Code"
        value={formData.schemeCode}
        onChange={(e) => setFormData({ ...formData, schemeCode: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Monthly Amount"
        value={formData.monthlyAmount}
        onChange={(e) => setFormData({ ...formData, monthlyAmount: e.target.value })}
        required
        fullWidth
      />
      
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
        label="Duration (Months)"
        value={formData.durationMonths}
        onChange={(e) => setFormData({ ...formData, durationMonths: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Current NAV"
        value={formData.currentNav}
        onChange={(e) => setFormData({ ...formData, currentNav: e.target.value })}
        required
        fullWidth
      />
      
      <Input
        type="number"
        label="Total Units"
        value={formData.totalUnits}
        onChange={(e) => setFormData({ ...formData, totalUnits: e.target.value })}
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
          {mode === 'create' ? 'Add SIP' : 'Update SIP'}
        </Button>
      </div>
    </form>
  );
}; 