'use strict';

const DAY_JP = ['日', '月', '火', '水', '木', '金', '土'];
let schedules = [];
let dragInfo = { isStock: false, row: null, amOrPm: null, staffs: null, staff: null };
const today = (() => { const n = new Date(); return new Date(n.getFullYear(), n.getMonth(), n.getDate()); })();

// --- Filter helpers ---
function buildDowFilters() {
    const el = document.getElementById('dow-filters');
    el.innerHTML = DAY_JP.map((d, i) =>
        `<div class="form-check form-check-inline">
          <input class="form-check-input filter-dow" type="checkbox" id="filter-dow-${i}" checked>
          <label class="form-check-label" for="filter-dow-${i}">${d}曜</label>
        </div>`
    ).join('') +
    `<div class="btn-group">
      <button class="btn btn-outline-success btn-sm" onclick="setAllDow(false)">全部OFF</button>
      <button class="btn btn-outline-success btn-sm" onclick="setAllDow(true)">全部ON</button>
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
      <button class="btn btn-outline-success btn-sm" onclick="setAllWeek(false)">全部OFF</button>
      <button class="btn btn-outline-success btn-sm" onclick="setAllWeek(true)">全部ON</button>
    </div>`;
}
function setAllDow(val) { document.querySelectorAll('.filter-dow').forEach(el => el.checked = val); renderTable(); }
function setAllWeek(val) { document.querySelectorAll('.filter-week').forEach(el => el.checked = val); renderTable(); }

function getFilter() {
    return {
        showPastDay: document.getElementById('filter-past-day').checked,
        showDetail:  document.getElementById('filter-show-detail').checked,
        am:          document.getElementById('filter-am').checked,
        pm:          document.getElementById('filter-pm').checked,
        week:        [0,1,2,3,4].map(i => { const el = document.getElementById('filter-week-'+i); return el ? el.checked : true; }),
        dayOfWeek:   DAY_JP.map((_, i) => { const el = document.getElementById('filter-dow-'+i); return el ? el.checked : true; }),
    };
}
function passesFilter(s, f) {
    if (!f.showPastDay && today.getTime() > new Date(s.schedule_date).getTime()) return false;
    if (!f.week[s.week]) return false;
    if (!f.dayOfWeek[s.day_of_week]) return false;
    return true;
}
function toDateStr(s) {
    const d = new Date(s.schedule_date);
    return (d.getUTCMonth()+1) + '/' + d.getUTCDate() + '(' + DAY_JP[s.day_of_week] + ')';
}

// --- Render ---
function renderTable() {
    const f = getFilter();
    const tbl = document.getElementById('schedule-table');
    let html = '';
    schedules.forEach(s => {
        if (!passesFilter(s, f)) return;
        const works = s.works;
        html += `<thead><tr class="bg-success bg-gradient text-light">
          <th class="fs-5" style="width:7em">${toDateStr(s)}
            <i class="fa fa-print" style="cursor:pointer" onclick="openDailyReport(${s.id})"></i>
          </th>
          ${works.map(w => `<th class="fs-6">${w.name}</th>`).join('')}
        </tr></thead><tbody>`;
        if (f.am) {
            const amStaffs = s.staffs[0] || [];
            html += `<tr>
              <th class="table-secondary" data-sid="${s.id}" data-slot="AM" data-widx="pool" ondragover="event.preventDefault()" ondrop="dropStaff(event,'${s.id}','AM','pool')">
                <div class="fs-6">AM</div>
                <div>${amStaffs.map(st => staffBadge(s.id,'AM','pool',st)).join('')}</div>
              </th>
              ${works.map((w, wi) => {
                  const task = w.tasks[0];
                  const badgeHtml = (task.staffs||[]).map(st => staffBadge(s.id,'AM',wi,st)).join('');
                  return `<td ondblclick="editCell(event)" data-sid="${s.id}" data-widx="${wi}" data-tidx="0"
                              ondragover="event.preventDefault()" ondrop="dropStaff(event,'${s.id}','AM',${wi})">
                    <div>${badgeHtml}</div>
                    <pre class="cut cell-text">${escHtml(task.taskContents)}</pre>
                  </td>`;
              }).join('')}
            </tr>`;
        }
        if (f.pm) {
            const pmStaffs = s.staffs[1] || [];
            html += `<tr>
              <th class="table-secondary" data-sid="${s.id}" data-slot="PM" data-widx="pool" ondragover="event.preventDefault()" ondrop="dropStaff(event,'${s.id}','PM','pool')">
                <div class="fs-6">PM</div>
                <div>${pmStaffs.map(st => staffBadge(s.id,'PM','pool',st)).join('')}</div>
              </th>
              ${works.map((w, wi) => {
                  const task = w.tasks[1];
                  const badgeHtml = (task.staffs||[]).map(st => staffBadge(s.id,'PM',wi,st)).join('');
                  return `<td ondblclick="editCell(event)" data-sid="${s.id}" data-widx="${wi}" data-tidx="1"
                              ondragover="event.preventDefault()" ondrop="dropStaff(event,'${s.id}','PM',${wi})">
                    <div>${badgeHtml}</div>
                    <pre class="cut cell-text">${escHtml(task.taskContents)}</pre>
                  </td>`;
              }).join('')}
            </tr>`;
        }
        html += '</tbody>';
    });
    tbl.innerHTML = html;
}

function staffBadge(sid, slot, widx, staff) {
    return `<span class="badge rounded-pill bg-warning text-dark fs-6 m-1" draggable="true"
      ondragstart="dragStaff(event,'${sid}','${slot}','${widx}','${escAttr(staff.nickname)}')">${escHtml(staff.nickname)}</span>`;
}
function escHtml(s) { return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
function escAttr(s) { return (s||'').replace(/'/g,'&#39;'); }

// --- Inline edit ---
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

// --- Drag & Drop ---
function dragStaff(e, sid, slot, widx, nickname) {
    dragInfo = { isStock: true, sid: parseInt(sid), slot, widx: widx === 'pool' ? 'pool' : parseInt(widx), nickname };
    e.dataTransfer.effectAllowed = 'move';
}
function dropStaff(e, sid, slot, widx) {
    e.preventDefault();
    if (!dragInfo.isStock) return;
    if (parseInt(sid) !== dragInfo.sid) return;
    if (slot !== dragInfo.slot) return;
    const targetWidx = widx === 'pool' ? 'pool' : parseInt(widx);
    if (targetWidx === dragInfo.widx) return;

    const s = schedules.find(x => x.id === dragInfo.sid);
    const ti = slot === 'AM' ? 0 : 1;
    let srcList, dstList;

    if (dragInfo.widx === 'pool') {
        srcList = s.staffs[ti];
    } else {
        srcList = s.works[dragInfo.widx].tasks[ti].staffs;
    }
    if (targetWidx === 'pool') {
        dstList = s.staffs[ti];
    } else {
        dstList = s.works[targetWidx].tasks[ti].staffs;
    }

    const idx = srcList.findIndex(st => st.nickname === dragInfo.nickname);
    if (idx === -1) return;
    const [moved] = srcList.splice(idx, 1);
    dstList.push(moved);
    dragInfo.isStock = false;
    renderTable();
    saveSchedule(dragInfo.sid);
}

// --- Save ---
async function saveSchedule(sid) {
    const s = schedules.find(x => x.id === sid);
    await fetch('/schedule/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(s)
    }).catch(() => showErrorToast('更新に失敗しました'));
}

function openDailyReport(id) { window.open('/dailyReport?scheduleId=' + id); }

// --- Init ---
async function fetchSchedules() {
    const res = await fetch(`/schedule/${BOARD_ID}/${TARGET_MONTH}/readByMonth`);
    schedules = await res.json();
    // schedule_date を UTC 文字列として扱うための正規化
    schedules.forEach(s => { s.id = Number(s.id); });
    renderTable();
}

document.addEventListener('DOMContentLoaded', () => {
    // タイトルに年月表示
    const [y, m] = TARGET_MONTH.split('-');
    document.getElementById('pageTitle').textContent = `月別スケジュール (${Number(y)}/${Number(m)})`;

    buildDowFilters();
    buildWeekFilters();

    document.getElementById('filter-show-detail').addEventListener('change', function() {
        document.getElementById('detail-filters').style.display = this.checked ? '' : 'none';
    });
    ['filter-past-day','filter-am','filter-pm'].forEach(id => {
        document.getElementById(id).addEventListener('change', renderTable);
    });
    document.getElementById('dow-filters').addEventListener('change', renderTable);
    document.getElementById('week-filters').addEventListener('change', renderTable);

    fetchSchedules();
});
