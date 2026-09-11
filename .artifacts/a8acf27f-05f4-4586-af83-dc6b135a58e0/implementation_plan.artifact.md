# Implementation Plan: Prepare for Testing on Physical Phone

This plan prepares the mobile app and backend for testing on a physical device. Currently, the app is hardcoded to connect to `localhost`, which only works for the emulator.

## User Action Required

> [!IMPORTANT]
> 1. **Find your Computer's Local IP**: Open a terminal and run `ipconfig` (Windows) or `ifconfig` (Mac/Linux). Look for the IPv4 address (e.g., `192.168.1.5`).
> 2. **Connect to Same Network**: Ensure both your computer and your phone are connected to the same Wi-Fi network.
> 3. **Run MongoDB**: Make sure MongoDB is running on your computer.

## Proposed Changes

### 1. Mobile App Configuration

#### [MODIFY] [FeedApiClient.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/data/remote/FeedApiClient.kt)
- Update `BASE_URL` to a placeholder that reminds the user to use their local IP address.
- Add instructions in comments on how to find the IP.

### 2. Backend Fixes

#### [MODIFY] [TechIntelligenceHubApplication.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/TechIntelligenceHubApplication.kt)
- Mark the main class as `open` just in case the `all-open` plugin (applied via `kotlin-spring`) is having issues in this environment, as we saw with `AppConfig`.

## Verification Plan

### Automated Tests
- Run `./gradlew :server:assemble` and `./gradlew :composeApp:assembleDebug` to ensure no regression.

### Manual Verification
1. **Start Backend**:
   ```bash
   ./gradlew :server:bootRun
   ```
2. **Deploy App**: Run the app from Android Studio on your physical phone.
3. **Check Connection**: Verify the feed loads articles.
