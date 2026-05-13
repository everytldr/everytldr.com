"use client";

import { cn } from "@/shared/lib";
import type { PropsWithChildren } from "react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "./dialog";

type ModalProps = PropsWithChildren<{
  className?: string;
  hideCloseButton?: boolean;
  isOpen: boolean;
  position?: "center" | "top";
  size?: "sm" | "md" | "lg";
  header: {
    className?: string;
    title: string;
    description?: string;
  };
  onClose: () => void;
}>;

export function Modal({
  className,
  hideCloseButton,
  isOpen,
  position = "center",
  size = "sm",
  header,
  children,
  onClose,
}: ModalProps) {
  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent
        className={cn(
          { sm: "w-120", md: "w-140", lg: "w-160" }[size],
          position === "top" && "top-sm translate-y-0",
          className,
        )}
        showCloseButton={!hideCloseButton}
      >
        <DialogHeader className={header.className}>
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
