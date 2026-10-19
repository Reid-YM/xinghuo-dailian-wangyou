(() => {
  'use strict';
  const runtimeHref = value => window.XHFrontendShell?.runtimeHref?.(value) || value;
  const runtimePath = pathname => window.XHFrontendShell?.runtimePath?.(pathname) || pathname || location.pathname;

  const STAGE = 'N22 · GUIDE DETAIL PREVIEW';
  const DETAIL_RE = /^\/next\/guides\/([^/?#]+)\/?$/;
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const text = (value) => String(value ?? '').trim();
  const list = (value) => Array.isArray(value) ? value : [];
  let routeRequest = 0;

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function safeImage(value) {
    const url = text(value);
    if ((url.startsWith('/') && !url.startsWith('//')) || url.startsWith('https://')) return url;
    return '';
  }

  function detailRoute() {
    const match = runtimePath().match(DETAIL_RE);
    if (!match) return null;
    try { return decodeURIComponent(match[1]); } catch (_) { return match[1]; }
  }

  function installStyles() {
    if ($('#frontend-next-n22-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n22-style';
    style.textContent = `
      .n22-detail{min-height:100vh;padding:112px 0 84px;background:radial-gradient(circle at 80% 8%,#ffcb2d14,transparent 30rem)}
      .n22-breadcrumb{display:flex;gap:8px;align-items:center;flex-wrap:wrap;color:#858e98;font-size:11px;margin-bottom:18px}.n22-breadcrumb a{color:#d8bb61}.n22-breadcrumb span{opacity:.62}
      .n22-hero,.n22-section,.n22-side{border:1px solid #ffffff18;border-radius:18px;background:#0d1116;box-shadow:0 24px 62px #0006}.n22-hero{overflow:hidden;margin-bottom:16px}.n22-cover{min-height:260px;max-height:440px;background:radial-gradient(circle at 72% 28%,#ffcb2d2f,transparent 30%),linear-gradient(145deg,#211b12,#0b0e12);display:grid;place-items:center;overflow:hidden}.n22-cover img{display:block;width:100%;height:100%;min-height:260px;max-height:440px;object-fit:cover}.n22-hero-copy{padding:24px}.n22-kicker{color:#d9bb61;font-size:10px;font-weight:900;letter-spacing:.16em;text-transform:uppercase}.n22-title{font-size:clamp(36px,5vw,62px);line-height:1.04;letter-spacing:-.045em;margin:12px 0}.n22-summary{color:#a7afb8;line-height:1.8;max-width:900px}.n22-meta{display:flex;gap:8px;flex-wrap:wrap;margin-top:18px}.n22-chip{border:1px solid #ffffff16;border-radius:999px;padding:7px 10px;color:#9099a3;font-size:10px;background:#ffffff05}
      .n22-layout{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:16px;align-items:start}.n22-content{display:grid;gap:12px}.n22-section{padding:22px}.n22-section h2{font-size:24px;margin:0 0 13px}.n22-body{display:grid;gap:9px;color:#b0b7bf;line-height:1.82}.n22-body p{margin:0}.n22-body ul{margin:0;padding-left:20px;display:grid;gap:7px}.n22-image{margin-top:16px;border:1px solid #ffffff10;border-radius:13px;overflow:hidden}.n22-image img{display:block;width:100%;max-height:520px;object-fit:contain;background:#080b0e}
      .n22-side{padding:18px;position:sticky;top:94px}.n22-side h3{margin:5px 0 12px}.n22-actions{display:grid;gap:8px}.n22-actions .btn{text-align:center}.n22-note{margin-top:14px;padding-top:14px;border-top:1px solid #ffffff10;color:#7e8791;font-size:10px;line-height:1.7}.n22-note a{color:#d9bb61}
      .n22-products,.n22-related{display:grid;gap:9px}.n22-product,.n22-related a{display:block;border:1px solid #ffffff12;border-radius:12px;padding:13px;background:#0a0e12}.n22-product strong,.n22-related strong{display:block;margin-bottom:5px}.n22-product p,.n22-related span{margin:0;color:#89929c;font-size:10px;line-height:1.65}.n22-product-actions{display:flex;gap:7px;margin-top:10px}.n22-product-actions a{font-size:10px;padding:8px 10px}.n22-faq{border-bottom:1px solid #ffffff10;padding:10px 0}.n22-faq:last-child{border-bottom:0}.n22-faq summary{cursor:pointer;font-weight:700}.n22-faq p{color:#949da7;line-height:1.75;margin:9px 0 0}.n22-loading{min-height:320px;display:grid;place-items:center;text-align:center;color:#9aa2ac}.n22-error h1{font-size:36px;margin:10px 0}.n22-error p{color:#9aa2ac;line-height:1.7}.n22-error-actions{display:flex;gap:9px;flex-wrap:wrap;margin-top:16px}
      .n22-detail .gc-root{--xh-line:var(--line);--xh-line-strong:#ffffff38;--xh-surface-2:#090d11;--xh-text:var(--txt);--xh-text-muted:var(--muted);--xh-gold:var(--gold);--xh-danger:#ff9b87;--xh-warning:#d8bb61;min-width:0;overflow-wrap:anywhere}
      .n22-detail .gc-form[hidden]{display:none}.n22-detail .gc-comment-actions button{min-height:44px;min-width:44px}.n22-detail .gc-form .btn{min-height:44px}
      @media(max-width:880px){.n22-detail{padding-top:92px}.n22-layout{grid-template-columns:1fr}.n22-side{position:relative;top:auto}.n22-cover,.n22-cover img{min-height:210px}}
      @media(max-width:560px){.n22-hero-copy,.n22-section{padding:16px}.n22-title{font-size:38px}.n22-error-actions{display:grid}.n22-product-actions{display:grid}}
    `;
    document.head.append(style);
  }

  function activateDetailPage(section) {
    $$('.page').forEach((page) => page.classList.toggle('active', page === section));
    $$('.navlink[data-go]').forEach((link) => {
      link.classList.remove('active');
      link.removeAttribute('aria-current');
    });
    document.documentElement.dataset.frontendPage = 'guide-detail';
    document.documentElement.dataset.frontendNextStage = 'n22-guide-detail';
  }

  function ensureDetailSection() {
    let section = $('#n22GuideDetail');
    if (section) return section;
    section = node('section', 'page n22-detail');
    section.id = 'n22GuideDetail';
    section.setAttribute('role', 'main');
    section.tabIndex = -1;
    section.dataset.page = 'guide-detail';
    section.append(node('div', 'shell'));
    const footer = $('.next-footer') || $('.footer');
    if (footer) footer.before(section); else document.body.append(section);
    return section;
  }

  function renderLoading(section, slug) {
    const shell = $('.shell', section);
    if (!shell) return;
    const state = node('div', 'n22-section n22-loading', `正在读取 ${slug || '攻略'}…`);
    state.setAttribute('role', 'status');
    state.setAttribute('aria-live', 'polite');
    shell.replaceChildren(state);
  }

  function setMetadata(guide) {
    const brand = text(window.XHFrontendShell?.configuration?.brandName) || '星火网游';
    const slug = text(guide?.slug);
    document.title = `${text(guide?.seoTitle) || text(guide?.title) || '攻略详情'} · ${brand}`;
    let description = $('meta[name="description"]');
    if (!description) {
      description = document.createElement('meta');
      description.name = 'description';
      document.head.append(description);
    }
    description.content = text(guide?.seoDescription) || text(guide?.summary) || '游戏攻略详情';
    let canonical = $('link[rel="canonical"]');
    if (!canonical) {
      canonical = document.createElement('link');
      canonical.rel = 'canonical';
      document.head.append(canonical);
    }
    canonical.href = `https://wydlgf.cn/guides/${encodeURIComponent(slug)}`;
    let robots = $('meta[name="robots"]');
    if (!robots) {
      robots = document.createElement('meta');
      robots.name = 'robots';
      document.head.append(robots);
    }
    if (window.XHFrontendShell?.isPreview === false) robots.remove();
    else robots.content = 'noindex,follow';
  }

  function appendMarkdownText(root, raw) {
    const lines = String(raw ?? '').replace(/\r/g, '').split('\n');
    let listNode = null;
    const flushList = () => { listNode = null; };
    lines.forEach((line) => {
      const value = line.trim();
      if (!value) { flushList(); return; }
      const bullet = /^[-*+]\s+(.+)$/.exec(value);
      if (bullet) {
        if (!listNode) {
          listNode = document.createElement('ul');
          root.append(listNode);
        }
        listNode.append(node('li', '', bullet[1]));
        return;
      }
      flushList();
      const heading = /^#{1,4}\s+(.+)$/.exec(value);
      if (heading) {
        root.append(node('strong', '', heading[1]));
        return;
      }
      root.append(node('p', '', value));
    });
    if (!root.children.length) root.append(node('p', '', '暂无正文内容。'));
  }

  function renderSection(section) {
    const card = node('section', 'n22-section');
    card.append(node('h2', '', text(section?.heading) || '攻略步骤'));
    const body = node('div', 'n22-body');
    appendMarkdownText(body, section?.bodyMarkdown);
    card.append(body);
    const imageUrl = safeImage(section?.imageUrl);
    if (imageUrl) {
      const wrap = node('figure', 'n22-image');
      const image = document.createElement('img');
      image.src = imageUrl;
      image.alt = text(section?.imageAlt) || text(section?.heading) || '攻略配图';
      image.loading = 'lazy';
      wrap.append(image);
      card.append(wrap);
    }
    return card;
  }

  function renderProducts(products) {
    if (!products.length) return null;
    const card = node('section', 'n22-section');
    card.append(node('div', 'n22-kicker', 'Related Services'), node('h2', '', '相关服务'));
    const grid = node('div', 'n22-products');
    products.forEach((product) => {
      const slug = text(product?.slug);
      if (!slug) return;
      const item = node('div', 'n22-product');
      item.append(node('strong', '', text(product?.label) || text(product?.name) || '相关服务'));
      item.append(node('p', '', text(product?.description) || text(product?.shortDesc) || '查看服务详情与询价信息。'));
      const actions = node('div', 'n22-product-actions');
      const detail = node('a', 'btn', '查看服务');
      detail.href = `/next/service/${encodeURIComponent(slug)}`;
      const quote = node('a', 'btn gold', '询价');
      quote.href = `/next/quote?service=${encodeURIComponent(slug)}`;
      actions.append(detail, quote);
      item.append(actions);
      grid.append(item);
    });
    if (!grid.children.length) return null;
    card.append(grid);
    return card;
  }

  function renderFaqs(faqs) {
    if (!faqs.length) return null;
    const card = node('section', 'n22-section');
    card.append(node('div', 'n22-kicker', 'FAQ'), node('h2', '', '常见问题'));
    faqs.forEach((faq) => {
      const details = node('details', 'n22-faq');
      details.append(node('summary', '', text(faq?.question) || '常见问题'), node('p', '', text(faq?.answer) || '暂无说明。'));
      card.append(details);
    });
    return card;
  }

  function renderRelated(items) {
    if (!items.length) return null;
    const box = node('div', 'n22-related');
    items.forEach((item) => {
      const slug = text(item?.slug);
      if (!slug) return;
      const link = document.createElement('a');
      link.href = `/next/guides/${encodeURIComponent(slug)}`;
      link.append(node('strong', '', text(item?.title) || '相关攻略'), node('span', '', text(item?.gameName) || text(item?.gameCode) || '继续阅读'));
      box.append(link);
    });
    return box.children.length ? box : null;
  }

  function renderCommunity(slug) {
    const root = node('section', 'n22-section gc-root');
    root.dataset.guideCommunity = '';
    root.dataset.guideSlug = slug;
    const head = node('div', 'gc-head');
    head.append(node('h2', '', '玩家讨论'));
    const favorite = node('button', 'btn', '');
    favorite.type = 'button'; favorite.dataset.favoriteToggle = '';
    const label = node('span', '', '收藏本篇'); label.dataset.favoriteLabel = '';
    favorite.append(label); head.append(favorite);
    const note = node('p', 'gc-note', '正在加载讨论…');
    note.dataset.communityNote = ''; note.setAttribute('role', 'status');
    const form = document.createElement('form'); form.className = 'gc-form';
    form.dataset.commentForm = ''; form.hidden = true;
    const fieldLabel = node('label', 'gc-label', '写下你的经验或补充');
    fieldLabel.htmlFor = 'n22CommentInput';
    const input = document.createElement('textarea');
    input.id = 'n22CommentInput'; input.className = 'gc-input'; input.maxLength = 2000; input.rows = 3;
    const error = node('p', 'gc-error'); error.dataset.commentError = ''; error.hidden = true;
    error.setAttribute('role', 'alert');
    const submit = node('button', 'btn gold', '发表评论');
    submit.type = 'submit'; submit.dataset.commentSubmit = '';
    form.append(fieldLabel, input, error, submit);
    const comments = node('div', 'gc-list'); comments.dataset.commentList = '';
    root.append(head, note, form, comments);
    return root;
  }

  function renderDetail(section, detail) {
    const shell = $('.shell', section);
    if (!shell) return;
    const guide = detail?.guide || {};
    const slug = text(guide.slug) || detailRoute() || '';
    setMetadata(guide);

    const breadcrumb = node('nav', 'n22-breadcrumb');
    breadcrumb.setAttribute('aria-label', '面包屑');
    const home = node('a', '', '首页');
    home.href = '/next';
    const legacyList = node('a', '', '攻略中心');
    legacyList.href = runtimeHref('/next/guides');
    breadcrumb.append(home, node('span', '', '›'), legacyList, node('span', '', '›'), node('span', '', text(guide.title) || '攻略详情'));

    const hero = node('article', 'n22-hero');
    const cover = node('div', 'n22-cover');
    const coverUrl = safeImage(guide.coverImage);
    if (coverUrl) {
      const image = document.createElement('img');
      image.src = coverUrl;
      image.alt = `${text(guide.title) || '攻略'}封面`;
      image.loading = 'eager';
      cover.append(image);
    } else {
      cover.append(node('span', 'n22-kicker', text(guide.gameName) || text(guide.gameCode) || 'Game Guide'));
    }
    const copy = node('div', 'n22-hero-copy');
    copy.append(node('div', 'n22-kicker', `${text(guide.gameName) || text(guide.gameCode) || '游戏攻略'} · GUIDE`));
    copy.append(node('h1', 'n22-title', text(guide.title) || '攻略详情'));
    if (text(guide.subtitle)) copy.append(node('p', 'n22-summary', text(guide.subtitle)));
    copy.append(node('p', 'n22-summary', text(guide.summary) || '查看完整攻略步骤、相关服务与常见问题。'));
    const meta = node('div', 'n22-meta');
    if (text(guide.difficulty)) meta.append(node('span', 'n22-chip', `难度 · ${text(guide.difficulty)}`));
    if (Number(guide.estimatedMinutes) > 0) meta.append(node('span', 'n22-chip', `预计阅读 · ${Number(guide.estimatedMinutes)} 分钟`));
    if (text(guide.authorName)) meta.append(node('span', 'n22-chip', `作者 · ${text(guide.authorName)}`));
    list(guide.tags).slice(0, 8).forEach((tag) => { if (text(tag)) meta.append(node('span', 'n22-chip', text(tag))); });
    copy.append(meta);
    hero.append(cover, copy);

    const layout = node('div', 'n22-layout');
    const content = node('div', 'n22-content');
    list(detail?.sections).forEach((row) => content.append(renderSection(row)));
    const products = renderProducts(list(detail?.products));
    if (products) content.append(products);
    const faqs = renderFaqs(list(detail?.faqs));
    if (faqs) content.append(faqs);
    if (!content.children.length) {
      const empty = node('section', 'n22-section');
      empty.append(node('p', 'n22-summary', '这篇攻略暂时没有可展示的正文内容。'));
      content.append(empty);
    }

    const aside = node('aside', 'n22-side');
    aside.append(node('div', 'n22-kicker', 'Continue'), node('h3', '', '继续浏览'));
    const actions = node('div', 'n22-actions');
    const services = node('a', 'btn gold', '查看服务大厅');
    services.href = '/next/services';
    const guideList = node('a', 'btn', '攻略中心（旧版）');
    guideList.href = '/guides';
    guideList.dataset.n22Legacy = '1';
    actions.append(services, guideList);
    aside.append(actions);
    const related = renderRelated(list(detail?.relatedGuides));
    if (related) {
      aside.append(node('h3', '', '相关攻略'), related);
    }
    const note = node('div', 'n22-note');
    note.append(document.createTextNode('也可打开 '));
    const legacy = node('a', '', '旧版攻略详情');
    legacy.href = `/guides/${encodeURIComponent(slug)}`;
    legacy.dataset.n22Legacy = '1';
    note.append(legacy, document.createTextNode('。'));
    aside.append(note);

    const community = renderCommunity(slug);
    content.append(community);
    layout.append(content, aside);
    shell.replaceChildren(breadcrumb, hero, layout);
    window.XHGuideCommunity?.mount(community);
  }

  function renderError(section, message, slug) {
    const shell = $('.shell', section);
    if (!shell) return;
    const card = node('div', 'n22-section n22-error');
    card.append(node('div', 'n22-kicker', 'Guide Detail'), node('h1', '', '暂时无法显示此攻略'), node('p', '', message));
    const actions = node('div', 'n22-error-actions');
    const retry = node('button', 'btn gold', '重新读取');
    retry.type = 'button';
    retry.dataset.n22Retry = '1';
    const legacy = node('a', 'btn', '打开旧版攻略');
    legacy.href = `/guides/${encodeURIComponent(slug || '')}`;
    legacy.dataset.n22Legacy = '1';
    actions.append(retry, legacy);
    card.append(actions);
    shell.replaceChildren(card);
  }

  async function getJson(path) {
    const response = await fetch(path, {
      headers: { Accept: 'application/json' },
      credentials: 'same-origin',
      cache: 'no-store'
    });
    if (!response.ok) throw new Error(response.status === 404 ? '没有找到对应的公开攻略，可能已下线或链接已更新。' : `攻略读取失败（HTTP ${response.status}）。`);
    return response.json();
  }

  async function syncRoute() {
    const request = ++routeRequest;
    const slug = detailRoute();
    if (!slug) return;
    installStyles();
    const section = ensureDetailSection();
    activateDetailPage(section);
    renderLoading(section, slug);
    try {
      const detail = await getJson(`/api/guides/${encodeURIComponent(slug)}`);
      if (request !== routeRequest || detailRoute() !== slug) return;
      renderDetail(section, detail);
    } catch (error) {
      if (request !== routeRequest || detailRoute() !== slug) return;
      renderError(section, error?.message || '攻略详情读取失败，请稍后重试。', slug);
    }
  }

  document.addEventListener('click', (event) => {
    const target = event.target instanceof Element ? event.target : null;
    if (target?.closest('[data-n22-retry]')) syncRoute();
  });

  window.addEventListener('popstate', syncRoute);

  window.XHFrontendNextN22 = {
    stage: STAGE,
    detailRoute,
    syncRoute
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', syncRoute, { once: true });
  else syncRoute();
})();
