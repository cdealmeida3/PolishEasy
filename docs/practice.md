# Practice

Practice is the Games a Learner plays after today's Daily Set. This is how it is built and how to add a Game. The words are in `GLOSSARY.md`; why it has pictures and Bests is ADR-0011.

## The rules every Game keeps

- Practice is open when `CourseEngine.practiceOpen()` says so: today's new or Review-only Daily Set is completed. It locks again at midnight.
- A Game uses only what completed Daily Sets taught: their Items, their Tips' tables and the sentences of their Exercises. `Learned` is the one place that works this out.
- A Round saves nothing but the Best of the way it was played, the way the Game was last played, and one more Round for today. All of it goes to `practice.json` through `PracticeProgress`. Nothing is written to `progress.json`.
- A Round left unfinished is not kept. There is nothing to resume.
- The Sound setting is obeyed. A Game that is only listening says it needs sound; the others play without it.
- Each question is answered once. A right answer moves on by itself after a moment; a wrong one shows the right answer and waits for "Continue". The Quick round is the one Game with a clock: there a right answer brings the next card at once.
- The Best is kept for each way of playing: its key is the Game's id and the ids of the options chosen, such as `verb-challenge:być:type`. Ids are stable; renaming one loses the Bests saved under it.

## In the engine

| What | Where |
| --- | --- |
| `Game`, `GameRules`, `GameSetup`, `Round`, `RoundResult`, `Practice` | `engine/.../Practice.kt` |
| Bests and the day's count | `engine/.../PracticeProgress.kt` |
| The Verb challenge | `engine/.../VerbChallenge.kt` |
| Pictures, decks and Picture cards | `engine/.../Pictures.kt`, `Decks.kt`, `PictureCards.kt` |
| Memory pairs | `engine/.../MemoryPairs.kt` |
| Phrase chains | `engine/.../PhraseChains.kt` |
| Sound gaps | `engine/.../SoundGaps.kt` |
| Numbers (shown as "Number pad") | `engine/.../Numbers.kt` |
| Mój, moja, moje | `engine/.../MojMojaMoje.kt` |
| Spell it | `engine/.../SpellIt.kt` |
| Quick round | `engine/.../QuickRound.kt` |
| Listen and tap | `engine/.../ListenAndTap.kt` |

A Round is an immutable value. Each move returns the next Round and never fails: a move that makes no sense now returns the Round unchanged. `Run<Q>` is the shared shape for a Game that is a list of questions.

`Practice.start` remembers what each Game asked today and hands it to the next Round, which asks something else first where it can, and never opens with the question the last Round opened with.

## In the app

`ui/screens/practice/` holds the Practice screen, the screen before a Round (`GameSetupScreen`), `RoundScreen`, which shows the step of whichever Game is being played, and `RoundResultScreen`. `Games.kt` is the one place that gives a Game its name, its line, its icon and the words for its result.

The view model holds the Round. A screen makes a move by calling the engine's pure function and handing the Round it returns to `CourseViewModel.move`, which ends the Round when it is finished. A board is the exception: its last move should be seen, so the finished Round stays on screen and the screen calls `CourseViewModel.endRound` a moment later.

What a Game offers can depend on sound: `Practice.setup` and `Practice.start` are told whether sound is on, and a way to play that is only listening is not offered without it.

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

A Game that shows Items side by side uses `Learned.unalike` to leave out any that would look or sound like one already there: the same Polish, the same meaning or the same Picture.

## The Games

**Verb challenge** (Verbs). Opens with the first verb table a Tip shows. Ways to play: one verb or, once there are two, "Mixed"; "Choose" or "Type". A Round is three stages: six times a person is shown and the form is picked or typed; then one verb's empty table is filled a row at a time from tiles; then up to four sentences from completed Exercises, each with a gap. The person is always shown with a sentence, because another form could be grammatical too. The result is the answers right first try.

**Picture cards** (Words). Opens with four learned Items that have a Picture. Every card shows a Picture, two across, under the Unit that taught it; tapped, it turns over to the Polish, its meaning and a speaker, and says the word as it turns. "All words" adds the Items with no Picture, each shown by its meaning. Decks as above. Nothing is asked, so it has no Round and no Best, and does not count as a Round played.

**Memory pairs** (Words). Opens with six learned Items. Ways to play: pairs of a picture and its word, a word and its meaning, or a sound and its word (only with sound on); a deck; a board of 6, 9 or 12 pairs. Each is offered only where there is enough to lay it. Tiles lie face down, three across; two are turned up at a time, and Polish is said as its tile turns. A pair stays up. Two that are not a pair turn back after a moment, or at once when another tile is tapped. No two pairs of a board look alike: Items with the same Polish, the same meaning or the same Picture do not share one. The result is the turns taken, and the fewest is the Best.

**Phrase chains** (Phrases). Opens with three sentences built in completed word tile Exercises. Ways to play: the sentence heard (with sound on, where it has a Clip) or read as its meaning; short (up to three words), medium (up to five), long, or mixed, each offered with three sentences or more. A Round is up to five sentences. The words of a sentence lie mixed as cards with the Exercise's distractors; tapped in order they join the chain. A card is right if the chain with it starts a sentence the cards can make: heard, only the sentence that was said; read, any Accepted Answer. A wrong card stays and is a slip. The result is the slips, and the fewest is the Best.

**Listen and tap** (Sounds). Opens with four learned Items that have a Clip, and only with sound on: without it the card says to turn sound on. Ways to play: tap the picture or tap the meaning; a deck. A Round says up to ten words, each once, with four options from the same deck. Nothing of the word is written until it is answered. The result is the answers right first try.

**Sound gaps** (Sounds). Opens with four learned Items that have a Clip and a hard sound, and only with sound on. A word is said and written with a gap where one of a set of spellings that sound alike is written; the Learner chooses the letters. The sets: sz · ś · s, cz · ć · c, ż · ź · z, dz · dź · dż, l · ł, e · ę, a · ą, n · ń. Ways to play: one set, offered with four Items or more, or mixed. A Round is up to ten words. The gap is derived from the Item's text: two letters written for one sound are one piece, a capital is never a gap, and neither is a spelling softened by the i after it (the *s* of *siedem* is said as ś). Only the right letters are ever written into the word, so no invented spelling is shown as one. A gap whose wrong option would spell a real word is never asked: those words are listed in `content/practice/practice.json`, and the [Polish check](polish-check.md) fails until every one is. The result is the answers right first try.

**Numbers** (Numbers; its card says "Number pad", so the group's heading is not repeated). Opens with four learned numbers. A number is an Item whose Picture is a numeral, and "plus" is the Item whose Picture is the sign, so the Game needs no content of its own. Ways to play: the number heard (with sound on) or read as its Polish words; 0 to 10; 0 to 20, once a number above ten is learned; runs of three digits, said as three Clips one after the other; sums of two learned numbers that come to a learned number, once "plus" is learned. The Learner types on a keypad of up to three digits, whose last key checks. A run is typed digit for digit; a number may have a zero in front. A Round is up to ten numbers, six runs or eight sums. The result is the answers right first try.

**Mój, moja, moje** (Words). Opens with two of the words for "my" and four nouns that go with them. A noun is shown, with its Picture when it has one, and the Learner taps the word for "my" that goes with it. A noun is asked only once its own word is learned, so *dziecko* waits for *moje*. Ways to play: the noun written with its meaning, heard (with sound on), or given by its meaning alone. Each word counts the nouns that belong under it so far. Answered, the two words are written together and said, where there is a Clip of them, and if not the noun is. A Round is up to ten nouns. The result is the answers right first try. When a Unit teaches *ten, ta, to*, the same rules can sort under those.

**Spell it** (Words). Opens with four learned Items that can be spelled: one word of small letters, three to nine of them. Ways to play: from the word's Picture or from its meaning; a deck. A Round is up to six words. The word's letters lie mixed as tiles, Polish letters included, never already in order; tapped in order they fill the word's slots. A wrong tile stays and is a slip; where a letter is written twice, either tile of it will do. Whole, the word is said. The result is the words spelled with no slip.

**Quick round** (Words). Opens with six learned Items. A Polish word is shown beside a Picture or a meaning, its own or another word's, and the Learner says "match" or "no match", for 60 seconds or 120. A right answer brings the next card at once. A wrong one shows what the word means and waits for "Continue", while the clock runs, which is also what makes guessing slow. Every word comes round before any comes again. The result is the right answers, and the most is the Best of that time.

The clock is the only one in the app. The engine does not read the time: the screen calls `QuickRound.tick()` once a second, and the Round is finished when no seconds are left. The bar at the top of the Round shows the time left and empties; it moves once a second and nothing loops. When the time runs out the Round ends, even with a wrong answer still showing.

## What Practice needs from the content

`course.json` may name one file, `"practice": "practice/practice.json"`, for what the Games need to know that no Item says. It holds:

- `realWords`, for Sound gaps. After adding or changing Items, run `./gradlew :polishcheck:checkPolish`: it names any new wrong spelling that is a real word, to add to the list.
- `genders`, for Mój, moja, moje: the id of each noun Item to sort and `"m"`, `"f"` or `"n"`. List a noun in the form a dictionary gives (*siostra*, not *siostrę*), one word, and only where "my" with it makes sense. The Polish check confirms each gender against the dictionary.
- `my`: the Item that says "my" for each gender, `{ "m": "moj", "f": "moja", "n": "moje" }`.

A new Unit adds its nouns to `genders`.
