package com.polisheasy.audio

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ClipPlanTest {
    @Test
    fun `a new Clip's id is its text in plain lower-case letters, joined by hyphens`() {
        assertEquals("dzien-dobry", newClipId("Dzień dobry!", emptySet()))
        assertEquals("zolc-gesla-jazn", newClipId("Żółć, gęślą jaźń", emptySet()))
        assertEquals("clip", newClipId("?!", emptySet()))
        assertEquals("clip-2", newClipId("...", setOf("clip")))
    }

    @Test
    fun `a long text's id stops at 48 characters and never ends in a hyphen`() {
        val id = newClipId("a".repeat(47) + " " + "b".repeat(10), emptySet())

        assertEquals("a".repeat(47), id)
    }
}
