# The checks run before the merge, and GitHub only builds what is published

The repository is private, so GitHub Actions minutes come out of a monthly free allowance, and they ran out. Every pull request ran the whole build twice: once for the push to its branch and once for the pull request, each with every test, the Robolectric screen tests, the Polish check and three app builds. The merge then ran it all a third time before publishing the `latest` release. Every change was already built and tested in full on the computer that made it before it was merged, so two of the three runs repeated a result we had.

So the full check is one Gradle task, `./gradlew checkBeforeMerge`, and every change passes it before it is merged. GitHub's Build workflow no longer runs on pull requests or on other branches. On a merge to the default branch it checks the Polish, builds the debug and release app and publishes them; it does not run the tests. A merge that touches only documentation (`*.md` at the root, `docs/`, `.claude/`) starts no run, since the app it would build is the same. "Run workflow" on the Actions tab still runs `checkBeforeMerge` on GitHub, on any branch, and keeps the screenshots.

## Consequences

- Where earlier records say CI fails a change (ADR-0008) or keeps a picture of every screen on every run (ADR-0010), read the check before merging. The pull request shows the pictures it changed.
- The Polish check still runs on every published build. It takes seconds, and it is the one gate ADR-0002 asks for before anything ships.
- Nothing on GitHub stops a merge that skipped the check. Whoever merges says in the pull request that it passed.
- The workflow names the default branch, `ccr-6bf7efb8-o771mt`. If the branch is renamed, the workflow's `branches` must be too, or merges stop publishing.
- `versionCode` is still the workflow's run number. Fewer runs only means it rises more slowly.

## Considered options

- *Keep the pull request run and drop the run after the merge.* Rejected: the `latest` release must be built from the merged commit, and its run number is the `versionCode` Play needs.
- *Keep a fast pull request run with only the engine tests.* Rejected: it still repeats a local result, and a run starts again for every push to the branch.
