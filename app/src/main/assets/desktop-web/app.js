const HOUR_HEIGHT = 64;
const EVENT_HEADER_HEIGHT = 58;
const FIFTEEN_MINUTES = 15 * 60 * 1000;
const THIRTY_MINUTES = 30 * 60 * 1000;
const DEFAULT_EVENT_COLOR = '#4e87e1';
const state = { token: '', snapshot: null, currentTab: 'todos', selectedEventDay: dayKey(new Date()), editingTodoId: null, editingEventId: null, pendingEventSeed: null };

const els = {
  token: document.getElementById('token'),
  status: document.getElementById('status'),
  todoSummary: document.getElementById('todo-summary'),
  todoTimeline: document.getElementById('todo-timeline'),
  eventSelectedDate: document.getElementById('event-selected-date'),
  eventSelectedSubtitle: document.getElementById('event-selected-subtitle'),
  hourAxis: document.getElementById('hour-axis'),
  eventDayHeaders: document.getElementById('event-day-headers'),
  eventTimeline: document.getElementById('event-timeline'),
  snapshotMeta: document.getElementById('snapshot-meta'),
  panelTitle: document.getElementById('panel-title'),
  viewCaption: document.getElementById('view-caption'),
  openCreate: document.getElementById('open-create'),
  boardScroll: document.getElementById('board-scroll'),
  eventAnchorDate: document.getElementById('event-anchor-date'),
  eventPrevDay: document.getElementById('event-prev-day'),
  eventNextDay: document.getElementById('event-next-day'),
  applyEventDay: document.getElementById('apply-event-day')
};

function headers() {
  return {
    'Content-Type': 'application/json',
    'X-Payki-Token': state.token
  };
}

async function api(path, options = {}) {
  const res = await fetch(path, { ...options, headers: { ...headers(), ...(options.headers || {}) } });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || '请求失败');
  return data;
}

function dayKey(date) {
  return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
}

function dayKeyFromMillis(millis) {
  return dayKey(new Date(millis || 0));
}

function dateFromKey(key) {
  const parts = String(key).split('-').map(Number);
  return new Date(parts[0], parts[1] - 1, parts[2]);
}

function dayStartMillis(key) {
  return dateFromKey(key).getTime();
}

function addDays(key, delta) {
  const date = dateFromKey(key);
  date.setDate(date.getDate() + delta);
  return dayKey(date);
}

function formatWeekday(date) {
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()];
}

function formatTimeLabel(millis) {
  const date = new Date(millis || 0);
  return String(date.getHours()).padStart(2, '0') + ':' + String(date.getMinutes()).padStart(2, '0');
}

function formatDateTimeLabel(millis) {
  if (!millis) return '—';
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false
  }).format(new Date(millis));
}

function formatShortDateLabel(millis) {
  const date = new Date(millis || 0);
  return (date.getMonth() + 1) + '月' + date.getDate() + '日 ' + formatWeekday(date);
}

function formatFullDateLabel(date) {
  return date.getFullYear() + '年' + (date.getMonth() + 1) + '月' + date.getDate() + '日 ' + formatWeekday(date);
}

function formatCompactDateLabel(key) {
  const date = dateFromKey(key);
  return (date.getMonth() + 1) + '月' + date.getDate() + '日';
}

function groupOptionsHtml(selectedId) {
  const groups = state.snapshot?.groups || [];
  return groups.map(group => '<option value="' + group.id + '"' + (Number(selectedId) === Number(group.id) ? ' selected' : '') + '>' + escapeHtml(group.name) + '</option>').join('');
}

function fillGroupSelect(selectId, selectedId) {
  const node = document.getElementById(selectId);
  if (!node) return;
  node.innerHTML = groupOptionsHtml(selectedId);
  if (!node.value && node.options.length) node.value = node.options[0].value;
}

function toDateTimeLocalParts(value) {
  if (!value) return null;
  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/);
  if (!match) return null;
  return { year: match[1], month: match[2], day: match[3], hour: match[4], minute: match[5] };
}

function readDateTimeValue(prefix) {
  const year = document.getElementById(prefix + '-year')?.value.trim() || '';
  const month = document.getElementById(prefix + '-month')?.value.trim() || '';
  const day = document.getElementById(prefix + '-day')?.value.trim() || '';
  const hour = document.getElementById(prefix + '-hour')?.value.trim() || '';
  const minute = document.getElementById(prefix + '-minute')?.value.trim() || '';
  if (!year && !month && !day && !hour && !minute) return null;
  if (year.length !== 4 || month.length < 1 || day.length < 1 || hour.length < 1 || minute.length < 1) {
    throw new Error('请完整填写 ' + prefix + ' 对应的日期和时间');
  }
  const mm = month.padStart(2, '0');
  const dd = day.padStart(2, '0');
  const hh = hour.padStart(2, '0');
  const min = minute.padStart(2, '0');
  return year + '-' + mm + '-' + dd + 'T' + hh + ':' + min;
}

function writeDateTimeValue(prefix, value) {
  const parts = toDateTimeLocalParts(value);
  ['year', 'month', 'day', 'hour', 'minute'].forEach(key => {
    const node = document.getElementById(prefix + '-' + key);
    if (node) node.value = parts ? parts[key] : '';
  });
}

function setEventSeed(startMillis, endMillis) {
  state.pendingEventSeed = { startMillis, endMillis };
  writeDateTimeValue('event-start', formatDateTimeLocalValue(startMillis));
  writeDateTimeValue('event-end', formatDateTimeLocalValue(endMillis));
}

function snapToQuarterHour(millis) {
  return Math.round(millis / FIFTEEN_MINUTES) * FIFTEEN_MINUTES;
}

function activeTodos() {
  return (state.snapshot?.todos || []).filter(item => !(item.completed || item.canceled));
}

function historyTodos() {
  return (state.snapshot?.todos || []).filter(item => item.completed || item.canceled);
}

function activeEvents() {
  return (state.snapshot?.events || []).filter(item => !(item.completed || item.canceled));
}

function eventStart(item) {
  return item.startAtMillis || item.dueAtMillis || 0;
}

function eventEnd(item) {
  return item.endAtMillis || (eventStart(item) + 30 * 60 * 1000);
}

function reminderModeLabel(mode) {
  if (mode === 'NOTIFICATION') return '通知栏提醒';
  if (mode === 'FULLSCREEN') return '全屏提醒';
  return mode || '默认提醒';
}

function reminderText(item) {
  const offsets = item.reminderOffsetsMinutes || [];
  if (!item.reminderEnabled) return '未开启提醒';
  if (offsets.length) return '提醒：' + offsets.join(' / ') + ' 分钟前 · ' + reminderModeLabel(item.reminderDeliveryMode);
  if (item.reminderAtMillis) return '提醒：' + formatDateTimeLabel(item.reminderAtMillis) + ' · ' + reminderModeLabel(item.reminderDeliveryMode);
  return '提醒已开启 · ' + reminderModeLabel(item.reminderDeliveryMode);
}

function todoStateLabel(item) {
  if (item.completed) return '已完成';
  if (item.canceled) return '已取消';
  if (item.missed) return '已错过';
  return '待处理';
}

function todoMarker(item) {
  if (item.completed) return { main: '已完成', sub: formatDateTimeLabel(item.completedAtMillis || item.dueAtMillis) };
  if (item.canceled) return { main: '已取消', sub: formatDateTimeLabel(item.canceledAtMillis || item.dueAtMillis) };
  if (!item.hasDueDate) return { main: '无 DDL', sub: item.reminderEnabled ? '保留提醒' : '无提醒' };
  return { main: formatTimeLabel(item.dueAtMillis), sub: formatShortDateLabel(item.dueAtMillis) };
}

function renderSummaryCard(label, value) {
  return '<div class="summary-card"><div class="summary-label">' + escapeHtml(label) + '</div><div class="summary-value">' + value + '</div></div>';
}

function renderTodoItem(item) {
  const accent = item.groupColorHex || item.accentColorHex || '#4e87e1';
  const marker = todoMarker(item);
  const meta = [item.groupName || '未分组', item.location || '', item.isRecurring ? '循环' : ''].filter(Boolean).join(' · ');
  const actionHtml = (item.completed || item.canceled)
    ? '<button data-action="delete" data-id="' + item.id + '" class="secondary">删除</button>'
    : '<button data-action="edit-todo" data-id="' + item.id + '" class="secondary">编辑</button><button data-action="complete" data-id="' + item.id + '" class="success">完成</button><button data-action="cancel" data-id="' + item.id + '" class="danger">取消</button>';
  return ''
    + '<article class="timeline-item" style="--accent:' + accent + '">'
    +   '<div class="timeline-marker">'
    +     '<div class="timeline-time">' + escapeHtml(marker.main) + '</div>'
    +     '<div class="timeline-subtime">' + escapeHtml(marker.sub) + '</div>'
    +   '</div>'
    +   '<div class="timeline-card">'
    +     '<div class="timeline-card-title-row">'
    +       '<div class="timeline-card-title">' + escapeHtml(item.title) + '</div>'
    +       '<div class="pill">' + escapeHtml(todoStateLabel(item)) + '</div>'
    +     '</div>'
    +     '<div class="timeline-card-meta">' + escapeHtml(meta || '无附加信息') + '</div>'
    +     (item.notes ? '<div class="timeline-card-notes">' + escapeHtml(item.notes) + '</div>' : '')
    +     '<div class="timeline-card-meta">' + escapeHtml(reminderText(item)) + '</div>'
    +     '<div class="actions">' + actionHtml + '</div>'
    +   '</div>'
    + '</article>';
}

function renderTodoSection(section) {
  return ''
    + '<section id="todo-section-' + section.key + '" class="timeline-section">'
    +   '<div class="timeline-section-header">'
    +     '<div class="timeline-section-title">' + escapeHtml(section.title) + '</div>'
    +     '<div class="timeline-section-count">' + section.items.length + ' 项</div>'
    +   '</div>'
    +   '<div class="timeline-list">'
    +     (section.items.length ? section.items.map(renderTodoItem).join('') : '<div class="empty-state">' + escapeHtml(section.empty) + '</div>')
    +   '</div>'
    + '</section>';
}

function buildEventSegment(item, key) {
  const start = Math.max(eventStart(item), dayStartMillis(key));
  const end = Math.min(eventEnd(item), dayStartMillis(key) + 24 * 60 * 60 * 1000);
  const minuteStart = (start - dayStartMillis(key)) / 60000;
  const minuteEnd = (end - dayStartMillis(key)) / 60000;
  return {
    item: item,
    top: minuteStart / 60 * HOUR_HEIGHT,
    height: Math.max(30, minuteEnd - minuteStart || 30) / 60 * HOUR_HEIGHT,
    startLabel: formatTimeLabel(start),
    endLabel: formatTimeLabel(end)
  };
}

function renderHourAxis() {
  let html = '<div class="hour-axis-spacer"></div>';
  for (let hour = 0; hour < 24; hour += 1) {
    html += '<div class="hour-label">' + String(hour).padStart(2, '0') + ':00</div>';
  }
  const now = new Date();
  const top = EVENT_HEADER_HEIGHT + ((now.getHours() * 60 + now.getMinutes()) / 60 * HOUR_HEIGHT);
  html += '<div class="hour-current-chip" style="top:' + top + 'px">' + escapeHtml(formatTimeLabel(now.getTime())) + '</div>';
  return html;
}

function renderHourGrid() {
  let html = '';
  for (let hour = 0; hour < 24; hour += 1) {
    html += '<div class="hour-row" style="top:' + (hour * HOUR_HEIGHT) + 'px"></div>';
    html += '<div class="half-row" style="top:' + (hour * HOUR_HEIGHT + HOUR_HEIGHT / 2) + 'px"></div>';
  }
  return html;
}

function renderCurrentLine(key) {
  const now = new Date();
  const todayKey = dayKey(now);
  const top = (now.getHours() * 60 + now.getMinutes()) / 60 * HOUR_HEIGHT;
  return '<div class="current-line ' + (key < todayKey ? 'past' : '') + '" style="top:' + top + 'px"></div>';
}

function visibleEventDayCount() {
  const width = Math.max(0, els.boardScroll?.clientWidth || 0);
  if (!width) return 7;
  return Math.max(1, Math.min(10, Math.max(7, Math.floor(width / 128))));
}

function getVisibleEventKeys() {
  const count = visibleEventDayCount();
  return Array.from({ length: count }, (_, index) => addDays(state.selectedEventDay, index));
}

function renderEventDayHeader(key, items) {
  const classes = ['event-day-header'];
  if (key === dayKey(new Date())) classes.push('today');
  if (key === state.selectedEventDay) classes.push('selected');
  return ''
    + '<button class="' + classes.join(' ') + '" data-day="' + key + '">'
    +   '<div class="event-day-header-date">' + escapeHtml(formatCompactDateLabel(key) + ' ' + formatWeekday(dateFromKey(key))) + '</div>'
    +   '<div class="event-day-header-meta">' + items.length + ' 项</div>'
    + '</button>';
}

function renderEventDayAllDayStrip(key, allDayItems) {
  if (!allDayItems.length) return '';
  return '<div class="event-all-day-strip">' + allDayItems.map(item => renderAllDayEventPill(item, key)).join('') + '</div>';
}

function renderAllDayEventPill(item, key) {
  const accent = item.groupColorHex || item.accentColorHex || '#4e87e1';
  const startKey = dayKeyFromMillis(eventStart(item));
  const endKey = dayKeyFromMillis(Math.max(eventStart(item), eventEnd(item) - 1));
  const range = startKey === endKey ? '全天' : formatCompactDateLabel(startKey) + ' - ' + formatCompactDateLabel(endKey);
  return ''
    + '<button type="button" class="event-all-day-pill" data-event-id="' + escapeHtml(String(item.id ?? '')) + '" style="--accent:' + accent + '">'
    +   '<span class="event-all-day-dot"></span>'
    +   '<span class="event-all-day-title">' + escapeHtml(item.title) + '</span>'
    +   '<span class="event-all-day-range">' + escapeHtml(range) + '</span>'
    + '</button>';
}

function renderEventDayColumn(key, timed, allDayItems) {
  return ''
    + '<div class="event-day-column" data-column-day="' + key + '">'
    +   renderEventDayAllDayStrip(key, allDayItems)
    +   renderHourGrid()
    +   renderCurrentLine(key)
    +   timed.map(renderEventCard).join('')
    + '</div>';
}

function renderVisibleRangeTitle(keys) {
  if (!keys.length) return formatFullDateLabel(dateFromKey(state.selectedEventDay));
  if (keys.length === 1) return formatFullDateLabel(dateFromKey(keys[0]));
  return formatCompactDateLabel(keys[0]) + ' - ' + formatCompactDateLabel(keys[keys.length - 1]);
}

function renderEventCard(segment) {
  const item = segment.item;
  const accent = item.groupColorHex || item.accentColorHex || '#4e87e1';
  const meta = [item.groupName || '未分组', item.location || '', item.isRecurring ? '循环' : ''].filter(Boolean).join(' · ');
  return ''
    + '<article class="event-card" data-event-id="' + escapeHtml(String(item.id ?? '')) + '" style="--accent:' + accent + ';top:' + segment.top + 'px;height:' + segment.height + 'px">'
    +   '<div class="event-card-title">' + escapeHtml(item.title) + '</div>'
    +   '<div class="event-card-meta">' + escapeHtml(segment.startLabel) + ' - ' + escapeHtml(segment.endLabel) + '</div>'
    +   '<div class="event-card-meta">' + escapeHtml(meta || '定时日程') + '</div>'
    +   (item.notes ? '<div class="event-card-notes">' + escapeHtml(item.notes) + '</div>' : '')
    + '</article>';
}

function syncTopbar() {
  const todoMode = state.currentTab === 'todos';
  els.panelTitle.textContent = todoMode ? '待办时间轴' : '日程时间轴';
  els.viewCaption.textContent = todoMode ? '桌面端待办模式' : '桌面端日程模式';
  els.openCreate.textContent = todoMode ? '新增待办' : '新增日程';
  els.snapshotMeta.textContent = state.snapshot ? ('最近刷新：' + formatDateTimeLabel(state.snapshot.generatedAtMillis)) : '连接后即可读取手机端当前数据';
}

function ensureSelectedEventDay() {
  if (!state.selectedEventDay) state.selectedEventDay = dayKey(new Date());
}

async function connect() {
  state.token = els.token.value.trim();
  await loadSnapshot();
  els.status.textContent = '已连接';
}

async function loadSnapshot() {
  state.snapshot = await api('/api/snapshot');
  ensureSelectedEventDay();
  renderTodos();
  renderEvents();
  syncTopbar();
}

function renderTodos() {
  const now = Date.now();
  const today = dayKey(new Date());
  const active = activeTodos().slice().sort((a, b) => (a.hasDueDate ? (a.dueAtMillis || Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER) - (b.hasDueDate ? (b.dueAtMillis || Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER));
  const history = historyTodos().slice().sort((a, b) => (b.completedAtMillis || b.canceledAtMillis || b.missedAtMillis || b.dueAtMillis || 0) - (a.completedAtMillis || a.canceledAtMillis || a.missedAtMillis || a.dueAtMillis || 0));
  const sections = [
    { key: 'missed', title: '已错过', empty: '当前没有已错过的待办。', items: active.filter(item => item.missed || (item.hasDueDate && (item.dueAtMillis || 0) < now && dayKeyFromMillis(item.dueAtMillis) < today)) },
    { key: 'today', title: '今日待办', empty: '今天暂时没有待办。', items: active.filter(item => item.hasDueDate && !item.missed && dayKeyFromMillis(item.dueAtMillis) === today) },
    { key: 'upcoming', title: '计划中', empty: '后续暂时没有排期。', items: active.filter(item => item.hasDueDate && !item.missed && dayKeyFromMillis(item.dueAtMillis) > today) },
    { key: 'floating', title: '未设置 DDL', empty: '所有待办目前都带有 DDL。', items: active.filter(item => !item.hasDueDate) },
    { key: 'history', title: '历史记录', empty: '暂无历史记录。', items: history }
  ];
  els.todoSummary.innerHTML = [
    renderSummaryCard('活动待办', active.length),
    renderSummaryCard('已错过', sections[0].items.length),
    renderSummaryCard('今日待办', sections[1].items.length),
    renderSummaryCard('历史记录', history.length)
  ].join('');
  els.todoTimeline.innerHTML = sections.map(renderTodoSection).join('');
  bindActions();
}

function renderEvents() {
  const events = activeEvents().slice().sort((a, b) => eventStart(a) - eventStart(b));
  const dayMap = new Map();
  events.forEach(item => {
    let cursor = new Date(eventStart(item));
    cursor.setHours(0, 0, 0, 0);
    const last = new Date(Math.max(eventStart(item), eventEnd(item) - 1));
    last.setHours(0, 0, 0, 0);
    while (cursor.getTime() <= last.getTime()) {
      const key = dayKey(cursor);
      const list = dayMap.get(key) || [];
      list.push(item);
      dayMap.set(key, list);
      cursor.setDate(cursor.getDate() + 1);
    }
  });
  const visibleKeys = getVisibleEventKeys();
  const visibleDays = visibleKeys.map(key => {
    const items = (dayMap.get(key) || []).slice().sort((a, b) => eventStart(a) - eventStart(b));
    return {
      key: key,
      items: items,
      timed: items.filter(item => !item.allDay).map(item => buildEventSegment(item, key)),
      allDay: items.filter(item => item.allDay)
    };
  });
  const totalTimed = visibleDays.reduce((sum, day) => sum + day.timed.length, 0);
  const totalAllDay = visibleDays.reduce((sum, day) => sum + day.allDay.length, 0);
  els.eventDayHeaders.style.setProperty('--day-count', String(visibleKeys.length || 1));
  els.eventTimeline.style.setProperty('--day-count', String(visibleKeys.length || 1));
  els.eventDayHeaders.innerHTML = visibleDays.map(day => renderEventDayHeader(day.key, day.items)).join('');
  if (els.eventAnchorDate) els.eventAnchorDate.value = state.selectedEventDay;
  els.eventSelectedDate.textContent = renderVisibleRangeTitle(visibleKeys);
  els.eventSelectedSubtitle.textContent = '起始日：' + formatCompactDateLabel(state.selectedEventDay) + ' · 连续 ' + visibleKeys.length + ' 天 · 定时 ' + totalTimed + ' 项 · 全天 ' + totalAllDay + ' 项';
  els.hourAxis.innerHTML = renderHourAxis();
  els.eventTimeline.innerHTML = visibleDays.map(day => renderEventDayColumn(day.key, day.timed, day.allDay)).join('');
  if (els.boardScroll) {
    if (visibleKeys.includes(dayKey(new Date()))) {
      const nowDate = new Date();
      els.boardScroll.scrollTop = Math.max(0, EVENT_HEADER_HEIGHT + ((nowDate.getHours() * 60 + nowDate.getMinutes()) / 60 * HOUR_HEIGHT) - 180);
    } else {
      const firstTimed = visibleDays.flatMap(day => day.timed).sort((a, b) => a.top - b.top)[0];
      els.boardScroll.scrollTop = firstTimed ? Math.max(0, EVENT_HEADER_HEIGHT + firstTimed.top - 80) : 0;
    }
  }
  document.querySelectorAll('[data-column-day]').forEach(node => {
    node.onclick = event => {
      if (event.target.closest('[data-event-id]')) return;
      const rect = node.getBoundingClientRect();
      const relativeY = Math.max(0, Math.min(rect.height, event.clientY - rect.top));
      const minutes = Math.round((relativeY / HOUR_HEIGHT) * 4) * 15;
      const baseMillis = dayStartMillis(node.dataset.columnDay) + minutes * 60 * 1000;
      const startMillis = snapToQuarterHour(baseMillis);
      const endMillis = startMillis + THIRTY_MINUTES;
      clearEventForm();
      setEventSeed(startMillis, endMillis);
      openModal('event-modal');
    };
  });
  bindActions();
}

function escapeHtml(text) {
  return String(text || '').replace(/[&<>\"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[ch]));
}

function sameId(left, right) {
  return String(left ?? '') === String(right ?? '');
}

function findEventById(id) {
  return (state.snapshot?.events || []).find(item => sameId(item.id, id));
}

function parseIntList(text) {
  return String(text || '')
    .split(',')
    .map(v => Number(v.trim()))
    .filter(v => Number.isFinite(v) && v >= 0);
}

function parseWeekdays(text) {
  return String(text || '')
    .split(',')
    .map(v => Number(v.trim()))
    .filter(v => Number.isInteger(v) && v >= 1 && v <= 7);
}

function recurrencePayload(typeValue, endValue, weekdaysValue) {
  if (!typeValue || typeValue === 'NONE') {
    return { enabled: false, type: 'NONE', weeklyDays: [], endDate: null };
  }
  return {
    enabled: true,
    type: typeValue,
    weeklyDays: parseWeekdays(weekdaysValue),
    endDate: endValue || null
  };
}

function setTodoDueEnabled(enabled) {
  const checkbox = document.getElementById('todo-has-due');
  if (checkbox) checkbox.checked = enabled;
  document.querySelectorAll('.todo-due-dependent').forEach(node => {
    node.classList.toggle('disabled-block', !enabled);
    node.querySelectorAll('input, select, textarea, button').forEach(input => {
      input.disabled = !enabled;
    });
  });
  if (!enabled) {
    writeDateTimeValue('todo-due', '');
    writeDateTimeValue('todo-reminder', '');
    document.getElementById('todo-recurrence-type').value = 'NONE';
    document.getElementById('todo-recurrence-end').value = '';
    document.getElementById('todo-weekdays').value = '';
  }
}
function clearTodoForm() {
  state.editingTodoId = null;
  fillGroupSelect('todo-group');
  document.getElementById('todo-modal-title').textContent = '新增待办';
  document.getElementById('create-todo').textContent = '创建待办';
  document.getElementById('delete-todo').classList.add('hidden');
  document.getElementById('todo-title').value = '';
  document.getElementById('todo-notes').value = '';
  writeDateTimeValue('todo-due', '');
  writeDateTimeValue('todo-reminder', '');
  document.getElementById('todo-recurrence-type').value = 'NONE';
  document.getElementById('todo-recurrence-end').value = '';
  document.getElementById('todo-weekdays').value = '';
  document.getElementById('todo-ring').checked = true;
  document.getElementById('todo-vibrate').checked = true;
  const hasDueCheckbox = document.getElementById('todo-has-due');
  if (hasDueCheckbox) hasDueCheckbox.disabled = false;
  setTodoDueEnabled(true);
}

function openTodoEditor(item) {
  state.editingTodoId = item.id;
  fillGroupSelect('todo-group', item.groupId);
  document.getElementById('todo-modal-title').textContent = '编辑待办';
  document.getElementById('create-todo').textContent = '保存修改';
  document.getElementById('delete-todo').classList.remove('hidden');
  document.getElementById('todo-title').value = item.title || '';
  document.getElementById('todo-notes').value = item.notes || '';
  const hasDueCheckbox = document.getElementById('todo-has-due');
  if (hasDueCheckbox) hasDueCheckbox.disabled = item.isRecurring === true;
  setTodoDueEnabled(item.hasDueDate !== false);
  writeDateTimeValue('todo-due', item.hasDueDate ? formatDateTimeLocalValue(item.dueAtMillis) : '');
  const reminderOffsets = item.reminderOffsetsMinutes || [];
  const reminderMillis = reminderOffsets.length && item.hasDueDate
    ? item.dueAtMillis - reminderOffsets[0] * 60 * 1000
    : item.reminderAtMillis;
  writeDateTimeValue('todo-reminder', reminderMillis ? formatDateTimeLocalValue(reminderMillis) : '');
  document.getElementById('todo-recurrence-type').value = recurrenceTypeValue(item);
  document.getElementById('todo-recurrence-end').value = item.recurrenceEndDate || '';
  document.getElementById('todo-weekdays').value = csvValue(item.recurrenceWeekdays);
  document.getElementById('todo-ring').checked = item.ringEnabled !== false;
  document.getElementById('todo-vibrate').checked = item.vibrateEnabled !== false;
  openModal('todo-modal');
}

function clearEventForm() {
  state.editingEventId = null;
  state.pendingEventSeed = null;
  fillGroupSelect('event-group');
  document.getElementById('event-modal-title').textContent = '新增日程';
  document.getElementById('save-event').textContent = '创建日程';
  document.getElementById('delete-event').classList.add('hidden');
  document.getElementById('event-title').value = '';
  document.getElementById('event-color').value = DEFAULT_EVENT_COLOR;
  document.getElementById('event-location').value = '';
  document.getElementById('event-notes').value = '';
  writeDateTimeValue('event-start', '');
  writeDateTimeValue('event-end', '');
  document.getElementById('event-reminder-mode').value = 'NOTIFICATION';
  document.getElementById('event-reminder-offsets').value = '';
  document.getElementById('event-recurrence-type').value = 'NONE';
  document.getElementById('event-recurrence-end').value = '';
  document.getElementById('event-weekdays').value = '';
  document.getElementById('event-all-day').checked = false;
  document.getElementById('event-ring').checked = true;
  document.getElementById('event-vibrate').checked = true;
}

function formatDateTimeLocalValue(millis) {
  if (!millis) return '';
  const date = new Date(millis);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return year + '-' + month + '-' + day + 'T' + hour + ':' + minute;
}

function recurrenceTypeValue(item) {
  return item.isRecurring ? (item.recurrenceType || 'NONE') : 'NONE';
}


function csvValue(value) {
  if (Array.isArray(value)) return value.join(',');
  return String(value || '');
}
function openEventEditor(item) {
  state.editingEventId = item.id;
  state.pendingEventSeed = null;
  fillGroupSelect('event-group', item.groupId);
  document.getElementById('event-modal-title').textContent = '编辑日程';
  document.getElementById('save-event').textContent = '保存修改';
  document.getElementById('delete-event').classList.remove('hidden');
  document.getElementById('event-title').value = item.title || '';
  document.getElementById('event-color').value = item.accentColorHex || item.groupColorHex || DEFAULT_EVENT_COLOR;
  document.getElementById('event-location').value = item.location || '';
  document.getElementById('event-notes').value = item.notes || '';
  writeDateTimeValue('event-start', formatDateTimeLocalValue(item.startAtMillis));
  writeDateTimeValue('event-end', formatDateTimeLocalValue(item.endAtMillis || item.startAtMillis));
  document.getElementById('event-reminder-mode').value = item.reminderDeliveryMode || 'NOTIFICATION';
  document.getElementById('event-reminder-offsets').value = (item.reminderOffsetsMinutes || []).join(',');
  document.getElementById('event-recurrence-type').value = recurrenceTypeValue(item);
  document.getElementById('event-recurrence-end').value = item.recurrenceEndDate || '';
  document.getElementById('event-weekdays').value = csvValue(item.recurrenceWeekdays);
  document.getElementById('event-all-day').checked = item.allDay === true;
  document.getElementById('event-ring').checked = item.ringEnabled !== false;
  document.getElementById('event-vibrate').checked = item.vibrateEnabled !== false;
  openModal('event-modal');
}

function bindDigitInputs() {
  document.querySelectorAll('.digit-input').forEach(node => {
    node.addEventListener('input', () => {
      const digitsOnly = node.value.replace(/\D+/g, '').slice(0, Number(node.dataset.maxlength || node.maxLength || 4));
      node.value = digitsOnly;
      if (digitsOnly.length >= Number(node.dataset.maxlength || node.maxLength || 4) && node.dataset.next) {
        document.getElementById(node.dataset.next)?.focus();
      }
    });
    node.addEventListener('blur', () => {
      if (node.dataset.pad && node.value) node.value = node.value.padStart(Number(node.dataset.pad), '0');
    });
  });
}

function openModal(id) {
  const node = document.getElementById(id);
  if (!node) return;
  node.classList.remove('hidden');
  const focusTarget = node.querySelector('.primary-editor-field') || node.querySelector('input, textarea, select, button');
  if (focusTarget) focusTarget.focus();
}

function closeModal(id) {
  const node = document.getElementById(id);
  if (!node) return;
  node.classList.add('hidden');
}

function confirmDanger(title, message, confirmLabel = '删除') {
  return new Promise(resolve => {
    const modal = document.getElementById('confirm-modal');
    const titleNode = document.getElementById('confirm-title');
    const messageNode = document.getElementById('confirm-message');
    const cancelButton = document.getElementById('confirm-cancel');
    const okButton = document.getElementById('confirm-ok');
    if (!modal || !cancelButton || !okButton) {
      resolve(false);
      return;
    }
    titleNode.textContent = title;
    messageNode.textContent = message;
    okButton.textContent = confirmLabel;
    const cleanup = result => {
      modal.classList.add('hidden');
      cancelButton.onclick = null;
      okButton.onclick = null;
      modal.onclick = null;
      resolve(result);
    };
    cancelButton.onclick = () => cleanup(false);
    okButton.onclick = () => cleanup(true);
    modal.onclick = event => {
      if (event.target === modal) cleanup(false);
    };
    modal.classList.remove('hidden');
    cancelButton.focus();
  });
}
function bindActions() {
  document.querySelectorAll('[data-action]').forEach(node => {
    node.onclick = async event => {
      event.stopPropagation();
      const id = node.dataset.id;
      if (node.dataset.action === 'edit-todo') {
        const todoItem = (state.snapshot?.todos || []).find(item => Number(item.id) === Number(id));
        if (todoItem) openTodoEditor(todoItem);
        return;
      } else if (node.dataset.action === 'complete') {
        await api(`/api/items/${id}/complete`, { method: 'POST' });
      } else if (node.dataset.action === 'cancel') {
        await api(`/api/items/${id}/cancel`, { method: 'POST' });
      } else {
        if (!await confirmDanger('确认删除项目', '删除后无法恢复。')) return;
        await api(`/api/items/${id}`, { method: 'DELETE' });
      }
      await loadSnapshot();
    };
  });
}

document.getElementById('todo-has-due')?.addEventListener('change', event => setTodoDueEnabled(event.target.checked));
document.getElementById('connect').onclick = () => connect().catch(err => els.status.textContent = err.message);
els.token.addEventListener('keydown', event => {
  if (event.key !== 'Enter') return;
  event.preventDefault();
  connect().catch(err => els.status.textContent = err.message);
});
document.getElementById('refresh').onclick = () => loadSnapshot().catch(err => els.status.textContent = err.message);
window.addEventListener('resize', () => {
  if (state.currentTab === 'events' && state.snapshot) renderEvents();
});
document.getElementById('jump-today').onclick = () => {
  if (state.currentTab === 'events') {
    state.selectedEventDay = dayKey(new Date());
    renderEvents();
  } else {
    document.getElementById('todo-section-today')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};
els.eventPrevDay.onclick = () => {
  state.selectedEventDay = addDays(state.selectedEventDay, -1);
  renderEvents();
};
els.eventNextDay.onclick = () => {
  state.selectedEventDay = addDays(state.selectedEventDay, 1);
  renderEvents();
};
els.applyEventDay.onclick = () => {
  if (!els.eventAnchorDate.value) return;
  state.selectedEventDay = els.eventAnchorDate.value;
  renderEvents();
};
els.openCreate.onclick = () => {
  if (state.currentTab === 'events') {
    clearEventForm();
    const start = snapToQuarterHour(Date.now() + THIRTY_MINUTES);
    setEventSeed(start, start + THIRTY_MINUTES);
  } else {
    clearTodoForm();
  }
  openModal(state.currentTab === 'todos' ? 'todo-modal' : 'event-modal');
};
if (els.eventTimeline) {
  els.eventTimeline.addEventListener('click', event => {
    const eventNode = event.target.closest?.('[data-event-id]');
    if (!eventNode) return;
    event.preventDefault();
    event.stopPropagation();
    const eventItem = findEventById(eventNode.dataset.eventId);
    if (eventItem) openEventEditor(eventItem);
  });
}

document.querySelectorAll('[data-tab]').forEach(node => {
  node.onclick = () => {
    state.currentTab = node.dataset.tab;
    document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById(`${node.dataset.tab}-panel`).classList.add('active');
    node.classList.add('active');
    syncTopbar();
  };
});
document.querySelectorAll('[data-close-modal]').forEach(node => {
  node.onclick = () => closeModal(node.dataset.closeModal);
});
document.querySelectorAll('.modal-backdrop').forEach(node => {
  node.onclick = event => {
    if (event.target === node) node.classList.add('hidden');
  };
});
document.addEventListener('keydown', event => {
  if (event.key === 'Escape') document.querySelectorAll('.modal-backdrop').forEach(node => node.classList.add('hidden'));
});

document.getElementById('create-todo').onclick = async () => {
  const hasDueDate = document.getElementById('todo-has-due')?.checked !== false;
  const dueAt = hasDueDate ? readDateTimeValue('todo-due') : null;
  const reminderAt = hasDueDate ? readDateTimeValue('todo-reminder') : null;
  const payload = {
    title: document.getElementById('todo-title').value,
    notes: document.getElementById('todo-notes').value,
    dueAt: dueAt,
    reminderAt: reminderAt,
    groupId: Number(document.getElementById('todo-group').value || 0),
    ringEnabled: document.getElementById('todo-ring').checked,
    vibrateEnabled: document.getElementById('todo-vibrate').checked,
    recurrence: hasDueDate ? recurrencePayload(
      document.getElementById('todo-recurrence-type').value,
      document.getElementById('todo-recurrence-end').value,
      document.getElementById('todo-weekdays').value
    ) : { enabled: false, type: 'NONE', weeklyDays: [], endDate: null }
  };
  if (state.editingTodoId) {
    await api(`/api/todos/${state.editingTodoId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  } else {
    await api('/api/todos', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }
  clearTodoForm();
  closeModal('todo-modal');
  await loadSnapshot();
};

document.getElementById('delete-todo').onclick = async () => {
  if (!state.editingTodoId) return;
  if (!await confirmDanger('确认删除待办', '删除后无法恢复。')) return;
  await api(`/api/items/${state.editingTodoId}`, { method: 'DELETE' });
  clearTodoForm();
  closeModal('todo-modal');
  await loadSnapshot();
};

document.getElementById('save-event').onclick = async () => {
  const startAt = readDateTimeValue('event-start');
  const endAt = readDateTimeValue('event-end');
  const payload = {
    title: document.getElementById('event-title').value,
    groupId: Number(document.getElementById('event-group').value || 0),
    location: document.getElementById('event-location').value,
    notes: document.getElementById('event-notes').value,
    startAt: startAt,
    endAt: endAt,
    allDay: document.getElementById('event-all-day').checked,
    accentColorHex: document.getElementById('event-color').value || DEFAULT_EVENT_COLOR,
    reminderOffsetsMinutes: parseIntList(document.getElementById('event-reminder-offsets').value),
    ringEnabled: document.getElementById('event-ring').checked,
    vibrateEnabled: document.getElementById('event-vibrate').checked,
    reminderDeliveryMode: document.getElementById('event-reminder-mode').value,
    recurrence: recurrencePayload(
      document.getElementById('event-recurrence-type').value,
      document.getElementById('event-recurrence-end').value,
      document.getElementById('event-weekdays').value
    )
  };
  if (state.editingEventId) {
    await api(`/api/events/${state.editingEventId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  } else {
    await api('/api/events', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }
  clearEventForm();
  closeModal('event-modal');
  await loadSnapshot();
};

document.getElementById('delete-event').onclick = async () => {
  if (!state.editingEventId) return;
  if (!await confirmDanger('确认删除日程', '删除后无法恢复。')) return;
  await api(`/api/items/${state.editingEventId}`, { method: 'DELETE' });
  clearEventForm();
  closeModal('event-modal');
  await loadSnapshot();
};

document.addEventListener('click', event => {
  const dayHeader = event.target.closest?.('[data-day]');
  if (!dayHeader) return;
  state.selectedEventDay = dayHeader.dataset.day;
  renderEvents();
});

bindDigitInputs();
syncTopbar();
