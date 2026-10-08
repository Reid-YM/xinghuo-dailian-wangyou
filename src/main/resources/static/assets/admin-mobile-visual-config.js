/* Admin-only controls for mobile background opacity/blur stored outside site_config. */
(() => {
  const section = document.querySelector('[data-config-section="visual"]');
  const form = document.getElementById('configForm');
  if (!section || !form) return;

  const csrf = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || '';
  let saved = null;
  let dirty = false;

  const clamp = (value, min, max, fallback) => {
    const n = Number(value);
    return Number.isFinite(n) ? Math.max(min, Math.min(max, n)) : fallback;
  };

  async function api(url, options = {}) {
    const method = String(options.method || 'GET').toUpperCase();
    const headers = { ...(options.headers || {}) };
    if (options.body != null) headers['Content-Type'] = 'application/json';
    if (csrf && csrfHeader && !['GET', 'HEAD'].includes(method)) headers[csrfHeader] = csrf;
    const response = await fetch(url, { ...options, method, headers });
    let payload = null;
    try { payload = await response.json(); } catch (_) {}
    if (!response.ok) throw new Error(payload?.message || '手机背景参数请求失败');
    return payload;
  }

  function installControls() {
    if (document.getElementById('heroBackgroundMobileOpacityIndependent')) return;

    const desktopOpacity = form.elements.namedItem('heroBackgroundOpacity');
    const desktopBlur = form.elements.namedItem('heroBackgroundBlur');
    const opacityField = desktopOpacity?.closest('.field');
    const blurField = desktopBlur?.closest('.field');
    const opacityLabel = opacityField?.querySelector('label');
    const blurLabel = blurField?.querySelector('label');
    if (opacityLabel) opacityLabel.textContent = '电脑背景显示强度（%）';
    if (blurLabel) blurLabel.textContent = '电脑背景模糊（px）';
    const sharedHelp = opacityField?.querySelector('.form-help');
    if (sharedHelp) sharedHelp.textContent = '电脑端独立参数；手机强度与模糊在“手机端背景”中单独保存。';

    const mobileHeight = form.elements.namedItem('heroBackgroundMobileHeight');
    const mobilePanel = mobileHeight?.closest('.responsive-visual-grid > div');
    if (!mobilePanel) return;

    const controls = document.createElement('div');
    controls.className = 'mobile-independent-visual-controls';
    controls.innerHTML = `
      <div class="grid2">
        <div class="field">
          <label for="heroBackgroundMobileOpacityIndependent">手机背景显示强度（%）</label>
          <input id="heroBackgroundMobileOpacityIndependent" type="number" min="0" max="100" step="1" inputmode="numeric">
        </div>
        <div class="field">
          <label for="heroBackgroundMobileBlurIndependent">手机背景模糊（px）</label>
          <input id="heroBackgroundMobileBlurIndependent" type="number" min="0" max="24" step="1" inputmode="numeric">
        </div>
      </div>
      <div class="row-actions">
        <button type="button" class="btn small-btn" id="saveMobileBackgroundVisual">保存手机背景参数</button>
        <span class="form-help" id="mobileBackgroundVisualState" role="status">读取中…</span>
      </div>
      <div class="form-help">这两项只影响手机端；首次未单独保存时自动继承当前电脑端强度与模糊。</div>`;
    mobilePanel.appendChild(controls);

    const opacity = document.getElementById('heroBackgroundMobileOpacityIndependent');
    const blur = document.getElementById('heroBackgroundMobileBlurIndependent');
    const save = document.getElementById('saveMobileBackgroundVisual');

    [opacity, blur].forEach(input => input?.addEventListener('input', () => {
      dirty = true;
      updateState('手机参数有未保存修改', 'dirty');
    }));
    save?.addEventListener('click', saveConfig);
  }

  function updateState(text, tone = '') {
    const state = document.getElementById('mobileBackgroundVisualState');
    if (!state) return;
    state.textContent = text;
    state.dataset.state = tone;
  }

  function current() {
    const opacity = document.getElementById('heroBackgroundMobileOpacityIndependent');
    const blur = document.getElementById('heroBackgroundMobileBlurIndependent');
    return {
      heroBackgroundMobileOpacity: clamp(opacity?.value, 0, 100, 90),
      heroBackgroundMobileBlur: clamp(blur?.value, 0, 24, 0)
    };
  }

  function setValues(config) {
    const opacity = document.getElementById('heroBackgroundMobileOpacityIndependent');
    const blur = document.getElementById('heroBackgroundMobileBlurIndependent');
    if (!opacity || !blur) return;
    opacity.value = clamp(config?.heroBackgroundMobileOpacity, 0, 100,
      clamp(form.elements.namedItem('heroBackgroundOpacity')?.value, 0, 100, 90));
    blur.value = clamp(config?.heroBackgroundMobileBlur, 0, 24,
      clamp(form.elements.namedItem('heroBackgroundBlur')?.value, 0, 24, 0));
    saved = JSON.stringify(current());
    dirty = false;
    updateState('手机参数已同步', 'saved');
    applyPreview();
  }

  function applyPreview() {
    const box = document.getElementById('heroBackgroundLivePreview');
    const layer = box?.querySelector('.admin-hero-bg-live-layer');
    if (!box || !layer || box.dataset.device !== 'mobile') return;
    const values = current();
    const opacity = values.heroBackgroundMobileOpacity / 100;
    const blur = values.heroBackgroundMobileBlur;
    const dim = Math.max(.18, .58 - opacity * .4);
    const brightness = Math.max(1.02, Math.min(1.22, .98 + opacity * .22));
    layer.style.setProperty('opacity', String(opacity), 'important');
    layer.style.setProperty('filter', `brightness(${brightness}) saturate(1.04) blur(${blur}px)`, 'important');
    box.style.setProperty('--image-text-dim', String(dim));
  }

  async function loadConfig(force = false) {
    if (dirty && !force) return;
    try {
      const config = await api('/api/admin/mobile-visual-config');
      setValues(config);
    } catch (error) {
      updateState(error.message, 'error');
    }
  }

  async function saveConfig(event) {
    const button = event?.currentTarget;
    if (button) {
      button.disabled = true;
      button.dataset.oldText = button.textContent;
      button.textContent = '保存中…';
    }
    try {
      const values = current();
      const reason = document.getElementById('configSectionReason')?.value || '后台手机背景参数保存';
      const config = await api('/api/admin/mobile-visual-config', {
        method: 'PATCH',
        body: JSON.stringify({ ...values, reason })
      });
      setValues(config);
    } catch (error) {
      updateState(error.message, 'error');
    } finally {
      if (button) {
        button.disabled = false;
        button.textContent = button.dataset.oldText || '保存手机背景参数';
        delete button.dataset.oldText;
      }
    }
  }

  installControls();

  section.addEventListener('input', event => {
    if (event.target?.id === 'heroBackgroundMobileOpacityIndependent'
        || event.target?.id === 'heroBackgroundMobileBlurIndependent') {
      queueMicrotask(applyPreview);
    }
  });
  document.addEventListener('visual-device-changed', () => queueMicrotask(applyPreview));
  document.addEventListener('site-image-updated', () => queueMicrotask(applyPreview));
  document.addEventListener('site-config-loaded', () => loadConfig(false));

  document.getElementById('autoFitHeroBackground')?.addEventListener('click', () => {
    const opacity = document.getElementById('heroBackgroundMobileOpacityIndependent');
    const blur = document.getElementById('heroBackgroundMobileBlurIndependent');
    if (!opacity || !blur) return;
    opacity.value = '90';
    blur.value = '0';
    dirty = true;
    updateState('手机参数有未保存修改', 'dirty');
    queueMicrotask(applyPreview);
  });

  window.addEventListener('beforeunload', event => {
    if (dirty && saved !== JSON.stringify(current())) {
      event.preventDefault();
      event.returnValue = '';
    }
  });

  loadConfig(true);
})();
