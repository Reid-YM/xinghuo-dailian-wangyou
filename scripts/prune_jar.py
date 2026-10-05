#!/usr/bin/env python3
import fnmatch
import os
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path


def main():
    if len(sys.argv) != 3:
        print('usage: prune_jar.py JAR PATTERNS_FILE', file=sys.stderr)
        return 2
    jar = Path(sys.argv[1])
    patterns = [line.strip() for line in Path(sys.argv[2]).read_text(encoding='utf-8').splitlines() if line.strip() and not line.startswith('#')]
    removed = []
    temp_fd, temp_name = tempfile.mkstemp(prefix=jar.name + '.', suffix='.tmp', dir=str(jar.parent))
    os.close(temp_fd)
    try:
        with zipfile.ZipFile(jar, 'r') as source, zipfile.ZipFile(temp_name, 'w') as target:
            for info in source.infolist():
                if any(fnmatch.fnmatch(info.filename, pattern) for pattern in patterns):
                    removed.append(info.filename)
                    continue
                target.writestr(info, source.read(info.filename))
        shutil.move(temp_name, jar)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)
    print(f'pruned {len(removed)} jar entries')
    for name in removed:
        print(f'  - {name}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())