# How PolishEasy looks

The brief for every screen. The direction is **playful, no mascot**: the app is warm, confident and fun through colour, a friendly typeface, shape, motion and touch. It has no character and no illustration. `docs/adr/0010-the-look-is-playful-with-no-mascot.md` records why.

Read this before changing a screen or a component. If a value you need is missing, add it here in the same pull request; do not invent one in a screen file.

## See what you build

```
./gradlew :app:testDebugUnitTest --tests '*ScreenshotsTest'
```

writes one PNG per screen to `app/build/screenshots/`: the fixture course in the light theme (`light-…`) and the dark theme (`dark-…`), the light theme again at the largest font size a phone offers (`large-…`), and the real course on Home, the Path and its first step (`real-…`). They are 411 × 891 dp. A run of the Build workflow started by hand on GitHub keeps them as its `screenshots` artifact; merges do not run the screen tests (ADR-0015).

- Look at the pictures before and after any change to a screen. Open the PNGs themselves; they are the only way to see the app without a phone.
- A pull request that changes how a screen looks shows the changed pictures, light and dark, in its description.
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
| `secondaryContainer` | `#F7E6DB` | `#2B211E` | Word tiles, the Polish key row, a grammar table |
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
| `note` | `#FFF1A6` | `#4A4015` | A sticky note of a fill-the-gaps Exercise, and a gap holding one |
| `noteEdge` | `#D9B93A` | `#7A6A22` | A sticky note's bottom edge |
| `onNote` | `#332800` | `#FFF1A6` | The word on a sticky note |

### Lesson colours

Each chapter of the book has a colour of its own, as the book does: `PolishEasyTheme.colors.accents`, eight `AccentColors` (`fill`, `onFill`, `container`, `onContainer`), taken in turn by the chapters (`accent` in `course.json`, chapter *n* has (*n* − 1) mod 8, chapter 0 the first). In order: red, teal, purple, green, slate, olive, brown, lavender. The fill is the banner, the number disc, the section letters and the seal; the container is the current card, a filled strand and a look-up word. `ContrastTest` holds every accent: white on the fill, `onContainer` on the container and `onContainer` on a card at 4.5 to 1. The fill never carries meaning alone (its number or letter does), so it need not stand out from a card: in the dark theme it cannot, while keeping white text readable. The seal on a chapter's card is drawn in `onContainer` for that reason.

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
| `large` | 20 | Cards, a grammar table, a callout |
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
- **Listening controls** (`PlayControls`): the `PlayButton` centred with "Slow" beside it as a `TonalButton`, which goes under it at a large font size. "Can't listen now" is a subdued `QuietButton` under the answers. Once a listen-and-choose Exercise is answered, a small `MicButton` takes its place with "Now say it aloud." beside it, where the phone can hear; what it hears becomes "Well said!" in `success`, or what was heard with "Not quite". Nothing waits on it.
- **`ScreenFrame`**: the layout of a screen outside a Daily Set, with the edge spacing under Space.
- **`Instruction`**, **`PolishPrompt`**, **`BasePrompt`**: the label above a prompt and the prompt itself, so the choice of style between a short and a long Polish prompt is made in one place.
- **`PromptCard`**: what an Exercise asks about, set apart at the top: `surfaceContainerHigh`, `large` corners, at least 96 high, 24 at the sides, the prompt in `onSurface` and its `SpeakerButton` at the end when it can be heard. No border and no edge: it is not tapped.
- **`PromptChip`**: the verb and the person of a conjugation Exercise in one small label ("być · I (ja)"): `titleSmall` on a `secondaryContainer` pill with a 1 dp `outlineVariant` border.
- **`SentenceField`**: the sentence of a conjugation Exercise in `headlineMedium`, with the gap drawn as a slot the Learner types into: `secondaryContainer`, `small` corners, at least 96 wide, growing with what is typed, with a 2 dp border in `outline`, `primary` while it has focus. The meaning of the sentence is under it in `bodyLarge` in `onSurfaceVariant`. A conjugation Exercise with no sentence has the chip and an `AnswerField` at the bottom.
- **`ProgressBar`**: 12 high, fully rounded, `brand` on `surfaceContainer`. In a Daily Set it counts steps, the Tips read and the Exercises answered correctly, so it moves the first time Continue is tapped on a Tip; a screen reader hears "3 of 9 steps done".
- **`TipPoints`**: a Tip's prose in bites. Its first sentence is the lead, in `titleMedium`; each sentence after it is on a line of its own in `bodyLarge` behind an 8 `primary` dot, 12 apart. The content is not changed: the screen splits the body at its full stops. The Polish a Tip marks (`docs/authoring.md`) is set in Nunito ExtraBold in the text's own colour (`withPolish`), and a screen reader says it as Polish.
- **`GrammarTable`**: a table as the book draws one, for a Tip's verb or its endings. `secondaryContainer`, `large` corners, a 1 dp `outlineVariant` border, 16 padding; its title in `titleLarge`; the column heads in `labelLarge`, each with its gender as a `primaryContainer` pill holding the book's icon (a man, a woman, a child, a group, things) and *m*, *f*, *n*, *mo* or *nmo*; then a row for each case or person, at least 48 high, headed in `titleSmall` `primary` with its question (*kto? co?*) in `labelSmall` under it. The words are `titleMedium` on one line, a word too wide for its column at a large font size set smaller to fit, never split. **The ending is in a red box**, as the book prints it: `primary` letters in a 2 dp `primary` outline with 4 corners, a narrow space either side. A screen reader hears a row as one: "feminine, studentka". A table of one column, a verb's forms, ends each row in a `SpeakerButton` when the form has a Clip.
- **`Callout`**: the book's *Uwaga!* (watch out) and *Zapamiętaj!* (remember) boxes, a Tip of its own kind. *Uwaga!* is `cautionContainer` with a `priority_high` icon, *Zapamiętaj!* is `tertiaryContainer` with a light bulb; the label in `titleLarge` in Polish, then the Tip's `TipPoints` in the container's own text colour. `large` corners, no border.
- **`StickyNote`**: a word to put in a gap, on a `note` card with `small` corners and a `noteEdge` bottom edge like a tile's, tipped 1.5 degrees one way, the next the other way, in `titleMedium` `onNote`, at least 48 high. A used note stays where it was, faded to 70%, and cannot be tapped, so the notes never shift; a screen reader hears "used".
- **`GapText`**: a fill-the-gaps text in `headlineSmall`, flowing like a sentence, with each gap a slot at least 72 wide with `small` corners: its number when empty, the selected one with a doubled `primary` border, a filled one the note's colours with its word. Tapping a filled gap gives the note back. Once checked, a gap is `successContainer` with a tick or `cautionContainer` with a cross.
- **`MatchBoard`**: the two columns of a match-the-pairs Exercise, the Polish on the left and the meanings mixed on the right, as `PairCard`s 56 high and 8 apart in `titleSmall`, 32 between the columns. The word tapped first is `primaryContainer` with a `primary` border, and its Clip plays. A right pair turns `successContainer` with a `success` border and is joined by a 4 dp round-capped `success` line from one card to the other, so the board ends as the book's lines between the columns. A wrong meaning shakes and the word stays chosen. The Exercise is answered when the last pair joins: right with no wrong meaning tapped, else wrong, and it comes back later.
- **`StatementRow`**: a true-or-false statement on `surfaceContainerLowest` with `medium` corners and a hairline `outlineVariant` border: its number in `titleMedium` `primary`, the Polish in `titleMedium`, its meaning in `bodySmall` under it, and two 40 round chips, P and N, 48 to a finger. The chosen chip is filled; once checked it is `success` or `caution`.
- **`DialogueLine`**: a line of a dialogue to put in order. Not yet placed: an outlined card. Placed: `surfaceContainerHigh` with its number in a small `primary` circle; tapping it takes it back. Once checked, each line is `successContainer` with a tick or amber with a cross.
- **`ReadingText`**: a short text to read, a line to each turn in `titleMedium`, on `surfaceContainerHigh` with `large` corners, a `SpeakerButton` on each line that can be heard.
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
- **The Summary** from the top: a cheer in Polish in `displaySmall` ("Brawo!", "Świetnie!", "Dobra robota!", taken in turn) with its meaning under it in `bodyMedium`, then what was finished in `titleLarge` ("Daily Set complete"), the `StreakHero` when the Streak went up, the `StatRow`, and "You learned" with an `ItemCard` for each Item. The list scrolls and fades out above the main button, which says "Done". At the Learner's own pace, after a new Daily Set with another one to start, the main button is "Start the next one" and "Done" is a `SecondaryButton` under it.
- **Streak flame**: the flame icon in `streak` beside the number. With a Streak of zero the flame and the number are `outline`, not hidden.
- **`LessonCard`**: a chapter on the Path. A card at least 72 high with the chapter's number in a 48 disc in its colour (a Starter Unit's place stands in), the Polish title in `titleMedium` and the English under it in `bodyMedium`, and "2 of 4 sections" in `labelSmall`. The card of the chapter the Learner is at is in its colour's container, with "You are here" above the title. A chapter all completed has the `success` tick, one whose Test is passed the seal in its `onContainer`; a coming one says "Coming soon" and its title is `onSurfaceVariant`. It ends with an arrow.
- **`LessonBanner`**: the top of a Lesson screen, like the book's opening page: the chapter's fill with `extraLarge` corners, "Lekcja 5" in `titleSmall`, the title in `headlineMedium` and the English in `bodyLarge`, and the number in `displayMedium` at the end, all in `onFill`.
- **`StrandChip`**: one of K, L and G, as the book marks what a chapter teaches: the letter on a small square in the chapter's fill, the strand's name in `labelSmall` ("Gramatyka · grammar"), its topics in `bodyMedium` as the contents give them, and "1/3" at the end. Its background fills from the start in the chapter's container as its sections are completed. A screen reader hears it as one sentence.
- **`WordChip`** in a **`ChipRow`**: a look-up word of "Sprawdź w słowniku", the Polish in `labelLarge` on the chapter's container. Tapped, it says the word and opens to show its meaning in `bodySmall` under it.
- **`ToolTile`**: one of the Powtórzenie tools, Słownik, Fiszki and Test, three side by side: an icon in a 48 circle of the chapter's container, the Polish name in `titleSmall` and a note in `labelSmall` ("30 words", "20 questions", "Best 95%"). The Test's tile wears the seal once passed.
- **`RegisterChip`**: the book's register mark, O (formal), N (informal) or U (both), as a letter on a small `secondaryContainer` square, with the word ("formal") beside it where there is room. A screen reader hears "O, formal".
- **`PolishInstruction`**: an Exercise's instruction as the book gives it, "Proszę przetłumaczyć." in `titleSmall`, with a `SpeakerButton` to hear it and the translate icon button, which shows the English under it in `labelMedium` and hides it again. Without a Polish instruction in the course, the Exercise keeps its English `Instruction`.
- **`Flashcard`**: a card at least 260 high with `extraLarge` corners and a 2 dp border in the chapter's fill. Face up, the Polish in `displaySmall` on the chapter's container; tapped, it turns over (`Motion.flip`) to the Polish small and the meaning in `headlineSmall`. Its register and speaker sit at its foot.
- **`DictionaryRow`**: a word in the Słownik, its Polish in `titleMedium`, its meaning in `bodyMedium`, its register and speaker at the end; rows are divided by `outlineVariant` lines in one `ContentCard`. **`SearchField`** above them is outlined, with the search icon, and does not open the keyboard by itself.
- **`Seal`**: a Test's score, a 128 disc with the percent in `headlineLarge`: in the chapter's fill with the seal's mark when passed, `surfaceContainerHighest` until then.
- **`RecordingRow`**: a class recording in a chapter's Słuchaj: its number ("064") in `labelLarge` on a 48 rounded square of the chapter's container, its title in `titleSmall` and "0:32 · ćwiczenie 1" in `labelMedium`, and the play arrow at the end. One not on the phone yet is faint, has no arrow and cannot be tapped; a screen reader hears "Not on this phone yet."
- **`AudioPlayerBar`**: the player of a recording on a `large` card: the `ProgressBar`, "0:06 / 0:13" (with "Line 3 of 9" in step mode) in `labelMedium`, then 5 s back, the line before, play or pause in a 72 circle of the chapter's fill, the next line and 5 s on, each at least 48.
- **`YourTurn`**: step mode's pause, "Twoja kolej! Powtórz." in `titleMedium` and its English in `labelMedium`, with the voice icon, on the chapter's container, and a small `MicButton` at the end where the phone can hear; it appears and goes with the pause, and a screen reader hears it. Tapped, the microphone holds the pause, and the note becomes what was heard ("You said: …"), with no verdict: a recording's lines have no text to hold it against.
- **`ListeningTaskCard`**: a listening task on a `large` outlined card: its kind as the book asks it ("Prawda czy nieprawda?") in `labelMedium`, the Polish question in `titleMedium`, its English under it, then `OptionCard`s ("P · prawda", "N · nieprawda", or the choices) or an `AnswerField` with "Check". Answered, the options are marked as an Exercise's are and a `RoundNote` says "Dobrze!" or "It was: …".
- **`TrailStop`**: one Daily Set on a Unit's trail. Its node, then its title in `titleMedium` and under it a `bodyMedium` line with its number in its Unit and its state in words ("4 of 8 · Completed"). The stop the Learner is at has a `titleSmall` label above its title: "Today" in `primary`, or "Up next" in `onSurfaceVariant` when it unlocks tomorrow. A completed stop ends with the replay icon in `primary`. The whole stop is tapped as one, with no ripple: its node goes down. A locked stop's title is `onSurfaceVariant` and it is not tappable.
- **Path node**: a 56 circle. Completed: `completed` fill with a tick in `onSuccess`, on the bottom edge in `completedEdge`. Today: `brand` fill with a play icon in `onBrand`, on the bottom edge in `brandEdge`, inside a 2 dp `brand` ring 72 across. Open: `surfaceContainerHighest` with a play icon in `onSurfaceVariant`, on the bottom edge in `outlineVariant`. Locked: `surfaceContainerHighest` with a lock icon in `onSurfaceVariant`, flat. Today's node sits on the card, never on a container colour.
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
| Done for today (One a day) | The tick, "Done for today", "Your next Daily Set unlocks tomorrow.", a `SecondaryButton`, "Play a Game", which opens Practice, and a `QuietButton`, "Replay today's Daily Set", which counts for nothing and comes back to Home. Then an "Up next" card for the next Daily Set, with no button |
| Today's done, keep going (own pace) | The line under the greeting is "Today's done. Keep going?". The tick, "Today's done", a note that today counts for the Streak, and "Replay today's Daily Set". Then the card of the next Daily Set, or the Review, labelled "Up next", with its Start or Continue. The Streak's note is "Your Streak is safe for today." |
| Review-only, ready or in progress | "Today", "Review", a note, Start or Continue |
| Review-only, done | The tick, "Done for today", "All caught up. Your next Review unlocks tomorrow." and "Play a Game" |
| Nothing left and nothing to review | The tick and "Every Daily Set completed" |

At the Learner's own pace Practice is always open, so "Play a Game", a `SecondaryButton`, sits under the card whenever something has been completed. With One a day it is only on the done card.

A Daily Set has one name and one number on every screen: its topic, the title of its first Tip, and its number in its Unit. The course-wide number is never shown. On screen a Review-only Daily Set is called a Review; the Glossary's term stays in the docs and the code.

- **"Your Polish so far"**, 24 below the cards: a `StatRow` of three `StatTile`s, the words learned, the Daily Sets completed and the longest Streak. They only ever go up, and they are not there on day one, when all three would be zero.

## The welcome

A new Learner sees it once, before Home: "Cześć!" in `displayLarge` in `primary` with "hello" under it, "Polish, a little every day" in `headlineSmall`, three `Point`s (short Daily Sets, about five minutes, go at your own pace) and one `PrimaryButton`, "Start my first Daily Set", which goes straight into it. There is nothing to choose and nothing to skip.

Home scrolls if the font size makes it too tall. Nothing on it moves at rest. Coming back from the Summary of the Daily Set that raised the Streak, Home shows it happen once: the number rolls, today's dot fills in and the "Done for today" card arrives.

## The Path

The Path answers where the Learner is and how far they have come. It is the book's contents page.

- **Shelves.** "Starter", the three Units the app began with, then "Polski krok po kroku 1", the book, each with its name in `headlineSmall` and "12 of 23 sections completed" under it. Every Level is on it from the start: nothing waits for another shelf.
- **A `LessonCard` for each chapter**, 8 apart, in the book's order and colours: *Lekcja 0, Dzień dobry!* to *Lekcja 26, Same problemy!* A chapter still being written is on the shelf, "Coming soon", and opens on what it will teach.
- **It opens at the chapter the Learner is at**, the first with a section not completed, whose card says "You are here".
- **Tapping a card** opens the chapter's Lesson screen. Back goes to Home.

## The Lesson screen

A chapter, laid out like its opening page in the book, from the top:

- **The `LessonBanner`**, then a **`StrandChip`** for each of K, L and G the chapter teaches.
- **"Sprawdź w słowniku"**, with the look-up words of the book's page as `WordChip`s.
- **"Sections"**: the chapter's sections as the trail of `TrailStop`s, each titled with its letter and the book's heading ("B · Liczebniki 20–100") and the English under it, ending in a `TrailMilestone`. A Starter Unit's Daily Sets are titled by their first Tip, as before.
- **"Słuchaj"**: the chapter's class recordings, under "Część A", "Część B" … (with the section's heading once the chapter has it; "Nagrania" in chapter 0, whose recordings have no letter), a `RecordingRow` each, and "3 of 3 recordings on this phone". With none on the phone, a `Point`, "Add the class recordings", and a `TonalButton`, "Add them", which opens Class audio.
- **"Powtórzenie"**: the three `ToolTile`s, Słownik, Fiszki and Test, open whatever has been completed.
- **Komentarze gramatyczne**: under them, when the chapter has a grammar table, a `LinkSetting`, "1 of 2 grammar tables of this chapter", to the chapter's own Grammar shelf.
- A chapter still being written has, in place of its sections and Powtórzenie, a `Point`: "This lesson is on its way", and then its Słuchaj, since the recordings are there before the sections are.

## A recording

Tapping a recording in Słuchaj opens it in the player (ADR-0014). From the top: the `LessonBanner` in the chapter's colour with "Nagranie 064", its title, "Lekcja 5 · część A" and the number large; the `AudioPlayerBar`; `YourTurn` while step mode waits; "Speed" as `ChoiceChips`, 0.75×, 1× and 1.25×; a `SettingsCard` with two `SwitchSetting`s, "Posłuchaj i powtórz" (step mode) and "Repeat"; "Zadania" with a `ListeningTaskCard` for each listening task, when it has any; and `QuietButton`s to the recordings before and after it in the chapter. The recording goes on in the background with Android's media notification; leaving its screen stops it. Nothing on it is saved.

The rules of the trail are the Path's own, unchanged by being on a chapter's screen:

- **Completed stops are compact**, with a 40 node. **Today's stop has the room**: its ring, "Today", its title, what else it teaches ("Also today") and a `PrimaryButton`, Start or Continue, right there. The whole stop can still be tapped.
- **A locked stop does not say "Locked"**: its padlock does, and a screen reader is still told. The next stop says "Unlocks tomorrow". Padlocks and "Unlocks tomorrow" appear only with One a day.
- **At the Learner's own pace nothing is locked.** The stop they are at is labelled "Up next" until it is started, then "Today". Every other stop not completed is open: the plain node with a play arrow, which a screen reader hears as "Open", and tapping it starts that Daily Set. If another Daily Set is in progress a dialog asks first: "Start this one instead? Your progress in … will be lost."
- **`TrailMilestone`**: the end of a chapter's trail, a 40 circle with a star. Grey with "End of Lekcja 0" until every section is completed, then `streak` with "Lekcja 0 complete" ("Unit 2 complete" for Starter). The Summary of the Daily Set that completes a Unit says "Unit 2 complete" with the Unit's title in place of "Daily Set complete", and its confetti is doubled, once.
- **Four states, each with its own node**: a tick (completed), a play arrow in a ring (today), a plain play arrow (open, own pace only), a lock (locked, One a day only).
- **Tapping** a completed stop starts a Replay, today's opens today's Daily Set, an open one starts it, a locked one does nothing. Whatever was started here comes back here when it ends or is closed.

## Słownik, Fiszki and Test

- **Słownik**: "Słownik" with the chapter under it, the `SearchField`, and every word the chapter teaches as a `DictionaryRow` in the order it teaches them. Typing finds a word by its Polish, with or without its marks (*dziekuje* finds *dziękuję*), or by its meaning. Nothing found says so in a `CardNote`.
- **Fiszki**: "Card 3 of 30", a label ("Due for Review" in `primary`, "Flashcards" for a word met, "New word"), the `Flashcard` and "Say what it means, then tap to turn it." Once turned, "Not yet" (`SecondaryButton`) and "I knew it" (`PrimaryButton`) side by side at the bottom: the word is rescheduled in Review as an answer would reschedule it. The cards due for Review come first. At the end, "Wszystkie!" in `displaySmall` with "All of them!" under it, how many were known, "Go again" and "Back to the lesson".
- **Test**: the chapter's Checkpoint is played in the Daily Set frame, labelled "Test" like a Replay is labelled "Replay"; a wrong answer is not asked again. **Its result** has the `Seal`, "Zdane!" (passed) or "Jeszcze raz!" (once more) in `displaySmall` with its English, "Test · Dzień dobry!", "New Best" when beaten, a `StatRow` with "17 of 20 right · This time" and "95% · Best", a line on the pass mark, then "Try again" and "Done". A seal newly won gets the confetti. Nothing is locked behind a Test.

## Settings

Settings is quiet: five `SettingsCard`s, 16 apart, and nothing that moves.

- **The Reminder**: its switch, and when it is on, its time. If Android will not show notifications, a `SettingNotice` between them says so and opens Android's settings.
- **Sound**: one switch.
- **One new Daily Set a day**: one switch, off by default, with "Off: learn at your own pace, as many as you like. On: the next one opens tomorrow." It takes effect at once.
- **Class audio**: a `LinkSetting`, "28 of 271 recordings of Polski krok po kroku 1 on this phone", to its screen; under it, in the same card, *Komentarze gramatyczne*, to the whole Grammar shelf.
- **Report a mistake**: a `LinkSetting` to the email app.
- **The version**, 24 below the last card.

### Class audio

Opened from Settings or from a chapter's "Add them", in a `DetailFrame`: "Class audio" and what the recordings are and that they stay on the phone in a `CardNote`; a `StatRow`, "28 of 271 · recordings on this phone" and "2.4 MB · of room they take"; what the last import did as a `RoundNote` ("Added 25 recordings.") and, if some files were not the book's, a `CardNote`; a `Point` on how to add them; and "Remove the recordings", a `QuietButton` that asks first. At the bottom, "Add the class zips" (`PrimaryButton`, Android's picker with several files at once) and "Add a folder" (`SecondaryButton`). While files are read the main button says "Adding the recordings…".

### Komentarze gramatyczne

The Grammar shelf, the book's grammar commentary (ADR-0013), in a `DetailFrame`. Opened from Settings: "Komentarze gramatyczne" as the `TopBar` with "The grammar tables of the sections you have done, by chapter.", then each chapter with a table so far under a `SectionHeading`, "Lekcja 0 · Dzień dobry!". Opened from a chapter: its `LessonBanner` with *Komentarze gramatyczne* as the title. Each table is its Tip again, in a `ContentCard`: the title, the `TipPoints` and the `GrammarTable`. A chapter with tables still to come says how many in a `CardNote`; with none yet, a `CardNote` says they come as sections are done.

## Practice

Practice is the fourth tab, between the Path and Settings, with a game controller for its icon. See `docs/practice.md` for how it is built.

- **The Practice screen**: a greeting in Polish in the display typeface, one line under it, and "Played today · 2 Rounds" as a chip once a Round has been played. Then the Games under the names of their groups, one `GameCard` each.
- **Locked**: until today's Daily Set is completed, a `TodayCard` with a padlock says what opens Practice and holds the button that starts or continues the Daily Set. Every `GameCard` under it is flat, with a padlock for its icon. A Game the Learner has not learned enough for is locked in the same way and says what opens it.
- **`GameCard`**: `surfaceContainerLowest`, the bottom edge, the Game's picture at 48, its name in `titleLarge`, one line in `bodyMedium`, and the Best under them with a small star in `primary`. A chevron at the end, which is dropped at large font sizes.
- **Before a Round** (`DetailFrame`): a back arrow, the Game's picture at 72, its name, and each thing to choose as a row of `ChoiceChips`. The chip chosen is `primaryContainer` with a `primary` border and a tick, so it is not marked by colour alone. Under them the Best of what is chosen, and "Play" pinned to the bottom.
- **A Round** (`RoundFrame`, `RoundStep`): the frame of a Daily Set, with the close button and a progress bar of questions answered. What is asked sits at the top and the answers at the bottom, as in an Exercise. There is no feedback panel: an answer is marked where it is, on the option, in the table's row or in the sentence's gap, in `success` with a tick or `caution` with a cross. "Continue" appears in the same colour. A typed answer gets a `RoundNote` under the question.
- **`VerbTable`**: the table of a Tip with slots for forms. An empty slot is dashed; the one asked for is `primaryContainer` with a `primary` border and a "?".
- **Pictures**: the pictures of Practice are Fluent Emoji in the 3D style. They are glossy, so they are the one place the app has shading: the tile under a picture stays flat with the usual bottom edge, and nothing else gains a gradient. A Game's icon is a picture from the same set, shown as it is at 48, with no circle behind it. A number's picture is its numeral in `displayMedium` in `primary`.
- **`PictureTile`**: `surfaceContainerLowest` with the outlined bottom edge and `large` corners, at least 168 high, the picture at 62% of its width. Turned: `primaryContainer` with a `primary` border, the Polish in `titleLarge` (never shrunk; a long phrase wraps), its meaning in `bodyMedium` and a `SpeakerButton`. A card with no picture shows its meaning in `titleMedium` on the front.
- **`FlipTile`**: the turn itself, about the upright axis, with the face changing at the half turn (`Motion.flip()`, 300 ms; at once when animations are removed). Each face has a `TileLook`: plain, the brand's tint, found or missed.
- **`MemoryTile`**: a `FlipTile` with four looks. Face down: `primaryContainer` with a `primary` border and a question mark. Turned up: a plain card with its picture, its Polish in `titleMedium` or its meaning in `bodyLarge`, or a speaker for a tile that is only heard. One of a pair found: `successContainer` with a `success` border and a small tick in the corner. One of two that are not a pair: `cautionContainer` with a `caution` border, for the 1.1 s before they turn back (`Motion.MISS_MILLIS`). A long word is hyphenated, never shrunk. A screen reader hears "Tile 5, face down", and for a turned tile its words, or what its picture means.
- **Memory pairs**: the instruction and the turns so far on one line, then the board three across. The rows share the height of the screen, each between 64 and 160 high; at a large font size the board scrolls. The finished board stands for 0.9 s (`Motion.BOARD_DONE_MILLIS`) before the result.
- **`PhraseChain`**: the chain being made, on `surfaceContainerHigh` with `large` corners, at least 96 high. Each word is a pill in `secondaryContainer` with an `outline` border, joined to the one before by a short line. Empty, it says "Tap the first word". Whole, the pills and the lines turn to the colours of a right answer and a tick ends the chain.
- **Phrase chains**: the instruction and the slips so far on one line; the play button when the sentence is heard, or its meaning in a `PromptCard` when it is read; the chain; and at the bottom the cards as `WordTile`s in a `TileBank`. A card that joins the chain leaves an `EmptyTileSlot`, so the others do not move. A wrong card shakes once.
- **`PictureOption`**: a picture to choose, in the states of an `OptionCard`: plain, right (`successContainer`, a tick in the corner), wrong (`cautionContainer`, a cross and one shake) and faded. At least 120 high.
- **Listen and tap**: the instruction, the play button, and at the bottom four `PictureOption`s two across or four `OptionCard`s. Once answered, a `RoundNote` writes the word that was said.
- **`GapWordText`**: a word or phrase at `headlineLarge` with a small slot inside a word, centred. The slot is `secondaryContainer` with an `outline` border while empty, and takes the colours of a right or a missed answer once filled. The word is never broken at the gap.
- **`LetterOption`**: letters to choose, at `headlineMedium`, centred on a card at least 88 high, two or three side by side, in the states of an `OptionCard` with a small tick or cross in the corner.
- **Sound gaps**: the instruction, the play button, the word with its gap and its meaning under it, and at the bottom the `LetterOption`s in one row.
- **`Keypad`**: the digits as on a telephone, three across, each a white key 56 high with the bottom edge. Left of the zero is the key that deletes, in `secondaryContainer`; right of it the key that checks, in the brand's colour with a tick, grey and flat until something is typed. Once the question is answered the whole keypad fades and does not respond.
- **`NumberDisplay`**: what has been typed, at `displaySmall`, centred in a slot like the gap of `GapWordText`. Empty, it says "?". Answered, it takes the colours of right or missed, with the tick or cross beside the digits.
- **`BucketRow`**: two or three words to sort under, side by side at least 104 high, in the states of an `OptionCard`: the Polish word at `headlineMedium` and under it a small outlined count of what it holds. At a large font they go one under the other, word and count on one line.
- **Numbers**: the instruction, the play button (or the Polish words at `headlineLarge`), the `NumberDisplay`, and the `Keypad` at the bottom. Answered, a `RoundNote` writes the question out in numerals.
- **Mój, moja, moje**: the instruction, the noun's Picture when it has one, the noun at `headlineLarge` with its meaning (or the play button, or the meaning alone), and the `BucketRow` at the bottom. Answered, a `RoundNote` writes the two words together.
- **`SpelledWord`**: a slot for each letter of a word, centred, the placed letters at `headlineSmall` in `secondaryContainer`. Whole, the slots take the colours of right, or of missed when a tile was tapped wrongly on the way, with the tick or the cross after them.
- **`MatchButtons`**: "No match" with a cross in the caution colour and "Match" with a tick in the success colour, side by side on white cards at least 88 high. The icon and the word say which is which; the colour only helps.
- **Spell it**: the instruction, the Picture or the meaning in a `PromptCard`, the `SpelledWord`, and the letters as word tiles at the bottom, each at least 48 wide. A placed tile leaves its dashed slot behind, as in an Exercise.
- **Quick round**: the instruction with the time left beside it as `0:42`, a `PromptCard` with the Polish word at `headlineLarge` over the Picture or the meaning, the count of right answers, and the `MatchButtons` at the bottom. The bar at the top of the Round is the clock: it starts full and empties, one step a second. Register sort has the same clock; nothing about it loops, pulses or changes colour as it runs out.
- **`RegisterButton`**: a register to sort into, on a white card with the bottom edge and `large` corners, at least 88 high: its mark (O, N, U) in `titleLarge` on a `secondaryContainer` square, who it is for in `titleSmall`, and the count it holds in `labelMedium`. Side by side, as tall as the tallest. A screen reader hears it as one sentence: "O, Pan or pani, 2 sorted". Faded while a wrong answer shows.
- **Register sort**: the Quick round's layout. The instruction with the time left beside it, the phrase alone at `headlineLarge` in a `PromptCard`, the count of right answers, and the `RegisterButton`s at the bottom. A wrong answer adds the phrase's `RegisterChip` and meaning to the card and a `RoundNote` saying who it is said to.
- **`FormCard`**: a paper form being filled in: an outlined `large` card on `surfaceContainerLowest`, its title in `titleMedium` in `primary`, then a row a field: the Polish label in `labelLarge` with its meaning in `bodySmall` under it, and on the right a `small` box for the answer in `titleSmall`. The box is `primaryContainer` with "?" while its field is asked, `surfaceContainerHigh` while blank, `successContainer` filled in right and `cautionContainer` put right. At a large font size the box goes under its label.
- **`OptionGrid`**: `OptionCard`s two across, each row as tall as its tallest, or one under the other at a large font size.
- **Ankieta**: the instruction with "Person 1 of 3" beside it; read, what the person says in one `ChatBubble` with a speaker, its meaning added once the form is filled in; heard, the play button until then. Under it the `FormCard`, and the options in an `OptionGrid` at the bottom.
- **`LabelBoard`**: a board of pictures to label, three across (two at a large font size), each an outlined `medium` card on `surfaceContainerLowest` as tall as the tallest in its row, the picture square at the top and its label under it in `titleSmall` on a `small` box, coloured as a `FormCard` box is. The picture asked has a `border` edge in `primary`.
- **Label the picture**: the instruction with "2 of 6" beside it, the `LabelBoard`, and the meaning of the picture asked under it, or a `RoundNote` with the right label when it was put right. The labels lie as word tiles in a `TileBank` at the bottom, a placed one leaving its dashed slot; typed, an `AnswerField` with "Check" and the Polish keys.
- **Opposites snap**: the Quick round's layout. The instruction with the time left beside it, the two words one above the other at `headlineLarge` in a `PromptCard` with a large ↔ (`displaySmall`) between them in `primary`, the count of right answers, and the `MatchButtons` at the bottom, labelled "Not opposites" and "Opposites!". A wrong answer shows a `RoundNote` with the word's real opposite and both meanings.
- **Who's who**: the instruction, the job's Picture at `pictureOption` size, or its Polish at `headlineLarge` with the meaning under it, then *Kim on jest?* in `titleLarge` and `primary`, and four `OptionCard`s at the bottom. Once answered, a `RoundNote` with the whole sentence, and under it, when the word alone was chosen, a line on the instrumental in `bodyMedium`.
- **Three groups**: the instruction, the verb at `headlineLarge` with its meaning under it, and, with *ja*, its *ja* form in `titleLarge` and `primary`; one `OptionCard` per conjugation learned, named by its endings, at the bottom. Once answered, a `RoundNote` with *ja* and *ty*, in place of the *ja* clue.
- **Hobby charades**: the instruction and the hobby's Picture at `pictureTile` size, the whole clue; four `OptionCard`s at the bottom, or, typed, an `AnswerField` with the Polish keys and "Check". Once answered, a `RoundNote` with *Lubię …* and the hobby.
- **`MoneyKeys`**: the coins and notes of Kiosk till. A coin is a round key at least 56 across on `tertiaryContainer` with a `tertiary` edge, its value (*50 gr*, *2 zł*) in `titleMedium`; a note is a wide `small`-cornered key on `secondaryContainer` with a `secondary` edge. Coins in one centred row, wrapping, and the notes in a row under them. Faded once the price is answered.
- **`MoneyCounter`**: what is on the counter, each coin or note small (40 across, `labelMedium`) in the order it came, tapped to take it back; empty, a line in `bodyMedium` `onSurfaceVariant` says to tap coins.
- **Kiosk till**: the instruction, the good's Picture at `pictureOption` size, then the price written in `titleLarge` (a sentence: *Ciastko kosztuje siedem złotych i czterdzieści groszy.*) or the play button; the sum on the counter in a `NumberDisplay` as a till shows it, *7,40 zł*, with the `MoneyCounter` under it; the `MoneyKeys` at the bottom and "Pay". Once paid, the display takes the mark and a `RoundNote` gives the price, with its words if it was heard.
- **`LottoCard`**: a card of numbers three across, each a tile at least 88 high with `medium` corners and the number in `headlineSmall`. Open, white with an `outlineVariant` edge; ticked for an earlier call, `primaryContainer` with a small tick; the number called, once answered, `successContainer` with a tick; a wrong tap, `cautionContainer` with a cross and one shake. While a call is answered the open numbers fade.
- **Lotto**: the instruction, the number called written at `headlineMedium` or the play button, and the `LottoCard` at the bottom. Once answered, a `RoundNote` with the number, and its words if it was heard.
- **`PlateView`**: the plate of Plate builder. A wide dish 144 high with 72 corners, flat, `surfaceContainerLowest` with an `outlineVariant` border; each food on it a round 72 tile on `primaryContainer` with a `primary` edge, centred and wrapping in the order it came, tapped to take it off. Empty, a line in `bodyMedium` `onSurfaceVariant` says to tap the foods below.
- **Plate builder**: the instruction, the order written in `titleLarge` (*Chleb z masłem i herbata z cytryną.*) or the play button, the `PlateView`, and the tray at the bottom as `PictureOption`s three across, a food on the plate shown chosen; "Serve". Once served, the foods wanted are right, those put on wrongly wrong, the rest faded, and a `RoundNote` gives the order.
- **`BasketView`**: the basket of Market list. A deep crate at least 128 high with `large` corners, flat, `surfaceContainer` with an `outline` border; each good in it a 64 tile with `medium` corners on `surfaceContainerLowest`, the same goods side by side so two pears are two pictures, tapped to take one out. Empty, a line in `bodyMedium` `onSurfaceVariant` says to tap the stall below.
- **Market list**: Plate builder's layout with the `BasketView` for the plate and the stall of six goods for the tray; "That's all". A tap on the stall adds one more of a good, up to four, and the good stays chosen while the basket holds it. Once paid, a good the basket holds as many of as the list asks is right, one with another number or not asked for wrong, the rest faded, and a `RoundNote` gives the list.
- **`BucketGrid`**: the cards of a `BucketRow` two across, in as many rows as it takes, each row as tall as its tallest card; one under the other when the text is large.
- **Sort the aisles**: Register sort's layout: the instruction and the time, a `PromptCard` with the product's Picture (when it has one) over its Polish in `headlineLarge`, and the aisles' signs in a `BucketGrid`, each counting what it holds. A wrong aisle marks the right one and the one tapped, writes the meaning on the card, and a `RoundNote` says which aisle the product is in.
- **Take the order**: the instruction, the guests' orders as `ChatBubble`s or, heard, the play button, the waiter's pad, and the menu at the bottom as `OptionCard`s two across; "Serve". The pad is a line for each guest, "Guest 1" in `labelLarge` `onSurfaceVariant` beside an `OptionCard` with the dish written or "…", the last dish written shown chosen and tapped to rub it out. Once served, each line is right or wrong, the dishes ordered are right on the menu and the rest faded, and a wrong table has a `RoundNote` with each guest's dish.
- **Negation flip**: Sort the aisles' clock and instruction, a `PromptCard` with the sentence in `headlineMedium`, and its two negations as `OptionCard`s one above the other. A wrong one marks the right negation and the one tapped, and a `RoundNote` gives what the right one means and says that after *nie* the thing takes the genitive.
- **Family tree**: the instruction, and in "Find them" a `PromptCard` with the phrase in `headlineMedium` and a `SpeakerButton` with sound on; then `FamilyTreeBoard`, five columns of round pictures, 52 dp, joined by `outline` lines, each with a label under it: blank, "?" in the brand's tint for the one asked, green named right, amber put right, and the Learner's *ja* on `inverseSurface`. A label shrinks to fit its column and never wraps. "Who is it?" has four `OptionCard`s two across at the bottom, one across when the text is large; in "Find them" every person but the Learner is tapped. A `RoundNote` writes who it was and what the word means.
- **Times of day**: Negation flip's clock and instruction, a `PromptCard` with the time in `displayMedium`, and four times of day as `OptionCard`s one above the other. A wrong one marks the right answer and the one tapped, and a `RoundNote` gives what the right one means.
- **Tell the time**: the instruction; in "Tell the time" a `ClockPrompt`, a clock face up to 220 dp across with 12, 3, 6 and 9 written in `titleMedium`, the hour hand short in `onSurface` and the minute hand long in the brand's colour, and four times as `OptionCard`s one above the other; in "Find the clock" a `PromptCard` with the time in `headlineMedium` and a `SpeakerButton` with sound on, and `ClockOptions`, four faces without numbers two across, each a tile marked with a tick or a cross once answered. A `RoundNote` gives the time in digits.
- **Week planner**: the instruction, which says to tap the day and then to choose its time; heard, a `PlayButton`, read and once answered a `PromptCard` with the plan in `titleLarge`; then the `Diary`, seven rows, Monday first, each the day's Polish name in a column of its own and the plans written in it, time and what, the day picked outlined in the brand's colour, the right day ticked and a wrong one crossed and shaken. Once a day is picked the four times are `OptionCard`s in a row at the bottom. A `RoundNote` gives the day, the time and what the plan means.
- **Story strip**: the instruction and the story's title in `titleMedium`; heard, a `PlayButton`, read, a `PromptCard` with the story in `titleMedium`; then the `StoryStrip`, its places side by side and square, each empty one numbered in `titleLarge`, the next to fill outlined in the brand's colour, and a filled one its picture; below, the story's pictures as `PictureOption`s two across, those already placed faded. A `RoundNote` gives the sentence of the picture placed and what it means.
- **Who said it?**: the instruction; heard, a `PlayButton`, read and once answered a `PromptCard` with the line in `titleLarge`; the speakers as `OptionCard`s one above the other. A `RoundNote` gives who said it and what the line means.
- **Directions**: the instruction; heard, a `PlayButton`, read and once answered a `PromptCard` with the route in `titleMedium`; the map as `PictureOption`s, square, three across in two rows with `Space.s` between them like streets, and under it where the Learner starts in `labelLarge` in the brand's colour. A `RoundNote` gives the place and what the route means.
- **Pronoun swap**: the instruction; a `PromptCard` with the sentence in `titleLarge`, its noun marked by `withMarked` (at its heaviest, underlined, in the brand's colour), and once answered the shorter sentence with the pronoun marked; heard, a `PlayButton` under it; the pronouns as `OptionCard`s two across. A `RoundNote` gives the sentence, what it means and what the shorter one means.
- **Ten, ta, to**: Mój, moja, moje's layout, with *ten, ta, to* in the `BucketRow`.
- **Spell it aloud**: Spell it with the play button where the Picture or meaning was; the meaning is written under the word once it is whole.
- **`ChatHeader`**: who the chat is with: the first letter of their name in a `primaryContainer` circle 48 across (the app shows no faces), the name in `titleLarge`, the chat's `RegisterChip` beside it, and the scene under them in `bodyMedium` `onSurfaceVariant`.
- **`ChatBubble`**: one line of a chat, at most 300 wide (`Size.bubbleMax`), with `large` corners except a small one by the speaker, as a messenger draws its tail. The person's are on the left on `surfaceContainerHigh`, with a speaker; the Learner's on the right in `primaryContainer`, or amber when it is the right reply put in for a wrong one. The Polish in `titleMedium`, its meaning small under it. Before a line arrives the bubble shows three dots for 650 ms (`Motion.TYPING_MILLIS`), then fades to the words (150 ms); with animations removed the words are there at once.
- **`MicButton`**: the microphone of *Powtórz na głos*. Large, where speaking is the answer, it is the play button's size in the brand's colour; small, beside something, it is a speaker's size in `primaryContainer`. While it listens it turns from a circle to a rounded square, as the play button does while it plays, and a screen reader hears "Listening". Where the phone cannot hear nothing is drawn.
- **Chat**: the `ChatHeader`, then the bubbles, newest at the bottom, scrolling. Under them the reply, never more than half the screen: `OptionCard`s to choose, the answer field with "Check" and the row of Polish letters to type, or the large `MicButton` with "Choose instead" under it. An answer is marked on the option; a missed one adds a `RoundNote` with the right reply, what the phone heard when it was said, and the note in amber with the `RegisterChip` of the reply given. The last line stands 1.6 s (`Motion.CHAT_DONE_MILLIS`) before the result.
- **Picture cards**: a `DetailFrame` with the Game's name, chips for what the cards show and which words, then the cards two across under each Unit's title.
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
| A wrong meaning on the `MatchBoard` | The same 8 dp shake, each time it is tapped |
| A right pair on the `MatchBoard` | The line between the cards is drawn as they turn green |
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
- Each pair on the `MatchBoard`: right or wrong, as for an answer, since the pairs answer one by one.
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
