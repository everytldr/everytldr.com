---
version: alpha
name: everytldr
---

# 1. everytldr Design System.

Design specification for a multilingual news-summarisation web service supporting Korean and English with light and dark themes.

## 1.1. Purpose.

Define design tokens, primitive UI components, theming behaviour, and authoring rules for the everytldr web product. Feature-level compositions (article cards, comment threads, navigation chrome, page layouts, etc.) live with their owning feature, not in this document. Reference UX: news.hada.io, Naver / Daum 뉴스 (Korean news portals).

## 1.2. PRD Scope.

Aspects of the PRD that constrain this design system.

| Requirement       | Spec                                                                                                   |
| ----------------- | ------------------------------------------------------------------------------------------------------ |
| Categories        | Closed enum; extensible — every category MUST pick from the seven category-tint pairs (§ 3.1.3.)       |
| Summary languages | Korean + English at MVP; extensible — every new language requires per-script density review (§ 3.2.3.) |
| Themes            | Light + Dark + Auto (follows OS) — all tokens MUST define dark counterparts (§ 3.1.2.)                 |

## 1.3. Glossary.

| Term                   | Definition                                                                                                                                                                            |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Pretendard             | Open-source Korean variable typeface; metric-compatible with Inter                                                                                                                    |
| Inter                  | Open-source Latin sans-serif; reference metric for Pretendard                                                                                                                         |
| Hangul                 | Korean writing system; syllabic blocks                                                                                                                                                |
| Jamo (자모)            | Component letter of a Hangul syllable                                                                                                                                                 |
| Myungjo (명조)         | Korean serif typeface; reads as old-print / funerary                                                                                                                                  |
| Locale                 | A `(language, region)` pair driving content + display rules                                                                                                                           |
| FOIT                   | Flash of Invisible Text during font load                                                                                                                                              |
| FOUT                   | Flash of Unstyled Text during font load                                                                                                                                               |
| `prefers-color-scheme` | CSS media query exposing OS-level dark/light preference                                                                                                                               |
| WCAG AA                | Web accessibility standard: 4.5:1 contrast for body text, 3:1 for large text and UI components                                                                                        |
| OLED                   | Display technology emitting no light at black pixels; #000000 produces "scroll smear" artifact                                                                                        |
| SSR                    | Server-Side Rendering: HTML produced server-side before browser receives it                                                                                                           |
| PRD                    | Product Requirements Document                                                                                                                                                         |
| MVP                    | Minimum Viable Product                                                                                                                                                                |
| Tailwind v4            | Utility-first CSS framework with CSS-first configuration via the `@theme` directive; declarations in `@theme` auto-generate matching utilities (`bg-*`, `text-*`, `rounded-*`, `p-*`) |
| shadcn                 | Component registry providing primitive UI scaffolds installed via CLI                                                                                                                 |
| lucide                 | Open-source SVG icon set; default icon source for `icon-button-circle` (§ 5.1.3.)                                                                                                     |
| Read Blue              | Project name for the `primary` accent; #0a66ff light, #5e93ff dark (§ 3.1.1., § 3.1.2.)                                                                                               |

# 2. Design Lineage.

The system composes parts of three external systems with one project-original innovation.

## 2.1. Adopted Patterns.

| Pattern                                          | Source           | Reason                                                                                          |
| ------------------------------------------------ | ---------------- | ----------------------------------------------------------------------------------------------- |
| Dense gutters (16–24px) and 64px section spacing | Airbnb           | Marketplace density maps 1:1 to news-feed expectation; denser than typical 80–96px SaaS section |
| Modest display weights (500–700)                 | Airbnb           | Hangul reads "blocky" at 700+; weight 800–900 reads as black squares                            |
| Single accent + image-led visual heft            | Airbnb           | News imagery carries weight; type recedes                                                       |
| Sober rectangles (8px button, 12px container)    | Notion           | Editorial / docs register; not consumer marketplace                                             |
| Pastel category-tint pairs (7 variants)          | Notion           | Mature, accessible, multi-hue tag system                                                        |
| Hairline-as-elevation                            | Notion + Verge   | 1px border replaces shadow; pairs with single hover-shadow tier                                 |
| Inter-metric-compatible UI font                  | Notion           | Pretendard substitutes cleanly without re-laying body text                                      |
| Mono-numeric numerics                            | The Verge        | Timestamps and metric counters anchor to a tabular mono register                                |
| Per-locale font scaling                          | Project-original | None of three sources address bilingual KR/EN typography                                        |

## 2.2. Rejected Patterns.

| Pattern                                | Source    | Reason                                                       |
| -------------------------------------- | --------- | ------------------------------------------------------------ |
| Acid-mint + ultraviolet hazard palette | The Verge | Too loud for serious foreign-news service to Korean readers  |
| Manuka 107px display                   | The Verge | Hangul at that scale reads as advertisement, not news        |
| Serif body (FK Roman)                  | The Verge | Korean serif (myungjo) carries old-print / funerary register |
| Rausch (#ff385c) primary               | Airbnb    | Too consumer-marketplace for a reading service               |
| Pricing tiers, workspace mockup hero   | Notion    | No matching SaaS sales motion                                |

# 3. Tokens.

## 3.1. Color.

Color tokens live in the `@theme` block of `src/app/styles/theme.css`. Each `--color-{token}` auto-generates Tailwind utilities `bg-{token}`, `text-{token}`, `border-{token}`, `ring-{token}`, etc. Dark values rebind the same name inside `.dark` — there are no `-dark` utilities (§ 3.1.2.).

### 3.1.1. Light Palette.

| Token              | Hex     | Use                                                                                                                                        |
| ------------------ | ------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `primary`          | #0a66ff | Read Blue (§ 1.3.). Reserved for primary CTAs, links, and focus rings; never for selection markers, toggles, or stateful indicators (§ 6.) |
| `primary-hover`    | #095ce6 | `:hover` on `primary` CTA / link                                                                                                           |
| `primary-pressed`  | #0850cc | `:active` (held-down) on `primary` CTA / link                                                                                              |
| `primary-disabled` | #cce0ff | Disabled CTA                                                                                                                               |
| `on-primary`       | #ffffff | Text on Read Blue                                                                                                                          |
| `on-ink`           | #ffffff | Text on `ink` fill (e.g. `button-pill-filter-active`, active `pagination-button` — §§ 5.1.1., 5.3.). Rebinds on dark.                       |
| `like-active`      | #e11d48 | Filled-heart state ONLY                                                                                                                    |
| `like-inactive`    | #6a6a6a | Outline-heart state                                                                                                                        |
| `canvas`           | #ffffff | Page floor (light); see § 3.1.6. for surface-ladder semantics                                                                              |
| `surface-soft`     | #f7f7f7 | Resting fill for static raised regions and chip-style controls; see § 3.1.6.                                                               |
| `surface-strong`   | #f2f2f2 | `:hover` escalation for `surface-soft`-rest chip controls; disabled-input fill (§ 3.1.6.)                                                  |
| `surface-pressed`  | #e8e8e8 | `:active` (held-down) escalation; ladder ceiling (§ 3.1.6.)                                                                                |
| `hairline`         | #e5e3df | Default 1px border (warm neutral)                                                                                                          |
| `hairline-soft`    | #ebebeb | Long-scroll dividers                                                                                                                       |
| `hairline-strong`  | #c1c1c1 | Input outlines                                                                                                                             |
| `ink`              | #1a1a1a | Headlines, primary text                                                                                                                    |
| `body`             | #3f3f3f | Long-form reading text                                                                                                                     |
| `meta`             | #6a6a6a | Secondary text, timestamps, captions                                                                                                       |
| `meta-soft`        | #929292 | Disabled-text and lowest-emphasis labels                                                                                                   |
| `scrim`            | #000000 | Modal overlay base; opacity applied at usage site — `bg-scrim/50` (light), `bg-scrim/65` (dark). See § 3.5.                                |

The token name is `meta`, not `muted`, to avoid colliding with shadcn's `--color-muted` (which means a _subtle surface_, not a text colour). shadcn-style `bg-muted` continues to work via the `surface-soft` alias defined in `shadcn.css`.

`scrim` does not rebind on `.dark` (the hex stays #000000 across themes); only the opacity multiplier in § 3.5. differs.

`ink` = #1a1a1a (not Airbnb's #222) because Hangul stroke density makes #222 perceptually one shade lighter than Latin on white.

### 3.1.2. Dark Palette.

Dark-mode hex values rebind the **same** custom property name within `.dark` (e.g. `--color-canvas` flips from `#ffffff` to `#131316`). There is no separate `bg-canvas-dark` Tailwind utility — `bg-canvas` automatically swaps when `<html class="dark">` is applied (§ 4.3.). The `-dark` suffix in the table below labels the override **value**, not a distinct token.

Hex values are hand-tuned, not arithmetic inversions. Rationale: § 3.1.5.

| Token                   | Hex     | Light counterpart  |
| ----------------------- | ------- | ------------------ |
| `primary-dark`          | #5e93ff | `primary`          |
| `primary-hover-dark`    | #5589f4 | `primary-hover`    |
| `primary-pressed-dark`  | #4d82e8 | `primary-pressed`  |
| `primary-disabled-dark` | #1f3055 | `primary-disabled` |
| `on-ink-dark`           | #131316 | `on-ink`           |
| `like-active-dark`      | #fb7185 | `like-active`      |
| `like-inactive-dark`    | #8a8a92 | `like-inactive`    |
| `canvas-dark`           | #131316 | `canvas`           |
| `surface-soft-dark`     | #1c1c20 | `surface-soft`     |
| `surface-strong-dark`   | #26262b | `surface-strong`   |
| `surface-pressed-dark`  | #2f2f35 | `surface-pressed`  |
| `hairline-dark`         | #2a2a2f | `hairline`         |
| `hairline-soft-dark`    | #1f1f23 | `hairline-soft`    |
| `hairline-strong-dark`  | #3d3d44 | `hairline-strong`  |
| `ink-dark`              | #f5f5f7 | `ink`              |
| `body-dark`             | #c8c8d0 | `body`             |
| `meta-dark`             | #8a8a92 | `meta`             |
| `meta-soft-dark`        | #6a6a72 | `meta-soft`        |

### 3.1.3. Category Tints.

Seven hand-balanced bg/text pairs, exposed as `tint-{hue}` / `text-{hue}` token pairs. Each pair is a closed accessible palette intended for tag/chip/badge surfaces that need a categorical hue beyond `primary` and `like-active`.

| Variant  | Light bg / text   | Dark bg / text    |
| -------- | ----------------- | ----------------- |
| Emerald  | #d9f3e1 / #15803d | #0e2419 / #6ee7b7 |
| Sky      | #dcecfa / #0369a1 | #0a1e30 / #7dd3fc |
| Rose     | #fde0ec / #be123c | #2c1019 / #fda4af |
| Peach    | #ffe8d4 / #c2410c | #29170c / #fdba74 |
| Lavender | #e6e0f5 / #6b21a8 | #1d172e / #c4b5fd |
| Yellow   | #fef7d6 / #854d0e | #292210 / #fde047 |
| Gray     | #f0eeec / #404040 | #232328 / #d4d4d8 |

All pairs ≥ 7:1 contrast (well above WCAG AA — § 1.3.).

A finite, hand-balanced set (vs. free-form colour) guarantees that any new categorical tint picks from a tested pool, preventing accessibility-failure drift.

### 3.1.4. Semantic.

| Token              | Light   | Dark    |
| ------------------ | ------- | ------- |
| `semantic-success` | #15803d | #34d399 |
| `semantic-warning` | #ea580c | #fb923c |
| `semantic-error`   | #dc2626 | #f87171 |

### 3.1.5. Dark-Palette Rationale.

| Decision                                                                                 | Reason                                                                                                                                                                        |
| ---------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `canvas-dark` = #131316, NOT #000000                                                     | Pure black causes OLED (§ 1.3.) scroll smear and harsh perception. #131316 matches BBC News, NYT, The Guardian, Naver Dark, Daum Dark canon.                                  |
| `ink-dark` = #f5f5f7, NOT #ffffff                                                        | Pure white on dark vibrates at small sizes (especially Hangul). Apple macOS dark precedent.                                                                                   |
| `primary-dark` = #5e93ff (lifted from #0a66ff)                                           | #0a66ff on `canvas-dark` is ~4.0:1 — fails WCAG AA (§ 1.3.). Lifted variant is ~5.4:1. Apple `primary-on-dark` (#2997ff) precedent.                                           |
| `primary-hover-dark` = #5589f4, `primary-pressed-dark` = #4d82e8                         | Hover/active on dark must darken (not lift) — additive light on dark reads as "release" not "press". Two-step darken from #5e93ff matches Material density (8% / 16%).        |
| `like-active-dark` = #fb7185 (lifted from #e11d48)                                       | #e11d48 on dark over-saturates and reads as alert; softer rose retains affirmation register.                                                                                  |
| Surface ladder (§ 3.1.6.) roles invert (cards on `surface-soft-dark`, not `canvas-dark`) | Additive light reads "raised" on dark; cards must be lighter than container to feel elevated.                                                                                 |
| `surface-pressed-dark` = #2f2f35 (lifted from #26262b)                                   | Symmetric to light: extends ladder one tier above hover for `:active` feedback. Stays below `hairline-strong-dark` (#3d3d44) so chip fills never collide with input outlines. |
| Light tints replaced with deep-bg + bright-text pairs                                    | Light pastel on dark canvas glows as halo. Inverted luminance with same hue identity preserves recognition.                                                                   |

### 3.1.6. Surface Ladder Semantics.

The surface ladder is closed at four tiers. Each tier maps to a single role; mixing roles across tiers is forbidden.

| Tier              | Light   | Dark    | Role                                                                                                                                | Example                                                                                                 |
| ----------------- | ------- | ------- | ----------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `canvas`          | #ffffff | #131316 | Page floor. Resting fill for components that sit directly on the page on light.                                                     | `<body>`, `text-input` rest, `button-secondary` rest                                                    |
| `surface-soft`    | #f7f7f7 | #1c1c20 | Default raised surface. Resting fill for chip-style controls and static raised regions. On dark, also the canonical container fill. | `icon-button-circle` rest, `search-input` rest, `kbd` marker                                            |
| `surface-strong`  | #f2f2f2 | #26262b | `:hover` escalation for chip-style controls. Static "fixed-emphasis" fill where no further escalation will be applied.              | `icon-button-circle` hover, `search-input` hover, `text-input` disabled fill (terminal), skeleton block |
| `surface-pressed` | #e8e8e8 | #2f2f35 | `:active` (held-down) escalation. Ladder ceiling.                                                                                   | `icon-button-circle` active, `button-ghost` active, `button-secondary` active                           |

| Rule                                                                                                                                                                                                                          | Reason                                                                                                                                                                                                      |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Interactive component rest fills MUST start at `canvas` or `surface-soft`** — never `surface-strong` or `surface-pressed`.                                                                                                  | A chip starting at `surface-strong` has no headroom for `:hover`; one starting at `surface-pressed` has no headroom for either `:hover` or `:active`.                                                       |
| `surface-strong` may serve as a static base ONLY when the element is non-interactive AND visually nested inside a non-canvas container (e.g. a `<kbd>` keyboard-shortcut marker placed inside a `surface-soft` search input). | Stacked depth is conveyed by tier delta, not by promoting interactive components to the ceiling.                                                                                                            |
| `surface-pressed` is reserved for `:active`. It MUST NOT appear as any component's resting fill.                                                                                                                              | The ladder must terminate; promoting any rest above it would break the entire interaction model.                                                                                                            |
| No fifth tier (`surface-deeper`, `surface-stronger`, …) will be added.                                                                                                                                                        | Each new tier creates the same endpoint problem one step higher. Four tiers cover rest + hover + active + terminal-static; further escalation uses `ring-1 ring-inset` or `scale-[0.98]`, not surface tone. |

Dark-mode caveat: on dark, the visual delta between `canvas-dark` (#131316) and `surface-soft-dark` (#1c1c20) is intentionally narrow (per § 3.1.5.). When a `bg-canvas`-resting element (e.g. `button-secondary`) hovers, escalating to `surface-soft-dark` reads as "no change". Such components hover directly to `surface-strong` and press to `surface-pressed`, skipping `surface-soft` on dark. Map this with `dark:hover:bg-surface-strong dark:active:bg-surface-pressed`. § 5.1. tables encode this per variant.

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

| Decision                       | Reason                                                                                                                       |
| ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| Single family across UI        | Korean serif (myungjo — § 1.3.) reads as old-print/funerary; serif/sans contrast does not register on Hangul syllabic blocks |
| Pretendard chosen              | Inter-metric-compatible (§ 1.3.); used by Toss, Daangn (modern Korean product canon); SIL OFL — no licensing risk            |
| Mono restricted to numerics    | Hangul in monospace breaks compositional rhythm                                                                              |
| `font-display: swap` mandatory | FOUT (§ 1.3.) acceptable; FOIT (§ 1.3.) blocks time-sensitive news scanning                                                  |

### 3.2.2. Hierarchy.

Each `--text-{token}` (with companion `--text-{token}--line-height`, `--text-{token}--letter-spacing`, `--text-{token}--font-weight`) generates one Tailwind utility (`text-display-xl`, `text-body-md`, …) that bundles all four properties at once. English baseline; Korean overrides: § 3.2.3.

| Token          | Size (px) | Weight | Line-height | Letter-spacing (px) | Use                                      |
| -------------- | --------- | ------ | ----------- | ------------------- | ---------------------------------------- |
| `hero-display` | 56        | 700    | 1.10        | -1                  | Marketing hero (rare)                    |
| `display-xl`   | 40        | 700    | 1.15        | -0.5                | Page `<h1>`                              |
| `display-lg`   | 32        | 700    | 1.20        | -0.5                | Section heading                          |
| `display-md`   | 24        | 700    | 1.30        | -0.25               | Sub-section / featured heading           |
| `display-sm`   | 20        | 600    | 1.35        | 0                   | Default heading / card-style title       |
| `title-md`     | 16        | 600    | 1.40        | 0                   | Strong inline labels (author, publisher) |
| `title-sm`     | 14        | 600    | 1.40        | 0                   | Tertiary headings, list-section labels   |
| `body-lg`      | 18        | 400    | 1.65        | 0                   | Long-form reading body                   |
| `body-md`      | 16        | 400    | 1.60        | 0                   | Default body                             |
| `body-sm`      | 14        | 400    | 1.55        | 0                   | Secondary copy, summaries                |
| `caption`      | 13        | 500    | 1.40        | 0                   | Counters and inline metrics              |
| `caption-mono` | 12        | 500    | 1.40        | 0.2                 | Tabular numerics (timestamps, counts)    |
| `micro`        | 11        | 600    | 1.30        | 0.4                 | Tag / badge text                         |
| `button-md`    | 15        | 600    | 1.25        | 0                   | Default button label                     |
| `button-sm`    | 14        | 500    | 1.25        | 0                   | Pill / chip label                        |
| `nav-md`       | 18        | 700    | 1.25        | -0.25               | Primary navigation label (tabs, `<nav>`) |
| `nav-sm`       | 12        | 500    | 1.30        | 0                   | Secondary / sub-nav label (tabs, `<nav>`) |

### 3.2.3. Korean Overrides.

Apply via `:lang(ko)` selector. Mechanism: § 4.5.

| Token                            | English           | Korean (size / line-height / letter-spacing) |
| -------------------------------- | ----------------- | -------------------------------------------- |
| `hero-display`                   | 56 / 1.10 / -1    | 44 / 1.25 / 0                                |
| `display-xl`                     | 40 / 1.15 / -0.5  | 32 / 1.30 / 0                                |
| `display-lg`                     | 32 / 1.20 / -0.5  | 26 / 1.35 / 0                                |
| `display-md`                     | 24 / 1.30 / -0.25 | 20 / 1.40 / 0                                |
| `display-sm`                     | 20 / 1.35 / 0     | 18 / 1.45 / 0                                |
| `title-*`                        | unchanged         | size unchanged; line-height +0.05            |
| `body-*`                         | unchanged         | size unchanged; line-height +0.05            |
| `caption` / `micro` / `button-*` | unchanged         | unchanged                                    |
| `nav-md`                         | 18 / 1.25 / -0.25 | 17 / 1.30 / 0                                |
| `nav-sm`                         | 12 / 1.30 / 0     | 12 / 1.35 / 0                                |

### 3.2.4. Rules.

| Rule                                       | Reason                                                                         |
| ------------------------------------------ | ------------------------------------------------------------------------------ |
| Negative letter-spacing on Latin only      | Hangul jamo (§ 1.3.) collide visually at negative tracking                     |
| Body line-height +0.05 in Korean           | Hangul x-height + full-block glyph requires more leading than Latin descenders |
| Weight ladder = 400 / 500 / 600 / 700 only | 300 too thin for Hangul small sizes; 800+ becomes black squares                |

## 3.3. Spacing.

Base unit 4px. Tokens: `2xs=4 xs=8 sm=12 md=16 lg=24 xl=32 2xl=48 section=64`. Each generates `p-*`, `m-*`, `gap-*`, `space-*` utilities (e.g. `p-md` = 16px padding, `gap-section` = 64px gap, `mt-xl` = 32px top margin).

Section vertical padding 64px is denser than typical 80–96px SaaS marketing — matches news-feed expectation (§ 2.1.).

## 3.4. Radius.

Tokens generate `rounded-{token}` utilities. `rounded-none` and `rounded-full` are Tailwind built-ins.

| Token  | px   | Use                              |
| ------ | ---- | -------------------------------- |
| `none` | 0    | Reserved                         |
| `xs`   | 4    | Tag / badge                      |
| `sm`   | 8    | Buttons, inputs, pagination      |
| `md`   | 12   | Containers, textareas            |
| `lg`   | 16   | Featured containers, modals      |
| `xl`   | 24   | Reserved                         |
| `full` | 9999 | Pills, search input, icon button |

Container radius 12px (not Airbnb's 14px) for editorial register; composes against 8px button radius as 4px-step nesting.

## 3.5. Elevation.

Hover shadow defined as `--shadow-hover` (utility: `shadow-hover`).

| Level       | Light                                                       | Dark                   | Tailwind                       |
| ----------- | ----------------------------------------------------------- | ---------------------- | ------------------------------ |
| 0 (flat)    | 1px `hairline` border                                       | 1px `hairline` border (rebound) | `border border-hairline`       |
| 1 (hover)   | `0 2px 6px rgb(0 0 0 / 0.04), 0 4px 12px rgb(0 0 0 / 0.08)` | (same)                 | `hover:shadow-hover`           |
| Modal scrim | `scrim` at 50% opacity                                      | `scrim` at 65% opacity | `bg-scrim/50 dark:bg-scrim/65` |

| Decision                            | Reason                                                                     |
| ----------------------------------- | -------------------------------------------------------------------------- |
| Scrim opacity bumped 50→65% on dark | 50% reads as content-disappeared because scrim darkens already-dark canvas |
| Two tiers (not Notion's four)       | No marketing pricing-tier or workspace-mockup surfaces                     |

# 4. Theming.

## 4.1. State Model.

| State            | Behaviour                                  |
| ---------------- | ------------------------------------------ |
| `auto` (default) | Follows OS `prefers-color-scheme` (§ 1.3.) |
| `light`          | Forced light regardless of OS              |
| `dark`           | Forced dark regardless of OS               |

A client-side theme provider reads the preference and toggles the `.dark` class on `<html>`. Mirrors PRD § 2.3. language preference state model.

## 4.2. Persistence.

Single anonymous cookie shared with language preference. Cookie key `theme`; values `auto | light | dark`; default `auto`.

## 4.3. CSS Mechanism.

Tailwind v4 CSS-first config. Tokens declared in the `@theme` block of `src/app/styles/theme.css` — each `--color-*`, `--text-*`, `--radius-*`, `--spacing-*` token auto-generates a matching utility (`bg-canvas`, `text-display-xl`, `rounded-md`, `p-md`, …). Components compose Tailwind utilities exclusively — no inline hex, no component stylesheets.

```css
/* theme.css */
@theme {
  --color-canvas: #ffffff;
  --color-ink: #1a1a1a;
  --color-primary: #0a66ff;
  --color-hairline: #e5e3df;
}

.dark {
  --color-canvas: #131316;
  --color-ink: #f5f5f7;
  --color-primary: #5e93ff;
  --color-hairline: #2a2a2f;
}

/* globals.css */
@custom-variant dark (&:where(.dark, .dark *));
```

| Property                                                     | Reason                                                                                                   |
| ------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------- |
| `@theme` block, not per-component stylesheets                | Tailwind v4 builds utilities from this block at compile time; hot-swap on toggle without React re-render |
| `.dark` rebinds the same `--color-*` names                   | One utility (`bg-canvas`) auto-adapts; there is no `bg-canvas-dark` (§ 3.1.2.)                           |
| `.dark` set on `<html>` server-side from cookie before paint | Prevents flash of wrong theme on first render                                                            |
| `@custom-variant dark` enables `dark:` prefix                | Component-level overrides like `dark:bg-surface-soft`, `dark:text-meta`, `dark:hover:bg-surface-strong`  |

## 4.4. Toggle UI.

Theme toggle uses `icon-button-circle` (§ 5.1.) with three cyclic states. Cycle: `auto → light → dark → auto`. Icons: half-sun-half-moon / sun / moon.

Cycle `auto → light → dark` (not `auto → dark`) so a user wanting OS-sync after exploring fixed modes returns to `auto` within one full revolution.

## 4.5. Theme × Locale Composition.

Theme = value swap of `--color-*` (rebound on `.dark`). Locale = value swap of `--text-*` (rebound on `:lang(ko)` — § 3.2.3.). Both axes operate on the CSS-variable layer that Tailwind utilities read from, so a single `text-display-xl text-ink` markup adapts to all four (theme × locale) combinations with no per-state class.

```css
@theme {
  --text-display-xl: 40px;
  --text-display-xl--line-height: 1.15;
  --text-display-xl--letter-spacing: -0.5px;
  --text-display-xl--font-weight: 700;
}

:lang(ko) {
  --text-display-xl: 32px;
  --text-display-xl--line-height: 1.3;
  --text-display-xl--letter-spacing: 0;
}
```

```jsx
<h1 className="text-display-xl text-ink">{title}</h1>
```

Six render states (3 themes × 2 locales) handled with no per-state class duplication.

# 5. Components.

Components are compositions of Tailwind utilities — no component stylesheets, no inline hex / px (§ 6.1.). Token references in the tables below map directly to Tailwind classes: `primary` → `bg-primary` / `text-primary`, `sm` → `rounded-sm`, `md` → `p-md` / `gap-md`, `display-xl` → `text-display-xl`, etc.

## 5.1. Buttons.

### 5.1.1. Variant Geometry.

| Variant                     | Background (rest) | Text         | Radius | Padding    | Height | Border                |
| --------------------------- | ----------------- | ------------ | ------ | ---------- | ------ | --------------------- |
| `button-primary`            | `primary`         | `on-primary` | `sm`   | 12×16      | 44     | none                  |
| `button-secondary`          | `canvas`          | `ink`        | `sm`   | 12×16      | 44     | 1px `hairline-strong` |
| `button-ghost`              | transparent       | `ink`        | `sm`   | 8×16       | auto   | none                  |
| `button-link`               | transparent       | `primary`    | —      | 0          | auto   | none                  |
| `button-pill-filter`        | `canvas`          | `body`       | `full` | 8×16       | 36     | 1px `hairline`        |
| `button-pill-filter-active` | `ink`             | `on-ink`     | `full` | 8×16       | 36     | none                  |
| `icon-button-circle`        | `surface-soft`    | `ink`        | `full` | 8 (square) | 36×36  | none                  |

Active filter uses `ink`, not `primary` (§ 3.1.1.).

### 5.1.2. State Matrix.

Pseudo-class mapping is fixed: `pressed` ↔ `:active`; `hover` ↔ `:hover`; `disabled` ↔ `:disabled`; `focus` ↔ `:focus-visible` (keyboard-only — no focus ring on mouse click). Every interactive variant defines all five states. State tokens already encode dark via `.dark` rebind (§ 3.1.2.) — no `dark:` prefix is needed for the **value** swap; `dark:` prefixes appear only when a different **token** is chosen per theme (see § 3.1.6. caveat for `surface-soft` skip on dark).

| Variant                      | `:hover`                                                 | `:active`                        | `:disabled`                                 | `:focus-visible`                                                   |
| ---------------------------- | -------------------------------------------------------- | -------------------------------- | ------------------------------------------- | ------------------------------------------------------------------ |
| `button-primary`             | `bg-primary-hover`                                       | `bg-primary-pressed`             | `bg-primary-disabled`, `cursor-not-allowed` | `ring-2 ring-primary ring-offset-2 ring-offset-canvas`             |
| `button-secondary` (light)   | `bg-surface-soft`                                        | `bg-surface-strong`              | `opacity-50`, `cursor-not-allowed`          | `ring-2 ring-primary ring-offset-2 ring-offset-canvas`             |
| `button-secondary` (dark)    | `bg-surface-strong`                                      | `bg-surface-pressed`             | `opacity-50`, `cursor-not-allowed`          | (same)                                                             |
| `button-ghost` (light)       | `bg-surface-soft`                                        | `bg-surface-strong`              | `opacity-50`, `cursor-not-allowed`          | `ring-2 ring-primary ring-offset-2 ring-offset-canvas`             |
| `button-ghost` (dark)        | `bg-surface-strong`                                      | `bg-surface-pressed`             | `opacity-50`, `cursor-not-allowed`          | (same)                                                             |
| `button-link`                | `text-primary-hover`, `underline`                        | `text-primary-pressed`           | `opacity-50`, `cursor-not-allowed`          | `ring-2 ring-primary ring-offset-2` (offset on text bbox)          |
| `button-pill-filter` (light) | `bg-surface-soft`, `text-ink`                            | `bg-surface-strong`, `text-ink`  | `opacity-50`, `cursor-not-allowed`          | `ring-2 ring-primary ring-offset-2 ring-offset-canvas`             |
| `button-pill-filter` (dark)  | `bg-surface-strong`, `text-ink`                          | `bg-surface-pressed`, `text-ink` | `opacity-50`, `cursor-not-allowed`          | (same)                                                             |
| `button-pill-filter-active`  | unchanged (active filter is a state, not a hover target) | `bg-ink/90`                      | n/a                                         | (same)                                                             |
| `icon-button-circle`         | `bg-surface-strong`                                      | `bg-surface-pressed`             | `opacity-50`, `cursor-not-allowed`          | `ring-2 ring-primary` (no offset — circle bbox is the visual edge) |

| Decision                                                                                                                   | Reason                                                                                                                                                                                   |
| -------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `button-primary` distinguishes `:hover` (`primary-hover`) from `:active` (`primary-pressed`) — two visually distinct steps | Conflating the two erases pointer-down feedback. Two-step Material/web convention; Δ ≈ 8% then 16% darken.                                                                               |
| `button-secondary` / `ghost` skip `surface-soft` on dark                                                                   | `surface-soft-dark` (#1c1c20) on `canvas-dark` (#131316) reads as "no change" — § 3.1.6. caveat. Hover jumps directly to `surface-strong`; active to `surface-pressed`.                  |
| `:disabled` uses `opacity-50` for non-primary variants, dedicated `primary-disabled` token for `button-primary`            | Solid-fill primary needs an explicit muted hex (opacity over photo-led card grids leaks background); transparent / canvas variants are robust under opacity.                             |
| `:focus-visible`, not `:focus`                                                                                             | Avoid focus rings on mouse click — only show on keyboard (`Tab`, arrow).                                                                                                                 |
| Focus ring is `ring-2 ring-primary` with 2px `canvas` offset (chip variants without offset on `icon-button-circle`)        | 2px ring + 2px offset = 4px total halo, matches WCAG 2.4.7 visible-focus guidance. Offset against `canvas` (auto-rebinds on dark) prevents ring blending into `surface-strong` on hover. |

### 5.1.3. `icon-button-circle` Detail.

Used for any single-glyph affordance (theme toggle, settings, etc.). § 5.1.1. row sets geometry; § 5.1.2. row sets states. Additional spec:

| Property         | Value                                                                            |
| ---------------- | -------------------------------------------------------------------------------- |
| Container        | 36×36 (square aspect, `rounded-full`)                                            |
| Icon size        | 18×18 (lucide default; 50% of container — leaves 9px optical margin)             |
| Padding          | `xs` (8px) on all sides — `(36 − 18) / 2` rounds to 9; `xs` is the closest token |
| Resting fill     | `surface-soft` (light) / `surface-soft` (dark — same token, value rebinds)       |
| `:hover`         | `surface-strong`                                                                 |
| `:active`        | `surface-pressed`                                                                |
| `:disabled`      | `opacity-50`, `cursor-not-allowed`                                               |
| `:focus-visible` | `ring-2 ring-primary` (no offset; circle is the focal element)                   |
| Icon color       | `ink` at all states; never `primary` (reserved for CTAs — § 6.1.)                |

Tap target: 36×36 is below the WCAG 2.5.5 44×44 touch-target floor. `icon-button-circle` is therefore restricted to sparse-control rails where pointer use dominates; touch-priority surfaces MUST use 44×44 alternatives.

## 5.2. Inputs.

### 5.2.1. Variant Geometry.

| Variant        | Background (rest) | Radius | Padding | Height | Border (rest)         |
| -------------- | ----------------- | ------ | ------- | ------ | --------------------- |
| `text-input`   | `canvas`          | `sm`   | 12×16   | 44     | 1px `hairline-strong` |
| `search-input` | `surface-soft`    | `full` | 12×16   | 44     | 1px `hairline`        |

Border is implemented as `ring-1 ring-inset ring-{token}` (not `border-1 border-{token}`) so focus's 2px ring expands inward without shifting layout.

### 5.2.2. State Matrix.

Pseudo-class mapping mirrors § 5.1.2.: `:hover`, `:focus-visible`, `:disabled`, `aria-invalid`. `aria-invalid="true"` is the canonical error trigger; never style errors via a class only.

| Variant        | `:hover`                             | `:focus-visible`                | `:disabled`                                                            | `aria-invalid="true"`        |
| -------------- | ------------------------------------ | ------------------------------- | ---------------------------------------------------------------------- | ---------------------------- |
| `text-input`   | `ring-ink` (1px)                     | `ring-2 ring-primary`           | `bg-surface-strong text-meta-soft cursor-not-allowed`, `ring-hairline` | `ring-2 ring-semantic-error` |
| `search-input` | `bg-surface-strong` (no ring change) | `bg-canvas ring-2 ring-primary` | `opacity-50 cursor-not-allowed`                                        | `ring-2 ring-semantic-error` |

| Decision                                                     | Reason                                                                                                                                                                |
| ------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `text-input:hover` darkens ring to `ink` (not a fill change) | Light `canvas` fill is already at the floor — escalating fill would conflict with `:disabled` (`surface-strong`). Outline is the available channel.                   |
| `search-input:hover` escalates fill to `surface-strong`      | Search input's rest is `surface-soft`, so it follows the chip-control ladder (rest → strong → pressed). Search has no `:active` because it isn't a momentary trigger. |
| `search-input:focus-visible` fills with `canvas`             | A focused search input switches to active-input semantics; `canvas` matches `text-input` focus state for visual consistency across input types.                       |
| `:disabled` fill uses `surface-strong`                       | Terminal-static fill (§ 3.1.6.) — no further escalation needed. Combined with `meta-soft` text and `hairline` (not `hairline-strong`) ring, signals "off".            |
| Errors use `aria-invalid`, not a class                       | `aria-invalid` is read by screen readers; class-only error styling fails accessibility. Helper text below uses `caption` typography, `semantic-error` color.          |

Placeholder copy renders as a positioned overlay over a `placeholder=" "` (single space) input so it can be styled, animated, or replaced with React nodes; the native placeholder is suppressed (see `input.tsx`).

## 5.3. Pagination.

`pagination-button` — 36px height, `sm` radius, `button-sm` typography, 1px `hairline` border. Active page: `ink` fill + `on-ink` text (rebinds on dark — § 3.1.2.).

Mobile defaults to "Load more" single button. Tablet+ uses numbered pagination.

## 5.4. Empty State.

`empty-state` — `surface-soft` card, `body-md` `meta`, `md` radius, `2xl` padding, 1px `hairline-soft` border. Used wherever a list, panel, or section has no content to render.

## 5.5. Modal.

`canvas` surface, `lg` radius. Scrim per § 3.5. Desktop 480px wide; mobile full-bleed slide-up.

# 6. Rules.

## 6.1. Do.

- Use `primary` for primary-action affordances and links only; reserve `like-active` strictly for its dedicated toggle state (§ 3.1.1.).
- Use one of the seven category tint pairs (§ 3.1.3.) for any tag/chip needing a categorical hue — never invent a tint.
- Use `caption-mono` for tabular numerics only (§ 3.2.2., § 3.2.4.).
- Use 1px `hairline` (or `-dark`) as default elevation (§ 3.5.).
- Apply `:lang(ko)` overrides for every display token (§ 3.2.3.).
- Reference colours via Tailwind utilities (`bg-*`, `text-*`, `border-*`) or `var(--color-*)` directly — never hard-code hex (§ 4.3.).
- Set the `.dark` class on `<html>` server-side from cookie before first paint (§ 4.3.).
- Define `-dark` companion for every new colour token; verify ≥ WCAG AA against `canvas-dark` (§ 3.1.2., § 3.1.5.).
- Start every interactive component's resting fill at `canvas` or `surface-soft` (§ 3.1.6.); never higher.
- Define the full state matrix (`:hover`, `:active`, `:disabled`, `:focus-visible`) for every interactive variant (§ 5.1.2., § 5.2.2.); never ship a variant with only base + disabled.
- Use `:focus-visible` (not `:focus`) for keyboard rings (§ 5.1.2.).
- Trigger error styling via `aria-invalid="true"` (§ 5.2.2.) — class-only error styling fails accessibility.

## 6.2. Don't.

- No serif anywhere — light or dark, Korean or English (§ 3.2.1.).
- No negative letter-spacing on Korean (§ 3.2.4.).
- No `primary` on stateful selection or toggle markers; use `ink` instead (e.g. `button-pill-filter-active` — § 3.1.1., § 5.1.1.).
- No `like-active` outside its dedicated toggle state (§ 3.1.1.).
- No second accent colour — extend via category tints first (§ 3.1.3.).
- No default shadow (§ 3.5.).
- No Hangul in `caption-mono` (§ 3.2.4.).
- No `display-*`, `title-*`, or `body-*` on categorical navigation labels; use `nav-*` (§ 3.2.2.).
- No second-nav size hierarchy by font weight alone (§ 3.2.2.).
- No `rounded.xl` (24px) without explicit justification (§ 3.4.).
- No background gradients (§ 3.1.).
- No #000000 canvas-dark; no #ffffff ink-dark; no light tints on dark canvas; no 50% scrim on dark (§ 3.1.5., § 3.5.).
- No mechanical surface-ladder inversion (§ 3.1.5.).
- No `surface-strong` or `surface-pressed` as an interactive component's resting fill (§ 3.1.6.).
- No fifth surface tier (`surface-stronger`, `surface-deeper`, …) (§ 3.1.6.).
- No `hover:bg-primary-pressed` shorthand (§ 5.1.2.).
- No conflated hover/active on chip controls (§ 5.1.2.).
- No `:focus` rings on mouse click — use `:focus-visible` (§ 5.1.2.).

# 7. Known Gaps.

| Gap                             | Recommendation                                                                                            |
| ------------------------------- | --------------------------------------------------------------------------------------------------------- |
| Skeleton / loading states       | `surface-strong` rectangles (terminal-static fill — § 3.1.6.), 1.5s pulse alternating with `surface-soft` |
| Languages beyond KR/EN          | Per-script density evaluation (§ 3.2.3.)                                                                  |
| OS theme transition mid-session | `transition: background-color 200ms, color 200ms, border-color 200ms` on `<html>` and `<body>`            |
