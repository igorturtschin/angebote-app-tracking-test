# mobile-test-app

A test app for checking analytics tracking. This branch holds the app
**without any tracking**; it is the starting point of every tracking
branch. The app is described in `docs/app-description.md`.

## Language

Inside this folder **everything is in English**: code, variable names,
comments, documentation, commit messages. The level is B2: simple
sentences, common words, no rare vocabulary.

The only exception is **text the user sees on the phone screen: that is in
German**. The app is shown to a German-speaking audience.

The other folders of the project are in Russian. This rule applies here
only.

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

Do not add an analytics SDK to this branch. Each SDK goes on its own
branch — that is the point of the app: to measure what it costs to add
tracking to code that is already done. If the SDK went in here, that cost
could not be seen.
