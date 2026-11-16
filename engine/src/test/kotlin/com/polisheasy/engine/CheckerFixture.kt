package com.polisheasy.engine

import kotlinx.serialization.encodeToString

private fun en(text: String): Localized = mapOf("en" to text)

/**
 * A small course's files, held in memory. [valid] passes every rule of the content checker; a
 * test breaks it in one place with the `with…` functions, which each return a changed copy.
 */
internal data class CheckerFixture(val texts: Map<String, String>) {
    fun files(): ContentFiles = object : ContentFiles {
        override fun read(path: String): String = texts[path] ?: throw NoSuchElementException("There is no such file")
        override fun paths(): Set<String> = texts.keys
    }

    fun withFile(path: String, text: String): CheckerFixture = copy(texts = texts + (path to text))

    fun without(path: String): CheckerFixture = copy(texts = texts - path)

    fun withIndex(edit: (CourseFile) -> CourseFile): CheckerFixture =
        withFile(COURSE_FILE, contentJson.encodeToString(edit(contentJson.decodeFromString<CourseFile>(texts.getValue(COURSE_FILE)))))

    /** Changes the fixture's one Level. */
    fun withLevel(edit: (LevelEntry) -> LevelEntry): CheckerFixture =
        withIndex { it.copy(levels = it.levels.map(edit)) }

    /** Changes the fixture's one Unit. */
    fun withUnit(edit: (UnitEntry) -> UnitEntry): CheckerFixture =
        withLevel { it.copy(units = it.units.map(edit)) }

    fun withDailySet(path: String, edit: (DailySet) -> DailySet): CheckerFixture =
        withFile(path, contentJson.encodeToString(edit(contentJson.decodeFromString<DailySet>(texts.getValue(path)))))

    /** Changes the Exercise with this id, wherever it is. */
    fun withExercise(id: String, edit: (Exercise) -> Exercise): CheckerFixture =
        listOf(DAY_1, DAY_2).fold(this) { fixture, path ->
            fixture.withDailySet(path) { day -> day.copy(exercises = day.exercises.map { if (it.id == id) edit(it) else it }) }
        }

    /** Adds [dailySet] as a new file at [path], listed last in the Unit. */
    fun withNewDailySet(path: String, dailySet: DailySet): CheckerFixture =
        withFile(path, contentJson.encodeToString(dailySet)).withUnit { it.copy(dailySets = it.dailySets + path) }

    fun withItems(edit: (List<Item>) -> List<Item>): CheckerFixture =
        withFile(ITEMS, contentJson.encodeToString(ItemsFile(edit(contentJson.decodeFromString<ItemsFile>(texts.getValue(ITEMS)).items))))

    fun withClips(edit: (List<Clip>) -> List<Clip>): CheckerFixture =
        withFile(CLIPS, contentJson.encodeToString(ClipsFile(edit(contentJson.decodeFromString<ClipsFile>(texts.getValue(CLIPS)).clips))))

    companion object {
        const val DAY_1 = "a1/unit-01/day-01.json"
        const val DAY_2 = "a1/unit-01/day-02.json"
        const val DAY_3 = "a1/unit-01/day-03.json"
        const val ITEMS = "a1/unit-01/items.json"
        const val CLIPS = "audio/clips.json"

        /** Day 1 of [valid] has the fewest Exercises a Daily Set may have. */
        val EXERCISES_ON_DAY_1 = DAILY_SET_EXERCISES.first

        /** Day 2 of [valid] has one Exercise to spare, so a test can take its last one away. */
        val EXERCISES_ON_DAY_2 = DAILY_SET_EXERCISES.first + 1

        /** The id of the last Exercise of day 2 of [valid]. */
        val LAST_OF_DAY_2 = "a1-u01-d02-e%02d".format(EXERCISES_ON_DAY_2)

        /** How many Items day 1 of [valid] introduces. */
        const val NEW_ITEMS_ON_DAY_1 = 7

        private fun choice(id: String, prompt: String, right: String, wrong: String, vararg items: String) =
            ChooseTranslation(id, prompt, listOf(en(right), en(wrong)), correctOption = 0, items = items.toList())

        /**
         * Choice Exercises numbered [from] to [to]. They only make a day long enough: each practises
         * "tak", which already has a Clip and enough Exercises.
         */
        private fun padding(dailySet: String, from: Int, to: Int): List<Exercise> =
            (from..to).map { number -> choice("$dailySet-e%02d".format(number), "tak", "yes", "no", "tak") }

        private val dayOne = DailySet(
            id = "a1-u01-d01",
            tips = listOf(Tip("a1-u01-d01-t01", before = "a1-u01-d01-e01", title = en("Saying hello"), body = en("Cześć is informal."))),
            exercises = listOf(
                choice("a1-u01-d01-e01", "cześć", "hi", "thank you", "czesc"),
                TypeAnswer("a1-u01-d01-e02", en("thank you"), listOf("dziękuję"), listOf("dziekuje")),
                WordTiles("a1-u01-d01-e03", en("good morning"), listOf("dzień dobry"), listOf("tak"), listOf("dzien-dobry")),
                Conjugation(
                    id = "a1-u01-d01-e04",
                    verb = "być",
                    meaning = en("to be"),
                    person = Person.I,
                    sentence = "Ja ___ w domu.",
                    sentenceMeaning = en("I am at home."),
                    acceptedAnswers = listOf("jestem"),
                    items = listOf("byc-jestem"),
                ),
                ListenChoose("a1-u01-d01-e05", "tak", en("yes"), listOf("nie"), listOf("tak")),
                ListenType("a1-u01-d01-e06", listOf("nie"), en("no"), items = listOf("nie")),
                choice("a1-u01-d01-e07", "jestem", "I am", "you are", "prosze", "byc-jestem"),
            ) + padding("a1-u01-d01", from = 8, to = EXERCISES_ON_DAY_1),
        )

        private val dayTwo = DailySet(
            id = "a1-u01-d02",
            tips = listOf(Tip("a1-u01-d02-t01", before = "a1-u01-d02-e02", title = en("Yes and no"), body = en("Tak is yes."))),
            exercises = listOf(
                choice("a1-u01-d02-e01", "tak", "yes", "no", "tak"),
                choice("a1-u01-d02-e02", "nie", "no", "yes", "nie"),
                TypeAnswer("a1-u01-d02-e03", en("hi"), listOf("cześć"), listOf("czesc")),
                ListenType("a1-u01-d02-e04", listOf("dziękuję"), en("thank you (to anyone)"), items = listOf("dziekuje")),
                ListenChoose("a1-u01-d02-e05", "dzień dobry", en("good morning"), listOf("cześć"), listOf("dzien-dobry")),
                Conjugation(
                    id = "a1-u01-d02-e06",
                    verb = "być",
                    meaning = en("to be"),
                    person = Person.I,
                    acceptedAnswers = listOf("jestem"),
                    items = listOf("byc-jestem"),
                ),
                // Spare Exercises: any one can go, and the Item "prosze" with them, without breaking another rule.
                choice("a1-u01-d02-e07", "proszę", "please", "sorry", "prosze", "tak"),
                choice("a1-u01-d02-e08", "proszę", "please", "thanks", "prosze", "nie"),
            ) + padding("a1-u01-d02", from = 9, to = EXERCISES_ON_DAY_2),
        )

        private val items = listOf(
            Item("czesc", "cześć", en("hi")),
            Item("dziekuje", "dziękuję", en("thank you")),
            Item("dzien-dobry", "dzień dobry", en("good morning")),
            Item("byc-jestem", "jestem", en("I am")),
            Item("tak", "tak", en("yes")),
            Item("nie", "nie", en("no")),
            Item("prosze", "proszę", en("please")),
        )

        private val clips =
            listOf("cześć", "dziękuję", "dzień dobry", "Ja jestem w domu.", "tak", "nie", "jestem", "proszę")
                .mapIndexed { index, text -> Clip("c${index + 1}", text) }

        val valid = CheckerFixture(
            mapOf(
                COURSE_FILE to contentJson.encodeToString(
                    CourseFile(
                        levels = listOf(
                            LevelEntry("a1", en("A1"), listOf(UnitEntry("a1-u01", en("Greetings"), listOf(DAY_1, DAY_2)))),
                        ),
                        items = listOf(ITEMS),
                        audio = CLIPS,
                    ),
                ),
                DAY_1 to contentJson.encodeToString(dayOne),
                DAY_2 to contentJson.encodeToString(dayTwo),
                ITEMS to contentJson.encodeToString(ItemsFile(items)),
                CLIPS to contentJson.encodeToString(ClipsFile(clips)),
            ) + clips.associate { "audio/${it.id}.ogg" to "OggS" },
        )

        /** Every identifier of [valid], as if it had shipped. */
        val shipped: ReleasedIds = identifiersOf(valid.files())

        /** A Daily Set of a Tip and [exercises] choice Exercises that all practise "tak", which already has a Clip. */
        fun fillerDay(exercises: Int): DailySet = DailySet(
            id = "a1-u01-d03",
            tips = listOf(Tip("a1-u01-d03-t01", before = "a1-u01-d03-e01", title = en("Yes"), body = en("Tak is yes."))),
            exercises = (1..exercises).map { number ->
                choice("a1-u01-d03-e%02d".format(number), "tak", "yes", "no", "tak")
            },
        )
    }
}
