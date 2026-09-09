# scraper-service/Dockerfile
FROM python:3.12-slim

WORKDIR /app

# System deps needed by Playwright/Patchright (browser-based fetching)
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget gnupg ca-certificates fonts-liberation libnss3 libatk-bridge2.0-0 \
    libdrm2 libxkbcommon0 libgbm1 libasound2 libxdamage1 libxfixes3 \
    libxrandr2 libu2f-udev libvulkan1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Installs the browser binaries Scrapling's StealthyFetcher needs
RUN scrapling install

COPY app/ ./app/

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
