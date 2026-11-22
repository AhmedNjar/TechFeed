# 🚀 TechFeed

<p align="center">
  <b>A modern, full-stack tech news & article aggregator built with Compose Multiplatform, Spring Boot, and Python FastAPI.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin" alt="Kotlin Multiplatform">
  <img src="https://img.shields.io/badge/Compose-Multiplatform-brightgreen?logo=jetpackcompose" alt="Compose Multiplatform">
  <img src="https://img.shields.io/badge/Spring%2C%20Boot-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Python-FastAPI-009688?logo=fastapi&logoColor=white" alt="FastAPI">
  <img src="https://img.shields.io/badge/MongoDB-4EA94B?logo=mongodb&logoColor=white" alt="MongoDB">
</p>

---

## 🏗️ Architecture & Overview

TechFeed is structured as a robust multi-service system:
1. **`composeApp` / `iosApp`**: A cross-platform mobile application targeting **Android** and **iOS** built using **Jetpack / Compose Multiplatform**. Provides a clean, modern reading and feed experience.
2. **`server`**: A **Spring Boot** backend service providing REST APIs for the mobile client, managing post ingestion schedules, and serving an Admin Dashboard.
3. **`scraper`**: A high-performance **Python FastAPI** microservice powered by **Scrapling** and **Trafilatura** to extract clean article content from public tech blogs and sites that lack RSS feeds.
4. **Database**: **MongoDB** for storing articles, feeds, and metadata.

---

## 📂 Project Structure

```text
TechFeed/
├── composeApp/          # Compose Multiplatform shared code (Android & iOS common UI/logic)
├── iosApp/              # iOS specific runner entry point (SwiftUI)
├── server/              # Spring Boot backend API & ingestion scheduler
├── scraper/             # Python FastAPI scraping microservice (Scrapling)
└── settings.gradle.kts  # Gradle root settings
```

---

## 🛠️ Tech Stack

- **Client**: Kotlin Multiplatform, Compose Multiplatform, Kotlin Coroutines, Ktor.
- **Backend**: Spring Boot, Spring Data MongoDB, RESTful APIs.
- **Scraper**: Python 3.10+, FastAPI, Uvicorn, Scrapling, Trafilatura.
- **Database**: MongoDB (Local or MongoDB Atlas).

---

## 🚀 Getting Started

### 1. Database Setup (MongoDB)
Ensure MongoDB is running locally on `localhost:27017` or have your MongoDB Atlas connection string ready.
- **Local MongoDB**: Start your MongoDB service (`mongod`).

---

### 2. Running the Backend Server (`server`)
The Spring Boot backend manages post APIs and coordinates ingestion.

```bash
cd server
```

Configure `MONGO_URI` if using cloud MongoDB (optional, defaults to `mongodb://localhost:27017`):
- **Windows (CMD)**:
  ```cmd
  set MONGO_URI=your_mongodb_connection_string
  ../gradlew :server:bootRun
  ```
- **macOS / Linux / PowerShell**:
  ```bash
  $env:MONGO_URI="your_mongodb_connection_string" # PowerShell
  # or export MONGO_URI="..." # bash/zsh
  ../gradlew :server:bootRun
  ```

The server will start at `http://localhost:8080`.
- **Health Check**: `http://localhost:8080/actuator/health`
- **Posts API**: `http://localhost:8080/api/posts`

---

### 3. Running the Scraper Microservice (`scraper`)
The Python FastAPI service handles web scraping for non-RSS sites.

```bash
cd scraper

# Create & activate virtual environment
python -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate

# Install dependencies & browser binaries for Scrapling
pip install -r requirements.txt
scrapling install

# Run the API
uvicorn app.main:app --reload --port 8000
```

---

### 4. Building and Running the Mobile App

#### **Android Application**
Build and run via Android Studio or command line:
- **macOS / Linux**:
  ```bash
  ./gradlew :composeApp:assembleDebug
  ```
- **Windows**:
  ```bash
  .\gradlew.bat :composeApp:assembleDebug
  ```

#### **iOS Application**
Open the `iosApp` directory in Xcode:
```bash
open iosApp/iosApp.xcodeproj
```
Select your target simulator and hit **Run** in Xcode.

---

## 📖 Key Endpoints & Admin Actions

### Backend (`:8080`)
- `GET /api/posts` - Retrieve ingested tech articles feed for the mobile app.
- `POST /api/admin/ingest/rss` - Trigger RSS ingestion manually (Requires admin secret header `X-Admin-Secret`).

### Scraper (`:8000`)
- `GET /health` - Liveness check.
- `POST /scrape/article` - Scrape article content from a specific URL.
- `POST /scrape/discover` - Discover article links from listing/blog homepages.

---

## 📄 License

[MIT License](LICENSE)
