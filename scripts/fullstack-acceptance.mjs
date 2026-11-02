// Run only against a disposable demo deployment. Creates fictional test records.
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const base = process.env.BASE_URL || 'http://127.0.0.1:19084';
const out = process.env.EVIDENCE_DIR || '.runtime/fullstack-live';
const results = [];
class Client {
  cookies = new Map(); csrf = '';
  async request(route, method = 'GET', body, headers = {}) {
    const r = await fetch(base + '/api' + route, {method, headers: {
      'Content-Type': 'application/json', Cookie: [...this.cookies].map(([k,v]) => `${k}=${v}`).join('; '),
      ...(method !== 'GET' ? {'X-CSRF-TOKEN': this.csrf} : {}), ...headers
    }, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(150000)});
    for (const cookie of r.headers.getSetCookie()) {
      const first = cookie.split(';')[0], i = first.indexOf('='); this.cookies.set(first.slice(0,i),first.slice(i+1));
    }
    const text = await r.text(); let data;
    try { data = JSON.parse(text); } catch { data = text; }
    return {status: r.status, data};
  }
  async login(username, password) {
    this.csrf = (await this.request('/auth/csrf')).data.token;
    assert.equal((await this.request('/auth/login','POST',{username,password})).status,200);
    this.csrf = (await this.request('/auth/csrf')).data.token;
  }
}
function check(name, pass) { results.push({name,pass}); console.log(pass ? 'PASS' : 'FAIL', name); }
const admin = new Client(), customer = new Client(), second = new Client();
await admin.login('admin', process.env.ADMIN_PASSWORD || 'DemoAdmin!2026');
await customer.login('customer', process.env.CUSTOMER_PASSWORD || 'DemoCustomer!2026');
await second.login('customer2', process.env.CUSTOMER_PASSWORD || 'DemoCustomer!2026');
const first = await customer.request('/orders/page?size=1');
check('typed order page reports exact total', first.status===200 && first.data.total===2 && first.data.items.length===1);
const next = await customer.request('/orders/page?size=1&page=2');
check('second page is distinct', next.status===200 && next.data.items[0].id!==first.data.items[0].id);
check('paged orders retain owner isolation', (await second.request('/orders/page')).data.items.every(o=>o.owner_id==='customer2'));
check('customer cannot submit index job', (await customer.request('/knowledge/jobs','POST',{}, {'Idempotency-Key':crypto.randomUUID()})).status===403);
const payload = {name:'全栈隔离验收商品',category:'测试',priceCents:100,description:'虚构验收数据',active:true};
const created = await admin.request('/admin/products','POST',payload);
assert.equal(created.status,200);
const id = created.data.id;
await customer.request('/products');
await customer.request('/products');
check('product cache path serves new product', (await customer.request('/products')).data.some(p=>p.id===id && p.price_cents===100));
assert.equal((await admin.request('/admin/products/'+id,'PUT',{...payload,priceCents:200})).status,200);
check('product update switches cache generation', (await customer.request('/products')).data.some(p=>p.id===id && p.price_cents===200));
await admin.request('/admin/products/'+id,'PUT',{...payload,active:false});
check('deactivated product disappears immediately', !(await customer.request('/products')).data.some(p=>p.id===id));
const key = crypto.randomUUID();
const documents = (await admin.request('/knowledge')).data;
const documentId = documents.find(d=>d.active && d.chunks>0)?.id;
assert.ok(documentId, 'An active demo knowledge document is required');
const submitted = await admin.request('/knowledge/jobs','POST',{documentId}, {'Idempotency-Key':key});
assert.equal(submitted.status,202);
check('async indexing accepts durable job', submitted.data.state==='PENDING' || submitted.data.state==='RUNNING');
check('index submission idempotency', (await admin.request('/knowledge/jobs','POST',{documentId}, {'Idempotency-Key':key})).data.id===submitted.data.id);
let job = submitted.data;
const deadline = Date.now()+180000;
while (!['SUCCEEDED','FAILED'].includes(job.state) && Date.now()<deadline) {
  await new Promise(resolve=>setTimeout(resolve,1500));
  job=(await admin.request('/knowledge/jobs/'+job.id)).data;
}
check('real worker completes real model embeddings', job.state==='SUCCEEDED' && job.indexed===job.total && job.total>0);
check('completed index job cannot be retried', (await admin.request('/knowledge/jobs/'+job.id+'/retry','POST',{})).status===409);
const marker = 'isolation-'+crypto.randomUUID();
const foreign = await second.request('/tickets','POST',{kind:'QUESTION',subject:marker,description:'虚构客户隔离验收'}, {'Idempotency-Key':crypto.randomUUID()});
assert.equal(foreign.status,200);
const visible = (await second.request('/tickets/page?query='+marker)).data;
const hidden = (await customer.request('/tickets/page?query='+marker)).data;
check('customer paged tickets remain private', visible.total===1 && visible.items[0].id===foreign.data.id && hidden.total===0 && (await customer.request('/tickets/'+foreign.data.id)).status===404);
const csv = await customer.request('/tickets/export');
check('CSV download is available', csv.status===200 && typeof csv.data==='string' && csv.data.includes('工单'));
const hiddenMetrics = await fetch(base+'/actuator/prometheus');
check('public frontend does not expose metrics', !(hiddenMetrics.headers.get('Content-Type')||'').includes('openmetrics') && !(await hiddenMetrics.text()).includes('jvm_memory_used_bytes'));
await fs.mkdir(out,{recursive:true});
const summary={timestamp:new Date().toISOString(),baseUrl:base,tests:results.length,passed:results.filter(r=>r.pass).length,indexJob:job,results};
await fs.writeFile(out+'/summary.json',JSON.stringify(summary,null,2));
if (summary.passed!==summary.tests) process.exitCode=1;
