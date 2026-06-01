const HOUR_HEIGHT = 64;
const EVENT_HEADER_HEIGHT = 58;
const FIFTEEN_MINUTES = 15 * 60 * 1000;
const THIRTY_MINUTES = 30 * 60 * 1000;
const DEFAULT_EVENT_COLOR = '#4e87e1';
const EVENT_COLOR_PRESETS = ['#4E87E1', '#4CB782', '#FF6B4A', '#BF7B4D', '#8B5CF6', '#0F766E', '#D97706', '#E11D48'];
const TODO_PAGE_LIMIT = 80;
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
  planningNodes: [],
  planningNodesMarkdown: '',
  activePlanningNoteId: null,
  planningParseResult: null,
  planningSaveTimer: null,
  planningDirty: false,
  planningSaving: false,
  planningRenderedNoteId: null,
  planningParsing: false,
  planningMappings: [],
  planningLoaded: false,
  planningNodesLoaded: false,
  planningDraggingNodeId: null,
  planningPendingCommits: [],
  planningParseMarkdown: '',
  todosLoaded: false,
  todoOffset: 0,
  todoTotal: 0,
  todoHasMore: false,
  todoQuery: '',
  selectedTodoGroupIds: [],
  todoLoading: false,
  eventsLoaded: false,
  eventRangeStart: null,
  eventRangeEnd: null,
  eventLoadSerial: 0,
  eventCheckInLoadSerial: 0,
  desktopHeartbeatTimer: null,
  editingTodoOriginalRecurring: false,
  editingEventOriginalRecurring: false
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
  planningOutline: document.getElementById('planning-outline'),
  planningRootInput: document.getElementById('planning-root-input'),
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

function sortedGroups() {
  return (state.snapshot?.groups || []).slice().sort((a, b) => {
    if (a.isDefault !== b.isDefault) return a.isDefault ? -1 : 1;
    return String(a.name || '').localeCompare(String(b.name || ''), 'zh-CN');
  });
}

function groupById(id) {
  return (state.snapshot?.groups || []).find(group => Number(group.id) === Number(id)) || null;
}

function todoGroupIds(item) {
  const raw = Array.isArray(item?.groupIds) && item.groupIds.length ? item.groupIds : [item?.groupId];
  return Array.from(new Set(raw.map(value => Number(value)).filter(value => Number.isFinite(value) && value > 0)));
}

function todoGroupNames(item) {
  const names = todoGroupIds(item)
    .map(id => groupById(id)?.name)
    .filter(Boolean);
  if (names.length) return names;
  return [item?.groupName || '未分组'];
}

function todoGroupLabel(item) {
  return todoGroupNames(item).join(' / ');
}

function todoGroupAccent(item) {
  const primaryGroup = groupById(todoGroupIds(item)[0]);
  return item?.groupColorHex || primaryGroup?.colorHex || item?.accentColorHex || '#4e87e1';
}

function todoMatchesSelectedGroups(item) {
  const selected = state.selectedTodoGroupIds || [];
  if (!selected.length) return true;
  const ids = new Set(todoGroupIds(item).map(String));
  return selected.every(id => ids.has(String(id)));
}

function normalizeSelectedTodoGroupFilters() {
  const validIds = new Set(sortedGroups().map(group => String(group.id)));
  if (!validIds.size) {
    state.selectedTodoGroupIds = [];
    return;
  }
  state.selectedTodoGroupIds = Array.from(new Set((state.selectedTodoGroupIds || [])
    .map(Number)
    .filter(id => Number.isFinite(id) && id > 0 && validIds.has(String(id)))));
}

function defaultTodoGroupIds() {
  const groups = sortedGroups();
  return groups.length ? [Number(groups[0].id)] : [];
}

function setTodoEditorGroupIds(groupIds) {
  const normalized = Array.from(new Set((groupIds || []).map(Number).filter(value => Number.isFinite(value) && value > 0)));
  const selected = normalized.length ? normalized : defaultTodoGroupIds();
  const primary = selected[0] || 0;
  fillGroupSelect('todo-group', primary);
  const select = document.getElementById('todo-group');
  if (select && primary) select.value = String(primary);
  renderTodoGroupChips(selected);
}

function readTodoEditorGroupIds() {
  const chips = Array.from(document.querySelectorAll('#todo-group-chips [data-todo-group-editor].active'));
  const selected = chips.map(node => Number(node.dataset.todoGroupEditor)).filter(value => Number.isFinite(value) && value > 0);
  if (selected.length) return Array.from(new Set(selected));
  const fallback = Number(document.getElementById('todo-group')?.value || 0);
  return fallback > 0 ? [fallback] : [];
}

function renderTodoGroupChips(selectedIds) {
  const host = document.getElementById('todo-group-chips');
  if (!host) return;
  const selected = new Set((selectedIds || []).map(String));
  const groups = sortedGroups();
  if (!groups.length) {
    host.innerHTML = '<span class="group-chip-empty">暂无分组</span>';
    return;
  }
  host.innerHTML = groups.map(group => {
    const active = selected.has(String(group.id));
    const color = group.colorHex || '#4e87e1';
    return ''
      + '<button type="button" class="group-chip editor-chip' + (active ? ' active' : '') + '" data-todo-group-editor="' + escapeHtml(String(group.id)) + '" style="--chip-color:' + escapeHtml(color) + '">'
      +   '<span class="group-chip-dot"></span>'
      +   '<span>' + escapeHtml(group.name || '未分组') + '</span>'
      + '</button>';
  }).join('');
  host.querySelectorAll('[data-todo-group-editor]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      toggleTodoEditorGroup(node.dataset.todoGroupEditor);
    };
  });
}

function toggleTodoEditorGroup(groupId) {
  const selected = readTodoEditorGroupIds();
  const key = Number(groupId);
  const next = selected.includes(key)
    ? selected.filter(id => id !== key)
    : [...selected, key];
  setTodoEditorGroupIds(next.length ? next : [key]);
}

function renderTodoGroupFilterChips() {
  normalizeSelectedTodoGroupFilters();
  const groups = sortedGroups();
  if (!groups.length) return '';
  const selected = new Set((state.selectedTodoGroupIds || []).map(String));
  return ''
    + '<div class="todo-filter-row" aria-label="待办分组筛选">'
    +   '<button type="button" class="group-chip filter-chip' + (!selected.size ? ' active' : '') + '" data-todo-group-filter="all">'
    +     '<span>全部</span>'
    +   '</button>'
    +   groups.map(group => {
      const color = group.colorHex || '#4e87e1';
      const active = selected.has(String(group.id));
      return ''
        + '<button type="button" class="group-chip filter-chip' + (active ? ' active' : '') + '" data-todo-group-filter="' + escapeHtml(String(group.id)) + '" style="--chip-color:' + escapeHtml(color) + '">'
        +   '<span class="group-chip-dot"></span>'
        +   '<span>' + escapeHtml(group.name || '未分组') + '</span>'
        + '</button>';
    }).join('')
    + '</div>';
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

function formatDurationMinutes(minutes) {
  const total = Math.max(0, Number(minutes || 0));
  const hours = Math.floor(total / 60);
  const rest = total % 60;
  if (hours > 0 && rest > 0) return hours + 'h ' + rest + 'm';
  if (hours > 0) return hours + 'h';
  return rest + 'm';
}

function checkInDurationMinutes(checkIn, now = Date.now()) {
  if (!checkIn) return 0;
  if (checkIn.checkOutAtMillis) return Number(checkIn.durationMinutes || 0);
  const start = Number(checkIn.checkInAtMillis || 0);
  return Math.max(0, Math.floor((now - start) / 60000));
}

function checkInRangeText(checkIn, now = Date.now()) {
  const start = formatTimeLabel(checkIn.checkInAtMillis);
  const duration = formatDurationMinutes(checkInDurationMinutes(checkIn, now));
  if (!checkIn.checkOutAtMillis) return start + '-进行中 · 已 ' + duration;
  return start + '-' + formatTimeLabel(checkIn.checkOutAtMillis) + ' · ' + duration;
}

function countdownTargetMillis(item) {
  if (!item || item.countdownEnabled !== true) return null;
  if (item.itemType === 'EVENT') return item.startAtMillis || item.dueAtMillis || null;
  return item.hasDueDate ? item.dueAtMillis : null;
}

function countdownDays(item, boardDateKey) {
  const target = countdownTargetMillis(item);
  if (!target) return null;
  const start = dayStartMillis(boardDateKey || dayKey(new Date()));
  const targetKey = dayKeyFromMillis(target);
  const targetStart = dayStartMillis(targetKey);
  return Math.round((targetStart - start) / (24 * 60 * 60 * 1000));
}

function renderSummaryCard(label, value) {
  return '<div class="summary-card"><div class="summary-label">' + escapeHtml(label) + '</div><div class="summary-value">' + value + '</div></div>';
}

function renderTodoItem(item) {
  const accent = todoGroupAccent(item);
  const marker = todoMarker(item);
  const meta = [todoGroupLabel(item), item.location || '', item.isRecurring ? '循环' : ''].filter(Boolean).join(' · ');
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

function renderTodoPagerControls() {
  const loaded = state.snapshot?.todos?.length || 0;
  const total = state.todoTotal || loaded;
  const visibleLoaded = (state.snapshot?.todos || []).filter(todoMatchesSelectedGroups).length;
  const query = state.todoQuery || '';
  const hasSearch = query.trim().length > 0;
  const canLoadMore = state.todoHasMore && !state.todoLoading;
  return ''
    + renderTodoGroupFilterChips()
    + '<div class="todo-data-toolbar">'
    +   '<div class="todo-search-row">'
    +     '<input type="search" data-todo-search-input="true" value="' + escapeHtml(query) + '" placeholder="搜索标题、备注或地点" />'
    +     '<button type="button" class="ghost mini" data-search-todos="true">' + (state.todoLoading ? '加载中' : '搜索') + '</button>'
    +     (hasSearch ? '<button type="button" class="ghost mini" data-clear-todo-search="true">清空</button>' : '')
    +   '</div>'
    +   '<div class="todo-page-meta">'
    +     '显示 ' + visibleLoaded + ' / 已加载 ' + loaded + ' / 共 ' + total + ' 条'
    +     (hasSearch ? ' · 搜索：' + escapeHtml(query) : '')
    +   '</div>'
    +   '<button type="button" class="ghost mini todo-load-more" data-load-more-todos="true" ' + (canLoadMore ? '' : 'disabled') + '>'
    +     (state.todoLoading ? '加载中…' : (state.todoHasMore ? '加载更多' : '没有更多'))
    +   '</button>'
    + '</div>';
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
  els.viewCaption.textContent = todoMode ? '桌面端每日看板' : eventMode ? '桌面端日程模式' : '大纲草稿 / Markdown 兼容';
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
  startDesktopHeartbeat();
  els.status.textContent = '已连接';
}

function stopDesktopHeartbeat() {
  if (state.desktopHeartbeatTimer != null) {
    window.clearInterval(state.desktopHeartbeatTimer);
    state.desktopHeartbeatTimer = null;
  }
}

function startDesktopHeartbeat() {
  stopDesktopHeartbeat();
  state.desktopHeartbeatTimer = window.setInterval(async () => {
    try {
      await api('/api/status');
    } catch (error) {
      stopDesktopHeartbeat();
      els.status.textContent = '电脑同步连接已断开，请在手机端重新开启后再连接';
    }
  }, 60 * 1000);
}

async function loadSnapshot(options = {}) {
  const shouldLoadPlanning = options.planning === true || state.currentTab === 'planning';
  state.snapshot = await api('/api/snapshot?scope=board');
  state.todosLoaded = false;
  resetTodoPageState();
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

function resetTodoPageState() {
  state.todoOffset = 0;
  state.todoTotal = 0;
  state.todoHasMore = false;
  state.todoLoading = false;
}

function mergeTodosById(existing, incoming) {
  const merged = [];
  const seen = new Set();
  [...(existing || []), ...(incoming || [])].forEach(item => {
    const key = String(item.id ?? '');
    if (!key || seen.has(key)) return;
    seen.add(key);
    merged.push(item);
  });
  return merged;
}

async function loadDesktopTodos(options = {}) {
  const reset = options.reset !== false;
  if (state.todoLoading) return;
  state.todoLoading = true;
  try {
    const offset = reset ? 0 : state.todoOffset;
    const query = state.todoQuery.trim();
    const params = new URLSearchParams({
      offset: String(offset),
      limit: String(TODO_PAGE_LIMIT)
    });
    if (query) params.set('q', query);
    const data = await api('/api/todos?' + params.toString());
    ensureSnapshotContainer();
    mergeSnapshotGroups(data.groups || []);
    const incoming = data.todos || [];
    state.snapshot.todos = reset ? incoming : mergeTodosById(state.snapshot.todos, incoming);
    state.snapshot.generatedAtMillis = data.generatedAtMillis || Date.now();
    state.todoOffset = Number(data.offset || offset) + incoming.length;
    state.todoTotal = Number(data.total || state.snapshot.todos.length);
    state.todoHasMore = Boolean(data.hasMore);
    state.todosLoaded = true;
  } finally {
    state.todoLoading = false;
  }
  if (options.render !== false) {
    renderTodos();
    syncTopbar();
  }
}

async function ensureTodoData() {
  if (state.todosLoaded) return;
  els.status.textContent = '正在加载待办管理列表…';
  await loadDesktopTodos({ reset: true });
  els.status.textContent = '已加载待办管理列表';
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
  await loadPlanningNodes();
  await loadPlanningMappings();
}

async function loadPlanningNodes() {
  const active = activePlanningNote();
  if (!active) {
    state.planningNodes = [];
    state.planningNodesMarkdown = '';
    state.planningNodesLoaded = false;
    return;
  }
  const data = await api('/api/planning/nodes?noteId=' + encodeURIComponent(active.id));
  state.planningNodes = data.nodes || [];
  state.planningNodesMarkdown = data.markdown || '';
  state.planningNodesLoaded = true;
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
    const visibleTodayEvents = board.visibleTodayEvents || [];
    const tomorrowEvents = board.tomorrowEvents || [];
    els.todoSummary.innerHTML = [
      renderSummaryCard('今日待办', todoItems.length),
      renderSummaryCard('今日日程', visibleTodayEvents.length),
      renderSummaryCard('明日日程', tomorrowEvents.length),
      renderSummaryCard('加载模式', '轻量')
    ].join('');
    els.todoTimeline.innerHTML = ''
      + '<div class="empty-state">'
      +   '<strong>当前仅加载每日看板数据。</strong><br />'
      +   '为了减少电脑端首屏读取压力，完整待办时间轴会按需加载。'
      +   '<div class="actions"><button type="button" class="ghost mini" data-load-todos="true">加载待办管理列表</button></div>'
      + '</div>';
    bindActions();
    return;
  }
  const now = Date.now();
  const today = dayKey(new Date());
  const activeAll = activeTodos();
  const historyAll = historyTodos();
  const active = activeAll.filter(todoMatchesSelectedGroups).slice().sort((a, b) => (a.hasDueDate ? (a.dueAtMillis || Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER) - (b.hasDueDate ? (b.dueAtMillis || Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER));
  const history = historyAll.filter(todoMatchesSelectedGroups).slice().sort((a, b) => (b.completedAtMillis || b.canceledAtMillis || b.missedAtMillis || b.dueAtMillis || 0) - (a.completedAtMillis || a.canceledAtMillis || a.missedAtMillis || a.dueAtMillis || 0));
  const sections = [
    { key: 'missed', title: '已错过', empty: '当前没有已错过的待办。', items: active.filter(item => item.missed || (item.hasDueDate && (item.dueAtMillis || 0) < now && dayKeyFromMillis(item.dueAtMillis) < today)) },
    { key: 'today', title: '今日待办', empty: '今天暂时没有待办。', items: active.filter(item => !item.missed && (!item.hasDueDate || dayKeyFromMillis(item.dueAtMillis) === today)) },
    { key: 'upcoming', title: '计划中', empty: '后续暂时没有排期。', items: active.filter(item => item.hasDueDate && !item.missed && dayKeyFromMillis(item.dueAtMillis) > today) },
    { key: 'history', title: '历史记录', empty: '暂无历史记录。', items: history }
  ];
  els.todoSummary.innerHTML = [
    renderSummaryCard('已加载活动', activeAll.length),
    renderSummaryCard('已加载今日', sections[1].items.length),
    renderSummaryCard('已加载历史', historyAll.length),
    renderSummaryCard('总匹配', state.todoTotal || state.snapshot?.todos?.length || 0)
  ].join('');
  renderDesktopDailyBoard();
  els.todoTimeline.innerHTML = ''
    + renderTodoPagerControls()
    + sections.map(renderTodoSection).join('')
    + (state.todoHasMore ? '<div class="todo-bottom-more"><button type="button" class="ghost mini" data-load-more-todos="true">加载更多待办</button></div>' : '');
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
  const countdownItems = board.countdownItems || [];
  const allTodayEvents = board.allTodayEvents || [];
  const visibleTodayEvents = board.visibleTodayEvents || [];
  const tomorrowEvents = board.tomorrowEvents || [];
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
    + '</section>'
    + '<section class="desktop-board-grid">'
    +   renderBoardCountdownCard(countdownItems, board.date || dayKey(new Date()))
    +   renderBoardTodoCard(todoItems)
    +   renderBoardScheduleCard(board.date || dayKey(new Date()), nowMillis, allTodayEvents, visibleTodayEvents, tomorrowEvents)
    + '</section>';
}

function renderBoardNowCard(nowMillis, visibleTodayEvents, todoItems) {
  const currentEvent = visibleTodayEvents.find(item => eventStart(item) <= nowMillis && nowMillis < eventEnd(item));
  if (currentEvent) {
    return ''
      + '<button type="button" class="desktop-now-card active" data-event-preview-id="' + escapeHtml(String(currentEvent.id ?? '')) + '" style="--accent:' + escapeHtml(currentEvent.accentColorHex || currentEvent.groupColorHex || '#FFC94A') + '">'
      +   '<span>正在进行</span>'
      +   '<strong>' + escapeHtml(currentEvent.title || '未命名日程') + '</strong>'
      +   '<small>' + escapeHtml(eventTimeText(currentEvent)) + '</small>'
      + '</button>';
  }
  const nextEvent = visibleTodayEvents.find(item => eventStart(item) > nowMillis);
  if (nextEvent) {
    return ''
      + '<button type="button" class="desktop-now-card" data-event-preview-id="' + escapeHtml(String(nextEvent.id ?? '')) + '" style="--accent:' + escapeHtml(nextEvent.accentColorHex || nextEvent.groupColorHex || '#4e87e1') + '">'
      +   '<span>下一项日程</span>'
      +   '<strong>' + escapeHtml(nextEvent.title || '未命名日程') + '</strong>'
      +   '<small>' + escapeHtml(eventTimeText(nextEvent)) + '</small>'
      + '</button>';
  }
  const nextTodo = todoItems.find(item => !item.missed) || todoItems[0];
  if (nextTodo) {
    return ''
      + '<button type="button" class="desktop-now-card" data-todo-id="' + escapeHtml(String(nextTodo.id ?? '')) + '" style="--accent:' + escapeHtml(todoGroupAccent(nextTodo)) + '">'
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

function renderBoardCountdownCard(items, boardDateKey) {
  const visible = (items || []).filter(item => countdownDays(item, boardDateKey) != null && countdownDays(item, boardDateKey) >= 0).slice(0, 5);
  if (!visible.length) return '';
  return ''
    + '<article class="desktop-board-card countdown">'
    +   '<div class="desktop-board-card-head"><h3>倒数日</h3><span>' + visible.length + ' 项</span></div>'
    +   '<div class="desktop-board-list">' + visible.map(item => renderBoardCountdownRow(item, boardDateKey)).join('') + '</div>'
    + '</article>';
}

function renderBoardCountdownRow(item, boardDateKey) {
  const accent = item.itemType === 'EVENT' ? (item.accentColorHex || item.groupColorHex || '#4e87e1') : todoGroupAccent(item);
  const target = countdownTargetMillis(item);
  const remain = target ? remainingCountdownDisplay(target) : { primary: '--', secondary: '' };
  const rowAttr = item.itemType === 'EVENT'
    ? 'data-event-preview-id="' + escapeHtml(String(item.id ?? '')) + '"'
    : 'data-todo-id="' + escapeHtml(String(item.id ?? '')) + '"';
  const meta = item.itemType === 'EVENT'
    ? eventCountdownTimeText(item)
    : [target ? ('DDL ' + formatDateTimeLabel(target)) : '', todoGroupLabel(item)].filter(Boolean).join(' · ');
  return ''
    + '<button type="button" class="desktop-board-row countdown" ' + rowAttr + ' style="--accent:' + escapeHtml(accent) + '">'
    +   '<span class="desktop-board-strip"></span>'
    +   '<span class="desktop-board-row-main">'
    +     '<strong>' + escapeHtml(item.title || '未命名目标') + '</strong>'
    +     '<small>' + escapeHtml(meta) + '</small>'
    +   '</span>'
    +   '<span class="desktop-board-chip">' + escapeHtml(remain.primary) + (remain.secondary ? '<small>' + escapeHtml(remain.secondary) + '</small>' : '') + '</span>'
    + '</button>';
}

function remainingCountdownDisplay(targetMillis) {
  const remaining = Math.max(0, Number(targetMillis || 0) - Date.now());
  const totalMinutes = Math.floor(remaining / 60000);
  if (totalMinutes >= 1440) {
    const days = Math.floor(totalMinutes / 1440);
    const rest = totalMinutes % 1440;
    return { primary: days + 'd', secondary: Math.floor(rest / 60) + 'h ' + (rest % 60) + 'm' };
  }
  if (totalMinutes >= 60) {
    return { primary: Math.floor(totalMinutes / 60) + 'h', secondary: (totalMinutes % 60) + 'm' };
  }
  return { primary: totalMinutes + 'm', secondary: '' };
}

function eventCountdownTimeText(item) {
  const start = item.startAtMillis || item.dueAtMillis;
  const end = item.endAtMillis;
  if (!start) return '未设置时间';
  if (item.allDay) return formatShortDateLabel(start) + ' 全天';
  if (end && !sameLocalDate(start, end)) return formatDateTimeLabel(start) + '-' + formatDateTimeLabel(end);
  if (end) return formatDateTimeLabel(start) + '-' + formatTimeLabel(end);
  return formatDateTimeLabel(start);
}

function sameLocalDate(leftMillis, rightMillis) {
  const left = new Date(leftMillis);
  const right = new Date(rightMillis);
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth() && left.getDate() === right.getDate();
}

function renderBoardTodoRow(item) {
  const accent = todoGroupAccent(item);
  const status = item.missed ? '已错过' : (item.hasDueDate ? formatTimeLabel(item.dueAtMillis) : '无 DDL');
  const meta = [todoGroupLabel(item), item.hasDueDate ? ('DDL ' + formatDateTimeLabel(item.dueAtMillis)) : '', item.notes || ''].filter(Boolean).join(' · ');
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
      +       '<div class="desktop-board-card-head"><h3>今日日程</h3><span>' + visibleTodayEvents.length + ' 项</span></div>'
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
    + '<button type="button" class="' + classes + '" data-event-preview-id="' + escapeHtml(String(item.id ?? '')) + '" style="--accent:' + escapeHtml(accent) + '">'
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
    if (activeChanged) state.planningParseMarkdown = '';
    if (activeChanged || !state.planningDirty) {
      els.planningEditor.value = state.planningNodesMarkdown || active.contentMarkdown || '';
      state.planningDirty = false;
    }
    if (els.planningActiveTitle) els.planningActiveTitle.textContent = active.title || '我的规划';
    state.planningRenderedNoteId = active.id;
  } else if (els.planningActiveTitle) {
    els.planningActiveTitle.textContent = '未选择规划文档';
  }
  const undoButton = document.getElementById('planning-undo');
  if (undoButton) undoButton.disabled = !latestPlanningUndoSummary();
  renderPlanningOutline();
  renderPlanningPreview();
}

function applyPlanningNodeResponse(data) {
  if (!data) return;
  if (Array.isArray(data.nodes)) state.planningNodes = data.nodes;
  if (typeof data.markdown === 'string') state.planningNodesMarkdown = data.markdown;
  refreshPlanningMarkdownEditor(state.planningNodesMarkdown || '');
  renderPlanningOutline();
}

function planningNodeById(id) {
  return (state.planningNodes || []).find(node => sameId(node.id, id)) || null;
}

function planningSiblingNodes(parentNodeId) {
  return (state.planningNodes || [])
    .filter(node => parentNodeId == null ? node.parentNodeId == null : sameId(node.parentNodeId, parentNodeId))
    .sort((a, b) => (Number(a.sortOrder || 0) - Number(b.sortOrder || 0)) || (Number(a.id || 0) - Number(b.id || 0)));
}

function flattenPlanningNodes(nodes) {
  const children = new Map();
  (nodes || []).forEach(node => {
    const key = node.parentNodeId == null ? 'root' : String(node.parentNodeId);
    if (!children.has(key)) children.set(key, []);
    children.get(key).push(node);
  });
  children.forEach(list => list.sort((a, b) => (Number(a.sortOrder || 0) - Number(b.sortOrder || 0)) || (Number(a.id || 0) - Number(b.id || 0))));
  const result = [];
  const visiting = new Set();
  const append = (node, depth) => {
    if (!node || visiting.has(String(node.id))) return;
    visiting.add(String(node.id));
    const childList = children.get(String(node.id)) || [];
    result.push({ node, depth, hasChildren: childList.length > 0 });
    if (!node.collapsed) childList.forEach(child => append(child, Math.min(depth + 1, 8)));
    visiting.delete(String(node.id));
  };
  (children.get('root') || []).forEach(node => append(node, 0));
  return result;
}

function planningNodeMetaText(node) {
  if (!node) return '';
  const location = String(node.location || '').trim();
  const draft = node.isDraft ? '草稿' : '';
  if (node.syncEnabled === false) {
    return [draft, '结构标题', location].filter(Boolean).join(' · ');
  }
  let time = '';
  if (node.startAtMillis && node.endAtMillis) {
    time = '日程 ' + planningNodeDateTimeValue(node, 'startAt').replace('T', ' ') + ' - ' + planningNodeDateTimeValue(node, 'endAt').replace('T', ' ');
  } else if (node.dueAtMillis) {
    time = 'DDL ' + planningNodeDateTimeValue(node, 'dueAt').replace('T', ' ');
  } else {
    time = '无 DDL 待办';
  }
  return [draft, time, location].filter(Boolean).join(' · ');
}

function planningNodeDateTimeValue(node, field) {
  if (!node) return '';
  const textValue = node[field];
  if (textValue) return editableDateTimeValue(textValue);
  const millis = node[field + 'Millis'];
  return millis ? formatDateTimeLocalValue(Number(millis)) : '';
}

function planningNodeHeaderHint(text) {
  const value = String(text || '');
  const hasTime = /\d{1,2}[:：]\d{2}|ddl|截止|今天|明天|后天|周[一二三四五六日天]/i.test(value);
  const hasLocation = /@\S+|地点[:：]/.test(value);
  const token = (label, active) => '<span class="' + (active ? 'active' : '') + '">' + escapeHtml(label) + '</span>';
  return '<div class="planning-node-hint">' + token('时间', hasTime) + '<b>|</b>' + token('事项', value.trim().length > 0) + '<b>|</b>' + token('地点', hasLocation) + '</div>';
}

function renderPlanningOutline() {
  if (!els.planningOutline) return;
  const active = activePlanningNote();
  const draftCount = (state.planningNodes || []).filter(node => node.isDraft).length;
  const publishAll = document.getElementById('planning-publish-all');
  if (publishAll) {
    publishAll.disabled = !active || draftCount === 0;
    publishAll.textContent = draftCount > 0 ? '发布' + draftCount + '条' : '发布草稿';
  }
  if (!active) {
    els.planningOutline.innerHTML = '<button type="button" class="planning-outline-empty" data-focus-planning-root="true">请先新建或打开一个规划文档。</button>';
    autosizePlanningTextarea(els.planningRootInput);
    return;
  }
  const flattened = flattenPlanningNodes(state.planningNodes || []);
  if (!flattened.length) {
    els.planningOutline.innerHTML = '<button type="button" class="planning-outline-empty" data-focus-planning-root="true">当前文档还没有大纲事项。点这里或上方输入框，写下第一件事：10:00-12:00 写论文 @图书馆3楼，或 任务M ddl 15:00。</button>';
    autosizePlanningTextarea(els.planningRootInput);
    return;
  }
  els.planningOutline.innerHTML = flattened.map(item => {
    const node = item.node;
    const completedClass = node.completed ? ' completed' : '';
    const draftClass = node.isDraft ? ' draft' : '';
    const hasChildrenClass = item.hasChildren ? ' has-children' : '';
    const collapseText = item.hasChildren ? (node.collapsed ? '▶' : '▼') : '·';
    const siblings = planningSiblingNodes(node.parentNodeId);
    const siblingIndex = siblings.findIndex(sibling => sameId(sibling.id, node.id));
    const moveUpDisabled = siblingIndex <= 0 ? ' disabled' : '';
    const moveDownDisabled = siblingIndex < 0 || siblingIndex >= siblings.length - 1 ? ' disabled' : '';
    const dueAt = planningNodeDateTimeValue(node, 'dueAt').replace('T', ' ');
    const startAt = planningNodeDateTimeValue(node, 'startAt').replace('T', ' ');
    const endAt = planningNodeDateTimeValue(node, 'endAt').replace('T', ' ');
    const syncChecked = node.syncEnabled === false ? '' : ' checked';
    return ''
      + '<article class="planning-node-row' + completedClass + draftClass + hasChildrenClass + '" style="--depth:' + Number(item.depth || 0) + '" data-planning-node-row="' + escapeHtml(node.id) + '">'
      +   planningNodeHeaderHint(node.text)
      +   '<div class="planning-node-main">'
      +     '<button type="button" class="planning-node-drag" draggable="true" data-node-drag="' + escapeHtml(node.id) + '" title="拖拽调整同层级顺序">⋮⋮</button>'
      +     '<button type="button" class="planning-node-icon" data-node-collapse="' + escapeHtml(node.id) + '"' + (item.hasChildren ? '' : ' disabled') + '>' + escapeHtml(collapseText) + '</button>'
      +     '<button type="button" class="planning-node-check" data-node-complete="' + escapeHtml(node.id) + '">' + (node.completed ? '✓' : '○') + '</button>'
      +     '<textarea class="planning-node-text" data-node-text="' + escapeHtml(node.id) + '" rows="1" placeholder="写下事项、DDL 或日程">' + escapeHtml(node.text || '') + '</textarea>'
      +     '<span class="planning-node-order">'
      +       '<button type="button" class="ghost mini planning-node-move" data-node-move-up="' + escapeHtml(node.id) + '"' + moveUpDisabled + ' title="上移">↑</button>'
      +       '<button type="button" class="ghost mini planning-node-move" data-node-move-down="' + escapeHtml(node.id) + '"' + moveDownDisabled + ' title="下移">↓</button>'
      +     '</span>'
      +     '<button type="button" class="ghost mini" data-node-child="' + escapeHtml(node.id) + '">子项</button>'
      +     (node.isDraft ? '<button type="button" class="mini planning-node-publish" data-node-publish="' + escapeHtml(node.id) + '">发布</button>' : '')
      +     '<button type="button" class="ghost mini danger-lite-action" data-node-delete="' + escapeHtml(node.id) + '">删除</button>'
      +   '</div>'
      +   '<div class="planning-node-meta">' + escapeHtml(planningNodeMetaText(node)) + '</div>'
      +   '<div class="planning-node-fields">'
      +     '<label class="planning-node-sync"><input type="checkbox" data-node-sync="' + escapeHtml(node.id) + '"' + syncChecked + ' />同步为待办/日程</label>'
      +     '<label>DDL<input data-node-field="dueAt" data-node-id="' + escapeHtml(node.id) + '" value="' + escapeHtml(dueAt) + '" placeholder="明天 16:30 / 5.28 23:59" /></label>'
      +     '<label>开始<input data-node-field="startAt" data-node-id="' + escapeHtml(node.id) + '" value="' + escapeHtml(startAt) + '" placeholder="10:00" /></label>'
      +     '<label>结束<input data-node-field="endAt" data-node-id="' + escapeHtml(node.id) + '" value="' + escapeHtml(endAt) + '" placeholder="12:00" /></label>'
      +     '<label>地点<input data-node-field="location" data-node-id="' + escapeHtml(node.id) + '" value="' + escapeHtml(node.location || '') + '" placeholder="@图书馆3楼" /></label>'
      +   '</div>'
      + '</article>';
  }).join('');
  autosizePlanningTextareas(els.planningOutline);
  autosizePlanningTextarea(els.planningRootInput);
}

function autosizePlanningTextarea(node) {
  if (!node || node.tagName !== 'TEXTAREA') return;
  const minHeight = node.id === 'planning-root-input' ? 58 : 38;
  node.style.height = 'auto';
  node.style.height = Math.max(minHeight, node.scrollHeight) + 'px';
}

function autosizePlanningTextareas(root = document) {
  root.querySelectorAll?.('textarea[data-node-text], #planning-root-input').forEach(autosizePlanningTextarea);
}

function trackPlanningCommit(promise) {
  const tracked = Promise.resolve(promise)
    .catch(error => {
      els.status.textContent = error.message;
      return null;
    })
    .finally(() => {
      state.planningPendingCommits = state.planningPendingCommits.filter(item => item !== tracked);
    });
  state.planningPendingCommits.push(tracked);
  return tracked;
}

async function waitPlanningPendingCommits() {
  if (!state.planningPendingCommits.length) return;
  await Promise.all(state.planningPendingCommits.slice());
}

async function flushPlanningOutlineInputs(options = {}) {
  await waitPlanningPendingCommits();
  const createRoot = options.createRoot !== false;
  const rootText = String(els.planningRootInput?.value || '').trim();
  if (createRoot && rootText) {
    await createPlanningOutlineNodesFromText(rootText);
    if (els.planningRootInput) {
      els.planningRootInput.value = '';
      autosizePlanningTextarea(els.planningRootInput);
    }
  }
  await waitPlanningPendingCommits();
}

function currentPlanningMarkdownForAction() {
  if (state.planningDirty) return els.planningEditor?.value || '';
  return state.planningNodesMarkdown || els.planningEditor?.value || '';
}

function refreshPlanningMarkdownEditor(markdown = state.planningNodesMarkdown || '') {
  if (!els.planningEditor || state.planningDirty) return;
  els.planningEditor.value = markdown;
}

async function createPlanningOutlineNode(text, parentNodeId = null, options = {}) {
  const active = activePlanningNote();
  const clean = String(text || '').trim();
  if (!active) throw new Error('没有可写入的规划文档');
  if (!clean) throw new Error('事项不能为空');
  const body = { noteId: active.id, parentNodeId, text: clean };
  if (Number.isFinite(Number(options.sortOrder))) body.sortOrder = Number(options.sortOrder);
  const data = await api('/api/planning/nodes/create', {
    method: 'POST',
    body: JSON.stringify(body)
  });
  applyPlanningNodeResponse(data);
  await refreshAfterMutation();
  els.status.textContent = '已添加草稿到规划台';
  if (data.node?.id) focusPlanningNodeText(data.node.id, String(data.node.text || clean).length);
  return data.node || null;
}

async function createPlanningOutlineNodesFromText(text, parentNodeId = null, options = {}) {
  const lines = String(text || '')
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean);
  const entries = lines.length > 1 ? lines : [String(text || '').trim()];
  let lastNode = null;
  let nextSortOrder = Number(options.sortOrder);
  for (const entry of entries) {
    const entryOptions = Number.isFinite(nextSortOrder) ? { ...options, sortOrder: nextSortOrder++ } : options;
    lastNode = await createPlanningOutlineNode(entry, parentNodeId, entryOptions);
  }
  return lastNode;
}

async function updatePlanningOutlineNode(nodeId, patch) {
  const existing = planningNodeById(nodeId);
  if (!existing) throw new Error('规划节点不存在');
  const data = await api('/api/planning/nodes/update', {
    method: 'POST',
    body: JSON.stringify({ id: existing.id, ...patch })
  });
  applyPlanningNodeResponse(data);
  await refreshAfterMutation();
  els.status.textContent = existing.isDraft ? '规划草稿已更新' : '规划节点已同步';
}

async function publishPlanningOutlineNode(nodeId) {
  const existing = planningNodeById(nodeId);
  if (!existing) throw new Error('规划节点不存在');
  const data = await api('/api/planning/nodes/' + encodeURIComponent(existing.id) + '/publish', {
    method: 'POST',
    body: JSON.stringify({})
  });
  applyPlanningNodeResponse(data);
  await refreshAfterMutation();
  els.status.textContent = '已发布 1 条草稿';
}

async function publishAllPlanningDrafts() {
  const active = activePlanningNote();
  if (!active) throw new Error('没有可发布的规划文档');
  const data = await api('/api/planning/nodes/publish-all', {
    method: 'POST',
    body: JSON.stringify({ noteId: active.id })
  });
  applyPlanningNodeResponse(data);
  await refreshAfterMutation();
  els.status.textContent = '已发布 ' + (data.publishedCount || 0) + ' 条，' + (data.failedCount || 0) + ' 条失败';
}

async function deletePlanningOutlineNode(nodeId, options = {}) {
  const existing = planningNodeById(nodeId);
  if (!existing) return;
  if (options.confirm !== false) {
    if (!await confirmDanger('确认删除规划事项', '会同时删除它和子项关联的待办 / 日程：' + (existing.text || ''), '删除')) return;
  }
  const data = await api('/api/planning/nodes/delete', {
    method: 'POST',
    body: JSON.stringify({ id: existing.id })
  });
  applyPlanningNodeResponse(data);
  await refreshAfterMutation();
  els.status.textContent = '规划事项已删除';
}

async function commitPlanningNodeText(input) {
  const nodeId = input?.dataset?.nodeText;
  const existing = planningNodeById(nodeId);
  if (!existing) return;
  const next = String(input.value || '').trim();
  if (!next) {
    await deletePlanningOutlineNode(nodeId);
    return;
  }
  if (next !== String(existing.text || '').trim()) {
    await updatePlanningOutlineNode(nodeId, { text: next });
  }
}

async function commitPlanningNodeField(input) {
  const nodeId = input?.dataset?.nodeId;
  const field = input?.dataset?.nodeField;
  const existing = planningNodeById(nodeId);
  if (!existing || !field) return;
  const value = String(input.value || '').trim();
  const patch = {};
  if (['dueAt', 'startAt', 'endAt'].includes(field)) {
    const parsedMillis = value ? parseLocalDateTimeMillis(value, new Date()) : null;
    patch[field] = value ? (parsedMillis == null ? value.replace(' ', 'T') : formatDateTimeLocalValue(parsedMillis)) : null;
  } else if (field === 'location') {
    patch.location = value || null;
  }
  await updatePlanningOutlineNode(nodeId, patch);
}

function previousVisiblePlanningNode(nodeId) {
  const flattened = flattenPlanningNodes(state.planningNodes || []);
  const index = flattened.findIndex(item => sameId(item.node.id, nodeId));
  return index > 0 ? flattened[index - 1].node : null;
}

function nextVisiblePlanningNode(nodeId) {
  const flattened = flattenPlanningNodes(state.planningNodes || []);
  const index = flattened.findIndex(item => sameId(item.node.id, nodeId));
  return index >= 0 ? (flattened[index + 1]?.node || null) : null;
}

function previousSiblingPlanningNode(nodeId) {
  const existing = planningNodeById(nodeId);
  if (!existing) return null;
  const siblings = planningSiblingNodes(existing.parentNodeId);
  const index = siblings.findIndex(node => sameId(node.id, existing.id));
  return index > 0 ? siblings[index - 1] : null;
}

function focusPlanningNodeText(nodeId, cursor = null) {
  window.requestAnimationFrame(() => {
    const input = Array.from(document.querySelectorAll('[data-node-text]'))
      .find(node => sameId(node.dataset.nodeText, nodeId));
    if (!input) return;
    const position = Math.max(0, Math.min(Number(cursor ?? input.value.length), input.value.length));
    input.focus();
    input.setSelectionRange(position, position);
  });
}

function focusPlanningRootInput(cursor = null) {
  window.requestAnimationFrame(() => {
    if (!els.planningRootInput) return;
    const position = Math.max(0, Math.min(Number(cursor ?? els.planningRootInput.value.length), els.planningRootInput.value.length));
    els.planningRootInput.focus();
    els.planningRootInput.setSelectionRange(position, position);
  });
}

async function mergePlanningTextIntoPrevious(previous, text, afterMerge) {
  const clean = String(text || '').replace(/^\s+/, '');
  if (!previous || !clean.trim()) return false;
  const cursor = String(previous.text || '').length;
  await updatePlanningOutlineNode(previous.id, { text: String(previous.text || '') + clean });
  if (afterMerge) await afterMerge();
  focusPlanningNodeText(previous.id, cursor);
  return true;
}

async function splitPlanningNodeAtCursor(input) {
  const node = planningNodeById(input?.dataset?.nodeText);
  if (!node) return false;
  const start = input.selectionStart ?? input.value.length;
  const end = input.selectionEnd ?? start;
  const before = input.value.slice(0, start).trim();
  const after = input.value.slice(end).trim();
  if (!before || !after) return false;
  input.dataset.skipCommit = 'true';
  await updatePlanningOutlineNode(node.id, { text: before });
  const sortOrder = Number(node.sortOrder || 0) + 1;
  const created = await createPlanningOutlineNode(after, node.parentNodeId ?? null, { sortOrder });
  focusPlanningNodeText(created?.id, 0);
  return true;
}

async function mergePlanningNodeWithPrevious(input) {
  const nodeId = input?.dataset?.nodeText;
  const previous = previousSiblingPlanningNode(nodeId);
  if (!previous) return false;
  input.dataset.skipCommit = 'true';
  return mergePlanningTextIntoPrevious(previous, input.value, async () => {
    await deletePlanningOutlineNode(nodeId, { confirm: false });
  });
}

async function indentPlanningNode(nodeId, outdent = false) {
  const existing = planningNodeById(nodeId);
  if (!existing) return;
  if (outdent) {
    const parent = existing.parentNodeId == null ? null : planningNodeById(existing.parentNodeId);
    await updatePlanningOutlineNode(nodeId, { parentNodeId: parent ? parent.parentNodeId : null });
    return;
  }
  const previous = previousVisiblePlanningNode(nodeId);
  if (!previous || sameId(previous.id, existing.id)) return;
  await updatePlanningOutlineNode(nodeId, { parentNodeId: previous.id });
}

async function movePlanningNode(nodeId, direction) {
  const existing = planningNodeById(nodeId);
  const active = activePlanningNote();
  if (!existing || !active) return;
  const siblings = planningSiblingNodes(existing.parentNodeId);
  const index = siblings.findIndex(node => sameId(node.id, existing.id));
  const targetIndex = index + direction;
  if (index < 0 || targetIndex < 0 || targetIndex >= siblings.length) return;
  const ordered = siblings.map(node => node.id);
  [ordered[index], ordered[targetIndex]] = [ordered[targetIndex], ordered[index]];
  const data = await api('/api/planning/nodes/reorder', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      parentNodeId: existing.parentNodeId ?? null,
      orderedNodeIds: ordered
    })
  });
  applyPlanningNodeResponse(data);
  await refreshAfterMutation();
  els.status.textContent = '规划节点顺序已更新';
}

async function reorderPlanningNodeNear(draggedId, targetId, insertAfter = false) {
  const dragged = planningNodeById(draggedId);
  const target = planningNodeById(targetId);
  const active = activePlanningNote();
  if (!dragged || !target || !active || sameId(dragged.id, target.id)) return;
  const sameParent = dragged.parentNodeId == null
    ? target.parentNodeId == null
    : sameId(dragged.parentNodeId, target.parentNodeId);
  if (!sameParent) {
    els.status.textContent = '只能在同一层级内拖拽排序';
    return;
  }
  const siblings = planningSiblingNodes(dragged.parentNodeId);
  const ordered = siblings.map(node => node.id).filter(id => !sameId(id, dragged.id));
  const targetIndex = ordered.findIndex(id => sameId(id, target.id));
  if (targetIndex < 0) return;
  ordered.splice(targetIndex + (insertAfter ? 1 : 0), 0, dragged.id);
  const data = await api('/api/planning/nodes/reorder', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      parentNodeId: dragged.parentNodeId ?? null,
      orderedNodeIds: ordered
    })
  });
  applyPlanningNodeResponse(data);
  await refreshAfterMutation();
  els.status.textContent = '规划节点顺序已更新';
}

function planningDragNodeId(event) {
  return state.planningDraggingNodeId
    || event.dataTransfer?.getData('application/x-paykitodo-planning-node')
    || event.dataTransfer?.getData('text/plain')
    || '';
}

function clearPlanningDragTargets() {
  document.querySelectorAll('.planning-node-row.drag-over, .planning-node-row.drag-after').forEach(row => {
    row.classList.remove('drag-over', 'drag-after');
  });
}

function clearPlanningDragState() {
  state.planningDraggingNodeId = null;
  document.querySelectorAll('.planning-node-row.dragging, .planning-node-row.drag-over, .planning-node-row.drag-after').forEach(row => {
    row.classList.remove('dragging', 'drag-over', 'drag-after');
  });
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

function planningEditableSelect(id, field, label, value, options) {
  return '<label class="planning-edit-field"><span>' + escapeHtml(label) + '</span><select data-planning-field="' + escapeHtml(field) + '" data-planning-id="' + escapeHtml(id) + '">'
    + options.map(option => '<option value="' + escapeHtml(option.value) + '"' + (option.value === value ? ' selected' : '') + '>' + escapeHtml(option.label) + '</option>').join('')
    + '</select></label>';
}

function planningRecurrenceFields(item) {
  const recurrence = item.recurrence || {};
  const type = recurrence.enabled ? (recurrence.type || 'NONE') : 'NONE';
  const weeklyDays = Array.isArray(recurrence.weeklyDays) ? recurrence.weeklyDays.join(',') : '';
  return ''
    + planningEditableSelect(item.id, 'recurrenceType', '重复', type, [
      { value: 'NONE', label: '不重复' },
      { value: 'DAILY', label: '每天' },
      { value: 'WEEKLY', label: '每周' },
      { value: 'MONTHLY_NTH_WEEKDAY', label: '每月第几个星期几' },
      { value: 'MONTHLY_DAY', label: '每月D日' },
      { value: 'YEARLY_DATE', label: '每年M月D日' },
      { value: 'YEARLY_LUNAR_DATE', label: '每年同农历月日' }
    ])
    + planningEditableField(item.id, 'recurrenceEndDate', '重复截止', recurrence.endDate || '', '2026-06-30')
    + planningEditableField(item.id, 'recurrenceWeekdays', '每周周几', weeklyDays, '1,3,5');
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
    els.planningPreviewMeta.textContent = state.planningParsing ? '识别中…' : '尚未识别 · 写完大纲后点“识别预览”，会先同步最新草稿';
    const mappingHtml = renderPlanningMappingPreview();
    els.planningPreview.innerHTML = mappingHtml || '<div class="empty-state">左侧大纲适合日常一条条写；右侧只负责检查识别结果。<br>需要从大段文本导入时，再展开 Markdown 兼容编辑区。</div>';
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
    const linked = item.type === 'EVENT' ? '<label class="planning-linked"><input type="checkbox" data-planning-linked="' + escapeHtml(item.id) + '"' + (item.createLinkedTodo ? ' checked' : '') + ' /> 同步创建以日程结束时间为 DDL 的待办任务</label>' : '';
    const editFields = editable ? (
      '<div class="planning-edit-grid">'
      + planningEditableField(item.id, 'title', '标题', item.title || '', '任务标题')
      + planningEditableField(item.id, 'groupName', '分组', item.groupName || '', '例行')
      + (item.type === 'TODO'
        ? planningEditableField(item.id, 'dueAt', 'DDL', editableDateTimeValue(item.dueAt), '2026-05-28 14:30')
        : planningEditableField(item.id, 'location', '地点', item.location || '', '@主楼B1-412') + planningEditableField(item.id, 'startAt', '开始', editableDateTimeValue(item.startAt), '2026-05-28 10:00') + planningEditableField(item.id, 'endAt', '结束', editableDateTimeValue(item.endAt), '2026-05-28 12:00'))
      + planningEditableField(item.id, 'reminders', '提醒', item.reminderInputText || (item.reminderOffsetsMinutes || []).join(','), '5,15,16:30,05-10 15:00')
      + planningRecurrenceFields(item)
      + '</div>'
      + '<label class="planning-edit-field full"><span>备注</span><textarea data-planning-field="notes" data-planning-id="' + escapeHtml(item.id) + '" rows="2">' + escapeHtml(item.notes || '') + '</textarea></label>'
      + '<div class="planning-preview-options">'
      +   (item.type === 'EVENT' ? '<label class="planning-linked"><input type="checkbox" data-planning-flag="allDay" data-planning-id="' + escapeHtml(item.id) + '"' + (item.allDay ? ' checked' : '') + ' /> 全天</label>' : '')
      +   '<label class="planning-linked"><input type="checkbox" data-planning-flag="countdownEnabled" data-planning-id="' + escapeHtml(item.id) + '"' + (item.countdownEnabled ? ' checked' : '') + ' /> 倒数日</label>'
      + '</div>'
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
    } else if (node.dataset.planningField === 'recurrenceType') {
      item.recurrence = item.recurrence || {};
      item.recurrence.type = value || 'NONE';
      item.recurrence.enabled = !!value && value !== 'NONE';
    } else if (node.dataset.planningField === 'recurrenceEndDate') {
      item.recurrence = item.recurrence || {};
      item.recurrence.endDate = value.trim() || null;
    } else if (node.dataset.planningField === 'recurrenceWeekdays') {
      item.recurrence = item.recurrence || {};
      item.recurrence.weeklyDays = parseWeekdays(value);
    } else {
      item[node.dataset.planningField] = value;
    }
  });
  document.querySelectorAll('[data-planning-flag]').forEach(node => {
    const item = byId.get(String(node.dataset.planningId));
    if (item) item[node.dataset.planningFlag] = node.checked;
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
  await waitPlanningPendingCommits();
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
  await flushPlanningOutlineInputs({ createRoot: true });
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
  await flushPlanningOutlineInputs({ createRoot: true });
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
  await flushPlanningOutlineInputs({ createRoot: true });
  await flushPlanningAutosave();
  const markdown = currentPlanningMarkdownForAction();
  state.planningParsing = true;
  renderPlanningPreview();
  try {
    const active = activePlanningNote();
    state.planningParseResult = await api('/api/planning/parse', {
      method: 'POST',
      body: JSON.stringify({ markdown, noteId: active && active.id })
    });
    state.planningParseMarkdown = markdown;
    renderPlanningPreview();
  } finally {
    state.planningParsing = false;
    renderPlanningPreview();
  }
}

async function importSelectedPlanning() {
  await flushPlanningOutlineInputs({ createRoot: true });
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
  const markdown = state.planningParseMarkdown || currentPlanningMarkdownForAction();
  const result = await api('/api/planning/import', {
    method: 'POST',
    body: JSON.stringify({ markdown, selectedIds, candidates: collectPlanningCandidates(), noteId: active && active.id })
  });
  if (result.updatedMarkdown != null) {
    els.planningEditor.value = result.updatedMarkdown;
    state.planningDirty = true;
    state.planningParseMarkdown = '';
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
  await flushPlanningOutlineInputs({ createRoot: true });
  const active = activePlanningNote();
  if (!active) throw new Error('没有可刷新的规划文档');
  const markdown = currentPlanningMarkdownForAction();
  const wholeDocument = window.confirm('确定要刷新整篇文档的未完成已导入项吗？\n选择“取消”则只刷新当前标题分区。');
  const result = await api('/api/planning/refresh', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      markdown,
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
  await flushPlanningOutlineInputs({ createRoot: true });
  const active = activePlanningNote();
  if (!active) throw new Error('没有可顺延的规划文档');
  const markdown = currentPlanningMarkdownForAction();
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
      markdown,
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
  await flushPlanningOutlineInputs({ createRoot: true });
  const active = activePlanningNote();
  if (!active) throw new Error('没有可撤销的规划文档');
  const markdown = currentPlanningMarkdownForAction();
  const undoSummary = latestPlanningUndoSummary();
  if (!undoSummary) throw new Error('当前没有可撤销的导入、刷新或顺延批次');
  if (!await confirmDanger('撤销上次规划台操作', '会回滚最近一次' + undoSummary.label + '批次，影响 ' + undoSummary.affectedCount + ' 条事项。', '撤销')) return;
  const result = await api('/api/planning/undo-last', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      markdown
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
  const board = state.snapshot?.todayBoard || {};
  const boardEvents = [
    ...(board.allTodayEvents || []),
    ...(board.visibleTodayEvents || []),
    ...(board.tomorrowEvents || []),
    ...(board.countdownItems || []).filter(item => item.itemType === 'EVENT')
  ];
  return [
    ...(state.snapshot?.events || []),
    ...boardEvents
  ].find(item => sameId(item.id, id));
}

function renderEventCheckInPanelLoading(eventId, accent) {
  return ''
    + '<section id="event-checkin-panel" class="event-checkin-panel" data-checkin-panel-event-id="' + escapeHtml(eventId) + '" style="--accent:' + escapeHtml(accent || DEFAULT_EVENT_COLOR) + '">'
    +   '<div class="event-checkin-panel-head">'
    +     '<div>'
    +       '<div class="event-checkin-title">打卡追踪</div>'
    +       '<div class="event-checkin-subtitle">正在读取打卡记录…</div>'
    +     '</div>'
    +   '</div>'
    + '</section>';
}

function renderEventCheckInPanel(data, eventItem) {
  const panel = document.getElementById('event-checkin-panel');
  if (!panel || !sameId(panel.dataset.checkinPanelEventId, data.eventId)) return;
  const now = Date.now();
  const checkIns = Array.isArray(data.checkIns) ? data.checkIns : [];
  const active = checkIns.find(item => item.checkOutAtMillis == null);
  const savedTotal = Math.max(0, Number(data.totalCheckInMinutes || eventItem?.totalCheckInMinutes || 0));
  const activeMinutes = active ? checkInDurationMinutes(active, now) : 0;
  const displayTotal = savedTotal + activeMinutes;
  const records = checkIns.length
    ? checkIns.slice().reverse().map(item => (
      '<div class="event-checkin-record ' + (item.checkOutAtMillis ? '' : 'active') + '">'
      + '<span class="event-checkin-dot"></span>'
      + '<span>' + escapeHtml(checkInRangeText(item, now)) + '</span>'
      + '</div>'
    )).join('')
    : '<div class="event-checkin-empty">还没有打卡记录。</div>';
  panel.innerHTML = ''
    + '<div class="event-checkin-panel-head">'
    +   '<div>'
    +     '<div class="event-checkin-title">打卡追踪</div>'
    +     '<div class="event-checkin-subtitle">' + (active ? '签到中 · 当前段 ' + formatDurationMinutes(activeMinutes) : '当前未签到') + '</div>'
    +   '</div>'
    +   '<div class="event-checkin-total">' + escapeHtml(formatDurationMinutes(displayTotal)) + '<span>总投入</span></div>'
    + '</div>'
    + '<div class="event-checkin-records">' + records + '</div>'
    + '<div class="event-checkin-actions">'
    +   '<button type="button" class="' + (active ? 'danger-lite-action' : 'success-action') + '" data-event-checkin-action="' + (active ? 'check-out' : 'check-in') + '" data-event-checkin-id="' + escapeHtml(String(data.eventId)) + '">'
    +     (active ? '签退' : '签到')
    +   '</button>'
    + '</div>';
}

function renderEventCheckInPanelError(eventId, message) {
  const panel = document.getElementById('event-checkin-panel');
  if (!panel || !sameId(panel.dataset.checkinPanelEventId, eventId)) return;
  panel.innerHTML = ''
    + '<div class="event-checkin-panel-head">'
    +   '<div>'
    +     '<div class="event-checkin-title">打卡追踪</div>'
    +     '<div class="event-checkin-subtitle error">' + escapeHtml(message || '打卡记录加载失败') + '</div>'
    +   '</div>'
    + '</div>';
}

async function loadEventCheckInPanel(eventId) {
  const serial = ++state.eventCheckInLoadSerial;
  try {
    const data = await api(`/api/events/${eventId}/check-ins`);
    if (serial !== state.eventCheckInLoadSerial || !sameId(state.previewEventId, eventId)) return;
    const eventItem = findEventById(eventId);
    if (eventItem) eventItem.totalCheckInMinutes = data.totalCheckInMinutes || eventItem.totalCheckInMinutes || 0;
    renderEventCheckInPanel(data, eventItem);
  } catch (error) {
    if (serial !== state.eventCheckInLoadSerial) return;
    renderEventCheckInPanelError(eventId, error.message || '打卡记录加载失败');
  }
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

function setRecurrenceScopeBlock(prefix, visible, defaultScope = 'CURRENT_AND_FUTURE') {
  const block = document.getElementById(prefix + '-recurrence-scope-block');
  const select = document.getElementById(prefix + '-recurrence-scope');
  if (!block || !select) return;
  block.classList.toggle('hidden', !visible);
  select.disabled = !visible;
  if (visible) select.value = defaultScope;
}

function recurrenceScopePayload(prefix, originalRecurring, recurrenceTypeValue) {
  if (!originalRecurring) return 'CURRENT';
  const select = document.getElementById(prefix + '-recurrence-scope');
  const selected = select?.value || 'CURRENT_AND_FUTURE';
  if ((!recurrenceTypeValue || recurrenceTypeValue === 'NONE') && selected === 'CURRENT') {
    return 'CURRENT_AND_FUTURE';
  }
  return selected;
}

function recurrenceScopeQuery(prefix, originalRecurring) {
  if (!originalRecurring) return '';
  const select = document.getElementById(prefix + '-recurrence-scope');
  const scope = select?.value || 'CURRENT_AND_FUTURE';
  return '?scope=' + encodeURIComponent(scope);
}

function recurrenceScopeQueryForItem(item) {
  return item?.isRecurring ? '?scope=CURRENT_AND_FUTURE' : '';
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
  state.editingTodoOriginalRecurring = false;
  setTodoEditorGroupIds(defaultTodoGroupIds());
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
  setRecurrenceScopeBlock('todo', false);
  document.getElementById('todo-countdown').checked = false;
  document.getElementById('todo-ring').checked = true;
  document.getElementById('todo-vibrate').checked = true;
  const hasDueCheckbox = document.getElementById('todo-has-due');
  if (hasDueCheckbox) hasDueCheckbox.disabled = false;
  setTodoDueEnabled(true);
}

function openTodoEditor(item) {
  state.editingTodoId = item.id;
  state.editingTodoOriginalRecurring = item.isRecurring === true;
  setTodoEditorGroupIds(todoGroupIds(item));
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
  setRecurrenceScopeBlock('todo', item.isRecurring === true, 'CURRENT_AND_FUTURE');
  document.getElementById('todo-countdown').checked = item.countdownEnabled === true;
  document.getElementById('todo-ring').checked = item.ringEnabled !== false;
  document.getElementById('todo-vibrate').checked = item.vibrateEnabled !== false;
  openModal('todo-modal');
}

function clearEventForm() {
  state.editingEventId = null;
  state.editingEventOriginalRecurring = false;
  state.pendingEventSeed = null;
  fillGroupSelect('event-group');
  document.getElementById('event-modal-title').textContent = '新增日程';
  document.getElementById('save-event').textContent = '创建日程';
  document.getElementById('delete-event').classList.add('hidden');
  document.getElementById('event-title').value = '';
  setEventColor(DEFAULT_EVENT_COLOR);
  document.getElementById('event-location').value = '';
  document.getElementById('event-notes').value = '';
  writeDateTimeValue('event-start', '');
  writeDateTimeValue('event-end', '');
  document.getElementById('event-reminder-mode').value = 'NOTIFICATION';
  document.getElementById('event-reminder-offsets').value = '';
  document.getElementById('event-recurrence-type').value = 'NONE';
  document.getElementById('event-recurrence-end').value = '';
  document.getElementById('event-weekdays').value = '';
  setRecurrenceScopeBlock('event', false);
  document.getElementById('event-countdown').checked = false;
  document.getElementById('event-check-in').checked = false;
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

function normalizeHexColor(value) {
  return String(value || '').trim().toLowerCase();
}

function setEventColor(value) {
  const input = document.getElementById('event-color');
  if (!input) return;
  input.value = normalizeHexColor(value || DEFAULT_EVENT_COLOR);
  syncEventColorPresets();
}

function syncEventColorPresets() {
  const input = document.getElementById('event-color');
  const current = normalizeHexColor(input && input.value);
  document.querySelectorAll('[data-event-color-preset]').forEach(node => {
    node.classList.toggle('active', normalizeHexColor(node.dataset.eventColorPreset) === current);
  });
}

function bindEventColorPresets() {
  const host = document.getElementById('event-color-presets');
  const input = document.getElementById('event-color');
  if (!host || !input) return;
  host.innerHTML = EVENT_COLOR_PRESETS.map(color => (
    '<button type="button" class="color-swatch" data-event-color-preset="' + escapeHtml(color) + '" style="--swatch:' + escapeHtml(color) + '" aria-label="选择颜色 ' + escapeHtml(color) + '"></button>'
  )).join('');
  host.addEventListener('click', event => {
    const swatch = event.target.closest?.('[data-event-color-preset]');
    if (!swatch) return;
    setEventColor(swatch.dataset.eventColorPreset || DEFAULT_EVENT_COLOR);
  });
  input.addEventListener('input', syncEventColorPresets);
  syncEventColorPresets();
}

function openEventEditor(item) {
  state.editingEventId = item.id;
  state.editingEventOriginalRecurring = item.isRecurring === true;
  state.pendingEventSeed = null;
  fillGroupSelect('event-group', item.groupId);
  document.getElementById('event-modal-title').textContent = '编辑日程';
  document.getElementById('save-event').textContent = '保存修改';
  document.getElementById('delete-event').classList.remove('hidden');
  document.getElementById('event-title').value = item.title || '';
  setEventColor(item.accentColorHex || item.groupColorHex || DEFAULT_EVENT_COLOR);
  document.getElementById('event-location').value = item.location || '';
  document.getElementById('event-notes').value = item.notes || '';
  writeDateTimeValue('event-start', formatDateTimeLocalValue(item.startAtMillis));
  writeDateTimeValue('event-end', formatDateTimeLocalValue(item.endAtMillis || item.startAtMillis));
  document.getElementById('event-reminder-mode').value = item.reminderDeliveryMode || 'NOTIFICATION';
  document.getElementById('event-reminder-offsets').value = reminderSpecFromOffsets(item, item.startAtMillis);
  document.getElementById('event-recurrence-type').value = recurrenceTypeValue(item);
  document.getElementById('event-recurrence-end').value = item.recurrenceEndDate || '';
  document.getElementById('event-weekdays').value = csvValue(item.recurrenceWeekdays);
  setRecurrenceScopeBlock('event', item.isRecurring === true, 'CURRENT_AND_FUTURE');
  document.getElementById('event-countdown').checked = item.countdownEnabled === true;
  document.getElementById('event-check-in').checked = item.checkInEnabled === true;
  document.getElementById('event-all-day').checked = item.allDay === true;
  document.getElementById('event-ring').checked = item.ringEnabled !== false;
  document.getElementById('event-vibrate').checked = item.vibrateEnabled !== false;
  openModal('event-modal');
}

function openTodoPreview(item) {
  state.previewTodoId = item.id;
  const accent = todoGroupAccent(item);
  const body = document.getElementById('todo-preview-body');
  const completeButton = document.getElementById('preview-todo-complete');
  const cancelButton = document.getElementById('preview-todo-cancel');
  const cancelTopButton = document.getElementById('preview-todo-cancel-top');
  if (!body) return;
  body.innerHTML = ''
    + '<div class="preview-title-row" style="--accent:' + accent + '">'
    +   '<div class="preview-color-block"></div>'
    +   '<div class="preview-main-title">' + escapeHtml(item.title) + '</div>'
    +   '<div class="pill">' + escapeHtml(todoStateLabel(item)) + '</div>'
    + '</div>'
    + previewRow('分', '分组', todoGroupLabel(item))
    + previewRow('时', 'DDL', todoDueText(item))
    + previewRow('倒', '倒数日', item.countdownEnabled ? '已显示在每日看板倒数日' : '未开启')
    + previewRow('循', '循环', recurrenceLabel(item))
    + previewRow('铃', '提醒', reminderText(item))
    + (item.notes ? previewRow('记', '备注', item.notes) : '');
  const inactive = item.completed || item.canceled;
  completeButton?.classList.toggle('hidden', inactive);
  cancelButton?.classList.toggle('hidden', inactive);
  cancelTopButton?.classList.toggle('hidden', inactive);
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
    + previewRow('倒', '倒数日', item.countdownEnabled ? '已显示在每日看板倒数日' : '未开启')
    + previewRow('循', '重复', recurrenceLabel(item))
    + previewRow('地', '地点', item.location || '未填写')
    + previewRow('铃', '提醒', reminderText(item))
    + (item.notes ? previewRow('记', '备注', item.notes) : '')
    + (item.checkInEnabled ? renderEventCheckInPanelLoading(item.id, accent) : previewRow('打', '打卡追踪', '未开启'));
  openModal('event-preview-modal');
  if (item.checkInEnabled) loadEventCheckInPanel(item.id);
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
  document.querySelectorAll('[data-todo-group-filter]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      const value = node.dataset.todoGroupFilter;
      if (value === 'all') {
        state.selectedTodoGroupIds = [];
      } else {
        const groupId = Number(value);
        const selected = new Set((state.selectedTodoGroupIds || []).map(Number));
        if (selected.has(groupId)) {
          selected.delete(groupId);
        } else if (Number.isFinite(groupId) && groupId > 0) {
          selected.add(groupId);
        }
        state.selectedTodoGroupIds = Array.from(selected);
      }
      renderTodos();
    };
  });
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
  document.querySelectorAll('[data-event-preview-id]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      event.stopPropagation();
      openEventPreviewById(node.dataset.eventPreviewId);
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
  document.querySelectorAll('[data-search-todos]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      const input = document.querySelector('[data-todo-search-input]');
      state.todoQuery = (input?.value || '').trim();
      resetTodoPageState();
      els.status.textContent = state.todoQuery ? '正在搜索待办…' : '正在加载待办管理列表…';
      loadDesktopTodos({ reset: true })
        .then(() => { els.status.textContent = state.todoQuery ? '待办搜索完成' : '已加载待办管理列表'; })
        .catch(err => els.status.textContent = err.message);
    };
  });
  document.querySelectorAll('[data-clear-todo-search]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      state.todoQuery = '';
      resetTodoPageState();
      els.status.textContent = '正在清空搜索条件…';
      loadDesktopTodos({ reset: true })
        .then(() => { els.status.textContent = '已恢复待办管理列表'; })
        .catch(err => els.status.textContent = err.message);
    };
  });
  document.querySelectorAll('[data-load-more-todos]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      if (!state.todoHasMore || state.todoLoading) return;
      els.status.textContent = '正在加载更多待办…';
      loadDesktopTodos({ reset: false })
        .then(() => { els.status.textContent = '已加载更多待办'; })
        .catch(err => els.status.textContent = err.message);
    };
  });
  document.querySelectorAll('[data-todo-search-input]').forEach(node => {
    node.onkeydown = event => {
      if (event.key !== 'Enter') return;
      event.preventDefault();
      document.querySelector('[data-search-todos]')?.click();
    };
  });
}

document.getElementById('todo-has-due')?.addEventListener('change', event => setTodoDueEnabled(event.target.checked));
document.getElementById('todo-recurrence-type')?.addEventListener('change', event => {
  if (!state.editingTodoOriginalRecurring || event.target.value !== 'NONE') return;
  const scopeSelect = document.getElementById('todo-recurrence-scope');
  if (scopeSelect?.value === 'CURRENT') scopeSelect.value = 'CURRENT_AND_FUTURE';
});
document.getElementById('event-recurrence-type')?.addEventListener('change', event => {
  if (!state.editingEventOriginalRecurring || event.target.value !== 'NONE') return;
  const scopeSelect = document.getElementById('event-recurrence-scope');
  if (scopeSelect?.value === 'CURRENT') scopeSelect.value = 'CURRENT_AND_FUTURE';
});
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
document.getElementById('planning-publish-all')?.addEventListener('click', () => {
  flushPlanningOutlineInputs({ createRoot: true })
    .then(() => publishAllPlanningDrafts())
    .catch(err => els.status.textContent = err.message);
});
document.getElementById('planning-parse')?.addEventListener('click', () => parsePlanningEditor().catch(err => els.status.textContent = err.message));
document.getElementById('planning-import')?.addEventListener('click', () => importSelectedPlanning().catch(err => els.status.textContent = err.message));
document.getElementById('planning-refresh')?.addEventListener('click', () => refreshPlanningImported().catch(err => els.status.textContent = err.message));
document.getElementById('planning-postpone')?.addEventListener('click', () => postponePlanningImported().catch(err => els.status.textContent = err.message));
document.getElementById('planning-undo')?.addEventListener('click', () => undoPlanningLastOperation().catch(err => els.status.textContent = err.message));
document.getElementById('planning-export-markdown')?.addEventListener('click', () => {
  flushPlanningOutlineInputs({ createRoot: true })
    .then(() => {
      if (!els.planningEditor) return;
      els.planningEditor.value = state.planningNodesMarkdown || '';
      state.planningDirty = false;
      state.planningParseMarkdown = '';
      els.status.textContent = '已同步大纲 Markdown 到兼容编辑区';
    })
    .catch(err => els.status.textContent = err.message);
});
document.getElementById('planning-root-add')?.addEventListener('click', () => {
  const value = els.planningRootInput?.value || '';
  createPlanningOutlineNodesFromText(value)
    .then(() => {
      if (els.planningRootInput) {
        els.planningRootInput.value = '';
        autosizePlanningTextarea(els.planningRootInput);
        focusPlanningRootInput();
      }
    })
    .catch(err => els.status.textContent = err.message);
});
els.planningRootInput?.addEventListener('keydown', event => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    document.getElementById('planning-root-add')?.click();
    return;
  }
  if (event.key === 'Backspace') {
    const atStart = (event.target.selectionStart || 0) === 0 && (event.target.selectionEnd || 0) === 0;
    const previous = (flattenPlanningNodes(state.planningNodes || []).slice(-1)[0] || {}).node || null;
    if (!event.target.value && previous) {
      event.preventDefault();
      focusPlanningNodeText(previous.id, String(previous.text || '').length);
    } else if (event.target.value && atStart && previous) {
      event.preventDefault();
      mergePlanningTextIntoPrevious(previous, event.target.value, async () => {
        event.target.value = '';
      }).catch(err => { els.status.textContent = err.message; });
    }
    return;
  }
  if (event.key === 'ArrowUp') {
    const previous = (flattenPlanningNodes(state.planningNodes || []).slice(-1)[0] || {}).node || null;
    if (previous) {
      event.preventDefault();
      focusPlanningNodeText(previous.id, String(previous.text || '').length);
    }
  }
});
els.planningRootInput?.addEventListener('input', event => autosizePlanningTextarea(event.target));
els.planningOutline?.addEventListener('click', event => {
  const focusRoot = event.target.closest?.('[data-focus-planning-root]');
  const collapse = event.target.closest?.('[data-node-collapse]');
  const complete = event.target.closest?.('[data-node-complete]');
  const child = event.target.closest?.('[data-node-child]');
  const del = event.target.closest?.('[data-node-delete]');
  const sync = event.target.closest?.('[data-node-sync]');
  const moveUp = event.target.closest?.('[data-node-move-up]');
  const moveDown = event.target.closest?.('[data-node-move-down]');
  const publish = event.target.closest?.('[data-node-publish]');
  if (focusRoot) {
    focusPlanningRootInput();
    return;
  }
  if (sync) {
    const node = planningNodeById(sync.dataset.nodeSync);
    if (node) updatePlanningOutlineNode(node.id, { syncEnabled: sync.checked }).catch(err => els.status.textContent = err.message);
  }
  if (collapse && !collapse.disabled) {
    const node = planningNodeById(collapse.dataset.nodeCollapse);
    if (node) updatePlanningOutlineNode(node.id, { collapsed: !node.collapsed }).catch(err => els.status.textContent = err.message);
  }
  if (complete) {
    const node = planningNodeById(complete.dataset.nodeComplete);
    if (node) updatePlanningOutlineNode(node.id, { completed: !node.completed }).catch(err => els.status.textContent = err.message);
  }
  if (child) {
    const text = prompt('新增子项', '');
    if (text) createPlanningOutlineNode(text, Number(child.dataset.nodeChild)).catch(err => els.status.textContent = err.message);
  }
  if (moveUp && !moveUp.disabled) {
    movePlanningNode(moveUp.dataset.nodeMoveUp, -1).catch(err => els.status.textContent = err.message);
  }
  if (moveDown && !moveDown.disabled) {
    movePlanningNode(moveDown.dataset.nodeMoveDown, 1).catch(err => els.status.textContent = err.message);
  }
  if (publish) {
    waitPlanningPendingCommits()
      .then(() => publishPlanningOutlineNode(publish.dataset.nodePublish))
      .catch(err => els.status.textContent = err.message);
  }
  if (del) {
    deletePlanningOutlineNode(del.dataset.nodeDelete).catch(err => els.status.textContent = err.message);
  }
});
els.planningOutline?.addEventListener('dragstart', event => {
  const handle = event.target.closest?.('[data-node-drag]');
  const row = handle?.closest?.('[data-planning-node-row]');
  if (!handle || !row) {
    event.preventDefault();
    return;
  }
  const nodeId = row.dataset.planningNodeRow || '';
  state.planningDraggingNodeId = nodeId;
  event.dataTransfer?.setData('text/plain', nodeId);
  event.dataTransfer?.setData('application/x-paykitodo-planning-node', nodeId);
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
  row.classList.add('dragging');
});
els.planningOutline?.addEventListener('dragover', event => {
  const row = event.target.closest?.('[data-planning-node-row]');
  const draggedId = planningDragNodeId(event);
  const targetId = row?.dataset?.planningNodeRow || '';
  if (!row || !draggedId || !targetId || sameId(draggedId, targetId)) return;
  const dragged = planningNodeById(draggedId);
  const target = planningNodeById(targetId);
  const sameParent = dragged && target && (dragged.parentNodeId == null
    ? target.parentNodeId == null
    : sameId(dragged.parentNodeId, target.parentNodeId));
  if (!sameParent) return;
  event.preventDefault();
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
  const rect = row.getBoundingClientRect();
  const insertAfter = event.clientY > rect.top + rect.height / 2;
  clearPlanningDragTargets();
  row.classList.add('drag-over');
  row.classList.toggle('drag-after', insertAfter);
});
els.planningOutline?.addEventListener('dragleave', event => {
  const row = event.target.closest?.('[data-planning-node-row]');
  if (row && !row.contains(event.relatedTarget)) row.classList.remove('drag-over', 'drag-after');
});
els.planningOutline?.addEventListener('drop', event => {
  const row = event.target.closest?.('[data-planning-node-row]');
  const draggedId = planningDragNodeId(event);
  const targetId = row?.dataset?.planningNodeRow || '';
  const insertAfter = row?.classList?.contains('drag-after') === true;
  clearPlanningDragState();
  if (!row || !draggedId || !targetId || sameId(draggedId, targetId)) return;
  event.preventDefault();
  reorderPlanningNodeNear(draggedId, targetId, insertAfter).catch(err => els.status.textContent = err.message);
});
els.planningOutline?.addEventListener('dragend', () => clearPlanningDragState());
els.planningOutline?.addEventListener('focusout', event => {
  if (event.target.matches?.('[data-node-text]')) {
    if (event.target.dataset.skipCommit === 'true') {
      delete event.target.dataset.skipCommit;
      return;
    }
    trackPlanningCommit(commitPlanningNodeText(event.target));
  }
  if (event.target.matches?.('[data-node-field]')) {
    trackPlanningCommit(commitPlanningNodeField(event.target));
  }
});
els.planningOutline?.addEventListener('input', event => {
  if (event.target.matches?.('[data-node-text]')) autosizePlanningTextarea(event.target);
});
els.planningOutline?.addEventListener('keydown', event => {
  const textInput = event.target.matches?.('[data-node-text]') ? event.target : null;
  if (!textInput) return;
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    const start = textInput.selectionStart ?? textInput.value.length;
    const end = textInput.selectionEnd ?? start;
    const atMiddle = start > 0 && start < textInput.value.length;
    const hasTail = String(textInput.value.slice(end)).trim().length > 0;
    if (atMiddle || hasTail) {
      splitPlanningNodeAtCursor(textInput).catch(err => { els.status.textContent = err.message; });
    } else {
      textInput.dataset.skipCommit = 'true';
      commitPlanningNodeText(textInput)
        .then(() => focusPlanningRootInput())
        .catch(err => { els.status.textContent = err.message; });
    }
  } else if (event.key === 'Tab') {
    event.preventDefault();
    indentPlanningNode(textInput.dataset.nodeText, event.shiftKey).catch(err => els.status.textContent = err.message);
  } else if (event.key === 'Backspace') {
    const atStart = (textInput.selectionStart || 0) === 0 && (textInput.selectionEnd || 0) === 0;
    if (!textInput.value) {
      event.preventDefault();
      const previous = previousVisiblePlanningNode(textInput.dataset.nodeText);
      textInput.dataset.skipCommit = 'true';
      deletePlanningOutlineNode(textInput.dataset.nodeText, { confirm: false })
        .then(() => {
          if (previous) focusPlanningNodeText(previous.id, String(previous.text || '').length);
        })
        .catch(err => { els.status.textContent = err.message; });
    } else if (atStart) {
      event.preventDefault();
      mergePlanningNodeWithPrevious(textInput).catch(err => { els.status.textContent = err.message; });
    }
  } else if (event.key === 'ArrowUp') {
    if ((textInput.selectionStart || 0) === 0 && (textInput.selectionEnd || 0) === 0) {
      const previous = previousVisiblePlanningNode(textInput.dataset.nodeText);
      if (previous) {
        event.preventDefault();
        focusPlanningNodeText(previous.id, String(previous.text || '').length);
      }
    }
  } else if (event.key === 'ArrowDown') {
    const end = String(textInput.value || '').length;
    if ((textInput.selectionStart || 0) === end && (textInput.selectionEnd || 0) === end) {
      const next = nextVisiblePlanningNode(textInput.dataset.nodeText);
      event.preventDefault();
      if (next) {
        focusPlanningNodeText(next.id, String(next.text || '').length);
      } else {
        focusPlanningRootInput();
      }
    }
  }
});
els.planningEditor?.addEventListener('input', () => {
  state.planningParseResult = null;
  state.planningParseMarkdown = '';
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
    await flushPlanningOutlineInputs({ createRoot: true });
    await flushPlanningAutosave();
  } catch (err) {
    els.status.textContent = err.message;
    return;
  }
  state.activePlanningNoteId = event.target.value;
  const note = activePlanningNote();
  if (note && els.planningEditor) {
    await loadPlanningNodes();
    els.planningEditor.value = state.planningNodesMarkdown || note.contentMarkdown || '';
    state.planningDirty = false;
    state.planningRenderedNoteId = note.id;
    state.planningParseResult = null;
    state.planningParseMarkdown = '';
    await loadPlanningMappings();
    renderPlanningOutline();
    renderPlanningPreview();
  }
});
window.addEventListener('beforeunload', event => {
  const hasRootDraft = !!String(els.planningRootInput?.value || '').trim();
  if (!state.planningDirty && !hasRootDraft && !state.planningPendingCommits.length) return;
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
    const groupIds = readTodoEditorGroupIds();
    const recurrenceType = document.getElementById('todo-recurrence-type').value;
    const payload = {
      title: document.getElementById('todo-title').value,
      notes: document.getElementById('todo-notes').value,
      dueAt: dueAt,
      reminderAt: null,
      reminderOffsetsMinutes: reminderOffsets,
      groupId: groupIds[0] || Number(document.getElementById('todo-group').value || 0),
      groupIds: groupIds,
      countdownEnabled: hasDueDate && document.getElementById('todo-countdown').checked,
      ringEnabled: document.getElementById('todo-ring').checked,
      vibrateEnabled: document.getElementById('todo-vibrate').checked,
      reminderDeliveryMode: document.getElementById('todo-reminder-mode').value,
      scope: recurrenceScopePayload('todo', state.editingTodoOriginalRecurring, hasDueDate ? recurrenceType : 'NONE'),
      recurrence: hasDueDate ? recurrencePayload(
        recurrenceType,
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
  await api(`/api/items/${state.editingTodoId}${recurrenceScopeQuery('todo', state.editingTodoOriginalRecurring)}`, { method: 'DELETE' });
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
  if (!await confirmDanger('确认取消待办', '取消后会停止提醒，并进入历史记录；这不是删除。', '取消待办')) return;
  const todoItem = findTodoById(state.previewTodoId);
  await api(`/api/items/${state.previewTodoId}/cancel${recurrenceScopeQueryForItem(todoItem)}`, { method: 'POST' });
  closeModal('todo-preview-modal');
  await refreshAfterMutation();
};

document.getElementById('preview-todo-cancel-top').onclick = async () => {
  document.getElementById('preview-todo-cancel')?.click();
};

document.getElementById('preview-todo-delete').onclick = async () => {
  if (!state.previewTodoId) return;
  if (!await confirmDanger('确认删除待办', '删除后会直接移除，不进入历史记录，也无法恢复。')) return;
  const todoItem = findTodoById(state.previewTodoId);
  await api(`/api/items/${state.previewTodoId}${recurrenceScopeQueryForItem(todoItem)}`, { method: 'DELETE' });
  closeModal('todo-preview-modal');
  await refreshAfterMutation();
};

document.getElementById('save-event').onclick = async () => {
  try {
    setInputInvalid('event-reminder-offsets', false);
    const startAt = readDateTimeValue('event-start');
    const endAt = readDateTimeValue('event-end');
    const startAtMillis = startAt ? new Date(startAt).getTime() : null;
    const recurrenceType = document.getElementById('event-recurrence-type').value;
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
      countdownEnabled: document.getElementById('event-countdown').checked,
      checkInEnabled: document.getElementById('event-check-in').checked,
      ringEnabled: document.getElementById('event-ring').checked,
      vibrateEnabled: document.getElementById('event-vibrate').checked,
      reminderDeliveryMode: document.getElementById('event-reminder-mode').value,
      scope: recurrenceScopePayload('event', state.editingEventOriginalRecurring, recurrenceType),
      recurrence: recurrencePayload(
        recurrenceType,
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
  await api(`/api/items/${state.editingEventId}${recurrenceScopeQuery('event', state.editingEventOriginalRecurring)}`, { method: 'DELETE' });
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
  const eventItem = findEventById(state.previewEventId);
  await api(`/api/items/${state.previewEventId}${recurrenceScopeQueryForItem(eventItem)}`, { method: 'DELETE' });
  closeModal('event-preview-modal');
  await refreshAfterMutation();
};

document.addEventListener('click', async event => {
  const actionNode = event.target.closest?.('[data-event-checkin-action]');
  if (!actionNode) return;
  event.preventDefault();
  const eventId = actionNode.dataset.eventCheckinId;
  if (!eventId) return;
  actionNode.disabled = true;
  try {
    const action = actionNode.dataset.eventCheckinAction;
    const result = await api(`/api/events/${eventId}/${action === 'check-out' ? 'check-out' : 'check-in'}`, { method: 'POST' });
    const eventItem = findEventById(eventId);
    if (eventItem && result.totalCheckInMinutes != null) eventItem.totalCheckInMinutes = result.totalCheckInMinutes;
    await loadEventCheckInPanel(eventId);
    els.status.textContent = action === 'check-out' ? '已签退' : '已签到';
  } catch (error) {
    els.status.textContent = error.message || '打卡操作失败';
    renderEventCheckInPanelError(eventId, error.message || '打卡操作失败');
  } finally {
    actionNode.disabled = false;
  }
});

document.addEventListener('click', event => {
  const dayHeader = event.target.closest?.('[data-day]');
  if (!dayHeader) return;
  state.selectedEventDay = dayHeader.dataset.day;
  renderEvents();
});

bindDigitInputs();
bindEventColorPresets();
syncTopbar();
