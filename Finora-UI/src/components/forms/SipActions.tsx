import React, { useState } from 'react';
import { Button } from '../ui/Button';
import { sipApi } from '../../api/sipApi';
import { Sip } from '../../types/Sip';
import { SipForm } from './SipForm';

interface SipActionsProps {
  sip: Sip;
  onUpdate: (updatedSip: Sip) => void;
  onDelete: (id: number) => void;
}

export const SipActions: React.FC<SipActionsProps> = ({
  sip,
  onUpdate,
  onDelete,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleUpdate = async (data: Partial<Sip>) => {
    if (!sip.id) return;
    
    try {
      setIsLoading(true);
      const updatedSip = await sipApi.update(sip.id, data);
      onUpdate(updatedSip);
      setIsEditing(false);
    } catch (error) {
      console.error('Failed to update SIP:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!sip.id) return;
    
    if (!window.confirm('Are you sure you want to delete this SIP?')) {
      return;
    }

    try {
      setIsLoading(true);
      await sipApi.delete(sip.id);
      onDelete(sip.id);
    } catch (error) {
      console.error('Failed to delete SIP:', error);
    } finally {
      setIsLoading(false);
    }
  };

  if (isEditing) {
    return (
      <div className="p-4 border rounded-lg bg-white shadow-sm">
        <h3 className="text-lg font-semibold mb-4">Edit SIP</h3>
        <SipForm
          initialData={sip}
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