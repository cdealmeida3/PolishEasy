# The Learner chooses the pace

The v1 spec (issue #1) unlocked one new Daily Set per calendar day: the next one opened at midnight, a Review-only Daily Set was offered once a day, and ADR-0011 opened Practice only once today's Daily Set was done. These were product rules to build a daily habit. After v1 Carlos asked for the opposite: "let's not have a limitation for lessons or interactions in the app, I want to learn on my own path and timeframe".

So the habit rules become a setting, **One a day**, off by default.

**Off, the Learner's own pace.** Any Daily Set on the Path not yet completed can be started at any time, in any order, as many in a day as they like. Every Level is open. A Review-only Daily Set is offered again as soon as one is finished. Practice is always open; each Game still waits until enough has been learned for it. On the Path the suggested next Daily Set is "Up next", and every other one not completed can be tapped to start it. One Daily Set is kept in progress at a time, as before: starting another forgets the one in progress, and the app asks first.

**On, One a day.** Exactly the v1 rules: only the next Daily Set, at most one new or Review-only Daily Set per calendar day, Practice once it is done, the rest of the Path locked.

What does not change in either: the **Streak** is still the habit signal. A day counts when at least one new or Review-only Daily Set was completed on it, and several in one day count as one day. The **Reminder** is due while nothing has been completed today and something is left to do. A **Replay** still counts for nothing.

The setting reaches the engine as a function, `CourseEngine(oneADay = { … })`, read each time a rule needs it, so turning it on or off takes effect at once without rebuilding anything: turned on after a completion today, Home says done for today; turned off, the next Daily Set can be started straight away. `progress.json` does not change shape.

We rejected removing the daily rules. Some Learners want the habit enforced, and the code for it is small and fully tested; keeping it behind a switch costs little.

We rejected allowing several Daily Sets in progress at once. It would change `progress.json` and add a choice to every screen that resumes; one at a time, with a question before replacing it, covers the need.

## Consequences

- Daily Sets can be completed out of order. An Item belongs to the first Daily Set on the Path that has it (`Course.introducedIn`, `Learned.introduced`), so if the Learner skips ahead, a later Daily Set's Summary does not list an Item that a skipped, earlier one introduces, and a Game grouped by Unit leaves it out of that Unit until the earlier one is done. Words learned, Review and every Game's pool of Items still include it, since they use the Items of every completed Daily Set.
- A Unit is completed by whichever of its Daily Sets is completed last, not by its last one.
- The engine tests written before the setting construct their engine with `oneADay = { true }`: they are the proof that One a day is exactly the v1 rules. `OwnPaceTest` covers the Learner's own pace.
