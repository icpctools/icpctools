#!/usr/bin/env python3
import sys, glob, pathlib, os

zipDir = sys.argv[1]

# Mapping from doc name to zip prefix
docMapping = {
    "BalloonUtil": "balloonUtil",
    "CoachView": "coachview",
    "ContestDataServer": "CDS",
    "ContestUtil": "contestUtil",
    "PresentationAdmin": "presentationAdmin",
    "PresentationClient": "presentations",
    "ProblemSet": "problemset",
    "Resolver": "resolver"
}

# Get the current version, needed for getting the readme from the CDS
version = pathlib.Path("version.properties").read_text().strip().replace('version=', '')

for doc in docMapping:
    files = glob.glob(zipDir + '/' + docMapping[doc] + '*.zip')
    if len(files) == 0:
        continue

    file = files[0]
    readme = 'README.pdf'
    if docMapping[doc] == "CDS":
        readme = 'CDS-{}/README.pdf'.format(version)

    command = "unzip -p {} {} > website/static/docs/{}.pdf".format(file, readme, doc)
    os.system(command)
