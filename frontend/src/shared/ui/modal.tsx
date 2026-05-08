"use client";

import type { PropsWithChildren } from "react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "./dialog";

type ModalProps = PropsWithChildren<{
  className?: string;
  headerClassName?: string;
  isOpen: boolean;
  header: {
    title: string;
    description?: string;
  };
  onClose: () => void;
}>;

export function Modal({
  className,
  headerClassName,
  isOpen,
  header,
  children,
  onClose,
}: ModalProps) {
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
