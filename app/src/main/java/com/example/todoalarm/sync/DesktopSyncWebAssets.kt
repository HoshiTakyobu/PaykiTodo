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
                  <p class="sidebar-tip">输入手机设置页里显示的 4 位访问密钥后，再连接这台手机。</p>
                  <div class="connect-stack">
                    <input id="token" type="password" maxlength="4" placeholder="例如 A7K3" />
                    <button id="connect">连接手机</button>
                  </div>
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
                <div class="card-panel schedule-panel">
                  <div class="schedule-head schedule-toolbar">
                    <div>
                      <div id="event-selected-date" class="schedule-date-title">今天</div>
                      <div id="event-selected-subtitle" class="muted">可查看任意日期，并按窗口宽度展示多天日程。</div>
                    </div>
                    <div class="schedule-toolbar-actions">
                      <button id="event-prev-day" class="ghost mini">前一天</button>
                      <button id="event-next-day" class="ghost mini">后一天</button>
                      <div class="inline-picker">
                        <label for="event-anchor-date">查看起始日期</label>
                        <input id="event-anchor-date" type="date" />
                      </div>
                      <button id="apply-event-day" class="ghost mini">跳转</button>
                    </div>
                  </div>
                  <section class="all-day-section">
                    <div class="slot-title">全天</div>
                    <div id="all-day-list" class="all-day-list"></div>
                  </section>
                  <section class="schedule-board-shell">
                    <div id="hour-axis" class="hour-axis"></div>
                    <div id="board-scroll" class="board-scroll">
                      <div id="event-day-headers" class="event-day-headers"></div>
                      <div id="event-timeline" class="event-timeline"></div>
                    </div>
                  </section>
                </div>
              </section>
            </main>
          </div>

          <div id="todo-modal" class="modal-backdrop hidden">
            <div class="modal-sheet">
              <div class="modal-handle"></div>
              <div class="modal-head">
                <button class="sheet-text-button" data-close-modal="todo-modal">取消</button>
                <div class="modal-title-block">
                  <h2 id="todo-modal-title">新增待办</h2>
                  <p id="todo-modal-subtitle" class="muted">编辑 DDL、提醒、循环与提醒方式。</p>
                </div>
                <button id="create-todo" class="sheet-primary-action">创建待办</button>
              </div>
              <div class="editor-body">
              <div class="form-grid editor-grid">
                <div class="span-2"><label>标题</label><input id="todo-title" class="primary-editor-field" placeholder="标题" /></div>
                <div class="span-2"><label>备注</label><textarea id="todo-notes" placeholder="备注"></textarea></div>
                <div>
                  <label>DDL</label>
                  <div class="date-time-field">
                    <div class="date-time-row">
                      <input id="todo-due-year" class="digit-input year-input" type="text" inputmode="numeric" maxlength="4" data-maxlength="4" data-next="todo-due-month" placeholder="2026" />
                      <span class="segment-separator">/</span>
                      <input id="todo-due-month" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="todo-due-day" placeholder="05" />
                      <span class="segment-separator">/</span>
                      <input id="todo-due-day" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="todo-due-hour" placeholder="10" />
                    </div>
                    <div class="date-time-row compact-row">
                      <input id="todo-due-hour" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="todo-due-minute" placeholder="09" />
                      <span class="segment-separator">:</span>
                      <input id="todo-due-minute" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" placeholder="30" />
                    </div>
                  </div>
                </div>
                <div>
                  <label>提醒时间</label>
                  <div class="date-time-field">
                    <div class="date-time-row">
                      <input id="todo-reminder-year" class="digit-input year-input" type="text" inputmode="numeric" maxlength="4" data-maxlength="4" data-next="todo-reminder-month" placeholder="2026" />
                      <span class="segment-separator">/</span>
                      <input id="todo-reminder-month" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="todo-reminder-day" placeholder="05" />
                      <span class="segment-separator">/</span>
                      <input id="todo-reminder-day" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="todo-reminder-hour" placeholder="10" />
                    </div>
                    <div class="date-time-row compact-row">
                      <input id="todo-reminder-hour" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="todo-reminder-minute" placeholder="09" />
                      <span class="segment-separator">:</span>
                      <input id="todo-reminder-minute" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" placeholder="25" />
                    </div>
                  </div>
                </div>
                <div>
                  <label>分组</label>
                  <select id="todo-group"></select>
                </div>
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
              </div>
              <div class="modal-actions destructive-row"><button id="delete-todo" class="danger hidden">删除待办</button></div>
            </div>
          </div>

          <div id="event-modal" class="modal-backdrop hidden">
            <div class="modal-sheet wide-sheet">
              <div class="modal-handle"></div>
              <div class="modal-head">
                <button class="sheet-text-button" data-close-modal="event-modal">取消</button>
                <div class="modal-title-block">
                  <h2 id="event-modal-title">新增日程</h2>
                  <p id="event-modal-subtitle" class="muted">编辑起止时间、地点、颜色、循环与提醒。</p>
                </div>
                <button id="save-event" class="sheet-primary-action">创建日程</button>
              </div>
              <div class="editor-body">
              <div class="form-grid editor-grid">
                <div class="span-2"><label>标题</label><input id="event-title" class="primary-editor-field" placeholder="标题" /></div>
                <div><label>分组</label><select id="event-group"></select></div>
                <div><label>日程颜色</label><input id="event-color" type="color" value="#4e87e1" /></div>
                <div><label>地点</label><input id="event-location" placeholder="地点" /></div>
                <div><label>提醒方式</label><select id="event-reminder-mode"><option value="NOTIFICATION">通知栏提醒</option><option value="FULLSCREEN">全屏提醒</option></select></div>
                <div class="span-2"><label>备注</label><textarea id="event-notes" placeholder="备注"></textarea></div>
                <div>
                  <label>开始</label>
                  <div class="date-time-field">
                    <div class="date-time-row">
                      <input id="event-start-year" class="digit-input year-input" type="text" inputmode="numeric" maxlength="4" data-maxlength="4" data-next="event-start-month" placeholder="2026" />
                      <span class="segment-separator">/</span>
                      <input id="event-start-month" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="event-start-day" placeholder="05" />
                      <span class="segment-separator">/</span>
                      <input id="event-start-day" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="event-start-hour" placeholder="10" />
                    </div>
                    <div class="date-time-row compact-row">
                      <input id="event-start-hour" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="event-start-minute" placeholder="09" />
                      <span class="segment-separator">:</span>
                      <input id="event-start-minute" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" placeholder="30" />
                    </div>
                  </div>
                </div>
                <div>
                  <label>结束</label>
                  <div class="date-time-field">
                    <div class="date-time-row">
                      <input id="event-end-year" class="digit-input year-input" type="text" inputmode="numeric" maxlength="4" data-maxlength="4" data-next="event-end-month" placeholder="2026" />
                      <span class="segment-separator">/</span>
                      <input id="event-end-month" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="event-end-day" placeholder="05" />
                      <span class="segment-separator">/</span>
                      <input id="event-end-day" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="event-end-hour" placeholder="10" />
                    </div>
                    <div class="date-time-row compact-row">
                      <input id="event-end-hour" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" data-next="event-end-minute" placeholder="10" />
                      <span class="segment-separator">:</span>
                      <input id="event-end-minute" class="digit-input mini-input" type="text" inputmode="numeric" maxlength="2" data-maxlength="2" data-pad="2" placeholder="00" />
                    </div>
                  </div>
                </div>
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
                <div class="span-2 switch-row"><label><input id="event-all-day" type="checkbox" /> 全天</label><label><input id="event-ring" type="checkbox" checked /> 铃声</label><label><input id="event-vibrate" type="checkbox" checked /> 震动</label></div>
              </div>
              </div>
              <div class="modal-actions destructive-row"><button id="delete-event" class="danger hidden">删除日程</button></div>
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
        .sidebar-tip { margin: 0 0 12px; font-size: 12px; line-height: 1.55; color: rgba(255,255,255,.72); }
        .connect-stack { display: flex; flex-direction: column; gap: 12px; }
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
        .status { margin-top: 12px; font-size: 13px; line-height: 1.5; }
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
        .hidden { display: none !important; }
        .timeline-panel, .schedule-panel { padding: 18px; }
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
        .timeline-card { min-width: 0; border-radius: 22px; border: 1px solid rgba(211,220,230,.9); border-left: 5px solid var(--accent, var(--primary)); background: rgba(255,255,255,.92); box-shadow: 0 10px 26px rgba(46,64,98,.06); padding: 13px 14px 13px 16px; transition: transform .16s ease, box-shadow .16s ease, border-color .16s ease; }
        .timeline-card:hover, .all-day-card:hover, .event-card:hover { transform: translateY(-1px); box-shadow: 0 16px 32px rgba(46,64,98,.11); }
        .timeline-card-title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 5px; }
        .timeline-card-title { font-size: 18px; font-weight: 850; line-height: 1.32; word-break: break-word; }
        .pill { flex-shrink: 0; display: inline-flex; align-items: center; padding: 5px 9px; border-radius: 999px; font-size: 12px; font-weight: 800; color: #36506f; background: rgba(53,104,212,.1); }
        .timeline-card-meta, .timeline-card-notes, .all-day-card-meta, .event-card-meta, .event-card-notes { font-size: 13px; color: var(--muted); line-height: 1.48; white-space: pre-wrap; word-break: break-word; }
        .timeline-card-notes, .event-card-notes { margin-top: 5px; color: #405063; }
        .actions { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 10px; }
        .actions button { width: auto; min-width: 64px; padding: 7px 10px; border-radius: 999px; font-size: 12px; border: 1px solid transparent; }
        .actions .secondary { background: rgba(53,104,212,.1); color: #214fbb; border-color: rgba(53,104,212,.18); }
        .actions .danger { background: rgba(214,60,60,.1); color: #b52727; border-color: rgba(214,60,60,.18); }
        .actions .success { background: rgba(29,143,90,.12); color: #117549; border-color: rgba(29,143,90,.2); }
        .empty-state { padding: 16px 18px; border-radius: 18px; border: 1px dashed var(--line-strong); color: var(--muted); background: rgba(255,255,255,.58); }
        .schedule-panel { display: flex; flex-direction: column; gap: 18px; }
        .schedule-toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
        .schedule-toolbar-actions { display: flex; align-items: flex-end; gap: 10px; flex-wrap: wrap; }
        .inline-picker { display: flex; flex-direction: column; gap: 6px; min-width: 170px; }
        .inline-picker label { font-size: 12px; font-weight: 700; color: var(--muted); }
        .schedule-date-title { font-size: 22px; font-weight: 800; margin-bottom: 6px; }
        .all-day-section { border-radius: 20px; border: 1px solid rgba(211,220,230,.9); background: rgba(255,255,255,.66); padding: 14px; }
        .slot-title { font-size: 13px; font-weight: 800; color: var(--muted); margin-bottom: 10px; }
        .all-day-list { display: grid; grid-template-columns: repeat(var(--day-count, 1), minmax(0, 1fr)); gap: 8px 12px; align-items: stretch; }
        .all-day-row { display: contents; }
        .all-day-day { min-width: 0; display: flex; flex-direction: column; gap: 8px; }
        .all-day-day-label { font-size: 13px; font-weight: 800; color: #314154; padding: 0 2px; }
        .all-day-card { min-width: 0; padding: 12px 14px; border-radius: 17px; border: 1px solid rgba(211,220,230,.88); border-left: 5px solid var(--accent, var(--primary)); background: rgba(255,255,255,.93); box-shadow: 0 8px 18px rgba(39,56,88,.05); transition: transform .16s ease, box-shadow .16s ease; }
        .all-day-card.spanning { border-left-width: 5px; border-right: 1px solid rgba(211,220,230,.92); }
        .all-day-card-title, .event-card-title { font-size: 16px; font-weight: 850; line-height: 1.28; margin-bottom: 4px; }
        .all-day-empty { padding: 10px 12px; border-radius: 14px; background: rgba(98,114,133,.08); color: var(--muted); font-size: 13px; }
        .schedule-board-shell { display: grid; grid-template-columns: 84px minmax(0, 1fr); gap: 14px; align-items: start; min-width: 0; }
        .hour-axis { position: sticky; top: 126px; }
        .hour-axis-spacer { height: 58px; }
        .hour-label { height: var(--hour-height); display: flex; align-items: flex-start; justify-content: flex-end; font-size: 12px; font-weight: 700; color: var(--muted); }
        .board-scroll { min-width: 0; overflow-y: auto; overflow-x: hidden; border-radius: 22px; border: 1px solid rgba(211,220,230,.92); background: rgba(255,255,255,.72); }
        .event-day-headers { position: sticky; top: 0; z-index: 3; display: grid; grid-template-columns: repeat(var(--day-count, 1), minmax(0, 1fr)); background: rgba(255,255,255,.94); border-bottom: 1px solid rgba(211,220,230,.92); }
        .event-day-header { min-width: 0; padding: 12px 10px; text-align: left; background: transparent; border: 0; border-left: 1px solid rgba(185,200,218,.9); cursor: pointer; }
        .event-day-header:last-child { border-right: 1px solid rgba(185,200,218,.9); }
        .event-day-header.today { background: rgba(53,104,212,.06); }
        .event-day-header.selected { background: rgba(53,104,212,.12); }
        .event-day-header-date { font-size: 14px; font-weight: 800; color: #243446; }
        .event-day-header-meta { margin-top: 4px; font-size: 12px; color: var(--muted); }
        .event-timeline { position: relative; width: 100%; min-width: 0; height: calc(var(--hour-height) * 24); display: grid; grid-template-columns: repeat(var(--day-count, 1), minmax(0, 1fr)); }
        .event-day-column { position: relative; min-width: 0; height: 100%; border-left: 1px solid rgba(185,200,218,.9); cursor: crosshair; }
        .event-day-column:last-child { border-right: 1px solid rgba(185,200,218,.9); }
        .hour-row { position: absolute; left: 0; right: 0; height: var(--hour-height); border-top: 1px solid rgba(188,201,217,.9); }
        .half-row { position: absolute; left: 0; right: 0; border-top: 1px dashed rgba(188,201,217,.4); }
        .event-card { position: absolute; left: 7px; right: 7px; min-height: 34px; border-radius: 17px; padding: 8px 10px 8px 12px; border: 1px solid rgba(211,220,230,.9); border-left: 5px solid var(--accent, var(--primary)); background: rgba(255,255,255,.96); box-shadow: 0 10px 22px rgba(40,57,88,.08); overflow: hidden; transition: transform .16s ease, box-shadow .16s ease; }
        .event-card .event-card-meta, .event-card .event-card-notes { font-size: 12px; line-height: 1.35; }
        .event-card .actions { margin-top: 6px; gap: 5px; }
        .event-card .actions button { min-width: 42px; padding: 4px 8px; font-size: 11px; }
        .current-line { position: absolute; left: 0; right: 0; height: 2px; background: rgba(229,57,53,.88); }
        .current-line.past { background: rgba(229,57,53,.42); }
        .hour-current-chip { position: absolute; left: 0; transform: translateY(-12px); padding: 3px 6px; border-radius: 10px; background: #e53935; color: #fff; font-size: 10px; font-weight: 800; line-height: 1; white-space: nowrap; }
        .modal-backdrop { position: fixed; inset: 0; background: rgba(12,20,31,.46); display: flex; align-items: center; justify-content: center; padding: 28px; z-index: 100; backdrop-filter: blur(10px); }
        .modal-backdrop.hidden { display: none; }
        .modal-sheet { width: min(820px, 100%); max-height: min(86vh, 860px); overflow: hidden; display: flex; flex-direction: column; border-radius: 30px; background: linear-gradient(180deg, rgba(255,255,255,.99), rgba(247,250,255,.98)); border: 1px solid rgba(255,255,255,.76); box-shadow: 0 30px 86px rgba(15,24,39,.28); padding: 10px 18px 18px; }
        .modal-backdrop:not(.hidden) .modal-sheet { animation: sheet-enter .18s ease-out; }
        @keyframes sheet-enter { from { opacity: .72; transform: translateY(18px) scale(.985); } to { opacity: 1; transform: translateY(0) scale(1); } }
        .wide-sheet { width: min(960px, 100%); }
        .modal-handle { width: 54px; height: 5px; border-radius: 999px; background: rgba(98,114,133,.28); margin: 4px auto 10px; flex-shrink: 0; }
        .modal-head { position: sticky; top: 0; z-index: 2; display: grid; grid-template-columns: 96px minmax(0, 1fr) 120px; align-items: center; gap: 12px; padding: 8px 0 14px; background: linear-gradient(180deg, rgba(255,255,255,.99), rgba(255,255,255,.94)); border-bottom: 1px solid rgba(215,224,234,.72); }
        .modal-title-block { text-align: center; min-width: 0; }
        .modal-title-block h2 { font-size: 21px; font-weight: 900; letter-spacing: -.02em; }
        .modal-title-block .muted { margin-top: 4px; font-size: 12px; line-height: 1.35; }
        .sheet-text-button, .sheet-primary-action { width: auto !important; min-width: 0; border-radius: 999px !important; padding: 9px 13px !important; font-size: 14px; }
        .sheet-text-button { background: rgba(98,114,133,.1) !important; color: #405063 !important; border: 1px solid rgba(98,114,133,.14) !important; }
        .sheet-primary-action { background: linear-gradient(135deg, #3568d4, #4e87e1) !important; color: #fff !important; border: 0 !important; box-shadow: 0 8px 18px rgba(53,104,212,.22); }
        .editor-body { overflow: auto; padding: 16px 2px 2px; }
        .modal-actions { display: flex; align-items: center; gap: 12px; margin-top: 12px; }
        .destructive-row { justify-content: flex-start; border-top: 1px solid rgba(215,224,234,.72); padding-top: 12px; }
        .destructive-row button { width: auto !important; min-width: 112px; border-radius: 999px; padding: 10px 14px; }
        .destructive-row .danger { background: rgba(214,60,60,.1) !important; color: #b52727 !important; border: 1px solid rgba(214,60,60,.2) !important; }
        .form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
        .editor-grid > div { min-width: 0; border-radius: 20px; background: rgba(255,255,255,.82); border: 1px solid rgba(215,224,234,.82); padding: 12px; box-shadow: 0 8px 20px rgba(46,64,98,.045); }
        .span-2 { grid-column: 1 / -1; }
        .modal-sheet label { display: block; margin-bottom: 8px; font-size: 14px; font-weight: 900; color: #243446; }
        .modal-sheet input:focus, .modal-sheet textarea:focus, .modal-sheet select:focus { outline: 2px solid rgba(53,104,212,.2); border-color: rgba(53,104,212,.42); box-shadow: 0 0 0 4px rgba(53,104,212,.08); }
        .primary-editor-field { font-size: 20px !important; font-weight: 850; padding: 14px 16px !important; }
        .date-time-field { display: flex; flex-direction: column; gap: 8px; }
        .date-time-row { display: flex; align-items: center; gap: 7px; flex-wrap: nowrap; }
        .compact-row { justify-content: flex-start; }
        .digit-input { text-align: center; letter-spacing: .02em; font-variant-numeric: tabular-nums; padding: 10px 8px !important; }
        .year-input { width: 88px !important; min-width: 88px; }
        .mini-input { width: 58px !important; min-width: 58px; }
        .segment-separator { color: var(--muted); font-weight: 800; }
        .modal-sheet textarea { min-height: 82px; resize: vertical; }
        .switch-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
        .switch-row label { display: inline-flex; align-items: center; gap: 8px; margin: 0; padding: 9px 12px; border-radius: 999px; background: rgba(53,104,212,.08); color: #2f4c78; }
        .switch-row input { width: auto; margin: 0; }
        @media (max-width: 1080px) {
          .shell { grid-template-columns: 1fr; }
          .sidebar { position: static; height: auto; }
          .topbar, .hour-axis { position: static; }
        }
        @media (max-width: 840px) {
          .toolbar-row, .form-grid { display: flex; flex-wrap: wrap; }
          .schedule-board-shell, .timeline-item { grid-template-columns: 1fr; }
          .schedule-toolbar, .schedule-toolbar-actions { align-items: stretch; }
          .date-time-row { flex-wrap: wrap; }
          .modal-backdrop { align-items: flex-end; padding: 12px; }
          .modal-sheet { width: 100%; max-height: 92vh; border-radius: 28px 28px 18px 18px; padding: 8px 14px 14px; }
          .modal-head { grid-template-columns: 68px minmax(0, 1fr) 88px; gap: 8px; }
          .modal-title-block h2 { font-size: 18px; }
          .modal-title-block .muted { display: none; }
          .sheet-text-button, .sheet-primary-action { padding: 8px 10px !important; font-size: 13px; }
          .editor-grid > div { flex: 1 1 100%; }
          .year-input { width: 82px !important; min-width: 82px; }
          .mini-input { width: 54px !important; min-width: 54px; }
        }
      """.trimIndent()

    fun appJs(): String = """
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
          allDayList: document.getElementById('all-day-list'),
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

        function renderAllDayCard(item, span = null) {
          const accent = item.groupColorHex || item.accentColorHex || '#4e87e1';
          const meta = [item.groupName || '未分组', item.location || '', item.isRecurring ? '循环' : ''].filter(Boolean).join(' · ');
          const style = '--accent:' + accent + (span ? ';grid-column:' + span.start + ' / span ' + span.length : '');
          return ''
            + '<article class="all-day-card' + (span ? ' spanning' : '') + '" data-event-id="' + item.id + '" style="' + style + '">'
            +   '<div class="all-day-card-title">' + escapeHtml(item.title) + '</div>'
            +   '<div class="all-day-card-meta">' + escapeHtml(meta || '全天日程') + '</div>'
            +   (item.notes ? '<div class="all-day-card-meta">' + escapeHtml(item.notes) + '</div>' : '')
            +   '<div class="actions"><button data-action="edit-event" data-id="' + item.id + '" class="secondary">编辑</button><button data-action="delete" data-id="' + item.id + '" class="danger">删除</button></div>'
            + '</article>';
        }

        function allDaySpanForVisibleKeys(item, visibleKeys) {
          const visibleStart = dayStartMillis(visibleKeys[0]);
          const visibleEnd = dayStartMillis(visibleKeys[visibleKeys.length - 1]) + 24 * 60 * 60 * 1000;
          const start = Math.max(eventStart(item), visibleStart);
          const end = Math.min(eventEnd(item), visibleEnd);
          if (end <= start) return null;
          const startIndex = Math.max(0, Math.floor((start - visibleStart) / (24 * 60 * 60 * 1000)));
          const inclusiveEndIndex = Math.min(visibleKeys.length - 1, Math.floor((end - 1 - visibleStart) / (24 * 60 * 60 * 1000)));
          return { start: startIndex + 1, length: Math.max(1, inclusiveEndIndex - startIndex + 1) };
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

        function renderAllDayDay(key, items) {
          return ''
            + '<div class="all-day-day">'
            +   '<div class="all-day-day-label">' + escapeHtml(formatCompactDateLabel(key) + ' ' + formatWeekday(dateFromKey(key))) + '</div>'
            +   (items.length ? items.map(renderAllDayCard).join('') : '<div class="all-day-empty">本日没有全天日程。</div>')
            + '</div>';
        }

        function renderAllDayBoard(visibleKeys, allDayEvents) {
          const header = '<div class="all-day-row">' + visibleKeys.map(key =>
            '<div class="all-day-day-label">' + escapeHtml(formatCompactDateLabel(key) + ' ' + formatWeekday(dateFromKey(key))) + '</div>'
          ).join('') + '</div>';
          if (!allDayEvents.length) {
            return header + '<div class="all-day-empty" style="grid-column:1 / span ' + Math.max(1, visibleKeys.length) + '">当前可见日期没有全天日程。</div>';
          }
          const cards = allDayEvents.map(item => {
            const span = allDaySpanForVisibleKeys(item, visibleKeys);
            return span ? renderAllDayCard(item, span) : '';
          }).join('');
          return header + cards;
        }

        function renderEventDayColumn(key, timed) {
          return ''
            + '<div class="event-day-column" data-column-day="' + key + '">'
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
          const accent = item.accentColorHex || item.groupColorHex || '#4e87e1';
          const meta = [item.groupName || '未分组', item.location || '', item.isRecurring ? '循环' : ''].filter(Boolean).join(' · ');
          return ''
            + '<article class="event-card" data-event-id="' + item.id + '" style="--accent:' + accent + ';top:' + segment.top + 'px;height:' + segment.height + 'px">'
            +   '<div class="event-card-title">' + escapeHtml(item.title) + '</div>'
            +   '<div class="event-card-meta">' + escapeHtml(segment.startLabel) + ' - ' + escapeHtml(segment.endLabel) + '</div>'
            +   '<div class="event-card-meta">' + escapeHtml(meta || '定时日程') + '</div>'
            +   (item.notes ? '<div class="event-card-notes">' + escapeHtml(item.notes) + '</div>' : '')
            +   '<div class="actions"><button data-action="edit-event" data-id="' + item.id + '" class="secondary">编辑</button><button data-action="delete" data-id="' + item.id + '" class="danger">删除</button></div>'
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
              allDay: items.filter(item => item.allDay),
              timed: items.filter(item => !item.allDay).map(item => buildEventSegment(item, key))
            };
          });
          const totalAllDay = visibleDays.reduce((sum, day) => sum + day.allDay.length, 0);
          const totalTimed = visibleDays.reduce((sum, day) => sum + day.timed.length, 0);
          const visibleAllDayEvents = events
            .filter(item => item.allDay && allDaySpanForVisibleKeys(item, visibleKeys))
            .sort((a, b) => eventStart(a) - eventStart(b));
          els.eventDayHeaders.style.setProperty('--day-count', String(visibleKeys.length || 1));
          els.eventTimeline.style.setProperty('--day-count', String(visibleKeys.length || 1));
          els.allDayList.style.setProperty('--day-count', String(visibleKeys.length || 1));
          els.eventDayHeaders.innerHTML = visibleDays.map(day => renderEventDayHeader(day.key, day.items)).join('');
          if (els.eventAnchorDate) els.eventAnchorDate.value = state.selectedEventDay;
          els.eventSelectedDate.textContent = renderVisibleRangeTitle(visibleKeys);
          els.eventSelectedSubtitle.textContent = '起始日：' + formatCompactDateLabel(state.selectedEventDay) + ' · 连续 ' + visibleKeys.length + ' 天 · 全天 ' + visibleAllDayEvents.length + ' 项，定时 ' + totalTimed + ' 项';
          els.allDayList.innerHTML = renderAllDayBoard(visibleKeys, visibleAllDayEvents);
          els.hourAxis.innerHTML = renderHourAxis();
          els.eventTimeline.innerHTML = visibleDays.map(day => renderEventDayColumn(day.key, day.timed)).join('');
          if (els.boardScroll) {
            if (visibleKeys.includes(dayKey(new Date()))) {
              const nowDate = new Date();
              els.boardScroll.scrollTop = Math.max(0, EVENT_HEADER_HEIGHT + ((nowDate.getHours() * 60 + nowDate.getMinutes()) / 60 * HOUR_HEIGHT) - 180);
            } else {
              const firstTimed = visibleDays.flatMap(day => day.timed).sort((a, b) => a.top - b.top)[0];
              els.boardScroll.scrollTop = firstTimed ? Math.max(0, EVENT_HEADER_HEIGHT + firstTimed.top - 80) : 0;
            }
          }
          document.querySelectorAll('[data-event-id]').forEach(node => {
            node.onclick = () => {
              const id = Number(node.dataset.eventId);
              const eventItem = (state.snapshot?.events || []).find(item => item.id === id);
              if (eventItem) openEventEditor(eventItem);
            };
          });
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
          state.editingTodoId = null;
          fillGroupSelect('todo-group');
          document.getElementById('todo-modal-title').textContent = '新增待办';
          document.getElementById('todo-modal-subtitle').textContent = '编辑 DDL、提醒、循环与提醒方式。';
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
        }

        function openTodoEditor(item) {
          state.editingTodoId = item.id;
          fillGroupSelect('todo-group', item.groupId);
          document.getElementById('todo-modal-title').textContent = '编辑待办';
          document.getElementById('todo-modal-subtitle').textContent = '可修改标题、备注、DDL、提醒、分组、循环、铃声和震动。';
          document.getElementById('create-todo').textContent = '保存修改';
          document.getElementById('delete-todo').classList.remove('hidden');
          document.getElementById('todo-title').value = item.title || '';
          document.getElementById('todo-notes').value = item.notes || '';
          writeDateTimeValue('todo-due', item.hasDueDate ? formatDateTimeLocalValue(item.dueAtMillis) : '');
          const reminderOffsets = item.reminderOffsetsMinutes || [];
          const reminderMillis = reminderOffsets.length && item.hasDueDate
            ? item.dueAtMillis - reminderOffsets[0] * 60 * 1000
            : item.reminderAtMillis;
          writeDateTimeValue('todo-reminder', reminderMillis ? formatDateTimeLocalValue(reminderMillis) : '');
          document.getElementById('todo-recurrence-type').value = recurrenceTypeValue(item);
          document.getElementById('todo-recurrence-end').value = item.recurrenceEndDate || '';
          document.getElementById('todo-weekdays').value = (item.recurrenceWeekdays || []).join(',');
          document.getElementById('todo-ring').checked = item.ringEnabled !== false;
          document.getElementById('todo-vibrate').checked = item.vibrateEnabled !== false;
          openModal('todo-modal');
        }

        function clearEventForm() {
          state.editingEventId = null;
          state.pendingEventSeed = null;
          fillGroupSelect('event-group');
          document.getElementById('event-modal-title').textContent = '新增日程';
          document.getElementById('event-modal-subtitle').textContent = '编辑起止时间、地点、颜色、循环与提醒。';
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

        function openEventEditor(item) {
          state.editingEventId = item.id;
          state.pendingEventSeed = null;
          fillGroupSelect('event-group', item.groupId);
          document.getElementById('event-modal-title').textContent = '编辑日程';
          document.getElementById('event-modal-subtitle').textContent = '可修改起止时间、地点、颜色、循环与提醒。';
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
          document.getElementById('event-weekdays').value = (item.recurrenceWeekdays || []).join(',');
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

        function bindActions() {
          document.querySelectorAll('[data-action]').forEach(node => {
            node.onclick = async event => {
              event.stopPropagation();
              const id = node.dataset.id;
              if (node.dataset.action === 'edit-todo') {
                const todoItem = (state.snapshot?.todos || []).find(item => Number(item.id) === Number(id));
                if (todoItem) openTodoEditor(todoItem);
                return;
              } else if (node.dataset.action === 'edit-event') {
                const eventItem = (state.snapshot?.events || []).find(item => Number(item.id) === Number(id));
                if (eventItem) openEventEditor(eventItem);
                return;
              } else if (node.dataset.action === 'complete') {
                await api(`/api/items/${'$'}{id}/complete`, { method: 'POST' });
              } else if (node.dataset.action === 'cancel') {
                await api(`/api/items/${'$'}{id}/cancel`, { method: 'POST' });
              } else {
                if (!confirm('确定删除这个项目吗？删除后无法恢复。')) return;
                await api(`/api/items/${'$'}{id}`, { method: 'DELETE' });
              }
              await loadSnapshot();
            };
          });
        }

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
          const dueAt = readDateTimeValue('todo-due');
          const reminderAt = readDateTimeValue('todo-reminder');
          const payload = {
            title: document.getElementById('todo-title').value,
            notes: document.getElementById('todo-notes').value,
            dueAt: dueAt,
            reminderAt: reminderAt,
            groupId: Number(document.getElementById('todo-group').value || 0),
            ringEnabled: document.getElementById('todo-ring').checked,
            vibrateEnabled: document.getElementById('todo-vibrate').checked,
            recurrence: recurrencePayload(
              document.getElementById('todo-recurrence-type').value,
              document.getElementById('todo-recurrence-end').value,
              document.getElementById('todo-weekdays').value
            )
          };
          if (state.editingTodoId) {
            await api(`/api/todos/${'$'}{state.editingTodoId}`, {
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
          if (!confirm('确定删除这个待办吗？删除后无法恢复。')) return;
          await api(`/api/items/${'$'}{state.editingTodoId}`, { method: 'DELETE' });
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
            await api(`/api/events/${'$'}{state.editingEventId}`, {
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
          if (!confirm('确定删除这个日程吗？删除后无法恢复。')) return;
          await api(`/api/items/${'$'}{state.editingEventId}`, { method: 'DELETE' });
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
      """.trimIndent()
}
