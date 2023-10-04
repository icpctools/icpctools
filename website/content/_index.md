---
title: "Home"
description: "The ICPC Tools are a variety of tools implemented by the ICPC Tools Group for use at the ICPC World Finals and other programming contests"
date: 2019-11-12T21:10:52+01:00
draft: false
weight: 1
icon: fas fa-home
---

{{< figure src="/img/logo-full.png" alt="ICPC Tools logo" class="float-right" >}}

{{< button name="Github" icon="fab fa-github" link="https://github.com/icpctools/icpctools" >}}
{{< button name="Slack" icon="fab fa-slack" link="https://join.slack.com/t/icpctools/shared_invite/zt-wti6k1r6-t25~VYEcyKbVn4Vj_ecBLA" >}}

WARNING: We are in the middle of making massive code changes for the ICPC 46th and 47th World Finals. We would love feedback on whether the latest changes cause any regressions, but if you are running a live contest we strongly suggest you use v2.5.940 or earlier until we are able to test and release in November.

Welcome to the ICPC Tools web page! This page contains a variety of tools implemented by the International Collegiate Programming Contest (ICPC) Tools Group, most of which were originally developed for use at the ICPC World Finals and have been adapted for use at other programming contests.
These tools have been used to support a wide variety of programming contests including local contests at Universities world-wide, multiple ICPC Regional Contests around the world, and a number of ICPC World Finals.

All of the ICPC tools are designed to work together, and where applicable they are based on published ICPC standards. In particular, many of the tools are derived from and based on the specifications published under the auspices of the *Competitive Learning Initiative* as posted on the [CLI Specification Website](https://ccs-specs.icpc.io/).

## The Tools

Follow the links below to download any of the tools. Click on the "More information" link to get more information for each tool.

{{< toolrow >}}
    {{< toolblock shortname=CDS name="Contest Data Server" description="Single-point URL services for accessing contest data" toolname=wlp.CDS doc=ContestDataServer page=cds >}}
    {{< toolblock shortname=Resolver name=Resolver description="Animated reveal of final contest results" toolname=resolver doc=Resolver page=resolver >}}
{{</ toolrow >}}
{{< toolrow >}}
    {{< toolblock shortname="Presentation Admin" name="Presentation Admin" description="Remote administration of multiple presentations (requires CDS)" toolname=presentationAdmin doc=PresentationAdmin page=pres-admin >}}
    {{< toolblock shortname="Presentation Client" name="Presentation Client" description="Animated display of scoreboard and other contest data" toolname=presentations doc=PresentationClient page=pres-client >}}
{{</ toolrow >}}
{{< toolrow >}}
    {{< toolblock shortname="Balloon Utility" name="Balloon Utility" description="Manages and prints which teams to award a balloon" toolname=balloonUtil doc=BalloonUtil page=balloon-util >}}
    {{< toolblock shortname="Coach View" name="Coach View" description="Ability to remotely see the camera and desktop of a team" toolname=coachview doc=CoachView page=coach-view >}}
{{</ toolrow >}}
{{< toolrow >}}
    {{< toolblock shortname="Problem Set Editor" name="Problem Set Editor" description="Generate/Edit YAML descriptions of problem sets for input to CLICS-compatible CCS's" toolname=problemset doc=ProblemSet page=problem-set-editor >}}
    {{< toolblock shortname="Contest Utilities" name="Contest Utilities" description="A variety of useful contest-related utilities: event feed checkers, floor map generators, submission extractors, and more!" toolname=contestUtil doc=ContestUtil page=contest-utils >}}
{{</ toolrow >}}

{{< lastbuilds >}}

## Disclaimer

The tools on this page are provided free and "as is", with the usual disclaimers: lack of guarantee of suitability for any particular purpose, no stated or implied responsibility for the results of their use, etc.

In other words, we find these tools to be very useful in supporting a variety of common programming contest operations, and we think you will too; but we do not guarantee that they will do exactly what you want for *your* programming contest. All of the code has been written by and is directly under the control of the ICPC Systems Group, including that we take particular care to insure that there are no intentional bad things (malware) in them; however, neither ICPC nor its affiliates or volunteers make any guarantees at all regarding the code.

## Contributing

The ICPC Tools are developed, tested, and maintained by a group of ICPC volunteers on [GitHub](https://github.com/icpctools/icpctools).
Bug reports, feature requests, and even just knowing what worked or didn't for your contest are always appreciated.
Pull requests are also welcome, but if you want to implement a big feature it might be best to first create an issue to discuss it.

To become a committer you must have a history of high quality bug reports, PRs, and be approved by the ICPC Tools team.

## Community

A slack workspace exists to discuss anything and everything ICPC Tools related.

It can be found at https://icpctools.slack.com/. To join as a new member, use
[this link to sign up](https://join.slack.com/t/icpctools/shared_invite/zt-wti6k1r6-t25~VYEcyKbVn4Vj_ecBLA).

## License

All of the tools are provided under the [ICPC Software License](https://github.com/icpctools/icpctools/blob/main/LICENSE) license
and are "Free as in Beer". We welcome you to use and enjoy them, but if you ever run into anyone who has contributed to
them - Tim, John, Nicky, Sam, Troy, etc. we would greatly appreciate it if you'd buy us a beer, a stroopwafel, or some other suitable token!

All ICPC Tools are Copyright &copy; by the ICPC.
