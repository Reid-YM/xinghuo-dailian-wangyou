(() => {
  'use strict';const $=s=>document.querySelector(s);let sequence=0;
  const node=(tag,text)=>{const n=document.createElement(tag);if(text!=null)n.textContent=String(text);return n;};
  const table=(selector,rows,fields)=>{const list=Array.isArray(rows)?rows:[];$(selector).replaceChildren(...list.map(item=>{const row=node('tr');fields.forEach(field=>row.append(node('td',field==='event'?(window.XWLabels?.value(item[field])??item[field]):item[field]??'—')));return row;}));if(!list.length){const row=node('tr'),cell=node('td','暂无事件记录');cell.colSpan=fields.length;row.append(cell);$(selector).append(row);}};
  async function load(){const id=++sequence;$('#statisticsMessage').textContent='正在读取统计…';try{const days=new FormData($('#statisticsFilters')).get('days');const response=await fetch('/api/admin/xingwei/statistics?days='+encodeURIComponent(days),{credentials:'same-origin',cache:'no-store'});const data=await response.json();if(!response.ok)throw new Error(data.message||'统计读取失败，请重试。');if(id!==sequence)return;
    $('#statisticsSummary').replaceChildren(...[['累计事件',data.total],['窗口内事件',data.recent],['最近发生',data.lastEventAt||'暂无'],...(Array.isArray(data.operations)?data.operations:[]).map(item=>[item.label,item.count])].map(([label,value])=>{const box=node('div');box.append(node('strong',value),node('span',label));return box;}));table('#statisticsFunnel',data.funnel,['label','count']);table('#statisticsTrend',data.trend,['date','count']);table('#statisticsEvents',data.events,['event','total','recent','lastAt']);$('#statisticsMessage').textContent='已读取最近 '+data.days+' 天数据。';
  }catch(error){if(id===sequence)$('#statisticsMessage').textContent=error.message;}}
  $('#statisticsFilters').onsubmit=event=>{event.preventDefault();load();};load();
})();
