# Polski krok po kroku 1

The book's Level ([ADR-0013](adr/0013-the-book-is-the-path.md)): the 27 chapters of the class book, in its order, one Unit each. This file says what each chapter written so far gives the chapters after it, and what it leaves for later, so a chapter can be written without reading the ones before. Read it with [authoring.md](authoring.md) before writing a chapter. The plan of the whole book, with the pages, tracks and the signature activity of each chapter, is `plans/teacher-course.md` in the project files.

Starter (`a1-u01` to `a1-u03`, see [a1-path.md](a1-path.md)) comes first on the Path. A chapter does not assume a Learner has played it: every word a chapter uses, it introduces again under Starter's Item id.

## Writing a chapter

- Files under `content/kpk1/lesson-NN/`: `items.json`, then one file per section, `section-a.json`, `section-b.json` … (`section-a1.json`, `section-a2.json` for a section split in two because it has more than 8 new Items). Daily Set ids are `kpk1-lNN-a`, `kpk1-lNN-b` …
- Each section file has `section`: `letter` as in the book (none for chapter 0, whose parts are unlettered), `heading` in Polish as in the book, `meaning` in English, and `strands`, which of K, L and G it teaches. `AuthoredContentTest` and the Lesson screen read them.
- In `course.json` the chapter's entry keeps its `number`, `polishTitle`, English `title`, `accent` and `strands` (from the contents), gains `dailySets` and `lookUp` (the Item ids of the "Sprawdź w słowniku" box, introduced in the chapter), and loses `coming`.
- Mark the register of every phrase the book marks (`"register": "formal" | "informal" | "universal"` on the Item).
- Nothing is copied from the book but what ADR-0013 allows.
- **Ćwiczenia.** End each chapter with a section of the book's own kinds of Exercise (`fill_gaps`, `match_pairs`, `true_false`, `order_lines`; see [authoring.md](authoring.md)) over the chapter's words, introducing nothing new, as the book's exercise pages do. Give each grammar point of the chapter's *Komentarze gramatyczne* a Tip with a `table` in the section that teaches it, and a book's *Uwaga!* or *Zapamiętaj!* a callout Tip.
- **Class recordings** ([ADR-0014](adr/0014-class-recordings-stay-on-the-phone.md)): `content/kpk1/tracks.json` already lists every recording of every chapter, with its section letter (P for Powtórzenie), exercise number and title as the publisher tagged it, and `segments.json` their lines for step mode. A chapter's Słuchaj lists them with no work. Never commit a recording. If the section letters of the app's sections differ from the book's, the Słuchaj still groups by the book's.
- **Listening tasks** go in `content/kpk1/listening.json`, at least one per dialogue recording of the chapter: `{"type": "true_false", "id": "l05-066-1", "track": 66, "statement": "…", "meaning": {"en": "…"}, "answer": true}`, `{"type": "choose", …, "question": "…", "options": […], "correctOption": 0}` or `{"type": "dictation", …, "question": "…", "acceptedAnswers": […]}` for a number, a name or a price. `"lines": [2, 4]` narrows one to those lines of the recording (counted from 1, as `segments.json` cuts it). Write them fresh on what the recording says, after listening to it (the audio memory says how to transcribe one with Whisper in a session); the Polish check reads them like an Exercise.

## Chapter 0: Dzień dobry!

Five unlettered parts. Four have 18 Exercises each: *Polecenia* (the book's instructions: *posłuchać, powtórzyć, przeczytać, napisać, uzupełnić, przeliterować, przetłumaczyć, wybrać*), *W klasie* (*lekcja, dialog, tekst, test, ćwiczenie, polecenie, przykład, strona*), *W szkole* (*nauczyciel, nauczycielka, student, studentka, kolega, koleżanka, chłopak, dziewczyna*) and *Proszę, dziękuję, przepraszam* (*imię, nazwisko, narodowość*, with *proszę*, *dziękuję*, *przepraszam* introduced again). 27 new Items, 30 words in its Słownik. The fifth, *Ćwiczenia*, introduces nothing: 12 Exercises in the book's own kinds (match the pairs, fill the gaps from sticky notes, put a dialogue in order, mark a heard text true or false) over the words of the four, with an *Uwaga!* callout on *kolega*. *W szkole*'s first Tip has the chapter's grammar table, *a man and a woman* (*student*, *student[ka]*), the first on the Grammar shelf.

It gives the later chapters:

- the verbs of the instructions, only as infinitives after *proszę* (*proszę posłuchać*), which is how every instruction in the app is worded (`content/kpk1/instructions.json`);
- the classroom nouns in the nominative singular, with their genders in `practice.json`;
- *proszę*, *dziękuję*, *przepraszam* and the greetings of Starter, with their register.

It leaves for later: every verb in a conjugated form, every case but the nominative, plurals and adjectives. Chapter 1 takes up greetings and introducing oneself, the alphabet and the numbers 0 to 10, as its contents list them.

## Chapter 1: Pierwszy dzień w szkole

Book pages 10 to 17, recordings 004 to 028. Nine parts, 150 Exercises and 36 new Items, with 62 words in its Słownik (the Starter greetings, names and numbers it introduces again count):

- **A** *Sekretariat, prezentacja* (18): *miło mi*, *bardzo mi miło*, *mnie również* and *miło mi cię / pana / panią poznać*, with the chapter's table of what is formal, informal and universal (O, N, U, as the book marks them). **A** again, *Powitania i pożegnania* (17): the Starter greetings and goodbyes sorted the same way.
- **B** *Alfabet* (18): the alphabet and how a word is spelt aloud (*proszę przeliterować*), over *szkoła, książka, język, długopis, komputer, restauracja* and *alfabet*, with two Tips: the letters and their names, and spelling a word.
- **C** *Klasa, prezentacja* (17): *przedstawić się* and asking a name in each register (*jak pan / pani ma na imię?*, *jak masz na imię?*, *jak się pan / pani nazywa?*), with a table.
- **D** *Gdzie jest…?* (18): *gdzie, tu, tam, na lewo, na prawo*, and *toaleta, woda, kawa* to ask after.
- **E** *Nie rozumiem! Proszę powtórzyć!* (18): *nie rozumiem, nie wiem, nie pamiętam, co to znaczy?, jak się mówi…*, and the book's instructions *dopasować, podkreślić, ponumerować*, with a *Zapamiętaj!* callout.
- **F** *Liczebniki 0–10*, in two (14 each): the numbers again, now in sums with *plus*, *minus* and *równa się*.
- *Ćwiczenia* (16): the book's kinds again (pairs, gaps, dialogues in order, a heard text true or false) over the whole chapter. It introduces nothing.

There are 18 listening tasks on the chapter's 12 dialogue recordings (004, 005, 013, 015 to 018, 020 to 023, and 027 from *Powtórzenie*), two Games, **Register sort** (sort the learned phrases into O, N and U against the clock) and the **Spelled aloud** way of Spell it (hear the letters' names, as section B spells a word, and spell it with tiles), and two Chats: *At the front desk* (Pan Jan, formal: where the toilet and the coffee are, and asking him to repeat) and *In class* (Kasia, informal: names, *twój* and *mój*, sums).

It gives the later chapters:

- the three registers, which every later phrase is marked with (O, N, U on a table; the register chip in a Chat);
- *pan* and *pani* in the accusative only inside *miło mi pana / panią poznać*, learnt as a phrase;
- the classroom phrases for not understanding, which later Chats use as replies;
- *minus* and *równa się* after a number, which the Numbers rules in `docs/authoring.md` now allow.

It leaves for later: *jak się mówi* only with *po polsku*, no verb but *być*, *mieć* and *nazywać się* in the forms Starter taught, places only as *tu*, *tam*, *na lewo* and *na prawo* (no locative), and no plural nouns.
