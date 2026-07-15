"use client";

import { cn } from "@/shared/lib";
import type { PropsWithChildren } from "react";
import { Drawer, DrawerContent, DrawerDescription, DrawerTitle } from "./drawer";

type BottomSheetProps = PropsWithChildren<{
  className?: string;
  isOpen: boolean;
  header: {
    className?: string;
    title: string;
    description?: string;
  };
  onClose: () => void;
}>;

export function BottomSheet({ className, isOpen, header, children, onClose }: BottomSheetProps) {
  return (
    <Drawer open={isOpen} direction="bottom" onOpenChange={handleOpenChange}>
      <DrawerContent
        className={cn(
          "mx-sm mb-sm flex h-auto! max-h-[calc(90dvh-var(--spacing-sm))] flex-col gap-y-sm overflow-hidden rounded-lg border border-hairline bg-canvas p-md pb-0 shadow-floating",
          className,
        )}
      >
        <div className="mx-auto block h-1.5 w-12 shrink-0 rounded-full bg-hairline-strong" />
        <div className="scrollbar-hidden space-y-xs overflow-y-auto overscroll-contain">
          <div className={cn("space-y-2xs", header.className)}>
            <DrawerTitle className="text-center text-title-md text-ink">{header.title}</DrawerTitle>
            {header.description && (
              <DrawerDescription className="text-center whitespace-pre-line">
                {header.description}
              </DrawerDescription>
            )}
          </div>
          {children}
        </div>
      </DrawerContent>
    </Drawer>
  );

  function handleOpenChange(open: boolean) {
    if (open) {
      return;
    }
    onClose();
  }
}
