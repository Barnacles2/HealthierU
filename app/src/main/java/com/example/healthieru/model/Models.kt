package com.example.healthieru.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private fun dateLabel(daysAgo: Int = 0): String {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return SimpleDateFormat("d MMM", Locale.getDefault()).format(c.time)
}

/* INHERITANCE: common properties live in Person */
open class Person(
    val name: String,
    val email: String,
    val phone: String,
    private val password: String
) {
    fun checkLogin(email: String, password: String) =
        this.email.equals(email.trim(), ignoreCase = true) && this.password == password
}

/* Categories used for both automatic recommendations and doctor advice */
object NoteCategory {
    const val EXERCISE = "Exercise"
    const val DIET = "Diet"
    const val SLEEP = "Sleep"
    const val GENERAL = "General"
    val all = listOf(EXERCISE, DIET, SLEEP, GENERAL)
}

data class DoctorNote(val author: String, val category: String, val text: String, val date: String)

data class HealthEntry(val label: String, val weightKg: Double, val heartRate: Int)

/* ENCAPSULATION: private health data + validated update methods */
class Patient(
    name: String, email: String, phone: String, password: String,
    val age: Int,
    var healthGoal: String
) : Person(name, email, phone, password) {

    var activityLevel: String = "Moderate"
        private set

    private var weightKg = 0.0
    private var heightCm = 0.0
    private var heartRateBpm = 0
    private var exerciseMinutes = 0
    private var fruitVegServings = 0
    private var sleepHours = 0.0

    // Doctor feedback
    private val doctorNotes = mutableListOf<DoctorNote>()
    var followUpStatus: String = "Not reviewed"
        private set

    // Progress history (one entry per day)
    private val history = mutableListOf<HealthEntry>()

    fun addDoctorNote(author: String, category: String, advice: String) {
        if (advice.isNotBlank()) {
            doctorNotes.add(0, DoctorNote(author, category, advice.trim(), dateLabel()))
        }
    }

    fun updateFollowUpStatus(status: String) { followUpStatus = status }

    fun getDoctorNotes(category: String? = null): List<DoctorNote> =
        if (category == null) doctorNotes.toList() else doctorNotes.filter { it.category == category }

    /** Saves today's weight and heart rate. A second save on the same day replaces the first. */
    fun logCurrentEntry() {
        val entry = HealthEntry(dateLabel(), weightKg, heartRateBpm)
        if (history.isNotEmpty() && history.last().label == entry.label) {
            history[history.lastIndex] = entry
        } else history.add(entry)
    }

    /** Used for demo or imported data. */
    fun addHistoryEntry(daysAgo: Int, weight: Double, heartRate: Int) {
        if (weight in 20.0..300.0 && heartRate in 30..220) {
            history.add(HealthEntry(dateLabel(daysAgo), weight, heartRate))
        }
    }

    fun getHistory(): List<HealthEntry> = history.toList()

    fun updateWeight(v: Double): Result<Unit> =
        if (v in 20.0..300.0) { weightKg = v; Result.success(Unit) }
        else Result.failure(IllegalArgumentException("Weight must be between 20 and 300 kg."))

    fun updateHeight(v: Double): Result<Unit> =
        if (v in 50.0..250.0) { heightCm = v; Result.success(Unit) }
        else Result.failure(IllegalArgumentException("Height must be between 50 and 250 cm."))

    fun recordHeartRate(v: Int): Result<Unit> =
        if (v in 30..220) { heartRateBpm = v; Result.success(Unit) }
        else Result.failure(IllegalArgumentException("Heart rate must be between 30 and 220 bpm."))

    fun updateActivityLevel(level: String) { activityLevel = level }

    fun recordLifestyle(exercise: Int, fruitVeg: Int, sleep: Double): Result<Unit> = when {
        exercise !in 0..600 -> Result.failure(IllegalArgumentException("Exercise must be between 0 and 600 minutes."))
        fruitVeg !in 0..30 -> Result.failure(IllegalArgumentException("Servings must be between 0 and 30."))
        sleep !in 0.0..24.0 -> Result.failure(IllegalArgumentException("Sleep must be between 0 and 24 hours."))
        else -> { exerciseMinutes = exercise; fruitVegServings = fruitVeg; sleepHours = sleep; Result.success(Unit) }
    }

    fun getWeight() = weightKg
    fun getHeight() = heightCm
    fun getHeartRate() = heartRateBpm
    fun hasRecords() = weightKg > 0 && heightCm > 0 && heartRateBpm > 0

    /* POLYMORPHISM: a list of different HealthData objects */
    fun getHealthData(): List<HealthData> = listOf(
        ExerciseData(exerciseMinutes), DietData(fruitVegServings), SleepData(sleepHours)
    )
}

/* INHERITANCE: second child of Person */
class HealthProfessional(
    name: String, email: String, phone: String, password: String,
    val specialty: String
) : Person(name, email, phone, password) {

    // The professional's own responsibility: review a patient and respond
    fun reviewPatient(patient: Patient, status: String, category: String, advice: String) {
        patient.updateFollowUpStatus(status)
        patient.addDoctorNote(name, category, advice)
    }
}

/* POLYMORPHISM: same method, different behaviour */
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

/* ABSTRACTION: screens just call generate(); the maths is hidden */
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
    fun generate(patient: Patient): HealthReport {
        val bmi = calculateBmi(patient)
        return HealthReport(
            patientName = patient.name,
            bmi = bmi,
            bmiCategory = categorizeBmi(bmi),
            heartRate = patient.getHeartRate(),
            heartStatus = categorizeHeartRate(patient.getHeartRate()),
            activityLevel = patient.activityLevel,
            goal = patient.healthGoal,
            recommendations = patient.getHealthData().map { it.category to it.generateRecommendation() }
        )
    }

    private fun calculateBmi(p: Patient): Double {
        val m = p.getHeight() / 100.0
        return p.getWeight() / (m * m)
    }

    private fun categorizeBmi(bmi: Double) = when {
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

/* ABSTRACTION: progress maths hidden from the screens */
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

/* Simple in-memory storage shared by all screens */
object AppData {
    val professional = HealthProfessional(
        "Dr. Amina Otieno", "doctor@clinic.com", "0700000000", "doctor123", "General practitioner"
    )
    val patients = mutableListOf<Patient>()
    var currentUser: Patient? = null

    init {
        val demo = Patient("Wanjiru Kamau", "wanjiru@mail.com", "0711111111", "1234", 28, "Lose 5 kg")
        // Past entries so the progress chart has something to show
        demo.addHistoryEntry(28, 76.0, 84)
        demo.addHistoryEntry(21, 75.2, 82)
        demo.addHistoryEntry(14, 74.1, 81)
        demo.addHistoryEntry(7, 73.0, 79)
        demo.updateWeight(72.0); demo.updateHeight(165.0); demo.recordHeartRate(78)
        demo.recordLifestyle(20, 3, 6.0)
        demo.logCurrentEntry()
        patients.add(demo)
    }
}