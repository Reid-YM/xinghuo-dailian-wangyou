import { useEffect, useMemo, useState } from 'react';
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
} from '../../lib/api';
import { serviceGame, serviceLabel, type ServiceRecord } from '../services/model';

export type QuoteFlowProps = {
  services: ServiceRecord[];
  config: JsonRecord | null | undefined;
  catalog: JsonRecord | null | undefined;
};

export function useQuoteFlow({ services, config, catalog }: QuoteFlowProps) {
  const preferredSlug = queryParam('service');
  const preferred = services.find(service => String(service.slug || '') === preferredSlug);
  const [step, setStep] = useState(1);
  const [selected, setSelected] = useState<ServiceRecord | null>(preferred ?? null);
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
    return rows
      .filter((item): item is JsonRecord => typeof item === 'object' && !!item && item.active !== false)
      .map(item => pickText(item, ['name', 'code']))
      .filter(Boolean);
  }, [catalog]);

  const chooseService = (service: ServiceRecord, index: number) => {
    setSelected(service);
    setDemand(serviceLabel(service, index));
  };

  const setCustomDemand = (value: string) => {
    setDemand(value);
    setSelected(null);
  };

  const next = () => {
    setError('');
    if (step === 1 && !demand.trim()) {
      setError('请选择服务或填写具体需求。');
      return;
    }
    setStep(value => Math.min(3, value + 1));
  };

  const previous = () => {
    setError('');
    setStep(value => Math.max(1, value - 1));
  };

  const submit = async () => {
    setError('');
    if (!quoteEnabled) return setError('当前网页询价已暂停，请通过客服渠道联系。');
    if (!demand.trim()) return setError('请填写具体需求。');
    if (!privacyConsent) return setError('请先阅读并同意服务规则与隐私说明。');
    if (!accountContactAvailable && (!contactMethod || !customerContact.trim())) {
      return setError('请填写联系方式，方便客服确认报价。');
    }

    setBusy(true);
    try {
      const screenshotAssetId = file ? await uploadQuoteScreenshot(file) : null;
      const result = await submitQuote({
        game: serviceGame(selected ?? {}),
        demand: demand.trim(),
        currentProgress: currentProgress.trim() || null,
        screenshotAssetId,
        platform: platform || null,
        expectedTime: expectedTime.trim() || null,
        contactMethod: contactMethod || null,
        customerContact: customerContact.trim() || null,
        privacyConsent: true,
        firstTouchSource: firstTouchSource(),
        utmSource: queryParam('utm_source'),
        utmMedium: queryParam('utm_medium'),
        utmCampaign: queryParam('utm_campaign'),
        referrer: document.referrer ? document.referrer.slice(0, 300) : null,
      }, idempotencyKey);
      setSuccess(result);
      setIdempotencyKey(makeIdempotencyKey());
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '提交失败，请稍后重试。');
    } finally {
      setBusy(false);
    }
  };

  return {
    step,
    selected,
    demand,
    currentProgress,
    platform,
    expectedTime,
    contactMethod,
    customerContact,
    privacyConsent,
    file,
    accountContactAvailable,
    busy,
    error,
    success,
    quoteEnabled,
    platforms,
    chooseService,
    setCustomDemand,
    setCurrentProgress,
    setPlatform,
    setExpectedTime,
    setContactMethod,
    setCustomerContact,
    setPrivacyConsent,
    setFile,
    next,
    previous,
    submit,
  };
}

export function quoteSuccessText(config: JsonRecord | null | undefined) {
  return pickText(config, ['quoteSuccessText'], '客服会根据你提交的需求与当前进度确认报价。');
}
