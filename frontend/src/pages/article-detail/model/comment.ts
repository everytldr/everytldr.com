import type { ArticleCommentListItem } from "@/shared/api";
import { type Nullable } from "@/shared/lib";

export const MIN_PASSWORD_LENGTH = 4;
export const MAX_PASSWORD_LENGTH = 100;

export type CommentNode = ArticleCommentListItem & {
  children: CommentNode[];
};

export function buildCommentTree(comments: ArticleCommentListItem[]): CommentNode[] {
  const nodes = comments.map((comment) => ({ ...comment, children: [] }));
  const nodesById = new Map<string, CommentNode>();
  const roots: CommentNode[] = [];

  for (const node of nodes) {
    nodesById.set(node.id, node);
  }

  for (const node of nodes) {
    const parent = findParent(node.parentId, nodesById);
    if (parent) {
      parent.children.push(node);
      continue;
    }
    roots.push(node);
  }

  return roots;
}

function findParent(
  parentId: ArticleCommentListItem["parentId"],
  nodesById: Map<string, CommentNode>,
): Nullable<CommentNode> {
  if (parentId === null) {
    return null;
  }
  return nodesById.get(parentId) ?? null;
}
