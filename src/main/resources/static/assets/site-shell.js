(() => {
  const getBar = () => document.getElementById('announcementBar');
  const brandLogoHref = '/assets/brand-logo.svg?v=20261008logo01';
  const mobileMedia = '(max-width: 760px)';
  let frame = 0;

  function ensureBrandAssets() {
    if (!document.head) return;
    let icon = document.head.querySelector('link[data-xh-brand-favicon]');
    if (!icon) {
      icon = document.createElement('link');
      icon.rel = 'icon';
      icon.type = 'image/svg+xml';
      icon.setAttribute('sizes', 'any');
      icon.setAttribute('data-xh-brand-favicon', 'true');
      document.head.appendChild(icon);
    }
    icon.href = brandLogoHref;
  }

  function ensureStylesheet(href, key) {
    if (!document.head) return;
    if (document.head.querySelector(`link[data-${key}]`)) return;
    if ([...document.styleSheets].some(sheet => String(sheet.href || '').includes(href.split('?')[0]))) return;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = href;
    link.setAttribute(`data-${key}`, 'true');
    document.head.appendChild(link);
  }

  function ensureMobileFrontend() {
    const body = document.body;
    if (!body) return;
    const root = document.documentElement;
    const forcedMobile = root.dataset.frontend === 'mobile' || body.dataset.frontend === 'mobile';
    const compactViewport = typeof window.matchMedia === 'function'
      ? window.matchMedia(mobileMedia).matches
      : window.innerWidth <= 760;
    if (!forcedMobile && !compactViewport) return;

    root.dataset.frontend = 'mobile';
    body.dataset.frontend = 'mobile';
    body.classList.add('mobile-frontend');

    ensureStylesheet('/assets/mobile-app.css?v=20261009m02', 'xh-mobile-app-css');
    ensureStylesheet('/assets/mobile-pages-v1.css?v=20261009m04', 'xh-mobile-pages-css');
    ensureStylesheet('/assets/mobile-flows-v1.css?v=20261009m01', 'xh-mobile-flows-css');
    ensureStylesheet('/assets/mobile-content-v1.css?v=20261009m01', 'xh-mobile-content-css');
    ensureStylesheet('/assets/mobile-account-v1.css?v=20261009m01', 'xh-mobile-account-css');
    ensureStylesheet('/assets/mobile-tune-v1.css?v=20261009m01', 'xh-mobile-tune-css');

    const existingScript = document.querySelector('script[src*="/assets/mobile-app.js"]');
    if (!existingScript) {
      const script = document.createElement('script');
      script.src = '/assets/mobile-app.js?v=20261009m02';
      script.async = false;
      script.setAttribute('data-xh-mobile-app-js', 'true');
      document.body.appendChild(script);
    }
  }

  function ensureFloatingSupport() {
    if (document.getElementById('floatingCustomerService')) return;
    const link = document.createElement('a');
    link.id = 'floatingCustomerService';
    link.className = 'floating-customer-service';
    link.href = '/contact';
    link.setAttribute('aria-label', '联系客服');
    link.setAttribute('data-global-visible', 'navContactVisible');
    link.innerHTML = '<span class="floating-customer-service-icon" aria-hidden="true"><svg viewBox="0 0 24 24" focusable="false" style="width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round"><use href="/assets/icons.svg#i-support"></use></svg></span><span class="floating-customer-service-text">联系客服</span>';
    link.style.setProperty('background', '#07c160', 'important');
    link.style.setProperty('color', '#fff', 'important');
    link.style.setProperty('border-color', 'rgba(7,193,96,.62)', 'important');
    link.style.setProperty('box-shadow', '0 14px 34px rgba(0,0,0,.28), 0 8px 24px rgba(7,193,96,.20)', 'important');
    const icon = link.querySelector('.floating-customer-service-icon');
    if (icon) {
      icon.style.setProperty('background', '#05984b', 'important');
      icon.style.setProperty('color', '#fff', 'important');
    }
    document.body.appendChild(link);
  }

  function evaluateMode(bar) {
    const first = bar?.querySelector('.announcement-ticker span:not([aria-hidden="true"])');
    if (!bar || !first) return;
    const shouldScroll = first.getBoundingClientRect().width > Math.max(320, bar.clientWidth * 0.72);
    bar.classList.toggle('announcement-static', !shouldScroll);
  }

  function enhanceAnnouncement() {
    const bar = getBar();
    if (!bar || bar.classList.contains('hidden')) return;
    if (bar.querySelector('.announcement-ticker')) {
      evaluateMode(bar);
      return;
    }

    const text = String(bar.textContent || '').trim();
    if (!text) return;
    bar.textContent = '';
    bar.setAttribute('aria-label', text);

    const track = document.createElement('div');
    track.className = 'announcement-ticker';
    const primary = document.createElement('span');
    primary.textContent = text;
    const duplicate = document.createElement('span');
    duplicate.textContent = text;
    duplicate.setAttribute('aria-hidden', 'true');
    track.append(primary, duplicate);
    bar.append(track);
    requestAnimationFrame(() => evaluateMode(bar));
  }

  function scheduleEnhance() {
    cancelAnimationFrame(frame);
    frame = requestAnimationFrame(enhanceAnnouncement);
  }

  function start() {
    ensureBrandAssets();
    ensureMobileFrontend();
    ensureFloatingSupport();
    const bar = getBar();
    if (!bar) return;
    enhanceAnnouncement();
    const observer = new MutationObserver(scheduleEnhance);
    observer.observe(bar, { childList: true, subtree: true, characterData: true, attributes: true, attributeFilter: ['class'] });
    window.addEventListener('resize', scheduleEnhance, { passive: true });
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start, { once: true });
  else start();
})();