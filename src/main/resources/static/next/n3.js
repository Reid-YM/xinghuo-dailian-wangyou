(() => {
  'use strict';

  const $=(selector,root=document)=>root.querySelector(selector);
  const text=value=>String(value??'').trim();
  const trim=(value,max)=>text(value).slice(0,max);
  let submitting=false;
  let idempotencyKey='';

  function make(tag,className,value){
    const element=document.createElement(tag);
    if(className) element.className=className;
    if(value!==undefined) element.textContent=String(value);
    return element;
  }

  function uuid(){
    if(globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
    const bytes=new Uint8Array(16);
    globalThis.crypto?.getRandomValues?.(bytes);
    if(!bytes.some(Boolean)) for(let i=0;i<bytes.length;i+=1) bytes[i]=Math.floor(Math.random()*256);
    bytes[6]=(bytes[6]&15)|64;
    bytes[8]=(bytes[8]&63)|128;
    const hex=[...bytes].map(value=>value.toString(16).padStart(2,'0')).join('');
    return `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`;
  }

  function urlParam(name){
    return trim(new URLSearchParams(location.search).get(name)||'',64)||null;
  }

  function field(labelText,control){
    const wrapper=make('div','field');
    const label=make('label','',labelText);
    if(control.id) label.htmlFor=control.id;
    wrapper.append(label,control);
    return wrapper;
  }

  function select(id,items){
    const control=document.createElement('select');
    control.id=id;
    items.forEach(([value,label])=>{
      const option=document.createElement('option');
      option.value=value;
      option.textContent=label;
      control.append(option);
    });
    return control;
  }

  function installFields(){
    const quotePage=$('.page[data-page="quote"]');
    if(!quotePage||$('#n3ContactMethod')) return;

    const sections=[...quotePage.querySelectorAll('.formSec')];
    const chooseGrid=sections[0]?.querySelector('.formGrid');
    if(chooseGrid){
      const platform=select('n3Platform',[
        ['','请选择平台 / 不确定'],
        ['Steam','Steam · PC'],
        ['Battle.net','Battle.net · PC'],
        ['PlayStation','PlayStation'],
        ['Xbox','Xbox'],
        ['Nintendo Switch 2','Nintendo Switch 2']
      ]);
      const expectedTime=select('n3ExpectedTime',[
        ['','按客服安排'],['尽快','尽快'],['今天','今天'],['24小时内','24小时内'],['1–3天','1–3天'],['不着急','不着急']
      ]);
      chooseGrid.append(field('游戏平台',platform),field('期望时间',expectedTime));
    }

    const contactGrid=sections[2]?.querySelector('.formGrid');
    if(contactGrid){
      const existingInputs=[...contactGrid.querySelectorAll('input')];
      if(existingInputs[0]){
        existingInputs[0].id='n3CustomerName';
        existingInputs[0].maxLength=80;
      }
      if(existingInputs[1]){
        existingInputs[1].id='n3CustomerContact';
        existingInputs[1].maxLength=120;
        existingInputs[1].autocomplete='off';
      }
      const method=select('n3ContactMethod',[
        ['','请选择联系方式'],['微信','微信'],['QQ','QQ'],['手机','手机'],['其他','其他']
      ]);
      contactGrid.append(field('联系渠道 *',method));
    }

    const goal=$('#goal');
    if(goal) goal.maxLength=500;

    const submit=$('#submit');
    if(submit){
      const legal=make('label','n3Legal');
      legal.style.cssText='display:flex;gap:9px;align-items:flex-start;margin-top:14px;color:#9ea5ad;font-size:11px;line-height:1.55';
      const consent=document.createElement('input');
      consent.type='checkbox';
      consent.id='n3PrivacyConsent';
      consent.style.marginTop='2px';
      const copy=document.createElement('span');
      copy.append(document.createTextNode('我已阅读并同意 '));
      const rules=document.createElement('a');
      rules.href='/rules';
      rules.target='_blank';
      rules.rel='noopener';
      rules.textContent='服务规则';
      rules.style.color='var(--gold)';
      copy.append(rules,document.createTextNode(' 和 '));
      const privacy=document.createElement('a');
      privacy.href='/privacy';
      privacy.target='_blank';
      privacy.rel='noopener';
      privacy.textContent='隐私说明';
      privacy.style.color='var(--gold)';
      copy.append(privacy,document.createTextNode('，不会提交账号密码、验证码或令牌。'));
      legal.append(consent,copy);
      submit.before(legal);

      const message=make('div','', '');
      message.id='n3QuoteMessage';
      message.setAttribute('aria-live','polite');
      message.style.cssText='margin-top:10px;min-height:18px;color:#9ea5ad;font-size:11px;line-height:1.5';
      submit.after(message);
    }
  }

  function selectedText(id){
    const control=$(id);
    return text(control?.selectedOptions?.[0]?.textContent)||text(control?.value);
  }

  function payload(){
    const game=$('#qGame');
    const selectedService=selectedText('#qSvc');
    const contact=trim($('#n3CustomerContact')?.value,120);
    const method=trim($('#n3ContactMethod')?.value,40);
    const currentProgress=trim($('#goal')?.value,500);
    return {
      game:trim(game?.value||selectedText('#qGame'),100),
      demand:trim(selectedService,200),
      currentProgress:currentProgress||null,
      screenshotAssetId:null,
      platform:trim($('#n3Platform')?.value,50)||null,
      expectedTime:trim($('#n3ExpectedTime')?.value,100)||null,
      contactMethod:method||null,
      customerContact:contact||null,
      privacyConsent:!!$('#n3PrivacyConsent')?.checked,
      firstTouchSource:'frontend-next',
      utmSource:urlParam('utm_source'),
      utmMedium:urlParam('utm_medium'),
      utmCampaign:urlParam('utm_campaign'),
      referrer:trim(document.referrer,300)||null
    };
  }

  function validate(body){
    if(!body.game) return '请选择游戏。';
    if(!body.demand) return '请选择具体服务。';
    if(!body.contactMethod) return '请选择联系方式。';
    if(!body.customerContact) return '请填写微信号、手机号或 QQ 等联系号。';
    if(!body.privacyConsent) return '请先阅读并同意服务规则与隐私说明。';
    return '';
  }

  function message(value,kind='normal'){
    const node=$('#n3QuoteMessage');
    if(!node) return;
    node.textContent=value;
    node.style.color=kind==='ok'?'#76e3a3':kind==='error'?'#ff9b87':'#9ea5ad';
  }

  function serverMessage(body,status){
    const candidate=text(body?.message)||text(body?.error)||text(body?.detail);
    if(candidate) return candidate.slice(0,240);
    if(status===400) return '提交内容需要调整，请检查必填项后重试。';
    if(status===409) return '该询价正在处理中，请稍后查看结果。';
    if(status===429) return '提交过于频繁，请稍后再试。';
    return `提交失败（${status}），请稍后重试。`;
  }

  async function submitQuote(){
    if(submitting) return;
    const button=$('#submit');
    if(!button) return;
    const shell=window.XHFrontendShell;
    if(shell){await shell.configurationReady;if(!shell.configuration){message('站点配置尚未读取，请先重试读取配置。','error');return;}if(shell.configuration.quoteEnabled===false){message('当前暂停在线询价，请联系客服核对。','error');return;}}
    const catalog=window.XHFrontendNextN2?.state;
    if(catalog && (!catalog.catalog || !Array.isArray(catalog.services) || !catalog.services.length)){message('请先读取有效的游戏与服务目录。','error');return;}
    if(submitting)return;
    const body=payload();
    const problem=validate(body);
    if(problem){
      message(problem,'error');
      return;
    }

    if(!idempotencyKey) idempotencyKey=uuid();
    submitting=true;
    button.disabled=true;
    const previous=button.textContent;
    button.textContent='正在提交…';
    message('正在安全提交询价，请勿重复点击。');

    try{
      const response=await fetch('/api/quote',{
        method:'POST',
        credentials:'same-origin',
        headers:{
          Accept:'application/json',
          'Content-Type':'application/json',
          'Idempotency-Key':idempotencyKey
        },
        body:JSON.stringify(body)
      });
      let result={};
      try{ result=await response.json(); }catch(ignore){ result={}; }
      if(!response.ok) throw Object.assign(new Error(serverMessage(result,response.status)),{status:response.status});

      const businessNo=trim(result?.businessNo,100);
      message(businessNo?`询价提交成功，业务编号：${businessNo}`:'询价提交成功，客服将尽快确认。','ok');
      button.textContent='✓ 提交成功';
      idempotencyKey='';
      if(businessNo){
        const record=document.createElement('a');
        record.href=`/record/${encodeURIComponent(businessNo)}`;
        record.textContent='查看业务记录 →';
        record.style.cssText='display:inline-block;margin-top:8px;color:var(--gold);font-size:11px';
        $('#n3QuoteMessage')?.append(document.createElement('br'),record);
      }
      setTimeout(()=>{
        button.disabled=false;
        button.textContent='再次提交询价 →';
      },1800);
      return;
    }catch(error){
      message(text(error?.message)||'网络异常，当前内容已保留，可直接重试。','error');
      button.disabled=false;
      button.textContent='重试提交 →';
      // Keep the same idempotency key after failures so a retry cannot create a duplicate.
    }finally{
      submitting=false;
      if(button.disabled&&button.textContent==='正在提交…'){
        button.disabled=false;
        button.textContent=previous;
      }
    }
  }

  function activate(){
    installFields();
    const button=$('#submit');
    if(!button) return;
    button.onclick=null;
    button.textContent='提交询价 →';
    button.dataset.n3RealSubmit='true';
    button.addEventListener('click',submitQuote);
    const badge=$('.badge');
    if(badge) badge.textContent='FRONTEND NEXT · N3 QUOTE ENABLED';
    const demoNote=[...document.querySelectorAll('.page[data-page="quote"] .sub')].find(node=>node.textContent.includes('演示模式'));
    if(demoNote) demoNote.textContent='新版询价已接入现有安全提交接口；旧 /quote 仍保留，可随时回退。';
  }

  window.XHFrontendNextN3={submit:submitQuote,payload};
  activate();
})();
