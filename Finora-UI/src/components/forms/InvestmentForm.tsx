import React, { useState, useEffect, useRef } from 'react';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Button } from '../ui/Button';
import { Investment } from '../../types/Investment';
import { investmentApi } from '../../api/investmentApi';
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
    type: INVESTMENT_TYPES[0] || '',
    quantity: '',
    purchasePrice: '',
    currentPrice: '',
    purchaseDate: new Date().toISOString().split('T')[0],
  });

  // MF fund search state
  const [fundQuery, setFundQuery] = useState('');
  const [fundResults, setFundResults] = useState<{ schemeCode: string; name: string; nav: number }[]>([]);
  const [showFundDropdown, setShowFundDropdown] = useState(false);
  const [fundSearching, setFundSearching] = useState(false);
  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

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
      // Pre-fill fund search display with the fund name for edit mode
      if (initialData.type === 'MUTUAL_FUND') setFundQuery(initialData.name);
    } else {
      setFormData({
        name: '',
        symbol: '',
        type: INVESTMENT_TYPES[0] || '',
        quantity: '',
        purchasePrice: '',
        currentPrice: '',
        purchaseDate: new Date().toISOString().split('T')[0],
      });
      setFundQuery('');
    }
  }, [initialData]);

  // Debounced MF fund search
  useEffect(() => {
    if (formData.type !== 'MUTUAL_FUND') return;
    if (fundQuery.trim().length < 2) { setFundResults([]); return; }
    if (searchTimeout.current) clearTimeout(searchTimeout.current);
    searchTimeout.current = setTimeout(async () => {
      setFundSearching(true);
      const results = await investmentApi.searchMf(fundQuery);
      setFundResults(results);
      setShowFundDropdown(results.length > 0);
      setFundSearching(false);
    }, 350);
    return () => { if (searchTimeout.current) clearTimeout(searchTimeout.current); };
  }, [fundQuery, formData.type]);

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
    if (formData.type === 'MUTUAL_FUND' && !formData.symbol) {
      toast.error('Please select a fund from the search results');
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
      <Select
        label="Investment Type"
        value={formData.type}
        onChange={(e) => {
          setFormData({ ...formData, type: e.target.value, symbol: '', name: '' });
          setFundQuery('');
          setFundResults([]);
        }}
        options={INVESTMENT_TYPES.map(type => ({ value: type, label: type }))}
        required
        fullWidth
      />

      {formData.type === 'MUTUAL_FUND' ? (
        <div className="relative">
          <Input
            label="Search Fund Name"
            value={fundQuery}
            onChange={(e) => {
              setFundQuery(e.target.value);
              if (formData.symbol) setFormData(prev => ({ ...prev, symbol: '', name: '' }));
            }}
            onFocus={() => fundResults.length > 0 && setShowFundDropdown(true)}
            onBlur={() => setTimeout(() => setShowFundDropdown(false), 150)}
            placeholder="Type fund name to search…"
            fullWidth
          />
          {fundSearching && <p className="text-xs text-neutral-400 mt-1">Searching…</p>}
          {formData.symbol && (
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
              Selected — scheme code:{' '}
              <span className="font-mono text-blue-600 dark:text-blue-400">{formData.symbol}</span>
            </p>
          )}
          {showFundDropdown && (
            <ul className="absolute z-50 w-full mt-1 bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-lg max-h-56 overflow-y-auto text-sm">
              {fundResults.map(f => (
                <li
                  key={f.schemeCode}
                  className="px-3 py-2 cursor-pointer hover:bg-neutral-100 dark:hover:bg-neutral-700"
                  onMouseDown={() => {
                    setFormData(prev => ({
                      ...prev,
                      name: f.name,
                      symbol: f.schemeCode,
                      currentPrice: String(f.nav),
                    }));
                    setFundQuery(f.name);
                    setShowFundDropdown(false);
                  }}
                >
                  <span className="block font-medium text-neutral-800 dark:text-neutral-100 leading-tight">{f.name}</span>
                  <span className="text-neutral-400 text-xs">Code: {f.schemeCode} · NAV: ₹{Number(f.nav).toFixed(2)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      ) : (
        <>
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
        </>
      )}

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