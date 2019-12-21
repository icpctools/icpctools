---
title: "Home"
date: 2019-11-12T21:10:52+01:00
draft: false
weight: 1
icon: fas fa-home
---

{{< figure src="/img/logo-full.png" alt="ICPC Tools logo" class="float-right" >}}


Welcome to the ICPC Tools web page! This page contains a variety of tools implemented by the International Collegiate Programming Contest (ICPC) Tools Group, most of of which were originally developed for use at the ICPC World Finals and have been adapted for use at other programming contests.
These tools have been used to support a wide variety of programming contests including local contests at Universities world-wide, multiple ICPC Regional Contests around the world, and a number of ICPC World Finals.

All of the ICPC tools are designed to work together, and where applicable they are based on published ICPC standards. In particular, many of the tools are derived from and based on the specifications published under the auspices of the *Competitive Learning Initiative* as posted on the [CLI Wiki](https://clics.ecs.baylor.edu/index.php).

## The Tools

Follow the links below to download any of the tools. Click on the "More information" link to get more information for each tool.

<div class="row">
    {{< toolblock shortname=CDS name="Contest Data Server" description="Single-point URL services for accessing contest data" toolname=wlp.CDS page=cds >}}
    {{< toolblock shortname=Resolver name=Resolver description="Animated reveal of final contest results" toolname=resolver page=resolver >}}
</div>
<div class="row">
    {{< toolblock shortname="Presentation Admin" name="Presentation Admin" description="Remote administration of multiple presentations (requires CDS)" toolname=presentationAdmin page=pres-admin >}}
    {{< toolblock shortname="Presentation Client" name="Presentation Client" description="Animated reveal of final contest results" toolname=presentations page=pres-client >}}
</div>
<div class="row">
    {{< toolblock shortname="Balloon Utility" name="Balloon Utility" description="Manages and prints which teams to award a balloon" toolname=balloonUtil page=balloon-util >}}
    {{< toolblock shortname="Coach View" name="Coach View" description="Ability to remotely see the camera and desktop of a team" toolname=coachview page=coach-view >}}
</div>
<div class="row">
    {{< toolblock shortname="Problem Set Editor" name="Problem Set Editor" description="Generate/Edit YAML descriptions of problem sets for input to CLICS-compatible CCS's" toolname=problemset page=problem-set-editor >}}
    {{< toolblock shortname="Contest Utilities" name="Contest Utilities" description="A variety of useful contest-related utilities: event feed checkers, floor map generators, submission extractors, and more!" toolname=contestUtil page=contest-utils >}}
</div>

{{< lastbuilds >}}

## Disclaimer

The tools on this page are provided free and "as is", with the usual disclaimers: lack of guarantee of suitability for any particular purpose, no stated or implied responsibility for the results of their use, etc.

In other words, we find these tools to be very useful in supporting a variety of common programming contest operations, and we think you will too; but we do not guarantee that they will do exactly what you want for *your* programming contest. All of the code has been written by and is directly under the control of the ICPC Systems Group, including that we take particular care to insure that there are no intentional bad things (malware) in them; however, neither ICPC nor its affiliates or volunteers make any guarantees at all regarding the code.

All ICPC Tools are Copyright &copy; by the ICPC.
