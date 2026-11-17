# Coding standards

How the app's screens are written. `docs/design.md` says how they look; this says where things go. The build checks what a rule can check; the rest is judgement, and a review holds a change to it.

## Where things live

| Folder (under `app/src/main/kotlin/com/polisheasy/app/ui`) | Holds |
| --- | --- |
| `theme/` | Every value: colours, the type scale, corners, space, sizes, durations |
| `components/` | Shared pieces a screen is built from: `PrimaryButton`, `OptionCard`, `DailySetFrame` and the rest |
| `screens/` | One file per screen. `Exercises.kt` holds the Exercises the player shows |
| `screens/practice/` | The screens of Practice. `Games.kt` gives each Game its words and icon; `RoundScreen.kt` shows the step of whichever Game is played (`docs/practice.md`) |
| `PolishEasyApp.kt` | Which screen shows, and what each is given |
| `Theme.kt` | `PolishEasyTheme`, which hands the values in `theme/` to everything inside it |

## Checked by the build

`./gradlew :app:checkDesignTokens` runs before every build and fails on any of these outside `ui/theme`:

- a colour value (`Color(0xFF…)`) or a named colour (`Color.Red`). `Color.Transparent` and `Color.Unspecified` are allowed.
- a font size (`16.sp`), or `fontSize =`, `fontWeight =`, `fontFamily =`, `FontWeight`, `FontFamily`.

The fix is never to silence the check. Name a role (`MaterialTheme.colorScheme.primary`, `PolishEasyTheme.colors.success`) or a style (`MaterialTheme.typography.titleMedium`). If the role or style does not exist, add it to `ui/theme` and to `docs/design.md` in the same pull request.

## Before merging

Run `./gradlew checkBeforeMerge` and merge only when it passes: every test (engine, Polish check, audio, screens), the Polish check itself, and the debug and release builds. GitHub does not run any of it on a pull request, and after a merge it only checks the Polish and builds the app (ADR-0015). Say in the pull request that it passed.

## Judgement calls

- **A screen adds no styling.** It arranges components and passes them words and callbacks. If a screen needs a shape, a border or a background of its own, that is a component missing: add it to `ui/components`.
- **Space comes from `Space`, sizes from `Size`.** A screen file has no `.dp` number. A component may define a size only it uses; if a second component needs it, it moves to `Size`.
- **A component takes words, not string resources or engine types**, so it can be shown with anything. The screen calls `stringResource` and reads the engine's views. A component may hold its own icon.
- **A component takes a `modifier` and applies it to its outermost element**, and does not set its own outer padding.
- **A Game's rules are the engine's.** A Round is an immutable value from `:engine` with pure moves. A screen shows it and hands the Round a move returns to the view model; it decides nothing about what is right.
- **State stays in the screen.** A component is told what to show and reports taps. What the Learner has typed, placed or tapped is held by the screen, because the main action below the Exercise needs it too.
- **Never colour alone.** A state that has a colour also has an icon or words (`OptionCard`'s tick and cross). A state without either is not finished.
- **Motion asks `Motion`.** A component that animates takes its spec from `Motion`, which answers "remove animations" in one place. Screens do not animate.
- **Every icon button has a content description**, passed in by the screen from `strings.xml`. A decorative icon has `contentDescription = null`.
- **A new screen or a new state of one gets a `shot(…)` in `ScreenshotsTest`**, and the pull request shows the pictures in both themes.
- **Names come from `GLOSSARY.md`**: `DailySetFrame`, not `LessonScaffold`.
