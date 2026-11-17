# PolishEasy

An Android app for learning Polish in short daily sessions. See `GLOSSARY.md` for the project's vocabulary and `docs/adr/` for recorded decisions.

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues on `cdealmeida3/PolishEasy`. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Before merging

Run `./gradlew checkBeforeMerge` and merge only when it passes. GitHub does not test pull requests (ADR-0015), so this run is the only one. See `README.md`.

### Authoring content

Read `docs/authoring.md` before writing or changing a Daily Set, `docs/kpk1.md` for what each chapter of the book (the Path since ADR-0013) covers, and `docs/a1-path.md` for the Starter Units.

### Changing how the app looks

Read `docs/design.md` before changing a screen or a component: it has the colours, type, shapes and motion rules, and how to get a picture of every screen. Read `CODING_STANDARDS.md` for where theme values, components and screens go, and what the build checks.

### Practice and its Games

Read `docs/practice.md` before changing Practice or adding a Game: it has the rules every Game keeps and the steps to add one.
