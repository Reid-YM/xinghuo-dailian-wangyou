import { Boxes, CircleUserRound, House, MessageSquareText, Search } from 'lucide-react';

export type PageKey = 'home' | 'services' | 'quote' | 'records' | 'account';

export const pagePaths: Record<PageKey, string> = {
  home: '/',
  services: '/services',
  quote: '/quote',
  records: '/records',
  account: '/account',
};

export const navItems: Array<{ key: PageKey; label: string; icon: typeof House }> = [
  { key: 'home', label: '首页', icon: House },
  { key: 'services', label: '服务', icon: Boxes },
  { key: 'quote', label: '询价', icon: MessageSquareText },
  { key: 'records', label: '进度', icon: Search },
  { key: 'account', label: '我的', icon: CircleUserRound },
];

export function pageFromPath(pathname: string): PageKey {
  if (pathname.startsWith('/services')) return 'services';
  if (pathname.startsWith('/quote')) return 'quote';
  if (pathname.startsWith('/records')) return 'records';
  if (pathname === '/account' || pathname === '/account/') return 'account';
  return 'home';
}
