"""
FastAPI microservice — exposes Scrapling-powered scraping to the Spring Boot backend.

Endpoints:
  GET  /health                → liveness check
  POST /scrape/article        → scrape one article URL, return clean content
  POST /scrape/discover       → find article links on a blog listing page

Run locally:
  uvicorn app.main:app --host 0.0.0.0 --port 8000

The Spring Boot backend calls this via WebClient (see ScraperApiClient.kt).
"""
import logging

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

from app.models import (
    ArticleScrapeRequest, ArticleScrapeResponse,
    DiscoverRequest, DiscoverResponse,
    HealthResponse,
)
from app.scraper import fetch_page
from app.extractor import extract_article, discover_article_links

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("main")

app = FastAPI(
    title="TechHub Scraper Service",
    description="Scrapling-powered public page scraping for blogs without RSS feeds",
    version="1.0.0",
)


@app.get("/health", response_model=HealthResponse)
def health():
    return HealthResponse(status="ok", scrapling_ready=True)


@app.post("/scrape/article", response_model=ArticleScrapeResponse)
def scrape_article(req: ArticleScrapeRequest):
    try:
        fetched = fetch_page(req.url, force_browser=req.force_browser)
        article = extract_article(fetched.html, req.url)

        if not article.content or len(article.content.strip()) < 100:
            return ArticleScrapeResponse(
                success=False,
                url=req.url,
                fetch_method=fetched.method,
                error="Extracted content too short — likely not an article page, "
                      "or page requires login/JS interaction beyond our reach.",
            )

        return ArticleScrapeResponse(
            success=True,
            url=req.url,
            title=article.title,
            content=article.content,
            image_url=article.image_url,
            author=article.author,
            published_at=article.published_at,
            fetch_method=fetched.method,
        )

    except Exception as e:
        logger.exception(f"Failed to scrape article: {req.url}")
        return ArticleScrapeResponse(
            success=False,
            url=req.url,
            error=str(e),
        )


@app.post("/scrape/discover", response_model=DiscoverResponse)
def scrape_discover(req: DiscoverRequest):
    try:
        fetched = fetch_page(req.listing_url)
        urls = discover_article_links(
            fetched.html,
            listing_url=req.listing_url,
            link_selector=req.link_selector,
            limit=req.limit,
        )
        return DiscoverResponse(
            success=True,
            listing_url=req.listing_url,
            article_urls=urls,
        )
    except Exception as e:
        logger.exception(f"Failed to discover links: {req.listing_url}")
        return DiscoverResponse(
            success=False,
            listing_url=req.listing_url,
            error=str(e),
        )


@app.exception_handler(Exception)
async def unhandled_exception_handler(request, exc):
    logger.exception("Unhandled exception")
    return JSONResponse(status_code=500, content={"error": str(exc)})
