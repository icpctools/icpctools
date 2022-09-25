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

The CDS also retains support for the deprecated [XML Event Feed](https://clics.ecs.baylor.edu/index.php?title=Event_Feed_2016), [Mirror](https://web.archive.org/web/20160601232618/https://clics.ecs.baylor.edu/index.php/Event_Feed_2016).
If your CCS supports this feed as specified then the tools should still work with a CDS in the middle, albeit with some missing function.

The most popular CCSs that have been tested and successfully used at multiple contests with the ICPC Tools are listed here:

Compatible CCS | Mechanism
| --- | ---
| [DOMjudge](https://www.domjudge.org) | Contest API
| [Kattis](https://www.kattis.com) | Contest API
| [PC^2](https://pc2.ecs.csus.edu) | XML event feed

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

## License

All of the tools are provided under the included license and are "Free as in Beer". We welcome you to use
and enjoy them, but if you ever run into anyone who has contributed to them - Tim, John, Nicky, Sam, Troy, etc.
we would greatly appreciate it if you'd buy us a beer, a stroopwafel, or some other suitable token!

All ICPC Tools are Copyright © by the ICPC.
