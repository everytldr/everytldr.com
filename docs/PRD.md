# TLDR Times — Product Requirements Document

## 1. Goals
- Let readers consume news originally written in languages they don't read, via AI-generated summaries in their preferred language.
- Provide a lightweight discussion surface (comments, likes) per article.
- Reference UX: https://news.hada.io/lists.

## 2. Functional Requirements

### 2.1 Content Ingestion
- The system collects non-Korean news articles from external sources on a schedule.
- Each ingested article is automatically translated, summarized, and classified into a category.

### 2.2 Categorization
- Articles carry exactly one category (e.g., football, baseball, basketball, politics, economy, tech, entertainment, …).
- The category taxonomy is a closed, product-defined enum. The enum is extensible without breaking existing data.

### 2.3 Multi-Language Summaries
- Every article is available in every supported summary language.
- The set of supported languages is extensible (Korean, English, Spanish, Japanese, …); adding a language must not require disruptive changes.
- **Language selection rules**
  - Default: the reader's browser language preference.
  - If the reader's browser language is not among supported languages, fall back to English.
  - The reader can override the default via an in-site settings control; the override persists across visits for that reader.

### 2.4 Article List Page
- Displays: title, 2-line summary, thumbnail, category, published time, all rendered in the reader's selected language (see §2.3).
- Supports pagination.
- Filterable by category.

### 2.5 Article Detail Page
- Displays: AI-generated summary in the reader's selected language, link to original source article, view count, like button with count, comment thread.
- Repeat views from the same reader do not inflate the view count.

### 2.6 Likes (Anonymous)
- One like per reader per article. Unliking is allowed.
- Like counts are displayed on list and detail pages.
- The system must reasonably prevent automated or large-scale like abuse.

### 2.7 Comments (Anonymous)
- Threaded comments per article.
- Each comment has: content, nickname, timestamp.
- A default nickname is auto-generated for the reader on first use; the reader can change it in settings.
- Soft delete supported (hidden from UI, retained for moderation).
- Anonymous posting is permitted without login.

### 2.8 Source Link & Attribution
- Every article detail page links out to the original source URL.
- Original publisher name is displayed as attribution.

## 3. Non-Functional Requirements

- **Content freshness**
  - Newly published source articles must appear on the service within 15 minutes of original publication under normal operation.
- **Content durability**
  - No article that has been successfully ingested is silently lost due to transient external failures (LLM outage, network error, etc.).
- **Compliance**
  - Respect `robots.txt` and each source's Terms of Service.
  - Store and display only summaries and metadata; never redistribute full copyrighted article bodies.

## 4. MVP Scope
All MVP-specific constraints are consolidated here. Anything not listed under "In Scope" is deferred.

### 4.1 In Scope
- Ingest foreign football articles only (single active category: `FOOTBALL`).
- Translate and summarize into Korean and English.
- Article list page.
- Article detail page.
- Anonymous likes.
- Anonymous threaded comments.
- View counts.
- Original source link + publisher attribution.

### 4.2 Out of Scope
- Categories other than football.
- Summary languages other than Korean and English.
- Breaking-news push notifications.
- Per-team / per-league / per-player filtering within a category.
- Native mobile applications.

## 5. Success Metrics *(to be refined)*
- Daily active readers.
- Articles viewed per session.
- Likes per article.
- Comments per article.
- Summary quality rating (qualitative review, post-launch).

## 6. Open Questions
- Is real-time breaking-news push in scope post-MVP?
- What is the policy when a source's `robots.txt` or ToS prohibits ingestion?
- Definition of "high importance" for breaking-news eligibility.
- Moderation policy for anonymous comments.
