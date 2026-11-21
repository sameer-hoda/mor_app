package dev.mor.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Issue 20: the live voice text must contain each spoken word once, and the
 * same text is what the lookup receives. These tests drive the pure merge rule
 * with both vendor styles (cumulative and incremental partials).
 */
class MergeVoiceTest {

    private class Rec {
        var settled = ""
        var partial = ""
        fun partial(text: String) {
            val r = mergeVoiceText(settled, partial, text, false)
            settled = r.first
            partial = r.second
        }
        fun final(text: String) {
            val r = mergeVoiceText(settled, partial, text, true)
            settled = r.first
            partial = r.second
        }
        fun display() = listOf(settled, partial).filter { it.isNotBlank() }
            .joinToString(" ").trim()
    }

    @Test
    fun cumulativePartialsAreReplacedNotAppended() {
        val r = Rec()
        r.partial("paneer")
        r.partial("paneer butter")
        r.partial("paneer butter masala")
        assertEquals("paneer butter masala", r.display())
        r.final("paneer butter masala rice eggs and water")
        assertEquals("paneer butter masala rice eggs and water", r.display())
        assertEquals(1, Regex("paneer").findAll(r.display()).count())
        assertEquals(1, Regex("rice").findAll(r.display()).count())
        assertEquals(1, Regex("eggs").findAll(r.display()).count())
    }

    @Test
    fun incrementalPartialsAreJoined() {
        val r = Rec()
        r.partial("chicken")
        r.partial("rogan josh")
        r.partial("biryani and lassi")
        assertEquals("chicken rogan josh biryani and lassi", r.display())
        r.final("chicken rogan josh biryani and lassi")
        assertEquals("chicken rogan josh biryani and lassi", r.display())
        assertEquals(1, Regex("lassi").findAll(r.display()).count())
    }

    @Test
    fun repeatedFinalIsNotDuplicated() {
        val r = Rec()
        r.partial("rasmalai")
        r.final("rasmalai")
        r.final("rasmalai")
        assertEquals("rasmalai", r.display())
        assertEquals(1, Regex("rasmalai").findAll(r.display()).count())
    }

    @Test
    fun blankTextIsIgnored() {
        val r = Rec()
        r.partial("paneer")
        r.partial("   ")
        assertEquals("paneer", r.display())
    }
}
