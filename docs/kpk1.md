# Polski krok po kroku 1

The book's Level ([ADR-0013](adr/0013-the-book-is-the-path.md)): the 27 chapters of the class book, in its order, one Unit each. This file says what each chapter written so far gives the chapters after it, and what it leaves for later, so a chapter can be written without reading the ones before. Read it with [authoring.md](authoring.md) before writing a chapter. The plan of the whole book, with the pages, tracks and the signature activity of each chapter, is `plans/teacher-course.md` in the project files.

Starter (`a1-u01` to `a1-u03`, see [a1-path.md](a1-path.md)) comes first on the Path. A chapter does not assume a Learner has played it: every word a chapter uses, it introduces again under Starter's Item id.

## Writing a chapter

- Files under `content/kpk1/lesson-NN/`: `items.json`, then one file per section, `section-a.json`, `section-b.json` … (`section-a1.json`, `section-a2.json` for a section split in two because it has more than 8 new Items). Daily Set ids are `kpk1-lNN-a`, `kpk1-lNN-b` …
- Each section file has `section`: `letter` as in the book (none for chapter 0, whose parts are unlettered), `heading` in Polish as in the book, `meaning` in English, and `strands`, which of K, L and G it teaches. `AuthoredContentTest` and the Lesson screen read them.
- In `course.json` the chapter's entry keeps its `number`, `polishTitle`, English `title`, `accent` and `strands` (from the contents), gains `dailySets` and `lookUp` (the Item ids of the "Sprawdź w słowniku" box, introduced in the chapter), and loses `coming`.
- Mark the register of every phrase the book marks (`"register": "formal" | "informal" | "universal"` on the Item).
- Nothing is copied from the book but what ADR-0013 allows.

## Chapter 0: Dzień dobry!

Four unlettered parts, 18 Exercises each: *Polecenia* (the book's instructions: *posłuchać, powtórzyć, przeczytać, napisać, uzupełnić, przeliterować, przetłumaczyć, wybrać*), *W klasie* (*lekcja, dialog, tekst, test, ćwiczenie, polecenie, przykład, strona*), *W szkole* (*nauczyciel, nauczycielka, student, studentka, kolega, koleżanka, chłopak, dziewczyna*) and *Proszę, dziękuję, przepraszam* (*imię, nazwisko, narodowość*, with *proszę*, *dziękuję*, *przepraszam* introduced again). 27 new Items, 30 words in its Słownik.

It gives the later chapters:

- the verbs of the instructions, only as infinitives after *proszę* (*proszę posłuchać*), which is how every instruction in the app is worded (`content/kpk1/instructions.json`);
- the classroom nouns in the nominative singular, with their genders in `practice.json`;
- *proszę*, *dziękuję*, *przepraszam* and the greetings of Starter, with their register.

It leaves for later: every verb in a conjugated form, every case but the nominative, plurals and adjectives. Chapter 1 takes up greetings and introducing oneself, the alphabet and the numbers 0 to 10, as its contents list them.
