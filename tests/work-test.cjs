const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path');
// 근무 시간 창(수면 탭). 저장·후보 계산은 Android 쪽이라 테스트 더블로 대신한다.
function mock(){
 let days=[{on:false,start:0,end:480},{on:false,start:0,end:480},{on:true,start:0,end:480},{on:true,start:0,end:480},{on:true,start:0,end:480},{on:true,start:0,end:600},{on:true,start:0,end:600}];window.savedWork=[];window.refreshes=0;
 window.Native={
  refresh:()=>{refreshes++;return JSON.stringify({ok:true,records:[],permission:true,minimum:240,recorder:{state:'idle',unsaved:[]}})},
  recorder:()=>JSON.stringify({ok:true,state:'idle',unsaved:[]}),
  workHours:()=>JSON.stringify({ok:true,days}),
  saveWorkHours:x=>{let d=JSON.parse(x);savedWork.push(d);days=d;return '{"ok":true}'},
  dashboard(){},closeNativeEditor(){},permission(){},exit(){}
 };
}
async function layout(p,label,size){const r=await p.evaluate(()=>{let c=document.querySelector('#content'),d=document.querySelector('#modal');let bad=[...document.querySelectorAll('button')].filter(b=>{if(!b.getClientRects().length||b.closest('dialog')&&!b.closest('dialog').open)return false;let r=b.getBoundingClientRect();return r.left<-.5||r.right>innerWidth+.5||r.top<-.5||r.bottom>innerHeight+.5});let inputs=[...d.querySelectorAll('.work-row')].filter(x=>x.scrollWidth>x.clientWidth+1).length;let dr=d.getBoundingClientRect(),dc=d.querySelector('#modalContent').getBoundingClientRect();return {body:document.documentElement.scrollHeight>innerHeight+1,content:c.scrollHeight>c.clientHeight+2,bad:bad.map(b=>b.textContent),rowOverflow:inputs,dialogClipped:d.open&&dc.bottom>dr.bottom+1}});assert.deepEqual(r,{body:false,content:false,bad:[],rowOverflow:0,dialogClipped:false},`${size} ${label} ${JSON.stringify(r)}`)}
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||undefined,args:['--no-sandbox']});let count=0;
 for(const [width,height] of [[412,850],[360,740],[320,568],[412,440],[800,600]]){
  const size=`${width}x${height}`,p=await browser.newPage({viewport:{width,height},timezoneId:'Asia/Seoul'});let errors=[];p.on('pageerror',e=>errors.push(e.message));
  await p.addInitScript(mock);await p.goto('file://'+path.resolve(__dirname,'../assets/index.html'));await p.evaluate(()=>startApp('pattern'));await p.waitForTimeout(60);
  await layout(p,'pattern',size);count++;
  await p.getByRole('button',{name:'근무 시간',exact:true}).click();await layout(p,'work dialog',size);count++;if(width===412&&height===850)await p.screenshot({path:path.resolve(__dirname,'../output/qa-work.png')});
  assert.deepEqual(errors,[]);await p.close();
 }
 const p=await browser.newPage({viewport:{width:393,height:825},timezoneId:'Asia/Seoul'});let errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.addInitScript(mock);await p.goto('file://'+path.resolve(__dirname,'../assets/index.html'));await p.evaluate(()=>startApp('pattern'));await p.waitForTimeout(60);
 await p.getByRole('button',{name:'근무 시간',exact:true}).click();
 assert.equal(await p.locator('#workSummary').textContent(),'수–금 00:00–08:00 · 토–일 00:00–10:00','summary groups same consecutive days');
 assert.equal(await p.locator('#workStart0').isDisabled(),true,'off day times are disabled');
 // 월요일에 다음 날로 넘어가는 근무 추가
 await p.locator('#workOn0').check();await p.locator('#workStart0').fill('22:00');await p.locator('#workEnd0').fill('06:00');
 assert.equal(await p.locator('#workNext0').textContent(),'다음 날');assert.match(await p.locator('#workSummary').textContent(),/^월 22:00–06:00\(다음 날\) · 수–금/);
 // 시작=끝은 저장하지 않는다
 await p.locator('#workEnd0').fill('22:00');await p.getByRole('button',{name:'저장',exact:true}).click();
 assert.match(await p.locator('#workError').textContent(),/시작과 끝을 다르게/);assert.equal(await p.evaluate(()=>savedWork.length),0);
 await p.locator('#workEnd0').fill('06:00');const before=await p.evaluate(()=>refreshes);await p.getByRole('button',{name:'저장',exact:true}).click();await p.waitForTimeout(60);
 const saved=await p.evaluate(()=>savedWork[0]);assert.equal(saved.length,7);assert.deepEqual(saved[0],{on:true,start:1320,end:360});assert.deepEqual(saved[5],{on:true,start:0,end:600});assert.deepEqual(saved[1],{on:false,start:0,end:480},'off days keep their times');
 assert.equal(await p.locator('#modal').evaluate(d=>d.open),false,'dialog closes after save');assert.ok(await p.evaluate(()=>refreshes)>before,'candidates are recomputed after save');
 // 다시 열면 저장한 값, 끄기도 저장
 await p.getByRole('button',{name:'근무 시간',exact:true}).click();assert.equal(await p.locator('#workStart0').inputValue(),'22:00');
 for(let i=0;i<7;i++)await p.locator('#workOn'+i).uncheck();assert.equal(await p.locator('#workSummary').textContent(),'근무 시간 없음');
 await p.getByRole('button',{name:'저장',exact:true}).click();assert.ok((await p.evaluate(()=>savedWork[1])).every(d=>!d.on));
 // 설정 창에서도 열 수 있다
 await p.locator('#settings').click();await p.locator('#modal').getByRole('button',{name:'근무 시간',exact:true}).click();assert.equal(await p.locator('#modal h2').textContent(),'근무 시간');
 assert.deepEqual(errors,[]);await browser.close();
 console.log(`PASS: ${count} work-hour layouts, weekly summary, overnight shift, start=end rejected, save + recompute, reopen, all off, from settings`);
})().catch(e=>{console.error(e);process.exit(1)});
