(() => {
  'use strict';
  const hash = (location.hash || '#overview').slice(1).toLowerCase();
  const routes = {
    overview: '/account',
    profile: '/account/settings#profile',
    quotes: '/account/requests',
    requests: '/account/requests',
    orders: '/account/orders',
    progress: '/account/progress',
    security: '/account/settings#security',
    settings: '/account/settings#security',
    support: '/contact'
  };
  location.replace(routes[hash] || '/account');
})();
