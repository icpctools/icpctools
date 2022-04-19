ICPC Tools Account Types
========================

The [Contest API specification](https://ccs-specs.icpc.io/draft/contest_api) defines 5 standard account types:
`team`, `judge`, `admin`, `analyst`, and `staff`. The ICPC Tools adds 4 more: `spectator`, `balloon`, `presAdmin`, and `public`.
This document covers the differences between accounts and what is visible to each type of account.

There are three priviledged accounts (`admin`, `staff`, and `judges`) that have access to most contest data. All other
account types are best described by comparing to `public` data, i.e. information that is truly public and visible to all people,
including spectators and teams. This data includes:

- Contest
- Contest state
- Groups *
- Teams *
- Organizations
- Judgement Types
- Problems (after contest start)
- Submissions *
- Judgements (for submissions before the freeze) *
- Broadcast clarifications
- Your own account (if logged in)
- People
- Awards (until the freeze)
- CDS extensions (Map Info amd Start Status)

(*) Teams can be marked hidden (and groups could be hidden in a previous version of the spec). Hidden teams and
all downstream information about them (submissions, judgements, clarifications, etc) are not visible to the
public.

### File References

Priviledged accounts can access all file references. Other account types can access file references (e.g. logos,
photos, banners) with the following exceptions:
- Submission files are only accessible by the team that submitted them.
- Video and audio streams are only accessible until the freeze.
- Team backups, key logs, and tool data are not accessible.

### Account Types

| Name       | Description                 | Visibility
| :--------- | :-------------------------- | :--------
| admin      | CDS administrators          | All contest data
| staff      | Contest staff               | Same as admin, but no account passwords   
| judge      | Contest judges              | Same as staff
| public     | Default visibility          | See above - data that is visible to anyone, any time
| spectator  | Contest spectators          | Same as public but adds commentary
| presAdmin  | Presentation administrators | Same as spectator
| team       | Individual team accounts    | Same as public, but can see all team activity (clarifications to/from the team, team judgements even during the freeze)
| balloon    | Balloon printer             | Used at World Finals for the balloon printer. Same as public, but can see judgements during the freeze until a team has reached three solved problems
| analyst    | Contest analysts            | Same as public, but can also see all clarifications, and runs until the freeze
