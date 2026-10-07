(() => {
  const getBar = () => document.getElementById('announcementBar');
  let frame = 0;

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
