"use client";

import { cn } from "@/shared/lib";
import { Tabs as TabsPrimitive } from "radix-ui";
import type { ComponentProps } from "react";

type TabsProps = ComponentProps<typeof TabsPrimitive.Root>;

export function Tabs({ className, ...props }: TabsProps) {
  return (
    <TabsPrimitive.Root
      className={cn("flex flex-col gap-md", className)}
      data-slot="tabs"
      {...props}
    />
  );
}

type TabsListProps = ComponentProps<typeof TabsPrimitive.List>;

export function TabsList({ className, ...props }: TabsListProps) {
  return (
    <TabsPrimitive.List
      className={cn(
        "scrollbar-hidden flex h-12 items-stretch gap-xl overflow-x-auto border-b border-hairline px-md",
        className,
      )}
      data-slot="tabs-list"
      {...props}
    />
  );
}

type TabsTriggerProps = ComponentProps<typeof TabsPrimitive.Trigger>;

export function TabsTrigger({ className, ...props }: TabsTriggerProps) {
  return (
    <TabsPrimitive.Trigger
      className={cn(
        "-mb-px inline-flex cursor-pointer items-center border-b-2 border-transparent text-nav-md whitespace-nowrap text-meta transition-colors outline-none hover:text-ink focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset active:text-ink data-disabled:cursor-not-allowed data-disabled:opacity-50 data-[state=active]:border-ink data-[state=active]:text-ink",
        className,
      )}
      data-slot="tabs-trigger"
      {...props}
    />
  );
}

type TabsContentProps = ComponentProps<typeof TabsPrimitive.Content>;

export function TabsContent({ className, ...props }: TabsContentProps) {
  return (
    <TabsPrimitive.Content
      className={cn("outline-none", className)}
      data-slot="tabs-content"
      {...props}
    />
  );
}
