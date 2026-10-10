import { pickText, type JsonRecord } from '../../lib/api';

export type ServiceRecord = JsonRecord;

export function serviceLabel(service: ServiceRecord, index: number) {
  return pickText(service, ['name', 'title', 'serviceName'], `服务方案 ${index + 1}`);
}

export function serviceKey(service: ServiceRecord, index: number) {
  return pickText(service, ['id', 'slug', 'name'], String(index));
}

export function servicePrice(service: ServiceRecord) {
  const mode = pickText(service, ['priceMode']).toLowerCase();
  const fixed = Number(service.priceFixed);
  const min = Number(service.priceMin);
  const max = Number(service.priceMax);
  if (mode === 'fixed' && Number.isFinite(fixed)) return `¥${fixed}`;
  if (mode === 'range' && Number.isFinite(min) && Number.isFinite(max)) return `¥${min}–${max}`;
  if (Number.isFinite(min) && min > 0) return `参考 ¥${min} 起`;
  return '人工报价';
}

export function serviceName(service: ServiceRecord) {
  return pickText(service, ['name'], '服务项目');
}

export function serviceSlug(service: ServiceRecord) {
  return pickText(service, ['slug']);
}

export function serviceCoverImage(service: ServiceRecord) {
  return pickText(service, ['coverImage']);
}

export function serviceCategory(service: ServiceRecord) {
  return pickText(service, ['gameCode', 'category'], '游戏服务');
}

export function serviceDescription(service: ServiceRecord) {
  return pickText(service, ['shortDesc'], '具体内容与价格以当前服务配置为准。');
}

export function serviceDuration(service: ServiceRecord) {
  return pickText(service, ['durationText'], '按进度确认');
}

export function serviceGame(service: ServiceRecord) {
  return pickText(service, ['gameCode'], 'CODMW4');
}

export function filterServices(services: ServiceRecord[], query: string) {
  const value = query.trim().toLowerCase();
  if (!value) return services;
  return services.filter(service => [service.name, service.shortDesc, service.gameCode, service.category]
    .some(item => String(item ?? '').toLowerCase().includes(value)));
}
