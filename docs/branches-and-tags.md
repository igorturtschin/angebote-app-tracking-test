# Branches and tags

Where the work stands. Check this file before every commit: the change
belongs on the branch this map names for it.

State: 2026-09-03.

## Branch names

Format `<version>/<variant>`, lower case, slash as separator.

| Branch | What |
|---|---|
| `v1/no-tracking` | app v1 without an SDK — the common base |
| `v1/amplitude` | v1, Amplitude goes in here |
| `main` | v1 with Firebase / GA4; there is no `v1/firebase` branch |
| `v2/no-tracking` | app v2 without an SDK (later) |
| `v2/amplitude` | v2 + Amplitude (later) |
| `v2/firebase` | v2 + Firebase / GA4 (later) |

## Tree

```
b1d99c0  shared history of v1
   │
   ├─● 263d8b3   v1/no-tracking       tag v1/base
   │   │
   │   ├─► v1/amplitude               ← in progress: Amplitude goes in here
   │   │
   │   └─► (later) v2/no-tracking ─┬─► v2/amplitude
   │                               └─► v2/firebase
   │
   └─● 5930871                        tag v1/firebase
     │
     ● 9fc161e   main                 tag shared/v1-firebase-2026-09-02
```

## Tags

| Tag | Commit | Points at |
|---|---|---|
| `v1/base` | `263d8b3` | app v1 without tracking — the starting point of every branch |
| `v1/firebase` | `5930871` | app v1 with Firebase / GA4, first finished version |
| `shared/v1-firebase-2026-09-02` | `9fc161e` | state handed over on 2026-09-02 (= tip of `main`) |

## Rules

- Nothing goes into `main`. It stays on the finished Firebase / GA4 line.
- A link given to someone outside carries the commit hash, not the tag: a
  tag can be moved, a hash cannot.
