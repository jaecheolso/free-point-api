"""
erd.mmd 의 엔티티 정의(컬럼)를 읽어, 테이블 배치와 관계선을 고정한 ERD HTML 을 만든다.
배치와 관계선은 erd-template.html 에 정의되어 있다.

erd.png 재생성 (src/main/resources/erd 에서 실행, Node.js 필요):
    python3 erd-build.py erd.mmd erd-template.html /tmp/erd.html
    npm install --prefix /tmp/erd-tools puppeteer    # 저장소 밖에 설치
    NODE_PATH=/tmp/erd-tools/node_modules node erd-render.js /tmp/erd.html erd.png
"""
import json
import re
import sys

src = open(sys.argv[1], encoding="utf-8").read()
entities = {}
for name, body in re.findall(r"^\s{4}(\w+) \{\n(.*?)\n\s{4}\}", src, re.S | re.M):
    cols = []
    for line in body.strip().splitlines():
        m = re.match(r'\s*(\w+)\s+(\w+)\s*([A-Z, ]*?)\s*(?:"(.*)")?\s*$', line)
        typ, col, key, note = m.groups()
        cols.append({"type": typ, "name": col, "key": key.replace(" ", ""), "note": note or ""})
    entities[name] = cols

template = open(sys.argv[2], encoding="utf-8").read()
open(sys.argv[3], "w", encoding="utf-8").write(template.replace("/*ENTITIES*/", json.dumps(entities, ensure_ascii=False)))
print(", ".join(f"{k}({len(v)})" for k, v in entities.items()))
