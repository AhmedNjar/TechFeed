"""
Request/response schemas for the scraper microservice.
Mirrors the field names the Spring Boot backend expects.
"""
from typing import Optional, List
from pydantic import BaseModel, HttpUrl, Field


# ── Requests ──────────────────────────────────────────────────────────────────

class ArticleScrapeRequest(BaseModel):
    url: str = Field(..., description="Full URL of the article to scrape")
    force_browser: bool = Field(
        default=False,
        description="Skip the fast HTTP fetch and go straight to the browser fetcher"
    )


class DiscoverRequest(BaseModel):
    listing_url: str = Field(..., description="Blog homepage / article listing page")
    link_selector: Optional[str] = Field(
        default=None,
        description="CSS selector for article links, e.g. 'article a.post-link'. "
                     "If omitted, Scrapling's smart link discovery is used."
    )
    limit: int = Field(default=20, ge=1, le=100)


# ── Responses ─────────────────────────────────────────────────────────────────

class ArticleScrapeResponse(BaseModel):
    success: bool
    url: str
    title: Optional[str] = None
    content: Optional[str] = None          # clean markdown/text — feeds into Ollama
    image_url: Optional[str] = None
    author: Optional[str] = None
    published_at: Optional[str] = None     # ISO 8601 if extractable
    fetch_method: Optional[str] = None     # "fast" | "browser" — useful for debugging
    error: Optional[str] = None


class DiscoverResponse(BaseModel):
    success: bool
    listing_url: str
    article_urls: List[str] = []
    error: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    scrapling_ready: bool
