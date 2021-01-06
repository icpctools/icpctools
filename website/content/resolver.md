---
title: "Resolver"
description: "The ICPC Resolver is a tool for graphical animation of contest results. It shows the final runs submitted during a contest in an interesting way, and leads up to display of the award winners"
date: 2019-11-12T21:20:38+01:00
draft: false
weight: 3
icon: fas fa-trophy
---

The _ICPC Resolver_ is a tool for graphical animation of contest results.
It shows the final runs submitted during a contest in an
interesting way, and leads up to display of the award winners.
The Resolver concept was created by Fredrik Niemela and Mattias de Zalenski
at KTH Royal Technical University.
The ICPC Tools Resolver implementation was developed by Tim deBoer of
IBM Corporation.

The Resolver is designed to be used in contests where the scoreboard is "frozen"
prior to the end of the contest - that is, where the result of runs submitted in the
last part of the contest are not displayed on the scoreboard
(such runs are typically marked as "pending").
The Resolver produces a dynamic display by stepping through ("resolving") pending runs
and generating displays showing the contest winners in ranked order,
along with citations for awards earned.

After displaying an introductory "splash screen",
a single keystroke or mouse click causes the Resolver
to display the contest standings as of the time the scoreboard was frozen.
A key or mouse click then causes it to advance to the bottom of the standings;
a subsequent key/click starts the "resolving" process:
starting at the bottom, it moves up until it reaches a team that has one or more
pending submissions during the freeze time.
Each pending run is 'resolved' (to either a "yes" or "no" judgment),
and if the run was successful the team
will move 'up' into their new position based on the results.

Options allow you to configure when the resolver pauses, but by default it will
continue moving up and resolving until it gets to an 'interesting' case -
typically a first-to-solve award, a "group" or "region" winner,
or a gold/silver/bronze award winner. When it reaches an award, the resolver will pause
and switch to a screen showing the team name,
the logo and image (if available from a CDS; see below), and an award citation.
Once the award has been handed out, clicking returns to the regular Resolver
screen and continues the resolving process.

A variety of options are available, including managing the speed at which the
Resolver runs, controlling various "single-step" operations,
configuring categories of awards to be acknowledged during the resolving
process, and controlling simultaneous Resolver operations at multiple contest sites.

{{< tooldownload name="Resolver" toolname=resolver >}}
