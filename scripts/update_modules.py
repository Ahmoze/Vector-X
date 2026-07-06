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
    cache_req = urllib.request.Request('https://raw.githubusercontent.com/Ahmoze/Vector/gh-pages/modules.json', headers={'User-Agent': 'Mozilla/5.0'})
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
