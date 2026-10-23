const {chromium}=require('playwright'),assert=require('node:assert/strict'),path=require('node:path');
// 일기: 하루 하나, 이어 쓰기, 자동 저장. Android 브리지는 메모리 테스트 더블.
function mock(){
 const ok=o=>JSON.stringify({ok:true,...o});const key=ms=>{let d=new Date(ms);return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0')};
 let y=new Date();y.setDate(y.getDate()-1);
 window.entries={[key(+y)]:{text:'어제 일기 첫 줄\n둘째 줄',updated:+y}};window.saves=[];window.calls=[];
 window.Native={
  refresh:()=>ok({records:[],permission:true,minimum:240,timer:0}),tasks:()=>ok({open:[],finished:[],today:0}),
  dayReport:day=>{let e=entries[key(day)];return ok({day,permission:true,available:true,total:0,first:-1,last:-1,apps:[],spans:[],tasks:[],diary:e?e.text.split('\n')[0]:'',diaryLength:e?e.text.length:0})},
  diary:day=>ok({day,text:entries[day]?.text||'',updated:entries[day]?.updated||0}),
  saveDiary:(day,text)=>{saves.push([day,text]);if(!text.trim())delete entries[day];else entries[day]={text,updated:Date.now()};return ok({updated:Date.now()})},
  diaryList:()=>ok({entries:Object.keys(entries).sort().reverse().map(day=>({day,preview:entries[day].text.trim().split('\n')[0],length:entries[day].text.length,updated:entries[day].updated}))}),
  dashboard:p=>calls.push(['dashboard',p]),dashboardAt:(p,d)=>calls.push(['dashboardAt',p,d]),exit(){},closeNativeEditor(){},timer(){},permission(){}
 };
}
async function layout(p,label,size){const r=await p.evaluate(()=>{let c=document.querySelector('#content');let bad=[...document.querySelectorAll('button')].filter(b=>{if(!b.getClientRects().length||b.closest('dialog')&&!b.closest('dialog').open)return false;let r=b.getBoundingClientRect();return r.left<-.5||r.right>innerWidth+.5||r.top<-.5||r.bottom>innerHeight+.5});let t=document.querySelector('#diaryText');return {body:document.documentElement.scrollHeight>innerHeight+1,content:c.scrollHeight>c.clientHeight+2,bad:bad.map(b=>b.textContent),smallText:t?t.getBoundingClientRect().height<80:false}});assert.deepEqual(r,{body:false,content:false,bad:[],smallText:false},`${size} ${label} ${JSON.stringify(r)}`)}
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||undefined,args:['--no-sandbox']});let count=0;
 for(const [width,height] of [[412,850],[360,740],[320,568],[412,440],[800,600]]){
  const size=`${width}x${height}`,p=await browser.newPage({viewport:{width,height},timezoneId:'Asia/Seoul'});let errors=[];p.on('pageerror',e=>errors.push(e.message));
  await p.addInitScript(mock);await p.goto('file://'+path.resolve(__dirname,'../assets/index.html'));await p.evaluate(()=>startApp('diary'));await p.waitForTimeout(60);
  await layout(p,'diary',size);count++;if(width===412&&height===850)await p.screenshot({path:path.resolve(__dirname,'../output/qa-diary.png')});
  await p.evaluate(()=>openRoute('diary-list'));await layout(p,'diary list',size);count++;
  await p.evaluate(()=>openRoute('report'));await layout(p,'report with diary line',size);count++;
  assert.deepEqual(errors,[]);await p.close();
 }
 const p=await browser.newPage({viewport:{width:393,height:825},timezoneId:'Asia/Seoul'});let errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.addInitScript(mock);await p.goto('file://'+path.resolve(__dirname,'../assets/index.html'));await p.evaluate(()=>startApp('report'));await p.waitForTimeout(60);
 await p.getByRole('button',{name:'일기',exact:true}).click();assert.equal(await p.locator('#title').textContent(),'일기');assert.equal(await p.locator('#diaryText').inputValue(),'','today starts empty');
 await p.locator('#diaryText').click();assert.equal(await p.evaluate(()=>document.body.classList.contains('typing')),true,'nav hidden while typing');
 await p.locator('#diaryText').pressSequentially('아침에 산책');assert.equal(await p.evaluate(()=>saves.length),0,'not saved on every key');await p.waitForTimeout(1200);
 const today=await p.evaluate(()=>Logic.key(Date.now()));assert.deepEqual(await p.evaluate(()=>saves.at(-1)),[today,'아침에 산책'],'autosave after pause');assert.match(await p.locator('#diaryStatus').textContent(),/저장됨/);
 // 키보드가 열려 크기가 바뀌거나 새로고침되어도 입력칸·커서 유지
 await p.locator('#diaryText').type('. 날씨 맑음');const before=await p.evaluate(()=>{window.keep=document.querySelector('#diaryText');return 1});
 await p.setViewportSize({width:393,height:460});await p.waitForTimeout(150);await p.evaluate(()=>{lastRefresh=0;refresh()});await p.waitForTimeout(80);
 assert.equal(await p.evaluate(()=>document.querySelector('#diaryText')===window.keep&&document.activeElement===window.keep),true,'same textarea keeps focus');
 await p.setViewportSize({width:393,height:825});
 await p.getByRole('button',{name:'＋ 지금 시각'}).click();assert.match(await p.locator('#diaryText').inputValue(),/^아침에 산책\. 날씨 맑음\n\n\[\d\d:\d\d\] $/,'timestamp appended on a new paragraph');
 await p.locator('#diaryText').pressSequentially('점심 먹음');
 await p.evaluate(()=>document.activeElement.blur());await p.locator('nav button',{hasText:'시간대'}).click();
 assert.match(await p.evaluate(()=>entries[Logic.key(Date.now())].text),/점심 먹음$/,'leaving the tab saves immediately');assert.deepEqual(await p.evaluate(()=>calls.at(-1).slice(0,2)),['dashboard',0]);
 // 다시 들어오면 같은 글에 이어서
 await p.evaluate(()=>openRoute('diary'));assert.match(await p.locator('#diaryText').inputValue(),/점심 먹음$/);
 await p.getByRole('button',{name:'이전 날'}).click();assert.equal(await p.locator('#diaryText').inputValue(),'어제 일기 첫 줄\n둘째 줄','past day loads its own entry');assert.equal(await p.getByRole('button',{name:'＋ 지금 시각'}).count(),0,'no current-time stamp on past days');
 await p.locator('#diaryText').fill('어제 일기 첫 줄\n둘째 줄\n덧붙임');await p.getByRole('button',{name:'다음 날'}).click();assert.match(await p.evaluate(()=>{let d=new Date();d.setDate(d.getDate()-1);return entries[Logic.key(+d)].text}),/덧붙임$/,'changing day saves');
 await p.getByRole('button',{name:'모아보기'}).click();assert.equal(await p.locator('.diary-row').count(),2);assert.match(await p.locator('.diary-row').first().textContent(),/아침에 산책/);
 await p.locator('.diary-row').nth(1).click();assert.match(await p.locator('#diaryText').inputValue(),/덧붙임$/,'list row opens that day');
 await p.locator('#diaryText').fill('<img src=x onerror=alert(1)>');await p.waitForTimeout(1100);await p.getByRole('button',{name:'모아보기'}).click();assert.equal(await p.locator('img').count(),0,'preview escaped');
 await p.locator('.diary-row').nth(1).click();await p.locator('#diaryText').fill('   ');await p.waitForTimeout(1100);assert.equal(await p.evaluate(()=>Object.keys(entries).length),1,'blank entry deletes the day');
 await p.evaluate(()=>openRoute('report'));assert.match(await p.locator('.diary-line').textContent(),/아침에 산책/);await p.locator('.diary-line').click();assert.equal(await p.locator('#title').textContent(),'일기');assert.match(await p.locator('#diaryText').inputValue(),/^아침에 산책/,'report line opens that day');
 await p.screenshot({path:path.resolve(__dirname,'../output/qa-diary-flow.png')});
 assert.deepEqual(errors,[]);await browser.close();
 console.log(`PASS: ${count} diary layouts, one entry per day, autosave after pause, save on leave/day change, focus kept across resize/refresh, time stamp, past days, list, escaping, blank deletes, report link`);
})().catch(e=>{console.error(e);process.exit(1)});
