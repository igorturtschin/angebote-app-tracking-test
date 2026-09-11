# Branches and tags

Where the work stands. Check this file before every commit: the change
belongs on the branch this map names for it.

State: 2026-09-11.

## Branch names

Format `<version>/<variant>`, lower case, slash as separator.

| Branch | What |
|---|---|
| `v1/no-tracking` | app v1 without an SDK — the common base |
| `v1/amplitude` | v1, Amplitude goes in here |
| `main` | v1 with Firebase / GA4; there is no `v1/firebase` branch |
| `v2/amplitude` | app v2 + Amplitude — branched from `v1/amplitude`, same Amplitude project. Finished: app, tracking and concept |
| `v2/firebase` | v2 + Firebase / GA4 (later) |

App v2 (bottom navigation, eight offers in four categories, two lists on
the start screen, five test pages) is built directly on `v2/amplitude`.
There is no `v2/no-tracking`: the SDK install does not change from v1, so
there is nothing new to measure by keeping a tracking-free v2 base.

## Tree

Branches carry no commit hash here: a branch moves with every commit, so a
hash written next to it is wrong again the moment it is written — and the
file cannot name the commit it is part of. Hashes belong to the fixed
points only, and those are in *Tags* below.

```
shared history of v1
   │
   ├─► v1/no-tracking                 tag v1/base
   │   │
   │   └─► v1/amplitude
   │       │
   │       └─► v2/amplitude           tag shared/v2-amplitude-2026-09-11
   │
   └─►                               tag v1/firebase
     │
     ► main                          tag shared/v1-firebase-2026-09-02
                                      (later) ─► v2/firebase
```

## Tags

| Tag | Commit | Points at |
|---|---|---|
| `v1/base` | `263d8b3` | app v1 without tracking — the starting point of every branch |
| `v1/firebase` | `5930871` | app v1 with Firebase / GA4, first finished version |
| `shared/v1-firebase-2026-09-02` | `9fc161e` | state handed over on 2026-09-02 (= tip of `main`) |
| `shared/v2-amplitude-2026-09-11` | — | app v2 with Amplitude, finished on 2026-09-11 (= tip of `v2/amplitude`) |

## Rules

- Nothing goes into `main`. It stays on the finished Firebase / GA4 line.
- A link given to someone outside carries the commit hash, not the tag: a
  tag can be moved, a hash cannot.
