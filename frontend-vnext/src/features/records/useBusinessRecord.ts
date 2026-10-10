import { useMemo, useState } from 'react';
import { lookupBusinessNo } from '../../lib/api';
import { toBusinessRecordView, type BusinessRecord } from './model';

export function useBusinessRecord() {
  const [businessNo, setBusinessNo] = useState('');
  const [record, setRecord] = useState<BusinessRecord | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const result = useMemo(() => record ? toBusinessRecordView(record, businessNo) : null, [record, businessNo]);

  const search = async () => {
    const value = businessNo.trim();
    if (!value) return;
    setBusy(true);
    setError('');
    setRecord(null);
    try {
      setRecord(await lookupBusinessNo(value));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '没有查到该业务编号。');
    } finally {
      setBusy(false);
    }
  };

  return { businessNo, setBusinessNo, result, error, busy, search };
}
