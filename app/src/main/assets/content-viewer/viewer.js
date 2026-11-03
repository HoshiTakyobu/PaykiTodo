/* Local-only display projection. Markdown never becomes executable host code. */
(() => {
  'use strict';
  const escape = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const renderer = new marked.Renderer();
  renderer.html = token => escape(token.text);
  renderer.image = token => escape(`[图片：${token.text || '未自动加载'}]`);
  function render(payload) {
    if (!payload || typeof payload.content !== 'string') return;
    document.documentElement.classList.toggle('dark', payload.dark === true);
    document.documentElement.style.fontSize = `${Math.min(36, Math.max(12, Number(payload.fontSize) || 17))}px`;
    document.body.style.fontFamily = ['sans-serif','serif','monospace'].includes(payload.fontFamily) ? payload.fontFamily : payload.fontFamily ? `${JSON.stringify(payload.fontFamily)}, sans-serif` : 'sans-serif';
    document.title = payload.title || 'PaykiTodo 阅读器';
    const content = document.getElementById('content');
    const html = marked.parse(payload.content || '（空报告）', {gfm:true, async:false, renderer});
    content.innerHTML = DOMPurify.sanitize(html, {
      ALLOWED_TAGS:['p','br','h1','h2','h3','h4','h5','h6','strong','em','del','hr','blockquote','ul','ol','li','pre','code','a','table','thead','tbody','tr','th','td','input'],
      ALLOWED_ATTR:['href','title','start','type','checked','disabled','align','class'],
      ALLOWED_URI_REGEXP:/^(?:https?:\/\/|#)/i
    });
    content.querySelectorAll('input').forEach(input => { input.type='checkbox'; input.disabled=true; });
    content.querySelectorAll('a').forEach(link => {
      if (!/^(https?:\/\/|#)/i.test(link.getAttribute('href') || '')) link.removeAttribute('href');
      link.setAttribute('rel','noreferrer noopener');
    });
    content.querySelectorAll('table').forEach(table => { const box=document.createElement('div'); box.className='table-scroll'; table.before(box); box.append(table); });
    const toc=document.getElementById('toc'); toc.classList.toggle('enabled', payload.help === true);
    const nav=toc.querySelector('nav'); nav.replaceChildren();
    if (payload.help) content.querySelectorAll('h2,h3').forEach((heading,i) => {
      heading.id=`chapter-${i}`; const link=document.createElement('a'); link.href=`#${heading.id}`; link.textContent=heading.textContent;
      link.addEventListener('click',()=> { toc.open=false; }); nav.append(link);
    });
    window.scrollTo(0,0);
  }
  window.addEventListener('message', event => {
    // Android postWebMessage targets this local origin, but its native sender has no Window/origin.
    if (event.origin !== 'https://payki.content' && !(event.origin === '' && event.source === null)) return;
    try { render(typeof event.data === 'string' ? JSON.parse(event.data) : event.data); } catch { document.getElementById('content').textContent='无法排版，请返回后重试或复制原文。'; }
  });
  if (window.chrome?.webview) {
    window.chrome.webview.addEventListener('message', event => render(event.data));
    window.chrome.webview.postMessage({type:'ready'});
  }
})();
