# mobile-test-app

A test app for checking analytics tracking. The tracking concept and the
app description are in `docs/tracking-concept.md` (the app itself is
described in its Appendix A).

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

## What not to do

Do not add analytics SDKs. The user installs them in a separate step —
that is the point of the app: to measure what it costs to add tracking to
code that is already done.
