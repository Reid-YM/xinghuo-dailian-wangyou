export type JsonRecord = Record<string, unknown>;

export type PublicBootstrap = {
  siteConfig: JsonRecord | null;
  services: JsonRecord[];
  catalog: JsonRecord | null;
  gameStatus: JsonRecord | null;
};

async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, {
    signal,
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

export async function loadPublicBootstrap(signal?: AbortSignal): Promise<PublicBootstrap> {
  const [siteConfig, services, catalog, gameStatus] = await Promise.allSettled([
    getJson<JsonRecord>('/api/site-config', signal),
    getJson<JsonRecord[]>('/api/services', signal),
    getJson<JsonRecord>('/api/catalog', signal),
    getJson<JsonRecord>('/api/public/game-status?game=CODMW4', signal),
  ]);

  return {
    siteConfig: siteConfig.status === 'fulfilled' ? siteConfig.value : null,
    services: services.status === 'fulfilled' && Array.isArray(services.value) ? services.value : [],
    catalog: catalog.status === 'fulfilled' ? catalog.value : null,
    gameStatus: gameStatus.status === 'fulfilled' ? gameStatus.value : null,
  };
}

export async function lookupBusinessNo(businessNo: string, signal?: AbortSignal): Promise<JsonRecord> {
  return getJson<JsonRecord>(`/api/records/${encodeURIComponent(businessNo.trim())}`, signal);
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
