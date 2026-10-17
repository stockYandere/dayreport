const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path');
// 하루 리포트 · 할 일 화면. Android 브리지는 메모리 테스트 더블로 대신한다.
function mock(){
 const now=Date.now(),day=new Date();day.setHours(0,0,0,0);const d0=+day,H=3600000;let id=10;
 window.tasks=[{id:1,title:'창문에 검은 네모 붙이기',created:now-5*H,state:'open',finished:0},{id:2,title:'세탁기 돌리기',created:now-4*H,state:'open',finished:0},{id:3,title:'아침 산책',created:d0+7*H,state:'done',finished:Math.min(now-60000,d0+8*H)},{id:4,title:'분리수거',created:d0-20*H,state:'skipped',finished:d0-18*H}];
 const records=[{id:1,start:d0-1.5*H,end:d0+6.5*H,state:'confirmed',kind:'sleep',note:'',zone:'Asia/Seoul'},{id:2,start:d0-25.5*H,end:d0-17.5*H,state:'confirmed',kind:'sleep',note:'',zone:'Asia/Seoul'}];
 const ok=m=>JSON.stringify({ok:true,message:m}),fail=m=>JSON.stringify({ok:false,message:m});
 window.calls=[];
 window.Native={
  refresh:()=>JSON.stringify({ok:true,records,permission:true,minimum:300,timer:0}),
  tasks:from=>{let start=new Date(from);start.setHours(0,0,0,0);return JSON.stringify({ok:true,open:tasks.filter(t=>t.state==='open'),finished:tasks.filter(t=>t.state!=='open'&&t.finished>=+start),today:tasks.filter(t=>t.state==='done'&&t.finished>=d0).length,now:Date.now()})},
  dayReport:dayStart=>{let end=dayStart+24*H;calls.push(['dayReport',dayStart]);return JSON.stringify({ok:true,day:dayStart,permission:true,available:dayStart>=d0-6*24*H,total:3*H+12*60000,first:dayStart+7*H+15*60000,last:Math.min(now,dayStart+23*H),apps:[{name:'유튜브',ms:1.5*H,count:4,color:'#78A7FF'},{name:'카카오톡',ms:40*60000,count:20,color:'#FFB86B'},{name:'Chrome',ms:20*60000,count:3,color:'#9AD0C2'}],spans:[[dayStart+7.25*H,dayStart+7.6*H],[dayStart+12*H,dayStart+13.1*H]],tasks:tasks.filter(t=>t.state!=='open'&&t.finished>=dayStart&&t.finished<end)})},
  addTask:title=>{title=String(title||'').trim();if(!title)return fail('할 일을 입력해 줘.');tasks.push({id:++id,title,created:Date.now(),state:'open',finished:0});return ok('추가했어')},
  addDone:(title,time)=>{title=String(title||'').trim();if(!title)return fail('할 일을 입력해 줘.');tasks.push({id:++id,title,created:time,state:'done',finished:time});return ok('기록')},
  taskState:(i,state)=>{let t=tasks.find(t=>t.id===i);t.state=state;t.finished=state==='open'?0:Date.now();return ok('처리')},
  editTask:(i,title,finished)=>{let t=tasks.find(t=>t.id===i);t.title=title;if(t.state!=='open'&&finished>0)t.finished=finished;return ok('저장')},
  deleteTask:i=>{tasks=tasks.filter(t=>t.id!==i);return ok('삭제')},
  dashboard:p=>calls.push(['dashboard',p]),dashboardAt:(p,d)=>calls.push(['dashboardAt',p,d]),exit:()=>calls.push(['exit']),
  closeNativeEditor(){},save:()=>ok(''),remove:()=>ok(''),timer(){},permission(){},export(){},restore(){},minimum(){}
 };
}
async function layout(p,label,size){const r=await p.evaluate(()=>{let c=document.querySelector('#content');let bad=[...document.querySelectorAll('button')].filter(b=>{if(!b.getClientRects().length||b.closest('dialog')&&!b.closest('dialog').open)return false;let r=b.getBoundingClientRect();return r.left<-.5||r.right>innerWidth+.5||r.top<-.5||r.bottom>innerHeight+.5});let d=document.querySelector('dialog');let clipped=[...document.querySelectorAll('#content > *')].filter(e=>e.getBoundingClientRect().bottom>c.getBoundingClientRect().bottom+1).map(e=>e.className);return {body:document.documentElement.scrollHeight>innerHeight+1,content:c.scrollHeight>c.clientHeight+2,bad:bad.map(b=>b.textContent),clipped,dialogOverflow:d.open&&d.scrollHeight>d.clientHeight+2}});assert.deepEqual(r,{body:false,content:false,bad:[],clipped:[],dialogOverflow:false},`${size} ${label} ${JSON.stringify(r)}`)}
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||undefined,args:['--no-sandbox']});let count=0;
 for(const [width,height] of [[412,850],[360,740],[320,568],[412,440],[800,600]]){
  const size=`${width}x${height}`,p=await browser.newPage({viewport:{width,height},timezoneId:'Asia/Seoul'});let errors=[];p.on('pageerror',e=>errors.push(e.message));
  await p.addInitScript(mock);await p.goto('file://'+path.resolve(__dirname,'../assets/index.html'));await p.evaluate(()=>startApp());await p.waitForTimeout(80);
  assert.equal(await p.locator('#title').textContent(),'하루');await layout(p,'report',size);count++;await p.screenshot({path:path.resolve(__dirname,`../output/qa-report-${size}.png`)});
  await p.locator('.task-line').first().click();await layout(p,'task editor',size);count++;await p.getByRole('button',{name:'닫기',exact:true}).click();
  await p.evaluate(()=>openDoneEditor(Logic.day(Date.now())));await layout(p,'done editor',size);count++;await p.getByRole('button',{name:'닫기',exact:true}).click();
  assert.deepEqual(errors,[]);await p.close();
 }
 // 동작 검사
 const p=await browser.newPage({viewport:{width:393,height:825},timezoneId:'Asia/Seoul'});let errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.addInitScript(mock);await p.goto('file://'+path.resolve(__dirname,'../assets/index.html'));await p.evaluate(()=>{tasks.find(t=>t.id===1).state='done';tasks.find(t=>t.id===1).finished=Date.now()-60000;tasks.find(t=>t.id===2).state='skipped';tasks.find(t=>t.id===2).finished=Date.now()-30000;startApp('report')});await p.waitForTimeout(80);
 assert.equal(await p.locator('nav button').count(),4,'4 tabs');assert.deepEqual(await p.locator('nav button').allTextContents(),['시간대','하루','앱별','수면']);
 assert.equal(await p.locator('.task-line').count(),3,'report lists done + skipped');assert.equal(await p.locator('.task-line.skipped').count(),1);assert.equal(await p.locator('.day-tick').count(),2,'done ticks on day flow');
 assert.match(await p.locator('.sleep-tile').textContent(),/8시간 0분/);assert.match(await p.locator('.use-tile').textContent(),/3시간 12분/);assert.match(await p.locator('.done-tile').textContent(),/2개/);
 await p.evaluate(()=>openRoute('todo'));assert.equal(await p.locator('#title').textContent(),'하루','old todo route falls back to report');
 await p.screenshot({path:path.resolve(__dirname,'../output/qa-report-flow.png')});
 await p.getByRole('button',{name:'＋ 한 일 기록'}).click();await p.locator('#doneTitle').fill('빨래 개기');await p.getByRole('button',{name:'기록',exact:true}).click();await p.waitForTimeout(30);assert.equal(await p.locator('.task-line').count(),4,'direct done record');
 await p.locator('.task-line.skipped').click();await p.getByRole('button',{name:'할 일로 되돌리기'}).click();await p.waitForTimeout(30);assert.equal(await p.locator('.task-line.skipped').count(),0,'reopen');assert.equal(await p.evaluate(()=>tasks.find(t=>t.id===2).state),'open');
 await p.getByRole('button',{name:'이전 날'}).click();await p.waitForTimeout(30);assert.equal(await p.locator('.task-line').count(),1,'yesterday shows skipped item');
 await p.locator('.use-tile').click();let last=await p.evaluate(()=>calls.filter(c=>c[0]==='dashboardAt').pop());assert.equal(last[1],0);assert.equal(last[2],await p.evaluate(()=>reportDay),'opens timeline on report day');
 await p.evaluate(()=>{openRoute('report');back()});assert.deepEqual(await p.evaluate(()=>calls.pop().slice(0,2)),['dashboardAt',0],'back returns to timeline');
 await p.evaluate(()=>{openRoute('sleep');back()});assert.deepEqual(await p.evaluate(()=>calls.pop()),['dashboard',0]);
 assert.deepEqual(errors,[]);await p.close();await browser.close();
 console.log(`PASS: ${count} report layouts, 4 tabs, todo screens removed, reopen/direct record, report tiles & day flow, day-linked timeline, back to timeline`);
})().catch(e=>{console.error(e);process.exit(1)});
