package com.example.healthieru.data.local

object HealthScore {

    fun calculateScore(record: HealthRecord): Int {
        var score = 0

        // steps: up to 40 points
        if (record.steps >= 10000) {
            score += 40
        } else if (record.steps >= 5000) {
            score += 20
        }

        // water: up to 30 points
        if (record.waterLitres >= 2.5) {
            score += 30
        } else if (record.waterLitres >= 1.5) {
            score += 15
        }

        // calories: 30 points if in a healthy range
        if (record.caloriesIntake in 1500..2500) {
            score += 30
        }

        return score
    }

    fun getStatus(score: Int): String {
        return if (score >= 70) "Healthy" else "Needs Improvement"
    }
}