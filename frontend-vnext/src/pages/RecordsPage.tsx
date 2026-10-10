import { ShieldCheck } from 'lucide-react';
import { Badge, Button, Card } from '../components/ui';
import { EmptyState, PageIntro } from '../components/public-content';
import { useBusinessRecord } from '../features/records/useBusinessRecord';

export function RecordsPage() {
  const { businessNo, setBusinessNo, result, error, busy, search } = useBusinessRecord();

  return <section className="v-page v-shell"><PageIntro eyebrow="ORDER / PROGRESS" title="业务进度查询" text="输入询价或订单业务编号，查看当前阶段、进度和最近更新时间。" /><div className="v-record-layout"><Card className="v-query-card"><label><span>业务编号</span><div className="v-input-action"><input value={businessNo} onChange={event => setBusinessNo(event.target.value)} placeholder="例如：XH7KM4Q2AZ8P" onKeyDown={event => event.key === 'Enter' && search()} /><Button className="v-primary" disabled={busy || !businessNo.trim()} onClick={search}>{busy ? '查询中' : '查询'}</Button></div></label><p><ShieldCheck size={16} /> 查询接口保留现有限流与公开信息边界。</p></Card><Card className="v-result-card">{!result && !error && <EmptyState title="等待查询" text="结果会以卡片显示，手机端不需要横向滚动。" />}{error && <EmptyState title="未找到结果" text={error} />}{result && <div className="v-record-result"><Badge>{result.stage}</Badge><h3>{result.title}</h3><dl><div><dt>状态</dt><dd>{result.status}</dd></div><div><dt>进度</dt><dd>{result.progress}</dd></div><div><dt>更新时间</dt><dd>{result.updatedAt}</dd></div></dl>{result.customerNote && <p className="v-record-note">客服备注：{result.customerNote}</p>}</div>}</Card></div></section>;
}
