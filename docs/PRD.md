# everytldr — Product Requirements Document

## 1. Goals
- Let readers consume foreign-language news via AI summaries in their own language.
- Organize summaries under a browsable, hierarchical topic taxonomy.
- Provide lightweight per-article discussion (comments, likes).
- Surface the catalog to external readers and search engines via feeds and standard discovery files.
- Reference UX: https://news.hada.io/lists.

## 2. Functional Requirements

### 2.1 Content Ingestion
- News is collected from external sources on a schedule, then automatically translated, summarized, and classified into exactly one category (§2.2).
- Ingestion and enrichment are separate stages: metadata is captured first; translation/summarization/category selection follow and are independently retryable.

### 2.2 Categorization
- Each article carries exactly one category.
- The taxonomy is a closed, product-defined **hierarchy**: top-level topics (world, politics, society, economy, environment, technology, science, health, education, culture, sport, …) nested several levels deep into sub-topics and specific entities (e.g. sport → competition → team).
- Extensible: adding/renaming/merging/re-nesting categories must not break existing data.
- A category can be administratively hidden from readers without deletion (blocked categories), so the published set is a subset of the defined set.

### 2.3 Multi-Language Summaries
- Every article is available in every supported language.
- Supported: Korean and English; the set is extensible without disruptive changes.
- Selection: default English; the reader switches language via an in-site control; the choice persists across visits and is reflected in the page URL (localized URLs are shareable).

### 2.4 Article List & Browse
- Each item shows title, summary, thumbnail, category, and published time in the selected language (§2.3).
- Continuation-based pagination (load-more / infinite scroll), newest-first.
- Browsable by the category hierarchy; filterable by category, where a parent includes all descendants.

### 2.5 Article Detail Page
- Shows the summary in the selected language, a link to the source (§2.8), publisher attribution, a like button with count, and the comment thread with count.

### 2.6 Likes (Anonymous)
- One like per reader per article; unliking allowed; count shown on the detail page.
- Must reasonably prevent automated or large-scale abuse.

### 2.7 Comments (Anonymous)
- Threaded, no login. Each comment requires a nickname and a password (the password gates later moderation of that comment).
- Displays content, nickname, timestamp, and a partially masked reader network address for lightweight accountability.
- Soft delete (moderation): hidden from UI but retained. Deleting a parent keeps its replies and shows a placeholder for the parent.

### 2.8 Source Link & Attribution
- Every detail page links out to the original source URL and displays the publisher name as attribution.

### 2.9 Syndication & Discovery
- Machine-readable feeds segmented per language and per category.
- Search across summarized articles.
- Search-engine discovery surfaces (sitemap, crawler directives).
- Static pages: about, privacy, terms.

## 3. Non-Functional Requirements
- **Freshness**: new source articles appear shortly after publication under normal operation.
- **Durability**: a successfully ingested article is never silently lost on transient failures (LLM outage, network error); failed enrichment is retryable without re-ingesting.
- **Compliance**: respect `robots.txt` and each source's ToS; store/display only summaries and metadata, never full copyrighted bodies.

## 4. Scope
Anything not under In Scope is deferred.

### 4.1 In Scope
- Scheduled ingestion across the full taxonomy from multiple sources (§2.1–§2.2).
- Korean and English summaries (§2.3).
- Hierarchical browse + article list (§2.4), article detail (§2.5).
- Anonymous likes (§2.6) and threaded comments (§2.7).
- Source link + attribution (§2.8).
- Per-language/per-category feeds, search, discovery files, static pages (§2.9).

### 4.2 Out of Scope
- Languages other than Korean and English.
- Breaking-news push notifications.
- Per-reader accounts / authenticated profiles.
- Reader-facing view counts.
- Reader self-service comment deletion (moderation-only; §2.7).
- Native mobile apps.

## 5. Success Metrics *(to be refined)*
- Daily active readers; articles viewed per session; likes per article; comments per article; summary quality (qualitative, post-launch).

## 6. Open Questions
- Is real-time breaking-news push in scope post-MVP?
- Policy when a source's `robots.txt` or ToS prohibits ingestion?
- Should language default additionally consider the reader's browser preference?
- Moderation policy for anonymous comments.
