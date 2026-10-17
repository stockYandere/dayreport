'use strict';
/* 하루 리포트(수면·앱 사용·한 일). 할 일 추가·O/X는 바탕화면 위젯에서 한다. app.js의 call/$/esc/modal/toast/pager를 사용한다. */
let reportDay=Logic.day(Date.now()),reportCache={},taskData={open:[],finished:[],today:0},reportPage=0,taskEdit=null;
const Day={
 sleepFor(records,day){const end=Logic.shift(day,1),rows=records.filter(r=>r.state==='confirmed'&&r.end>=day&&r.end<end).sort((a,b)=>a.start-b.start);let main=null,total=0;for(const r of rows){total+=r.end-r.start;if(r.kind!=='nap'&&(!main||r.end-r.start>main.end-main.start))main=r}return {rows,main,total,naps:rows.filter(r=>r.kind==='nap').length,pending:records.filter(r=>r.state==='candidate'&&r.end>=day&&r.end<end).length}},
 percent(ms,day){return Math.max(0,Math.min(100,(ms-day)/(Logic.shift(day,1)-day)*100))},
 clockLabel(ms){return ms>0?Logic.time(ms):'—'}
};
function reportData(day){if(!reportCache[day])reportCache[day]=call('dayReport',day);return reportCache[day]}
function loadTasks(){let t=call('tasks',Logic.shift(Logic.day(Date.now()),-29));if(t?.ok)taskData=t;return t}
function reportShift(n){let next=Logic.shift(reportDay,n);if(next>Logic.day(Date.now()))return;reportDay=next;reportPage=0;render()}
function reportToday(){reportDay=Logic.day(Date.now());reportPage=0;render()}
function lane(label,cls,items){return `<div class="day-lane"><span class="day-lane-label">${label}</span><div class="day-track">${items}</div></div>`}
function reportLimit(){return Math.max(1,Math.min(12,Math.floor((innerHeight-(innerHeight<600?420:470))/38)))}
function renderReport(){
 const day=reportDay,r=reportData(day)||{},sleep=Day.sleepFor(data.records,day),tasks=[...(r.tasks||[])].sort((a,b)=>a.finished-b.finished),done=tasks.filter(t=>t.state==='done'),skipped=tasks.length-done.length,isToday=day===Logic.day(Date.now());
 const sleepItems=Logic.segments(data.records,day).map(s=>`<span class="day-seg sleep ${s.kind==='nap'?'nap':''} ${s.state==='candidate'?'candidate':''}" style="left:${s.left}%;width:${s.width}%"></span>`).join('');
 const useItems=(r.spans||[]).map(([s,e])=>`<span class="day-seg use" style="left:${Day.percent(s,day)}%;width:${Math.max(.4,Day.percent(e,day)-Day.percent(s,day))}%"></span>`).join('');
 const doneItems=done.map(t=>`<span class="day-tick" style="left:${Day.percent(t.finished,day)}%" title="${esc(Logic.time(t.finished)+' '+t.title)}"></span>`).join('');
 const usage=!r.ok?'<b>—</b><small>불러오지 못함</small>':!r.permission?'<b>권한 필요</b><small>사용정보 접근</small>':!r.available?'<b>—</b><small>최근 7일만 조회</small>':`<b>${Logic.duration(r.total||0)}</b><small>${Day.clockLabel(r.first)} – ${Day.clockLabel(r.last)}</small>`;
 const sleepTile=sleep.rows.length?`<b>${Logic.duration(sleep.total)}</b><small>${sleep.main?esc(rowTime(sleep.main)):'낮잠 '+sleep.naps+'회'}</small>`:`<b>기록 없음</b><small>${sleep.pending?'미확정 '+sleep.pending+'개':'직접 기록 가능'}</small>`;
 const limit=reportLimit(),total=Math.ceil(tasks.length/limit);reportPage=Math.min(reportPage,Math.max(0,total-1));
 const apps=(r.apps||[]).slice(0,3).map(a=>`<span class="app-chip"><i class="dot" style="background:${esc(a.color)}"></i>${esc(a.name)} ${Logic.duration(a.ms)}</span>`).join('');
 html(`<div class="row between"><div class="row"><button class="small" onclick="reportShift(-1)" aria-label="이전 날">‹</button><b>${dateLabel(day)}</b><button class="small" onclick="reportShift(1)" aria-label="다음 날" ${isToday?'disabled':''}>›</button></div>${isToday?'':'<button class="small" onclick="reportToday()">오늘</button>'}</div>
 <div class="day-flow"><div class="axis day-axis"><span>0시</span><span>6시</span><span>12시</span><span>18시</span><span>24시</span></div>${lane('수면','sleep',sleepItems)}${lane('사용','use',useItems)}${lane('한 일','done',doneItems)}</div>
 <div class="tiles"><button class="tile sleep-tile" onclick="anchor=reportDay;selected=reportDay;openRoute('pattern')"><span>수면</span>${sleepTile}</button><button class="tile use-tile" onclick="goDashboard(0)"><span>앱 사용</span>${usage}</button><div class="tile done-tile"><span>한 일</span><b>${done.length}개</b><small>${skipped?'안 함 '+skipped+'개':isToday?'남은 할 일 '+taskData.open.length+'개':'&nbsp;'}</small></div></div>
 ${apps?`<div class="app-chips">${apps}</div>`:''}
 <div class="day-done"><div class="row between"><b>한 일</b><button class="small olive" onclick="openDoneEditor(${day})">＋ 한 일 기록</button></div><div class="list done-list">${tasks.length?tasks.slice(reportPage*limit,(reportPage+1)*limit).map(t=>taskLine(t)).join(''):`<div class="empty">${isToday?'위젯에서 O로 완료하거나<br>한 일을 바로 기록해 줘.':'이 날 기록한 일이 없어.'}</div>`}</div>${total>1?pager(reportPage,total,'report'):''}</div>`)
}
function taskLine(t){return `<div class="task-line ${t.state==='skipped'?'skipped':''}" role="button" tabindex="0" onclick="openTaskEditor(${t.id})"><span class="task-time">${Logic.time(t.finished)}</span><span class="task-title">${esc(t.title)}</span>${t.state==='skipped'?'<span class="tag">안 함</span>':''}</div>`}
function afterTask(result,message){if(!result?.ok){toast(result?.message||'처리하지 못했어.');return false}loadTasks();reportCache={};render();if(message)toast(message);return true}
function findTask(id){for(const k in reportCache)for(const t of reportCache[k]?.tasks||[])if(t.id===id)return t;return null}
function openTaskEditor(id){const t=findTask(id);if(!t)return;taskEdit=t;const finished=t.state!=='open';modal(`<div class="row between"><h2>${finished?'한 일 수정':'할 일 수정'}</h2><button class="small" onclick="closeModal()">닫기</button></div><label>내용<input id="taskTitle" maxlength="200" value="${esc(t.title)}"></label>${finished?`<label>${t.state==='done'?'완료':'안 함 처리'} 날짜·시간<input id="taskTime" type="datetime-local" value="${Logic.input(t.finished)}"></label>`:''}<div id="error" class="error" role="alert"></div><div class="row"><button onclick="deleteTask(${t.id})">삭제</button>${finished?`<button onclick="reopenTask(${t.id})">할 일로 되돌리기</button>`:''}<button class="primary grow" onclick="saveTask()">저장</button></div>`)}
function saveTask(){const t=taskEdit;if(!t)return;let time=0;if($('#taskTime')){time=$('#taskTime').value===Logic.input(t.finished)?t.finished:+new Date($('#taskTime').value);if(!Number.isFinite(time)||time>Date.now()+60000){$('#error').textContent='미래가 아닌 날짜와 시간을 입력해 줘.';return}}let r=call('editTask',t.id,$('#taskTitle').value,time);if(!r?.ok){$('#error').textContent=r?.message||'저장하지 못했어.';return}closeModal();afterTask(r,'저장했어')}
function reopenTask(id){closeModal();afterTask(call('taskState',id,'open'),'할 일 목록으로 되돌렸어')}
function deleteTask(id){closeModal();modal(`<h2>삭제</h2><p>이 일을 목록과 기록에서 지울게.</p><div class="row"><button class="grow" onclick="closeModal()">취소</button><button class="grow primary" onclick="closeModal();afterTask(call('deleteTask',${id}),'삭제했어')">삭제</button></div>`)}
function openDoneEditor(day){let now=Date.now(),time=day>=Logic.day(now)?now:day+21*3600000;modal(`<div class="row between"><h2>한 일 기록</h2><button class="small" onclick="closeModal()">닫기</button></div><p class="muted">할 일 목록을 거치지 않고 바로 한 일로 남겨.</p><label>무엇을 했어?<input id="doneTitle" maxlength="200" placeholder="예: 장보기"></label><label>한 시간<input id="doneTime" type="datetime-local" value="${Logic.input(time)}"></label><div id="error" class="error" role="alert"></div><button class="primary" onclick="saveDone(${time})">기록</button>`);$('#doneTitle').focus()}
function saveDone(original){let raw=$('#doneTime').value,time=raw===Logic.input(original)?original:+new Date(raw);if(!Number.isFinite(time)||time>Date.now()+60000){$('#error').textContent='미래가 아닌 날짜와 시간을 입력해 줘.';return}let r=call('addDone',$('#doneTitle').value,time);if(!r?.ok){$('#error').textContent=r?.message||'기록하지 못했어.';return}closeModal();afterTask(r,'한 일을 기록했어')}
if(typeof module!=='undefined')module.exports=Day;
