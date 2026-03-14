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
  // Default next installment date = one month from today
  const defaultNextInstallment = () => {
    const d = new Date();
    d.setMonth(d.getMonth() + 1);
    return d.toISOString().split('T')[0];
  };

  const [formData, setFormData] = useState({
    name: '',
    schemeCode: '',
    monthlyAmount: '',
    startDate: defaultNextInstallment(),
    currentNav: '',
    totalUnits: '',
    totalInvested: '',
  });

  useEffect(() => {
    if (initialData) {
      setFormData({
        name: initialData.name,
        schemeCode: initialData.schemeCode ?? '',
        monthlyAmount: initialData.monthlyAmount.toString(),
        startDate: initialData.startDate
          ? new Date(initialData.startDate).toISOString().split('T')[0]
          : defaultNextInstallment(),
        currentNav: initialData.currentNav?.toString() ?? '',
        totalUnits: initialData.totalUnits?.toString() ?? '',
        totalInvested: initialData.totalInvested?.toString() ?? '',
      });
    } else {
      setFormData({
        name: '',
        schemeCode: '',
        monthlyAmount: '',
        startDate: defaultNextInstallment(),
        currentNav: '',
        totalUnits: '',
        totalInvested: '',
      });
    }
  }, [initialData]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      ...formData,
      monthlyAmount: parseFloat(formData.monthlyAmount),
      durationMonths: 120,          // always default; not shown in form
      currentNav: formData.currentNav ? parseFloat(formData.currentNav) : undefined,
      totalUnits: formData.totalUnits ? parseFloat(formData.totalUnits) : undefined,
      totalInvested: formData.totalInvested ? parseFloat(formData.totalInvested) : undefined,
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
        label="Monthly Amount (₹)"
        value={formData.monthlyAmount}
        onChange={(e) => setFormData({ ...formData, monthlyAmount: e.target.value })}
        required
        fullWidth
      />

      <Input
        type="date"
        label="Next Installment Date"
        value={formData.startDate}
        onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
        required
        fullWidth
      />

      <Input
        type="number"
        label="Total Invested So Far (₹)  — optional, for P&L tracking"
        placeholder="Leave blank to start fresh"
        value={formData.totalInvested}
        onChange={(e) => setFormData({ ...formData, totalInvested: e.target.value })}
        fullWidth
      />

      <Input
        type="number"
        label="Current NAV — optional"
        placeholder="Auto-fetched from AMFI"
        value={formData.currentNav}
        onChange={(e) => setFormData({ ...formData, currentNav: e.target.value })}
        fullWidth
      />

      <Input
        type="number"
        label="Total Units — optional"
        placeholder="Auto-fetched from AMFI"
        value={formData.totalUnits}
        onChange={(e) => setFormData({ ...formData, totalUnits: e.target.value })}
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