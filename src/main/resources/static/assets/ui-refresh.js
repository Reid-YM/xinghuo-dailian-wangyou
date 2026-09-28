(() => {
  if (!window.XH || typeof window.XH.serviceCard !== 'function') return;
  if (window.XH.serviceCard.__xhUiRefresh) return;

  const baseServiceCard = window.XH.serviceCard;

  function gameCode(service) {
    return String(service && service.gameCode || '').toUpperCase();
  }

  function platformLabel(service) {
    const code = gameCode(service);
    if (code === 'CODMW4') return 'Steam / PS / Xbox / Switch 2';
    if (code === 'WAR_DOGS') return 'Steam';
    return '按平台确认';
  }

  function gameLabel(service) {
    const code = gameCode(service);
    if (code === 'CODMW4') return '使命召唤 MW4';
    if (code === 'WAR_DOGS') return '战狗';
    return service && service.name || '游戏服务';
  }

  function audienceLabel(service) {
    const category = String(service && service.category || '').toLowerCase();
    if (category === 'camo' || category === 'mission') return '适合想补齐剩余挑战与奖励的玩家';
    if (category === 'weapon' || category === 'class') return '适合需要推进等级与解锁内容的玩家';
    if (category === 'rank') return '适合目标明确、希望按段差确认报价的玩家';
    return '适合下单前先确认进度与目标的玩家';
  }

  function appendFact(root, label, value) {
    const row = document.createElement('span');
    const key = document.createElement('b');
    const text = document.createElement('i');
    key.textContent = label;
    text.textContent = value;
    row.append(key, text);
    root.append(row);
  }

  function enhanceCard(service, compact) {
    const html = baseServiceCard.call(this, service, compact);
    if (compact) return html;

    const template = document.createElement('template');
    template.innerHTML = String(html || '').trim();
    const card = template.content.firstElementChild;
    if (!card) return html;

    const headTag = card.querySelector('.service-card-head .tag');
    if (headTag) headTag.textContent = gameLabel(service);

    if (!card.querySelector('.service-card-facts')) {
      const facts = document.createElement('div');
      facts.className = 'service-card-facts';
      appendFact(facts, '平台', platformLabel(service));
      appendFact(facts, '服务', '手打 · 纯绿 · 进度可查');
      appendFact(facts, '适合', audienceLabel(service));
      const meta = card.querySelector('.service-meta');
      if (meta) meta.insertAdjacentElement('afterend', facts);
      else card.append(facts);
    }

    if (!card.querySelector('.service-card-action')) {
      const action = document.createElement('div');
      action.className = 'service-card-action';
      const detail = document.createElement('span');
      const consult = document.createElement('span');
      detail.textContent = '查看详情';
      consult.textContent = '咨询 / 下单 →';
      action.append(detail, consult);
      card.append(action);
    }

    card.dataset.gameCode = gameCode(service);
    card.dataset.category = String(service && service.category || '');
    return card.outerHTML;
  }

  enhanceCard.__xhUiRefresh = true;
  window.XH.serviceCard = enhanceCard;
})();
