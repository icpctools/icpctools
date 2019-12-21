#!/usr/bin/env python3
import requests
import os
import re
import json
import datetime

destination = "website/data/releases/"

def all_releases():
    result = []

    url = 'https://api.github.com/repos/icpctools/builds/releases'

    linkPattern = re.compile(r'<(?P<url>.*)>; rel="(?P<type>.*)"')

    r = request_from_github(url)
    result += r.json()
    while "Link" in r.headers:
        links = r.headers["Link"].split(", ")
        nextFound = False
        for link in links:
            match = linkPattern.search(link)
            if match.group("type") == "next":
                r = request_from_github(match.group("url"))
                result += r.json()
                nextFound = True
        if not nextFound:
            break

    return sorted(result, key=lambda release: release["published_at"], reverse=True)

def request_from_github(url):
    token = os.environ["GITHUB_TOKEN"]
    headers = {
        'Accept': 'application/vnd.github.v3+json',
        'Authorization': 'token {}'.format(token)
    }
    r = requests.get(url, headers=headers)

    return r

def release_info(release):
    assetPattern = re.compile(r'(?P<tool>.*)-(?P<version>\d+\.\d+\.\d+)\.zip\.?(?P<check>.*)?')
    info = {
        'version': release['tag_name'].replace('v', ''),
        'date': datetime.datetime.strptime(release['published_at'], "%Y-%m-%dT%H:%M:%SZ").strftime("%d %B %Y"),
        'time': datetime.datetime.strptime(release['published_at'], "%Y-%m-%dT%H:%M:%SZ").strftime("%H:%M:%S"),
        'downloads': {}
    }

    for asset in release["assets"]:
        match = assetPattern.search(asset['name'])
        tool = match.group('tool')
        version = match.group('version')
        check = match.group('check')
        url = asset['browser_download_url']
        if tool == "problemSet":
            tool = "problemset"
        if not tool in info['downloads']:
            info['downloads'][tool] = {
                'version': version,
                'urls': {}
            }
        if check == "":
            info['downloads'][tool]['urls']['zip'] = url
            info['downloads'][tool]['size'] = round(asset['size'] / 1024 / 1024, 2)
        else:
            info['downloads'][tool]['urls'][check] = url

    return info

if not os.path.isdir(destination): 
    os.mkdir(destination, 0o755)

releases = all_releases()

latest_stable = list(filter(lambda release: not release["prerelease"], releases))[0]
latest_prerelease = list(filter(lambda release: release["prerelease"], releases))[0]

files = {
    'stable': json.dumps(release_info(latest_stable), indent=4, sort_keys=True),
    'prerelease': json.dumps(release_info(latest_prerelease), indent=4, sort_keys=True),
    'all': json.dumps(list(map(lambda release: release_info(release), releases)), indent=4, sort_keys=True)
}

for file in files:
    f = open(destination + file + '.json', 'w')
    f.write(files[file])
    f.close()
    print("Wrote " + file + " to " + destination + file + '.json')
