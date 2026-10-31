/* Daily Board transient planner. All business validation runs on the phone. */
(() => {
  const byId = id => document.getElementById(id);
  const modal = byId('transient-planning-modal');
  const input = byId('transient-planning-text');
  const confirm = byId('transient-planning-confirm');
  const error = byId('transient-planning-error');
  let epoch = 0, busy = false, importing = false, preview = null;

  function feedback(message) { error.textContent = message || ''; }
  function setBusy(value) {
    busy = value;
    modal.querySelectorAll('input,textarea,select,button').forEach(node => { node.disabled = value; });
    confirm.textContent = value ? '处理中' : preview ? '确认导入' : '识别';
    if (!value && preview?.diagnostics?.length) confirm.disabled = true;
  }
  function inputPage() {
    preview = null; feedback('');
    byId('transient-planning-title').textContent = '输入规划';
    byId('transient-planning-input-page').classList.remove('hidden');
    byId('transient-planning-preview-page').classList.add('hidden');
    byId('transient-planning-preview').replaceChildren();
    setBusy(false);
  }
  function cancel() {
    if (importing) return;
    epoch++; input.value = ''; byId('transient-planning-ai').checked = false;
    inputPage(); closeModal('transient-planning-modal');
  }
  byId('open-transient-planning').onclick = () => {
    epoch++; input.value = ''; inputPage(); openModal('transient-planning-modal'); input.focus();
  };
  byId('transient-planning-cancel').onclick = cancel;
  byId('transient-planning-back').onclick = inputPage;
  byId('transient-planning-example').onclick = () => {
    input.value = '- 准备出行\n  - 买车票, 18:00;\n  - 收拾行李;\n- 高等数学, @教学楼201, 每周[周一 08:00>09:40, 周三 14:00>15:40], (10,0);';
  };
  function outdent() {
    const start = input.selectionStart;
    const begin = start === 0 ? 0 : input.value.lastIndexOf('\n', start - 1) + 1;
    const count = input.value[begin] === '\t' ? 1 : (input.value.slice(begin).match(/^ {1,2}/)?.[0].length || 0);
    if (count) {
      input.value = input.value.slice(0, begin) + input.value.slice(begin + count);
      input.setSelectionRange(Math.max(begin, start - count), Math.max(begin, start - count));
    }
    input.focus();
  }
  byId('transient-planning-outdent').onclick = outdent;
  input.addEventListener('keydown', async event => {
    if (event.key === 'Tab' && event.shiftKey) { event.preventDefault(); outdent(); return; }
    if (event.key !== 'Enter' || event.ctrlKey || event.metaKey || event.altKey || event.isComposing) return;
    event.preventDefault();
    const start = input.selectionStart, end = input.selectionEnd, source = input.value, ownEpoch = epoch;
    const begin = start === 0 ? 0 : source.lastIndexOf('\n', start - 1) + 1;
    try {
      // Reuse the authoritative quote/bracket scanner for keyboard assistance too.
      const result = await api('/api/planning/transient/continuation', { method: 'POST', body: JSON.stringify({ line: source.slice(begin, start) }) });
      if (ownEpoch !== epoch || input.value !== source || input.selectionStart !== start || input.selectionEnd !== end) return;
      input.setRangeText('\n' + result.prefix, start, end, 'end');
    } catch (exception) { if (ownEpoch === epoch) feedback(exception.message); }
  });

  function field(host, label, value, key, multiline = false) {
    const row = document.createElement('label'); row.textContent = label;
    const control = document.createElement(multiline ? 'textarea' : 'input');
    control.value = value || ''; control.dataset.field = key;
    if (multiline) control.rows = 2;
    row.append(control); host.append(row); return control;
  }
  function choice(host, label, value, key, options) {
    const row = document.createElement('label'); row.textContent = label;
    const select = document.createElement('select'); select.dataset.field = key;
    for (const [id, title] of options) {
      const option = document.createElement('option'); option.value = id ?? ''; option.textContent = title; select.append(option);
    }
    select.value = value ?? ''; row.append(select); host.append(row);
  }
  function showPreview(result) {
    preview = result;
    byId('transient-planning-title').textContent = '确认规划';
    byId('transient-planning-input-page').classList.add('hidden');
    byId('transient-planning-preview-page').classList.remove('hidden');
    const host = byId('transient-planning-preview'); host.replaceChildren();
    feedback([result.message, ...(result.diagnostics || []).map(d => `第${d.lineNumber}行，第${d.column}列：${d.message}`)].filter(Boolean).join('\n'));
    for (const candidate of result.candidates || []) {
      const card = document.createElement('section'); card.className = 'transient-planning-candidate'; card.dataset.id = candidate.id;
      const label = document.createElement('label');
      const selected = document.createElement('input'); selected.type = 'checkbox'; selected.checked = true; selected.dataset.selected = '';
      label.append(selected, document.createTextNode(`第${candidate.lineNumber}行 · ${candidate.type === 'EVENT' ? '日程' : '待办'}`)); card.append(label);
      field(card, '标题', candidate.title, 'title');
      field(card, 'DDL / 起止 / 循环时间（清空成为普通待办）', candidate.time, 'time');
      field(card, '地点', candidate.location, 'location');
      field(card, '提醒：空白默认(5,0)，()关闭', candidate.reminders, 'reminders');
      field(card, '范围：自日期至日期 / 截至日期（可省略）', candidate.range, 'range');
      field(card, '备注', candidate.notes, 'notes', true);
      choice(card, '父事项', candidate.parentId, 'parentId', [['', '无父事项'], ...result.candidates.filter(c => c.id !== candidate.id).map(c => [c.id, c.title])]);
      choice(card, '分组', candidate.groupId, 'groupId', [['', '默认分组'], ...(state.snapshot?.groups || []).map(g => [String(g.id), g.name])]);
      choice(card, '日历', candidate.calendarId, 'calendarId', (result.calendars?.length ? result.calendars : [{ id: 'default', name: '常规' }]).map(c => [c.id, c.name]));
      if (candidate.slots?.length) {
        const slots = document.createElement('p'); slots.className = 'muted';
        slots.textContent = '首次时段：' + candidate.slots.map(s => s.start.replace('T', ' ') + ' > ' + s.end.replace('T', ' ')).join('；');
        if (candidate.slots.some(s => s.recurring)) slots.textContent += '。未写截止即持续重复，近期实例滚动生成。月末与闰日采用实际月份末日。';
        card.append(slots);
      }
      host.append(card);
    }
  }
  confirm.onclick = async () => {
    if (busy || !state.token) return;
    const ownEpoch = epoch, generation = connectionGeneration;
    if (!preview && !input.value.trim()) { feedback('请先填写规划内容'); return; }
    const selected = preview ? [...byId('transient-planning-preview').children].filter(card => card.querySelector('[data-selected]')?.checked).map(card => {
      const candidate = { id: card.dataset.id };
      card.querySelectorAll('[data-field]').forEach(control => { candidate[control.dataset.field] = control.value; });
      return candidate;
    }) : null;
    if (selected?.length === 0) { feedback('请选择至少一条事项'); return; }
    importing = !!preview; setBusy(true); feedback('');
    try {
      const result = await api(preview ? '/api/planning/transient/import' : '/api/planning/transient/preview', {
        method: 'POST', body: JSON.stringify(preview ? { sessionId: preview.sessionId, candidates: selected } : { text: input.value, useAi: byId('transient-planning-ai').checked })
      });
      if (ownEpoch !== epoch || generation !== connectionGeneration || modal.classList.contains('hidden')) return;
      if (importing) {
        importing = false; cancel(); els.status.textContent = `已导入${result.imported}条规划事项`;
        await loadSnapshot({ planning: false });
      } else showPreview(result);
    } catch (exception) { if (ownEpoch === epoch) feedback(exception.message); }
    finally { if (ownEpoch === epoch) { importing = false; setBusy(false); } }
  };
  byId('open-legacy-planning').onclick = async () => {
    const ownGeneration = connectionGeneration;
    try {
      const result = await api('/api/planning/archive');
      if (ownGeneration !== connectionGeneration) return;
      const host = byId('legacy-planning-content'); host.replaceChildren();
      const info = document.createElement('p'); info.textContent = '旧原文保留在数据与迁移包中。新规划从每日看板输入。'; host.append(info);
      for (const note of result.notes || []) {
        const details = document.createElement('details'), title = document.createElement('summary'), text = document.createElement('pre');
        title.textContent = note.title; text.textContent = note.contentMarkdown || ''; details.append(title, text); host.append(details);
      }
      if (!result.notes?.length) info.textContent += ' 暂无旧文档。';
      openModal('legacy-planning-modal');
    } catch (exception) { els.status.textContent = exception.message; }
  };
  // Capture before the shared modal handlers so an in-flight confirmation stays visible.
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !modal.classList.contains('hidden')) {
      event.preventDefault(); event.stopImmediatePropagation(); cancel();
    }
  }, true);
  modal.addEventListener('click', event => {
    if (event.target === modal) { event.stopImmediatePropagation(); cancel(); }
  }, true);
  const connectionCancel = () => { importing = false; cancel(); pendingDialogs.add(connectionCancel); };
  pendingDialogs.add(connectionCancel);
})();
