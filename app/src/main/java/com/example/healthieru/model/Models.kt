package com.example.healthieru.model

import com.example.healthieru.data.local.DoctorNote
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))

interface Person {
    val name: String
    val email: String
    val phone: String
    fun contactLine() = "$name, $email, $phone"
}

class HealthProfessional(
    override val name: String,
    override val email: String,
    override val phone: String,
    private val password: String,
    val specialty: String
) : Person {
    fun checkLogin(email: String, password: String) =
        this.email.equals(email.trim(), ignoreCase = true) && this.password == password

    fun writeNote(patientId: Long, category: String, text: String, structuredData: String? = null) = DoctorNote(
        userId = patientId, author = name, category = category,
        text = text.trim(), date = System.currentTimeMillis(),
        structuredData = structuredData
    )
}

object Doctor {
    val account = HealthProfessional(
        "Dr. Amina Otieno", "doctor@clinic.com", "0700000000", "doctor123", "General practitioner"
    )
}

object NoteCategory {
    const val EXERCISE = "Exercise"
    const val DIET = "Diet"
    const val SLEEP = "Sleep"
    const val GENERAL = "General"
    val all = listOf(EXERCISE, DIET, SLEEP, GENERAL)
}

object Validator {
    fun checkMeasurements(weight: Double?, height: Double?, heartRate: Int?): String? {
        if (weight == null || height == null || heartRate == null) return "Enter a number in every field."
        return when {
            weight !in 20.0..300.0 -> "Weight must be between 20 and 300 kg."
            height !in 50.0..250.0 -> "Height must be between 50 and 250 cm."
            heartRate !in 30..220 -> "Heart rate must be between 30 and 220 bpm."
            else -> null
        }
    }

    fun checkLifestyle(exercise: Int?, fruitVeg: Int?, sleep: Double?): String? {
        if (exercise == null || fruitVeg == null || sleep == null) return "Enter a number in every field."
        return when {
            exercise !in 0..600 -> "Exercise must be between 0 and 600 minutes."
            fruitVeg !in 0..30 -> "Servings must be between 0 and 30."
            sleep !in 0.0..24.0 -> "Sleep must be between 0 and 24 hours."
            else -> null
        }
    }

    fun checkProfile(name: String, phone: String, age: Int?, goal: String): String? = when {
        name.isBlank() || phone.isBlank() || goal.isBlank() -> "Name, phone number and goal cannot be empty."
        age == null || age !in 1..120 -> "Enter an age between 1 and 120."
        else -> null
    }
}

abstract class HealthData {
    abstract val category: String
    abstract fun generateRecommendation(): String
}

class ExerciseData(private val minutesPerDay: Int) : HealthData() {
    override val category = NoteCategory.EXERCISE
    override fun generateRecommendation() =
        if (minutesPerDay < 30) "Increase daily walking to at least 30 minutes."
        else "Great job. Keep up your exercise routine."
}

class DietData(private val fruitVegServings: Int) : HealthData() {
    override val category = NoteCategory.DIET
    override fun generateRecommendation() =
        if (fruitVegServings < 5) "Increase fruit and vegetable intake to 5 servings a day."
        else "Your fruit and vegetable intake is on target."
}

class SleepData(private val hoursPerNight: Double) : HealthData() {
    override val category = NoteCategory.SLEEP
    override fun generateRecommendation() =
        if (hoursPerNight !in 7.0..9.0) "Aim for 7 to 9 hours of sleep each night."
        else "Your sleep duration is healthy."
}

data class HealthReport(
    val patientName: String,
    val bmi: Double,
    val bmiCategory: String,
    val heartRate: Int,
    val heartStatus: String,
    val activityLevel: String,
    val goal: String,
    val recommendations: List<Pair<String, String>>
) {
    fun toText() = buildString {
        appendLine("Patient: $patientName")
        appendLine("Goal: $goal")
        appendLine("BMI: ${"%.1f".format(bmi)} ($bmiCategory)")
        appendLine("Heart rate: $heartRate bpm ($heartStatus)")
        appendLine("Activity level: $activityLevel")
        appendLine()
        recommendations.forEach { appendLine("${it.first}: ${it.second}") }
    }
}

object HealthReportGenerator {
    fun generate(user: User, record: HealthRecord): HealthReport {
        val bmi = bmi(record.weightKg, record.heightCm)
        val data: List<HealthData> = listOf(
            ExerciseData(record.exerciseMinutes), DietData(record.fruitVeg), SleepData(record.sleepHours)
        )
        return HealthReport(
            patientName = user.name,
            bmi = bmi,
            bmiCategory = categorizeBmi(bmi),
            heartRate = record.heartRate,
            heartStatus = categorizeHeartRate(record.heartRate),
            activityLevel = record.activityLevel,
            goal = user.healthGoal,
            recommendations = data.map { it.category to it.generateRecommendation() }
        )
    }

    fun bmi(weightKg: Double, heightCm: Double): Double {
        val m = heightCm / 100.0
        return if (m <= 0) 0.0 else weightKg / (m * m)
    }

    fun categorizeBmi(bmi: Double) = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }

    private fun categorizeHeartRate(bpm: Int) = when {
        bpm < 60 -> "Below resting range"
        bpm <= 100 -> "Normal resting range"
        else -> "Above resting range"
    }
}

data class TrendSummary(val start: Double, val latest: Double, val change: Double)

object ProgressAnalyzer {
    fun trend(values: List<Double>): TrendSummary? =
        if (values.size < 2) null
        else TrendSummary(values.first(), values.last(), values.last() - values.first())

    fun describeChange(change: Double, unit: String, decimals: Int = 1): String {
        val amount = "%.${decimals}f".format(abs(change))
        return when {
            abs(change) < 0.1 -> "No change"
            change < 0 -> "Down $amount $unit"
            else -> "Up $amount $unit"
        }
    }
}