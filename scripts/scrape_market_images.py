#!/usr/bin/env python3
import csv
import hashlib
import json
import mimetypes
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from difflib import SequenceMatcher
from html import unescape
from pathlib import Path

USER_AGENT = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124 Safari/537.36 XinghuoMarketBot/1.0'
STEAM_APPS = {
    'APEX': 1172470, 'CS2': 730, 'PUBG': 578080, 'R6_SIEGE': 359550,
    'MARVEL_RIVALS': 2767030, 'DELTA_FORCE': 2507950, 'DESTINY2': 1085660,
    'WARFRAME': 230410, 'ARC_RAIDERS': 1808500, 'DIABLO4': 2344520,
    'ZZZ': 4162040, 'WUTHERING_WAVES': 3513350, 'GTA5_ONLINE': 271590,
    'MH_WILDS': 2246340, 'EA_FC26': 3405690, 'STREET_FIGHTER6': 1364780,
    'TEKKEN8': 1778820, 'NARAKA': 1203220,
}
DIRECT_IMAGES = {
    'CODMW4': ('Call of Duty Official', 'https://imgs.callofduty.com/content/dam/atvi/callofduty/cod-touchui/kronos/common/social-share/social-share-image.jpg', 'https://www.callofduty.com/modernwarfare'),
    'COD_WARZONE': ('Call of Duty Warzone Official', 'https://imgs.callofduty.com/content/dam/atvi/callofduty/cod-touchui/warzone2/blackops7/evergreen/WZ_LP-Update_Meta.webp', 'https://www.callofduty.com/warzone'),
    'VALORANT': ('Riot Games / VALORANT API', 'https://media.valorant-api.com/agents/add6443a-41bd-e414-f6ad-e58d267f4e95/fullportrait.png', 'https://playvalorant.com/en-us/'),
    'LOL': ('Riot Data Dragon', 'https://ddragon.leagueoflegends.com/cdn/img/champion/splash/Aatrox_0.jpg', 'https://www.leagueoflegends.com/'),
    'FORTNITE': ('Epic Games Store', 'https://cdn2.unrealengine.com/en-fn-og-42-10-c1sx-egs-launcher-blade-2560x1440-2560x1440-319ff81b274e.jpg', 'https://store.epicgames.com/en-US/p/fortnite'),
    'ZZZ': ('Steam Store', 'https://shared.cdn.queniuqe.com/store_item_assets/steam/apps/4162040/34da5c8202eb188fc2fefd386a37bd31c0e402cc/capsule_616x353_alt_assets_1.jpg?t=1788905636', 'https://store.steampowered.com/app/4162040/'),
    'WUTHERING_WAVES': ('Steam Store', 'https://shared.cdn.queniuqe.com/store_item_assets/steam/apps/3513350/dd1bfd403173b32367807e955ef33da7682bfee1/capsule_616x353.jpg?t=1787182292', 'https://store.steampowered.com/app/3513350/'),
    'EA_FC26': ('Steam Store', 'https://shared.cdn.queniuqe.com/store_item_assets/steam/apps/3405690/7a4a2467300194840eb2116c2126c2d2422b4e12/capsule_616x353.jpg?t=1789751159', 'https://store.steampowered.com/app/3405690/'),
}
OFFICIAL_PAGES = {
    'CODMW4': ('Call of Duty Official Store', 'https://www.callofduty.com/modernwarfare'),
    'COD_WARZONE': ('Call of Duty Warzone', 'https://www.callofduty.com/warzone'),
    'VALORANT': ('VALORANT Official', 'https://playvalorant.com/en-us/'),
    'FORTNITE': ('Fortnite Official', 'https://www.fortnite.com/'),
    'LOL': ('League of Legends Official', 'https://www.leagueoflegends.com/en-us/'),
    'WOW': ('World of Warcraft Official', 'https://worldofwarcraft.blizzard.com/en-us/'),
    'GENSHIN': ('Genshin Impact Official', 'https://ys.mihoyo.com/'),
    'HONKAI_SR': ('Honkai: Star Rail Official', 'https://sr.mihoyo.com/'),
    'HONOR_KINGS': ('Honor of Kings Official', 'https://pvp.qq.com/'),
}

def request(url, timeout=25):
    req = urllib.request.Request(url, headers={'User-Agent': USER_AGENT, 'Accept': '*/*'})
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return response.read(), response.headers, response.geturl()

def get_json(url):
    data, _, _ = request(url)
    return json.loads(data.decode('utf-8', 'replace'))

def normalize(value):
    return re.sub(r'[^a-z0-9\u4e00-\u9fff]+', '', (value or '').lower())

def similarity(left, right):
    a, b = normalize(left), normalize(right)
    if not a or not b:
        return 0.0
    if a in b or b in a:
        return 0.92
    return SequenceMatcher(None, a, b).ratio()

def steam_name(appid):
    data = get_json(f'https://store.steampowered.com/api/appdetails?appids={appid}&l=schinese&cc=cn')
    entry = data.get(str(appid), {})
    return entry.get('data', {}).get('name') if entry.get('success') else None

def pick_itunes(term, expected_names):
    url = 'https://itunes.apple.com/search?' + urllib.parse.urlencode({'term': term, 'entity': 'software', 'country': 'cn', 'limit': 10})
    data = get_json(url)
    best, best_score = None, 0.0
    for item in data.get('results', []):
        score = max(similarity(item.get('trackName', ''), name) for name in expected_names)
        if score > best_score:
            best, best_score = item, score
    if best and best_score >= 0.55:
        return {'source_name': 'Apple App Store', 'source_page_url': best.get('trackViewUrl'), 'source_url': best.get('artworkUrl512') or best.get('artworkUrl100'), 'matched_name': best.get('trackName'), 'score': best_score}
    return None
def pick_steam(code, names):
    appid = STEAM_APPS.get(code)
    if appid:
        name = steam_name(appid) or names[0]
        return {'source_name': 'Steam Store', 'source_page_url': f'https://store.steampowered.com/app/{appid}/', 'source_url': f'https://cdn.cloudflare.steamstatic.com/steam/apps/{appid}/header.jpg', 'matched_name': name, 'score': 1.0}
    query = names[1] if names[1] and not re.search(r'[\u4e00-\u9fff]', names[1]) else names[0]
    data = get_json('https://store.steampowered.com/api/storesearch/?' + urllib.parse.urlencode({'term': query, 'l': 'schinese', 'cc': 'cn'}))
    best, best_score = None, 0.0
    for item in data.get('items', []):
        score = max(similarity(item.get('name', ''), name) for name in names)
        if score > best_score:
            best, best_score = item, score
    if best and best_score >= 0.72:
        appid = best['id']
        return {'source_name': 'Steam Store', 'source_page_url': f'https://store.steampowered.com/app/{appid}/', 'source_url': f'https://cdn.cloudflare.steamstatic.com/steam/apps/{appid}/header.jpg', 'matched_name': best.get('name'), 'score': best_score}
    return None

def og_image(page_url):
    html, _, final_url = request(page_url)
    text = html.decode('utf-8', 'replace')
    patterns = [
        r'<meta[^>]+property=["\']og:image["\'][^>]+content=["\']([^"\']+)',
        r'<meta[^>]+content=["\']([^"\']+)["\'][^>]+property=["\']og:image["\']',
        r'<meta[^>]+name=["\']twitter:image["\'][^>]+content=["\']([^"\']+)',
    ]
    for pattern in patterns:
        match = re.search(pattern, text, re.I)
        if match:
            return urllib.parse.urljoin(final_url, unescape(match.group(1)))
    return None

def choose_source(row):
    code, name, name_en = row[0], row[1], row[2] or row[1]
    names = [name, name_en]
    direct = DIRECT_IMAGES.get(code)
    if direct:
        return {'source_name': direct[0], 'source_page_url': direct[2], 'source_url': direct[1], 'matched_name': name, 'score': 1.0}
    try:
        steam = pick_steam(code, names)
        if steam:
            return steam
    except Exception as exc:
        print(f'{code}: Steam fallback ({exc})', file=sys.stderr)
    try:
        itunes = pick_itunes(name_en if name_en and not re.search(r'[\u4e00-\u9fff]', name_en) else name, names)
        if itunes:
            return itunes
    except Exception as exc:
        print(f'{code}: iTunes fallback ({exc})', file=sys.stderr)
    official = OFFICIAL_PAGES.get(code)
    if official:
        try:
            image = og_image(official[1])
            if image:
                return {'source_name': official[0], 'source_page_url': official[1], 'source_url': image, 'matched_name': name, 'score': 0.8}
        except Exception as exc:
            print(f'{code}: official page fallback ({exc})', file=sys.stderr)
    return None

def extension_for(url, content_type):
    ext = Path(urllib.parse.urlparse(url).path).suffix.lower()
    if ext in {'.jpg', '.jpeg', '.png', '.webp'}:
        return '.jpg' if ext == '.jpeg' else ext
    guessed = mimetypes.guess_extension((content_type or '').split(';')[0].strip())
    if guessed in {'.jpg', '.jpeg', '.png', '.webp'}:
        return '.jpg' if guessed == '.jpeg' else guessed
    return '.jpg'
def main():
    if len(sys.argv) < 5:
        print('usage: scrape_market_images.py GAMES_TSV OUTPUT_ROOT JSON_OUT SQL_OUT', file=sys.stderr)
        return 2
    games_path, output_root, json_out, sql_out = map(Path, sys.argv[1:5])
    cover_dir = output_root / 'assets' / 'covers' / 'market'
    cover_dir.mkdir(parents=True, exist_ok=True)
    rows = list(csv.reader(games_path.open(encoding='utf-8'), delimiter='\t'))
    results, failures = [], []
    for row in rows:
        if len(row) < 3 or not row[0]:
            continue
        code = row[0].strip()
        source = choose_source(row)
        if not source or not source.get('source_url'):
            failures.append({'code': code, 'name': row[1]})
            print(f'{code}: no market image', file=sys.stderr)
            continue
        try:
            data, headers, final_url = request(source['source_url'])
            if len(data) < 4096:
                raise ValueError(f'image too small: {len(data)} bytes')
            ext = extension_for(final_url, headers.get('Content-Type'))
            local_name = code.lower() + ext
            local_file = cover_dir / local_name
            local_file.write_bytes(data)
            local_path = f'/assets/covers/market/{local_name}'
            digest = hashlib.sha256(data).hexdigest()
            results.append({
                'game_code': code, 'title': row[1], 'source_name': source['source_name'],
                'source_page_url': source.get('source_page_url'), 'source_url': final_url,
                'matched_name': source.get('matched_name'), 'score': round(float(source.get('score') or 0), 3),
                'local_path': local_path, 'checksum_sha256': digest, 'bytes': len(data),
            })
            print(f'{code}: {source["source_name"]} -> {local_path}')
        except Exception as exc:
            failures.append({'code': code, 'name': row[1], 'error': str(exc)})
            print(f'{code}: download failed: {exc}', file=sys.stderr)
        time.sleep(0.12)
    json_out.write_text(json.dumps({'generated_at': int(time.time()), 'images': results, 'failures': failures}, ensure_ascii=False, indent=2), encoding='utf-8')
    lines = ['-- V31 generated by scrape_market_images.py']
    for item in results:
        game_code = item['game_code'].replace("'", "''")
        local_path = item['local_path'].replace("'", "''")
        source_name = item['source_name'].replace("'", "''")
        source_url = item['source_url'].replace("'", "''")
        source_page = (item['source_page_url'] or '').replace("'", "''")
        digest = item['checksum_sha256']
        lines.append(f"UPDATE game_catalog SET cover_image='{local_path}' WHERE code='{game_code}';")
        lines.append(f"UPDATE services SET cover_image='{local_path}' WHERE game_code='{game_code}' AND is_active=1;")
    if not results:
        lines.append('-- no images were downloaded; existing covers are preserved')
    sql_out.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(f'complete: {len(results)} images, {len(failures)} failures')
    return 0 if results else 1

if __name__ == '__main__':
    raise SystemExit(main())
