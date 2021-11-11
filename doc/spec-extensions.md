ICPC Tools Spec Extensions
==========================

The ICPC Tools support the full [Contest API specification](https://ccs-specs.icpc.io/draft/contest_api), but some
features require additional contest objects or attributes that aren't in the specification.
This document lists what they are and how to use them.

As per the contest spec, additional objects and attributes are allowed, and tools that don't support them simply ignore
them. All of these features are independent, so you can decide which to use (or not) as your contest requires.

## Contest Data and Priority

The [Contest Archive Format](https://ccs-specs.icpc.io/draft/contest_archive_format), or CAF, is a specification for how to
lay out contest data on disk. Every contest configured on the CDS has a location that points to where you have - or
want to store - a contest archive.

The CDS is typically used as a proxy to a CCS. If your CCS has all the contest data you need, there is no need to
pre-populate the CDS's local contest archive. As data is read from the CCS, the event feed will be cached along with
any file references. This reduces load on the CCS and means the CDS can continue to serve data even if the connection
to the CCS is lost or the CCS is stopped.

If you want to provide additional contest data that the CCS is _not_ serving (e.g. the CCS does not support team photos,
organization logos are not provided, or you want to use one of the spec-extensions below), place the files locally in
the corresponding CAF structure and restart the CDS. The CDS will always read local data before connecting to
the CCS. If the CCS provides an object with the same type and id, local attributes are used as defaults. e.g. if
there is a team defined in the CAF with name and photo, and the CCS provides a team with the same id that has a
name but no photo, the CDS will serve the team with the name as defined by the CCS, but including the local photo.

### Multiple data sources

In some cases you may use contest data from multiple sources: e.g. registration data from CMS, a floor map crafted
by hand, or additional organization locations from a script. In order to avoid having to merge these files the CDS allows
you to have any number of additional contest archives folders. These folders must be sub-folders of the main contest archive
named `extend-<something>` and each subfolder must also follow the contest archive format.

### Floor map

To show a contest floor map in the balloon printer or use the contest floor presentations, additional data is needed:
what size the team desks and team floor area is, where teams are located, and where balloons (problems) and the printer
are. In order to do this there are additional attributes on teams and problems, and a separate map-info object to store
everything else. 

All measurements are in meters, with the origin at the top-left corner. Angles are in degrees, with 0 degrees east
and increasing toward the north. It is a best practise to create the map from the spectator's viewpoint.

All of this data can be created by hand or other tools and then served by placing the corresponding json files in
an `extend-` folder on the CDS. We typically create a Java class to build the floor, since this allows easy visualization and
modification if anything changes. See the FloorGenerator2020 and FloorGeneratorNAC source files as examples of this:
[Floor generator examples](https://github.com/icpctools/icpctools/tree/main/ContestUtil/src/org/icpc/tools/contest/util/floor)

## Objects with additional attributes

The following contest objects are defined in the Contest API but the tools support additional optional attributes.
Only the additional attributes are shown below, along with minimal examples that include only id and these
attributes. In case of any conflict, the Contest API takes precedence.

### Contest

The ICPC tools support two additional attributes: time multiplier and location.

Time multiplier is purely for testing. It is output from the CDS when replaying past contests at increased speed
and allows clients to keep contest time clocks in sync.

The contest location provides the GPS location of the contest site. This allows you to configure where the contest site
is shown on maps or where balloons 'fly' toward on the balloon map presentation.

Additional JSON attributes of contest objects:

| Name                 | Type          | Description
| :------------------- | :------------ | :----------
| time\_multiplier     | number        | The amount time is sped up, e.g. `2.5` for a contest replaying at 2.5x original speed.
| location             | object        | JSON object as specified in the rows below.
| location.latitude    | number        | Latitude in degrees. Required iff location is present.
| location.longitude   | number        | Longitude in degrees. Required iff location is present.

#### Examples

Request:

`GET https://example.com/api/contests/wf14`

Returned data:

```json
{"id":"wf14",
 "time_multiplier":15,
 "location":{"latitude":47.174,"longitude":27.5699}}
```

### Problems

Problems (aka balloons) can have an x, y location just like teams, but without rotation.

Additional JSON attributes of problem objects:

| Name            | Type          | Description
| :-------------- | :------------ | :----------
| location        | object        | JSON object as specified in the rows below.
| location.x      | number        | Problem x position in meters. Required iff location is present.
| location.y      | number        | Problem y position in meters. Required iff location is present.

#### Examples

Request:

`GET https://example.com/api/contests/wf14/problems`

Returned data:

```json
[{"id":"azulejos","location":{"x":-1.97,"y":23.03}},
{"id":"beautifulbridges","location":{"x":-1.97,"y":21.03}},
{"id":"checks","location":{"x":-1.97,"y":19.03}}]
```

### Groups

Contests and organizations have a logo, groups can have one too. This was added
in a year where we expected to have sponsors for each group award and would have
shown their logo alongside the award. If it exists, the logo will show up in the
resolver screen for the corresponding group award.

Additional JSON attributes of group objects:

| Name           | Type            | Description
| :------------- | :-------------- | :----------
| logo           | array of IMAGE  | A logo for the group.

Request:

`GET https://example.com/api/contests/wf14/groups`

Returned data:

```json
[{"id":"12346","logo":[{"href":"http://example.com/api/contests/wf14/groups/12346/logo/56px","width":56,"height":56}]}]
```

### Award

Awards have one additional attribute, used to decide how to display the award in
the resolver. Other tools are unlikely to use this attribute (unless maybe
useful for deciding how to output awards via HTML/PDF?) but if a CCS supports
PUTting awards it would be useful to include this attribute so that you can run
the resolver directly from the CCS's contest archive or event feed.

Additional JSON attributes of award objects:

| Name               | Type          | Description
| :----------------- | :------------ | :----------
| display_mode       | string        | One of `detail`, `pause`, `list` or `ignore` as described below.

Description of each display mode option:

| Name      | Description
| :-------- | :----------
| detail    | The default if no display_mode exists: shows an award screen with the team picture and award summary.
| pause     | Stop to acknowledge the team, but don't show a separate detail screen.
| list      | Show a list of teams that won the award, used for things like Honourable Mention.
| ignore    | Skip the award entirely.


## New objects

The following contest objects are entirely new and not included in the Contest API specification,
but follow the same rules for behaviour, notifications, and location in the CAF.

### Start status

The countdown status presentation allows a number of items to be displayed along with their
current readiness status sliders: yes, maybe, or no. These can be configured on the CDS Admin web page,
or preconfigured via start-status.json.

JSON attributes of start-status objects:

| Name             | Type          | Description
| :--------------- | :------------ | :----------
| id               | ID            | Identifier.
| label            | string        | Label to show in the UI.
| status           | integer       | One of `0` (no), `1` (maybe), or `2` (yes).

#### Examples

Request:

`GET https://example.com/api/contests/wf14/start-status`

Returned data:

```json
[{"id":"1","label":"Security","status":0},
 {"id":"2","label":"Sysops","status":1}]
```

### Map Info

This object includes all information required to draw a contest floor map, outside of
the team and problem locations which are available directly on those objects.
It includes information on the team desk width and depth, team area width and depth,
spare team desk locations, the open aisles that balloon runners can use (used for
calculating paths), and the printer location. Like state, map-info is a singleton.
Map info is typically stored in map-info.json.

#### Examples

Request:

`GET https://example.com/api/contests/wf14/map-info`

Returned data:

```json
{
    "table_width": "1.8",
    "table_depth": "0.8",
    "team_area_width": "2.2",
    "team_area_depth": "2.0",
    "aisles": [
        {
            "x1": 8.46,
            "y1": 29.05,
            "x2": 12.01,
            "y2": 25.57
        },
        {
            "x1": 8.46,
            "y1": 29.05,
            "x2": 3.49,
            "y2": 29.01
        },
        {
            "x1": 12.01,
            "y1": 25.57,
            "x2": 12.05,
            "y2": 20.59
        },
        ...
    ],
    "spare_teams":[
        { "x": 21.03,
          "y": 3.97,
          "rotation":90},
        ...
    ],
    "printer": {
        "x": 11.03,
        "y": -1.97
    }
}
```