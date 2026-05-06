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
            <aside class="panel left">
              <h1>PaykiTodo</h1>
              <p class="muted">电脑端同步控制台</p>
              <label>访问密钥</label>
              <input id="token" type="password" placeholder="输入手机里显示的密钥" />
              <button id="connect">连接手机</button>
              <div id="status" class="muted">尚未连接</div>
              <hr />
              <button class="ghost" data-tab="todos">待办</button>
              <button class="ghost" data-tab="events">日程</button>
            </aside>
            <main class="panel main">
              <section class="toolbar">
                <button id="refresh">刷新</button>
              </section>
              <section id="todos-panel" class="tab active">
                <div class="grid two">
                  <div class="card">
                    <h2>新增待办</h2>
                    <input id="todo-title" placeholder="标题" />
                    <textarea id="todo-notes" placeholder="备注"></textarea>
                    <input id="todo-due" type="datetime-local" />
                    <input id="todo-reminder" type="datetime-local" />
                    <button id="create-todo">创建待办</button>
                  </div>
                  <div class="card">
                    <h2>待办列表</h2>
                    <div id="todo-list"></div>
                  </div>
                </div>
              </section>
              <section id="events-panel" class="tab">
                <div class="grid two">
                  <div class="card">
                    <h2>新增日程</h2>
                    <input id="event-title" placeholder="标题" />
                    <input id="event-location" placeholder="地点" />
                    <textarea id="event-notes" placeholder="备注"></textarea>
                    <label>开始</label>
                    <input id="event-start" type="datetime-local" />
                    <label>结束</label>
                    <input id="event-end" type="datetime-local" />
                    <button id="create-event">创建日程</button>
                  </div>
                  <div class="card">
                    <h2>日程列表</h2>
                    <div id="event-list"></div>
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
          --bg: #eef3f7;
          --panel: rgba(255,255,255,.88);
          --text: #18212d;
          --muted: #617082;
          --line: #d4dde7;
          --primary: #3568d4;
        }
        * { box-sizing: border-box; }
        body {
          margin: 0;
          font-family: "Microsoft YaHei UI", "PingFang SC", sans-serif;
          background: linear-gradient(135deg, #dbeafe, #eef3f7 45%, #d1fae5);
          color: var(--text);
        }
        .shell { display: grid; grid-template-columns: 320px 1fr; min-height: 100vh; }
        .panel { padding: 24px; }
        .left { background: rgba(18,28,45,.92); color: white; }
        .left .muted { color: rgba(255,255,255,.72); }
        .main { background: transparent; }
        .toolbar { display: flex; justify-content: flex-end; margin-bottom: 16px; }
        .grid.two { display: grid; grid-template-columns: 380px 1fr; gap: 16px; }
        .card {
          background: var(--panel);
          border: 1px solid rgba(255,255,255,.45);
          backdrop-filter: blur(18px);
          border-radius: 22px;
          padding: 18px;
          box-shadow: 0 14px 42px rgba(41,55,87,.08);
        }
        .tab { display: none; }
        .tab.active { display: block; }
        input, textarea, button {
          width: 100%;
          margin-top: 10px;
          border-radius: 14px;
          border: 1px solid var(--line);
          padding: 12px 14px;
          font: inherit;
        }
        textarea { min-height: 88px; resize: vertical; }
        button {
          background: var(--primary);
          color: white;
          border: none;
          cursor: pointer;
          font-weight: 700;
        }
        button.ghost {
          background: transparent;
          border: 1px solid rgba(255,255,255,.22);
          margin-bottom: 10px;
        }
        .item {
          border: 1px solid var(--line);
          border-left: 6px solid #4e87e1;
          border-radius: 18px;
          padding: 14px;
          margin-bottom: 12px;
          background: rgba(255,255,255,.78);
        }
        .item h3 { margin: 0 0 6px; font-size: 18px; }
        .meta { color: var(--muted); font-size: 13px; margin-top: 4px; }
        .actions { display: flex; gap: 8px; margin-top: 12px; }
        .actions button { width: auto; padding: 8px 12px; border-radius: 12px; }
        .actions .danger { background: #dc2626; }
        @media (max-width: 1080px) {
          .shell, .grid.two { grid-template-columns: 1fr; }
        }
      """.trimIndent()

    fun appJs(): String = """
        const state = { token: '', snapshot: null };

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
          return `
            <div class="item" style="border-left-color:${'$'}{item.groupColorHex || item.accentColorHex || '#4e87e1'}">
              <h3>${'$'}{escapeHtml(item.title)}</h3>
              <div class="meta">${'$'}{escapeHtml(item.groupName || '')} · ${'$'}{escapeHtml(line)}</div>
              <div class="meta">${'$'}{escapeHtml(item.location || item.notes || '')}</div>
              <div class="actions">
                ${'$'}{!isEvent ? `<button data-action="complete" data-id="${'$'}{item.id}">完成</button>` : ''}
                <button data-action="cancel" data-id="${'$'}{item.id}">${'$'}{isEvent ? '删除' : '取消'}</button>
              </div>
            </div>`;
        }

        function escapeHtml(text) {
          return String(text || '').replace(/[&<>\"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[ch]));
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
            document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
            document.getElementById(`${'$'}{node.dataset.tab}-panel`).classList.add('active');
          };
        });

        document.getElementById('create-todo').onclick = async () => {
          await api('/api/todos', {
            method: 'POST',
            body: JSON.stringify({
              title: document.getElementById('todo-title').value,
              notes: document.getElementById('todo-notes').value,
              dueAt: document.getElementById('todo-due').value || null,
              reminderAt: document.getElementById('todo-reminder').value || null
            })
          });
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
              reminderOffsetsMinutes: [5],
              ringEnabled: true,
              vibrateEnabled: true,
              reminderDeliveryMode: 'NOTIFICATION'
            })
          });
          await loadSnapshot();
        };
      """.trimIndent()
}
