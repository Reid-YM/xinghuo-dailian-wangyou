import { useMemo, useState } from 'react';
import { ArrowRight, Search } from 'lucide-react';
import { Badge, Button, Card, Skeleton } from '../components/ui';
import { EmptyState, PageIntro, ServiceCard, serviceKey } from '../components/public-content';
import type { JsonRecord } from '../lib/api';
import type { PageKey } from '../app/navigation';

export function ServicesPage({ loading, services, onNavigate }: { loading: boolean; services: JsonRecord[]; onNavigate: (page: PageKey) => void }) {
  const [query, setQuery] = useState('');
  const filtered = useMemo(() => {
    const value = query.trim().toLowerCase();
    if (!value) return services;
    return services.filter(service => [service.name, service.shortDesc, service.gameCode, service.category].some(item => String(item ?? '').toLowerCase().includes(value)));
  }, [query, services]);

  return <section className="v-page v-shell">
    <PageIntro eyebrow="SERVICE BROWSER" title="服务中心" text="按游戏、目标与当前进度选择服务。页面价格用于参考，最终方案以客服确认结果为准。" />
    <div className="v-service-toolbar"><div className="v-search-box"><Search size={18} /><input value={query} onChange={event => setQuery(event.target.value)} aria-label="搜索服务" placeholder="搜索服务、游戏或任务" /></div><Badge>{loading ? '正在同步' : `${filtered.length} 项服务`}</Badge></div>
    <div className="v-services-layout"><div className="v-service-grid">{loading && Array.from({ length: 8 }).map((_, index) => <Card key={index}><Skeleton className="h-5 w-24" /><Skeleton className="h-8 w-full" /><Skeleton className="h-4 w-2/3" /></Card>)}{!loading && filtered.map((service, index) => <ServiceCard key={serviceKey(service, index)} service={service} />)}{!loading && !filtered.length && <EmptyState title="没有匹配的服务" text="换一个关键词，或直接提交需求让客服确认。" />}</div><aside className="v-summary-card"><span className="v-eyebrow">HOW IT WORKS</span><h3>不知道选哪项也可以询价</h3><p>描述目标、当前进度、游戏平台和时间要求即可。客服会按真实需求确认，不强制用户理解内部分类。</p><Button className="v-primary" onClick={() => onNavigate('quote')}>提交需求 <ArrowRight size={17} /></Button></aside></div>
  </section>;
}
