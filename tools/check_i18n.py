from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ES = (ROOT / "src" / "i18n" / "es.ts").read_text(encoding="utf-8")
ES_KEYS = set(re.findall(r"^  '([^']+)':", ES, re.M))

KEY_PATTERNS = [
    re.compile(r"\bt\(\s*'([^']+)'"),
    re.compile(r"\bt\(\s*\"([^\"]+)\""),
    re.compile(r"labelKey:\s*'([^']+)'"),
    re.compile(r"nameKey:\s*'([^']+)'"),
    re.compile(r"descKey:\s*'([^']+)'"),
    re.compile(r"rulesKey:\s*'([^']+)'"),
    re.compile(r"titleKey:\s*'([^']+)'"),
    re.compile(r"rewardKey:\s*'([^']+)'"),
    re.compile(r"reasonKey:\s*'([^']+)'"),
]
USED: set[str] = set()
for path in (ROOT / "src").rglob("*.ts*"):
    if path.name == "es.ts":
        continue
    text = path.read_text(encoding="utf-8")
    for pattern in KEY_PATTERNS:
        USED.update(pattern.findall(text))

missing = sorted(k for k in USED if k not in ES_KEYS)
unused = sorted(k for k in ES_KEYS if k not in USED)
print("missing", len(missing))
for key in missing:
    print("  ", key)
print("unused", len(unused))
for key in unused[:80]:
    print("  ", key)

if missing:
    raise SystemExit(1)
