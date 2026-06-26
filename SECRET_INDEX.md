# Ultima-Grid Secret Index Sheet

This document acts as the reference for layered/buried environment variables used in the DJ MIDI WATTS project. Do not commit actual values to this repository.

| Index | System Key | App Reference | Purpose | Storage Recommendation |
| :--- | :--- | :--- | :--- | :--- |
| **S1** | `UG_S1` | `BuildConfig.UG_S1` | Gemini AI Orchestration Key | System Env or `local.properties` |
| **S2** | `UG_S2` | `BuildConfig.UG_S2` | Google OAuth Web Client ID | `.env` or System Env |
| **S3** | `UG_S3_CERT` | `BuildConfig.UG_S3` | Wireless Injection Certificate (BKS/Base64) | `.env` or KeyStore |
| **S3.1** | `UG_S3_KEYSTORE_PASS` | N/A | Password for client.bks | `.env` |