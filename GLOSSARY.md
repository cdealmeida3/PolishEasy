# PolishEasy

An Android app for learning Polish in short daily sessions, starting at A1 and designed to grow to further levels.

## Language

**Learner**:
A person using the app to learn Polish.
_Avoid_: User, student, player

**Base Language**:
The language the Learner already knows, in which instructions and translations are shown. English at first.
_Avoid_: Native language, source language, UI language

**Level**:
A complete body of course material, shown on the Path as a shelf. There are two: **Starter**, the three Units the app began with, and **Polski krok po kroku 1**, the A1 class book Carlos studies from, whose chapters are its Units ([ADR-0013](docs/adr/0013-the-book-is-the-path.md)). At the Learner's own pace every Level is open; with One a day the next opens when the one before is complete.
_Avoid_: Course, grade, difficulty

**Unit**:
A themed group of consecutive Daily Sets within a Level. In the book's Level a Unit is one chapter of the book, with the book's number (0 to 26), its Polish title and colour, and is called a lesson on screen as the book does ("Lekcja 5"). A Unit with no content yet is **coming**: it is on the Path with what it will teach, and has no Daily Sets.
_Avoid_: Module, topic; "chapter" only for the book's Units

**Path**:
The ordered sequence of Daily Sets a Learner works through within a Level, Unit by Unit. Every Learner follows the same Path, starting from its first Daily Set whenever they begin. At the Learner's own pace its order is suggested, not required: any Daily Set on it not yet completed can be started.
_Avoid_: Curriculum, track, journey

**Daily Set**:
One day's worth of Exercises on the Path, designed to take 10 to 15 minutes. Unless the Learner chose One a day, they can start any Daily Set not yet completed, at any time, and as many in a day as they like; with One a day they can start at most one new Daily Set per calendar day, the next one. Only one is in progress at a time. It belongs to the calendar day on which it is completed.
_Avoid_: Lesson, session, daily challenge

**Section**:
A Daily Set of the book's Level: one section of a chapter, with the book's letter (A, B, C) and heading ("Liczebniki 20–100"), its English, and the strands it teaches. On screen a section is called by its letter and heading, never "Daily Set".
_Avoid_: Part, step

**Strand**:
One of the three things the book's contents list for each chapter: K, *komunikacja* (what to say), L, *leksyka* (words) and G, *gramatyka* (grammar). A chapter lists topics under each; a section says which strands it teaches.

**Lesson screen**:
A Unit's own screen, opened from its card on the Path: its banner, strands and look-up words, its sections as a trail, and its Powtórzenie (the Słownik, the Fiszki and the Test).

**Słownik**:
A Unit's word list: every Item a Unit introduces (as rule 1 of authoring does), with its meaning, register and Clip, searchable by Polish or meaning. A chapter of the book introduces again a word met earlier, so its Słownik has every word it uses.
_Avoid_: Glossary, vocabulary

**Fiszki**:
A Unit's words as flashcards, those due for Review first. Turning one and saying whether it was known reschedules the Item as an answer to an Exercise would, so the Fiszki feed Review.
_Avoid_: Cards (Picture cards is a Game)

**Checkpoint**:
The end of a Unit: up to 20 of its Exercises, every kind mixed, each asked once, with a wrong answer not asked again. Called "Test" on screen. It keeps a Best per Unit, in percent, in `checkpoints.json`; at 80 % or more the Unit is sealed. It changes nothing on the Path, in Review or in the Streak, and nothing is locked behind it.
_Avoid_: Exam, quiz

**Register**:
The book's mark on a phrase: O (*oficjalnie*, formal), N (*nieoficjalnie*, informal), U (*uniwersalnie*, both). An Item may carry one; it is shown beside the phrase when it is introduced and in the Słownik.

**Polish instruction**:
An Exercise's instruction as the book words it ("Proszę przetłumaczyć."), one per kind of Exercise, in `content/kpk1/instructions.json`, with its English one tap away and a Clip. Without one, an Exercise shows its English instruction.

**Class recording**:
One of the book's 271 recordings (001 to 271), from the class's `lekcja_NN.zip`. `content/kpk1/tracks.json` lists each with its chapter, section letter (P for Powtórzenie), exercise number, title and length, and `segments.json` its lines, pause to pause, for step mode. The recordings themselves are never in the repository or the app: the Learner adds them on the phone in Settings, Class audio, and they are kept where no backup reaches (ADR-0014). A chapter's **Słuchaj** lists its recordings by section.
_Avoid_: Track (except for the number), audio file, Clip (a Clip is the app's own synthetic sound)

**Step mode**:
The player's *posłuchaj i powtórz*: it plays one line of a Class recording, waits a little longer than the line took for the Learner to say it aloud, then plays the next.

**Powtórz na głos**:
The Learner saying Polish aloud into the microphone, heard by the phone's speech recognizer and matched leniently. It is never required: where it is offered there is always another way on, and without a recognizer or the microphone it is not offered.
_Avoid_: Speech test, pronunciation score

**Listening task**:
A short question on a Class recording, or on some of its lines, written for the app: true or false (*Prawda czy nieprawda?*), a choice, or a short dictation (*Co mówi lektor?*) checked like a typed answer. In `content/kpk1/listening.json`. Answered on the recording's screen, from Słuchaj, never inside a Daily Set, so the Path never depends on the recordings; nothing about it is saved.
_Avoid_: Exercise (an Exercise is part of a Daily Set)

**New Material**:
The part of a Daily Set that is Authored Content, identical for every Learner.

**Item**:
The smallest thing a Learner can know or not know: one word, one phrase, or one verb form such as *jestem*. Every Exercise practises one or more Items, and an Item has several Exercises. Items stay with the Learner across Levels.
_Avoid_: Card (Picture cards is a Game), fact, vocabulary entry

**Review**:
The part of a Daily Set chosen per Learner from Items they previously got wrong or have not seen recently.
_Avoid_: Revision, recap

**Review-only Daily Set**:
A Daily Set made entirely of Review, offered when the Learner has completed every Daily Set on the Path released so far: again as soon as one is finished, or once per day with One a day. It has about 20 Exercises: Items that are due first, then those answered longest ago. It is not on the Path.

**One a day**:
The setting that brings back one new Daily Set per calendar day: only the next one can be started, a Review-only Daily Set is offered once a day, and Practice opens once today's is done. It is off until the Learner turns it on; off, they go at their own pace. The Streak counts the same either way.
_Avoid_: Pacing mode, daily limit

**Tip**:
A short explanation of new grammar or words shown before the Exercises that rely on it.
_Avoid_: Lesson, hint, note

**Grammar table**:
A table in a Tip, drawn as the book's *Komentarze gramatyczne* draw one: columns by gender or person, rows by case or form, endings in red boxes. A verb's conjugation in a Tip is one too.
_Avoid_: Chart, grid

**Callout**:
A Tip set apart as the book sets apart an *Uwaga!* (a trap to watch for) or a *Zapamiętaj!* (a thing to learn by heart).
_Avoid_: Warning, alert, note

**Grammar shelf**:
Every grammar table of the Daily Sets the Learner has completed, by Unit, called *Komentarze gramatyczne* on screen. Opened from Settings, or from a Lesson screen for its own Unit.
_Avoid_: Grammar reference, cheat sheet

**Replay**:
Doing an already completed Daily Set again, started from the Path screen. It has the Daily Set's Tips and New Material but no Review. It does not advance the Path, does not count toward the Streak and does not affect Review, and one left unfinished is not kept.
_Avoid_: Redo, practice mode, Practice (which is the Games)

**Practice**:
The part of the app outside the Path where the Learner plays Games with what they have already learned. It is always open, unless the Learner chose One a day: then it opens each day once today's new or Review-only Daily Set is completed and stays open until midnight. Nothing in it advances the Path, counts toward the Streak or affects Review.
_Avoid_: Arcade, extras, practice mode

**Game**:
One kind of play in Practice, such as the Verb challenge. A Game uses only Items and Tips from Daily Sets the Learner has completed, and opens once they have learned enough for it. Most Games can be played in several ways, such as one verb or all of them, choosing or typing.
_Avoid_: Mini-game, activity, Exercise (which belongs to a Daily Set)

**Chat**:
The Game of a messenger conversation with one person, from a script written for the app. The Learner replies by choosing, typing or saying it; a reply in the wrong register is put right with a note.
_Avoid_: Dialogue (the book's printed dialogues), conversation Exercise

**Ankieta**:
The Game of filling in a form for people who introduce themselves, heard or read: their name, where they are from and live, their phone number and why they learn Polish. Chapter 2's Game.
_Avoid_: Survey, questionnaire, profile

**Label the picture**:
The Game of labelling a board of six pictures one at a time, by tapping the word among the labels below or typing it. Chapter 3's Game.
_Avoid_: Picture quiz, labelling exercise

**Ten, ta, to**:
The Game of sorting learned nouns under the word for "this" of their gender, as Mój, moja, moje sorts them under "my". Chapter 3's Game.
_Avoid_: Gender quiz, demonstratives game

**Opposites snap**:
The Game of saying, against the clock, whether two learned adjectives shown in the same gender are opposites, as *wesoły* and *smutny* are. Chapter 4's Game.
_Avoid_: Antonym quiz, opposites match

**Who said it?**:
The Game of telling who said a sentence in the past from its verb's ending: a man (*byłem*), a woman (*byłam*), a group with a man in it (*byliśmy*) or women only (*byłyśmy*). Chapter 13's second Game.
_Avoid_: Gender quiz, Speaker game

**Who's who**:
The Game of saying who someone is by their job, *on jest lekarzem*, by choosing the job's instrumental after *jest*. Chapter 5's Game.
_Avoid_: Jobs quiz, profession game

**Three groups**:
The Game of sorting a learned verb into the book's three conjugations by how its *ja* and *ty* forms end: *-m, -sz*; *-ę, -esz*; *-ę, -isz / -ysz*. Chapter 6's Game.
_Avoid_: Conjugation quiz, verb sort

**Hobby charades**:
The Game of naming a learned hobby, *jeździć na rowerze*, from its Picture, by choosing or typing. Chapter 6's Game.
_Avoid_: Hobby quiz, activities game

**Family tree**:
The Game of a family tree seen from the Learner, *ja*, where each relative is named, *mój wujek*, *moja ciocia*, or found on the tree. Chapter 11's Game.
_Avoid_: Relatives quiz, family game

**Kiosk till**:
The Game of paying a price said in Polish, *trzy złote i czterdzieści groszy*, exactly, with coins and notes. Chapter 7's Game.
_Avoid_: Shop game, money quiz

**Lotto**:
The Game of ticking numbers from a hundred to a thousand on a card as they are called in Polish. Chapter 7's Game.
_Avoid_: Bingo, number card

**Market list**:
The Game of filling a basket with the goods of a shopping list said in Polish, as many of each as it asks: *dwie gruszki i trzy banany*. Chapter 9's Game.
_Avoid_: Shopping game, counting quiz

**Negation flip**:
The Game of picking, against the clock, the right way to say a sentence with *nie*: *Lubię kawę.* is *Nie lubię kawy.*, not *Nie lubię kawę.* Chapter 10's Game.
_Avoid_: Genitive quiz, negation drill

**Plate builder**:
The Game of putting on a plate the foods of a dish ordered in Polish, *chleb z masłem*, or of two, *i herbata z cytryną*. Chapter 8's Game.
_Avoid_: Breakfast game, food quiz

**Sort the aisles**:
The Game of sorting learned products into the supermarket's aisles, *nabiał*, *pieczywo*, *słodycze*, against the clock. Chapter 9's Game.
_Avoid_: Supermarket game, aisle quiz

**Take the order**:
The Game of writing each guest's dish on a waiter's pad as a table orders in Polish: *Dla mnie żurek. A dla mnie to samo.* Chapter 10's Game.
_Avoid_: Restaurant game, waiter quiz

**Story strip**:
The Game of putting a short story's pictures in order as it is heard or read in the past: *Najpierw wstałam o siódmej. Potem umyłam zęby.* Chapter 13's Game.
_Avoid_: Comic game, Picture order

**Tell the time**:
The Game of saying a time on a clock face as the book says it, *pięć po ósmej*, *wpół do dziewiątej*, *za kwadrans dziewiąta*, or finding the face for a time said. Chapter 12's Game.
_Avoid_: Clock game, Clock quiz

**Times of day**:
The Game of saying, against the clock, when in the day a time on the clock is: 7:30 is *rano*, 21:00 *wieczorem*, 3:00 *w nocy*. Chapter 11's second Game.
_Avoid_: Clock quiz, time drill

**Register sort**:
The Game of sorting learned phrases by Register, against the clock: to *pan* or *pani* (O), to a friend (N) or to anyone (U).
_Avoid_: Formality quiz

**Round**:
One play of a Game from start to result. A Round left unfinished is not kept.
_Avoid_: Match, session, level

**Best**:
The Learner's best result in a Game, kept separately for each way of playing it. It is the only score in the app: there are no points, levels or leaderboards.
_Avoid_: High score, record, points

**Picture**:
An image, or a numeral or sign, that stands for an Item in the Games of Practice. Only an Item whose meaning a picture shows to someone who has not learned the word has one. A Daily Set has no Pictures.
_Avoid_: Icon, illustration, emoji

**Streak**:
The number of consecutive calendar days on which the Learner completed a new or Review-only Daily Set. Until today's is completed it is the Streak earned up to yesterday; a day with none resets it to zero. A Replay does not count.

**Reminder**:
One notification a day, at a time the Learner chooses, sent only while no new or Review-only Daily Set has been completed today and something is left to do. It is off until the Learner turns it on. A Replay does not count as completing today's.
_Avoid_: Alert, alarm, push notification

**Exercise**:
A single task inside a Daily Set that the Learner answers and the app checks. A Daily Set is complete once every Exercise has been answered correctly.
The book's own kinds of Exercise (fill the gaps, match the pairs, true or false, put a dialogue in order) are answered in several parts, and are right only when every part is.
_Avoid_: Question, activity, task

**Accepted Answer**:
Any one of the responses an Exercise treats as correct; an Exercise may have several because Polish word order is flexible.
_Avoid_: Solution, correct answer

**Clip**:
One recording of spoken Polish, made ahead of time and packaged with the app. A listening Exercise plays one, and a speaker icon plays the Clip of the Polish beside it.
_Avoid_: Audio file, sound, recording

**Reading form**:
What a listening Exercise becomes when the Learner cannot listen, because they chose "can't listen now" for the Daily Set or turned sound off: the same Exercise, practising the same Items, asked from its meaning in the Base Language instead of from its Clip.
_Avoid_: Substitute, replacement Exercise

**Authored Content**:
Course material written and reviewed ahead of time and shipped with the app, as opposed to material generated while the Learner is using it.
_Avoid_: Static content, generated content
