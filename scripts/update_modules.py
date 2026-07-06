import urllib.request
import json
import re
import os
import time

GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
headers = {'User-Agent': 'Mozilla/5.0'}
if GITHUB_TOKEN:
    headers['Authorization'] = f'token {GITHUB_TOKEN}'

print("Fetching modules.lsposed.org...")
req = urllib.request.Request('https://modules.lsposed.org/', headers={'User-Agent': 'Mozilla/5.0'})
try:
    html = urllib.request.urlopen(req).read().decode('utf-8')
except Exception as e:
    print("Failed to fetch HTML:", e)
    exit(1)

# Regex to parse the HTML
# <article class="module-list-row">...<a href="/module/PKG/">FRIENDLY_NAME</a>...<span class="Label Label--secondary module-list-package">PKG</span>...<p class="module-list-description">SUMMARY</p>...<a class="btn btn-sm" href="GITHUB_URL" target="_blank" rel="noreferrer">Source</a>
pattern = re.compile(
    r'<article class="module-list-row">.*?'
    r'<a href="/module/[^/]+/">([^<]+)</a>.*?'
    r'<span class="Label Label--secondary module-list-package">([^<]+)</span>.*?'
    r'<p class="module-list-description">([^<]+)</p>.*?'
    r'<a class="btn btn-sm" href="([^"]+)"[^>]*>Source</a>',
    re.DOTALL | re.IGNORECASE
)

modules = []
matches = pattern.findall(html)
print(f"Found {len(matches)} modules in HTML.")

for friendly_name, pkg_name, summary, source_url in matches:
    # clean html entities
    friendly_name = friendly_name.replace('&#x27;', "'").replace('&amp;', '&').strip()
    summary = summary.replace('&#x27;', "'").replace('&amp;', '&').strip()
    
    # Extract owner and repo from github url
    m = re.match(r'https://github\.com/([^/]+)/([^/]+)', source_url)
    if not m:
        continue
    owner, repo = m.groups()
    repo = repo.replace('.git', '')
    
    mod = {
        "name": pkg_name,
        "description": friendly_name,
        "summary": summary,
        "sourceUrl": source_url,
        "releases": []
    }
    
    # Query GitHub API for releases
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
                "name": r['name'] or r['tag_name'],
                "tagName": r['tag_name'],
                "createdAt": r['created_at'],
                "publishedAt": r['published_at'],
                "releaseAssets": assets
            })
    except Exception as e:
        print(f"Failed to fetch releases for {owner}/{repo}: {e}")
        
    modules.append(mod)
    time.sleep(0.1) # Small delay to be polite

with open('modules.json', 'w') as f:
    json.dump({"modules": modules}, f, indent=2)
print("Saved modules.json successfully.")
