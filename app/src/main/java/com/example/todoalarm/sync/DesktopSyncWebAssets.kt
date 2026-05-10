package com.example.todoalarm.sync

object DesktopSyncWebAssets {
    fun indexHtml(): String = """
        <!doctype html>
        <html lang="zh-CN">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width,initial-scale=1" />
          <title>PaykiTodo Desktop Sync</title>
          <link rel="stylesheet" href="/app.css" />
        </head>
        <body>
          <div class="shell">
            <aside class="sidebar">
              <div class="sidebar-inner">
                <div class="brand-block">
                  <h1>PaykiTodo</h1>
                  <p class="muted">电脑端时间轴控制台</p>
                </div>
                <div class="sidebar-card">
                  <label>访问密钥</label>
                  <input id="token" type="password" placeholder="输入手机里显示的密钥" />
                  <button id="connect">连接手机</button>
                  <div id="status" class="muted status">尚未连接</div>
                </div>
                <div class="sidebar-card">
                  <button class="ghost tab-btn active" data-tab="todos">待办时间轴</button>
                  <button class="ghost tab-btn" data-tab="events">日程时间轴</button>
                </div>
              </div>
            </aside>
            <main class="content">
              <div class="topbar card-panel">
                <div>
                  <div id="view-caption" class="eyebrow">桌面端时间轴模式</div>
                  <div id="panel-title" class="title">待办时间轴</div>
                  <div id="snapshot-meta" class="muted">连接后即可读取手机端当前数据</div>
                </div>
                <div class="toolbar-row">
                  <button id="jump-today" class="ghost mini">今天</button>
                  <button id="refresh" class="ghost mini">刷新</button>
                  <button id="open-create" class="mini">新增待办</button>
                </div>
              </div>

              <section id="todos-panel" class="tab active">
                <div id="todo-summary" class="summary-grid"></div>
                <div class="card-panel timeline-panel">
                  <div class="panel-head">
                    <h2>待办主时间轴</h2>
                    <p class="muted">按已错过、今日、计划中、无 DDL、历史记录分区展示。</p>
                  </div>
                  <div id="todo-timeline" class="timeline-root"></div>
                </div>
              </section>

              <section id="events-panel" class="tab">
                <div id="event-summary" class="summary-grid"></div>
                <div class="card-panel day-strip-panel">
                  <div class="panel-head">
                    <h2>日期轴</h2>
                    <p class="muted">选择一天，在下方查看该日的全天日程和时间轴。</p>
                  </div>
                  <div id="event-day-strip" class="day-strip"></div>
                </div>
                <div class="card-panel schedule-panel">
                  <div class="schedule-head">
                    <div id="event-selected-date" class="schedule-date-title">今天</div>
                    <div id="event-selected-subtitle" class="muted">暂无日程</div>
                  </div>
                  <section class="all-day-section">
                    <div class="slot-title">全天</div>
                    <div id="all-day-list" class="all-day-list"></div>
                  </section>
                  <section class="schedule-board-shell">
                    <div id="hour-axis" class="hour-axis"></div>
                    <div id="board-scroll" class="board-scroll">
                      <div id="event-timeline" class="event-timeline"></div>
                    </div>
                  </section>
                </div>
              </section>
            </main>
          </div>

          <div id="todo-modal" class="modal-backdrop hidden">
            <div class="modal-sheet">
              <div class="modal-head">
                <div>
                  <h2>新增待办</h2>
                  <p class="muted">表单先收进弹层，主界面优先展示时间轴。</p>
                </div>
                <button class="ghost mini" data-close-modal="todo-modal">关闭</button>
              </div>
              <div class="form-grid">
                <div class="span-2"><label>标题</label><input id="todo-title" placeholder="标题" /></div>
                <div class="span-2"><label>备注</label><textarea id="todo-notes" placeholder="备注"></textarea></div>
                <div><label>DDL</label><input id="todo-due" type="datetime-local" /></div>
                <div><label>提醒时间</label><input id="todo-reminder" type="datetime-local" /></div>
                <div>
                  <label>循环</label>
                  <select id="todo-recurrence-type">
                    <option value="NONE">不重复</option><option value="DAILY">每天</option><option value="WEEKLY">每周</option>
                    <option value="MONTHLY_NTH_WEEKDAY">每月第几个星期几</option><option value="MONTHLY_DAY">每月D日</option><option value="YEARLY_DATE">每年M月D日</option>
                  </select>
                </div>
                <div><label>循环截止日期</label><input id="todo-recurrence-end" type="date" /></div>
                <div class="span-2"><label>每周循环的周几（逗号分隔，例如 1,3,5）</label><input id="todo-weekdays" placeholder="1,3,5" /></div>
                <div class="span-2 switch-row"><label><input id="todo-ring" type="checkbox" checked /> 铃声</label><label><input id="todo-vibrate" type="checkbox" checked /> 震动</label></div>
              </div>
              <div class="modal-actions"><button id="create-todo">创建待办</button></div>
            </div>
          </div>

          <div id="event-modal" class="modal-backdrop hidden">
            <div class="modal-sheet wide-sheet">
              <div class="modal-head">
                <div>
                  <h2>新增日程</h2>
                  <p class="muted">日程编辑先集中在弹层，主体区域优先展示时间轴。</p>
                </div>
                <button class="ghost mini" data-close-modal="event-modal">关闭</button>
              </div>
              <div class="form-grid">
                <div class="span-2"><label>标题</label><input id="event-title" placeholder="标题" /></div>
                <div><label>地点</label><input id="event-location" placeholder="地点" /></div>
                <div><label>提醒方式</label><select id="event-reminder-mode"><option value="NOTIFICATION">通知栏提醒</option><option value="FULLSCREEN">全屏提醒</option></select></div>
                <div class="span-2"><label>备注</label><textarea id="event-notes" placeholder="备注"></textarea></div>
                <div><label>开始</label><input id="event-start" type="datetime-local" /></div>
                <div><label>结束</label><input id="event-end" type="datetime-local" /></div>
                <div><label>提醒时间点（分钟，逗号分隔，例如 5,15,60）</label><input id="event-reminder-offsets" placeholder="5,15" /></div>
                <div>
                  <label>循环</label>
                  <select id="event-recurrence-type">
                    <option value="NONE">不重复</option><option value="DAILY">每天</option><option value="WEEKLY">每周</option>
                    <option value="MONTHLY_NTH_WEEKDAY">每月第几个星期几</option><option value="MONTHLY_DAY">每月D日</option><option value="YEARLY_DATE">每年M月D日</option>
                  </select>
                </div>
                <div><label>循环截止日期</label><input id="event-recurrence-end" type="date" /></div>
                <div><label>每周循环的周几（逗号分隔，例如 1,3,5）</label><input id="event-weekdays" placeholder="1,3,5" /></div>
                <div class="span-2 switch-row"><label><input id="event-ring" type="checkbox" checked /> 铃声</label><label><input id="event-vibrate" type="checkbox" checked /> 震动</label></div>
              </div>
              <div class="modal-actions"><button id="create-event">创建日程</button></div>
            </div>
          </div>
          <script src="/app.js"></script>
        </body>
        </html>
    """.trimIndent()

    fun appCss(): String = """
        :root {
          --bg-1: #eef4fb;
          --bg-2: #dfeeff;
          --panel: rgba(255,255,255,.9);
          --panel-strong: rgba(255,255,255,.96);
          --text: #182230;
          --muted: #627285;
          --line: #d7e0ea;
          --line-strong: #b9c8da;
          --primary: #3568d4;
          --danger: #d63c3c;
          --success: #1d8f5a;
          --sidebar: #243246;
          --timeline-rail: #d5e1f0;
          --hour-height: 64px;
          --card-shadow: 0 18px 40px rgba(36,57,86,.09);
        }
        * { box-sizing: border-box; }
        html, body { margin: 0; min-height: 100%; }
        body {
          font-family: "Microsoft YaHei UI", "PingFang SC", sans-serif;
          color: var(--text);
          background: radial-gradient(circle at top left, rgba(255,255,255,.72), transparent 32%), linear-gradient(135deg, var(--bg-1), var(--bg-2) 46%, #d8f4e5);
        }
        button, input, textarea, select { font: inherit; }
        .shell { display: grid; grid-template-columns: 256px minmax(0, 1fr); min-height: 100vh; }
        .sidebar {
          position: sticky;
          top: 0;
          height: 100vh;
          overflow: hidden;
          background: linear-gradient(180deg, #223146, #263751 45%, #213147);
          color: #fff;
          border-right: 1px solid rgba(255,255,255,.08);
        }
        .sidebar-inner {
          height: 100%;
          overflow: auto;
          padding: 18px;
          display: flex;
          flex-direction: column;
          gap: 14px;
        }
        .brand-block h1, h2, h3, p { margin: 0; }
        .brand-block h1 { font-size: 28px; font-weight: 800; }
        .muted { color: var(--muted); }
        .sidebar .muted { color: rgba(255,255,255,.72); }
        .sidebar-card, .card-panel {
          background: var(--panel);
          border: 1px solid rgba(255,255,255,.46);
          box-shadow: var(--card-shadow);
          backdrop-filter: blur(18px);
          border-radius: 24px;
        }
        .sidebar-card {
          padding: 16px;
          background: rgba(255,255,255,.08);
          border-color: rgba(255,255,255,.08);
          box-shadow: none;
        }
        .sidebar-card label { display: block; margin-bottom: 8px; font-size: 13px; font-weight: 700; color: rgba(255,255,255,.92); }
        .sidebar-card input,
        .sidebar-card button,
        .modal-sheet input,
        .modal-sheet textarea,
        .modal-sheet select,
        .modal-sheet button {
          width: 100%;
          border-radius: 16px;
          border: 1px solid var(--line);
          padding: 12px 14px;
          background: #fff;
          color: var(--text);
        }
        button {
          cursor: pointer;
          border: none;
          background: var(--primary);
          color: #fff;
          font-weight: 700;
        }
        button.ghost {
          background: transparent;
          color: inherit;
          border: 1px solid rgba(255,255,255,.18);
        }
        .tab-btn { margin-bottom: 10px; text-align: left; background: rgba(255,255,255,.04); color: #fff; border-color: rgba(255,255,255,.12); }
        .tab-btn.active { background: rgba(53,104,212,.9); border-color: transparent; }
        .status { margin-top: 10px; font-size: 13px; }
        .content { padding: 18px 18px 24px; min-width: 0; }
        .topbar {
          position: sticky;
          top: 18px;
          z-index: 20;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 18px;
          padding: 18px 20px;
          margin-bottom: 16px;
        }
        .eyebrow { font-size: 12px; font-weight: 800; letter-spacing: .08em; color: var(--primary); text-transform: uppercase; margin-bottom: 6px; }
        .title { font-size: 28px; font-weight: 800; margin-bottom: 4px; }
        .toolbar-row { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 10px; }
        .mini { width: auto !important; min-width: 96px; }
        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 14px; }
        .summary-card { padding: 16px 18px; border-radius: 20px; background: var(--panel-strong); border: 1px solid rgba(255,255,255,.58); box-shadow: var(--card-shadow); }
        .summary-label { font-size: 13px; color: var(--muted); margin-bottom: 6px; }
        .summary-value { font-size: 26px; font-weight: 800; }
        .tab { display: none; }
        .tab.active { display: block; }
        .timeline-panel, .day-strip-panel, .schedule-panel { padding: 18px; }
        .panel-head { margin-bottom: 16px; }
        .timeline-root { display: flex; flex-direction: column; gap: 18px; }
        .timeline-section { position: relative; padding-left: 18px; }
        .timeline-section::before { content: ""; position: absolute; top: 40px; bottom: 8px; left: 8px; width: 2px; background: linear-gradient(180deg, var(--timeline-rail), rgba(213,225,240,.18)); }
        .timeline-section-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
        .timeline-section-title { font-size: 19px; font-weight: 800; }
        .timeline-section-count { font-size: 12px; color: var(--muted); padding: 5px 10px; border-radius: 999px; background: rgba(98,114,133,.1); }
        .timeline-list { display: flex; flex-direction: column; gap: 12px; }
        .timeline-item { position: relative; display: grid; grid-template-columns: 124px minmax(0, 1fr); gap: 14px; align-items: start; min-width: 0; }
        .timeline-item::before { content: ""; position: absolute; left: -13px; top: 14px; width: 10px; height: 10px; border-radius: 50%; background: var(--accent, var(--primary)); box-shadow: 0 0 0 5px rgba(53,104,212,.12); }
        .timeline-time { font-size: 18px; font-weight: 800; line-height: 1.1; }
        .timeline-subtime { margin-top: 4px; font-size: 12px; color: var(--muted); }
        .timeline-card { min-width: 0; border-radius: 20px; border: 1px solid rgba(211,220,230,.92); background: rgba(255,255,255,.9); box-shadow: 0 10px 26px rgba(46,64,98,.06); padding: 14px 16px; }
        .timeline-card-title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 6px; }
        .timeline-card-title { font-size: 18px; font-weight: 800; line-height: 1.35; word-break: break-word; }
        .pill { flex-shrink: 0; display: inline-flex; align-items: center; padding: 6px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; color: var(--muted); background: rgba(98,114,133,.1); }
        .timeline-card-meta, .timeline-card-notes, .all-day-card-meta, .event-card-meta, .event-card-notes { font-size: 13px; color: var(--muted); line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
        .timeline-card-notes, .event-card-notes { margin-top: 6px; color: #405063; }
        .actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
        .actions button { width: auto; min-width: 76px; padding: 9px 12px; border-radius: 12px; }
        .actions .secondary { background: #6b7280; }
        .actions .danger { background: var(--danger); }
        .actions .success { background: var(--success); }
        .empty-state { padding: 16px 18px; border-radius: 18px; border: 1px dashed var(--line-strong); color: var(--muted); background: rgba(255,255,255,.58); }
        .day-strip { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 4px; }
        .day-pill { flex: 0 0 auto; min-width: 116px; padding: 14px 16px; border-radius: 18px; border: 1px solid rgba(211,220,230,.94); background: rgba(255,255,255,.9); color: var(--text); text-align: left; }
        .day-pill.active { background: rgba(53,104,212,.96); color: #fff; border-color: transparent; box-shadow: 0 12px 26px rgba(53,104,212,.24); }
        .day-pill-weekday { font-size: 12px; opacity: .82; }
        .day-pill-date { margin-top: 4px; font-size: 20px; font-weight: 800; }
        .day-pill-meta { margin-top: 4px; font-size: 12px; opacity: .8; }
        .schedule-panel { display: flex; flex-direction: column; gap: 18px; }
        .schedule-date-title { font-size: 22px; font-weight: 800; margin-bottom: 6px; }
        .all-day-section { border-radius: 20px; border: 1px solid rgba(211,220,230,.9); background: rgba(255,255,255,.66); padding: 14px; }
        .slot-title { font-size: 13px; font-weight: 800; color: var(--muted); margin-bottom: 10px; }
        .all-day-list { display: flex; flex-direction: column; gap: 10px; }
        .all-day-card { padding: 12px 14px; border-radius: 16px; border-left: 4px solid var(--accent, var(--primary)); background: rgba(255,255,255,.92); box-shadow: 0 8px 18px rgba(39,56,88,.05); }
        .all-day-card-title, .event-card-title { font-size: 16px; font-weight: 800; margin-bottom: 4px; }
        .schedule-board-shell { display: grid; grid-template-columns: 84px minmax(0, 1fr); gap: 14px; align-items: start; min-width: 0; }
        .hour-axis { position: sticky; top: 126px; }
        .hour-label { height: var(--hour-height); display: flex; align-items: flex-start; justify-content: flex-end; font-size: 12px; font-weight: 700; color: var(--muted); }
        .board-scroll { min-width: 0; overflow: auto; border-radius: 22px; border: 1px solid rgba(211,220,230,.92); background: rgba(255,255,255,.72); padding: 0 10px 0 0; }
        .event-timeline { position: relative; min-width: 720px; height: calc(var(--hour-height) * 24); border-left: 1px solid rgba(185,200,218,.9); margin-left: 10px; }
        .hour-row { position: absolute; left: 0; right: 0; height: var(--hour-height); border-top: 1px solid rgba(188,201,217,.9); }
        .half-row { position: absolute; left: 0; right: 0; border-top: 1px dashed rgba(188,201,217,.4); }
        .event-card { position: absolute; left: 18px; right: 18px; min-height: 34px; border-radius: 18px; padding: 12px 14px; border-left: 4px solid var(--accent, var(--primary)); background: rgba(255,255,255,.95); box-shadow: 0 10px 24px rgba(40,57,88,.08); overflow: hidden; }
        .current-line { position: absolute; left: 0; right: 0; height: 2px; background: #dc2626; }
        .current-line::before { content: ""; position: absolute; left: -6px; top: -5px; width: 12px; height: 12px; border-radius: 50%; background: #dc2626; }
        .modal-backdrop { position: fixed; inset: 0; background: rgba(12,20,31,.45); display: flex; align-items: center; justify-content: center; padding: 24px; z-index: 100; }
        .modal-backdrop.hidden { display: none; }
        .modal-sheet { width: min(760px, 100%); max-height: calc(100vh - 48px); overflow: auto; border-radius: 26px; background: rgba(255,255,255,.98); border: 1px solid rgba(255,255,255,.72); box-shadow: 0 28px 70px rgba(15,24,39,.22); padding: 20px; }
        .wide-sheet { width: min(900px, 100%); }
        .modal-head, .modal-actions { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
        .modal-head { margin-bottom: 16px; }
        .modal-actions { margin-top: 18px; justify-content: flex-end; }
        .form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
        .span-2 { grid-column: 1 / -1; }
        .modal-sheet label { display: block; margin-bottom: 8px; font-size: 13px; font-weight: 800; color: #314154; }
        .modal-sheet textarea { min-height: 86px; resize: vertical; }
        .switch-row { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
        .switch-row label { display: inline-flex; align-items: center; gap: 8px; margin: 0; }
        .switch-row input { width: auto; margin: 0; }
        @media (max-width: 1080px) {
          .shell { grid-template-columns: 1fr; }
          .sidebar { position: static; height: auto; }
          .topbar, .hour-axis { position: static; }
        }
        @media (max-width: 840px) {
          .toolbar-row, .form-grid { display: flex; flex-wrap: wrap; }
          .schedule-board-shell, .timeline-item { grid-template-columns: 1fr; }
          .event-timeline { min-width: 100%; }
        }
      """.trimIndent()

    fun appJs(): String = """
        const HOUR_HEIGHT = 64;
        const state = { token: '', snapshot: null, currentTab: 'todos', selectedEventDay: dayKey(new Date()) };

        const els = {
          token: document.getElementById('token'),
          status: document.getElementById('status'),
          todoSummary: document.getElementById('todo-summary'),
          eventSummary: document.getElementById('event-summary'),
          todoTimeline: document.getElementById('todo-timeline'),
          eventDayStrip: document.getElementById('event-day-strip'),
          eventSelectedDate: document.getElementById('event-selected-date'),
          eventSelectedSubtitle: document.getElementById('event-selected-subtitle'),
          allDayList: document.getElementById('all-day-list'),
          hourAxis: document.getElementById('hour-axis'),
          eventTimeline: document.getElementById('event-timeline'),
          snapshotMeta: document.getElementById('snapshot-meta'),
          panelTitle: document.getElementById('panel-title'),
          viewCaption: document.getElementById('view-caption'),
          openCreate: document.getElementById('open-create'),
          boardScroll: document.getElementById('board-scroll')
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
            : '<button data-action="complete" data-id="' + item.id + '" class="success">完成</button><button data-action="cancel" data-id="' + item.id + '" class="danger">取消</button>';
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

        function renderDayPill(key, count) {
          const date = dateFromKey(key);
          return ''
            + '<button class="day-pill ' + (key === state.selectedEventDay ? 'active' : '') + '" data-day="' + key + '">'
            +   '<div class="day-pill-weekday">' + escapeHtml(formatWeekday(date)) + '</div>'
            +   '<div class="day-pill-date">' + (date.getMonth() + 1) + '/' + date.getDate() + '</div>'
            +   '<div class="day-pill-meta">' + count + ' 项</div>'
            + '</button>';
        }

        function renderAllDayCard(item) {
          const accent = item.groupColorHex || item.accentColorHex || '#4e87e1';
          const meta = [item.groupName || '未分组', item.location || '', item.isRecurring ? '循环' : ''].filter(Boolean).join(' · ');
          return ''
            + '<article class="all-day-card" style="--accent:' + accent + '">'
            +   '<div class="all-day-card-title">' + escapeHtml(item.title) + '</div>'
            +   '<div class="all-day-card-meta">' + escapeHtml(meta || '全天日程') + '</div>'
            +   (item.notes ? '<div class="all-day-card-meta">' + escapeHtml(item.notes) + '</div>' : '')
            +   '<div class="actions"><button data-action="cancel" data-id="' + item.id + '" class="danger">删除</button></div>'
            + '</article>';
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
          let html = '';
          for (let hour = 0; hour < 24; hour += 1) {
            html += '<div class="hour-label">' + String(hour).padStart(2, '0') + ':00</div>';
          }
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
          if (key !== dayKey(new Date())) return '';
          const now = new Date();
          const top = (now.getHours() * 60 + now.getMinutes()) / 60 * HOUR_HEIGHT;
          return '<div class="current-line" style="top:' + top + 'px"></div>';
        }

        function renderEventCard(segment) {
          const item = segment.item;
          const accent = item.accentColorHex || item.groupColorHex || '#4e87e1';
          const meta = [item.groupName || '未分组', item.location || '', item.isRecurring ? '循环' : ''].filter(Boolean).join(' · ');
          return ''
            + '<article class="event-card" style="--accent:' + accent + ';top:' + segment.top + 'px;height:' + segment.height + 'px">'
            +   '<div class="event-card-title">' + escapeHtml(item.title) + '</div>'
            +   '<div class="event-card-meta">' + escapeHtml(segment.startLabel) + ' - ' + escapeHtml(segment.endLabel) + '</div>'
            +   '<div class="event-card-meta">' + escapeHtml(meta || '定时日程') + '</div>'
            +   (item.notes ? '<div class="event-card-notes">' + escapeHtml(item.notes) + '</div>' : '')
            +   '<div class="actions"><button data-action="cancel" data-id="' + item.id + '" class="danger">删除</button></div>'
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
          const keys = new Set([dayKey(new Date())]);
          activeEvents().forEach(item => {
            let cursor = new Date(eventStart(item));
            cursor.setHours(0, 0, 0, 0);
            const last = new Date(Math.max(eventStart(item), eventEnd(item) - 1));
            last.setHours(0, 0, 0, 0);
            while (cursor.getTime() <= last.getTime()) {
              keys.add(dayKey(cursor));
              cursor.setDate(cursor.getDate() + 1);
            }
          });
          const list = Array.from(keys).sort();
          if (!list.includes(state.selectedEventDay)) state.selectedEventDay = list[0] || dayKey(new Date());
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
          dayMap.set(dayKey(new Date()), []);
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
          const keys = Array.from(dayMap.keys()).sort();
          const selected = (dayMap.get(state.selectedEventDay) || []).slice().sort((a, b) => eventStart(a) - eventStart(b));
          const allDay = selected.filter(item => item.allDay);
          const timed = selected.filter(item => !item.allDay).map(item => buildEventSegment(item, state.selectedEventDay));
          els.eventSummary.innerHTML = [
            renderSummaryCard('活动日程', events.length),
            renderSummaryCard('可切换日期', keys.length),
            renderSummaryCard('全天日程', allDay.length),
            renderSummaryCard('当天定时日程', timed.length)
          ].join('');
          els.eventDayStrip.innerHTML = keys.length
            ? keys.map(key => renderDayPill(key, (dayMap.get(key) || []).length)).join('')
            : '<div class="empty-state">当前没有活动日程。创建后会在这里按日期排开。</div>';
          document.querySelectorAll('[data-day]').forEach(node => {
            node.onclick = () => {
              state.selectedEventDay = node.dataset.day;
              renderEvents();
            };
          });
          const selectedDate = dateFromKey(state.selectedEventDay);
          els.eventSelectedDate.textContent = formatFullDateLabel(selectedDate);
          els.eventSelectedSubtitle.textContent = selected.length ? ('全天 ' + allDay.length + ' 项，定时 ' + timed.length + ' 项') : '本日暂无日程';
          els.allDayList.innerHTML = allDay.length ? allDay.map(renderAllDayCard).join('') : '<div class="empty-state">本日没有全天日程。</div>';
          els.hourAxis.innerHTML = renderHourAxis();
          els.eventTimeline.innerHTML = renderHourGrid() + renderCurrentLine(state.selectedEventDay) + timed.map(renderEventCard).join('');
          if (els.boardScroll) {
            if (state.selectedEventDay === dayKey(new Date())) {
              const nowDate = new Date();
              els.boardScroll.scrollTop = Math.max(0, ((nowDate.getHours() * 60 + nowDate.getMinutes()) / 60 * HOUR_HEIGHT) - 180);
            } else {
              els.boardScroll.scrollTop = timed.length ? Math.max(0, timed[0].top - 80) : 0;
            }
          }
          bindActions();
        }

        function escapeHtml(text) {
          return String(text || '').replace(/[&<>\"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[ch]));
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

        function clearTodoForm() {
          document.getElementById('todo-title').value = '';
          document.getElementById('todo-notes').value = '';
          document.getElementById('todo-due').value = '';
          document.getElementById('todo-reminder').value = '';
          document.getElementById('todo-recurrence-type').value = 'NONE';
          document.getElementById('todo-recurrence-end').value = '';
          document.getElementById('todo-weekdays').value = '';
          document.getElementById('todo-ring').checked = true;
          document.getElementById('todo-vibrate').checked = true;
        }

        function clearEventForm() {
          document.getElementById('event-title').value = '';
          document.getElementById('event-location').value = '';
          document.getElementById('event-notes').value = '';
          document.getElementById('event-start').value = '';
          document.getElementById('event-end').value = '';
          document.getElementById('event-reminder-mode').value = 'NOTIFICATION';
          document.getElementById('event-reminder-offsets').value = '';
          document.getElementById('event-recurrence-type').value = 'NONE';
          document.getElementById('event-recurrence-end').value = '';
          document.getElementById('event-weekdays').value = '';
          document.getElementById('event-ring').checked = true;
          document.getElementById('event-vibrate').checked = true;
        }

        function openModal(id) {
          const node = document.getElementById(id);
          if (!node) return;
          node.classList.remove('hidden');
          const focusTarget = node.querySelector('input, textarea, select, button');
          if (focusTarget) focusTarget.focus();
        }

        function closeModal(id) {
          const node = document.getElementById(id);
          if (!node) return;
          node.classList.add('hidden');
        }

        function bindActions() {
          document.querySelectorAll('[data-action]').forEach(node => {
            node.onclick = async () => {
              const id = node.dataset.id;
              if (node.dataset.action === 'complete') {
                await api(`/api/items/${'$'}{id}/complete`, { method: 'POST' });
              } else if (node.dataset.action === 'cancel') {
                await api(`/api/items/${'$'}{id}/cancel`, { method: 'POST' });
              } else {
                await api(`/api/items/${'$'}{id}`, { method: 'DELETE' });
              }
              await loadSnapshot();
            };
          });
        }

        document.getElementById('connect').onclick = () => connect().catch(err => els.status.textContent = err.message);
        document.getElementById('refresh').onclick = () => loadSnapshot().catch(err => els.status.textContent = err.message);
        document.getElementById('jump-today').onclick = () => {
          if (state.currentTab === 'events') {
            state.selectedEventDay = dayKey(new Date());
            renderEvents();
          } else {
            document.getElementById('todo-section-today')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }
        };
        els.openCreate.onclick = () => openModal(state.currentTab === 'todos' ? 'todo-modal' : 'event-modal');
        document.querySelectorAll('[data-tab]').forEach(node => {
          node.onclick = () => {
            state.currentTab = node.dataset.tab;
            document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
            document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
            document.getElementById(`${'$'}{node.dataset.tab}-panel`).classList.add('active');
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
          await api('/api/todos', {
            method: 'POST',
            body: JSON.stringify({
              title: document.getElementById('todo-title').value,
              notes: document.getElementById('todo-notes').value,
              dueAt: document.getElementById('todo-due').value || null,
              reminderAt: document.getElementById('todo-reminder').value || null,
              ringEnabled: document.getElementById('todo-ring').checked,
              vibrateEnabled: document.getElementById('todo-vibrate').checked,
              recurrence: recurrencePayload(
                document.getElementById('todo-recurrence-type').value,
                document.getElementById('todo-recurrence-end').value,
                document.getElementById('todo-weekdays').value
              )
            })
          });
          clearTodoForm();
          closeModal('todo-modal');
          await loadSnapshot();
        };

        document.getElementById('create-event').onclick = async () => {
          await api('/api/events', {
            method: 'POST',
            body: JSON.stringify({
              title: document.getElementById('event-title').value,
              location: document.getElementById('event-location').value,
              notes: document.getElementById('event-notes').value,
              startAt: document.getElementById('event-start').value,
              endAt: document.getElementById('event-end').value,
              reminderOffsetsMinutes: parseIntList(document.getElementById('event-reminder-offsets').value),
              ringEnabled: document.getElementById('event-ring').checked,
              vibrateEnabled: document.getElementById('event-vibrate').checked,
              reminderDeliveryMode: document.getElementById('event-reminder-mode').value,
              recurrence: recurrencePayload(
                document.getElementById('event-recurrence-type').value,
                document.getElementById('event-recurrence-end').value,
                document.getElementById('event-weekdays').value
              )
            })
          });
          clearEventForm();
          closeModal('event-modal');
          await loadSnapshot();
        };

        syncTopbar();
      """.trimIndent()
}
