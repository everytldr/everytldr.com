"use client";

import { useIsCoarsePointer } from "@/shared/hooks";
import { cn } from "@/shared/lib";
import type { PropsWithChildren } from "react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "./dialog";
import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle } from "./drawer";

type ResponsiveDialogProps = PropsWithChildren<{
  className?: string;
  headerClassName?: string;
  isOpen: boolean;
  header: {
    title: string;
    description?: string;
  };
  onClose: () => void;
}>;

export function ResponsiveDialog({
  className,
  headerClassName,
  isOpen,
  header,
  children,
  onClose,
}: ResponsiveDialogProps) {
  const isCoarsePointer = useIsCoarsePointer();

  if (isCoarsePointer) {
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
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent className={className}>
        <DialogHeader className={headerClassName}>
          <DialogTitle>{header.title}</DialogTitle>
          {header.description && <DialogDescription>{header.description}</DialogDescription>}
        </DialogHeader>
        {children}
      </DialogContent>
    </Dialog>
  );

  function handleOpenChange(open: boolean) {
    if (open) {
      return;
    }
    onClose();
  }
}
