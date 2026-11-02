"""Build a source + precompiled delivery ZIP; never include local credentials or data."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from hashlib import sha256
import json
import argparse
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--name',default='java-ai-service-desk-1.0.0-delivery.zip')
args=parser.parse_args()
if Path(args.name).name!=args.name or not args.name.endswith('.zip'):
    parser.error('--name must be a ZIP filename without directory components')
root=Path(__file__).resolve().parents[1]
files=[]
for directory in ('backend/src','backend/.mvn','frontend/src','frontend/public','frontend/tests','frontend/dist','scripts','docs','learning','.vscode'):
    files.extend(p for p in (root/directory).rglob('*') if p.is_file())
for name in ('README.md','LICENSE','CONTRIBUTING.md','THIRD_PARTY.md','.gitattributes','.gitignore','.env.example','compose.yaml','compose.models.yaml','compose.gpu.yaml','backend/pom.xml','backend/mvnw','backend/mvnw.cmd','backend/Dockerfile','backend/target/service-desk-1.0.0.jar','frontend/package.json','frontend/package-lock.json','frontend/index.html','frontend/tsconfig.json','frontend/vite.config.ts','frontend/Dockerfile','frontend/nginx.conf'):
    path=root/name
    if not path.is_file():raise SystemExit('Missing required delivery file: '+name)
    files.append(path)
files=sorted(set(files))
manifest={p.relative_to(root).as_posix():sha256(p.read_bytes()).hexdigest() for p in files}
out=root/'dist';out.mkdir(exist_ok=True)
archive=out/args.name
prefix='java-ai-service-desk/'
with ZipFile(archive,'w',ZIP_DEFLATED) as z:
    for path in files:z.write(path,prefix+path.relative_to(root).as_posix())
    z.writestr(prefix+'MANIFEST.sha256',''.join(digest+'  '+name+'\n' for name,digest in manifest.items()))
with ZipFile(archive) as z:
    assert z.testzip() is None
    assert all(sha256(z.read(prefix+name)).hexdigest()==digest for name,digest in manifest.items())
    assert not any(p.endswith('/.env') or '/node_modules/' in p or '/.runtime/' in p or '/.tools/' in p for p in z.namelist())
digest=sha256(archive.read_bytes()).hexdigest()
(out/(archive.name+'.sha256')).write_text(digest+'  '+archive.name+'\n',encoding='utf-8')
result={'archive':str(archive),'files':len(manifest),'bytes':archive.stat().st_size,'sha256':digest,'all_entries_verified':True,'local_credentials_excluded':True}
(out/(archive.stem+'-verification.json')).write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result,indent=2))
