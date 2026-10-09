(() => {
  const root = document.documentElement;
  root.dataset.xhUi = 'v6';

  const ready = () => {
    const body = document.body;
    if (!body) return;
    body.classList.add('xh-v6-ui');

    const page = body.dataset.page || location.pathname.replace(/^\//, '').split('/')[0] || 'home';
    body.dataset.v6Page = page;

    document.querySelectorAll('main').forEach((node) => node.classList.add('xh-v6-main'));
    document.querySelectorAll('.section').forEach((node) => node.classList.add('xh-v6-section'));
    document.querySelectorAll('.card').forEach((node) => node.classList.add('xh-v6-card'));
    document.querySelectorAll('.section-title,.section-title-rich').forEach((node) => node.classList.add('xh-v6-heading'));
    document.querySelectorAll('form').forEach((node) => node.classList.add('xh-v6-form'));

    const hero = document.getElementById('homeHero');
    if (hero) hero.classList.add('xh-v6-hero');
    const quick = document.getElementById('quick-actions');
    if (quick) quick.classList.add('xh-v6-conversion-zone');

    const normalize = (href) => {
      try {
        const url = new URL(href, location.origin);
        return url.pathname.replace(/\/$/, '') || '/';
      } catch (_) {
        return '';
      }
    };

    const here = location.pathname.replace(/\/$/, '') || '/';
    document.querySelectorAll('.site-header .nav a[href],.mobile-primary-nav a[href]').forEach((link) => {
      const target = normalize(link.getAttribute('href'));
      const active = target === '/' ? here === '/' : here === target || here.startsWith(target + '/');
      if (active) link.setAttribute('aria-current', 'page');
      else if (link.getAttribute('aria-current') === 'page') link.removeAttribute('aria-current');
    });

    document.querySelectorAll('input,select,textarea').forEach((field) => {
      field.addEventListener('focus', () => field.closest('.field,.contact-combo-field')?.classList.add('is-focused'));
      field.addEventListener('blur', () => field.closest('.field,.contact-combo-field')?.classList.remove('is-focused'));
    });
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', ready, { once:true });
  else ready();
})();
