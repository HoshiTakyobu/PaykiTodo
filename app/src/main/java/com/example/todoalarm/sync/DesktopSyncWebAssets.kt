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
                <h1>PaykiTodo</h1>
                <p class="muted">电脑端同步控制台</p>
                <label>访问密钥</label>
                <input id="token" type="password" placeholder="输入手机里显示的密钥" />
                <button id="connect">连接手机</button>
                <div id="status" class="muted status">尚未连接</div>
                <hr />
                <button class="ghost tab-btn active" data-tab="todos">待办</button>
                <button class="ghost tab-btn" data-tab="events">日程</button>
              </div>
            </aside>
            <main class="content">
              <div class="topbar">
                <div>
                  <div class="title">PaykiTodo Desktop Sync</div>
                  <div class="muted">直接管理手机端待办与日程</div>
                </div>
                <button id="refresh" class="mini">刷新</button>
              </div>

              <section id="todos-panel" class="tab active">
                <div class="layout-grid">
                  <div class="editor-column">
                    <div class="card sticky-card">
                      <h2>新增待办</h2>
                      <input id="todo-title" placeholder="标题" />
                      <textarea id="todo-notes" placeholder="备注"></textarea>
                      <label>DDL</label>
                      <input id="todo-due" type="datetime-local" />
                      <label>提醒时间</label>
                      <input id="todo-reminder" type="datetime-local" />
                      <label>循环</label>
                      <select id="todo-recurrence-type">
                        <option value="NONE">不重复</option>
                        <option value="DAILY">每天</option>
                        <option value="WEEKLY">每周</option>
                        <option value="MONTHLY_NTH_WEEKDAY">每月第几个星期几</option>
                        <option value="MONTHLY_DAY">每月D日</option>
                        <option value="YEARLY_DATE">每年M月D日</option>
                      </select>
                      <label>循环截止日期</label>
                      <input id="todo-recurrence-end" type="date" />
                      <label>每周循环的周几（逗号分隔，例如 1,3,5）</label>
                      <input id="todo-weekdays" placeholder="1,3,5" />
                      <div class="switch-row">
                        <label><input id="todo-ring" type="checkbox" checked /> 铃声</label>
                        <label><input id="todo-vibrate" type="checkbox" checked /> 震动</label>
                      </div>
                      <button id="create-todo">创建待办</button>
                    </div>
                  </div>
                  <div class="list-column">
                    <div class="card">
                      <h2>待办列表</h2>
                      <div id="todo-list"></div>
                    </div>
                  </div>
                </div>
              </section>

              <section id="events-panel" class="tab">
                <div class="layout-grid">
                  <div class="editor-column">
                    <div class="card sticky-card">
                      <h2>新增日程</h2>
                      <input id="event-title" placeholder="标题" />
                      <input id="event-location" placeholder="地点" />
                      <textarea id="event-notes" placeholder="备注"></textarea>
                      <label>开始</label>
                      <input id="event-start" type="datetime-local" />
                      <label>结束</label>
                      <input id="event-end" type="datetime-local" />
                      <label>提醒方式</label>
                      <select id="event-reminder-mode">
                        <option value="NOTIFICATION">通知栏提醒</option>
                        <option value="FULLSCREEN">全屏提醒</option>
                      </select>
                      <label>提醒时间点（分钟，逗号分隔，例如 5,15,60）</label>
                      <input id="event-reminder-offsets" placeholder="5,15" />
                      <label>循环</label>
                      <select id="event-recurrence-type">
                        <option value="NONE">不重复</option>
                        <option value="DAILY">每天</option>
                        <option value="WEEKLY">每周</option>
                        <option value="MONTHLY_NTH_WEEKDAY">每月第几个星期几</option>
                        <option value="MONTHLY_DAY">每月D日</option>
                        <option value="YEARLY_DATE">每年M月D日</option>
                      </select>
                      <label>循环截止日期</label>
                      <input id="event-recurrence-end" type="date" />
                      <label>每周循环的周几（逗号分隔，例如 1,3,5）</label>
                      <input id="event-weekdays" placeholder="1,3,5" />
                      <div class="switch-row">
                        <label><input id="event-ring" type="checkbox" checked /> 铃声</label>
                        <label><input id="event-vibrate" type="checkbox" checked /> 震动</label>
                      </div>
                      <button id="create-event">创建日程</button>
                    </div>
                  </div>
                  <div class="list-column">
                    <div class="card">
                      <h2>日程列表</h2>
                      <div id="event-list"></div>
                    </div>
                  </div>
                </div>
              </section>
            </main>
          </div>
          <script src="/app.js"></script>
        </body>
        </html>
    """.trimIndent()

    fun appCss(): String = """
        :root {
          --bg: #eaf1f9;
          --panel: rgba(255,255,255,.92);
          --text: #17202b;
          --muted: #617084;
          --line: #d4dde8;
          --primary: #3568d4;
          --sidebar: #263145;
        }
        * { box-sizing: border-box; }
        html, body { margin: 0; min-height: 100%; }
        body {
          font-family: "Microsoft YaHei UI", "PingFang SC", sans-serif;
          background: linear-gradient(135deg, #dbeafe, #edf3f8 45%, #d7f7e4);
          color: var(--text);
        }
        .shell { display: grid; grid-template-columns: 232px 1fr; min-height: 100vh; }
        .sidebar {
          background: var(--sidebar);
          color: white;
          position: sticky;
          top: 0;
          height: 100vh;
          overflow: hidden;
          border-right: 1px solid rgba(255,255,255,.08);
        }
        .sidebar-inner {
          padding: 20px;
          height: 100%;
          overflow: auto;
        }
        .content { padding: 20px 14px 24px; }
        .topbar {
          position: sticky;
          top: 0;
          z-index: 20;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 12px;
          padding: 14px 16px;
          margin-bottom: 14px;
          border-radius: 18px;
          background: rgba(234,241,249,.78);
          backdrop-filter: blur(14px);
        }
        .title { font-size: 22px; font-weight: 800; }
        .muted { color: var(--muted); }
        .sidebar .muted { color: rgba(255,255,255,.72); }
        .status { margin-top: 10px; }
        .layout-grid { display: grid; grid-template-columns: 320px 1fr; gap: 14px; align-items: start; }
        .editor-column { min-width: 0; }
        .list-column { min-width: 0; }
        .card {
          background: var(--panel);
          border: 1px solid rgba(255,255,255,.5);
          backdrop-filter: blur(18px);
          border-radius: 22px;
          padding: 18px;
          box-shadow: 0 14px 42px rgba(41,55,87,.08);
        }
        .sticky-card {
          position: sticky;
          top: 90px;
          max-height: calc(100vh - 110px);
          overflow: auto;
        }
        .tab { display: none; }
        .tab.active { display: block; }
        h1, h2 { margin: 0 0 10px; }
        label { display: block; font-size: 13px; font-weight: 700; margin-top: 10px; }
        input, textarea, button, select {
          width: 100%;
          margin-top: 8px;
          border-radius: 14px;
          border: 1px solid var(--line);
          padding: 11px 13px;
          font: inherit;
          background: white;
        }
        textarea { min-height: 78px; resize: vertical; }
        button {
          background: var(--primary);
          color: white;
          border: none;
          cursor: pointer;
          font-weight: 700;
        }
        button.ghost {
          background: transparent;
          border: 1px solid rgba(255,255,255,.16);
          color: white;
          margin-bottom: 10px;
        }
        button.tab-btn.active { background: rgba(53,104,212,.9); }
        button.mini { width: auto; min-width: 104px; }
        .switch-row { display: flex; gap: 14px; margin-top: 10px; }
        .switch-row label {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          margin-top: 0;
          font-weight: 600;
        }
        .switch-row input { width: auto; margin-top: 0; }
        .item {
          border: 1px solid var(--line);
          border-left: 6px solid #4e87e1;
          border-radius: 18px;
          padding: 14px;
          margin-bottom: 12px;
          background: rgba(255,255,255,.82);
        }
        .item h3 { margin: 0 0 6px; font-size: 18px; }
        .meta { color: var(--muted); font-size: 13px; margin-top: 4px; white-space: pre-wrap; }
        .actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
        .actions button { width: auto; padding: 8px 12px; border-radius: 12px; }
        .actions .danger { background: #dc2626; }
        .actions .secondary { background: #6b7280; }
        @media (max-width: 1120px) {
          .shell, .layout-grid { grid-template-columns: 1fr; }
          .sidebar { position: static; height: auto; }
          .sticky-card, .topbar { position: static; max-height: none; }
        }
      """.trimIndent()

    fun appJs(): String = """
        const state = { token: '', snapshot: null, currentTab: 'todos' };

        const els = {
          token: document.getElementById('token'),
          status: document.getElementById('status'),
          todoList: document.getElementById('todo-list'),
          eventList: document.getElementById('event-list')
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

        async function connect() {
          state.token = els.token.value.trim();
          await loadSnapshot();
          els.status.textContent = '已连接';
        }

        async function loadSnapshot() {
          state.snapshot = await api('/api/snapshot');
          renderTodos();
          renderEvents();
        }

        function renderTodos() {
          els.todoList.innerHTML = state.snapshot.todos.map(item => itemCard(item, false)).join('');
          bindActions();
        }

        function renderEvents() {
          els.eventList.innerHTML = state.snapshot.events.map(item => itemCard(item, true)).join('');
          bindActions();
        }

        function itemCard(item, isEvent) {
          const line = isEvent ? (item.startAtLabel || '') : (item.dueAtLabel || '未设置DDL');
          const remind = item.reminderOffsetsMinutes && item.reminderOffsetsMinutes.length ? `提醒: ${'$'}{item.reminderOffsetsMinutes.join(', ')} 分钟前` : '无提醒';
          return `
            <div class="item" style="border-left-color:${'$'}{item.groupColorHex || item.accentColorHex || '#4e87e1'}">
              <h3>${'$'}{escapeHtml(item.title)}</h3>
              <div class="meta">${'$'}{escapeHtml(item.groupName || '')} · ${'$'}{escapeHtml(line)}</div>
              <div class="meta">${'$'}{escapeHtml(item.location || item.notes || '')}</div>
              <div class="meta">${'$'}{escapeHtml(remind)} · ${'$'}{escapeHtml(item.reminderDeliveryMode || '')}</div>
              <div class="actions">
                ${'$'}{!isEvent ? `<button data-action="complete" data-id="${'$'}{item.id}">完成</button>` : ''}
                <button data-action="cancel" data-id="${'$'}{item.id}" class="danger">${'$'}{isEvent ? '删除' : '取消'}</button>
              </div>
            </div>`;
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

        function bindActions() {
          document.querySelectorAll('[data-action]').forEach(node => {
            node.onclick = async () => {
              const id = node.dataset.id;
              if (node.dataset.action === 'complete') {
                await api(`/api/items/${'$'}{id}/complete`, { method: 'POST' });
              } else {
                await api(`/api/items/${'$'}{id}/cancel`, { method: 'POST' });
              }
              await loadSnapshot();
            };
          });
        }

        document.getElementById('connect').onclick = () => connect().catch(err => els.status.textContent = err.message);
        document.getElementById('refresh').onclick = () => loadSnapshot().catch(err => els.status.textContent = err.message);
        document.querySelectorAll('[data-tab]').forEach(node => {
          node.onclick = () => {
            state.currentTab = node.dataset.tab;
            document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
            document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
            document.getElementById(`${'$'}{node.dataset.tab}-panel`).classList.add('active');
            node.classList.add('active');
          };
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
          await loadSnapshot();
        };
      """.trimIndent()
}
