(() => {
  'use strict';

  const match = /^\/next\/(rules|privacy)\/?$/.exec(location.pathname);
  if (!match) return;

  const STAGE = 'N28 · LEGAL SINGLE SOURCE';
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const text = (value) => String(value ?? '').trim();
  let requestToken = 0;

  function currentDoc() {
    const current = /^\/next\/(rules|privacy)\/?$/.exec(location.pathname);
    return current ? current[1] : '';
  }

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function safeLink(value) {
    const raw = text(value);
    if (raw.startsWith('/') && !raw.startsWith('//')) return raw;
    try {
      const url = new URL(raw);
      if (url.protocol === 'https:' && !url.username && !url.password) return url.href;
    } catch (_) {}
    return '';
  }

  function nextLegalHref(value) {
    const safe = safeLink(value);
    if (!safe) return '';
    try {
      const url = new URL(safe, location.origin);
      if (url.origin !== location.origin) return safe;
      if (url.pathname === '/rules') return '/next/rules' + url.search + url.hash;
      if (url.pathname === '/privacy') return '/next/privacy' + url.search + url.hash;
      if (url.pathname === '/contact') return '/next/contact' + url.search + url.hash;
      if (url.pathname === '/trust') return '/next/trust' + url.search + url.hash;
      if (url.pathname === '/records') return '/next/records' + url.search + url.hash;
      if (url.pathname === '/quote') return '/next/quote' + url.search + url.hash;
      return url.pathname + url.search + url.hash;
    } catch (_) {
      return '';
    }
  }

  function cloneInline(source, target) {
    source.childNodes.forEach((child) => {
      if (child.nodeType === Node.TEXT_NODE) {
        target.append(document.createTextNode(child.textContent || ''));
        return;
      }
      if (!(child instanceof Element)) return;
      const tag = child.tagName.toLowerCase();
      if (tag === 'a') {
        const href = nextLegalHref(child.getAttribute('href'));
        if (!href) {
          target.append(document.createTextNode(child.textContent || ''));
          return;
        }
        const link = document.createElement('a');
        link.className = 'n28-link';
        link.href = href;
        cloneInline(child, link);
        target.append(link);
        return;
      }
      if (['b', 'strong', 'em', 'code'].includes(tag)) {
        const allowed = document.createElement(tag);
        cloneInline(child, allowed);
        target.append(allowed);
        return;
      }
      if (tag === 'br') {
        target.append(document.createElement('br'));
        return;
      }
      cloneInline(child, target);
    });
  }

  function copyParagraph(source) {
    const paragraph = document.createElement('p');
    cloneInline(source, paragraph);
    return paragraph;
  }

  function copyList(source) {
    const list = document.createElement(source.tagName.toLowerCase() === 'ol' ? 'ol' : 'ul');
    list.className = 'n28-list';
    source.querySelectorAll(':scope > li').forEach((item) => {
      const li = document.createElement('li');
      cloneInline(item, li);
      list.append(li);
    });
    return list;
  }

  function installStyles() {
    if ($('#frontend-next-n28-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n28-style';
    style.textContent = `
      .n28-legal{min-height:100vh;padding:112px 0 84px;background:radial-gradient(circle at 82% 7%,#ffcb2d12,transparent 31rem)}
      .n28-head{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:18px;align-items:end;margin-bottom:16px}.n28-kicker{font-size:10px;font-weight:900;letter-spacing:.16em;color:#d8bd68;text-transform:uppercase}.n28-head h1{font-size:clamp(40px,6vw,68px);margin:10px 0 8px}.n28-head p{max-width:760px;color:#909aa4;line-height:1.75;margin:0}.n28-actions{display:flex;gap:8px;flex-wrap:wrap}
      .n28-source{border:1px solid #ffcb2d28;border-radius:13px;background:#17140d;padding:12px 14px;color:#a99d74;font-size:10px;line-height:1.7;margin-bottom:12px}.n28-source strong{color:#ead27a}
      .n28-document{border:1px solid #ffffff17;border-radius:17px;background:#0d1116;padding:clamp(18px,4vw,34px)}.n28-document>.n28-eyebrow{font-size:10px;color:#d7bd6a;font-weight:900;letter-spacing:.12em}.n28-document>h2{font-size:clamp(28px,4vw,42px);margin:8px 0 18px}.n28-alert{border-left:3px solid #ffcb2d;padding:12px 14px;background:#ffcb2d0c;color:#c7bd9c;line-height:1.7;margin:0 0 18px}
      .n28-section{padding:20px 0;border-top:1px solid #ffffff10}.n28-section:first-of-type{border-top:0}.n28-section h3{font-size:18px;margin:0 0 9px}.n28-section p,.n28-section li{color:#99a2ac;line-height:1.85}.n28-section p{margin:0 0 9px}.n28-list{padding-left:20px;margin:9px 0}.n28-list li+li{margin-top:7px}.n28-link{color:#e7c85e;text-decoration:underline;text-underline-offset:3px}
      .n28-state{min-height:280px;border:1px solid #ffffff17;border-radius:16px;background:#0d1116;display:grid;place-items:center;text-align:center;padding:30px;color:#929ca6}.n28-state h2{color:#f1f3f5;margin:0 0 8px}.n28-state p{line-height:1.7;margin:0 0 14px}.n28-state .btn{display:inline-flex}
      .n28-footnote{color:#69737d;font-size:9px;line-height:1.7;margin-top:14px}
      @media(max-width:700px){.n28-legal{padding-top:92px}.n28-head{grid-template-columns:1fr}.n28-actions{display:grid}.n28-document{border-radius:14px}}
      /* N29 mobile: allow long customer-provided and published content to wrap safely. */
      .n28-legal .n28-document,.n28-legal .n28-section,.n28-legal .n28-source,.n28-legal .n28-state{min-width:0}
      .n28-legal .n28-document,.n28-legal .n28-section,.n28-legal .n28-link,.n28-legal .n28-state{overflow-wrap:anywhere}
    `;
    document.head.append(style);
  }

  function ensureSection() {
    let section = $('#n28Legal');
    if (section) return section;
    section = node('section', 'page n28-legal');
    section.id = 'n28Legal';
    section.dataset.page = 'legal';
    section.append(node('div', 'shell'));
    const footer = $('.next-footer') || $('.footer');
    if (footer) footer.before(section); else document.body.append(section);
    return section;
  }

  function activate(section) {
    $$('.page').forEach((candidate) => candidate.classList.toggle('active', candidate === section));
    $$('.navlink[data-go]').forEach((link) => {
      link.classList.remove('active');
      link.removeAttribute('aria-current');
    });
    document.documentElement.dataset.frontendPage = 'legal';
    document.documentElement.dataset.frontendNextStage = 'n28-legal-single-source';
  }

  function setMetadata(source, doc) {
    const formalPath = `/${doc}`;
    const sourceTitle = text(source.querySelector('title')?.textContent);
    document.title = sourceTitle || (doc === 'rules' ? '服务规则 · 星火网游' : '隐私说明 · 星火网游');

    let description = $('meta[name="description"]');
    if (!description) {
      description = document.createElement('meta');
      description.name = 'description';
      document.head.append(description);
    }
    description.content = text(source.querySelector('meta[name="description"]')?.getAttribute('content'))
      || (doc === 'rules' ? '服务规则。' : '隐私说明。');

    let canonical = $('link[rel="canonical"]');
    if (!canonical) {
      canonical = document.createElement('link');
      canonical.rel = 'canonical';
      document.head.append(canonical);
    }
    const sourceCanonical = safeLink(source.querySelector('link[rel="canonical"]')?.getAttribute('href'));
    try {
      const url = new URL(sourceCanonical || `https://wydlgf.cn${formalPath}`);
      canonical.href = url.hostname === 'wydlgf.cn' ? url.href : `https://wydlgf.cn${formalPath}`;
    } catch (_) {
      canonical.href = `https://wydlgf.cn${formalPath}`;
    }

    let robots = $('meta[name="robots"]');
    if (!robots) {
      robots = document.createElement('meta');
      robots.name = 'robots';
      document.head.append(robots);
    }
    robots.content = 'noindex,follow';
  }

  function header(doc) {
    const head = node('div', 'n28-head');
    const intro = node('div');
    intro.append(node('div', 'n28-kicker', STAGE));
    intro.append(node('h1', '', doc === 'rules' ? '服务规则' : '隐私说明'));
    intro.append(node('p', '', '新版只负责展示；正文实时读取正式法律页面并安全重建，因此不会维护第二份法律文案。'));
    const actions = node('div', 'n28-actions');
    const legacy = node('a', 'btn', '打开正式原文');
    legacy.href = `/${doc}`;
    legacy.dataset.n28Legacy = '1';
    const sibling = node('a', 'btn gold', doc === 'rules' ? '查看隐私说明' : '查看服务规则');
    sibling.href = doc === 'rules' ? '/next/privacy' : '/next/rules';
    actions.append(legacy, sibling);
    head.append(intro, actions);
    return head;
  }

  function renderFormalDocument(source, doc) {
    const sourceCard = source.querySelector('.rules-card');
    if (!sourceCard) throw new Error('formal legal source missing');

    const container = node('div', 'n28-document');
    const eyebrow = sourceCard.querySelector(':scope > .eyebrow');
    if (eyebrow) container.append(node('div', 'n28-eyebrow', eyebrow.textContent));
    const mainTitle = sourceCard.querySelector(':scope > h1');
    if (mainTitle) container.append(node('h2', '', mainTitle.textContent));
    const alert = sourceCard.querySelector(':scope > .rule-alert');
    if (alert) container.append(node('div', 'n28-alert', alert.textContent));

    sourceCard.querySelectorAll(':scope > section').forEach((sourceSection) => {
      const section = node('section', 'n28-section');
      const heading = sourceSection.querySelector(':scope > h2');
      if (heading) section.append(node('h3', '', heading.textContent));
      sourceSection.childNodes.forEach((child) => {
        if (!(child instanceof Element) || child === heading) return;
        const tag = child.tagName.toLowerCase();
        if (tag === 'p') section.append(copyParagraph(child));
        else if (tag === 'ul' || tag === 'ol') section.append(copyList(child));
      });
      container.append(section);
    });

    if (!container.querySelector('.n28-section')) throw new Error('formal legal sections missing');
    container.dataset.legalSource = `/${doc}`;
    return container;
  }

  function renderState(shell, doc, message) {
    const state = node('div', 'n28-state');
    const box = node('div');
    box.append(node('h2', '', '正式法律内容暂时无法读取'));
    box.append(node('p', '', message || '请重试，或直接打开正式原文。'));
    const retry = node('button', 'btn gold', '重新读取');
    retry.type = 'button';
    retry.addEventListener('click', () => render());
    const legacy = node('a', 'btn', '打开正式原文');
    legacy.href = `/${doc}`;
    legacy.dataset.n28Legacy = '1';
    box.append(retry, legacy);
    state.append(box);
    shell.append(state);
  }

  async function loadFormalSource(doc, request) {
    const response = await fetch(`/${doc}`, {
      method: 'GET',
      credentials: 'same-origin',
      cache: 'no-store',
      headers: { Accept: 'text/html' }
    });
    if (!response.ok) throw new Error(`formal legal source returned ${response.status}`);
    const html = await response.text();
    if (request !== requestToken) return null;
    return new DOMParser().parseFromString(html, 'text/html');
  }

  async function render() {
    const doc = currentDoc();
    if (!doc) return;
    installStyles();
    const section = ensureSection();
    activate(section);
    const shell = $('.shell', section);
    const request = ++requestToken;
    shell.replaceChildren(header(doc));
    const sourceNote = node('div', 'n28-source');
    sourceNote.append(document.createTextNode('正文来源：'));
    sourceNote.append(node('strong', '', `正式 /${doc}`));
    sourceNote.append(document.createTextNode(' · Next 不保存第二份法律正文'));
    shell.append(sourceNote, node('div', 'n28-state', '正在读取正式法律内容…'));

    try {
      const source = await loadFormalSource(doc, request);
      if (!source || request !== requestToken || doc !== currentDoc()) return;
      setMetadata(source, doc);
      shell.replaceChildren(header(doc), sourceNote, renderFormalDocument(source, doc));
      shell.append(node('p', 'n28-footnote', '本预览页仅安全重建正式法律页面中的标题、段落、列表与允许链接；更新法律文案时只需修改正式 `/rules` 或 `/privacy` 源页面。'));
    } catch (_) {
      if (request !== requestToken || doc !== currentDoc()) return;
      shell.replaceChildren(header(doc), sourceNote);
      renderState(shell, doc, '新版没有使用备用法律副本，以避免正式正文与预览正文不一致。');
    }
  }

  window.addEventListener('popstate', () => {
    if (currentDoc()) render();
  });

  window.XHFrontendNextN28 = { render, currentDoc };
  render();
})();
