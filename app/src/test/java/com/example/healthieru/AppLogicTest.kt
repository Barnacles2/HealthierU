package com.example.healthieru

import com.example.healthieru.data.remote.AdviceResponse
import com.example.healthieru.model.ProgressAnalyzer
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppLogicTest {

    @Test
    fun trend_withTwoValues_givesStartLatestAndChange() {
        val t = ProgressAnalyzer.trend(listOf(80.0, 78.0))
        assertNotNull(t)
        assertEquals(80.0, t!!.start, 0.001)
        assertEquals(78.0, t.latest, 0.001)
        assertEquals(-2.0, t.change, 0.001)
    }

    @Test
    fun trend_withOneValue_returnsNull() {
        assertNull(ProgressAnalyzer.trend(listOf(80.0)))
    }

    @Test
    fun adviceJson_parsesCorrectly() {
        val json = """{"slip":{"id":5,"advice":"Drink water."}}"""
        val result = Gson().fromJson(json, AdviceResponse::class.java)
        assertEquals("Drink water.", result.slip.advice)
    }
}