# Authoring Daily Sets

The rules the Authored Content is written to. The [content checker](content-checks.md) decides whether content can ship; these rules are about whether it teaches. `AuthoredContentTest` holds the content to rules 1, 2, 3 and 8. The rest need a person, or the Polish check of issue #16.

Before writing a Unit, read [a1-path.md](a1-path.md) for what it covers and what the Units around it cover.

## Rules

1. **Introduce, then use.** The first Exercise that lists an Item is a `choose_translation` whose prompt is exactly the Item's text and which lists only that Item. Its right option is the Item's meaning.
2. **List what is used.** An Exercise lists every Item whose Polish is in what the Learner reads, hears or must produce: the prompt of `choose_translation`, the first Accepted Answer of `type_answer`, `word_tiles` and `listen_type`, the `text` of `listen_choose`, the filled sentence of `conjugation`. Every word there comes from one of the listed Items. Distractors and later Accepted Answers are free of this rule.
3. **Every Item gets at least 2 Exercises of different types on the day it is introduced, and at least 3 in all.** At least one of them is not a listening Exercise, so the Item can still be practised when the Learner cannot listen.
4. **Sounds before spelling.** On the day an Item is introduced, the Learner types it (`type_answer`, `listen_type`) only if a Tip has already taught every marked letter and letter pair in it. From the next day on, any Item may be typed. A `listen_choose` whose distractors differ from the answer by one sound comes only after that sound's Tip.
5. **Distractors are real Polish words that sound different from the answer.** Never a homophone (*może* and *morze*, *Bóg* and *Buk*), never a pair that differs only by a final *ę* and *e* (*dziękuję* and *dziękuje*), and never an invented spelling. For the same reason there is no `listen_type` on a word that has a homophone.
6. **Accepted Answers list every reasonable answer**, using the synonyms below and both word orders where Polish allows (*dziękuję bardzo*, *bardzo dziękuję*). A `type_answer` prompt is worded so that the listed answers are the only sensible ones. In `word_tiles`, the distractors must not let the Learner build a right answer that is not accepted.
7. **Punctuation is part of a Clip's text**, so write a phrase the same way everywhere: *jak się masz?* and *co słychać?* always with the question mark. Word tiles answers have no sentence capital. Conjugation sentences are written as sentences ("Bardzo ___.").
8. **Each Daily Set mixes at least four Exercise types.** A rough share for 17 Exercises: 6 to 8 `choose_translation` (the introductions), 3 to 5 listening, 1 or 2 `type_answer`, 3 to 5 `word_tiles`.
9. **Tips are short** (three or four sentences), explain a sound with an English comparison and one or two Polish words from that day, and sit before the first Exercise that needs them. At most 2 a day.
10. **Two Exercises that look the same to the Learner have the same answer.** Two `type_answer` Exercises with the same prompt, or two `word_tiles` Exercises with the same prompt and tiles, list the same Accepted Answers.

A Daily Set has 15 to 17 Exercises and 6 to 8 new Items. With up to 8 Review Exercises that is 23 to 25 Exercises, about 10 to 15 minutes.

## Synonyms to accept

| English | Polish |
|---|---|
| hi | cześć, hej |
| bye | pa, cześć, na razie |
| see you | do zobaczenia, na razie |
| thank you | dziękuję, dzięki |
| you're welcome | nie ma za co, proszę, proszę bardzo |
| all right, OK | w porządku, dobrze, dobra, okej |
| how are you? | jak się masz?, co słychać?, co u ciebie? |
| likewise | nawzajem, wzajemnie |
| just a moment | chwileczkę, chwilę, momencik |
| great | świetnie, super |

## How the Polish is checked

[ADR-0002](adr/0002-automated-content-check-instead-of-native-review.md) asks for a check that is separate from the drafting. Until issue #16 automates it, do it by hand and say in the pull request what each step found:

1. Look up every distinct Polish word (Items, prompts, Accepted Answers, distractors, conjugation tables) in a dictionary of inflected forms. The `morfeusz2` package from PyPI carries the SGJP dictionary and can also generate a verb's forms to compare with a table.
2. Have someone who did not draft the content review it: spelling and diacritics, grammar, register, naturalness, misleading English, missing Accepted Answers, distractors that sound like the answer, and wrong claims in Tips.
3. Drop or replace anything still in doubt.

Nobody can hear the Clips in a build session. How they sound is checked on a phone.

## After changing content

Follow "After changing content" in [content-checks.md](content-checks.md): make the audio, record the new identifiers, run the checker.

Replacing a Daily Set under an id that has shipped means a phone that completed the old one will skip the new one. Say so in the pull request: the Learner has to clear the app's storage.
