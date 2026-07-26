const META_DESCRIPTION_MAX_LENGTH = 200;

export function toMetaDescription(markdown: string): string {
  const text = markdownToPlainText(markdown);

  return text.length <= META_DESCRIPTION_MAX_LENGTH
    ? text
    : `${text.slice(0, META_DESCRIPTION_MAX_LENGTH).trimEnd()}…`;
}

export function markdownToPlainText(markdown: string) {
  return markdown
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/^\s{0,3}#{1,6}\s+/gm, "")
    .replace(/^\s{0,3}>\s?/gm, "")
    .replace(/^\s*[-*+]\s+/gm, "")
    .replace(/^\s*\d+\.\s+/gm, "")
    .replace(/!\[([^\]]*)]\([^)]+\)/g, "$1")
    .replace(/\[([^\]]+)]\([^)]+\)/g, "$1")
    .replace(/\[([^\]]+)]\[[^\]]*]/g, "$1")
    .replace(/^\s*\[[^\]]+]:\s+\S+.*$/gm, "")
    .replace(/<\/?[^>]+>/g, "")
    .replace(/[*_~>#|]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}
