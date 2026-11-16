# The look is playful, with no mascot

The v1 spec (issue #1) asked for "a clean, standard Android look, so that the app feels familiar" (story 54), with "a red-and-white accent". The screens built to it are tidy and correct, and they read as a default Material 3 template: text and pill buttons in the middle of an empty screen, Material's baseline lavender beside Polish red, red for both the brand and a wrong answer, feedback as one line of text, and a single animation in the whole app. After v1 Carlos asked for an app that is modern, beautiful, engaging, fun and professional-looking. A daily habit depends on wanting to open the app, and a plain look does nothing for that.

So story 54 is replaced. The app's look is **playful**: a full warm colour scheme with crimson kept for the main action, green for a right answer and amber for a wrong one, a friendly rounded typeface for the Polish and the headings, large tactile controls, motion and haptics that answer each tap, and confetti when a Daily Set is finished. `docs/design.md` is the brief, with every value. The story now reads: *as a Learner, I want the app to feel warm, lively and rewarding, so that I look forward to my Daily Set.*

There is **no mascot**, character or illustration. The spec's "no mascot or custom illustration" stands.

We rejected keeping the standard look and only finishing the theme. It would remove the lavender and the pink buttons and leave the app as forgettable as before.

We rejected a mascot or illustrated style, which is what the best-known language apps use. It needs artwork nobody on the project can draw, generated vector art would have to be kept consistent across every screen and state, and a character done badly looks worse than none. Everything in the chosen direction is built from colour, type, shape and motion in code. A mascot can still be added later on top of it.

We rejected adding points, levels or leaderboards to make the app engaging. They stay out of scope as in the spec. What the app celebrates is what it already counts: an Exercise answered, a Daily Set finished, the Streak kept.

## Consequences

- Nobody had seen a screen while v1 was built, which is how a template look went unnoticed. `ScreenshotsTest` now writes a picture of every screen in both themes on every build and CI keeps them, and a pull request that changes a screen shows them.
- The app bundles a typeface (Nunito, under the SIL Open Font Licence, with its licence text). It stays offline and grows by a few hundred kilobytes.
- Motion and haptics cannot be seen in a screenshot or checked by a test. They need a run-through on a phone, and every animation must also work as "no animation" for Learners who turn animations off.
- Material's dynamic colour stays off, so the app does not take on the phone's wallpaper colours.
- Red no longer means "wrong" anywhere. Material's `error` role is kept for real failures only.
- The spec's "Text and audio only; no images" for content is untouched: this decision is about the app's screens, not about Exercises.
- Sound effects for right and wrong answers are allowed by this direction but not decided here. If added they obey the Sound setting like a Clip.
