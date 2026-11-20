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
    name: String,
    val email: String,
    phone: String,
    private val password: String
) {
    var name: String = name
        private set
    var phone: String = phone
        private set

    fun checkLogin(email: String, password: String) =
        this.email.equals(email.trim(), ignoreCase = true) && this.password == password

    protected fun changeContact(newName: String, newPhone: String) {
        name = newName
        phone = newPhone
    }
}

/* Categories used for both automatic recommendations and doctor advice */
object NoteCategory {
    const val EXERCISE = "Exercise"
    const val DIET = "Diet"
    const val SLEEP = "Sleep"
    const val GENERAL = "General"
    val all = listOf(EXERCISE, DIET, SLEEP, GENERAL)
}

data class DoctorNote(val id: Long, val author: String, val category: String, val text: String, val date: String)

data class HealthEntry(val id: Long, val label: String, val weightKg: Double, val heartRate: Int)

/* ENCAPSULATION: private health data + validated CRUD methods */
class Patient(
    name: String, email: String, phone: String, password: String,
    age: Int,
    healthGoal: String
) : Person(name, email, phone, password) {

    var age: Int = age
        private set
    var healthGoal: String = healthGoal
        private set
    var activityLevel: String = "Moderate"
        private set

    // FIXED: Removed 'private' so the database can read these values
    var weightKg = 0.0
    var heightCm = 0.0
    var heartRateBpm = 0
    private var exerciseMinutes = 0
    private var fruitVegServings = 0
    private var sleepHours = 0.0

    private val doctorNotes = mutableListOf<DoctorNote>()
    var followUpStatus: String = "Not reviewed"
        private set

    private val history = mutableListOf<HealthEntry>()
    private var nextId = 1L

    /* ---------- PROFILE: update ---------- */
    fun updateProfile(newName: String, newPhone: String, newAge: Int, newGoal: String): Result<Unit> = when {
        newName.isBlank() || newPhone.isBlank() || newGoal.isBlank() ->
            Result.failure(IllegalArgumentException("Name, phone number and goal cannot be empty."))
        newAge !in 1..120 ->
            Result.failure(IllegalArgumentException("Enter an age between 1 and 120."))
        else -> {
            changeContact(newName.trim(), newPhone.trim())
            age = newAge
            healthGoal = newGoal.trim()
            Result.success(Unit)
        }
    }

    /* ---------- DOCTOR NOTES: create, read, update, delete ---------- */
    fun addDoctorNote(author: String, category: String, advice: String) {
        if (advice.isNotBlank()) {
            doctorNotes.add(0, DoctorNote(nextId++, author, category, advice.trim(), dateLabel()))
        }
    }

    fun getDoctorNotes(category: String? = null): List<DoctorNote> =
        if (category == null) doctorNotes.toList() else doctorNotes.filter { it.category == category }

    fun updateDoctorNote(id: Long, newText: String): Result<Unit> {
        val i = doctorNotes.indexOfFirst { it.id == id }
        return when {
            i < 0 -> Result.failure(IllegalArgumentException("That note no longer exists."))
            newText.isBlank() -> Result.failure(IllegalArgumentException("Advice cannot be empty."))
            else -> { doctorNotes[i] = doctorNotes[i].copy(text = newText.trim()); Result.success(Unit) }
        }
    }

    fun deleteDoctorNote(id: Long): Boolean = doctorNotes.removeAll { it.id == id }

    fun updateFollowUpStatus(status: String) { followUpStatus = status }

    /* ---------- HEALTH HISTORY: create, read, update, delete ---------- */
    /** Saves today's weight and heart rate. A second save on the same day replaces the first. */
    fun logCurrentEntry() {
        val label = dateLabel()
        if (history.isNotEmpty() && history.last().label == label) {
            history[history.lastIndex] = history.last().copy(weightKg = weightKg, heartRate = heartRateBpm)
        } else {
            history.add(HealthEntry(nextId++, label, weightKg, heartRateBpm))
        }
    }

    /** Used for demo or imported data. */
    fun addHistoryEntry(daysAgo: Int, weight: Double, heartRate: Int) {
        if (weight in 20.0..300.0 && heartRate in 30..220) {
            history.add(HealthEntry(nextId++, dateLabel(daysAgo), weight, heartRate))
        }
    }

    fun getHistory(): List<HealthEntry> = history.toList()

    fun updateHistoryEntry(id: Long, weight: Double, heartRate: Int): Result<Unit> {
        val i = history.indexOfFirst { it.id == id }
        return when {
            i < 0 -> Result.failure(IllegalArgumentException("That entry no longer exists."))
            weight !in 20.0..300.0 -> Result.failure(IllegalArgumentException("Weight must be between 20 and 300 kg."))
            heartRate !in 30..220 -> Result.failure(IllegalArgumentException("Heart rate must be between 30 and 220 bpm."))
            else -> {
                history[i] = history[i].copy(weightKg = weight, heartRate = heartRate)
                syncCurrentFromHistory()
                Result.success(Unit)
            }
        }
    }

    fun deleteHistoryEntry(id: Long): Boolean {
        val removed = history.removeAll { it.id == id }
        if (removed) syncCurrentFromHistory()
        return removed
    }

    // The current weight and heart rate always match the latest history entry
    private fun syncCurrentFromHistory() {
        val last = history.lastOrNull()
        weightKg = last?.weightKg ?: 0.0
        heartRateBpm = last?.heartRate ?: 0
    }

    /* ---------- CURRENT MEASUREMENTS ---------- */
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

    // FIXED: These getters still work perfectly
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

    fun reviewPatient(patient: Patient, status: String, category: String, advice: String) {
        patient.updateFollowUpStatus(status)
        patient.addDoctorNote(name, category, advice)
    }

    fun editNote(patient: Patient, noteId: Long, newText: String) = patient.updateDoctorNote(noteId, newText)

    fun deleteNote(patient: Patient, noteId: Long) = patient.deleteDoctorNote(noteId)
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

    /** DELETE a patient account */
    fun deletePatient(patient: Patient) {
        patients.remove(patient)
        if (currentUser == patient) currentUser = null
    }

    init {
        val demo = Patient("Wanjiru Kamau", "wanjiru@mail.com", "0711111111", "1234", 28, "Lose 5 kg")
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