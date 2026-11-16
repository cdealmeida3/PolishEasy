# The sentence review is recorded, and the check enforces it

ADR-0002 asks for an automated check of every sentence and verb form before it ships. Spelling and verb forms can be compared with reference sources, and the Polish check does that. Whether a sentence is grammatical and natural cannot: no reference source judges naturalness, and the grammar rules of the best free checker miss most wrong agreement and case in beginner sentences (*mam siostra*, *to jest moja brat* and *on ma dwa lat* all pass).

So the grammar and naturalness review of each sentence is done by a reviewer who did not draft it, a fresh agent session or a person, and its verdicts are committed in `polish-reviews.json`. The Polish check does not perform the review. It enforces it: a sentence or Tip with no verdict, or with a verdict that has a finding, fails the check, and a sentence that changes is unreviewed again.

We rejected having the command call a language model itself. It would need a paid key in every build, cost money on every run, and could give a different answer each time, which makes a poor gate for a release.

## Consequences

- A pull request that adds or changes Polish text brings its review entries, or CI fails.
- The verdicts are in the diff, where the reviewer of the pull request can see them, and so is every accepted problem, with its reason, in `polish-accepted.json`.
- The check runs with no network and no key, the same way on a laptop and in CI.
- The review is only as good as its reviewer. A native speaker's review later fills the same file and changes nothing else, which is what ADR-0002 calls additive.
- A model called at build time would produce the same file, so it can be added later as one more way to review.
