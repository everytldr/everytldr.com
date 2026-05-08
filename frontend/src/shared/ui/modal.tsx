"use client";

import { cn } from "@/shared/lib";
import type { PropsWithChildren } from "react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "./dialog";

type ModalProps = PropsWithChildren<{
  className?: string;
  headerClassName?: string;
  hideCloseButton?: boolean;
  isOpen: boolean;
  position?: "center" | "top";
  header: {
    title: string;
    description?: string;
  };
  onClose: () => void;
}>;

export function Modal({
  className,
  headerClassName,
  hideCloseButton = false,
  isOpen,
  position = "center",
  header,
  children,
  onClose,
}: ModalProps) {
  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent
        className={cn(position === "top" && "top-2xl translate-y-0", className)}
        showCloseButton={!hideCloseButton}
      >
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
