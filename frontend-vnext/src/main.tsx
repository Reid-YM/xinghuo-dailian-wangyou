import { StrictMode, useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  ArrowRight,
  BadgeCheck,
  Boxes,
  ChevronRight,
  CircleUserRound,
  Clock3,
  Gamepad2,
  House,
  LayoutDashboard,
  Menu,
  MessageSquareText,
  MonitorSmartphone,
  PackageCheck,
  Search,
  ShieldCheck,
  Smartphone,
  Sparkles,
} from 'lucide-react';
import { Badge, Button, Card, Sheet, Skeleton } from './components/ui';
import { loadPublicBootstrap, lookupBusinessNo, pickText, type JsonRecord, type PublicBootstrap } from './lib/api';
import { detectEnvironment, installViewportSync } from './lib/environment';
import './styles.css';

type PageKey = 'home' | 'services' | 'quote' | 'records' | 'account' | 'admin';

const navItems: Array<{ key: PageKey; label: string; icon: typeof House }> = [
  { key: 'home', label: '首页', icon: House },
  { key: 'services', label: '服务', icon: Boxes },
  { key: 'quote', label: '询价', icon: MessageSquareText },
  { key: 'records', label: '进度', icon: Search },
  { key: 'account', label: '我的', icon: CircleUserRound },
];

function App() {
  const [page, setPage] = useState<PageKey>('home');
  const [bootstrap, setBootstrap] = useState<PublicBootstrap | null>(null);
  const [loading, setLoading] = useState(true);
  const environment = useMemo(() => detectEnvironment(), []);

  useEffect(() => installViewportSync(), []);
  useEffect(() => {
    const controller = new AbortController();
    loadPublicBootstrap(controller.signal)
      .then(setBootstrap)
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, []);

  const statusLabel = pickText(bootstrap?.gameStatus, ['label', 'status'], '服务状态实时同步');

  return (
    <div className="v-app" data-page={page}>
      <header className="v-header">
        <div className="v-shell v-header-inner">
          <button className="v-brand" type="button" onClick={() => setPage('home')} aria-label="返回 VNext 首页">
            <span className="v-brand-mark">XH</span>
            <span className="v-brand-copy">
              <strong>星火 VNext</strong>
              <small>RESPONSIVE PREVIEW</small>
            </span>
          </button>

          <nav className="v-desktop-nav" aria-label="主导航">
            {navItems.slice(0, 4).map(({ key, label }) => (
              <button key={key} className={page === key ? 'is-active' : ''} onClick={() => setPage(key)}>
                {label}
              </button>
            ))}
            <button className={page === 'admin' ? 'is-active' : ''} onClick={() => setPage('admin')}>后台框架</button>
          </nav>

          <div className="v-header-actions">
            <Badge className="v-env-badge"><MonitorSmartphone size={14} /> {environment.label}</Badge>
            <Button className="v-primary v-desktop-only" onClick={() => setPage('quote')}>提前询价</Button>
            <Sheet
              title="VNext 导航"
              trigger={<Button className="v-menu-button" aria-label="打开导航"><Menu size={20} /></Button>}
            >
              <div className="v-sheet-nav">
                {[...navItems, { key: 'admin' as PageKey, label: '后台框架', icon: LayoutDashboard }].map(({ key, label, icon: Icon }) => (
                  <button key={key} onClick={() => setPage(key)}>
                    <Icon size={19} />
                    <span>{label}</span>
                    <ChevronRight size={17} />
                  </button>
                ))}
              </div>
            </Sheet>
          </div>
        </div>
      </header>

      <main>
        {page === 'home' && <HomeView loading={loading} data={bootstrap} statusLabel={statusLabel} onNavigate={setPage} />}
        {page === 'services' && <ServicesView loading={loading} services={bootstrap?.services ?? []} onNavigate={setPage} />}
        {page === 'quote' && <QuoteView services={bootstrap?.services ?? []} />}
        {page === 'records' && <RecordsView />}
        {page === 'account' && <AccountView />}
        {page === 'admin' && <AdminView />}
      </main>

      <footer className="v-footer">
        <div className="v-shell v-footer-inner">
          <div><strong>星火 VNext Preview</strong><p>仅用于新架构与跨端 UI 审核，现网主入口未替换。</p></div>
          <div className="v-footer-meta"><span>React + TypeScript</span><span>Vite + Tailwind</span><span>Radix primitives</span></div>
        </div>
      </footer>

      <nav className="v-mobile-dock" aria-label="移动端导航">
        {navItems.map(({ key, label, icon: Icon }) => (
          <button key={key} className={page === key ? 'is-active' : ''} onClick={() => setPage(key)}>
            <Icon size={20} />
            <span>{label}</span>
          </button>
        ))}
      </nav>
    </div>
  );
}

function HomeView({
  loading,
  data,
  statusLabel,
  onNavigate,
}: {
  loading: boolean;
  data: PublicBootstrap | null;
  statusLabel: string;
  onNavigate: (page: PageKey) => void;
}) {
  return (
    <>
      <section className="v-hero">
        <div className="v-shell v-hero-grid">
          <div className="v-hero-copy">
            <div className="v-kicker"><Sparkles size={16} /> 方案 A · 隔离预览</div>
            <h1>一次重构，适配手机、电脑、微信窗口与任意浏览器尺寸。</h1>
            <p className="v-lead">保留现有 Spring Boot 业务能力，新前端以组件化、响应式和终端环境适配为底座。你确认 UI 体系前，现网视觉不会被替换。</p>
            <div className="v-hero-actions">
              <Button className="v-primary" onClick={() => onNavigate('services')}>查看服务架构 <ArrowRight size={18} /></Button>
              <Button className="v-secondary" onClick={() => onNavigate('quote')}>查看询价流程</Button>
            </div>
            <div className="v-trust-row">
              <span><ShieldCheck size={18} /> 现网入口不变</span>
              <span><Smartphone size={18} /> 320px 起完整适配</span>
              <span><BadgeCheck size={18} /> 微信安全区适配</span>
            </div>
          </div>

          <div className="v-hero-panel" aria-label="设备适配预览">
            <div className="v-device-stack">
              <div className="v-device v-device-desktop">
                <div className="v-device-bar"><i /><i /><i /></div>
                <div className="v-device-content">
                  <span className="v-demo-title">Desktop Canvas</span>
                  <div className="v-demo-grid"><i /><i /><i /><i /></div>
                  <div className="v-demo-line wide" /><div className="v-demo-line" />
                </div>
              </div>
              <div className="v-device v-device-phone">
                <div className="v-phone-notch" />
                <div className="v-device-content">
                  <span className="v-demo-title">Mobile</span>
                  <div className="v-demo-line wide" /><div className="v-demo-line" />
                  <div className="v-demo-card" />
                </div>
              </div>
            </div>
            <div className="v-status-strip"><span className="v-status-dot" /> {statusLabel}</div>
          </div>
        </div>
      </section>

      <section className="v-section v-shell">
        <div className="v-section-heading">
          <div><span className="v-eyebrow">FOUNDATION</span><h2>第一阶段先锁架构，不锁死视觉</h2></div>
          <p>设计 Token、组件 API 和响应式规则已经建立成可替换层。后续换 UI 体系，不需要再次推翻业务页面。</p>
        </div>
        <div className="v-feature-grid">
          {[
            ['响应式布局引擎', '流式容器、CSS Grid、clamp、safe-area 与超宽屏约束。', MonitorSmartphone],
            ['组件化交互', '按钮、卡片、抽屉等采用本地可控组件，底层接 Radix。', Boxes],
            ['微信环境适配', '识别 MicroMessenger 与 VisualViewport，不依赖固定高度。', Smartphone],
            ['现有 API 复用', '服务、站点配置、状态与进度查询继续使用现有后端接口。', PackageCheck],
          ].map(([title, text, Icon]) => (
            <Card key={String(title)} className="v-feature-card">
              <span className="v-icon-tile"><Icon size={22} /></span>
              <h3>{String(title)}</h3><p>{String(text)}</p>
            </Card>
          ))}
        </div>
      </section>

      <section className="v-section v-shell">
        <div className="v-section-heading compact">
          <div><span className="v-eyebrow">LIVE DATA</span><h2>现有业务数据直接进入新前端</h2></div>
          <Button className="v-secondary" onClick={() => onNavigate('services')}>全部服务</Button>
        </div>
        <div className="v-service-preview-grid">
          {loading && Array.from({ length: 4 }).map((_, index) => <Card key={index}><Skeleton className="h-5 w-20" /><Skeleton className="h-8 w-full" /><Skeleton className="h-4 w-3/4" /></Card>)}
          {!loading && (data?.services ?? []).slice(0, 4).map((service, index) => <ServiceCard key={serviceKey(service, index)} service={service} />)}
          {!loading && !(data?.services?.length) && <EmptyState title="服务数据暂未返回" text="VNext 会保持可用并回退到现网服务入口。" />}
        </div>
      </section>
    </>
  );
}

function ServicesView({ loading, services, onNavigate }: { loading: boolean; services: JsonRecord[]; onNavigate: (page: PageKey) => void }) {
  return (
    <section className="v-page v-shell">
      <PageIntro eyebrow="SERVICE BROWSER" title="服务中心重构" text="PC 采用高密度网格与侧边摘要；手机自动改成单列触控卡片，不再把桌面表格硬塞进小屏幕。" />
      <div className="v-service-toolbar">
        <div className="v-search-box"><Search size={18} /><input aria-label="搜索服务" placeholder="搜索服务、模式或需求" /></div>
        <Badge>{services.length ? `${services.length} 项服务已同步` : '等待服务数据'}</Badge>
      </div>
      <div className="v-services-layout">
        <div className="v-service-grid">
          {loading && Array.from({ length: 8 }).map((_, index) => <Card key={index}><Skeleton className="h-5 w-24" /><Skeleton className="h-8 w-full" /><Skeleton className="h-4 w-2/3" /></Card>)}
          {!loading && services.map((service, index) => <ServiceCard key={serviceKey(service, index)} service={service} />)}
          {!loading && !services.length && <EmptyState title="暂无可展示服务" text="请检查 /api/services；现网业务没有因此被改动。" />}
        </div>
        <aside className="v-summary-card">
          <span className="v-eyebrow">NEXT STEP</span><h3>把复杂下拉框改成分步选择</h3>
          <p>游戏 → 模式 → 服务 → 需求 → 确认。移动端每一步都保持 44px 以上触控面积。</p>
          <Button className="v-primary" onClick={() => onNavigate('quote')}>体验询价步骤 <ArrowRight size={17} /></Button>
        </aside>
      </div>
    </section>
  );
}

function QuoteView({ services }: { services: JsonRecord[] }) {
  const [step, setStep] = useState(1);
  const [selected, setSelected] = useState('');
  return (
    <section className="v-page v-shell">
      <PageIntro eyebrow="QUOTE FLOW" title="三步询价流程" text="当前为安全预览：最后一步不会写入生产订单，确认 UI 后再把提交动作切到现有 /api/quote。" />
      <div className="v-stepper" aria-label="询价步骤">
        {['选择服务', '填写需求', '确认信息'].map((label, index) => <div key={label} className={step >= index + 1 ? 'is-active' : ''}><span>{index + 1}</span><strong>{label}</strong></div>)}
      </div>
      <Card className="v-form-card">
        {step === 1 && <div className="v-choice-grid">
          {(services.length ? services.slice(0, 6) : [{ name: '服务方案 A' }, { name: '服务方案 B' }, { name: '服务方案 C' }]).map((service, index) => {
            const label = serviceLabel(service as JsonRecord, index);
            return <button key={label} className={selected === label ? 'v-choice is-selected' : 'v-choice'} onClick={() => setSelected(label)}><Gamepad2 size={20} /><span><strong>{label}</strong><small>点击选择，不使用原生下拉框</small></span><ChevronRight size={18} /></button>;
          })}
        </div>}
        {step === 2 && <div className="v-form-grid">
          <label><span>需求说明</span><textarea rows={5} placeholder="例如：目标、当前进度、时间要求" /></label>
          <label><span>联系方式</span><input type="text" placeholder="微信 / QQ / 手机" /></label>
          <label><span>期望完成时间</span><input type="text" placeholder="例如：本周内" /></label>
        </div>}
        {step === 3 && <div className="v-confirm-panel"><BadgeCheck size={42} /><h3>预览确认页</h3><p>服务：{selected || '尚未选择'}</p><p>生产提交暂时保持关闭，避免 UI 审核阶段生成真实询价。</p></div>}
        <div className="v-form-actions">
          <Button className="v-secondary" disabled={step === 1} onClick={() => setStep(value => Math.max(1, value - 1))}>上一步</Button>
          {step < 3 ? <Button className="v-primary" disabled={step === 1 && !selected} onClick={() => setStep(value => Math.min(3, value + 1))}>下一步 <ArrowRight size={17} /></Button> : <Button className="v-primary" onClick={() => window.location.assign('/quote')}>前往现网询价</Button>}
        </div>
      </Card>
    </section>
  );
}

function RecordsView() {
  const [businessNo, setBusinessNo] = useState('');
  const [record, setRecord] = useState<JsonRecord | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const search = async () => {
    const value = businessNo.trim();
    if (!value) return;
    setBusy(true); setError(''); setRecord(null);
    try { setRecord(await lookupBusinessNo(value)); }
    catch { setError('没有查到该业务编号，或当前查询服务不可用。'); }
    finally { setBusy(false); }
  };

  return (
    <section className="v-page v-shell">
      <PageIntro eyebrow="ORDER / PROGRESS" title="业务进度查询" text="读取现有公开进度 API。桌面端左右分栏，手机端自动堆叠为卡片。" />
      <div className="v-record-layout">
        <Card className="v-query-card">
          <label><span>业务编号</span><div className="v-input-action"><input value={businessNo} onChange={event => setBusinessNo(event.target.value)} placeholder="输入业务编号" onKeyDown={event => event.key === 'Enter' && search()} /><Button className="v-primary" disabled={busy || !businessNo.trim()} onClick={search}>{busy ? '查询中' : '查询'}</Button></div></label>
          <p><ShieldCheck size={16} /> 查询沿用后端现有限流与公开数据边界。</p>
        </Card>
        <Card className="v-result-card">
          {!record && !error && <EmptyState title="等待查询" text="结果会显示在这里，不使用横向滚动表格。" />}
          {error && <EmptyState title="未找到结果" text={error} />}
          {record && <div className="v-record-result"><Badge>查询成功</Badge><h3>{pickText(record, ['businessNo', 'orderNo', 'quoteNo'], businessNo)}</h3><dl><div><dt>状态</dt><dd>{pickText(record, ['statusLabel', 'status'], '处理中')}</dd></div><div><dt>进度</dt><dd>{pickText(record, ['progressText', 'progress', 'stage'], '已同步')}</dd></div><div><dt>更新时间</dt><dd>{pickText(record, ['updatedAt', 'lastUpdatedAt'], '以后台为准')}</dd></div></dl></div>}
        </Card>
      </div>
    </section>
  );
}

function AccountView() {
  const items = [
    ['/account', '账户首页', '统一查看账号与快捷入口'],
    ['/account/orders', '我的订单', '移动端使用卡片，不强制横向表格'],
    ['/account/progress', '进度记录', '按业务编号集中查看进度'],
    ['/account/settings', '账户设置', '资料、安全与联系方式'],
  ];
  return <section className="v-page v-shell"><PageIntro eyebrow="ACCOUNT" title="用户中心信息架构" text="当前先保留业务链接，确认 UI 后再逐页迁移真实账号页面。" /><div className="v-account-grid">{items.map(([href, title, text]) => <a key={href} className="v-account-link" href={href}><span className="v-icon-tile"><CircleUserRound size={21} /></span><span><strong>{title}</strong><small>{text}</small></span><ChevronRight size={18} /></a>)}</div></section>;
}

function AdminView() {
  const areas = ['Dashboard', '商品管理', '分类管理', '询价管理', '订单管理', '用户管理', '视觉配置', '联系方式', '网站配置'];
  return <section className="v-page v-shell"><PageIntro eyebrow="ADMIN SHELL" title="后台统一框架预览" text="不更改现有管理员权限与业务逻辑，只先确认新的信息架构、响应式侧栏和组件边界。" /><div className="v-admin-preview"><aside>{areas.map((area, index) => <button className={index === 0 ? 'is-active' : ''} key={area}><span>{area}</span><ChevronRight size={16} /></button>)}</aside><div className="v-admin-canvas"><div className="v-admin-title"><div><span className="v-eyebrow">DASHBOARD</span><h3>运营总览</h3></div><Badge>UI PREVIEW</Badge></div><div className="v-metric-grid">{['今日询价', '处理中订单', '待跟进', '服务状态'].map((item, index) => <Card key={item}><span>{item}</span><strong>{index === 3 ? '正常' : '—'}</strong><small>不编造生产数值</small></Card>)}</div><Card className="v-admin-table-placeholder"><div className="v-demo-line wide" /><div className="v-demo-line" /><div className="v-demo-line wide" /><div className="v-demo-line" /></Card></div></div></section>;
}

function ServiceCard({ service }: { service: JsonRecord }) {
  return <Card className="v-service-card"><div className="v-service-card-top"><Badge>{pickText(service, ['gameName', 'gameCode', 'category'], '游戏服务')}</Badge><Clock3 size={18} /></div><h3>{pickText(service, ['name', 'title', 'serviceName'], '服务项目')}</h3><p>{pickText(service, ['summary', 'description', 'subtitle'], '具体内容与价格以现网配置为准。')}</p><button type="button">查看服务结构 <ArrowRight size={16} /></button></Card>;
}

function EmptyState({ title, text }: { title: string; text: string }) {
  return <div className="v-empty"><PackageCheck size={30} /><strong>{title}</strong><p>{text}</p></div>;
}

function PageIntro({ eyebrow, title, text }: { eyebrow: string; title: string; text: string }) {
  return <div className="v-page-intro"><span className="v-eyebrow">{eyebrow}</span><h1>{title}</h1><p>{text}</p></div>;
}

function serviceLabel(service: JsonRecord, index: number) {
  return pickText(service, ['name', 'title', 'serviceName'], `服务方案 ${index + 1}`);
}

function serviceKey(service: JsonRecord, index: number) {
  return pickText(service, ['id', 'code', 'slug', 'name'], String(index));
}

createRoot(document.getElementById('root')!).render(<StrictMode><App /></StrictMode>);
