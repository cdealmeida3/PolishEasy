# Practice

Practice is the Games a Learner plays after today's Daily Set. This is how it is built and how to add a Game. The words are in `GLOSSARY.md`; why it has pictures and Bests is ADR-0011.

## The rules every Game keeps

- Practice is open when `CourseEngine.practiceOpen()` says so: always at the Learner's own pace, and with One a day once today's new or Review-only Daily Set is completed, locking again at midnight (ADR-0012).
- At the Learner's own pace Daily Sets can be completed out of order. An Item belongs to the first Daily Set on the Path that has it (`Learned.introduced`), so a Game grouped by Unit leaves out an Item whose first Daily Set was skipped until that one is done. A Game's pool of Items is every Item of every completed Daily Set, so the Item is still played.
- A Game uses only what completed Daily Sets taught: their Items, their Tips' tables and the sentences of their Exercises. `Learned` is the one place that works this out.
- A Round saves nothing but the Best of the way it was played, the way the Game was last played, and one more Round for today. All of it goes to `practice.json` through `PracticeProgress`. Nothing is written to `progress.json`.
- A Round left unfinished is not kept. There is nothing to resume.
- The Sound setting is obeyed. A Game that is only listening says it needs sound; the others play without it.
- Each question is answered once. A right answer moves on by itself after a moment; a wrong one shows the right answer and waits for "Continue". The Quick round and Register sort are the Games with a clock: there a right answer brings the next card at once.
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
| Mój, moja, moje and Ten, ta, to (`GenderSort`) | `engine/.../MojMojaMoje.kt` |
| Label the picture | `engine/.../LabelIt.kt` |
| Who's who | `engine/.../WhosWho.kt` |
| Spell it | `engine/.../SpellIt.kt` |
| Quick round | `engine/.../QuickRound.kt` |
| Register sort | `engine/.../RegisterSort.kt` |
| Ankieta, and its form and people | `engine/.../Ankieta.kt` |
| Listen and tap | `engine/.../ListenAndTap.kt` |
| Chat, and its scripts | `engine/.../Chat.kt` |
| Hearing Polish said (`saidAs`) | `engine/.../Speaking.kt` |

A Round is an immutable value. Each move returns the next Round and never fails: a move that makes no sense now returns the Round unchanged. `Run<Q>` is the shared shape for a Game that is a list of questions.

`Practice.start` remembers what each Game asked today and hands it to the next Round, which asks something else first where it can, and never opens with the question the last Round opened with.

## In the app

`ui/screens/practice/` holds the Practice screen, the screen before a Round (`GameSetupScreen`), `RoundScreen`, which shows the step of whichever Game is being played, and `RoundResultScreen`. `Games.kt` is the one place that gives a Game its name, its line, its icon and the words for its result.

The view model holds the Round. A screen makes a move by calling the engine's pure function and handing the Round it returns to `CourseViewModel.move`, which ends the Round when it is finished. A board and a chat are the exceptions: their last move should be seen, so the finished Round stays on screen and the screen calls `CourseViewModel.endRound` a moment later.

What a Game offers can depend on sound: `Practice.setup` and `Practice.start` are told whether sound is on, and a way to play that is only listening is not offered without it. They are told the same way whether the phone can hear Polish said (`speech`), and a way to play by speaking is not offered without it.

## Speaking

*Powtórz na głos* is the Learner saying Polish aloud into the microphone. `Speech` (`app/.../Speech.kt`) is the one way the screens hear: Android's `SpeechRecognizer` in pl-PL, offline where the phone has it, with five guesses. It is provided through `LocalSpeech`, and the tests provide `FakeSpeech`, which they speak into. `MicButton` is the microphone; it shows nothing where speech is not `available`, which is when the phone has no recognizer or the Learner refused the microphone. The microphone is asked for on its first tap only.

What was heard is matched by the engine's `saidAs`: case, punctuation and Polish marks do not count, the words must come in the same number and order, and one word may be a little off (two letters, and less than half the word). Speaking never blocks: wherever the microphone is the answer, choosing instead is there too, and hearing nothing changes nothing.

It is in three places: the Chat's "say" way; step mode of a Class recording, where the turn holds while the Learner speaks and what was heard is shown, without a verdict since a recording's lines have no text; and after a listen-and-choose Exercise is answered, where the Learner may say the answer and see whether it was understood. Nothing about it is saved.

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

**Verb challenge** (Verbs). Opens with the first verb table a Tip shows. Ways to play: one verb; a conjugation, such as *-ę, -isz/-ysz*, once two of its verbs are learned, which mixes only those; or, once there are two verbs, "Mixed"; "Choose" or "Type". A table's conjugation is read from its *ja* and *ty* forms (`ConjugationTable.group`): *-ę, -isz/-ysz*, *-ę, -esz*, *-am, -asz* or *-em, -esz*; a verb such as *być* follows none. A Round is three stages: six times a person is shown and the form is picked or typed; then one verb's empty table is filled a row at a time from tiles; then up to four sentences from completed Exercises, each with a gap. The person is always shown with a sentence, because another form could be grammatical too. The result is the answers right first try.

**Picture cards** (Words). Opens with four learned Items that have a Picture. Every card shows a Picture, two across, under the Unit that taught it; tapped, it turns over to the Polish, its meaning and a speaker, and says the word as it turns. "All words" adds the Items with no Picture, each shown by its meaning. Decks as above. Nothing is asked, so it has no Round and no Best, and does not count as a Round played.

**Memory pairs** (Words). Opens with six learned Items. Ways to play: pairs of a picture and its word, a word and its meaning, or a sound and its word (only with sound on); a deck; a board of 6, 9 or 12 pairs. Each is offered only where there is enough to lay it. Tiles lie face down, three across; two are turned up at a time, and Polish is said as its tile turns. A pair stays up. Two that are not a pair turn back after a moment, or at once when another tile is tapped. No two pairs of a board look alike: Items with the same Polish, the same meaning or the same Picture do not share one. The result is the turns taken, and the fewest is the Best.

**Phrase chains** (Phrases). Opens with three sentences built in completed word tile Exercises. Ways to play: the sentence heard (with sound on, where it has a Clip) or read as its meaning; short (up to three words), medium (up to five), long, or mixed, each offered with three sentences or more. A Round is up to five sentences. The words of a sentence lie mixed as cards with the Exercise's distractors; tapped in order they join the chain. A card is right if the chain with it starts a sentence the cards can make: heard, only the sentence that was said; read, any Accepted Answer. A wrong card stays and is a slip. The result is the slips, and the fewest is the Best.

**Listen and tap** (Sounds). Opens with four learned Items that have a Clip, and only with sound on: without it the card says to turn sound on. Ways to play: tap the picture or tap the meaning; a deck. A Round says up to ten words, each once, with four options from the same deck. Nothing of the word is written until it is answered. The result is the answers right first try.

**Sound gaps** (Sounds). Opens with four learned Items that have a Clip and a hard sound, and only with sound on. A word is said and written with a gap where one of a set of spellings that sound alike is written; the Learner chooses the letters. The sets: sz · ś · s, cz · ć · c, ż · ź · z, dz · dź · dż, l · ł, e · ę, a · ą, n · ń. Ways to play: one set, offered with four Items or more, or mixed. A Round is up to ten words. The gap is derived from the Item's text: two letters written for one sound are one piece, a capital is never a gap, and neither is a spelling softened by the i after it (the *s* of *siedem* is said as ś). Only the right letters are ever written into the word, so no invented spelling is shown as one. A gap whose wrong option would spell a real word is never asked: those words are listed in `content/practice/practice.json`, and the [Polish check](polish-check.md) fails until every one is. The result is the answers right first try.

**Numbers** (Numbers; its card says "Number pad", so the group's heading is not repeated). Opens with four learned numbers. A number is an Item whose Picture is a numeral, and "plus" is the Item whose Picture is the sign, so the Game needs no content of its own. Ways to play: the number heard (with sound on) or read as its Polish words; 0 to 10; 0 to 20, once a number above ten is learned; 20 to 100, once a ten above twenty is learned, which asks the learned tens and every number made of a learned ten and a learned unit, said as the two words one after the other (*czterdzieści dwa*); runs of three digits, said as three Clips one after the other; sums of two learned numbers that come to a learned number, once "plus" is learned. The Learner types on a keypad of up to three digits, whose last key checks. A run is typed digit for digit; a number may have a zero in front. A Round is up to ten numbers, six runs or eight sums. The result is the answers right first try.

**Mój, moja, moje** (Words). Opens with two of the words for "my" and four nouns that go with them. A noun is shown, with its Picture when it has one, and the Learner taps the word for "my" that goes with it. A noun is asked only once its own word is learned, so *dziecko* waits for *moje*. Ways to play: the noun written with its meaning, heard (with sound on), or given by its meaning alone. Each word counts the nouns that belong under it so far. Answered, the two words are written together and said, where there is a Clip of them, and if not the noun is. A Round is up to ten nouns. The result is the answers right first try.

**Ten, ta, to** (Words). Chapter 3's Game: Mój, moja, moje's rules, sorting under the words for "this" (`this` in `practice.json`). It opens with two of *ten, ta, to* and four nouns, and its Best is its own.

**Label the picture** (Words). Chapter 3's Game, after the book's labelled pictures of a classroom. Opens with six learned Items that have an image (a numeral is no picture to label). Ways to play: tap the labels or type them; a deck. A Round is one board of six pictures that look and read unalike, each with an empty label under it, asked one at a time in the board's order; the meaning of the one asked is written under the board. Tapping, the six labels lie mixed below as word tiles: a right one sticks in green and is said, and a wrong one puts the right label on in amber and stays below, waiting for "Continue". Typed, the word is checked as a typed answer is, Polish marks forgiven. The result is the labels right first try.

**Spell it** (Words). Opens with four learned Items that can be spelled: one word of small letters, three to nine of them. Ways to play: from the word's Picture or from its meaning; a deck. A Round is up to six words. The word's letters lie mixed as tiles, Polish letters included, never already in order; tapped in order they fill the word's slots. A wrong tile stays and is a slip; where a letter is written twice, either tile of it will do. Whole, the word is said. The result is the words spelled with no slip.

Spelled aloud, a third way to play with sound on, nothing of the word shows: the names of its letters are said one after another, as the book's chapter 1 spells a word (*ka, o, te*), and a button says them again. The Learner spells with the same tiles, and the meaning is written once the word is whole. Only words whose every letter has a named Clip are offered. The names are `letters` in `practice.json`.

**Chat** (Phrases). A messenger conversation with someone, from a script in `content/kpk1/chats/`. Opens with a script whose every Item is learned. Ways to play: the script; reply by choosing from the replies, by typing, or by saying it (only where the phone can hear). The person's lines arrive one at a time, typing first, and are said. Each reply is answered once. A wrong one, often in the wrong register, shows the right one and a gentle note on why, and the chat goes on with the right one. Typed replies are checked like a typed answer against the Accepted Answers of each right reply; a typed or said wrong reply that is in the script gets its note. A right reply may lead to its own next turn, so a chat can branch. The last line stands a moment before the result, which is the replies right first time.

**Quick round** (Words). Opens with six learned Items. A Polish word is shown beside a Picture or a meaning, its own or another word's, and the Learner says "match" or "no match", for 60 seconds or 120. A right answer brings the next card at once. A wrong one shows what the word means and waits for "Continue", while the clock runs, which is also what makes guessing slow. Every word comes round before any comes again. The result is the right answers, and the most is the Best of that time.

The clock is the only kind in the app. The engine does not read the time: the screen calls `tick()` of a `TimedRound` (the Quick round, Register sort, Opposites snap) once a second, and the Round is finished when no seconds are left. The bar at the top of the Round shows the time left and empties; it moves once a second and nothing loops. When the time runs out the Round ends, even with a wrong answer still showing.

**Register sort** (Phrases). Chapter 1's Game. Opens with six learned Items the content marks with a register, of two registers at least. A phrase is shown alone and the Learner sorts it by who it is said to: "Pan or pani" (O), "A friend" (N) or "Anyone" (U), the last only once a universal phrase is learned. Each button counts what it holds. The meaning is not shown at first, because it often gives the register away. A right answer brings the next phrase at once; a wrong one shows the phrase's register chip and meaning, says it, and waits for "Continue", while the clock runs. It keeps the Quick round's clock, 60 seconds or 120, and every phrase comes round before any comes again. The result is the right answers, and the most is the Best of that time.

**Opposites snap** (Words). Chapter 4's Game, after the book's pairs of opposite adjectives. Opens with three learned pairs of opposites, listed under `opposites` in `practice.json`; both words of a pair must be learned. Two adjectives are shown one above the other, in the same gender (both masculine, feminine or neuter, at random), and the Learner says "Opposites!" or "Not opposites". About half are a pair; the others put a word beside another word of the learned pairs, never one that is its opposite by any pair. Either word of a pair may come first. A right answer brings the next pair at once; a wrong one says what the word's opposite really is, with both meanings, and waits for "Continue", while the clock runs. It keeps the Quick round's clock, 60 seconds or 120, and every pair comes round before any comes again. The result is the right answers, and the most is the Best of that time.

**Who's who** (Words). Chapter 5's Game, after the book's *Kim ona jest?*. Opens once the Item `jobs.after` names (*kim*, which brings the instrumental) is learned, with four learned jobs listed under `jobs.items` in `practice.json`. A job is a noun Item for a man or a woman (its gender under `genders`) whose first form is its instrumental: *lekarz*, *lekarzem*. Ways to play: the job by its Picture (once four learned jobs have one) or written with its meaning. Under *Kim on jest?* or *Kim ona jest?* the Learner finishes *On jest …* with one of four: the instrumental, the job as it is written alone, and two other jobs in the instrumental, of the same gender first. Once answered, the whole sentence is written (*On jest lekarzem.*) and said where there is a Clip of it; choosing the word as it is written alone adds that after *jest* a job takes the instrumental. A right answer moves on by itself; a wrong one waits for "Continue". A Round is up to ten jobs. The result is the answers right first try.

**Ankieta** (Phrases). Chapter 2's Game, after the book's *dane personalne*. Opens once three people of `content/kpk1/ankieta.json` can be met: every Item a person lists is learned. Ways to play: their introductions heard (with sound on, where every line has a Clip) or read. A Round meets three people, those not met today first. Each introduces themselves in a few lines, and the Learner fills in a form for them a field at a time: *Imię*, *Skąd jest?*, *Gdzie mieszka?*, *Numer telefonu*, *Dlaczego uczy się polskiego?*. Each field offers the right answer among those of the other people who can be met and the wrong ones written for it, at most four, two across. Heard, all a person says is played when they appear and again from the play button, and is written only once the form is filled in; read, it is one chat bubble from the start. Once the form is filled in, what was said is shown with its meaning. A right answer fills the field and moves on by itself; a wrong one fills it with the right answer in amber and waits for "Continue". The result is the fields right first try.

## What Practice needs from the content

`course.json` may name one file, `"practice": "practice/practice.json"`, for what the Games need to know that no Item says. It holds:

- `realWords`, for Sound gaps. After adding or changing Items, run `./gradlew :polishcheck:checkPolish`: it names any new wrong spelling that is a real word, to add to the list.
- `genders`, for Mój, moja, moje: the id of each noun Item to sort and `"m"`, `"f"` or `"n"`. List a noun in the form a dictionary gives (*siostra*, not *siostrę*), one word, and only where "my" with it makes sense. The Polish check confirms each gender against the dictionary.
- `my`: the Item that says "my" for each gender, `{ "m": "moj", "f": "moja", "n": "moje" }`.
- `this`: the Item that says "this" for each gender, `{ "m": "ten", "f": "ta", "n": "to" }`, for Ten, ta, to. The Polish check confirms each against the dictionary as it does `my`.
- `letters`: each small letter of the alphabet and its Polish name, `{ "a": "a", "b": "be", "ą": "a z ogonkiem" }`, for Spell it aloud. Each name is a spoken text, so `./gradlew :audio:makeClips` records it; the content check fails on a key that is not one small letter or on a blank name.

- `opposites`: pairs of adjective Item ids that are opposites, `[["nowy", "stary"], ["mlody", "stary"]]`, for Opposites snap. A word may be in more than one pair. The content check fails on a pair that is not two different defined Items, or one given twice.

- `jobs`: for Who's who, `{ "after": "kim", "items": ["lekarz", "lekarka"] }`: the Item the Game waits for, and the job Items, each with its instrumental as its first form and its gender, `"m"` or `"f"`, under `genders`. The content check fails on an Item that is not defined, a job with no forms or no such gender, or a job listed twice.

A new Unit adds its nouns to `genders`, its opposite adjectives to `opposites`, and its jobs to `jobs`.

## Chat scripts

`course.json` lists the scripts under `"chats"`. A script has an `id`, the `person`, a `title` and a `scene` in each Base Language (who the Learner is and where, and how to address the person), the `register` the chat is held in, the `items` it uses, and its `turns`. Each turn has an `id`, what the person `says` and its `meaning`, and two to four `replies`. A reply has its `text` and `meaning`; a right one is marked `right` and names the turn it leads to with `next` (none ends the chat), and may list `acceptedAnswers` for typing and saying. A wrong one may name its `register` and a `note` in each Base Language, its Polish marked with `*` as in a Tip. The last turn has no replies.

Write it fresh, with only the Items listed, and list every Item it uses: `PracticeOverAuthoredContentTest` checks that its words come from them. `./gradlew :engine:checkContent` checks the rest: every turn reachable from the first, an end, a right reply in every turn that has replies, no wrong reply that leads on. Its Polish goes through the Polish check and review like a Daily Set's, and its lines and right replies need Clips (`./gradlew :audio:makeClips`).

## The Ankieta's people

`course.json` names one file, `"ankieta": "kpk1/ankieta.json"`. It holds the form's `fields`, in order, each an `id`, its Polish `label` and its `meaning` in each Base Language; and its `people`. A person has an `id`, the `items` they need, what they `says` (lines, each a `text` and its `meaning`), the form filled in from it as `answers` by field id, and may give `wrong` answers for a field, such as a phone number a digit off. A person may leave a field out; the form then has no row for it.

Write people fresh, in the chapter's words, and list every Item they use: `PracticeOverAuthoredContentTest` checks their words come from them, their own name aside. An answer is about the person, in the third person (*bo jej mama jest z Polski*), and reads right after its field's label. A phone number is written in digits in pairs (`21 14 28`) and said in words in a line. `./gradlew :engine:checkContent` checks that every field answered is on the form and has at least two other answers to choose among, from the other people or `wrong`. The lines need Clips (`./gradlew :audio:makeClips`), and the labels, lines and answers go through the Polish check and review like a Daily Set's. A later chapter adds people, and fields where the book teaches more to ask (*wiek*, *zawód*).
