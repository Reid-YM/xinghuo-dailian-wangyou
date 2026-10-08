(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  let configPromise = null;
  let currentConfig = null;

  function replaceAllCompat(value, search, replacement) {
    return String(value ?? '').split(search).join(replacement);
  }

  function makeUuid() {
    if (globalThis.crypto && typeof globalThis.crypto.randomUUID === 'function') return globalThis.crypto.randomUUID();
    const bytes = new Uint8Array(16);
    if (globalThis.crypto && typeof globalThis.crypto.getRandomValues === 'function') globalThis.crypto.getRandomValues(bytes);
    else for (let i = 0; i < bytes.length; i += 1) bytes[i] = Math.floor(Math.random() * 256);
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = [...bytes].map(x => x.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`;
  }

  function esc(value) {
    return String(value ?? '').replace(/[&<>'"]/g, c => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    }[c]));
  }

  function safeUrl(value) {
    if (!value) return null;
    try {
      const url = new URL(value, location.origin);
      return ['http:', 'https:'].includes(url.protocol) ? url.href : null;
    } catch {
      return null;
    }
  }

  function contactJumpUrl(value) {
    const raw = String(value || '').trim();
    if (!raw) return null;
    if (/^(weixin|wechat):\/\//i.test(raw)) return raw;
    /* 只有真正的绝对链接才算跳转；微信号（如 AAC9878）不能被当成相对地址解析成 /AAC9878。 */
    if (!/^https?:\/\//i.test(raw)) return null;
    return safeUrl(raw);
  }

  function contactDisplayValue(value) {
    const raw = String(value || '').trim();
    return contactJumpUrl(raw) ? '点击打开微信' : raw;
  }

  async function api(url, opt = {}) {
    const hasBody = opt.body !== undefined && opt.body !== null;
    const isForm = opt.body instanceof FormData;
    const headers = {
      ...(hasBody && !isForm ? { 'Content-Type': 'application/json' } : {}),
      ...(opt.headers || {})
    };
    const response = await fetch(url, { ...opt, headers });
    if (response.status === 204) return null;

    let payload = null;
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      try { payload = await response.json(); } catch { payload = null; }
    } else {
      try { payload = await response.text(); } catch { payload = null; }
    }

    if (!response.ok) {
      const message = payload && typeof payload === 'object'
        ? payload.message
        : (typeof payload === 'string' && payload.trim() ? payload : '请求失败，请稍后重试');
      throw new Error(message || '请求失败，请稍后重试');
    }
    return payload;
  }

  function track(eventName, refValue = null) {
    const body = JSON.stringify({ eventName, path: location.pathname, refValue });
    fetch('/api/track', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body, keepalive: true
    }).catch(() => {});
  }

  // V22 P1-07: record quote source for promotion channel measurement
  function urlParam(name) {
    try { var v = new URLSearchParams(window.location.search).get(name); return v ? v.slice(0, 64) : null; }
    catch (e) { return null; }
  }
  function touchOrigin() {
    var src = urlParam('utm_source');
    if (src) return src.slice(0, 64);
    var ref = document.referrer || '';
    if (!ref) return 'direct';
    try { return new URL(ref).hostname.slice(0, 64) || 'referral'; } catch (e) { return 'referral'; }
  }

  function cookieValue(name) {
    return document.cookie.split(';').map(x => x.trim()).find(x => x.startsWith(`${name}=`))?.slice(name.length + 1) || '';
  }

  let i18nPromise = null;
  let i18nObserver = null;
  const I18N_ATTRIBUTES = ['placeholder','title','aria-label','alt','data-label','data-title','data-note','data-placeholder'];

  function englishPage() {
    const value = cookieValue('xh_lang') || document.documentElement.lang || 'zh-CN';
    return String(value).toLowerCase().indexOf('en') === 0;
  }

  function loadI18nDictionary() {
    if (!i18nPromise) {
      i18nPromise = fetch('/assets/i18n-extra.json', { cache: 'force-cache' })
        .then(response => response.ok ? response.json() : {})
        .catch(() => ({}));
    }
    return i18nPromise;
  }

  function translatedText(value, dictionary, phrases) {
    if (!value || !/[\u4e00-\u9fff]/.test(value)) return value;
    if (Object.prototype.hasOwnProperty.call(dictionary, value)) return dictionary[value];
    let result = '';
    let index = 0;
    while (index < value.length) {
      let match = null;
      for (const entry of phrases) {
        if (entry[0].length >= 2 && value.startsWith(entry[0], index)) { match = entry; break; }
      }
      if (match) {
        result += match[1];
        index += match[0].length;
      } else {
        result += value.charAt(index);
        index += 1;
      }
    }
    return result;
  }

  function translateAttributeValue(element, name, value, dictionary, phrases) {
    if (!value || !/[\u4e00-\u9fff]/.test(value)) return;
    const translated = translatedText(value, dictionary, phrases);
    if (translated !== value) element.setAttribute(name, translated);
  }

  function translateDom(root, dictionary, phrases) {
    if (!root) return;
    const elements = root.nodeType === 1 ? [root, ...root.querySelectorAll('*')] : [...document.querySelectorAll('*')];
    for (const element of elements) {
      if (element.matches('script,style,code,pre,[data-i18n-skip]')) continue;
      for (const name of I18N_ATTRIBUTES) {
        if (element.hasAttribute(name)) translateAttributeValue(element, name, element.getAttribute(name), dictionary, phrases);
      }
    }
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    const nodes = [];
    while (walker.nextNode()) nodes.push(walker.currentNode);
    for (const node of nodes) {
      const parent = node.parentElement;
      if (!parent || parent.closest('script,style,code,pre,[data-i18n-skip]')) continue;
      const raw = node.nodeValue || '';
      const start = raw.search(/\S/);
      if (start < 0) continue;
      let end = raw.length;
      while (end > start && /\s/.test(raw.charAt(end - 1))) end--;
      const core = raw.slice(start, end);
      const translated = translatedText(core, dictionary, phrases);
      if (translated !== core) node.nodeValue = raw.slice(0, start) + translated + raw.slice(end);
    }
  }

  function translateDocumentMeta(dictionary, phrases) {
    const title = translatedText(document.title, dictionary, phrases);
    if (title !== document.title) document.title = title;
    document.querySelectorAll('meta[name="description"],meta[property="og:title"],meta[property="og:description"],meta[name="twitter:title"],meta[name="twitter:description"]').forEach(meta => {
      translateAttributeValue(meta, 'content', meta.getAttribute('content'), dictionary, phrases);
    });
  }

  function initDynamicI18n() {
    const page = document.body && document.body.dataset ? document.body.dataset.page : '';
    if (page === 'privacy' || page === 'rules') return;
    if (!englishPage()) return;
    loadI18nDictionary().then(dictionary => {
      const phrases = Object.entries(dictionary).filter(([zh]) => zh.length >= 2).sort((a, b) => b[0].length - a[0].length);
      if (!phrases.length) return;
      translateDocumentMeta(dictionary, phrases);
      if (document.body) translateDom(document.body, dictionary, phrases);
      if (i18nObserver || !document.body) return;
      i18nObserver = new MutationObserver(() => {
        clearTimeout(i18nObserver._timer);
        i18nObserver._timer = setTimeout(() => {
          translateDocumentMeta(dictionary, phrases);
          translateDom(document.body, dictionary, phrases);
        }, 0);
      });
      i18nObserver.observe(document.body, { childList: true, subtree: true, characterData: true, attributes: true,
        attributeFilter: I18N_ATTRIBUTES });
    }).catch(() => {});
  }

  // 语言切换按钮已下线：英文版尚未开放，按钮点进去只会提示「暂未开放」，
  // 放在导航里属于无效入口。恢复英文版时再把入口加回来。

  const categoryMap = {
    activity: '活动任务', weapon: '武器升级', camo: '迷彩任务', rank: '排位 / 段位', ranked: '排位模式',
    level: '等级 / 进度', leveling: '等级提升', quest: '任务 / 挑战', mission: '任务挑战', event: '活动 / 赛季',
    unlock: '解锁 / 收集', battlepass: '战斗通行证', class: '兵种 / 职能', squad: '组队协同', other: '其他任务'
  };
  const orderStatusMap = {
    WAITING_CONFIRMATION: '等待确认', IN_PROGRESS: '进行中', COMPLETED: '已完成', EXCEPTION: '异常'
  };
  const quoteStatusMap = {
    NEW: '已提交', CONTACTED: '客服已联系', QUOTED: '已报价', CONVERTED: '已进入履约', CLOSED: '已关闭', CANCELLED: '已取消'
  };
  const RECORDS_KEY = 'xh_client_records_v1';
  let servicesPromise = null;
  let taxonomyPromise = null;
  let taxonomyLoadError = null;
  let currentTaxonomy = { games: [], categories: [], platforms: [], gamePlatforms: [] };
  const FALLBACK_PLATFORMS = [
    { code: 'STEAM', name: 'Steam', platformGroup: 'PC', active: true, sortOrder: 10 },
    { code: 'EPIC', name: 'Epic Games', platformGroup: 'PC', active: true, sortOrder: 20 },
    { code: 'BATTLE_NET', name: 'Battle.net', platformGroup: 'PC', active: true, sortOrder: 30 },
    { code: 'RIOT', name: 'Riot Client', platformGroup: 'PC', active: true, sortOrder: 40 },
    { code: 'EA_APP', name: 'EA app', platformGroup: 'PC', active: true, sortOrder: 50 },
    { code: 'UBISOFT', name: 'Ubisoft Connect', platformGroup: 'PC', active: true, sortOrder: 60 },
    { code: 'ROCKSTAR', name: 'Rockstar Games Launcher', platformGroup: 'PC', active: true, sortOrder: 70 },
    { code: 'WEGAME', name: 'WeGame', platformGroup: 'PC', active: true, sortOrder: 80 },
    { code: 'HOYOPLAY', name: 'HoYoPlay', platformGroup: 'PC', active: true, sortOrder: 90 },
    { code: 'PLAYSTATION', name: 'PlayStation', platformGroup: 'CONSOLE', active: true, sortOrder: 100 },
    { code: 'XBOX', name: 'Xbox', platformGroup: 'CONSOLE', active: true, sortOrder: 110 },
    { code: 'SWITCH2', name: 'Nintendo Switch 2', platformGroup: 'CONSOLE', active: true, sortOrder: 120 },
    { code: 'SWITCH', name: 'Nintendo Switch', platformGroup: 'CONSOLE', active: true, sortOrder: 130 },
    { code: 'IOS', name: 'iOS', platformGroup: 'MOBILE', active: true, sortOrder: 140 },
    { code: 'ANDROID', name: 'Android', platformGroup: 'MOBILE', active: true, sortOrder: 150 },
    { code: 'OFFICIAL', name: '官方客户端', platformGroup: 'PC', active: true, sortOrder: 160 }
  ];

  const PHONE_CODES = [
    { code: '+86', label: '+86 中国' }, { code: '+852', label: '+852 中国香港' }, { code: '+853', label: '+853 中国澳门' },
    { code: '+886', label: '+886 中国台湾' }, { code: '+1', label: '+1 美国/加拿大' }, { code: '+44', label: '+44 英国' },
    { code: '+81', label: '+81 日本' }, { code: '+82', label: '+82 韩国' }, { code: '+65', label: '+65 新加坡' },
    { code: '+61', label: '+61 澳大利亚' }, { code: '+64', label: '+64 新西兰' }, { code: '+49', label: '+49 德国' },
    { code: '+33', label: '+33 法国' }, { code: '+39', label: '+39 意大利' }, { code: '+34', label: '+34 西班牙' },
    { code: '+31', label: '+31 荷兰' }, { code: '+46', label: '+46 瑞典' }, { code: '+41', label: '+41 瑞士' },
    { code: '+7', label: '+7 俄罗斯' }, { code: '+91', label: '+91 印度' }, { code: '+62', label: '+62 印度尼西亚' },
    { code: '+60', label: '+60 马来西亚' }, { code: '+66', label: '+66 泰国' }, { code: '+84', label: '+84 越南' },
    { code: '+971', label: '+971 阿联酋' }, { code: '+55', label: '+55 巴西' }, { code: '+27', label: '+27 南非' },
    { code: '+20', label: '+20 埃及' }
  ];
  const normalizeCode = value => String(value || '').trim().toUpperCase();

  function loadServices() {
    if (!servicesPromise) {
      servicesPromise = api('/api/services').then(rows => Array.isArray(rows) ? rows : []).catch(error => {
        servicesPromise = null;
        throw error;
      });
    }
    return servicesPromise;
  }

  function categoryLabel(value, gameCode = null) {
    const code=String(value||'').toUpperCase();
    const row=currentTaxonomy.categories.find(x=>x.code===code && (!gameCode || x.gameCode===gameCode));
    return row?.name || categoryMap[String(value || '').toLowerCase()] || String(value || '');
  }

  function gameLabel(value) { const code=normalizeCode(value); return currentTaxonomy.games.find(x=>normalizeCode(x.code)===code)?.name || code; }
  function categoryIcon(value, gameCode = null) { const code=normalizeCode(value); const game=normalizeCode(gameCode); return currentTaxonomy.categories.find(x=>normalizeCode(x.code)===code && (!game || normalizeCode(x.gameCode)===game))?.iconKey || 'service'; }
  const iconUse = key => `<svg class="ui-icon" aria-hidden="true"><use href="/assets/icons.svg#i-${esc(key||'service')}"></use></svg>`;
  function loadTaxonomy(force = false){
    if (force) taxonomyPromise = null;
    if(!taxonomyPromise){
      taxonomyPromise=api('/api/catalog').then(x=>{
        taxonomyLoadError = null;
        currentTaxonomy={
          games:Array.isArray(x?.games)?x.games:[],
          categories:Array.isArray(x?.categories)?x.categories:[],
          platforms:Array.isArray(x?.platforms)&&x.platforms.length?x.platforms:FALLBACK_PLATFORMS,
          gamePlatforms:Array.isArray(x?.gamePlatforms)?x.gamePlatforms:[]
        };
        return currentTaxonomy;
      }).catch(error=>{
        taxonomyLoadError = error;
        currentTaxonomy={...currentTaxonomy,platforms:currentTaxonomy.platforms.length?currentTaxonomy.platforms:FALLBACK_PLATFORMS};
        return currentTaxonomy;
      });
    }
    return taxonomyPromise;
  }

  function setSelectOptions(select, rows, placeholder, valueGetter, labelGetter) {
    if (!select) return;
    const current = select.value;
    let html;
    if (select.matches('[data-link-platform]') && rows.some(x => x && x.platformGroup)) {
      const grouped = new Map();
      rows.forEach(row => {
        const group = ({ PC: 'PC', CONSOLE: '主机', MOBILE: '移动端' }[String(row.platformGroup || '').toUpperCase()] || '全平台');
        if (!grouped.has(group)) grouped.set(group, []);
        grouped.get(group).push(row);
      });
      html = `<option value="">${esc(placeholder)}</option>` + [...grouped.entries()].map(([group, items]) =>
        `<optgroup label="${esc(group)}">${items.map(row => `<option value="${esc(valueGetter(row))}">${esc(labelGetter(row))}</option>`).join('')}</optgroup>`
      ).join('');
    } else {
      html = `<option value="">${esc(placeholder)}</option>` + rows.map(row =>
        `<option value="${esc(valueGetter(row))}">${esc(labelGetter(row))}</option>`
      ).join('');
    }
    select.innerHTML = html;
    if ([...select.options].some(x => x.value === current)) select.value = current;
  }

  async function initLinkedQuoteForm(form, preferredService = null) {
    if (!form) return;
    const game = form.querySelector('[data-link-game]');
    const platform = form.querySelector('[data-link-platform]');
    const platformStatus = form.querySelector('[data-platform-status]');
    const category = form.querySelector('[data-link-category]');
    const service = form.querySelector('[data-link-service]');
    if (!game || !category || !service) return;

    // R3: 平台选择 fail-open。即使目录或其它模块暂时异常，平台也不锁死。
    if (platform) {
        const fallbackRows = FALLBACK_PLATFORMS.map(x => ({ ...x, code: normalizeCode(x.code) }));
        setSelectOptions(platform, fallbackRows, '请选择平台 / 不确定', x => x.name, x => `${x.name}${x.platformGroup ? ` · ${x.platformGroup === 'PC' ? 'PC' : x.platformGroup === 'CONSOLE' ? '主机' : '移动端'}` : ''}`);
      platform.disabled = false;
      platform.removeAttribute('disabled');
    }

    let services = [];
    try { services = await loadServices(); } catch { services = []; }
    await loadTaxonomy();
    const available = services.filter(x => x && x.status !== 'offline');
    const serviceGames = [...new Set(available.map(x => normalizeCode(x.gameCode || 'CODMW4')).filter(Boolean))];
    const games = serviceGames.length ? serviceGames : currentTaxonomy.games.filter(x => x.active !== false).map(x => normalizeCode(x.code)).filter(Boolean);
    setSelectOptions(game, games, '请选择游戏', x => x, x => gameLabel(x));

    const setPlatformStatus = (text = '', tone = '') => {
      if (!platformStatus) return;
      platformStatus.textContent = text;
      platformStatus.className = `form-help platform-status${tone ? ` ${tone}` : ''}`;
    };

    const contactMethod = form.querySelector('[name="contactMethod"]');
    const customerContact = form.querySelector('[name="customerContact"]');
    const phoneCode = form.querySelector('[data-phone-code]');
    if (phoneCode && phoneCode.options.length === 0) {
      phoneCode.innerHTML = PHONE_CODES.map(item => `<option value="${esc(item.code)}">${esc(item.label)}</option>`).join('');
      phoneCode.value = '+86';
    }
    const normalizePhone = value => String(value || '').replace(/[\s()-]/g, '');
    const syncPhoneValidation = () => {
      if (!customerContact) return;
      const isPhone = contactMethod?.value === '手机';
      phoneCode?.classList.toggle('hidden', !isPhone);
      customerContact.inputMode = isPhone ? 'tel' : 'text';
      customerContact.placeholder = isPhone ? '请输入手机号' : '微信号 / 手机号 / QQ号';
      const local = normalizePhone(customerContact.value).replace(/\D/g, '');
      const code = normalizePhone(phoneCode?.value || '+86').replace(/\D/g, '');
      const invalidChars = isPhone && /[^\d\s()-]/.test(customerContact.value);
      const invalid = isPhone && customerContact.value.trim() && (local.length < 6 || code.length + local.length > 15 || invalidChars);
      customerContact.setCustomValidity(invalid ? '请输入正确的手机号和国家区号' : '');
      if (phoneCode) phoneCode.title = phoneCode.options[phoneCode.selectedIndex]?.textContent || '';
    };
    contactMethod?.addEventListener('change', syncPhoneValidation);
    phoneCode?.addEventListener('change', syncPhoneValidation);
    customerContact?.addEventListener('input', syncPhoneValidation);
    syncPhoneValidation();
    const platformPicker = form.querySelector('[data-platform-picker]');
    const platformField = platform?.closest('.field');
    const platformGroupLabel = group => ({ PC: 'PC', CONSOLE: '主机', MOBILE: '移动端' }[String(group || '').toUpperCase()] || '全平台');
    const closePlatformPicker = () => {
      if (!platformPicker) return;
      platformPicker.classList.remove('open');
      platformPicker.querySelector('[data-platform-toggle]')?.setAttribute('aria-expanded', 'false');
    };
    const updatePlatformPicker = () => {
      if (!platformPicker) return;
      const value = platform?.value || '';
      const label = platformPicker.querySelector('[data-platform-label]');
      if (label) label.textContent = value || '请选择平台 / 不确定';
      platformPicker.querySelectorAll('[data-platform-value]').forEach(button => {
        const active = (button.dataset.platformValue || '') === value;
        button.classList.toggle('active', active);
        button.setAttribute('aria-selected', active ? 'true' : 'false');
      });
    };
    const renderPlatformPicker = rows => {
      if (!platformPicker || !platform || !platformField) return;
      const current = platform.value || '';
      const grouped = new Map();
      (rows || []).forEach(row => {
        const value = row.name || row.code || '';
        if (!value) return;
        const group = platformGroupLabel(row.platformGroup);
        if (!grouped.has(group)) grouped.set(group, []);
        grouped.get(group).push({ value, group });
      });
      platformPicker.innerHTML = `<button type="button" class="platform-select-trigger" data-platform-toggle aria-haspopup="listbox" aria-expanded="false"><span data-platform-label>${esc(current || '请选择平台 / 不确定')}</span><span class="platform-select-arrow" aria-hidden="true"></span></button><div class="platform-select-menu" data-platform-menu role="listbox" tabindex="-1"><button type="button" class="platform-option${current === '' ? ' active' : ''}" data-platform-value="" role="option" aria-selected="${current === '' ? 'true' : 'false'}">不确定，由客服确认</button>${[...grouped.entries()].map(([group, items]) => `<div class="platform-option-group">${esc(group)}</div>${items.map(item => `<button type="button" class="platform-option${item.value === current ? ' active' : ''}" data-platform-value="${esc(item.value)}" role="option" aria-selected="${item.value === current ? 'true' : 'false'}">${esc(item.value)}</button>`).join('')}`).join('')}</div>`;
      platformField.classList.add('platform-picker-enabled');
      updatePlatformPicker();
    };
    function renderPlatforms() {
      if (!platform) return;
      const gameCode = normalizeCode(game.value);
      const previous = platform.value;
      if (!gameCode) {
        const fallbackRows = FALLBACK_PLATFORMS.map(x => ({ ...x, code: normalizeCode(x.code) }));
        setSelectOptions(platform, fallbackRows, '请选择平台 / 不确定', x => x.name, x => `${x.name}${x.platformGroup ? ` · ${x.platformGroup === 'PC' ? 'PC' : x.platformGroup === 'CONSOLE' ? '主机' : '移动端'}` : ''}`);
        platform.disabled = false;
        platform.removeAttribute('disabled');
        setPlatformStatus('');
        renderPlatformPicker(fallbackRows);
        return;
      }

      const links = currentTaxonomy.gamePlatforms.filter(x => x.active !== false && normalizeCode(x.gameCode) === gameCode);
      const allowed = new Set(links.map(x => normalizeCode(x.platformCode)).filter(Boolean));
      const source = (currentTaxonomy.platforms.length ? currentTaxonomy.platforms : FALLBACK_PLATFORMS)
        .filter(x => x.active !== false)
        .map(x => ({ ...x, code: normalizeCode(x.code) }))
        .sort((a,b) => Number(a.sortOrder||0) - Number(b.sortOrder||0));
      let rows = allowed.size ? source.filter(x => allowed.has(x.code)) : source;
      if (!rows.length) rows = FALLBACK_PLATFORMS;

      setSelectOptions(
        platform,
        rows,
        '请选择平台 / 不确定',
        x => x.name,
        x => `${x.name}${x.platformGroup ? ` · ${x.platformGroup === 'PC' ? 'PC' : x.platformGroup === 'CONSOLE' ? '主机' : x.platformGroup === 'MOBILE' ? '移动端' : x.platformGroup}` : ''}`
      );
      platform.disabled = false;
      platform.removeAttribute('disabled');
      if ([...platform.options].some(x => x.value === previous)) platform.value = previous;
      if (!platform.value && rows.length === 1) platform.value = rows[0].name;
      renderPlatformPicker(rows);

      if (taxonomyLoadError) {
        setPlatformStatus('平台目录连接异常，已启用通用平台列表；仍可正常提交询价。', 'warn');
      } else if (!allowed.size) {
        setPlatformStatus('该游戏暂未设置专属平台，已显示通用平台。', 'muted-status');
      } else {
        setPlatformStatus(`已匹配 ${rows.length} 个可用平台。`, 'ok-status');
      }
    }

    function renderCategories(preferredCategory = null, preferredService = null) {
      const rows = available.filter(x => normalizeCode(x.gameCode || 'CODMW4') === normalizeCode(game.value));
      const categories = [...new Set(rows.map(x => String(x.category || '').toLowerCase()).filter(Boolean))]
        .sort((a, b) => categoryLabel(a, normalizeCode(game.value)).localeCompare(categoryLabel(b, normalizeCode(game.value)), 'zh-CN'));
      setSelectOptions(category, categories, game.value ? '请选择需求类型' : '请先选择游戏', x => x, x => categoryLabel(x, normalizeCode(game.value)));
      category.disabled = !game.value || categories.length === 0;
      if (preferredCategory && categories.includes(preferredCategory)) category.value = preferredCategory;
      if (!category.value && categories.length === 1) category.value = categories[0];
      renderServices(preferredService);
    }

    function renderServices(preferred = null) {
      const rows = available.filter(x => normalizeCode(x.gameCode || 'CODMW4') === normalizeCode(game.value) && String(x.category || '').toLowerCase() === category.value);
      setSelectOptions(service, rows, category.value ? '请选择具体服务' : '请先选择需求类型', x => x.name, x => `${x.name} · ${price(x)}`);
      service.disabled = !category.value || rows.length === 0;
      if (preferred) {
        const target = rows.find(x => x.slug === preferred || String(x.id) === String(preferred) || x.name === preferred);
        if (target) service.value = target.name;
      }
      if (service.value) service.dispatchEvent(new Event('change'));
    }

    game.addEventListener('change', () => {
      if (platform) platform.value = '';
      renderPlatforms();
      renderCategories();
    });
    category.addEventListener('change', () => renderServices());
    service.addEventListener('change', () => {
      const selected = available.find(x => x.name === service.value && normalizeCode(x.gameCode || 'CODMW4') === normalizeCode(game.value) && String(x.category || '').toLowerCase() === category.value);
      const box = form.closest('main, .quick-card, .card')?.querySelector('#selectedService') || document.querySelector('#selectedService');
      if (box && selected) {
        const platformText = platform?.value ? ` · ${platform.value}` : '';
        box.textContent = `已选择：${selected.name}${platformText} · ${price(selected)}`;
        box.classList.remove('hidden');
      } else if (box && !service.value) {
        box.classList.add('hidden');
      }
    });
    platform?.addEventListener('change', () => {
      updatePlatformPicker();
      if (service.value) service.dispatchEvent(new Event('change'));
    });
    platformPicker?.addEventListener('click', event => {
      const toggle = event.target.closest('[data-platform-toggle]');
      if (toggle) {
        const open = !platformPicker.classList.contains('open');
        platformPicker.classList.toggle('open', open);
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        return;
      }
      const option = event.target.closest('[data-platform-value]');
      if (!option || !platform) return;
      platform.value = option.dataset.platformValue || '';
      platform.dispatchEvent(new Event('change', { bubbles: true }));
      updatePlatformPicker();
      closePlatformPicker();
    });
    document.addEventListener('pointerdown', event => {
      if (platformPicker && !platformPicker.contains(event.target)) closePlatformPicker();
    });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') closePlatformPicker();
    });

    if (preferredService) {
      const target = available.find(x => x.slug === preferredService || String(x.id) === String(preferredService));
      if (target) {
        game.value = normalizeCode(target.gameCode || 'CODMW4');
        renderPlatforms();
        renderCategories(String(target.category || '').toLowerCase(), target.slug);
        return;
      }
    }
    if (games.length === 1) game.value = games[0];
    renderPlatforms();
    renderCategories();
  }

  function price(service) {
    if (service.priceMode === 'fixed') return `¥${service.priceFixed}`;
    if (service.priceMode === 'range') return `¥${service.priceMin}–${service.priceMax}`;
    if (service.priceMode === 'quote' && Number(service.priceMin) > 0) return `参考 ¥${Number(service.priceMin).toFixed(2).replace(/\.00$/, '')} 起`;
    return '人工报价';
  }

  // V22：仅当全局运营状态为 open 时才显示"可接单"；
  // 预约/维护等阶段统一显示"可预约"，避免与服务大厅、首页口径冲突。
  function statusText(status) {
    if (status === 'offline') return '已下架';
    var open = currentConfig && currentConfig.serviceStatus === 'open';
    if (status === 'busy') return '排队中';
    if (status !== 'available') return '已下架';
    return open ? '可接单' : '可预约';
  }

  function quoteHref(service) {
    if (currentConfig && !currentConfig.quoteEnabled) return '/contact';
    return `/quote?service=${encodeURIComponent(service.slug)}`;
  }

  function serviceHref(service) {
    return service?.slug ? `/service/${encodeURIComponent(service.slug)}` : quoteHref(service);
  }

  function serviceCard(service, compact = false) {
    const image = safeUrl(service.coverImage);
    return `<a class="service-card card${compact ? ' compact' : ''}" href="${esc(serviceHref(service))}" data-service-id="${service.id}" data-track="service_card_click" data-ref="${service.id}">
      ${image ? `<div class="service-cover"><img src="${esc(image)}" alt="${esc(service.name)}封面" loading="lazy"></div>` : `<div class="service-cover service-fallback-icon">${iconUse(categoryIcon(service.category, String(service.gameCode||'').toUpperCase()))}</div>`}
      <div class="service-card-head"><span class="tag">${esc(categoryLabel(service.category, String(service.gameCode||'').toUpperCase()))}</span><span class="availability ${service.status === 'busy' ? 'busy' : ''}">${esc(statusText(service.status))}</span></div>
      <h3>${esc(service.name)}</h3><p class="muted clamp2">${esc(service.shortDesc)}</p>
      <div class="service-meta"><strong>${esc(price(service))}</strong><span>${esc(service.durationText)}</span></div>
    </a>`;
  }

  function updateDynamicStartingPrice(services = []) {
    if (currentConfig && currentConfig.showStartingPrice === false) {
      $$('[data-config=priceRow]').forEach(x => x.classList.add('hidden'));
      return '';
    }
    const values = [];
    services.filter(x => x && x.status !== 'offline').forEach(x => {
      if (x.priceMode === 'fixed' && Number(x.priceFixed) > 0) values.push(Number(x.priceFixed));
      if (x.priceMode === 'range' && Number(x.priceMin) > 0) values.push(Number(x.priceMin));
    });
    const value = values.length ? `¥${Math.min(...values).toFixed(2).replace(/\.00$/, '')} 起` : '按需求报价';
    setText('[data-config=price]', value);
    $$('[data-config=priceRow]').forEach(x => x.classList.remove('hidden'));
    return value;
  }

  function bindUploadFields(root = document) {
    $$('input[type="file"][data-upload-input]', root).forEach(input => {
      if (input.dataset.boundUpload === '1') return;
      input.dataset.boundUpload = '1';
      const field = input.closest('.upload-field');
      const name = field?.querySelector('[data-file-name]');
      const update = () => {
        const file = input.files?.[0] || null;
        if (field) field.classList.toggle('has-file', !!file);
        if (name) name.textContent = file ? file.name : '上传任务截图';
      };
      input.addEventListener('change', update);
      update();
    });
  }

  function initResponsiveDetails(root = document) {
    const mobile = window.matchMedia('(max-width: 760px)').matches;
    $$('[data-responsive-details]', root).forEach(details => { details.open = !mobile; });
  }

  function setBusy(button, busy, busyText = '处理中…') {
    if (!button) return;
    if (busy) {
      button.dataset.label = button.textContent;
      button.textContent = busyText;
      button.disabled = true;
      button.classList.add('is-busy');
    } else {
      button.textContent = button.dataset.label || button.textContent;
      button.disabled = false;
      button.classList.remove('is-busy');
    }
  }

  function showMessage(box, message, type = 'success') {
    if (!box) return;
    box.innerHTML = `<div class="notice ${type === 'error' ? 'error' : ''}" role="status">${esc(message)}</div>`;
  }

  function quoteSuccessHtml(businessNo, config, message) {
    const channels = contactChannels(config);
    const primary = channels[0] || { label: '主客服', wechatId: config.wechatId || 'AAC9878', qrImage: config.wechatQrImage || null, note: '发送业务编号确认报价' };
    const wechat = primary.wechatId || config.wechatId || 'AAC9878';
    const wechatLink = contactJumpUrl(wechat);
    const wechatDisplay = contactDisplayValue(wechat) || '扫码添加';
    const qr = safeUrl(primary.qrImage || config.wechatQrImage);
    const taobao = safeUrl(config.taobaoUrl);
    const xianyu = safeUrl(config.xianyuUrl);
    return `<div class="quote-success-card" role="status">
      <span class="eyebrow">提交成功</span>
      <h2>业务编号</h2>
      <div class="success-no mono">${esc(businessNo)}</div>
      <p>${esc(message).replace(/\n/g, '<br>')}</p>
      <div class="success-primary-actions">
        <button class="btn primary" type="button" data-copy-business-no="${esc(businessNo)}">复制编号</button>
        ${wechatLink ? `<a class="btn ghost" href="${esc(wechatLink)}" target="_blank" rel="noopener noreferrer">打开微信</a>` : '<a class="btn ghost" href="/contact">加微信</a>'}
      </div>
      <div class="success-contact-box">
        ${qr ? `${wechatLink ? `<a href="${esc(wechatLink)}" target="_blank" rel="noopener noreferrer" aria-label="打开微信客服">` : ''}<img class="success-qr" src="${esc(qr)}" alt="${esc(primary.label || '微信')}二维码">${wechatLink ? '</a>' : ''}` : `<div class="success-qr success-qr-text">微信<br><strong>${esc(wechatDisplay)}</strong></div>`}
        <div><strong>${esc(primary.label || '微信')} <span id="successWechatId">${esc(wechatDisplay)}</span></strong><span>${esc(primary.note || '把业务编号发给客服，确认报价后再走淘宝/闲鱼付款。')}</span>${wechatLink ? `<a class="btn small-btn ghost" href="${esc(wechatLink)}" target="_blank" rel="noopener noreferrer">打开微信</a>` : '<button class="btn small-btn ghost copy-btn" type="button" data-copy="#successWechatId">复制微信号</button>'}</div>
      </div>
      ${channels.length > 1 ? `<div class="success-channel-strip">${channels.slice(1, 4).map(c => `<span>${esc(c.label || '客服')} · ${esc(c.wechatId || '扫码添加')}</span>`).join('')}</div>` : ''}
      <div class="success-market-actions">
        ${taobao ? `<a class="btn small-btn" href="${esc(taobao)}" target="_blank" rel="noopener noreferrer">打开淘宝</a>` : '<a class="btn small-btn" href="/contact">打开淘宝</a>'}
        ${xianyu ? `<a class="btn small-btn" href="${esc(xianyu)}" target="_blank" rel="noopener noreferrer">打开闲鱼</a>` : '<a class="btn small-btn" href="/contact">打开闲鱼</a>'}
        <a class="btn small-btn ghost" href="/records">业务记录</a>
      </div>
    </div>`;
  }

  function contactChannels(config = {}) {
    const rows = Array.isArray(config.contactChannels) ? config.contactChannels : [];
    const out = rows.map((x, i) => ({
      label: String(x?.label || `客服${i + 1}`).trim(),
      wechatId: String(x?.wechatId || '').trim(),
      jumpUrl: contactJumpUrl(x?.jumpUrl) || '',
      qrImage: safeUrl(x?.qrImage) || '',
      note: String(x?.note || '').trim()
    })).filter(x => x.wechatId || x.jumpUrl || x.qrImage);
    if (out.length) return out;
    return [{ label: '主客服', wechatId: config.wechatId || 'AAC9878', jumpUrl: '', qrImage: safeUrl(config.wechatQrImage) || '', note: '发送业务编号确认报价' }];
  }

  function contactChannelCards(config = {}) {
    return contactChannels(config).map((c, i) => {
      const qr = safeUrl(c.qrImage);
      const id = `contactChannelWechat${i}`;
      const jump = c.jumpUrl || contactJumpUrl(c.wechatId);
      const display = contactDisplayValue(c.wechatId) || '扫码添加';
      const qrImg = qr ? `<img class="contact-channel-qr" src="${esc(qr)}" alt="${esc(c.label || '客服')}二维码" loading="lazy">` : '<div class="contact-channel-qr contact-channel-qr-text">微信</div>';
      /* 二维码点击只放大查看，不跳转；跳转保留在「一键跳转微信」按钮上。 */
      const qrHtml = qr ? `<button type="button" class="contact-channel-qr-btn" data-qr-zoom="${esc(qr)}" data-qr-label="${esc(c.label || '客服')}" aria-label="放大二维码">${qrImg}</button>` : qrImg;
      return `<article class="contact-channel-card ${jump ? 'is-clickable' : ''}">
        <div class="contact-channel-main">
          <span class="tag">${esc(c.label || `客服${i + 1}`)}</span>
          <strong id="${id}">${esc(display)}</strong>
          ${c.note ? `<p>${esc(c.note)}</p>` : '<p>发送业务编号确认报价。</p>'}
          ${c.wechatId && !contactJumpUrl(c.wechatId) ? `<button class="btn small-btn copy-btn" type="button" data-copy="#${id}">复制微信</button>` : ''}
          ${c.jumpUrl ? `<a class="btn small-btn" href="${esc(c.jumpUrl)}" target="_blank" rel="noopener noreferrer">一键跳转微信</a>` : (contactJumpUrl(c.wechatId) ? `<a class="btn small-btn" href="${esc(contactJumpUrl(c.wechatId))}" target="_blank" rel="noopener noreferrer">打开微信</a>` : '')}
        </div>
        ${qrHtml}
      </article>`;
    }).join('');
  }

  /* 客服二维码放大查看：点击图片弹出大图，再点任意处/按 Esc 关闭。 */
  function qrLightbox() {
    let box = document.getElementById('qrLightbox');
    if (!box) {
      box = document.createElement('div');
      box.id = 'qrLightbox';
      box.className = 'qr-lightbox hidden';
      box.setAttribute('role', 'dialog');
      box.setAttribute('aria-label', '二维码大图');
      box.innerHTML = '<button type="button" class="qr-lightbox-close" aria-label="关闭">×</button>'
        + '<img class="qr-lightbox-img" alt="客服二维码大图">'
        + '<a class="qr-lightbox-save" download="客服二维码.png" href="#">保存二维码</a>'
        + '<small class="qr-lightbox-hint">手机可长按图片保存到相册</small>';
      document.body.appendChild(box);
      /* 点图片或保存按钮时不关闭，方便长按保存；点背景/关闭按钮才收起。 */
      box.addEventListener('click', event => {
        if (event.target.closest('.qr-lightbox-img') || event.target.closest('.qr-lightbox-save')) return;
        box.classList.add('hidden');
      });
      document.addEventListener('keydown', event => { if (event.key === 'Escape') box.classList.add('hidden'); });
    }
    return box;
  }

  document.addEventListener('click', event => {
    const trigger = event.target.closest('[data-qr-zoom]');
    if (!trigger) return;
    event.preventDefault();
    const box = qrLightbox();
    const url = trigger.getAttribute('data-qr-zoom') || '';
    const img = box.querySelector('.qr-lightbox-img');
    const save = box.querySelector('.qr-lightbox-save');
    if (img) img.src = url;
    if (save) {
      save.setAttribute('href', url);
      save.setAttribute('download', `${trigger.getAttribute('data-qr-label') || '客服'}二维码.png`);
    }
    box.classList.remove('hidden');
  });

  async function uploadQuoteImage(file) {
    if (!file) return null;
    const maxBytes = 5 * 1024 * 1024;
    if (file.size > maxBytes) throw new Error('截图不能超过 5MB，请压缩后重新选择。');
    if (file.type && !file.type.startsWith('image/')) throw new Error('截图仅支持图片文件。');
    const fd = new FormData();
    fd.append('file', file);
    const out = await api('/api/uploads/quote', { method: 'POST', body: fd });
    track('screenshot_upload');
    return out.assetId;
  }

  async function submitQuoteForm(form, msgBox) {
    const config = await loadConfig();
    if (!config.quoteEnabled) {
      const message = '当前已关闭网页询价，请通过客服渠道联系。';
      showMessage(msgBox, message, 'error');
      throw new Error(message);
    }
    const contactMethodValue = form.contactMethod?.value.trim() || '';
    const rawContactValue = form.customerContact?.value.trim() || '';
    const phoneCodeValue = form.querySelector('[data-phone-code]')?.value || '+86';
    const localPhone = rawContactValue.replace(/[\s()-]/g, '').replace(/\D/g, '');
    const customerContactValue = contactMethodValue === '手机' ? `${phoneCodeValue}${localPhone}` : rawContactValue;
    if (contactMethodValue === '手机' && (!/^\+\d{1,4}$/.test(phoneCodeValue) || localPhone.length < 6 || (phoneCodeValue.replace(/\D/g, '').length + localPhone.length) > 15 || /[^\d\s()-]/.test(rawContactValue))) {
      const message = '请输入正确的手机号和国家区号。';
      showMessage(msgBox, message, 'error');
      form.customerContact?.focus();
      throw new Error(message);
    }
    if (!form.privacyConsent?.checked) {
      const message = '请先阅读并同意服务规则与隐私说明。';
      showMessage(msgBox, message, 'error');
      form.privacyConsent?.focus();
      throw new Error(message);
    }
    const submit = form.querySelector('button[type=submit]');
    setBusy(submit, true, '正在提交…');
    try {
      const assetId = await uploadQuoteImage(form.file?.files?.[0] || null);
      const key = form.dataset.idempotencyKey || makeUuid();
      form.dataset.idempotencyKey = key;
      const currentProgress = form.currentProgress?.value.trim() || '';
      const out = await api('/api/quote', {
        method: 'POST', headers: { 'Idempotency-Key': key },
        body: JSON.stringify({
          game: form.game.value.trim(), demand: form.demand.value.trim(),
          currentProgress: currentProgress || null,
          screenshotAssetId: assetId,
          platform: form.platform?.value.trim() || null,
          expectedTime: form.expectedTime?.value.trim() || null,
          contactMethod: form.contactMethod?.value.trim() || null,
          customerContact: customerContactValue || null,
          privacyConsent: !!form.privacyConsent?.checked,
          firstTouchSource: touchOrigin(),
          utmSource: urlParam('utm_source'),
          utmMedium: urlParam('utm_medium'),
          utmCampaign: urlParam('utm_campaign'),
          referrer: document.referrer || null
        })
      });
      const businessNo = out.businessNo || out.id;
      const template = config.quoteSuccessText || '提交成功，业务编号：{id}。可在“业务记录”查看进度，或联系客服一对一咨询。';
      const message = replaceAllCompat(template, '{id}', businessNo);
      if (msgBox) msgBox.innerHTML = quoteSuccessHtml(businessNo, config, message);
      rememberRecord({ businessNo, stage: 'QUOTE', createdAt: out.createdAt || new Date().toISOString() });
      track('quote_submit', businessNo);
      form.dataset.idempotencyKey = '';
      if (form.file) { form.file.value = ''; form.file.dispatchEvent(new Event('change')); }
      return out;
    } catch (error) {
      showMessage(msgBox, error.message, 'error');
      throw error;
    } finally {
      setBusy(submit, false);
    }
  }

  function normalizeRecordNo(value) {
    return String(value || '').trim().toUpperCase();
  }

  function validRecordNo(value) {
    const no = normalizeRecordNo(value);
    return /^XH[A-Z0-9]{10}$/.test(no) || /^XH-[A-Z2-9]{24}$/.test(no);
  }

  function getSavedRecords() {
    try {
      const rows = JSON.parse(localStorage.getItem(RECORDS_KEY) || '[]');
      return Array.isArray(rows) ? rows.filter(x => x && x.businessNo).slice(0, 30) : [];
    } catch { return []; }
  }

  function rememberRecord(record) {
    const businessNo = normalizeRecordNo(record?.businessNo);
    if (!validRecordNo(businessNo)) return;
    const rows = getSavedRecords().filter(x => normalizeRecordNo(x.businessNo) !== businessNo);
    rows.unshift({
      businessNo,
      stage: record?.stage || null,
      createdAt: record?.createdAt || new Date().toISOString(),
      savedAt: new Date().toISOString()
    });
    try { localStorage.setItem(RECORDS_KEY, JSON.stringify(rows.slice(0, 30))); } catch {}
  }

  function clearSavedRecords() {
    try { localStorage.removeItem(RECORDS_KEY); } catch {}
  }

  function recordStatusLabel(stage, status) {
    return stage === 'QUOTE' ? (quoteStatusMap[status] || status) : (orderStatusMap[status] || status);
  }

  function setText(selector, value) {
    $$(selector).forEach(x => x.textContent = value ?? '');
  }

  function setRowVisible(name, visible) {
    $$(`[data-contact-row="${name}"]`).forEach(x => x.classList.toggle('hidden', !visible));
  }

  function setExternalLink(name, value) {
    const url = safeUrl(value);
    $$(`[data-contact-link="${name}"]`).forEach(x => {
      if (url) { x.href = url; x.classList.remove('hidden'); }
      else { x.removeAttribute('href'); x.classList.add('hidden'); }
    });
    return !!url;
  }

  // 站点运营状态文案：优先使用后台配置短文案，前台不自行承诺"正常接单"。
  function statusHeadline(config) {
    const custom = (config.statusShortText || '').trim();
    if (custom) return `● ${custom}`;
    switch (config.serviceStatus) {
      case 'open': return '● 当前正常接单';
      case 'reservation': return '● 预约中 · 可提前询价';
      case 'preheat': return '● 预热中 · 可提前询价';
      case 'maintenance': return '● 维护中 · 暂时无法开工';
      case 'suspended': case 'closed': return '● 当前暂停接单';
      case 'busy': return '● 当前繁忙，建议先询价';
      default: return '● 状态确认中';
    }
  }

  function applyConfig(config) {
    currentConfig = config;
    setText('[data-config=brandName]', config.brandName || '星火网游代练平台');
    setText('[data-config=heroTitle]', config.heroTitle);
    setText('[data-config=heroSubtitle]', config.heroSubtitle);
    $$('[data-config=priceRow]').forEach(x => x.classList.toggle('hidden', config.showStartingPrice === false));
    setText('[data-config=contactTitle]', config.contactTitle || '联系客服');
    setText('[data-config=contactIntro]', config.contactIntro || '');
    setText('[data-config=serviceHours]', config.serviceHours || '');
    setText('[data-config=contactNotice]', config.contactNotice || '');
    setText('[data-config=orderHelpText]', config.orderHelpText || '输入统一业务编号即可查询询价线索或履约订单进度。');
    setText('[data-config=footerText]', config.footerText || '');
    setText('[data-config=toolUpdatedNote]', config.toolUpdatedNote || '');
    setText('[data-config=legalEntityName]', config.legalEntityName || '星维网络服务工作室');

    $$('[data-config=status]').forEach(x => {
      x.textContent = statusHeadline(config);
      x.classList.toggle('status-busy', config.serviceStatus === 'busy' || config.serviceStatus === 'preheat');
      x.classList.toggle('status-closed', config.serviceStatus === 'closed' || config.serviceStatus === 'suspended' || config.serviceStatus === 'maintenance');
    });

    // 主按钮文案由后台统一控制（预约阶段显示"提前询价"）
    $$('[data-primary-cta]').forEach(x => {
      if (config.primaryButtonLabel) x.textContent = config.primaryButtonLabel;
    });

    const tags = $('#trustTags');
    if (tags) tags.innerHTML = (config.trustTags || []).map(x => `<span>${esc(x)}</span>`).join('');

    const trustCaseGrid = $('#trustCaseGrid');
    if (trustCaseGrid && Array.isArray(config.trustCases) && config.trustCases.length) {
      trustCaseGrid.innerHTML = config.trustCases.map(item => `<article class="card trust-case">
        <span class="tag">${esc(item.tag || '服务示例')}</span>
        <h2>${esc(item.title || '服务示例')}</h2>
        <p>${esc(item.metric || '按进度确认')}</p>
        ${item.detail ? `<small>${esc(item.detail)}</small>` : ''}
      </article>`).join('');
    }

    // V22 P1-02：只在后台人工开启后展示真实完成记录
    const caseSection = $('#publicCaseSection');
    const caseGrid = $('#publicCaseGrid');
    if (caseSection && caseGrid) {
      api('/api/public/cases').then(function (rows) {
        const list = Array.isArray(rows) ? rows : [];
        if (!list.length) return;
        caseGrid.innerHTML = list.slice(0, 6).map(function (c) {
          const when = c.finishedAt ? String(c.finishedAt).slice(0, 10) : '';
          return `<article class="card trust-case">
            <span class="tag">已完成</span>
            <h3>${esc(c.maskedNo || 'XH****')}</h3>
            <p>${esc(c.summary || '已完成交付')}</p>
            ${when ? `<small>完成日期 ${esc(when)}</small>` : ''}
          </article>`;
        }).join('');
        caseSection.hidden = false;
      }).catch(function () { /* 无数据或失败时保持隐藏 */ });
    }

    const trustFaqList = $('#trustFaqList');
    if (trustFaqList && Array.isArray(config.trustFaq) && config.trustFaq.length) {
      trustFaqList.innerHTML = config.trustFaq.map(item => `<section>
        <h2>${esc(item.question || '常见问题')}</h2>
        <p>${esc(item.answer || '')}</p>
      </section>`).join('');
    }

    const hot = $('#hotServices');
    if (hot) hot.innerHTML = config.hotServices.length ? config.hotServices.map(s => serviceCard(s, true)).join('') : '<div class="empty">暂无热门推荐，可直接联系客服。</div>';

    const heroVisual = $('#heroVisual');
    const heroImage = safeUrl(config.heroImage);
    if (heroVisual) {
      heroVisual.style.height = `${Math.max(80, Math.min(420, Number(config.heroImageHeight || 150)))}px`;
      if (heroImage) {
        heroVisual.innerHTML = `<img src="${esc(heroImage)}" alt="服务展示" loading="eager">`; heroVisual.classList.add('has-image');
        const img = heroVisual.querySelector('img');
        img.style.objectFit = ['cover','contain','fill'].includes(config.heroImageFit) ? config.heroImageFit : 'cover';
        img.style.objectPosition = `${Number(config.heroImagePositionX ?? 50)}% ${Number(config.heroImagePositionY ?? 50)}%`;
      } else { heroVisual.innerHTML = ''; heroVisual.classList.remove('has-image'); }
    }

    window.SiteImageControls.main(heroVisual, heroVisual?.querySelector('img'), config);

    const hero = $('#homeHero');
    const heroBg = $('#heroBackground');
    const heroBgImage = safeUrl(config.heroBackgroundImage);
    if (hero && heroBg) {
      hero.classList.toggle('has-background', !!heroBgImage);
      window.SiteImageControls.background(hero, heroBg, config, heroBgImage);
    }

    const coverVars = window.SiteImageControls.serviceCoverVars(config);
    document.documentElement.style.setProperty('--service-cover-height', coverVars.height);
    document.documentElement.style.setProperty('--service-cover-fit', coverVars.fit);
    document.documentElement.style.setProperty('--service-cover-position', coverVars.position);
    document.documentElement.style.setProperty('--wechat-qr-size', `${Math.max(120, Math.min(420, Number(config.wechatQrSize || 260)))}px`);

    const announce = $('#announcementBar');
    if (announce) {
      const show = !!config.announcementEnabled && !!config.announcementText;
      announce.classList.toggle('hidden', !show);
      if (show) announce.textContent = config.announcementText;
    }

    setText('[data-contact=wechat]', config.wechatId || '未配置');
    setText('[data-contact=qq]', config.qqId || '未配置');
    setText('[data-contact=phone]', config.phone || '未配置');
    setRowVisible('wechat', !!config.wechatId || !!config.wechatQrImage);
    setRowVisible('qq', !!config.qqId || !!config.qqLink);
    setRowVisible('phone', !!config.phone);
    setRowVisible('hours', !!config.serviceHours);
    setRowVisible('taobao', setExternalLink('taobao', config.taobaoUrl));
    setRowVisible('xianyu', setExternalLink('xianyu', config.xianyuUrl));
    setExternalLink('qq', config.qqLink);
    $$('[data-contact-link="phone"]').forEach(x => {
      if (config.phone) { x.href = `tel:${config.phone.replace(/\s+/g, '')}`; x.classList.remove('hidden'); }
      else x.classList.add('hidden');
    });

    const qr = $('#wechatQr');
    const qrUrl = safeUrl(config.wechatQrImage);
    if (qr) {
      qr.classList.toggle('hidden', !qrUrl);
      qr.innerHTML = qrUrl ? `<img src="${esc(qrUrl)}" alt="微信二维码" loading="lazy">` : '';
    }

    const channelGrid = $('#contactChannelGrid');
    if (channelGrid) channelGrid.innerHTML = contactChannelCards(config);

    const wecomUrl = safeUrl(config.wecomKefuUrl);
    $$('[data-wecom-entry]').forEach(el => el.classList.toggle('hidden', !wecomUrl));
    $$('a[data-wecom-link]').forEach(a => { if (wecomUrl) a.setAttribute('href', wecomUrl); });
    $$('[data-wecom-note]').forEach(el => { if (config.wecomKefuNote) el.textContent = config.wecomKefuNote; });
    $$('[data-wecom-label]').forEach(el => { el.textContent = config.wecomKefuLabel || '企业微信客服'; });
    $$('[data-wecom-button]').forEach(el => { el.textContent = config.wecomKefuButton || '联系客服'; });
    setText('[data-config=quickContactTitle]', config.quickContactTitle || '客服联系');
    setText('[data-config=taobaoLabel]', config.taobaoLabel || '店铺入口');
    setText('[data-config=xianyuLabel]', config.xianyuLabel || '主页入口');
    const homeList = document.getElementById('homeContactList');
    if (homeList) {
      homeList.innerHTML = contactChannels(config).map((c, i) => {
        const id = `homeContactValue${i}`;
        const raw = String(c.wechatId || '').trim();
        const legacyJump = contactJumpUrl(raw);
        const jump = c.jumpUrl || legacyJump;
        return `<div class="quick-contact-item">
          <div class="quick-contact-row"><span>${esc(c.label || `客服${i + 1}`)}</span><strong id="${id}">${esc(raw || '扫码联系')}</strong>${legacyJump ? '' : `<button class="btn small-btn copy-btn" type="button" data-copy="#${id}">复制</button>`}</div>
          ${jump ? `<a class="quick-contact-jump" href="${esc(jump)}" target="_blank" rel="noopener noreferrer">一键跳转微信</a>` : ''}
        </div>`;
      }).join('');
      $$('[data-copy]', homeList).forEach(button => {
        if (button.dataset.bound === '1') return;
        button.dataset.bound = '1';
        button.addEventListener('click', async () => {
          const target = document.querySelector(button.dataset.copy);
          const text = target?.textContent?.trim();
          if (!text || text === '未配置') return;
          try {
            await navigator.clipboard.writeText(text);
            const old = button.textContent;
            button.textContent = '已复制';
            setTimeout(() => button.textContent = old, 1200);
          } catch {}
        });
      });
    }

    $$('a[href^="/quote"], [data-quote-link]').forEach(a => {
      if (!config.quoteEnabled) {
        if (!a.dataset.originalHref) a.dataset.originalHref = a.getAttribute('href') || '/quote';
        a.setAttribute('href', '/contact');
        if (a.classList.contains('primary') && !a.closest('.service-card')) a.textContent = '联系客服';
      } else if (a.dataset.originalHref) {
        a.setAttribute('href', a.dataset.originalHref);
        delete a.dataset.originalHref;
      }
    });

    $$('[data-order-form], [data-record-form]').forEach(form => {
      const disabled = !config.orderQueryEnabled;
      [...form.elements].forEach(el => el.disabled = disabled);
      const msg = form.querySelector('[data-message]');
      if (disabled && msg) showMessage(msg, '进度查询正在维护，请联系客服查询。', 'error');
    });
    $$('a[href="/order"], a[href^="/order?"], a[href="/records"], a[href^="/record/"]').forEach(a => {
      if (!config.orderQueryEnabled) {
        if (!a.dataset.originalHref) a.dataset.originalHref = a.getAttribute('href') || '/records';
        a.setAttribute('href', '/contact');
      } else if (a.dataset.originalHref) {
        a.setAttribute('href', a.dataset.originalHref);
        delete a.dataset.originalHref;
      }
    });

    const brand = config.brandName || '星火网游代练平台';
    if (document.body.dataset.page === 'home' && config.seoTitle) document.title = config.seoTitle;
    else document.title = replaceAllCompat(document.title, '星火网游代练平台', brand);
    if (document.body.dataset.page === 'home' && config.seoDescription) {
      const meta = $('meta[name="description"]'); if (meta) meta.content = config.seoDescription;
    }
  }

  function loadConfig(force = false) {
    if (force) configPromise = null;
    if (!configPromise) {
      configPromise = api('/api/site-config', { cache: 'no-store' }).then(config => { applyConfig(config); return config; }).catch(error => { configPromise = null; throw error; });
    }
    return configPromise;
  }

  function orderStatusLabel(status) { return orderStatusMap[status] || status; }

  document.addEventListener('click', event => {
    const tracked = event.target.closest('[data-track]');
    if (tracked) track(tracked.dataset.track, tracked.dataset.ref || null);
    const copyButton = event.target.closest('[data-copy]');
    if (copyButton) {
      const target = document.querySelector(copyButton.dataset.copy);
      const value = target?.textContent?.trim();
      if (value && value !== '未配置') {
        navigator.clipboard?.writeText(value).then(() => {
          const old = copyButton.textContent;
          copyButton.textContent = '已复制';
          setTimeout(() => { copyButton.textContent = old; }, 1200);
        }).catch(() => {});
      }
    }
    const copyBusinessNo = event.target.closest('[data-copy-business-no]');
    if (copyBusinessNo) {
      const value = copyBusinessNo.dataset.copyBusinessNo || '';
      navigator.clipboard?.writeText(value).then(() => {
        const old = copyBusinessNo.textContent;
        copyBusinessNo.textContent = '已复制';
        setTimeout(() => { copyBusinessNo.textContent = old; }, 1200);
      }).catch(() => {});
    }
  });

  window.addEventListener('focus', () => loadConfig(true).catch(() => {}));
  document.addEventListener('visibilitychange', () => { if (!document.hidden) loadConfig(true).catch(() => {}); });

  function initSmartMobileDock() {
    const dock = document.querySelector('[data-smart-dock]');
    if (!dock) return;
    let ticking = false;
    const update = () => {
      const mobile = window.matchMedia('(max-width: 760px)').matches;
      const threshold = Math.min(320, Math.max(180, window.innerHeight * 0.28));
      document.body.classList.toggle('mobile-dock-visible', mobile && window.scrollY > threshold);
      ticking = false;
    };
    const requestUpdate = () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(update);
    };
    window.addEventListener('scroll', requestUpdate, { passive: true });
    window.addEventListener('resize', requestUpdate, { passive: true });
    update();
  }

  function initCyberVisuals() {
    if (document.body.dataset.cyberVisuals === '1') return;
    document.body.dataset.cyberVisuals = '1';
    const lightMotion = window.matchMedia('(max-width: 760px), (pointer: coarse), (prefers-reduced-motion: reduce)').matches;
    if (lightMotion) {
      document.body.classList.add('lite-visuals');
      return;
    }
    const stage = document.createElement('div');
    stage.className = 'cyber-stage';
    stage.setAttribute('aria-hidden', 'true');
    stage.innerHTML = '<span class="cyber-grid"></span><span class="cyber-scan"></span><span class="cyber-radar"></span><span class="cyber-noise"></span>';
    document.body.prepend(stage);

    const cards = $$('.card,.service-card,.quick-card,.page-hero,.service-browser-console,.contact-console,.rules-console,.sidebar,.admin-main section');
    cards.forEach(card => {
      card.classList.add('cyber-surface');
      card.addEventListener('pointermove', event => {
        const rect = card.getBoundingClientRect();
        card.style.setProperty('--mx', `${((event.clientX - rect.left) / rect.width * 100).toFixed(1)}%`);
        card.style.setProperty('--my', `${((event.clientY - rect.top) / rect.height * 100).toFixed(1)}%`);
      }, { passive: true });
    });
  }

  initCyberVisuals();
  initSmartMobileDock();
  initDynamicI18n();

  window.XH = { initLinkedQuoteForm, loadServices, loadTaxonomy, updateDynamicStartingPrice, bindUploadFields, initResponsiveDetails,
    $, $$, api, esc, safeUrl, track, price, serviceCard, loadConfig, gameLabel, categoryLabel,
    submitQuoteForm, setBusy, showMessage, orderStatusLabel, recordStatusLabel, categoryMap,
    normalizeRecordNo, validRecordNo, getSavedRecords, rememberRecord, clearSavedRecords,
    getConfig: () => currentConfig
  };

  bindUploadFields();
  initResponsiveDetails();
  loadConfig().catch(() => {});
})();

