(function(){
 const form=document.getElementById('configForm'), section=document.querySelector('[data-config-section="visual"]');
 if(!form||!section)return;
 const state=document.getElementById('visualSaveState');
 section.querySelectorAll('.field').forEach(field=>{const input=field.querySelector('input:not([type="hidden"]),select'),label=field.querySelector('label');if(input&&label){input.id=input.id||'visual-'+input.name;label.htmlFor=input.id;}});
 let saved=null;
 const fields=()=>Array.from(section.querySelectorAll('input:not([type="file"]),select')).filter(x=>x.name&&!['heroImage','heroBackgroundImage'].includes(x.name));
 const snapshot=()=>JSON.stringify(fields().map(x=>[x.name,x.value]));
 function refresh(){
   section.querySelectorAll('input[type="range"]').forEach(input=>{
     let out=input.parentElement.querySelector('output');
     if(!out){out=document.createElement('output');input.parentElement.append(out);} out.value=input.value+'%';
   });
   const box=document.getElementById('heroImagePreview');
   const c=Object.fromEntries(fields().map(x=>[x.name,x.value]));
   window.SiteImageControls.main(box,box?.querySelector('img'),c);
   const dirty=saved!==null&&snapshot()!==saved;
   state.textContent=dirty?'参数有未保存修改':'参数已同步';state.dataset.dirty=String(dirty);
 }
 section.addEventListener('input',refresh);section.addEventListener('change',refresh);
 section.querySelectorAll('[data-visual-device]').forEach(button=>button.addEventListener('click',()=>{
   document.getElementById('heroBackgroundLivePreview').dataset.device=button.dataset.visualDevice;
   section.querySelectorAll('[data-visual-device]').forEach(b=>b.setAttribute('aria-pressed',String(b===button)));
 }));
 document.addEventListener('site-config-loaded',()=>{saved=snapshot();refresh();});
 document.addEventListener('site-image-updated',refresh);
 window.addEventListener('beforeunload',event=>{if(saved!==null&&saved!==snapshot()){event.preventDefault();event.returnValue='';}});
 refresh();
})();
