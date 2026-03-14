import React, { useState } from 'react';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';
import { investmentApi } from '../../api/investmentApi';
import { Investment } from '../../types/Investment';
import { InvestmentForm } from './InvestmentForm';

interface InvestmentActionsProps {
  investment: Investment;
  onUpdate: (updatedInvestment: Investment) => void;
  onDelete: (id: number) => void;
}

export const InvestmentActions: React.FC<InvestmentActionsProps> = ({
  investment,
  onUpdate,
  onDelete,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleUpdate = async (data: Partial<Investment>) => {
    if (!investment.id) return;
    try {
      setIsLoading(true);
      const updatedInvestment = await investmentApi.update(investment.id, data);
      onUpdate(updatedInvestment);
      setIsEditing(false);
    } catch (error) {
      console.error('Failed to update investment:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!investment.id) return;
    if (!window.confirm('Are you sure you want to delete this investment?')) return;
    try {
      setIsLoading(true);
      await investmentApi.delete(investment.id);
      onDelete(investment.id);
    } catch (error) {
      console.error('Failed to delete investment:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <div className="flex space-x-2">
        <Button variant="outline" onClick={() => setIsEditing(true)} disabled={isLoading}>
          Edit
        </Button>
        <Button variant="danger" onClick={handleDelete} isLoading={isLoading}>
          Delete
        </Button>
      </div>

      <Dialog isOpen={isEditing} onClose={() => setIsEditing(false)} title="Edit Investment">
        <InvestmentForm
          initialData={investment}
          mode="edit"
          onSubmit={handleUpdate}
          onCancel={() => setIsEditing(false)}
          isLoading={isLoading}
        />
      </Dialog>
    </>
  );
};