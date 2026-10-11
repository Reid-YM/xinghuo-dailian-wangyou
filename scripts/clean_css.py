#!/usr/bin/env python3
import re
import sys
from pathlib import Path

BANNED = ('.loadout', '.ai-chat', '#aichat', '[data-ai-prompt]')


def strip_rules(css, banned):
    out = []
    i = 0
    n = len(css)
    while i < n:
        open_at = css.find('{', i)
        close_at = css.find('}', i)
        if open_at == -1:
            out.append(css[i:])
            break
        if close_at != -1 and close_at < open_at:
            out.append(css[i:close_at + 1])
            i = close_at + 1
            continue
        prelude = css[i:open_at]
        depth = 1
        p = open_at + 1
        while p < n and depth:
            if css[p] == '{':
                depth += 1
            elif css[p] == '}':
                depth -= 1
            p += 1
        if depth:
            out.append(css[i:])
            break
        block = css[open_at + 1:p - 1]
        lower = prelude.lower()
        if any(marker in lower for marker in banned):
            i = p
            continue
        stripped = prelude.lstrip().lower()
        if stripped.startswith(('@media', '@supports', '@layer')):
            block = strip_rules(block, banned)
        out.append(prelude + '{' + block + '}')
        i = p
    return ''.join(out)


def main():
    if len(sys.argv) != 2:
        print('usage: clean_css.py FILE', file=sys.stderr)
        return 2
    path = Path(sys.argv[1])
    css = path.read_text(encoding='utf-8')
    css = css.replace('home-loadout-entry-copy', 'home-tool-entry-copy')
    css = css.replace('home-loadout-entry', 'home-tool-entry')
    css = re.sub(r'/\*.*?\*/', '', css, flags=re.S)
    path.write_text(strip_rules(css, BANNED), encoding='utf-8')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())