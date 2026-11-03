import fs from 'node:fs/promises';
const base=process.env.BASE_URL||'http://127.0.0.1:19082', out=process.env.EVIDENCE_DIR||'docs/verification/http-postgres';
await fs.mkdir(out,{recursive:true});const results=[];
class Client{
 cookies=new Map();csrf='';
 async req(route,method='GET',body,headers={}){
  const r=await fetch(base+route,{method,headers:{'Content-Type':'application/json',Cookie:[...this.cookies].map(([k,v])=>`${k}=${v}`).join('; '),...(method!=='GET'?{'X-CSRF-TOKEN':this.csrf}:{}),...headers},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(160000)});
  for(const c of r.headers.getSetCookie()){const p=c.split(';')[0],i=p.indexOf('=');this.cookies.set(p.slice(0,i),p.slice(i+1));}
  const text=await r.text();let data;try{data=JSON.parse(text)}catch{data=text};if(data?.code===0)data=data.data;return {status:r.status,data,headers:r.headers};
 }
 async login(user,password){this.csrf=(await this.req('/api/auth/csrf')).data.token;const before=this.cookies.get('DESK_SESSION');const r=await this.req('/api/auth/login','POST',{username:user,password});if(r.status!==200)throw Error('Login failed');this.csrf=(await this.req('/api/auth/csrf')).data.token;return before!==this.cookies.get('DESK_SESSION');}
}
function check(name,pass,detail=''){results.push({name,pass,detail});console.log(pass?'PASS':'FAIL',name)}
const c=new Client(), a=new Client(), stranger=new Client();
check('unauthenticated orders rejected',(await c.req('/api/orders')).status===401);
check('login without CSRF rejected',(await c.req('/api/auth/login','POST',{username:'customer',password:'DemoCustomer!2026'})).status===403);
check('login rotates session',await c.login('customer','DemoCustomer!2026'));
await a.login('agent','DemoAgent!2026');await stranger.login('customer2','DemoCustomer!2026');
const orders=await c.req('/api/orders');check('own orders only',orders.status===200&&orders.data.length===2&&orders.data.every(o=>o.owner_id==='customer'));
check('foreign order hidden',(await c.req('/api/orders/ZS20261030003')).status===404);
check('customer cannot read admin audit',(await c.req('/api/admin/audit')).status===403);
check('agent cannot manage knowledge',(await a.req('/api/knowledge')).status===403);
check('untrusted origin rejected',(await c.req('/api/orders','GET',undefined,{Origin:'https://untrusted.example'})).status===403);
check('trusted origin accepted',(await c.req('/api/orders','GET',undefined,{Origin:base})).status===200);
check('bad CSRF rejected',(await c.req('/api/tickets','POST',{}, {'X-CSRF-TOKEN':'invalid'})).status===403);
const key=crypto.randomUUID(),payload={kind:'QUESTION',subject:'HTTP 并发验收',description:'虚构测试：相同请求同时发送只能生成一张工单。'};
const concurrent=await Promise.all([c.req('/api/tickets','POST',payload,{'Idempotency-Key':key}),c.req('/api/tickets','POST',payload,{'Idempotency-Key':key})]);
const t=concurrent[0].data;check('concurrent creation is idempotent',concurrent.every(x=>x.status===200&&x.data.id===t.id));
check('same key different body rejected',(await c.req('/api/tickets','POST',{...payload,subject:'变更'}, {'Idempotency-Key':key})).status===409);
check('foreign ticket hidden',(await stranger.req('/api/tickets/'+t.id)).status===404);
const updates=await Promise.all([a.req('/api/tickets/'+t.id+'/events','POST',{version:0,status:'IN_PROGRESS',message:'并发接单 A'}),a.req('/api/tickets/'+t.id+'/events','POST',{version:0,status:'IN_PROGRESS',message:'并发接单 B'})]);
check('concurrent stale version rejected',updates.map(x=>x.status).sort().join(',')==='200,409');
check('customer cannot advance work status',(await c.req('/api/tickets/'+t.id+'/events','POST',{version:1,status:'RESOLVED',message:'越权状态测试'})).status===403);
const thread=(await c.req('/api/chat/threads','POST',{})).data;
check('foreign conversation hidden',(await stranger.req('/api/chat/threads/'+thread.id+'/messages')).status===404);
const streamed=await c.req('/api/chat/stream','POST',{threadId:thread.id,question:'查询订单 ZS20261030001'},{'Idempotency-Key':crypto.randomUUID()});
check('SSE phases and result delivered',streamed.status===200&&String(streamed.data).includes('event:phase')&&String(streamed.data).includes('event:result')&&String(streamed.data).includes('event:done'));
check('logout succeeds',(await c.req('/api/auth/logout','POST',{})).status===204);
check('session invalid after logout',(await c.req('/api/orders')).status===401);
const summary={timestamp:new Date().toISOString(),baseUrl:base,tests:results.length,passed:results.filter(x=>x.pass).length,results};await fs.writeFile(out+'/summary.json',JSON.stringify(summary,null,2));if(summary.passed!==summary.tests)process.exitCode=1;
