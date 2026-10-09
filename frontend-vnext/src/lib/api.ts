export type JsonRecord = Record<string, unknown>;

export type PublicBootstrap = {
  siteConfig: JsonRecord | null;
  services: JsonRecord[];
  catalog: JsonRecord | null;
  gameStatus: JsonRecord | null;
};

export type QuotePayload = {
  game: string;
  demand: string;
  currentProgress: string | null;
  screenshotAssetId: string | null;
  platform: string | null;
  expectedTime: string | null;
  contactMethod: string | null;
  customerContact: string | null;
  privacyConsent: boolean;
  firstTouchSource: string | null;
  utmSource: string | null;
  utmMedium: string | null;
  utmCampaign: string | null;
  referrer: string | null;
};

export type QuoteResponse = {
  id: string;
  businessNo: string;
  status: string;
  createdAt: string;
};

type ApiOptions = RequestInit & { signal?: AbortSignal };

async function requestJson<T>(url: string, options: ApiOptions = {}): Promise<T> {
  const response = await fetch(url, {
    credentials: 'same-origin',
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body && !(options.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers || {}),
    },
  });

  if (response.status === 204) return null as T;
  const contentType = response.headers.get('content-type') || '';
  let payload: unknown = null;
  try {
    payload = contentType.includes('application/json') ? await response.json() : await response.text();
  } catch {
    payload = null;
  }

  if (!response.ok) {
    const message = typeof payload === 'object' && payload && 'message' in payload
      ? String((payload as { message?: unknown }).message || '请求失败，请稍后重试')
      : typeof payload === 'string' && payload.trim()
        ? payload.trim()
        : `${response.status} ${response.statusText}`;
    throw new Error(message);
  }
  return payload as T;
}

export async function loadPublicBootstrap(signal?: AbortSignal): Promise<PublicBootstrap> {
  const [siteConfig, services, catalog, gameStatus] = await Promise.allSettled([
    requestJson<JsonRecord>('/api/site-config', { signal }),
    requestJson<JsonRecord[]>('/api/services', { signal }),
    requestJson<JsonRecord>('/api/catalog', { signal }),
    requestJson<JsonRecord>('/api/public/game-status?game=CODMW4', { signal }),
  ]);

  return {
    siteConfig: siteConfig.status === 'fulfilled' ? siteConfig.value : null,
    services: services.status === 'fulfilled' && Array.isArray(services.value) ? services.value : [],
    catalog: catalog.status === 'fulfilled' ? catalog.value : null,
    gameStatus: gameStatus.status === 'fulfilled' ? gameStatus.value : null,
  };
}

export async function lookupBusinessNo(businessNo: string, signal?: AbortSignal): Promise<JsonRecord> {
  return requestJson<JsonRecord>(`/api/records/${encodeURIComponent(businessNo.trim())}`, { signal });
}

export async function loadAccountMe(signal?: AbortSignal): Promise<JsonRecord | null> {
  try {
    return await requestJson<JsonRecord>('/api/account/me', { signal });
  } catch {
    return null;
  }
}

export async function uploadQuoteScreenshot(file: File): Promise<string> {
  const form = new FormData();
  form.append('file', file);
  const out = await requestJson<{ assetId: string }>('/api/uploads/quote', { method: 'POST', body: form });
  if (!out?.assetId) throw new Error('截图上传失败，请重新选择图片');
  return out.assetId;
}

export async function submitQuote(payload: QuotePayload, idempotencyKey: string): Promise<QuoteResponse> {
  return requestJson<QuoteResponse>('/api/quote', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(payload),
  });
}

export function makeIdempotencyKey(): string {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  const bytes = new Uint8Array(16);
  globalThis.crypto?.getRandomValues?.(bytes);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function queryParam(name: string): string | null {
  try {
    const value = new URLSearchParams(window.location.search).get(name);
    return value ? value.slice(0, 64) : null;
  } catch {
    return null;
  }
}

export function firstTouchSource(): string {
  const campaign = queryParam('utm_source');
  if (campaign) return campaign;
  if (!document.referrer) return 'direct';
  try {
    return new URL(document.referrer).hostname.slice(0, 64) || 'referral';
  } catch {
    return 'referral';
  }
}

export function pickText(record: JsonRecord | null | undefined, keys: string[], fallback = ''): string {
  if (!record) return fallback;
  for (const key of keys) {
    const value = record[key];
    if (typeof value === 'string' && value.trim()) return value.trim();
    if (typeof value === 'number') return String(value);
  }
  return fallback;
}

export function pickNumber(record: JsonRecord | null | undefined, key: string, fallback = 0): number {
  const value = record?.[key];
  const number = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(number) ? number : fallback;
}

export function pickBoolean(record: JsonRecord | null | undefined, key: string, fallback = false): boolean {
  const value = record?.[key];
  return typeof value === 'boolean' ? value : fallback;
}
