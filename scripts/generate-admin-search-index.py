#!/usr/bin/env python3
# coding: utf-8
"""Build navigation metadata from owned templates and site field definitions.
Run with --check in validation. No API data, employee names or business records are indexed.
"""
from html.parser import HTMLParser
from pathlib import Path
import json, re, sys
ROOT=Path(__file__).resolve().parents[1]
T=ROOT/'src/main/resources/templates'
A=ROOT/'src/main/resources/static/assets'
class Node:
    def __init__(self, tag='', attrs=(), parent=None):
        self.tag=tag; self.attrs=dict(attrs); self.parent=parent; self.children=[]; self.parts=[]
    def text(self):
        return re.sub(r'\s+',' ',''.join(x.text() if isinstance(x,Node) else x for x in self.parts)).strip()
    def all(self):
        yield self
        for x in self.children: yield from x.all()
    def ancestors(self):
        n=self.parent
        while n: yield n; n=n.parent
class Parser(HTMLParser):
    def __init__(self, text):
        super().__init__(convert_charrefs=True); self.root=Node();self.current=self.root;self.feed(text)
    def handle_starttag(self,tag,attrs):
        n=Node(tag,attrs,self.current);self.current.children.append(n);self.current.parts.append(n)
        if tag not in ['input','meta','link','img','br','hr','source','wbr','area','base','embed','param']:self.current=n
    def handle_startendtag(self,tag,attrs):self.handle_starttag(tag,attrs);self.handle_endtag(tag)
    def handle_endtag(self,tag):
        n=self.current
        while n.parent:
            if n.tag==tag:self.current=n.parent;return
            n=n.parent
    def handle_data(self,data):self.current.parts.append(data)
def parse(path):return Parser(path.read_text()).root
rows=[];seen=set()
def add(code,label,href,note,keywords='',permission=''):
    label=re.sub(r'\s+',' ',label).strip()
    key=(code,label,href)
    if not label or key in seen:return
    seen.add(key);row=dict(module=code,label=label,href=href,note=note,keywords=keywords)
    if permission:row['permission']=permission
    rows.append(row)
aliases={'cases':'after-sales'}
fragments={'frontend-controls':['frontend-control-workspace'],'guides':['guide-workspace'],'tool-station':['tool-station-editor']}
for module in parse(T/'admin-xingwei.html').all():
    if 'xw-module' not in module.attrs.get('class','').split():continue
    code=next(n.text() for n in module.all() if 'xw-module-index' in n.attrs.get('class',''))
    href=module.attrs.get('href');
    if not href:continue
    title=next(n.text() for n in module.all() if n.tag=='h3')
    add(code,title,href,'打开功能工作台',module.attrs.get('data-search',''))
    if href.startswith('#'):continue
    name=aliases.get(href.split('/')[-1].split('?')[0],href.split('/')[-1].split('?')[0])
    path=T/('admin-xingwei-'+name+'.html')
    if not path.exists():continue
    roots=[parse(path)]+[parse(T/'fragments'/(f+'.html')) for f in fragments.get(name,[])]
    for root in roots:
        for n in root.all():
            if n.tag=='button' and n.text() and n.text() not in ['×','关闭','取消']:
                ancestors=list(n.ancestors());concealed=any(a.tag=='dialog' or 'hidden' in a.attrs for a in ancestors) or 'hidden' in n.attrs
                if not concealed:
                    add(code,n.text(),href+('#'+n.attrs['id'] if n.attrs.get('id') else ''),'定位到操作按钮；点击搜索结果不会执行操作')
            if n.tag not in ['input','select','textarea']:continue
            if n.attrs.get('type') in ['hidden','submit','button']:continue
            label=next((a for a in n.ancestors() if a.tag=='label'),None)
            if label:
                labeltext=''.join(part if isinstance(part,str) else part.text() if part.tag in ['span','b','strong'] else '' for part in label.parts).strip()
            else:
                label=next((a for a in root.all() if a.tag=='label' and a.attrs.get('for')==n.attrs.get('id')),None)
                if not label and n.parent:
                    siblings=n.parent.children;index=siblings.index(n)
                    if index and siblings[index-1].tag=='label':label=siblings[index-1]
                labeltext=label.text() if label else n.attrs.get('aria-label','')
            if not labeltext:continue
            ancestors=list(n.ancestors());dialog=next((a for a in ancestors if a.tag=='dialog'),None)
            concealed=any('hidden' in a.attrs for a in ancestors)
            if concealed and not dialog:continue
            if name=='business' and dialog:
                owner=dialog.attrs.get('id')
                if owner=='xwbOrderActionDialog' and code!='B02':continue
                if owner=='xwbQuoteActionDialog' and code!='B01':continue
                if owner=='xwbAfterSalesDetailDialog':continue
            anchor=n.attrs.get('id')
            # Never claim a closed record editor or hidden panel is a directly reachable field.
            direct=bool(anchor and not dialog and not concealed)
            dest=href+'#'+anchor if direct else href
            add(code,labeltext,dest,'定位到筛选或设置字段' if direct else '进入工作台后选择记录或新建，再操作此字段',n.attrs.get('name','')+' '+n.attrs.get('placeholder',''))
    # Explicit labels from a module's own record editor; shared multi-mode editors
    # are deliberately excluded to avoid attaching a field to the wrong record type.
    js=A/('admin-xingwei-'+name+'.js')
    if name in ['catalog-details','channels','accounts','security','maintenance']:
        if name=='security':js=A/'admin-xingwei-accounts.js'
        if js.exists():
            for key,label in re.findall(r"(?:field|number|image)\('([^']+)','([^']+)'",js.read_text()):
                add(code,label,href,'进入工作台，选择对应资料类型和记录后编辑此字段',key)
            for label in re.findall(r"button\('([^']+)'",js.read_text()):
                add(code,label,href,'进入工作台后选择对应记录；搜索不会代为执行操作')
# Root employee records are permission scoped and require an explicit record choice.
for n in parse(T/'admin-xingwei.html').all():
    if n.tag!='label':continue
    dialog=next((a for a in n.ancestors() if a.tag=='dialog'),None)
    if dialog and dialog.attrs.get('id')=='xwEmployeeDialog':
        label=''.join(part for part in n.parts if isinstance(part,str)).strip()
        add('O02',label,'#org-employees','定位到员工档案，选择员工后编辑对应字段')
add('O01','按中心筛选岗位','#org-positions','定位到岗位词库与中心筛选')
add('O02','按中心筛选岗位','#org-positions','定位到岗位词库与中心筛选')
add('O02','员工角色','#org-employees','定位到员工档案，选择员工后维护角色')
add('O03','角色数据范围','#org-roles','定位到角色与数据范围资料')
add('O03','角色权限点','#org-roles','定位到角色列表，选择角色后维护权限')
# Schema-driven site controls are rendered after the permission/data requests complete.
source=(A/'admin-xingwei-site.js').read_text()
for line in source.splitlines():
    m=re.match(r"\s*(\w+):\[",line)
    if not m:continue
    section=m[1];repeat=None
    for match in re.finditer(r"(text|number|check|choice|area|image|repeat)\('([^']+)','([^']+)'",line):
        kind,key,label=match.groups()
        if kind=='repeat':repeat=key
        anchor=repeat or key
        add('S01',label,'/admin/xingwei/site?section='+section+'#site-field-'+anchor,'定位到站点配置字段' if not repeat else '定位到资料组；已有条目可编辑，空资料组可新增',key,'seo:view' if section=='seo' else 'site:view')
# Reject stale/broken direct anchors rather than publishing plausible-looking links.
from urllib.parse import urlsplit
for row in rows:
    url=urlsplit(row['href']);anchor=url.fragment
    if not anchor or anchor.startswith('site-field-'):continue
    name=aliases.get(url.path.split('/')[-1],url.path.split('/')[-1]) if url.path else ''
    path=T/('admin-xingwei-'+name+'.html') if name else T/'admin-xingwei.html'
    material=path.read_text()+''.join((T/'fragments'/(f+'.html')).read_text() for f in fragments.get(name,[]))
    if ('id="'+anchor+'"') not in material:raise SystemExit('搜索目标不存在: '+row['href'])
payload='// Generated by scripts/generate-admin-search-index.py; do not edit by hand.\nwindow.XWSearchIndex=Object.freeze('+json.dumps(rows,ensure_ascii=False,separators=(',',':'))+');\n'
out=A/'admin-xingwei-search-index.js'
if '--check' in sys.argv:
    if not out.exists() or out.read_text()!=payload:raise SystemExit('后台搜索索引已过期，请重新生成')
    print('Search index verified:',len(rows),'entries')
else:out.write_text(payload);print('Search index generated:',len(rows),'entries')
