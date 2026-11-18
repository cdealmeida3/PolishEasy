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

A noun's plural is its form (*gruszka*, *gruszki*), so the Items of chapter 8 that this chapter puts in the plural gained theirs (*banany*, *jabłka*, *pomarańcze*, *truskawki*, *maliny*, *cytryny*, *pomidory*, *ogórki*, *ryby*). There are 37 listening tasks on all 9 recordings (103 is a word list; 107 to 109 repeat 104 to 106 to fill in), and one Chat: *At the market* (Pani Ewa, formal: four tomatoes, two yellow peppers, fresh basil in a pot, ripe bananas). The new nouns are in Practice's `genders`, *miękki / twardy*, *świeży / suszony* and *surowy / gotowany* are in `opposites`, and 29 Items have pictures (the fruit and vegetables, the aisles, a pan and scales).

It gives the later chapters:

- the nominative and accusative plural of things, and adjectives in the plural in *-e*;
- *dwa / dwie*, *jeden / jedna / jedno*, *kilo* and *pół kilo*, *poproszę*;
- *kupować* and *uwielbiać*, and the cooked adjectives (*gotowany*, *pieczony*, *smażony*).

It leaves for later: the genitive plural after five and after *kilo* (*pięć jabłek*, *kilo pomidorów*, only in the Tip and the recordings), the plural of people (*oni*, *studenci*), and the comparative as a rule (*zdrowszy* is an Item).
