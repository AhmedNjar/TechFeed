# TechHub Scraper Service

Python microservice using [Scrapling](https://github.com/D4Vinci/Scrapling) to scrape
**public** blog/article pages that don't offer an RSS feed. Called by the Spring Boot
backend over REST — it never talks to the mobile app or any authenticated site directly.

## Local setup

```bash
cd scraper
python -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate

pip install -r requirements.txt
scrapling install                 # downloads browser binaries for StealthyFetcher

uvicorn app.main:app --reload --port 8000
```

Test it:
```bash
curl -X POST http://localhost:8000/scrape/article \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example-tech-blog.com/some-post"}'
```

## Docker

```bash
docker build -t techhub-scraper .
docker run -p 8000:8000 techhub-scraper
```

## How it works

1. **`Fetcher`** (fast HTTP) is tried first — covers most server-rendered blogs.
2. If the response looks too thin (likely a JS shell, or blocked), it falls back to
   **`StealthyFetcher`** — a real headless browser with anti-bot hardening.
3. **`trafilatura`** extracts the clean article body (title, text, image, date, author)
   from whatever HTML template the blog uses — no per-site CSS selectors needed for
   the article page itself.
4. `/scrape/discover` finds article links on a blog's homepage/listing page, either via
   a CSS selector you provide or a generic heuristic fallback.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Liveness check |
| POST | `/scrape/article` | Scrape one article URL → `{title, content, image_url, ...}` |
| POST | `/scrape/discover` | Find article URLs on a listing page |

## Scope & etiquette

- Only intended for **public pages that don't require login**.
- Identifies itself with a custom `User-Agent` (see `app/scraper.py`).
- Respect each site's `robots.txt` and rate limits when configuring which blogs to
  ingest and how often (the Spring Boot scheduler controls ingestion frequency).


.\venv\Scripts\python.exe -m uvicorn app.main:app --port 8000