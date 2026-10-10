import { ArrowRight, Clock3, PackageCheck } from 'lucide-react';
import { Badge, Card } from './ui';
import { pickNumber, pickText, type JsonRecord } from '../lib/api';

export function ServiceCard({ service }: { service: JsonRecord }) {
  const slug = pickText(service, ['slug']);
  const image = pickText(service, ['coverImage']);
  const href = slug ? `/service/${encodeURIComponent(slug)}` : '/quote';

  return (
    <Card className="v-service-card">
      {image && <a className="v-service-cover" href={href}><img src={image} alt={pickText(service, ['name'], '服务封面')} loading="lazy" /></a>}
      <div className="v-service-card-top"><Badge>{pickText(service, ['gameCode', 'category'], '游戏服务')}</Badge><Clock3 size={18} /></div>
      <h3>{pickText(service, ['name'], '服务项目')}</h3>
      <p>{pickText(service, ['shortDesc'], '具体内容与价格以当前服务配置为准。')}</p>
      <div className="v-service-price"><strong>{servicePrice(service)}</strong><span>{pickText(service, ['durationText'], '按进度确认')}</span></div>
      <a className="v-service-link" href={href}>查看详情 <ArrowRight size={16} /></a>
    </Card>
  );
}

export function EmptyState({ title, text }: { title: string; text: string }) {
  return <div className="v-empty"><PackageCheck size={30} /><strong>{title}</strong><p>{text}</p></div>;
}

export function PageIntro({ eyebrow, title, text }: { eyebrow: string; title: string; text: string }) {
  return <div className="v-page-intro"><span className="v-eyebrow">{eyebrow}</span><h1>{title}</h1><p>{text}</p></div>;
}

export function serviceLabel(service: JsonRecord, index: number) {
  return pickText(service, ['name', 'title', 'serviceName'], `服务方案 ${index + 1}`);
}

export function serviceKey(service: JsonRecord, index: number) {
  return pickText(service, ['id', 'slug', 'name'], String(index));
}

export function servicePrice(service: JsonRecord) {
  const mode = pickText(service, ['priceMode']).toLowerCase();
  const fixed = Number(service.priceFixed);
  const min = Number(service.priceMin);
  const max = Number(service.priceMax);
  if (mode === 'fixed' && Number.isFinite(fixed)) return `¥${fixed}`;
  if (mode === 'range' && Number.isFinite(min) && Number.isFinite(max)) return `¥${min}–${max}`;
  if (Number.isFinite(min) && min > 0) return `参考 ¥${min} 起`;
  return '人工报价';
}

export function formatDate(value: string) {
  if (!value) return '以后台为准';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false });
}
