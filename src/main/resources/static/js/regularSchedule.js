'use strict';

const DAY_JP = ['日', '月', '火', '水', '木', '金', '土'];

// --- Render (編集後の再描画用) ---
function renderTable() {
    const tbl = document.getElementById('schedule-table');
    let html = '';
    schedules.forEach(s => {
        const works = s.works;
        html += `<thead><tr class="bg-dark bg-gradient text-light">
          <th class="fs-5" style="width:7em">第${s.week + 1}${DAY_JP[s.day_of_week]}曜
            <i class="fa fa-print" style="cursor:pointer" onclick="openReport(${s.id})"></i>
          </th>
          ${works.map(w => `<th class="fs-6">${escHtml(w.name)}</th>`).join('')}
        </tr></thead><tbody>`;
        if (SHOW_AM) {
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
        if (SHOW_PM) {
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

function escHtml(s) { return (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

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

// --- 保存 ---
async function saveSchedule(sid) {
    const s = schedules.find(x => x.id === sid);
    await fetch('/regular_schedules/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(s)
    }).catch(() => showErrorToast('更新に失敗しました'));
}

function openReport(id) { window.open('/dailyReportSample?scheduleId=' + id); }
