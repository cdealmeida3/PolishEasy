# The A1 Path

> **Superseded for Units 4 and on** by the class book: see [ADR-0013](adr/0013-the-book-is-the-path.md) and [kpk1.md](kpk1.md). Units 1 to 3 are the Starter shelf; the table below is kept for what they cover.

An outline of the Units of the A1 Path, so that each Unit fits what follows it. It is a guide, not a commitment: a Unit's contents are settled when it is authored. About 92 Daily Sets in all. The scope follows the A1 standard of the Polish state certificate. Units 1 to 3 are v1.

| # | Unit | Daily Sets | Main grammar and words |
|---|---|---|---|
| 1 | Sounds and greetings | 7 | Alphabet, sounds, stress; fixed courtesy phrases; first look at verb endings |
| 2 | About me | 8 | Personal pronouns; present of być, mieć, mówić; name, nationality, languages; czy questions; nie |
| 3 | Numbers and family | 8 | 0 to 20; age; family; possessives mój, twój; gender of nouns |
| 4 | People and work | 7 | Jobs with the instrumental (jestem lekarzem); adjectives agreeing in the nominative; kto to jest, co to jest; ten, ta, to |
| 5 | Food and drink | 8 | Accusative singular; lubić, jeść, pić; ordering in a café |
| 6 | Shopping | 7 | Numbers to 1000, prices; genitive singular after nie ma and quantities; plurals of things |
| 7 | My day | 8 | Clock time, days of the week; reflexive verbs with się; -ować verbs; how often |
| 8 | Home and city | 8 | Locative singular with w and na; gdzie; iść and jechać; directions |
| 9 | Free time | 8 | chcieć, móc, musieć, umieć with the infinitive; z with the instrumental; invitations |
| 10 | What happened | 8 | Past tense, masculine and feminine; months and dates |
| 11 | Travel and plans | 8 | Future with będę; do with the genitive; transport, tickets, weather |
| 12 | Health and help | 7 | Body; boli mnie; mi, ci; polite requests; revision of A1 |

## What Unit 1 left for Unit 2

Unit 1 has no *być*, no *pan* and *pani*, and shows the endings of only one verb, *dziękować*. Unit 2 took all three up: it introduces *być* under the ids `byc-jestem`, `byc-jestes`, `byc-jest`, `byc-jestesmy`, `byc-jestescie` and `byc-sa`, which an early build shipped, teaches *pan* and *pani* in the nominative (*Jak się pan ma?*, *czy pani mówi po polsku?*), and teaches conjugation with three full tables.

## What Unit 2 gives, and leaves for later

Unit 2 has 8 Daily Sets and 56 Items. A later Unit can rely on:

- every personal pronoun except *ono*: *ja, ty, on, ona, my, wy, oni, one*, and formal *pan*, *pani*;
- all six present forms of *być*, *mieć* and *mówić*, each its own Item (`byc-…`, `miec-…`, `mowic-…`);
- *to jest…*, *czy* questions, *nie* before a verb, *i*, *a*, *też*, *ale*, *jak*, *skąd*, *tutaj*, *trochę*;
- the names *Adam* and *Ewa*, which are Items so that sentences can have a subject;
- *mam na imię…*, *nazywam się…*, *jak się nazywasz?*; *z Polski*, *z Anglii*; *po polsku*, *po angielsku*; *czas*, *pytanie*, *problem*.

Left for later, on purpose:

- **Cases.** The instrumental is met only as four fixed words, *Polakiem*, *Polką*, *Anglikiem*, *Angielką* (ids `polakiem`, `polka-instr`, `anglikiem`, `angielka-instr`, so that the dictionary forms *Polak*, *Polka* can have their own ids). Unit 4 explains the ending. The genitive after *z* is met only as two fixed phrases.
- ***mieć* is never negated with an object** (*nie mam czasu* needs the genitive, Unit 6). *Mieć* takes only *czas*, *pytanie*, *problem*, whose form does not change after it. Unit 3 can use *mam* for age and *mieć* for family.
- ***ono*** is named in a Tip but is not an Item. ***państwo***, ***jak się pan nazywa?***, surnames, other countries and other languages (some are listed in Tips, none is an Item), ***kto*** and ***co to jest?*** (Unit 4), and the gender of nouns (Unit 3).
- *nie* keeps its Unit 1 meaning "no" as an Item; that it also means "not" is taught by a Tip.

## What Unit 3 gives, and leaves for later

Unit 3 has 8 Daily Sets and 51 Items. It is the last Unit of v1, and ends with a short speech about oneself and one's family. A later Unit can rely on:

- the numbers 0 to 20, each its own Item (ids are the word without its marks: `piec`, `szesc`, `dwadziescia`), with *plus*, *ile* and *numer*;
- age: *mam … lat*, *ile masz lat?*, and *rok*, *lata*, *lat* as three Items;
- the three genders of nouns, and the six possessives *mój, moja, moje* and *twój, twoja, twoje*, so Unit 4 can make adjectives agree;
- 14 family words in the nominative: *mama, tata, brat, siostra, syn, córka, mąż, żona, dziecko, dzieci, rodzeństwo, rodzina, babcia, dziadek* (*dzieci* is the only plural).

Left for later, on purpose:

- **Numbers count only years.** *jedna, jedno, dwie*, counting people or things, and numbers above 20 wait for Unit 6, which takes numbers further and makes them count things.
- **The accusative exists only as four fixed forms**, *brata, siostrę, syna, córkę* (ids `brat-acc`, `siostra-acc`, `syn-acc`, `corka-acc`), taught as "the form after mam, masz, ma…". Unit 5 explains the ending. They never follow a possessive, and *mieć* is still never negated with an object.
- ***matka***, ***ojciec***, ***rodzice*** (needs *moi*), ***wujek***, ***ciocia***.
- ***jego***, ***jej***, ***nasz***, ***wasz***, and *pana*, *pani* as "your". Plural possessives: *moje dzieci* is named in a Tip, never in an Exercise.
- ***kto to jest?*** (Unit 4).
