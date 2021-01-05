# The ICPC Coach View

<img src="docs/coachViewIcon.png" alt="Coach View" width="150px"/>

An ICPC Tool

## Introduction

The Coach View is a software component designed to provide the ability for spectators
(coaches as well as other interested people) to view either or both of a selected team's desktop (machine screen)
or web camera during a contest. It requires that the Contest Administrator configure desktop
and/or webcam streaming from team machines during the contest, and also requires a
[Contest API](https://clics.ecs.baylor.edu/index.php/Contest_API) source that implements video streaming, for
example the ICPC Tools [Contest Data Server](https://clics.ecs.baylor.edu/index.php/CDS).

The Coach View provides the ability for a coach to select a specific team by name or team number and to view
either or both of that team's desktop and webcam video feeds.

## Installing the Coach View

To install the Coach View software, download a copy of the latest version from the [ICPCTools web page](https://icpc.baylor.edu/icpctools/)
and unzip it into any convenient directory. That's it!

## Usage

The general form for executing the Coach View is

```
  coachView.bat/sh contestURL user password [options]
``` 
where
```
  <contestURL> is either an HTTP or HTTPS URL to connect to a Contest API
                  source, followed by user and password

  [options] can be "--display <num>", where <num> is which desktop display to use
                  in full-screen exclusive mode. The primary display is number 1,
                  secondary is number 2, etc. If this option is not specified
                  the default is the primary display.
```

## Examples

```
  coachView.bat https://cds/api/contests/MyContest coach myPassword
```

The above command starts the Coach View and connects it securely to a CDS at the specified
URL using the specified user name ("coach") and password ("myPassword").

# Appendix

## Setting up Video Streaming on Team machines

In order for the Coach View to be able to provide access to team desktops and/or webcams, two things must be done.
First, the Coach View software must be told how to access team desktop and/or camera feeds.
Second, the Contest Administrator must arrange for team machines to 
generate appropriate video streams on the ports which are specified in the Coach View configuration.

Any mechanism can be used to generate the video streams on the team machines. At the World Finals, the free open-source package
called [VLC](http://www.videolan.org/vlc/index.html) is used.
Specifically, each team machine has VLC installed, and two instances of VLC are running on each team machine: one to generate
the team desktop stream, and a second to generate the team webcam stream. The commands used to generate these streams are shown below.

Note that while this works at the World Finals, it is possible you will have to do some tweaking to get it to work in your
environment. In particular, for example, the commands contain references to specific image resolutions.
In addition, note that the commands are run using Linux sudo at the World Finals. That is, the actual command used is

```
  exec /usr/bin/sudo -u camera -H vlc_command  > /dev/null 2>&1 &
```

where *vlc_command* represents the VLC command shown below.
 
In any case, the commands shown below will hopefully be enough to point you in the proper direction.

### Team WebCam Streaming

The following VLC command is used at the World Finals to start a stream on port 8080 containing the team's web camera output:

```
  vlc -I dummy -q v4l2:///dev/video0:width=1280:height=720:aspect-ratio="16:9" \
  :input-slave=alsa://plughw:1,0 --sout '#transcode{venc=x264{keyint=15},vcodec=h264,\
  vb=0,scale=0,fps=30,acodec=mpga,ab=128,channels=2}:http{mux=ts,dst=:8080}'
```

### Team Desktop Streaming

The following VLC command is used at the World Finals to start a stream on port 9090 showing the team's desktop:

```
  vlc -I dummy -q screen:// --screen-fps=30 --sout "#transcode{venc=x264{keyint=15},\
  vcodec=h264,vb=0}:http{mux=ts,dst=:9090/}"
```