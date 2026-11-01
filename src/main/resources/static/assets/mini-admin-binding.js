'use strict';
(function () {
  var out = document.getElementById('result');
  var field = document.getElementById('pair');
  var confirmButton = document.getElementById('confirm');
  var revokeButton = document.getElementById('revoke');
  var tokenMeta = document.querySelector('meta[name="_csrf"]');
  var headerMeta = document.querySelector('meta[name="_csrf_header"]');
  if (!out || !field || !confirmButton || !revokeButton || !tokenMeta || !headerMeta ||
      !headerMeta.content || !tokenMeta.content) return;
  var busy = false;
  async function act(path, body) {
    if (busy) return;
    busy = true;
    confirmButton.disabled = true;
    revokeButton.disabled = true;
    out.textContent = '正在通过网站安全验证…';
    try {
      var headers = { 'Content-Type': 'application/json' };
      headers[headerMeta.content] = tokenMeta.content;
      var res = await fetch(path, {
        method: 'POST', credentials: 'same-origin', cache: 'no-store',
        headers: headers, body: JSON.stringify(body || {})
      });
      var data = await res.json().catch(function () { return {}; });
      out.textContent = (!res.ok && res.status === 403)
        ? '管理员权限或登录会话失效，请重新登录网站后台'
        : (data.message || (res.ok ? '操作成功' : '操作失败（HTTP ' + res.status + '）'));
      if (res.ok && path.endsWith('/confirm')) field.value = '';
    } catch (err) {
      out.textContent = '网络异常，网站未确认操作结果，请先检查后台绑定状态';
    } finally {
      busy = false;
      confirmButton.disabled = false;
      revokeButton.disabled = false;
    }
  }
  confirmButton.addEventListener('click', function () {
    var code = field.value.trim();
    if (!/^[0-9]{8}$/.test(code)) {
      out.textContent = '请输入小程序生成的八位绑定码';
      return;
    }
    act('/api/admin/xingwei/mini-binding/confirm', { code: code });
  });
  revokeButton.addEventListener('click', function () {
    if (window.confirm('确定解除当前网站管理员的微信绑定吗？解除后小程序管理权限立即失效。')) {
      act('/api/admin/xingwei/mini-binding/revoke', {});
    }
  });
}());
