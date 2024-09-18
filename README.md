![platforms](https://img.shields.io/badge/platforms-macos|linux|windows-lightgrey.svg)
[![ICPC Software License](https://img.shields.io/badge/license-ICPC%20Software%20License-brightgreen.svg)](https://github.com/icpctools/icpctools/blob/main/LICENSE)
[![pipeline status](https://gitlab.com/icpctools/icpctools/badges/main/pipeline.svg)](https://gitlab.com/icpctools/icpctools/commits/main)

Welcome to the ICPC Tools!
==========================

The ICPC Tools are a set of tools to support running programming contests. For the latest downloads, please go to the [ICPC Tools website](https://tools.icpc.global).

Each of the ICPC tools can be used individually, or together in any combination. They are all designed to support
the REST-based [Contest API](https://ccs-specs.icpc.io/2021-11/contest_api) as defined by the Competitive Learning Initiative (CLI).
Some features require extensions to the specification, and those are described [here](doc/spec-extensions.md).

These tools were built to support the
[International Collegiate Programming Contest (ICPC)](https://icpc.global) World Finals and have been used there for many years, but
the intention is that they are usable for local and regional contests as well.

## The ICPC Tools

Tool | Description
--- | ---
Balloon Utility | Manages and prints which teams to award a balloon
Contest Data Server (CDS) | Single-point URL services for accessing all contest data
Presentation | Animated display of scoreboards or other contest data
Presentation Admin | Remote administration of multiple presentations (requires the CDS)
Resolver | Animated reveal of final contest results
Coach View | Ability to remotely see the webcam and desktop of a team
Problem Set Editor | Generate/Edit YAML descriptions of problem sets for input to CLICS-compatible Contest Control Systems.
Contest Utilities | A variety of useful contest-related utilities: event feed validation, scoreboard comparison, floor map generators, and more!

## Contest Control System Compatibility

The ICPC Tools are built to work with any Contest Control System (CCS) that supports the REST-based [Contest API](https://ccs-specs.icpc.io/2021-11/contest_api).

To be more specific, the only part of the Contest API that is strictly required is the event feed and any file
references that the feed refers to. If your CCS correctly supports the event feed, then all of the ICPC Tools will
work even if the rest of the API is not implemented. The one exception to this is the CDS' and contest utility support
for comparing scoreboards - to compare a scoreboard, the CCS must have one, of course!

The most popular CCSs that have been tested and successfully used at multiple contests with the ICPC Tools are listed here:

* [DOMjudge](https://www.domjudge.org)
* [Kattis](https://www.kattis.com)
* [PC^2](https://pc2ccs.github.io)

## Contributing

The ICPC Tools are developed, tested, and maintained by a group of ICPC volunteers. Bug reports, feature requests,
and even just knowing what worked or didn't for your contest are always appreciated. Pull requests are also welcome,
but if you want to implement a big feature it might be best to first create an issue to discuss it.

To become a committer you must have a history of high quality bug reports, PRs, and be approved by the ICPC Tools team.

## Development

### Using Eclipse

You can use [Eclipse](https://www.eclipse.org) to develop and debug the ICPC Tools.
We will add information on how to run and debug later.

### Using IntelliJ IDEA

See [the IntelliJ IDEA specific documentation](doc/intellij-idea.md).

### Using the CDS docker image

A [Docker image](https://ghcr.io/icpctools/cds) is provided to run the CDS without having to install it yourself.

#### Running it

The basic way to run it, is to run

```bash
docker run --name cds --rm -it  -p 8080:8080 -p 8443:8443 -e CCS_URL=https://www.domjudge.org/demoweb/api/contests/nwerc18 -e CCS_USER=admin -e CCS_PASSWORD=admin ghcr.io/icpctools/cds:2.2.407
```

Replace `https://www.domjudge.org/demoweb/api/contests/nwerc18` with your CCS contest API URL, `CCS_USER` with a user with admin privileges to the CCS URL, `CCS_PASSWORD` with the password for the given user and `2.2.407` with the version of the CDS you want to run.

Now you can access the CDS at https://localhost:8443/.

*Note*: this will use default credentials for all users, so it is recommended to change them. This can be done using environment variables or overwriting the `users.xml` file.

#### Environment variables

* `ADMIN_PASSWORD`: the password for the user with admin privileges.
* `PRESADMIN_PASSWORD`: the password for the user with presentation admin privileges.
* `BLUE_PASSWORD`: the password for the user with blue privileges.
* `BALLOON_PASSWORD`: the password for the user with balloon privileges.
* `PUBLIC_PASSWORD`: the password for the user with public privileges.
* `PRESENTATION_PASSWORD`: the password for the user with presentation privileges.
* `MYICPC_PASSWORD`: the password for the user with MyICPC privileges.
* `LIVE_PASSWORD`: the password for the user with Live privileges.
* `CCS_URL`: the URL to a CCS contest.
* `CCS_USER`: the user that has admin access to `CCS_URL`.
* `CCS_PASSWORD`: the password for `CCS_USER`.

#### Overwriting config files

* If you want to supply your own `users.xml`, mount a file at `/opt/wlp/usr/servers/cds/users.xml`.
* If you want to supply your own `cdsConfig.xml`, mount a file at `/opt/wlp/usr/servers/cds/config/cdsConfig.xml`.

#### Contest data directory

The contest data directory is located at `/contest` so you can mount that as a volume if you want to store the data or use team photos, organization logos, banners or any other files.

## License

All of the tools are provided under the included license and are "Free as in Beer". We welcome you to use
and enjoy them, but if you ever run into anyone who has contributed to them - Tim, John, Nicky, Sam, Troy, etc.
we would greatly appreciate it if you'd buy us a beer, a stroopwafel, or some other suitable token!

All ICPC Tools are Copyright © by the ICPC.
