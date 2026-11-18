# Every Game is open from the start, and any question can be skipped

Carlos learns the book at his own pace and found the app showed him questions and nothing else: the Games and Chats were all on the Practice tab, one long list, each locked until the sections that teach its words were completed (ADR-0011). He asked for everything to be available and skippable.

So at the Learner's own pace every Game and every Chat is open from the start. Practice reads the Learner as if every Daily Set on the Path were completed: a Game plays with the words of the whole book, and its decks still offer each chapter's words alone. A chapter's Lesson screen lists its own Games and its Chats under "Gry", so they are found where the chapter is. And every Exercise, in a Daily Set, a Replay or a Test, has a "Skip": the question leaves without an answer and is not asked again. Skipping the last one completes the Daily Set as an answer would.

## Consequences

- A Game can ask a word the Learner has not met. That is the price of playing a chapter's Games before its sections; the meaning is always on the card or shown with the answer.
- Only Practice reads the Path so. The Path, the Streak, Review and `progress.json` are what the Learner did. A skip reschedules no Item and is not a miss; it is not counted among the right answers in the Summary or the Test's percent.
- With One a day on (Settings), Practice keeps the rules of ADR-0011 and ADR-0012: a Game opens with the words of completed Daily Sets.
- A new Game names its chapter in `Game.chapter`, or no chapter's Gry lists it.

## Considered options

- *Keep the Games locked and only list them on the chapter.* Rejected: Carlos asked for everything to be open.
- *Open a chapter's Games with that chapter's words only.* Possible later; the decks already let him pick a chapter's words.
