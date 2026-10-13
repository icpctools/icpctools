# CDS web UI

A CDS web front end: a SvelteKit single-page app served by the CDS under `/ui`
(e.g. `/ui/contests/{id}/teams`). It uses the
[`@icpctools/contest-api`](https://www.npmjs.com/package/@icpctools/contest-api) and
[`@icpctools/contest-ui`](https://www.npmjs.com/package/@icpctools/contest-ui) packages, and loads all
data in the browser from the CDS Contest API using the logged-in user's session.

Requires Node 24 and pnpm.

## Development

Start a CDS (default `https://localhost:8443`, or set `CDS_URL`), then:

```bash
pnpm install
pnpm dev
```

and open http://localhost:5173/ui. `/api` requests are proxied to the CDS.

## Building

`pnpm build` writes the app to `../WebContent/ui`, where it is picked up by the CDS war (the `ui`
target in `CDS/build.xml` runs this automatically).
