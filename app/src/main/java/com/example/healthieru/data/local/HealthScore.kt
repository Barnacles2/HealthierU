package com.example.healthieru.data.local

object HealthScore {

    /** Returns a score from 0 to 100 based on the patient's latest record. */
    fun calculateScore(record: HealthRecord): Int {
        var score = 0

        // Heart rate: up to 25 points (normal resting range is 60 to 100 bpm)
        score += when (record.heartRate) {
            in 60..100 -> 25
            in 50..110 -> 12
            else -> 0
        }

        // Weight: up to 15 points for having a valid recorded weight
        if (record.weightKg > 0) score += 15

        // Exercise: up to 20 points (30 minutes a day or more is the target)
        score += when {
            record.exerciseMinutes >= 30 -> 20
            record.exerciseMinutes >= 15 -> 10
            else -> 0
        }

        // Diet: up to 20 points (5 servings of fruit and vegetables a day)
        score += when {
            record.fruitVeg >= 5 -> 20
            record.fruitVeg >= 3 -> 10
            else -> 0
        }

        // Sleep: up to 20 points (7 to 9 hours a night)
        score += when {
            record.sleepHours in 7.0..9.0 -> 20
            record.sleepHours in 6.0..10.0 -> 10
            else -> 0
        }

        return score
    }

    fun getStatus(score: Int): String =
        if (score >= 70) "Healthy" else "Needs Improvement"
}