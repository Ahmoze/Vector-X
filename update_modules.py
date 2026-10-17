import urllib.request
import json
import re
import os
import time

GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
headers = {'User-Agent': 'Mozilla/5.0'}
if GITHUB_TOKEN:
    # Use Bearer token format for GitHub API
    headers['Authorization'] = f'Bearer {GITHUB_TOKEN}'

print("Loading existing modules.json cache...")
cache_modules = {}
try:
    cache_req = urllib.request.Request('https://raw.githubusercontent.com/Ahmoze/Vector-X/gh-pages/modules.json', headers={'User-Agent': 'Mozilla/5.0'})
    cache_data = json.loads(urllib.request.urlopen(cache_req).read().decode('utf-8'))
    for m in cache_data.get('modules', []):
        cache_modules[m['name']] = m
    print(f"Loaded {len(cache_modules)} modules from cache.")
except Exception as e:
    print(f"No existing cache found or failed to load: {e}")

print("Fetching paginated module list from modules.lsposed.org...")
page = 1
total_pages = 1
modules = []
new_fetches = 0

while page <= total_pages:
    print(f"Fetching page {page}/{total_pages}...")
    url = f'https://modules.lsposed.org/module-list/{page}.json'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        data = json.loads(urllib.request.urlopen(req).read().decode('utf-8'))
        if page == 1:
            total_pages = data.get('pageCount', 1)
        
        for item in data.get('modules', []):
            pkg_name = item.get('name')
            friendly_name = item.get('description', '')
            summary = item.get('summary', '')
            source_url = item.get('sourceUrl', '')
            latest_api_release = item.get('latestReleaseTime', '')
            
            # Use cached module if it hasn't been updated
            if pkg_name in cache_modules:
                cached = cache_modules[pkg_name]
                # If we have cached releases and the API says there's no new release, use cache!
                if cached.get('releases') and cached.get('latestReleaseTime') == latest_api_release:
                    # Keep the cached version but update description/summary in case they changed
                    cached['description'] = friendly_name
                    cached['summary'] = summary
                    modules.append(cached)
                    continue

            # If we reach here, we need to fetch releases from GitHub API
            new_fetches += 1
            mod = {
                "name": pkg_name,
                "description": friendly_name,
                "summary": summary,
                "sourceUrl": source_url,
                "latestReleaseTime": latest_api_release,
                "releases": []
            }
            
            m = re.match(r'https://github\.com/([^/]+)/([^/]+)', source_url)
            if not m:
                # Still add it even without releases
                modules.append(mod)
                continue
                
            owner, repo = m.groups()
            repo = repo.replace('.git', '')
            api_url = f'https://api.github.com/repos/{owner}/{repo}/releases'
            
            try:
                api_req = urllib.request.Request(api_url, headers=headers)
                resp = urllib.request.urlopen(api_req)
                releases_data = json.loads(resp.read().decode('utf-8'))
                
                for r in releases_data:
                    if 'draft' in r and r['draft']:
                        continue
                    assets = []
                    for a in r.get('assets', []):
                        if a['name'].endswith('.apk') or a['name'].endswith('.zip'):
                            assets.append({
                                "name": a['name'],
                                "downloadUrl": a['browser_download_url'],
                                "size": a['size']
                            })
                    if not assets:
                        continue
                        
                    mod['releases'].append({
                        "name": r['name'] or r.get('tag_name', ''),
                        "tagName": r.get('tag_name', ''),
                        "createdAt": r.get('created_at', ''),
                        "publishedAt": r.get('published_at', ''),
                        "releaseAssets": assets
                    })
            except Exception as e:
                print(f"Failed to fetch releases for {owner}/{repo}: {e}")
                # Fallback to cache if available
                if pkg_name in cache_modules:
                    mod['releases'] = cache_modules[pkg_name].get('releases', [])
            
            modules.append(mod)
            time.sleep(0.2) # Small delay to be polite to GitHub API
            
    except Exception as e:
        print(f"Failed to fetch page {page}: {e}")
        break
        
    page += 1

with open('scripts/modules.json', 'w', encoding='utf-8') as f:
    json.dump({"modules": modules}, f, indent=2, ensure_ascii=False)
print(f"Saved {len(modules)} modules to modules.json successfully. Performed {new_fetches} new GitHub API fetches.")

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

