import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const children=[];
let closing=false;
function stop(code=0) {
  if(closing)return; closing=true;
  for(const child of children)child.kill();
  process.exit(code);
}
process.on('SIGINT',()=>stop());
process.on('SIGTERM',()=>stop());
function run(command,args,env,cwd=root) {
  const child=spawn(command,args,{cwd,env:{...process.env,...env},stdio:'inherit',windowsHide:true});
  children.push(child);
  child.on('error',error=>{console.error(error.message);stop(1)});
  child.on('exit',code=>{if(!closing)stop(code||1)});
  return child;
}
run('java',['-Dfile.encoding=UTF-8','-jar',path.join(root,'backend/desk-app/target/service-desk-1.0.0.jar')],{
  PORT:'21980', BIND_ADDRESS:'127.0.0.1',
  DATABASE_URL:'jdbc:h2:mem:browser_tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1',
  DATABASE_USER:'sa', DATABASE_PASSWORD:'', SPRING_PROFILES_ACTIVE:'',
  APP_DEMO:'true', AI_ENABLED:'false', INDEX_WORKER_ENABLED:'false',
  REDIS_ENABLED:'false', RABBITMQ_ENABLED:'false',
  ADMIN_PASSWORD:'DemoAdmin!2026', CUSTOMER_PASSWORD:'DemoCustomer!2026', AGENT_PASSWORD:'DemoAgent!2026',
  APP_ORIGIN:'http://127.0.0.1:21981', DEPLOYMENT_MODE:'local', COOKIE_SECURE:'false',
});
let ready=false;
for(let i=0;i<180;i++){
  try{const response=await fetch('http://127.0.0.1:21980/actuator/health');if(response.ok){ready=true;break}}catch{}
  await new Promise(resolve=>setTimeout(resolve,500));
}
if(!ready){console.error('Isolated browser backend did not become healthy');stop(1)}
run(process.execPath,['node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','21981','--strictPort'],{
  VITE_API_TARGET:'http://127.0.0.1:21980',
},path.join(root,'frontend'));
