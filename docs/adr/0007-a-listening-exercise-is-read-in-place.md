# A listening Exercise is read in place

When the Learner cannot listen, because they tapped "can't listen now" in a Daily Set or turned Sound off in Settings, a listening Exercise is shown in its reading form: the same Exercise, asked from its meaning in the Base Language instead of from its Clip. "Listen and choose" becomes "choose the Polish for this meaning" with the same options; "listen and type" becomes "type this in Polish".

The issue asked to swap listening Exercises for non-listening Exercises that practise the same Items. We looked for those in the shipped content first. Of the 88 listening Exercises, 35 have no reading Exercise anywhere up to their own Daily Set that covers the same Items (they are sentences of 2 to 6 Items), and for 58 the only candidates are Exercises already in the same Daily Set, so the Learner would answer the same Exercise twice. Authoring a second Exercise beside every listening one would double that part of the content for a case most Learners rarely meet.

## Consequences

- Every `listen_choose` and `listen_type` Exercise carries a `meaning`. It is required: a listening Exercise without one does not load, and the content checker requires its English text.
- Read from its meaning, a typed sentence can have more right answers than when it is copied from what was said (*jestem Ewa* for "I am Ewa"). A `listen_type` lists those as `readingAnswers`. They count only in the reading form, so dictation is not loosened, and they are never spoken, so they need no Clip.
- The Exercise keeps its id, its Items, its place in the queue, its Tip and its place in Review. "The same Items" is true by construction, and the progress bar and the Summary count do not move.
- Opting out is not an answer: it records no miss and changes no Item.
- The saved state is one flag on the Daily Set in progress. Sound off is a setting, passed to the engine when a Daily Set is started, continued or replayed, and never saved with the course.
- With sound off the engine hands the app no Clip at all, so speaker icons are hidden too. That is stricter than "nothing plays by itself"; relaxing it is a change to `view()` and to the Clip put on `Feedback` in `CourseEngine`.
- The Learner picks or types the same Polish they would have heard, so the reading form is practice of the same thing, not a lighter stand-in.
