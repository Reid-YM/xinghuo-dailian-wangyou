import { pickNumber, pickText, type JsonRecord } from '../../lib/api';

export type BusinessRecord = JsonRecord;

export type BusinessRecordView = {
  stage: string;
  title: string;
  status: string;
  progress: string;
  updatedAt: string;
  customerNote: string;
};

export function formatRecordDate(value: string) {
  if (!value) return '以后台为准';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false });
}

export function toBusinessRecordView(record: BusinessRecord, fallbackBusinessNo: string): BusinessRecordView {
  return {
    stage: pickText(record, ['stage'], '业务记录'),
    title: pickText(record, ['title'], pickText(record, ['businessNo'], fallbackBusinessNo)),
    status: pickText(record, ['status'], '处理中'),
    progress: pickText(record, ['progressText'], `${pickNumber(record, 'progressPercent', 0)}%`),
    updatedAt: formatRecordDate(pickText(record, ['updatedAt'])),
    customerNote: pickText(record, ['customerNote']),
  };
}
