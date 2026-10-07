(() => {
  const getBar = () => document.getElementById('announcementBar');
  const brandLogoHref = '/assets/brand-logo.svg?v=20261008logo01';
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
