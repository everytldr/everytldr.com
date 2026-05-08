"use client";

import { cn } from "@/shared/lib";
import type { ComponentProps } from "react";
import { Drawer as DrawerPrimitive } from "vaul";

type DrawerProps = ComponentProps<typeof DrawerPrimitive.Root>;

export function Drawer({ ...props }: DrawerProps) {
  return <DrawerPrimitive.Root data-slot="drawer" {...props} />;
}

type DrawerTriggerProps = ComponentProps<typeof DrawerPrimitive.Trigger>;

export function DrawerTrigger({ ...props }: DrawerTriggerProps) {
  return <DrawerPrimitive.Trigger data-slot="drawer-trigger" {...props} />;
}

type DrawerPortalProps = ComponentProps<typeof DrawerPrimitive.Portal>;

export function DrawerPortal({ ...props }: DrawerPortalProps) {
  return <DrawerPrimitive.Portal data-slot="drawer-portal" {...props} />;
}

type DrawerCloseProps = ComponentProps<typeof DrawerPrimitive.Close>;

export function DrawerClose({ ...props }: DrawerCloseProps) {
  return <DrawerPrimitive.Close data-slot="drawer-close" {...props} />;
}

type DrawerOverlayProps = ComponentProps<typeof DrawerPrimitive.Overlay>;

export function DrawerOverlay({ className, ...props }: DrawerOverlayProps) {
  return (
    <DrawerPrimitive.Overlay
      className={cn(
        "fixed inset-0 z-50 bg-scrim/50 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0 dark:bg-scrim/65",
        className,
      )}
      data-slot="drawer-overlay"
      {...props}
    />
  );
}

type DrawerContentProps = ComponentProps<typeof DrawerPrimitive.Content>;

export function DrawerContent({ className, children, ...props }: DrawerContentProps) {
  return (
    <DrawerPortal>
      <DrawerOverlay />
      <DrawerPrimitive.Content
        className={cn(
          "group/drawer-content fixed z-50 flex h-auto flex-col bg-canvas",
          "data-[vaul-drawer-direction=top]:inset-x-0 data-[vaul-drawer-direction=top]:top-0 data-[vaul-drawer-direction=top]:mb-24 data-[vaul-drawer-direction=top]:max-h-[80vh] data-[vaul-drawer-direction=top]:rounded-b-lg data-[vaul-drawer-direction=top]:border-b data-[vaul-drawer-direction=top]:border-hairline",
          "data-[vaul-drawer-direction=bottom]:inset-x-0 data-[vaul-drawer-direction=bottom]:bottom-0 data-[vaul-drawer-direction=bottom]:mt-24 data-[vaul-drawer-direction=bottom]:max-h-[80vh] data-[vaul-drawer-direction=bottom]:rounded-t-lg data-[vaul-drawer-direction=bottom]:border-t data-[vaul-drawer-direction=bottom]:border-hairline",
          "data-[vaul-drawer-direction=right]:inset-y-0 data-[vaul-drawer-direction=right]:right-0 data-[vaul-drawer-direction=right]:w-3/4 data-[vaul-drawer-direction=right]:border-l data-[vaul-drawer-direction=right]:border-hairline data-[vaul-drawer-direction=right]:pc:max-w-sm",
          "data-[vaul-drawer-direction=left]:inset-y-0 data-[vaul-drawer-direction=left]:left-0 data-[vaul-drawer-direction=left]:w-3/4 data-[vaul-drawer-direction=left]:border-r data-[vaul-drawer-direction=left]:border-hairline data-[vaul-drawer-direction=left]:pc:max-w-sm",
          className,
        )}
        data-slot="drawer-content"
        {...props}
      >
        <div className="mx-auto mt-md hidden h-1.5 w-12 shrink-0 rounded-full bg-hairline-strong group-data-[vaul-drawer-direction=bottom]/drawer-content:block" />
        {children}
      </DrawerPrimitive.Content>
    </DrawerPortal>
  );
}

type DrawerHeaderProps = ComponentProps<"div">;

export function DrawerHeader({ className, ...props }: DrawerHeaderProps) {
  return (
    <div
      className={cn(
        "flex flex-col gap-2xs p-md group-data-[vaul-drawer-direction=bottom]/drawer-content:text-center group-data-[vaul-drawer-direction=top]/drawer-content:text-center pc:gap-xs pc:text-left",
        className,
      )}
      data-slot="drawer-header"
      {...props}
    />
  );
}

type DrawerFooterProps = ComponentProps<"div">;

export function DrawerFooter({ className, ...props }: DrawerFooterProps) {
  return (
    <div
      className={cn("mt-auto flex flex-col gap-xs p-md", className)}
      data-slot="drawer-footer"
      {...props}
    />
  );
}

type DrawerTitleProps = ComponentProps<typeof DrawerPrimitive.Title>;

export function DrawerTitle({ className, ...props }: DrawerTitleProps) {
  return (
    <DrawerPrimitive.Title
      className={cn("text-display-sm text-ink", className)}
      data-slot="drawer-title"
      {...props}
    />
  );
}

type DrawerDescriptionProps = ComponentProps<typeof DrawerPrimitive.Description>;

export function DrawerDescription({ className, ...props }: DrawerDescriptionProps) {
  return (
    <DrawerPrimitive.Description
      className={cn("text-body-sm text-meta", className)}
      data-slot="drawer-description"
      {...props}
    />
  );
}
