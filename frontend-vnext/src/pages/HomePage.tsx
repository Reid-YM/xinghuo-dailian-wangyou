import type { CSSProperties } from 'react';
import { ArrowRight, BadgeCheck, MonitorSmartphone, Search, ShieldCheck, Smartphone, Sparkles } from 'lucide-react';
import { Button, Card, Skeleton } from '../components/ui';
import { EmptyState, ServiceCard, serviceKey } from '../components/public-content';
import { pickBoolean, pickNumber, pickText, type PublicBootstrap } from '../lib/api';
import type { PageKey } from '../app/navigation';

export function HomePage({ loading, data, statusLabel, onNavigate }: { loading: boolean; data: PublicBootstrap | null; statusLabel: string; onNavigate: (page: PageKey) => void }) {
  const config = data?.siteConfig;
  const services = data?.services ?? [];
  const trustTags = Array.isArray(config?.trustTags) ? config.trustTags.filter((x): x is string => typeof x === 'string').slice(0, 6) : [];
  const heroTitle = pickText(config, ['heroTitle'], '使命召唤 MW4 任务服务');
  const heroSubtitle = pickText(config, ['heroSubtitle'], '武器升级 · 迷彩解锁 · 活动任务 · 排位冲分');
  const background = pickText(config, ['heroBackgroundImage'], '');
  const heroStyle: CSSProperties = background ? {
    backgroundImage: `linear-gradient(90deg, rgba(11,12,15,.96), rgba(11,12,15,.72) 48%, rgba(11,12,15,.35)), url("${background.replace(/"/g, '%22')}")`,
    backgroundSize: pickText(config, ['heroBackgroundFit'], 'cover'),
    backgroundPosition: `${pickNumber(config, 'heroBackgroundPositionX', 50)}% ${pickNumber(config, 'heroBackgroundPositionY', 50)}%`,
  } : {};
  const limit = Math.max(2, Math.min(8, pickNumber(config, 'homeServiceLimit', 4)));

  return <>
    {pickBoolean(config, 'announcementEnabled', false) && pickText(config, ['announcementText']) && <div className="v-announcement"><div className="v-shell">{pickText(config, ['announcementText'])}</div></div>}
    <section className="v-hero v-hero-live" style={heroStyle}>
      <div className="v-shell v-hero-grid">
        <div className="v-hero-copy">
          <div className="v-kicker"><Sparkles size={16} /> CODMW4 · 当前服务</div><h1>{heroTitle}</h1><p className="v-lead">{heroSubtitle}</p>
          <div className="v-hero-actions"><Button className="v-primary" onClick={() => onNavigate('quote')}>立即询价 <ArrowRight size={18} /></Button><Button className="v-secondary" onClick={() => onNavigate('services')}>查看全部服务</Button></div>
          <div className="v-trust-row">{(trustTags.length ? trustTags : ['可靠手打', '纯绿服务', '进度可查', '一对一咨询']).map((tag, index) => <span key={tag}>{index === 0 ? <ShieldCheck size={18} /> : index === 1 ? <BadgeCheck size={18} /> : <Smartphone size={18} />}{tag}</span>)}</div>
        </div>
        <div className="v-hero-panel v-live-panel" aria-label="服务状态">
          <div className="v-live-panel-inner"><span className="v-eyebrow">LIVE SERVICE</span><strong>{statusLabel}</strong><p>提交需求后生成业务编号；确认报价后再进入履约，进度可在网站查询。</p><div className="v-flow-list"><span><b>01</b> 选择服务与目标</span><span><b>02</b> 客服确认报价</span><span><b>03</b> 业务编号查进度</span></div></div>
          <div className="v-status-strip"><span className="v-status-dot" /> {statusLabel}</div>
        </div>
      </div>
    </section>
    <section className="v-section v-shell">
      <div className="v-section-heading"><div><span className="v-eyebrow">SERVICE PROMISE</span><h2>清楚的流程，比夸张承诺更重要</h2></div><p>网页不要求填写账号密码、验证码或令牌。需求、报价和履约通过同一业务编号串联，方便查询和售后核对。</p></div>
      <div className="v-feature-grid">{[
        ['可靠手打', '人工推进任务，按目标与当前进度确认具体方案。', ShieldCheck],
        ['纯绿服务', '不把外挂、注入器或来路不明工具作为服务方式。', BadgeCheck],
        ['进度可查', '提交后获得业务编号，可在网站查询履约状态。', Search],
        ['多端可用', '手机、电脑、微信窗口与主流浏览器统一适配。', MonitorSmartphone],
      ].map(([title, text, Icon]) => <Card key={String(title)} className="v-feature-card"><span className="v-icon-tile"><Icon size={22} /></span><h3>{String(title)}</h3><p>{String(text)}</p></Card>)}</div>
    </section>
    <section className="v-section v-shell">
      <div className="v-section-heading compact"><div><span className="v-eyebrow">POPULAR SERVICES</span><h2>当前服务</h2></div><Button className="v-secondary" onClick={() => onNavigate('services')}>全部服务</Button></div>
      <div className="v-service-preview-grid">{loading && Array.from({ length: 4 }).map((_, index) => <Card key={index}><Skeleton className="h-5 w-20" /><Skeleton className="h-8 w-full" /><Skeleton className="h-4 w-3/4" /></Card>)}{!loading && services.slice(0, limit).map((service, index) => <ServiceCard key={serviceKey(service, index)} service={service} />)}{!loading && !services.length && <EmptyState title="服务数据暂未返回" text="可稍后刷新或直接联系客服确认。" />}</div>
    </section>
  </>;
}
