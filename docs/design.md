# How PolishEasy looks

The brief for every screen. The direction is **playful, no mascot**: the app is warm, confident and fun through colour, a friendly typeface, shape, motion and touch. It has no character and no illustration. `docs/adr/0010-the-look-is-playful-with-no-mascot.md` records why.

Read this before changing a screen or a component. If a value you need is missing, add it here in the same pull request; do not invent one in a screen file.

## See what you build

```
./gradlew :app:testDebugUnitTest --tests '*ScreenshotsTest'
```

writes one PNG per screen to `app/build/screenshots/`: the fixture course in the light theme (`light-…`) and the dark theme (`dark-…`), the light theme again at the largest font size a phone offers (`large-…`), and the real course on Home, the Path and its first step (`real-…`). They are 411 × 891 dp. CI keeps them as the `screenshots` artifact of every run.

- Look at the pictures before and after any change to a screen. Open the PNGs themselves; they are the only way to see the app without a phone.
- A pull request that changes how a screen looks shows the changed pictures, light and dark, in its description or links the run's `screenshots` artifact.
- A new screen, or a new state of one, gets a `shot(…)` in `ScreenshotsTest`.
- Every `shot(…)` also checks what a picture cannot show: that each thing a finger can tap has a name for a screen reader and is at least 48 dp each way.
- The pictures show layout, colour and type. They do not show motion, haptics, the real keyboard or sound. Those stay with a run-through on a phone, so say in the pull request what could not be seen.

## Principles

1. **The Polish is the hero.** On every Exercise the Polish being learned is the biggest, boldest thing on the screen, and it is heard as well as seen.
2. **One thing per screen, one main action.** The main action is a full-width button pinned to the bottom, at the same height on every screen. Home is the one exception: its main action sits inside the Today card, with what it starts.
3. **Crimson is for the main action and for highlights.** It never means "wrong".
4. **Never colour alone.** Right, wrong, chosen, locked and completed each have an icon or words as well as a colour.
5. **Things respond.** A tap gives way under the finger, an answer gets a reaction, a finished Daily Set gets a celebration. Every reaction is short and none blocks the next tap.
6. **Calm outside the moment.** Colour fields and motion mark what just happened. The resting screen is quiet: warm neutral surfaces, plenty of space.
7. **Words from `GLOSSARY.md`, and praise in Polish.** Short, friendly, no label-and-colon. A right answer and a finished Daily Set are praised in Polish ("Dobrze!", "Świetnie!"), which is one more word learned; a screen reader is told the meaning. No exclamation marks anywhere else.

## Colour

Every value lives in `ui/theme` and nowhere else. A screen file names a role, never a hex value. All Material roles are set, so none falls back to Material's purple baseline. The build fails on a colour or a font value anywhere else (`./gradlew :app:checkDesignTokens`, see `CODING_STANDARDS.md`).

Two values are copied outside `ui/theme`, in `res/values/colors.xml`, for what Android draws before the app can: `window_background` (and its dark value in `res/values-night`), the `background` value shown for a moment before the first screen is drawn, and `brand`, the light `brand` value behind the app icon, on the splash screen and on the Reminder's notification.

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
| `completed` | `#17733A` | `#3FA65C` | A completed stop on the Path and the trail walked so far. `success` in the light theme, one step quieter in the dark one |
| `completedEdge` | `#0E5428` | `#2B7A42` | The bottom edge of a completed stop |

Rules:

- **The main button is `brand` with `onBrand` in both themes.** In the dark theme `primary` is a light pink so that crimson text stays readable on a dark surface; it is for text and icons, never for a button's fill.
- **One accent in the dark theme.** Everything that is filled with the accent is `brand` in both themes: buttons, the progress bar, progress segments. The dark theme's pink is for text and icons only. A completed stop is `completed`, quieter than `success` in the dark theme, so that today's `brand` stop is the loudest thing on the Path.
- **Inside the feedback panel the main button takes the panel's colour**: `success` with `onSuccess` after a right answer, `caution` with `onCaution` after a wrong one or an accent warning. A crimson button there would be red on "wrong", and in the dark theme `brand` is under 3 to 1 against either container.
- **A wrong answer is `caution` (amber), not red and not `error`.** A right answer is `success` (green).
- **An accent warning is `caution` with the tick icon**: accepted, with something to notice.
- Text and icons that carry meaning have a contrast of at least 4.5 to 1 against what they sit on, and shapes that carry meaning at least 3 to 1. `ContrastTest` holds every pair a screen puts together to that, in both themes; a new pair is added there.
- `outlineVariant` and the track colours are under 3 to 1 on purpose: a card's border, an idle option's edge and an empty progress segment are quiet. So none of them carries meaning alone. A control is known by its words, and what a track shows is also said in words ("4 of 7 completed"). In the dark theme the `brand` fill is 3.5 to 1 against the background, so a main button there never sits on a container colour.
- `streak` is for graphics and large numbers only, not for small text in the light theme (3.6 to 1).
- No gradients, no shadows deeper than the bottom edge described under Shape, no transparency used as a colour. The one exception is `fadingBottom`: a scrolling list that runs under a pinned button fades into `background` over its last 48 while there is more below, so it is not cut off hard.
- Dynamic colour (Material You) stays off: the app looks the same on every phone.

## Type

Two families. **Nunito** (SIL Open Font Licence, covers every Polish letter) is bundled in `res/font` in the three weights the table uses, with its licence text in `res/raw/nunito_ofl.txt`, so the app stays offline. It is used for headings, Polish prompts, buttons and numbers. Body text uses the phone's own sans-serif.

| Style | Family | Weight | Size / line (sp) | Used for |
| --- | --- | --- | --- | --- |
| `displayLarge` | Nunito | ExtraBold 800 | 57 / 64 | The Streak number on the Summary, "Cześć!" on the welcome |
| `displayMedium` | Nunito | ExtraBold 800 | 45 / 52 | The Streak number on Home |
| `displaySmall` | Nunito | ExtraBold 800 | 36 / 44 | A Polish prompt of a word or two, up to 12 letters, the Summary's cheer |
| `headlineLarge` | Nunito | ExtraBold 800 | 32 / 40 | A Polish prompt of 13 to 20 letters, Home's greeting |
| `headlineMedium` | Nunito | Bold 700 | 26 / 34 | A longer Polish prompt, the sentence of a conjugation Exercise, the Summary's title |
| `headlineSmall` | Nunito | Bold 700 | 22 / 28 | Screen titles, a Tip's title, a prompt in the Base Language, the number on a `StatTile` |
| `titleLarge` | Nunito | Bold 700 | 20 / 26 | Card titles, the feedback heading |
| `titleMedium` | Nunito | SemiBold 600 | 18 / 24 | Option cards, word tiles, Polish key row, text a Learner types |
| `titleSmall` | Nunito | Bold 700 | 14 / 20 | Unit headers, small headings |
| `bodyLarge` | System | Regular 400 | 16 / 24 | A Tip's sentences after its lead, the correct answer in feedback |
| `bodyMedium` | System | Regular 400 | 14 / 20 | Secondary text, meanings under Polish words |
| `bodySmall` | System | Regular 400 | 12 / 16 | Not used yet |
| `labelLarge` | Nunito | Bold 700 | 16 / 24 | Buttons |
| `labelMedium` | System | Medium 500 | 14 / 20 | The instruction above a prompt, in `onSurfaceVariant` |
| `labelSmall` | System | Medium 500 | 12 / 16 | Navigation labels, "Locked", the version |

Rules:

- Sizes are in sp and nothing sets a fixed height around text, so the phone's font size setting works.
- No size, weight or family is set in a screen file. Name a style.
- No all-capitals text, and no italics except a Polish word quoted inside a sentence in the Base Language.
- Polish is never shrunk to fit. A short prompt is set very large; a long one wraps, and uses `headlineMedium`.

## Space

A 4 dp grid. Use only these steps: 4, 8, 12, 16, 24, 32, 48.

- Screen edge to content: 24 at the sides, 16 at the top below the bar, 24 above the bottom edge or the navigation bar.
- Between an instruction and its prompt card: 12. Between a prompt and its answers: at least 24. Between option cards: 12. Between word tiles: 8. Inside a card: 16.
- Anything a finger taps is at least 48 × 48, including icons.
- Content is at most 480 wide and centred beyond that.
- **In an Exercise the prompt starts from the top and the answers from the bottom**, just above the main action, where a thumb reaches them. The space between the two is the screen's breathing room. Everywhere else content starts from the top, on the Summary too. Nothing is centred vertically.

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
- **`QuietButton`** (a text button): `primary` text, 48 high. Subdued, it is `onSurfaceVariant`: a way out that is rarely taken, such as "Can't listen now".
- **`TonalButton`**: a small helper beside a bigger control. A pill at least 48 high in `primaryContainer` with an icon and a word in `onPrimaryContainer`. "Slow".
- **`OptionCard`**: an answer to choose. Full width, at least 56 high, text at the start in `titleMedium`, an icon slot at the end. States:
  - idle: `surfaceContainerLowest`, `outlineVariant` border
  - chosen (before Check, where there is one): `primaryContainer`, `primary` border
  - right: `successContainer`, `success` border, tick icon
  - wrong (the one tapped): `cautionContainer`, `caution` border, cross icon
  - others after an answer: idle at 70% opacity, not tappable
  
  After an answer the right option is always marked, whichever was tapped.
- **`WordTile`**: `secondaryContainer`, `titleMedium`, 48 high, the bottom edge. A tile that has been used leaves an empty slot in the bank, so the bank never shifts. The slot is `surfaceContainerHigh` with a dashed `outline` line around it, so it shows in bright light.
- **`AnswerField`** and **`PolishKeyRow`**: outlined, `titleMedium` for what is typed, 2 dp `primary` border when focused. The Polish key row sits directly above the keyboard, with keys at least 48 high.
- **`FeedbackPanel`**: rises from the bottom over the main button's place and holds the next one. It has a heading in `titleLarge` in the panel's accent colour and, under it, a line in `bodyLarge`:
  - right: `successContainer`, a tick, praise in Polish taken in turn ("Dobrze!", "Świetnie!", "Tak!", said as "Correct" by a screen reader), and under it the other side of the answer: what it means, or for a meaning the Polish
  - accent warning: `cautionContainer`, a tick, "Correct", then "Mind the accents:" and the proper spelling
  - wrong: `cautionContainer`, a cross, "Not quite", then "Correct answer:" and the answer

  It holds the speaker for the Polish, Continue as a `PrimaryButton` in the panel's tone, and "Report a mistake" as a small flag icon button with that label for screen readers.
- **`DailySetFrame`** and **`DailySetStep`**: the layout of every Tip and Exercise. The frame is the top bar, a close button and the progress bar, which stays put for the whole Daily Set while the steps slide under it. A step has two parts: the prompt from the top and the answers from the bottom (options, the word tile board, the text field), sitting just above the main action, which is pinned to the bottom with the Polish key row below it. If the two parts do not fit they scroll as one. The feedback panel rises over the main action's place, and 176 of room is kept there from the start, so nothing moves when it appears; only a panel taller than that, at a large font size, pushes the answers up. An Exercise draws only what is above the main action: "Check" belongs to the step's bottom, not to the Exercise.
- **The close button** leaves the Daily Set at once, with no question: every answer is already saved, and Home offers to continue. It is the close icon in `onSurfaceVariant`, lined up with the content's edge.
- **`WordTileBoard`**: the answer being built, on two ruled lines in `outlineVariant` that are there before the first tile is, and under it the bank of `WordTile`s. It owns the tiles' movement between the two.
- **Listening controls** (`PlayControls`): the `PlayButton` centred with "Slow" beside it as a `TonalButton`, which goes under it at a large font size. "Can't listen now" is a subdued `QuietButton` under the answers.
- **`ScreenFrame`**: the layout of a screen outside a Daily Set, with the edge spacing under Space.
- **`Instruction`**, **`PolishPrompt`**, **`BasePrompt`**: the label above a prompt and the prompt itself, so the choice of style between a short and a long Polish prompt is made in one place.
- **`PromptCard`**: what an Exercise asks about, set apart at the top: `surfaceContainerHigh`, `large` corners, at least 96 high, 24 at the sides, the prompt in `onSurface` and its `SpeakerButton` at the end when it can be heard. No border and no edge: it is not tapped.
- **`PromptChip`**: the verb and the person of a conjugation Exercise in one small label ("być · I (ja)"): `titleSmall` on a `secondaryContainer` pill with a 1 dp `outlineVariant` border.
- **`SentenceField`**: the sentence of a conjugation Exercise in `headlineMedium`, with the gap drawn as a slot the Learner types into: `secondaryContainer`, `small` corners, at least 96 wide, growing with what is typed, with a 2 dp border in `outline`, `primary` while it has focus. The meaning of the sentence is under it in `bodyLarge` in `onSurfaceVariant`. A conjugation Exercise with no sentence has the chip and an `AnswerField` at the bottom.
- **`ProgressBar`**: 12 high, fully rounded, `brand` on `surfaceContainer`. In a Daily Set it counts steps, the Tips read and the Exercises answered correctly, so it moves the first time Continue is tapped on a Tip; a screen reader hears "3 of 9 steps done".
- **`TipPoints`**: a Tip's prose in bites. Its first sentence is the lead, in `titleMedium`; each sentence after it is on a line of its own in `bodyLarge` behind an 8 `primary` dot, 12 apart. The content is not changed: the screen splits the body at its full stops. The Polish a Tip marks (`docs/authoring.md`) is set in Nunito ExtraBold in the text's own colour (`withPolish`), and a screen reader says it as Polish.
- **`ConjugationTable`** rows end in a `SpeakerButton` when the form has a Clip.
- **`PlayButton`**: 72 across in `brand` with the speaker icon in `onBrand`, for listening Exercises. A circle at rest and a rounded square with `large` corners while a Clip plays. **`SpeakerButton`**, the small speaker beside Polish text, is a 40 circle in `primaryContainer` with the icon in `onPrimaryContainer`, inside a 48 touch area, so it reads as a button; it grows with the font size. On the feedback panel it has no field and takes the panel's accent.
- **`TopBar`**: Path and Settings have a `headlineSmall` title, and the Path a `bodyLarge` line under it in `onSurfaceVariant`. Home has a greeting in its place (see Home). Inside a Daily Set it is the close button and the progress bar only.
- **`NavigationFrame`**, the navigation bar: Home, Path, Settings, with icons and `labelSmall` labels, on `surfaceContainer`. The selected one has a `primaryContainer` pill behind a `primary` icon and its label in `onSurface`; the others are `onSurfaceVariant`. Hidden inside a Daily Set and on the Summary. Back from the Path or Settings goes to Home. Moving between tabs is a fade-through; the pill moves as Material's own navigation bar moves it.
- **`StreakWeekCard`**: the Streak on Home. A `ContentCard` with the Streak flame at 48 and the number in `displayMedium`, "day Streak" in `titleLarge` and a line in `bodyMedium` saying what today's Daily Set would do for it. Under them this calendar week, Monday to Sunday, as 32 dots with two letters of the weekday below each: a completed day is `streak` with a tick, a day that passed without one is `surfaceContainerHigh` with a dashed `outline` line, today has a 2 dp `streak` ring until it is completed and its letters in `titleSmall`, and a day still to come is a small 12 dot, never drawn as missed; nor is a day before the Learner's first completed one, which is drawn the same way. At a large font size the words go under the flame and the number. A screen reader hears the card as one sentence.
- **`TodayCard`**: what today holds, on Home. A `ContentCard` with a small label in `titleSmall` ("Today" in `primary` on the card with the main action, "Up next" in `onSurfaceVariant`), a `headlineSmall` title, and below it what the state needs: `UnitProgress`, `Teaches`, a `CardNote`, the `PrimaryButton`. Done states have no label and put the `success` tick beside the title.
- **`UnitProgress`**: the Unit's name in `bodyMedium`, and under it `ProgressSegments`: one 12 high segment for each of the Unit's Daily Sets, `brand` when completed, `primaryContainer` with a 2 dp `brand` border for the one the card is about, `surfaceContainerHigh` for the rest.
- **`Teaches`**: "Also today" as an `Instruction`, then one `bodyLarge` line for each of the Daily Set's Tip titles after the first, which is the card's title, with an 8 dot in `primary`.
- **`ContentCard`**: `surfaceContainerLowest`, `large` corners, 1 dp `outlineVariant` border, 16 padding, no shadow.
- **`StreakHero`**: the Streak going up, the hero of the Summary of the Daily Set that raised it and nowhere else. A `ContentCard` with everything centred: the flame at 72 and the number in `displayLarge`, both in `streak`, then "day Streak" in `titleLarge`, a line in `bodyLarge` and the week's dots (`WeekRow`), with today's filling in. At 7, 30 and 100 days the line names the milestone and the confetti is doubled. A Replay never shows it.
- **`StreakStartCard`**: the Streak before it has started, in place of the `StreakWeekCard` until a first Daily Set is completed. No number and no week: today's 2 dp `streak` ring at 48 with the flame at 24 inside it, "Start your Streak today" in `titleLarge` and how in `bodyMedium`. An empty history is never shown as a zero or as a row of empty days.
- **`Point`**: one thing worth knowing on the welcome: an icon at 24 in a 48 `primaryContainer` circle, a title in `titleMedium` and a note in `bodyMedium`.
- **`OfferCard`**: something offered once, where the Learner has a reason to want it. A `ContentCard` with an icon in `primary`, a title in `titleMedium`, a note in `bodyMedium` and a `SecondaryButton` under them. Once taken it loses the button and says what happened. The first Summary offers the Reminder with it ("Want a nudge tomorrow?", "Remind me"); no later Summary asks again.
- **`StatTile`** and **`StatRow`**: a small fact with a number. A card with the number in `headlineSmall` and what it counts under it in `bodyMedium` in `onSurfaceVariant`, centred; tiles sit side by side, equally wide, 8 apart, and go one under the other at a large font size. A screen reader hears each as a sentence. The Summary has "5 of 6 · first try" and "7 · new words". The score is a fact here, never the hero: the Summary celebrates finishing, it does not grade.
- **`ItemCard`**: one Item under "You learned": a `ContentCard` with the Polish in `titleMedium`, its meaning under it in `bodyMedium`, and at the end its `SpeakerButton` when it has a Clip.
- **`WeekRow`**: the seven dots of the Streak's week, shared by Home and the Summary.
- **`RollingNumber`**: a number that changes by rolling: the old one up and out, the new one in from below.
- **`Arriving`**: wraps something that appears to mark what just happened, such as Home's "Done for today" card when the Learner comes back from the Summary.
- **`SettingsCard`**: a group of settings that belong together. A `ContentCard` whose rows reach its edges, with a `SettingDivider` between rows that starts where their words start. Every row is at least 56 high, has its icon in `primary` on a 40 `primaryContainer` circle, its label in `titleMedium` and, under it, a note in `bodyMedium` in `onSurfaceVariant`. The whole row is tapped, and a screen reader hears it as one thing.
  - **`SwitchSetting`**: on or off. At a large font size the switch goes under the words. The switch is `brand` with an `onBrand` thumb when on, in both themes, and `surfaceContainerHighest` with an `outline` border and thumb when off.
  - **`ValueSetting`**: a value to change, such as the Reminder's time, shown in `titleMedium` on a `secondaryContainer` field with `small` corners. At a large font size the value goes under the label.
  - **`LinkSetting`**: leads out of the screen, such as to the email app. It ends with an arrow in `onSurfaceVariant`.
  - **`SettingNotice`**: something in the way of a setting, such as notifications turned off in Android: `cautionContainer` with `medium` corners inside the card, a sentence in `bodyLarge` and the one `SecondaryButton` that fixes it. It is a caution, not an `error`: nothing broke.
  - **`SettingsFootnote`**: the version, centred in `labelSmall` under the cards.
- **`Confetti`**: see Motion and touch. It is laid over the Summary's content, above the main button. A Replay gets none.
- **The Summary** from the top: a cheer in Polish in `displaySmall` ("Brawo!", "Świetnie!", "Dobra robota!", taken in turn) with its meaning under it in `bodyMedium`, then what was finished in `titleLarge` ("Daily Set complete"), the `StreakHero` when the Streak went up, the `StatRow`, and "You learned" with an `ItemCard` for each Item. The list scrolls and fades out above the main button, which says "Done".
- **Streak flame**: the flame icon in `streak` beside the number. With a Streak of zero the flame and the number are `outline`, not hidden.
- **`UnitCard`**: a Unit on the Path. A `ContentCard` with "Unit N" in `titleSmall` in `primary`, the Unit's title in `titleLarge`, "4 of 7 completed" in `bodyMedium`, `ProgressSegments` with today's Daily Set outlined, and under them the Unit's stops. A Unit that is all completed has the `success` tick beside its title.
- **`TrailStop`**: one Daily Set on a Unit's trail. Its node, then its title in `titleMedium` and under it a `bodyMedium` line with its number in its Unit and its state in words ("4 of 8 · Completed"). The stop the Learner is at has a `titleSmall` label above its title: "Today" in `primary`, or "Up next" in `onSurfaceVariant` when it unlocks tomorrow. A completed stop ends with the replay icon in `primary`. The whole stop is tapped as one, with no ripple: its node goes down. A locked stop's title is `onSurfaceVariant` and it is not tappable.
- **Path node**: a 56 circle. Completed: `completed` fill with a tick in `onSuccess`, on the bottom edge in `completedEdge`. Today: `brand` fill with a play icon in `onBrand`, on the bottom edge in `brandEdge`, inside a 2 dp `brand` ring 72 across. Locked: `surfaceContainerHighest` with a lock icon in `onSurfaceVariant`, flat. Today's node sits on the card, never on a container colour.
- **The trail**: a 4 dp line joining a Unit's nodes. It is `completed` as far as the Learner has come, up to today's node, and `outlineVariant` beyond.
- **`Journey`**: the Path's scrolling column. It opens already in place, with no visible jump (see The Path).

Icons are Material Symbols, rounded style, filled, 24 dp, kept as vector drawables in `res/drawable`. An icon that sits beside text grows with the phone's font size, up to 36 dp (`Scaled.icon()`). No icon library is added to the build.

## Home

Home answers four things from the top down: hello, how the Streak stands, what to do today, and what has been built up so far.

- **The greeting** is Polish, in `headlineLarge`: "Cześć", and "Do jutra" once today's Daily Set is done. Under it one `bodyLarge` line in `onSurfaceVariant` says the state in the Base Language.
- **The `StreakWeekCard`**, 24 below the greeting. Until a first Daily Set is completed it is the `StreakStartCard`.
- **The `TodayCard`**, 16 below that. Each state has its own:

| State | Card |
| --- | --- |
| A Daily Set is ready | "Today", the Daily Set's topic as on the Path ("Czy and nie"), then "Unit 2 · About me · 4 of 8" over `UnitProgress`, `Teaches`, Start |
| In progress | The same, with "N of M Exercises done" and Continue |
| Done for today | The tick, "Done for today", "Your next Daily Set unlocks tomorrow." and a `QuietButton`, "Replay today's Daily Set", which counts for nothing and comes back to Home. Then an "Up next" card for the next Daily Set, with no button |
| Review-only, ready or in progress | "Today", "Review", a note, Start or Continue |
| Review-only, done | The tick, "Done for today" and "All caught up. Your next Review unlocks tomorrow." |
| Nothing left and nothing to review | The tick and "Every Daily Set completed" |

A Daily Set has one name and one number on every screen: its topic, the title of its first Tip, and its number in its Unit. The course-wide number is never shown. On screen a Review-only Daily Set is called a Review; the Glossary's term stays in the docs and the code.

- **"Your Polish so far"**, 24 below the cards: a `StatRow` of three `StatTile`s, the words learned, the Daily Sets completed and the longest Streak. They only ever go up, and they are not there on day one, when all three would be zero.

## The welcome

A new Learner sees it once, before Home: "Cześć!" in `displayLarge` in `primary` with "hello" under it, "Polish, a little every day" in `headlineSmall`, three `Point`s (one Daily Set a day, about five minutes, the next one unlocks tomorrow) and one `PrimaryButton`, "Start my first Daily Set", which goes straight into it. There is nothing to choose and nothing to skip.

Home scrolls if the font size makes it too tall. Nothing on it moves at rest. Coming back from the Summary of the Daily Set that raised the Streak, Home shows it happen once: the number rolls, today's dot fills in and the "Done for today" card arrives.

## The Path

The Path answers where the Learner is and how far they have come.

- **The title** is the Level's name and "Path" ("A1 Path"), with "10 of 23 Daily Sets completed" under it.
- **A `UnitCard` for each Unit**, 16 apart, each holding its Daily Sets as `TrailStop`s and ending in a `TrailMilestone`.
- **The road ahead is told a little at a time.** A Unit the Learner has not reached is only its heading, "Unit 3", its title and "8 Daily Sets · Locked", with an arrow; tapping the heading opens it and tapping again closes it.
- **Completed stops are compact**, with a 40 node. **Today's stop has the room**: its ring, "Today", its title, what else it teaches ("Also today") and a `PrimaryButton`, Start or Continue, right there. The whole stop can still be tapped.
- **A locked stop does not say "Locked"**: its padlock does, and a screen reader is still told. The next stop says "Unlocks tomorrow".
- **`TrailMilestone`**: the end of a Unit's trail, a 40 circle with a star. Grey with "End of Unit 2" until every Daily Set of the Unit is completed, then `streak` with "Unit 2 complete". The Summary of the Daily Set that completes a Unit says "Unit 2 complete" with the Unit's title in place of "Daily Set complete", and its confetti is doubled, once.
- **A Daily Set's title is the title of its first Tip** (`docs/authoring.md`). A Daily Set with no Tip shows "Daily Set N" in its place, numbered within its Unit.
- **Three states, each with its own icon**: a tick (completed), a play arrow in a ring (today), a lock (locked).
- **It opens at the stop the Learner is at**: the first Daily Set not yet completed. If that stop is within the top 60% of the screen when its Unit's card is at the top, the Path opens at the top of the card; otherwise the stop sits a third of the way down. With every Daily Set completed it opens at the top.
- **Tapping** a completed stop starts a Replay, today's opens today's Daily Set, a locked one does nothing.
- With every Daily Set completed, a line after the last Unit says more Units are on the way.

## Settings

Settings is quiet: three `SettingsCard`s, 16 apart, and nothing that moves.

- **The Reminder**: its switch, and when it is on, its time. If Android will not show notifications, a `SettingNotice` between them says so and opens Android's settings.
- **Sound**: one switch.
- **Report a mistake**: a `LinkSetting` to the email app.
- **The version**, 24 below the last card.

## Practice

Practice is the fourth tab, between the Path and Settings, with a game controller for its icon. See `docs/practice.md` for how it is built.

- **The Practice screen**: a greeting in Polish in the display typeface, one line under it, and "Played today · 2 Rounds" as a chip once a Round has been played. Then the Games under the names of their groups, one `GameCard` each.
- **Locked**: until today's Daily Set is completed, a `TodayCard` with a padlock says what opens Practice and holds the button that starts or continues the Daily Set. Every `GameCard` under it is flat, with a padlock for its icon. A Game the Learner has not learned enough for is locked in the same way and says what opens it.
- **`GameCard`**: `surfaceContainerLowest`, the bottom edge, the Game's icon on a 48 circle of `primaryContainer`, its name in `titleLarge`, one line in `bodyMedium`, and the Best under them with a small star in `primary`. A chevron at the end, which is dropped at large font sizes.
- **Before a Round** (`DetailFrame`): a back arrow, the Game's icon at 72, its name, and each thing to choose as a row of `ChoiceChips`. The chip chosen is `primaryContainer` with a `primary` border and a tick, so it is not marked by colour alone. Under them the Best of what is chosen, and "Play" pinned to the bottom.
- **A Round** (`RoundFrame`, `RoundStep`): the frame of a Daily Set, with the close button and a progress bar of questions answered. What is asked sits at the top and the answers at the bottom, as in an Exercise. There is no feedback panel: an answer is marked where it is, on the option, in the table's row or in the sentence's gap, in `success` with a tick or `caution` with a cross. "Continue" appears in the same colour. A typed answer gets a `RoundNote` under the question.
- **`VerbTable`**: the table of a Tip with slots for forms. An empty slot is dashed; the one asked for is `primaryContainer` with a `primary` border and a "?".
- **The result**: the Game's icon, a cheer in Polish with its meaning, "Round complete", and two `StatTile`s: the result and the Best. A beaten Best is announced by `BestLine` with a star that bounces in once. "Play again" is the main action and "Done" the quiet one. There is no confetti: that is for a Daily Set.

## App icon and splash screen

- **The icon** is a white speech bubble on `brand` saying **ą**, a letter only Polish has in this shape, set in Nunito ExtraBold in `brand`. Under the bubble is its bottom edge in `brandEdge`, 3 of the icon's 108 units deep: the same edge every button in the app has. It is drawn from shapes and a letter, which keeps to "no mascot". Everything sits inside the middle 66 units, so no launcher's mask cuts it.
- **The themed icon** (`ic_launcher_monochrome`) is the bubble with the ą cut out of it, for launchers that colour icons themselves. The Reminder's notification uses the same shape as its small icon, on `brand`.
- **The splash screen** is the icon's bubble on `brand`, in both themes, and lasts only until the first screen is drawn. Nothing is animated and nothing is loaded behind it. `Theme.PolishEasy.Starting` holds it and `MainActivity` hands over to `Theme.PolishEasy`.

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
| The play button while a Clip plays | Its corners go from a circle to a rounded square in 200 ms, and back when the Clip ends |
| Summary | The cheer stands for 500 ms, then confetti falls once for about 2 seconds |
| The Streak going up, on the Summary and then once on Home | After the same 500 ms: the old number rolls up and out, the new one rolls in from below, 350 ms; on the Summary the flame scales once from 1.35 back to 1 |
| Today's dot in the Streak's week | Starts as today's ring and fills from its centre with a little bounce, the tick growing with it |
| Home's "Done for today" card, arriving from the Summary | Fades in and grows from 0.92 to 1, 250 ms |
| From one tab to another | A fade-through: the old screen fades out in 90 ms, then the new one fades in over 210 ms |
| A Streak of 7, 30 or 100 days | The Summary's line names it and the confetti is doubled, once |

Rules:

- **Reduced motion.** When Android's "remove animations" is on (the animator duration scale is 0), every change is instant, there is no confetti and no shake, and the final state shows at once. One helper in `ui/theme` answers this; components ask it and screens do not.
- Nothing loops.
- Confetti is drawn in Compose in `brand`, `success`, `caution` and `streak`, with no library and no image. It does not cover the main button and cannot be tapped. It is for a Daily Set that counts, new or Review-only, not for a Replay.
- Durations and springs are named values in `ui/theme`, like colours.

**Haptics.** Through the view's haptic feedback, so the phone's own touch setting is respected and no permission is needed.

- Felt once, as the feedback panel appears. Right answer, with or without an accent warning: `CONFIRM`. Wrong answer: `REJECT`. On Android versions without those (below 11): `KEYBOARD_TAP` for right and `LONG_PRESS` for wrong.
- Placing a word tile: `CLOCK_TICK`.
- Finishing a Daily Set: `CONFIRM` once as the Summary appears (`KEYBOARD_TAP` below Android 11).
- Nothing else vibrates.

**Sound.** Only Clips play; there are no sound effects. Polish is said when it appears, without a tap: a Polish prompt as its Exercise opens, the answer as the feedback panel appears (not after a listening Exercise, where it was just heard), and a word tile as it is placed, where a Clip exists for the word. All of it obeys the Sound setting and "can't listen now": with either, nothing plays that the Learner did not tap a speaker for. Any sound effect added later obeys them too.

### In a Game

- A right answer moves on by itself after `Motion.MOVE_ON_MILLIS` (700 ms). "Continue" is there as well, for a Learner who does not want to wait or who uses a screen reader. A wrong answer always waits.
- The same haptics as an Exercise for right and wrong, and one for the result.
- Closing a Round asks nothing: a Round left unfinished is not kept, so nothing is lost that matters.

## Accessibility

- Every state has words or an icon as well as colour, and a state change is announced to a screen reader (the feedback panel is a live region).
- Every icon button has a content description from `strings.xml`. Decorative icons have none.
- Touch targets are at least 48 dp. Contrast as under Colour.
- The app works at the largest font size: content scrolls, the main button stays pinned and nothing is cut off. The `large-…` pictures show it. Above a font scale of 1.3 two things side by side go one under the other (`Scaled.roomy()`): a setting's switch under its words, the Streak card's words under its number, "Slow" under the play button. Icons beside text grow with it. `ValueSetting` wraps when its two parts no longer fit, and the Summary's `StatTile`s go one under the other.
- Text that is always Polish (a Polish prompt, Home's greeting, an Item's Polish) is marked as Polish with `inPolish()`, so a screen reader says it with a Polish voice. Word tiles are always Polish and are marked; option cards are marked when the Exercise's answers are Polish.
- Both themes are designed, not derived: check both sets of screenshots.

## Not part of the look

No mascot, character or illustration on the app's own screens. No photographs or images in Exercises; the pictures of Practice are for its Games only (ADR-0011). No points, levels, leaderboards or Streak freezes. A Best in a Game is the Learner's own result to beat, and nothing is bought or unlocked with it. The counts under "Your Polish so far" are not points: they count what the Learner did, nothing is bought or unlocked with them, and they are never compared with anyone's. The celebration is for finishing a Daily Set and keeping the Streak, which are the rules the app already has.
