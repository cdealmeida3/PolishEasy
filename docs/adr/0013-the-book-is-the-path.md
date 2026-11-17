# The book is the Path

`docs/a1-path.md` planned an A1 Path of twelve invented Units. After v1 Carlos's Polish teacher gave him the class book, *Polski krok po kroku 1*, with a video of its slides and the class recordings, and he asked for the app to be built on it, "using the slides as a source of truth", chapter by chapter (spec #92).

So the Path is the book. A Level, `kpk1`, "Polski krok po kroku 1", has the book's 27 chapters as its Units, `kpk1-l00` (*Dzień dobry!*) to `kpk1-l26` (*Same problemy!*), in the book's order, with the book's numbers, Polish titles and K, L and G topics from its contents. A section of a chapter (A, B, C …) is a Daily Set, with its letter and heading. Units 1 to 3, which v1 shipped and Carlos has played, stay as a Level of their own, **Starter**, shown first; nothing he has done is lost and their ids do not change. The twelve-Unit plan is dropped: Units 4 to 12 will not be written.

The Path is shown as shelves of chapter cards, and each chapter has a **Lesson screen** in the book's colour, with its sections and a Powtórzenie of three tools that need no content of their own: the **Słownik** and the **Fiszki**, made from the Items the chapter introduces, and the **Checkpoint** ("Test"), made from its Exercises. Every Exercise shows the book's Polish instruction, with the English one tap away, and phrases carry the book's register mark (O, N, U).

The chapters not yet written are in `course.json` as **coming** Units: titled, with what they will teach, and no Daily Sets. The Path shows all 27 from the start, so Carlos sees the whole book, and each chapter step (L01 to L26) only adds files and takes off `coming`.

## Rules that follow

- **No text from the book is copied into the content.** The book and its recordings belong to the publisher. The repository may hold the chapters' titles, the K, L and G lists of the contents, word lists, the headings of sections, track numbers and timings. Dialogues, readings, exercises and illustrations are not copied: Daily Sets are written fresh on the chapter's words and grammar. The recordings never go in the repository or the APK (step F2 imports them on the phone).
- **A chapter introduces again a word that Starter or an earlier chapter introduced** (rule 1 of authoring), under the same Item id, so its Słownik and Fiszki have every word it uses and Review history carries over. An Item belongs to the first Daily Set on the Path that introduces it, so a Summary in a later chapter does not list it again as learned.
- **A Checkpoint saves nothing but its Best**, in a file of its own (`checkpoints.json`), the way Practice keeps its Bests apart (ADR-0011). It changes nothing on the Path, in Review or in the Streak.
- **A flashcard answer is a Review answer**: "I knew it" and "Not yet" move the Item through the same Leitner boxes as an Exercise's first attempt.

## Considered options

- *Keep the invented A1 Path and use the book as a source of words.* Rejected: Carlos asked for the book as the source of truth, and his classes follow its order.
- *Replace Units 1 to 3 with chapter 0 and 1.* Rejected: their completed Daily Sets and Review history are on his phone; renaming ids would lose them.
- *Add chapters only when written.* Rejected: the whole book on the shelf, "coming soon", shows where the class is going, and costs one entry per chapter.
