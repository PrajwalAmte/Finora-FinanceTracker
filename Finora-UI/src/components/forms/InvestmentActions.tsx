import React, { useState } from 'react';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Dialog } from '../ui/Dialog';
import { useInvestmentApi } from '../../utils/data-context';
import { Investment } from '../../types/Investment';
import { InvestmentForm } from './InvestmentForm';
import { toast } from '../../utils/notifications';

type DialogMode = 'edit' | 'add' | 'sell';

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
  const investmentApi = useInvestmentApi();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<DialogMode>('edit');
  const [isLoading, setIsLoading] = useState(false);

  const [tradeQty, setTradeQty] = useState('');
  const [tradePrice, setTradePrice] = useState('');

  const openDialog = (mode: DialogMode) => {
    setDialogMode(mode);
    setTradeQty('');
    setTradePrice(String(investment.currentPrice ?? ''));
    setDialogOpen(true);
  };

  const closeDialog = () => {
    if (!isLoading) setDialogOpen(false);
  };

  const handleEdit = async (data: Partial<Investment>) => {
    if (!investment.id) return;
    try {
      setIsLoading(true);
      const updated = await investmentApi.update(investment.id, data);
      onUpdate(updated);
      setDialogOpen(false);
      toast.success('Investment updated');
    } catch {
      toast.error('Failed to update investment');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddUnits = async (e: React.FormEvent) => {
    e.preventDefault();
    const qty   = parseFloat(tradeQty);
    const price = parseFloat(tradePrice);
    if (!qty || qty <= 0 || !price || price <= 0) {
      toast.error('Quantity and price must be positive numbers');
      return;
    }
    if (!investment.id) return;
    try {
      setIsLoading(true);
      const updated = await investmentApi.addUnits(investment.id, qty, price);
      onUpdate(updated);
      setDialogOpen(false);
      toast.success(
        `Added ${qty} units. New avg price: ₹${Number(updated.purchasePrice).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
      );
    } catch {
      toast.error('Failed to add units');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSellUnits = async (e: React.FormEvent) => {
    e.preventDefault();
    const qty   = parseFloat(tradeQty);
    const price = parseFloat(tradePrice);
    if (!qty || qty <= 0 || !price || price <= 0) {
      toast.error('Quantity and price must be positive numbers');
      return;
    }
    if (!investment.id) return;
    try {
      setIsLoading(true);
      const result = await investmentApi.sellUnits(investment.id, qty, price);
      if (result === null) {
        onDelete(investment.id);
        setDialogOpen(false);
        toast.success('All units sold — investment removed');
      } else {
        onUpdate(result);
        setDialogOpen(false);
        toast.success(
          `Sold ${qty} units. Remaining: ${Number(result.quantity).toLocaleString('en-IN', { maximumFractionDigits: 4 })} units`
        );
      }
    } catch {
      toast.error('Failed to sell units');
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
    } catch {
      toast.error('Failed to delete investment');
    } finally {
      setIsLoading(false);
    }
  };

  const dialogTitle =
    dialogMode === 'edit' ? 'Edit Investment' :
    dialogMode === 'add'  ? `Add Units — ${investment.name}` :
                            `Sell Units — ${investment.name}`;

  const TradeForm = ({ mode }: { mode: 'add' | 'sell' }) => {
    const isSell = mode === 'sell';
    const realizedPnl =
      isSell && tradeQty && tradePrice
        ? (parseFloat(tradePrice) - investment.purchasePrice) * parseFloat(tradeQty)
        : null;

    return (
      <form onSubmit={isSell ? handleSellUnits : handleAddUnits} className="space-y-4">
        {/* Current holdings summary */}
        <div className="rounded-md bg-neutral-50 dark:bg-neutral-700/40 border border-neutral-200 dark:border-neutral-600 px-4 py-3 text-sm space-y-1">
          <div className="flex justify-between">
            <span className="text-neutral-500 dark:text-neutral-400">Current holdings</span>
            <span className="font-medium text-neutral-800 dark:text-neutral-100">
              {Number(investment.quantity).toLocaleString('en-IN', { maximumFractionDigits: 4 })} units
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-neutral-500 dark:text-neutral-400">Avg buy price</span>
            <span className="font-medium text-neutral-800 dark:text-neutral-100">
              ₹{Number(investment.purchasePrice).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-neutral-500 dark:text-neutral-400">Current price</span>
            <span className="font-medium text-neutral-800 dark:text-neutral-100">
              ₹{Number(investment.currentPrice).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
          </div>
        </div>

        <Input
          type="number"
          label="Quantity"
          value={tradeQty}
          onChange={e => setTradeQty(e.target.value)}
          placeholder={isSell ? `Max ${Number(investment.quantity).toLocaleString('en-IN', { maximumFractionDigits: 4 })}` : 'Units to add'}
          required
          fullWidth
          step="any"
          min="0.000001"
        />

        <Input
          type="number"
          label={isSell ? 'Sell Price (per unit)' : 'Buy Price (per unit)'}
          value={tradePrice}
          onChange={e => setTradePrice(e.target.value)}
          placeholder="Price per unit"
          required
          fullWidth
          step="any"
          min="0.000001"
        />

        {/* Preview */}
        {tradeQty && tradePrice && parseFloat(tradeQty) > 0 && parseFloat(tradePrice) > 0 && (
          <div className="rounded-md bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 px-4 py-3 text-sm space-y-1">
            {isSell ? (
              <>
                <div className="flex justify-between">
                  <span className="text-neutral-500 dark:text-neutral-400">Sell value</span>
                  <span className="font-medium text-neutral-800 dark:text-neutral-100">
                    ₹{(parseFloat(tradeQty) * parseFloat(tradePrice)).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </span>
                </div>
                {realizedPnl !== null && (
                  <div className="flex justify-between">
                    <span className="text-neutral-500 dark:text-neutral-400">Realized P&amp;L</span>
                    <span className={`font-semibold ${realizedPnl >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`}>
                      {realizedPnl >= 0 ? '+' : ''}₹{realizedPnl.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-neutral-500 dark:text-neutral-400">Remaining units</span>
                  <span className="font-medium text-neutral-800 dark:text-neutral-100">
                    {parseFloat(tradeQty) >= investment.quantity
                      ? <span className="text-orange-600 dark:text-orange-400">0 (removed)</span>
                      : (investment.quantity - parseFloat(tradeQty)).toLocaleString('en-IN', { maximumFractionDigits: 4 })}
                  </span>
                </div>
              </>
            ) : (
              <>
                <div className="flex justify-between">
                  <span className="text-neutral-500 dark:text-neutral-400">New total units</span>
                  <span className="font-medium text-neutral-800 dark:text-neutral-100">
                    {(investment.quantity + parseFloat(tradeQty)).toLocaleString('en-IN', { maximumFractionDigits: 4 })}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500 dark:text-neutral-400">New avg price</span>
                  <span className="font-medium text-neutral-800 dark:text-neutral-100">
                    ₹{((investment.quantity * investment.purchasePrice + parseFloat(tradeQty) * parseFloat(tradePrice)) /
                        (investment.quantity + parseFloat(tradeQty))).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </span>
                </div>
              </>
            )}
          </div>
        )}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={closeDialog} disabled={isLoading}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant={isSell ? 'danger' : 'primary'}
            isLoading={isLoading}
          >
            {isSell ? 'Confirm Sale' : 'Add Units'}
          </Button>
        </div>
      </form>
    );
  };

  return (
    <>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" onClick={() => openDialog('edit')} disabled={isLoading}>
          Edit
        </Button>
        <Button variant="danger" size="sm" onClick={handleDelete} isLoading={isLoading}>
          Delete
        </Button>
      </div>

      <Dialog isOpen={dialogOpen} onClose={closeDialog} title={dialogTitle}>
        {/* Mode tab switcher */}
        <div className="flex gap-1 mb-5 bg-neutral-100 dark:bg-neutral-700 rounded-lg p-1">
          {(['edit', 'add', 'sell'] as DialogMode[]).map(m => (
            <button
              key={m}
              type="button"
              onClick={() => { setDialogMode(m); setTradeQty(''); setTradePrice(String(investment.currentPrice ?? '')); }}
              className={`flex-1 py-1.5 text-sm font-medium rounded-md transition-colors ${
                dialogMode === m
                  ? 'bg-white dark:bg-neutral-600 text-neutral-900 dark:text-white shadow-sm'
                  : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200'
              }`}
            >
              {m === 'edit' ? 'Edit Details' : m === 'add' ? 'Buy / Add' : 'Sell'}
            </button>
          ))}
        </div>

        {dialogMode === 'edit' && (
          <InvestmentForm
            initialData={investment}
            mode="edit"
            onSubmit={handleEdit}
            onCancel={closeDialog}
            isLoading={isLoading}
          />
        )}
        {dialogMode === 'add'  && <TradeForm mode="add"  />}
        {dialogMode === 'sell' && <TradeForm mode="sell" />}
      </Dialog>
    </>
  );
};