'use strict';

const DAY_JP = ['日', '月', '火', '水', '木', '金', '土'];
let schedules = [];

function buildDowFilters() {
    const el = document.getElementById('dow-filters');
    el.innerHTML = DAY_JP.map((d, i) =>
        `<div class="form-check form-check-inline">
          <input class="form-check-input filter-dow" type="checkbox" id="filter-dow-${i}" checked>
          <label class="form-check-label" for="filter-dow-${i}">${d}曜</label>
        </div>`
    ).join('') +
    `<div class="btn-group">
      <button class="btn btn-outline-dark btn-sm" onclick="setAllDow(false)">全部OFF</button>
      <button class="btn btn-outline-dark btn-sm" onclick="setAllDow(true)">全部ON</button>
    </div>`;
}
function buildWeekFilters() {
    const el = document.getElementById('week-filters');
    el.innerHTML = [0,1,2,3,4].map(i =>
        `<div class="form-check form-check-inline">
          <input class="form-check-input filter-week" type="checkbox" id="filter-week-${i}" checked>
          <label class="form-check-label" for="filter-week-${i}">${i+1}週目</label>
        </div>`
    ).join('') +
    `<div class="btn-group">
      <button class="btn btn-outline-dark btn-sm" onclick="setAllWeek(false)">全部OFF</button>
      <button class="btn btn-outline-dark btn-sm" onclick="setAllWeek(true)">全部ON</button>
    </div>`;
}
function setAllDow(val) { document.querySelectorAll('.filter-dow').forEach(el => el.checked = val); renderTable(); }
function setAllWeek(val) { document.querySelectorAll('.filter-week').forEach(el => el.checked = val); renderTable(); }

function getFilter() {
    return {
        am:        document.getElementById('filter-am').checked,
        pm:        document.getElementById('filter-pm').checked,
        week:      [0,1,2,3,4].map(i => { const el = document.getElementById('filter-week-'+i); return el ? el.checked : true; }),
        dayOfWeek: DAY_JP.map((_, i) => { const el = document.getElementById('filter-dow-'+i); return el ? el.checked : true; }),
    };
}
function passesFilter(s, f) {
    if (!f.week[s.week]) return false;
    if (!f.dayOfWeek[s.day_of_week]) return false;
    return true;
}
function toDateStr(s) {
    return '第' + (s.week + 1) + DAY_JP[s.day_of_week] + '曜';
}
function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }

function renderTable() {
    const f = getFilter();
    const tbl = document.getElementById('schedule-table');
    let html = '';
    schedules.forEach(s => {
        if (!passesFilter(s, f)) return;
        const works = s.works;
        html += `<thead><tr class="bg-dark bg-gradient text-light">
          <th class="fs-5" style="width:7em">${toDateStr(s)}
            <i class="fa fa-print" style="cursor:pointer" onclick="openReport(${s.id})"></i>
          </th>
          ${works.map(w => `<th class="fs-6">${w.name}</th>`).join('')}
        </tr></thead><tbody>`;
        if (f.am) {
            html += `<tr>
              <th class="table-secondary"><div class="fs-6">AM</div></th>
              ${works.map((w, wi) => {
                  const task = w.tasks[0];
                  return `<td ondblclick="editCell(event)" data-sid="${s.id}" data-widx="${wi}" data-tidx="0">
                    <pre class="cut cell-text">${escHtml(task.taskContents)}</pre>
                  </td>`;
              }).join('')}
            </tr>`;
        }
        if (f.pm) {
            html += `<tr>
              <th class="table-secondary"><div class="fs-6">PM</div></th>
              ${works.map((w, wi) => {
                  const task = w.tasks[1];
                  return `<td ondblclick="editCell(event)" data-sid="${s.id}" data-widx="${wi}" data-tidx="1">
                    <pre class="cut cell-text">${escHtml(task.taskContents)}</pre>
                  </td>`;
              }).join('')}
            </tr>`;
        }
        html += '</tbody>';
    });
    tbl.innerHTML = html;
}

function editCell(e) {
    const td = e.currentTarget;
    const pre = td.querySelector('pre.cell-text');
    if (!pre) return;
    const sid = parseInt(td.dataset.sid);
    const wi  = parseInt(td.dataset.widx);
    const ti  = parseInt(td.dataset.tidx);
    const s = schedules.find(x => x.id === sid);
    const task = s.works[wi].tasks[ti];

    const ta = document.createElement('textarea');
    ta.className = 'form-control';
    ta.rows = 10;
    ta.value = task.taskContents;
    pre.replaceWith(ta);
    ta.focus();
    ta.addEventListener('blur', () => {
        task.taskContents = ta.value;
        const newPre = document.createElement('pre');
        newPre.className = 'cut cell-text';
        newPre.textContent = task.taskContents;
        ta.replaceWith(newPre);
        saveSchedule(sid);
    });
}

async function saveSchedule(sid) {
    const s = schedules.find(x => x.id === sid);
    await fetch('/regular_schedules/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(s)
    }).catch(() => alert('更新に失敗しました'));
}

function openReport(id) { window.open('/dailyReportSample?scheduleId=' + id); }

async function createMonthlySchedules() {
    const fromDate = document.getElementById('fromDate').value;
    if (!fromDate) { alert('年月を選択してください'); return; }

    const countRes = await fetch(`/schedule/${BOARD_ID}/${fromDate}/countByMonth`);
    const countData = await countRes.json();
    if (countData.count !== 0) {
        if (!window.confirm(fromDate + 'のスケジュールは作成済みです。上書きで作成しますか？\n(上書きすると、元のスケジュール情報は復元できません)')) return;
    }

    await fetch(`/schedule/${BOARD_ID}/${fromDate}/createByMonth`);
    if (window.confirm(fromDate + 'の月別スケジュールを作成しました。\n作成したスケジュールへ移動しますか？')) {
        location.href = `/schedule?boardId=${BOARD_ID}&targetMonth=${fromDate}`;
    }
}

async function fetchSchedules() {
    const res = await fetch(`/regular_schedules/list/${BOARD_ID}`);
    schedules = await res.json();
    schedules.forEach(s => { s.id = Number(s.id); });
    renderTable();
}

document.addEventListener('DOMContentLoaded', () => {
    buildDowFilters();
    buildWeekFilters();

    // 翌月をデフォルト設定
    const next = new Date();
    next.setMonth(next.getMonth() + 1);
    const mm = String(next.getMonth() + 1).padStart(2, '0');
    document.getElementById('fromDate').value = `${next.getFullYear()}-${mm}`;

    ['filter-am','filter-pm'].forEach(id => {
        document.getElementById(id).addEventListener('change', renderTable);
    });
    document.getElementById('dow-filters').addEventListener('change', renderTable);
    document.getElementById('week-filters').addEventListener('change', renderTable);
    document.getElementById('createBtn').addEventListener('click', createMonthlySchedules);

    fetchSchedules();
});
