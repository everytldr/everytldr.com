import type {
  ArticleBriefingResponse,
  BriefingDetailResponse,
  BriefingListItem,
  BriefingListResponse,
  SitemapBriefingListResponse,
} from "@/shared/api";
import { A_DAY } from "@/shared/lib";
import { drop, take, times } from "lodash-es";
import { HttpResponse } from "msw";
import { ALL_ARTICLES } from "./article";

const BRIEFING_COUNT = 30;
const BRIEFING_ARTICLE_COUNT = 10;

const MOCK_CONTENT = `Markets steadied while regulators sharpened their focus on AI, and diplomatic talks resumed on two fronts. The day's most-read stories cluster around policy catching up with technology.

## AI regulation moves from talk to drafts

Lawmakers on both sides of the Atlantic circulated draft rules that would require model-risk disclosures for large deployments. Industry groups pushed back on audit timelines, while two major labs signaled they would comply early.

## Markets digest a soft-landing signal

Equities closed mixed after inflation data came in below forecasts. Bond yields eased, and analysts read the print as room for one more rate cut this year.

## Diplomacy returns to the table

Cease-fire negotiators reconvened with a narrower agenda focused on humanitarian corridors, and a separate trade dispute saw its first ministerial meeting in months.`;

const MOCK_EXCERPT = MOCK_CONTENT.split("\n\n")[0];

const ALL_BRIEFINGS: BriefingListItem[] = times(BRIEFING_COUNT, (index) => ({
  date: toDateString(index + 1),
  title: `Daily Briefing ${index + 1}: AI Rules, Market Signals, and Renewed Talks`,
  excerpt: MOCK_EXCERPT,
}));

function toDateString(daysAgo: number) {
  return new Date(Date.now() - daysAgo * A_DAY).toISOString().slice(0, 10);
}

export const listBriefings = ({ request }: { request: Request }) => {
  const url = new URL(request.url);
  const cursor = url.searchParams.get("cursor");
  const size = Number(url.searchParams.get("size") ?? "20");

  const filtered = cursor
    ? ALL_BRIEFINGS.filter((briefing) => briefing.date < cursor)
    : ALL_BRIEFINGS;
  const items = take(filtered, size);
  const nextCursor = filtered.length > size ? items[items.length - 1].date : null;

  const responseData: BriefingListResponse = {
    items,
    nextCursor,
  };

  return HttpResponse.json(responseData);
};

export const listSitemapBriefings = ({ request }: { request: Request }) => {
  const url = new URL(request.url);
  const page = Number(url.searchParams.get("page") ?? "0");
  const size = Number(url.searchParams.get("size") ?? "2000");

  const items = take(drop(ALL_BRIEFINGS, page * size), size).map((briefing, index) => ({
    date: briefing.date,
    languages: index % 5 === 0 ? ["en"] : ["en", "ko"],
  }));

  const responseData: SitemapBriefingListResponse = {
    items,
    total: ALL_BRIEFINGS.length,
  };

  return HttpResponse.json(responseData);
};

const BRIEFING_ARTICLE_IDS = new Set(
  take(ALL_ARTICLES, BRIEFING_ARTICLE_COUNT).map((article) => article.id),
);

export const getArticleBriefing = ({
  params: { articleId },
}: {
  params: { articleId: string };
}) => {
  if (!BRIEFING_ARTICLE_IDS.has(articleId)) {
    return new HttpResponse(null, { status: 404 });
  }

  const briefing = ALL_BRIEFINGS[0];
  const responseData: ArticleBriefingResponse = {
    date: briefing.date,
    title: briefing.title,
  };

  return HttpResponse.json(responseData);
};

export const getBriefing = ({ params: { date } }: { params: { date: string } }) => {
  const index = ALL_BRIEFINGS.findIndex((item) => item.date === date);

  if (index === -1) {
    return new HttpResponse(null, { status: 404 });
  }

  const briefing = ALL_BRIEFINGS[index];

  const responseData: BriefingDetailResponse = {
    date: briefing.date,
    title: briefing.title,
    content: MOCK_CONTENT,
    requiresShareAlike: false,
    articles: take(ALL_ARTICLES, BRIEFING_ARTICLE_COUNT),
    previousDate: ALL_BRIEFINGS[index + 1]?.date ?? null,
    nextDate: ALL_BRIEFINGS[index - 1]?.date ?? null,
  };

  return HttpResponse.json(responseData);
};
