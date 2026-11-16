# Shipped identifiers are recorded in a committed file

Learner progress refers to content only by id (ADR-0003): completed Daily Sets, Exercises still to answer, Items and their Review boxes, recently shown Exercises. An id that is renamed or removed after it has shipped silently loses that progress, so the content checker must fail the build when one is.

To know what has shipped, the repository keeps `released-ids.json` at its root: every Level, Unit, Daily Set, Exercise and Item id that has been in a build. The checker reports an id that is in the file but not in the content (gone), and an id that is in the content but not in the file (not recorded). `./gradlew :engine:recordContentIds` adds new ids and never removes one.

We rejected comparing the content with git history or with the last release tag. CI checks out a single commit, every merge to the default branch is published as the `latest` build, and a check that needs the network or a deep clone would not run the same way on a laptop. A file is there in every checkout and its changes are reviewed like any other.

## Consequences

- The "not recorded" rule is what makes the "gone" rule dependable: the file can never fall behind the content, so a rename always shows up as one id gone and one not recorded.
- Removing a shipped id is still possible, as a deliberate hand edit of the file that shows in the pull request's diff.
- The file is outside `content/`, so it is not packaged in the app.
- Tip ids are not recorded: a renamed Tip is only shown once more. Clip ids are not recorded either: they name audio files, nothing a Learner keeps refers to them, and a changed text gets a new Clip, so recording them would fail the build on every spelling fix.
- Every merge counts as shipped, because every merge is published. Identifiers of placeholder content are recorded too, and replacing that content means deleting their lines.
