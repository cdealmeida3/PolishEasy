# Authoring Daily Sets

The rules the Authored Content is written to. The [content checker](content-checks.md) decides whether content can ship; these rules are about whether it teaches. `AuthoredContentTest` holds the content to rules 1, 2, 3 and 8, and to part of rules 10 and 11: every reading form plays, every reading answer is accepted read and refused heard, and a `listen_type` that is read looks like no `type_answer` with other answers. Whether the Polish itself is right is the job of the [Polish check](polish-check.md). The rest need a person.

Before writing a chapter of the book, read [kpk1.md](kpk1.md) for what it covers, how its files are laid out and what the chapters before it give. [a1-path.md](a1-path.md) covers the Starter Units.

## Rules

1. **Introduce, then use.** The first Exercise that lists an Item is a `choose_translation` whose prompt is exactly the Item's text and which lists only that Item. Its right option is the Item's meaning.
2. **List what is used.** An Exercise lists every Item whose Polish is in what the Learner reads, hears or must produce: the prompt of `choose_translation`, the first Accepted Answer of `type_answer`, `word_tiles` and `listen_type`, the `text` of `listen_choose`, the filled sentence of `conjugation`. Every word there comes from one of the listed Items. Distractors and later Accepted Answers are free of this rule.
3. **Every Item gets at least 2 Exercises of different types on the day it is introduced, and at least 3 in all.** At least one of them is not a listening Exercise, so the Item can still be practised when the Learner cannot listen.
4. **Sounds before spelling.** On the day an Item is introduced, the Learner types it (`type_answer`, `listen_type`) only if a Tip has already taught every marked letter and letter pair in it. From the next day on, any Item may be typed. A `listen_choose` whose distractors differ from the answer by one sound comes only after that sound's Tip.
5. **Distractors are real Polish words that sound different from the answer.** Never a homophone (*może* and *morze*, *Bóg* and *Buk*), never a pair that differs only by a final *ę* and *e* (*dziękuję* and *dziękuje*), and never an invented spelling. For the same reason there is no `listen_type` on a word that has a homophone.
6. **Accepted Answers list every reasonable answer**, using the synonyms below and both word orders where Polish allows (*dziękuję bardzo*, *bardzo dziękuję*). A `type_answer` prompt is worded so that the listed answers are the only sensible ones. In `word_tiles`, the distractors must not let the Learner build a right answer that is not accepted.
7. **Punctuation is part of a Clip's text**, so write a phrase the same way everywhere: *jak się masz?* and *co słychać?* always with the question mark. Word tiles answers have no sentence capital. Conjugation sentences are written as sentences ("Bardzo ___.").
8. **Each Daily Set mixes at least four Exercise types.** A rough share for 17 Exercises: 6 to 8 `choose_translation` (the introductions), 3 to 5 listening, 1 or 2 `type_answer`, 3 to 5 `word_tiles`.
9. **Tips are short** (three or four sentences), explain a sound with an English comparison and one or two Polish words from that day, and sit before the first Exercise that needs them. At most 2 a day, and at least 1: **the title of a day's first Tip is the Daily Set's title on the Path**, so it names what the day is about in a few words ("Counting to five"), and no two days of a Unit share one. **In a Tip's body, put `*` on each side of the Polish:** `*Dziękuję* is thank you`. A word, a phrase with its question mark (`*jak się masz?*`), a letter or pair of letters (`*sz*`) and an ending (`*-my*`) are all marked; an English sound written out ("pshe") is not. The Learner sees the marked text in bold and never sees the marks. The content check fails on a mark without its partner and on a word spelt with a Polish letter outside the marks, and the Polish check looks up every marked word in the dictionary. Marks are not part of the reviewed text, so adding or moving one does not send a Tip back to review. Titles are not marked.
10. **Two Exercises that look the same to the Learner have the same answer.** Two `type_answer` Exercises with the same prompt, or two `word_tiles` Exercises with the same prompt and tiles, list the same Accepted Answers.
11. **A listening Exercise can also be read.** Its `meaning` is what the spoken text means, worded like a `type_answer` prompt (the "you" and "your" conventions apply). For `listen_choose` it fits the text and none of the distractors, so a distractor is never another way to say the same thing (*chwila* beside *chwileczkę*). For `listen_type`, `readingAnswers` lists every other reasonable answer to that meaning (rule 6 and the conventions below); they are accepted only when the Exercise is read ([ADR-0007](adr/0007-a-listening-exercise-is-read-in-place.md)).

A Daily Set has 15 to 18 Exercises and 6 to 8 new Items. With up to 8 Review Exercises that is 23 to 25 Exercises, about 10 to 15 minutes.

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
| here | tutaj, tu |
| my name is | mam na imię, nazywam się, jestem (only where the prompt does not name the verb) |
| what's your name? | jak masz na imię?, jak się nazywasz? |
| mum | mama |
| dad | tata |
| how old are you? | ile masz lat?, ile lat masz? |

## Conventions since Unit 2

- **English "you" always says which.** "you (one friend)" is *ty*, "you (several people)" is *wy*, "you (to a man, formal)" is *pan*, "you (to a woman, formal)" is *pani*. The same for "they": "they (men, or men and women)" is *oni*, "they (women)" is *one*. Where a sentence depends on who is speaking, the prompt says so: "I am English (a woman speaking)".
- **The pronoun is optional, so both are accepted.** The first Accepted Answer is the natural one without *ja*, *ty*, *my*, *wy* (*jestem z Polski*), and the version with the pronoun is also accepted (*ja jestem z Polski*). The third person keeps its pronoun or name. When the first Accepted Answer has no pronoun, the Exercise does not list the pronoun's Item.
- **A question is accepted with and without *czy*** in `type_answer` (*czy pan ma czas?*, *pan ma czas?*). In `word_tiles`, the tiles of the first Accepted Answer include *czy*, so the answer without it is listed too.
- **Formal "you" takes the form of *on*:** *czy pan mówi po angielsku?* A conjugation Exercise with *pan* or *pani* in its sentence has the person `on`.
- **Word order to accept:** *mówię trochę po polsku* and *trochę mówię po polsku*; *ona mówi dobrze po angielsku* and *ona dobrze mówi po angielsku*; *ja też jestem z Polski* and *też jestem z Polski*.
- **A masculine and a feminine answer are both accepted** when the prompt does not say who is speaking: "I am Polish" is *jestem Polakiem* and *jestem Polką*.
- **Word tiles can be left unused**, so a shorter right answer that the tiles can build is listed too: *to jest Adam* and *to Adam*.
- **Listening:** no `listen_type` on a bare one-syllable word (*i*, *a*, *on*, *są*, *ma*, *czy*): put it in a short sentence. *czy* is never a distractor for or against *trzy*. Never pair *Polką* with *Polka*, or *mówię* with a made-up *mówie* (rule 5). When the spoken text is a question, the wrong options carry the question mark too.
- **No two conjugation Exercises have the same verb, person and sentence** (rule 10, and the Review needs variety): only one bare Exercise for each form.
- **Every form of a verb is introduced on a day whose Tip shows that verb's table**, and is practised by a conjugation Exercise. `AuthoredContentTest` holds *być*, *mieć* and *mówić* to this.

## Conventions since Unit 3

- **Numbers count only years.** *Jeden* and *dwa* change with gender (*jedna siostra*, *dwie siostry*) and counted people need other forms (*dwóch braci*, *dwoje dzieci*). Until a Unit teaches that, a number is followed only by *rok*, *lata*, *lat*, *plus*, *to*, *jest*, another number, or nothing. *numer dziewięć* is the other honest use. `AuthoredContentTest` holds the content to this.
- **A sum is accepted with *to*, *jest* and *to jest*:** *dwa plus dwa to cztery*, *dwa plus dwa jest cztery*, *dwa plus dwa to jest cztery*. Its tiles include *jest* so that all three can be built. A question about a sum is accepted as *ile to jest dwa plus sześć?*, *ile jest…?*, *ile to…?*, and as *dwa plus sześć to ile?*, *…to jest ile?*
- **Age prompts read "X is N years old".** For one year, *on ma rok* comes first and *on ma jeden rok* is accepted. 2 to 4 take *lata*, 0 and 5 to 20 take *lat*; a test checks every number that stands before one of the three.
- **Asking an age:** *ile lat ma twój syn?*, *ile ma lat twój syn?* and *ile twój syn ma lat?* are all accepted. *ile ma twój syn lat?* and *twój syn ile ma lat?* are not: the review for the Polish check found the first unnatural and the second natural only as a follow-up (*a twój syn ile ma lat?*). The echo question *twój syn ma ile lat?* is not offered either.
- **English "your" always says "(one friend)"** and is *twój*, *twoja*, *twoje*. It is never used to *pan* or *pani*, whose word (*pana*, *pani*) is not taught yet.
- **A possessive agrees with the noun after it**, and a test checks each one against the gender of its noun, so a new family noun has to be added to the lists in that test.
- **No possessive before *brata*, *siostrę*, *syna*, *córkę*** (*mam mojego brata* needs a form that is not taught), and they stand only after a form of *mieć*, after *i*, or after *rodzeństwo*.
- **Listening:** no `listen_type` on a bare one-syllable number or noun (*dwa*, *lat*, *brat*): use a run of three numbers or a sentence. *czy* is still never against *trzy*.

## The book's chapters

The rules above hold for every section of the book. On top of them ([ADR-0013](adr/0013-the-book-is-the-path.md)):

- **Nothing is copied from the book** but its titles, the K, L and G lists of its contents, its word lists and its section headings. Dialogues, readings, exercises and illustrations are not: a section's Exercises are written fresh on the chapter's words and grammar.
- **A section is a Daily Set** with a `section` (its `letter`, the book's `heading` in Polish, its English `meaning`, and the `strands` it teaches). The headings are in the Polish check like any sentence.
- **Every word a section uses is introduced in its chapter**, even one Starter or an earlier chapter introduced, under the same Item id (rule 1). Then the chapter's Słownik and Fiszki have every word it uses.
- **Register.** An Item the book marks O, N or U carries `"register": "formal"`, `"informal"` or `"universal"`. It is shown beside the phrase when it is introduced and in the Słownik.
- **Instructions are the book's.** Every Exercise shows its kind's Polish instruction from `content/kpk1/instructions.json` ("Proszę przetłumaczyć."), with its English one tap away. A new kind of Exercise adds its instruction there; the content check fails while one is missing.
- **The chapter's Test is made, not written**: up to 20 of its Exercises, every kind mixed. A chapter of fewer than 20 Exercises has a shorter Test.

## How the Polish is checked

[ADR-0002](adr/0002-automated-content-check-instead-of-native-review.md) asks for a check that is separate from the drafting. That is the [Polish check](polish-check.md):

1. Run `./gradlew :polishcheck:checkPolish`. It compares every word with a dictionary and every verb form with a reference conjugation, applies grammar rules, and lists every sentence and Tip that has no review verdict.
2. Have the unreviewed sentences reviewed by someone who did not draft them, and record the reviewed file. In an agent session that is one fresh agent given only `to-review.json` and the checklist in [polish-check.md](polish-check.md).
3. Fix what is wrong, or accept it with a reason. Drop or replace anything still in doubt.

The review is of each sentence. Missing Accepted Answers (rule 6) and distractors that sound like the answer (rule 5) are still for the author and the pull request's reviewer to look for.

Nobody can hear the Clips in a build session. How they sound is checked on a phone.

## After changing content

Follow "After changing content" in [content-checks.md](content-checks.md): make the audio, give the new words that can be pictured their pictures, record the new identifiers, run the checker, run the Polish check and record the review.

Replacing a Daily Set under an id that has shipped means a phone that completed the old one will skip the new one. Say so in the pull request: the Learner has to clear the app's storage.
