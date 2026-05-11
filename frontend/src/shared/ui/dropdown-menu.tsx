"use client";

import { cn } from "@/shared/lib";
import { Check } from "lucide-react";
import { DropdownMenu as DropdownMenuPrimitive } from "radix-ui";
import type { ComponentProps } from "react";

type DropdownMenuProps = ComponentProps<typeof DropdownMenuPrimitive.Root>;

export function DropdownMenu({ ...props }: DropdownMenuProps) {
  return <DropdownMenuPrimitive.Root data-slot="dropdown-menu" {...props} />;
}

type DropdownMenuTriggerProps = ComponentProps<typeof DropdownMenuPrimitive.Trigger>;

export function DropdownMenuTrigger({ ...props }: DropdownMenuTriggerProps) {
  return <DropdownMenuPrimitive.Trigger data-slot="dropdown-menu-trigger" {...props} />;
}

type DropdownMenuPortalProps = ComponentProps<typeof DropdownMenuPrimitive.Portal>;

export function DropdownMenuPortal({ ...props }: DropdownMenuPortalProps) {
  return <DropdownMenuPrimitive.Portal data-slot="dropdown-menu-portal" {...props} />;
}

type DropdownMenuContentProps = ComponentProps<typeof DropdownMenuPrimitive.Content>;

export function DropdownMenuContent({
  className,
  sideOffset = 6,
  ...props
}: DropdownMenuContentProps) {
  return (
    <DropdownMenuPrimitive.Portal>
      <DropdownMenuPrimitive.Content
        className={cn(
          "z-50 min-w-32 origin-(--radix-dropdown-menu-content-transform-origin) overflow-hidden rounded-md border border-hairline bg-canvas p-2xs text-ink shadow-hover outline-none data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95 dark:bg-surface-soft",
          className,
        )}
        sideOffset={sideOffset}
        data-slot="dropdown-menu-content"
        {...props}
      />
    </DropdownMenuPrimitive.Portal>
  );
}

type DropdownMenuLabelProps = ComponentProps<typeof DropdownMenuPrimitive.Label>;

export function DropdownMenuLabel({ className, ...props }: DropdownMenuLabelProps) {
  return (
    <DropdownMenuPrimitive.Label
      className={cn("px-sm pt-xs pb-2xs text-micro text-meta uppercase", className)}
      data-slot="dropdown-menu-label"
      {...props}
    />
  );
}

type DropdownMenuItemProps = ComponentProps<typeof DropdownMenuPrimitive.Item>;

export function DropdownMenuItem({ className, ...props }: DropdownMenuItemProps) {
  return (
    <DropdownMenuPrimitive.Item
      className={cn(
        "relative flex h-9 cursor-pointer items-center gap-sm rounded-sm px-sm text-button-sm text-ink transition-colors outline-none select-none data-[disabled]:cursor-not-allowed data-[disabled]:opacity-50 data-[highlighted]:bg-surface-soft dark:data-[highlighted]:bg-surface-strong",
        className,
      )}
      data-slot="dropdown-menu-item"
      {...props}
    />
  );
}

type DropdownMenuRadioGroupProps = ComponentProps<typeof DropdownMenuPrimitive.RadioGroup>;

export function DropdownMenuRadioGroup({ ...props }: DropdownMenuRadioGroupProps) {
  return <DropdownMenuPrimitive.RadioGroup data-slot="dropdown-menu-radio-group" {...props} />;
}

type DropdownMenuRadioItemProps = ComponentProps<typeof DropdownMenuPrimitive.RadioItem>;

export function DropdownMenuRadioItem({
  className,
  children,
  ...props
}: DropdownMenuRadioItemProps) {
  return (
    <DropdownMenuPrimitive.RadioItem
      className={cn(
        "relative flex h-9 cursor-pointer items-center justify-between gap-sm rounded-sm px-sm text-button-sm text-ink transition-colors outline-none select-none data-[disabled]:cursor-not-allowed data-[disabled]:opacity-50 data-[highlighted]:bg-surface-soft dark:data-[highlighted]:bg-surface-strong",
        className,
      )}
      data-slot="dropdown-menu-radio-item"
      {...props}
    >
      <span>{children}</span>
      <DropdownMenuPrimitive.ItemIndicator>
        <Check className="size-4 text-ink" />
      </DropdownMenuPrimitive.ItemIndicator>
    </DropdownMenuPrimitive.RadioItem>
  );
}

type DropdownMenuSeparatorProps = ComponentProps<typeof DropdownMenuPrimitive.Separator>;

export function DropdownMenuSeparator({ className, ...props }: DropdownMenuSeparatorProps) {
  return (
    <DropdownMenuPrimitive.Separator
      className={cn("my-2xs h-px bg-hairline", className)}
      data-slot="dropdown-menu-separator"
      {...props}
    />
  );
}
