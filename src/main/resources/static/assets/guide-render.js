/**
 * 攻略正文 Markdown 渲染（轻量、无依赖、XSS 安全）
 *
 * 背景：服务端模板用 th:text 输出 section.bodyMarkdown，Markdown 源码被原样当纯文本显示，
 *       页面上直接出现 "- **武器升级**：..." 这类标记。
 * 方案：在浏览器端把已经很安全的 textContent 解析成真实 DOM 节点（不使用 innerHTML，
 *       因此不存在注入风险），只支持正文实际使用的语法子集：
 *         - 无序列表   "- xxx" / "* xxx"
 *         - 有序列表   "1. xxx"
 *         - 加粗       **xxx**
 *         - 空行分段
 */
(() => {
  const blocks = document.querySelectorAll('p.guide-prose');
  if (!blocks.length) return;

  const MD_HINT = /^\s*(?:[-*]\s|\d+\.\s)|\*\*/m;

  /** 把一行文本按 **加粗** 拆成文本节点与 <strong> */
  function appendInline(parent, text) {
    const parts = String(text).split(/(\*\*[^*]+\*\*)/g);
    for (const part of parts) {
      if (!part) continue;
      const bold = part.match(/^\*\*([^*]+)\*\*$/);
      if (bold) {
        const strong = document.createElement('strong');
        strong.textContent = bold[1];
        parent.append(strong);
      } else {
        parent.append(document.createTextNode(part));
      }
    }
  }

  for (const block of blocks) {
    const raw = block.textContent || '';
    if (!MD_HINT.test(raw)) continue; // 没有 Markdown 标记就保持原样

    const out = [];
    let list = null;
    let listTag = '';

    const flushList = () => {
      if (list) { out.push(list); list = null; listTag = ''; }
    };

    for (const line of raw.split(/\r?\n/)) {
      const text = line.trim();
      if (!text) { flushList(); continue; }

      const ul = text.match(/^[-*]\s+(.*)$/);
      const ol = text.match(/^\d+\.\s+(.*)$/);

      if (ul || ol) {
        const tag = ul ? 'ul' : 'ol';
        if (!list || listTag !== tag) {
          flushList();
          list = document.createElement(tag);
          listTag = tag;
        }
        const li = document.createElement('li');
        appendInline(li, ul ? ul[1] : ol[1]);
        list.append(li);
        continue;
      }

      flushList();
      const para = document.createElement('p');
      appendInline(para, text);
      out.push(para);
    }
    flushList();

    if (!out.length) continue;
    block.replaceChildren(...out);
    block.classList.add('guide-prose-rich');
  }
})();



/* ---------------------------------------------------------------------------
   攻略组件渲染：后台 sectionKey 与前台组件一一对应。
   仍以服务端纯文本输出为基础，前端只把约定格式转换为安全 DOM。
   --------------------------------------------------------------------------- */
(() => {
  const sections = document.querySelectorAll('.guide-section[data-guide-component]');
  if (!sections.length) return;

  const isSafeUrl = (value) => /^https?:\/\//i.test(String(value || '').trim()) || String(value || '').startsWith('/');
  const linesOf = (text) => String(text || '').split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  const splitCells = (line) => line.split('|').map((part) => part.trim());

  function textEl(tag, className, text) {
    const el = document.createElement(tag);
    if (className) el.className = className;
    el.textContent = text || '';
    return el;
  }

  function replaceProse(section, node) {
    const prose = section.querySelector('.guide-prose');
    if (prose && node) prose.replaceWith(node);
  }

  function renderGallery(section, prose) {
    const items = linesOf(prose.textContent).map(splitCells).filter((cells) => isSafeUrl(cells[0]));
    if (!items.length) return;
    const grid = document.createElement('div');
    grid.className = 'guide-component-gallery';
    items.forEach(([url, caption]) => {
      const figure = document.createElement('figure');
      const img = document.createElement('img');
      img.loading = 'lazy';
      img.src = url;
      img.alt = caption || '攻略图片';
      figure.append(img);
      if (caption) figure.append(textEl('figcaption', '', caption));
      grid.append(figure);
    });
    replaceProse(section, grid);
  }

  function renderVideo(section, prose) {
    const url = linesOf(prose.textContent)[0];
    if (!isSafeUrl(url)) return;
    const wrap = document.createElement('div');
    wrap.className = 'guide-component-video';
    if (/\.(mp4|webm)(\?.*)?$/i.test(url)) {
      const video = document.createElement('video');
      video.controls = true;
      video.preload = 'metadata';
      video.src = url;
      wrap.append(video);
    } else {
      const iframe = document.createElement('iframe');
      iframe.loading = 'lazy';
      iframe.referrerPolicy = 'no-referrer';
      iframe.allowFullscreen = true;
      iframe.src = url;
      wrap.append(iframe);
    }
    replaceProse(section, wrap);
  }

  function renderTable(section, prose) {
    const rows = linesOf(prose.textContent).map(splitCells).filter((cells) => cells.length > 1);
    if (!rows.length) return;
    const table = document.createElement('table');
    table.className = 'guide-component-table';
    rows.forEach((cells, index) => {
      const tr = document.createElement('tr');
      cells.forEach((cell) => tr.append(textEl(index === 0 ? 'th' : 'td', '', cell)));
      table.append(tr);
    });
    const wrap = document.createElement('div');
    wrap.className = 'guide-component-table-wrap';
    wrap.append(table);
    replaceProse(section, wrap);
  }

  function renderLinks(section, prose, className) {
    const rows = linesOf(prose.textContent).map(splitCells).filter((cells) => cells[0] && isSafeUrl(cells[1]));
    if (!rows.length) return;
    const list = document.createElement('div');
    list.className = className;
    rows.forEach(([label, url, note]) => {
      const a = document.createElement('a');
      a.href = url;
      a.target = '_blank';
      a.rel = 'noopener nofollow';
      a.append(textEl('strong', '', label));
      if (note) a.append(textEl('small', '', note));
      list.append(a);
    });
    replaceProse(section, list);
  }

  sections.forEach((section) => {
    const type = (section.dataset.guideComponent || '').toLowerCase();
    const prose = section.querySelector('.guide-prose');
    if (!prose) return;
    section.classList.add(`guide-component-${type || 'paragraph'}`);
    if (type === 'gallery') renderGallery(section, prose);
    else if (type === 'video') renderVideo(section, prose);
    else if (type === 'table') renderTable(section, prose);
    else if (type === 'attachment') renderLinks(section, prose, 'guide-component-links');
    else if (type === 'related') renderLinks(section, prose, 'guide-component-related-links');
  });
})();

/* ---------------------------------------------------------------------------
   攻略正文：目录锚点 + 长文阅读体验
   服务端模板只输出 <section class="guide-section"><h2>…</h2>…，没有 id，
   长文既没法跳转也没法定位。这里在客户端：
     ① 给每个 h2 生成稳定 id（支持分享 #段落 直达）
     ② 在正文前插入可折叠的目录
     ③ 加阅读进度条与「回到顶部」
   全部用 DOM API 构建，不使用 innerHTML。
   --------------------------------------------------------------------------- */
(() => {
  const body = document.querySelector('.guide-article-body');
  if (!body) return;

  const sections = [...body.querySelectorAll('.guide-section')];
  if (sections.length < 2) return;

  /** 中文标题不适合做 slug，用「小标题-序号」保守生成，冲突再加后缀 */
  const used = new Set();
  const items = [];
  sections.forEach((sec, i) => {
    const h = sec.querySelector('h2');
    if (!h) return;
    let id = 'sec-' + (i + 1);
    let n = 1;
    while (used.has(id)) id = 'sec-' + (i + 1) + '-' + n++;
    used.add(id);
    h.id = id;
    sec.id = id + '-wrap';
    items.push({ id, text: h.textContent.trim() || `第 ${i + 1} 节` });
  });
  if (!items.length) return;

  // ① 目录
  const toc = document.createElement('nav');
  toc.className = 'guide-toc';
  toc.setAttribute('aria-label', '本页目录');

  const toggle = document.createElement('button');
  toggle.type = 'button';
  toggle.className = 'guide-toc-toggle';
  toggle.setAttribute('aria-expanded', 'true');
  toggle.textContent = `目录（${items.length} 节）`;

  const list = document.createElement('ol');
  list.className = 'guide-toc-list';
  items.forEach((it) => {
    const li = document.createElement('li');
    const a = document.createElement('a');
    a.href = '#' + it.id;
    a.textContent = it.text;
    li.appendChild(a);
    list.appendChild(li);
  });

  toggle.addEventListener('click', () => {
    const open = toc.classList.toggle('collapsed');
    toggle.setAttribute('aria-expanded', String(!open));
  });

  toc.append(toggle, list);
  body.parentNode.insertBefore(toc, body);

  // ② 当前小节高亮
  const links = [...list.querySelectorAll('a')];
  const byId = new Map(links.map((a) => [a.getAttribute('href').slice(1), a]));
  if ('IntersectionObserver' in window) {
    const seen = new Map();
    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => seen.set(e.target.id, e.isIntersecting ? e.intersectionRatio : 0));
      let best = null;
      let bestRatio = 0;
      seen.forEach((ratio, id) => { if (ratio > bestRatio) { bestRatio = ratio; best = id; } });
      links.forEach((a) => a.classList.toggle('active', best !== null && a.getAttribute('href') === '#' + best));
    }, { rootMargin: '-96px 0px -60% 0px', threshold: [0, 0.25, 0.5, 1] });
    items.forEach((it) => { const el = document.getElementById(it.id); if (el) io.observe(el); });
  }

  // ③ 阅读进度 + 回到顶部
  const bar = document.createElement('div');
  bar.className = 'guide-progress';
  bar.setAttribute('aria-hidden', 'true');
  const fill = document.createElement('i');
  bar.appendChild(fill);
  document.body.appendChild(bar);

  const top = document.createElement('button');
  top.type = 'button';
  top.className = 'guide-to-top';
  top.textContent = '回到顶部';
  top.hidden = true;
  top.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
  document.body.appendChild(top);

  let raf = 0;
  const onScroll = () => {
    if (raf) return;
    raf = requestAnimationFrame(() => {
      raf = 0;
      const doc = document.documentElement;
      const max = Math.max(1, doc.scrollHeight - window.innerHeight);
      const ratio = Math.min(1, Math.max(0, (window.scrollY || doc.scrollTop || 0) / max));
      fill.style.width = (ratio * 100).toFixed(2) + '%';
      const article = body.closest('.guide-article');
      const past = article ? (window.scrollY || 0) > article.offsetTop + 400 : false;
      top.hidden = !past;
    });
  };
  window.addEventListener('scroll', onScroll, { passive: true });
  window.addEventListener('resize', onScroll);
  onScroll();
})();
