'use strict';

const DAY_JP = ['日', '月', '火', '水', '木', '金', '土'];
let dragInfo = { isStock: false, sid: null, slot: null, widx: null, nickname: null };

// --- Render (D&D後の再描画用) ---
function renderTable() {
    const tbl = document.getElementById('schedule-table');
    let html = '';
    schedules.forEach(s => {
        const works = s.works;
        html += `<thead><tr class="bg-success bg-gradient text-light">
          <th class="fs-5" style="width:7em">${toDateStr(s)}
            <i class="fa fa-print" style="cursor:pointer" onclick="openDailyReport(${s.id})"></i>
          </th>
          ${works.map(w => `<th class="fs-6">${escHtml(w.name)}</th>`).join('')}
        </tr></thead><tbody>`;
        if (SHOW_AM) {
            const amStaffs = s.staffs[0] || [];
            html += `<tr>
              <th class="table-secondary" data-sid="${s.id}" data-slot="AM" data-widx="pool"
                  ondragover="event.preventDefault()" ondrop="dropStaff(event)">
                <div class="fs-6">AM</div>
                <div>${amStaffs.map(st => staffBadge(s.id, 'AM', 'pool', st)).join('')}</div>
              </th>
              ${works.map((w, wi) => {
                  const task = w.tasks[0];
                  const badges = (task.staffs || []).map(st => staffBadge(s.id, 'AM', wi, st)).join('');
                  return `<td ondblclick="editCell(event)" data-sid="${s.id}" data-widx="${wi}" data-tidx="0" data-slot="AM"
                              ondragover="event.preventDefault()" ondrop="dropStaff(event)">
                    <div>${badges}</div>
                    <pre class="cut cell-text">${escHtml(task.taskContents)}</pre>
                  </td>`;
              }).join('')}
            </tr>`;
        }
        if (SHOW_PM) {
            const pmStaffs = s.staffs[1] || [];
            html += `<tr>
              <th class="table-secondary" data-sid="${s.id}" data-slot="PM" data-widx="pool"
                  ondragover="event.preventDefault()" ondrop="dropStaff(event)">
                <div class="fs-6">PM</div>
                <div>${pmStaffs.map(st => staffBadge(s.id, 'PM', 'pool', st)).join('')}</div>
              </th>
              ${works.map((w, wi) => {
                  const task = w.tasks[1];
                  const badges = (task.staffs || []).map(st => staffBadge(s.id, 'PM', wi, st)).join('');
                  return `<td ondblclick="editCell(event)" data-sid="${s.id}" data-widx="${wi}" data-tidx="1" data-slot="PM"
                              ondragover="event.preventDefault()" ondrop="dropStaff(event)">
                    <div>${badges}</div>
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
      data-sid="${sid}" data-slot="${escAttr(slot)}" data-widx="${escAttr(String(widx))}" data-nickname="${escAttr(staff.nickname)}"
      ondragstart="dragStaff(event)">${escHtml(staff.nickname)}</span>`;
}
function toDateStr(s) {
    const d = new Date(s.schedule_date);
    return (d.getUTCMonth() + 1) + '/' + d.getUTCDate() + '(' + DAY_JP[s.day_of_week] + ')';
}
function escHtml(s) { return (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }
function escAttr(s) { return (s || '').replace(/&/g, '&amp;').replace(/"/g, '&quot;'); }

// --- インライン編集 ---
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

// --- D&D ---
function dragStaff(e) {
    const el = e.currentTarget;
    dragInfo = {
        isStock: true,
        sid:      parseInt(el.dataset.sid),
        slot:     el.dataset.slot,
        widx:     el.dataset.widx === 'pool' ? 'pool' : parseInt(el.dataset.widx),
        nickname: el.dataset.nickname
    };
    e.dataTransfer.effectAllowed = 'move';
}
function dropStaff(e) {
    e.preventDefault();
    const el = e.currentTarget;
    const sid    = parseInt(el.dataset.sid);
    const slot   = el.dataset.slot;
    const widx   = el.dataset.widx === 'pool' ? 'pool' : parseInt(el.dataset.widx);

    if (!dragInfo.isStock) return;
    if (sid !== dragInfo.sid) return;
    if (slot !== dragInfo.slot) return;
    if (widx === dragInfo.widx) return;

    const s = schedules.find(x => x.id === dragInfo.sid);
    const ti = slot === 'AM' ? 0 : 1;

    const srcList = dragInfo.widx === 'pool' ? s.staffs[ti] : s.works[dragInfo.widx].tasks[ti].staffs;
    const dstList = widx === 'pool'          ? s.staffs[ti] : s.works[widx].tasks[ti].staffs;

    const idx = srcList.findIndex(st => st.nickname === dragInfo.nickname);
    if (idx === -1) return;
    const [moved] = srcList.splice(idx, 1);
    dstList.push(moved);
    dragInfo.isStock = false;
    renderTable();
    saveSchedule(dragInfo.sid);
}

// --- 保存 ---
async function saveSchedule(sid) {
    const s = schedules.find(x => x.id === sid);
    await fetch('/schedule/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(s)
    }).catch(() => showErrorToast('更新に失敗しました'));
}

function openDailyReport(id) { window.open('/dailyReport?scheduleId=' + id); }
