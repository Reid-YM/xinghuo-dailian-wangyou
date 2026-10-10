import { useEffect, useMemo, useState } from 'react';
import { ArrowRight, BadgeCheck, ChevronRight, Gamepad2, Upload } from 'lucide-react';
import { Button, Card } from '../components/ui';
import { PageIntro, serviceKey, serviceLabel, servicePrice } from '../components/public-content';
import {
  firstTouchSource,
  loadAccountMe,
  makeIdempotencyKey,
  pickBoolean,
  pickText,
  queryParam,
  submitQuote,
  uploadQuoteScreenshot,
  type JsonRecord,
  type QuoteResponse,
} from '../lib/api';

export function QuotePage({ services, config, catalog }: { services: JsonRecord[]; config: JsonRecord | null | undefined; catalog: JsonRecord | null | undefined }) {
  const preferredSlug = queryParam('service');
  const preferred = services.find(service => String(service.slug || '') === preferredSlug);
  const [step, setStep] = useState(1);
  const [selected, setSelected] = useState<JsonRecord | null>(preferred ?? null);
  const [demand, setDemand] = useState(preferred ? serviceLabel(preferred, 0) : '');
  const [currentProgress, setCurrentProgress] = useState('');
  const [platform, setPlatform] = useState('');
  const [expectedTime, setExpectedTime] = useState('');
  const [contactMethod, setContactMethod] = useState('');
  const [customerContact, setCustomerContact] = useState('');
  const [privacyConsent, setPrivacyConsent] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [accountContactAvailable, setAccountContactAvailable] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<QuoteResponse | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState(() => makeIdempotencyKey());
  const quoteEnabled = pickBoolean(config, 'quoteEnabled', true);

  useEffect(() => {
    const controller = new AbortController();
    loadAccountMe(controller.signal).then(me => {
      if (!me) return;
      const contacts = Array.isArray(me.quoteContacts) ? me.quoteContacts : [];
      const type = String(me.contactType || '').toUpperCase();
      setAccountContactAvailable(contacts.length > 0 || ['PHONE', 'WECHAT', 'QQ'].includes(type));
    });
    return () => controller.abort();
  }, []);

  const platforms = useMemo(() => {
    const rows = Array.isArray(catalog?.platforms) ? catalog.platforms : [];
    return rows.filter((item): item is JsonRecord => typeof item === 'object' && !!item && item.active !== false).map(item => pickText(item, ['name', 'code'])).filter(Boolean);
  }, [catalog]);

  const chooseService = (service: JsonRecord, index: number) => { setSelected(service); setDemand(serviceLabel(service, index)); };
  const next = () => { setError(''); if (step === 1 && !demand.trim()) return setError('请选择服务或填写具体需求。'); setStep(value => Math.min(3, value + 1)); };
  const submit = async () => {
    setError('');
    if (!quoteEnabled) return setError('当前网页询价已暂停，请通过客服渠道联系。');
    if (!demand.trim()) return setError('请填写具体需求。');
    if (!privacyConsent) return setError('请先阅读并同意服务规则与隐私说明。');
    if (!accountContactAvailable && (!contactMethod || !customerContact.trim())) return setError('请填写联系方式，方便客服确认报价。');
    setBusy(true);
    try {
      const screenshotAssetId = file ? await uploadQuoteScreenshot(file) : null;
      const result = await submitQuote({
        game: pickText(selected, ['gameCode'], 'CODMW4'), demand: demand.trim(), currentProgress: currentProgress.trim() || null,
        screenshotAssetId, platform: platform || null, expectedTime: expectedTime.trim() || null, contactMethod: contactMethod || null,
        customerContact: customerContact.trim() || null, privacyConsent: true, firstTouchSource: firstTouchSource(), utmSource: queryParam('utm_source'),
        utmMedium: queryParam('utm_medium'), utmCampaign: queryParam('utm_campaign'), referrer: document.referrer ? document.referrer.slice(0, 300) : null,
      }, idempotencyKey);
      setSuccess(result); setIdempotencyKey(makeIdempotencyKey());
    } catch (cause) { setError(cause instanceof Error ? cause.message : '提交失败，请稍后重试。'); }
    finally { setBusy(false); }
  };

  if (success) return <section className="v-page v-shell"><PageIntro eyebrow="QUOTE CREATED" title="需求已提交" text="请保存业务编号，后续报价确认与履约进度都以该编号为准。" /><Card className="v-success-card"><BadgeCheck size={48} /><span>业务编号</span><strong>{success.businessNo}</strong><p>{pickText(config, ['quoteSuccessText'], '客服会根据你提交的需求与当前进度确认报价。')}</p><div className="v-hero-actions"><Button className="v-primary" onClick={() => window.location.assign(`/record/${encodeURIComponent(success.businessNo)}`)}>查看业务进度</Button><Button className="v-secondary" onClick={() => window.location.assign('/contact')}>联系客服</Button></div></Card></section>;

  return <section className="v-page v-shell">
    <PageIntro eyebrow="QUOTE" title="提交询价" text="选择服务并说明当前进度。网站只收集完成报价所需的信息，不要提交账号密码、验证码或令牌。" />
    <div className="v-stepper" aria-label="询价步骤">{['选择服务', '补充需求', '确认提交'].map((label, index) => <div key={label} className={step >= index + 1 ? 'is-active' : ''}><span>{index + 1}</span><strong>{label}</strong></div>)}</div>
    <Card className="v-form-card">
      {step === 1 && <><div className="v-choice-grid">{services.slice(0, 8).map((service, index) => { const label = serviceLabel(service, index); return <button key={serviceKey(service, index)} className={selected === service ? 'v-choice is-selected' : 'v-choice'} onClick={() => chooseService(service, index)}><Gamepad2 size={20} /><span><strong>{label}</strong><small>{servicePrice(service)} · {pickText(service, ['durationText'], '按进度确认')}</small></span><ChevronRight size={18} /></button>; })}</div><label className="v-standalone-field"><span>没有合适选项？直接描述需求</span><input value={demand} onChange={event => { setDemand(event.target.value); setSelected(null); }} maxLength={200} placeholder="例如：某武器升满 / 指定迷彩 / 活动奖励" /></label></>}
      {step === 2 && <div className="v-form-grid"><label><span>当前进度</span><textarea rows={4} value={currentProgress} onChange={event => setCurrentProgress(event.target.value)} maxLength={500} placeholder="例如：已完成一半；具体进度可结合截图说明" /></label><label><span>游戏平台</span><select value={platform} onChange={event => setPlatform(event.target.value)}><option value="">不确定，由客服确认</option>{(platforms.length ? platforms : ['Steam', 'Battle.net', 'PlayStation', 'Xbox', 'Nintendo Switch 2']).map(item => <option key={item} value={item}>{item}</option>)}</select></label><label><span>期望完成时间</span><input value={expectedTime} onChange={event => setExpectedTime(event.target.value)} maxLength={100} placeholder="例如：本周内 / 不加急" /></label><label className="v-upload-field"><span>任务截图（可选，≤5MB）</span><span className="v-upload-control"><Upload size={19} /><input type="file" accept="image/jpeg,image/png,image/*" onChange={event => setFile(event.target.files?.[0] ?? null)} />{file ? file.name : '选择图片'}</span></label></div>}
      {step === 3 && <div className="v-form-grid">{accountContactAvailable && <div className="v-account-contact-note"><BadgeCheck size={18} /> 账号已绑定可用联系方式；下面可以留空，也可以临时填写其他联系方式。</div>}<label><span>联系方式</span><select value={contactMethod} onChange={event => setContactMethod(event.target.value)}><option value="">{accountContactAvailable ? '使用账号绑定方式' : '请选择'}</option><option value="微信">微信</option><option value="QQ">QQ</option><option value="手机">手机</option><option value="其他">其他</option></select></label><label><span>联系号</span><input value={customerContact} onChange={event => setCustomerContact(event.target.value)} maxLength={120} placeholder={accountContactAvailable ? '留空使用账号绑定联系方式' : '微信号 / 手机号 / QQ号'} /></label><label className="v-consent"><input type="checkbox" checked={privacyConsent} onChange={event => setPrivacyConsent(event.target.checked)} /><span>我已阅读并同意 <a href="/rules" target="_blank" rel="noreferrer">服务规则</a> 和 <a href="/privacy" target="_blank" rel="noreferrer">隐私说明</a>，不会提交账号密码、验证码或令牌。</span></label><div className="v-confirm-summary"><span>当前需求</span><strong>{demand}</strong><small>{platform || '平台待确认'} · {expectedTime || '时间待确认'}</small></div></div>}
      {error && <div className="v-form-error" role="alert">{error}</div>}
      <div className="v-form-actions"><Button className="v-secondary" disabled={step === 1 || busy} onClick={() => { setError(''); setStep(value => Math.max(1, value - 1)); }}>上一步</Button>{step < 3 ? <Button className="v-primary" disabled={busy} onClick={next}>下一步 <ArrowRight size={17} /></Button> : <Button className="v-primary" disabled={busy || !quoteEnabled} onClick={submit}>{busy ? '正在提交…' : quoteEnabled ? '确认提交' : '当前暂停询价'}</Button>}</div>
    </Card>
  </section>;
}
