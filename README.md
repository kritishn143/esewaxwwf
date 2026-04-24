# eSewa KYC Verification — Android App

An intelligent KYC onboarding solution built for **eSewa** as part of **Challenge 4: Intelligent KYC Experience**.

## 📌 Problem Solved
- Reduces form entry errors via automated OCR data capture
- Provides real-time KYC progress transparency
- Reduces support tickets through a proactive SmartAssist chatbot

## 🚀 Features
| Feature | Technology |
|---|---|
| Automated NID Data Extraction | Google ML Kit (Devanagari OCR) |
| Real-time Form Validation | TextWatcher + Error Guards |
| 4-Step Animated Progress Dashboard | Custom Progress Engine |
| Status-Aware SmartAssist Bot | SharedPreferences + State Machine |
| Server Status Polling | OkHttp Callbacks |
| Premium Slide Navigation | Custom Anim XML |
| Admin Rejection Feedback | Per-field Rejection Highlighting |

## 🛠️ Setup

### 1. Configure API Server
Edit `app/src/main/java/app/kyc/verification/Config.java`:
```java
public static final String BASE_URL = "http://YOUR_SERVER_IP:3001";
```

### 2. Run the Backend
The Node.js admin server is located in the companion repo. Run:
```bash
npm install && node server.js
```

### 3. Build & Run
Open in **Android Studio** and run on a physical device or emulator (API 26+).

## 📁 Project Structure
```
app/
├── java/app/kyc/verification/
│   ├── Config.java                  ← API endpoint config
│   ├── MainActivity.java            ← Home / Notifications
│   ├── KycDashboardActivity.java    ← KYC Progress Dashboard
│   ├── DocumentCaptureActivity.java ← OCR ID Capture
│   ├── SelfieVerificationActivity.java
│   ├── ReviewSubmitActivity.java
│   ├── PersonalInfoActivity.java    ← Manual Data Entry + Validation
│   ├── KycSmartAssistActivity.java  ← AI Chatbot
│   └── ProfileActivity.java
└── res/
    ├── layout/
    ├── anim/                        ← Slide transition animations
    └── drawable/
```

## 📋 Requirements
- Android Studio Hedgehog or later
- Min SDK: 26 (Android 8.0)
- Target SDK: 34

## 🏆 Challenge
This project was built for **eSewa Challenge 4 — Intelligent KYC Experience** at the eSewa Innovation Hackathon.

> *Faster onboarding. Fewer errors. Smarter support.*
