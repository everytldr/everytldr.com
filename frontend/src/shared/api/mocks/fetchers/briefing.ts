import type { BriefingDetailResponse, BriefingListItem, BriefingListResponse } from "@/shared/api";
import { A_DAY } from "@/shared/lib";
import { take, times } from "lodash-es";
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

const ALL_BRIEFINGS: BriefingListItem[] = times(BRIEFING_COUNT, (index) => ({
  date: toDateString(index + 1),
  title: `Daily Briefing ${index + 1}: AI Rules, Market Signals, and Renewed Talks`,
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

export const getBriefing = ({ params: { date } }: { params: { date: string } }) => {
  const briefing = ALL_BRIEFINGS.find((item) => item.date === date);

  if (!briefing) {
    return new HttpResponse(null, { status: 404 });
  }

  const responseData: BriefingDetailResponse = {
    date: briefing.date,
    title: briefing.title,
    content: MOCK_CONTENT,
    articles: take(ALL_ARTICLES, BRIEFING_ARTICLE_COUNT),
  };

  return HttpResponse.json(responseData);
};
