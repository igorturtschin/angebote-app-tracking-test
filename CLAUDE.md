# mobile-test-app

A test app for checking analytics tracking. This branch adds
**Amplitude** to the app. It grows from `v1/no-tracking`, tag `v1/base`.
The tracking concept, and the description of the app inside it, are in
`docs/tracking-concept.md`; the branches and tags in
`docs/branches-and-tags.md`.

## Language

Inside this folder **everything is in English**: code, variable names,
comments, documentation, commit messages. The level is B2: simple
sentences, common words, no rare vocabulary.

The only exception is **text the user sees on the phone screen: that is in
German**. The app is shown to a German-speaking audience.

The other folders of the project are in Russian. This rule applies here
only.

**One exception right now:** `docs/tracking-concept.md` is kept in Russian
while it is being written, because the owner reads it faster that way. Do
not translate it back to English on your own — it gets translated when the
owner asks.

## Commits

Subject: `type: what was done`, imperative mood, lower case, no full stop
at the end, up to 72 characters.

| Type | When |
|---|---|
| `feat` | a new app feature |
| `fix` | a bug fix |
| `docs` | documentation only |
| `refactor` | code rewritten, same behavior |
| `chore` | build, dependencies, moving files around |

After a blank line comes the body: what and why, not how. Also in
English B2.

Before every commit, open `docs/branches-and-tags.md` and check that this
change belongs on the branch you are on. `git branch --show-current` says
where you are.

Before `git push`, check the commit author: run `git log -1 --format=%ae`.
If it is not `293591015+igorturtschin@users.noreply.github.com`, GitHub
rejects the push to protect a private email. Fix it with
`git commit --amend --reset-author`, then push.

## Build

```
cd android
./gradlew assembleDebug
```

Java and the Android SDK are required. Both come with Android Studio.

Java is often not in PATH. Android Studio brings its own runtime, so point
`JAVA_HOME` at it before the build, for example
`C:\Program Files\Android\Android Studio\jbr`.

## What not to do

Do not commit to `main`. It holds the finished Firebase / GA4 line and is
the state shown to people outside.

Do not add a second analytics SDK here. One branch carries one SDK — that
is the point of the app: to measure what it costs to add tracking to code
that is already done. Two SDKs in one branch make that number unreadable.

Do not commit the API key. It lives in `android/api-key.properties`, which
is in `.gitignore`.
