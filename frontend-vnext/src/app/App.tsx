import { useEffect, useMemo, useState } from 'react';
import { ChevronRight, Menu, MonitorSmartphone } from 'lucide-react';
import { Badge, Button, Sheet } from '../components/ui';
import { loadPublicBootstrap, pickBoolean, pickText, type PublicBootstrap } from '../lib/api';
import { detectEnvironment, installViewportSync } from '../lib/environment';
import { AccountPage } from '../pages/AccountPage';
import { HomePage } from '../pages/HomePage';
import { QuotePage } from '../pages/QuotePage';
import { RecordsPage } from '../pages/RecordsPage';
import { ServicesPage } from '../pages/ServicesPage';
import { navItems, pageFromPath, pagePaths, type PageKey } from './navigation';

export function App() {
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
        {page === 'home' && <HomePage loading={loading} data={bootstrap} statusLabel={statusLabel} onNavigate={navigate} />}
        {page === 'services' && <ServicesPage loading={loading} services={bootstrap?.services ?? []} onNavigate={navigate} />}
        {page === 'quote' && <QuotePage services={bootstrap?.services ?? []} config={config} catalog={bootstrap?.catalog} />}
        {page === 'records' && <RecordsPage />}
        {page === 'account' && <AccountPage />}
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
