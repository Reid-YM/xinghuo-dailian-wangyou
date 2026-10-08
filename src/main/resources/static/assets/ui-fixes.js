/**
 * 前端交互补丁（零依赖）
 *
 * 1) 移动端主导航抽屉：线上在 ≤760px 时用 CSS 隐藏了 .nav 里的全部链接，
 *    底部 mobile-dock 又只在滚动后出现，导致「业务记录」「一对一咨询」等
 *    入口在手机上完全到不了。
 *    按钮与抽屉已直接写在模板里（不依赖 JS 也能渲染），本脚本负责接线；
 *    若模板里没有，则自动补建，保证任何页面都能用。
 *
 * 2) 横向滚动提示：筛选行、标签行、步骤条等 overflow-x:auto 容器在手机上被
 *    视口硬切断且没有任何提示（出现「Xbo」「武器升级服」这类残字）。这里只在
 *    容器确实溢出时给它加渐隐遮罩，不溢出时不加，避免误伤。
 *
 * 3) 破坏性操作二次确认：「清除浏览器记录」原先一点即清空，没有任何确认。
 *    在捕获阶段拦截，拒绝时不执行原有处理逻辑。
 */
(() => {
  /* ---------------------------------------------------------------- 1. 导航抽屉 */
  // 兜底链接：即使页头 .nav 结构与预期不一致，抽屉也能给出完整入口
  const FALLBACK_LINKS = [
    ['/services', '服务'],
    ['/guides', '攻略'],
    ['/mastery', 'MW4 工具站'],
    ['/records', '业务记录'],
    ['/account', '我的'],
    ['/contact', '一对一咨询'],
  ];

  function buildNavDrawer() {
    try {
      const header = document.querySelector('.site-header');
      if (!header) return;

      const nav = header.querySelector('.nav');
      let links = [];
      if (nav) {
        links = [...nav.querySelectorAll('a')]
          .filter((a) => !a.classList.contains('btn'))
          .map((a) => [a.getAttribute('href') || '/', (a.textContent || '').trim()])
          .filter((pair) => pair[0] && pair[1]);
      }
      if (!links.length) links = FALLBACK_LINKS;

      // 模板已写好的按钮直接复用；缺失时才创建
      let toggle = header.querySelector('.nav-toggle');
      if (!toggle) {
        toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'nav-toggle';
        toggle.setAttribute('aria-label', '打开导航菜单');
        for (let i = 0; i < 3; i += 1) toggle.append(document.createElement('span'));
      }
      toggle.setAttribute('aria-expanded', 'false');
      toggle.setAttribute('aria-controls', 'mobileNavDrawer');

      let drawer = header.querySelector('#mobileNavDrawer');
      if (!drawer) {
        drawer = document.createElement('div');
        drawer.className = 'nav-drawer';
        drawer.id = 'mobileNavDrawer';
        header.append(drawer);
      }
      drawer.hidden = true;

      const list = document.createElement('div');
      list.className = 'nav-drawer-links';
      links.forEach(([href, label]) => {
        const item = document.createElement('a');
        item.href = href;
        item.textContent = label;
        list.append(item);
      });

      const quote = document.createElement('a');
      quote.className = 'btn primary nav-drawer-cta';
      quote.href = '/quote';
      quote.textContent = '提交询价';
      list.append(quote);

      drawer.replaceChildren(list);

      const topbar = header.querySelector('.topbar') || header;
      if (!toggle.parentElement) topbar.append(toggle);

      if (toggle.dataset.navReady === '1') return;
      toggle.dataset.navReady = '1';

      const setOpen = (open) => {
        drawer.hidden = !open;
        toggle.classList.toggle('is-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? '关闭导航菜单' : '打开导航菜单');
      };

      toggle.addEventListener('click', () => setOpen(drawer.hidden));
      drawer.addEventListener('click', (event) => {
        if (event.target.closest('a')) setOpen(false);
      });
      document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') setOpen(false);
      });
      const wide = window.matchMedia('(min-width: 761px)');
      const onChange = (event) => { if (event.matches) setOpen(false); };
      if (wide.addEventListener) wide.addEventListener('change', onChange);
      else if (wide.addListener) wide.addListener(onChange);
    } catch (error) {
      /* 导航补丁失败不应影响其余补丁 */
    }
  }

  buildNavDrawer();
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', buildNavDrawer);
  }

  /* ------------------------------------------------------- 2. 横向滚动提示 */
  const SCROLLERS = [
    '.filters', '.trust', '.hero-flow', '.page-stepbar', '.service-filterbar',
    '.game-filter-rail', '.platform-filter-rail', '.browser-row .filters',
    '.home-proof-strip', '.hot-rail', '.home-game-rail', '.mobile-x-scroll',
    '.actions', '.row-actions', '.sidebar-nav',
  ].join(',');

  const markOverflow = (el) => {
    const more = el.scrollWidth - el.clientWidth;
    el.classList.toggle('x-overflow', more > 4);
    el.classList.toggle('x-scrolled', more > 4 && el.scrollLeft > 2);
  };

  const bindScrollers = () => {
    document.querySelectorAll(SCROLLERS).forEach((el) => {
      if (el.dataset.xHint === '1') { markOverflow(el); return; }
      el.dataset.xHint = '1';
      el.addEventListener('scroll', () => markOverflow(el), { passive: true });
      markOverflow(el);
    });
  };

  bindScrollers();
  window.addEventListener('resize', bindScrollers, { passive: true });
  window.addEventListener('load', bindScrollers);
  window.setTimeout(bindScrollers, 800);

  /* ------------------------------------------------- 3. 破坏性操作二次确认 */
  document.addEventListener('click', (event) => {
    const target = event.target instanceof Element ? event.target.closest('button,a') : null;
    if (!target || target.id !== 'clearLocalRecords') return;
    if (!window.confirm('确定清除本机浏览器里保存的业务记录吗？\n\n只影响当前浏览器，服务器上的记录不受影响。')) {
      event.preventDefault();
      event.stopImmediatePropagation();
    }
  }, true);
})();

/* ---------------------------------------------------------------------------
   联系页：去掉「主客服」这类主次标签。
   public.js 渲染渠道卡片时会写入 label（配置默认值为「主客服」），
   这里在渲染后把它改成中性的渠道名，避免出现主次之分。
   --------------------------------------------------------------------------- */
(function () {
  const NEUTRAL = { '主客服': '微信', '主账号': '微信', '默认客服': '微信' };
  const fix = () => {
    document.querySelectorAll('#contactChannelGrid .tag').forEach((tag) => {
      const key = (tag.textContent || '').trim();
      if (NEUTRAL[key]) tag.textContent = NEUTRAL[key];
    });
  };
  fix();
  // public.js 是 defer 脚本，渲染时机不定，这里再补两次
  if (document.readyState !== 'complete') window.addEventListener('load', fix);
  setTimeout(fix, 1200);
  setTimeout(fix, 3000);
})();

/* 联系页：把微信二维码挪到所有渠道卡片之后，
   避免它插在卡片中间、把并排的渠道网格切断。 */
(function () {
  const qr = document.getElementById('wechatQr');
  const consoleBox = document.querySelector('.contact-console');
  if (!qr || !consoleBox) return;
  const notice = consoleBox.querySelector('.contact-safety');
  if (notice) consoleBox.insertBefore(qr, notice);
  else consoleBox.appendChild(qr);
})();

/* ---------------------------------------------------------------------------
   首页 Hero：「当前推荐」模块原本只有静态文案（<strong>热门任务</strong>），
   下方 127px 全是空白，看起来像坏掉了。
   这里用站点配置里的 hotServices 渲染真实推荐条目；
   若没有推荐数据则整块隐藏，不留一个空标签。
   --------------------------------------------------------------------------- */
(function () {
  const copy = document.querySelector('.hero-media-panel .hero-media-copy');
  if (!copy) return;

  const priceText = (s) => {
    if (!s) return '';
    if (s.priceMode === 'fixed' && s.priceFixed != null) return `¥${s.priceFixed}`;
    if (s.priceMin != null && s.priceMax != null) return `¥${s.priceMin}–${s.priceMax}`;
    if (s.priceMin != null) return `¥${s.priceMin} 起`;
    return '按需求报价';
  };

  fetch('/api/site-config', { cache: 'no-store' })
    .then((r) => (r.ok ? r.json() : null))
    .then((config) => {
      const hot = Array.isArray(config?.hotServices) ? config.hotServices.slice(0, 2) : [];
      if (!hot.length) { copy.remove(); return; }

      const list = document.createElement('div');
      list.className = 'hero-hot-list';

      hot.forEach((s) => {
        const a = document.createElement('a');
        a.className = 'hero-hot-item';
        a.href = `/service/${encodeURIComponent(s.slug || '')}`;

        const b = document.createElement('b');
        b.textContent = s.name || '热门任务';
        a.appendChild(b);

        if (s.shortDesc) {
          const small = document.createElement('small');
          small.textContent = s.shortDesc;
          a.appendChild(small);
        }

        const em = document.createElement('em');
        em.textContent = priceText(s);
        a.appendChild(em);

        list.appendChild(a);
      });

      copy.textContent = '';
      const eyebrow = document.createElement('span');
      eyebrow.className = 'eyebrow';
      eyebrow.textContent = '当前推荐';
      copy.appendChild(eyebrow);
      copy.appendChild(list);
    })
    .catch(() => {});
})();

/* ---------------------------------------------------------------------------
   工具站「迷彩预览图」：原来所有武器共用同一张模式图（previewImage 是
   游戏级配置），点开哪把武器看到的都一样，而且内容是普通游戏场景，
   与「迷彩」无关。
   改成双层预览：模式图退为底衬，上层叠加当前选中武器的渲染图，
   做到「每个模式不同 + 每把武器不同」。
   --------------------------------------------------------------------------- */
(function () {
  if (!document.querySelector('.tool-hero') && !document.getElementById('weaponList')) return;

  // 幂等：每次都以当前 DOM 为准重新判断，不做任何"上次是什么"的短路缓存。
  // （mastery.js 切换模式/武器时会重建 .camo-mode-preview-art 元素，
  //   任何缓存都会让新元素拿不到叠加图。）
  function sync() {
    const active = document.querySelector('.weapon-row.active');
    const art = document.querySelector('.camo-mode-preview-art');
    if (!active || !art) return;

    const code = active.dataset.weapon;
    if (!code) return;

    // 图片地址直接取 .weapon-mark 的 computed background-image，
    // 不再自己拼文件名 —— 有 11 张是不透明底、扩展名是 .jpg，
    // 写死 .png 会 404，导致预览只剩底色、武器不见。
    const mark = active.querySelector('.weapon-mark');
    const bg = mark ? getComputedStyle(mark).backgroundImage : '';
    if (bg && bg !== 'none' && art.style.getPropertyValue('--weapon-preview') !== bg) {
      art.style.setProperty('--weapon-preview', bg);
    }
    if (!art.classList.contains('weapon-preview')) art.classList.add('weapon-preview');
    if (art.dataset.weaponCode !== code) art.dataset.weaponCode = code;

    const name = active.querySelector('.weapon-row-main b');
    art.setAttribute('data-weapon-name', name ? name.textContent.trim() : code);
  }

  // 轮询保证正确性（不依赖事件时序），MutationObserver 让其更及时。
  new MutationObserver(sync).observe(document.body, { childList: true, subtree: true });
  document.addEventListener('click', () => setTimeout(sync, 120), true);
  setInterval(sync, 600);
  sync();
})();

/* ---------------------------------------------------------------------------
   联系页：微信行保持显示。
   V56 起联系方式按「微信/QQ、电话/客服时间、淘宝/闲鱼」两列展示，
   微信必须是其中一项，因此不再隐藏 [data-contact-row="wechat"]。
   --------------------------------------------------------------------------- */

/* ---------------------------------------------------------------------------
   工具站 02 面板：空状态（未选武器时）原本只有两行字，在 970×820px 的
   面板里显得大片留白。这里补一排「快捷选枪」，直接复用左侧武器列表的数据，
   点了就等于点左侧对应的武器行，空状态因此变得可用。
   --------------------------------------------------------------------------- */
(function () {
  if (!document.getElementById('progressSuggest')) return;

  const PICK = 6;
  let filled = false;

  function fill() {
    const box = document.getElementById('progressSuggest');
    const empty = document.getElementById('progressEmpty');
    if (!box || !empty || empty.classList.contains('hidden')) return;
    if (filled && box.children.length) return;

    const rows = [...document.querySelectorAll('.weapon-row')].slice(0, PICK);
    if (!rows.length) return;

    box.textContent = '';
    rows.forEach((row) => {
      const code = row.dataset.weapon;
      if (!code) return;
      const name = row.querySelector('.weapon-row-main b');
      const type = row.querySelector('.weapon-row-main small');

      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'progress-suggest-item';
      // 同样不要自己拼后缀：11 把已转成 .jpg，写死 .png 会 404。
      // 直接复用左侧那一行 .weapon-mark 的图片地址。
      const mark = row.querySelector('.weapon-mark');
      const img = mark ? getComputedStyle(mark).backgroundImage : '';
      if (img && img !== 'none') btn.style.setProperty('--suggest-weapon', img);
      btn.title = `选择 ${name ? name.textContent.trim() : code}`;

      const art = document.createElement('span');
      art.className = 'progress-suggest-art';

      const label = document.createElement('b');
      label.textContent = name ? name.textContent.trim() : code;
      const sub = document.createElement('small');
      sub.textContent = type ? type.textContent.trim() : '';

      btn.append(art, label, sub);
      btn.addEventListener('click', () => row.click());
      box.appendChild(btn);
    });

    filled = box.children.length > 0;
  }

  new MutationObserver(fill).observe(document.body, { childList: true, subtree: true });
  setInterval(fill, 900);
  fill();
})();

/* ---------------------------------------------------------------------------
   工具站迷彩预览：悬停/点击某个迷彩时，预览切换成「该迷彩的花纹本身」。
   迷彩项是 <input data-camo-item="GOLD_TIGER" …>，其代码与 /assets/camo/
   下的图案文件一一对应，所以不需要改基线 JS。
   --------------------------------------------------------------------------- */
(function () {
  const art = () => document.querySelector('.camo-mode-preview-art');
  // 迷彩条目代码 → 纹理文件。MW4 的条目代码改成了 MASTERY_1/2/3
  // （每个模式名称不同），但纹理文件仍是原来的那几张，所以要显式映射，
  // 否则会去请求不存在的 /assets/camo/MASTERY_1.jpg（404 空白）。
  const CAMO_TEX = {
    BASE: 'BASE',
    SPECIAL_1: 'SPECIAL_1',
    SPECIAL_2: 'SPECIAL_2',
    MASTERY_1: 'GOLD_TIGER',
    MASTERY_2: 'KINGS_RANSOM',
    MASTERY_3: 'CATALYST',
  };

  function show(code) {
    const box = art();
    const tex = CAMO_TEX[code];
    if (!box || !tex) return;
    box.style.setProperty('--camo-pattern', `url("/assets/camo/${tex}.jpg")`);
    box.classList.add('camo-focus');
  }
  function clear() {
    const box = art();
    if (box) box.classList.remove('camo-focus');
  }

  // 事件委托：迷彩项是动态渲染的，不能直接绑
  document.addEventListener('mouseover', (e) => {
    const label = e.target.closest && e.target.closest('.camo-check');
    if (!label) return;
    const input = label.querySelector('input[data-camo-item]');
    if (input) show(input.dataset.camoItem);
  }, true);

  document.addEventListener('mouseout', (e) => {
    if (e.target.closest && e.target.closest('.camo-check')) clear();
  }, true);

  document.addEventListener('click', (e) => {
    const label = e.target.closest && e.target.closest('.camo-check');
    if (!label) return;
    const input = label.querySelector('input[data-camo-item]');
    if (input) {
      show(input.dataset.camoItem);
      // 触屏没有 hover，点完停留 2.4s 再回到武器图
      clearTimeout(show._t);
      show._t = setTimeout(clear, 2400);
    }
  }, true);

  // 键盘可达
  document.addEventListener('focusin', (e) => {
    const input = e.target.closest && e.target.closest('input[data-camo-item]');
    if (input) show(input.dataset.camoItem);
  });
  document.addEventListener('focusout', clear);
})();


/* 账号入口：未登录显示“登录”，登录后显示头像 + 昵称。 */
(function () {
  const entry = document.querySelector('[data-account-entry]');
  if (!entry) return;
  fetch('/api/account/me', { credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' } })
    .then((r) => (r.ok ? r.json() : null))
    .then((me) => {
      if (!me) return;
      entry.href = '/console';
      entry.classList.add('account-entry-authed');
      entry.textContent = '';
      const avatar = document.createElement('span');
      avatar.className = 'account-entry-avatar';
      if (me.avatarUrl) avatar.style.backgroundImage = `url("${String(me.avatarUrl).replace(/"/g, '%22')}")`;
      else avatar.textContent = String(me.displayName || '星').trim().slice(0, 1) || '星';
      const name = document.createElement('span');
      name.className = 'account-entry-name';
      name.textContent = me.displayName || me.publicUserId || '控制台';
      entry.append(avatar, name);
    })
    .catch(() => {});
})();


/* 语言按钮：当前暂无完整英文站，避免右上角 EN 跳到 403/错误页。 */
(function () {
  function patchLang() {
    document.querySelectorAll('.language-switcher a, a.lang-current').forEach((node) => {
      if (node.dataset.langPatched === '1') return;
      node.dataset.langPatched = '1';
      node.setAttribute('role', 'button');
      node.setAttribute('aria-label', '英文版暂未开放');
      node.setAttribute('title', '英文版暂未开放');
      node.addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        const old = node.textContent;
        node.textContent = '中文';
        clearTimeout(node._langTimer);
        node._langTimer = setTimeout(() => { node.textContent = old || 'EN'; }, 1200);
      }, true);
    });
  }
  patchLang();
  new MutationObserver(patchLang).observe(document.documentElement, { childList: true, subtree: true });
})();
