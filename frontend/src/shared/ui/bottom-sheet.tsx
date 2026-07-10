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
    <Drawer open={isOpen} onOpenChange={handleOpenChange}>
      <DrawerContent
        className={cn(
          "mx-sm mb-sm overflow-hidden rounded-lg border pb-md shadow-floating",
          className,
        )}
      >
        <div className={cn("px-md pt-md pb-2xs", header.className)}>
          <DrawerTitle className="text-center text-title-md text-ink">{header.title}</DrawerTitle>
          {header.description && (
            <DrawerDescription className="mt-2xs text-center whitespace-pre-line">
              {header.description}
            </DrawerDescription>
          )}
        </div>
        <div className="max-h-[70vh] min-h-0 overflow-y-auto overscroll-contain">{children}</div>
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
