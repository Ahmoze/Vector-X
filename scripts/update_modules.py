import urllib.request
import json
import re
import os
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
gh_headers = {'User-Agent': USER_AGENT}
if GITHUB_TOKEN:
    gh_headers['Authorization'] = f'Bearer {GITHUB_TOKEN}'

print("Loading existing modules.json cache...")
cache_modules = {}
for source_label, loader in (
    ("remote gh-pages", lambda: json.loads(urllib.request.urlopen(
        urllib.request.Request('https://raw.githubusercontent.com/Ahmoze/Vector-X/gh-pages/modules.json', headers={'User-Agent': USER_AGENT}),
        timeout=20
    ).read().decode('utf-8'))),
    ("local scripts/modules.json", lambda: json.load(open('scripts/modules.json', 'r', encoding='utf-8')) if os.path.exists('scripts/modules.json') else {}),
):
    try:
        cache_data = loader()
        raw_list = cache_data.get('modules', []) if isinstance(cache_data, dict) else cache_data
        for m in (raw_list or []):
            if isinstance(m, dict) and m.get('name'):
                cache_modules[m['name']] = m
        print(f"Loaded {len(cache_modules)} total modules after reading {source_label}.")
    except Exception as e:
        print(f"Cache source {source_label} unavailable: {e}")

print("Fetching paginated module list from modules.lsposed.org...")
page = 1
total_pages = 1
raw_items = []

while page <= total_pages:
    print(f"Fetching page {page}/{total_pages}...")
    url = f'https://modules.lsposed.org/module-list/{page}.json'
    req = urllib.request.Request(url, headers={'User-Agent': USER_AGENT})
    try:
        data = json.loads(urllib.request.urlopen(req, timeout=20).read().decode('utf-8'))
        if page == 1:
            total_pages = data.get('pageCount', 1)
        raw_items.extend(data.get('modules', []))
    except Exception as e:
        print(f"Failed to fetch page {page}: {e}")
        break
    page += 1


def normalize_releases(releases_list):
    normalized = []
    for r in (releases_list or []):
        if not isinstance(r, dict):
            continue
        assets = []
        for a in (r.get('releaseAssets') or []):
            if not isinstance(a, dict):
                continue
            dl_url = a.get('originalDownloadUrl') or a.get('downloadUrl') or ''
            name = a.get('name') or ''
            if not dl_url:
                continue
            asset_obj = {
                "name": name,
                "downloadUrl": dl_url,
                "size": a.get('size', 0),
                "downloadCount": a.get('downloadCount', 0),
            }
            if a.get('contentType'):
                asset_obj["contentType"] = a['contentType']
            if a.get('originalDownloadUrl'):
                asset_obj["originalDownloadUrl"] = a['originalDownloadUrl']
            assets.append(asset_obj)
        if not assets:
            continue
        rel_obj = {
            "name": r.get('name') or r.get('tagName') or '',
            "tagName": r.get('tagName') or r.get('name') or '',
            "createdAt": r.get('createdAt') or '',
            "publishedAt": r.get('publishedAt') or r.get('createdAt') or '',
            "isPrerelease": bool(r.get('isPrerelease', False)),
            "releaseAssets": assets,
        }
        if r.get('url'):
            rel_obj["url"] = r['url']
        desc_html = r.get('descriptionHTML')
        if desc_html:
            if len(desc_html) <= 15000:
                rel_obj["descriptionHTML"] = desc_html
            else:
                rel_obj["descriptionHTML"] = desc_html[:15000] + "\n<p>...</p>"
        normalized.append(rel_obj)

    if len(normalized) > 3:
        top = normalized[:3]
        if all(x.get('isPrerelease') for x in top):
            for extra in normalized[3:]:
                if not extra.get('isPrerelease'):
                    top.append(extra)
                    break
        return top
    return normalized


def fetch_github_releases(repo_url):
    if not repo_url:
        return []
    m = re.match(r'https://github\.com/([^/]+)/([^/#?]+)', repo_url.strip().rstrip('/'))
    if not m:
        return []
    owner, repo = m.groups()
    if repo.endswith('.git'):
        repo = repo[:-4]
    api_url = f'https://api.github.com/repos/{owner}/{repo}/releases?per_page=10'
    api_req = urllib.request.Request(api_url, headers=gh_headers)
    resp = urllib.request.urlopen(api_req, timeout=20)
    releases_data = json.loads(resp.read().decode('utf-8'))
    rels = []
    for r in releases_data:
        if r.get('draft'):
            continue
        assets = []
        for a in r.get('assets', []):
            aname = a.get('name', '')
            if aname.endswith('.apk') or aname.endswith('.zip'):
                assets.append({
                    "name": aname,
                    "downloadUrl": a.get('browser_download_url', ''),
                    "originalDownloadUrl": a.get('browser_download_url', ''),
                    "size": a.get('size', 0),
                    "downloadCount": a.get('download_count', 0),
                    "contentType": a.get('content_type', 'application/vnd.android.package-archive'),
                })
        if not assets:
            continue
        rel_entry = {
            "name": r.get('name') or r.get('tag_name', ''),
            "url": r.get('html_url', ''),
            "tagName": r.get('tag_name', ''),
            "createdAt": r.get('created_at', ''),
            "publishedAt": r.get('published_at') or r.get('created_at', ''),
            "isPrerelease": bool(r.get('prerelease', False)),
            "releaseAssets": assets,
        }
        body_text = (r.get('body') or '').strip()
        if body_text:
            escaped = (
                body_text.replace('&', '&amp;')
                .replace('<', '&lt;')
                .replace('>', '&gt;')
                .replace('\r\n', '<br>')
                .replace('\n', '<br>')
            )
            rel_entry["descriptionHTML"] = f"<div>{escaped[:15000]}</div>"
        rels.append(rel_entry)
        if len(rels) >= 3 and any(not x.get('isPrerelease') for x in rels):
            break
    return rels


def process_module(item):
    pkg_name = item.get('name')
    if not pkg_name:
        return None, False

    mod = {
        "name": pkg_name,
        "description": item.get('description', ''),
        "summary": item.get('summary', ''),
        "url": item.get('url', f'https://github.com/Xposed-Modules-Repo/{pkg_name}'),
        "homepageUrl": item.get('homepageUrl') or item.get('sourceUrl', ''),
        "sourceUrl": item.get('sourceUrl', ''),
        "hide": bool(item.get('hide', False)),
        "latestRelease": item.get('latestRelease') or '',
        "latestReleaseTime": item.get('latestReleaseTime') or '',
        "latestBetaRelease": item.get('latestBetaRelease') or '',
        "latestBetaReleaseTime": item.get('latestBetaReleaseTime') or '',
        "latestSnapshotRelease": item.get('latestSnapshotRelease') or '',
        "latestSnapshotReleaseTime": item.get('latestSnapshotReleaseTime') or '',
        "updatedAt": item.get('updatedAt') or '',
        "createdAt": item.get('createdAt') or '',
        "stargazerCount": item.get('stargazerCount', 0),
        "collaborators": item.get('collaborators') or [],
        "scope": item.get('scope') or [],
        "releases": [],
    }

    cached = cache_modules.get(pkg_name)
    if cached:
        cached_rels = cached.get('releases') or []
        first_tag = cached_rels[0].get('tagName', '') if cached_rels else ''
        has_canonical_cache = (
            bool(cached_rels)
            and cached.get('latestReleaseTime') == mod['latestReleaseTime']
            and cached.get('latestRelease') == mod['latestRelease']
            and bool(mod['latestRelease'])
            and all(bool(r.get('descriptionHTML')) for r in cached_rels)
        )
        if has_canonical_cache:
            mod['releases'] = normalize_releases(cached_rels)
            if cached.get('collaborators'):
                mod['collaborators'] = cached['collaborators']
            if cached.get('scope'):
                mod['scope'] = cached['scope']
            return mod, False

    # Primary fetch: modules.lsposed.org/module/{pkg_name}.json
    try:
        detail_url = f'https://modules.lsposed.org/module/{pkg_name}.json'
        detail_req = urllib.request.Request(detail_url, headers={'User-Agent': USER_AGENT})
        detail = json.loads(urllib.request.urlopen(detail_req, timeout=20).read().decode('utf-8'))
        if isinstance(detail, dict):
            for k in (
                'url', 'homepageUrl', 'sourceUrl', 'latestRelease', 'latestReleaseTime',
                'latestBetaRelease', 'latestBetaReleaseTime', 'latestSnapshotRelease',
                'latestSnapshotReleaseTime', 'updatedAt', 'createdAt', 'stargazerCount'
            ):
                if detail.get(k) is not None and detail.get(k) != '':
                    mod[k] = detail[k]
            if detail.get('collaborators'):
                mod['collaborators'] = detail['collaborators']
            if detail.get('scope'):
                mod['scope'] = detail['scope']
            mod['releases'] = normalize_releases(detail.get('releases'))
    except Exception as e:
        print(f"Warn: modules.lsposed.org detail fetch failed for {pkg_name}: {e}")

    # Fallback to GitHub API if releases still empty
    if not mod['releases']:
        for candidate_url in (mod.get('url'), mod.get('sourceUrl')):
            if not candidate_url:
                continue
            try:
                rels = fetch_github_releases(candidate_url)
                if rels:
                    mod['releases'] = rels
                    break
            except Exception as e:
                print(f"Warn: GitHub fallback failed for {pkg_name} ({candidate_url}): {e}")

    # Final fallback to cache if network failed
    if not mod['releases'] and cached and cached.get('releases'):
        mod['releases'] = cached['releases']

    if not mod.get('latestRelease') and mod['releases']:
        mod['latestRelease'] = mod['releases'][0].get('tagName') or mod['releases'][0].get('name', '')
    if not mod.get('latestReleaseTime') and mod['releases']:
        mod['latestReleaseTime'] = mod['releases'][0].get('publishedAt') or mod['releases'][0].get('createdAt', '')

    return mod, True


modules = [None] * len(raw_items)
new_fetches = 0

print(f"Processing {len(raw_items)} modules with parallel workers...")
with ThreadPoolExecutor(max_workers=16) as executor:
    future_to_idx = {executor.submit(process_module, item): idx for idx, item in enumerate(raw_items)}
    for future in as_completed(future_to_idx):
        idx = future_to_idx[future]
        try:
            mod, fetched = future.result()
            modules[idx] = mod
            if fetched:
                new_fetches += 1
        except Exception as e:
            print(f"Error processing module at index {idx}: {e}")

modules = [m for m in modules if m is not None]

with open('scripts/modules.json', 'w', encoding='utf-8') as f:
    json.dump({"modules": modules}, f, indent=2, ensure_ascii=False)
with_latest = sum(1 for m in modules if m.get('latestRelease'))
with_releases = sum(1 for m in modules if m.get('releases'))
print(f"Saved {len(modules)} modules to modules.json ({with_releases} with releases, {with_latest} with latestRelease). Performed {new_fetches} detail fetches.")

# 1. Create .nojekyll to bypass Jekyll processing on GitHub Pages
with open('scripts/.nojekyll', 'w', encoding='utf-8') as f:
    f.write('')
print("Created scripts/.nojekyll to disable Jekyll on GitHub Pages.")

# 2. Create index.html for gh-pages web landing page
index_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vector-X Modules Repository</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
    <style>
        :root {{
            --bg-color: #0b0f19;
            --card-bg: #151c2c;
            --accent-color: #6366f1;
            --accent-hover: #4f46e5;
            --text-main: #f3f4f6;
            --text-muted: #9ca3af;
            --border-color: #1f293d;
            --success-color: #10b981;
        }}
        * {{ margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }}
        body {{
            background-color: var(--bg-color);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: 2rem 1rem;
        }}
        .container {{
            max-width: 650px;
            width: 100%;
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 2.5rem;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5);
            text-align: center;
        }}
        .badge {{
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(16, 185, 129, 0.1);
            color: var(--success-color);
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 0.875rem;
            font-weight: 600;
            margin-bottom: 1.5rem;
            border: 1px solid rgba(16, 185, 129, 0.2);
        }}
        .dot {{
            width: 8px;
            height: 8px;
            background-color: var(--success-color);
            border-radius: 50%;
            display: inline-block;
            box-shadow: 0 0 8px var(--success-color);
        }}
        h1 {{
            font-size: 2rem;
            font-weight: 700;
            margin-bottom: 0.75rem;
            background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }}
        p {{
            color: var(--text-muted);
            font-size: 1rem;
            line-height: 1.6;
            margin-bottom: 2rem;
        }}
        .stats-grid {{
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 1rem;
            margin-bottom: 2rem;
        }}
        .stat-card {{
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 1.25rem;
        }}
        .stat-val {{
            font-size: 1.75rem;
            font-weight: 700;
            color: var(--accent-color);
        }}
        .stat-lbl {{
            font-size: 0.85rem;
            color: var(--text-muted);
            margin-top: 4px;
        }}
        .btn {{
            display: inline-block;
            background-color: var(--accent-color);
            color: #fff;
            padding: 0.85rem 1.75rem;
            border-radius: 10px;
            text-decoration: none;
            font-weight: 600;
            transition: all 0.2s ease;
            box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
        }}
        .btn:hover {{
            background-color: var(--accent-hover);
            transform: translateY(-2px);
        }}
        footer {{
            margin-top: 2rem;
            color: var(--text-muted);
            font-size: 0.85rem;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="badge">
            <span class="dot"></span> Repository Online & Active
        </div>
        <h1>Vector-X Modules Repository</h1>
        <p>Official online module repository endpoint for Vector-X Manager. Automatically synced and updated twice daily.</p>
        
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-val">{len(modules)}</div>
                <div class="stat-lbl">Indexed Modules</div>
            </div>
            <div class="stat-card">
                <div class="stat-val">v2.0.20+</div>
                <div class="stat-lbl">Supported App Version</div>
            </div>
        </div>
        
        <a href="modules.json" class="btn">View Raw modules.json</a>
    </div>
    <footer>
        &copy; Vector-X Project &bull; Powered by GitHub Pages
    </footer>
</body>
</html>
"""
with open('scripts/index.html', 'w', encoding='utf-8') as f:
    f.write(index_html)
print("Created scripts/index.html web landing page.")

