import { StrictMode, useEffect, useMemo, useState, type CSSProperties } from 'react';
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
  Menu,
  MessageSquareText,
  MonitorSmartphone,
  PackageCheck,
  Search,
  ShieldCheck,
  Smartphone,
  Sparkles,
  Upload,
} from 'lucide-react';
import { Badge, Button, Card, Sheet, Skeleton } from './components/ui';
import {
  firstTouchSource,
  loadAccountMe,
  loadPublicBootstrap,
  lookupBusinessNo,
  makeIdempotencyKey,
  pickBoolean,
  pickNumber,
  pickText,
  queryParam,
  submitQuote,
  uploadQuoteScreenshot,
  type JsonRecord,
  type PublicBootstrap,
  type QuoteResponse,
} from './lib/api';
import { detectEnvironment, installViewportSync } from './lib/environment';
import './styles.css';

type PageKey = 'home' | 'services' | 'quote' | 'records' | 'account';

const pagePaths: Record<PageKey, string> = {
  home: '/',
  services: '/services',
  quote: '/quote',
  records: '/records',
  account: '/account',
};

const navItems: Array<{ key: PageKey; label: string; icon: typeof House }> = [
  { key: 'home', label: '首页', icon: House },
  { key: 'services', label: '服务', icon: Boxes },
  { key: 'quote', label: '询价', icon: MessageSquareText },
  { key: 'records', label: '进度', icon: Search },
  { key: 'account', label: '我的', icon: CircleUserRound },
];

function pageFromPath(pathname: string): PageKey {
  if (pathname.startsWith('/services')) return 'services';
  if (pathname.startsWith('/quote')) return 'quote';
  if (pathname.startsWith('/records')) return 'records';
  if (pathname === '/account' || pathname === '/account/') return 'account';
  return 'home';
}

function App() {
  const [page] = useState<PageKey>(() => pageFromPath(window.location.pathname));
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

  const config = bootstrap?.siteConfig;
  const brand = pickText(config, ['brandName'], '星火网游代练平台');
  const statusLabel = pickText(bootstrap?.gameStatus, ['label'], pickText(config, ['statusShortText'], '服务状态实时同步'));
  const quoteEnabled = pickBoolean(config, 'quoteEnabled', true);
  const navigate = (target: PageKey) => window.location.assign(pagePaths[target]);

  return (
    <div className="v-app v-production" data-page={page}>
      <header className="v-header">
        <div className="v-shell v-header-inner">
          <button className="v-brand" type="button" onClick={() => navigate('home')} aria-label="返回首页">
            <span className="v-brand-mark">XH</span>
            <span className="v-brand-copy"><strong>{brand}</strong><small>可靠手打 · 进度可查</small></span>
          </button>
          <nav className="v-desktop-nav" aria-label="主导航">
            {navItems.slice(0, 4).map(({ key, label }) => <button key={key} className={page === key ? 'is-active' : ''} onClick={() => navigate(key)}>{label}</button>)}
          </nav>
          <div className="v-header-actions">
            <Badge className="v-env-badge"><MonitorSmartphone size={14} /> {environment.label}</Badge>
            <Button className="v-primary v-desktop-only" onClick={() => navigate(quoteEnabled ? 'quote' : 'home')}>{quoteEnabled ? pickText(config, ['primaryButtonLabel'], '立即询价') : '查看服务状态'}</Button>
            <Sheet title={brand} trigger={<Button className="v-menu-button" aria-label="打开导航"><Menu size={20} /></Button>}>
              <div className="v-sheet-nav">{navItems.map(({ key, label, icon: Icon }) => <button key={key} onClick={() => navigate(key)}><Icon size={19} /><span>{label}</span><ChevronRight size={17} /></button>)}</div>
            </Sheet>
          </div>
        </div>
      </header>

      <main>
        {page === 'home' && <HomeView loading={loading} data={bootstrap} statusLabel={statusLabel} onNavigate={navigate} />}
        {page === 'services' && <ServicesView loading={loading} services={bootstrap?.services ?? []} onNavigate={navigate} />}
        {page === 'quote' && <QuoteView services={bootstrap?.services ?? []} config={config} catalog={bootstrap?.catalog} />}
        {page === 'records' && <RecordsView />}
        {page === 'account' && <AccountView />}
      </main>

      <footer className="v-footer">
        <div className="v-shell v-footer-inner">
          <div><strong>{brand}</strong><p>{pickText(config, ['footerText'], '可靠手打 · 纯绿服务 · 进度可查 · 一对一咨询')}</p></div>
          <div className="v-footer-meta"><a href="/rules">服务规则</a><a href="/privacy">隐私说明</a><a href="/contact">联系客服</a><span>蜀ICP备2026047129号</span></div>
        </div>
      </footer>

      <nav className="v-mobile-dock" aria-label="移动端导航">
        {navItems.map(({ key, label, icon: Icon }) => <button key={key} className={page === key ? 'is-active' : ''} onClick={() => navigate(key)}><Icon size={20} /><span>{label}</span></button>)}
      </nav>
    </div>
  );
}

function HomeView({ loading, data, statusLabel, onNavigate }: { loading: boolean; data: PublicBootstrap | null; statusLabel: string; onNavigate: (page: PageKey) => void }) {
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

function ServicesView({ loading, services, onNavigate }: { loading: boolean; services: JsonRecord[]; onNavigate: (page: PageKey) => void }) {
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

function QuoteView({ services, config, catalog }: { services: JsonRecord[]; config: JsonRecord | null | undefined; catalog: JsonRecord | null | undefined }) {
  const preferredSlug = queryParam('service');
  const preferred = services.find(service => String(service.slug || '') === preferredSlug);
  const [step, setStep] = useState(1);
  const [selected, setSelected] = useState<JsonRecord | null>(preferred ?? null);
  const [demand, setDemand] = useState(preferred ? serviceLabel(preferred, 0) : '');
  const [currentProgress, setCurrentProgress] = useState('');
  const [platform, setPlatform] = useState('');
  const [expectedTime, setExpectedTime] = useState('');
  const [contactMethod, setContactMethod] = useState('');
  const [customerContact, setCustomerContact] = useState('');
  const [privacyConsent, setPrivacyConsent] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [accountContactAvailable, setAccountContactAvailable] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<QuoteResponse | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState(() => makeIdempotencyKey());
  const quoteEnabled = pickBoolean(config, 'quoteEnabled', true);

  useEffect(() => {
    const controller = new AbortController();
    loadAccountMe(controller.signal).then(me => {
      if (!me) return;
      const contacts = Array.isArray(me.quoteContacts) ? me.quoteContacts : [];
      const type = String(me.contactType || '').toUpperCase();
      setAccountContactAvailable(contacts.length > 0 || ['PHONE', 'WECHAT', 'QQ'].includes(type));
    });
    return () => controller.abort();
  }, []);

  const platforms = useMemo(() => {
    const rows = Array.isArray(catalog?.platforms) ? catalog.platforms : [];
    return rows.filter((item): item is JsonRecord => typeof item === 'object' && !!item && item.active !== false).map(item => pickText(item, ['name', 'code'])).filter(Boolean);
  }, [catalog]);

  const chooseService = (service: JsonRecord, index: number) => { setSelected(service); setDemand(serviceLabel(service, index)); };
  const next = () => { setError(''); if (step === 1 && !demand.trim()) return setError('请选择服务或填写具体需求。'); setStep(value => Math.min(3, value + 1)); };
  const submit = async () => {
    setError('');
    if (!quoteEnabled) return setError('当前网页询价已暂停，请通过客服渠道联系。');
    if (!demand.trim()) return setError('请填写具体需求。');
    if (!privacyConsent) return setError('请先阅读并同意服务规则与隐私说明。');
    if (!accountContactAvailable && (!contactMethod || !customerContact.trim())) return setError('请填写联系方式，方便客服确认报价。');
    setBusy(true);
    try {
      const screenshotAssetId = file ? await uploadQuoteScreenshot(file) : null;
      const result = await submitQuote({
        game: pickText(selected, ['gameCode'], 'CODMW4'), demand: demand.trim(), currentProgress: currentProgress.trim() || null,
        screenshotAssetId, platform: platform || null, expectedTime: expectedTime.trim() || null, contactMethod: contactMethod || null,
        customerContact: customerContact.trim() || null, privacyConsent: true, firstTouchSource: firstTouchSource(), utmSource: queryParam('utm_source'),
        utmMedium: queryParam('utm_medium'), utmCampaign: queryParam('utm_campaign'), referrer: document.referrer ? document.referrer.slice(0, 300) : null,
      }, idempotencyKey);
      setSuccess(result); setIdempotencyKey(makeIdempotencyKey());
    } catch (cause) { setError(cause instanceof Error ? cause.message : '提交失败，请稍后重试。'); }
    finally { setBusy(false); }
  };

  if (success) return <section className="v-page v-shell"><PageIntro eyebrow="QUOTE CREATED" title="需求已提交" text="请保存业务编号，后续报价确认与履约进度都以该编号为准。" /><Card className="v-success-card"><BadgeCheck size={48} /><span>业务编号</span><strong>{success.businessNo}</strong><p>{pickText(config, ['quoteSuccessText'], '客服会根据你提交的需求与当前进度确认报价。')}</p><div className="v-hero-actions"><Button className="v-primary" onClick={() => window.location.assign(`/record/${encodeURIComponent(success.businessNo)}`)}>查看业务进度</Button><Button className="v-secondary" onClick={() => window.location.assign('/contact')}>联系客服</Button></div></Card></section>;

  return <section className="v-page v-shell">
    <PageIntro eyebrow="QUOTE" title="提交询价" text="选择服务并说明当前进度。网站只收集完成报价所需的信息，不要提交账号密码、验证码或令牌。" />
    <div className="v-stepper" aria-label="询价步骤">{['选择服务', '补充需求', '确认提交'].map((label, index) => <div key={label} className={step >= index + 1 ? 'is-active' : ''}><span>{index + 1}</span><strong>{label}</strong></div>)}</div>
    <Card className="v-form-card">
      {step === 1 && <><div className="v-choice-grid">{services.slice(0, 8).map((service, index) => { const label = serviceLabel(service, index); return <button key={serviceKey(service, index)} className={selected === service ? 'v-choice is-selected' : 'v-choice'} onClick={() => chooseService(service, index)}><Gamepad2 size={20} /><span><strong>{label}</strong><small>{servicePrice(service)} · {pickText(service, ['durationText'], '按进度确认')}</small></span><ChevronRight size={18} /></button>; })}</div><label className="v-standalone-field"><span>没有合适选项？直接描述需求</span><input value={demand} onChange={event => { setDemand(event.target.value); setSelected(null); }} maxLength={200} placeholder="例如：某武器升满 / 指定迷彩 / 活动奖励" /></label></>}
      {step === 2 && <div className="v-form-grid"><label><span>当前进度</span><textarea rows={4} value={currentProgress} onChange={event => setCurrentProgress(event.target.value)} maxLength={500} placeholder="例如：已完成一半；具体进度可结合截图说明" /></label><label><span>游戏平台</span><select value={platform} onChange={event => setPlatform(event.target.value)}><option value="">不确定，由客服确认</option>{(platforms.length ? platforms : ['Steam', 'Battle.net', 'PlayStation', 'Xbox', 'Nintendo Switch 2']).map(item => <option key={item} value={item}>{item}</option>)}</select></label><label><span>期望完成时间</span><input value={expectedTime} onChange={event => setExpectedTime(event.target.value)} maxLength={100} placeholder="例如：本周内 / 不加急" /></label><label className="v-upload-field"><span>任务截图（可选，≤5MB）</span><span className="v-upload-control"><Upload size={19} /><input type="file" accept="image/jpeg,image/png,image/*" onChange={event => setFile(event.target.files?.[0] ?? null)} />{file ? file.name : '选择图片'}</span></label></div>}
      {step === 3 && <div className="v-form-grid">{accountContactAvailable && <div className="v-account-contact-note"><BadgeCheck size={18} /> 账号已绑定可用联系方式；下面可以留空，也可以临时填写其他联系方式。</div>}<label><span>联系方式</span><select value={contactMethod} onChange={event => setContactMethod(event.target.value)}><option value="">{accountContactAvailable ? '使用账号绑定方式' : '请选择'}</option><option value="微信">微信</option><option value="QQ">QQ</option><option value="手机">手机</option><option value="其他">其他</option></select></label><label><span>联系号</span><input value={customerContact} onChange={event => setCustomerContact(event.target.value)} maxLength={120} placeholder={accountContactAvailable ? '留空使用账号绑定联系方式' : '微信号 / 手机号 / QQ号'} /></label><label className="v-consent"><input type="checkbox" checked={privacyConsent} onChange={event => setPrivacyConsent(event.target.checked)} /><span>我已阅读并同意 <a href="/rules" target="_blank" rel="noreferrer">服务规则</a> 和 <a href="/privacy" target="_blank" rel="noreferrer">隐私说明</a>，不会提交账号密码、验证码或令牌。</span></label><div className="v-confirm-summary"><span>当前需求</span><strong>{demand}</strong><small>{platform || '平台待确认'} · {expectedTime || '时间待确认'}</small></div></div>}
      {error && <div className="v-form-error" role="alert">{error}</div>}
      <div className="v-form-actions"><Button className="v-secondary" disabled={step === 1 || busy} onClick={() => { setError(''); setStep(value => Math.max(1, value - 1)); }}>上一步</Button>{step < 3 ? <Button className="v-primary" disabled={busy} onClick={next}>下一步 <ArrowRight size={17} /></Button> : <Button className="v-primary" disabled={busy || !quoteEnabled} onClick={submit}>{busy ? '正在提交…' : quoteEnabled ? '确认提交' : '当前暂停询价'}</Button>}</div>
    </Card>
  </section>;
}

function RecordsView() {
  const [businessNo, setBusinessNo] = useState(''); const [record, setRecord] = useState<JsonRecord | null>(null); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  const search = async () => { const value = businessNo.trim(); if (!value) return; setBusy(true); setError(''); setRecord(null); try { setRecord(await lookupBusinessNo(value)); } catch (cause) { setError(cause instanceof Error ? cause.message : '没有查到该业务编号。'); } finally { setBusy(false); } };
  return <section className="v-page v-shell"><PageIntro eyebrow="ORDER / PROGRESS" title="业务进度查询" text="输入询价或订单业务编号，查看当前阶段、进度和最近更新时间。" /><div className="v-record-layout"><Card className="v-query-card"><label><span>业务编号</span><div className="v-input-action"><input value={businessNo} onChange={event => setBusinessNo(event.target.value)} placeholder="例如：XH7KM4Q2AZ8P" onKeyDown={event => event.key === 'Enter' && search()} /><Button className="v-primary" disabled={busy || !businessNo.trim()} onClick={search}>{busy ? '查询中' : '查询'}</Button></div></label><p><ShieldCheck size={16} /> 查询接口保留现有限流与公开信息边界。</p></Card><Card className="v-result-card">{!record && !error && <EmptyState title="等待查询" text="结果会以卡片显示，手机端不需要横向滚动。" />}{error && <EmptyState title="未找到结果" text={error} />}{record && <div className="v-record-result"><Badge>{pickText(record, ['stage'], '业务记录')}</Badge><h3>{pickText(record, ['title'], pickText(record, ['businessNo'], businessNo))}</h3><dl><div><dt>状态</dt><dd>{pickText(record, ['status'], '处理中')}</dd></div><div><dt>进度</dt><dd>{pickText(record, ['progressText'], `${pickNumber(record, 'progressPercent', 0)}%`)}</dd></div><div><dt>更新时间</dt><dd>{formatDate(pickText(record, ['updatedAt']))}</dd></div></dl>{pickText(record, ['customerNote']) && <p className="v-record-note">客服备注：{pickText(record, ['customerNote'])}</p>}</div>}</Card></div></section>;
}

function AccountView() {
  const items = [['/account/requests', '我的需求', '查看询价与客服处理状态'], ['/account/orders', '我的订单', '查看履约状态与交付进度'], ['/account/progress', '工具存档', '查看跨设备保存的工具进度'], ['/account/settings', '账号设置', '资料、回访方式与安全设置']];
  return <section className="v-page v-shell"><PageIntro eyebrow="ACCOUNT" title="个人中心" text="需求、订单、工具进度与账号设置集中管理。当前页面继续沿用原有账号登录与权限校验。" /><div className="v-account-grid">{items.map(([href, title, text]) => <a key={href} className="v-account-link" href={href}><span className="v-icon-tile"><CircleUserRound size={21} /></span><span><strong>{title}</strong><small>{text}</small></span><ChevronRight size={18} /></a>)}</div><Card className="v-account-help"><span className="v-eyebrow">SECURITY</span><h2>账号与业务信息分开管理</h2><p>询价时已绑定的手机号、微信或 QQ 可由后端自动补齐；网页不会展示或要求游戏账号密码、验证码或令牌。</p></Card></section>;
}

function ServiceCard({ service }: { service: JsonRecord }) {
  const slug = pickText(service, ['slug']); const image = pickText(service, ['coverImage']); const href = slug ? `/service/${encodeURIComponent(slug)}` : '/quote';
  return <Card className="v-service-card">{image && <a className="v-service-cover" href={href}><img src={image} alt={pickText(service, ['name'], '服务封面')} loading="lazy" /></a>}<div className="v-service-card-top"><Badge>{pickText(service, ['gameCode', 'category'], '游戏服务')}</Badge><Clock3 size={18} /></div><h3>{pickText(service, ['name'], '服务项目')}</h3><p>{pickText(service, ['shortDesc'], '具体内容与价格以当前服务配置为准。')}</p><div className="v-service-price"><strong>{servicePrice(service)}</strong><span>{pickText(service, ['durationText'], '按进度确认')}</span></div><a className="v-service-link" href={href}>查看详情 <ArrowRight size={16} /></a></Card>;
}

function EmptyState({ title, text }: { title: string; text: string }) { return <div className="v-empty"><PackageCheck size={30} /><strong>{title}</strong><p>{text}</p></div>; }
function PageIntro({ eyebrow, title, text }: { eyebrow: string; title: string; text: string }) { return <div className="v-page-intro"><span className="v-eyebrow">{eyebrow}</span><h1>{title}</h1><p>{text}</p></div>; }
function serviceLabel(service: JsonRecord, index: number) { return pickText(service, ['name', 'title', 'serviceName'], `服务方案 ${index + 1}`); }
function serviceKey(service: JsonRecord, index: number) { return pickText(service, ['id', 'slug', 'name'], String(index)); }
function servicePrice(service: JsonRecord) { const mode = pickText(service, ['priceMode']).toLowerCase(); const fixed = Number(service.priceFixed); const min = Number(service.priceMin); const max = Number(service.priceMax); if (mode === 'fixed' && Number.isFinite(fixed)) return `¥${fixed}`; if (mode === 'range' && Number.isFinite(min) && Number.isFinite(max)) return `¥${min}–${max}`; if (Number.isFinite(min) && min > 0) return `参考 ¥${min} 起`; return '人工报价'; }
function formatDate(value: string) { if (!value) return '以后台为准'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false }); }

createRoot(document.getElementById('root')!).render(<StrictMode><App /></StrictMode>);
