---
title: "CDS"
description: "The Contest Data Server is a software component designed to allow secure, authenticated HTTP connections to contest data residing on a contest network"
date: 2019-11-12T21:20:38+01:00
draft: false
weight: 2
icon: fas fa-server
---

The Contest Data Server (CDS) is a software component designed to allow secure,
authenticated HTTP connections to contest data residing on a contest network.
It provides [REST](https://en.wikipedia.org/wiki/Representational_state_transfer)
entry points for accessing a variety of contest-related services, allowing clients
to access those services via standard HTTP requests.

The CDS provides authentication services using configuration
data supplied by the Contest Administrator (CA).
Authenticated users can be assigned _roles_ by the CA; user access
to specific services is governed by the role(s) assigned to them.

The set of services made available by the CDS is dependent on the facilities available on the contest network.
Examples of services which the CDS knows how to support (assuming that the contest network provides the back-end data
required and that the CA configures their use into the CDS) include

* an overview description of the contest (title, date, etc.)
* a Countdown Clock for the start of the contest
* the current contest time (time remaining in the contest)
* the contest configuration, as defined by any of several standard file types (contest.yaml, problemset.yaml, teams.tsv, etc.)
* the current backup copy of any specified team's home directory
* the event feed as provided by the Contest Control System (CCS)
* images for each team and for each team's University Logo
* "reaction videos" showing a team's web camera at the moment they received a run submission response from the judges
* the current contest scoreboard
* an RSS feed for the contest
* the set of files which a team submitted to the judges for a specific run

{{< tooldownload name=CDS toolname=wlp.CDS >}}
