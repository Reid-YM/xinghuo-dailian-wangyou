(() => {
  'use strict';
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const bool = (value, fallback = true) => value == null || value === '' ? fallback : String(value).toLowerCase() === 'true';
  const text = (selector, value, root = document) => {
    if (value == null || value === '') return;
    $$(selector, root).forEach(node => { node.textContent = value; });
  };
  const visible = (selector, show, root = document) => $$(selector, root).forEach(node => { node.hidden = !show; });
  const safeHref = value => {
    const href = String(value || '').trim();
    if (href.startsWith('#')) return href.length > 1 ? href : null;
    if (href.startsWith('/')) return href.startsWith('//') ? null : href;
    return href.startsWith('https://') ? href : null;
  };
  const href = (selector, value, root = document) => {
    const safe = safeHref(value);
    if (safe) $$(selector, root).forEach(node => node.setAttribute('href', safe));
  };

  const DESKTOP_HOME_MODULES = {
    hero: '#homeHero',
    command: '.xh-desktop-command',
    game: '#gameEntrySection',
    why: 'main section[aria-label="为什么选择星火"]',
    hot: '.xh-desktop-hot',
    services: '.home-service-showcase',
    tool: '.xh-desktop-lower-grid',
    cases: '#publicCaseSection',
    faq: '.xh-desktop-faq'
  };

  const MOBILE_HOME_MODULES = {
    hero: '#homeHero',
    launcher: '.xh-mobile-launcher',
    game: '#gameEntrySection',
    quote: '.xh-mobile-quote-card',
    support: '.xh-mobile-two-up',
    hot: '.xh-mobile-hot',
    services: '.xh-mobile-services',
    why: 'main section[aria-label="为什么选择星火"]',
    tool: '.xh-mobile-tool',
    cases: '#publicCaseSection',
    faq: '.xh-mobile-faq'
  };

  function rgb(hex) {
    const match = /^#([0-9a-f]{6})$/i.exec(String(hex || '').trim());
    if (!match) return null;
    const value = Number.parseInt(match[1], 16);
    return `${value >> 16 & 255},${value >> 8 & 255},${value & 255}`;
  }

  function applyFeatureSwitches(control = {}) {
    visible('.site-header', bool(control.siteHeaderVisible));
    visible('body > footer', bool(control.siteFooterVisible));
    visible('a[href="/services"]', bool(control.servicesEntryVisible));
    visible('a[href="/quote"], [data-primary-cta]', bool(control.quoteEntryVisible));
    visible('a[href="/records"], .hero-progress-link', bool(control.recordsEntryVisible));
    visible('.mobile-dock', bool(control.mobileDockVisible));

    text('.mobile-dock a[href="/services"]', control.mobileDockServicesLabel);
    text('.mobile-dock a[href="/guides"]', control.mobileDockGuidesLabel);
    text('.mobile-dock a[href="/records"]', control.mobileDockRecordsLabel);
    text('.mobile-dock a[href="/contact"]', control.mobileDockContactLabel);
    text('.mobile-dock [data-primary-cta]', control.mobileDockQuoteLabel);
  }

  function applyTheme(control = {}) {
    const root = document.documentElement;
    const accent = String(control.themeAccentColor || '').trim();
    const accentRgb = rgb(accent);
    if (accentRgb) {
      root.style.setProperty('--accent-rgb', accentRgb);
      ['--accent', '--accent-hover', '--accent-text', '--yellow', '--yellow-2'].forEach(key => root.style.setProperty(key, accent));
    }
    const background = String(control.themeBackgroundColor || '').trim();
    const surface = String(control.themeSurfaceColor || '').trim();
    const foreground = String(control.themeTextColor || '').trim();
    if (rgb(background)) root.style.setProperty('--bg', background);
    if (rgb(surface)) root.style.setProperty('--surface-2', surface);
    if (rgb(foreground)) root.style.setProperty('--text', foreground);

    const radius = Number(control.themeRadius);
    const width = Number(control.themeContentWidth);
    const scale = Number(control.themeFontScale);
    if (radius >= 6 && radius <= 30) root.style.setProperty('--radius', `${radius}px`);
    if (width >= 960 && width <= 1440) root.style.setProperty('--max', `${width}px`);
    if (scale >= 90 && scale <= 115) root.style.fontSize = `${scale}%`;

    if (!document.getElementById('frontendMotionControlStyle')) {
      const style = document.createElement('style');
      style.id = 'frontendMotionControlStyle';
      style.textContent = '.frontend-motion-off *, .frontend-motion-off *::before, .frontend-motion-off *::after{animation-duration:.001ms!important;animation-iteration-count:1!important;transition-duration:.001ms!important;scroll-behavior:auto!important}';
      document.head.appendChild(style);
    }
    root.classList.toggle('frontend-motion-off', !bool(control.motionEnabled));
  }

  function homeMode() {
    if (document.body.classList.contains('xh-mobile-home-page')) return 'mobile';
    if (document.body.classList.contains('xh-desktop-home-page')) return 'desktop';
    return document.documentElement.dataset.deviceMode === 'mobile' ? 'mobile' : 'desktop';
  }

  function applyHomeModuleOrder(control = {}) {
    const raw = String(control.homeModuleOrder || '').trim();
    if (!raw) return;
    const mode = homeMode();
    const map = mode === 'mobile' ? MOBILE_HOME_MODULES : DESKTOP_HOME_MODULES;
    const main = document.querySelector(mode === 'mobile' ? 'main.xh-mobile-home' : 'main.xh-desktop-home');
    if (!main) return;

    const requested = raw.split(',').map(value => value.trim()).filter(id => map[id]);
    const orderedIds = [...new Set([...requested, ...Object.keys(map)])];
    orderedIds.forEach(id => {
      const node = document.querySelector(map[id]);
      if (node && node.parentElement === main) main.appendChild(node);
    });
  }

  function applyHomeSizing(control = {}) {
    const hero = document.querySelector('#homeHero');
    const heroHeight = Number(control.homeHeroHeight || 0);
    if (hero) {
      if (heroHeight > 0 && heroHeight <= 900) hero.style.minHeight = `${heroHeight}px`;
      else hero.style.removeProperty('min-height');
    }

    const root = document.documentElement;
    const cardScale = Number(control.homeCardScale || 0);
    if (!document.getElementById('frontendHomeLayoutControlStyle')) {
      const style = document.createElement('style');
      style.id = 'frontendHomeLayoutControlStyle';
      style.textContent = '.frontend-card-scale-active .xh-desktop-home .service-card,.frontend-card-scale-active .xh-mobile-home .service-card,.frontend-card-scale-active .xh-desktop-proof-grid .card,.frontend-card-scale-active .xh-mobile-proof-grid .card{padding:var(--xh-home-card-padding)!important}';
      document.head.appendChild(style);
    }
    const active = cardScale >= 70 && cardScale <= 140;
    root.classList.toggle('frontend-card-scale-active', active);
    if (active) root.style.setProperty('--xh-home-card-padding', `${Math.round(16 * cardScale / 100)}px`);
    else root.style.removeProperty('--xh-home-card-padding');
  }

  function applyHomeVisibility(control = {}) {
    visible('#homeHero', bool(control.homeHeroVisible));
    visible('#homeHero .hero-promise', bool(control.homeHeroPromisesVisible));
    visible('#homeHero .hero-media-panel', bool(control.homeHeroMediaVisible));
    visible('.xh-mobile-launcher', bool(control.homeLauncherVisible));
    visible('.quote-card-home', bool(control.homeQuotePanelVisible));
    visible('.desktop-secondary-panel', bool(control.homeRecordPanelVisible));
    visible('.xh-desktop-hot, .xh-mobile-hot', bool(control.homeHotVisible));
    visible('.home-service-showcase, .xh-mobile-services', bool(control.homeServicesVisible));
    visible('.xh-desktop-lower-grid, .xh-mobile-tool', bool(control.homeToolVisible));
    visible('.xh-desktop-faq, .xh-mobile-faq', bool(control.homeFaqVisible));

    if (!bool(control.homeGameEntryVisible)) visible('#gameEntrySection', false);
    if (!bool(control.homeCasesVisible)) visible('#publicCaseSection', false);

    const desktopContact = document.querySelector('.xh-desktop-command .quick-contact-card');
    const desktopCommand = document.querySelector('.xh-desktop-command');
    if (desktopCommand) {
      desktopCommand.hidden = !bool(control.homeQuotePanelVisible)
        && !bool(control.homeRecordPanelVisible)
        && !(desktopContact && !desktopContact.hidden);
    }
    const mobileContact = document.querySelector('.xh-mobile-two-up .quick-contact-card');
    const mobileSupport = document.querySelector('.xh-mobile-two-up');
    if (mobileSupport) {
      mobileSupport.hidden = !bool(control.homeRecordPanelVisible)
        && !(mobileContact && !mobileContact.hidden);
    }
  }

  function applyHome(control = {}) {
    if (location.pathname !== '/') return;
    applyHomeVisibility(control);
    applyHomeSizing(control);
    applyHomeModuleOrder(control);

    text('#homeHero .hero-kicker', control.homeHeroKicker);
    text('#homeHero [data-primary-cta]', control.homePrimaryCtaLabel);
    href('#homeHero [data-primary-cta]', control.homePrimaryCtaHref);
    const secondary = '#homeHero .hero-actions .btn.ghost:not(.hero-progress-link)';
    visible(secondary, bool(control.homeSecondaryCtaVisible));
    text(secondary, control.homeSecondaryCtaLabel);
    href(secondary, control.homeSecondaryCtaHref);
    visible('#homeHero .hero-progress-link', bool(control.homeProgressCtaVisible));
    text('#homeHero .hero-progress-link', control.homeProgressCtaLabel);
    href('#homeHero .hero-progress-link', control.homeProgressCtaHref);

    text('.quote-card-home .quote-card-head .eyebrow', control.homeQuoteEyebrow);
    text('.quote-card-home .quote-card-head h2', control.homeQuoteTitle);
    text('.desktop-secondary-panel .quote-card-head .eyebrow', control.homeRecordEyebrow);
    text('.desktop-secondary-panel .quote-card-head h2', control.homeRecordTitle);

    text('#gameEntrySection .game-entry-head .eyebrow', control.homeGameEyebrow);
    text('#gameEntrySection .game-entry-head h2', control.homeGameTitle);
    text('#gameEntrySection .game-entry-head .text-link', control.homeGameLinkLabel);
    href('#gameEntrySection .game-entry-head .text-link', control.homeGameLinkHref);

    const why = document.querySelector('main section[aria-label="为什么选择星火"]');
    if (why) {
      why.hidden = !bool(control.homeWhyVisible);
      text('.section-title .eyebrow', control.homeWhyEyebrow, why);
      text('.section-title h2', control.homeWhyTitle, why);
      text('.section-title .text-link', control.homeWhyLinkLabel, why);
      href('.section-title .text-link', control.homeWhyLinkHref, why);
    }
  }

  async function load() {
    try {
      const response = await fetch('/api/public/frontend-controls', { cache: 'no-store' });
      if (!response.ok) return;
      const control = await response.json();
      const apply = () => {
        applyTheme(control || {});
        applyHome(control || {});
        applyFeatureSwitches(control || {});
      };
      apply();
      [120, 600, 1500].forEach(delay => setTimeout(apply, delay));
    } catch (_) {
      // 配置层异常时保留模板默认值，不阻断任何业务页面。
    }
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load, { once: true });
  else load();
})();
