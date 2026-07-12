# Podocare_AI
# 🦶 PodoCare AI
## Distributed AI-Powered Diabetic Foot Monitoring System

PodoCare AI is a multi-device healthcare system designed for **early detection of diabetic foot complications** using wearable sensors, Edge AI, and Snapdragon-powered devices.

The system continuously monitors plantar pressure, foot temperature, blood oxygen level, heart rate, and walking activity to identify early signs of diabetic foot ulcers before severe complications occur.

---

# Problem Statement

According to the International Diabetes Federation (IDF), over **77 million people in India** live with diabetes.

Approximately **15–25%** of diabetic patients develop foot ulcers during their lifetime, and nearly **85% of lower-limb amputations** are preceded by diabetic foot ulcers.

Most complications can be prevented through continuous monitoring and early detection.

Current solutions are expensive, hospital-centric, and unsuitable for continuous home monitoring.

---

# Proposed Solution

PodoCare AI is a wearable intelligent monitoring system that combines:

- Embedded Sensor Network
- Bluetooth Low Energy (BLE)
- Edge AI on Mobile
- AI-assisted Clinical Dashboard

The wearable continuously acquires physiological data and wirelessly transmits it to a smartphone.

The smartphone performs real-time AI inference using an ONNX model to classify the patient's condition as:

- Normal
- Medium Risk
- High Risk

The processed information is forwarded to a local PC dashboard for visualization, history tracking, and clinical analysis.

---

# System Architecture

```
             Wearable Device
          (Arduino UNO Q / ESP32)

      LM35 Temperature Sensor
      FSR402 Pressure Sensor
      MAX30102 Pulse Oximeter
      MPU6050 Motion Sensor
                │
                │ BLE
                ▼
       Snapdragon Android Phone

      ONNX Edge AI Inference
      Risk Prediction Engine

                │ Wi-Fi
                ▼

        Flask Python Server

                │

      Web Dashboard

      Live Monitoring
      Historical Trends
      Risk Alerts
```

---

# Hardware Used

| Component | Purpose |
|------------|----------|
| Arduino UNO Q / ESP32 | Sensor Data Acquisition |
| LM35 | Skin Temperature |
| FSR402 | Plantar Pressure |
| MAX30102 | Heart Rate & SpO₂ |
| MPU6050 | Motion / Fall Detection |

---

# Software Stack

## Embedded

- C++
- Arduino App Lab
- Arduino RouterBridge

---

## Mobile

- Android Studio
- Kotlin
- Bluetooth Low Energy (BLE)
- ONNX Runtime Mobile

---

## AI

- Python
- ONNX Runtime
- Scikit-Learn
- Isolation Forest
- Feature Engineering

---

## Backend

- Python Flask
- REST API
- SQLite Database

---

## Dashboard

- HTML
- CSS
- JavaScript
- Chart.js

---

# Sensor Parameters

| Sensor | Parameter |
|----------|-----------|
| LM35 | Foot Temperature |
| FSR402 | Plantar Pressure |
| MAX30102 | Heart Rate |
| MAX30102 | Blood Oxygen (SpO₂) |
| MPU6050 | Foot Movement |

---

# Data Flow

```
Sensors

↓

Arduino UNO Q

↓

Bluetooth Low Energy (BLE)

↓

Android Application

↓

ONNX AI Model

↓

Risk Prediction

↓

Wi-Fi

↓

Flask Server

↓

Dashboard

↓

Doctor / Family Notification
```

---

# AI Workflow

Sensor data is converted into a feature vector containing:

- Temperature
- Pressure
- Heart Rate
- Blood Oxygen
- Motion Features

The ONNX model performs anomaly detection to estimate the patient's diabetic foot risk.

Output:

- Normal
- Medium Risk
- High Risk

---

# Dashboard Features

- Live Sensor Monitoring
- AI Risk Prediction
- Historical Records
- Alert Generation
- Patient Status
- Trend Visualization

---

# Future Scope

- Cloud Synchronization
- Doctor Portal
- Hospital Integration
- Predictive Risk Forecasting
- Smart Insole PCB
- Rechargeable Battery System
- TinyML Deployment
- Qualcomm NPU Optimization

---

# Technologies Used

- Arduino App Lab
- Arduino RouterBridge
- Bluetooth Low Energy
- Python
- Flask
- SQLite
- Android Studio
- Kotlin
- ONNX Runtime
- Scikit-Learn
- HTML
- CSS
- JavaScript
- Chart.js

---

# Target Users

- Diabetic Patients
- Hospitals
- Clinics
- Rural Healthcare Centers
- Home Healthcare Services
- Telemedicine Platforms

---

# Expected Outcome

The proposed system enables continuous, low-cost diabetic foot monitoring capable of detecting early signs of ulcer formation before severe tissue damage occurs.

By combining wearable sensing, Edge AI, and real-time visualization, PodoCare AI aims to reduce unnecessary hospital visits, improve patient outcomes, and support preventive healthcare.
