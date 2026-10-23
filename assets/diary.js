'use strict';
/* 일기: 날짜마다 하나. 같은 날은 계속 고쳐 쓰고 덧붙인다. 입력을 멈추면 1초 뒤, 화면을 떠날 때 저장한다. */
let diaryDay=Logic.day(Date.now()),diaryEntry=null,diaryTimer=null,diaryDirty=false,diaryEntries=null,diaryPage=0;
function dayFromKey(key){let [y,m,d]=key.split('-').map(Number);return +new Date(y,m-1,d)}
function loadDiary(){let key=Logic.key(diaryDay),r=call('diary',key);diaryEntry=r?.ok?{day:key,text:r.text||'',updated:r.updated||0}:{day:key,text:'',updated:0};diaryDirty=false;if(!r?.ok&&r?.message)toast(r.message)}
function renderDiary(){
 const key=Logic.key(diaryDay),area=$('#diaryText');
 // 키보드가 열리며 화면 크기가 바뀌거나 새로고침해도 쓰던 입력칸을 다시 만들지 않는다(커서·키보드 유지).
 if(area&&area.dataset.day===key){diaryStatus();return}
 if(!diaryEntry||diaryEntry.day!==key)loadDiary();
 const isToday=diaryDay===Logic.day(Date.now());
 html(`<div class="row between diary-nav"><div class="row"><button class="small" onclick="diaryShift(-1)" aria-label="이전 날">‹</button><b>${dateLabel(diaryDay)}</b><button class="small" onclick="diaryShift(1)" aria-label="다음 날" ${isToday?'disabled':''}>›</button></div>${isToday?'':'<button class="small" onclick="diaryGo(Logic.day(Date.now()))">오늘</button>'}</div>
 <textarea id="diaryText" data-day="${key}" maxlength="50000" placeholder="있었던 일, 생각, 기분…&#10;같은 날은 언제든 이어서 쓸 수 있어." aria-label="${dateLabel(diaryDay)} 일기"></textarea>
 <div class="row between diary-bar"><span id="diaryStatus" class="muted"></span>${isToday?'<button class="small" onmousedown="event.preventDefault()" onclick="diaryStamp()">＋ 지금 시각</button>':''}</div>`);
 const t=$('#diaryText');t.value=diaryEntry.text;
 t.addEventListener('input',()=>{diaryEntry.text=t.value;diaryDirty=true;diaryStatus();clearTimeout(diaryTimer);diaryTimer=setTimeout(flushDiary,1000)});
 t.addEventListener('focus',()=>document.body.classList.add('typing'));
 t.addEventListener('blur',()=>{document.body.classList.remove('typing');flushDiary()});
 diaryStatus()
}
function diaryStatus(){const el=$('#diaryStatus');if(!el||!diaryEntry)return;const n=diaryEntry.text.length;el.textContent=diaryDirty?'저장 중…':diaryEntry.updated?`저장됨 · ${Logic.key(diaryEntry.updated)===Logic.key(Date.now())?'':shortDate(diaryEntry.updated)+' '}${Logic.time(diaryEntry.updated)} · ${n}자`:n?`${n}자`:'아직 쓴 내용 없음'}
/* 저장할 것이 없거나 저장에 성공하면 true. */
function flushDiary(){clearTimeout(diaryTimer);if(!diaryDirty||!diaryEntry)return true;let r=call('saveDiary',diaryEntry.day,diaryEntry.text);if(!r?.ok){toast(r?.message||'일기를 저장하지 못했어.');diaryStatus();return false}diaryDirty=false;diaryEntry.updated=diaryEntry.text.trim()?r.updated:0;diaryEntries=null;reportCache={};diaryStatus();return true}
function diaryGo(day){flushDiary();diaryDay=Logic.day(day);tab='diary';render()}
function diaryShift(n){let next=Logic.shift(diaryDay,n);if(next>Logic.day(Date.now()))return;diaryGo(next)}
function openDiary(day){openRoute('diary');diaryGo(day)}
// 끝에 "[14:32] " 줄을 붙이고 바로 이어 쓰게 커서를 둔다. 버튼이 포커스를 가져가지 않아 키보드가 그대로 열려 있다.
function diaryStamp(){const t=$('#diaryText');if(!t)return;let v=t.value,sep=!v.trim()?'':v.endsWith('\n\n')?'':v.endsWith('\n')?'\n':'\n\n';t.value=v+sep+'['+Logic.time(Date.now())+'] ';t.dispatchEvent(new Event('input'));t.focus();t.setSelectionRange(t.value.length,t.value.length);t.scrollTop=t.scrollHeight}
function renderDiaryList(){
 flushDiary();if(!diaryEntries){let r=call('diaryList');diaryEntries=r?.ok?r.entries:[]}
 const rows=diaryEntries,limit=lineLimit(),total=Math.ceil(rows.length/limit);diaryPage=Math.min(diaryPage,Math.max(0,total-1));
 html(`<div class="muted record-count">일기 ${rows.length}일 · 최근 순</div><div class="list record-list">${rows.length?rows.slice(diaryPage*limit,(diaryPage+1)*limit).map(e=>{const day=dayFromKey(e.day);return `<div class="sleep-row diary-row" role="button" tabindex="0" onclick="diaryGo(${day})" onkeydown="if(event.key==='Enter')diaryGo(${day})"><span class="diary-date">${esc(dateLabel(day))}</span><span class="task-title">${esc(e.preview)||'…'}</span><span class="muted">${e.length}자</span></div>`}).join(''):'<div class="empty">아직 쓴 일기가 없어.<br>쓰기에서 오늘 일기를 시작해 봐.</div>'}</div>${total>1?pager(diaryPage,total,'diary'):''}<div class="muted row-hint">줄을 누르면 그날 일기를 이어서 쓸 수 있어</div>`)
}
addEventListener('visibilitychange',()=>{if(document.hidden)flushDiary()});addEventListener('pagehide',()=>flushDiary());
