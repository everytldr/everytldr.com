---
version: alpha
name: everytldr
description: A multilingual, theme-aware news-summary canvas — light surfaces anchored on white (#ffffff), dark surfaces on warm near-black (#131316). Pretendard Variable typography adapts to Korean and English with locale-aware sizing. Single Read Blue accent (#0a66ff light / #5e93ff dark) for primary actions and links; Liked Rose reserved for the heart-toggled state. Article cards photo-first with pastel category badges that swap to deep-tint variants in dark mode (Notion lineage); comments and source-link cards inherit Notion's sober rectangular geometry; card-grid density and modest display weights from Airbnb; elevation flat-with-hairline by default, with hover-shadow on light converting to surface-lift on dark. The Verge contributes only its monospaced timestamp pattern. Built for a card-grid list page and a focused reading detail page with anonymous likes and threaded comments.

colors:
  # Brand
  primary: "#0a66ff"
  primary-dark: "#5e93ff"
  primary-pressed: "#0850cc"
  primary-pressed-dark: "#4d82e8"
  primary-disabled: "#cce0ff"
  primary-disabled-dark: "#1f3055"
  on-primary: "#ffffff"

  # Like state
  like-active: "#e11d48"
  like-active-dark: "#fb7185"
  like-inactive: "#6a6a6a"
  like-inactive-dark: "#8a8a92"

  # Surface (light)
  canvas: "#ffffff"
  surface-soft: "#f7f7f7"
  surface-strong: "#f2f2f2"

  # Surface (dark)
  canvas-dark: "#131316"
  surface-soft-dark: "#1c1c20"
  surface-strong-dark: "#26262b"

  # Hairlines (light)
  hairline: "#e5e3df"
  hairline-soft: "#ebebeb"
  hairline-strong: "#c1c1c1"

  # Hairlines (dark)
  hairline-dark: "#2a2a2f"
  hairline-soft-dark: "#1f1f23"
  hairline-strong-dark: "#3d3d44"

  # Text (light)
  ink: "#1a1a1a"
  body: "#3f3f3f"
  meta: "#6a6a6a"
  meta-soft: "#929292"

  # Text (dark)
  ink-dark: "#f5f5f7"
  body-dark: "#c8c8d0"
  meta-dark: "#8a8a92"
  meta-soft-dark: "#6a6a72"

  on-dark: "#ffffff"
  on-dark-meta: "#a4a097"

  # Category tints (light)
  tint-emerald: "#d9f3e1"
  tint-sky: "#dcecfa"
  tint-rose: "#fde0ec"
  tint-peach: "#ffe8d4"
  tint-lavender: "#e6e0f5"
  tint-yellow: "#fef7d6"
  tint-gray: "#f0eeec"

  text-emerald: "#15803d"
  text-sky: "#0369a1"
  text-rose: "#be123c"
  text-peach: "#c2410c"
  text-lavender: "#6b21a8"
  text-yellow: "#854d0e"
  text-gray: "#404040"

  # Category tints (dark)
  tint-emerald-dark: "#0e2419"
  tint-sky-dark: "#0a1e30"
  tint-rose-dark: "#2c1019"
  tint-peach-dark: "#29170c"
  tint-lavender-dark: "#1d172e"
  tint-yellow-dark: "#292210"
  tint-gray-dark: "#232328"

  text-emerald-dark: "#6ee7b7"
  text-sky-dark: "#7dd3fc"
  text-rose-dark: "#fda4af"
  text-peach-dark: "#fdba74"
  text-lavender-dark: "#c4b5fd"
  text-yellow-dark: "#fde047"
  text-gray-dark: "#d4d4d8"

  # Semantic (paired)
  semantic-success: "#15803d"
  semantic-success-dark: "#34d399"
  semantic-warning: "#ea580c"
  semantic-warning-dark: "#fb923c"
  semantic-error: "#dc2626"
  semantic-error-dark: "#f87171"

  # Misc
  scrim: "#000000"
  legal-link: "#428bff"
  legal-link-dark: "#7ab2ff"

typography:
  hero-display:
    fontFamily: "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif"
    fontSize: 56px
    fontWeight: 700
    lineHeight: 1.10
    letterSpacing: -1px
  display-xl:
    fontFamily: "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif"
    fontSize: 40px
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: -0.5px
  display-lg:
    fontFamily: "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif"
    fontSize: 32px
    fontWeight: 700
    lineHeight: 1.20
    letterSpacing: -0.5px
  display-md:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 24px
    fontWeight: 700
    lineHeight: 1.30
    letterSpacing: -0.25px
  display-sm:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 20px
    fontWeight: 600
    lineHeight: 1.35
    letterSpacing: 0
  title-md:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 16px
    fontWeight: 600
    lineHeight: 1.40
    letterSpacing: 0
  title-sm:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 14px
    fontWeight: 600
    lineHeight: 1.40
    letterSpacing: 0
  body-lg:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 18px
    fontWeight: 400
    lineHeight: 1.65
    letterSpacing: 0
  body-md:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.60
    letterSpacing: 0
  body-sm:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: 0
  caption:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 13px
    fontWeight: 500
    lineHeight: 1.40
    letterSpacing: 0
  caption-mono:
    fontFamily: "'JetBrains Mono', 'IBM Plex Mono', ui-monospace, SFMono-Regular, Menlo, monospace"
    fontSize: 12px
    fontWeight: 500
    lineHeight: 1.40
    letterSpacing: 0.2px
  micro:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 11px
    fontWeight: 600
    lineHeight: 1.30
    letterSpacing: 0.4px
  button-md:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 15px
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: 0
  button-sm:
    fontFamily: "'Pretendard Variable', Pretendard, sans-serif"
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.25
    letterSpacing: 0

rounded:
  none: 0px
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 24px
  full: 9999px

spacing:
  2xs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
  section: 64px

components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button-md}"
    rounded: "{rounded.sm}"
    padding: "10px 18px"
    height: 44px
  button-primary-pressed:
    backgroundColor: "{colors.primary-pressed}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.sm}"
  button-primary-disabled:
    backgroundColor: "{colors.primary-disabled}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.sm}"
  button-secondary:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.button-md}"
    rounded: "{rounded.sm}"
    padding: "10px 18px"
    height: 44px
    border: "1px solid {colors.hairline-strong}"
  button-ghost:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    typography: "{typography.button-md}"
    rounded: "{rounded.sm}"
    padding: "8px 14px"
  button-link:
    backgroundColor: transparent
    textColor: "{colors.primary}"
    typography: "{typography.body-sm}"
    padding: "0"
  button-pill-filter:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.body}"
    typography: "{typography.button-sm}"
    rounded: "{rounded.full}"
    padding: "8px 16px"
    border: "1px solid {colors.hairline}"
    height: 36px
  button-pill-filter-active:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.on-dark}"
    typography: "{typography.button-sm}"
    rounded: "{rounded.full}"
    padding: "8px 16px"
    height: 36px
  icon-button-circle:
    backgroundColor: "{colors.surface-strong}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    height: 36px
  article-card:
    backgroundColor: "{colors.canvas}"
    rounded: "{rounded.md}"
    padding: "0"
    border: "1px solid {colors.hairline}"
  article-card-photo:
    rounded: "{rounded.md}"
    aspectRatio: "16/9"
  article-card-body:
    backgroundColor: "{colors.canvas}"
    padding: "{spacing.md}"
  article-card-featured:
    backgroundColor: "{colors.canvas}"
    rounded: "{rounded.lg}"
    padding: "0"
    border: "1px solid {colors.hairline}"
  article-card-compact:
    backgroundColor: "{colors.canvas}"
    rounded: "{rounded.sm}"
    padding: "{spacing.sm}"
  category-badge-emerald:
    backgroundColor: "{colors.tint-emerald}"
    textColor: "{colors.text-emerald}"
    typography: "{typography.micro}"
    rounded: "{rounded.xs}"
    padding: "4px 8px"
  category-badge-sky:
    backgroundColor: "{colors.tint-sky}"
    textColor: "{colors.text-sky}"
    typography: "{typography.micro}"
    rounded: "{rounded.xs}"
    padding: "4px 8px"
  category-badge-rose:
    backgroundColor: "{colors.tint-rose}"
    textColor: "{colors.text-rose}"
    typography: "{typography.micro}"
    rounded: "{rounded.xs}"
    padding: "4px 8px"
  category-badge-peach:
    backgroundColor: "{colors.tint-peach}"
    textColor: "{colors.text-peach}"
    typography: "{typography.micro}"
    rounded: "{rounded.xs}"
    padding: "4px 8px"
  category-badge-lavender:
    backgroundColor: "{colors.tint-lavender}"
    textColor: "{colors.text-lavender}"
    typography: "{typography.micro}"
    rounded: "{rounded.xs}"
    padding: "4px 8px"
  category-badge-yellow:
    backgroundColor: "{colors.tint-yellow}"
    textColor: "{colors.text-yellow}"
    typography: "{typography.micro}"
    rounded: "{rounded.xs}"
    padding: "4px 8px"
  category-badge-gray:
    backgroundColor: "{colors.tint-gray}"
    textColor: "{colors.text-gray}"
    typography: "{typography.micro}"
    rounded: "{rounded.xs}"
    padding: "4px 8px"
  like-button:
    backgroundColor: transparent
    textColor: "{colors.meta}"
    typography: "{typography.caption}"
    padding: "6px 10px"
    rounded: "{rounded.full}"
  like-button-active:
    backgroundColor: transparent
    textColor: "{colors.like-active}"
    typography: "{typography.caption}"
    padding: "6px 10px"
    rounded: "{rounded.full}"
  comment-thread:
    backgroundColor: "{colors.canvas}"
    padding: "{spacing.lg} 0"
  comment-card:
    backgroundColor: transparent
    padding: "{spacing.md} 0"
    border: "0 0 1px {colors.hairline-soft} solid"
  comment-card-nested:
    backgroundColor: "{colors.surface-soft}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"
  comment-textarea:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"
    border: "1px solid {colors.hairline-strong}"
    minHeight: 96px
  source-link-card:
    backgroundColor: "{colors.surface-soft}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"
    border: "1px solid {colors.hairline}"
  text-input:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.body-md}"
    rounded: "{rounded.sm}"
    padding: "12px 16px"
    height: 44px
    border: "1px solid {colors.hairline-strong}"
  text-input-focused:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    border: "2px solid {colors.primary}"
  search-input:
    backgroundColor: "{colors.surface-soft}"
    textColor: "{colors.ink}"
    typography: "{typography.body-md}"
    rounded: "{rounded.full}"
    padding: "10px 20px"
    height: 44px
    border: "1px solid {colors.hairline}"
  top-nav:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.button-md}"
    height: 64px
    border: "0 0 1px {colors.hairline} solid"
  category-tab-strip:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.meta}"
    typography: "{typography.button-sm}"
    border: "0 0 1px {colors.hairline} solid"
  category-tab:
    backgroundColor: transparent
    textColor: "{colors.meta}"
    typography: "{typography.button-sm}"
    padding: "{spacing.sm} {spacing.md}"
  category-tab-active:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    typography: "{typography.button-sm}"
    padding: "{spacing.sm} {spacing.md}"
    border: "0 0 2px {colors.ink} solid"
  language-pill:
    backgroundColor: "{colors.surface-soft}"
    textColor: "{colors.ink}"
    typography: "{typography.button-sm}"
    rounded: "{rounded.full}"
    padding: "6px 12px"
    height: 32px
  pagination-button:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.button-sm}"
    rounded: "{rounded.sm}"
    padding: "8px 14px"
    border: "1px solid {colors.hairline}"
    height: 36px
  empty-state:
    backgroundColor: "{colors.surface-soft}"
    textColor: "{colors.meta}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: "{spacing.2xl}"
    border: "1px solid {colors.hairline-soft}"
  footer-region:
    backgroundColor: "{colors.surface-soft}"
    textColor: "{colors.body}"
    typography: "{typography.body-sm}"
    padding: "{spacing.section} {spacing.xl}"
    border: "1px solid 0 0 0 {colors.hairline}"
  footer-link:
    backgroundColor: transparent
    textColor: "{colors.body}"
    typography: "{typography.body-sm}"
---

# 1. everytldr Design System.

Design specification for a multilingual news-summarisation web service supporting Korean and English with light and dark themes.

## 1.1. Purpose.

Define tokens, components, layouts, and rules for a card-grid list page and a focused reading detail page with anonymous likes and threaded comments. Reference UX: news.hada.io, Naver / Daum 뉴스 (Korean news portals).

## 1.2. PRD Scope.

| Requirement | Spec |
|---|---|
| Categories | Closed enum; MVP = `football` only |
| Summary languages | Korean + English; extensible |
| Article list | Title + 2-line summary + thumbnail + category + time + likes + comments |
| Article detail | Summary + source link + view count + like + comment thread |
| Likes | Anonymous; one per reader; toggleable |
| Comments | Anonymous; threaded; soft-deletable; auto-generated nicknames |
| Themes | Light + Dark + Auto (follows OS) |

## 1.3. Glossary.

| Term | Definition |
|---|---|
| Pretendard | Open-source Korean variable typeface; metric-compatible with Inter |
| Inter | Open-source Latin sans-serif; reference metric for Pretendard |
| Hangul | Korean writing system; syllabic blocks |
| Jamo (자모) | Component letter of a Hangul syllable |
| Myungjo (명조) | Korean serif typeface; reads as old-print / funerary |
| Locale | A `(language, region)` pair driving content + display rules |
| FOIT | Flash of Invisible Text during font load |
| FOUT | Flash of Unstyled Text during font load |
| `prefers-color-scheme` | CSS media query exposing OS-level dark/light preference |
| WCAG AA | Web accessibility standard: 4.5:1 contrast for body text, 3:1 for large text and UI components |
| OLED | Display technology emitting no light at black pixels; #000000 produces "scroll smear" artifact |
| SSR | Server-Side Rendering: HTML produced server-side before browser receives it |
| PRD | Product Requirements Document |
| MVP | Minimum Viable Product |

# 2. Design Lineage.

The system composes parts of three external systems with one project-original innovation.

## 2.1. Adopted Patterns.

| Pattern | Source | Reason |
|---|---|---|
| Card-grid density (16–24px gutters, 64px section) | Airbnb | Marketplace card-grid maps 1:1 to news feed; denser than typical 80–96px SaaS section |
| Photo-first article card schema | Airbnb | `property-card` slot order (thumbnail → badge → title → meta → action) matches news card |
| Modest display weights (500–700) | Airbnb | Hangul reads "blocky" at 700+; weight 800–900 reads as black squares |
| Single accent + photo-led visual heft | Airbnb | News thumbnails carry weight; type recedes |
| Sober rectangles (8px button, 12px card) | Notion | Editorial / docs register; not consumer marketplace |
| Pastel category-tint badges (7 variants) | Notion | Mature, accessible, multi-hue tag system |
| Comment / threaded discussion | Notion | Only source with stacked-authored-content components |
| Hairline-as-elevation | Notion + Verge | 1px border replaces shadow; pairs with single hover-shadow tier |
| Inter-metric-compatible UI font | Notion | Pretendard substitutes cleanly without re-laying body text |
| Mono-numeric timestamp | The Verge | News-feed timestamp convention; "5h ago" |
| Per-locale font scaling | Project-original | None of three sources address bilingual KR/EN typography |

## 2.2. Rejected Patterns.

| Pattern | Source | Reason |
|---|---|---|
| Acid-mint + ultraviolet hazard palette | The Verge | Too loud for serious foreign-news service to Korean readers |
| Manuka 107px display | The Verge | Hangul at that scale reads as advertisement, not news |
| Serif body (FK Roman) | The Verge | Korean serif (myungjo) carries old-print / funerary register |
| Rausch (#ff385c) primary | Airbnb | Too consumer-marketplace for a reading service |
| Pricing tiers, workspace mockup hero | Notion | No matching SaaS sales motion |

# 3. Tokens.

## 3.1. Color.

### 3.1.1. Light Palette.

| Token | Hex | Use |
|---|---|---|
| `primary` | #0a66ff | Primary CTA, link, focus ring (Read Blue) |
| `primary-pressed` | #0850cc | Press state |
| `primary-disabled` | #cce0ff | Disabled CTA |
| `on-primary` | #ffffff | Text on Read Blue |
| `like-active` | #e11d48 | Filled-heart state ONLY |
| `like-inactive` | #6a6a6a | Outline-heart state |
| `canvas` | #ffffff | Page floor; default card surface |
| `surface-soft` | #f7f7f7 | Footer band, source-link card, nested replies |
| `surface-strong` | #f2f2f2 | Icon-button surface; disabled-input fill |
| `hairline` | #e5e3df | Default 1px border (warm neutral) |
| `hairline-soft` | #ebebeb | Long-scroll dividers |
| `hairline-strong` | #c1c1c1 | Input outlines |
| `ink` | #1a1a1a | Headlines, primary text |
| `body` | #3f3f3f | Long-form summary text |
| `meta` | #6a6a6a | Card meta, timestamps, footer links |
| `meta-soft` | #929292 | Disabled link text |

The token name is `meta`, not `muted`, to avoid colliding with shadcn's `--color-muted` (which means a *subtle surface*, not a text colour). shadcn-style `bg-muted` continues to work via the `surface-soft` alias defined in `shadcn.css`.

`ink` = #1a1a1a (not Airbnb's #222) because Hangul stroke density makes #222 perceptually one shade lighter than Latin on white.

### 3.1.2. Dark Palette.

Each light token has a `-dark` companion. Hex values are hand-tuned, not arithmetic inversions. Rationale: § 3.1.5.

| Token | Hex | Light counterpart |
|---|---|---|
| `primary-dark` | #5e93ff | `primary` |
| `primary-pressed-dark` | #4d82e8 | `primary-pressed` |
| `primary-disabled-dark` | #1f3055 | `primary-disabled` |
| `like-active-dark` | #fb7185 | `like-active` |
| `like-inactive-dark` | #8a8a92 | `like-inactive` |
| `canvas-dark` | #131316 | `canvas` |
| `surface-soft-dark` | #1c1c20 | `surface-soft` |
| `surface-strong-dark` | #26262b | `surface-strong` |
| `hairline-dark` | #2a2a2f | `hairline` |
| `hairline-soft-dark` | #1f1f23 | `hairline-soft` |
| `hairline-strong-dark` | #3d3d44 | `hairline-strong` |
| `ink-dark` | #f5f5f7 | `ink` |
| `body-dark` | #c8c8d0 | `body` |
| `meta-dark` | #8a8a92 | `meta` |
| `meta-soft-dark` | #6a6a72 | `meta-soft` |

### 3.1.3. Category Tints.

Seven variants. MVP exercises `emerald` only; remainder are extensible reserves for the closed category enum (§ 1.2.).

| Variant | Light bg / text | Dark bg / text | Recommended category |
|---|---|---|---|
| Emerald | #d9f3e1 / #15803d | #0e2419 / #6ee7b7 | Football (MVP) |
| Sky | #dcecfa / #0369a1 | #0a1e30 / #7dd3fc | Tech |
| Rose | #fde0ec / #be123c | #2c1019 / #fda4af | Entertainment |
| Peach | #ffe8d4 / #c2410c | #29170c / #fdba74 | Economy |
| Lavender | #e6e0f5 / #6b21a8 | #1d172e / #c4b5fd | Politics |
| Yellow | #fef7d6 / #854d0e | #292210 / #fde047 | General |
| Gray | #f0eeec / #404040 | #232328 / #d4d4d8 | Fallback |

All pairs ≥ 7:1 contrast (well above WCAG AA — § 1.3.).

A finite, hand-balanced set (vs. free-form colour) guarantees that any new category added later picks from a tested pool, preventing accessibility-failure drift.

### 3.1.4. Semantic.

| Token | Light | Dark |
|---|---|---|
| `semantic-success` | #15803d | #34d399 |
| `semantic-warning` | #ea580c | #fb923c |
| `semantic-error` | #dc2626 | #f87171 |

### 3.1.5. Dark-Palette Rationale.

| Decision | Reason |
|---|---|
| `canvas-dark` = #131316, NOT #000000 | Pure black causes OLED (§ 1.3.) scroll smear and harsh perception. #131316 matches BBC News, NYT, The Guardian, Naver Dark, Daum Dark canon. |
| `ink-dark` = #f5f5f7, NOT #ffffff | Pure white on dark vibrates at small sizes (especially Hangul). Apple macOS dark precedent. |
| `primary-dark` = #5e93ff (lifted from #0a66ff) | #0a66ff on `canvas-dark` is ~4.0:1 — fails WCAG AA (§ 1.3.). Lifted variant is ~5.4:1. Apple `primary-on-dark` (#2997ff) precedent. |
| `like-active-dark` = #fb7185 (lifted from #e11d48) | #e11d48 on dark over-saturates and reads as alert; softer rose retains affirmation register. |
| Surface ladder roles invert (cards on `surface-soft-dark`, not `canvas-dark`) | Additive light reads "raised" on dark; cards must be lighter than container to feel elevated. |
| Light tints replaced with deep-bg + bright-text pairs | Light pastel on dark canvas glows as halo. Inverted luminance with same hue identity preserves recognition. |

## 3.2. Typography.

### 3.2.1. Font Family.

```
'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont,
system-ui, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif
```

Mono fallback (timestamps only):
```
'JetBrains Mono', 'IBM Plex Mono', ui-monospace, SFMono-Regular, Menlo, monospace
```

| Decision | Reason |
|---|---|
| Single family across UI | Korean serif (myungjo — § 1.3.) reads as old-print/funerary; serif/sans contrast does not register on Hangul syllabic blocks |
| Pretendard chosen | Inter-metric-compatible (§ 1.3.); used by Toss, Daangn (modern Korean product canon); SIL OFL — no licensing risk |
| Mono restricted to numerics | Hangul in monospace breaks compositional rhythm |
| `font-display: swap` mandatory | FOUT (§ 1.3.) acceptable; FOIT (§ 1.3.) blocks time-sensitive news scanning |

### 3.2.2. Hierarchy.

English baseline. Korean overrides: § 3.2.3.

| Token | Size (px) | Weight | Line-height | Letter-spacing (px) | Use |
|---|---|---|---|---|---|
| `hero-display` | 56 | 700 | 1.10 | -1 | Marketing landing (rare) |
| `display-xl` | 40 | 700 | 1.15 | -0.5 | Article-detail `<h1>` |
| `display-lg` | 32 | 700 | 1.20 | -0.5 | List section header |
| `display-md` | 24 | 700 | 1.30 | -0.25 | Featured card title |
| `display-sm` | 20 | 600 | 1.35 | 0 | Default card title |
| `title-md` | 16 | 600 | 1.40 | 0 | Comment author, source publisher |
| `title-sm` | 14 | 600 | 1.40 | 0 | Footer column heads |
| `body-lg` | 18 | 400 | 1.65 | 0 | Article-detail summary (reading register) |
| `body-md` | 16 | 400 | 1.60 | 0 | Comment body, default |
| `body-sm` | 14 | 400 | 1.55 | 0 | Card summary, secondary copy |
| `caption` | 13 | 500 | 1.40 | 0 | Like / comment counts |
| `caption-mono` | 12 | 500 | 1.40 | 0.2 | Relative-time timestamps |
| `micro` | 11 | 600 | 1.30 | 0.4 | Category badge text |
| `button-md` | 15 | 600 | 1.25 | 0 | Default button label |
| `button-sm` | 14 | 500 | 1.25 | 0 | Pill filter, language pill |

### 3.2.3. Korean Overrides.

Apply via `:lang(ko)` selector. Mechanism: § 4.5.

| Token | English | Korean (size / line-height / letter-spacing) |
|---|---|---|
| `hero-display` | 56 / 1.10 / -1 | 44 / 1.25 / 0 |
| `display-xl` | 40 / 1.15 / -0.5 | 32 / 1.30 / 0 |
| `display-lg` | 32 / 1.20 / -0.5 | 26 / 1.35 / 0 |
| `display-md` | 24 / 1.30 / -0.25 | 20 / 1.40 / 0 |
| `display-sm` | 20 / 1.35 / 0 | 18 / 1.45 / 0 |
| `title-*` | unchanged | size unchanged; line-height +0.05 |
| `body-*` | unchanged | size unchanged; line-height +0.05 |
| `caption` / `micro` / `button-*` | unchanged | unchanged |

### 3.2.4. Rules.

| Rule | Reason |
|---|---|
| Negative letter-spacing on Latin only | Hangul jamo (§ 1.3.) collide visually at negative tracking |
| Body line-height +0.05 in Korean | Hangul x-height + full-block glyph requires more leading than Latin descenders |
| Weight ladder = 400 / 500 / 600 / 700 only | 300 too thin for Hangul small sizes; 800+ becomes black squares |

## 3.3. Spacing.

Base unit 4px. Tokens: `2xs=4 xs=8 sm=12 md=16 lg=24 xl=32 2xl=48 section=64`.

Section vertical padding 64px is denser than typical 80–96px SaaS marketing — matches news-feed expectation (§ 2.1.).

## 3.4. Radius.

| Token | px | Use |
|---|---|---|
| `none` | 0 | Reserved |
| `xs` | 4 | Category badge |
| `sm` | 8 | Buttons, inputs, pagination |
| `md` | 12 | Cards, comment-textarea, source-link card |
| `lg` | 16 | Featured cards, modals |
| `xl` | 24 | Reserved |
| `full` | 9999 | Pills, search input, like button, icon button |

Card radius 12px (not Airbnb's 14px) for editorial register; composes against 8px button radius as 4px-step nesting.

## 3.5. Elevation.

| Level | Light | Dark | Use |
|---|---|---|---|
| 0 (flat) | 1px `hairline` border | 1px `hairline-dark` border | All defaults |
| 1 (hover) | `box-shadow: rgba(0,0,0,0.04) 0 2px 6px, rgba(0,0,0,0.08) 0 4px 12px` | Background lifts to `surface-strong-dark`; hairline → `rgba(255,255,255,0.08)` | Card hover; dropdowns |
| Modal scrim | `scrim` at 50% opacity | `scrim` at 65% opacity | Modals, sheets |

| Decision | Reason |
|---|---|
| Shadow → surface-lift on dark | Drop shadow on dark reads as smudge; precedent: GitHub, Linear, Vercel, Apple macOS |
| Scrim opacity bumped 50→65% on dark | 50% reads as content-disappeared because scrim darkens already-dark canvas |
| Two tiers (not Notion's four) | No marketing pricing-tier or workspace-mockup surfaces |

# 4. Theming.

## 4.1. State Model.

| State | Behaviour |
|---|---|
| `auto` (default) | Follows OS `prefers-color-scheme` (§ 1.3.) |
| `light` | Forced light regardless of OS |
| `dark` | Forced dark regardless of OS |

Mirrors PRD §2.3 language preference state model.

## 4.2. Persistence.

Single anonymous cookie shared with language preference. Cookie key `theme`; values `auto | light | dark`; default `auto`.

## 4.3. CSS Mechanism.

Tokens exposed as CSS custom properties on `:root`. Components reference `var(--colors-*)` exclusively — no hard-coded hex.

```css
:root {
  --colors-canvas: #ffffff;
  --colors-ink: #1a1a1a;
  --colors-primary: #0a66ff;
  --colors-hairline: #e5e3df;
}

@media (prefers-color-scheme: dark) {
  :root {
    --colors-canvas: #131316;
    --colors-ink: #f5f5f7;
    --colors-primary: #5e93ff;
    --colors-hairline: #2a2a2f;
  }
}

[data-theme="light"] { /* explicit overrides — win over @media */ }
[data-theme="dark"]  { /* explicit overrides — win over @media */ }
```

| Property | Reason |
|---|---|
| CSS variables, not separate stylesheets | Hot-swap on toggle without React re-render; SSR (§ 1.3.) safe |
| `[data-theme]` set on `<html>` server-side from cookie before paint | Prevents FOIT (§ 1.3.) on first render |
| Explicit `[data-theme]` overrides win over `@media` | User's manual choice supersedes OS state |

## 4.4. Toggle UI.

`icon-button-circle` (§ 5.1.) in top-nav next to language pill. Cycle: `auto → light → dark → auto`. Icons: half-sun-half-moon / sun / moon. Mobile collapses into settings drawer.

Cycle `auto → light → dark` (not `auto → dark`) so a user wanting OS-sync after exploring fixed modes returns to `auto` within one full revolution.

## 4.5. Theme × Locale Composition.

Theme = value swap (CSS variable). Locale = property swap (`:lang(ko)` rewrites size / leading / tracking — § 3.2.3.). Independent selectors compose without conflict.

```css
.display-xl {
  font-size: 40px;
  line-height: 1.15;
  letter-spacing: -0.5px;
  color: var(--colors-ink);
}
:lang(ko) .display-xl {
  font-size: 32px;
  line-height: 1.30;
  letter-spacing: 0;
}
```

Six render states (3 themes × 2 locales) handled with no per-state class duplication.

# 5. Components.

All components consume token references only — no inline hex / px (§ 7.1.).

## 5.1. Buttons.

| Component | Background | Text | Radius | Padding | Height |
|---|---|---|---|---|---|
| `button-primary` | `primary` | `on-primary` | `sm` | 10×18 | 44 |
| `button-primary-pressed` | `primary-pressed` | `on-primary` | `sm` | — | — |
| `button-primary-disabled` | `primary-disabled` | `on-primary` | `sm` | — | — |
| `button-secondary` | `canvas` | `ink` | `sm` (1px `hairline-strong` border) | 10×18 | 44 |
| `button-ghost` | transparent | `ink` | `sm` | 8×14 | — |
| `button-link` | transparent | `primary` | — | 0 | — |
| `button-pill-filter` | `canvas` | `body` | `full` (1px `hairline` border) | 8×16 | 36 |
| `button-pill-filter-active` | `ink` | `on-dark` | `full` | 8×16 | 36 |
| `icon-button-circle` | `surface-strong` | `ink` | `full` | — | 36 |

Active filter uses `ink`, not `primary`: Read Blue is reserved for primary-action affordances.

## 5.2. Article Card.

The list-page workhorse. Anatomy:

| Slot | Content | Token |
|---|---|---|
| Photo plate | 16:9 thumbnail; `border-radius: md md 0 0` | `article-card-photo` |
| Category badge | One of seven tints (§ 5.3.) | — |
| Title | Article title; line-clamp 2 | `display-sm` |
| Summary | 2-line truncated summary | `body-sm`, color `body` |
| Meta | publisher · timestamp · likes · comments | `caption` / `caption-mono` |

| State | Light | Dark |
|---|---|---|
| Default | `canvas` bg, 1px `hairline` | `surface-soft-dark` bg, 1px `hairline-dark` |
| Hover | shadow Level 1 (§ 3.5.); title → `primary` | bg → `surface-strong-dark`; title → `primary-dark` |

Variants: `article-card-featured` (16px radius, `display-md` title); `article-card-compact` (no photo; sidebar use).

Photo fallback: when source has no thumbnail, render `surface-soft` block at 16:9 with category badge centred at 1.5× size. Mandatory — grid alignment depends on every card having a 16:9 lead element.

## 5.3. Category Badge.

Seven variants: `category-badge-{emerald|sky|rose|peach|lavender|yellow|gray}` (§ 3.1.3.).

| Property | Value |
|---|---|
| Typography | `micro` (11/600/0.4px) |
| Radius | `xs` (4px) |
| Padding | 4×8 |

MVP exercises `emerald` only.

## 5.4. Like Button.

Two distinct components — colour and icon swap:

| State | Icon | Text colour | Padding | Radius |
|---|---|---|---|---|
| `like-button` | outline heart | `meta` | 6×10 | `full` |
| `like-button-active` | filled heart | `like-active` (light) / `like-active-dark` (dark) | 6×10 | `full` |

Both render count in `caption`. Counts shorten beyond 999 → `1.2k`, `12k`, `1.2m`. Korean locale: `1.2천`, `1.2만` (Naver convention).

Count text stays in `meta` even when active — only the icon flips colour. Reason: full-colour count makes meta-strip read as alert.

## 5.5. Comment Thread.

| Component | Surface | Indent | Radius |
|---|---|---|---|
| `comment-thread` | `canvas` | — | — |
| `comment-card` (top-level) | transparent | 0 | 0; bottom 1px `hairline-soft` |
| `comment-card-nested` (reply) | `surface-soft` | 32 | `md` |
| `comment-textarea` | `canvas` | — | `md`; min-height 96; focus border 2px `primary` |

Per-card layout: avatar (32×32 circle) + nickname (`title-md`) + timestamp (`caption-mono`) + body (`body-md`) + action bar (like, reply).

Depth cap = 2. Replies-to-replies render flat with `@nickname` mention prefix. Reason: deeper indents break visually on mobile.

Soft-delete: deleted comments removed from DOM. No `[deleted]` placeholder. Threads collapse around the absence (PRD §2.7).

## 5.6. Source Link Card.

Sits above comment thread on detail page (PRD §2.8 attribution on primary scroll path).

| Property | Value |
|---|---|
| Background | `surface-soft` |
| Radius | `md` |
| Border | 1px `hairline` |
| Padding | `md` |
| Line 1 | Publisher name (`title-md` `ink`) |
| Line 2 | "View original article →" (`body-sm` `primary`) |
| Anchor attrs | `target="_blank"` `rel="noopener noreferrer"` |

## 5.7. Inputs.

| Component | Background | Radius | Padding | Height | Border |
|---|---|---|---|---|---|
| `text-input` | `canvas` | `sm` | 12×16 | 44 | 1px `hairline-strong` |
| `text-input-focused` | `canvas` | `sm` | 12×16 | 44 | 2px `primary` |
| `search-input` | `surface-soft` | `full` | 10×20 | 44 | 1px `hairline` |
| `comment-textarea` | see § 5.5. | — | — | — | — |

Validation error: border switches to `semantic-error`; helper text below in `caption` `semantic-error`.

## 5.8. Navigation.

| Component | Property | Value |
|---|---|---|
| `top-nav` | Background | `canvas` (light) / `canvas-dark` (dark) |
| | Height | 64; sticky |
| | Border | 1px bottom `hairline` |
| | Left | logo (max 28px height) |
| | Right | language pill + theme toggle (§ 4.4.) + settings icon |
| `category-tab-strip` | Border | 1px bottom `hairline` |
| `category-tab-active` | Active marker | 2px `ink` underline (NOT `primary`) |
| `language-pill` | Background | `surface-soft` |
| | Radius | `full`; padding 6×12; height 32 |

Active category tab uses `ink`, not `primary` — same logic as filter pill (§ 5.1.).

Mobile: language pill + theme toggle collapse into settings drawer.

## 5.9. Footer.

| Property | Value |
|---|---|
| Background | `surface-soft` |
| Padding | section × xl |
| Border | 1px top `hairline` |
| Layout | 3-column at desktop (Service / Categories / About); accordion below 768px |
| Link | `body-sm` `body` (hover → `ink`) |

## 5.10. Pagination.

`pagination-button` — 36px height, `sm` radius, `button-sm` typography, 1px `hairline` border. Active page: `ink` fill + `on-dark` text.

Mobile defaults to "Load more" single button. Tablet+ uses numbered pagination.

## 5.11. Empty State.

`surface-soft` card, `body-md` `meta`, `md` radius, `2xl` padding. Used for: empty category filter, empty comment thread, empty-language fallback prompt.

## 5.12. Modal.

`canvas` surface, `lg` radius. Scrim per § 3.5. Desktop 480px wide; mobile full-bleed slide-up.

# 6. Layouts.

## 6.1. Article List.

Top-to-bottom: top-nav (sticky) → category-tab-strip (sticky) → 3-column card grid → pagination → footer.

| Breakpoint | Grid |
|---|---|
| ≥ 1024px | 3-column |
| 768–1023 | 2-column |
| < 768 | 1-column |

Gutter 16. 3-up cap (not 4-up): at 1280px content / 4 cols / 16 gutter the summary line drops to ~50–55 chars, forcing line-clamps that lose information. 3-up gives ~70–75 chars per line — comfortable for both Korean and English.

Multi-select category filter NOT supported in MVP (PRD §2.4).

## 6.2. Article Detail.

Single-column 720px reading column. Order:

1. top-nav
2. Hero image (16:9, content-width ~1024px)
3. Category badge (§ 5.3.)
4. Title (`display-xl`)
5. Meta strip: publisher · `caption-mono` timestamp · view count
6. Like button (large variant)
7. `xl` gap
8. Summary body (`body-lg` — reading register)
9. `xl` gap
10. Source-link card (§ 5.6.)
11. `section` (64px) gap
12. Comments header (`display-md` + count in `caption meta`)
13. `comment-textarea` (§ 5.5.)
14. `comment-thread`
15. footer

| Constraint | Spec |
|---|---|
| Reading column max-width | 720px (50–70 char per line at `body-lg` for KR + EN) |
| Source-link card position | ABOVE comment thread (PRD §2.8 attribution on primary scroll path) |
| View count dedup (PRD §2.5) | Server cookie + IP-hash bucket; UI is read-only |
| Like state (PRD §2.6) | `(article, anonymous_reader_id)`; same dedup bucket |

## 6.3. Responsive Behaviour.

| Name | Width | Changes |
|---|---|---|
| Small mobile | < 360 | 1-col grid; nav 56; reply indent 16; hero 21:9 → 16:9 |
| Mobile | 360–767 | 1-col grid; nav 56; "Load more" pagination; modal slide-up |
| Tablet | 768–1023 | 2-col grid; reading column 720 (32px gutters) |
| Desktop | 1024–1279 | 3-col grid; full top-nav |
| Wide | ≥ 1280 | 3-col cap; content cap 1280 |

| Element | Collapse |
|---|---|
| Top nav | Language pill + theme toggle → settings drawer below 768 |
| Category tab strip | Horizontal scroll below 1024; never hamburger (always-visible category nav is news convention) |
| Footer | 3-col → accordion below 768 |
| Reply indent | 32 → 16 below 360 |

Touch targets ≥ 44×44 for primary actions; pill controls 32–36 with extended tap zones (8–12px internal padding).

# 7. Rules.

## 7.1. Do.

- Use `primary` for primary-action affordances; `like-active` for the filled-heart only.
- Use one of the seven `category-badge-*` variants — never invent a tint.
- Place source-link card above comment thread (§ 6.2.).
- Use `caption-mono` for relative-time strings only.
- Use 1px `hairline` (or `-dark`) as default elevation.
- Apply `:lang(ko)` overrides for every display token (§ 3.2.3.).
- Reference colours via `var(--colors-*)` — never hard-code hex (§ 4.3.).
- Set `[data-theme]` on `<html>` server-side from cookie before first paint (§ 4.3.).
- Render `surface-soft` (or `-dark`) + centred badge as fallback for any thumbnail-less article (§ 5.2.).
- Cap comment thread depth at 2; use `@nickname` mentions beyond.
- Define `-dark` companion for every new colour token; verify ≥ WCAG AA against `canvas-dark`.

## 7.2. Don't.

- No serif anywhere — light or dark, Korean or English (§ 3.2.1.).
- No negative letter-spacing on Korean (§ 3.2.4.).
- No `primary` on active filter / category tab / heart-filled (§ 5.1., § 5.4., § 5.8.).
- No `like-active` outside the filled-heart state (§ 5.4.).
- No second accent colour — extend via category tints first (§ 3.1.3.).
- No default shadow on cards — hover-float only (§ 3.5.).
- No Hangul in `caption-mono` (§ 3.2.4.).
- No `rounded.xl` (24px) without explicit justification (§ 3.4.).
- No background gradients (compete with thumbnails — § 3.1.).
- No `[deleted]` placeholder for soft-deleted comments (§ 5.5.).
- No `like-active` on the count text — only the icon flips (§ 5.4.).
- No #000000 canvas-dark; no #ffffff ink-dark; no shadow on dark; no light tints on dark canvas; no 50% scrim on dark (§ 3.1.5., § 3.5.).
- No mechanical surface-ladder inversion — cards on `surface-soft-dark`, canvas behind is deepest tone (§ 3.1.5.).

# 8. Known Gaps.

| Gap | Recommendation |
|---|---|
| Search results page UI | PRD post-MVP |
| Push notification banner | PRD §6 open question |
| Comment moderation surfaces | PRD §6 open question |
| Skeleton / loading states | `surface-strong` rectangles, 1.5s pulse |
| Form validation helpers | Extend `semantic-error` token usage |
| Languages beyond KR/EN | Per-script density evaluation (§ 3.2.3.) |
| Featured-card selection logic | Product decision pending |
| Bright thumbnail hard-edge in dark | 1px `hairline-dark` ring or 5% darkening overlay |
| Like-button hover lift in dark | Consider 1px `primary-dark` ring instead of surface-lift |
| OS theme transition mid-session | `transition: background-color 200ms, color 200ms, border-color 200ms` on `<html>` and `<body>` |
