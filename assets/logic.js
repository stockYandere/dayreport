/* Calendar days follow local time, including daylight saving changes. */
const Logic={
 day(ms){let d=new Date(ms);d.setHours(0,0,0,0);return +d},
 shift(ms,n){let d=new Date(ms);d.setDate(d.getDate()+n);return +d},
 key(ms){let d=new Date(ms);return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0')},
 duration(ms){let m=Math.round(ms/60000);return m>=60?Math.floor(m/60)+'시간 '+m%60+'분':m+'분'},
 time(ms){return new Date(ms).toLocaleTimeString('ko-KR',{hour:'2-digit',minute:'2-digit',hour12:false})},
 circular(values){if(!values.length)return null;let s=0,c=0;for(const m of values){s+=Math.sin(m/1440*Math.PI*2);c+=Math.cos(m/1440*Math.PI*2)}if(Math.hypot(s,c)/values.length<.3)return null;return (Math.atan2(s,c)/(2*Math.PI)*1440+1440)%1440},
 averageTime(v){if(v===null)return '다양함';let m=Math.round(v)%1440;return String(Math.floor(m/60)).padStart(2,'0')+':'+String(m%60).padStart(2,'0')},
 stats(records,start,end){let days=new Map();for(const r of records){if(r.state!=='confirmed'||r.end<start||r.end>=end)continue;let k=this.key(r.end),d=days.get(k)||{total:0,main:null};d.total+=r.end-r.start;if(r.kind!=='nap'&&(!d.main||r.end-r.start>d.main.end-d.main.start))d.main=r;days.set(k,d)}let sum=0,bed=[],wake=[];for(const d of days.values()){sum+=d.total;if(d.main){let a=new Date(d.main.start),b=new Date(d.main.end);bed.push(a.getHours()*60+a.getMinutes());wake.push(b.getHours()*60+b.getMinutes())}}return {count:days.size,avg:days.size?sum/days.size:0,bed:this.circular(bed),wake:this.circular(wake),hasMain:bed.length>0}},
 segments(records,day){let end=this.shift(day,1),out=[];for(const r of records){let s=Math.max(day,r.start),e=Math.min(end,r.end);if(e>s)out.push({...r,left:(s-day)/(end-day)*100,width:(e-s)/(end-day)*100})}return out},
 input(ms){let d=new Date(ms);return this.key(ms)+'T'+String(d.getHours()).padStart(2,'0')+':'+String(d.getMinutes()).padStart(2,'0')},
 validate(start,end,now){return Number.isFinite(start)&&Number.isFinite(end)&&start>0&&end>start&&end-start<=86400000&&end<=now+60000}
};
if(typeof module!=='undefined')module.exports=Logic;
