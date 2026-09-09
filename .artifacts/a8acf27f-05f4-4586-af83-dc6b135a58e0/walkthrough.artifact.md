# Walkthrough: Reorganized Project Structure

I have moved all files from the `temp` directory to their appropriate locations in the mobile app, backend, and scraper service. The `temp` directory has been removed.

## Changes Made

### 1. Mobile App (`composeApp`)
Updated the mobile app to include new features and align with the latest backend models.
- **Model Update**: `FeedItem` now uses `String` for the source field to support dynamic blog names from the backend.
- **UI Screens**: Added `StatsScreen.kt` and `ForYouScreen.kt` to `ui/screens`.
- **Components**: Added `AnimatedBookmarkIcon.kt`, `AnimatedCounterText.kt`, and utility functions like `pressScale`.
- **Repositories**: Added `ForYouRepository.kt`, `StatsRepository.kt`, and `RelatedArticlesRepository.kt` to the domain layer.
- **Dependency Injection**: Updated `AppModule.kt` to inject the new repositories and view models.

### 2. Backend (`server`)
Established a dedicated `server` module structure for the Spring Boot application.
- **Domain**: `Post.kt` for MongoDB persistence.
- **Controllers**: `PostController.kt` for mobile APIs and `AdminController.kt` for ingestion triggers.
- **Service Layer**: Added `BlogIngestionService.kt` and `DuplicateDetectionService.kt`.
- **Resources**: Moved `application.yml` and the admin dashboard (`index.html`) to the resources directory.

### 3. Scraper (`scraper`)
Moved the Python-based scraper service to its own directory.
- **App Logic**: `main.py`, `scraper.py`, `extractor.py`, and `models.py`.
- **Configuration**: `requirements.txt`, `Dockerfile`, and a dedicated `README.md`.

## Verification Results

- [x] All files from `temp` moved or integrated.
- [x] `temp` directory deleted.
- [x] Resolved major compilation errors in `composeApp` caused by model changes.
- [x] Fixed `FeedCard` to support the new `index` parameter and dynamic source names.

> [!NOTE]
> Some icons in the new screens were updated to use standard Material icons (`Star`, `List`) because the extended icon set was not found in the project's dependencies.
