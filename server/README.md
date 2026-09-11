# TechHub Backend

Spring Boot service providing APIs for the TechFeed mobile app and handling article ingestion from various sources.

## Prerequisites

*   **JDK 17**
*   **MongoDB**: Can be local or cloud-based.

### Option A: Local MongoDB (Recommended for Dev)
1.  Install [MongoDB Community Server](https://www.mongodb.com/try/download/community).
2.  Ensure it is running on `localhost:27017`. The backend connects here by default.

### Option B: MongoDB Atlas (Cloud Cluster)
1.  Create a free cluster at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas).
2.  Get your connection string (e.g., `mongodb+srv://<user>:<password>@cluster0.abcde.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0`).
3.  Run the backend using the `MONGO_URI` environment variable:
    ```bash
    # Windows (CMD)
    set MONGO_URI=your_connection_string
    ./gradlew :server:bootRun

    # Mac/Linux/PowerShell
    $env:MONGO_URI="your_connection_string"
    ./gradlew :server:bootRun
    ```

## Running Locally

You can run the backend directly from the root of the project using Gradle:

```bash
./gradlew :server:bootRun
```

Or if you are in the `server` directory:

```bash
../gradlew bootRun
```

The server will start on `http://localhost:8080`.

## Key Endpoints

*   **Health Check**: `http://localhost:8080/actuator/health`
*   **Mobile API (Feed)**: `http://localhost:8080/api/posts`
*   **Admin Dashboard**: `http://localhost:8080/admin` (Redirects to static dashboard)

## Admin Actions

You can manually trigger ingestion using the Admin Dashboard or via `curl`. A secret is required (configured in `application.yml` as `api.chrome-extension-secret`).

Example triggering RSS ingestion:
```bash
curl -X POST http://localhost:8080/api/admin/ingest/rss \
  -H "X-Admin-Secret: dev-secret-change-in-prod"
```
