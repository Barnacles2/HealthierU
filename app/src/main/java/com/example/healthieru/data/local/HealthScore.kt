package com.example.healthieru.data.local

object HealthScore {

    fun calculateScore(record: HealthRecord): Int {
        var score = 0

        // heartRate: up to 40 points (Normal resting is 60-100 bpm)
        if (record.heartRate in 60..100) {
            score += 40
        } else if (record.heartRate in 50..110) {
            score += 20
        }

        // weight: up to 30 points (Just a placeholder check for healthy weight)
        // You might want to check BMI instead, but for now, let's give points if weight is recorded
        if (record.weightKg > 0) {
            score += 30
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