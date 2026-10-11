/* Shared image rendering for public pages and the admin preview. */
(function () {
  const number = (v, min, max, fallback) => Number.isFinite(Number(v)) && v !== '' && v != null ? Math.max(min, Math.min(max, Number(v))) : fallback;
  const fit = v => ['cover', 'contain', 'fill'].includes(String(v).toLowerCase()) ? String(v).toLowerCase() : 'cover';
  const set = (el, values) => Object.entries(values).forEach(([k,v]) => el.style.setProperty(k, String(v), 'important'));
  function background(box, layer, c, url) {
    if (!box || !layer) return;
    const opacity = number(c.heroBackgroundOpacity, 0, 100, 90) / 100;
    const dim = Math.max(.18, .58 - opacity * .4);
    box.classList.toggle('has-background', !!url);
    box.classList.add('image-controlled-background');
    box.style.setProperty('--image-text-dim', String(dim));
    set(layer, { 'background-image': url ? `url("${String(url).replace(/"/g, '%22')}")` : 'none',
      'background-size': fit(c.heroBackgroundFit) === 'fill' ? '100% 100%' : fit(c.heroBackgroundFit),
      'background-position': `${number(c.heroBackgroundPositionX,0,100,50)}% ${number(c.heroBackgroundPositionY,0,100,50)}%`,
      'background-repeat': 'no-repeat', opacity,
      filter: `brightness(${Math.max(1.02, Math.min(1.22, .98 + opacity * .22))}) saturate(1.04) blur(${number(c.heroBackgroundBlur,0,24,0)}px)`,
      inset: '0', transform: 'none', transition: 'none' });
  }
  function main(box, img, c) {
    if (!box) return;
    set(box, {height: `${number(c.heroImageHeight,80,420,150)}px`, 'min-height':'0', 'max-height':'none', 'aspect-ratio':'auto', flex:'0 0 auto'});
    if (img) set(img, {'object-fit':fit(c.heroImageFit), 'object-position':`${number(c.heroImagePositionX,0,100,50)}% ${number(c.heroImagePositionY,0,100,50)}%`, width:'100%', height:'100%', 'min-height':'0'});
  }
  window.SiteImageControls = { background, main };
})();
