import { cn } from "@/shared/lib";
import type { ComponentProps } from "react";

type TableProps = ComponentProps<"table"> & {
  className?: string;
  tableClassName?: string;
};

export function Table({ className, tableClassName, ...props }: TableProps) {
  return (
    <div className={cn("relative w-full overflow-x-auto", className)} data-slot="table-container">
      <table
        className={cn("w-full border-collapse text-body-sm text-body", tableClassName)}
        data-slot="table"
        {...props}
      />
    </div>
  );
}

type TableHeaderProps = ComponentProps<"thead">;

export function TableHeader({ className, ...props }: TableHeaderProps) {
  return (
    <thead
      className={cn("[&_tr]:border-b [&_tr]:border-hairline", className)}
      data-slot="table-header"
      {...props}
    />
  );
}

type TableBodyProps = ComponentProps<"tbody">;

export function TableBody({ className, ...props }: TableBodyProps) {
  return (
    <tbody
      className={cn(
        "[&_tr]:border-b [&_tr]:border-hairline-soft [&_tr:last-child]:border-0",
        className,
      )}
      data-slot="table-body"
      {...props}
    />
  );
}

type TableFooterProps = ComponentProps<"tfoot">;

export function TableFooter({ className, ...props }: TableFooterProps) {
  return (
    <tfoot
      className={cn("border-t border-hairline bg-surface-soft text-title-sm text-ink", className)}
      data-slot="table-footer"
      {...props}
    />
  );
}

type TableRowProps = ComponentProps<"tr">;

export function TableRow({ className, ...props }: TableRowProps) {
  return <tr className={className} data-slot="table-row" {...props} />;
}

type TableHeadProps = ComponentProps<"th">;

export function TableHead({ className, ...props }: TableHeadProps) {
  return (
    <th
      className={cn(
        "h-10 px-sm text-left align-middle text-caption whitespace-nowrap text-meta",
        className,
      )}
      data-slot="table-head"
      {...props}
    />
  );
}

type TableCellProps = ComponentProps<"td">;

export function TableCell({ className, ...props }: TableCellProps) {
  return (
    <td
      className={cn("px-sm py-sm align-middle whitespace-nowrap text-ink", className)}
      data-slot="table-cell"
      {...props}
    />
  );
}

type TableCaptionProps = ComponentProps<"caption">;

export function TableCaption({ className, ...props }: TableCaptionProps) {
  return (
    <caption
      className={cn("mt-md text-caption text-meta", className)}
      data-slot="table-caption"
      {...props}
    />
  );
}
