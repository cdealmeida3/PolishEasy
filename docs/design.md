# How PolishEasy looks

The brief for every screen. The direction is **playful, no mascot**: the app is warm, confident and fun through colour, a friendly typeface, shape, motion and touch. It has no character and no illustration. `docs/adr/0010-the-look-is-playful-with-no-mascot.md` records why.

Read this before changing a screen or a component. If a value you need is missing, add it here in the same pull request; do not invent one in a screen file.

## See what you build

```
./gradlew :app:testDebugUnitTest --tests '*ScreenshotsTest'
```

writes one PNG per screen to `app/build/screenshots/`: the fixture course in the light theme (`light-…`) and the dark theme (`dark-…`), and the real course on Home, the Path and its first step (`real-…`). They are 411 × 891 dp. CI keeps them as the `screenshots` artifact of every run.

- Look at the pictures before and after any change to a screen. Open the PNGs themselves; they are the only way to see the app without a phone.
- A pull request that changes how a screen looks shows the changed pictures, light and dark, in its description or links the run's `screenshots` artifact.
- A new screen, or a new state of one, gets a `shot(…)` in `ScreenshotsTest`.
- The pictures show layout, colour and type. They do not show motion, haptics, the real keyboard or sound. Those stay with a run-through on a phone, so say in the pull request what could not be seen.

## Principles

1. **The Polish is the hero.** On every Exercise the Polish being learned is the biggest, boldest thing on the screen.
2. **One thing per screen, one main action.** The main action is a full-width button pinned to the bottom, at the same height on every screen. Home is the one exception: its main action sits inside the Today card, with what it starts.
3. **Crimson is for the main action and for highlights.** It never means "wrong".
4. **Never colour alone.** Right, wrong, chosen, locked and completed each have an icon or words as well as a colour.
5. **Things respond.** A tap gives way under the finger, an answer gets a reaction, a finished Daily Set gets a celebration. Every reaction is short and none blocks the next tap.
6. **Calm outside the moment.** Colour fields and motion mark what just happened. The resting screen is quiet: warm neutral surfaces, plenty of space.
7. **Words from `GLOSSARY.md`.** Short, friendly, no exclamation marks except "Correct!" and the Summary.

## Colour

Every value lives in `ui/theme` and nowhere else. A screen file names a role, never a hex value. All Material roles are set, so none falls back to Material's purple baseline. The build fails on a colour or a font value anywhere else (`./gradlew :app:checkDesignTokens`, see `CODING_STANDARDS.md`).

The one copy outside `ui/theme` is `window_background` in `res/values/colors.xml` and `res/values-night/colors.xml`: the `background` value, which Android shows for a moment before the first screen is drawn.

### Material roles

| Role | Light | Dark | Used for |
| --- | --- | --- | --- |
| `primary` | `#C8102E` | `#FFB3B5` | Text buttons, links, selected icons, progress |
| `onPrimary` | `#FFFFFF` | `#680012` | |
| `primaryContainer` | `#FFDAD9` | `#7A0A1C` | The chosen option, the segment of the Daily Set a card is about |
| `onPrimaryContainer` | `#410008` | `#FFDAD9` | |
| `secondary` | `#5A4740` | `#D9C3B9` | Quiet controls |
| `onSecondary` | `#FFFFFF` | `#2B1D17` | |
| `secondaryContainer` | `#F7E6DB` | `#2B211E` | Word tiles, the Polish key row, the conjugation table |
| `onSecondaryContainer` | `#2B1D17` | `#F2E0D8` | |
| `tertiary` | `#E2570B` | `#FF8A3D` | The Streak flame (see below) |
| `onTertiary` | `#FFFFFF` | `#3A1400` | |
| `background`, `surface` | `#FFF8F3` | `#1A1311` | Every screen |
| `onBackground`, `onSurface` | `#231A16` | `#F2E0D8` | Text |
| `surfaceVariant` | `#F7E6DB` | `#2B211E` | |
| `onSurfaceVariant` | `#5A4740` | `#D9C3B9` | Instruction labels, secondary text |
| `surfaceContainerLowest` | `#FFFFFF` | `#140E0C` | Cards |
| `surfaceContainerLow` | `#FFF1E8` | `#211917` | |
| `surfaceContainer` | `#FBEADF` | `#261D1A` | The navigation bar |
| `surfaceContainerHigh` | `#F7E6DB` | `#2B211E` | The progress bar's track |
| `surfaceContainerHighest` | `#EFDFD4` | `#362B27` | Disabled fills |
| `outline` | `#8C7369` | `#A18C82` | Text field borders |
| `outlineVariant` | `#DEC8BC` | `#4A3B35` | Card borders, dividers |
| `error`, `onError` | `#BA1A1A`, `#FFFFFF` | `#FFB4AB`, `#690005` | Something broke. Never a wrong answer |

The roles no screen names yet are set too, so that a Material component which reaches for one stays in the palette:

| Role | Light | Dark |
| --- | --- | --- |
| `tertiaryContainer`, `onTertiaryContainer` | `#FFDBCB`, `#351000` | `#7A2E00`, `#FFDBCB` |
| `errorContainer`, `onErrorContainer` | `#FFDAD6`, `#410002` | `#93000A`, `#FFDAD6` |
| `inverseSurface`, `inverseOnSurface` | `#382E2A`, `#FFEDE5` | `#F2E0D8`, `#382E2A` |
| `inversePrimary` | `#FFB3B5` | `#C8102E` |
| `surfaceBright`, `surfaceDim` | `#FFF8F3`, `#E8D7CC` | `#42362F`, `#1A1311` |
| `surfaceTint` | same as `primary` | same as `primary` |
| `scrim` | `#000000` | `#000000` |

### PolishEasy roles

Material has no role for these, so the theme adds them: a `PolishEasyColors` value read as `PolishEasyTheme.colors`, next to `MaterialTheme.colorScheme`.

| Role | Light | Dark | Used for |
| --- | --- | --- | --- |
| `brand` | `#C8102E` | `#D11F3B` | The fill of the main button and the play button |
| `onBrand` | `#FFFFFF` | `#FFFFFF` | |
| `brandEdge` | `#9A0A22` | `#8E0B22` | The main button's bottom edge |
| `success` | `#17733A` | `#6FDB8C` | "Correct": icon, heading, the right option's border |
| `onSuccess` | `#FFFFFF` | `#00390F` | |
| `successEdge` | `#0E5428` | `#3FA65C` | The bottom edge of Continue after a right answer |
| `successContainer` | `#D3F2DA` | `#0F3D1F` | The feedback panel and the right option's fill |
| `onSuccessContainer` | `#0A3A1B` | `#BFF5CB` | |
| `caution` | `#8A5200` | `#FFBA4A` | "Not quite" and the accent warning |
| `onCaution` | `#FFFFFF` | `#3A2200` | |
| `cautionEdge` | `#643B00` | `#C98A1E` | The bottom edge of Continue after a wrong answer |
| `cautionContainer` | `#FFE3B3` | `#4A2C00` | The feedback panel and the wrong option's fill |
| `onCautionContainer` | `#3A2200` | `#FFE3B3` | |
| `streak` | `#E2570B` | `#FF8A3D` | The flame and the completed-day dots. Same value as `tertiary` |

Rules:

- **The main button is `brand` with `onBrand` in both themes.** In the dark theme `primary` is a light pink so that crimson text stays readable on a dark surface; it is for text and icons, never for a button's fill.
- **Inside the feedback panel the main button takes the panel's colour**: `success` with `onSuccess` after a right answer, `caution` with `onCaution` after a wrong one or an accent warning. A crimson button there would be red on "wrong", and in the dark theme `brand` is under 3 to 1 against either container.
- **A wrong answer is `caution` (amber), not red and not `error`.** A right answer is `success` (green).
- **An accent warning is `caution` with the tick icon**: accepted, with something to notice.
- Text and icons that carry meaning have a contrast of at least 4.5 to 1 against what they sit on, and shapes that carry meaning at least 3 to 1. The pairs above were checked against that. In the dark theme the `brand` fill is 3.5 to 1 against the background, so a main button there never sits on a container colour.
- `streak` is for graphics and large numbers only, not for small text in the light theme (3.6 to 1).
- No gradients, no shadows deeper than the bottom edge described under Shape, no transparency used as a colour.
- Dynamic colour (Material You) stays off: the app looks the same on every phone.

## Type

Two families. **Nunito** (SIL Open Font Licence, covers every Polish letter) is bundled in `res/font` in the three weights the table uses, with its licence text in `res/raw/nunito_ofl.txt`, so the app stays offline. It is used for headings, Polish prompts, buttons and numbers. Body text uses the phone's own sans-serif.

| Style | Family | Weight | Size / line (sp) | Used for |
| --- | --- | --- | --- | --- |
| `displayLarge` | Nunito | ExtraBold 800 | 57 / 64 | Not used yet |
| `displayMedium` | Nunito | ExtraBold 800 | 45 / 52 | The Streak number, the score on the Summary |
| `displaySmall` | Nunito | ExtraBold 800 | 36 / 44 | Not used yet |
| `headlineLarge` | Nunito | ExtraBold 800 | 32 / 40 | A Polish prompt of up to about 20 letters |
| `headlineMedium` | Nunito | Bold 700 | 26 / 34 | A longer Polish prompt, the Summary's title |
| `headlineSmall` | Nunito | Bold 700 | 22 / 28 | Screen titles, a Tip's title, a prompt in the Base Language |
| `titleLarge` | Nunito | Bold 700 | 20 / 26 | Card titles, the feedback heading |
| `titleMedium` | Nunito | SemiBold 600 | 18 / 24 | Option cards, word tiles, Polish key row, text a Learner types |
| `titleSmall` | Nunito | Bold 700 | 14 / 20 | Unit headers, small headings |
| `bodyLarge` | System | Regular 400 | 16 / 24 | A Tip's body, the correct answer in feedback |
| `bodyMedium` | System | Regular 400 | 14 / 20 | Secondary text, meanings under Polish words |
| `bodySmall` | System | Regular 400 | 12 / 16 | Not used yet |
| `labelLarge` | Nunito | Bold 700 | 16 / 24 | Buttons |
| `labelMedium` | System | Medium 500 | 14 / 20 | The instruction above a prompt, in `onSurfaceVariant` |
| `labelSmall` | System | Medium 500 | 12 / 16 | Navigation labels, "Locked", the version |

Rules:

- Sizes are in sp and nothing sets a fixed height around text, so the phone's font size setting works.
- No size, weight or family is set in a screen file. Name a style.
- No all-capitals text, and no italics except a Polish word quoted inside a sentence in the Base Language.
- Polish is never shrunk to fit. A long prompt wraps, and uses `headlineMedium`.

## Space

A 4 dp grid. Use only these steps: 4, 8, 12, 16, 24, 32, 48.

- Screen edge to content: 24 at the sides, 16 at the top below the bar, 24 above the bottom edge or the navigation bar.
- Between an instruction and its prompt: 8. Between a prompt and its answer area: 32. Between option cards: 12. Between word tiles: 8. Inside a card: 16.
- Anything a finger taps is at least 48 × 48, including icons.
- Content is at most 480 wide and centred beyond that.
- Content starts from the top, on the Summary too. Nothing is centred vertically.

## Shape

| Token | Corner | Used for |
| --- | --- | --- |
| `extraSmall` | 8 | The Polish key row keys |
| `small` | 12 | Word tiles, text fields |
| `medium` | 16 | Buttons, option cards |
| `large` | 20 | Cards, the conjugation table |
| `extraLarge` | 28 | The feedback panel's top corners, dialogs |
| full | half the height | The progress bar, day dots, Path nodes |

**The bottom edge.** Buttons, option cards and word tiles are flat with a darker bottom edge 4 dp deep, and no shadow. Pressing one moves it down 4 dp so the edge disappears, which is what makes a tap feel physical. The main button's edge is `brandEdge`; an outlined card or tile has a 2 dp `outlineVariant` border whose bottom side is 4 dp. A disabled control has no edge.

## Components

Shared components live in `ui/components`, one screen per file in `ui/screens`. A screen builds from these and adds no styling of its own. The space steps are `Space`, the shared sizes `Size`, the corners `Corner` and the bottom edge `Edge`, all in `ui/theme`.

- **`PrimaryButton`**: the one main action. Full width, 56 high, `brand` fill, `labelLarge` in `onBrand`, the bottom edge. Disabled: `surfaceContainerHighest` with `onSurfaceVariant` text. Its `tone` is set only by the feedback panel (see Colour).
- **`SecondaryButton`**: 56 high, `surfaceContainerLowest` fill, outlined, `primary` text. Used rarely; most screens have no second button.
- **`QuietButton`** (a text button): `primary` text, 48 high. "Can't listen now", "Slow".
- **`OptionCard`**: an answer to choose. Full width, at least 56 high, text at the start in `titleMedium`, an icon slot at the end. States:
  - idle: `surfaceContainerLowest`, `outlineVariant` border
  - chosen (before Check, where there is one): `primaryContainer`, `primary` border
  - right: `successContainer`, `success` border, tick icon
  - wrong (the one tapped): `cautionContainer`, `caution` border, cross icon
  - others after an answer: idle at 60% opacity, not tappable
  
  After an answer the right option is always marked, whichever was tapped.
- **`WordTile`**: `secondaryContainer`, `titleMedium`, 48 high, the bottom edge. A tile that has been used leaves an empty slot in the bank, so the bank never shifts.
- **`AnswerField`** and **`PolishKeyRow`**: outlined, `titleMedium` for what is typed, 2 dp `primary` border when focused. The Polish key row sits directly above the keyboard, with keys at least 48 high.
- **`FeedbackPanel`**: rises from the bottom over the main button's place and holds the next one. It has a heading in `titleLarge` in the panel's accent colour and, under it, a line in `bodyLarge`:
  - right: `successContainer`, a tick, "Correct!", no second line
  - accent warning: `cautionContainer`, a tick, "Correct", then "Mind the accents:" and the proper spelling
  - wrong: `cautionContainer`, a cross, "Not quite", then "Correct answer:" and the answer

  It holds the speaker for the Polish, Continue as a `PrimaryButton` in the panel's tone, and "Report a mistake" as a small flag icon button with that label for screen readers.
- **`DailySetFrame`** and **`DailySetStep`**: the layout of every Tip and Exercise. The frame is the top bar, a close button and the progress bar, which stays put for the whole Daily Set while the steps slide under it. A step is content from the top, scrolling if it must, and the main action pinned to the bottom, with the Polish key row below it. Nothing moves when feedback appears. An Exercise draws only what is above the main action: "Check" belongs to the step's bottom, not to the Exercise.
- **The close button** leaves the Daily Set at once, with no question: every answer is already saved, and Home offers to continue. It is the close icon in `onSurfaceVariant`, lined up with the content's edge.
- **`WordTileBoard`**: the answer being built, a divider, and the bank of `WordTile`s. It owns the tiles' movement between the two.
- **Listening controls**: the `PlayButton` centred, and under it "Slow" and "Can't listen now" side by side as `QuietButton`s.
- **`ScreenFrame`**: the layout of a screen outside a Daily Set, with the edge spacing under Space.
- **`Instruction`**, **`PolishPrompt`**, **`BasePrompt`**: the label above a prompt and the prompt itself, so the choice of style between a short and a long Polish prompt is made in one place.
- **`ProgressBar`**: 12 high, fully rounded, `primary` on `surfaceContainerHigh`.
- **`PlayButton`**: a 72 circle in `brand` with the speaker icon in `onBrand`, for listening Exercises. **`SpeakerButton`**, the small speaker beside Polish text, is a 48 icon button in `primary`.
- **`TopBar`**: Path and Settings have a `headlineSmall` title, and the Path a `bodyLarge` line under it in `onSurfaceVariant`. Home has a greeting in its place (see Home). Inside a Daily Set it is the close button and the progress bar only.
- **`NavigationFrame`**, the navigation bar: Home, Path, Settings, with icons and `labelSmall` labels, on `surfaceContainer`. The selected one has a `primaryContainer` pill behind a `primary` icon and its label in `onSurface`; the others are `onSurfaceVariant`. Hidden inside a Daily Set and on the Summary. Back from the Path or Settings goes to Home.
- **`StreakWeekCard`**: the Streak on Home. A `ContentCard` with the Streak flame at 48 and the number in `displayMedium`, "day Streak" in `titleLarge` and a line in `bodyMedium` saying what today's Daily Set would do for it. Under them the last seven days, oldest first and ending today, as 32 dots with the weekday's letter below each: a completed day is `streak` with a tick, a day without is `surfaceContainerHigh`, and today has a 2 dp `streak` ring until it is completed and its letter in `titleSmall`. A screen reader hears the card as one sentence.
- **`TodayCard`**: what today holds, on Home. A `ContentCard` with a small label in `titleSmall` ("Today" in `primary` on the card with the main action, "Up next" in `onSurfaceVariant`), a `headlineSmall` title, and below it what the state needs: `UnitProgress`, `Teaches`, a `CardNote`, the `PrimaryButton`. Done states have no label and put the `success` tick beside the title.
- **`UnitProgress`**: the Unit's name in `bodyMedium`, and under it `ProgressSegments`: one 12 high segment for each of the Unit's Daily Sets, `primary` when completed, `primaryContainer` with a 2 dp `primary` border for the one the card is about, `surfaceContainerHigh` for the rest.
- **`Teaches`**: "You'll learn" as an `Instruction`, then one `bodyLarge` line for each of the Daily Set's Tip titles, with an 8 dot in `primary`.
- **`ContentCard`**: `surfaceContainerLowest`, `large` corners, 1 dp `outlineVariant` border, 16 padding, no shadow.
- **`ScoreRing`**: the Summary's score. A 184 ring, 16 thick with round ends, `success` on `surfaceContainerHigh`, with the share of Exercises answered correctly on the first try inside it in `displayMedium`. The same score is written in words under it.
- **`StreakCard`**: the Streak going up, on the Summary of the Daily Set that raised it and nowhere else. A `ContentCard` with the flame at 48 and the number in `displayMedium`, both in `streak`, then "day Streak" in `titleLarge` and a line in `bodyMedium`. A Replay never shows it.
- **`ItemCard`**: one Item under "You learned": a `ContentCard` with the Polish in `titleMedium` and its meaning under it in `bodyMedium`.
- **`Confetti`**: see Motion and touch. It is laid over the Summary's content, above the main button. A Replay gets none.
- **Streak flame**: the flame icon in `streak` beside the number. With a Streak of zero the flame and the number are `outline`, not hidden.
- **`UnitCard`**: a Unit on the Path. A `ContentCard` with "Unit N" in `titleSmall` in `primary`, the Unit's title in `titleLarge`, "4 of 7 completed" in `bodyMedium`, `ProgressSegments` with today's Daily Set outlined, and under them the Unit's stops. A Unit that is all completed has the `success` tick beside its title.
- **`TrailStop`**: one Daily Set on a Unit's trail. Its node, then its title in `titleMedium` and under it a `bodyMedium` line with its number and its state in words. The stop the Learner is at has a `titleSmall` label above its title: "Today" in `primary`, or "Up next" in `onSurfaceVariant` when it unlocks tomorrow. A completed stop ends with the replay icon in `primary`. The whole stop is tapped as one, with no ripple: its node goes down. A locked stop's title is `onSurfaceVariant` and it is not tappable.
- **Path node**: a 56 circle. Completed: `success` fill with a tick in `onSuccess`, on the bottom edge in `successEdge`. Today: `brand` fill with a play icon in `onBrand`, on the bottom edge in `brandEdge`, inside a 2 dp `brand` ring 72 across. Locked: `surfaceContainerHighest` with a lock icon in `onSurfaceVariant`, flat. Today's node sits on the card, never on a container colour.
- **The trail**: a 4 dp line joining a Unit's nodes. It is `success` as far as the Learner has come, up to today's node, and `outlineVariant` beyond.
- **`Journey`**: the Path's scrolling column. It opens already in place, with no visible jump (see The Path).

Icons are Material Symbols, rounded style, filled, 24 dp, kept as vector drawables in `res/drawable`. No icon library is added to the build.

## Home

Home answers three things from the top down: hello, how the Streak stands, and what to do today.

- **The greeting** is Polish, in `headlineLarge`: "Cześć", and "Do jutra" once today's Daily Set is done. Under it one `bodyLarge` line in `onSurfaceVariant` says the state in the Base Language.
- **The `StreakWeekCard`**, 24 below the greeting.
- **The `TodayCard`**, 16 below that. Each state has its own:

| State | Card |
| --- | --- |
| A Daily Set is ready | "Today", "Daily Set N of M", `UnitProgress`, `Teaches`, Start |
| In progress | The same, with "In progress: N of M Exercises done." and Continue |
| Done for today | The tick, "Done for today" and the unlock time. Then an "Up next" card for the next Daily Set, with no button |
| Review-only, ready or in progress | "Today", "Review-only Daily Set", a note, Start or Continue |
| Review-only, done | The tick, "Done for today" and when the next Review-only Daily Set unlocks |
| Nothing left and nothing to review | The tick and "Every Daily Set completed" |

Home scrolls if the font size makes it too tall. Nothing on it moves at rest.

## The Path

The Path answers where the Learner is and how far they have come.

- **The title** is the Level's name and "Path" ("A1 Path"), with "10 of 23 Daily Sets completed" under it.
- **A `UnitCard` for each Unit**, 16 apart, each holding its Daily Sets as `TrailStop`s.
- **A Daily Set's title is the title of its first Tip** (`docs/authoring.md`). A Daily Set with no Tip shows "Daily Set N" in its place.
- **Three states, each with its own icon**: a tick (completed), a play arrow in a ring (today), a lock (locked). The words under the title say the same.
- **It opens at the stop the Learner is at**: the first Daily Set not yet completed. If that stop is within the top 60% of the screen when its Unit's card is at the top, the Path opens at the top of the card; otherwise the stop sits a third of the way down. With every Daily Set completed it opens at the top.
- **Tapping** a completed stop starts a Replay, today's opens today's Daily Set, a locked one does nothing.
- With every Daily Set completed, a line after the last Unit says more Units are on the way.

## Motion and touch

Motion says what just happened. It never makes the Learner wait: every tap is accepted while something is still moving.

| What | How |
| --- | --- |
| Pressing a button, card or tile | Moves down 4 dp, 80 ms |
| From one Tip or Exercise to the next | The old one slides out to the left and fades, the new one slides in from the right, 250 ms, ease out |
| A word tile going to its slot or back | Travels there, 200 ms, a slight overshoot |
| Feedback panel | Slides up from the bottom, 250 ms, ease out. Leaves in 150 ms |
| Progress bar | A spring to the new value, with a little bounce |
| The right option, a tick appearing | Scales from 0.8 to 1 with a little bounce, 200 ms |
| The wrong option | One short side-to-side shake of 8 dp, 250 ms |
| The play button while a Clip plays | A slow pulse, 1 to 1.06, 700 ms each way |
| Summary | The score ring fills in 800 ms, numbers count up, then confetti falls once for about 2 seconds |
| The Streak going up | When the score ring has filled: the old number rolls up and out, the new one rolls in from below, 350 ms, and the flame scales once from 1.35 back to 1 |

Rules:

- **Reduced motion.** When Android's "remove animations" is on (the animator duration scale is 0), every change is instant, there is no confetti and no shake, and the final state shows at once. One helper in `ui/theme` answers this; components ask it and screens do not.
- Nothing loops except the play button's pulse while a Clip is playing.
- Confetti is drawn in Compose in `brand`, `success`, `caution` and `streak`, with no library and no image. It does not cover the main button and cannot be tapped. It is for a Daily Set that counts, new or Review-only, not for a Replay.
- Durations and springs are named values in `ui/theme`, like colours.

**Haptics.** Through the view's haptic feedback, so the phone's own touch setting is respected and no permission is needed.

- Felt once, as the feedback panel appears. Right answer, with or without an accent warning: `CONFIRM`. Wrong answer: `REJECT`. On Android versions without those (below 11): `KEYBOARD_TAP` for right and `LONG_PRESS` for wrong.
- Placing a word tile: `CLOCK_TICK`.
- Finishing a Daily Set: `CONFIRM` once as the Summary appears (`KEYBOARD_TAP` below Android 11).
- Nothing else vibrates.

**Sound.** Only Clips play today; the Daily Set player was built without sound effects. Any sound effect added later obeys the Sound setting and "can't listen now", exactly as Clips do.

## Accessibility

- Every state has words or an icon as well as colour, and a state change is announced to a screen reader (the feedback panel is a live region).
- Every icon button has a content description from `strings.xml`. Decorative icons have none.
- Touch targets are at least 48 dp. Contrast as under Colour.
- The app works at the largest font size: content scrolls, the main button stays pinned and nothing is cut off.
- Both themes are designed, not derived: check both sets of screenshots.

## Not part of the look

No mascot, character or illustration. No photographs or images in Exercises. No points, levels, leaderboards or Streak freezes: the celebration is for finishing a Daily Set and keeping the Streak, which are the rules the app already has.
