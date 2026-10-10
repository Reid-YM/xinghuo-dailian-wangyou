import { ChevronRight, CircleUserRound } from 'lucide-react';
import { Card } from '../components/ui';
import { PageIntro } from '../components/public-content';

export function AccountPage() {
  const items = [
    ['/account/requests', '我的需求', '查看询价与客服处理状态'],
    ['/account/orders', '我的订单', '查看履约状态与交付进度'],
    ['/account/progress', '工具存档', '查看跨设备保存的工具进度'],
    ['/account/settings', '账号设置', '资料、回访方式与安全设置'],
  ];

  return <section className="v-page v-shell"><PageIntro eyebrow="ACCOUNT" title="个人中心" text="需求、订单、工具进度与账号设置集中管理。当前页面继续沿用原有账号登录与权限校验。" /><div className="v-account-grid">{items.map(([href, title, text]) => <a key={href} className="v-account-link" href={href}><span className="v-icon-tile"><CircleUserRound size={21} /></span><span><strong>{title}</strong><small>{text}</small></span><ChevronRight size={18} /></a>)}</div><Card className="v-account-help"><span className="v-eyebrow">SECURITY</span><h2>账号与业务信息分开管理</h2><p>询价时已绑定的手机号、微信或 QQ 可由后端自动补齐；网页不会展示或要求游戏账号密码、验证码或令牌。</p></Card></section>;
}
