(() => {
  const $all = (selector, root = document) => [...root.querySelectorAll(selector)];
  const text = (selector, value) => {
    if (!value) return;
    $all(selector).forEach((node) => { node.textContent = value; });
  };
  const bool = (value, fallback = true) => {
    if (value == null || value === '') return fallback;
    return String(value).toLowerCase() === 'true';
  };

  function removeEmbeddedCustomerServiceEntries() {
    // 产品规则：普通页面只保留公共页脚里的悬浮在线客服。
    // /contact 与 /support/* 作为历史兼容和悬浮客服的分类落地页继续保留自身内容。
    const path = location.pathname || '/';
    if (path === '/contact' || path === '/support' || path.startsWith('/support/')) return;

    $all('.quick-contact-card,[data-embedded-customer-service]').forEach((node) => node.remove());
    $all('a[href="/contact"],a[href^="/support/"]').forEach((link) => {
      if (link.closest('.native-customer-service')) return;
      link.remove();
    });
  }

  function applyGlobalSettings(settings = {}) {
    text('[data-global-ui="nav-services"]', settings.navServicesLabel);
    text('[data-global-ui="nav-guides"]', settings.navGuidesLabel);
    text('[data-global-ui="nav-mastery"]', settings.navMasteryLabel);
    text('[data-global-ui="nav-records"]', settings.navRecordsLabel);
    text('[data-global-ui="nav-account"]', settings.navAccountLabel);
    text('[data-global-ui="footer-rules"]', settings.footerRulesLabel);
    text('[data-global-ui="footer-privacy"]', settings.footerPrivacyLabel);
    text('[data-global-ui="customer-service-label"]', settings.floatingCustomerServiceLabel);
    text('[data-global-ui="customer-service-title"]', settings.floatingCustomerServiceTitle);
    text('[data-global-ui="customer-service-intro"]', settings.floatingCustomerServiceIntro);

    const floating = document.querySelector('.native-customer-service');
    if (floating) floating.hidden = !bool(settings.floatingCustomerServiceEnabled, true);

    const hours = settings.serviceHours || '10:00–24:00';
    const safety = settings.floatingCustomerServiceNote || '请勿发送密码、验证码或令牌';
    text('[data-global-ui="customer-service-note"]', `客服时间 ${hours} · ${safety}`);

    const icp = settings.icpNumber;
    if (icp) text('[data-global-ui="icp-number"]', icp);
    const police = settings.publicSecurityNumber;
    if (police) {
      text('[data-global-ui="public-security-number"]', police);
      const digits = String(police).replace(/\D/g, '');
      const link = document.querySelector('[data-global-ui-link="public-security"]');
      if (link && digits) link.href = `https://www.beian.gov.cn/portal/registerSystemInfo?recordcode=${encodeURIComponent(digits)}`;
    }
  }

  function applySiteConfig(config = {}) {
    if (config.primaryButtonLabel) {
      $all('[data-primary-cta]').forEach((node) => { node.textContent = config.primaryButtonLabel; });
    }
    if (config.statusShortText) {
      text('[data-config="status"]', config.statusShortText);
    }
    if (config.toolPublicEnabled === false) {
      $all('a[href="/mastery"]').forEach((link) => {
        if (!link.closest('main[data-page="mw4-camo-tracker"]')) link.hidden = true;
      });
    }
  }

  async function load() {
    removeEmbeddedCustomerServiceEntries();
    const requests = [
      fetch('/api/public/site-settings', { cache: 'no-store' }).then((r) => r.ok ? r.json() : {}),
      fetch('/api/site-config', { cache: 'no-store' }).then((r) => r.ok ? r.json() : {})
    ];
    try {
      const [settings, config] = await Promise.all(requests);
      applyGlobalSettings(settings || {});
      applySiteConfig(config || {});
    } catch (_) {
      // 配置接口异常时保持模板默认值，不影响页面主流程。
    }
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load, { once: true });
  else load();
})();
