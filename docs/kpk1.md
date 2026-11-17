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

## Chapter 2: Cześć, skąd jesteś?

Book pages 18 to 27, recordings 029 to 046. Twelve parts, 193 Exercises and 58 new Items. Pages 20–21 and 26–27 are missing from the saved spreads; frames of them are in `teacher-course/pages-extra/`.

- **A** *Dane personalne* (18): *imię, nazwisko, adres, ulica, numer telefonu, mieszkanie*, *przez* in a house number (*dziesięć przez trzy*), and *jaki jest twój…?*.
- **B** *Co słychać?*, in two (15 and 13): the questions by register (*co u ciebie?*, *co u pana słychać?*, *co u pani słychać?*, *co słychać?*, *co nowego?*), with a table marked O, N and U, and the answers *wszystko dobrze, wszystko w porządku, po staremu, nic nowego* and *a u ciebie / pana / pani?*.
- **C** *Gdzie ona jest?* (18): *być* again with its table, *ono*, and *panowie, panie, państwo*, with a table of who is *oni* and who *one*.
- **D** *Skąd jesteś? Gdzie mieszkasz?*, in four: where from (17: *skąd jesteś? / pan jest? / pani jest?*, *z Polski, z Anglii, z Niemiec, z Hiszpanii, z Francji, z Włoch*, with a table of the countries); *mieszkać* with its table (15); *w Krakowie, w Warszawie, w Londynie, w Berlinie* (13); and the -m, -sz verbs with the table of *grać* (18: *gram, grasz, czytam, oglądam, kocham*, *w tenisa*, *serial*, *Kraków*).
- **E** *Liczebniki 11–29* (18): 11 to 20 again and 21 to 29 built from them, phone numbers in pairs.
- **F** *Dlaczego uczysz się polskiego?*, in two (14 and 15): *dlaczego* and *bo*; then *żeby* with a verb's basic form (*pracować, studiować w Polsce*), *hobby*, and an *Uwaga!* on *jego* and *jej*, which never change.
- *Ćwiczenia* (18): the book's kinds again over the whole chapter.

There are 35 listening tasks on all 18 recordings, and two Chats: *Your details* (Pani Anna at the school office, formal: how are things, where you are from and live, your phone number, why you learn Polish) and *Catching up* (Ewa on the phone, informal: how are things, where you are, tennis, why Polish). The chapter's Game is **Ankieta**: six people (Julia, Maria, Anna, Robert, Leon and Daniel) introduce themselves, heard or read, and the Learner fills in a form for each: name, where from, where they live, phone number, and why they learn Polish (*bo jej / jego…* or *żeby…*). A later chapter adds people, and fields where the book teaches more to ask.

It gives the later chapters:

- *mieszkać* and the -m, -sz pattern, which the Verb challenge can grow into;
- *w* with a city in the locative and *z* with a country in the genitive, each learnt as a phrase;
- 11 to 29, and *przez* after a house number, which the number rule in `AuthoredContentTest` now allows;
- *jego* and *jej*, and *chłopak*, *dziewczyna*, *adres* and *hobby* in the possessive test's lists.

It leaves for later: no other verb forms of the -m, -sz verbs than those listed, *uczyć się* only as the three phrases with *polskiego*, cities and countries only as the phrases above, no *pana* or *pani* as "your" (*jaki ma pan numer telefonu?* instead), and no counting with 21 to 29 (*dwadzieścia jeden lat* waits for its rule).

## Chapter 3: Mami, kto to jest?

Book pages 28 to 35, recordings 047 to 057. Ten parts, 173 Exercises and 61 new Items.

- **A** *Kto…? Co…?*, in three (18, 17 and 17): *kto to jest?* and *co to jest?*, and the classroom and the bag: *słownik, zeszyt, torba, plecak, komórka, ołówek, gumka, klucz, ładowarka, kubek, słuchawki, tablica, mapa, okno, stół, krzesło, laptop, paszport*, and *niestety*.
- **B** *Czy…?* (17): *czy to jest…?*, *tak, to jest…* and *nie, to nie jest…*, with a table; *pamiętam* and *tylko*.
- **C** *Mianownik*, in three: the three genders by their ending and *ten, ta, to* (18, with a table, *auto, muzeum, akwarium, słońce, segregator*, and *ten kolega*); *jaki? jaka? jakie?* and the adjective's endings *-y, -a, -e* (17, with a table: *nowy, stary, dobry, zły, duży, mały*); more adjectives (16: *praktyczny, fajny, modny, sympatyczny, polski, interesujący*, and *-i* after *k*).
- **D** *Kolory*, in two (18 and 17): eleven colours as adjectives, with a table, and *kolor*.
- *Ćwiczenia* (18): the book's kinds again over the whole chapter.

Each adjective is one Item with its feminine and neuter as `forms` (see [authoring.md](authoring.md)). There are 23 listening tasks on all 11 recordings (049 and 057 are word lists: the first and last word), and one Chat: *On the desk* (Tomek, informal: *co to jest?*, *czy to jest…?*, what your things are like and their colour). The new nouns are in Practice's `genders`, and 28 of them and the colours have pictures (the colours as hearts). The chapter's Games are **Label the picture** (a board of six pictures labelled one at a time, by tapping the word or typing it) and **Ten, ta, to** (nouns sorted under the word for "this", by Mój, moja, moje's rules).

It gives the later chapters:

- adjective agreement in the nominative singular, which later cases build on;
- *ten, ta, to*, and the classroom nouns with their genders for Mój, moja, moje;
- *plecak*, *słownik* and *torba* in the possessive test's lists.

It leaves for later: no plural but *słuchawki* (*to są słuchawki*), no adjective before a noun in a case other than the nominative, and *zły* only of things (said of a person it reads as "angry").

## Chapter 4: Jaki jesteś?

Book pages 36 to 41, recordings 058 to 063. Ten parts, 172 Exercises and 71 new Items. The saved spreads skip pages 40 and 41; a frame of them is in project files `teacher-course/pages-extra/lesson-04-pages-40-41.jpg`.

- **A** *Kto to jest? To nowy student*, in two (18 and 18): *mówić po…* with six languages (*po francusku, po hiszpańsku, po niemiecku, po włosku, po japońsku, po ukraińsku*) and *z Japonii*, *z Ukrainy*; then *myślę, że…*, *czy wiesz, kto to jest?*, *ciekawy*, *chyba*, *żart*, *słowo* and *masz rację*.
- **B** *Koniugacja -ę, -isz/-ysz*, in three (18, 17 and 18), each with two verb tables: *chodzić* and *myśleć* (*-ę, -isz*); *uczyć się* and *kończyć* (*-ę, -ysz* after *cz*, *sz*, *rz*, *ż*); *słyszeć* and *robić* (the extra *i* after *b, p, w, f, m, n*, with *lubić*). Also *do szkoły*, *kurs*, *film*, *sport* and *cieszę się*.
- **C** *Jaki on jest?*, in four: describing a person (17, with a table: *smutny, wesoły, wysoki, niski, przystojny, średniego wzrostu, wysportowany, młody*); opposites (17: *pracowity, leniwy, gruby, szczupły, ładny, brzydki, zdrowy, chory*); *czysty, brudny, zmęczony* and the international adjectives (17: *aktywny, energiczny, inteligentny, romantyczny, kreatywny*); giving an opinion (16, a Zapamiętaj callout: *myślę, że tak*, *nie wiem, czy…*, *jestem pewny, że…*, *na pewno*, *miły*, *dziś*, *atrakcyjny, spontaniczny, ambitny, zestresowany*).
- *Ćwiczenia* (16): the book's kinds again, with *i*, *ale* and *bo* and the opposites.

Only the verb forms the Exercises use are Items; a table's other forms are shown in its Tip and asked in the Verb challenge. *Uczyć się* is the first verb with *się* in a table: the Polish check conjugates it without *się* and puts *się* after each form. There are 20 listening tasks on all 6 recordings (061 and 062 are word lists: the first and last word), and one Chat: *The new student* (Kasia, informal: who the new student is, where he is from and what he is like, and whether you like sport). The new nouns are in Practice's `genders`, and 16 Items have pictures (faces for the moods, a sloth for *leniwy*, a bee for *pracowity*). The chapter's Game is **Opposites snap** (two adjectives in the same gender, against the clock: opposites or not?), over the twelve pairs in Practice's `opposites`, chapter 3's *nowy, stary* among them; and the Verb challenge can now be played over a conjugation, such as *-ę, -isz/-ysz*, once two of its verbs are learned.

It gives the later chapters:

- the *-ę, -isz/-ysz* conjugation and six verb tables for the Verb challenge;
- describing a person with adjectives in all three genders, and *myślę, że…* for an opinion;
- *kolega* and *koleżanka* in the possessive test's lists.

It leaves for later: no adjective in the plural (*oni są zmęczeni*), no *lubić* table of its own (its Tip is *robić*'s), and the languages only after *mówić po…* (*znam polski* and *uczę się polskiego* wait).
