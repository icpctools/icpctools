---
title: "Presentation Client"
date: 2019-12-15T11:42:54+01:00
draft: false
weight: 5
icon: fas fa-desktop
---

The ICPC Presentation System provides a mechanism for generating "slide show" presentations containing
user-provided images, photos, and a variety of live data from a running contest.
The Presentation System consists of two components: _Presentation Clients_ and the _Presentation Admin_.
This document describes the Presentation Client component;
refer to the separate documentation on the Presentation Admin for an explanation of how that component operates.

A Presentation Client is a single process which displays (rotates between) one or more _presentations_.
The Presentation System contains a wide variety of built-in presentations, each of which is identified
by having both a _number_ and a _title_ (also referred to as its _name_).
Each presentation client is started by giving it a list of the
presentations (by number or title) that the client should display.  It is allowable to start multiple
simultaneous presentation clients, each displaying its own (possibly overlapping) set of presentations.
Each separate presentation client is typically run on a separate machine, with each machine connected
to a projector or large display to allow many people to watch. (At the ICPC World Finals, for example,
as many as 10 or more separate screens are displayed, each running its own specified sequence of presentations.)

Some built-in presentations incorporate data from a running contest.  For example, one presentation
knows how to display the current contest scoreboard, updating it as the contest progresses;
another displays a rising (animated) balloon, labeled with the appropriate team and problem letter/color,
whenever a team solves a problem;
another presentation shows a graph of the languages used to solve problems so far in the contest;
another shows the runs currently in the "judge's queue" and the judgment each runs receives
as it exits the queue;
still another presentation shows the current contest clock (remaining time);
and so forth.

{{< tooldownload name="Presentation Client" toolname=presentations >}}
