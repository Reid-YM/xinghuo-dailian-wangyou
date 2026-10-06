/* V21 询价两步式：第 1 步说明需求并显示参考信息，第 2 步留联系方式并提交。 */
(function () {
  'use strict';

  function panelOf(form, n) {
    return form.querySelector('[data-step-panel="' + n + '"]');
  }

  function show(form, n) {
    form.querySelectorAll('[data-step-panel]').forEach(function (p) {
      p.hidden = p.getAttribute('data-step-panel') !== String(n);
    });
    var bar = document.querySelector('.quote-stepbar');
    if (bar) {
      bar.querySelectorAll('span').forEach(function (s, i) {
        s.classList.toggle('active', i === n - 1);
      });
    }
    var current = panelOf(form, n);
    if (current) current.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function firstInvalid(scope) {
    var bad = null;
    scope.querySelectorAll('select[required], input[required]').forEach(function (el) {
      if (!bad && !el.checkValidity()) bad = el;
    });
    return bad;
  }

  // 第 1 步选择具体服务后，即时显示参考价格与预计时间（数据来自 /api/services）
  function bindReference(form) {
    var demand = form.querySelector('[data-link-service]');
    var price = form.querySelector('#stepPriceHint');
    var duration = form.querySelector('#stepDurationHint');
    if (!demand || !price || !duration) return;
    var services = [];
    var PENDING = '\u9009\u62e9\u5177\u4f53\u670d\u52a1\u540e\u663e\u793a';

    function priceText(svc) {
      if (!svc) return null;
      var mode = String(svc.priceMode || '').toLowerCase();
      if (mode === 'fixed' && svc.priceFixed != null) return '\u56fa\u5b9a\u4ef7 \u00a5' + svc.priceFixed;
      if (mode === 'range' && svc.priceMin != null && svc.priceMax != null) {
        return '\u533a\u95f4\u4ef7 \u00a5' + svc.priceMin + '\u2013' + svc.priceMax;
      }
      if (svc.priceMin != null) return '\u4eba\u5de5\u62a5\u4ef7\uff0c\u53c2\u8003 \u00a5' + svc.priceMin + ' \u8d77';
      return '\u4eba\u5de5\u62a5\u4ef7\uff0c\u4ee5\u5ba2\u670d\u786e\u8ba4\u4e3a\u51c6';
    }

    function render() {
      var name = demand.value;
      var svc = null;
      for (var i = 0; i < services.length; i++) {
        if (services[i] && services[i].name === name) { svc = services[i]; break; }
      }
      if (!name) {
        price.textContent = PENDING;
        duration.textContent = PENDING;
        return;
      }
      price.textContent = priceText(svc) || '\u4ee5\u5ba2\u670d\u786e\u8ba4\u4e3a\u51c6';
      duration.textContent = (svc && svc.durationText) ? svc.durationText : '\u6309\u8fdb\u5ea6\u786e\u8ba4';
    }

    if (window.XH && typeof XH.loadServices === 'function') {
      try {
        Promise.resolve(XH.loadServices()).then(function (rows) {
          services = Array.isArray(rows) ? rows : [];
          render();
        }).catch(function () { render(); });
      } catch (e) { render(); }
    }
    demand.addEventListener('change', render);
    render();
  }

  function init(form) {
    if (!form || form.dataset.stepBound === '1') return;
    form.dataset.stepBound = '1';
    if (!panelOf(form, 1) || !panelOf(form, 2)) return;

    bindReference(form);

    form.addEventListener('click', function (e) {
      var next = e.target.closest('[data-step-next]');
      var prev = e.target.closest('[data-step-prev]');
      if (next) {
        var bad = firstInvalid(panelOf(form, 1));
        if (bad) { bad.reportValidity(); bad.focus(); return; }
        show(form, 2);
      } else if (prev) {
        show(form, 1);
      }
    });

    // 从第 2 步以外提交时（例如回车），先回到第 1 步校验
    form.addEventListener('submit', function (e) {
      var bad = firstInvalid(panelOf(form, 1));
      if (bad) { e.preventDefault(); show(form, 1); bad.reportValidity(); bad.focus(); return; }
      var contactBad = firstInvalid(panelOf(form, 2));
      if (contactBad) { e.preventDefault(); contactBad.reportValidity(); contactBad.focus(); }
    });
  }

  function boot() {
    document.querySelectorAll('form[data-two-step]').forEach(init);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
