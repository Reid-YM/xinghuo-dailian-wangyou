(function(){
 if(!document.querySelector('link[data-vnext-formal-theme]')){
   const link=document.createElement('link');
   link.rel='stylesheet';link.href='/assets/vnext-formal-ui.css?v=20261010ui03';link.dataset.vnextFormalTheme='1';
   document.head.append(link);
 }
 if(!document.querySelector('link[data-admin-brand-layout-fixes]')){
   const fixes=document.createElement('link');
   fixes.rel='stylesheet';fixes.href='/assets/admin-brand-layout-fixes.css?v=20261008b002';fixes.dataset.adminBrandLayoutFixes='1';
   document.head.append(fixes);
 }
})();
(function(){
 const form=document.getElementById('configForm'), section=document.querySelector('[data-config-section="visual"]');
 if(!form||!section||!window.SiteImageControls)return;
 const state=document.getElementById('visualSaveState');
 const set=(el,values)=>Object.entries(values).forEach(([k,v])=>el&&el.style.setProperty(k,String(v),'important'));
 const value=name=>form.elements.namedItem(name)?.value??'';
 section.querySelectorAll('.field').forEach(field=>{const input=field.querySelector('input:not([type="hidden"]),select'),label=field.querySelector('label');if(input&&label){input.id=input.id||'visual-'+input.name;label.htmlFor=input.id;}});
 let saved=null;
 const fields=()=>Array.from(section.querySelectorAll('input:not([type="file"]),select')).filter(x=>x.name&&!['heroImage','heroBackgroundImage','heroBackgroundMobileImage'].includes(x.name));
 const config=()=>Object.fromEntries(fields().map(x=>[x.name,x.value]));
 const snapshot=()=>JSON.stringify(fields().map(x=>[x.name,x.value]));

 function refreshHeroImage(c){
   const box=document.getElementById('heroImagePreview'),img=box?.querySelector('img');
   if(!box)return;
   const r=window.SiteImageControls.resolveMain(c,'auto');
   set(box,{height:r.height,'min-height':'0','max-height':'none','aspect-ratio':'auto',flex:'0 0 auto'});
   if(img)set(img,{'object-fit':r.fit,'object-position':r.position,width:'100%',height:'100%','min-height':'0'});
 }

 function refreshBackground(c){
   const box=document.getElementById('heroBackgroundLivePreview'),layer=box?.querySelector('.admin-hero-bg-live-layer');
   if(!box||!layer)return;
   const scope=box.dataset.device==='mobile'?'mobile':'desktop';
   const url=scope==='mobile'?(value('heroBackgroundMobileImage')||value('heroBackgroundImage')):value('heroBackgroundImage');
   if(!url){
     box.classList.add('hidden');
     layer.style.backgroundImage='';
     return;
   }
   const r=window.SiteImageControls.resolveBackground(c,scope);
   box.classList.remove('hidden');
   box.classList.add('image-controlled-background');
   box.classList.toggle('has-background',true);
   box.style.setProperty('--image-text-dim',String(r.dim));
   if(scope==='mobile')box.style.setProperty('--hero-mobile-background-height',r.height);
   else box.style.removeProperty('--hero-mobile-background-height');
   set(layer,{
     'background-image':`url("${String(url).replace(/"/g,'%22')}")`,
     'background-size':r.backgroundSize,
     'background-position':r.backgroundPosition,
     'background-repeat':'no-repeat',
     opacity:r.opacity,
     filter:r.filter,
     inset:'0',
     transform:'none',
     transition:'none'
   });
 }

 function refresh(){
   section.querySelectorAll('input[type="range"]').forEach(input=>{
     let out=input.parentElement.querySelector('output');
     if(!out){out=document.createElement('output');input.parentElement.append(out);} out.value=input.value+'%';
   });
   const c=config();
   refreshHeroImage(c);
   refreshBackground(c);
   const dirty=saved!==null&&snapshot()!==saved;
   if(state){state.textContent=dirty?'参数有未保存修改':'参数已同步';state.dataset.dirty=String(dirty);}
 }

 section.addEventListener('input',refresh);section.addEventListener('change',refresh);
 section.querySelectorAll('[data-visual-device]').forEach(button=>button.addEventListener('click',()=>{
   const preview=document.getElementById('heroBackgroundLivePreview');
   if(preview)preview.dataset.device=button.dataset.visualDevice;
   section.querySelectorAll('[data-visual-device]').forEach(b=>b.setAttribute('aria-pressed',String(b===button)));
   refreshBackground(config());
   document.dispatchEvent(new CustomEvent('visual-device-changed'));
 }));
 document.addEventListener('site-config-loaded',()=>{saved=snapshot();refresh();});
 document.addEventListener('site-image-updated',refresh);
 window.addEventListener('beforeunload',event=>{if(saved!==null&&saved!==snapshot()){event.preventDefault();event.returnValue='';}});
 refresh();
})();
