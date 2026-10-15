(()=>{
  'use strict';
  const form=document.getElementById('caseFilters');
  if(!form)return;
  document.querySelectorAll('[data-case-attention-age-link]').forEach(link=>{
    link.addEventListener('click',event=>{
      event.preventDefault();
      const age=link.dataset.caseAttentionAgeLink;
      const url=new URL('/admin/xingwei/after-sales/attention',location.origin);
      url.searchParams.set('age',age);
      for(const key of ['status','category','followup','q']){
        const value=String(form.elements[key]?.value||'').trim();
        if(value)url.searchParams.set(key,value);
      }
      location.assign(url.pathname+url.search);
    });
  });
})();
