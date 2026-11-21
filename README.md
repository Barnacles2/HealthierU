# HealthierU

An Android app that helps users track daily health habits (steps, water intake and calories) and shows whether they are meeting their wellness goals.

BBIT 3.2 Mobile Application Development, CAT 2
Health and Wellness for All


## Problem
Many people struggle to monitor their health consistently because they lack a simple tool to track daily habits like exercise, diet and water intake. This leads to poor awareness of their health status and inconsistent follow-up on fitness goals.

## Solution
HealthierU lets users record daily health data, calculates a daily health score against set goals, and shows a status of **Healthy** or **Needs Improvement**.

## Target users
- Students and young professionals
- People starting a fitness journey
- Anyone who wants to improve their daily health habits

## Team
152568 Kimberly Oware
168010 Vicky Kanyama
193490 Allen Murimi


## Features
- Create a profile (name, age)
- Add, view, edit and delete daily health records (CRUD)
- Automatic daily score and status
- Data persists after the app is closed (Room)
- Daily reminder notification (WorkManager)
- Input validation and error messages

## Architecture
UI (Fragments) → ViewModel → Repository → Room Database

### Project creation
- Created in Android Studio as an Empty Views Activity, Kotlin, minimum SDK 24.
- Package name `com.example.healthieru`, app name HealthierU.
- Uses Android Gradle Plugin 9.4.1, which has built-in Kotlin support, so no separate Kotlin plugin is applied.


