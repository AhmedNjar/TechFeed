# Walkthrough: Ready for Physical Device Testing

I have updated the project to facilitate testing on a physical phone. This involves making the network configuration explicit and ensuring the backend can start correctly.

## Changes Made

### 1. Mobile App Configuration
- **File**: [FeedApiClient.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/data/remote/FeedApiClient.kt)
- **Change**: Added documentation and a placeholder in `BASE_URL` to help you configure the app to connect to your computer's local IP address.

### 2. Backend Stability
- **File**: [TechIntelligenceHubApplication.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/TechIntelligenceHubApplication.kt)
- **Change**: Marked the main class as `open`. This prevents potential Spring Boot startup issues where `@Configuration` or `@SpringBootApplication` classes are expected to be non-final by the proxying mechanism.

## Steps to Test on Your Phone

1.  **Get your Local IP**:
    - On Windows: Run `ipconfig` in CMD. Look for `IPv4 Address` (e.g., `192.168.1.10`).
    - On Mac/Linux: Run `ifconfig` or `ip addr`.
2.  **Update the App**:
    - Open [FeedApiClient.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/data/remote/FeedApiClient.kt).
    - Change `http://localhost:8080/api/` to `http://YOUR_IP:8080/api/`.
3.  **Start Services**:
    - Ensure **MongoDB** is running.
    - Run the backend: `./gradlew :server:bootRun`.
4.  **Connect Phone**:
    - Connect your phone to the **same Wi-Fi** as your computer.
    - Run the app from Android Studio onto your phone.

## Verification Results

- [x] `:server:assemble` passed.
- [x] `:composeApp:assembleDebug` passed.
