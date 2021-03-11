---
title: "Presentation Admin"
description: "The Presentation Admin is one of the components comprising the ICPC Presentation System"
date: 2019-12-15T11:40:23+01:00
draft: false
weight: 4
icon: fas fa-user-cog
---

The _Presentation Admin_ is one of the components
comprising the ICPC _Presentation System_.
The other major components of the Presentation System
are the _Presentation Client_ and a collection of _presentations_.

The ICPC Presentation System defines a large number of built-in
"presentations", each of which displays programming contest-related
data in some fashion.  For example, one presentation shows the
_current contest scoreboard_, scrolling it automatically from the current
leaders down through all contenders and then repeating.
Other presentations show data such as the languages being used,
the current number of solutions to each contest problem,
notifications that a particular team has just solved a particular problem,
and so forth, all updating in real time based on input from a
Contest Control System (CCS).
There are also pre-defined presentations for showing a variety of user-selected
data such as team photographs, contest logos and related images,
local sites of interest, fireworks for the end
of the contest, and so forth.

The ICPC Presentation System will work with any
CCS that produces an event feed which is
compliant with the [_CLI Contest API_](https://ccs-specs.icpc.io/contest_api).
Systems known to produce compliant event feeds include
[PC-Squared](http://pc2.ecs.csus.edu),
[Kattis](https://www.kattis.com/) and [DOMjudge](https://www.domjudge.org);
other Contest Control Systems may also produce compatible event feeds and
hence work with the Presentation System.

{{< tooldownload name="Presentation Admin" toolname=presentationAdmin doc=PresentationAdmin >}}
