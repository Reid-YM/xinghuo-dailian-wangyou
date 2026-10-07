(() => {
  const getBar = () => document.getElementById('announcementBar');
  let frame = 0;

  function ensureFloatingSupport() {
    if (document.getElementById('floatingCustomerService')) return;
    const link = document.createElement('a');
    link.id = 'floatingCustomerService';
    link.className = 'floating-customer-service';
    link.href = '/contact';
    link.setAttribute('aria-label', '联系客服');
    link.setAttribute('data-global-visible', 'navContactVisible');
    link.innerHTML = '<span class="floating-customer-service-icon" aria-hidden="true"><svg viewBox="0 0 24 24" focusable="false"><use href="/assets/icons.svg#i-support"></use></svg></span><span class="floating-customer-service-text">联系客服</span>';
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
