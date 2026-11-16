# Content checks

The **content checker** reads all the Authored Content and lists everything wrong with it. Every build of the app runs it first, so no APK or bundle is ever made from broken content.

## Running it

```
./gradlew :engine:checkContent
```

It also runs by itself before `:app:assembleDebug`, `:app:assembleRelease` and `:app:bundleRelease`, which is how CI runs it. When nothing in `content/` or `released-ids.json` has changed since it last passed, Gradle skips it (`UP-TO-DATE`).

It never stops at the first problem. Each problem is one line:

```
a1/unit-01/day-05.json: a1-u01-d05-e03: Daily Set a1-u01-d05: Exercise a1-u01-d05-e03 lists Item byc-jestx, which is not defined
audio/clips.json: jestes: Clip jestes has no audio file audio/jestes.ogg
Content check failed: 2 problems
```

First the file to open, relative to `content/`, then the identifier concerned (left out when the problem is the file itself), then what is wrong.

## After changing content

1. If you added or changed any Polish text, make its audio with `./gradlew :audio:makeClips`, which needs the voice service's key: see [audio.md](audio.md).
2. If you added Levels, Units, Daily Sets, Exercises or Items, record their identifiers and commit the file:

   ```
   ./gradlew :engine:recordContentIds
   ```

3. Run `./gradlew :engine:checkContent`.
4. If you added or changed any Polish text, a Tip, or the English a Polish text stands for, run `./gradlew :polishcheck:checkPolish`, have what it lists reviewed, and record the review: see [polish-check.md](polish-check.md). CI fails until every sentence has a verdict.

## What it checks

The rules are in two tiers. **Loading rules** are what the course engine needs in order to run; the app applies the same rules when it opens and refuses content that breaks one. **Shipping rules** are what makes content good enough to release; only the checker applies them. Both live in the engine (`CourseLoader.kt` and `ContentCheck.kt`), written once.

### Loading rules

- Every file `course.json` lists is there and decodes. A misspelt field, an unknown Exercise type or an unknown person is reported as a file that cannot be read.
- No two Exercises, two Items or two Clips share an id, and no Daily Set file is listed twice.
- Every `type_answer`, `word_tiles`, `conjugation` and `listen_type` Exercise has at least one Accepted Answer, and none is blank. A `listen_choose` Exercise has text and distractors, with no two options that read the same. A `listen_type` has no blank reading answer, and both listening types have a `meaning`.
- The `correctOption` of a `choose_translation` Exercise is one of its options.
- Every Item an Exercise lists is defined, and every Item has text.
- Every listening Exercise has a Clip for what it speaks.
- A Daily Set has at most 2 Tips, each before a different one of its Exercises.
- Word tiles can build every Accepted Answer; a conjugation sentence has exactly one gap; a conjugation table has a form for every person.
- A Clip has text, an id made of lower-case letters, digits and single hyphens, and no other Clip says the same text.

### Shipping rules

- **Sizes.** A Daily Set has 12 to 18 Exercises and introduces at most 8 new Items. (Constants `DAILY_SET_EXERCISES` and `MAX_NEW_ITEMS`.)
- **Items.** Every Exercise lists at least one Item, and every Item is practised by at least 2 Exercises (`MIN_EXERCISES_PER_ITEM`), so Review has something to vary.
- **Choices.** A `choose_translation` Exercise has at least 2 options, and no two read the same.
- **Base Language.** Everything the Learner reads in the Base Language has English text that is not blank: Level and Unit titles, Tip titles and bodies, the meaning in a conjugation table, the options of `choose_translation`, the prompt of `type_answer` and `word_tiles`, the `meaning` and `sentenceMeaning` of `conjugation`, the `meaning` of `listen_choose` and `listen_type`, and the `meaning` of an Item. (`BASE_LANGUAGES` lists the languages required.)
- **Audio.** Every Polish text the app can speak has a Clip, every Clip has its `.ogg` file beside the manifest, every Clip says something the course speaks, and no other file sits beside the manifest.
- **Belonging.** Every `.json` file under `content/` is listed by `course.json`. A Level has Units and a Unit has Daily Sets. A Unit's id starts with its Level's id and a hyphen, a Daily Set's with its Unit's, and an Exercise's or Tip's with its Daily Set's (`a1` → `a1-u01` → `a1-u01-d05` → `a1-u01-d05-e03`).
- **No id twice.** No two Levels, Units, Daily Sets or Tips share an id.
- **Shipped identifiers.** See below.

When a listed file cannot be read, the rules that count across files (an Item's Exercises, Clips nothing speaks, identifiers that are gone) are skipped until it can, so that one broken file is one problem.

The checker does not judge whether the Polish is correct. That is the [Polish check](polish-check.md). Nor does it judge how the content teaches: the rules for that are in [authoring.md](authoring.md), and `AuthoredContentTest` holds the content to the ones a test can check.

## Identifiers that have shipped

A Learner's saved progress refers to content only by id. If the id of a Daily Set, Exercise or Item changes or disappears after people have it on their phones, their progress on it is silently lost. So `released-ids.json`, at the repository root, records every Level, Unit, Daily Set, Exercise and Item id that has been in a build, and the checker compares the content with it:

- An id in the file that is no longer in the content is reported at `released-ids.json`: it **shipped before and is gone**.
- An id in the content that is not in the file is reported at its content file: it is **not recorded**. `./gradlew :engine:recordContentIds` adds it. The command only ever adds.

A renamed id shows up as both at once.

To remove a shipped id on purpose, delete its line from `released-ids.json` by hand in the same change, where the reviewer of the pull request can see it.

Tip and Clip ids are not recorded: nothing a Learner keeps depends on them. See [ADR-0005](adr/0005-shipped-identifiers-are-recorded-in-a-file.md).
