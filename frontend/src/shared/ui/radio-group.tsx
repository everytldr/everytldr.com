"use client";

import { cn } from "@/shared/lib";
import { RadioGroup as RadioGroupPrimitive } from "radix-ui";
import type { ComponentProps } from "react";

type RadioGroupProps = ComponentProps<typeof RadioGroupPrimitive.Root>;

export function RadioGroup({ ...props }: RadioGroupProps) {
  return <RadioGroupPrimitive.Root data-slot="radio-group" {...props} />;
}

type RadioGroupItemProps = ComponentProps<typeof RadioGroupPrimitive.Item>;

export function RadioGroupItem({ className, ...props }: RadioGroupItemProps) {
  return (
    <RadioGroupPrimitive.Item
      className={cn(
        "cursor-pointer transition-colors outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
      data-slot="radio-group-item"
      {...props}
    />
  );
}

type RadioGroupIndicatorProps = ComponentProps<typeof RadioGroupPrimitive.Indicator>;

export function RadioGroupIndicator({ ...props }: RadioGroupIndicatorProps) {
  return <RadioGroupPrimitive.Indicator data-slot="radio-group-indicator" {...props} />;
}
