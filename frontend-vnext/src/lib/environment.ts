export type DeviceEnvironment = {
  isWeChat: boolean;
  isMobile: boolean;
  isIOS: boolean;
  isStandalone: boolean;
  label: string;
};

export function detectEnvironment(): DeviceEnvironment {
  const ua = navigator.userAgent || '';
  const isWeChat = /MicroMessenger/i.test(ua);
  const isMobile = /Android|iPhone|iPad|iPod|Mobile/i.test(ua) || window.matchMedia('(max-width: 767px)').matches;
  const isIOS = /iPhone|iPad|iPod/i.test(ua);
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches;
  const label = isWeChat ? '微信 WebView' : isMobile ? (isIOS ? 'iOS 浏览器' : '移动浏览器') : '桌面浏览器';
  return { isWeChat, isMobile, isIOS, isStandalone, label };
}

export function installViewportSync(): () => void {
  const root = document.documentElement;
  const update = () => {
    const viewport = window.visualViewport;
    const height = viewport?.height ?? window.innerHeight;
    root.style.setProperty('--vnext-vh', `${height * 0.01}px`);
    root.style.setProperty('--vnext-viewport-width', `${window.innerWidth}px`);
  };

  const environment = detectEnvironment();
  root.classList.toggle('is-wechat', environment.isWeChat);
  root.classList.toggle('is-ios', environment.isIOS);
  root.classList.toggle('is-mobile', environment.isMobile);

  update();
  window.addEventListener('resize', update, { passive: true });
  window.visualViewport?.addEventListener('resize', update, { passive: true });

  return () => {
    window.removeEventListener('resize', update);
    window.visualViewport?.removeEventListener('resize', update);
  };
}
