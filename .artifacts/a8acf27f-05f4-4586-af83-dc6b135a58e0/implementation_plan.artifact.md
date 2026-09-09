# Plan: Organize Project Structure by Moving Files from `temp`

The `temp` directory currently contains a mix of upgraded mobile app components, a new backend implementation, and a scraper microservice. This plan aims to organize these files into their logical locations to improve project maintainability and separate concerns.

## Proposed Changes

### 1. Mobile App (`composeApp`)
Move upgraded and new Kotlin Multiplatform files to their appropriate packages in the `composeApp` module. These files improve the feed, add statistics, "For You" recommendations, and related articles.

#### [MODIFY] [FeedViewModel.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/presentation/FeedViewModel.kt)
- Replace with the upgraded version from `temp`.

#### [MODIFY] [AppModule.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/di/AppModule.kt)
- Replace with the version that includes new repository injections.

#### [MODIFY] [FeedItem.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/data/model/FeedItem.kt)
- Replace with the version that fixes serialization naming and source type.

#### [MODIFY] [FeedApiClient.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/data/remote/FeedApiClient.kt)
- Replace with the version that points to the correct backend endpoints.

#### [NEW] [StatsScreen.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/ui/screens/StatsScreen.kt)
#### [NEW] [ForYouScreen.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/ui/screens/ForYouScreen.kt)
#### [NEW] [AnimatedCounter.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/ui/components/AnimatedCounter.kt)
#### [NEW] [AnimatedBookmarkIcon.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/ui/components/AnimatedBookmarkIcon.kt)
#### [NEW] [PressScale.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/ui/components/PressScale.kt)
#### [NEW] [WindowSizeClass.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/ui/utils/WindowSizeClass.kt)
#### [NEW] [ForYouRepository.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/domain/ForYouRepository.kt)
#### [NEW] [StatsRepository.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/domain/StatsRepository.kt)
#### [NEW] [RelatedArticlesRepository.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/domain/RelatedArticlesRepository.kt)
#### [NEW] [ScraperApiClient.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/composeApp/src/commonMain/kotlin/com/techfeed/data/remote/ScraperApiClient.kt)

---

### 2. Backend (`server` - New Module)
Establish a new `server` directory for the Spring Boot backend implementation.

#### [NEW] [Post.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/domain/document/Post.kt)
#### [NEW] [PostController.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/api/controller/PostController.kt)
#### [NEW] [AdminController.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/api/controller/AdminController.kt)
#### [NEW] [PostRepository.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/data/repository/PostRepository.kt)
#### [NEW] [AppConfig.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/config/AppConfig.kt)
#### [NEW] [Properties.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/config/Properties.kt)
#### [NEW] [Dtos.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/api/dto/Dtos.kt)
#### [NEW] [BlogIngestionService.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/service/BlogIngestionService.kt)
#### [NEW] [DuplicateDetectionService.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/service/DuplicateDetectionService.kt)
#### [NEW] [IngestionScheduler.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/service/IngestionScheduler.kt)
#### [NEW] [application.yml](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/resources/application.yml)

---

### 3. Scraper (`scraper` - New Directory)
Move the Python scraper components to a dedicated `scraper` directory.

#### [NEW] [main.py](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/scraper/app/main.py)
#### [NEW] [models.py](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/scraper/app/models.py)
#### [NEW] [scraper.py](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/scraper/app/scraper.py)
#### [NEW] [extractor.py](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/scraper/app/extractor.py)
#### [NEW] [requirements.txt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/scraper/requirements.txt)
#### [NEW] [Dockerfile](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/scraper/Dockerfile)
#### [NEW] [index.html](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/scraper/index.html)

---

### 4. Cleanup
Delete the `temp` folder after all files are successfully moved and verified.

## Verification Plan

### Automated Tests
- Since this is a structural move, I will verify that the Kotlin files in `composeApp` do not have unresolved references by checking their imports and package declarations.
- I will not be able to run the full build or tests without further configuration (Gradle module setup for `server`, etc.), but I will ensure the file paths match the package declarations.

### Manual Verification
- Verify all files from `temp` are accounted for in the new structure.
- Verify `temp` directory is empty/removed.
