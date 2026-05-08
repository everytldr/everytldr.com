"use client";

import { cn } from "@/shared/lib";
import type { PropsWithChildren } from "react";
import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle } from "./drawer";

type BottomSheetProps = PropsWithChildren<{
  className?: string;
  headerClassName?: string;
  isOpen: boolean;
  header: {
    title: string;
    description?: string;
  };
  onClose: () => void;
}>;

export function BottomSheet({
  className,
  headerClassName,
  isOpen,
  header,
  children,
  onClose,
}: BottomSheetProps) {
  return (
    <Drawer open={isOpen} onOpenChange={handleOpenChange}>
      <DrawerContent className={cn("pb-xl", className)}>
        <DrawerHeader className={headerClassName}>
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
