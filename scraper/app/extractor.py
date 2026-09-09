"""
Content extraction layer.

We deliberately split concerns:
  - scraper.py  → reliably GET the HTML (handles anti-bot, JS rendering)
  - extractor.py → turn arbitrary blog HTML into clean article data

trafilatura is purpose-built for "give me the article, not the nav/ads/footer"
across arbitrary, unknown site templates — which is exactly what we need since
these are blogs without RSS feeds and no per-site selectors configured.
"""
import logging
import re
from dataclasses import dataclass
from typing import Optional, List
from urllib.parse import urljoin, urlparse

import trafilatura
from trafilatura.metadata import extract_metadata
from scrapling.parser import Adaptor

logger = logging.getLogger("extractor")


@dataclass
class ExtractedArticle:
    title: Optional[str]
    content: Optional[str]       # markdown
    image_url: Optional[str]
    author: Optional[str]
    published_at: Optional[str]


def extract_article(html: str, url: str) -> ExtractedArticle:
    # trafilatura: main content as markdown, no boilerplate
    content_md = trafilatura.extract(
        html,
        url=url,
        output_format="markdown",
        include_images=True,
        include_links=False,
        favor_recall=True,   # err toward keeping more content for tech articles
    )

    meta = extract_metadata(html, default_url=url)

    image_url = None
    if meta and meta.image:
        image_url = _absolute_url(meta.image, url)

    return ExtractedArticle(
        title=(meta.title if meta and meta.title else None),
        content=content_md,
        image_url=image_url,
        author=(meta.author if meta else None),
        published_at=(meta.date if meta else None),
    )


def discover_article_links(
    html: str,
    listing_url: str,
    link_selector: Optional[str] = None,
    limit: int = 20,
) -> List[str]:
    """
    Find article URLs on a blog listing/homepage.

    If link_selector is given (e.g. "article a.post-title"), use it directly —
    most reliable. Otherwise fall back to a heuristic: collect same-domain
    links with a "slug-like" path, excluding common non-article patterns.
    """
    page = Adaptor(html, url=listing_url)
    domain = urlparse(listing_url).netloc

    if link_selector:
        elements = page.css(link_selector)
        hrefs = [el.attrib.get("href") for el in elements if el.attrib.get("href")]
    else:
        hrefs = _heuristic_article_links(page, domain)

    seen = set()
    results = []
    for href in hrefs:
        absolute = _absolute_url(href, listing_url)
        if not absolute or absolute in seen:
            continue
        if urlparse(absolute).netloc != domain:
            continue
        seen.add(absolute)
        results.append(absolute)
        if len(results) >= limit:
            break

    return results


# ── Helpers ───────────────────────────────────────────────────────────────────

_EXCLUDE_PATTERNS = re.compile(
    r"(tag/|category/|author/|page/\d+|/login|/signup|/about|/contact|"
    r"#|mailto:|\.(jpg|jpeg|png|gif|svg|pdf|css|js)$|/feed/?$)",
    re.IGNORECASE,
)


def _heuristic_article_links(page: Adaptor, domain: str) -> List[str]:
    links = page.css("a")
    candidates = []
    for el in links:
        href = el.attrib.get("href")
        if not href or _EXCLUDE_PATTERNS.search(href):
            continue
        # Article URLs usually have a meaningful path depth/slug length
        path = urlparse(href).path
        slug = path.rstrip("/").split("/")[-1] if path else ""
        if len(slug) < 8:   # too short to be a real article slug
            continue
        candidates.append(href)
    return candidates


def _absolute_url(href: str, base_url: str) -> Optional[str]:
    try:
        return urljoin(base_url, href)
    except Exception:
        return None
