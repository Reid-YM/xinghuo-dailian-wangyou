(() => {
  'use strict';

  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const bool = (value, fallback = true) => {
    if (value == null || value === '') return fallback;
    return String(value).toLowerCase() === 'true';
  };
  const setText = (selector, value) => {
    if (value == null || value === '') return;
    $$(selector).forEach(node => { node.textContent = value; });
  };
  const setVisible = (selector, visible) => {
    $$(selector).forEach(node => { node.hidden = !visible; });
  };
  const safeHref = value => {
    const href = String(value || '').trim();
    if (href.startsWith('#')) return href.length > 1 ? href : null;
    if (href.startsWith('/')) return href.startsWith('//') ? null : href;
    return href.startsWith('https://') ? href : null;
  };
  const setHref = (selector, value) => {
    const href = safeHref(value);
    if (!href) return;
    $$(selector).forEach(node => { node.setAttribute('href', href); });
  };

  function protectUserCenterFromAdminLinks() {
    if (!(location.pathname === '/account' || location.pathname.startsWith('/account/') || location.pathname === '/console' || location.pathname.startsWith('/console/'))) return;
    document.addEventListener('click', event => {
      const link = event.target instanceof Element ? event.target.closest('a[href]') : null;
      if (!link) return;
      const href = String(link.getAttribute('href') || '').trim();
      if (href === '/admin' || href.startsWith('/admin/')) {
        event.preventDefault();
        event.stopImmediatePropagation();
        location.assign('/account');
      }
    }, true);
  }

  function applySharedNavigation(settings = {}, site = {}) {
    const labels = [
      ['nav-services', settings.navServicesLabel],
      ['nav-guides', settings.navGuidesLabel],
      ['nav-mastery', settings.navMasteryLabel],
      ['nav-records', settings.navRecordsLabel],
      ['nav-contact', settings.navContactLabel],
      ['nav-account', settings.navAccountLabel],
      ['footer-services', settings.footerServicesLabel],
      ['footer-records', settings.footerRecordsLabel],
      ['footer-console', settings.footerConsoleLabel],
      ['footer-rules', settings.footerRulesLabel],
      ['footer-privacy', settings.footerPrivacyLabel],
      ['footer-contact', settings.footerContactLabel],
      ['icp-number', settings.icpNumber],
      ['public-security-number', settings.publicSecurityNumber]
    ];
    labels.forEach(([key, value]) => setText(`[data-global-ui="${key}"]`, value));

    const masteryVisible = bool(settings.navMasteryVisible, true) && site.toolPublicEnabled !== false;
    const visibility = {
      navServicesVisible: bool(settings.navServicesVisible, true),
      navGuidesVisible: bool(settings.navGuidesVisible, true),
      navMasteryVisible: masteryVisible,
      navRecordsVisible: bool(settings.navRecordsVisible, true),
      navContactVisible: bool(settings.navContactVisible, true),
      navAccountVisible: bool(settings.navAccountVisible, true),
      headerQuoteVisible: bool(settings.headerQuoteVisible, true),
      footerServicesVisible: bool(settings.footerServicesVisible, true),
      footerRecordsVisible: bool(settings.footerRecordsVisible, true),
      footerConsoleVisible: bool(settings.footerConsoleVisible, true),
      footerRulesVisible: bool(settings.footerRulesVisible, true),
      footerPrivacyVisible: bool(settings.footerPrivacyVisible, true),
      footerContactVisible: bool(settings.footerContactVisible, true),
      icpVisible: bool(settings.icpVisible, true),
      publicSecurityVisible: bool(settings.publicSecurityVisible, true)
    };
    Object.entries(visibility).forEach(([key, visible]) => setVisible(`[data-global-visible="${key}"]`, visible));

    const police = String(settings.publicSecurityNumber || '').trim();
    const policeLink = document.querySelector('[data-global-ui-link="public-security"]');
    const digits = police.replace(/\D/g, '');
    if (policeLink && digits) {
      policeLink.href = `https://www.beian.gov.cn/portal/registerSystemInfo?recordcode=${encodeURIComponent(digits)}`;
    }

    const drawerRules = [
      ['/services', settings.navServicesLabel || '服务', visibility.navServicesVisible],
      ['/guides', settings.navGuidesLabel || '攻略', visibility.navGuidesVisible],
      ['/mastery', settings.navMasteryLabel || 'MW4 工具站', visibility.navMasteryVisible],
      ['/records', settings.navRecordsLabel || '业务记录', visibility.navRecordsVisible],
      ['/contact', settings.navContactLabel || '一对一咨询', visibility.navContactVisible],
      ['/account', settings.navAccountLabel || '个人中心', visibility.navAccountVisible]
    ];
    drawerRules.forEach(([href, label, visible]) => {
      $$(`.nav-drawer a[href="${href}"]`).forEach(link => {
        link.textContent = label;
        link.hidden = !visible;
      });
    });
    setVisible('.nav-drawer .nav-drawer-cta', visibility.headerQuoteVisible);
  }

  function applyPageControls(settings = {}, site = {}) {
    setVisible('.quick-contact-card', bool(settings.homeQuickContactVisible, true));
    if (site.toolPublicEnabled === false) setVisible('a[href="/mastery"]', false);
  }

  function hexToRgb(hex) {
    const match = /^#([0-9a-f]{6})$/i.exec(String(hex || '').trim());
    if (!match) return null;
    const value = Number.parseInt(match[1], 16);
    return `${(value >> 16) & 255},${(value >> 8) & 255},${value & 255}`;
  }

  function applyTheme(controls = {}) {
    const root = document.documentElement;
    const accent = String(controls.themeAccentColor || '').trim();
    const accentRgb = hexToRgb(accent);
    if (accentRgb) {
      root.style.setProperty('--accent-rgb', accentRgb);
      root.style.setProperty('--accent', accent);
      root.style.setProperty('--accent-hover', accent);
      root.style.setProperty('--accent-text', accent);
      root.style.setProperty('--yellow', accent);
      root.style.setProperty('--yellow-2', accent);
    }
    const background = String(controls.themeBackgroundColor || '').trim();
    const surface = String(controls.themeSurfaceColor || '').trim();
    const text = String(controls.themeTextColor || '').trim();
    if (hexToRgb(background)) root.style.setProperty('--bg', background);
    if (hexToRgb(surface)) root.style.setProperty('--surface-2', surface);
    if (hexToRgb(text)) root.style.setProperty('--text', text);

    const radius = Number(controls.themeRadius);
    if (Number.isFinite(radius) && radius >= 6 && radius <= 30) root.style.setProperty('--radius', `${radius}px`);
    const width = Number(controls.themeContentWidth);
    if (Number.isFinite(width) && width >= 960 && width <= 1440) root.style.setProperty('--max', `${width}px`);
    const scale = Number(controls.themeFontScale);
    if (Number.isFinite(scale) && scale >= 90 && scale <= 115) root.style.fontSize = `${scale}%`;

    if (!document.getElementById('frontendMotionControlStyle')) {
      const style = document.createElement('style');
      style.id = 'frontendMotionControlStyle';
      style.textContent = '.frontend-motion-off *, .frontend-motion-off *::before, .frontend-motion-off *::after{animation-duration:.001ms!important;animation-iteration-count:1!important;transition-duration:.001ms!important;scroll-behavior:auto!important}';
      document.head.appendChild(style);
    }
    root.classList.toggle('frontend-motion-off', !bool(controls.motionEnabled, true));
  }

  function applyHomeControls(controls = {}) {
    if (location.pathname !== '/') return;

    setVisible('#homeHero', bool(controls.homeHeroVisible, true));
    setText('#homeHero .hero-kicker', controls.homeHeroKicker);
    setVisible('#homeHero .hero-promise', bool(controls.homeHeroPromisesVisible, true));
    setVisible('#homeHero .hero-media-panel', bool(controls.homeHeroMediaVisible, true));

    setText('#homeHero [data-primary-cta]', controls.homePrimaryCtaLabel);
    setHref('#homeHero [data-primary-cta]', controls.homePrimaryCtaHref);

    const secondarySelector = '#homeHero .hero-actions .btn.ghost:not(.hero-progress-link)';
    setVisible(secondarySelector, bool(controls.homeSecondaryCtaVisible, true));
    setText(secondarySelector, controls.homeSecondaryCtaLabel);
    setHref(secondarySelector, controls.homeSecondaryCtaHref);

    setVisible('#homeHero .hero-progress-link', bool(controls.homeProgressCtaVisible, true));
    setText('#homeHero .hero-progress-link', controls.homeProgressCtaLabel);
    setHref('#homeHero .hero-progress-link', controls.homeProgressCtaHref);

    setVisible('.quote-card-home', bool(controls.homeQuotePanelVisible, true));
    setText('.quote-card-home .quote-card-head .eyebrow', controls.homeQuoteEyebrow);
    setText('.quote-card-home .quote-card-head h2', controls.homeQuoteTitle);

    setVisible('.desktop-secondary-panel', bool(controls.homeRecordPanelVisible, true));
    setText('.desktop-secondary-panel .quote-card-head .eyebrow', controls.homeRecordEyebrow);
    setText('.desktop-secondary-panel .quote-card-head h2', controls.homeRecordTitle);

    setVisible('#gameEntrySection', bool(controls.homeGameEntryVisible, true));
    setText('#gameEntrySection .game-entry-head .eyebrow', controls.homeGameEyebrow);
    setText('#gameEntrySection .game-entry-head h2', controls.homeGameTitle);
    setText('#gameEntrySection .game-entry-head .text-link', controls.homeGameLinkLabel);
    setHref('#gameEntrySection .game-entry-head .text-link', controls.homeGameLinkHref);

    const why = document.querySelector('main.container section[aria-label="为什么选择星火"]');
    if (why) {
      why.hidden = !bool(controls.homeWhyVisible, true);
      setText('.section-title .eyebrow', controls.homeWhyEyebrow, why);
      setText('.section-title h2', controls.homeWhyTitle, why);
      setText('.section-title .text-link', controls.homeWhyLinkLabel, why);
      const href = safeHref(controls.homeWhyLinkHref);
      if (href) why.querySelectorAll('.section-title .text-link').forEach(link => link.setAttribute('href', href));
    }
  }

  function applyFrontendControls(controls = {}) {
    applyTheme(controls);
    applyHomeControls(controls);
  }

  async function load() {
    try {
      const [settingsResponse, siteResponse, frontendResponse] = await Promise.all([
        fetch('/api/public/site-settings', { cache: 'no-store' }),
        fetch('/api/site-config', { cache: 'no-store' }),
        fetch('/api/public/frontend-controls', { cache: 'no-store' })
      ]);
      const settings = settingsResponse.ok ? await settingsResponse.json() : {};
      const site = siteResponse.ok ? await siteResponse.json() : {};
      const controls = frontendResponse.ok ? await frontendResponse.json() : {};

      const applyAll = () => {
        applySharedNavigation(settings || {}, site || {});
        applyPageControls(settings || {}, site || {});
        applyFrontendControls(controls || {});
      };
      applyAll();
      setTimeout(applyAll, 120);
      setTimeout(applyAll, 600);
      setTimeout(applyAll, 1500);
    } catch (_) {
      // 后台配置接口异常时保留模板默认值，不影响站点主流程。
    }
  }

  protectUserCenterFromAdminLinks();
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load, { once: true });
  else load();
})();
