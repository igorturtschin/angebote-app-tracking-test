# mobile-test-app

A test app for checking analytics tracking. The tracking concept and the
app description are in `docs/tracking-concept.md` (the app itself is
described in its Attachment 1). How those events are wired into the code,
and how to check that they go out, is in `docs/implementation-notes.md`.

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

### Co-authorship

The user decides this per commit. The default:

- **Code and technical work** — add a `Co-Authored-By` line naming the
  model that did the work, for example
  `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`. The user sets
  the task and the model writes the code, so the model is a real author.
  The model is named so a reader can see which models were used.
- **Documentation** — no `Co-Authored-By` line. Most of the work here is
  the user's; the model writes and advises, which is help, not
  authorship.

## Build

```
cd android
./gradlew assembleDebug
```

Java and the Android SDK are required. Both come with Android Studio.

Java is often not in PATH. Android Studio brings its own runtime, so point
`JAVA_HOME` at it before the build, for example
`C:\Program Files\Android\Android Studio\jbr`.
