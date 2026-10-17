(() => {
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  let currentSettings = null;
  let currentSite = null;
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
  const serviceEntryHref = settings => String(settings?.servicesEntryMode || 'legacy').toLowerCase() === 'next'
    ? '/next/services'
    : '/services';

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

    const serviceHref = serviceEntryHref(settings);
    $$('[data-global-ui="nav-services"], [data-global-ui="footer-services"]').forEach(link => {
      if (link instanceof HTMLAnchorElement) link.setAttribute('href', serviceHref);
    });

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

    // The drawer can be rendered after settings load, so match both possible rollout targets.
    $$('.nav-drawer a[href="/services"], .nav-drawer a[href="/next/services"]').forEach(link => {
      link.setAttribute('href', serviceHref);
      link.textContent = settings.navServicesLabel || '服务';
      link.hidden = !visibility.navServicesVisible;
    });

    const drawerRules = [
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
    const quickContactVisible = bool(settings.homeQuickContactVisible, true);
    setVisible('.quick-contact-card', quickContactVisible);

    if (site.toolPublicEnabled === false) {
      setVisible('a[href="/mastery"]', false);
    }
  }

  function reapplySharedNavigation() {
    if (!currentSettings || !currentSite) return;
    applySharedNavigation(currentSettings, currentSite);
  }

  async function load() {
    try {
      const [settingsResponse, siteResponse] = await Promise.all([
        fetch('/api/public/site-settings', { cache: 'no-store' }),
        fetch('/api/site-config', { cache: 'no-store' })
      ]);
      currentSettings = settingsResponse.ok ? await settingsResponse.json() : {};
      currentSite = siteResponse.ok ? await siteResponse.json() : {};
      reapplySharedNavigation();
      applyPageControls(currentSettings, currentSite);
    } catch (_) {
      // 后台配置接口异常时保留模板默认值，不影响站点主流程。
    }
  }

  protectUserCenterFromAdminLinks();
  document.addEventListener('xh:mobile-nav-rendered', reapplySharedNavigation);
  window.addEventListener('xh:site-config', event => {
    currentSite = event.detail || {};
    reapplySharedNavigation();
    applyPageControls(currentSettings || {}, currentSite);
  });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load, { once: true });
  else load();
})();
