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

  function protectUserCenterFromAdminLinks() {
    if (!/^\/(account|console)(\/|$)/.test(location.pathname)) return;
    document.addEventListener('click', event => {
      const link = event.target instanceof Element ? event.target.closest('a[href]') : null;
      const target = String(link?.getAttribute('href') || '').trim();
      if (target === '/admin' || target.startsWith('/admin/')) {
        event.preventDefault();
        event.stopImmediatePropagation();
        location.assign('/account');
      }
    }, true);
  }

  function applyShared(settings = {}, site = {}) {
    [
      ['nav-services', settings.navServicesLabel], ['nav-guides', settings.navGuidesLabel],
      ['nav-mastery', settings.navMasteryLabel], ['nav-records', settings.navRecordsLabel],
      ['nav-contact', settings.navContactLabel], ['nav-account', settings.navAccountLabel],
      ['footer-services', settings.footerServicesLabel], ['footer-records', settings.footerRecordsLabel],
      ['footer-console', settings.footerConsoleLabel], ['footer-rules', settings.footerRulesLabel],
      ['footer-privacy', settings.footerPrivacyLabel], ['footer-contact', settings.footerContactLabel],
      ['icp-number', settings.icpNumber], ['public-security-number', settings.publicSecurityNumber]
    ].forEach(([key, value]) => text(`[data-global-ui="${key}"]`, value));

    const states = {
      navServicesVisible: bool(settings.navServicesVisible), navGuidesVisible: bool(settings.navGuidesVisible),
      navMasteryVisible: bool(settings.navMasteryVisible) && site.toolPublicEnabled !== false,
      navRecordsVisible: bool(settings.navRecordsVisible), navContactVisible: bool(settings.navContactVisible),
      navAccountVisible: bool(settings.navAccountVisible), headerQuoteVisible: bool(settings.headerQuoteVisible),
      footerServicesVisible: bool(settings.footerServicesVisible), footerRecordsVisible: bool(settings.footerRecordsVisible),
      footerConsoleVisible: bool(settings.footerConsoleVisible), footerRulesVisible: bool(settings.footerRulesVisible),
      footerPrivacyVisible: bool(settings.footerPrivacyVisible), footerContactVisible: bool(settings.footerContactVisible),
      icpVisible: bool(settings.icpVisible), publicSecurityVisible: bool(settings.publicSecurityVisible)
    };
    Object.entries(states).forEach(([key, show]) => visible(`[data-global-visible="${key}"]`, show));

    const police = String(settings.publicSecurityNumber || '').replace(/\D/g, '');
    const policeLink = document.querySelector('[data-global-ui-link="public-security"]');
    if (policeLink && police) policeLink.href = `https://www.beian.gov.cn/portal/registerSystemInfo?recordcode=${encodeURIComponent(police)}`;

    [
      ['/services', settings.navServicesLabel || '服务', states.navServicesVisible],
      ['/guides', settings.navGuidesLabel || '攻略', states.navGuidesVisible],
      ['/mastery', settings.navMasteryLabel || 'MW4 工具站', states.navMasteryVisible],
      ['/records', settings.navRecordsLabel || '业务记录', states.navRecordsVisible],
      ['/contact', settings.navContactLabel || '一对一咨询', states.navContactVisible],
      ['/account', settings.navAccountLabel || '个人中心', states.navAccountVisible]
    ].forEach(([path, label, show]) => {
      $$(`.nav-drawer a[href="${path}"]`).forEach(link => { link.textContent = label; link.hidden = !show; });
    });
    visible('.nav-drawer .nav-drawer-cta', states.headerQuoteVisible);
    visible('.quick-contact-card', bool(settings.homeQuickContactVisible));
    if (site.toolPublicEnabled === false) visible('a[href="/mastery"]', false);
  }

  function rgb(hex) {
    const match = /^#([0-9a-f]{6})$/i.exec(String(hex || '').trim());
    if (!match) return null;
    const value = Number.parseInt(match[1], 16);
    return `${value >> 16 & 255},${value >> 8 & 255},${value & 255}`;
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

  function applyHome(control = {}) {
    if (location.pathname !== '/') return;
    visible('#homeHero', bool(control.homeHeroVisible));
    text('#homeHero .hero-kicker', control.homeHeroKicker);
    visible('#homeHero .hero-promise', bool(control.homeHeroPromisesVisible));
    visible('#homeHero .hero-media-panel', bool(control.homeHeroMediaVisible));
    text('#homeHero [data-primary-cta]', control.homePrimaryCtaLabel);
    href('#homeHero [data-primary-cta]', control.homePrimaryCtaHref);

    const secondary = '#homeHero .hero-actions .btn.ghost:not(.hero-progress-link)';
    visible(secondary, bool(control.homeSecondaryCtaVisible));
    text(secondary, control.homeSecondaryCtaLabel);
    href(secondary, control.homeSecondaryCtaHref);
    visible('#homeHero .hero-progress-link', bool(control.homeProgressCtaVisible));
    text('#homeHero .hero-progress-link', control.homeProgressCtaLabel);
    href('#homeHero .hero-progress-link', control.homeProgressCtaHref);

    visible('.quote-card-home', bool(control.homeQuotePanelVisible));
    text('.quote-card-home .quote-card-head .eyebrow', control.homeQuoteEyebrow);
    text('.quote-card-home .quote-card-head h2', control.homeQuoteTitle);
    visible('.desktop-secondary-panel', bool(control.homeRecordPanelVisible));
    text('.desktop-secondary-panel .quote-card-head .eyebrow', control.homeRecordEyebrow);
    text('.desktop-secondary-panel .quote-card-head h2', control.homeRecordTitle);

    visible('#gameEntrySection', bool(control.homeGameEntryVisible));
    text('#gameEntrySection .game-entry-head .eyebrow', control.homeGameEyebrow);
    text('#gameEntrySection .game-entry-head h2', control.homeGameTitle);
    text('#gameEntrySection .game-entry-head .text-link', control.homeGameLinkLabel);
    href('#gameEntrySection .game-entry-head .text-link', control.homeGameLinkHref);

    const why = document.querySelector('main.container section[aria-label="为什么选择星火"]');
    if (why) {
      why.hidden = !bool(control.homeWhyVisible);
      text('.section-title .eyebrow', control.homeWhyEyebrow, why);
      text('.section-title h2', control.homeWhyTitle, why);
      text('.section-title .text-link', control.homeWhyLinkLabel, why);
      href('.section-title .text-link', control.homeWhyLinkHref, why);
    }
  }

  function applyAll(settings, site, control) {
    applyShared(settings || {}, site || {});
    applyTheme(control || {});
    applyHome(control || {});
  }

  async function load() {
    try {
      const [settingsResponse, siteResponse, controlsResponse] = await Promise.all([
        fetch('/api/public/site-settings', { cache: 'no-store' }),
        fetch('/api/site-config', { cache: 'no-store' }),
        fetch('/api/public/frontend-controls', { cache: 'no-store' })
      ]);
      const settings = settingsResponse.ok ? await settingsResponse.json() : {};
      const site = siteResponse.ok ? await siteResponse.json() : {};
      const controls = controlsResponse.ok ? await controlsResponse.json() : {};
      const run = () => applyAll(settings, site, controls);
      run();
      [120, 600, 1500].forEach(delay => setTimeout(run, delay));
    } catch (_) {
      // 配置读取失败时保留服务端模板默认值，不阻断主业务路径。
    }
  }

  protectUserCenterFromAdminLinks();
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load, { once: true });
  else load();
})();
