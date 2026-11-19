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
- **Games**: a chapter's own Game sets `chapter` in `Game` to the chapter's number, so the chapter's Gry lists it ([practice.md](practice.md)). Its Chat needs nothing: a conversation is listed on the first chapter that teaches all its Items.
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

## Chapter 5: Jesteś instruktorem tanga?

Book pages 42 to 49, recordings 064 to 070. Fourteen parts, 244 Exercises and 83 new Items, and 13 Starter Items introduced again (*ile*, *ma*, *masz*, *rok*, *lata*, *lat*, *mąż*, *rodzina*, *syn* and the four nationalities in the instrumental).

- **A** *Kto to jest? Jaki on jest?*, in two (18 and 17): presenting someone (*Adam to biznesmen*, *lubi pracować*) and asking an age (*ile masz lat?*); then *rok*, *prawie*, *pół roku*, *co lubisz robić?* and *tańczyć tango*.
- **B** *Liczebniki 20–100*, in two (16 and 18): the tens to *sto*, sums with *razy*, and the Tip on *lata* after 2, 3 and 4 (*dwadzieścia dwa lata*) and *lat* after everything else, 12 to 14 and 21 among them.
- **C** *Zawody*, in four: jobs for a man (17 and 18: *lekarz, aktor, muzyk, kelner, kucharz, informatyk, policjant*, then *rolnik, dziennikarz, urzędnik, fotograf, tancerz, dentysta, inżynier, emeryt*); for a woman with *+ ka* (18, a table of the changes: *lekarz, lekarka*; *urzędnik, urzędniczka*; *dentysta, dentystka*); and *pracować* with five places (18: *w biurze, w szpitalu, w teatrze, w restauracji, w szkole*).
- **D** *Kim ona jest?*, in five: *kim jesteś z zawodu?* and the instrumental of a job (18); an adjective with its noun after *jest* (17, a table: *dobrym lekarzem, dobrą lekarką, dobrym dzieckiem*, and *on jest dobry* against *on jest dobrym lekarzem*); the nationalities (18: *Polakiem, Polką, Anglikiem, Angielką, Niemcem, Niemką, Japończykiem, Japonką*); *interesować się* with what in the instrumental (17); *czytasz?*, *pisarz*, *malarz* and five more interests (17).
- *Ćwiczenia* (17): sums written in words, who works where, how old people are, and who is who.

Each job is one Item with its instrumental as `forms` (*lekarz*, *lekarzem*), so word coverage takes both; an adjective before a noun in the instrumental is an Item of its own (*dobrym*, with *dobrą* as its form), as are the interests (*muzyką*, *sportem*). The numbers tests now know the tens: a number before *rok*, *lata* or *lat* is read with the ten before it (*dwadzieścia jeden lat*), and *razy* may follow a number. There are 23 listening tasks on all 7 recordings (065 and 069 are word lists: the first and last word; of 068 only the first number is clear enough to ask), and one Chat: *At the tango class* (Ola, informal: dancing, your job and where you work, your age, your interests). The new nouns are in Practice's `genders`; 23 Items have pictures (a man or a woman at each job where Fluent Emoji has one) and the tens are glyphs. The chapter's Game is **Who's who** (a job by its picture or its word: finish *On jest …* with its instrumental, among the word alone and other jobs), over the 28 jobs in Practice's `jobs`; and the Number pad has a level from 20 to 100, each number a ten and a unit said one after the other.

It gives the later chapters:

- numbers to 100 and *rok, lata, lat* after any of them;
- the instrumental after *być* for jobs, nationalities and an adjective with its noun, and after *interesować się*;
- *pracować* and *interesować się*, two verbs of the *-ę, -esz* conjugation (*pracuję, pracujesz*) for the Verb challenge.

It leaves for later: no plural in the instrumental (*interesuję się książkami*), *umieć* only as *umiesz*, and of *grać na…* only *na gitarze*.

## Chapter 6: Co robisz? Nudzę się!

Book pages 50 to 57 (pages 50 and 51 are missing from the spreads; a frame of them is `teacher-course/pages-extra/lesson-06-pages-50-51.jpg` in the project files), recordings 071 to 080 (079 and 080 are the same recording). Seventeen parts, 283 Exercises and 116 new Items, and 6 Starter Items introduced again (*pani*, *pan*, *tata*, *córka*, *siostra*, *twoje*).

- **A** *W domu: co robisz?*, in four (16, 17, 16 and 18): *co pani robi?*, cooking (*gotuję pierogi*, *umiem gotować tylko ryż*); reading and preferring (*czytam książkę*, *wolę grać na komputerze*); *co pan robi?*, *szukam informacji*, *piszę e-maile*, *ogląda zdjęcia*; and being bored (*nudzę się*, *nic nie robię*, *na wakacjach*, *na uniwersytecie*, *w Japonii*).
- **A** *Mój, twój* (16, a three-gender table), with *gospodarz*, *gospodyni* and *w domu*.
- **A** *Koniugacje*, in four, each with its verb tables for the Verb challenge: *-m, -sz* (15: *znać*, *umieć*); *-ę, -esz* (18: *pisać*, and *gotować* as the model of every *-ować* verb); more *-ować* verbs (16: *organizuję imprezę*, *planuję*, *rezerwuję bilety*, *trenuję w klubie*, *podróżować*, with *organizować*'s table); *-ę, -isz / -ysz* (17: *tańczyć* and *nudzić się*, with *robimy*, *robicie*, *uczycie się*).
- **B** *Co lubicie robić?*, in two (15 and 14): *lubicie*, *lubią*, *słuchać muzyki*, *malować*, *chodzić do klubu*; then *spotykać się ze znajomymi*, *robić zakupy*, *w weekend idziemy*.
- **B** *Hobby*, in three: *chodzić na spacery, na siłownię, do kina*, *oglądać telewizję*, *śpiewać*, *biegać*, *pływać* (17); *grać w piłkę, w szachy, w karty* against *grać na pianinie*, and *jeździć na rowerze, na nartach, konno* (18); *jeździć*'s table (*jeżdżę, jeżdżą*), *na rolkach*, *uprawiać sport*, *na basen* (18).
- **B** *Chcę, ale nie mogę*, in two: *chcieć*, *móc* and *musieć* with a verb's dictionary form, *iść do lekarza*, *mam kontuzję* (17, tables of *chcieć* and *móc*); and going out (17, *musieć*'s table: *na kawę*, *na zakupy*, *na koncert*, *uczyć się do egzaminu*).
- *Ćwiczenia* (18): dictionary forms (*idę* from *iść*, *mogę* from *móc*), *grać na / w*, *chodzić na / do*, and *na masaż*.

Verb forms are Items of their own (*gotuję*, *gotuje*, *gotujesz*); what follows a verb in another case is too (*książkę*, *informacji*, *muzyki*, *telewizję*, *imprezę*, *kontuzję*), as is each "where" or "what" after *grać*, *jeździć*, *chodzić* and *iść* (*w piłkę*, *na rowerze*, *na siłownię*, *do kina*, *na kawę*). There are 33 listening tasks on all 10 recordings (077 is a word list: the first and the last hobby), and one Chat: *Plans for the weekend* (Ola, informal: being bored, what you like doing, *umiem* against *mogę*, *chcę, ale muszę…*). The new nouns are in Practice's `genders`; 33 Items have pictures (the hobbies, sports and places). The chapter's Games are **Three groups** (a verb learned with its table: sort it by how *ja* and *ty* end, among the conjugations learned so far) and **Hobby charades** (a hobby by its picture: name it, choosing or typing, and it is said with *Lubię …*), over the 21 hobbies in Practice's `hobbies`.

It gives the later chapters:

- the three conjugations by their *ja* and *ty* forms, with 13 new verb tables;
- *chcieć*, *móc*, *musieć* and *umieć* with a verb's dictionary form;
- *mój* and *twój* in all three genders.

It leaves for later: the accusative as a rule (chapter 7 teaches it; here *książkę*, *imprezę* and *telewizję* are learned one by one), the genitive after *nie* (*nie oglądam telewizji*), and the plural of nouns after numbers (*pięć zdjęć*).

## Chapter 7: Małe zakupy

Book pages 58 to 65 (pages 58 and 59 are missing from the spreads; a frame of them is `teacher-course/pages-extra/lesson-07-pages-58-59.jpg` in the project files), recordings 081 to 092. Fifteen parts, 269 Exercises and 111 new Items.

- **A** *Biernik*, in four: free or busy (18: *masz wolny czas?*, *jestem zajęta*, *galeria handlowa*, *lista zakupów*, *krótki*, *długi*, *jutro*); the accusative of a feminine, a neuter and a masculine thing (18, a table: *mam długą listę*, *lubię zieloną herbatę*, *gotuję zupę*, *oglądam polską telewizję*, *mam sportowe auto*); of a man or an animal (18, a table and an *Uwaga!* on *mojego*, *twojego*: *mam inteligentnego brata*, *mam dobrego tatę*, *kogo znasz?*, *mam kota*); and verbs with it (18, *kupować*'s table: *czytam gazetę*, *piszę list*, *lubię kawę*, *robię pizzę*, *jaką herbatę lubisz?*).
- **B** *Kiosk*, in four: asking for things (18, an *Uwaga!* on *czy jest…? / czy są…? / nie, nie ma*: *proszę kanapkę*, *sok*, *wodę*, *chipsy*, *woda mineralna*); prices (18, a table of *złoty, złote, złotych* and *grosz, grosze, groszy*: *ile kosztuje?*, *ile kosztują?*, *kanapka z serem*, *proszę dużą kanapkę*, *małą wodę*); paying (18, *płacić*'s table: *płacę kartą*, *gotówką*, *płaci pan…?*, *coś jeszcze?*, *to wszystko*, *ciastko*); and a bus ticket (18: *bilet normalny*, *ulgowy*, *na autobus*, *drobne*, *monety*, *banknoty*, *kupić*, *magazyn*).
- **B** *Liczebniki 100–1000*, in two: the hundreds (18: *dwieście* to *dziewięćset*, and long numbers such as *sześćset siedemdziesiąt osiem*); *tysiąc* and the lottery (18: *loteria*, *kupon*, *wygrać*, *kiedy*, *promocja*, *albo*).
- **C** *Galeria handlowa*, in four: the shops (17: *sklep*, *apteka*, *piekarnia*, *drogeria*, *kwiaciarnia*, *cukiernia*, *optyk*, *fryzjer*); where to buy (18: *supermarket*, *kawiarnia*, *bank*, *kosmetyki*, *szampon*, *dezodorant*, *w drogerii*, *w supermarkecie*, *gdzie mogę kupić…?*); a camera and its price (18: *aparat*, *karta pamięci*, *album*, *tani*, *drogi*, *kolorowy*, *bogaty*, *biedny*); and the camera shop and money (18: *sklep samoobsługowy*, *kasa*, *jaki pan poleca?*, *biorę ten*, *pieniądze*, *bankomat*, *kantor*).
- *Ćwiczenia* (18): the accusative with *jaki*, *jakiego*, *jaką*; *złote* or *groszy* after a number; which shop sells what; a dialogue at the kiosk.

As in chapters 5 and 6, a case form is an Item of its own: each feminine noun in the accusative (*listę*, *herbatę*, *gazetę*, *kanapkę*, *wodę*), *brata*, *tatę*, *kota*, and each adjective in the accusative, by its form for a man with the feminine one as its form (*dobrego*, *dobrą*; *mojego*, *moją*; *jakiego*, *jaką*). *Złoty* and *grosz* carry their forms after numbers (*złote*, *złotych*; *grosze*, *groszy*), and the content tests check every number before them. There are 33 listening tasks on all 12 recordings (085 to 087 and 092 are numbers: the lottery numbers, the hundreds and long numbers; the questions ask one number each), and one Chat: *At the kiosk* (Pan Marek, formal: a newspaper, a big sandwich, a small water, paying by card when the kiosk only takes cash). The new nouns are in Practice's `genders`; 29 Items have pictures (the shops, the things at the kiosk, money) and the hundreds are glyphs. The chapter's Games are **Kiosk till** (a price said or written: pay it exactly with coins and notes, over Practice's `till`) and **Lotto** (numbers from 100 to 1000 called: tick them on a card). Number pad stays within a hundred.

It gives the later chapters:

- the accusative singular of nouns and adjectives, with *kogo? co?* and *jakiego? jaki? jaką? jakie?*;
- prices, *ile kosztuje?*, and *złoty*, *grosz* after a number;
- the numbers to 1000.

It leaves for later: the genitive after *nie ma* (*nie ma kantoru*, *nie mam pieniędzy* are learned as phrases), the locative of shops (*w drogerii*, *w supermarkecie* are Items), and the accusative plural.

## Chapter 8: Mami, jesteś głodna?

Book pages 66 to 73 (pages 72 and 73 are missing from the spreads; a frame of them is `teacher-course/pages-extra/lesson-08-pages-72-73.jpg` in the project files), recordings 093 to 102. Seventeen parts, 288 Exercises and 128 new Items.

- **A** *Śniadanie*, in ten: Saturday morning (16, *spać*'s table and a Tip on *już* and *jeszcze*: *głodny*, *śniadanie*, *rano*, *śpię*, *śpisz*, *śpią*); bread and milk (18: *chleb*, *jasny*, *ciemny*, *bułka*, *bułkę*, *ser*, *masło*, *mleko*); more for breakfast (17: *jogurt*, *dżem*, *miód*, *jajko*, *szynka*, *pomidor*, *ogórek*, *płatki śniadaniowe*); *woleć* (18, its table: *wolisz*, *niż*, *kakao*, *cukier*, *sól*, *pieprz*, *cytryna*, *sok pomarańczowy*); *jeść* and *pić* (18, both tables: *jem*, *jesz*, *je*, *piję*, *pijesz*, *pije*); *z czym?* (17, a table of the instrumental after *z* and a *Zapamiętaj!* of *z cukrem ≠ bez cukru*: *z masłem*, *z dżemem*, *z szynką*, *z cytryną*, *z mlekiem*, *z cukrem*, *bez cukru*, *bez mleka*); the table (17: *talerz*, *nóż*, *widelec*, *łyżka*, *łyżeczka*, *wędlina*, *kiełbasa*, *na przykład*); eggs (17: *jajecznica*, *omlet*, *mam ochotę na*, *chętnie*, *na miękko*, *na twardo*, *późno*); fruit (16: *owoc*, *banan*, *jabłko*, *pomarańcza*, *truskawka*, *malina*, *gorący*, *zimny*); and adjectives made from food (16, a table: *naturalny*, *owocowy*, *truskawkowy*, *malinowy*, *jabłkowy*, *ciasto*, *mocny*, *słaby*).
- **B** *Obiad*, in four: meals (17: *obiad*, *na śniadanie*, *na obiad*, *zupa*, *pierwsze danie*, *drugie danie*, *kurczak*, *ziemniaki*); the main course (16: *mięso*, *ryba*, *kotlet*, *sos*, *kasza*, *surówka*, *tłusty*, *chudy*); a favourite dish (17: *sałata*, *sałatka*, *frytki*, *warzywa*, *ulubiony*, *potrawa*, *owoce morza*, *jedzenie*); and pizza with the instrumental plural (16, a table of *-ami*: *oliwki*, *grzyby*, *pieczarki*, *brokuły*, *z oliwkami*, *z pieczarkami*, *z frytkami*, *z warzywami*).
- **C** *Kolacja*, in two: supper at the neighbours' (18, *zapraszać*'s table: *kolacja*, *na kolację*, *zapraszamy*, *sałatka jarzynowa*, *tost*, *grzanka*, *pyszny*, *smaczny*); and a snack bar (18, *zamawiać*'s table: *bar*, *zamawiam*, *zamawiają*, *wino*, *wytrawny*, *słodki*, *wieczorem*, *po południu*).
- *Ćwiczenia* (16): the accusative or the instrumental (*jem chleb z masłem*), *jeść* and *pić*, opposites, the meals of the day.

The phrases with *z* and *bez* are Items of their own (*z masłem*, *z cytryną*, *bez cukru*), as the case forms were in chapter 7; *mam ochotę na* carries *masz ochotę na* and *ma ochotę na* as its forms. There are 42 listening tasks on all 10 recordings (094 and 098 are word lists), and one Chat: *Breakfast with a friend* (Karolina, informal: brown bread with butter and ham, tea with lemon and no sugar, a hard-boiled egg). The new nouns are in Practice's `genders`, *jasny / ciemny*, *mocny / słaby*, *tłusty / chudy* and *gorący / zimny* are in `opposites`, and 32 Items have pictures (the food, the drinks, a knife and a spoon). Its Game is **Plate builder**: a dish is ordered, *chleb z masłem*, or two, *i herbata z cytryną*, and the Learner puts its foods on the plate; the dishes are under `plates` in `practice.json`.

It gives the later chapters:

- *jeść*, *pić*, *woleć* and *spać*, and *mam ochotę na* with the accusative;
- the instrumental after *z*, singular and plural, and *bez* as phrases;
- the meals, *na śniadanie / obiad / kolację*, and the times of day *rano*, *po południu*, *wieczorem*.

It leaves for later: the genitive after *bez* as a rule (*bez cukru* and *bez mleka* are Items), the accusative of masculine animate food (*jem kurczaka*), and adjectives in the instrumental (*z czarnymi oliwkami* is only in a Tip).

## Chapter 9: Lubisz marchewkę?

Book pages 74 to 79 (pages 78 and 79 are missing from the spreads; a frame of them is `teacher-course/pages-extra/lesson-09-pages-78-79.jpg` in the project files), recordings 103 to 111. Seventeen parts, 282 Exercises and 112 new Items.

- **A** *Owoce i warzywa*, in six: fruit (15: *gruszka*, *śliwka*, *brzoskwinia*, *kiwi*, *winogrona*, *wiśnia*, *ananas*, *arbuz*); vegetables (16: *marchewka*, *marchewkę*, *cebula*, *czosnek*, *papryka*, *kapusta*, *kalafior*, *burak*); more vegetables (16: *por*, *seler*, *pietruszka*, *rzodkiewka*, *fasola*, *groszek*, *kukurydza*, *dynia*); salads, with a Tip on *ze* (16: *śmietana*, *oliwa*, *orzechy*, *rodzynki*, *ze śmietaną*, *z orzechami*, *z rodzynkami*, *jagody*); taste, with the *Zapamiętaj!* of *słodki jak cukier* (15: *smak*, *kwaśny*, *słony*, *gorzki*, *ostry*, *delikatny*); and shape (15: *kształt*, *okrągły*, *owalny*, *podłużny*, *miękki*, *twardy*).
- **B** *Na targu*, in four: buying (17, *kupować*'s table: *targ*, *na targu*, *kupujemy*, *kupujesz*, *kupuje*, *zupa jarzynowa*, *jarzyna*, *spaghetti*); how much (17, a Tip on *dwa* and *dwie*: *poproszę*, *kilo*, *pół kilo*, *dwie*, *produkt*, *do picia*, *piwo*, *w takim razie*); fresh and ripe (18, a Tip on *te*: *dojrzały*, *świeży*, *suszony*, *bazylia*, *w doniczce*, *mniej*, *sezon*, *te*); and parsley at the stall (17: *natka*, *korzeń*, *kilogram*, *sprzedawca*, *sprzedawczyni*, *jak to się nazywa?*).
- **C** *Liczba mnoga*, in two: the nominative and accusative plural of things (17, a table of *-y*, *-i*, *-a*: *liczba mnoga*, *to są*, *jedna*, *jedno*); and adjectives in the plural, with the shopping list (17, a table of *-e*: *średni*).
- **D** *Gotowanie* and the supermarket, in four: ways of cooking (17: *gotowany*, *pieczony*, *duszony*, *smażony*, *wędzony*, *surowy*, *patelnia*); the kitchen (17, *uwielbiać*'s table and *zdrowszy … niż*: *uwielbiam*, *pachnie*, *pięknie*, *przyprawy*, *zdrowszy*, *zupa rybna*, *najpierw*, *potem*); the aisles (17: *nabiał*, *pieczywo*, *mrożonki*, *słodycze*, *napoje*, *alkohole*, *czekolada*, *cukierki*); and fish and more (18: *drożdżówka*, *pstrąg*, *dorsz*, *śledź*, *łosoś*, *produkty zbożowe*).
- *Ćwiczenia* (17): soups and juices named by adjectives (*grzybowy*, *cebulowy*, *kalafiorowy*, *zwykle*), the plural after numbers, *kupować*.

A noun's plural is its form (*gruszka*, *gruszki*), so the Items of chapter 8 that this chapter puts in the plural gained theirs (*banany*, *jabłka*, *pomarańcze*, *truskawki*, *maliny*, *cytryny*, *pomidory*, *ogórki*, *ryby*). There are 37 listening tasks on all 9 recordings (103 is a word list; 107 to 109 repeat 104 to 106 to fill in), and one Chat: *At the market* (Pani Ewa, formal: four tomatoes, two yellow peppers, fresh basil in a pot, ripe bananas). The new nouns are in Practice's `genders`, *miękki / twardy*, *świeży / suszony* and *surowy / gotowany* are in `opposites`, and 29 Items have pictures (the fruit and vegetables, the aisles, a pan and scales). Its Games are **Market list**: a shopping list is said, *dwie gruszki i trzy banany*, and the Learner fills the basket from the stall (the numbers and goods are under `market` in `practice.json`); and **Sort the aisles**: products are sorted into the supermarket's aisles against the clock (under `aisles`).

It gives the later chapters:

- the nominative and accusative plural of things, and adjectives in the plural in *-e*;
- *dwa / dwie*, *jeden / jedna / jedno*, *kilo* and *pół kilo*, *poproszę*;
- *kupować* and *uwielbiać*, and the cooked adjectives (*gotowany*, *pieczony*, *smażony*).

It leaves for later: the genitive plural after five and after *kilo* (*pięć jabłek*, *kilo pomidorów*, only in the Tip and the recordings), the plural of people (*oni*, *studenci*), and the comparative as a rule (*zdrowszy* is an Item).

## Chapter 10: Uwielbiam polskie jedzenie!

Book pages 80 to 85, recordings 112 to 120. Fourteen parts, 239 Exercises and 110 new Items.

- **A** *Zapraszam was na obiad*, in seven: a booking (17: *zapraszam*, *urodziny*, *rezerwacja*, *mam rezerwację*, *stolik*, *dla czterech osób*, *menu*, *zaraz*); soups (16: *barszcz*, *z uszkami*, *żurek*, *z jajkiem*, *rosół*, *z makaronem*, *krupnik*, *zupa pomidorowa*); main dishes (16: *danie*, *kotlet schabowy*, *kotlet mielony*, *bigos*, *gołąbki*, *pierogi ruskie*, *naleśniki*, *placki ziemniaczane*); meat (17: *wieprzowina*, *wołowina*, *cielęcina*, *drób*, *kaczka*, *indyk*, *z grilla*, *wegetariański*); ordering, with a Tip on *dla mnie* and *proszę* (17: *co podać?*, *polecam*, *dla mnie*, *to samo*, *kompot*, *danie główne*, *rachunek*); opinions, with *smakuje mi* and *za* (17: *smakuje ci?*, *smakuje mi*, *nie smakuje mi*, *niezły*, *okropny*, *niedobry*, *bez smaku*, *za*); and the *Zapamiętaj!* of opposites with the café's signs (18: *ciepły*, *chłodny*, *łagodny*, *czynny*, *nieczynny*, *zamknięty*, *danie dnia*, *napiwek*).
- **B** *A może rurka z kremem?*, in four: something sweet (18: *coś słodkiego*, *deser*, *rurka z kremem*, *gofr*, *lody*, *sernik*, *szarlotka*, *makowiec*); the genitive after *nie*, feminine and neuter, with a table of *biernik* and *dopełniacz* and *ani… ani…* (17: *dopełniacz*, *kawy*, *herbaty*, *wody*, *mleka*, *wina*, *ani*, *to dobrze brzmi*); masculine things and adjectives in *-ego*, *-ej* (17: *soku*, *kompotu*, *chleba*, *sera*, *zimnego*, *czerwonego*, *białego*); and the café's menu (17: *koktajl*, *z bitą śmietaną*, *z lodami*, *na ciepło*, *kawałek sernika*, *herbata owocowa*, *czarna kawa*, *gorąca czekolada*).
- **C** *Nareszcie weekend!*, in two: *ze mną*, *z tobą*, *z nim*, *z nią*, with the table of pronouns after *z* (16: *nareszcie*, *idziesz*, *do pubu*, *z kim?*); and *z nami*, *z wami*, *z nimi* (18: *idziecie*, *wszędzie*, *co z nim?*, *czekam*, *razem*).
- *Ćwiczenia* (18): the *zapiekanka* stall (*z podwójnym serem*, *z boczkiem*, *ze szpinakiem*, *z kurczakiem*, *keczup*, *majonez*, *sos czosnkowy*), *zamawiać*.

A noun after *nie* is its own Item (*kawy*, *soku*), as the accusative is (*kawę*); an adjective after *nie* is one Item with its feminine as its form (*zimnego*, *zimnej*). *Razy* now also means portions (*dwa razy kompot*). There are 39 listening tasks on all 9 recordings (114 repeats 113), and one Chat: *At the restaurant* (the waitress, formal: the soup she recommends, a pork chop with chips, a compote, no dessert, the bill). Eight Items have pictures (pancakes, duck, turkey, ice cream, a waffle, a birthday cake, a dessert, the bill). Its Games are **Take the order**: a table of two or three guests orders, *Dla mnie żurek. A dla mnie to samo.*, and the Learner, the waiter, writes each guest's dish on the pad (the words and dishes are under `restaurant` in `practice.json`); and **Negation flip**: a sentence, *Lubię kawę.*, is negated against the clock, *Nie lubię kawy.* or *Nie lubię kawę.* (under `negations`).

It gives the later chapters:

- the genitive singular after *nie* (*nie lubię kawy*, *nie mam soku*), with adjectives in *-ego* and *-ej*;
- the pronouns after *z* (*ze mną … z nimi*), *dla mnie*, *to samo*;
- the menu: soups, main dishes, meat, desserts and the café's drinks; *smakuje mi*, *za* before an adjective.

It leaves for later: the genitive after *kawałek* as a rule (*kawałek sernika* is an Item), the genitive of people (*nie znam tego profesora*), and the plural genitive (*nie mam pieniędzy* stays an Item).

## Chapter 11: Rodzina

Book pages 86 to 93, recordings 121 to 131. Thirteen parts, 230 Exercises and 94 new Items.

- **A** *Czyja to rodzina?*, in five: the family (18: *matka*, *ojciec*, *rodzice*, *dziadkowie*, *wnuk*, *wnuczka*, *wnuki*, *małżeństwo*); the *Zapamiętaj!* of *panna*, *kawaler*, *mężatka*, *żonaty*, *rozwiedziony* (18: with *kawalerem*, *nie żyje*, *drugi raz*); the wider family (18: *ciocia*, *wujek*, *kuzyn*, *kuzynka*, *teść*, *teściowa*, *synowa*, *bliźniaki*); people in the accusative, with a Tip on *kogo? co?* (18: *rodziców*, *męża*, *żonę*, *ciocię*, *wujka*, *młodszy*, *starszy*, *dwoje*); and talking about a family (18: *drzewo genealogiczne*, *lub*, *czyli*, *rozmawiam*, *spotykamy się*, *stuprocentowy*, *co powiedzieć*, *moi*).
- **B** *Czyj? Czyja? Czyje?*, in three: *czyj*, *nasz* and the table of possessives (17: *czyj*, *czyja*, *czyje*, *nasz*, *nasza*, *nasze*, *portfel*); *wasz* and *ich* (17: *wasz*, *wasza*, *wasze*, *ich*, *to musi być*); and who someone is to someone, in the instrumental, with a Tip on *kim jest?* (17: *dziadkiem*, *babcią*, *ojcem*, *matką*, *mężem*, *żoną*, *synem*, *córką*).
- **C** *Rano czy wieczorem?*, in two: the times of day (18: *pory dnia*, *przed południem*, *w południe*, *o północy*, *w nocy*, *w dzień*, *doba*); and a family's day, with a Tip on *wracać* (18: *wracam*, *do domu*, *odpocząć*, *sjesta*, *przerwa*, *później*, *zdalnie*, *wszyscy*).
- **D** *Jak często?*, in two: the scale from *zawsze* to *nigdy* (17: *jak często?*, *zawsze*, *często*, *czasem*, *rzadko*, *nigdy*, *codziennie*, *od czasu do czasu*); and Uwe the workaholic (18: *pracoholik*, *od rana do wieczora*, *dzwonisz*, *urlop*, *na dyskotekę*, *raz w roku*, *w tygodniu*, *samochodem*).
- *Ćwiczenia* (18): the pairs and the corrections of *Powtórzenie*, the quiz *czy to prawda, że…?*, *prawie nigdy*, *taksówką*.

A person in the accusative or the instrumental is its own Item (*męża*, *mężem*), as a thing after *nie* is. *Zwykle*, *rodzeństwo*, *rozwiedziony* and the other words the book sends to the dictionary are under Look up. There are 44 listening tasks on all 11 recordings (131 repeats 130), and one Chat: *A family photo* (a friend, informal: whose photo it is, your sister and her husband, their daughter, how often you meet, your grandparents, and that you are a bachelor). Nine Items have pictures (a sunrise, a city at dusk, a night sky, twelve o'clock, a family tree, a ring, an island, an older person, two people for the family). Its Games are **Family tree**: the Learner's family on a tree, where each relative is named, *mój wujek*, *moja ciocia*, or found (the people are under `family` in `practice.json`); and **Times of day**: a time on the clock, 7:30, and when in the day it is, *rano*, against the clock (under `timesOfDay`).

It gives the later chapters:

- the possessives in each gender (*mój … ich*) and *czyj?*, *czyja?*, *czyje?*;
- the family, its people in the accusative and the instrumental;
- the times of day and the adverbs of frequency, from *zawsze* to *nigdy*.

It leaves for later: the plural of the possessives with men (*moi* is an Item), the genitive of people (*siostra mojego taty* is heard, not taught), and the past tense of *żyć*.

## Chapter 12: Co robisz w poniedziałek o ósmej?

Book pages 94 to 101 (pages 96 and 97 are missing from the spreads; a frame of them is `teacher-course/pages-extra/lesson-12-pages-96-97.jpg` in the project files), recordings 132 to 143. Seventeen parts, 296 Exercises and 130 new Items.

- **A** *Macie ochotę pójść na spektakl?*, in five: a show tonight (17: *spektakl*, *zaproszenie*, *pójść*, *masz ochotę*, *randka*, *teatr*, *to nie wasza sprawa*, *komedia romantyczna*); inviting, from the box *Umawiamy się* (17: *zapraszam cię*, *na film*, *może pójdziemy…?*, *na spacer*, *co powiesz na…?*, *do parku*, *czy chciałby pan…?*, *umawiamy się*); saying yes (17: *dziękuję za zaproszenie*, *z przyjemnością*, *świetny*, *pomysł*, *jasne!*, *dlaczego nie?*, *na mecz*); saying no, with a Tip on *dokąd?* and *na co?* (18: *przykro mi*, *nie mam czasu*, *nie dam rady*, *dokąd?*, *do teatru*, *do kawiarni*, *na przedstawienie*, *na co?*); and making a plan (17: *potańczyć*, *nie mam planów*, *kto jeszcze?*, *coś innego*, *gdzie się spotkamy?*, *na rynku*, *pod pomnikiem*, *bez dyskusji*).
- **B** *Która godzina? O której godzinie?*, in five: the hours one to six (17: *godzina*, *która jest godzina?*, *pierwsza* … *szósta*); seven to twelve and *o której?* (17: *siódma* … *dwunasta*, *o*, *o której?*); the 24-hour clock (18: *trzynasta* … *dwudziesta*); minutes, with Tips on *po*, *za*, *wpół do* and the formal way (18: *kwadrans*, *po*, *za*, *wpół do*, *minuta*, *około*, *zegarek*, *budzik*); and *od której do której?* (17: *od*, *do*, *zajęcia*, *trwa*, *wizyta*, *u dentysty*, *szef*).
- **C** *W poniedziałek czy we wtorek?*, in two: the days of the week, with a Tip on *w* and *we* (17: *poniedziałek* … *niedziela*, *dni tygodnia*); and a week's diary (18: *tydzień*, *spotkanie*, *impreza*, *u babci*, *zaczyna się*, *spotyka się*, *do dentysty*, *w galerii*).
- **D** *Rutyna dnia*, in four: my morning (17: *budzę się*, *wstaję*, *prysznic*, *myję*, *zęby*, *ubieram się*, *wychodzę*, *z domu*); a friend's day, with *brać* (18: *budzisz się*, *wstajesz*, *bierzesz*, *myjesz*, *ubierasz się*, *wychodzisz*, *sprzątam*, *spać*); the verbs, with *wstawać* (18: *budzić się*, *wstawać*, *brać*, *myć się*, *ubierać się*, *wcześnie*, *odpoczywam*, *spotykam się*); and a working day (17: *zaczynam*, *kończę*, *pracę*, *sprawdzam*, *coś*, *w telefonie*, *otwieram*, *serwis informacyjny*).
- *Ćwiczenia* (17): invitations and answers, times said both ways, the verbs with what goes with them (*wstawać wcześnie*, *jechać na wycieczkę*), *jechać*, *na lunch*.

An hour is a feminine ordinal, one Item with its form after *o*, *od*, *do* and *około* (*ósma*, *ósmej*); the hours from 21 to 24 are *dwudziesta* with *pierwsza* to *czwarta*. *O*, *po*, *za*, *od* and *do* are Items only in telling the time, and a day carries its *w* or *we* as its form (*środa*, *w środę*). *Zaproszenie*, *umówić się* (as *umawiamy się*), *godzina* and *kwadrans*, the words the book sends to the dictionary, are under Look up. There are 58 listening tasks on all 12 recordings (133 repeats 132; for 136, 138 and 143 the speaker's times are asked letter by letter), and one Chat: *An invitation* (a friend, informal: a concert at the weekend, Friday you cannot, Saturday at eight, meeting by the monument at half past seven). Nine Items have pictures (an alarm clock, a party popper, a calendar, a football, a tooth, the theatre masks, a shower, a watch, a bed). Its Games are **Tell the time**: a clock face and its time in Polish, *pięć po ósmej*, *wpół do dziewiątej*, said or found (the hours and words are under `clock` in `practice.json`); and **Week planner**: a week's plans, *W piątek o dwudziestej: impreza.*, heard or read and put in the diary at their day and time (under `week`).

It gives the later chapters:

- the time: *która jest godzina?*, *o której?*, the hours 1 to 24 both ways, *kwadrans*, *wpół do*, *po*, *za*, *od… do…*, *około*;
- the days of the week with *w* and *we*;
- inviting, accepting and refusing, *dokąd?* with *do* and *na co?* with *na*;
- the daily routine in *ja* and *ty*, with *wstawać* and *brać*.

It leaves for later: the ordinals as adjectives beyond the hours (*pierwszy dzień tygodnia* is heard in the crossword, not taught), the third person of the routine verbs (*budzi się* is only in statements), and the genitive of the days (*do soboty*).

## Chapter 13: Co robiliście w weekend?

Book pages 102 to 107, recordings 144 to 150. Fourteen parts, 248 Exercises and 109 new Items: the past tense.

- **A** *Co robiliście w weekend?*, in six: *być* in the past, *ja* and *ty* (17: *byłem*, *byłam*, *wczoraj*, *w kinie*, *byłeś*, *byłaś*, *kiedyś*, *pierwszy raz*); *on*, *ona*, *ono* and *oni*, *one* (17: *był*, *była*, *było*, *ładnie*, *pogoda*, *byli*, *były*, *na meczu*); *my* and *wy*, with a Tip on a group with a man in it (17: *byliśmy*, *byłyśmy*, *byliście*, *byłyście*, *na treningu*, *na imprezie*, *weekend*, *jak było?*); a woman's weekend, with a Tip on the endings (18: *kupiłam*, *sweter*, *spędziłam*, *kilka godzin*, *tańczyłam*, *poznałam*, *wróciłam*, *wspaniale*); a quiet weekend, with a Tip on *-ować* (18: *robiłaś*, *czytałem*, *oglądałam*, *spałam*, *grałam*, *pomagałam*, *pracowałem*, *spokojny*); and *kiedy?* (18: *przedwczoraj*, *temu*, *miesiąc*, *dni*, *w zeszłym tygodniu*, *w zeszłym roku*, *w zeszłą sobotę*, *ostatnio*).
- **B** *Chciałem dobrze!*, in three: verbs in *-eć*, *e* to *a* (18: *chciałem*, *w muzeum*, *obraz*, *widziałem*, *musiałem*, *myślałam*, *piękny*); *mieć* and the *e* kept for a group with a man in it (18: *miałem*, *miał*, *mieliśmy*, *chcieliście*, *woleli*, *wiedziałem*, *rozumiałaś*, *zupełnie nic*); and the museum story (18: *zrobić*, *ochrona*, *prosiłem*, *zadzwonić*, *na policję*, *śmiali się*, *głośno*, *wstyd*).
- **C** *Jadłaś już obiad?*, in four: *jeść*, with Tips on *jadł* and *jedli* (18: *jadłem*, *jadłeś*, *jadł*, *sushi*, *jedliśmy*, *jedliście*, *jedli*, *marzenie*); *pójść*, with Tips on *poszedł* and *poszli* (18: *poszedłem*, *poszedłeś*, *poszedł*, *poszliśmy*, *poszliście*, *poszli*, *ugotowała*, *kartka*); *chodziłem*, *szedłem* and *poszedłem* (17: *chodziłem*, *szedłem*, *padał deszcz*, *na premierę*, *na piechotę*, *siedziałem*, *przed komputerem*); and no signal in the mountains, with the genitive after *nie miałem* (18: *w górach*, *zasięg*, *telefonu*, *odbierałeś*, *odpisywałeś*, *martwiliśmy się*, *szczęśliwy*).
- *Ćwiczenia* (18): a morning told in order, with Tips on *najpierw*, *potem*, *następnie* and on *właśnie* (*wstałam*, *umyłam*, *zjadłam*, *następnie*, *odwiedzić*, *szczęście*, *właśnie*, *jednocześnie*).

A past form is one Item for each person, its other gender as its form, and its meaning names both (*kupiłam*: a woman says it; *kupiłem*: a man). The tables of *być*, *czytać*, *mieć*, *jeść* and *pójść* are grammar Tips in two halves, *ja*, *ty*, *on* / *ona* with a man's and a woman's column, and *my*, *wy*, *oni* / *one* with a column for a group with a man in it and one for women only. They are not conjugation tables, so the Verb challenge stays in the present. *Wstyd*, *zasięg*, *marzenie*, *szczęście*, *szczęśliwy*, *randka*, *właśnie* and *jednocześnie*, the words the book sends to the dictionary, are under Look up. There are 45 listening tasks on all 7 recordings, and one Chat: *How was your weekend?* (Kasia, a friend, informal: Saturday at the cinema and a Polish film, Sunday at home, and no dinner yet, so she invites you; you are a man, so you say *byłem*). Seventeen Items have pictures (a cinema, a museum, a football, a party popper, a framed picture, a computer, a coat, sushi, mountains, rain, a cloud with the sun, a memo, a guard, a police light, signal bars, footprints, a smiling face). Its Games are **Story strip**: a short story in the past, *Najpierw wstałam o siódmej. Potem umyłam zęby.*, heard or read, and its pictures put in order (under `stories` in `practice.json`); and **Who said it?**: a sentence in the past, heard or read, and who said it from the verb's ending, a man, a woman, a group with a man in it or women only (under `speakers`).

It gives the later chapters:

- the past tense of every verb so far, in all persons and both genders, with *być*, *mieć*, *jeść*, *pójść* and *iść* as they are;
- *kiedy?* in the past: *wczoraj*, *przedwczoraj*, *… temu*, *w zeszłym tygodniu*, *w zeszłą sobotę*, *ostatnio*;
- telling what happened in order: *najpierw*, *potem*, *następnie*, *a później*;
- the genitive after a negated *mieć* in the past: *nie miałem zasięgu*, *telefonu*.

It leaves for later: aspect as a rule (*zrobić* and *zadzwonić* are met as done once, *robić* and *dzwonić* as often, without the word), and the past of the verbs the book has not met yet.

## Chapter 14: Z przewodnikiem po Krakowie

Book pages 110 to 115, recordings 151 to 156. Fourteen parts, 245 Exercises and 108 new Items: getting around a city and asking the way.

- **A** *Uczymy się o Krakowie*, in six: a quiz about Kraków (16: *na quiz*, *na wykłady*, *warsztaty*, *coś nowego*, *zobaczymy*, *nagroda*, *przewodnik*, *po Krakowie*); *iść* (17: *idzie*, *idą*, *raz*, *na legendy*, *hura!*, *serio?*); *chodzić*, with a Tip on *idę* or *chodzę* (16: *chodzicie*, *pieszo*, *do pracy*, *fotografujemy*, *w internecie*, *dzielnica*); *jechać*, *a* to *e* (18: *jadę* … *jadą*, *do Krakowa*, *do Warszawy*); *jeździć* and *czym?*, with a Tip on the instrumental (16: *jeździmy*, *jeździcie*, *czym jedziesz?*, *rowerem*, *autobusem*, *tramwajem*, *pociągiem*); and more ways to go (15: *metrem*, *motocyklem*, *skuterem*, *hulajnogą*, *windą*, *dorożką*, *na wakacje*).
- **B** *Zwiedzamy*, in five: the city (16: *miasto*, *stolica*, *Warszawa*, *skrzyżowanie*, *przystanek*, *światła*, *rzeka*, *most*); sights (16: *kościół*, *katedra*, *ratusz*, *zamek*, *pomnik*, *wieża*, *na Wawelu*, *znany*); the guide's tour (16: *na wprost*, *po prawej stronie*, *po lewej stronie*, *za pomnikiem*, *poeta*, *z pamiątkami*, *hejnał*); which way (15: *prosto*, *skręcić*, *w lewo*, *w prawo*, *na rogu*, *naprzeciwko*, *na górze*, *na dole*); and a group photo (16: *w środku*, *na zewnątrz*, *schody*, *długo*, *idealny*, *miejsce*, *zwiedzać*).
- **C** *Idziemy do pubu*, in three: the way to the pub (16: *jak dojść…?*, *dojść*, *przejść*, *przez park*, *daleko*, *blisko*, *w pubie*, *czekamy*); the accusative pronouns, with a table Tip (18: *mnie*, *cię*, *go*, *ją*, *nas*, *was*, *ich*, *je*); and after *na*, with a table Tip of both forms (17: *na ciebie*, *na niego*, *na nią*, *na nich*, *na nas*, *na was*, *czekaj!*, *już idziemy!*).
- *Ćwiczenia* (17): getting around, with *iść* or *chodzić*, *jechać* or *jeździć* in one sentence (*w dużych miastach*, *na targ*, *w góry*, *z kolegą*).

*Iść*, *chodzić*, *jechać* and *jeździć* are conjugation tables, so the Verb challenge asks all four; each Tip says which is now or once and which is often. A way of going is one Item in the instrumental (*rowerem*, *windą*), with its noun in the meaning. A pronoun after *na* is one phrase Item (*na niego*), because the book teaches *na* with each; *ich* (them) and *je* (them) share their spelling with *ich* (their) and *je* (he eats) and are separate Items. *Przewodnik*, *światła*, *schody*, *skrzyżowanie*, *zwiedzać*, *wygrać*, *nagroda*, *daleko* and *blisko*, the words the book sends to the dictionary, are under Look up. There are 39 listening tasks on all 6 recordings, and one Chat: *The way to the pub* (Pani Zofia, a stranger on the Rynek, formal: straight on, left, across the park, five minutes on foot; *dzień dobry* and *dziękuję bardzo*, not *cześć* and *dzięki*). Twenty-four Items have pictures (a city, a bridge, a church, a castle, traffic lights, a stop, a bike, a bus, a tram, a train, the underground, a motorbike, a scooter, a kick scooter, a lift, a horse, a walker, a trophy, a trumpet, three arrows, a beer, a mountain). Its Games are **Directions**: a map of six places, *kościół*, *zamek*, *przystanek*, and a route heard or read, *Proszę iść prosto, potem skręcić w lewo.*, to follow to its place (under `routes` and `places` in `practice.json`); and **Pronoun swap**: a sentence with its noun marked, *Kocham tatę.*, and the pronoun that says it shorter, *go*, *ją*, *je*, *ich*, or after *na*, *niego*, *nią*, *nich* (under `swaps`).

It gives the later chapters:

- *iść*, *chodzić*, *jechać* and *jeździć* in every person, and which to use;
- *czym?* and the instrumental of vehicles;
- the city and its sights, and directions: *prosto*, *w lewo*, *w prawo*, *na rogu*, *naprzeciwko*, *po prawej stronie*;
- the accusative pronouns, and their forms after *na*.

It leaves for later: the instrumental and the locative as rules (*rowerem*, *na Wawelu*, *w pubie* are met as phrases), the past of the verbs of motion beyond chapter 13's *szedłem* and *chodziłem*, and the pronouns after other prepositions (*z nim* was met in chapter 10).

## Chapter 15: Karton czy pudełko?

Book pages 116 to 123, recordings 157 to 176. Eighteen parts, 308 Exercises and 131 new Items: containers and amounts, the genitive plural, the post office, asking for help, and the pronouns in the genitive.

- **A** *Puszki, paczki i pudełka*, in five: containers (17: *puszka*, *karton*, *butelka*, *słoik*, *paczka*, *pudełko*, *kawałek*, *plasterek*); the genitive after a container (18: *kostka*, *cola*, *dżemu*, *masła*, *czekoladki*, *zapałki*, *tabliczka*, *czekolady*); what goes in what (17: *lód*, *makaronu*, *kukurydzy*, *kiełbasy*, *szynki*, *tort*, *ciasta*); a favour (16: *mam do ciebie prośbę*, *prośba*, *wolałbym nie*, *naprawdę*, *już idę*, *mamo!*); and at the counter, with a Tip on *gram*, *gramy*, *gramów* (18: *ćwierć kilo*, *gram*, *malinowego*, *żółtego*, *chudego*, *czarnego*, *pomarańczowego*, *jeszcze poproszę*).
- **B** *Zakupy na piknik*, in six: *dwa kartony*, *pięć kartonów* (16: *piknik*, *na piknik*, *za mało*, *musztardy*, *chipsów*); *ile?* and the words of amount (17: *dużo*, *mało*, *kilka*, *wiele*, *niewiele*, *osoba*, *studentów*, *nauczycieli*); verbs with the genitive, with *potrzebować* and *bać się* as conjugation Tips (17: *potrzebuję*, *potrzebują*, *samochodu*, *szukają*, *boję się*, *boją się*, *kierowców*, *historii*); the genitive plural, with a Tip on *-ów*, no ending, *-i* and *-ych* (18: *owoców*, *kotów*, *koleżanek*, *książek*, *jabłek*, *seriali*, *nowych*, *fajnych*); at the market (17: *co dla pani?*, *pieczarek*, *deka*, *może być*, *ziemniaków*, *kiszone ogórki*, *pół kilograma*, *wystarczy*); and packing for the picnic (17: *pakujesz się*, *ciężki*, *paluszki*, *orzeszki*, *zgrzewka*, *na osobę*, *gazowany*, *niegazowany*).
- **C** *Poczta, kurier, paczkomat*, in two: the post office (18: *poczta*, *na pocztę*, *znaczek*, *koperta*, *pocztówka*, *listonosz*, *kurier*, *paczkomat*); and at the counter (17: *wysłać*, *ważny*, *list polecony*, *awizo*, *kolejka*, *nie cierpię*, *nadać paczkę*, *zależy*).
- **D** *Czy pomożesz mi?*, in two: asking for help, from a friend to a stranger (17: *czy pomożesz mi?*, *czy możesz mi pomóc?*, *czy mógłbyś mi pomóc?*, *czy mogłaby mi pani pomóc?*, *czy byłby pan tak uprzejmy?*, *chciałem cię prosić o pomoc*, *pomoc*); and saying yes or no (17: *nie ma sprawy*, *nie ma problemu*, *o co chodzi?*, *czasu*, *umyć naczynia*, *nie mam pojęcia*, *pomogę*).
- **E** *Zaimki*, in two: after *do*, with a table Tip of the genitive pronouns alone and after *do* (17: *dzwonię*, *do ciebie*, *do niego*, *do niej*, *do nich*, *do mnie*, *do nas*, *do was*); and after *szukać* and *nie* (17: *szukasz*, *jej*, *ich*, *jego*, *nie widzę*, *nie ma go*, *nie odbierasz*).
- *Ćwiczenia* (17): what is in the bag (*w torbie*, *chusteczki*, *guma do żucia*, *za dużo*).

A container that is counted is one Item with its plural as its first form, its genitive plural as its second and, for a feminine one, its accusative as its third (*butelka*: *butelki*, *butelek*, *butelkę*), and a gender in `practice.json`; `AuthoredContentTest` holds *dwa*, *trzy* and *cztery* to the first form and *pięć* and on to the second. *Kilogram* gained its forms the same way. A noun in the genitive after a container, or a genitive plural after *dużo*, is its own Item (*dżemu*, *studentów*), as chapter 9's *wody* was. *Potrzebować*, *bać się*, *szukać* and *dzwonić* are conjugation tables. A genitive pronoun that is spelled like an older Item (*jej*, *ich*, *jego*) is a separate Item. *Plasterek*, *zapałki*, *zgrzewka*, *kolejka*, *nie cierpię*, *zależy*, *boję się*, *szukają*, *awizo* and *paczkomat*, the words the book sends to the dictionary, are under Look up. There are 64 listening tasks on all 20 recordings, and one Chat: *At the post office* (Pani Ola at the counter, formal: a parcel to Kraków and five stamps; *dzień dobry*, *chciałem* and *dziękuję*, not *cześć*, *chcę* and *dzięki*). Twenty-five Items have pictures (a can, a carton, a bottle, a jar, a package, a gift box, a piece of cake, a slice of bacon, butter, a chocolate bar, a cup with a straw, ice, a cake, a mushroom, a pretzel, peanuts, a basket, a post office, an envelope, a delivery van, a roll of paper, a cucumber, a car, a balance, a postbox). Its Games are **Packaging match**: what is in a container, *___ coli*, and the container it comes in, *puszka*, among four that do not go with it (under `packaging` in `practice.json`); and **How many?**: a container's picture shown two to ten times and the phrase that counts them, *pięć butelek*, not *pięć butelki* or *pięć butelka* (under `counting`).

It gives the later chapters:

- containers and amounts with the genitive: *butelka wody*, *dużo*, *mało*, *kilka*, *trochę*, *ćwierć kilo*;
- counting with the genitive plural: *dwie butelki*, *pięć butelek*;
- the genitive plural of nouns and adjectives;
- the post office, and asking for help politely;
- the pronouns in the genitive, alone and after *do*.

It leaves for later: the genitive plural as a rule for every noun (*-ów*, *-i* and the dropped ending are met in the words above), the conditional as a tense (*mógłbyś*, *wolałbym* are met as phrases), and people counted with *pięciu* or *dwóch*.

## Chapter 16: Co ma być, to będzie

Book pages 124 to 131, recordings 177 to 183. Seventeen parts, 294 Exercises and 111 new Items: the seasons and the weather, the forecast, the months and the holidays, the future with *będę*, plans, and Andrzejki.

- **A** *Parasol noś i przy pogodzie*, in seven: the seasons (18: *pora roku*, *wiosna*, *lato*, *jesień*, *zima*, *będzie*); when, with a Tip on *wiosną*, *latem*, *jesienią*, *zimą* (18: *czego słuchasz?*, *niedaleko*, *część*); how it is, with a Tip on *zimny*, *zimno* (17: *zimno*, *ciepło*, *gorąco*, *chłodno*, *brzydko*, *zielono*); the weather (17: *jaka jest pogoda?*, *pada deszcz*, *pada śnieg*, *świeci słońce*, *wieje wiatr*, *pochmurno*, *mgła*, *burza*); the forecast (18: *mróz*, *pada grad*, *prognoza pogody*, *temperatura*, *w radiu*, *w telewizji*, *niskie ciśnienie*); degrees and the weather to come, with a Tip on *jeden stopień*, *dwa stopnie*, *pięć stopni* (18: *poniżej zera*, *powyżej zera*, *będzie padać*, *będzie świecić słońce*, *będzie wiać wiatr*); and summer (17: *na plaży*, *w parku*, *spacerować*, *opalać się*, *kwiaty*).
- **B** *Miesiąc styczeń – czas do życzeń*, in three: the months, with a table Tip of each with its *w* (15 and 15: *styczeń* to *grudzień*, *w styczniu*, *we wrześniu*); and the holidays (18: *kiedy masz urodziny?*, *w jakim miesiącu?*, *Boże Narodzenie*, *sylwester*, *Nowy Rok*, *walentynki*, *wakacje*, *kończy się*).
- **C** *Dzieci najmilsze, gdy śpią*, in four: *być* in the future, with a table Tip (16: *będę*, *będziesz*, *będziemy*, *będziecie*, *będą*, *pojutrze*); *będę* with a verb, with a Tip (18: *remont*, *mam nadzieję*, *zapytać*, *dobre pytanie!*, *organizować*); when, with a Tip on *za* and *w przyszłym* (18: *za tydzień*, *za miesiąc*, *za rok*, *za dwa dni*, *w przyszłym tygodniu*, *w przyszłym roku*, *w przyszły poniedziałek*, *w przyszłą sobotę*); and *będę musiał*, *będę mogła* (18: *musiał*, *mógł*, *chciał*, *wcześnie rano*, *bawić się*, *z dzieckiem*).
- **D** *Andrzejki*, in two: the night of fortunes (17: *andrzejki*, *wróżyć*, *wróżba*, *świeca*, *wosk*, *cień*, *garnek*); and what the year will bring (18: *co będzie?*, *nikt nie wie*, *lać*, *uprawiać sport*, *grać koncerty*, *mieć dziecko*, *Paryż*).
- *Ćwiczenia* (18): adverbs, with a Tip on *szybki*, *szybko* (*szybko*, *wolno*, *drogo*, *tanio*, *miło*).

A month is one Item with its *w* as its form (*styczeń*, *w styczniu*; *wrzesień*, *we wrześniu*), as a day is since chapter 12; *wiosna* carries its genitive (*do wiosny*), chapter 13's *pogoda* gained its own (*od pogody*), and *miesiąc* its genitive plural (*miesięcy*). *Stopień* carries *stopnie* and *stopni* and a gender in `practice.json`, so the number test holds *dwa stopnie* and *dziesięć stopni*. The future of *być* is a table Tip, not a conjugation table; *będę* before an infinitive is the same Item (*będę robić*), and *musiał*, *mógł* and *chciał* carry their forms for a woman (*musiała*). *Część*, *pora roku*, *pogoda*, *prognoza pogody*, *zaczyna się*, *kończy się*, *mam nadzieję*, *wróżyć*, *andrzejki* and *cień*, the words the book sends to the dictionary, are under Look up. There are 32 listening tasks on all 7 recordings (181 repeats 180; 182 is the months), and one Chat: *Weekend weather* (Marek, a friend, informal: it depends on the weather, sun on Saturday so a walk in the park, rain and wind on Sunday, so a film at home; *do zobaczenia*, not *do widzenia*). Twenty-four Items have pictures (the seasons, rain, snow, the sun, wind, a cloud, fog, a storm, a snowflake, a thermometer, a bouquet, a Christmas tree, a heart with a ribbon, fireworks, a candle, a crystal ball, a radio, a television, a pot, tools, an island). Its Games are **Forecaster**: tomorrow's forecast heard or read, *Jutro będzie padać śnieg. Będą dwa stopnie poniżej zera.*, and its card made with the weather signs it names and its temperature (under `forecasts` in `practice.json`); and **Word search**: six of the months and seasons hidden in a grid of letters, found by what they mean (under `wordSearch`).

It gives the later chapters:

- the weather, now and in a forecast, and the seasons;
- the months, with *w*, and the holidays of the year;
- the future of *być*, and *będę* with an infinitive;
- *za tydzień*, *w przyszłym roku* and the other ways to say when;
- adverbs from adjectives, *szybko*, *tanio*.

It leaves for later: the future of perfective verbs (*zrobię*, *pójdę*), dates as ordinals with the month in the genitive (*dwudziestego dziewiątego listopada*), and *będę robił* (the future with the past form) beyond *musiał*, *mógł* and *chciał*.

## Chapter 17: Plotki, plotki. Kto z kim i o czym?

Book pages 132 to 137, recordings 184 to 190. Fifteen parts, 265 Exercises and 108 new Items: opinions and moods, *lubię* and *podoba mi się*, the dative pronouns, gossip and the pronouns after a preposition, presents, and souvenirs from Poland.

- **A** *Co o tym myślisz?*, in four: opinions (18: *co o tym myślisz?*, *podoba mi się*, *czy ja wiem?*, *nie mam zdania*, *wszystko jedno*, *nie mogę się zdecydować*); clothes for a party (18: *bluzka*, *w tej bluzce*, *w niej*, *spodnie*, *ładnie ci w tym kolorze*, *na imprezę*, *wyglądać*); how you look, with a Tip on *ładnie*, *grubo* (18: *wyglądam*, *wyglądasz*, *w nim*, *coś ty!*, *chciałabym*); and moods, with a Tip on *smutno mi*, *zimno mi* (18: *gorąco mi*, *chce mi się*, *zły dzień*, *co się dzieje?*, *beznadziejny*, *marudny*).
- **B** *Podoba ci się?*, in three: complaints said with feeling (18: *fuj!*, *strasznie*, *upał*, *czekać w korku*, *skandal*, *brudno*, *chodźmy*); *lubię* and *podoba mi się*, with a table Tip (18: *podoba ci się?*, *podobają mi się*, *nie bardzo*, *śliczny*, *denerwuje mnie to*, *irytuje mnie to*); and the dative, with a table Tip (15: *mi*, *ci*, *mu*, *jej*, *nam*, *wam*, *im*).
- **C** *Co o nim myślisz?*, in four: gossip (18: *plotki*, *zakochany*, *kochać*, *sprawy rodzinne*, *o czymś innym*, *spokojnie*, *nieprawda!*); *o mnie* to *o nich*, with the big table of the pronouns (17: *zaprosić*); *ze mną* to *z nimi* and the past of *podobać się* (17: *poznać*, *rozmawiałam*, *podobał się*, *podobała się*); and *dla ciebie*, *dla niego*, *jemu*, *tobie* (18: *prezent*, *dam*).
- **D** *Co mu kupić?*, in three: souvenirs (18: *pamiątka*, *bursztyn*, *z bursztynu*, *biżuteria*, *srebrny*, *oscypek*, *obwarzanek*, *grzaniec*, *kupimy*); presents (18: *co mu kupić?*, *płyta*, *koszulka*, *magnes*, *książka do słuchania*, *haftowany*, *tradycyjny*); and saying yes or no to an idea (18: *co ty na to?*, *idealnie!*, *to bez sensu*, *banalny*, *coś specjalnego*, *coś praktycznego*, *lepiej*, *nie znam się na tym*).
- *Powtórzenie* (18): a birthday, *sto lat!*, *moi drodzy*, *przyjaciel*, *przyjaciele*, *dziękuję za*, with the chapter's words again.

The pronouns after a preposition are phrase Items (*o niej*, *z nim*, *dla niej*), not the bare form, so a learner never meets *niej* on its own; the full table of the personal pronouns is a Tip. *Podobać się* is taught as *podoba się* with the dative Items, and *podobał się* carries its forms (*podobała się*, *podobało się*, *podobały się*). *Bluzka* carries *bluzkę* and *bluzce*, *spodnie* its genitive, *przyjaciel* its plurals. *Modny*, *marudny*, *wyglądać*, *plotki*, *strasznie*, *biżuteria*, *bluzka* and *spodnie*, the words the book sends to the dictionary, are under Look up. There are 34 listening tasks on all 7 recordings (185 repeats 184, 188 repeats 187, 190 repeats 189), and one Chat: *A birthday present* (Kasia, a friend, informal: what to buy Adam, a record or a dictionary, *to bez sensu* because he speaks Polish well, then an oscypek, a record and a T-shirt). Fifteen Items have pictures (moods as faces, a present, a T-shirt, jeans, a CD, a magnet, a gem, an OK hand). Its Game is Pronoun grid, which fills the table of the pronouns a square at a time, with *komu?*, *o kim?* and *dla kogo?* from this chapter.

It gives the later chapters:

- opinions and how to answer one: *podoba mi się*, *czy ja wiem?*, *wszystko jedno*;
- the dative pronouns with *podoba się*, *smutno*, *zimno* and *chce się*;
- the personal pronouns after *o*, *z* and *dla*;
- presents and souvenirs, and *co ty na to?*.

It leaves for later: the dative of nouns (*Adamowi*, *mamie*), the long dative forms as a rule (*jemu*, *tobie* are met as words), and the instrumental and locative of nouns beyond the phrases above.

## Chapter 18: Pokaż mi swoje mieszkanie…

Book pages 138 to 145, recordings 191 to 196. Seventeen parts, 293 Exercises and 130 new Items: the rooms and furniture of a flat, *stoi*, *leży*, *wisi*, the locative singular and plural after *w*, *na*, *przy*, *o* and *po*, *w* or *na*, dreams and travels, and a family's new flat.

- **A** *„Ciasne, ale własne!”*, in six: the rooms (17: *pokój*, *kuchnia*, *łazienka*, *sypialnia*, *salon*, *przedpokój*, *balkon*, *na*); walls, floors and storeys (17: *ściana*, *podłoga*, *sufit*, *drzwi*, *piętro*, *parter*, *blok*, *przy*); furniture (17: *łóżko*, *szafa*, *biurko*, *lampa*, *fotel*, *dywan*, *regał*, *kanapa*); the kitchen and the bathroom (17: *lodówka*, *kuchenka*, *zmywarka*, *pralka*, *wanna*, *umywalka*, *lustro*, *półka*); *stoi*, *leży*, *wisi* and their plurals (17: *telewizor*, *kwiatek*); and what a flat is like (17: *ciasny*, *własny*, *wygodny*, *cichy*, *z widokiem na park*, *w rogu*, *na środku*, *obok*).
- **B** *Gdzie on jest?*, in six: the locative singular, with a table Tip (17: *w banku*, *w sklepie*, *w hotelu*, *w kawiarni*, *na ulicy*, *w firmie*, *w mieszkaniu*, *w centrum*); the sounds that change, with a table Tip (18: *w mieście*, *w lesie*, *na świecie*, *po obiedzie*, *w kościele*, *o sąsiedzie*, *o panu*, *o synu*); describing words, with a table Tip (17: *w jakim?*, *w jakiej?*, *w dużym mieście*, *w małym mieście*, *w nowym bloku*, *w starej kamienicy*, *na czwartym piętrze*, *w dobrej szkole*); *w* or *na*, with a table Tip (17: *na poczcie*, *na policji*, *na dworcu*, *na basenie*, *na lotnisku*, *na koncercie*, *na filmie*, *w pracy*); the plural *-ach*, *-ych*, with a table Tip (17: *w różnych miejscach*, *w hotelach*, *w restauracjach*, *na Mazurach*, *w Stanach Zjednoczonych*, *na łyżwach*, *w spodniach*, *w rękawiczkach*); and a room that is *w sam raz* (16: *poduszka*, *ubranie*, *gwiazdka*, *świecą się*, *beżowy*, *plastikowy*).
- **C** *Marzę o dalekich podróżach*, in three: *o* and the locative after *myśleć*, *marzyć*, *rozmawiać*, *pamiętać*, with a table Tip (17: *marzę*, *marzysz*, *marzy*, *rozmawiamy*, *pamiętam*, *o czym?*, *o wakacjach*, *o pracy*); *po* as around (18: *po świecie*, *po Polsce*, *po mieście*, *po parku*, *po Europie*, *po górach*, *spaceruję*, *podróżuję*); and far-off travels (18: *niedługo*, *mam dość*, *daleki*, *podróż*, *kształcą*, *w weekendy*, *dla wszystkich*, *do rodziców*).
- *Powtórzenie*, in two: a family's new flat (18: *dom*, *w Śródmieściu*, *gabinet*, *na wsi*, *kominek*, *strych*, *piwnica*, *kredyt*), and where everything is (18: *meble*, *spiżarnia*, *garaż*), with the chapter's words again.

The locative is taught as phrase Items (*w banku*, *na poczcie*, *po świecie*), as the instrumental was, and the nouns of the flat carry theirs as forms (*pokój* carries *w pokoju*, *łóżko* carries *na łóżku*), so *w* is never an Item on its own; *na* and *przy* are, with their meaning of where. The verbs of place are taught as forms (*stoi*, *stoją*, *leży*, *leżą*, *wisi*, *wiszą*). *Piętro*, *meble*, *parter*, *w starej kamienicy*, *kominek*, *spiżarnia*, *strych*, *piwnica*, *garaż* and *w rękawiczkach*, the words the book sends to the dictionary, are under Look up. There are 50 listening tasks on all 6 recordings, and one Chat: *A new flat* (Ewa, a friend, informal: she shows you round her first flat of her own, you ask where the rooms are, the photos hang on the wall, and *ciasne, ale własne*). Nineteen Items have pictures (a sofa, a door, a mirror, a bath, a pot plant, a bed, a star, a hotel, a station, a forest, a globe, gloves, ice skates, luggage and others). Its Games are Room scene, which fills a room as the sentences put each thing on its place, *Lampa stoi na biurku*, and Stoi, leży czy wisi?, which gaps the verb of the same sentences.

It gives the later chapters:

- the rooms and furniture of a home, and *stoi*, *leży*, *wisi*;
- the locative after *w*, *na*, *przy*, *o* and *po*, singular and plural, with describing words;
- *w* or *na* with places, events and countries;
- *marzyć o*, *myśleć o*, *rozmawiać o*, and *po* as around.

It leaves for later: *pod*, *nad* and *za* for where things are, and the locative of the months and years (*w maju*, *w 2020 roku*).

## Chapter 19: Wszędzie dobrze, ale w domu najlepiej

Book pages 146 to 151, recordings 197 to 205. Seventeen parts, 292 Exercises and 128 new Items: a bad morning and a move to a hotel, booking a room, the kinds of hotel room, what a flat is like, kinds of flat, looking for one, adverts and a call about one, moving house, the prepositions of place with the genitive and the instrumental, and putting things where they go.

- **A** *Nie mam gdzie mieszkać*, in five: a bad morning (18: *spóźniony*, *co się stało?*, *wściekły*, *zaspałam*, *sąsiad*, *nie mam gdzie mieszkać*, *hotel*, *przeprowadzam się*); booking a room (17: *zarezerwować*, *jednoosobowy*, *dwuosobowy*, *z łazienką*, *od dziś*, *od jutra*, *na cały tydzień*, *na jakie nazwisko?*); the kinds of room (16: *trzyosobowy*, *czteroosobowy*, *z klimatyzacją*, *z widokiem na morze*, *z dostępem do internetu*, *ze śniadaniem*, *na pierwszym piętrze*, *na ostatnim piętrze*); how long and from when, with a table Tip (17: *na jak długo?*, *od kiedy?*, *noc*, *na weekend*, *od środy*, *do piątku*, *na którym piętrze?*, *z czym?*); and at reception (18: *recepcja*, *czy jest wolny pokój?*, *w cenie*, *bez łazienki*, *taras*, *gotowe!*, *mieszkać*).
- **B** *W poszukiwaniu mieszkania*, in six: what a flat is like, with a table Tip of opposites (18: *słoneczny*, *przestronny*, *przytulny*, *nowoczesny*, *stylowy*, *ponury*, *samodzielny*, *wspólny*); kinds of flat (17: *kawalerka*, *kamienica*, *jednopokojowy*, *dwupokojowy*, *metr kwadratowy*, *po remoncie*, *ogrzewanie*, *dom jednorodzinny*); *szukam mieszkania* (17: *blisko szkoły*, *wolałabym*, *sam*, *współlokator*, *maksymalnie*, *to nie ma znaczenia*, *ile możesz zapłacić?*); adverts (16: *do wynajęcia*, *umeblowany*, *czynsz*, *kaucja*, *media*, *prąd*, *gaz*, *aneks kuchenny*); a call about an advert (16: *dzwonię w sprawie ogłoszenia*, *czy oferta jest jeszcze aktualna?*, *obejrzeć*, *kiedy mogę przyjść?*, *koszty miesięczne*, *centralne ogrzewanie*, *od strony południowej*, *zapraszam od zaraz*); and packing (17: *spakować się*, *rzeczy*, *pobyć sama*, *w ciszy*, *od podwórka*, *mało słońca*, *zamówić taksówkę*, *za dziesięć minut*).
- **C** *Przeprowadzka*, in four: moving day (18: *przeprowadzka*, *dziękuję za pomoc*, *wszystko się dobrze skończyło*, *rozplanowany*, *pasują do ciebie*, *wpadniemy do ciebie*, *w każdy weekend*, *macie rację*); the prepositions with the genitive, with a table Tip (17: *koło*, *obok szafy*, *koło okna*, *blisko domu*, *na wprost drzwi*, *wzdłuż*, *wzdłuż ściany*, *pies*); the prepositions with the instrumental, with a table Tip (17: *przed*, *przed domem*, *za*, *pod*, *pod krzesłem*, *nad*, *nad stołem*, *między*); and Angela's new room (18: *komoda*, *przed telewizorem*, *pod oknem*, *między regałem a biurkiem*, *za biurkiem*, *nad łóżkiem*, *obok łóżka*, *za drzewem*).
- *Powtórzenie*, in two: *postawić*, *położyć*, *powiesić* (18: *wieszak*, *parapet*, *szafka*, *ręcznik*), and *na górze*, *na dole* (17: *góra*, *dół*), with the chapter's words again.

The prepositions of place are taught as Items with their meaning of where (*przed*, *za*, *pod*, *nad*, *między*, *koło*, *wzdłuż*) and as phrase Items (*przed domem*, *obok szafy*), and the Tip on the instrumental says they take it when they say where something is. *Wściekły*, *przeprowadzam się*, *zaspałam*, *przytulny*, *przestronny*, *ponury*, *kawalerka*, *czynsz* and *kaucja*, the words the book sends to the dictionary, are under Look up. There are 67 listening tasks on all 9 recordings, and one Chat: *A room for three nights* (a receptionist, formal: you ring a hotel in Kraków and book a single room with a bathroom from tomorrow, for three nights, *na moje nazwisko*). Fourteen Items have pictures (a hotel, a night, a dog, a taxi, a reception bell, a quiet face, a furious face, a sleeping face, electricity, the sea, a house with a garden, the sun, luggage and a television). Its Game is Flat hunt: someone says what they are looking for, *Mam psa. Szukam mieszkania na parterze.*, and of three adverts the Learner picks the one that fits.

It gives the later chapters:

- booking a room, and how long and from when (*na jak długo?*, *od środy do piątku*);
- describing a flat and reading a flat advert;
- *przed*, *za*, *pod*, *nad* and *między* with the instrumental, and *obok*, *koło*, *blisko*, *na wprost* and *wzdłuż* with the genitive, for where things are;
- *postawić*, *położyć*, *powiesić* beside *stoi*, *leży*, *wisi*.

It leaves for later: the same prepositions with the accusative for where things go (*pod stół*, *nad morze*).

## Chapter 20: Podróże kształcą

Book pages 152 to 157, recordings 206 to 210. Nineteen parts, 305 Exercises and 120 new Items: the compass points and the regions of Poland, its neighbours, its sea, rivers, mountains and lakes, sights worth seeing, the railway station, tickets and kinds of train, asking at the information desk and buying a ticket, a journey, a phone call about a weekend away, and *gdzie?* beside *dokąd?* with *w*, *do*, *nad* and *na*.

- **A** *Dokąd można pojechać na weekend?*, in six: the compass points (16: *można*, *pojechać*, *północ*, *południe*, *wschód*, *zachód*, *Gdańsk*); the points between, with a table Tip (18: *na północnym zachodzie*, *na północnym wschodzie*, *na południowym zachodzie*, *na południowym wschodzie*, *Szczecin*, *Wrocław*, *Poznań*, *Lublin*); Poland and its neighbours (18: *Polska*, *kraj*, *w Europie Środkowej*, *graniczy*, *granica*, *z Niemcami*, *z Czechami*, *z Ukrainą*); the sea, the Vistula, the Tatras and the lakes (17: *morze*, *Bałtyk*, *Wisła*, *najdłuższy*, *Tatry*, *jezioro*, *region*, *turystyczny*); words for places (17: *skała*, *szczyt*, *schronisko*, *szlak*, *wyspa*, *las*, *brama*, *mur*); and sights (17: *zabytek*, *kopalnia soli*, *Stare Miasto*, *średniowieczny*, *największy*, *najstarszy*, *najwyższy*, *warto zobaczyć*).
- **B** *Dworzec PKP*, in six: at the station (16: *dworzec*, *pociąg*, *odjeżdża*, *przyjeżdża*, *peron*, *informacja*, *w kasie*, *rozkład jazdy*); tickets, with a table Tip (16: *w jedną stronę*, *tam i z powrotem*, *zniżka*, *dla studentów*, *dla emerytów*, *za sto procent ceny*, *miejscówka*); kinds of train (17: *pociąg osobowy*, *pociąg pospieszny*, *Intercity*, *szybki*, *bezpośredni*, *przesiadka*, *wagon*, *przedział*); at the information desk (16: *Toruń*, *o której godzinie?*, *połączenie*, *to znaczy*, *trzeba*, *będziemy na miejscu*, *za późno*, *o cenę proszę pytać*); at the ticket office (18: *przez Warszawę*, *obcokrajowiec*, *klasa*, *która klasa?*, *czy można płacić kartą?*, *okienko*, *z którego peronu?*, *z drugiego peronu*); and the journey (16: *odjazd*, *przyjazd*, *ile trwa podróż?*, *opóźniony*, *Zakopane*, *wycieczka*, *Malbork*).
- **C** *Gdzie jesteś? Dokąd jedziesz?*, in five: a phone call (18: *dojechać*, *po co?*, *puszcza*, *natura*, *zwierzęta*, *do miasta*, *skąd wiesz?*, *przyjedź!*); a city, a country, a building: *w* + locative and *do* + genitive, with a table Tip (17: *do Polski*, *w Niemczech*, *do Niemiec*, *w Czechach*, *do Czech*, *Europa*, *Małopolska*, *Wieliczka*); mountains: *w Tatrach*, *w Tatry*, with a table Tip (15: *Beskidy*, *Bieszczady*, *Sudety*, *Alpy*, *na narty*); water: *nad morzem*, *nad morze*, with a table Tip (14: *Dunajec*, *Adriatyk*, *Morskie Oko*); and a square or an island: *na rynku*, *na rynek*, with a table Tip (14: *na rynek*, *Sycylia*, *na Mazury*).
- *Powtórzenie*, in two: *gdzie? dokąd?* all together, with a table Tip (13), and a trip by train (12), with the chapter's words again.

The places are taught as Items with their two forms (*Toruń*: *w Toruniu*, *do Torunia*; *Tatry*: *w Tatrach*, *w Tatry*; *morze*: *nad morzem*, *nad morze*), so every Exercise says *gdzie?* or *dokąd?* with a whole phrase. *Graniczy*, *granica*, *połączenie*, *wycieczka*, *średniowieczny* and *kopalnia soli*, the words the book sends to the dictionary, are under Look up. There are 46 listening tasks on all 5 recordings, and one Chat: *A ticket to Toruń* (a cashier, formal: you buy a full-price, second-class return ticket to Toruń, ask to pay by card and ask for the platform). Eighteen Items have pictures (a train, a station, an island, a forest, a summit, Europe, the sea, a card, a delay, an Intercity, a carriage, a rock, a mountain hut, a compass, skis, a wall, animals and a salt mine). Its Game is Poland map: a city, a range of mountains or a sea is named, *Gdańsk*, and the Learner taps where in Poland it lies on a compass of eight, *na północy*; then *Gdańsk leży na północy, nad morzem.* is said.

It gives the later chapters:

- the compass points and where a place lies (*na północy*, *na południowym wschodzie*);
- the railway station, buying a ticket and asking about a connection;
- *gdzie?* and *dokąd?* with *w*, *do*, *nad* and *na*, for cities, countries, mountains, water and squares.

## Chapter 21: Kiedy to było?

Book pages 158 to 165, recordings 211 to 221. Twenty-two parts, 376 Exercises and 155 new Items: birthdays and name days, the ordinals and the months in the genitive for a date, wishes for every occasion and *sto lat*, the holidays of the year, a life story in the past tense, the year said with an ordinal, a little Polish history, and the two aspects in the past, *robiłam* beside *zrobiłam*.

- **A** *Kiedy masz urodziny i imieniny?*, in five: birthdays and name days (18: *imieniny*, *obchodzić*, *na urodziny*, *dziadka*, *osiemdziesiąty*, *cała rodzina*, *z okazji urodzin*, *każdy*); *który dziś jest?*, with a table Tip (16: *pierwszy*, *stycznia*, *drugi*, *lutego*, *trzeci*, *marca*, *kwietnia*); *kiedy?* and the date in the genitive, with a table Tip (17: *maja*, *czwarty*, *czerwca*, *piąty*, *lipca*, *szósty*, *sierpnia*, *siódmy*); the rest of the year (16: *września*, *ósmy*, *października*, *dziewiąty*, *listopada*, *dziesiąty*, *grudnia*, *dwudziesty*); and name days in the calendar (18: *kiedy masz imieniny?*, *w kalendarzu*, *jedenasty*, *czternasty*, *raczej*, *piętnasty*, *trzydziesty*).
- **B** *Sto lat i wszystkiego najlepszego!*, in five: wishes, *życzę* and the genitive (17: *wszystkiego najlepszego!*, *życzę*, *zdrowia*, *szczęścia*, *miłości*, *sukcesów*, *pomyślności*, *życzenia*); *sto lat!* (17: *niech żyje*, *śpiewamy*, *składać*, *gość*, *świeczka*, *szampan*, *wszystkiego dobrego!*); a wish for each occasion (17: *wesołych świąt!*, *szczęśliwego Nowego Roku!*, *udanych wakacji!*, *szczęśliwej podróży!*, *szybkiego powrotu do zdrowia!*, *na nowej drodze życia*, *wesołego Alleluja!*); occasions and holidays (17: *ślub*, *egzamin*, *choroba*, *wyjazd*, *Wielkanoc*, *Wigilia*, *święto*); and the days of the year (17: *Dzień Kobiet*, *Dzień Matki*, *Dzień Dziecka*, *prima aprilis*, *Dzień Nauczyciela*, *Wszystkich Świętych*, *Święto Niepodległości*, *co to za święto?*).
- **C** *Biografia*, in six: born and at school (18: *urodziłem się*, *urodził się*, *kiedy się urodziłeś?*, *przedszkole*, *szkoła podstawowa*, *liceum*, *chodził*, *życie*); the *matura* and university (17: *zdałem*, *zdał*, *studia*, *studiowałem*, *skończyłem*, *politechnika*, *uczyłem się*); love and a family (17: *poznał*, *zakochał się*, *pobrali się*, *ożenił się*, *wyszła za mąż*, *w ciąży*, *urodziła*); work and retirement (18: *pracował*, *mieszkał*, *robił*, *kariera*, *został*, *emerytura*, *przeszedł*, *umarł*); *w którym roku?*, with a table Tip (18: *w roku*, *dziewięćdziesiąty*, *siedemdziesiąty*, *sześćdziesiąty*, *czterdziesty*, *dwutysięczny*, *tysiące*); and history (18: *wojna*, *Powstanie Warszawskie*, *zginął*, *stocznia*, *Solidarność*, *komunizm*, *upadł*, *pięćdziesiąty*).
- **D** *Mami, co robiłaś?*, in four: *robiłam* or *zrobiłam* (18: *praca domowa*, *zrobiłaś*, *przeczytałam*, *cały*, *do końca*, *wieczór*); aspect pairs, with a table Tip (18: *piłam*, *wypiłam*, *gotowałam*, *ugotowałam*, *kupowałam*, *sprzątałam*, *posprzątałam*, *czysto*); how many times (18: *dzwoniłem*, *zadzwoniłem*, *zapraszałam*, *zaprosiłam*, *spotykaliśmy się*, *spotkałam*, *podziękowałam*, *co tydzień*); and the words that pick an aspect, with a table Tip (18: *wstawałam*, *nagle*, *w końcu*, *wreszcie*, *obejrzałem*, *rozmawialiśmy*, *porozmawialiśmy*).
- *Powtórzenie*, in two: dates and years (14: *roku*, for a whole date such as *piątego maja dwutysięcznego roku*), and a weekend told with both aspects (14), with the chapter's words again.

The ordinals are Items with their forms for a date and a year (*piąty*: *piątego*, *piątym*), and the months are Items in the genitive (*maja*), so every date and year is said whole. *Obchodzić*, *imieniny*, *upaść* and *komunizm*, the words the book sends to the dictionary, are under Look up. There are 79 listening tasks on all 11 recordings, and one Chat: *An eightieth birthday* (Pan Henryk, formal: you wish him all the best, say when your birthday and name day are and where and when you were born, and ask about his life). Seventeen Items have pictures (a candle, champagne, a calendar, a wedding, an illness, a journey, Easter, Christmas Eve, an exam, a kindergarten, a shipyard, a pregnancy, falling in love, retirement, April Fools' Day, Women's Day and Children's Day). Its Game is Calendar: a page of the calendar shows a holiday and its date, *14.02, Walentynki*, and the Learner taps how to say when it is, *czternastego lutego*, not *czternasty lutego* nor *czwartego lutego*; then *Walentynki są czternastego lutego.* is said.

It gives the later chapters:

- the date, *który dziś jest?* and *kiedy?* (*piąty maja*, *piątego maja*), and the year (*w tysiąc dziewięćset dziewięćdziesiątym roku*);
- wishes with *życzę* and the genitive;
- a life story in the past, and the two aspects of the past tense (*robiłam*, *zrobiłam*).
