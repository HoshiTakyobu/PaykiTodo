(() => {
  const toc=document.getElementById('toc'), nav=toc.querySelector('nav');
  document.querySelectorAll('h2').forEach((heading,i) => {
    heading.id=`chapter-${i}`; const link=document.createElement('a'); link.href=`#${heading.id}`; link.textContent=heading.textContent;
    link.addEventListener('click',() => { toc.open=false; }); nav.append(link);
  });
  document.querySelectorAll('table').forEach(table => { const box=document.createElement('div'); box.className='table-scroll'; table.before(box); box.append(table); });
  window.addEventListener('message', event => {
    // Native Android messages are constrained to this local page by the host's targetOrigin.
    if (event.origin !== 'https://payki.content' && !(event.origin === '' && event.source === null)) return;
    const payload=typeof event.data==='string'?JSON.parse(event.data):event.data;
    document.documentElement.classList.toggle('dark',payload.dark===true);
    document.documentElement.style.fontSize=`${Math.max(12,Math.min(36,Number(payload.fontSize)||17))}px`;
    document.body.style.fontFamily=['sans-serif','serif','monospace'].includes(payload.fontFamily)?payload.fontFamily:`${JSON.stringify(payload.fontFamily)},sans-serif`;
  });
})();
