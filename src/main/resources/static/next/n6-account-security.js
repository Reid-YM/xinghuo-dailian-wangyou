(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const text = (value) => String(value ?? '').trim();
  const page = $('.page[data-page="account"]');
  if (!page) return;

  let observer = null;
  let mountTimer = null;
  let toastTimer = null;
  let mountedFingerprint = '';
  let emailToken='';
  if(location.pathname.includes('/settings')){const params=new URLSearchParams(location.search);emailToken=params.get('verifyToken')||params.get('token')||'';if(emailToken){const clean=new URL(location.href);clean.searchParams.delete('verifyToken');clean.searchParams.delete('token');history.replaceState(history.state,'',clean.pathname+clean.search+clean.hash);}}
  let initialFocus=false;

  function node(tag, className, value) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (value !== undefined && value !== null) element.textContent = String(value);
    return element;
  }

  function labelFor(value, id) {
    const label = node('label', '', value);
    label.htmlFor = id;
    return label;
  }

  function append(parent, ...children) {
    children.filter(Boolean).forEach((child) => parent.append(child));
    return parent;
  }

  function button(label, className = 'n6-btn') {
    const element = node('button', className, label);
    element.type = 'button';
    return element;
  }

  function fmtDate(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
    }).format(date);
  }

  function profile() {
    return window.XHFrontendNextN4?.state?.profile || null;
  }

  function sessions() {
    const list = window.XHFrontendNextN4?.state?.sessions;
    return Array.isArray(list) ? list : [];
  }

  function authenticated() {
    return !!profile() && !!$('.n4-shell', page);
  }

  function installStyles() {
    if ($('#frontend-next-n6-style')) return;
    const style = document.createElement('style');
    style.id = 'frontend-next-n6-style';
    style.textContent = `
      .n6-section{padding:0 0 100px}.n6-head{display:flex;align-items:end;justify-content:space-between;gap:16px;flex-wrap:wrap;margin:0 0 16px}.n6-head h2{margin:4px 0 0;font-size:26px}.n6-head p{margin:0;color:#7d8791;font-size:10px;line-height:1.65;max-width:690px}.n6-badge{display:inline-flex;align-items:center;gap:7px;border:1px solid #5c8dff44;color:#8db0ff;border-radius:99px;padding:6px 10px;font-size:9px}.n6-badge:before{content:"";width:6px;height:6px;border-radius:50%;background:#72a0ff;box-shadow:0 0 10px #72a0ff}
      .n6-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.n6-card{border:1px solid var(--line);border-radius:16px;background:#0c1015;padding:18px;min-width:0}.n6-card-wide{grid-column:1/-1}.n6-card h3{margin:0 0 6px;font-size:15px}.n6-card>p{margin:0 0 13px;color:#737d87;font-size:9px;line-height:1.6}.n6-status{display:inline-flex;align-items:center;gap:6px;border-radius:99px;padding:5px 8px;font-size:8px;margin-bottom:11px;border:1px solid #ffffff15;color:#a1a9b1}.n6-status.ok{border-color:#4be39344;color:#82eeb0}.n6-status.warn{border-color:#ffb52f44;color:#ffd06e}.n6-field{display:grid;gap:6px;margin-top:10px}.n6-field label{font-size:9px;color:#9aa3ac}.n6-field input{width:100%;border:1px solid #ffffff18;background:#080c10;color:#fff;border-radius:10px;padding:11px;font:inherit;font-size:10px}.n6-field input:focus{outline:none;border-color:#ffcb2d88;box-shadow:0 0 0 3px #ffcb2d12}.n6-field small{color:#69737d;font-size:8px;line-height:1.5}.n6-row{display:grid;grid-template-columns:1fr auto;gap:8px;align-items:end}.n6-btn{appearance:none;border:1px solid #ffffff1a;background:#10161d;color:#d9dee3;border-radius:9px;padding:9px 11px;font:inherit;font-size:9px;cursor:pointer}.n6-btn:hover:not(:disabled){border-color:#ffcb2d66;color:#fff}.n6-btn.primary{border-color:#ffcb2d77;background:#ffcb2d;color:#17130a;font-weight:700}.n6-btn.danger{border-color:#ff806344;color:#ffad9a;background:#160d0b}.n6-btn:disabled{opacity:.45;cursor:not-allowed}.n6-actions{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}.n6-message{min-height:16px;margin-top:8px;color:#7c8690;font-size:9px;line-height:1.5}.n6-message.ok{color:#7ae6a7}.n6-message.error{color:#ff9f8a}.n6-password-rules{margin:8px 0 0;padding:0;list-style:none;color:#737d87;font-size:8px;line-height:1.65}.n6-password-rules li:before{content:"•";margin-right:6px;color:#ffcb2d}.n6-devices{display:grid;gap:9px}.n6-device{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:12px;align-items:center;border:1px solid #ffffff10;border-radius:11px;background:#090d11;padding:12px}.n6-device b{display:block;font-size:10px}.n6-device span{display:block;color:#737c86;font-size:8px;line-height:1.5;margin-top:4px}.n6-current{color:#75e7a5!important}.n6-empty{border:1px dashed #ffffff18;border-radius:10px;padding:14px;color:#727c86;font-size:9px;line-height:1.6}.n6-email-steps{display:grid;gap:10px}.n6-email-confirm{padding-top:11px;border-top:1px solid #ffffff10}.n6-toast{position:fixed;right:22px;bottom:22px;z-index:10000;max-width:min(420px,calc(100vw - 32px));border:1px solid #4ade9166;background:#0b1711;color:#a3f2c0;border-radius:12px;padding:12px 14px;box-shadow:0 18px 48px #000a;font-size:10px;line-height:1.55}.n6-toast.error{border-color:#ff866766;background:#1a0e0b;color:#ffb09d}
      @media(max-width:820px){.n6-grid{grid-template-columns:1fr}.n6-card-wide{grid-column:auto}}
      @media(max-width:560px){.n6-card{padding:14px}.n6-row{grid-template-columns:1fr}.n6-device{grid-template-columns:1fr}.n6-device .n6-btn{width:100%}.n6-toast{right:16px;bottom:16px}}
    `;
    document.head.append(style);
  }

  function toast(message, kind = '') {
    let host = $('#n6Toast');
    if (!host) {
      host = node('div', 'n6-toast');
      host.id = 'n6Toast';
      host.setAttribute('role', 'status');
      host.setAttribute('aria-live', 'polite');
      document.body.append(host);
    }
    host.className = `n6-toast ${kind}`.trim();
    host.textContent = message;
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      if (host.isConnected) host.remove();
      toastTimer = null;
    }, 5000);
  }

  async function request(url, options = {}) {
    const headers = Object.assign({ Accept: 'application/json' }, options.headers || {});
    const response = await fetch(url, Object.assign({
      credentials: 'same-origin', cache: 'no-store'
    }, options, { headers }));
    let body = null;
    try { body = await response.json(); } catch (ignore) { body = null; }
    if (!response.ok) {
      const error = new Error(text(body?.message) || text(body?.error) || `请求失败（${response.status}）`);
      error.status = response.status;
      throw error;
    }
    return body;
  }

  function writeJson(url, body, method = 'POST') {
    return request(url, {
      method,
      headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'fetch' },
      body: JSON.stringify(body)
    });
  }

  function writeNoBody(url, method = 'POST') {
    return request(url, { method, headers: { 'X-Requested-With': 'fetch' } });
  }

  function setMessage(id, message, kind = '') {
    const host = $('#' + id);
    if (!host) return;
    host.className = `n6-message ${kind}`.trim();
    host.textContent = message || '';
  }

  async function reloadAccount() {
    if (window.XHFrontendNextN4?.reload) await window.XHFrontendNextN4.reload();
  }

  function sectionCard(title, description, wide = false) {
    const card = node('section', `n6-card${wide ? ' n6-card-wide' : ''}`);
    append(card, node('h3', '', title), node('p', '', description));
    return card;
  }

  function profileCard(current) {
    const card = sectionCard('账号资料', '昵称与头像使用当前账号资料；上传头像后立即由服务器保存。');
    const form = document.createElement('form'); form.id = 'n6ProfileForm';
    const field = node('div', 'n6-field');
    const input = document.createElement('input'); input.id = 'n6DisplayName'; input.maxLength = 60; input.required = true; input.value = text(current.displayName); input.autocomplete = 'nickname';
    const nameLabel=node('label','','昵称');nameLabel.htmlFor=input.id;append(field,nameLabel,input,node('small','','最多 60 个字符。'));
    const avatar = node('div','n6-field');const file=document.createElement('input');file.id='n6Avatar';file.type='file';file.accept='image/jpeg,image/png,image/webp,image/gif';
    const avatarLabel=node('label','','头像文件');avatarLabel.htmlFor=file.id;append(avatar,avatarLabel,file,node('small','','选择图片；保存时上传，失败可重试。'));
    const actions = node('div', 'n6-actions');
    const submit = button('保存资料', 'n6-btn primary'); submit.type = 'submit'; actions.append(submit);
    append(form, field, avatar, actions);
    const message = node('div', 'n6-message'); message.id = 'n6ProfileMessage'; message.setAttribute('aria-live', 'polite');
    append(card, form, message);
    form.addEventListener('submit', async (event) => {
      event.preventDefault();if(submit.disabled)return;
      const displayName = text(input.value);
      if (!displayName) { setMessage('n6ProfileMessage', '请填写昵称。', 'error'); input.focus(); return; }
      const image=file.files?.[0];if(image && !image.type.startsWith('image/')){setMessage('n6ProfileMessage','请选择图片文件。','error');return;}
      submit.disabled=true;file.disabled=true;input.disabled=true;submit.textContent='正在保存…';let uploaded=false;let saved=false;
      try {
        if(image){const body=new FormData();body.append('file',image);const profile=await request('/api/account/profile/avatar',{method:'POST',headers:{'X-Requested-With':'fetch'},body});current.avatarUrl=profile.avatarUrl;mountedFingerprint=fingerprint(current);file.value='';uploaded=true;}
        await writeJson('/api/account/profile', { displayName, avatarUrl: current.avatarUrl || null });saved=true;
        toast('账号资料已更新。');
        await reloadAccount();
      } catch (error) {
        setMessage('n6ProfileMessage',(saved?'资料已保存，账号显示刷新失败：':uploaded?'头像已保存，昵称保存失败：':'')+(text(error.message)||'资料保存失败。'),'error');
      } finally {
        if (submit.isConnected) { submit.disabled=false;file.disabled=false;input.disabled=false;submit.textContent='保存资料'; }
      }
    });
    return card;
  }
  function contactCard(current) {
    const card=sectionCard('客服回访联系方式','仅供询价和回访；这里的联系方式不用于登录或身份验证。');
    const rows=node('div');rows.id='n6Contacts';
    const form=document.createElement('form');form.id='n6ContactForm';form.className='n6-field';
    const type=document.createElement('select');type.id='n6ContactType';[['PHONE','手机号'],['WECHAT','微信号'],['QQ','QQ']].forEach(([value,label])=>type.append(new Option(label,value)));
    const value=document.createElement('input');value.id='n6ContactValue';value.maxLength=120;value.required=true;value.autocomplete='off';value.placeholder='填写回访联系方式';
    const typeLabel=node('label','','联系渠道');typeLabel.htmlFor=type.id;const valueLabel=node('label','','联系号');valueLabel.htmlFor=value.id;
    const submit=button('添加回访方式','n6-btn');submit.type='submit';
    const message=node('p','n6-message');message.id='n6ContactMessage';message.setAttribute('role','status');
    let busy=false;
    function render(list){rows.replaceChildren();if(!Array.isArray(list)||!list.length){rows.append(node('p','','暂无回访方式。'));return;}list.forEach((item,index)=>{const row=node('div','n6-row');row.append(node('span','',text(item.type)+' · '+text(item.masked)));const remove=button('移除','n6-btn');remove.addEventListener('click',()=>change(()=>writeNoBody('/api/account/quote-contacts/'+index,'DELETE'),false));row.append(remove);rows.append(row);});}
    async function change(action,clear){
      if(busy)return;busy=true;form.querySelectorAll('input,select,button').forEach(node=>node.disabled=true);rows.querySelectorAll('button').forEach(node=>node.disabled=true);
      try{const saved=await action();current.quoteContacts=saved.quoteContacts;mountedFingerprint=fingerprint(profile());render(saved.quoteContacts);if(clear)value.value='';setMessage('n6ContactMessage','回访方式已更新。','ok');}
      catch(error){setMessage('n6ContactMessage',text(error.message)||'回访方式更新失败。','error');}
      finally{busy=false;form.querySelectorAll('input,select,button').forEach(node=>node.disabled=false);rows.querySelectorAll('button').forEach(node=>node.disabled=false);}
    }
    form.addEventListener('submit',event=>{event.preventDefault();if(!text(value.value))return;change(()=>writeJson('/api/account/quote-contacts',{type:type.value,value:text(value.value)}),true);});
    append(form,typeLabel,type,valueLabel,value,submit);append(card,rows,form,message);render(current.quoteContacts);return card;
  }

  function emailCard(current) {
    const card = sectionCard('安全邮箱', '用于账号验证与找回密码。验证码由服务端发送并在服务端完成校验。');
    const status = node('span', `n6-status ${current.emailVerified ? 'ok' : 'warn'}`,
      current.emailVerified ? '邮箱已验证' : '邮箱未验证');
    card.append(status);
    const steps = node('div', 'n6-email-steps');
    const requestForm = document.createElement('form'); requestForm.id = 'n6EmailRequestForm';
    const row = node('div', 'n6-row');
    const field = node('div', 'n6-field');
    const email = document.createElement('input'); email.id = 'n6Email'; email.type = 'email'; email.maxLength = 160; email.required = true; email.autocomplete = 'email'; email.placeholder = 'name@example.com'; email.value = text(current.email);
    append(field, labelFor('邮箱地址', 'n6Email'), email, node('small', '', current.emailVerified ? '可重新验证新的安全邮箱。' : '发送后请在 30 分钟内完成验证。'));
    const send = button('发送验证码', 'n6-btn'); send.type = 'submit'; append(row, field, send); requestForm.append(row);
    const requestMessage = node('div', 'n6-message'); requestMessage.id = 'n6EmailRequestMessage'; requestMessage.setAttribute('aria-live', 'polite'); requestForm.append(requestMessage);

    const confirmForm = document.createElement('form'); confirmForm.id = 'n6EmailConfirmForm'; confirmForm.className = 'n6-email-confirm';
    const confirmRow = node('div', 'n6-row');
    const codeField = node('div', 'n6-field');
    const code = document.createElement('input'); code.id = 'n6EmailCode'; code.maxLength = 12; code.required = true; code.autocomplete = 'one-time-code'; code.inputMode = 'numeric'; code.placeholder = '输入邮箱验证码';
    append(codeField, labelFor('验证码', 'n6EmailCode'), code, node('small', '', '验证码最长 12 位；不要把验证码发送给其他人。'));
    const confirm = button('确认验证', 'n6-btn primary'); confirm.type = 'submit'; append(confirmRow, codeField, confirm); confirmForm.append(confirmRow);
    const confirmMessage = node('div', 'n6-message'); confirmMessage.id = 'n6EmailConfirmMessage'; confirmMessage.setAttribute('aria-live', 'polite'); confirmForm.append(confirmMessage);
    append(steps, requestForm, confirmForm); card.append(steps);

    requestForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const value = text(email.value);
      if (!value) { setMessage('n6EmailRequestMessage', '请填写邮箱地址。', 'error'); email.focus(); return; }
      send.disabled = true; send.textContent = '正在发送…';
      try {
        const result = await writeJson('/api/account/email/verify/request', { email: value });
        setMessage('n6EmailRequestMessage', text(result?.message) || '验证码已发送，请检查邮箱。', 'ok');
        code.focus();
      } catch (error) {
        setMessage('n6EmailRequestMessage', text(error.message) || '验证码发送失败。', 'error');
      } finally {
        if (send.isConnected) { send.disabled = false; send.textContent = '发送验证码'; }
      }
    });

    confirmForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const value = text(code.value);
      if (!value) { setMessage('n6EmailConfirmMessage', '请输入邮箱验证码。', 'error'); code.focus(); return; }
      confirm.disabled = true; confirm.textContent = '正在验证…';
      try {
        const result = await writeJson('/api/account/email/verify/confirm', { code: value });
        toast(text(result?.message) || '邮箱验证成功。');
        code.value = '';
        await reloadAccount();
      } catch (error) {
        setMessage('n6EmailConfirmMessage', text(error.message) || '邮箱验证失败。', 'error');
      } finally {
        if (confirm.isConnected) { confirm.disabled = false; confirm.textContent = '确认验证'; }
      }
    });
    return card;
  }

  function passwordKinds(value) {
    let kinds = 0;
    if (/[a-z]/.test(value)) kinds++;
    if (/[A-Z]/.test(value)) kinds++;
    if (/\d/.test(value)) kinds++;
    if (/[^A-Za-z0-9]/.test(value)) kinds++;
    return kinds;
  }

  function passwordCard() {
    const card = sectionCard('修改登录密码', '修改成功后服务端会撤销该账号全部登录会话，包括当前设备；随后需要使用新密码重新登录。');
    const form = document.createElement('form'); form.id = 'n6PasswordForm';
    const oldField = node('div', 'n6-field');
    const oldPassword = document.createElement('input'); oldPassword.id = 'n6OldPassword'; oldPassword.type = 'password'; oldPassword.maxLength = 72; oldPassword.required = true; oldPassword.autocomplete = 'current-password';
    append(oldField, labelFor('原密码', 'n6OldPassword'), oldPassword);
    const newField = node('div', 'n6-field');
    const newPassword = document.createElement('input'); newPassword.id = 'n6NewPassword'; newPassword.type = 'password'; newPassword.maxLength = 72; newPassword.required = true; newPassword.autocomplete = 'new-password';
    append(newField, labelFor('新密码', 'n6NewPassword'), newPassword);
    const confirmField = node('div', 'n6-field');
    const confirmPassword = document.createElement('input'); confirmPassword.id = 'n6ConfirmPassword'; confirmPassword.type = 'password'; confirmPassword.maxLength = 72; confirmPassword.required = true; confirmPassword.autocomplete = 'new-password';
    append(confirmField, labelFor('确认新密码', 'n6ConfirmPassword'), confirmPassword);
    const rules = node('ul', 'n6-password-rules');
    ['至少 8 位', '小写 / 大写 / 数字 / 符号至少包含两类', '服务端按 BCrypt 72 字节有效输入上限再次校验', '不能与原密码相同'].forEach((rule) => rules.append(node('li', '', rule)));
    const actions = node('div', 'n6-actions');
    const submit = button('修改密码并退出所有设备', 'n6-btn danger'); submit.type = 'submit'; actions.append(submit);
    append(form, oldField, newField, confirmField, rules, actions);
    const message = node('div', 'n6-message'); message.id = 'n6PasswordMessage'; message.setAttribute('aria-live', 'polite');
    append(card, form, message);

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const oldValue = String(oldPassword.value || '');
      const nextValue = String(newPassword.value || '');
      const confirmValue = String(confirmPassword.value || '');
      if (!oldValue || !nextValue || !confirmValue) { setMessage('n6PasswordMessage', '请完整填写原密码和新密码。', 'error'); return; }
      if (nextValue.length < 8 || passwordKinds(nextValue) < 2) { setMessage('n6PasswordMessage', '新密码至少 8 位，并至少包含两类字符。', 'error'); newPassword.focus(); return; }
      if (nextValue !== confirmValue) { setMessage('n6PasswordMessage', '两次输入的新密码不一致。', 'error'); confirmPassword.focus(); return; }
      if (!window.confirm('修改密码后，当前设备和其他设备都会被退出，需要使用新密码重新登录。确认继续？')) return;
      submit.disabled = true; submit.textContent = '正在修改密码…';
      try {
        const result = await writeJson('/api/account/password/change', { oldPassword: oldValue, newPassword: nextValue });
        oldPassword.value = ''; newPassword.value = ''; confirmPassword.value = '';
        toast(text(result?.message) || '密码已修改，请重新登录。');
        await reloadAccount();
      } catch (error) {
        setMessage('n6PasswordMessage', text(error.message) || '密码修改失败。', 'error');
      } finally {
        if (submit.isConnected) { submit.disabled = false; submit.textContent = '修改密码并退出所有设备'; }
      }
    });
    return card;
  }

  function deviceCard() {
    const card = sectionCard('登录设备', '只显示当前账号自己的有效会话。会话 ID 仅作为服务端下线句柄，不在页面展示。', true);
    const list = node('div', 'n6-devices'); list.id = 'n6Devices';
    const items = sessions();
    if (!items.length) {
      list.append(node('div', 'n6-empty', '暂时没有可读取的设备会话。请刷新重试或联系客服。'));
    }
    items.forEach((session) => {
      const row = node('div', 'n6-device');
      const copy = node('div');
      const title = text(session.deviceLabel) || '未知设备';
      append(copy,
        node('b', session.current ? 'n6-current' : '', session.current ? `${title} · 当前设备` : title),
        node('span', '', `${text(session.ip) || 'IP 已脱敏'} · 最近活动 ${fmtDate(session.lastSeenAt)} · 登录于 ${fmtDate(session.createdAt)}`)
      );
      if (session.current) {
        const current = button('当前会话', 'n6-btn'); current.disabled = true; append(row, copy, current);
      } else {
        const revoke = button('下线此设备', 'n6-btn danger');
        revoke.addEventListener('click', async () => {
          if (!window.confirm(`确认下线“${title}”？该设备需要重新登录。`)) return;
          revoke.disabled = true; revoke.textContent = '正在下线…';
          try {
            const result = await writeNoBody(`/api/account/sessions/${encodeURIComponent(String(session.id || ''))}`, 'DELETE');
            toast(text(result?.message) || '设备已下线。');
            await reloadAccount();
          } catch (error) {
            toast(text(error?.message) || '设备下线失败。', 'error');
            if (revoke.isConnected) { revoke.disabled = false; revoke.textContent = '下线此设备'; }
          }
        });
        append(row, copy, revoke);
      }
      list.append(row);
    });
    card.append(list);
    const actions = node('div', 'n6-actions');
    const otherCount = items.filter((item) => !item.current).length;
    const revokeAll = button(otherCount ? `下线其他 ${otherCount} 个设备` : '没有其他在线设备', 'n6-btn danger'); revokeAll.id = 'n6RevokeOthers'; revokeAll.disabled = otherCount === 0;
    revokeAll.addEventListener('click', async () => {
      if (!window.confirm('确认下线当前账号的其他所有设备？当前设备会继续保持登录。')) return;
      revokeAll.disabled = true; revokeAll.textContent = '正在下线其他设备…';
      try {
        const result = await writeNoBody('/api/account/sessions/revoke-all', 'POST');
        toast(text(result?.message) || '其他设备已下线。');
        await reloadAccount();
      } catch (error) {
        toast(text(error?.message) || '批量下线失败。', 'error');
        if (revokeAll.isConnected) { revokeAll.disabled = false; revokeAll.textContent = `下线其他 ${otherCount} 个设备`; }
      }
    });
    actions.append(revokeAll); card.append(actions);
    return card;
  }

  function fingerprint(current) {
    return JSON.stringify({
      displayName: current?.displayName || '', avatarUrl:current?.avatarUrl||'', quoteContacts:current?.quoteContacts||[], email: current?.email || '', emailVerified: !!current?.emailVerified,
      sessions: sessions().map((item) => [item.id, item.current, item.lastSeenAt])
    });
  }

  function mount() {
    if (!authenticated()) {
      $('#n6AccountSecurity')?.remove();
      mountedFingerprint = '';
      return;
    }
    const current = profile();
    const nextFingerprint = fingerprint(current);
    const existing = $('#n6AccountSecurity');
    if (existing && mountedFingerprint === nextFingerprint) return;
    existing?.remove();
    installStyles();

    const section = node('section', 'n6-section'); section.id = 'n6AccountSecurity';
    const shell = node('div', 'shell');
    const head = node('div', 'n6-head');
    const copy = node('div');
    append(copy, node('div', 'kicker', '账号与安全'), node('h2', '', '账号设置与安全'), node('p', '', '管理账号资料、验证邮箱、修改密码并查看登录设备。'));
    append(head, copy, node('span', 'n6-badge', 'ACCOUNT SECURITY LIVE'));
    shell.append(head);
    const grid = node('div', 'n6-grid');
    append(grid, profileCard(current), contactCard(current), emailCard(current), passwordCard(), deviceCard());
    shell.append(grid); section.append(shell);

    const n5 = $('#n5BusinessCenter');
    if (n5) n5.after(section);
    else {
      const n4Section = $('.n4-section', page);
      if (n4Section) n4Section.after(section); else page.append(section);
    }
    mountedFingerprint = nextFingerprint;
    if(emailToken){const token=emailToken;emailToken='';setMessage('n6EmailRequestMessage','正在验证邮箱…');request('/api/account/email/verify?token='+encodeURIComponent(token)).then(()=>{setMessage('n6EmailRequestMessage','邮箱验证成功。','ok');return reloadAccount();}).catch(error=>setMessage('n6EmailRequestMessage',text(error.message)||'邮箱验证失败。','error'));}
    if(!initialFocus && location.pathname.startsWith('/account/settings')){initialFocus=true;section.scrollIntoView({block:'start'});}
    const badge = $('.badge'); if (badge) badge.textContent = location.pathname.startsWith('/next') ? '新版预览' : '星火服务平台';
  }

  function scheduleMount() {
    if (mountTimer) clearTimeout(mountTimer);
    mountTimer = setTimeout(mount, 70);
  }

  installStyles();
  observer = new MutationObserver(scheduleMount);
  observer.observe(page, { childList: true, subtree: true });
  window.addEventListener('pagehide', () => {
    if (observer) observer.disconnect();
    if (mountTimer) clearTimeout(mountTimer);
    if (toastTimer) clearTimeout(toastTimer);
  });

  window.XHFrontendNextN6 = { mount };
  scheduleMount();
})();