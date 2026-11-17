# The Polish check

The **Polish check** reads every Polish text in the Authored Content and lists everything it cannot confirm is right. It is the check [ADR-0002](adr/0002-automated-content-check-instead-of-native-review.md) asks for: separate from the drafting, and run before anything ships. The [content checker](content-checks.md) decides whether content can run; this decides whether its Polish can be trusted.

## Running it

```
./gradlew :polishcheck:checkPolish
```

It needs no network once Gradle has its libraries, and takes a few seconds. It is part of `./gradlew checkBeforeMerge`, which every change passes before it is merged, and the build that publishes the `latest` release runs it again, so no APK or bundle is published from content with a problem that is not accepted. A local build of the app does not run it. When nothing in `content/`, `polish-reviews.json` or `polish-accepted.json` has changed since it last passed, Gradle skips it (`UP-TO-DATE`).

It exits 0 when no problem is left, 1 when any is, and 2 when it is run with the wrong arguments. Each problem is one line, like the content checker's: the file to open, the identifier, what is wrong.

```
a1/unit-02/day-03.json: a1-u02-d03-e09: jestesmy is not in the dictionary (and 3 more places)
a1/unit-03/day-02.json: a1-u03-d02-e11: "ile ma lat twój syn?" has not been reviewed
Polish check failed: 2 problems. Report: polishcheck/build/polish-check/report.md
```

The report, `polishcheck/build/polish-check/report.md`, has a table of what each pass looked at, every problem grouped by pass with its **key** and every place its text is written, what is awaiting review, what is accepted and why, and the sources. The same content always gives the same report. A run on GitHub keeps it as the artifact `polish-check`, also when the build fails.

## What it looks at

The engine lists the Polish (`polishContent` in `PolishContent.kt`): each Item's text; the prompt of a `choose_translation`; every Accepted Answer of `type_answer`, `word_tiles` and `listen_type`, and every reading answer; the sentence of a `conjugation` Exercise filled with each Accepted Answer; the text of a `listen_choose`; every distractor; every form in a Tip's conjugation table. Each comes with the English it stands for, where it stands for any. The same text with the same English is one **sentence**, however many places write it; *Ile masz lat?* and *ile masz lat?* are two, because the capital is part of what is checked.

## The passes

1. **Dictionary.** Every word of every sentence must be a form in the reference dictionary. In a Tip, which is written in English, every word of more than two letters that has a Polish letter (*ą ć ę ł ń ó ś ź ż*) must be.
2. **Verb forms.** Wherever the content says a form is the form of a verb for a person, the form is compared with the reference conjugation of that verb. The content says so in three places: an Accepted Answer of a conjugation Exercise, a form in a Tip's table, and an Item that a conjugation Exercise practises and accepts as its answer.
3. **Grammar rules.** Every sentence goes through the reference's grammar rules, and one rule written here, `PRONOUN_VERB`: a pronoun directly followed by a word that can only be a verb of another person (*my jestem tutaj*).
4. **Review.** Every sentence, with its English, and every Tip must have a recorded verdict from a reviewer who did not draft it. A sentence with no verdict is a problem. A verdict with a finding is a problem.

There is a fifth, smaller pass, **Sound gaps**. The Game of that name shows a learned word with a gap and offers spellings that sound alike (*sz*, *ś*, *s*). The engine lists every wrong spelling it would offer (`Course.soundGaps()`), and none may be a form in the dictionary: asking for the *sz* of *kasza* would call *kasa* wrong. Such a word is a problem until it is listed under `realWords` in `content/practice/practice.json`, and then that gap is never asked. A word listed there that the dictionary does not know is a problem too. The dictionary is generous (*sześc* and *jeszce* are in it), so a few gaps are left out that need not be; none is asked that should not be.

A sixth pass, **Genders**, is for Mój, moja, moje. The practice file gives each noun the Game sorts a gender, and names the word for "my" of each gender. The dictionary must know the noun as a singular nominative noun of that gender (*tata* is masculine, whatever its ending), and the word for "my" as one that goes with it. A noun in another form, such as *siostrę*, is a problem: list the form a dictionary gives.

## What the machine cannot see

The first three passes are a net with wide holes. The review is what catches agreement, case and whether anyone would say it.

- The tests keep eight wrong sentences the machine passes catch (*Dzien dobry*, *mówie po polsku*, *Ewa ma pięć lata*, *jestem z Polska*, *my jestem tutaj*…) and seven they miss: *mam siostra*, *to jest moja brat*, *mój mama ma na imię Ewa*, *ona mają syna*, *on ma dwa lat*, *Adam mam syna*, *czy ty mówić po polsku?*. When the check was planned, a wider sample of 20 wrong A1 sentences gave 7 caught.
- The dictionary ignores capitals: *polski* and *Polski*, *ewa* and *Ewa* are all known. A proper noun written small passes, and is left to the review.
- The dictionary knows forms, not sentences: *mam siostra* is two good words.
- `PRONOUN_VERB` only speaks when the word after the pronoun can be nothing but a verb. *mamy* is also a noun and *mają* is also a form of *maja*, so *ona mamy* and *ona mają* pass. A rule that cries wolf gets its findings accepted unread.
- Many grammar rules say nothing about the last word of a text that does not end its sentence, and answers are written without a full stop (authoring rule 7). The check adds one before asking the rules, except to a single word.
- Two rules of the reference are switched off: `UPPERCASE_SENTENCE_START`, because answers have no sentence capital on purpose, and `MORFOLOGIK_RULE_PL_PL`, the speller, because the dictionary pass already reports a misspelt word once.
- An Item that is a verb form but that no conjugation Exercise names (*przepraszam*, *proszę* as phrases) makes no claim about a verb and a person, so it gets the dictionary and the review only.
- Whether an Exercise lists every reasonable Accepted Answer (authoring rule 6) is not checked. The review is of each sentence, not of each Exercise.
- Nobody hears the Clips. See [audio.md](audio.md).

## The review

`polish-reviews.json`, at the repository root, holds the verdicts:

```json
{
  "sentences": [
    { "text": "jestem z Polski", "meaning": "I am from Poland" },
    { "text": "mają" },
    { "text": "to moja brat", "meaning": "this is my brother", "finding": "moja is feminine; brat needs mój" }
  ],
  "tips": [
    { "id": "a1-u02-d03-t01", "hash": "3fa9c1d2e07b" }
  ]
}
```

An entry with no `finding` says the text is right. A sentence entry stands for the sentence with exactly that text and that English. A Tip entry stands for the Tip with that id as it read when it was reviewed: `hash` is taken from its title, body and table, so editing a Tip sends it back to review, as changing a sentence or its English does.

### Who reviews

Someone who did not draft the text. In an agent session, the session that wrote or changed content starts one fresh agent that is given only `to-review.json` and the checklist below, and records what comes back as it is. A person can do the same job with the same file, which is how a native speaker's review would be added.

### How

1. Run the check. It writes everything with no verdict to `polishcheck/build/polish-check/to-review.json`, in the format above, each sentence with its `uses` (file, id and whether it is an Item, a prompt, an answer, a distractor or a table form) and each Tip with its `text`.
2. The reviewer adds a `finding` to each entry that has something wrong: one sentence saying what is wrong and what would be right. Everything else is left as it is.
3. Record the reviewed file:

   ```
   ./gradlew :polishcheck:recordPolishReview -Previewed=<file>
   ```

   It merges the file into `polish-reviews.json`, replaces an earlier verdict on the same sentence or Tip, drops verdicts on text the content no longer has, and writes the file sorted. It refuses a file with a verdict on something that is not in the content as it is now, and changes nothing, so a verdict cannot be recorded on text that has since changed.
4. Run the check again. For each finding, fix the content or accept the finding (below). A fixed sentence is a new sentence: it is unreviewed, and goes back through steps 1 to 3 with a reviewer who is not told what the fix was for.

### The reviewer's checklist

For each sentence:

- spelling and diacritics;
- a capital on every proper noun and nationality noun (*Polski*, *Anglii*, *Polakiem*) and on nothing else mid-sentence; no sentence capital and no full stop is how answers are written, and is not a finding;
- person and number of the verb against its subject;
- case after each preposition and after *mieć*;
- gender of each possessive and adjective against its noun;
- *rok*, *lata*, *lat* after a number;
- whether the Polish means the English it is given as, including which "you";
- register (*pan*, *pani* with the third person);
- whether a Pole would say it;
- for a distractor or a bare form, which has no English: only that it is real, correct Polish.

For each Tip: every claim about Polish is true, and every Polish word in it is spelt right.

## Accepting a problem

A problem that is known and right to leave is named in `polish-accepted.json`, at the repository root, with a reason:

```json
{
  "accepted": [
    {
      "problem": "grammar: PODMIOT_ORZECZENIE: to Adam, jest z Polski",
      "reason": "False alarm: \"to Adam\" is a whole clause, so the comma joins two clauses."
    }
  ]
}
```

`problem` is the key exactly as the report prints it: `dictionary: <word>`, `verb-form: <verb> <person> <form>`, `verb-form: <verb>` for a verb the reference does not know, `grammar: <RULE_ID>: <text>`, `review: <text>` or `review: <Tip id>`. An accepted problem moves to the report's Accepted section and no longer fails the check.

- A reason that is blank is a problem.
- An entry that names no problem the check now reports is a problem, so the file is always the exact list of known oddities. Delete the entry when its problem is fixed.
- A sentence or Tip that has not been reviewed cannot be accepted. Review it.

Both files are outside `content/`, so they are not packaged in the app.

## Sources

| What | Where it comes from | Licence |
|---|---|---|
| Dictionary of inflected forms | Morfologik Polish dictionary, built from PoliMorf, bundled in `org.languagetool:language-pl` | BSD 2-clause, © Marcin Miłkowski |
| Conjugation of a verb | the same dictionary, read from the dictionary form and person to the form | BSD 2-clause |
| Grammar rules | LanguageTool 6.6 | LGPL 2.1 or later |

The library comes from Maven Central and is used only by the `:polishcheck` module, a build tool. The app does not depend on that module, so none of it is in the APK or the bundle.

The version is pinned in `gradle/libs.versions.toml`. A newer LanguageTool can change what the rules report, so a bump is a pull request of its own, with the check run on the content before and after.

When the check was planned, the dictionary was compared once with SGJP (the `morfeusz2` package), which the first three Units were checked against by hand: both knew every word of the content and gave the same present-tense forms for *być*, *mieć*, *mówić*, *dziękować* and *przepraszać*. SGJP is not a source of the check.
