"""Build a portable source+binary ZIP without local secrets or databases (Python 3.10+)."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib,json

root=Path(__file__).resolve().parent.parent
out=root/'dist';out.mkdir(exist_ok=True)
roots=['backend/src','backend/.mvn','frontend/src','frontend/public','frontend/tests','frontend/dist','docs','learning','scripts']
files=set()
for folder in roots:
    files.update(p for p in (root/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
for name in ['README.md','LICENSE','CONTRIBUTING.md','THIRD_PARTY.md','.gitattributes','.gitignore','.env.example','compose.yaml','compose.models.yaml','compose.gpu.yaml','backend/pom.xml','backend/mvnw','backend/mvnw.cmd','backend/Dockerfile','backend/.dockerignore','backend/target/service-desk-1.0.0.jar','frontend/package.json','frontend/package-lock.json','frontend/tsconfig.json','frontend/vite.config.ts','frontend/index.html','frontend/Dockerfile','frontend/.dockerignore','frontend/nginx.conf']:
    p=root/name
    if not p.exists():raise FileNotFoundError(name)
    files.add(p)
manifest={p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(files)}
bundle=out/'知序_JavaAI客服工单系统_v1.0.0_交付包.zip'
prefix='java-ai-service-desk-1.0.0/'
with ZipFile(bundle,'w',ZIP_DEFLATED,compresslevel=6) as z:
    for name in manifest:z.write(root/name,prefix+name)
    z.writestr(prefix+'MANIFEST.sha256.json',json.dumps(manifest,ensure_ascii=False,indent=2))
with ZipFile(bundle) as z:
    assert z.testzip() is None
    assert not any(n.endswith('/.env') or '/.runtime/' in n or '/node_modules/' in n for n in z.namelist())
    assert all(hashlib.sha256(z.read(prefix+n)).hexdigest()==h for n,h in manifest.items())
digest=hashlib.sha256(bundle.read_bytes()).hexdigest()
bundle.with_suffix('.zip.sha256').write_text(digest+'  '+bundle.name+'\n',encoding='utf-8')
print(json.dumps({'files':len(manifest),'bytes':bundle.stat().st_size,'sha256':digest},indent=2))
