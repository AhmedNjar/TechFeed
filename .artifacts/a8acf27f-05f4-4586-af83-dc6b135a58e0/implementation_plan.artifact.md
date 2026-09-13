# Implementation Plan: AI Bypass for Development

This plan implements a "Development Bypass" to allow articles to show up in the mobile app immediately without requiring a running Ollama (AI) instance.

## Proposed Changes

### Backend Logic

#### [MODIFY] [PostProcessingService.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/TechFeed/server/src/main/kotlin/com/techhub/ai/PostProcessingService.kt)
- Add a boolean flag or check if Ollama is available.
- If Ollama is not being used, automatically promote `PENDING` posts to `TECHNICAL`.
- Set a fallback summary from the content so the feed isn't empty.

## Verification Plan

### Automated Tests
- Run `./gradlew :server:assemble` to ensure no syntax errors.

### Manual Verification
1. **Start Backend**: `./gradlew :server:bootRun`.
2. **Trigger Ingestion**: Run the RSS `curl` command.
3. **Verify Feed**: Pull to refresh on the phone. The articles should now appear even without Ollama running.
