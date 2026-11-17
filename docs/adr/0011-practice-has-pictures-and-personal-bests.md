# Practice has pictures and personal Bests

The v1 spec (issue #1) kept two things out of the app: images ("Text and audio only; no images") and scores ("no points, levels or leaderboards"). ADR-0010 kept both when the look became playful. After v1 Carlos asked for games to play once the Daily Set is done: picture cards to flip, chains of phrases, verb challenges, and "a lot of them". A Daily Set is ten to fifteen minutes, and a Learner who wants more that day had only Replay.

So the app gains **Practice** (spec #77): Games outside the Path, open each day once today's Daily Set is completed. Two of the earlier rules change for it, and only for it.

**Pictures.** Words that can be pictured get a picture, used by the picture Games. The pictures are Microsoft's Fluent Emoji in the 3D style, under the MIT licence, packaged with the app. They are named in the course files beside the Item they picture, and the content check holds them to the same standard as a Clip: a picture that is named must exist, and one that is packaged must be used.

**Bests.** Each Game keeps the Learner's best result for each way of playing it, and a count of the Rounds played today. That is all. The Best is a number to beat, shown on the Game's card and at the end of a Round.

What does not change:

- A Daily Set has no pictures and no score beyond its Summary.
- Practice counts for nothing else. A Round cannot advance the Path, raise the Streak, satisfy the Reminder or reschedule an Item for Review. The engine's `Practice` only reads the Learner's progress, and its own state is a second file, `practice.json`, so a fault in it cannot damage `progress.json`.
- There are still no points, levels, currencies, leaderboards or streaks of Rounds. Nothing is bought, unlocked by playing, or lost by not playing.
- There is still no mascot or illustration on the app's own screens. A Game's icon is a picture from the same set.

We rejected letting Practice feed Review. A right answer in a Game is easier than in an Exercise: the choices are fewer and the same Item comes round again within a minute. Counting it would push Items out of Review that the Learner does not yet know.

We rejected opening Practice before the Daily Set. The Daily Set is the habit the app exists for; Games that are open all day would be played instead of it. The gate also gives finishing the Daily Set a reward that costs nothing.

We rejected drawing pictures in code or generating them. One consistent, licensed set that already covers most everyday nouns is better than anything the project can draw.

## Consequences

- The app grows by the pictures: about 10 KB each at 256 pixels as WebP, under half a megabyte for the first sixty.
- A Game can only use what a completed Daily Set taught, so a new Learner sees most Games locked, each saying what opens it. The engine tests walk the real course and check this at every point on the Path.
- Each Game's rules live in the engine as an immutable Round with pure moves, tested without Android. The screens only show a Round and hand moves back.
- `practice.json` is backed up and moved to a new phone with `progress.json`.
- Motion, haptics and sound in a Game need a run-through on a phone, as in ADR-0010.
