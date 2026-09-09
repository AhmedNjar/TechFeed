"""
Fetching layer.

Strategy (since we don't know upfront if a site is static or JS-heavy):
  1. Try the fast HTTP `Fetcher` first — cheap, no browser, works for ~80% of
     server-rendered blogs.
  2. If the response looks too "thin" (likely a JS shell with no real content,
     or the site blocked the plain HTTP client), fall back to `StealthyFetcher`,
     which runs a real headless browser with anti-bot hardening.

This mirrors the escalation path Scrapling recommends: Fetcher → StealthyFetcher
→ DynamicFetcher. We stop at StealthyFetcher since it covers the vast majority
of JS-rendered blogs without the extra cost of full DynamicFetcher automation.
"""
import logging
from dataclasses import dataclass

from scrapling.fetchers import Fetcher, StealthyFetcher

logger = logging.getLogger("scraper")

# Custom UA — identify ourselves honestly, standard scraping etiquette
USER_AGENT = "TechHubBot/1.0 (+https://github.com/yourname/techhub; for personal tech feed aggregation)"

# Below this many characters of visible body text, we assume the page
# didn't render properly with the fast fetcher (likely JS-rendered SPA shell).
MIN_CONTENT_LENGTH = 400


@dataclass
class FetchResult:
    html: str
    method: str  # "fast" | "browser"


def fetch_page(url: str, force_browser: bool = False) -> FetchResult:
    if not force_browser:
        try:
            page = Fetcher.get(
                url,
                timeout=15,
                headers={"User-Agent": USER_AGENT},
            )
            if page.status == 200 and _looks_sufficient(page.body):
                return FetchResult(html=page.body, method="fast")
            logger.info(f"Fast fetch insufficient for {url} (status={page.status}), falling back to browser")
        except Exception as e:
            logger.info(f"Fast fetch failed for {url}: {e} — falling back to browser")

    # ── Browser fallback ─────────────────────────────────────────────────────
    page = StealthyFetcher.fetch(
        url,
        headless=True,
        network_idle=True,   # wait for JS-rendered content to settle
        timeout=30_000,
    )
    return FetchResult(html=page.body, method="browser")


def _looks_sufficient(html: str) -> bool:
    """Rough heuristic: strip tags, check remaining text length."""
    import re
    text = re.sub(r"<[^>]+>", " ", html)
    text = re.sub(r"\s+", " ", text).strip()
    return len(text) >= MIN_CONTENT_LENGTH
