const HOUR_HEIGHT = 64;
const EVENT_HEADER_HEIGHT = 58;
const FIFTEEN_MINUTES = 15 * 60 * 1000;
const THIRTY_MINUTES = 30 * 60 * 1000;
const DEFAULT_EVENT_COLOR = '#4e87e1';
const state = {
  token: '',
  snapshot: null,
  currentTab: 'todos',
  selectedEventDay: dayKey(new Date()),
  editingTodoId: null,
  editingEventId: null,
  previewTodoId: null,
  previewEventId: null,
  pendingEventSeed: null,
  planningNotes: [],
  activePlanningNoteId: null,
  planningParseResult: null,
  planningSaveTimer: null,
  planningDirty: false,
  planningSaving: false,
  planningRenderedNoteId: null,
  planningParsing: false,
  planningMappings: [],
  planningLoaded: false,
  todosLoaded: false,
  eventsLoaded: false,
  eventRangeStart: null,
  eventRangeEnd: null,
  eventLoadSerial: 0
};

const els = {
  token: document.getElementById('token'),
  status: document.getElementById('status'),
  desktopDailyBoard: document.getElementById('desktop-daily-board'),
  todoSummary: document.getElementById('todo-summary'),
  todoTimeline: document.getElementById('todo-timeline'),
  eventSelectedDate: document.getElementById('event-selected-date'),
  eventSelectedSubtitle: document.getElementById('event-selected-subtitle'),
  hourAxis: document.getElementById('hour-axis'),
  eventDayHeaders: document.getElementById('event-day-headers'),
  eventTimeline: document.getElementById('event-timeline'),
  snapshotMeta: document.getElementById('snapshot-meta'),
  announcementsBanner: document.getElementById('announcements-banner'),
  panelTitle: document.getElementById('panel-title'),
  viewCaption: document.getElementById('view-caption'),
  openCreate: document.getElementById('open-create'),
  boardScroll: document.getElementById('board-scroll'),
  eventAnchorDate: document.getElementById('event-anchor-date'),
  eventPrevDay: document.getElementById('event-prev-day'),
  eventNextDay: document.getElementById('event-next-day'),
  applyEventDay: document.getElementById('apply-event-day'),
  planningNoteSelect: document.getElementById('planning-note-select'),
  planningEditor: document.getElementById('planning-editor'),
  planningActiveTitle: document.getElementById('planning-active-title'),
  planningPreview: document.getElementById('planning-preview'),
  planningPreviewMeta: document.getElementById('planning-preview-meta')
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

function formatDateTimePreviewFromValue(value) {
  const parts = toDateTimeLocalParts(value);
  if (!parts) return null;
  const date = new Date(Number(parts.year), Number(parts.month) - 1, Number(parts.day), Number(parts.hour), Number(parts.minute));
  if (Number.isNaN(date.getTime())) return null;
  if (date.getFullYear() !== Number(parts.year) || date.getMonth() + 1 !== Number(parts.month) || date.getDate() !== Number(parts.day) || date.getHours() !== Number(parts.hour) || date.getMinutes() !== Number(parts.minute)) return null;
  const dateLabel = date.getFullYear() + '年' + (date.getMonth() + 1) + '月' + date.getDate() + '日';
  const timeLabel = String(date.getHours()).padStart(2, '0') + ':' + String(date.getMinutes()).padStart(2, '0');
  return { main: timeLabel, sub: dateLabel + ' · ' + formatWeekday(date) };
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
  updateDateTimePreview(prefix);
}

function updateDateTimePreview(prefix) {
  const node = document.getElementById(prefix + '-preview');
  if (!node) return;
  let preview = null;
  try {
    preview = formatDateTimePreviewFromValue(readDateTimeValue(prefix));
  } catch (_) {
    preview = null;
  }
  if (!preview) {
    node.classList.add('is-empty');
    node.textContent = '完整填写日期时间后显示预览';
    return;
  }
  node.classList.remove('is-empty');
  node.innerHTML = '<strong>' + escapeHtml(preview.main) + '</strong>' + escapeHtml(preview.sub);
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

function findTodoById(id) {
  return (state.snapshot?.todos || []).find(item => sameId(item.id, id));
}

function recurrenceLabel(item) {
  if (!item.isRecurring || !item.recurrenceType || item.recurrenceType === 'NONE') return '不重复';
  const labels = {
    DAILY: '每天重复',
    WEEKLY: '每周重复',
    MONTHLY_NTH_WEEKDAY: '每月第几个星期几',
    MONTHLY_DAY: '每月同日',
    YEARLY_DATE: '每年同日',
    YEARLY_LUNAR_DATE: '每年同农历月日'
  };
  const end = item.recurrenceEndDate ? '，到 ' + item.recurrenceEndDate : '';
  return (labels[item.recurrenceType] || item.recurrenceType) + end;
}

function previewRow(icon, label, value) {
  if (!value) return '';
  return ''
    + '<div class="preview-row">'
    +   '<div class="preview-row-icon">' + escapeHtml(icon) + '</div>'
    +   '<div class="preview-row-content">'
    +     '<div class="preview-row-label">' + escapeHtml(label) + '</div>'
    +     '<div class="preview-row-value">' + escapeHtml(value) + '</div>'
    +   '</div>'
    + '</div>';
}

function todoDueText(item) {
  return item.hasDueDate ? formatDateTimeLabel(item.dueAtMillis) : '不设置 DDL';
}

function eventTimeText(item) {
  if (item.allDay) {
    const startKey = dayKeyFromMillis(eventStart(item));
    const endKey = dayKeyFromMillis(Math.max(eventStart(item), eventEnd(item) - 1));
    return startKey === endKey ? formatCompactDateLabel(startKey) + ' 全天' : formatCompactDateLabel(startKey) + ' - ' + formatCompactDateLabel(endKey) + ' 全天';
  }
  const start = eventStart(item);
  const end = eventEnd(item);
  const startKey = dayKeyFromMillis(start);
  const endKey = dayKeyFromMillis(end);
  if (startKey === endKey) return formatCompactDateLabel(startKey) + ' ' + formatTimeLabel(start) + ' - ' + formatTimeLabel(end);
  return formatDateTimeLabel(start) + ' - ' + formatDateTimeLabel(end);
}

function renderSummaryCard(label, value) {
  return '<div class="summary-card"><div class="summary-label">' + escapeHtml(label) + '</div><div class="summary-value">' + value + '</div></div>';
}

function renderTodoItem(item) {
  const accent = item.groupColorHex || item.accentColorHex || '#4e87e1';
  const marker = todoMarker(item);
  const meta = [item.groupName || '未分组', item.location || '', item.isRecurring ? '循环' : ''].filter(Boolean).join(' · ');
  return ''
    + '<article class="timeline-item" style="--accent:' + accent + '">'
    +   '<div class="timeline-marker">'
    +     '<div class="timeline-time">' + escapeHtml(marker.main) + '</div>'
    +     '<div class="timeline-subtime">' + escapeHtml(marker.sub) + '</div>'
    +   '</div>'
    +   '<button type="button" class="timeline-card timeline-card-button" data-todo-id="' + escapeHtml(String(item.id ?? '')) + '">'
    +     '<div class="timeline-card-title-row">'
    +       '<div class="timeline-card-title">' + escapeHtml(item.title) + '</div>'
    +       '<div class="pill">' + escapeHtml(todoStateLabel(item)) + '</div>'
    +     '</div>'
    +     '<div class="timeline-card-meta">' + escapeHtml(meta || '无附加信息') + '</div>'
    +     (item.notes ? '<div class="timeline-card-notes">' + escapeHtml(item.notes) + '</div>' : '')
    +     '<div class="timeline-card-meta">' + escapeHtml(reminderText(item)) + '</div>'
    +   '</button>'
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
    + '<article class="event-card" role="button" tabindex="0" aria-label="编辑日程：' + escapeHtml(item.title) + '" data-event-id="' + escapeHtml(String(item.id ?? '')) + '" style="--accent:' + accent + ';top:' + segment.top + 'px;height:' + segment.height + 'px">'
    +   '<div class="event-card-title">' + escapeHtml(item.title) + '</div>'
    +   '<div class="event-card-meta">' + escapeHtml(segment.startLabel) + ' - ' + escapeHtml(segment.endLabel) + '</div>'
    +   '<div class="event-card-meta">' + escapeHtml(meta || '定时日程') + '</div>'
    +   (item.notes ? '<div class="event-card-notes">' + escapeHtml(item.notes) + '</div>' : '')
    + '</article>';
}

function openEventEditorById(id) {
  const eventItem = findEventById(id);
  if (!eventItem) {
    els.status.textContent = '没有找到这个日程，请刷新后重试。';
    return;
  }
  openEventEditor(eventItem);
}

function openEventPreviewById(id) {
  const eventItem = findEventById(id);
  if (!eventItem) {
    els.status.textContent = '没有找到这个日程，请刷新后重试。';
    return;
  }
  openEventPreview(eventItem);
}

function syncTopbar() {
  const todoMode = state.currentTab === 'todos';
  const eventMode = state.currentTab === 'events';
  els.panelTitle.textContent = todoMode ? '每日看板' : eventMode ? '日程时间轴' : '规划台';
  els.viewCaption.textContent = todoMode ? '桌面端每日看板' : eventMode ? '桌面端日程模式' : 'Markdown 规划模式';
  els.openCreate.textContent = todoMode ? '新增待办' : eventMode ? '新增日程' : '新建规划';
  els.openCreate.classList.toggle('hidden', state.currentTab === 'planning');
  const dataMode = todoMode && state.todosLoaded
    ? '待办按需数据'
    : eventMode && visibleEventRangeLoaded()
      ? '日程范围数据'
      : state.snapshot?.partial
        ? '看板轻量数据'
        : '完整数据';
  els.snapshotMeta.textContent = state.snapshot
    ? ('最近刷新：' + formatDateTimeLabel(state.snapshot.generatedAtMillis) + ' · ' + dataMode)
    : '连接后即可读取手机端当前数据';
}

function ensureSelectedEventDay() {
  if (!state.selectedEventDay) state.selectedEventDay = dayKey(new Date());
}

async function connect() {
  state.token = els.token.value.trim();
  state.planningLoaded = false;
  await loadSnapshot({ planning: false });
  els.status.textContent = '已连接';
}

async function loadSnapshot(options = {}) {
  const shouldLoadPlanning = options.planning === true || state.currentTab === 'planning';
  state.snapshot = await api('/api/snapshot?scope=board');
  state.todosLoaded = false;
  state.eventsLoaded = false;
  state.eventRangeStart = null;
  state.eventRangeEnd = null;
  state.eventLoadSerial += 1;
  if (shouldLoadPlanning) await loadPlanningNotes();
  ensureSelectedEventDay();
  if (options.todos === true) await loadDesktopTodos({ render: false });
  if (options.events === true) await loadDesktopEvents({ render: false });
  renderTodos();
  if (state.currentTab === 'events') renderEvents();
  renderAnnouncements();
  if (state.planningLoaded) renderPlanningNotes();
  syncTopbar();
}

async function ensureFullSnapshot() {
  await Promise.all([ensureTodoData(), ensureEventData()]);
}

function ensureSnapshotContainer() {
  if (state.snapshot) return;
  state.snapshot = {
    generatedAtMillis: Date.now(),
    partial: true,
    groups: [],
    todos: [],
    events: [],
    announcements: [],
    todayBoard: null
  };
}

function mergeSnapshotGroups(groups) {
  if (!Array.isArray(groups) || !groups.length) return;
  ensureSnapshotContainer();
  state.snapshot.groups = groups;
}

function currentEventRange() {
  const keys = getVisibleEventKeys();
  const start = keys[0] || dayKey(new Date());
  const end = addDays(keys[keys.length - 1] || start, 1);
  return { start, end };
}

function visibleEventRangeLoaded() {
  const range = currentEventRange();
  return state.eventsLoaded && state.eventRangeStart === range.start && state.eventRangeEnd === range.end;
}

async function loadDesktopTodos(options = {}) {
  const data = await api('/api/todos');
  ensureSnapshotContainer();
  mergeSnapshotGroups(data.groups || []);
  state.snapshot.todos = data.todos || [];
  state.snapshot.generatedAtMillis = data.generatedAtMillis || Date.now();
  state.todosLoaded = true;
  if (options.render !== false) {
    renderTodos();
    syncTopbar();
  }
}

async function ensureTodoData() {
  if (state.todosLoaded) return;
  els.status.textContent = '正在加载完整待办列表…';
  await loadDesktopTodos();
  els.status.textContent = '已加载完整待办列表';
}

async function loadDesktopEvents(options = {}) {
  const range = currentEventRange();
  const requestSerial = ++state.eventLoadSerial;
  const data = await api('/api/events?start=' + encodeURIComponent(range.start) + '&end=' + encodeURIComponent(range.end));
  if (requestSerial !== state.eventLoadSerial) return false;
  ensureSnapshotContainer();
  mergeSnapshotGroups(data.groups || []);
  state.snapshot.events = data.events || [];
  state.snapshot.generatedAtMillis = data.generatedAtMillis || Date.now();
  state.eventsLoaded = true;
  state.eventRangeStart = data.rangeStart || range.start;
  state.eventRangeEnd = data.rangeEnd || range.end;
  if (options.render !== false) {
    renderEvents();
    syncTopbar();
  }
  return true;
}

async function ensureEventData() {
  if (visibleEventRangeLoaded()) return;
  els.status.textContent = '正在加载当前日程范围…';
  const applied = await loadDesktopEvents();
  if (applied !== false) els.status.textContent = '已加载当前日程范围';
}

async function refreshCurrentData() {
  await loadSnapshot({
    planning: state.currentTab === 'planning',
    todos: state.currentTab === 'todos' && state.todosLoaded,
    events: state.currentTab === 'events'
  });
}

async function refreshAfterMutation() {
  await refreshCurrentData();
}

function renderAnnouncements() {
  if (!els.announcementsBanner) return;
  const announcements = state.snapshot?.announcements || [];
  if (!announcements.length) {
    els.announcementsBanner.classList.add('hidden');
    els.announcementsBanner.classList.remove('marquee-needed');
    els.announcementsBanner.innerHTML = '';
    return;
  }
  const text = announcements.map(item => {
    const range = item.rangeLabel ? item.rangeLabel + ' · ' : '';
    const source = item.sourceNoteTitle ? '（' + item.sourceNoteTitle + '）' : '';
    return range + item.text + source;
  }).join(' · ');
  els.announcementsBanner.innerHTML = '<span>' + escapeHtml(text) + '</span>';
  els.announcementsBanner.classList.toggle('marquee-needed', text.length > 60);
  els.announcementsBanner.classList.remove('hidden');
}

async function loadPlanningNotes() {
  const data = await api('/api/planning/notes');
  state.planningNotes = data.notes || [];
  state.activePlanningNoteId = data.activeNoteId || (state.planningNotes[0] && state.planningNotes[0].id) || null;
  state.planningLoaded = true;
  await loadPlanningMappings();
}

async function loadPlanningMappings() {
  const active = activePlanningNote();
  if (!active) {
    state.planningMappings = [];
    return;
  }
  const data = await api('/api/planning/mappings?noteId=' + encodeURIComponent(active.id));
  state.planningMappings = data.mappings || [];
}

function renderTodos() {
  if (state.snapshot?.partial && !state.todosLoaded) {
    renderDesktopDailyBoard();
    const board = state.snapshot.todayBoard || {};
    const todoItems = board.todoItems || [];
    const allTodayEvents = board.allTodayEvents || [];
    const tomorrowEvents = board.tomorrowEvents || [];
    els.todoSummary.innerHTML = [
      renderSummaryCard('今日待办', todoItems.length),
      renderSummaryCard('今日日程', allTodayEvents.length),
      renderSummaryCard('明日日程', tomorrowEvents.length),
      renderSummaryCard('加载模式', '轻量')
    ].join('');
    els.todoTimeline.innerHTML = ''
      + '<div class="empty-state">'
      +   '<strong>当前仅加载每日看板数据。</strong><br />'
      +   '为了减少电脑端首屏读取压力，完整待办时间轴会按需加载。'
      +   '<div class="actions"><button type="button" class="ghost mini" data-load-todos="true">加载完整待办列表</button></div>'
      + '</div>';
    bindActions();
    return;
  }
  const now = Date.now();
  const today = dayKey(new Date());
  const active = activeTodos().slice().sort((a, b) => (a.hasDueDate ? (a.dueAtMillis || Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER) - (b.hasDueDate ? (b.dueAtMillis || Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER));
  const history = historyTodos().slice().sort((a, b) => (b.completedAtMillis || b.canceledAtMillis || b.missedAtMillis || b.dueAtMillis || 0) - (a.completedAtMillis || a.canceledAtMillis || a.missedAtMillis || a.dueAtMillis || 0));
  const sections = [
    { key: 'missed', title: '已错过', empty: '当前没有已错过的待办。', items: active.filter(item => item.missed || (item.hasDueDate && (item.dueAtMillis || 0) < now && dayKeyFromMillis(item.dueAtMillis) < today)) },
    { key: 'today', title: '今日待办', empty: '今天暂时没有待办。', items: active.filter(item => !item.missed && (!item.hasDueDate || dayKeyFromMillis(item.dueAtMillis) === today)) },
    { key: 'upcoming', title: '计划中', empty: '后续暂时没有排期。', items: active.filter(item => item.hasDueDate && !item.missed && dayKeyFromMillis(item.dueAtMillis) > today) },
    { key: 'history', title: '历史记录', empty: '暂无历史记录。', items: history }
  ];
  els.todoSummary.innerHTML = [
    renderSummaryCard('活动待办', active.length),
    renderSummaryCard('已错过', sections[0].items.length),
    renderSummaryCard('今日待办', sections[1].items.length),
    renderSummaryCard('历史记录', history.length)
  ].join('');
  renderDesktopDailyBoard();
  els.todoTimeline.innerHTML = sections.map(renderTodoSection).join('');
  bindActions();
}

function renderDesktopDailyBoard() {
  if (!els.desktopDailyBoard) return;
  const board = state.snapshot?.todayBoard;
  if (!board) {
    els.desktopDailyBoard.innerHTML = '<div class="empty-state">连接手机后，这里会显示和手机端一致的每日看板。</div>';
    return;
  }
  const date = dateFromKey(board.date || dayKey(new Date()));
  const todoItems = board.todoItems || [];
  const allTodayEvents = board.allTodayEvents || [];
  const visibleTodayEvents = board.visibleTodayEvents || [];
  const tomorrowEvents = board.tomorrowEvents || [];
  const focusMinutes = Number(board.todayFocusMinutes || 0);
  const focusSessions = Number(board.todayFocusSessionCount || 0);
  const completedFocusSessions = Number(board.todayCompletedFocusSessionCount || 0);
  const nowMillis = Number(board.nowMillis || Date.now());
  const nowCard = renderBoardNowCard(nowMillis, visibleTodayEvents, todoItems);
  els.desktopDailyBoard.innerHTML = ''
    + '<section class="desktop-board-hero card-panel">'
    +   '<div>'
    +     '<div class="eyebrow">今日看板</div>'
    +     '<h2>' + escapeHtml(formatFullDateLabel(date)) + '</h2>'
    +     '<p class="muted">电脑端同步手机端当前看板数据，优先显示现在该处理什么。</p>'
    +   '</div>'
    +   nowCard
    +   '<div class="desktop-focus-metric">'
    +     '<div class="desktop-focus-minutes">' + escapeHtml(focusMinutes) + '</div>'
    +     '<div><strong>今日已专注</strong><span>' + escapeHtml(focusSessions + ' 次专注 · ' + completedFocusSessions + ' 次完成') + '</span></div>'
    +   '</div>'
    + '</section>'
    + '<section class="desktop-board-grid">'
    +   renderBoardTodoCard(todoItems)
    +   renderBoardScheduleCard(board.date || dayKey(new Date()), nowMillis, allTodayEvents, visibleTodayEvents, tomorrowEvents)
    + '</section>';
}

function renderBoardNowCard(nowMillis, visibleTodayEvents, todoItems) {
  const currentEvent = visibleTodayEvents.find(item => eventStart(item) <= nowMillis && nowMillis < eventEnd(item));
  if (currentEvent) {
    return ''
      + '<button type="button" class="desktop-now-card active" data-event-id="' + escapeHtml(String(currentEvent.id ?? '')) + '" style="--accent:' + escapeHtml(currentEvent.accentColorHex || currentEvent.groupColorHex || '#FFC94A') + '">'
      +   '<span>正在进行</span>'
      +   '<strong>' + escapeHtml(currentEvent.title || '未命名日程') + '</strong>'
      +   '<small>' + escapeHtml(eventTimeText(currentEvent)) + '</small>'
      + '</button>';
  }
  const nextEvent = visibleTodayEvents.find(item => eventStart(item) > nowMillis);
  if (nextEvent) {
    return ''
      + '<button type="button" class="desktop-now-card" data-event-id="' + escapeHtml(String(nextEvent.id ?? '')) + '" style="--accent:' + escapeHtml(nextEvent.accentColorHex || nextEvent.groupColorHex || '#4e87e1') + '">'
      +   '<span>下一项日程</span>'
      +   '<strong>' + escapeHtml(nextEvent.title || '未命名日程') + '</strong>'
      +   '<small>' + escapeHtml(eventTimeText(nextEvent)) + '</small>'
      + '</button>';
  }
  const nextTodo = todoItems.find(item => !item.missed) || todoItems[0];
  if (nextTodo) {
    return ''
      + '<button type="button" class="desktop-now-card" data-todo-id="' + escapeHtml(String(nextTodo.id ?? '')) + '" style="--accent:' + escapeHtml(nextTodo.groupColorHex || nextTodo.accentColorHex || '#4e87e1') + '">'
      +   '<span>' + escapeHtml(nextTodo.missed ? '已错过待办' : '今日待办') + '</span>'
      +   '<strong>' + escapeHtml(nextTodo.title || '未命名待办') + '</strong>'
      +   '<small>' + escapeHtml(nextTodo.hasDueDate ? ('DDL ' + formatDateTimeLabel(nextTodo.dueAtMillis)) : '无 DDL') + '</small>'
      + '</button>';
  }
  return '<div class="desktop-now-card empty"><span>当前状态</span><strong>暂无正在进行的事项</strong><small>可以继续查看下方今日 / 明日日程。</small></div>';
}

function renderBoardTodoCard(items) {
  const body = items.length
    ? items.map(renderBoardTodoRow).join('')
    : '<div class="board-empty">今天还没有安排任务。</div>';
  return ''
    + '<article class="desktop-board-card">'
    +   '<div class="desktop-board-card-head"><h3>今日待办</h3><span>' + items.length + ' 项</span></div>'
    +   '<div class="desktop-board-list">' + body + '</div>'
    + '</article>';
}

function renderBoardTodoRow(item) {
  const accent = item.groupColorHex || item.accentColorHex || '#4e87e1';
  const status = item.missed ? '已错过' : (item.hasDueDate ? formatTimeLabel(item.dueAtMillis) : '无 DDL');
  const meta = [item.groupName || '未分组', item.hasDueDate ? ('DDL ' + formatDateTimeLabel(item.dueAtMillis)) : '', item.notes || ''].filter(Boolean).join(' · ');
  return ''
    + '<button type="button" class="desktop-board-row todo" data-todo-id="' + escapeHtml(String(item.id ?? '')) + '" style="--accent:' + escapeHtml(accent) + '">'
    +   '<span class="desktop-board-strip"></span>'
    +   '<span class="desktop-board-row-main">'
    +     '<strong>' + escapeHtml(item.title) + '</strong>'
    +     '<small>' + escapeHtml(meta || '无附加信息') + '</small>'
    +   '</span>'
    +   '<span class="desktop-board-chip">' + escapeHtml(status) + '</span>'
    + '</button>';
}

function renderBoardScheduleCard(dateKey, nowMillis, allTodayEvents, visibleTodayEvents, tomorrowEvents) {
  const date = dateFromKey(dateKey);
  const todayBody = visibleTodayEvents.length
    ? visibleTodayEvents.map(item => renderBoardEventRow(item, nowMillis)).join('')
    : '<div class="board-empty ' + (allTodayEvents.length ? 'finished' : '') + '">' + escapeHtml(allTodayEvents.length ? '太棒了！今天的日程都结束了~' : '今天暂无日程') + '</div>';
  const tomorrowBody = tomorrowEvents.length
    ? tomorrowEvents.slice(0, 4).map(item => renderBoardEventRow(item, null)).join('')
    : '<button type="button" class="board-empty action" data-tab-jump="planning">明天暂无日程 · 去规划台安排一下？</button>';
  return ''
    + '<article class="desktop-board-card schedule">'
    +   '<div class="desktop-schedule-layout">'
    +     '<div class="desktop-schedule-date">'
    +       '<span>' + escapeHtml((date.getMonth() + 1) + '月') + '</span>'
    +       '<strong>' + escapeHtml(date.getDate()) + '</strong>'
    +       '<em>' + escapeHtml(formatWeekday(date)) + '</em>'
    +     '</div>'
    +     '<div class="desktop-schedule-main">'
    +       '<div class="desktop-board-card-head"><h3>今日日程</h3><span>' + allTodayEvents.length + ' 项</span></div>'
    +       '<div class="desktop-board-list">' + todayBody + '</div>'
    +       '<div class="desktop-tomorrow-title">明天</div>'
    +       '<div class="desktop-board-list">' + tomorrowBody + '</div>'
    +     '</div>'
    +   '</div>'
    + '</article>';
}

function renderBoardEventRow(item, nowMillis) {
  const accent = item.accentColorHex || item.groupColorHex || '#4e87e1';
  const inProgress = nowMillis != null && eventStart(item) <= nowMillis && nowMillis < eventEnd(item);
  const classes = 'desktop-board-row event' + (inProgress ? ' in-progress' : '');
  const meta = [eventTimeText(item), item.location || '', item.notes || ''].filter(Boolean).join(' · ');
  return ''
    + '<button type="button" class="' + classes + '" data-event-id="' + escapeHtml(String(item.id ?? '')) + '" style="--accent:' + escapeHtml(accent) + '">'
    +   '<span class="desktop-board-strip"></span>'
    +   '<span class="desktop-board-row-main">'
    +     '<strong>' + escapeHtml(item.title) + '</strong>'
    +     '<small>' + escapeHtml(meta || '定时日程') + '</small>'
    +   '</span>'
    +   (inProgress ? '<span class="desktop-board-chip gold">进行中</span>' : '')
    + '</button>';
}

function renderEvents() {
  if (!visibleEventRangeLoaded()) {
    els.eventDayHeaders.innerHTML = '';
    els.hourAxis.innerHTML = '';
    els.eventTimeline.innerHTML = '<div class="empty-state">日程时间轴正在加载当前可见日期范围。</div>';
    ensureEventData().catch(err => els.status.textContent = err.message);
    return;
  }
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
  document.querySelectorAll('[data-event-id]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      event.stopPropagation();
      openEventEditorById(node.dataset.eventId);
    };
    node.onkeydown = event => {
      if (event.key === 'Enter' || event.key === ' ') node.click();
    };
  });
  bindActions();
}

function activePlanningNote() {
  return (state.planningNotes || []).find(note => sameId(note.id, state.activePlanningNoteId)) || (state.planningNotes || [])[0] || null;
}

function renderPlanningNotes() {
  if (!els.planningNoteSelect || !els.planningEditor) return;
  const notes = state.planningNotes || [];
  els.planningNoteSelect.innerHTML = notes.map(note => '<option value="' + note.id + '">' + escapeHtml(note.title) + '</option>').join('');
  const active = activePlanningNote();
  if (active) {
    const activeChanged = !sameId(state.planningRenderedNoteId, active.id);
    state.activePlanningNoteId = active.id;
    els.planningNoteSelect.value = String(active.id);
    if (activeChanged || !state.planningDirty) {
      els.planningEditor.value = active.contentMarkdown || '';
      state.planningDirty = false;
    }
    if (els.planningActiveTitle) els.planningActiveTitle.textContent = active.title || '我的规划';
    state.planningRenderedNoteId = active.id;
  } else if (els.planningActiveTitle) {
    els.planningActiveTitle.textContent = '未选择规划文档';
  }
  const undoButton = document.getElementById('planning-undo');
  if (undoButton) undoButton.disabled = !latestPlanningUndoSummary();
  renderPlanningPreview();
}

function planningTypeLabel(type) {
  if (type === 'TODO') return '待办';
  if (type === 'EVENT') return '日程';
  if (type === 'SKIPPED') return '跳过';
  if (type === 'ERROR') return '错误';
  return type || '未知';
}

function planningTimeText(item) {
  if (item.type === 'TODO') return item.dueAt ? ('DDL：' + item.dueAt.replace('T', ' ')) : '无 DDL';
  if (item.type === 'EVENT') return (item.startAt || '未设置') + ' - ' + (item.endAt || '未设置') + (item.defaultToday ? '（默认今天）' : '');
  return item.message || '';
}

function editableDateTimeValue(value) {
  return String(value || '').replace('T', ' ');
}

function planningCursorLine() {
  const value = els.planningEditor?.value || '';
  const cursor = els.planningEditor?.selectionStart || 0;
  return value.slice(0, cursor).split('\n').length;
}

function planningEditableField(id, field, label, value, placeholder) {
  return '<label class="planning-edit-field"><span>' + escapeHtml(label) + '</span><input data-planning-field="' + escapeHtml(field) + '" data-planning-id="' + escapeHtml(id) + '" value="' + escapeHtml(value || '') + '" placeholder="' + escapeHtml(placeholder || '') + '" /></label>';
}

function planningMappingStatusLabel(status) {
  if (status === 'ACTIVE') return '已导入';
  if (status === 'COMPLETED') return '✓ 已完成';
  if (status === 'CANCELED') return '已取消';
  if (status === 'ORPHANED') return '映射丢失';
  if (status === 'CONFLICT') return '已手动修改';
  return '';
}

function latestPlanningUndoSummary() {
  const mappings = (state.planningMappings || []).filter(item => ['IMPORT', 'REFRESH', 'POSTPONE'].includes(String(item.operationType || '')));
  if (!mappings.length) return null;
  const latest = mappings.slice().sort((a, b) => {
    const timeDiff = Number(b.lastRefreshedAtMillis || 0) - Number(a.lastRefreshedAtMillis || 0);
    return timeDiff || (Number(b.id || 0) - Number(a.id || 0));
  })[0];
  const batch = mappings.filter(item => String(item.batchId || '') === String(latest.batchId || ''));
  const affectedIds = new Set(batch.map(item => String(item.todoId ?? item.eventId ?? '')).filter(Boolean));
  return {
    batchId: latest.batchId,
    operationType: latest.operationType,
    affectedCount: affectedIds.size || batch.length,
    label: latest.operationType === 'IMPORT' ? '导入' : latest.operationType === 'REFRESH' ? '刷新' : latest.operationType === 'POSTPONE' ? '顺延' : String(latest.operationType || '')
  };
}

function renderPlanningMappingPreview() {
  const mappings = state.planningMappings || [];
  if (!mappings.length) return '';
  const sorted = mappings.slice().sort((a, b) => (a.lastKnownLineNumber || 0) - (b.lastKnownLineNumber || 0));
  return sorted.map(mapping => {
    const status = String(mapping.status || '').toLowerCase();
    const conflictActions = mapping.status === 'CONFLICT'
      ? '<div class="planning-conflict-actions">'
          + '<button type="button" class="ghost mini" data-planning-conflict-document="' + escapeHtml(mapping.id) + '">以文档为准覆盖</button>'
          + '<button type="button" class="ghost mini" data-planning-conflict-item="' + escapeHtml(mapping.id) + '">以事项为准更新原文</button>'
        + '</div>'
      : '';
    return ''
      + '<article class="planning-candidate ' + escapeHtml(status) + '">'
      +   '<div class="planning-candidate-head">'
      +     '<span class="pill">' + escapeHtml(planningMappingStatusLabel(mapping.status)) + '</span>'
      +     '<span class="muted">第 ' + escapeHtml(mapping.lastKnownLineNumber || '-') + ' 行</span>'
      +   '</div>'
      +   '<div class="planning-source">' + escapeHtml(mapping.currentLineText || mapping.originalLineText || '') + '</div>'
      +   conflictActions
      + '</article>';
  }).join('');
}

function renderPlanningPreview() {
  if (!els.planningPreview || !els.planningPreviewMeta) return;
  const result = state.planningParseResult;
  const parseButton = document.getElementById('planning-parse');
  if (parseButton) {
    parseButton.disabled = state.planningParsing;
    parseButton.textContent = state.planningParsing ? '识别中' : '识别';
  }
  if (!result) {
    els.planningPreviewMeta.textContent = state.planningParsing ? '识别中…' : '尚未识别';
    const mappingHtml = renderPlanningMappingPreview();
    els.planningPreview.innerHTML = mappingHtml || '<div class="empty-state">写完计划后点&quot;识别&quot;，这里会显示识别结果。<br>你也可以直接自由书写，AI 会尝试理解你的意图。</div>';
    return;
  }
  const candidates = result.candidates || [];
  const summary = '共 ' + candidates.length + ' 行，' + (result.importableCount || 0) + ' 条可导入。';
  els.planningPreviewMeta.textContent = result.message ? (summary + ' ' + result.message) : summary;
  const actions = candidates.length ? (
    '<div class="planning-preview-actions">'
    + '<button type="button" class="ghost mini" data-planning-select-all="true">全选可导入项</button>'
    + '<button type="button" class="ghost mini" data-planning-clear-all="true">全不选</button>'
    + '</div>'
  ) : '';
  els.planningPreview.innerHTML = actions + (candidates.map(item => {
    const editable = item.type === 'TODO' || item.type === 'EVENT';
    const importable = editable ? (!item.imported && !item.completed) : (item.importable && !item.imported);
    const checked = importable && !item.importBlocked ? ' checked' : '';
    const linked = item.type === 'EVENT' ? '<label class="planning-linked"><input type="checkbox" data-planning-linked="' + escapeHtml(item.id) + '"' + (item.createLinkedTodo ? ' checked' : '') + ' /> 同时创建待办</label>' : '';
    const editFields = editable ? (
      '<div class="planning-edit-grid">'
      + planningEditableField(item.id, 'title', '标题', item.title || '', '任务标题')
      + planningEditableField(item.id, 'groupName', '分组', item.groupName || '', '例行')
      + (item.type === 'TODO'
        ? planningEditableField(item.id, 'dueAt', 'DDL', editableDateTimeValue(item.dueAt), '2026-05-28 14:30')
        : planningEditableField(item.id, 'startAt', '开始', editableDateTimeValue(item.startAt), '2026-05-28 10:00') + planningEditableField(item.id, 'endAt', '结束', editableDateTimeValue(item.endAt), '2026-05-28 12:00'))
      + planningEditableField(item.id, 'reminders', '提醒', item.reminderInputText || (item.reminderOffsetsMinutes || []).join(','), '5,15,16:30,05-10 15:00')
      + '</div>'
      + '<label class="planning-edit-field full"><span>备注</span><textarea data-planning-field="notes" data-planning-id="' + escapeHtml(item.id) + '" rows="2">' + escapeHtml(item.notes || '') + '</textarea></label>'
    ) : '';
    return ''
      + '<article class="planning-candidate ' + String(item.type || '').toLowerCase() + '">'
      +   '<div class="planning-candidate-head">'
      +     '<label><input type="checkbox" data-planning-select="' + escapeHtml(item.id) + '"' + checked + (importable ? '' : ' disabled') + ' /> 导入</label>'
      +     '<span class="pill">' + escapeHtml(planningTypeLabel(item.type)) + '</span>'
      +     '<span class="muted">第 ' + escapeHtml(item.lineNumber) + ' 行</span>'
      +   '</div>'
      +   '<div class="planning-source">' + escapeHtml(item.sourceLine || '') + '</div>'
      +   editFields
      +   (!editable ? '<div class="planning-meta-line">' + escapeHtml(planningTimeText(item)) + '</div>' : '<div class="planning-meta-line">提醒默认全屏 · 响铃 + 震动。</div>')
      +   linked
      +   (item.message ? '<div class="planning-message">' + escapeHtml(item.message) + '</div>' : '')
      + '</article>';
  }).join('') || '<div class="empty-state">没有识别结果。</div>');
}

async function resolvePlanningConflictDocument(mappingId) {
  const active = activePlanningNote();
  if (!active) throw new Error('没有可处理冲突的规划文档');
  const result = await api('/api/planning/conflict/document', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      markdown: els.planningEditor.value,
      mappingId
    })
  });
  if (result.updatedMarkdown != null) {
    els.planningEditor.value = result.updatedMarkdown;
    state.planningDirty = false;
  }
  await loadPlanningMappings();
  await refreshAfterMutation();
  renderPlanningNotes();
  els.status.textContent = result.message || '已按文档内容覆盖事项';
}

async function resolvePlanningConflictItem(mappingId) {
  const active = activePlanningNote();
  if (!active) throw new Error('没有可处理冲突的规划文档');
  const result = await api('/api/planning/conflict/item', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      markdown: els.planningEditor.value,
      mappingId
    })
  });
  if (result.updatedMarkdown != null) {
    els.planningEditor.value = result.updatedMarkdown;
    state.planningDirty = false;
    state.planningParseResult = null;
  }
  await loadPlanningMappings();
  renderPlanningNotes();
  els.status.textContent = result.message || '已按事项内容回写原文';
}

function collectPlanningCandidates() {
  const base = ((state.planningParseResult && state.planningParseResult.candidates) || []).map(item => ({ ...item }));
  const byId = new Map(base.map(item => [String(item.id), item]));
  document.querySelectorAll('[data-planning-field]').forEach(node => {
    const item = byId.get(String(node.dataset.planningId));
    if (!item) return;
    const value = node.value || '';
    if (node.dataset.planningField === 'reminders') {
      item.reminderInputText = value;
      item.reminderOffsetsMinutes = value.split(/[,，]/).map(token => Number(token.trim())).filter(Number.isFinite).map(value => Math.max(0, Math.floor(value)));
    } else if (['dueAt', 'startAt', 'endAt'].includes(node.dataset.planningField)) {
      const trimmed = value.trim();
      const parsedMillis = trimmed ? parseLocalDateTimeMillis(trimmed, new Date()) : null;
      item[node.dataset.planningField] = trimmed ? (parsedMillis == null ? trimmed.replace(' ', 'T') : formatDateTimeLocalValue(parsedMillis)) : null;
    } else {
      item[node.dataset.planningField] = value;
    }
  });
  document.querySelectorAll('[data-planning-linked]').forEach(node => {
    const item = byId.get(String(node.dataset.planningLinked));
    if (item) item.createLinkedTodo = node.checked;
  });
  return base;
}

function markPlanningDirty() {
  state.planningDirty = true;
  if (state.planningSaveTimer) window.clearTimeout(state.planningSaveTimer);
  state.planningSaveTimer = window.setTimeout(() => {
    savePlanningNote(true).catch(err => els.status.textContent = err.message);
  }, 2000);
}

async function flushPlanningAutosave() {
  if (state.planningSaveTimer) {
    window.clearTimeout(state.planningSaveTimer);
    state.planningSaveTimer = null;
  }
  if (state.planningDirty) await savePlanningNote(true);
}

async function savePlanningNote(quiet = false) {
  const active = activePlanningNote();
  if (!active) throw new Error('没有可保存的规划文档');
  if (state.planningSaving) return;
  if (state.planningSaveTimer) {
    window.clearTimeout(state.planningSaveTimer);
    state.planningSaveTimer = null;
  }
  state.planningSaving = true;
  const savedContent = els.planningEditor.value;
  try {
    await api('/api/planning/notes/' + active.id, {
      method: 'PUT',
      body: JSON.stringify({ contentMarkdown: savedContent })
    });
    await loadPlanningNotes();
    await loadPlanningMappings();
    renderPlanningNotes();
    state.planningDirty = els.planningEditor.value !== savedContent;
    if (!quiet) els.status.textContent = '规划文档已保存';
  } finally {
    state.planningSaving = false;
  }
}

async function createPlanningNote() {
  await flushPlanningAutosave();
  const title = prompt('新规划文档名称', '新的规划');
  if (!title) return;
  const data = await api('/api/planning/notes', { method: 'POST', body: JSON.stringify({ title }) });
  state.activePlanningNoteId = data.note && data.note.id;
  await loadPlanningNotes();
  await loadPlanningMappings();
  renderPlanningNotes();
  els.status.textContent = '已新建规划文档';
}

async function deletePlanningNote() {
  await flushPlanningAutosave();
  const active = activePlanningNote();
  if (!active) return;
  if (!await confirmDanger('确认删除规划文档', '删除后无法恢复：' + (active.title || '未命名规划'), '删除')) return;
  await api('/api/planning/notes/' + active.id, { method: 'DELETE' });
  state.planningParseResult = null;
  await loadPlanningNotes();
  await loadPlanningMappings();
  renderPlanningNotes();
}

async function parsePlanningEditor() {
  await flushPlanningAutosave();
  state.planningParsing = true;
  renderPlanningPreview();
  try {
    state.planningParseResult = await api('/api/planning/parse', {
      method: 'POST',
      body: JSON.stringify({ markdown: els.planningEditor.value })
    });
    renderPlanningPreview();
  } finally {
    state.planningParsing = false;
    renderPlanningPreview();
  }
}

async function importSelectedPlanning() {
  if (!state.planningParseResult) {
    els.status.textContent = '请先点击“识别”生成预览，再勾选并导入';
    renderPlanningPreview();
    return;
  }
  const selectedIds = Array.from(document.querySelectorAll('[data-planning-select]:checked')).map(node => node.dataset.planningSelect);
  if (!selectedIds.length) {
    els.status.textContent = '请先勾选至少一条可导入内容';
    return;
  }
  const active = activePlanningNote();
  const result = await api('/api/planning/import', {
    method: 'POST',
    body: JSON.stringify({ markdown: els.planningEditor.value, selectedIds, candidates: collectPlanningCandidates(), noteId: active && active.id })
  });
  if (result.updatedMarkdown != null) {
    els.planningEditor.value = result.updatedMarkdown;
    state.planningDirty = true;
    await savePlanningNote(true);
    state.planningParseResult = null;
    await loadPlanningMappings();
    renderPlanningPreview();
  }
  els.status.textContent = '已导入 ' + (result.imported || 0) + ' 条规划内容';
  await loadPlanningNotes();
  await refreshAfterMutation();
}

async function refreshPlanningImported() {
  const active = activePlanningNote();
  if (!active) throw new Error('没有可刷新的规划文档');
  const wholeDocument = window.confirm('确定要刷新整篇文档的未完成已导入项吗？\n选择“取消”则只刷新当前标题分区。');
  const result = await api('/api/planning/refresh', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      markdown: els.planningEditor.value,
      scope: wholeDocument ? 'WHOLE_DOCUMENT' : 'CURRENT_SECTION',
      cursorLineNumber: planningCursorLine()
    })
  });
  if (result.updatedMarkdown != null) {
    els.planningEditor.value = result.updatedMarkdown;
    state.planningDirty = true;
    await savePlanningNote(true);
  }
  await loadPlanningMappings();
  renderPlanningNotes();
  els.status.textContent = result.message || '刷新完成';
}

async function postponePlanningImported() {
  const active = activePlanningNote();
  if (!active) throw new Error('没有可顺延的规划文档');
  const mappings = (state.planningMappings || []).filter(item => item.status === 'ACTIVE');
  if (!mappings.length) throw new Error('当前没有可顺延的未完成已导入项');
  const summary = mappings.map((item, index) => '[' + (index + 1) + '] 第' + (item.lastKnownLineNumber || '-') + '行 ' + (item.currentLineText || item.originalLineText || '')).join('\n');
  const startIndex = Number(prompt('选择起始条目序号：\n' + summary, '1'));
  if (!Number.isFinite(startIndex) || startIndex < 1 || startIndex > mappings.length) return;
  const offsetMinutes = Number(prompt('输入顺延分钟数，例如 30 / 60 / -15', '30'));
  if (!Number.isFinite(offsetMinutes) || !offsetMinutes) return;
  const scopeRaw = prompt('范围：1=从此条目到分区末尾；2=从此条目到文档末尾；3=当前分区所有未完成项', '1');
  const scope = scopeRaw === '2' ? 'FROM_ITEM_TO_DOCUMENT_END' : (scopeRaw === '3' ? 'CURRENT_SECTION_ALL' : 'FROM_ITEM_TO_SECTION_END');
  const result = await api('/api/planning/postpone', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      markdown: els.planningEditor.value,
      mappingId: mappings[startIndex - 1].id,
      offsetMinutes,
      scope
    })
  });
  if (result.updatedMarkdown != null) {
    els.planningEditor.value = result.updatedMarkdown;
    state.planningDirty = true;
    await savePlanningNote(true);
  }
  await loadPlanningMappings();
  renderPlanningNotes();
  els.status.textContent = result.message || '顺延完成';
}

async function undoPlanningLastOperation() {
  const active = activePlanningNote();
  if (!active) throw new Error('没有可撤销的规划文档');
  const undoSummary = latestPlanningUndoSummary();
  if (!undoSummary) throw new Error('当前没有可撤销的导入、刷新或顺延批次');
  if (!await confirmDanger('撤销上次规划台操作', '会回滚最近一次' + undoSummary.label + '批次，影响 ' + undoSummary.affectedCount + ' 条事项。', '撤销')) return;
  const result = await api('/api/planning/undo-last', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      markdown: els.planningEditor.value
    })
  });
  if (result.updatedMarkdown != null) {
    els.planningEditor.value = result.updatedMarkdown;
    state.planningDirty = true;
    await savePlanningNote(true);
  }
  await loadPlanningMappings();
  await refreshAfterMutation();
  renderPlanningNotes();
  els.status.textContent = result.message || '撤销完成';
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

function parseLocalDateTimeMillis(text, fallbackDate) {
  const normalizeDateTimeText = raw => String(raw || '')
    .trim()
    .replace(/：/g, ':')
    .replace(/[，]/g, ',')
    .replace(/[．。]/g, '.')
    .replace(/／/g, '/')
    .replace(/[－–—]/g, '-')
    .replace(/[～〜]/g, '~')
    .replace('T', ' ');
  const value = normalizeDateTimeText(text);
  const buildDate = (year, month, day, hour, minute) => {
    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
    const date = new Date(year, month - 1, day, hour, minute, 0, 0);
    if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null;
    return date.getTime();
  };
  const parseHour = (rawHour, period, chinesePeriod) => {
    let hour = Number(rawHour);
    const lower = String(period || '').toLowerCase();
    if (lower) {
      if (hour < 1 || hour > 12) return null;
      if (lower === 'pm' && hour < 12) hour += 12;
      if (lower === 'am' && hour === 12) hour = 0;
    } else if (chinesePeriod) {
      if (hour < 0 || hour > 23) return null;
      if (['凌晨'].includes(chinesePeriod) && hour === 12) hour = 0;
      if (['早上', '上午'].includes(chinesePeriod) && hour === 12) hour = 0;
      if (chinesePeriod === '中午' && hour >= 1 && hour <= 10) hour += 12;
      if (['下午', '晚上'].includes(chinesePeriod) && hour < 12) hour += 12;
    }
    return hour;
  };
  const weekdayValue = label => {
    if (['周一', '星期一', '礼拜一'].includes(label)) return 1;
    if (['周二', '星期二', '礼拜二'].includes(label)) return 2;
    if (['周三', '星期三', '礼拜三'].includes(label)) return 3;
    if (['周四', '星期四', '礼拜四'].includes(label)) return 4;
    if (['周五', '星期五', '礼拜五'].includes(label)) return 5;
    if (['周六', '星期六', '礼拜六'].includes(label)) return 6;
    if (['周日', '周天', '星期日', '星期天', '礼拜日', '礼拜天'].includes(label)) return 7;
    return null;
  };
  const resolveRelativeDate = label => {
    const date = fallbackDate ? new Date(fallbackDate.getTime()) : new Date();
    if (label === '明天' || label === '明日') date.setDate(date.getDate() + 1);
    if (label === '后天') date.setDate(date.getDate() + 2);
    const targetWeekday = weekdayValue(label);
    if (targetWeekday != null) {
      const currentWeekday = date.getDay() === 0 ? 7 : date.getDay();
      date.setDate(date.getDate() + ((targetWeekday - currentWeekday + 7) % 7));
    }
    return date;
  };
  const timeExpr = '(凌晨|早上|上午|中午|下午|晚上)?\\s*(\\d{1,2}):(\\d{2})\\s*([aApP][mM])?';
  let match = value.match(new RegExp('^' + timeExpr + '$'));
  if (match) {
    const date = fallbackDate ? new Date(fallbackDate.getTime()) : new Date();
    const hour = parseHour(match[2], match[4], match[1]);
    return hour == null ? null : buildDate(date.getFullYear(), date.getMonth() + 1, date.getDate(), hour, Number(match[3]));
  }
  match = value.match(new RegExp('^(今天|今日|明天|明日|后天|周[一二三四五六日天]|星期[一二三四五六日天]|礼拜[一二三四五六日天])[\\s,]+' + timeExpr + '$'));
  if (match) {
    const date = resolveRelativeDate(match[1]);
    const hour = parseHour(match[3], match[5], match[2]);
    return hour == null ? null : buildDate(date.getFullYear(), date.getMonth() + 1, date.getDate(), hour, Number(match[4]));
  }
  match = value.match(/^(今天|今日|明天|明日|后天|周[一二三四五六日天]|星期[一二三四五六日天]|礼拜[一二三四五六日天])$/);
  if (match) {
    const date = resolveRelativeDate(match[1]);
    return buildDate(date.getFullYear(), date.getMonth() + 1, date.getDate(), 23, 59);
  }
  match = value.match(/^(\d{1,2})[-./](\d{1,2})$/);
  if (match) {
    const year = (fallbackDate || new Date()).getFullYear();
    return buildDate(year, Number(match[1]), Number(match[2]), 23, 59);
  }
  match = value.match(/^(\d{1,2})月(\d{1,2})日?$/);
  if (match) {
    const year = (fallbackDate || new Date()).getFullYear();
    return buildDate(year, Number(match[1]), Number(match[2]), 23, 59);
  }
  match = value.match(/^(\d{4})[-./](\d{1,2})[-./](\d{1,2})$/);
  if (match) {
    return buildDate(Number(match[1]), Number(match[2]), Number(match[3]), 23, 59);
  }
  match = value.match(/^(\d{4})年(\d{1,2})月(\d{1,2})日?$/);
  if (match) {
    return buildDate(Number(match[1]), Number(match[2]), Number(match[3]), 23, 59);
  }
  match = value.match(new RegExp('^(\\d{1,2})[-./](\\d{1,2})[\\s,]+' + timeExpr + '$'));
  if (match) {
    const year = (fallbackDate || new Date()).getFullYear();
    const hour = parseHour(match[4], match[6], match[3]);
    return hour == null ? null : buildDate(year, Number(match[1]), Number(match[2]), hour, Number(match[5]));
  }
  match = value.match(new RegExp('^(\\d{1,2})月(\\d{1,2})日?[\\s,]+' + timeExpr + '$'));
  if (match) {
    const year = (fallbackDate || new Date()).getFullYear();
    const hour = parseHour(match[4], match[6], match[3]);
    return hour == null ? null : buildDate(year, Number(match[1]), Number(match[2]), hour, Number(match[5]));
  }
  match = value.match(new RegExp('^(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})[\\s,]+' + timeExpr + '$'));
  if (match) {
    const hour = parseHour(match[5], match[7], match[4]);
    return hour == null ? null : buildDate(Number(match[1]), Number(match[2]), Number(match[3]), hour, Number(match[6]));
  }
  match = value.match(new RegExp('^(\\d{4})年(\\d{1,2})月(\\d{1,2})日?[\\s,]+' + timeExpr + '$'));
  if (match) {
    const hour = parseHour(match[5], match[7], match[4]);
    return hour == null ? null : buildDate(Number(match[1]), Number(match[2]), Number(match[3]), hour, Number(match[6]));
  }
  return null;
}

function splitReminderSpecTokens(raw) {
  const normalized = String(raw || '')
    .replace(/：/g, ':')
    .replace(/[，]/g, ',')
    .replace(/[．。]/g, '.')
    .replace(/／/g, '/');
  const parts = normalized.split(',').map(part => part.trim()).filter(Boolean);
  const tokens = [];
  const dateOnlyPattern = /^(今天|今日|明天|明日|后天|周[一二三四五六日天]|星期[一二三四五六日天]|礼拜[一二三四五六日天]|\d{4}[-./]\d{1,2}[-./]\d{1,2}|\d{4}年\d{1,2}月\d{1,2}日?|\d{1,2}[-./]\d{1,2}|\d{1,2}月\d{1,2}日?)$/;
  const timeOnlyPattern = /^(凌晨|早上|上午|中午|下午|晚上)?\s*\d{1,2}:\d{2}\s*([aApP][mM])?$/;
  for (let index = 0; index < parts.length; index += 1) {
    if (index + 1 < parts.length && dateOnlyPattern.test(parts[index]) && timeOnlyPattern.test(parts[index + 1])) {
      tokens.push(parts[index] + ' ' + parts[index + 1]);
      index += 1;
    } else {
      tokens.push(parts[index]);
    }
  }
  return tokens;
}

function parseReminderSpecs(text, anchorMillis) {
  const raw = String(text || '').trim();
  if (!raw) return [];
  if (!anchorMillis) throw new Error('请先填写 DDL 或日程开始时间');
  const anchor = new Date(anchorMillis);
  const offsets = [];
  splitReminderSpecTokens(raw).forEach(part => {
    if (/^\d+$/.test(part)) {
      offsets.push(Number(part));
      return;
    }
    const millis = parseLocalDateTimeMillis(part, anchor);
    if (millis == null) throw new Error('无法识别提醒时间：' + part);
    if (millis > anchorMillis) throw new Error('提醒时间不能晚于目标时间：' + part);
    offsets.push(Math.round((anchorMillis - millis) / 60000));
  });
  return Array.from(new Set(offsets.filter(value => Number.isFinite(value) && value >= 0))).sort((a, b) => a - b);
}

function setInputInvalid(id, invalid) {
  document.getElementById(id)?.classList.toggle('input-invalid', invalid);
}

function reminderSpecFromOffsets(item, anchorMillis) {
  const offsets = item.reminderOffsetsMinutes || [];
  if (offsets.length) return offsets.join(',');
  if (item.reminderAtMillis && anchorMillis) return String(Math.round((anchorMillis - item.reminderAtMillis) / 60000));
  return '';
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
    document.getElementById('todo-reminder-spec').value = '';
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
  document.getElementById('todo-reminder-spec').value = '';
  document.getElementById('todo-reminder-mode').value = 'FULLSCREEN';
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
  document.getElementById('todo-reminder-spec').value = item.hasDueDate ? reminderSpecFromOffsets(item, item.dueAtMillis) : '';
  document.getElementById('todo-reminder-mode').value = item.reminderDeliveryMode || 'FULLSCREEN';
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
  document.getElementById('event-reminder-offsets').value = reminderSpecFromOffsets(item, item.startAtMillis);
  document.getElementById('event-recurrence-type').value = recurrenceTypeValue(item);
  document.getElementById('event-recurrence-end').value = item.recurrenceEndDate || '';
  document.getElementById('event-weekdays').value = csvValue(item.recurrenceWeekdays);
  document.getElementById('event-all-day').checked = item.allDay === true;
  document.getElementById('event-ring').checked = item.ringEnabled !== false;
  document.getElementById('event-vibrate').checked = item.vibrateEnabled !== false;
  openModal('event-modal');
}

function openTodoPreview(item) {
  state.previewTodoId = item.id;
  const accent = item.groupColorHex || item.accentColorHex || DEFAULT_EVENT_COLOR;
  const body = document.getElementById('todo-preview-body');
  const completeButton = document.getElementById('preview-todo-complete');
  const cancelButton = document.getElementById('preview-todo-cancel');
  if (!body) return;
  body.innerHTML = ''
    + '<div class="preview-title-row" style="--accent:' + accent + '">'
    +   '<div class="preview-color-block"></div>'
    +   '<div class="preview-main-title">' + escapeHtml(item.title) + '</div>'
    +   '<div class="pill">' + escapeHtml(todoStateLabel(item)) + '</div>'
    + '</div>'
    + previewRow('分', '分组', item.groupName || '未分组')
    + previewRow('时', 'DDL', todoDueText(item))
    + previewRow('循', '循环', recurrenceLabel(item))
    + previewRow('铃', '提醒', reminderText(item))
    + (item.notes ? previewRow('记', '备注', item.notes) : '');
  const inactive = item.completed || item.canceled;
  completeButton?.classList.toggle('hidden', inactive);
  cancelButton?.classList.toggle('hidden', inactive);
  openModal('todo-preview-modal');
}

function openEventPreview(item) {
  state.previewEventId = item.id;
  const accent = item.groupColorHex || item.accentColorHex || DEFAULT_EVENT_COLOR;
  const body = document.getElementById('event-preview-body');
  if (!body) return;
  body.innerHTML = ''
    + '<div class="preview-title-row" style="--accent:' + accent + '">'
    +   '<div class="preview-color-block"></div>'
    +   '<div class="preview-main-title">' + escapeHtml(item.title) + '</div>'
    + '</div>'
    + previewRow('时', '时间', eventTimeText(item))
    + previewRow('循', '重复', recurrenceLabel(item))
    + previewRow('地', '地点', item.location || '未填写')
    + previewRow('铃', '提醒', reminderText(item))
    + (item.notes ? previewRow('记', '备注', item.notes) : '');
  openModal('event-preview-modal');
}

function bindDigitInputs() {
  document.querySelectorAll('.digit-input').forEach(node => {
    node.addEventListener('input', () => {
      const digitsOnly = node.value.replace(/\D+/g, '').slice(0, Number(node.dataset.maxlength || node.maxLength || 4));
      node.value = digitsOnly;
      if (digitsOnly.length >= Number(node.dataset.maxlength || node.maxLength || 4) && node.dataset.next) {
        document.getElementById(node.dataset.next)?.focus();
      }
      updateDateTimePreview(node.id.replace(/-(year|month|day|hour|minute)$/, ''));
    });
    node.addEventListener('blur', () => {
      if (node.dataset.pad && node.value) node.value = node.value.padStart(Number(node.dataset.pad), '0');
      updateDateTimePreview(node.id.replace(/-(year|month|day|hour|minute)$/, ''));
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
  document.querySelectorAll('[data-todo-id]').forEach(node => {
    node.onclick = event => {
      event.stopPropagation();
      const todoItem = findTodoById(node.dataset.todoId);
      if (todoItem) openTodoPreview(todoItem);
    };
  });
  document.querySelectorAll('[data-event-id]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      event.stopPropagation();
      openEventEditorById(node.dataset.eventId);
    };
  });
  document.querySelectorAll('[data-tab-jump]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      document.querySelector('[data-tab="' + node.dataset.tabJump + '"]')?.click();
    };
  });
  document.querySelectorAll('[data-load-full-snapshot]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      ensureFullSnapshot().catch(err => els.status.textContent = err.message);
    };
  });
  document.querySelectorAll('[data-load-todos]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      ensureTodoData().catch(err => els.status.textContent = err.message);
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
document.getElementById('refresh').onclick = () => refreshCurrentData().catch(err => els.status.textContent = err.message);
window.addEventListener('resize', () => {
  if (state.currentTab === 'events' && state.snapshot) renderEvents();
});
document.getElementById('jump-today').onclick = () => {
  if (state.currentTab === 'events') {
    state.selectedEventDay = dayKey(new Date());
    renderEvents();
  } else {
    els.desktopDailyBoard?.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
document.getElementById('planning-new')?.addEventListener('click', () => createPlanningNote().catch(err => els.status.textContent = err.message));
document.getElementById('planning-delete')?.addEventListener('click', () => deletePlanningNote().catch(err => els.status.textContent = err.message));
document.getElementById('planning-save')?.addEventListener('click', () => savePlanningNote().catch(err => els.status.textContent = err.message));
document.getElementById('planning-help')?.addEventListener('click', () => openModal('planning-help-modal'));
document.getElementById('planning-parse')?.addEventListener('click', () => parsePlanningEditor().catch(err => els.status.textContent = err.message));
document.getElementById('planning-import')?.addEventListener('click', () => importSelectedPlanning().catch(err => els.status.textContent = err.message));
document.getElementById('planning-refresh')?.addEventListener('click', () => refreshPlanningImported().catch(err => els.status.textContent = err.message));
document.getElementById('planning-postpone')?.addEventListener('click', () => postponePlanningImported().catch(err => els.status.textContent = err.message));
document.getElementById('planning-undo')?.addEventListener('click', () => undoPlanningLastOperation().catch(err => els.status.textContent = err.message));
els.planningEditor?.addEventListener('input', () => {
  state.planningParseResult = null;
  renderPlanningPreview();
  markPlanningDirty();
});
els.planningEditor?.addEventListener('keydown', event => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault();
    savePlanningNote().catch(err => els.status.textContent = err.message);
  }
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault();
    parsePlanningEditor().catch(err => els.status.textContent = err.message);
  }
});
els.planningPreview?.addEventListener('click', event => {
  const selectAll = event.target.closest?.('[data-planning-select-all]');
  const clearAll = event.target.closest?.('[data-planning-clear-all]');
  const conflictDocument = event.target.closest?.('[data-planning-conflict-document]');
  const conflictItem = event.target.closest?.('[data-planning-conflict-item]');
  if (selectAll) {
    document.querySelectorAll('[data-planning-select]:not(:disabled)').forEach(node => { node.checked = true; });
  }
  if (clearAll) {
    document.querySelectorAll('[data-planning-select]').forEach(node => { node.checked = false; });
  }
  if (conflictDocument) {
    resolvePlanningConflictDocument(Number(conflictDocument.dataset.planningConflictDocument)).catch(err => els.status.textContent = err.message);
  }
  if (conflictItem) {
    resolvePlanningConflictItem(Number(conflictItem.dataset.planningConflictItem)).catch(err => els.status.textContent = err.message);
  }
});
els.planningNoteSelect?.addEventListener('change', async event => {
  try {
    await flushPlanningAutosave();
  } catch (err) {
    els.status.textContent = err.message;
    return;
  }
  state.activePlanningNoteId = event.target.value;
  const note = activePlanningNote();
  if (note && els.planningEditor) {
    els.planningEditor.value = note.contentMarkdown || '';
    state.planningDirty = false;
    state.planningRenderedNoteId = note.id;
    state.planningParseResult = null;
    await loadPlanningMappings();
    renderPlanningPreview();
  }
});
window.addEventListener('beforeunload', event => {
  if (!state.planningDirty) return;
  event.preventDefault();
  event.returnValue = '';
});
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
    openEventEditorById(eventNode.dataset.eventId);
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
    if (state.currentTab === 'events') {
      ensureEventData().catch(err => els.status.textContent = err.message);
    } else if (state.currentTab === 'planning') {
      loadPlanningNotes()
        .then(() => renderPlanningNotes())
        .catch(err => els.status.textContent = err.message);
    } else if (state.currentTab === 'todos') {
      renderTodos();
      renderAnnouncements();
    }
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
  try {
    setInputInvalid('todo-reminder-spec', false);
    const hasDueDate = document.getElementById('todo-has-due')?.checked !== false;
    const dueAt = hasDueDate ? readDateTimeValue('todo-due') : null;
    const dueAtMillis = dueAt ? new Date(dueAt).getTime() : null;
    const reminderOffsets = hasDueDate ? parseReminderSpecs(document.getElementById('todo-reminder-spec').value, dueAtMillis) : [];
    const payload = {
      title: document.getElementById('todo-title').value,
      notes: document.getElementById('todo-notes').value,
      dueAt: dueAt,
      reminderAt: null,
      reminderOffsetsMinutes: reminderOffsets,
      groupId: Number(document.getElementById('todo-group').value || 0),
      ringEnabled: document.getElementById('todo-ring').checked,
      vibrateEnabled: document.getElementById('todo-vibrate').checked,
      reminderDeliveryMode: document.getElementById('todo-reminder-mode').value,
      recurrence: hasDueDate ? recurrencePayload(
        document.getElementById('todo-recurrence-type').value,
        document.getElementById('todo-recurrence-end').value,
        document.getElementById('todo-weekdays').value
      ) : { enabled: false, type: 'NONE', weeklyDays: [], endDate: null }
    };
    if (state.editingTodoId) {
      await api(`/api/todos/${state.editingTodoId}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await api('/api/todos', { method: 'POST', body: JSON.stringify(payload) });
    }
    clearTodoForm();
    closeModal('todo-modal');
    await refreshAfterMutation();
  } catch (error) {
    setInputInvalid('todo-reminder-spec', true);
    els.status.textContent = error.message || '保存待办失败';
  }
};

document.getElementById('delete-todo').onclick = async () => {
  if (!state.editingTodoId) return;
  if (!await confirmDanger('确认删除待办', '删除后无法恢复。')) return;
  await api(`/api/items/${state.editingTodoId}`, { method: 'DELETE' });
  clearTodoForm();
  closeModal('todo-modal');
  await refreshAfterMutation();
};

document.getElementById('preview-todo-edit').onclick = () => {
  const todoItem = findTodoById(state.previewTodoId);
  if (!todoItem) return;
  closeModal('todo-preview-modal');
  openTodoEditor(todoItem);
};

document.getElementById('preview-todo-complete').onclick = async () => {
  if (!state.previewTodoId) return;
  await api(`/api/items/${state.previewTodoId}/complete`, { method: 'POST' });
  closeModal('todo-preview-modal');
  await refreshAfterMutation();
};

document.getElementById('preview-todo-cancel').onclick = async () => {
  if (!state.previewTodoId) return;
  await api(`/api/items/${state.previewTodoId}/cancel`, { method: 'POST' });
  closeModal('todo-preview-modal');
  await refreshAfterMutation();
};

document.getElementById('preview-todo-delete').onclick = async () => {
  if (!state.previewTodoId) return;
  if (!await confirmDanger('确认删除待办', '删除后无法恢复。')) return;
  await api(`/api/items/${state.previewTodoId}`, { method: 'DELETE' });
  closeModal('todo-preview-modal');
  await refreshAfterMutation();
};

document.getElementById('save-event').onclick = async () => {
  try {
    setInputInvalid('event-reminder-offsets', false);
    const startAt = readDateTimeValue('event-start');
    const endAt = readDateTimeValue('event-end');
    const startAtMillis = startAt ? new Date(startAt).getTime() : null;
    const payload = {
      title: document.getElementById('event-title').value,
      groupId: Number(document.getElementById('event-group').value || 0),
      location: document.getElementById('event-location').value,
      notes: document.getElementById('event-notes').value,
      startAt: startAt,
      endAt: endAt,
      allDay: document.getElementById('event-all-day').checked,
      accentColorHex: document.getElementById('event-color').value || DEFAULT_EVENT_COLOR,
      reminderOffsetsMinutes: parseReminderSpecs(document.getElementById('event-reminder-offsets').value, startAtMillis),
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
      await api(`/api/events/${state.editingEventId}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await api('/api/events', { method: 'POST', body: JSON.stringify(payload) });
    }
    clearEventForm();
    closeModal('event-modal');
    await refreshAfterMutation();
  } catch (error) {
    setInputInvalid('event-reminder-offsets', true);
    els.status.textContent = error.message || '保存日程失败';
  }
};

document.getElementById('delete-event').onclick = async () => {
  if (!state.editingEventId) return;
  if (!await confirmDanger('确认删除日程', '删除后无法恢复。')) return;
  await api(`/api/items/${state.editingEventId}`, { method: 'DELETE' });
  clearEventForm();
  closeModal('event-modal');
  await refreshAfterMutation();
};

document.getElementById('preview-event-edit').onclick = () => {
  const eventItem = findEventById(state.previewEventId);
  if (!eventItem) return;
  closeModal('event-preview-modal');
  openEventEditor(eventItem);
};

document.getElementById('preview-event-delete').onclick = async () => {
  if (!state.previewEventId) return;
  if (!await confirmDanger('确认删除日程', '删除后无法恢复。')) return;
  await api(`/api/items/${state.previewEventId}`, { method: 'DELETE' });
  closeModal('event-preview-modal');
  await refreshAfterMutation();
};

document.addEventListener('click', event => {
  const dayHeader = event.target.closest?.('[data-day]');
  if (!dayHeader) return;
  state.selectedEventDay = dayHeader.dataset.day;
  renderEvents();
});

bindDigitInputs();
syncTopbar();
