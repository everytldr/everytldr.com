"use client";

import { cn } from "@/shared/lib";
import type { PropsWithChildren } from "react";
import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle } from "./drawer";

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
      <DrawerContent className={cn("pb-xl", className)}>
        <DrawerHeader className={header.className}>
          <DrawerTitle>{header.title}</DrawerTitle>
          {header.description && <DrawerDescription>{header.description}</DrawerDescription>}
        </DrawerHeader>
        {children}
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
