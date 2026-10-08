/* Shared image layout engine for public rendering. Admin previews consume the pure resolvers only. */
(function () {
  const number = (v, min, max, fallback) => Number.isFinite(Number(v)) && v !== '' && v != null
    ? Math.max(min, Math.min(max, Number(v))) : fallback;
  const fit = v => ['cover', 'contain', 'fill'].includes(String(v).toLowerCase()) ? String(v).toLowerCase() : 'cover';
  const set = (el, values) => Object.entries(values).forEach(([k, v]) => el.style.setProperty(k, String(v), 'important'));
  const isMobile = () => window.matchMedia && window.matchMedia('(max-width: 767px)').matches;

  function mobileFor(scope) {
    if (scope === 'mobile') return true;
    if (scope === 'desktop') return false;
    return isMobile();
  }

  function pick(c, desktopKey, mobileKey, scope) {
    const mobile = mobileFor(scope);
    const mobileValue = mobile ? c?.[mobileKey] : undefined;
    return mobileValue !== undefined && mobileValue !== null && mobileValue !== '' ? mobileValue : c?.[desktopKey];
  }

  function resolveBackground(c = {}, scope = 'auto') {
    const mobile = mobileFor(scope);
    const opacity = number(pick(c, 'heroBackgroundOpacity', 'heroBackgroundMobileOpacity', scope), 0, 100, 90) / 100;
    const blur = number(pick(c, 'heroBackgroundBlur', 'heroBackgroundMobileBlur', scope), 0, 24, 0);
    const resolvedFit = fit(pick(c, 'heroBackgroundFit', 'heroBackgroundMobileFit', scope));
    const positionX = number(pick(c, 'heroBackgroundPositionX', 'heroBackgroundMobilePositionX', scope), 0, 100, 50);
    const positionY = number(pick(c, 'heroBackgroundPositionY', 'heroBackgroundMobilePositionY', scope), 0, 100, 50);
    const dim = Math.max(.18, .58 - opacity * .4);
    return {
      mobile,
      opacity,
      blur,
      fit: resolvedFit,
      backgroundSize: resolvedFit === 'fill' ? '100% 100%' : resolvedFit,
      backgroundPosition: `${positionX}% ${positionY}%`,
      height: mobile ? `${number(c.heroBackgroundMobileHeight, 360, 760, 520)}px` : null,
      dim,
      filter: `brightness(${Math.max(1.02, Math.min(1.22, .98 + opacity * .22))}) saturate(1.04) blur(${blur}px)`
    };
  }

  function resolveMain(c = {}, scope = 'auto') {
    return {
      height: `${number(pick(c, 'heroImageHeight', 'heroImageMobileHeight', scope), 80, 420, 150)}px`,
      fit: fit(pick(c, 'heroImageFit', 'heroImageMobileFit', scope)),
      position: `${number(pick(c, 'heroImagePositionX', 'heroImageMobilePositionX', scope), 0, 100, 50)}% ${number(pick(c, 'heroImagePositionY', 'heroImageMobilePositionY', scope), 0, 100, 50)}%`
    };
  }

  function background(box, layer, c, url, scope = 'auto') {
    if (!box || !layer) return;
    const r = resolveBackground(c, scope);
    box.classList.toggle('has-background', !!url);
    box.classList.add('image-controlled-background');
    box.style.setProperty('--image-text-dim', String(r.dim));
    if (r.mobile) box.style.setProperty('--hero-mobile-background-height', r.height);
    else box.style.removeProperty('--hero-mobile-background-height');
    set(layer, {
      'background-image': url ? `url("${String(url).replace(/"/g, '%22')}")` : 'none',
      'background-size': r.backgroundSize,
      'background-position': r.backgroundPosition,
      'background-repeat': 'no-repeat',
      opacity: r.opacity,
      filter: r.filter,
      inset: '0',
      transform: 'none',
      transition: 'none'
    });
  }

  function main(box, img, c, scope = 'auto') {
    if (!box) return;
    const r = resolveMain(c, scope);
    set(box, { height: r.height, 'min-height': '0', 'max-height': 'none', 'aspect-ratio': 'auto', flex: '0 0 auto' });
    if (img) set(img, { 'object-fit': r.fit, 'object-position': r.position, width: '100%', height: '100%', 'min-height': '0' });
  }

  function serviceCoverVars(c, scope = 'auto') {
    const coverFit = fit(pick(c, 'serviceCoverFit', 'serviceCoverMobileFit', scope));
    return {
      height: `${number(pick(c, 'serviceCoverHeight', 'serviceCoverMobileHeight', scope), 70, 240, 105)}px`,
      fit: coverFit,
      position: `${number(pick(c, 'serviceCoverPositionX', 'serviceCoverMobilePositionX', scope), 0, 100, 50)}% ${number(pick(c, 'serviceCoverPositionY', 'serviceCoverMobilePositionY', scope), 0, 100, 50)}%`
    };
  }

  window.SiteImageControls = { resolveBackground, resolveMain, background, main, serviceCoverVars };
})();
