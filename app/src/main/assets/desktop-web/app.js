const HOUR_HEIGHT = 56;
const FIFTEEN_MINUTES = 15 * 60 * 1000;
const THIRTY_MINUTES = 30 * 60 * 1000;
const DEFAULT_EVENT_COLOR = '#df58a5';
const EVENT_COLOR_PRESETS = ['#DF58A5', '#F54A45', '#FF811A', '#FFC60A', '#35BD4B', '#33D6C0', '#336DF4', '#3EC3F7', '#4752E6', '#7A35F0', '#BF3DBF', '#8F959E'];
const REMINDER_OFFSET_PRESETS = [
  { minutes: 5, label: '提前 5 分钟' },
  { minutes: 15, label: '提前 15 分钟' },
  { minutes: 30, label: '提前 30 分钟' },
  { minutes: 60, label: '提前 1 小时' },
  { minutes: 120, label: '提前 2 小时' },
  { minutes: 1440, label: '提前 1 天' },
  { minutes: 2880, label: '提前 2 天' }
];
const WEEKDAY_OPTIONS = [
  { value: 1, label: '一' },
  { value: 2, label: '二' },
  { value: 3, label: '三' },
  { value: 4, label: '四' },
  { value: 5, label: '五' },
  { value: 6, label: '六' },
  { value: 7, label: '日' }
];
const TODO_PAGE_LIMIT = 80;
const state = {
  token: '',
  snapshot: null,
  currentTab: 'board',
  selectedEventDay: dayKey(new Date()),
  eventViewMode: 'week',
  weekStartMode: 'MONDAY',
  eventMiniMonth: dayKey(new Date()).slice(0, 7) + '-01',
  eventQuery: '',
  hiddenEventGroupIds: [],
  eventEditorPreviewDayOffset: 0,
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
  planningPreviewEdits: {},
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
  eventDragSuppressClickUntil: 0,
  eventCheckInLoadSerial: 0,
  aiReportStatus: null,
  aiConfigDraft: null,
  aiProviderModels: {},
  aiProviderFeedback: {},
  desktopHeartbeatTimer: null,
  editingTodoOriginalRecurring: false,
  editingEventOriginalRecurring: false,
  editingEventMultiSlotBundleId: null,
  eventCourseSlots: [],
  reviewImport: {
    fileName: '',
    candidates: [],
    existingEvents: [],
    rangeStart: null,
    rangeEnd: null
  }
};

const statusProxy = {
  set textContent(value) {
    const login = document.getElementById('login-status');
    const shell = document.getElementById('shell-status');
    if (login) login.textContent = value;
    if (shell) shell.textContent = value;
  },
  get textContent() {
    return document.getElementById('shell-status')?.textContent || '';
  }
};

function showAppShell() {
  document.getElementById('login-screen')?.classList.add('hidden');
  document.querySelector('.shell')?.classList.remove('hidden');
}

function showLoginScreen() {
  document.querySelector('.shell')?.classList.add('hidden');
  document.getElementById('login-screen')?.classList.remove('hidden');
}

const els = {
  token: document.getElementById('token'),
  status: statusProxy,
  desktopDailyBoard: document.getElementById('desktop-daily-board'),
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
  eventCalendarToday: document.getElementById('event-calendar-today'),
  eventMiniMonthTitle: document.getElementById('event-mini-month-title'),
  eventMiniCalendar: document.getElementById('event-mini-calendar'),
  eventMiniPrev: document.getElementById('event-mini-prev'),
  eventMiniNext: document.getElementById('event-mini-next'),
  calendarWeekStart: document.getElementById('calendar-week-start'),
  eventMiniWeekdays: document.getElementById('event-mini-weekdays'),
  eventCalendarSearch: document.getElementById('event-calendar-search'),
  eventCalendarGroups: document.getElementById('event-calendar-groups'),
  planningNoteSelect: document.getElementById('planning-note-select'),
  planningEditor: document.getElementById('planning-editor'),
  planningOutline: document.getElementById('planning-outline'),
  planningRootInput: document.getElementById('planning-root-input'),
  planningActiveTitle: document.getElementById('planning-active-title'),
  planningPreview: document.getElementById('planning-preview'),
  planningPreviewMeta: document.getElementById('planning-preview-meta'),
  reviewXlsFile: document.getElementById('review-xls-file'),
  reviewXlsOpen: document.getElementById('review-xls-open'),
  reviewXlsPanel: document.getElementById('review-xls-panel'),
  reviewXlsParse: document.getElementById('review-xls-parse'),
  reviewXlsImport: document.getElementById('review-xls-import'),
  reviewXlsMeta: document.getElementById('review-xls-meta'),
  reviewXlsPreview: document.getElementById('review-xls-preview'),
  aiReportStatus: document.getElementById('ai-report-status')
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

function normalizeReviewText(value) {
  return String(value || '')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/\u00a0/g, ' ')
    .replace(/：/g, ':')
    .replace(/，/g, ',')
    .replace(/[－–—]/g, '-')
    .trim();
}

function parseReviewTimeRange(value) {
  const text = normalizeReviewText(value);
  const match = text.match(/(\d{1,2})\s*:\s*(\d{2})\s*(?:-|~|～|至|到)\s*(\d{1,2})\s*:\s*(\d{2})/);
  if (!match) return null;
  const startHour = Number(match[1]);
  const startMinute = Number(match[2]);
  const endHour = Number(match[3]);
  const endMinute = Number(match[4]);
  if (
    startHour < 0 || startHour > 23 ||
    endHour < 0 || endHour > 23 ||
    startMinute < 0 || startMinute > 59 ||
    endMinute < 0 || endMinute > 59
  ) return null;
  return {
    start: { hour: startHour, minute: startMinute },
    end: { hour: endHour, minute: endMinute },
    matchedText: match[0]
  };
}

function parseReviewDateHeader(value) {
  const text = normalizeReviewText(value);
  const match = text.match(/(\d{1,2})\s*(?:\/|\.|-|月)\s*(\d{1,2})/);
  if (!match) return null;
  const month = Number(match[1]);
  const day = Number(match[2]);
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  return { month, day, label: text };
}

function inferReviewDateColumns(rows) {
  let headerRow = -1;
  let parsedHeaders = [];
  for (let r = 0; r < Math.min(rows.length, 8); r += 1) {
    const parsed = (rows[r] || [])
      .map((value, column) => ({ ...(parseReviewDateHeader(value) || {}), column }))
      .filter(item => item.month);
    if (parsed.length >= 2) {
      headerRow = r;
      parsedHeaders = parsed;
      break;
    }
  }
  if (headerRow < 0) return { headerRow: -1, columns: [] };

  const now = new Date();
  let year = now.getFullYear();
  const first = parsedHeaders[0];
  let firstDate = new Date(year, first.month - 1, first.day);
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const diffDays = (firstDate.getTime() - today.getTime()) / (24 * 60 * 60 * 1000);
  if (diffDays < -180) year += 1;
  if (diffDays > 180) year -= 1;

  const columns = [];
  let previousDate = null;
  parsedHeaders.forEach(item => {
    let date = new Date(year, item.month - 1, item.day);
    while (previousDate && date.getTime() < previousDate.getTime()) {
      year += 1;
      date = new Date(year, item.month - 1, item.day);
    }
    previousDate = date;
    columns.push({
      column: item.column,
      date,
      key: dayKey(date),
      label: item.label
    });
  });
  return { headerRow, columns };
}

function pad2(value) {
  return String(value).padStart(2, '0');
}

function reviewDateTimeLocal(dateKey, time) {
  return dateKey + 'T' + pad2(time.hour) + ':' + pad2(time.minute);
}

function reviewMillis(value) {
  return new Date(value).getTime();
}

function normalizeReviewLocation(value) {
  return normalizeReviewText(value)
    .replace(/^[@＠]+/, '')
    .replace(/^["“”'‘’]+|["“”'‘’]+$/g, '')
    .trim();
}

function normalizeReviewKeyText(value) {
  return normalizeReviewText(value)
    .replace(/\s+/g, '')
    .replace(/^[@＠]+/, '')
    .toLowerCase();
}

function looksLikeReviewLocation(value) {
  return /[@＠]|楼|室|馆|门|场|厅|房|中心|活动|教室|实验|校区|园|B\d|A\d|C\d|Room/i.test(value || '');
}

function parseReviewCellContent(rawValue) {
  const original = normalizeReviewText(rawValue);
  const timeRange = parseReviewTimeRange(original);
  const quotedLocations = [];
  original.replace(/["“”'‘’]\s*(@?[^"“”'‘’]+?)\s*["“”'‘’]/g, (_, location) => {
    quotedLocations.push(location);
    return '';
  });

  const parenthetical = [];
  original.replace(/[（(]([^（）()]*)[）)]/g, (_, inner) => {
    parenthetical.push(inner);
    return '';
  });

  let location = quotedLocations.map(normalizeReviewLocation).find(Boolean) || '';
  const notes = [];
  parenthetical.forEach(inner => {
    const cleaned = normalizeReviewText(inner).replace(/(\d{1,2})\s*:\s*(\d{2})\s*(?:-|~|～|至|到)\s*(\d{1,2})\s*:\s*(\d{2})/, ' ');
    const parts = cleaned.split(/[,，\n]/).map(part => normalizeReviewText(part)).filter(Boolean);
    if (!location && parts.length) {
      const last = parts[parts.length - 1];
      if (timeRange || looksLikeReviewLocation(last)) {
        location = normalizeReviewLocation(last);
        parts.slice(0, -1).forEach(part => notes.push(part));
        return;
      }
    }
    parts.forEach(part => {
      if (!location && looksLikeReviewLocation(part)) location = normalizeReviewLocation(part);
      else if (part !== location) notes.push(part);
    });
  });

  let title = original
    .replace(/[（(][^（）()]*[）)]/g, ' ')
    .replace(/["“”'‘’]\s*@?[^"“”'‘’]+?\s*["“”'‘’]/g, ' ')
    .replace(/(\d{1,2})\s*:\s*(\d{2})\s*(?:-|~|～|至|到)\s*(\d{1,2})\s*:\s*(\d{2})/, ' ')
    .replace(/^[,\s]+|[,\s]+$/g, ' ')
    .trim();
  const titleParts = title.split(/[,，]/).map(part => normalizeReviewText(part)).filter(Boolean);
  if (titleParts.length > 1) {
    const maybeLocation = titleParts[titleParts.length - 1];
    if (!location && looksLikeReviewLocation(maybeLocation)) {
      location = normalizeReviewLocation(maybeLocation);
      title = titleParts.slice(0, -1).join(' ');
    } else {
      title = titleParts.join(' ');
    }
  }
  title = normalizeReviewText(title).replace(/\s+/g, ' ');
  return {
    title,
    location,
    notes: [...new Set(notes.map(normalizeReviewText).filter(Boolean))].join('；'),
    explicitTimeRange: timeRange
  };
}

function reviewMergeStartingAt(merges, row, column) {
  return (merges || []).find(merge => merge.s?.r === row && merge.s?.c === column) || null;
}

function parseReviewWorkbook(arrayBuffer, fileName) {
  if (!window.XLSX) throw new Error('Excel 解析组件未加载，请刷新电脑端页面后重试');
  const workbook = window.XLSX.read(arrayBuffer, { type: 'array', cellDates: false });
  const sheetName = workbook.SheetNames[0];
  if (!sheetName) throw new Error('Excel 文件里没有工作表');
  const sheet = workbook.Sheets[sheetName];
  const rows = window.XLSX.utils.sheet_to_json(sheet, { header: 1, raw: false, defval: '' });
  const { headerRow, columns } = inferReviewDateColumns(rows);
  if (headerRow < 0 || !columns.length) throw new Error('没有识别到周历日期表头，例如 5/28 或 5月28日');
  const rowTimes = new Map();
  rows.forEach((row, index) => {
    const range = parseReviewTimeRange(row?.[0]);
    if (range) rowTimes.set(index, range);
  });

  const candidates = [];
  const merges = sheet['!merges'] || [];
  for (let r = headerRow + 1; r < rows.length; r += 1) {
    columns.forEach(columnInfo => {
      const raw = rows[r]?.[columnInfo.column];
      if (!normalizeReviewText(raw)) return;
      const merge = reviewMergeStartingAt(merges, r, columnInfo.column);
      const rowTime = rowTimes.get(r);
      const endRowTime = rowTimes.get(merge?.e?.r ?? r) || rowTime;
      const parsed = parseReviewCellContent(raw);
      const effectiveRange = parsed.explicitTimeRange || (rowTime && endRowTime ? { start: rowTime.start, end: endRowTime.end } : null);
      const startAt = effectiveRange ? reviewDateTimeLocal(columnInfo.key, effectiveRange.start) : null;
      let endAt = effectiveRange ? reviewDateTimeLocal(columnInfo.key, effectiveRange.end) : null;
      if (startAt && endAt && reviewMillis(endAt) <= reviewMillis(startAt)) {
        const endDate = new Date(reviewMillis(endAt));
        endDate.setDate(endDate.getDate() + 1);
        endAt = dayKey(endDate) + 'T' + pad2(endDate.getHours()) + ':' + pad2(endDate.getMinutes());
      }
      const invalidReason = !parsed.title
        ? '标题为空'
        : !startAt || !endAt
          ? '没有识别到时间段'
          : null;
      candidates.push({
        id: 'review-' + candidates.length,
        selected: !invalidReason,
        duplicate: false,
        duplicateReason: '',
        invalidReason,
        title: parsed.title || normalizeReviewText(raw),
        location: parsed.location,
        notes: parsed.notes,
        startAt,
        endAt,
        dateKey: columnInfo.key,
        sourceCell: window.XLSX.utils.encode_cell({ r, c: columnInfo.column }),
        sourceFile: fileName || 'review.xls'
      });
    });
  }
  return { sheetName, columns, candidates };
}

function reviewCandidateKey(candidate) {
  return [
    reviewMillis(candidate.startAt),
    reviewMillis(candidate.endAt),
    normalizeReviewKeyText(candidate.title),
    normalizeReviewKeyText(candidate.location)
  ].join('|');
}

function reviewEventKey(eventItem) {
  return [
    Number(eventItem.startAtMillis || 0),
    Number(eventItem.endAtMillis || 0),
    normalizeReviewKeyText(eventItem.title),
    normalizeReviewKeyText(eventItem.location)
  ].join('|');
}

async function loadExistingReviewEvents(candidates) {
  const valid = candidates.filter(item => !item.invalidReason && item.startAt && item.endAt);
  if (!valid.length) return [];
  const starts = valid.map(item => reviewMillis(item.startAt)).filter(Number.isFinite);
  const ends = valid.map(item => reviewMillis(item.endAt)).filter(Number.isFinite);
  const rangeStart = dayKey(new Date(Math.min(...starts)));
  const rangeEnd = addDays(dayKey(new Date(Math.max(...ends))), 1);
  const data = await api('/api/events?start=' + encodeURIComponent(rangeStart) + '&end=' + encodeURIComponent(rangeEnd));
  state.reviewImport.rangeStart = rangeStart;
  state.reviewImport.rangeEnd = rangeEnd;
  return data.events || [];
}

function applyReviewDuplicateMarks(candidates, existingEvents) {
  const existingKeys = new Set((existingEvents || []).filter(item => !(item.completed || item.canceled)).map(reviewEventKey));
  const seen = new Set();
  candidates.forEach(candidate => {
    if (candidate.invalidReason) {
      candidate.selected = false;
      return;
    }
    const key = reviewCandidateKey(candidate);
    if (existingKeys.has(key)) {
      candidate.duplicate = true;
      candidate.duplicateReason = '已存在于 PaykiTodo';
      candidate.selected = false;
      return;
    }
    if (seen.has(key)) {
      candidate.duplicate = true;
      candidate.duplicateReason = 'Excel 内重复';
      candidate.selected = false;
      return;
    }
    seen.add(key);
  });
}

function renderReviewImportPreview() {
  if (!els.reviewXlsPreview || !els.reviewXlsMeta) return;
  const candidates = state.reviewImport.candidates || [];
  const importable = candidates.filter(item => !item.invalidReason && !item.duplicate);
  const selectedCount = importable.filter(item => item.selected).length;
  if (els.reviewXlsImport) els.reviewXlsImport.disabled = selectedCount === 0;
  if (!candidates.length) {
    els.reviewXlsPreview.innerHTML = '';
    return;
  }
  const duplicateCount = candidates.filter(item => item.duplicate).length;
  const invalidCount = candidates.filter(item => item.invalidReason).length;
  els.reviewXlsMeta.textContent = '已解析 ' + candidates.length + ' 条，' + importable.length + ' 条可导入，' + duplicateCount + ' 条重复，' + invalidCount + ' 条无效；当前选中 ' + selectedCount + ' 条。';
  const actions = ''
    + '<div class="review-import-actions">'
    +   '<button type="button" class="ghost mini" data-review-select-all="true">全选可导入</button>'
    +   '<button type="button" class="ghost mini" data-review-clear-all="true">全不选</button>'
    + '</div>';
  els.reviewXlsPreview.innerHTML = actions + candidates.map(candidate => {
    const disabled = candidate.invalidReason || candidate.duplicate;
    const badge = candidate.invalidReason ? ('无效：' + candidate.invalidReason) : candidate.duplicate ? ('重复：' + candidate.duplicateReason) : '可导入';
    const meta = [
      candidate.startAt && candidate.endAt ? (formatDateTimeLabel(reviewMillis(candidate.startAt)) + ' - ' + formatDateTimeLabel(reviewMillis(candidate.endAt))) : '时间未识别',
      candidate.location ? ('地点：' + candidate.location) : '',
      candidate.notes ? ('备注：' + candidate.notes) : '',
      '来源：' + candidate.sourceFile + ' · ' + candidate.sourceCell
    ].filter(Boolean).join(' · ');
    return ''
      + '<div class="review-candidate' + (candidate.duplicate ? ' duplicate' : '') + (candidate.invalidReason ? ' invalid' : '') + '">'
      +   '<input type="checkbox" data-review-select="' + escapeHtml(candidate.id) + '"' + (candidate.selected ? ' checked' : '') + (disabled ? ' disabled' : '') + ' />'
      +   '<div>'
      +     '<div class="review-candidate-title">' + escapeHtml(candidate.title || '未命名日程') + '</div>'
      +     '<div class="review-candidate-meta">' + escapeHtml(meta) + '</div>'
      +   '</div>'
      +   '<div class="review-candidate-badge">' + escapeHtml(badge) + '</div>'
      + '</div>';
  }).join('');
}

function resetReviewImportPreview(message) {
  state.reviewImport.fileName = '';
  state.reviewImport.candidates = [];
  state.reviewImport.existingEvents = [];
  state.reviewImport.rangeStart = null;
  state.reviewImport.rangeEnd = null;
  if (els.reviewXlsImport) els.reviewXlsImport.disabled = true;
  if (els.reviewXlsPreview) els.reviewXlsPreview.innerHTML = '';
  if (els.reviewXlsMeta) {
    els.reviewXlsMeta.textContent = message || '未选择文件。浏览器安全限制下，网页不能自动读取 G:\\PlanGit\\review.xls，需要你手动选择一次文件。';
  }
}

async function parseReviewXlsFile() {
  if (!state.token) throw new Error('请先连接手机，再解析 review.xls');
  const file = els.reviewXlsFile?.files?.[0];
  if (!file) throw new Error('请先选择 review.xls 文件');
  const arrayBuffer = await file.arrayBuffer();
  const parsed = parseReviewWorkbook(arrayBuffer, file.name);
  const existingEvents = await loadExistingReviewEvents(parsed.candidates);
  applyReviewDuplicateMarks(parsed.candidates, existingEvents);
  state.reviewImport.fileName = file.name;
  state.reviewImport.candidates = parsed.candidates;
  state.reviewImport.existingEvents = existingEvents;
  renderReviewImportPreview();
  els.status.textContent = '已解析 ' + file.name + '，请在预览中确认后导入';
}

async function importSelectedReviewEvents() {
  const selected = (state.reviewImport.candidates || []).filter(item => item.selected && !item.invalidReason && !item.duplicate);
  if (!selected.length) throw new Error('没有选中的可导入日程');
  if (!await confirmDanger(
    '确认导入周历日程',
    '将向手机端新增 ' + selected.length + ' 条日程。Excel 文件不会被修改，也不会触发归档或设置桌面壁纸。',
    '导入选中'
  )) return;
  for (const candidate of selected) {
    const payload = {
      title: candidate.title,
      groupId: 0,
      location: candidate.location || '',
      notes: candidate.notes || '',
      startAt: candidate.startAt,
      endAt: candidate.endAt,
      allDay: false,
      accentColorHex: DEFAULT_EVENT_COLOR,
      reminderOffsetsMinutes: [],
      countdownEnabled: false,
      checkInEnabled: false,
      ringEnabled: false,
      vibrateEnabled: false,
      reminderDeliveryMode: 'FULLSCREEN_AND_NOTIFICATION',
      scope: 'CURRENT',
      recurrence: { enabled: false, type: 'NONE', weeklyDays: [], endDate: null }
    };
    await api('/api/events', { method: 'POST', body: JSON.stringify(payload) });
    candidate.selected = false;
    candidate.duplicate = true;
    candidate.duplicateReason = '刚刚已导入';
  }
  renderReviewImportPreview();
  await refreshAfterMutation();
  els.status.textContent = '已从 review.xls 导入 ' + selected.length + ' 条日程';
}

function bindReviewImportControls() {
  els.reviewXlsOpen?.addEventListener('click', () => els.reviewXlsFile?.click());
  els.reviewXlsFile?.addEventListener('change', () => {
    const file = els.reviewXlsFile?.files?.[0];
    if (!file) {
      resetReviewImportPreview();
      return;
    }
    els.reviewXlsPanel?.classList.remove('hidden');
    resetReviewImportPreview('已选择 ' + file.name + '。点击“解析预览”后会按表头日期和左侧节次时间生成候选；年份按距离今天最近的同月日推断。');
  });
  els.reviewXlsParse?.addEventListener('click', async () => {
    const button = els.reviewXlsParse;
    if (button) {
      button.disabled = true;
      button.textContent = '解析中';
    }
    try {
      await parseReviewXlsFile();
    } catch (error) {
      resetReviewImportPreview(error.message || '解析 review.xls 失败');
      els.status.textContent = error.message || '解析 review.xls 失败';
    } finally {
      if (button) {
        button.disabled = false;
        button.textContent = '解析预览';
      }
    }
  });
  els.reviewXlsImport?.addEventListener('click', async () => {
    const button = els.reviewXlsImport;
    if (button) {
      button.disabled = true;
      button.textContent = '导入中';
    }
    try {
      await importSelectedReviewEvents();
    } catch (error) {
      els.status.textContent = error.message || '导入 review.xls 日程失败';
    } finally {
      if (button) {
        button.textContent = '导入选中';
      }
      renderReviewImportPreview();
    }
  });
  els.reviewXlsPreview?.addEventListener('change', event => {
    const selectNode = event.target.closest?.('[data-review-select]');
    if (!selectNode) return;
    const candidate = (state.reviewImport.candidates || []).find(item => item.id === selectNode.dataset.reviewSelect);
    if (!candidate || candidate.invalidReason || candidate.duplicate) return;
    candidate.selected = selectNode.checked;
    renderReviewImportPreview();
  });
  els.reviewXlsPreview?.addEventListener('click', event => {
    if (event.target.closest?.('[data-review-select-all]')) {
      (state.reviewImport.candidates || []).forEach(candidate => {
        if (!candidate.invalidReason && !candidate.duplicate) candidate.selected = true;
      });
      renderReviewImportPreview();
    } else if (event.target.closest?.('[data-review-clear-all]')) {
      (state.reviewImport.candidates || []).forEach(candidate => {
        candidate.selected = false;
      });
      renderReviewImportPreview();
    }
  });
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
  return item?.groupColorHex || primaryGroup?.colorHex || item?.accentColorHex || DEFAULT_EVENT_COLOR;
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
    const color = group.colorHex || DEFAULT_EVENT_COLOR;
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
      const color = group.colorHex || DEFAULT_EVENT_COLOR;
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
    if (prefix.startsWith('event-')) renderEventEditorPreview();
    return;
  }
  node.classList.remove('is-empty');
  node.innerHTML = '<strong>' + escapeHtml(preview.main) + '</strong>' + escapeHtml(preview.sub);
  if (prefix.startsWith('event-')) renderEventEditorPreview();
}

function setEventSeed(startMillis, endMillis) {
  state.pendingEventSeed = { startMillis, endMillis };
  state.eventEditorPreviewDayOffset = 0;
  writeDateTimeValue('event-start', formatDateTimeLocalValue(startMillis));
  writeDateTimeValue('event-end', formatDateTimeLocalValue(endMillis));
  if (document.getElementById('event-course-enabled')?.checked) {
    ensureEventCourseSlots();
    renderEventCourseSlots();
  }
}

function readEventEditorDate(prefix) {
  try {
    const value = readDateTimeValue(prefix);
    return value ? new Date(value) : null;
  } catch (_) {
    return null;
  }
}

function renderEventEditorPreview() {
  const hours = document.getElementById('event-editor-preview-hours');
  const grid = document.getElementById('event-editor-preview-grid');
  const block = document.getElementById('event-editor-preview-block');
  const dateLabel = document.getElementById('event-editor-preview-date');
  const weekdayLabel = document.getElementById('event-editor-preview-weekday');
  if (!hours || !grid || !block || !dateLabel || !weekdayLabel) return;

  const start = readEventEditorDate('event-start') || new Date();
  const end = readEventEditorDate('event-end') || new Date(start.getTime() + THIRTY_MINUTES);
  const previewDate = new Date(start);
  previewDate.setDate(previewDate.getDate() + Number(state.eventEditorPreviewDayOffset || 0));
  const previewKey = dayKey(previewDate);
  const startKey = dayKey(start);
  const previewStartHour = Math.min(9, start.getHours());
  const previewHourCount = 24 - previewStartHour;
  const previewGridTop = 22;
  const previewHourHeight = 58;
  dateLabel.textContent = (previewDate.getMonth() + 1) + '月' + previewDate.getDate() + '日 ' + formatWeekday(previewDate);
  weekdayLabel.textContent = formatWeekday(previewDate) + ' · ' + previewDate.getDate();
  hours.style.height = (previewHourCount * previewHourHeight) + 'px';
  grid.style.height = (previewHourCount * previewHourHeight) + 'px';
  hours.innerHTML = Array.from({ length: previewHourCount }, (_, index) => {
    const hour = index + previewStartHour;
    return '<span style="top:' + (index * previewHourHeight) + 'px">' + String(hour).padStart(2, '0') + ':00</span>';
  }).join('');
  grid.innerHTML = Array.from({ length: previewHourCount }, (_, index) => '<span style="top:' + (index * previewHourHeight) + 'px"></span>').join('');

  if (previewKey !== startKey) {
    block.classList.add('hidden');
    return;
  }
  const startMinutes = start.getHours() * 60 + start.getMinutes();
  const endMinutes = dayKey(end) === startKey
    ? Math.max(startMinutes + 30, end.getHours() * 60 + end.getMinutes())
    : 24 * 60;
  const top = previewGridTop + ((startMinutes - previewStartHour * 60) / 60) * previewHourHeight;
  const height = Math.max(28, ((endMinutes - startMinutes) / 60) * previewHourHeight);
  block.classList.remove('hidden');
  block.style.top = Math.max(previewGridTop, top) + 'px';
  block.style.height = height + 'px';
  block.style.setProperty('--accent', document.getElementById('event-color')?.value || DEFAULT_EVENT_COLOR);
  block.querySelector('strong').textContent = document.getElementById('event-title')?.value.trim() || '添加主题';
  block.querySelector('span').textContent = formatTimeLabel(start.getTime()) + ' - ' + formatTimeLabel(end.getTime());
}

function snapToQuarterHour(millis) {
  return Math.round(millis / FIFTEEN_MINUTES) * FIFTEEN_MINUTES;
}

function weekdayValueFromDate(date) {
  const day = date.getDay();
  return day === 0 ? 7 : day;
}

function timeValueFromDate(date) {
  return String(date.getHours()).padStart(2, '0') + ':' + String(date.getMinutes()).padStart(2, '0');
}

function defaultEventCourseSlot() {
  let start = null;
  let end = null;
  try {
    const startValue = readDateTimeValue('event-start');
    const endValue = readDateTimeValue('event-end');
    start = startValue ? new Date(startValue) : null;
    end = endValue ? new Date(endValue) : null;
  } catch (_) {
    start = null;
    end = null;
  }
  const fallbackStart = start || new Date(snapToQuarterHour(Date.now() + THIRTY_MINUTES));
  const fallbackEnd = end && end > fallbackStart ? end : new Date(fallbackStart.getTime() + THIRTY_MINUTES);
  return {
    weekday: weekdayValueFromDate(fallbackStart),
    startTime: timeValueFromDate(fallbackStart),
    endTime: timeValueFromDate(fallbackEnd)
  };
}

function ensureEventCourseSlots() {
  if (!state.eventCourseSlots.length) {
    state.eventCourseSlots = [defaultEventCourseSlot()];
  }
}

function renderEventCourseSlots() {
  const host = document.getElementById('event-course-slots');
  if (!host) return;
  ensureEventCourseSlots();
  host.innerHTML = state.eventCourseSlots.map((slot, index) => {
    const canRemove = state.eventCourseSlots.length > 1;
    return ''
      + '<div class="course-slot-row" data-course-slot-index="' + index + '">'
      +   '<label>周几<select data-course-slot-field="weekday">'
      +     WEEKDAY_OPTIONS.map(day => '<option value="' + day.value + '"' + (Number(slot.weekday) === day.value ? ' selected' : '') + '>周' + escapeHtml(day.label) + '</option>').join('')
      +   '</select></label>'
      +   '<label>开始<input type="time" data-course-slot-field="startTime" value="' + escapeHtml(slot.startTime || '09:00') + '" /></label>'
      +   '<label>结束<input type="time" data-course-slot-field="endTime" value="' + escapeHtml(slot.endTime || '10:00') + '" /></label>'
      +   '<button type="button" class="ghost mini danger-lite-action" data-course-slot-remove="' + index + '"' + (canRemove ? '' : ' disabled') + '>删除</button>'
      + '</div>';
  }).join('');
}

function readEventCourseSlots() {
  return state.eventCourseSlots.map(slot => ({
    weekday: Number(slot.weekday),
    startTime: String(slot.startTime || '').trim(),
    endTime: String(slot.endTime || '').trim()
  })).filter(slot => Number.isInteger(slot.weekday) && slot.weekday >= 1 && slot.weekday <= 7);
}

function nextOrSameWeekdayDate(baseDate, weekday) {
  const date = new Date(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate());
  const current = weekdayValueFromDate(date);
  const delta = (Number(weekday) - current + 7) % 7;
  date.setDate(date.getDate() + delta);
  return date;
}

function courseSlotDateTimeValue(baseDate, weekday, timeValue) {
  const match = String(timeValue || '').match(/^(\d{1,2}):(\d{2})$/);
  if (!match) throw new Error('时间段格式错误，请填写 HH:mm。');
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) throw new Error('时间段超出有效时间范围。');
  const date = nextOrSameWeekdayDate(baseDate, weekday);
  date.setHours(hour, minute, 0, 0);
  return formatDateTimeLocalValue(date.getTime());
}

function defaultCourseRecurrenceEndDate(baseDate) {
  const date = new Date(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate());
  date.setDate(date.getDate() + 90);
  return dayKey(date);
}

function weekdayLabel(value) {
  const option = WEEKDAY_OPTIONS.find(day => day.value === Number(value));
  return option ? '周' + option.label : '周' + value;
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
  return mode ? '全屏 + 通知栏提醒' : '默认提醒';
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

function recurringWindowHint(item) {
  if (!item.isRecurring || !item.recurrenceEndDate || item.isHistory) return '';
  return '循环截止到 ' + item.recurrenceEndDate + '；列表只生成近未来实例，远期实例会随日期推进自动补齐。编辑时可选择“当前及之后”或“整个循环系列”。';
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
    +     (recurringWindowHint(item) ? '<div class="timeline-card-meta recurring-hint">' + escapeHtml(recurringWindowHint(item)) + '</div>' : '')
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
    +   (hasSearch ? '<div class="todo-page-meta">搜索：' + escapeHtml(query) + '</div>' : '')
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
  const now = new Date();
  const todayKey = dayKey(now);
  const currentMinutes = now.getHours() * 60 + now.getMinutes();
  const currentTimeOffset = (currentMinutes / 60) * HOUR_HEIGHT;

  let html = '<div class="hour-axis-header">GMT+8</div><div class="hour-axis-body">';
  for (let hour = 0; hour < 24; hour += 1) {
    html += '<div class="hour-axis-row"><div class="hour-label">' + String(hour).padStart(2, '0') + ':00</div></div>';
  }

  // 当前时间标签（红色 chip），和整点并列在最左边
  html += '<div class="hour-current-chip" style="top:' + currentTimeOffset + 'px">' + escapeHtml(formatTimeLabel(now.getTime())) + '</div></div>';

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
  if (key !== todayKey) return '';
  const top = (now.getHours() * 60 + now.getMinutes()) / 60 * HOUR_HEIGHT;
  return '<div class="current-line" style="top:' + top + 'px"></div>';
}

function visibleEventDayCount() {
  if (state.eventViewMode === 'day') return 1;
  if (state.eventViewMode === 'month') return 42;
  return 7;
}

function startOfCalendarWeek(key) {
  const date = dateFromKey(key);
  const offset = state.weekStartMode === 'SUNDAY'
    ? date.getDay()
    : (date.getDay() + 6) % 7;
  date.setDate(date.getDate() - offset);
  return dayKey(date);
}

function monthGridStart(key) {
  const date = dateFromKey(key);
  date.setDate(1);
  return startOfCalendarWeek(dayKey(date));
}

function getVisibleEventKeys() {
  if (state.eventViewMode === 'month') {
    const start = monthGridStart(state.selectedEventDay);
    return Array.from({ length: 42 }, (_, index) => addDays(start, index));
  }
  const count = visibleEventDayCount();
  const start = state.eventViewMode === 'week' ? startOfCalendarWeek(state.selectedEventDay) : state.selectedEventDay;
  return Array.from({ length: count }, (_, index) => addDays(start, index));
}

function renderEventDayHeader(key, items) {
  const classes = ['event-day-header'];
  const today = dayKey(new Date());
  if (key < today) classes.push('past');
  if (key === today) classes.push('today');
  if (key === state.selectedEventDay) classes.push('selected');
  const date = dateFromKey(key);
  return ''
    + '<button class="' + classes.join(' ') + '" data-day="' + key + '">'
    +   '<div class="event-day-header-weekday">' + escapeHtml(formatWeekday(date)) + '</div>'
    +   '<div class="event-day-header-date">' + escapeHtml(String(date.getDate())) + '</div>'
    +   (items.length ? '<div class="event-day-header-meta">' + items.length + '</div>' : '')
    + '</button>';
}

function renderEventDayAllDayStrip(key, allDayItems) {
  if (!allDayItems.length) return '';
  return '<div class="event-all-day-strip">' + allDayItems.map(item => renderAllDayEventPill(item, key)).join('') + '</div>';
}

function renderAllDayEventPill(item, key) {
  const accent = item.accentColorHex || item.groupColorHex || DEFAULT_EVENT_COLOR;
  const startKey = dayKeyFromMillis(eventStart(item));
  const endKey = dayKeyFromMillis(Math.max(eventStart(item), eventEnd(item) - 1));
  const range = startKey === endKey ? '全天' : formatCompactDateLabel(startKey) + ' - ' + formatCompactDateLabel(endKey);
  const classes = ['event-all-day-pill'];
  if (eventEnd(item) <= Date.now()) classes.push('past-event');
  return ''
    + '<button type="button" class="' + classes.join(' ') + '" data-event-id="' + escapeHtml(String(item.id ?? '')) + '" style="--accent:' + accent + '">'
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
  const first = dateFromKey(keys[0]);
  const last = dateFromKey(keys[keys.length - 1]);
  if (state.eventViewMode === 'month') {
    const selected = dateFromKey(state.selectedEventDay);
    return selected.getFullYear() + '年' + (selected.getMonth() + 1) + '月';
  }
  if (keys.length === 1) return first.getFullYear() + '年' + (first.getMonth() + 1) + '月' + first.getDate() + '日';
  if (first.getFullYear() === last.getFullYear() && first.getMonth() === last.getMonth()) {
    return first.getFullYear() + '年' + (first.getMonth() + 1) + '月' + first.getDate() + '日 - ' + last.getDate() + '日';
  }
  return formatCompactDateLabel(keys[0]) + ' - ' + formatCompactDateLabel(keys[keys.length - 1]);
}

function renderEventCard(segment) {
  const item = segment.item;
  const accent = item.accentColorHex || item.groupColorHex || DEFAULT_EVENT_COLOR;
  const meta = [item.groupName || '未分组', item.location || '', item.isRecurring ? '循环' : ''].filter(Boolean).join(' · ');
  const classes = ['event-card'];
  if (item.completed === true) classes.push('completed-event');
  if (eventEnd(item) <= Date.now()) classes.push('past-event');
  return ''
    + '<article class="' + classes.join(' ') + '" role="button" tabindex="0" aria-label="编辑日程：' + escapeHtml(item.title) + '" data-event-id="' + escapeHtml(String(item.id ?? '')) + '" style="--accent:' + accent + ';top:' + segment.top + 'px;height:' + segment.height + 'px">'
    +   '<div class="event-card-title">' + escapeHtml(item.title) + '</div>'
    +   '<div class="event-card-meta">' + escapeHtml(segment.startLabel) + ' - ' + escapeHtml(segment.endLabel) + '</div>'
    +   '<div class="event-card-meta">' + escapeHtml(meta || '定时日程') + '</div>'
    +   (item.notes ? '<div class="event-card-notes">' + escapeHtml(item.notes) + '</div>' : '')
    + '</article>';
}

function eventMatchesCalendarFilters(item) {
  const hiddenGroups = new Set((state.hiddenEventGroupIds || []).map(String));
  if (hiddenGroups.has(String(item.groupId ?? ''))) return false;
  const query = String(state.eventQuery || '').trim().toLocaleLowerCase();
  if (!query) return true;
  return [item.title, item.location, item.notes, item.groupName]
    .filter(Boolean)
    .some(value => String(value).toLocaleLowerCase().includes(query));
}

function renderCalendarGroups() {
  if (!els.eventCalendarGroups) return;
  const hiddenGroups = new Set((state.hiddenEventGroupIds || []).map(String));
  const groups = state.snapshot?.groups || [];
  els.eventCalendarGroups.innerHTML = groups.length
    ? groups.map(group => {
      const groupId = String(group.id ?? '');
      const color = group.colorHex || '#336DF4';
      return ''
        + '<label class="calendar-group-row">'
        +   '<input type="checkbox" data-calendar-group-id="' + escapeHtml(groupId) + '" ' + (hiddenGroups.has(groupId) ? '' : 'checked') + ' />'
        +   '<span class="calendar-group-color" style="--group-color:' + escapeHtml(color) + '"></span>'
        +   '<span>' + escapeHtml(group.name || '未命名分组') + '</span>'
        + '</label>';
    }).join('')
    : '<div class="calendar-side-empty">暂无分组</div>';
}

function miniCalendarKeys(monthKey) {
  const start = monthGridStart(monthKey);
  return Array.from({ length: 42 }, (_, index) => addDays(start, index));
}

function renderMiniCalendar() {
  if (!els.eventMiniCalendar || !els.eventMiniMonthTitle) return;
  const monthDate = dateFromKey(state.eventMiniMonth);
  const visibleSet = state.eventViewMode === 'month' ? new Set() : new Set(getVisibleEventKeys());
  const today = dayKey(new Date());
  els.eventMiniMonthTitle.textContent = monthDate.getFullYear() + '年' + (monthDate.getMonth() + 1) + '月';
  els.eventMiniCalendar.innerHTML = miniCalendarKeys(state.eventMiniMonth).map(key => {
    const date = dateFromKey(key);
    const classes = ['mini-calendar-day'];
    if (date.getMonth() !== monthDate.getMonth()) classes.push('outside');
    if (visibleSet.has(key)) classes.push('in-range');
    if (key === state.selectedEventDay) classes.push('selected');
    if (key === today) classes.push('today');
    return '<button type="button" class="' + classes.join(' ') + '" data-mini-calendar-day="' + key + '">' + date.getDate() + '</button>';
  }).join('');
}

function renderMonthEventPill(item) {
  const accent = item.accentColorHex || item.groupColorHex || DEFAULT_EVENT_COLOR;
  const time = item.allDay ? '全天' : formatTimeLabel(eventStart(item));
  const classes = ['month-event-pill'];
  if (eventEnd(item) <= Date.now()) classes.push('past-event');
  return ''
    + '<button type="button" class="' + classes.join(' ') + '" data-event-id="' + escapeHtml(String(item.id ?? '')) + '" style="--accent:' + escapeHtml(accent) + '">'
    +   '<span>' + escapeHtml(time) + '</span>'
    +   '<strong>' + escapeHtml(item.title || '未命名日程') + '</strong>'
    + '</button>';
}

function renderEventMonthGrid(visibleDays) {
  const selectedMonth = dateFromKey(state.selectedEventDay).getMonth();
  const weekdayHead = (state.weekStartMode === 'SUNDAY'
    ? ['日', '一', '二', '三', '四', '五', '六']
    : ['一', '二', '三', '四', '五', '六', '日'])
    .map(label => '<div class="month-weekday">' + label + '</div>').join('');
  const cells = visibleDays.map(day => {
    const date = dateFromKey(day.key);
    const classes = ['month-day-cell'];
    if (date.getMonth() !== selectedMonth) classes.push('outside');
    const today = dayKey(new Date());
    if (day.key < today) classes.push('past');
    if (day.key === today) classes.push('today');
    return ''
      + '<div class="' + classes.join(' ') + '" data-month-day="' + day.key + '">'
      +   '<button type="button" class="month-day-number" data-day="' + day.key + '">' + date.getDate() + '</button>'
      +   '<div class="month-day-events">' + day.items.slice(0, 3).map(renderMonthEventPill).join('') + '</div>'
      +   (day.items.length > 3 ? '<button type="button" class="month-more" data-day="' + day.key + '">还有 ' + (day.items.length - 3) + ' 项</button>' : '')
      + '</div>';
  }).join('');
  return '<div class="event-month-view"><div class="month-weekday-row">' + weekdayHead + '</div><div class="month-grid">' + cells + '</div></div>';
}

function currentWeekdayLabels() {
  return state.weekStartMode === 'SUNDAY'
    ? ['日', '一', '二', '三', '四', '五', '六']
    : ['一', '二', '三', '四', '五', '六', '日'];
}

function renderWeekStartControl() {
  if (els.calendarWeekStart) els.calendarWeekStart.value = state.weekStartMode;
  if (els.eventMiniWeekdays) {
    els.eventMiniWeekdays.innerHTML = currentWeekdayLabels()
      .map(label => '<span>' + label + '</span>')
      .join('');
  }
}

function eventPayloadFromItem(item, startMillis, endMillis) {
  const recurrenceType = item.isRecurring ? (item.recurrenceType || 'NONE') : 'NONE';
  return {
    title: item.title || '',
    groupId: Number(item.groupId || 0),
    location: item.location || '',
    notes: item.notes || '',
    startAt: formatDateTimeLocalValue(startMillis),
    endAt: formatDateTimeLocalValue(endMillis),
    allDay: item.allDay === true,
    accentColorHex: item.accentColorHex || item.groupColorHex || DEFAULT_EVENT_COLOR,
    reminderOffsetsMinutes: item.reminderEnabled === true ? (item.reminderOffsetsMinutes || []) : [],
    countdownEnabled: item.countdownEnabled === true,
    checkInEnabled: item.checkInEnabled === true,
    ringEnabled: item.ringEnabled !== false,
    vibrateEnabled: item.vibrateEnabled !== false,
    reminderDeliveryMode: 'FULLSCREEN_AND_NOTIFICATION',
    multiSlotBundleId: item.multiSlotBundleId || null,
    syncMultiSlotBundleSharedFields: false,
    scope: 'CURRENT',
    recurrence: {
      enabled: item.isRecurring === true && recurrenceType !== 'NONE',
      type: recurrenceType,
      weeklyDays: parseWeekdays(item.recurrenceWeekdays),
      endDate: item.recurrenceEndDate || null
    }
  };
}

async function moveEventByDrag(item, startMillis, endMillis) {
  if (!item?.id) return;
  const payload = eventPayloadFromItem(item, startMillis, endMillis);
  await api('/api/events/' + encodeURIComponent(item.id), { method: 'PUT', body: JSON.stringify(payload) });
  state.eventsLoaded = false;
  await ensureEventData();
  els.status.textContent = '已移动日程到 ' + formatDateTimeLabel(startMillis);
}

function beginEventDrag(pointerEvent, node) {
  if (!pointerEvent.isPrimary || pointerEvent.button !== 0) return;
  const item = findEventById(node.dataset.eventId);
  if (!item || item.allDay) return;
  const originalStart = eventStart(item);
  const originalEnd = eventEnd(item);
  const duration = Math.max(THIRTY_MINUTES, originalEnd - originalStart);
  const originRect = node.getBoundingClientRect();
  const pointerOffsetMinutes = Math.max(0, Math.round(((pointerEvent.clientY - originRect.top) / HOUR_HEIGHT) * 60));
  const startX = pointerEvent.clientX;
  const startY = pointerEvent.clientY;
  let moved = false;
  let targetStart = originalStart;

  const resolveTargetStart = event => {
    const column = document.elementFromPoint(event.clientX, event.clientY)?.closest?.('[data-column-day]');
    if (!column) return targetStart;
    const rect = column.getBoundingClientRect();
    const relativeY = Math.max(0, Math.min(rect.height, event.clientY - rect.top));
    const minutes = Math.max(0, Math.min(24 * 60 - 15, Math.round((relativeY / HOUR_HEIGHT) * 60) - pointerOffsetMinutes));
    const snappedMinutes = Math.round(minutes / 15) * 15;
    return dayStartMillis(column.dataset.columnDay) + snappedMinutes * 60 * 1000;
  };

  const onMove = event => {
    if (!event.isPrimary) return;
    const dx = event.clientX - startX;
    const dy = event.clientY - startY;
    if (Math.abs(dx) > 4 || Math.abs(dy) > 4) moved = true;
    if (!moved) return;
    node.classList.add('dragging');
    node.style.pointerEvents = 'none';
    targetStart = resolveTargetStart(event);
    node.style.transform = 'translate(' + dx + 'px,' + dy + 'px)';
  };

  const cleanup = () => {
    node.releasePointerCapture?.(pointerEvent.pointerId);
    node.removeEventListener('pointermove', onMove);
    node.removeEventListener('pointerup', onUp);
    node.removeEventListener('pointercancel', onCancel);
    node.classList.remove('dragging');
    node.style.transform = '';
    node.style.pointerEvents = '';
  };

  const onCancel = () => cleanup();
  const onUp = async event => {
    if (!moved) {
      cleanup();
      return;
    }
    state.eventDragSuppressClickUntil = Date.now() + 450;
    targetStart = snapToQuarterHour(resolveTargetStart(event));
    cleanup();
    if (targetStart === originalStart) return;
    try {
      els.status.textContent = '正在移动日程…';
      await moveEventByDrag(item, targetStart, targetStart + duration);
    } catch (error) {
      els.status.textContent = error.message || '移动日程失败';
      await ensureEventData().catch(() => {});
    }
  };

  node.setPointerCapture?.(pointerEvent.pointerId);
  node.addEventListener('pointermove', onMove);
  node.addEventListener('pointerup', onUp);
  node.addEventListener('pointercancel', onCancel);
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
  const boardMode = state.currentTab === 'board';
  const todoMode = state.currentTab === 'todos';
  const eventMode = state.currentTab === 'events';
  const planningMode = state.currentTab === 'planning';
  const reportMode = state.currentTab === 'reports';
  document.body.dataset.activeTab = state.currentTab;
  els.panelTitle.textContent = boardMode ? '每日看板' : todoMode ? '待办' : eventMode ? '日历' : planningMode ? '规划台' : 'AI 报告';
  els.viewCaption.textContent = boardMode ? '今天真正需要处理的事情' : todoMode ? '任务与历史记录' : eventMode ? '按周查看与安排日程' : planningMode ? '备忘录式规划与识别' : '日报调度、AI 状态与历史记录';
  els.openCreate.textContent = eventMode ? '创建日程' : todoMode || boardMode ? '新增待办' : '新建规划';
  els.openCreate.classList.toggle('hidden', planningMode || reportMode);
  const dataMode = todoMode && state.todosLoaded
    ? '待办按需数据'
    : eventMode && visibleEventRangeLoaded()
      ? '日程范围数据'
      : reportMode
        ? '日报诊断数据'
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
  const token = els.token.value.trim().toUpperCase();
  if (!token) {
    els.status.textContent = '请输入手机端显示的 4 位访问密钥';
    return;
  }
  els.token.value = token;
  state.token = token;
  state.planningLoaded = false;
  els.status.textContent = '正在连接手机…';
  await loadSnapshot({ planning: false });
  startDesktopHeartbeat();
  els.status.textContent = '已连接';
  showAppShell();
}

function disconnect() {
  stopDesktopHeartbeat();
  state.token = '';
  state.snapshot = null;
  if (els.token) els.token.value = '';
  els.status.textContent = '已断开连接';
  showLoginScreen();
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
      state.token = '';
      els.status.textContent = '电脑同步连接已断开，请在手机端重新开启后再连接';
      showLoginScreen();
    }
  }, 60 * 1000);
}

async function loadSnapshot(options = {}) {
  const shouldLoadPlanning = options.planning === true || state.currentTab === 'planning';
  state.snapshot = await api('/api/snapshot?scope=board');
  state.weekStartMode = state.snapshot.weekStartMode === 'SUNDAY' ? 'SUNDAY' : 'MONDAY';
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
    weekStartMode: 'MONDAY',
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
  if (state.currentTab === 'reports') await loadAiReportWorkspace();
}

async function loadAiReportStatus() {
  state.aiReportStatus = await api('/api/ai-report/status');
  renderAiReportStatus();
}

function createAiConfigDraft(config) {
  return {
    enabled: config?.enabled === true,
    providers: (config?.providers || []).map(provider => ({ ...provider, apiKey: '' })),
    dailyReportEnabled: config?.dailyReportEnabled === true,
    dailyReportTime: config?.dailyReportTime || '22:00',
    weeklyReportEnabled: config?.weeklyReportEnabled === true,
    weeklyReportTime: config?.weeklyReportTime || '22:00',
    retention: config?.retention || 'DAYS_90',
    retentionOptions: config?.retentionOptions || []
  };
}

async function loadAiReportWorkspace() {
  const [status, config] = await Promise.all([
    api('/api/ai-report/status'),
    api('/api/ai-config')
  ]);
  state.aiReportStatus = status;
  state.aiConfigDraft = createAiConfigDraft(config);
  state.aiProviderModels = {};
  state.aiProviderFeedback = {};
  renderAiReportStatus();
}

function aiProviderById(id) {
  return (state.aiConfigDraft?.providers || []).find(provider => String(provider.id) === String(id));
}

function syncAiDraftFromDom() {
  const draft = state.aiConfigDraft;
  if (!draft || !els.aiReportStatus) return draft;
  const globalEnabled = els.aiReportStatus.querySelector('[data-ai-global-enabled]');
  if (globalEnabled) draft.enabled = globalEnabled.checked;
  ['dailyReportEnabled', 'weeklyReportEnabled'].forEach(field => {
    const node = els.aiReportStatus.querySelector('[data-ai-field="' + field + '"]');
    if (node) draft[field] = node.checked;
  });
  ['dailyReportTime', 'weeklyReportTime', 'retention'].forEach(field => {
    const node = els.aiReportStatus.querySelector('[data-ai-field="' + field + '"]');
    if (node) draft[field] = node.value;
  });
  els.aiReportStatus.querySelectorAll('[data-ai-provider]').forEach(card => {
    const provider = aiProviderById(card.dataset.aiProvider);
    if (!provider) return;
    card.querySelectorAll('[data-provider-field]').forEach(node => {
      const field = node.dataset.providerField;
      provider[field] = node.type === 'checkbox' ? node.checked : node.value;
    });
  });
  return draft;
}

function aiProviderPayload(provider) {
  return {
    id: provider.id,
    name: provider.name || '',
    baseUrl: provider.baseUrl || '',
    apiKey: provider.apiKey || '',
    model: provider.model || '',
    enabled: provider.enabled !== false,
    supportsVision: provider.supportsVision === true
  };
}

function validateAiConfigDraft(draft) {
  const timePattern = /^(?:[01]\d|2[0-3]):[0-5]\d$/;
  if (!timePattern.test(draft.dailyReportTime || '')) return '日报时间需要使用 HH:mm 格式';
  if (!timePattern.test(draft.weeklyReportTime || '')) return '周报时间需要使用 HH:mm 格式';
  if (draft.enabled) {
    const invalid = draft.providers.find(provider => provider.enabled !== false && (
      !String(provider.baseUrl || '').trim() ||
      (!provider.apiKeyConfigured && !String(provider.apiKey || '').trim()) ||
      !String(provider.model || '').trim()
    ));
    if (invalid) return '启用的 AI 源需要填写 Base URL、API Key 和模型名';
  }
  return '';
}

async function saveAiConfigDraft() {
  const draft = syncAiDraftFromDom();
  const validation = validateAiConfigDraft(draft);
  if (validation) throw new Error(validation);
  const data = await api('/api/ai-config', {
    method: 'PUT',
    body: JSON.stringify({
      enabled: draft.enabled,
      providers: draft.providers.map(aiProviderPayload),
      dailyReportEnabled: draft.dailyReportEnabled,
      dailyReportTime: draft.dailyReportTime,
      weeklyReportEnabled: draft.weeklyReportEnabled,
      weeklyReportTime: draft.weeklyReportTime,
      retention: draft.retention
    })
  });
  state.aiConfigDraft = createAiConfigDraft(data.config);
  state.aiProviderFeedback = {};
  await loadAiReportStatus();
  els.status.textContent = 'AI 调用与日报设置已保存';
}

async function fetchAiProviderModels(providerId) {
  syncAiDraftFromDom();
  const provider = aiProviderById(providerId);
  if (!provider) return;
  state.aiProviderFeedback[providerId] = '正在获取模型…';
  renderAiReportStatus();
  const data = await api('/api/ai/providers/models', {
    method: 'POST',
    body: JSON.stringify({ provider: aiProviderPayload(provider) })
  });
  if (!data.ok) {
    state.aiProviderFeedback[providerId] = '获取失败：' + (data.message || '未知错误');
  } else {
    state.aiProviderModels[providerId] = data.models || [];
    if (!provider.model && state.aiProviderModels[providerId].length) provider.model = state.aiProviderModels[providerId][0];
    state.aiProviderFeedback[providerId] = '已获取 ' + state.aiProviderModels[providerId].length + ' 个模型';
  }
  renderAiReportStatus();
}

async function testAiProvider(providerId) {
  syncAiDraftFromDom();
  const provider = aiProviderById(providerId);
  if (!provider) return;
  state.aiProviderFeedback[providerId] = '正在发送单次测活请求…';
  renderAiReportStatus();
  const data = await api('/api/ai/providers/test', {
    method: 'POST',
    body: JSON.stringify({ provider: aiProviderPayload(provider) })
  });
  state.aiProviderFeedback[providerId] = data.ok
    ? ('连接成功：' + (data.responseText || data.model || '模型已响应'))
    : ('连接失败：' + (data.message || '未知错误'));
  renderAiReportStatus();
}

function renderAiProviderCard(provider, index, total) {
  const models = state.aiProviderModels[provider.id] || [];
  const feedback = state.aiProviderFeedback[provider.id] || '';
  const keyHint = provider.apiKeyConfigured
    ? ('已保存在手机，留空则保留' + (provider.apiKeyPreview ? '（尾号 ' + provider.apiKeyPreview + '）' : ''))
    : '请输入 API Key';
  const modelOptions = models.map(model => '<option value="' + escapeHtml(model) + '"></option>').join('');
  return ''
    + '<article class="ai-provider-card" data-ai-provider="' + escapeHtml(provider.id) + '">'
    +   '<div class="ai-provider-head"><div><strong>' + escapeHtml(provider.name || ('AI 源 ' + (index + 1))) + '</strong><span>优先级 ' + (index + 1) + '</span></div><label class="report-switch"><input type="checkbox" data-provider-field="enabled" ' + (provider.enabled !== false ? 'checked' : '') + ' /><span>启用</span></label></div>'
    +   '<div class="ai-provider-grid">'
    +     '<label><span>显示名</span><input data-provider-field="name" value="' + escapeHtml(provider.name || '') + '" placeholder="例如 DeepSeek" /></label>'
    +     '<label><span>模型名</span><input data-provider-field="model" list="ai-models-' + escapeHtml(provider.id) + '" value="' + escapeHtml(provider.model || '') + '" placeholder="例如 deepseek-chat" /><datalist id="ai-models-' + escapeHtml(provider.id) + '">' + modelOptions + '</datalist></label>'
    +     '<label class="span-2"><span>Base URL</span><input data-provider-field="baseUrl" value="' + escapeHtml(provider.baseUrl || '') + '" placeholder="https://api.example.com/v1" /></label>'
    +     '<label class="span-2"><span>API Key</span><input data-provider-field="apiKey" type="password" value="' + escapeHtml(provider.apiKey || '') + '" placeholder="' + escapeHtml(keyHint) + '" autocomplete="new-password" /></label>'
    +   '</div>'
    +   '<div class="ai-provider-actions">'
    +     '<label class="report-switch"><input type="checkbox" data-provider-field="supportsVision" ' + (provider.supportsVision ? 'checked' : '') + ' /><span>支持图片识别</span></label>'
    +     '<span class="ai-provider-action-spacer"></span>'
    +     '<button type="button" class="ghost mini" data-ai-provider-up="' + escapeHtml(provider.id) + '" ' + (index === 0 ? 'disabled' : '') + '>上移</button>'
    +     '<button type="button" class="ghost mini" data-ai-provider-down="' + escapeHtml(provider.id) + '" ' + (index === total - 1 ? 'disabled' : '') + '>下移</button>'
    +     '<button type="button" class="ghost mini" data-ai-fetch-models="' + escapeHtml(provider.id) + '">获取模型</button>'
    +     '<button type="button" class="ghost mini" data-ai-test-provider="' + escapeHtml(provider.id) + '">测试连接</button>'
    +     '<button type="button" class="ghost mini danger-lite-action" data-ai-provider-delete="' + escapeHtml(provider.id) + '">删除</button>'
    +   '</div>'
    +   (feedback ? '<div class="ai-provider-feedback">' + escapeHtml(feedback) + '</div>' : '')
    + '</article>';
}

function reportTypeLabel(type) {
  return type === 'WEEKLY' ? '周报' : type === 'DAILY' ? '日报' : '暂无';
}

function reportScheduleLabel(enabled, hour, minute, nextAtMillis, mode) {
  if (!enabled) return '未启用';
  const time = String(hour).padStart(2, '0') + ':' + String(minute).padStart(2, '0');
  const next = Number(nextAtMillis || 0) > 0 ? formatDateTimeLabel(nextAtMillis) : '等待系统安排';
  const modeText = mode === 'EXACT' ? '精确闹钟' : mode === 'INEXACT' ? '系统弹性调度' : '调度方式未知';
  return time + ' · 下次 ' + next + ' · ' + modeText;
}

function renderAiReportStatus() {
  if (!els.aiReportStatus) return;
  const data = state.aiReportStatus;
  if (!data) {
    els.aiReportStatus.innerHTML = '<div class="empty-state">尚未读取 AI 日报状态。</div>';
    return;
  }
  const runtime = data.runtime || {};
  const reports = data.recentReports || [];
  const draft = state.aiConfigDraft;
  const successText = runtime.lastSuccessAtMillis
    ? reportTypeLabel(runtime.lastSuccessType) + ' · ' + formatDateTimeLabel(runtime.lastSuccessAtMillis)
    : '尚无成功记录';
  const sourceText = runtime.lastSuccessAtMillis
    ? (runtime.lastSuccessUsedLocalFallback ? '本地模板' : (runtime.lastSuccessProviderName || 'AI 源'))
    : '—';
  const failure = runtime.lastFailureMessage || runtime.lastAiFailureMessage || '';
  const healthClass = runtime.lastFailureMessage ? 'danger' : runtime.lastSuccessAtMillis ? 'success' : 'neutral';
  const reportRows = reports.length ? reports.map(report => ''
    + '<article class="report-history-row">'
    +   '<div class="report-history-icon ' + (report.isLocalFallback ? 'local' : 'ai') + '">' + (report.type === 'WEEKLY' ? '周' : '日') + '</div>'
    +   '<div class="report-history-main">'
    +     '<div class="report-history-title"><strong>' + escapeHtml(report.type === 'WEEKLY' ? 'AI 周报' : 'AI 日报') + '</strong><span>' + escapeHtml(formatDateTimeLabel(report.generatedAtMillis)) + '</span></div>'
    +     '<p>' + escapeHtml(report.preview || '暂无摘要') + '</p>'
    +     '<small>' + escapeHtml(report.isLocalFallback ? '本地模板兜底' : (report.providerName || 'AI 源')) + '</small>'
    +   '</div>'
    + '</article>').join('') : '<div class="report-empty">尚无日报或周报记录。</div>';
  const retentionOptions = (draft?.retentionOptions || []).map(option =>
    '<option value="' + escapeHtml(option.value) + '" ' + (option.value === draft.retention ? 'selected' : '') + '>' + escapeHtml(option.label) + '</option>'
  ).join('');
  const configSection = draft ? ''
    + '<section class="report-config">'
    +   '<div class="report-config-head"><div><div class="eyebrow">手机端共享配置</div><h2>AI 调用与报告</h2><p>所有内容先保留为本页草稿；只有点击“完成”才写入手机，直接离开不会保存。API Key 不会从手机回传到浏览器。</p></div><button type="button" class="sheet-primary-action" data-ai-save="true">完成</button></div>'
    +   '<div class="report-config-grid">'
    +     '<article class="report-config-card"><div><strong>启用 AI 调用</strong><p>规划台识别和 AI 日报按下面的顺序尝试可用源，全部失败时日报使用本地模板。</p></div><label class="report-switch"><input type="checkbox" data-ai-global-enabled ' + (draft.enabled ? 'checked' : '') + ' /><span>' + (draft.enabled ? '已启用' : '未启用') + '</span></label></article>'
    +     '<article class="report-config-card report-schedule-card"><div><strong>日报</strong><p>每天设定时间生成。</p></div><label class="report-switch"><input type="checkbox" data-ai-field="dailyReportEnabled" ' + (draft.dailyReportEnabled ? 'checked' : '') + ' /><span>启用</span></label><input type="time" data-ai-field="dailyReportTime" value="' + escapeHtml(draft.dailyReportTime) + '" /></article>'
    +     '<article class="report-config-card report-schedule-card"><div><strong>周报</strong><p>每周日设定时间生成。</p></div><label class="report-switch"><input type="checkbox" data-ai-field="weeklyReportEnabled" ' + (draft.weeklyReportEnabled ? 'checked' : '') + ' /><span>启用</span></label><input type="time" data-ai-field="weeklyReportTime" value="' + escapeHtml(draft.weeklyReportTime) + '" /></article>'
    +     '<article class="report-config-card report-retention-card"><div><strong>报告保留时长</strong><p>生成新报告后自动清理超过保留期的记录。</p></div><select data-ai-field="retention">' + retentionOptions + '</select></article>'
    +   '</div>'
    +   '<div class="ai-provider-section-head"><div><h3>AI 源</h3><p>支持添加多个 OpenAI 兼容服务；测试连接只发送一条固定消息，不做并发探测或自动重试。</p></div><button type="button" class="ghost mini" data-ai-provider-add="true">添加 AI 源</button></div>'
    +   '<div class="ai-provider-list">' + (draft.providers.length
          ? draft.providers.map((provider, index) => renderAiProviderCard(provider, index, draft.providers.length)).join('')
          : '<div class="report-empty">尚未添加 AI 源。日报仍可使用本地模板。</div>') + '</div>'
    +   '<div class="report-config-footer"><button type="button" class="ghost" data-ai-generate-daily="true">立即生成一次日报</button><span>生成结果会写入手机端“AI 报告”归档。</span></div>'
    + '</section>' : '<section class="report-warning"><strong>AI 配置尚未加载</strong><p>刷新页面后重试。</p></section>';
  els.aiReportStatus.innerHTML = configSection
    + '<section class="report-hero">'
    +   '<div><div class="eyebrow">运行诊断</div><h2>AI 日报是否按计划工作，一眼就能看清</h2><p>这里不会显示 API Key 或 Base URL，只展示调度与生成结果。</p></div>'
    +   '<div class="report-health ' + healthClass + '"><span>最近结果</span><strong>' + escapeHtml(successText) + '</strong><small>' + escapeHtml(sourceText) + '</small></div>'
    + '</section>'
    + '<section class="report-stat-grid">'
    +   '<article class="report-stat"><span>日报计划</span><strong>' + escapeHtml(data.dailyEnabled ? '已启用' : '未启用') + '</strong><p>' + escapeHtml(data.dailyEnabled ? reportScheduleLabel(true, data.dailyHour, data.dailyMinute, runtime.nextDailyAtMillis, runtime.nextDailyScheduleMode) : ('在手机设置中开启后，每天 ' + String(data.dailyHour).padStart(2, '0') + ':' + String(data.dailyMinute).padStart(2, '0') + ' 生成')) + '</p></article>'
    +   '<article class="report-stat"><span>周报计划</span><strong>' + escapeHtml(data.weeklyEnabled ? '已启用' : '未启用') + '</strong><p>' + escapeHtml(data.weeklyEnabled ? reportScheduleLabel(true, data.weeklyHour, data.weeklyMinute, runtime.nextWeeklyAtMillis, runtime.nextWeeklyScheduleMode) : ('在手机设置中开启后，每周日 ' + String(data.weeklyHour).padStart(2, '0') + ':' + String(data.weeklyMinute).padStart(2, '0') + ' 生成')) + '</p></article>'
    +   '<article class="report-stat"><span>AI 调用</span><strong>' + escapeHtml(data.aiEnabled ? ('已启用 · ' + data.enabledProviderCount + ' 个源') : '未启用') + '</strong><p>' + escapeHtml(data.aiEnabled ? ('将按配置顺序尝试 ' + data.enabledProviderCount + ' 个 AI 源，全部失败时自动使用本地模板') : '当前直接使用本地模板；启用 AI 后可生成更完整的复盘') + '</p></article>'
    + '</section>'
    + (failure ? '<section class="report-warning"><strong>' + escapeHtml(runtime.lastFailureMessage ? '日报链路最近发生异常' : '最近一次 AI 调用失败，报告已用本地模板生成') + '</strong><p>' + escapeHtml(failure) + '</p></section>' : '')
    + '<section class="report-history"><div class="report-section-head"><div><h3>最近报告</h3><p>按生成时间倒序显示，便于确认昨晚是否真正写入数据库。</p></div><button type="button" class="ghost mini" data-refresh-ai-report="true">刷新状态</button></div>' + reportRows + '</section>';
  document.querySelector('[data-refresh-ai-report]')?.addEventListener('click', () => loadAiReportWorkspace().catch(error => { els.status.textContent = error.message; }));
}

function bindAiReportControls() {
  els.aiReportStatus?.addEventListener('click', async event => {
    const save = event.target.closest?.('[data-ai-save]');
    const add = event.target.closest?.('[data-ai-provider-add]');
    const remove = event.target.closest?.('[data-ai-provider-delete]');
    const moveUp = event.target.closest?.('[data-ai-provider-up]');
    const moveDown = event.target.closest?.('[data-ai-provider-down]');
    const fetchModels = event.target.closest?.('[data-ai-fetch-models]');
    const test = event.target.closest?.('[data-ai-test-provider]');
    const generate = event.target.closest?.('[data-ai-generate-daily]');
    try {
      if (save) {
        save.disabled = true;
        save.textContent = '保存中…';
        await saveAiConfigDraft();
        return;
      }
      if (add) {
        syncAiDraftFromDom();
        const id = globalThis.crypto?.randomUUID?.() || ('provider-' + Date.now());
        state.aiConfigDraft.providers.push({
          id,
          name: '',
          baseUrl: '',
          apiKey: '',
          model: '',
          enabled: true,
          supportsVision: false,
          apiKeyConfigured: false,
          apiKeyPreview: ''
        });
        renderAiReportStatus();
        return;
      }
      if (remove) {
        syncAiDraftFromDom();
        state.aiConfigDraft.providers = state.aiConfigDraft.providers.filter(provider => String(provider.id) !== String(remove.dataset.aiProviderDelete));
        renderAiReportStatus();
        return;
      }
      if (moveUp || moveDown) {
        syncAiDraftFromDom();
        const id = (moveUp || moveDown).dataset[moveUp ? 'aiProviderUp' : 'aiProviderDown'];
        const index = state.aiConfigDraft.providers.findIndex(provider => String(provider.id) === String(id));
        const target = index + (moveUp ? -1 : 1);
        if (index >= 0 && target >= 0 && target < state.aiConfigDraft.providers.length) {
          const providers = state.aiConfigDraft.providers.slice();
          [providers[index], providers[target]] = [providers[target], providers[index]];
          state.aiConfigDraft.providers = providers;
          renderAiReportStatus();
        }
        return;
      }
      if (fetchModels) {
        await fetchAiProviderModels(fetchModels.dataset.aiFetchModels);
        return;
      }
      if (test) {
        await testAiProvider(test.dataset.aiTestProvider);
        return;
      }
      if (generate) {
        generate.disabled = true;
        generate.textContent = '生成中…';
        const result = await api('/api/ai-report/generate-daily', { method: 'POST', body: '{}' });
        els.status.textContent = result.message || '日报已生成';
        await loadAiReportStatus();
      }
    } catch (error) {
      els.status.textContent = error.message || 'AI 配置操作失败';
      renderAiReportStatus();
    }
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
    els.todoTimeline.innerHTML = ''
      + '<div class="empty-state">'
      +   '<strong>待办</strong><br />'
      +   '加载完整待办后，可按分组筛选并查看历史待办。'
      +   '<div class="actions"><button type="button" class="ghost mini" data-load-todos="true">加载待办管理列表</button></div>'
      + '</div>';
    bindActions();
    return;
  }
  const activeAll = activeTodos();
  const historyAll = historyTodos();
  const active = activeAll.filter(todoMatchesSelectedGroups).slice().sort((a, b) => (a.hasDueDate ? (a.dueAtMillis || Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER) - (b.hasDueDate ? (b.dueAtMillis || Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER));
  const history = historyAll.filter(todoMatchesSelectedGroups).slice().sort((a, b) => (b.completedAtMillis || b.canceledAtMillis || b.missedAtMillis || b.dueAtMillis || 0) - (a.completedAtMillis || a.canceledAtMillis || a.missedAtMillis || a.dueAtMillis || 0));
  const sections = [
    { key: 'active', title: '待办', empty: '当前没有未完成的待办。', items: active },
    { key: 'history', title: '历史待办', empty: '暂无历史待办。', items: history }
  ];
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
      + '<button type="button" class="desktop-now-card" data-event-preview-id="' + escapeHtml(String(nextEvent.id ?? '')) + '" style="--accent:' + escapeHtml(nextEvent.accentColorHex || nextEvent.groupColorHex || DEFAULT_EVENT_COLOR) + '">'
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
  const accent = item.itemType === 'EVENT' ? (item.accentColorHex || item.groupColorHex || DEFAULT_EVENT_COLOR) : todoGroupAccent(item);
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
  const accent = item.accentColorHex || item.groupColorHex || DEFAULT_EVENT_COLOR;
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
  document.body.dataset.eventView = state.eventViewMode;
  renderWeekStartControl();
  document.querySelectorAll('[data-event-view]').forEach(button => {
    button.classList.toggle('active', button.dataset.eventView === state.eventViewMode);
  });
  renderMiniCalendar();
  if (!visibleEventRangeLoaded()) {
    els.eventDayHeaders.innerHTML = '';
    els.hourAxis.innerHTML = '';
    els.eventTimeline.innerHTML = '<div class="empty-state">日程时间轴正在加载当前可见日期范围。</div>';
    ensureEventData().catch(err => els.status.textContent = err.message);
    return;
  }
  const events = activeEvents().filter(eventMatchesCalendarFilters).slice().sort((a, b) => eventStart(a) - eventStart(b));
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
  if (els.eventAnchorDate) els.eventAnchorDate.value = state.selectedEventDay;
  els.eventSelectedDate.textContent = renderVisibleRangeTitle(visibleKeys);
  els.eventSelectedSubtitle.textContent = totalTimed + totalAllDay ? ('定时 ' + totalTimed + ' · 全天 ' + totalAllDay) : '';
  if (state.eventViewMode === 'month') {
    els.eventDayHeaders.innerHTML = '';
    els.hourAxis.innerHTML = '';
    els.eventTimeline.innerHTML = renderEventMonthGrid(visibleDays);
  } else {
    els.eventDayHeaders.innerHTML = visibleDays.map(day => renderEventDayHeader(day.key, day.items)).join('');
    els.hourAxis.innerHTML = renderHourAxis();
    els.eventTimeline.innerHTML = visibleDays.map(day => renderEventDayColumn(day.key, day.timed, day.allDay)).join('');
  }
  if (els.boardScroll) {
    if (state.eventViewMode === 'month') {
      els.boardScroll.scrollTop = 0;
    } else if (visibleKeys.includes(dayKey(new Date()))) {
      els.boardScroll.scrollTop = 8 * HOUR_HEIGHT;
    } else {
      const firstTimed = visibleDays.flatMap(day => day.timed).sort((a, b) => a.top - b.top)[0];
      els.boardScroll.scrollTop = firstTimed ? Math.max(0, firstTimed.top - HOUR_HEIGHT) : 0;
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
  document.querySelectorAll('.event-card[data-event-id]').forEach(node => {
    node.onpointerdown = event => beginEventDrag(event, node);
  });
  document.querySelectorAll('[data-event-id]').forEach(node => {
    node.onclick = event => {
      event.preventDefault();
      event.stopPropagation();
      if (Date.now() < state.eventDragSuppressClickUntil) return;
      openEventEditorById(node.dataset.eventId);
    };
    node.onkeydown = event => {
      if (event.key === 'Enter' || event.key === ' ') node.click();
    };
  });
  document.querySelectorAll('[data-month-day]').forEach(node => {
    node.onclick = event => {
      if (event.target.closest('[data-event-id]')) return;
      const key = node.dataset.monthDay;
      const startMillis = dayStartMillis(key) + 9 * 60 * 60 * 1000;
      clearEventForm();
      setEventSeed(startMillis, startMillis + THIRTY_MINUTES);
      openModal('event-modal');
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
      els.planningEditor.value = active.contentMarkdown || state.planningNodesMarkdown || '';
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
    els.planningOutline.innerHTML = '<button type="button" class="planning-outline-empty" data-focus-planning-root="true">当前文档还没有规划事项。点这里或上方输入框，写下第一件事：10:00-12:00 写论文 @图书馆3楼，或 任务M ddl 15:00。</button>';
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
  return els.planningEditor?.value || activePlanningNote()?.contentMarkdown || state.planningNodesMarkdown || '';
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

function planningReminderSummary(item) {
  const raw = String(item.reminderInputText || (item.reminderOffsetsMinutes || []).join(',')).trim();
  const enabled = item.reminderEnabled !== false && raw.length > 0;
  if (!enabled) return '未启用提醒；导入后不会补发过去的提醒。';
  return '提醒：' + raw + ' · 全屏 + 通知栏提醒 · 响铃 + 震动';
}

function editedPlanningCandidates() {
  return (state.planningParseResult && state.planningParseResult.candidates || []).map(item => {
    const edit = state.planningPreviewEdits && state.planningPreviewEdits[String(item.id)];
    return edit ? { ...item, ...edit } : { ...item };
  });
}

function rememberPlanningPreviewEdit(id, patch) {
  const key = String(id);
  state.planningPreviewEdits = state.planningPreviewEdits || {};
  state.planningPreviewEdits[key] = { ...(state.planningPreviewEdits[key] || {}), ...patch };
}

function planningCandidateMessage(item) {
  const message = String(item.message || '').trim();
  if (!message) return '';
  const rawReminder = String(item.reminderInputText || (item.reminderOffsetsMinutes || []).join(',')).trim();
  const reminderEnabled = item.reminderEnabled !== false && rawReminder.length > 0;
  if (reminderEnabled) return message;
  return message.split('；')
    .map(part => part.trim())
    .filter(Boolean)
    .filter(part => !part.includes('提醒时间已经过去') && !part.includes('提醒时间必须晚于当前时间') && !part.includes('DDL 已早于当前时间'))
    .join('；');
}

function planningWeekdayChips(id, recurrenceType, weekdays) {
  const selected = new Set(recurrenceType === 'WEEKLY' ? parseWeekdays(Array.isArray(weekdays) ? weekdays.join(',') : weekdays) : []);
  const hiddenClass = recurrenceType === 'WEEKLY' ? '' : ' hidden';
  return '<label class="planning-edit-field planning-weekday-field' + hiddenClass + '" data-planning-weekday-field="' + escapeHtml(id) + '"><span>每周周几</span>'
    + '<input type="hidden" data-planning-field="recurrenceWeekdays" data-planning-id="' + escapeHtml(id) + '" value="' + escapeHtml(Array.from(selected).sort((a, b) => a - b).join(',')) + '" />'
    + '<div class="weekday-chip-row planning-weekday-chips" data-planning-weekday-chips="' + escapeHtml(id) + '">'
    + WEEKDAY_OPTIONS.map(day => {
      const active = selected.has(day.value);
      return '<button type="button" class="weekday-chip' + (active ? ' active' : '') + '" data-planning-weekday-value="' + day.value + '" aria-pressed="' + (active ? 'true' : 'false') + '">周' + escapeHtml(day.label) + '</button>';
    }).join('')
    + '</div>'
    + '<small class="field-hint">只在重复为“每周”时生效。</small>'
    + '</label>';
}

function planningRecurrenceFields(item) {
  const recurrence = item.recurrence || {};
  const type = recurrence.enabled ? (recurrence.type || 'NONE') : 'NONE';
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
    + planningWeekdayChips(item.id, type, recurrence.weeklyDays);
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

function normalizePlanningMappingLine(raw) {
  return String(raw || '')
    .replace(/\s+#imported(?=\s|$)/gi, '')
    .trim()
    .replace(/\s+/g, ' ');
}

function applyPlanningMappingStatus(item) {
  const lineNumber = Number(item?.lineNumber || 0);
  if (!lineNumber || item?.imported) return item;
  const source = normalizePlanningMappingLine(item.sourceLine || '');
  const matched = (state.planningMappings || []).some(mapping =>
    mapping.status !== 'ORPHANED' &&
    Number(mapping.lastKnownLineNumber || 0) === lineNumber &&
    normalizePlanningMappingLine(mapping.currentLineText || mapping.originalLineText || '') === source
  );
  if (!matched) return item;
  return {
    ...item,
    imported: true,
    importable: false,
    message: item.message || '这一行已经导入过，默认不重复导入。'
  };
}

function renderPlanningPreview() {
  if (!els.planningPreview || !els.planningPreviewMeta) return;
  const result = state.planningParseResult;
  const parseButton = document.getElementById('planning-parse');
  if (parseButton) {
    parseButton.disabled = state.planningParsing;
    parseButton.textContent = state.planningParsing ? '识别中' : '识别计划';
  }
  if (!result) {
    els.planningPreviewMeta.textContent = state.planningParsing ? '识别中…' : '尚未识别 · 在左侧像备忘录一样写完后点“识别计划”';
    const mappingHtml = renderPlanningMappingPreview();
    els.planningPreview.innerHTML = mappingHtml || '<div class="empty-state">左侧是自由书写区。你可以直接粘贴 Word、Excel、聊天记录或手写计划，再点“识别计划”。</div>';
    return;
  }
  const candidates = editedPlanningCandidates().map(applyPlanningMappingStatus);
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
    const candidateMessage = planningCandidateMessage(item);
    const linked = item.type === 'EVENT' ? '<label class="planning-linked"><input type="checkbox" data-planning-linked="' + escapeHtml(item.id) + '"' + (item.createLinkedTodo ? ' checked' : '') + ' /> 同步创建以日程结束时间为 DDL 的待办任务</label>' : '';
    const editFields = editable ? (
      '<div class="planning-edit-grid">'
      + planningEditableField(item.id, 'title', '标题', item.title || '', '任务标题')
      + planningEditableField(item.id, 'groupName', '分组', item.groupName || '', '例行')
      + (item.type === 'TODO'
        ? planningEditableField(item.id, 'dueAt', 'DDL', editableDateTimeValue(item.dueAt), '2026-05-28 14:30')
        : planningEditableField(item.id, 'location', '地点', item.location || '', '@主楼B1-412') + planningEditableField(item.id, 'startAt', '开始', editableDateTimeValue(item.startAt), '2026-05-28 10:00') + planningEditableField(item.id, 'endAt', '结束', editableDateTimeValue(item.endAt), '2026-05-28 12:00'))
      + planningEditableField(item.id, 'reminders', '提醒', item.reminderInputText || (item.reminderOffsetsMinutes || []).join(','), '5,15,16:30,05-10 15:00')
      + '<div class="planning-reminder-channels"><span class="planning-edit-label">提醒</span>'
      + '<label><input type="checkbox" data-planning-reminder-enabled data-planning-id="' + escapeHtml(item.id) + '"' + (item.reminderEnabled !== false ? ' checked' : '') + ' /> 提醒（全屏 + 通知栏）</label>'
      + '</div>'
      + planningRecurrenceFields(item)
      + '</div>'
      + '<label class="planning-edit-field full"><span>备注</span><textarea data-planning-field="notes" data-planning-id="' + escapeHtml(item.id) + '" rows="2">' + escapeHtml(item.notes || '') + '</textarea></label>'
      + '<div class="planning-preview-options">'
      +   (item.type === 'EVENT' ? '<label class="planning-linked"><input type="checkbox" data-planning-flag="allDay" data-planning-id="' + escapeHtml(item.id) + '"' + (item.allDay ? ' checked' : '') + ' /> 全天</label>' : '')
      +   '<label class="planning-linked"><input type="checkbox" data-planning-flag="countdownEnabled" data-planning-id="' + escapeHtml(item.id) + '"' + (item.countdownEnabled ? ' checked' : '') + ' /> 倒数日</label>'
      +   (item.type === 'EVENT' ? '<label class="planning-linked"><input type="checkbox" data-planning-flag="checkInEnabled" data-planning-id="' + escapeHtml(item.id) + '"' + (item.checkInEnabled ? ' checked' : '') + ' /> 打卡追踪</label>' : '')
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
      +   (!editable ? '<div class="planning-meta-line">' + escapeHtml(planningTimeText(item)) + '</div>' : '<div class="planning-meta-line">' + escapeHtml(planningReminderSummary(item)) + '</div>')
      +   linked
      +   (candidateMessage ? '<div class="planning-message">' + escapeHtml(candidateMessage) + '</div>' : '')
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
  const base = editedPlanningCandidates();
  const byId = new Map(base.map(item => [String(item.id), item]));
  document.querySelectorAll('[data-planning-field]').forEach(node => {
    const item = byId.get(String(node.dataset.planningId));
    if (!item) return;
    const value = node.value || '';
    if (node.dataset.planningField === 'reminders') {
      item.reminderInputText = value;
      item.reminderOffsetsMinutes = value.split(/[,，]/).map(token => Number(token.trim())).filter(Number.isFinite).map(value => Math.max(0, Math.floor(value)));
      if (!value.trim()) item.reminderEnabled = false;
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
      item.recurrence.weeklyDays = item.recurrence.type === 'WEEKLY' ? parseWeekdays(value) : [];
    } else {
      item[node.dataset.planningField] = value;
    }
  });
  document.querySelectorAll('[data-planning-flag]').forEach(node => {
    const item = byId.get(String(node.dataset.planningId));
    if (item) item[node.dataset.planningFlag] = node.checked;
  });
  document.querySelectorAll('[data-planning-reminder-enabled]').forEach(node => {
    const item = byId.get(String(node.dataset.planningId));
    if (!item) return;
    item.reminderEnabled = node.checked;
  });
  base.forEach(item => {
    const rawReminder = String(item.reminderInputText || '').trim();
    if (!rawReminder || item.reminderEnabled === false) {
      item.reminderEnabled = false;
      item.reminderOffsetsMinutes = [];
      item.reminderInputText = rawReminder;
    } else {
      item.reminderEnabled = true;
      item.reminderDeliveryMode = 'FULLSCREEN_AND_NOTIFICATION';
    }
  });
  document.querySelectorAll('[data-planning-linked]').forEach(node => {
    const item = byId.get(String(node.dataset.planningLinked));
    if (item) item.createLinkedTodo = node.checked;
  });
  return base;
}

function syncPlanningWeekdayChips(candidateId) {
  const input = document.querySelector('[data-planning-field="recurrenceWeekdays"][data-planning-id="' + cssEscapeValue(candidateId) + '"]');
  const host = document.querySelector('[data-planning-weekday-chips="' + cssEscapeValue(candidateId) + '"]');
  if (!input || !host) return;
  const selected = new Set(parseWeekdays(input.value));
  host.querySelectorAll('[data-planning-weekday-value]').forEach(node => {
    const value = Number(node.dataset.planningWeekdayValue);
    node.classList.toggle('active', selected.has(value));
    node.setAttribute('aria-pressed', selected.has(value) ? 'true' : 'false');
  });
}

function syncPlanningWeekdayVisibility(candidateId) {
  const recurrenceSelect = document.querySelector('[data-planning-field="recurrenceType"][data-planning-id="' + cssEscapeValue(candidateId) + '"]');
  const field = document.querySelector('[data-planning-weekday-field="' + cssEscapeValue(candidateId) + '"]');
  const input = document.querySelector('[data-planning-field="recurrenceWeekdays"][data-planning-id="' + cssEscapeValue(candidateId) + '"]');
  const visible = recurrenceSelect?.value === 'WEEKLY';
  field?.classList.toggle('hidden', !visible);
  if (!visible && input) {
    input.value = '';
    syncPlanningWeekdayChips(candidateId);
  }
}

function cssEscapeValue(value) {
  if (window.CSS?.escape) return window.CSS.escape(String(value));
  return String(value).replace(/"/g, '\\"');
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
  const form = await showFormDialog({
    title: '新建规划文档',
    message: '文档会保存到手机端 Planning Desk，并在下次打开时保留。',
    confirmLabel: '新建',
    fields: [
      { id: 'title', label: '文档名称', type: 'text', value: '新的规划', required: true }
    ]
  });
  const title = form?.title?.trim();
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
  await flushPlanningAutosave();
  const markdown = currentPlanningMarkdownForAction();
  state.planningParsing = true;
  renderPlanningPreview();
  try {
    const active = activePlanningNote();
    state.planningPreviewEdits = {};
    state.planningParseResult = await api('/api/planning/parse', {
      method: 'POST',
      body: JSON.stringify({ markdown, noteId: active && active.id })
    });
    await loadPlanningMappings();
    state.planningParseMarkdown = markdown;
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
  const markdown = state.planningParseMarkdown || currentPlanningMarkdownForAction();
  const result = await api('/api/planning/import', {
    method: 'POST',
    body: JSON.stringify({ markdown, selectedIds, candidates: collectPlanningCandidates(), noteId: active && active.id })
  });
  if (result.updatedMarkdown != null) {
    els.planningEditor.value = result.updatedMarkdown;
    state.planningDirty = true;
    state.planningParseMarkdown = '';
    state.planningPreviewEdits = {};
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
  const form = await showFormDialog({
    title: '刷新已导入规划',
    message: '只会刷新未完成且仍可匹配原文的已导入事项。',
    confirmLabel: '刷新',
    fields: [
      {
        id: 'scope',
        label: '刷新范围',
        type: 'select',
        value: 'CURRENT_SECTION',
        options: [
          { value: 'CURRENT_SECTION', label: '当前标题分区' },
          { value: 'WHOLE_DOCUMENT', label: '整篇文档' }
        ],
        hint: '建议先刷新当前分区；整篇文档适合大范围改期或整理后统一同步。'
      }
    ]
  });
  if (!form) return;
  const result = await api('/api/planning/refresh', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      markdown,
      scope: form.scope === 'WHOLE_DOCUMENT' ? 'WHOLE_DOCUMENT' : 'CURRENT_SECTION',
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
  const form = await showFormDialog({
    title: '顺延规划事项',
    message: '把已导入且未完成的待办 / 日程按分钟整体前后移动，并同步改写规划原文。',
    confirmLabel: '顺延',
    fields: [
      {
        id: 'mappingId',
        label: '从哪一条开始',
        type: 'select',
        value: String(mappings[0].id),
        options: mappings.map((item, index) => ({
          value: item.id,
          label: '[' + (index + 1) + '] 第' + (item.lastKnownLineNumber || '-') + '行 ' + truncatePlainText(item.currentLineText || item.originalLineText || '', 72)
        }))
      },
      {
        id: 'offsetMinutes',
        label: '顺延分钟数',
        type: 'number',
        value: '30',
        required: true,
        hint: '正数表示往后推，例如 30；负数表示提前，例如 -15。'
      },
      {
        id: 'scope',
        label: '影响范围',
        type: 'select',
        value: 'FROM_ITEM_TO_SECTION_END',
        options: [
          { value: 'FROM_ITEM_TO_SECTION_END', label: '从此条目到当前分区末尾' },
          { value: 'FROM_ITEM_TO_DOCUMENT_END', label: '从此条目到文档末尾' },
          { value: 'CURRENT_SECTION_ALL', label: '当前分区所有未完成项' }
        ]
      }
    ]
  });
  if (!form || !form.offsetMinutes) return;
  const mapping = mappings.find(item => sameId(item.id, form.mappingId));
  if (!mapping) return;
  const result = await api('/api/planning/postpone', {
    method: 'POST',
    body: JSON.stringify({
      noteId: active.id,
      markdown,
      mappingId: mapping.id,
      offsetMinutes: form.offsetMinutes,
      scope: form.scope || 'FROM_ITEM_TO_SECTION_END'
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

function truncatePlainText(text, maxLength = 80) {
  const normalized = String(text || '').replace(/\s+/g, ' ').trim();
  if (normalized.length <= maxLength) return normalized;
  return normalized.slice(0, Math.max(0, maxLength - 1)) + '…';
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
  const node = document.getElementById(id);
  node?.classList.toggle('input-invalid', invalid);
  node?.classList.toggle('invalid', invalid);
}

function reminderOffsetLabel(minutes) {
  const preset = REMINDER_OFFSET_PRESETS.find(option => option.minutes === Number(minutes));
  if (preset) return preset.label;
  if (minutes % 1440 === 0) return '提前 ' + (minutes / 1440) + ' 天';
  if (minutes % 60 === 0) return '提前 ' + (minutes / 60) + ' 小时';
  return '提前 ' + minutes + ' 分钟';
}

function bindReminderOffsetPicker(prefix) {
  const host = document.getElementById(prefix + '-reminder-picker');
  if (!host) return;
  host.innerHTML = ''
    + '<details>'
    +   '<summary class="reminder-offset-summary"><span data-reminder-summary></span></summary>'
    +   '<div class="reminder-offset-menu">'
    +     REMINDER_OFFSET_PRESETS.map(option => (
            '<label class="reminder-offset-option">'
            + '<input type="checkbox" data-reminder-offset="' + option.minutes + '" />'
            + '<span>' + escapeHtml(option.label) + '</span>'
            + '</label>'
          )).join('')
    +     '<button type="button" class="ghost reminder-custom-trigger">自定义提醒时间</button>'
    +     '<div class="reminder-custom-shell hidden">'
    +       '<input type="text" data-reminder-custom placeholder="例：20，或 16:30，或 2026-09-01 08:00" />'
    +       '<p class="field-hint">可输入提前分钟或具体时间；多个值使用逗号分隔。</p>'
    +     '</div>'
    +   '</div>'
    + '</details>';
  host.querySelectorAll('[data-reminder-offset]').forEach(input => {
    input.addEventListener('change', () => syncReminderOffsetPicker(prefix));
  });
  const customShell = host.querySelector('.reminder-custom-shell');
  const customInput = host.querySelector('[data-reminder-custom]');
  host.querySelector('.reminder-custom-trigger')?.addEventListener('click', () => {
    customShell?.classList.remove('hidden');
    customInput?.focus();
  });
  customInput?.addEventListener('input', () => syncReminderOffsetPicker(prefix));
  syncReminderOffsetPicker(prefix);
}

function reminderPickerSpec(prefix) {
  const host = document.getElementById(prefix + '-reminder-picker');
  if (!host) return '';
  const presetTokens = Array.from(host.querySelectorAll('[data-reminder-offset]:checked'))
    .map(input => input.dataset.reminderOffset)
    .filter(Boolean);
  const custom = String(host.querySelector('[data-reminder-custom]')?.value || '').trim();
  return presetTokens.concat(custom ? [custom] : []).join(',');
}

function setReminderPickerOffsets(prefix, offsets) {
  const host = document.getElementById(prefix + '-reminder-picker');
  if (!host) return;
  const normalized = Array.from(new Set((offsets || []).map(Number).filter(value => Number.isFinite(value) && value >= 0)));
  const presetValues = new Set(REMINDER_OFFSET_PRESETS.map(option => option.minutes));
  host.querySelectorAll('[data-reminder-offset]').forEach(input => {
    input.checked = normalized.includes(Number(input.dataset.reminderOffset));
  });
  const customValues = normalized.filter(value => !presetValues.has(value));
  const customInput = host.querySelector('[data-reminder-custom]');
  const customShell = host.querySelector('.reminder-custom-shell');
  if (customInput) customInput.value = customValues.join(',');
  customShell?.classList.toggle('hidden', customValues.length === 0);
  syncReminderOffsetPicker(prefix);
}

function syncReminderOffsetPicker(prefix) {
  const host = document.getElementById(prefix + '-reminder-picker');
  if (!host) return;
  const labels = Array.from(host.querySelectorAll('[data-reminder-offset]:checked'))
    .map(input => reminderOffsetLabel(Number(input.dataset.reminderOffset)));
  const custom = String(host.querySelector('[data-reminder-custom]')?.value || '').trim();
  if (custom) labels.push('自定义：' + custom);
  const summary = host.querySelector('[data-reminder-summary]');
  if (summary) summary.textContent = labels.join('、') || host.dataset.emptySummary || '未选择提醒时间';
}

function setReminderPickerEnabled(prefix, enabled) {
  const host = document.getElementById(prefix + '-reminder-picker');
  if (!host) return;
  host.classList.toggle('disabled', !enabled);
  host.querySelectorAll('input, button').forEach(control => { control.disabled = !enabled; });
  if (!enabled) host.querySelector('details')?.removeAttribute('open');
}

function setEventCheckInReminderEnabled() {
  const enabled = document.getElementById('event-check-in')?.checked === true
    && document.getElementById('event-reminder-enabled')?.checked === true;
  document.getElementById('event-checkin-reminder-block')?.classList.toggle('disabled-block', !enabled);
  setReminderPickerEnabled('event-checkin', enabled);
}

function parseWeekdays(text) {
  return Array.from(new Set(String(text || '')
    .split(',')
    .map(v => Number(v.trim()))
    .filter(v => Number.isInteger(v) && v >= 1 && v <= 7)))
    .sort((a, b) => a - b);
}

function setWeekdays(prefix, weekdays) {
  const input = document.getElementById(prefix + '-weekdays');
  if (!input) return;
  input.value = parseWeekdays(Array.isArray(weekdays) ? weekdays.join(',') : weekdays).join(',');
  syncWeekdayChips(prefix);
}

function syncWeekdayChips(prefix) {
  const input = document.getElementById(prefix + '-weekdays');
  const host = document.getElementById(prefix + '-weekday-chips');
  if (!input || !host) return;
  const selected = new Set(parseWeekdays(input.value));
  host.querySelectorAll('[data-weekday-value]').forEach(node => {
    const value = Number(node.dataset.weekdayValue);
    node.classList.toggle('active', selected.has(value));
    node.setAttribute('aria-pressed', selected.has(value) ? 'true' : 'false');
  });
}

function bindWeekdayChips(prefix) {
  const host = document.getElementById(prefix + '-weekday-chips');
  const input = document.getElementById(prefix + '-weekdays');
  if (!host || !input) return;
  host.innerHTML = WEEKDAY_OPTIONS.map(day => (
    '<button type="button" class="weekday-chip" data-weekday-value="' + day.value + '" aria-pressed="false">周' + escapeHtml(day.label) + '</button>'
  )).join('');
  host.addEventListener('click', event => {
    const chip = event.target.closest?.('[data-weekday-value]');
    if (!chip || chip.disabled) return;
    const value = Number(chip.dataset.weekdayValue);
    const selected = new Set(parseWeekdays(input.value));
    if (selected.has(value)) selected.delete(value); else selected.add(value);
    input.value = Array.from(selected).sort((a, b) => a - b).join(',');
    syncWeekdayChips(prefix);
  });
  syncWeekdayChips(prefix);
}

function setWeekdayChipsEnabled(prefix, enabled) {
  document.querySelectorAll('#' + prefix + '-weekday-chips [data-weekday-value]').forEach(node => {
    node.disabled = !enabled;
  });
}

function syncRecurrenceWeekdayVisibility(prefix) {
  const type = document.getElementById(prefix + '-recurrence-type')?.value || 'NONE';
  const field = document.getElementById(prefix + '-weekday-field');
  const visible = type === 'WEEKLY';
  field?.classList.toggle('hidden', !visible);
  setWeekdayChipsEnabled(prefix, visible && (prefix !== 'todo' || document.getElementById('todo-has-due')?.checked !== false));
  if (!visible) setWeekdays(prefix, []);
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

async function recurrenceScopeQueryForItem(item, action = '操作') {
  if (!item?.isRecurring) return '';
  const isEvent = item.itemType === 'EVENT';
  const noun = isEvent ? '日程' : '任务';
  const seriesNoun = isEvent ? '循环日程' : '循环待办';
  const result = await showFormDialog({
    title: action + seriesNoun,
    message: action === '取消' && !isEvent
      ? '请选择要取消哪一部分循环待办。取消会停止对应提醒并进入历史记录，不会直接删除。'
      : '请选择这次操作影响哪一部分' + seriesNoun + '。',
    confirmLabel: '继续',
    fields: [
      {
        id: 'scope',
        label: '作用范围',
        type: 'select',
        value: 'CURRENT',
        options: [
          { value: 'CURRENT', label: '仅' + action + '当前' + noun },
          { value: 'CURRENT_AND_FUTURE', label: action + '当前及之后' + noun },
          { value: 'ALL', label: action + '全部' + seriesNoun }
        ]
      }
    ]
  });
  const scope = result?.scope;
  return scope ? '?scope=' + encodeURIComponent(scope) : null;
}

function setTodoReminderEnabled(enabled) {
  const checkbox = document.getElementById('todo-reminder-enabled');
  if (checkbox) checkbox.checked = enabled;
  if (enabled && !reminderPickerSpec('todo')) {
    setReminderPickerOffsets('todo', [5]);
  }
  document.querySelectorAll('.todo-reminder-dependent').forEach(node => {
    node.classList.toggle('disabled-block', !enabled);
    node.querySelectorAll('input, select, textarea, button').forEach(input => {
      input.disabled = !enabled;
    });
  });
  if (!enabled) {
    setReminderPickerOffsets('todo', []);
    document.getElementById('todo-alarm-mode').checked = false;
  }
  setReminderPickerEnabled('todo', enabled);
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
  setWeekdayChipsEnabled('todo', enabled);
  if (!enabled) {
    writeDateTimeValue('todo-due', '');
    setReminderPickerOffsets('todo', []);
    document.getElementById('todo-recurrence-type').value = 'NONE';
    document.getElementById('todo-recurrence-end').value = '';
    setWeekdays('todo', []);
    syncRecurrenceWeekdayVisibility('todo');
    const countdown = document.getElementById('todo-countdown');
    const hiddenBoard = document.getElementById('todo-hidden-board');
    const alarmMode = document.getElementById('todo-alarm-mode');
    if (countdown) countdown.checked = false;
    if (hiddenBoard) hiddenBoard.checked = false;
    if (alarmMode) alarmMode.checked = false;
    setTodoReminderEnabled(false);
  } else {
    setTodoReminderEnabled(document.getElementById('todo-reminder-enabled')?.checked === true);
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
  setReminderPickerOffsets('todo', [5]);
  setTodoReminderEnabled(true);
  document.getElementById('todo-recurrence-type').value = 'NONE';
  document.getElementById('todo-recurrence-end').value = '';
  setWeekdays('todo', []);
  syncRecurrenceWeekdayVisibility('todo');
  setRecurrenceScopeBlock('todo', false);
  document.getElementById('todo-countdown').checked = false;
  document.getElementById('todo-hidden-board').checked = false;
  document.getElementById('todo-alarm-mode').checked = false;
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
  setReminderPickerOffsets('todo', item.hasDueDate ? (item.reminderOffsetsMinutes || []) : []);
  setTodoReminderEnabled(item.hasDueDate !== false && item.reminderEnabled === true);
  document.getElementById('todo-recurrence-type').value = recurrenceTypeValue(item);
  document.getElementById('todo-recurrence-end').value = item.recurrenceEndDate || '';
  setWeekdays('todo', item.recurrenceWeekdays);
  syncRecurrenceWeekdayVisibility('todo');
  setRecurrenceScopeBlock('todo', item.isRecurring === true, 'CURRENT_AND_FUTURE');
  document.getElementById('todo-countdown').checked = item.countdownEnabled === true;
  document.getElementById('todo-hidden-board').checked = item.hiddenFromBoard === true;
  document.getElementById('todo-alarm-mode').checked = item.alarmMode === true;
  document.getElementById('todo-ring').checked = item.ringEnabled !== false;
  document.getElementById('todo-vibrate').checked = item.vibrateEnabled !== false;
  openModal('todo-modal');
}

function clearEventForm() {
  state.editingEventId = null;
  state.editingEventOriginalRecurring = false;
  state.editingEventMultiSlotBundleId = null;
  state.pendingEventSeed = null;
  state.eventEditorPreviewDayOffset = 0;
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
  setReminderPickerOffsets('event', []);
  setReminderPickerOffsets('event-checkin', []);
  setEventReminderEnabled(false);
  document.getElementById('event-recurrence-type').value = 'NONE';
  document.getElementById('event-recurrence-end').value = '';
  setWeekdays('event', []);
  syncRecurrenceWeekdayVisibility('event');
  setRecurrenceScopeBlock('event', false);
  document.getElementById('event-countdown').checked = false;
  document.getElementById('event-check-in').checked = false;
  setEventCheckInReminderEnabled();
  document.getElementById('event-all-day').checked = false;
  document.getElementById('event-ring').checked = true;
  document.getElementById('event-vibrate').checked = true;
  document.getElementById('event-multi-slot-sync').checked = false;
  document.getElementById('event-multi-slot-sync-block').classList.add('hidden');
  state.eventCourseSlots = [defaultEventCourseSlot()];
  setEventCourseModeEnabled(false, true);
  renderEventEditorPreview();
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

function setEventReminderEnabled(enabled, ensureOrdinaryDefault = true) {
  const checkbox = document.getElementById('event-reminder-enabled');
  if (checkbox) checkbox.checked = enabled;
  if (enabled && ensureOrdinaryDefault && !reminderPickerSpec('event')) {
    setReminderPickerOffsets('event', [15]);
  }
  document.querySelectorAll('.event-reminder-dependent').forEach(node => {
    node.classList.toggle('disabled-block', !enabled);
    node.querySelectorAll('input, select, textarea, button').forEach(input => {
      input.disabled = !enabled;
    });
  });
  if (!enabled) {
    setReminderPickerOffsets('event', []);
    setReminderPickerOffsets('event-checkin', []);
  }
  setReminderPickerEnabled('event', enabled);
  setEventCheckInReminderEnabled();
}

function setEventCourseModeEnabled(enabled, allowVisible = true) {
  const block = document.getElementById('event-course-block');
  const checkbox = document.getElementById('event-course-enabled');
  const body = document.getElementById('event-course-body');
  if (block) block.classList.toggle('hidden', !allowVisible);
  if (checkbox) {
    checkbox.checked = enabled && allowVisible;
    checkbox.disabled = !allowVisible;
  }
  if (body) body.classList.toggle('hidden', !(enabled && allowVisible));
  if (enabled && allowVisible) {
    ensureEventCourseSlots();
    renderEventCourseSlots();
    document.getElementById('event-all-day').checked = false;
    document.getElementById('event-recurrence-type').value = 'WEEKLY';
    const baseStart = readDateTimeValue('event-start');
    const baseDate = baseStart ? new Date(baseStart) : new Date();
    if (!document.getElementById('event-recurrence-end').value) {
      document.getElementById('event-recurrence-end').value = defaultCourseRecurrenceEndDate(baseDate);
    }
  }
  document.getElementById('event-recurrence-type').disabled = enabled && allowVisible;
  document.getElementById('event-all-day').disabled = enabled && allowVisible;
  syncRecurrenceWeekdayVisibility('event');
  if (enabled && allowVisible) {
    document.getElementById('event-weekday-field')?.classList.add('hidden');
    setWeekdayChipsEnabled('event', false);
  }
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
  renderEventEditorPreview();
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
  input.addEventListener('input', () => {
    syncEventColorPresets();
    renderEventEditorPreview();
  });
  syncEventColorPresets();
}

bindWeekdayChips('todo');
bindWeekdayChips('event');

function openEventEditor(item) {
  state.editingEventId = item.id;
  state.editingEventOriginalRecurring = item.isRecurring === true;
  state.editingEventMultiSlotBundleId = item.multiSlotBundleId || null;
  state.pendingEventSeed = null;
  state.eventEditorPreviewDayOffset = 0;
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
  setReminderPickerOffsets('event', item.reminderOffsetsMinutes || []);
  setReminderPickerOffsets('event-checkin', item.checkInReminderOffsetsMinutes || []);
  setEventReminderEnabled(item.reminderEnabled === true, false);
  document.getElementById('event-recurrence-type').value = recurrenceTypeValue(item);
  document.getElementById('event-recurrence-end').value = item.recurrenceEndDate || '';
  setWeekdays('event', item.recurrenceWeekdays);
  syncRecurrenceWeekdayVisibility('event');
  setRecurrenceScopeBlock('event', item.isRecurring === true, 'CURRENT_AND_FUTURE');
  document.getElementById('event-multi-slot-sync').checked = false;
  document.getElementById('event-multi-slot-sync-block').classList.toggle('hidden', !state.editingEventMultiSlotBundleId);
  document.getElementById('event-countdown').checked = item.countdownEnabled === true;
  document.getElementById('event-check-in').checked = item.checkInEnabled === true;
  setEventCheckInReminderEnabled();
  document.getElementById('event-all-day').checked = item.allDay === true;
  document.getElementById('event-ring').checked = item.ringEnabled !== false;
  document.getElementById('event-vibrate').checked = item.vibrateEnabled !== false;
  state.eventCourseSlots = [];
  setEventCourseModeEnabled(false, false);
  openModal('event-modal');
}

function openTodoPreview(item) {
  state.previewTodoId = item.id;
  const accent = todoGroupAccent(item);
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
    + previewRow('分', '分组', todoGroupLabel(item))
    + previewRow('时', 'DDL', todoDueText(item))
    + previewRow('倒', '倒数日', item.countdownEnabled ? '已显示在每日看板倒数日' : '未开启')
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
  const accent = item.accentColorHex || item.groupColorHex || DEFAULT_EVENT_COLOR;
  const body = document.getElementById('event-preview-body');
  const cancelButton = document.getElementById('preview-event-cancel');
  const deleteButton = document.getElementById('preview-event-delete');
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
  const inactive = item.completed || item.canceled;
  cancelButton?.classList.toggle('hidden', inactive);
  deleteButton?.classList.toggle('hidden', inactive);
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
  if (id === 'event-modal') renderEventEditorPreview();
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

function showFormDialog({ title, message = '', confirmLabel = '确定', fields = [] }) {
  return new Promise(resolve => {
    const modal = document.getElementById('form-modal');
    const titleNode = document.getElementById('form-title');
    const messageNode = document.getElementById('form-message');
    const fieldsHost = document.getElementById('form-fields');
    const cancelButton = document.getElementById('form-cancel');
    const okButton = document.getElementById('form-ok');
    if (!modal || !titleNode || !messageNode || !fieldsHost || !cancelButton || !okButton) {
      resolve(null);
      return;
    }

    titleNode.textContent = title || '输入';
    messageNode.textContent = message || '';
    okButton.textContent = confirmLabel || '确定';
    fieldsHost.innerHTML = '';
    const inputs = new Map();

    fields.forEach(field => {
      const wrapper = document.createElement('div');
      wrapper.className = 'form-dialog-field';

      const label = document.createElement('label');
      label.className = 'form-dialog-label';
      label.textContent = field.label || field.id;
      wrapper.appendChild(label);

      let input;
      if (field.type === 'textarea') {
        input = document.createElement('textarea');
      } else if (field.type === 'select') {
        input = document.createElement('select');
        (field.options || []).forEach(optionData => {
          const option = document.createElement('option');
          option.value = String(optionData.value ?? '');
          option.textContent = optionData.label || String(optionData.value ?? '');
          input.appendChild(option);
        });
      } else {
        input = document.createElement('input');
        input.type = field.type === 'number' ? 'number' : 'text';
        if (field.type === 'number') input.step = field.step || '1';
      }

      input.value = field.value == null ? '' : String(field.value);
      input.placeholder = field.placeholder || '';
      input.dataset.formFieldId = field.id;
      if (field.required) input.dataset.required = 'true';
      input.addEventListener('keydown', event => {
        if (event.key !== 'Enter') return;
        if (field.type === 'textarea' && !(event.ctrlKey || event.metaKey)) return;
        event.preventDefault();
        okButton.click();
      });
      wrapper.appendChild(input);

      if (field.hint) {
        const hint = document.createElement('p');
        hint.className = 'form-dialog-hint';
        hint.textContent = field.hint;
        wrapper.appendChild(hint);
      }

      fieldsHost.appendChild(wrapper);
      inputs.set(field.id, input);
    });

    const cleanup = result => {
      modal.classList.add('hidden');
      cancelButton.onclick = null;
      okButton.onclick = null;
      modal.onclick = null;
      fieldsHost.innerHTML = '';
      resolve(result);
    };

    cancelButton.onclick = () => cleanup(null);
    okButton.onclick = () => {
      const result = {};
      for (const field of fields) {
        const input = inputs.get(field.id);
        if (!input) continue;
        input.classList.remove('input-invalid');
        const raw = input.value || '';
        if (field.required && !raw.trim()) {
          input.classList.add('input-invalid');
          input.focus();
          return;
        }
        if (field.type === 'number') {
          const value = Number(raw);
          if (!Number.isFinite(value)) {
            input.classList.add('input-invalid');
            input.focus();
            return;
          }
          result[field.id] = value;
        } else {
          result[field.id] = raw;
        }
      }
      cleanup(result);
    };
    modal.onclick = event => {
      if (event.target === modal) cleanup(null);
    };
    modal.classList.remove('hidden');
    const firstInput = fieldsHost.querySelector('input, textarea, select');
    (firstInput || okButton).focus();
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
      if (Date.now() < state.eventDragSuppressClickUntil) return;
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
document.getElementById('todo-reminder-enabled')?.addEventListener('change', event => setTodoReminderEnabled(event.target.checked));
document.getElementById('event-reminder-enabled')?.addEventListener('change', event => setEventReminderEnabled(event.target.checked));
document.getElementById('event-check-in')?.addEventListener('change', setEventCheckInReminderEnabled);
['event-title', 'event-location', 'event-notes', 'event-group'].forEach(id => {
  document.getElementById(id)?.addEventListener('input', renderEventEditorPreview);
  document.getElementById(id)?.addEventListener('change', renderEventEditorPreview);
});
document.getElementById('event-preview-today')?.addEventListener('click', () => {
  state.eventEditorPreviewDayOffset = 0;
  renderEventEditorPreview();
});
document.getElementById('event-preview-prev')?.addEventListener('click', () => {
  state.eventEditorPreviewDayOffset = Number(state.eventEditorPreviewDayOffset || 0) - 1;
  renderEventEditorPreview();
});
document.getElementById('event-preview-next')?.addEventListener('click', () => {
  state.eventEditorPreviewDayOffset = Number(state.eventEditorPreviewDayOffset || 0) + 1;
  renderEventEditorPreview();
});
document.getElementById('event-course-enabled')?.addEventListener('change', event => {
  if (event.target.checked) {
    state.eventCourseSlots = [defaultEventCourseSlot()];
  }
  setEventCourseModeEnabled(event.target.checked, !state.editingEventId);
});
document.getElementById('event-course-add')?.addEventListener('click', () => {
  ensureEventCourseSlots();
  const last = state.eventCourseSlots[state.eventCourseSlots.length - 1] || defaultEventCourseSlot();
  const nextWeekday = Number(last.weekday) >= 7 ? 1 : Number(last.weekday) + 1;
  state.eventCourseSlots = state.eventCourseSlots.concat([{ ...last, weekday: nextWeekday }]);
  renderEventCourseSlots();
});
document.getElementById('event-course-slots')?.addEventListener('input', event => {
  const row = event.target.closest?.('[data-course-slot-index]');
  const field = event.target.dataset?.courseSlotField;
  if (!row || !field) return;
  const index = Number(row.dataset.courseSlotIndex);
  if (!Number.isInteger(index) || !state.eventCourseSlots[index]) return;
  state.eventCourseSlots[index] = {
    ...state.eventCourseSlots[index],
    [field]: field === 'weekday' ? Number(event.target.value) : event.target.value
  };
});
document.getElementById('event-course-slots')?.addEventListener('change', event => {
  const row = event.target.closest?.('[data-course-slot-index]');
  const field = event.target.dataset?.courseSlotField;
  if (row && field) {
    const index = Number(row.dataset.courseSlotIndex);
    if (Number.isInteger(index) && state.eventCourseSlots[index]) {
      state.eventCourseSlots[index] = {
        ...state.eventCourseSlots[index],
        [field]: field === 'weekday' ? Number(event.target.value) : event.target.value
      };
    }
  }
});
document.getElementById('event-course-slots')?.addEventListener('click', event => {
  const remove = event.target.closest?.('[data-course-slot-remove]');
  if (!remove) return;
  event.preventDefault();
  const index = Number(remove.dataset.courseSlotRemove);
  if (!Number.isInteger(index) || state.eventCourseSlots.length <= 1) return;
  state.eventCourseSlots = state.eventCourseSlots.filter((_, currentIndex) => currentIndex !== index);
  renderEventCourseSlots();
});
document.getElementById('todo-recurrence-type')?.addEventListener('change', event => {
  syncRecurrenceWeekdayVisibility('todo');
  if (state.editingTodoOriginalRecurring && event.target.value === 'NONE') {
    const scopeSelect = document.getElementById('todo-recurrence-scope');
    if (scopeSelect?.value === 'CURRENT') scopeSelect.value = 'CURRENT_AND_FUTURE';
  }
});
document.getElementById('event-recurrence-type')?.addEventListener('change', event => {
  if (document.getElementById('event-course-enabled')?.checked) {
    event.target.value = 'WEEKLY';
    syncRecurrenceWeekdayVisibility('event');
    setWeekdayChipsEnabled('event', false);
    return;
  }
  syncRecurrenceWeekdayVisibility('event');
  if (state.editingEventOriginalRecurring && event.target.value === 'NONE') {
    const scopeSelect = document.getElementById('event-recurrence-scope');
    if (scopeSelect?.value === 'CURRENT') scopeSelect.value = 'CURRENT_AND_FUTURE';
  }
});
document.getElementById('connect').onclick = () => connect().catch(err => els.status.textContent = err.message);
document.getElementById('disconnect')?.addEventListener('click', () => disconnect());
document.getElementById('sidebar-toggle')?.addEventListener('click', () => {
  const shell = document.querySelector('.shell');
  const btn = document.getElementById('sidebar-toggle');
  if (shell.classList.contains('sidebar-collapsed')) {
    shell.classList.remove('sidebar-collapsed');
    btn.textContent = '‹';
    btn.title = '收起侧边栏';
  } else {
    shell.classList.add('sidebar-collapsed');
    btn.textContent = '›';
    btn.title = '展开侧边栏';
  }
});
els.token.addEventListener('keydown', event => {
  if (event.key !== 'Enter') return;
  event.preventDefault();
  connect().catch(err => els.status.textContent = err.message);
});
els.token.addEventListener('input', () => {
  const normalized = els.token.value.replace(/\s+/g, '').toUpperCase().slice(0, 4);
  if (els.token.value !== normalized) els.token.value = normalized;
});
document.getElementById('refresh').onclick = () => refreshCurrentData().catch(err => els.status.textContent = err.message);
window.addEventListener('resize', () => {
  if (state.currentTab === 'events' && state.snapshot) renderEvents();
});
document.getElementById('jump-today').onclick = () => {
  if (state.currentTab === 'events') {
    state.selectedEventDay = dayKey(new Date());
    state.eventMiniMonth = state.selectedEventDay.slice(0, 7) + '-01';
    state.eventsLoaded = false;
    renderEvents();
  } else {
    els.desktopDailyBoard?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};
els.eventPrevDay.onclick = () => {
  if (state.eventViewMode === 'month') {
    const date = dateFromKey(state.selectedEventDay);
    date.setMonth(date.getMonth() - 1);
    state.selectedEventDay = dayKey(date);
  } else {
    state.selectedEventDay = addDays(state.selectedEventDay, state.eventViewMode === 'week' ? -7 : -1);
  }
  state.eventMiniMonth = state.selectedEventDay.slice(0, 7) + '-01';
  state.eventsLoaded = false;
  renderEvents();
};
els.eventNextDay.onclick = () => {
  if (state.eventViewMode === 'month') {
    const date = dateFromKey(state.selectedEventDay);
    date.setMonth(date.getMonth() + 1);
    state.selectedEventDay = dayKey(date);
  } else {
    state.selectedEventDay = addDays(state.selectedEventDay, state.eventViewMode === 'week' ? 7 : 1);
  }
  state.eventMiniMonth = state.selectedEventDay.slice(0, 7) + '-01';
  state.eventsLoaded = false;
  renderEvents();
};
els.applyEventDay.onclick = () => {
  if (!els.eventAnchorDate.value) return;
  state.selectedEventDay = els.eventAnchorDate.value;
  state.eventMiniMonth = state.selectedEventDay.slice(0, 7) + '-01';
  state.eventsLoaded = false;
  renderEvents();
};
els.eventAnchorDate?.addEventListener('change', () => els.applyEventDay?.click());
els.eventCalendarToday?.addEventListener('click', () => {
  state.selectedEventDay = dayKey(new Date());
  state.eventMiniMonth = state.selectedEventDay.slice(0, 7) + '-01';
  state.eventsLoaded = false;
  renderEvents();
});
els.eventMiniPrev?.addEventListener('click', () => {
  const date = dateFromKey(state.eventMiniMonth);
  date.setMonth(date.getMonth() - 1);
  state.eventMiniMonth = dayKey(date).slice(0, 7) + '-01';
  renderMiniCalendar();
});
els.eventMiniNext?.addEventListener('click', () => {
  const date = dateFromKey(state.eventMiniMonth);
  date.setMonth(date.getMonth() + 1);
  state.eventMiniMonth = dayKey(date).slice(0, 7) + '-01';
  renderMiniCalendar();
});
els.eventMiniCalendar?.addEventListener('click', event => {
  const day = event.target.closest?.('[data-mini-calendar-day]');
  if (!day) return;
  state.selectedEventDay = day.dataset.miniCalendarDay;
  state.eventMiniMonth = state.selectedEventDay.slice(0, 7) + '-01';
  state.eventsLoaded = false;
  renderEvents();
});
els.eventCalendarSearch?.addEventListener('input', event => {
  state.eventQuery = event.target.value || '';
  renderEvents();
});
els.eventCalendarGroups?.addEventListener('change', event => {
  const checkbox = event.target.closest?.('[data-calendar-group-id]');
  if (!checkbox) return;
  const hidden = new Set((state.hiddenEventGroupIds || []).map(String));
  if (checkbox.checked) hidden.delete(checkbox.dataset.calendarGroupId); else hidden.add(checkbox.dataset.calendarGroupId);
  state.hiddenEventGroupIds = Array.from(hidden);
  renderEvents();
});
document.querySelectorAll('[data-event-view]').forEach(button => {
  button.addEventListener('click', () => {
    state.eventViewMode = button.dataset.eventView || 'week';
    state.eventMiniMonth = state.selectedEventDay.slice(0, 7) + '-01';
    state.eventsLoaded = false;
    renderEvents();
  });
});
els.calendarWeekStart?.addEventListener('change', async event => {
  const nextMode = event.target.value === 'SUNDAY' ? 'SUNDAY' : 'MONDAY';
  if (nextMode === state.weekStartMode) return;
  try {
    await api('/api/calendar-preferences', {
      method: 'PUT',
      body: JSON.stringify({ weekStartMode: nextMode })
    });
    state.weekStartMode = nextMode;
    state.eventsLoaded = false;
    state.eventMiniMonth = state.selectedEventDay.slice(0, 7) + '-01';
    renderEvents();
  } catch (error) {
    event.target.value = state.weekStartMode;
    els.status.textContent = error.message || '保存周起始日失败';
  }
});
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
els.planningOutline?.addEventListener('click', async event => {
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
    const form = await showFormDialog({
      title: '新增子项',
      message: '子项会作为当前规划事项的下级草稿，发布前不会进入正式待办或日程。',
      confirmLabel: '添加',
      fields: [
        { id: 'text', label: '子项内容', type: 'textarea', placeholder: '例如：整理第一版材料', required: true }
      ]
    });
    const text = form?.text?.trim();
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
  const weekdayChip = event.target.closest?.('[data-planning-weekday-value]');
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
  if (weekdayChip) {
    const host = weekdayChip.closest?.('[data-planning-weekday-chips]');
    const candidateId = host?.dataset?.planningWeekdayChips;
    const input = candidateId ? document.querySelector('[data-planning-field="recurrenceWeekdays"][data-planning-id="' + cssEscapeValue(candidateId) + '"]') : null;
    if (input) {
      const value = Number(weekdayChip.dataset.planningWeekdayValue);
      const selected = new Set(parseWeekdays(input.value));
      if (selected.has(value)) selected.delete(value); else selected.add(value);
      input.value = Array.from(selected).sort((a, b) => a - b).join(',');
      syncPlanningWeekdayChips(candidateId);
    }
  }
});
els.planningPreview?.addEventListener('change', event => {
  if (event.target.matches?.('[data-planning-field="recurrenceType"]')) {
    syncPlanningWeekdayVisibility(event.target.dataset.planningId);
  }
  if (event.target.matches?.('[data-planning-field]')) {
    const field = event.target.dataset.planningField;
    const id = event.target.dataset.planningId;
    if (id && field) {
      rememberPlanningPreviewEdit(id, { [field]: event.target.value || '' });
    }
  }
  if (event.target.matches?.('[data-planning-flag]')) {
    const flag = event.target.dataset.planningFlag;
    const id = event.target.dataset.planningId;
    if (id && flag) {
      rememberPlanningPreviewEdit(id, { [flag]: event.target.checked });
      if (flag === 'reminderEnabled') renderPlanningPreview();
    }
  }
  if (event.target.matches?.('[data-planning-linked]')) {
    rememberPlanningPreviewEdit(event.target.dataset.planningLinked, { createLinkedTodo: event.target.checked });
  }
});
els.planningPreview?.addEventListener('input', event => {
  if (event.target.matches?.('[data-planning-field]')) {
    const field = event.target.dataset.planningField;
    const id = event.target.dataset.planningId;
    if (id && field) {
      rememberPlanningPreviewEdit(id, { [field]: event.target.value || '' });
    }
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
    els.planningEditor.value = note.contentMarkdown || state.planningNodesMarkdown || '';
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
  const creatingEvent = state.currentTab === 'events';
  if (state.currentTab === 'events') {
    clearEventForm();
    const start = snapToQuarterHour(Date.now() + THIRTY_MINUTES);
    setEventSeed(start, start + THIRTY_MINUTES);
  } else {
    clearTodoForm();
  }
  openModal(creatingEvent ? 'event-modal' : 'todo-modal');
};
if (els.eventTimeline) {
  els.eventTimeline.addEventListener('click', event => {
    const eventNode = event.target.closest?.('[data-event-id]');
    if (!eventNode) return;
    event.preventDefault();
    event.stopPropagation();
    if (Date.now() < state.eventDragSuppressClickUntil) return;
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
      state.eventMiniMonth = state.selectedEventDay.slice(0, 7) + '-01';
      ensureEventData().catch(err => els.status.textContent = err.message);
    } else if (state.currentTab === 'planning') {
      loadPlanningNotes()
        .then(() => renderPlanningNotes())
        .catch(err => els.status.textContent = err.message);
    } else if (state.currentTab === 'todos') {
      renderTodos();
      ensureTodoData().catch(err => els.status.textContent = err.message);
      renderAnnouncements();
    } else if (state.currentTab === 'board') {
      renderDesktopDailyBoard();
      renderAnnouncements();
    } else if (state.currentTab === 'reports') {
      loadAiReportWorkspace().catch(err => els.status.textContent = err.message);
    }
  };
});
document.querySelectorAll('[data-close-modal]').forEach(node => {
  node.onclick = () => closeModal(node.dataset.closeModal);
});
function cancelPromiseModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal || modal.classList.contains('hidden')) return false;
  if (modalId === 'form-modal') {
    document.getElementById('form-cancel')?.click();
    return true;
  }
  if (modalId === 'confirm-modal') {
    document.getElementById('confirm-cancel')?.click();
    return true;
  }
  return false;
}
document.querySelectorAll('.modal-backdrop').forEach(node => {
  node.onclick = event => {
    if (event.target !== node) return;
    if (node.id === 'form-modal' || node.id === 'confirm-modal') {
      cancelPromiseModal(node.id);
      return;
    }
    node.classList.add('hidden');
  };
});
document.addEventListener('keydown', event => {
  if (event.key !== 'Escape') return;
  if (cancelPromiseModal('form-modal') || cancelPromiseModal('confirm-modal')) return;
  document.querySelectorAll('.modal-backdrop:not(.hidden)').forEach(node => node.classList.add('hidden'));
});

document.getElementById('create-todo').onclick = async () => {
  try {
    setInputInvalid('todo-reminder-picker', false);
    const hasDueDate = document.getElementById('todo-has-due')?.checked !== false;
    const dueAt = hasDueDate ? readDateTimeValue('todo-due') : null;
    const dueAtMillis = dueAt ? new Date(dueAt).getTime() : null;
    const todoReminderEnabled = hasDueDate && document.getElementById('todo-reminder-enabled')?.checked === true;
    const reminderOffsets = todoReminderEnabled ? parseReminderSpecs(reminderPickerSpec('todo'), dueAtMillis) : [];
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
      hiddenFromBoard: hasDueDate && document.getElementById('todo-hidden-board').checked,
      alarmMode: todoReminderEnabled && document.getElementById('todo-alarm-mode').checked,
      ringEnabled: todoReminderEnabled && document.getElementById('todo-ring').checked,
      vibrateEnabled: todoReminderEnabled && document.getElementById('todo-vibrate').checked,
      reminderDeliveryMode: 'FULLSCREEN_AND_NOTIFICATION',
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
    setInputInvalid('todo-reminder-picker', true);
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
  const todoItem = findTodoById(state.previewTodoId);
  const scopeQuery = await recurrenceScopeQueryForItem(todoItem, '取消');
  if (scopeQuery === null) return;
  if (!todoItem?.isRecurring && !await confirmDanger('确认取消待办', '取消后会停止提醒，并进入历史记录；这不是删除。', '确认取消')) return;
  await api(`/api/items/${state.previewTodoId}/cancel${scopeQuery}`, { method: 'POST' });
  closeModal('todo-preview-modal');
  await refreshAfterMutation();
};

document.getElementById('preview-todo-delete').onclick = async () => {
  if (!state.previewTodoId) return;
  const todoItem = findTodoById(state.previewTodoId);
  const scopeQuery = await recurrenceScopeQueryForItem(todoItem, '删除');
  if (scopeQuery === null) return;
  if (!await confirmDanger('确认删除待办', '删除后会直接移除，不进入历史记录，也无法恢复。')) return;
  await api(`/api/items/${state.previewTodoId}${scopeQuery}`, { method: 'DELETE' });
  closeModal('todo-preview-modal');
  await refreshAfterMutation();
};

document.getElementById('save-event').onclick = async () => {
  try {
    setInputInvalid('event-reminder-picker', false);
    setInputInvalid('event-checkin-reminder-picker', false);
    const startAt = readDateTimeValue('event-start');
    const endAt = readDateTimeValue('event-end');
    const startAtMillis = startAt ? new Date(startAt).getTime() : null;
    const recurrenceType = document.getElementById('event-recurrence-type').value;
    const eventReminderEnabled = document.getElementById('event-reminder-enabled')?.checked === true;
    const courseModeEnabled = document.getElementById('event-course-enabled')?.checked === true && !state.editingEventId;
    const reminderRaw = reminderPickerSpec('event');
    const checkInReminderRaw = reminderPickerSpec('event-checkin');
    const checkInEnabled = document.getElementById('event-check-in').checked;
    let reminderOffsets = [];
    let checkInReminderOffsets = [];
    if (eventReminderEnabled && !courseModeEnabled) {
      try {
        reminderOffsets = parseReminderSpecs(reminderRaw, startAtMillis);
      } catch (error) {
        throw new Error('普通提醒：' + (error.message || '时间无效'));
      }
      if (checkInEnabled) {
        try {
          checkInReminderOffsets = parseReminderSpecs(checkInReminderRaw, startAtMillis);
        } catch (error) {
          throw new Error('签到提醒：' + (error.message || '时间无效'));
        }
      }
    }
    const payload = {
      title: document.getElementById('event-title').value,
      groupId: Number(document.getElementById('event-group').value || 0),
      location: document.getElementById('event-location').value,
      notes: document.getElementById('event-notes').value,
      startAt: startAt,
      endAt: endAt,
      allDay: document.getElementById('event-all-day').checked,
      accentColorHex: document.getElementById('event-color').value || DEFAULT_EVENT_COLOR,
      reminderOffsetsMinutes: reminderOffsets,
      countdownEnabled: document.getElementById('event-countdown').checked,
      checkInEnabled: checkInEnabled,
      checkInReminderOffsetsMinutes: checkInReminderOffsets,
      ringEnabled: eventReminderEnabled && document.getElementById('event-ring').checked,
      vibrateEnabled: eventReminderEnabled && document.getElementById('event-vibrate').checked,
      reminderDeliveryMode: 'FULLSCREEN_AND_NOTIFICATION',
      multiSlotBundleId: state.editingEventMultiSlotBundleId,
      syncMultiSlotBundleSharedFields: !!state.editingEventMultiSlotBundleId && document.getElementById('event-multi-slot-sync')?.checked === true,
      scope: recurrenceScopePayload('event', state.editingEventOriginalRecurring, recurrenceType),
      recurrence: recurrencePayload(
        recurrenceType,
        document.getElementById('event-recurrence-end').value,
        document.getElementById('event-weekdays').value
      )
    };
    if (courseModeEnabled) {
      if (!startAt) throw new Error('每周多时间段需要先填写起始日期作为起始周参考。');
      const baseDate = new Date(startAt);
      const recurrenceEnd = document.getElementById('event-recurrence-end').value || defaultCourseRecurrenceEndDate(baseDate);
      document.getElementById('event-recurrence-end').value = recurrenceEnd;
      const slots = readEventCourseSlots();
      if (!slots.length) throw new Error('请至少填写一个时间段。');
      const coursePayloads = [];
      for (const slot of slots) {
        const slotStart = courseSlotDateTimeValue(baseDate, slot.weekday, slot.startTime);
        const slotEnd = courseSlotDateTimeValue(baseDate, slot.weekday, slot.endTime);
        const slotStartMillis = new Date(slotStart).getTime();
        if (new Date(slotEnd).getTime() <= slotStartMillis) {
          throw new Error(weekdayLabel(slot.weekday) + ' ' + slot.startTime + '-' + slot.endTime + '：结束时间必须晚于开始时间');
        }
        let slotReminderOffsets = [];
        let slotCheckInReminderOffsets = [];
        if (eventReminderEnabled) {
          try {
            slotReminderOffsets = parseReminderSpecs(reminderRaw, slotStartMillis);
          } catch (error) {
            throw new Error(weekdayLabel(slot.weekday) + ' ' + slot.startTime + '：' + (error.message || '提醒时间无效'));
          }
          if (checkInEnabled) {
            try {
              slotCheckInReminderOffsets = parseReminderSpecs(checkInReminderRaw, slotStartMillis);
            } catch (error) {
              throw new Error(weekdayLabel(slot.weekday) + ' ' + slot.startTime + '：签到提醒' + (error.message || '时间无效'));
            }
          }
        }
        coursePayloads.push({
          ...payload,
          startAt: slotStart,
          endAt: slotEnd,
          allDay: false,
          reminderOffsetsMinutes: slotReminderOffsets,
          checkInReminderOffsetsMinutes: slotCheckInReminderOffsets,
          recurrence: {
            enabled: true,
            type: 'WEEKLY',
            weeklyDays: [slot.weekday],
            endDate: recurrenceEnd
          }
        });
      }
      await api('/api/events/batch', {
        method: 'POST',
        body: JSON.stringify({ multiSlotBundle: true, events: coursePayloads })
      });
      els.status.textContent = '已创建 ' + slots.length + ' 个每周时间段';
    } else if (state.editingEventId) {
      await api(`/api/events/${state.editingEventId}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await api('/api/events', { method: 'POST', body: JSON.stringify(payload) });
    }
    clearEventForm();
    closeModal('event-modal');
    await refreshAfterMutation();
  } catch (error) {
    const checkInError = String(error.message || '').includes('签到提醒');
    setInputInvalid(checkInError ? 'event-checkin-reminder-picker' : 'event-reminder-picker', true);
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

document.getElementById('preview-event-copy').onclick = () => {
  const eventItem = findEventById(state.previewEventId);
  if (!eventItem) return;
  closeModal('event-preview-modal');
  openEventEditor(eventItem);
  state.editingEventId = null;
  state.editingEventOriginalRecurring = false;
  state.editingEventMultiSlotBundleId = null;
  document.getElementById('event-modal-title').textContent = '创建日程副本';
  document.getElementById('save-event').textContent = '创建副本';
  document.getElementById('delete-event').classList.add('hidden');
  setRecurrenceScopeBlock('event', false);
  document.getElementById('event-multi-slot-sync').checked = false;
  document.getElementById('event-multi-slot-sync-block').classList.add('hidden');
};

document.getElementById('preview-event-cancel').onclick = async () => {
  if (!state.previewEventId) return;
  const eventItem = findEventById(state.previewEventId);
  const scopeQuery = await recurrenceScopeQueryForItem(eventItem, '取消');
  if (scopeQuery === null) return;
  if (!eventItem?.isRecurring && !await confirmDanger('确认取消日程', '取消后会停止提醒，并进入历史记录；这不是删除。', '确认取消')) return;
  await api(`/api/items/${state.previewEventId}/cancel${scopeQuery}`, { method: 'POST' });
  closeModal('event-preview-modal');
  await refreshAfterMutation();
};

document.getElementById('preview-event-delete').onclick = async () => {
  if (!state.previewEventId) return;
  const eventItem = findEventById(state.previewEventId);
  const scopeQuery = await recurrenceScopeQueryForItem(eventItem, '删除');
  if (scopeQuery === null) return;
  if (!await confirmDanger('确认删除日程', '删除后无法恢复。')) return;
  await api(`/api/items/${state.previewEventId}${scopeQuery}`, { method: 'DELETE' });
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
bindReminderOffsetPicker('todo');
bindReminderOffsetPicker('event');
bindReminderOffsetPicker('event-checkin');
bindEventColorPresets();
bindReviewImportControls();
bindAiReportControls();
syncTopbar();
