const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path');
// 수면 기록 버튼(앱 화면). 상태 저장·알림·위젯은 Android 쪽이라 테스트 더블로 대신한다.
function mock(){
 const H=3600000;let rec={state:'idle',start:-1,slept:0,pausedAt:-1,unsaved:[]};window.actions=[];window.saved=[];window.dropped=[];
 window.setRecorder=r=>rec=r;
 const state=()=>({ok:true,...rec,now:Date.now()});
 window.Native={
  refresh:()=>JSON.stringify({ok:true,records:saved,permission:true,minimum:240,recorder:state()}),
  recorder:()=>JSON.stringify(state()),
  recorderAction:a=>{actions.push(a);const now=Date.now();
   if(a==='start')rec={...rec,state:'recording',start:now,slept:0};else if(a==='pause')rec={...rec,state:'paused',pausedAt:now};else if(a==='resume')rec={...rec,state:'recording'};else if(a==='restart')rec={...rec,state:'recording',start:now,slept:0};else if(a==='stop')rec={...rec,state:'idle',start:-1,slept:0};
   return JSON.stringify({...state(),message:a+' done'})},
  recorderDropUnsaved:(s,e)=>{dropped.push([s,e]);rec={...rec,unsaved:rec.unsaved.filter(u=>u.start!==s)};return JSON.stringify(state())},
  save:x=>{let r=JSON.parse(x);saved.push({...r,id:saved.length+1,state:'confirmed'});return '{"ok":true}'},
  remove:()=>'{"ok":true}',dashboard(){},closeNativeEditor(){},permission(){},exit(){}
 };
}
async function layout(p,label,size){const r=await p.evaluate(()=>{let c=document.querySelector('#content');let bad=[...document.querySelectorAll('button')].filter(b=>{if(!b.getClientRects().length||b.closest('dialog')&&!b.closest('dialog').open)return false;let r=b.getBoundingClientRect();return r.left<-.5||r.right>innerWidth+.5||r.top<-.5||r.bottom>innerHeight+.5});return {body:document.documentElement.scrollHeight>innerHeight+1,content:c.scrollHeight>c.clientHeight+2,bad:bad.map(b=>b.textContent)}});assert.deepEqual(r,{body:false,content:false,bad:[]},`${size} ${label} ${JSON.stringify(r)}`)}
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||undefined,args:['--no-sandbox']});let count=0;
 for(const [width,height] of [[412,850],[360,740],[320,568],[412,440],[800,600]]){
  const size=`${width}x${height}`,p=await browser.newPage({viewport:{width,height},timezoneId:'Asia/Seoul'});let errors=[];p.on('pageerror',e=>errors.push(e.message));
  await p.addInitScript(mock);await p.goto('file://'+path.resolve(__dirname,'../assets/index.html'));await p.evaluate(()=>startApp('pattern'));await p.waitForTimeout(60);
  await layout(p,'idle',size);count++;
  await p.getByRole('button',{name:'수면 시작',exact:true}).click();await layout(p,'recording',size);count++;if(width===412&&height===850)await p.screenshot({path:path.resolve(__dirname,'../output/qa-recorder.png')});
  await p.getByRole('button',{name:'일시 중지',exact:true}).click();await layout(p,'paused',size);count++;
  await p.evaluate(()=>{setRecorder({state:'paused',start:Date.now()-3*3600000,slept:2*3600000,pausedAt:Date.now()-600000,unsaved:[{start:Date.now()-30*3600000,end:Date.now()-24*3600000}]});lastRefresh=0;refresh()});await p.waitForTimeout(60);await layout(p,'paused + unsaved',size);count++;
  assert.deepEqual(errors,[]);await p.close();
 }
 const p=await browser.newPage({viewport:{width:393,height:825},timezoneId:'Asia/Seoul'});let errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.addInitScript(mock);await p.goto('file://'+path.resolve(__dirname,'../assets/index.html'));await p.evaluate(()=>startApp('pattern'));await p.waitForTimeout(60);
 assert.equal(await p.getByRole('button',{name:'직접 기록',exact:true}).count(),1,'idle shows manual record + start');
 await p.getByRole('button',{name:'수면 시작',exact:true}).click();assert.match(await p.locator('#recClock').textContent(),/수면 기록 중 · 0분/);assert.equal(await p.getByRole('button',{name:'수면 시작'}).count(),0);
 await p.getByRole('button',{name:'일시 중지',exact:true}).click();assert.match(await p.locator('#recClock').textContent(),/일시 중지/);assert.match(await p.locator('.recorder').textContent(),/부터 깸/);
 await p.getByRole('button',{name:'재개',exact:true}).click();assert.match(await p.locator('#recClock').textContent(),/수면 기록 중/);
 await p.getByRole('button',{name:'다시 시작',exact:true}).click();assert.equal(await p.locator('#modal h2').textContent(),'다시 시작','restart asks first in the app');await p.getByRole('button',{name:'취소',exact:true}).click();assert.deepEqual(await p.evaluate(()=>actions),['start','pause','resume']);
 await p.getByRole('button',{name:'다시 시작',exact:true}).click();await p.locator('#modal').getByRole('button',{name:'다시 시작',exact:true}).click();
 // 위젯·잠금 화면에서 바꾼 상태를 가져온다(30초 주기 대신 직접 호출)
 await p.evaluate(()=>{setRecorder({state:'paused',start:Date.now()-5*3600000,slept:4*3600000,pausedAt:Date.now()-60000,unsaved:[]});lastRefresh=0;refresh()});await p.waitForTimeout(60);assert.match(await p.locator('#recClock').textContent(),/일시 중지 · 4시간 0분/,'state changed on lock screen/widget is shown');
 await p.getByRole('button',{name:'기록 정지',exact:true}).click();await p.waitForTimeout(60);assert.deepEqual(await p.evaluate(()=>actions.slice(-2)),['restart','stop']);assert.equal(await p.getByRole('button',{name:'수면 시작',exact:true}).count(),1,'back to idle after stop');
 // 저장 못 한 구간: 고쳐서 저장하면 목록에서 빠진다
 const u=await p.evaluate(()=>{let u={start:Date.now()-30*3600000,end:Date.now()-24*3600000};setRecorder({state:'idle',start:-1,slept:0,pausedAt:-1,unsaved:[u]});lastRefresh=0;refresh();return u});await p.waitForTimeout(60);
 assert.match(await p.locator('.notice').textContent(),/저장 못 한 수면 기록 1개/);await p.getByRole('button',{name:'고쳐서 저장',exact:true}).click();
 assert.equal(await p.locator('#start').inputValue(),await p.evaluate(u=>Logic.input(u.start),u));await p.getByRole('button',{name:'메모',exact:false}).click();await p.getByRole('button',{name:'적용',exact:true}).click();await p.getByRole('button',{name:'저장',exact:true}).click();await p.waitForTimeout(60);
 assert.deepEqual(await p.evaluate(()=>dropped),[[u.start,u.end]],'saved piece removed from unsaved (survives memo dialog)');assert.equal(await p.evaluate(()=>saved.length),1);
 assert.deepEqual(errors,[]);await browser.close();
 console.log(`PASS: ${count} recorder layouts, start/pause/resume, restart confirm, external state sync, stop, unsaved fix & drop`);
})().catch(e=>{console.error(e);process.exit(1)});
