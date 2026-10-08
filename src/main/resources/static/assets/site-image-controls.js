/* Shared image rendering for public pages and the admin preview. */
(function () {
  const number = (v, min, max, fallback) => Number.isFinite(Number(v)) && v !== '' && v != null ? Math.max(min, Math.min(max, Number(v))) : fallback;
  const fit = v => ['cover', 'contain', 'fill'].includes(String(v).toLowerCase()) ? String(v).toLowerCase() : 'cover';
  const set = (el, values) => Object.entries(values).forEach(([k,v]) => el.style.setProperty(k, String(v), 'important'));
  const isMobile = () => window.matchMedia && window.matchMedia('(max-width: 767px)').matches;
  function value(c, key, mobileKey) {
    const v = (isMobile() && c[mobileKey] !== undefined && c[mobileKey] !== null && c[mobileKey] !== '') ? c[mobileKey] : c[key];
    return v;
  }
  function scoped(c, scope) {
    if (scope === 'mobile') return true;
    if (scope === 'desktop') return false;
    return isMobile();
  }
  function pick(c, key, mobileKey, scope) {
    const mobile = scoped(c, scope);
    return (mobile && c[mobileKey] !== undefined && c[mobileKey] !== null && c[mobileKey] !== '') ? c[mobileKey] : c[key];
  }
  function background(box, layer, c, url) {
    if (!box || !layer) return;
    const mobile = isMobile();
    const opacity = number(c.heroBackgroundOpacity, 0, 100, 90) / 100;
    const dim = Math.max(.18, .58 - opacity * .4);
    box.classList.toggle('has-background', !!url);
    box.classList.add('image-controlled-background');
    box.style.setProperty('--image-text-dim', String(dim));
    if (mobile) {
      box.style.setProperty('--hero-mobile-background-height', `${number(c.heroBackgroundMobileHeight,360,760,520)}px`);
    } else {
      box.style.removeProperty('--hero-mobile-background-height');
    }
    const bgFit = fit(value(c, 'heroBackgroundFit', 'heroBackgroundMobileFit'));
    set(layer, { 'background-image': url ? `url("${String(url).replace(/"/g, '%22')}")` : 'none',
      'background-size': bgFit === 'fill' ? '100% 100%' : bgFit,
      'background-position': `${number(value(c,'heroBackgroundPositionX','heroBackgroundMobilePositionX'),0,100,50)}% ${number(value(c,'heroBackgroundPositionY','heroBackgroundMobilePositionY'),0,100,50)}%`,
      'background-repeat': 'no-repeat', opacity,
      filter: `brightness(${Math.max(1.02, Math.min(1.22, .98 + opacity * .22))}) saturate(1.04) blur(${number(c.heroBackgroundBlur,0,24,0)}px)`,
      inset: '0', transform: 'none', transition: 'none' });
  }
  function main(box, img, c) {
    if (!box) return;
    set(box, {height: `${number(value(c,'heroImageHeight','heroImageMobileHeight'),80,420,150)}px`, 'min-height':'0', 'max-height':'none', 'aspect-ratio':'auto', flex:'0 0 auto'});
    if (img) set(img, {'object-fit':fit(value(c,'heroImageFit','heroImageMobileFit')), 'object-position':`${number(value(c,'heroImagePositionX','heroImageMobilePositionX'),0,100,50)}% ${number(value(c,'heroImagePositionY','heroImageMobilePositionY'),0,100,50)}%`, width:'100%', height:'100%', 'min-height':'0'});
  }
  function serviceCoverVars(c, scope) {
    const coverFit = fit(pick(c, 'serviceCoverFit', 'serviceCoverMobileFit', scope));
    return {
      height: `${number(pick(c, 'serviceCoverHeight', 'serviceCoverMobileHeight', scope),70,240,105)}px`,
      fit: coverFit,
      position: `${number(pick(c,'serviceCoverPositionX','serviceCoverMobilePositionX',scope),0,100,50)}% ${number(pick(c,'serviceCoverPositionY','serviceCoverMobilePositionY',scope),0,100,50)}%`
    };
  }
  window.SiteImageControls = { background, main, serviceCoverVars };
})();
