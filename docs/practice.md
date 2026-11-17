# Practice

Practice is the Games a Learner plays after today's Daily Set. This is how it is built and how to add a Game. The words are in `GLOSSARY.md`; why it has pictures and Bests is ADR-0011.

## The rules every Game keeps

- Practice is open when `CourseEngine.practiceOpen()` says so: today's new or Review-only Daily Set is completed. It locks again at midnight.
- A Game uses only what completed Daily Sets taught: their Items, their Tips' tables and the sentences of their Exercises. `Learned` is the one place that works this out.
- A Round saves nothing but the Best of the way it was played, the way the Game was last played, and one more Round for today. All of it goes to `practice.json` through `PracticeProgress`. Nothing is written to `progress.json`.
- A Round left unfinished is not kept. There is nothing to resume.
- The Sound setting is obeyed. A Game that is only listening says it needs sound; the others play without it.
- Each question is answered once. A right answer moves on by itself after a moment; a wrong one shows the right answer and waits for "Continue".
- The Best is kept for each way of playing: its key is the Game's id and the ids of the options chosen, such as `verb-challenge:być:type`. Ids are stable; renaming one loses the Bests saved under it.

## In the engine

| What | Where |
| --- | --- |
| `Game`, `GameRules`, `GameSetup`, `Round`, `RoundResult`, `Practice` | `engine/.../Practice.kt` |
| Bests and the day's count | `engine/.../PracticeProgress.kt` |
| The Verb challenge | `engine/.../VerbChallenge.kt` |
| Pictures, decks and Picture cards | `engine/.../Pictures.kt`, `Decks.kt`, `PictureCards.kt` |

A Round is an immutable value. Each move returns the next Round and never fails: a move that makes no sense now returns the Round unchanged. `Run<Q>` is the shared shape for a Game that is a list of questions.

`Practice.start` remembers what each Game asked today and hands it to the next Round, which asks something else first where it can, and never opens with the question the last Round opened with.

## In the app

`ui/screens/practice/` holds the Practice screen, the screen before a Round (`GameSetupScreen`), `RoundScreen`, which shows the step of whichever Game is being played, and `RoundResultScreen`. `Games.kt` is the one place that gives a Game its name, its line, its icon and the words for its result.

The view model holds the Round. A screen makes a move by calling the engine's pure function and handing the Round it returns to `CourseViewModel.move`, which ends the Round when it is finished.

## Adding a Game

1. Add it to `Game` with a stable id and its group.
2. Write its `GameRules` and its `Round` in a file of their own, and add the rules to the list in `Practice`.
3. Test it by behaviour in the engine, over the fixture in `PracticeFixture.kt`, and add it to `PracticeOverAuthoredContentTest`, which plays every Game at every point on the real Path.
4. Give it words and an icon in `Games.kt`, a step in `RoundScreen`, and pictures of every state in `ScreenshotsTest`.
5. Add it to this file and to the list in `docs/design.md`.

## Pictures

A Picture stands for an Item in the Games. `content/pictures/pictures.json`, which `course.json` names as `"pictures"`, lists them:

```json
{ "item": "mama", "image": "woman-feeding-baby.webp" }
{ "item": "siedem", "glyph": "7" }
```

An `image` is a file beside the list. A `glyph` is a numeral or a sign the app draws in its own typeface, in the brand's colour: the numbers are numerals, not images.

The images are Microsoft's Fluent Emoji in the 3D style (MIT licence; its text is packaged as `res/raw/fluent_emoji_mit.txt`). To add one:

1. Find the emoji in `github.com/microsoft/fluentui-emoji` under `assets/`.
2. Add a line to `tools/pictures/sources.json`: the file name to make, and the path of the 3D PNG under `assets/`. People are under `<Name>/Default/3D/`, in the default yellow.
3. Run `python3 tools/make-pictures.py` from the repository root. It downloads what is missing and writes it at 256 px as WebP. The icon of each Game is made the same way, into `res/drawable-nodpi`.
4. Add the line to `pictures.json` and run `./gradlew :engine:checkContent`.

**Honest pictures only.** A Picture must show the word's meaning to someone who has not learned the word. Pronouns, verb forms and most small words have none: leave an Item without a Picture rather than force one. Two Items of one Unit may not share a Picture, because a Game could not tell them apart; where two words would get look-alike faces, such as "dad" and "brother", choose pictures that differ in what they show, or picture only one.

The checker's rules for pictures are in [content-checks.md](content-checks.md).

## Decks

A Game played with words offers decks to choose from: everything, each Unit the Learner has completed a Daily Set of, the latest words (those of the last three Daily Sets) and the tricky words (Items in the first two boxes of the Review schedule, which is read and never changed). A deck is offered only when it has enough Items for the Game and is not everything under another name. `Decks.kt` has the rule.

## The Games

**Verb challenge** (Verbs). Opens with the first verb table a Tip shows. Ways to play: one verb or, once there are two, "Mixed"; "Choose" or "Type". A Round is three stages: six times a person is shown and the form is picked or typed; then one verb's empty table is filled a row at a time from tiles; then up to four sentences from completed Exercises, each with a gap. The person is always shown with a sentence, because another form could be grammatical too. The result is the answers right first try.

**Picture cards** (Words). Opens with four learned Items that have a Picture. Every card shows a Picture, two across, under the Unit that taught it; tapped, it turns over to the Polish, its meaning and a speaker, and says the word as it turns. "All words" adds the Items with no Picture, each shown by its meaning. Decks as above. Nothing is asked, so it has no Round and no Best, and does not count as a Round played.
