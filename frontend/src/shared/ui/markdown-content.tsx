import { cn } from "@/shared/lib";
import type { Components } from "react-markdown";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

type MarkdownContentProps = {
  className?: string;
  markdown: string;
};

const components = {
  a: ({ className, ...props }) => (
    <a
      className={cn(
        "text-primary underline underline-offset-4 outline-none hover:text-primary-hover focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:text-primary-pressed",
        className,
      )}
      target="_blank"
      rel="noreferrer"
      {...props}
    />
  ),
  blockquote: ({ className, ...props }) => (
    <blockquote
      className={cn("border-l border-hairline-strong pl-md text-meta", className)}
      {...props}
    />
  ),
  code: ({ className, ...props }) => (
    <code
      className={cn("rounded-sm bg-surface-soft px-2xs py-2xs text-body-sm text-ink", className)}
      {...props}
    />
  ),
  h1: ({ className, ...props }) => (
    <h2 className={cn("pt-lg text-display-lg text-ink", className)} {...props} />
  ),
  h2: ({ className, ...props }) => (
    <h3 className={cn("pt-md text-display-md text-ink", className)} {...props} />
  ),
  h3: ({ className, ...props }) => (
    <h4 className={cn("pt-sm text-display-sm text-ink", className)} {...props} />
  ),
  h4: ({ className, ...props }) => (
    <h5 className={cn("pt-xs text-title-md text-ink", className)} {...props} />
  ),
  h5: ({ className, ...props }) => (
    <h6 className={cn("pt-xs text-title-sm text-ink", className)} {...props} />
  ),
  h6: ({ className, ...props }) => (
    <h6 className={cn("pt-xs text-title-sm text-meta", className)} {...props} />
  ),
  hr: ({ className, ...props }) => (
    <hr className={cn("border-hairline-soft", className)} {...props} />
  ),
  img: ({ className, alt, ...props }) => (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      className={cn("h-auto w-full rounded-md bg-surface-soft", className)}
      alt={alt ?? ""}
      loading="lazy"
      decoding="async"
      {...props}
    />
  ),
  li: ({ className, ...props }) => <li className={cn("pl-xs", className)} {...props} />,
  ol: ({ className, ...props }) => (
    <ol className={cn("list-decimal space-y-2xs pl-lg", className)} {...props} />
  ),
  p: ({ className, ...props }) => <p className={cn("text-body", className)} {...props} />,
  pre: ({ className, ...props }) => (
    <pre
      className={cn(
        "overflow-x-auto rounded-md bg-surface-soft p-md text-body-sm text-ink",
        className,
      )}
      {...props}
    />
  ),
  table: ({ className, ...props }) => (
    <div className="overflow-x-auto">
      <table className={cn("w-full border-collapse text-body-sm", className)} {...props} />
    </div>
  ),
  tbody: ({ className, ...props }) => (
    <tbody className={cn("divide-y divide-hairline", className)} {...props} />
  ),
  td: ({ className, ...props }) => (
    <td className={cn("border border-hairline px-sm py-xs align-top", className)} {...props} />
  ),
  th: ({ className, ...props }) => (
    <th
      className={cn(
        "border border-hairline bg-surface-soft px-sm py-xs text-left text-title-sm text-ink",
        className,
      )}
      {...props}
    />
  ),
  thead: ({ className, ...props }) => <thead className={cn("text-ink", className)} {...props} />,
  ul: ({ className, ...props }) => (
    <ul className={cn("list-disc space-y-2xs pl-lg", className)} {...props} />
  ),
} satisfies Components;

export function MarkdownContent({ className, markdown }: MarkdownContentProps) {
  return (
    <div className={cn("space-y-md text-body-lg text-body", className)}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {markdown}
      </ReactMarkdown>
    </div>
  );
}
