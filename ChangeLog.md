# ICPC Tools Changelog

## V2.5 - TBD
-----------------
* Contest API improvements:
  * Will be first release to support breaking changes in next spec release.

## V2.4 - April 2022
-----------------
* Java 17 support! (this will be the last release supporting Java 8)
* Apple M1 support!
* Contest API improvements:
  * Full support for 2021_11 spec.
  * SVG images.
  * Language entry points.
  * Country flags.
  * Commentary.
  * Scoring contests (beta).
  * Most non-breaking changes from next spec release (beta of /, /accounts, /persons, etc).
* Support for the Contest archive format: https://ccs-specs.icpc.io/draft/contest_archive_format.
* Several fixes based on use at regional and world finals contests.
* CDS:
  * UI improvements, new welcome page.
  * Support for multiple machines (streams, backups, key logs, etc) per team.
  * Host support for identifying team machines.
  * Direct video mode, just expose existing URLs.
  * More than one CDS? You can name them to tell the web pages apart.
  * Improved time syncing for clients.
  * Nginx proxying
* Presentation client:
  * Emojis! Woo hoo! Supported for team names, messages, and a few more. Please let us know if you need it anywhere else.
  * Light mode support for most presentations.
  * Updated problem summary presentation.
  * Initial Commentary presentation.
  * Performance and UI improvements (e.g. text fitting) to many presentations, especially 'tile' scoreboards.
* Presentation admin:
  * Dark mode (when supported by JDK and system default).
* Coach view:
  * Support for multiple streams per team.
  * Dark mode (when supported by JDK and system default).
* Resolver:
  * Support for display modes for every award: a detailed screen, pause and move on, display a list, or ignore any award.
  * Honors and Top award templates.
* Award UI:
  * Better support for award templates.
  * Dark mode (when supported by JDK and system default).

## V2.3 - March 2021
-----------------
* Several fixes based on use at regional contests.
* CDS:
  * Initial support for team login and team-specific contest views.
  * New dark mode!
  * Several UI improvements.
  * Preview of API for team submissions & clarifications.
* Presentation client:
  * Improved support for Windows multi-display and scaling.
  * New --display_name parameter to configure how teams are displayed (replaces --style).
* Resolver:
  * New --display_name parameter to configure how teams are displayed (replaces --style).

## V2.2 - February 2021
-----------------
* First open release!
* Moved to Github and Gitlab.
* Moved to new hostname and [website](https://tools.icpc.global).
* New logo and icons!
* Common command line parser and further command line consistency.
* XML event feed removed everywhere but CDS. If your CCS only supports the XML feed, you must use the CDS as a proxy to use the other tools.
* Support for Contest API improvements and numerous fixes.
* CDS:
  * Completely new UI, theme, and layout.
  * Automatic reloading - change CDS config without restarting the CDS.
  * All clients (balloon utility, coach view) detect and connect to CDS, allowing stop/restart/update from central location.
  * Ability to output custom Contest API events to handle feed problems.
  * Switched to use Open Liberty runtime.
  * Support for tool and keylog files.
  * Support for contest commentary events.
  * Support for team audio streaming.
  * Ability to drive resolver clients from web UI.
* Presentation admin:
  * Show presentations that extend across multiple displays as one.
* Presentation client:
  * Initial new fireworks presentation.
  * New organization logo presentations.
  * New ICPC Tools presentation.
  * New CCS presentation.
  * Fixed calculation of 'recent' events.
  * Support for presentations extending across multiple displays.
* Resolver:
  * Ability to provide custom resolution steps.
  * Refactored resolution into core contest API. Resolution no longer a bunch of custom code in the resolver, and other tools could use it as well.
  * Award templates - ability to apply awards based on a spec. Useful for easily applying the same awards to test & real contests.
  * Odd solution awards removed.
  * Ability to filter teams by group.
  * Support for presentations extending across multiple displays.
* Numerous release improvements:
  * Switched from asciidoc to markdown for documentation.
  * Build improvements & simplification.

## V2.1 - April 2019
-----------
* Used at World Finals but never released.
* XML event feed deprecated from all tools.
* Ability in all clients to detect multiple contests, and auto-connect to most likely contest if not specified.
* CDS:
  * Support for configuring multiple contests.
  * Ability to compare contests and scoreboards.
  * Switched to JSON for all client/server communication.
  * Presentation domains so multiple presentation admins can only control their own presentations.
* Presentation client:
  * New world map and contest floor (balloon) presentations.

## V2.0 - May 2018
-----------------
* Support for new REST Contest API across all tools.
* Balloon utility:
  * Support for printing by site (group).
* CDS:
  * Ability to configure multiple contests at the same time.
  * New web pages - dive into details on any configured contest, including validation, scoreboard, and result verification.
  * Significant performance improvements, especially for large contests/feeds.
  * Ability to eagerly load all video streams to reduce client connection delay.
  * Switched to use Open Liberty server.
  * Ability to block unauthenticated video during contest freeze.
  * Playback mode can start at a specific time.
  * Filtering for hidden teams.
* Coach view:
  * Ini file replaced by command line that is consistent with the other tools.
* Contest utilities:
  * Scoreboard and (initial) event feed comparison utils.
  * XML <-> JSON event feed conversion.
  * Image generator automatically removes padding.
* Presentation admin:
  * Consistent, more performant support for team presentations (running presentations on all team displays).
  * Ability to set default presentations.
  * Minor UI/action improvements.
* Presentation client:
  * Minor improvements to several presentations.
  * Problem totals on scoreboards.
  * Support for multiple displays for standalone presentations.
* Resolver - minor UI fixes and cleanup.

## V1.2 - June 2017
------------------
* Balloon utility:
  * Ability to customize 'escort' messages.
  * Test print a sample page.
  * Coloured balloon printouts.
  * Automatically prompt and reload balloons when reconnecting to same contest.
  * Minor updates to UI, contest info shown.
* CDS:
  * New NDJSON event feed in advance of spec.
  * Support for university location, hashcode, urls.
  * Support for team videos.
  * Better support for missing config files, merging data between CDP and CCS.
  * Overview page improvements.
  * Administrative stats on overview and video status pages.
  * Improved performance of event feeds.
* Coach view:
  * Added team submissions and current results. 
  * Minor UI improvements.
* Presentation admin:
  * Numerous UI improvements.
  * Performance improvements to scale to > 130 clients.
  * Admin support for team desktop clients.
  * Ability to pull client logs.
  * New transitions.
* Presentation client:
  * Better client time sync (within ms).
  * Client framerate improvements (and maximum).
  * Added several new presentations.
  * UI improvements in many presentations.
  * Webcam video test presentation.
* Resolver:
  * Award generator support for new award types.
  * UI fixes and timing improvements.
  * Test features (--bill) for 2017 finals.
* Initial release of the contest utilities!

## V1.1 - May 2017
-----------------
* Balloon utility:
  * Ability to filter the table and auto-print to one or more groups.
  * Fixed table column sorting.
  * Fixed CCS connection issue.
  * Merged 32 and 64 bit batch files.
  * Added contest time to UI.
  * Support for log files.
  * Fixed issue with black box size on printout.
* Resolver:
  * Added backup and fast forward/rewind capability (see --help).
  * Speed setting is now passed to clients.
  * Team images are now pre-loaded in the background.
  * Moved splash screen and 'missing team' image into the package.
  * New award UI allows you to customize individual awards.
  * Support for log files.
* Presentation client:
  * Added animation to Leaderboard and Timeline presentations.
  * Added BSoD presentation. 
  * Added initial 'tile' scoreboards.
  * Added Polar clock.
  * Fixed CCS connection parsing issue to match what was in --help.
  * Improved animations (e.g. sliding teams).
  * Added contest time to scoreboard presentations.
  * Support for log files.
* Initial release of the CDS!
* Initial release of the Presentation Admin!
* Initial release of the Coach view!

## V1.0.1 - 24 June 2015
---------------------
* Updated resolver splash screen so a mouse-click advances (rather than terminates).

## V1.0 - 18 June 2015
---------------------
* Initial release of Balloon Utility!
* Initial release of the Presentation Client!
* Initial release of the Resolver!