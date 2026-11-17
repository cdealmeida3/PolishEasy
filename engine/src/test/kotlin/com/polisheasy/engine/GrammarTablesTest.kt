package com.polisheasy.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The book's grammar tables under a Tip, its Uwaga! and Zapamiętaj! callouts, and the Grammar shelf that keeps the tables. */
class GrammarTablesTest {
    private fun en(text: String): Localized = mapOf("en" to text)

    private val genders = GrammarTable(
        title = en("A man and a woman"),
        columns = listOf(TableColumn("on", TableGender.MASCULINE), TableColumn("ona", TableGender.FEMININE)),
        rows = listOf(TableRow(cells = listOf("student", "student[ka]")), TableRow(cells = listOf("nauczyciel", "nauczyciel[ka]"))),
    )

    private val cases = GrammarTable(
        title = en("Who? What?"),
        columns = listOf(TableColumn("l.poj."), TableColumn("l.mn.")),
        rows = listOf(TableRow(GrammarCase.NOMINATIVE, cells = listOf("student", "studen[ci]"))),
    )

    private fun tip(id: String, before: String, table: GrammarTable? = null, kind: TipKind = TipKind.NOTE) =
        Tip(id, before, en("Title $id"), en("Body of $id."), table = table, kind = kind)

    private val first = DailySet(
        "a1-u01-d01",
        tips = listOf(tip("a1-u01-d01-t01", "a1-u01-d01-e01", genders), tip("a1-u01-d01-t02", "a1-u01-d01-e02", kind = TipKind.UWAGA)),
        exercises = listOf(choose("a1-u01-d01-e01", "a", "a"), choose("a1-u01-d01-e02", "b", "b")),
    )
    private val second = DailySet(
        "a1-u01-d02",
        tips = listOf(tip("a1-u01-d02-t01", "a1-u01-d02-e01", cases)),
        exercises = listOf(choose("a1-u01-d02-e01", "c", "c")),
    )
    private val course = courseOf(listOf(item("a"), item("b"), item("c")), first, second)

    @Test
    fun `an ending in square brackets is read out of the cell, to be shown in a red box`() {
        assertEquals(TableCellView("studentka", 7..8), readCell("student[ka]"))
        assertEquals(TableCellView("student"), readCell("student"))
        assertEquals(TableCellView("studenci", 6..7), readCell("studen[ci]"))
    }

    @Test
    fun `a Tip shows its table with gender icons over the columns, the endings marked, and a case row with its question`() {
        val engine = CourseEngine(course, InMemoryLearnerProgress(), FakeClock())
        val tip = (engine.startDailySet(dailySetId = "a1-u01-d02") as Step.ShowTip).tip
        val table = checkNotNull(tip.table)

        assertEquals("Who? What?", table.title)
        assertEquals(listOf("l.poj.", "l.mn."), table.columns.map { it.header })
        val row = table.rows.single()
        assertEquals("M.", row.header)
        assertEquals("kto? co?", row.question)
        assertEquals(listOf(TableCellView("student"), TableCellView("studenci", 6..7)), row.cells)

        val genderTip = (CourseEngine(course, InMemoryLearnerProgress(), FakeClock()).startDailySet() as Step.ShowTip).tip
        assertEquals(listOf(TableGender.MASCULINE, TableGender.FEMININE), genderTip.table?.columns?.map { it.gender })
    }

    @Test
    fun `an Uwaga or Zapamiętaj Tip says which it is`() {
        val engine = CourseEngine(course, InMemoryLearnerProgress(), FakeClock())
        engine.startDailySet()
        engine.next()
        engine.answer(Answer.Option(0))
        assertEquals(TipKind.UWAGA, (engine.next() as Step.ShowTip).tip.kind)
    }

    @Test
    fun `a verb's forms are shown as a table of one kind with the rest, a row a person`() {
        val conjugation = ConjugationTable("być", en("to be"), Person.entries.associateWith { "form-${it.name}" })
        val withVerb = courseOf(
            listOf(item("a")),
            DailySet("a1-u01-d01", listOf(Tip("a1-u01-d01-t01", "a1-u01-d01-e01", en("T"), en("B."), conjugation)), listOf(choose("a1-u01-d01-e01", "a", "a"))),
        )
        val table = checkNotNull((CourseEngine(withVerb, InMemoryLearnerProgress(), FakeClock()).startDailySet() as Step.ShowTip).tip.table)

        assertEquals("być", table.verb)
        assertEquals("być · to be", table.title)
        assertEquals(Person.entries.map { it.pronouns }, table.rows.map { it.header })
        assertEquals("form-I", table.rows.first().cells.single().text)
    }

    @Test
    fun `the Grammar shelf keeps the tables of the sections completed, by chapter, and says how many are to come`() {
        val learner = ItemLearner(course, oneADay = false)
        val engine = learner.openApp()
        assertEquals(emptyList(), engine.grammar())
        assertEquals(listOf(0 to 2), engine.grammar("a1-u01").map { it.tips.size to it.toCome })
        assertEquals(0 to 2, engine.lesson("a1-u01").let { it.grammarDone to it.grammarTotal })

        learner.play(engine, engine.startDailySet())
        val shelf = engine.grammar().single()
        assertEquals("a1-u01", shelf.unitId)
        assertEquals(listOf("Title a1-u01-d01-t01"), shelf.tips.map { it.title }, "Only Tips with a table, the Uwaga! is not one")
        assertEquals(1, shelf.toCome)
        assertEquals(1 to 2, engine.lesson("a1-u01").let { it.grammarDone to it.grammarTotal })
    }

    @Test
    fun `the loader rejects a table it could not draw`() {
        fun problems(table: GrammarTable) = checkContent(
            CheckerFixture.valid.withDailySet(CheckerFixture.DAY_1) { day -> day.copy(tips = day.tips.map { it.copy(table = table) }) }.files(),
            null,
        ).filter { it.id == "a1-u01-d01-t01" }.map { it.message }

        assertEquals(emptyList(), problems(genders))
        assertTrue(problems(genders.copy(rows = listOf(TableRow(cells = listOf("a")), TableRow(cells = listOf("a", "b"))))).any { "different lengths" in it })
        assertTrue(problems(genders.copy(columns = genders.columns.take(1))).any { "1 columns" in it })
        assertTrue(problems(genders.copy(rows = listOf(TableRow(cells = listOf("stu[dent", "x"))))).any { "one pair of [ ]" in it })
        assertTrue(problems(genders.copy(title = emptyMap())).any { "title of the grammar table" in it })
        assertTrue(problems(genders.copy(columns = listOf(TableColumn(), TableColumn("ona")))).any { "neither a header nor a gender" in it })
    }

    @Test
    fun `the Polish check reads every cell of a table, without its brackets, and the reviewer sees the table`() {
        val files = mapOf(
            "course.json" to contentJson.encodeToString(
                CourseFile(levels = listOf(LevelEntry("a1", en("A1"), listOf(UnitEntry("a1-u01", en("U"), listOf("d.json")))))),
            ),
            "d.json" to contentJson.encodeToString(first),
        )
        val content = polishContent(
            object : ContentFiles {
                override fun read(path: String): String = files.getValue(path)
                override fun paths(): Set<String> = files.keys
            },
        )

        assertTrue(listOf("student", "studentka", "nauczyciel", "nauczycielka").all { word -> content.sentences.any { it.text == word } })
        val tip = content.tips.first { it.id == "a1-u01-d01-t01" }
        assertTrue("student | studentka" in tip.text, tip.text)
        assertTrue("m on | f ona" in tip.text, tip.text)
        assertTrue("UWAGA" in content.tips.first { it.id == "a1-u01-d01-t02" }.text)
    }
}
